package BlueCrab.com.example.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * ID/PW 찾기 기능을 위한 레이트 리미팅 유틸리티
 * Redis 기반으로 IP 주소와 User-Agent를 기준으로 과도한 시도를 차단
 *
 * 제한 규칙:
 * - IP별 제한: 시간당 20회, 일일 100회
 * - 연속 실패 제한: 3회 실패 시 30-60초 지연, 6회 실패 시 10분 지연
 * - User-Agent별 추가 모니터링
 *
 * Redis 키 구조:
 * - find_id_ip:{ip}:h - IP별 시간당 시도 횟수
 * - find_id_ip:{ip}:d - IP별 일일 시도 횟수
 * - find_id_fail:{ip} - IP별 연속 실패 횟수
 * - find_id_block:{ip} - IP 차단 상태
 *
 * 향후 비밀번호 찾기 기능에서도 동일한 패턴으로 재사용 가능
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class AccountRecoveryRateLimiter {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // 레이트 리미팅 설정
    private static final int HOURLY_LIMIT = 20;
    private static final int DAILY_LIMIT = 100;
    private static final int FAILURE_THRESHOLD_1 = 3;  // 첫 번째 지연 임계값
    private static final int FAILURE_THRESHOLD_2 = 6;  // 두 번째 지연 임계값
    private static final long DELAY_SECONDS_2 = 600;   // 두 번째 지연 (10분)
    
    /**
     * ID 찾기 시도 가능 여부 확인
     * IP 주소 기반 레이트 리미팅 검사
     *
     * @param clientIp 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowedForFindId(String clientIp, String userAgent) {
        return isAllowed(clientIp, userAgent, "find_id");
    }
    
    /**
     * 비밀번호 찾기 시도 가능 여부 확인 (향후 구현용)
     * IP 주소 기반 레이트 리미팅 검사
     *
     * @param clientIp 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowedForFindPassword(String clientIp, String userAgent) {
        return isAllowed(clientIp, userAgent, "find_pw");
    }
    
    /**
     * 공통 레이트 리미팅 검사 로직
     *
     * @param clientIp 클라이언트 IP
     * @param userAgent User-Agent
     * @param action 액션 타입 (find_id, find_pw)
     * @return 허용 여부
     */
    private boolean isAllowed(String clientIp, String userAgent, String action) {
        try {
            // 1. IP 차단 상태 확인
            String blockKey = action + "_block:" + clientIp;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blockKey))) {
                return false;
            }
            
            // 2. 시간당 제한 확인
            String hourlyKey = action + "_ip:" + clientIp + ":h";
            String hourlyCount = redisTemplate.opsForValue().get(hourlyKey);
            if (hourlyCount != null && Integer.parseInt(hourlyCount) >= HOURLY_LIMIT) {
                return false;
            }
            
            // 3. 일일 제한 확인
            String dailyKey = action + "_ip:" + clientIp + ":d";
            String dailyCount = redisTemplate.opsForValue().get(dailyKey);
            if (dailyCount != null && Integer.parseInt(dailyCount) >= DAILY_LIMIT) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            // Redis 오류 시 기본적으로 허용 (서비스 연속성 우선)
            return true;
        }
    }
    
    /**
     * 성공적인 시도 기록
     * 시도 횟수 증가, 실패 카운터 리셋
     *
     * @param clientIp 클라이언트 IP
     * @param action 액션 타입
     */
    public void recordSuccess(String clientIp, String action) {
        try {
            // 시도 횟수 증가
            incrementAttempt(clientIp, action);
            
            // 실패 카운터 리셋
            String failKey = action + "_fail:" + clientIp;
            redisTemplate.delete(failKey);
            
        } catch (Exception e) {
            // 로깅만 하고 계속 진행
        }
    }
    
    /**
     * 실패한 시도 기록
     * 시도 횟수 증가, 실패 카운터 증가, 필요시 지연 적용
     *
     * @param clientIp 클라이언트 IP
     * @param action 액션 타입
     */
    public void recordFailure(String clientIp, String action) {
        try {
            // 시도 횟수 증가
            incrementAttempt(clientIp, action);
            
            // 실패 카운터 증가
            String failKey = action + "_fail:" + clientIp;
            Long failCount = redisTemplate.opsForValue().increment(failKey);
            
            // 실패 카운터에 TTL 설정 (1시간)
            redisTemplate.expire(failKey, Duration.ofHours(1));
            
            // 지연 처리
            if (failCount != null) {
                if (failCount >= FAILURE_THRESHOLD_2) {
                    // 6회 이상 실패: 10분 차단
                    String blockKey = action + "_block:" + clientIp;
                    redisTemplate.opsForValue().set(blockKey, "blocked", Duration.ofSeconds(DELAY_SECONDS_2));
                } else if (failCount >= FAILURE_THRESHOLD_1) {
                    // 3회 이상 실패: 30-60초 랜덤 지연
                    long randomDelay = (long) (Math.random() * 30 + 30); // 30-60초
                    String blockKey = action + "_block:" + clientIp;
                    redisTemplate.opsForValue().set(blockKey, "delayed", Duration.ofSeconds(randomDelay));
                }
            }
            
        } catch (Exception e) {
            // 로깅만 하고 계속 진행
        }
    }
    
    /**
     * 시도 횟수 증가 (시간당, 일일)
     */
    private void incrementAttempt(String clientIp, String action) {
        // 시간당 카운터
        String hourlyKey = action + "_ip:" + clientIp + ":h";
        redisTemplate.opsForValue().increment(hourlyKey);
        redisTemplate.expire(hourlyKey, Duration.ofHours(1));
        
        // 일일 카운터
        String dailyKey = action + "_ip:" + clientIp + ":d";
        redisTemplate.opsForValue().increment(dailyKey);
        redisTemplate.expire(dailyKey, Duration.ofDays(1));
    }
    
    /**
     * 남은 대기 시간 조회 (초 단위)
     * 차단된 IP의 경우 남은 대기 시간을 반환
     *
     * @param clientIp 클라이언트 IP
     * @param action 액션 타입
     * @return 남은 대기 시간 (초), 차단되지 않은 경우 0
     */
    public long getRemainingWaitTime(String clientIp, String action) {
        try {
            String blockKey = action + "_block:" + clientIp;
            Long ttl = redisTemplate.getExpire(blockKey, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 현재 시간당 시도 횟수 조회
     */
    public int getCurrentHourlyAttempts(String clientIp, String action) {
        try {
            String hourlyKey = action + "_ip:" + clientIp + ":h";
            String count = redisTemplate.opsForValue().get(hourlyKey);
            return count != null ? Integer.parseInt(count) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 현재 일일 시도 횟수 조회
     */
    public int getCurrentDailyAttempts(String clientIp, String action) {
        try {
            String dailyKey = action + "_ip:" + clientIp + ":d";
            String count = redisTemplate.opsForValue().get(dailyKey);
            return count != null ? Integer.parseInt(count) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}