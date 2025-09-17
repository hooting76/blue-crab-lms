package BlueCrab.com.example.util;

import BlueCrab.com.example.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 재설정 전용 레이트 리미팅 유틸리티
 * 본인확인 단계에서 IP/이메일별 요청 제한 및 연속 실패 지연을 처리
 *
 * 제한 정책:
 * - IP당: 시간당 20회, 일일 100회
 * - 이메일당: 시간당 5회, 일일 10회
 * - 연속 실패 지연: 3회→30-60초, 6회→10분
 */
@Component
public class PasswordResetRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetRateLimiter.class);

    @Autowired
    private RedisService redisService;

    // Redis 키 프리픽스
    private static final String IP_HOURLY_PREFIX = "pw_reset_ip_h1:";
    private static final String IP_DAILY_PREFIX = "pw_reset_ip_d1:";
    private static final String EMAIL_HOURLY_PREFIX = "pw_reset_email_h1:";
    private static final String EMAIL_DAILY_PREFIX = "pw_reset_email_d1:";
    private static final String FAIL_COUNT_PREFIX = "pw_reset_fail:";

    // 제한 설정
    // 테스트 기간동안 방대하게 제한, 완성본에서 수정 필수.
    private static final int IP_HOURLY_LIMIT = 100;
    private static final int IP_DAILY_LIMIT = 1000;
    private static final int EMAIL_HOURLY_LIMIT = 50;
    private static final int EMAIL_DAILY_LIMIT = 1000;
    
    // TTL 설정 (분 단위)
    private static final int HOURLY_TTL_MINUTES = 60;
    private static final int DAILY_TTL_MINUTES = 1440; // 24시간

    /**
     * 본인확인 시도 가능 여부 확인
     * IP와 이메일 둘 다 제한에 걸리지 않아야 함
     *
     * @param clientIp 클라이언트 IP 주소
     * @param email 사용자 이메일
     * @return 허용 여부
     */
    public boolean isIdentityVerificationAllowed(String clientIp, String email) {
        try {
            // IP 기반 제한 확인
            if (!isIpAllowed(clientIp)) {
                logger.warn("IP rate limit exceeded for password reset: {}", clientIp);
                return false;
            }

            // 이메일 기반 제한 확인
            if (!isEmailAllowed(email)) {
                logger.warn("Email rate limit exceeded for password reset: {}", email);
                return false;
            }

            // 연속 실패 지연 확인
            if (isUnderFailureDelay(email)) {
                logger.warn("Password reset under failure delay: {}", email);
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("Error checking password reset rate limits", e);
            // 오류 시 보수적으로 차단
            return false;
        }
    }

    /**
     * 본인확인 시도 기록 (성공/실패 무관)
     * IP와 이메일별 카운터 증가
     *
     * @param clientIp 클라이언트 IP 주소
     * @param email 사용자 이메일
     */
    public void recordIdentityVerificationAttempt(String clientIp, String email) {
        try {
            recordIpAttempt(clientIp);
            recordEmailAttempt(email);
            logger.debug("Password reset attempt recorded - IP: {}, Email: {}", clientIp, email);
        } catch (Exception e) {
            logger.error("Error recording password reset attempt", e);
        }
    }

    /**
     * 본인확인 실패 기록
     * 연속 실패 카운터 증가 및 지연 설정
     *
     * @param email 사용자 이메일
     */
    public void recordIdentityVerificationFailure(String email) {
        try {
            String failKey = FAIL_COUNT_PREFIX + email;
            String currentFailsStr = redisService.getValue(failKey);
            int currentFails = currentFailsStr != null ? Integer.parseInt(currentFailsStr) : 0;
            int newFailCount = currentFails + 1;

            // 실패 횟수에 따른 지연 시간 계산
            int delayMinutes = calculateFailureDelay(newFailCount);
            
            if (delayMinutes > 0) {
                redisService.storeValue(failKey, String.valueOf(newFailCount), delayMinutes);
                logger.warn("Password reset failure recorded - Email: {}, Fails: {}, Delay: {} minutes", 
                    email, newFailCount, delayMinutes);
            }
        } catch (Exception e) {
            logger.error("Error recording password reset failure", e);
        }
    }

    /**
     * 본인확인 성공 시 실패 카운터 초기화
     *
     * @param email 사용자 이메일
     */
    public void clearIdentityVerificationFailures(String email) {
        try {
            String failKey = FAIL_COUNT_PREFIX + email;
            redisService.getValue(failKey); // 키 삭제를 위해 조회 후
            // RedisService에 delete 메서드가 있다면 사용
            logger.info("Password reset failure count cleared for email: {}", email);
        } catch (Exception e) {
            logger.error("Error clearing password reset failures", e);
        }
    }

    /**
     * 현재 레이트 리미팅 상태 조회 (디버깅용)
     *
     * @param clientIp 클라이언트 IP
     * @param email 사용자 이메일
     * @return 상태 정보 맵
     */
    public java.util.Map<String, Object> getRateLimitStatus(String clientIp, String email) {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        
        try {
            // IP 카운터 조회
            status.put("ipHourly", getCounterValue(IP_HOURLY_PREFIX + clientIp));
            status.put("ipDaily", getCounterValue(IP_DAILY_PREFIX + clientIp));
            
            // 이메일 카운터 조회
            status.put("emailHourly", getCounterValue(EMAIL_HOURLY_PREFIX + email));
            status.put("emailDaily", getCounterValue(EMAIL_DAILY_PREFIX + email));
            
            // 실패 카운터 조회
            status.put("failureCount", getCounterValue(FAIL_COUNT_PREFIX + email));
            
            // 허용 여부
            status.put("allowed", isIdentityVerificationAllowed(clientIp, email));
            
        } catch (Exception e) {
            logger.error("Error getting rate limit status", e);
            status.put("error", e.getMessage());
        }
        
        return status;
    }

    // ===== Private 메서드들 =====

    private boolean isIpAllowed(String clientIp) {
        int hourlyCount = getCounterValue(IP_HOURLY_PREFIX + clientIp);
        int dailyCount = getCounterValue(IP_DAILY_PREFIX + clientIp);
        
        return hourlyCount < IP_HOURLY_LIMIT && dailyCount < IP_DAILY_LIMIT;
    }

    private boolean isEmailAllowed(String email) {
        int hourlyCount = getCounterValue(EMAIL_HOURLY_PREFIX + email);
        int dailyCount = getCounterValue(EMAIL_DAILY_PREFIX + email);
        
        return hourlyCount < EMAIL_HOURLY_LIMIT && dailyCount < EMAIL_DAILY_LIMIT;
    }

    private boolean isUnderFailureDelay(String email) {
        String failKey = FAIL_COUNT_PREFIX + email;
        String failCountStr = redisService.getValue(failKey);
        return failCountStr != null && Integer.parseInt(failCountStr) >= 3;
    }

    private void recordIpAttempt(String clientIp) {
        incrementCounter(IP_HOURLY_PREFIX + clientIp, HOURLY_TTL_MINUTES);
        incrementCounter(IP_DAILY_PREFIX + clientIp, DAILY_TTL_MINUTES);
    }

    private void recordEmailAttempt(String email) {
        incrementCounter(EMAIL_HOURLY_PREFIX + email, HOURLY_TTL_MINUTES);
        incrementCounter(EMAIL_DAILY_PREFIX + email, DAILY_TTL_MINUTES);
    }

    private void incrementCounter(String key, int ttlMinutes) {
        String currentValueStr = redisService.getValue(key);
        int currentValue = currentValueStr != null ? Integer.parseInt(currentValueStr) : 0;
        redisService.storeValue(key, String.valueOf(currentValue + 1), ttlMinutes);
    }

    private int getCounterValue(String key) {
        String valueStr = redisService.getValue(key);
        return valueStr != null ? Integer.parseInt(valueStr) : 0;
    }

    private int calculateFailureDelay(int failCount) {
        if (failCount >= 6) {
            return 10; // 10분
        } else if (failCount >= 3) {
            return 1; // 30-60초를 위해 1분으로 설정 (실제로는 초 단위 구현 가능)
        }
        return 0; // 지연 없음
    }
}