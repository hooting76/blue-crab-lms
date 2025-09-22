// 작업자 : 성태준
// 비밀번호 재설정 레이트 리미터 유틸리티

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========

// ========== 외부 라이브러리 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.service.RedisService;

/* 비밀번호 재설정 전용 레이트 리미팅 유틸리티
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
    // IP 및 이메일별 카운터와 실패 기록용
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

    // 본인확인 시도 가능 여부 확인
    // IP와 이메일 둘 다 제한에 걸리지 않아야 함
    public boolean isIdentityVerificationAllowed(String clientIp, String email) {
        // 입력값

        // try-catch로 예외 처리
        try {
            if (!isIpAllowed(clientIp)) {
                // IP 제한에 걸린 경우
                logger.warn("IP rate limit exceeded for password reset: {}", clientIp);
                // 경고 로그 기록
                return false;
                // false 반환하여 차단
            } // IP 제한에 걸린 경우 끝

            if (!isEmailAllowed(email)) {
                // 이메일 제한에 걸린 경우
                logger.warn("Email rate limit exceeded for password reset: {}", email);
                // 경고 로그 기록
                return false;
                // false 반환하여 차단
            } // 이메일 제한에 걸린 경우 끝

            if (isUnderFailureDelay(email)) {
                // 지연 상태인 경우
                logger.warn("Password reset under failure delay: {}", email); 
                // 경고 로그 기록
                return false;
                // false 반환하여 차단
            } // 지연 상태인 경우 끝

            return true;
            // 모두 통과 시 true 반환하여 허용

        } catch (Exception e) {
            // 예외 처리
            logger.error("Error checking password reset rate limits", e);
            // 오류 로그 기록
            return false;
            // flase 반환하여 안전하게 차단
        } // try-catch 끝
    } // isIdentityVerificationAllowed 끝

    public void recordIdentityVerificationAttempt(String clientIp, String email) {
        // 본인확인 시도 기록 (성공/실패 무관)

        // try-catch로 예외 처리
        try {
            recordIpAttempt(clientIp);  // IP별 카운터 증가
            recordEmailAttempt(email);  // 이메일별 카운터 증가
            logger.debug("Password reset attempt recorded - IP: {}, Email: {}", clientIp, email);
            // 디버그 로그 기록
        } catch (Exception e) {
            // 예외 처리
            logger.error("Error recording password reset attempt", e);
            // 오류 로그 기록
        } // try-catch 끝
    } // recordIdentityVerificationAttempt 끝

    public void recordIdentityVerificationFailure(String email) {
        // 본인확인 실패 기록 및 지연 적용
        // 실패 횟수에 따라 지연 시간 설정
        
        // try-catch로 예외 처리
        try {
            String failKey = FAIL_COUNT_PREFIX + email; // 실패 카운터 키
            String currentFailsStr = redisService.getValue(failKey);    // 현재 실패 횟수 조회
            int currentFails = currentFailsStr != null ? Integer.parseInt(currentFailsStr) : 0; // 없으면 0
            int newFailCount = currentFails + 1;    // 실패 횟수 증가

            int delayMinutes = calculateFailureDelay(newFailCount);
            // 실패 횟수에 따른 지연 시간 계산
            
            if (delayMinutes > 0) {
                // 지연이 필요한 경우
                redisService.storeValue(failKey, String.valueOf(newFailCount), delayMinutes);
                // 실패 횟수와 지연 시간으로 저장
                logger.warn("Password reset failure recorded - Email: {}, Fails: {}, Delay: {} minutes", 
                    email, newFailCount, delayMinutes);
                // 경고 로그 기록
            } // 지연이 필요한 경우 끝
        } catch (Exception e) {
            // 예외 처리
            logger.error("Error recording password reset failure", e);
            // 오류 로그 기록
        } // try-catch 끝
    } // recordIdentityVerificationFailure 끝

    public void clearIdentityVerificationFailures(String email) {
        // 본인확인 성공 시 실패 기록 초기화

        // try-catch로 예외 처리
        try {
            String failKey = FAIL_COUNT_PREFIX + email; // 실패 카운터 키
            redisService.getValue(failKey); // 키 삭제를 위해 조회 후
            // RedisService에 delete 메서드가 있다면 사용
            logger.info("Password reset failure count cleared for email: {}", email);
            // 정보 로그 기록
        } catch (Exception e) {
            // 예외 처리
            logger.error("Error clearing password reset failures", e);
            // 오류 로그 기록
        } // try-catch 끝
    } // clearIdentityVerificationFailures 끝


    public java.util.Map<String, Object> getRateLimitStatus(String clientIp, String email) {
        // 현재 IP/이메일별 카운터 상태 조회

        java.util.Map<String, Object> status = new java.util.HashMap<>();
        // 상태 정보 맵 생성 
        
        // try-catch로 예외 처리
        try {
            // IP 카운터 조회
            status.put("ipHourly", getCounterValue(IP_HOURLY_PREFIX + clientIp));   // 시간당
            status.put("ipDaily", getCounterValue(IP_DAILY_PREFIX + clientIp));     // 일일
            
            // 이메일 카운터 조회
            status.put("emailHourly", getCounterValue(EMAIL_HOURLY_PREFIX + email));    // 시간당
            status.put("emailDaily", getCounterValue(EMAIL_DAILY_PREFIX + email));      // 일일
            
            // 실패 카운터 조회
            status.put("failureCount", getCounterValue(FAIL_COUNT_PREFIX + email)); // 연속 실패 횟수
            
            // 허용 여부
            status.put("allowed", isIdentityVerificationAllowed(clientIp, email));  // 현재 허용 상태
            
        } catch (Exception e) {
            logger.error("Error getting rate limit status", e);
            // 오류 로그 기록
            status.put("error", e.getMessage());
            // 오류 메시지 포함
        }
        
        return status;
        // 상태 맵 반환
    } // getRateLimitStatus 끝

    // ===== 내부 유틸리티 메서드 =====

    private boolean isIpAllowed(String clientIp) {
        // IP별 제한 확인
        int hourlyCount = getCounterValue(IP_HOURLY_PREFIX + clientIp); // 시간당 카운터 조회
        int dailyCount = getCounterValue(IP_DAILY_PREFIX + clientIp);   // 일일 카운터 조회

        return hourlyCount < IP_HOURLY_LIMIT && dailyCount < IP_DAILY_LIMIT;
        // 둘 다 제한 미만인 경우 반환
    } // isIpAllowed 끝

    private boolean isEmailAllowed(String email) {
        // 이메일별 제한 확인
        int hourlyCount = getCounterValue(EMAIL_HOURLY_PREFIX + email); // 시간당 카운터 조회
        int dailyCount = getCounterValue(EMAIL_DAILY_PREFIX + email);   // 일일 카운터 조회
        
        return hourlyCount < EMAIL_HOURLY_LIMIT && dailyCount < EMAIL_DAILY_LIMIT;
        // 둘 다 제한 미만인 경우 반환
    } // isEmailAllowed 끝

    private boolean isUnderFailureDelay(String email) {
        // 연속 실패에 따른 지연 상태 확인
        String failKey = FAIL_COUNT_PREFIX + email;                 // 실패 카운터 키
        String failCountStr = redisService.getValue(failKey);       // 현재 실패 횟수 조회
        return failCountStr != null && Integer.parseInt(failCountStr) >= 3;
        // 3회 이상이면 지연 상태를 반환
    } // isUnderFailureDelay 끝

    public void recordIpAttempt(String clientIp) {
        // IP별 시도 기록
        incrementCounter(IP_HOURLY_PREFIX + clientIp, HOURLY_TTL_MINUTES);  // 시간당 카운터 증가
        incrementCounter(IP_DAILY_PREFIX + clientIp, DAILY_TTL_MINUTES);    // 일일 카운터 증가
    } // recordIpAttempt 끝

    public int getCurrentIpHourlyAttempts(String clientIp) {
        // IP별 현재 시간당 시도 횟수 조회
        return getCounterValue(IP_HOURLY_PREFIX + clientIp);
        // 현재 시간당 시도 횟수 반환
    } // getCurrentIpHourlyAttempts 끝

    public int getCurrentIpDailyAttempts(String clientIp) {
        // IP별 현재 일일 시도 횟수 조회
        return getCounterValue(IP_DAILY_PREFIX + clientIp);
        // 현재 일일 시도 횟수 반환
    } // getCurrentIpDailyAttempts 끝

    private void recordEmailAttempt(String email) {
        // 이메일별 시도 기록
        incrementCounter(EMAIL_HOURLY_PREFIX + email, HOURLY_TTL_MINUTES);  // 시간당 카운터 증가
        incrementCounter(EMAIL_DAILY_PREFIX + email, DAILY_TTL_MINUTES);    // 일일 카운터 증가
    } // recordEmailAttempt 끝

    private void incrementCounter(String key, int ttlMinutes) {
        // 카운터 증가 및 TTL 설정
        String currentValueStr = redisService.getValue(key);                                // 현재 값 조회
        int currentValue = currentValueStr != null ? Integer.parseInt(currentValueStr) : 0; // 없으면 0
        redisService.storeValue(key, String.valueOf(currentValue + 1), ttlMinutes);         // 증가된 값과 TTL로 저장
    } // incrementCounter 끝

    private int getCounterValue(String key) {
        // 카운터 값 조회
        String valueStr = redisService.getValue(key);               // 현재 값 조회
        return valueStr != null ? Integer.parseInt(valueStr) : 0;   // 없으면 0 반환
    } // getCounterValue 끝

    private int calculateFailureDelay(int failCount) {
        // 연속 실패 횟수에 따른 지연 시간 계산 (분 단위)
        if (failCount >= 6) {
            // 6회 이상 실패한 경우
            return 10; 
            // 10(분)을 반환
        } else if (failCount >= 3) {
            // 3회 이상 실패한 경우
            return 1; 
            // 30-60초를 위해 1분을 반환 (실제로는 초 단위 구현 가능)
        }
        return 0; 
        // 지연 없음을 반환
    } // calculateFailureDelay 끝
} // PasswordResetRateLimiter 끝

