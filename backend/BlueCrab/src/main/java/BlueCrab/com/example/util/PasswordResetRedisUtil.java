// 작성자 : 성태준
// 비밀번호 재설정 Redis 유틸리티
// AccountRecoveryRateLimiter 표준 키 구조 준수

package BlueCrab.com.example.util;

// ========== Java 표준 라이브러리 ==========
import java.time.Instant; // 시간 정보
import java.util.HashMap; // HashMap 클래스
import java.util.Map; // Map 인터페이스
import java.util.concurrent.TimeUnit; // 시간 단위

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired; // 의존성 주입
import org.springframework.data.redis.core.RedisTemplate; // Redis 템플릿
import org.springframework.stereotype.Component; // 컴포넌트 등록

// ========== 외부 라이브러리 ==========
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 변환
import lombok.extern.slf4j.Slf4j; // 로깅


@Component
@Slf4j
public class PasswordResetRedisUtil {
    
    // ========== 설정 상수 (TTL, 제한시간, 횟수 등) ==========
    
    public static final int EMAIL_CODE_TTL_MINUTES = 5; // 이메일 인증 코드 만료 시간 (분)
    public static final int PASSWORD_RESET_SESSION_TTL_MINUTES = 30; // 비밀번호 재설정 세션 만료 시간 (분)
    public static final int PASSWORD_RESET_CODE_TTL_MINUTES = 5; //비밀번호 재설정 코드 만료 시간 (분)
    public static final int DEFAULT_COUNTER_TTL_SECONDS = 3600; // 카운터 기본 만료 시간 (초) 시간당 제한을 위한 기본값
    public static final int DAILY_COUNTER_TTL_SECONDS = 86400; // 일일 카운터 만료 시간 (초)
    public static final int MAX_AUTH_ATTEMPTS = 5; // 최대 인증 시도 횟수
    public static final int BLOCK_DURATION_SECONDS = 600; // 실패 후 차단 시간 (초)
    
    // ========== 설정 편의 메서드 ==========
    
    public static int minutesToSeconds(int minutes) {
        // 시간 단위 변환 편의 메서드: 분을 초로
        return minutes * 60;
    }
    
    public static int hoursToSeconds(int hours) {
        // 시간 단위 변환 편의 메서드: 시간을 초로
        return hours * 3600;
    }
    
    // ========== 의존성 주입 ==========

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    // Redis 템플릿 (String 키/값 사용)
    
    @Autowired
    private ObjectMapper objectMapper;
    // JSON 직렬화/역직렬화용 ObjectMapper

    // =============== IRT 관련 ===============
    
    public Map<String, Object> getIRTData(String irtToken) {
        // IRT 토큰으로 세션 데이터 조회

        // try-catch로 예외 처리
        try {
            String irtKey = "irt:" + irtToken;
            // IRT 키 생성
            String irtJson = redisTemplate.opsForValue().get(irtKey);
            // Redis에서 조회
            if (irtJson == null) {
                // 토큰이 없거나 만료된 경우
                log.warn("IRT token not found or expired: {}", irtToken);
                // 경고 로그 기록
                return null;
                // null 반환하여 실패를 나타냄
            } // if 끝

            @SuppressWarnings("unchecked") // 타입 안전성 경고 억제
            Map<String, Object> result = objectMapper.readValue(irtJson, Map.class);
            // JSON을 Map으로 역직렬화
            log.debug("IRT data retrieved successfully for token: {}", irtToken);
            // 디버그 로그 기록
            return result;
            // 조회된 데이터 반환
        } catch (Exception e) {
            // 예외 발생 시
            log.error("Error retrieving IRT data for token: {}. Error: {}", irtToken, e.getMessage());
            // 에러 로그 기록
            return null;
            // null 반환하여 실패를 나타냄
        } // catch 끝
    } // getIRTData 끝
    
    public boolean isLockValid(String email, String irtLock) {
        // IRT 토큰의 세션 락 검증
        String lockKey = "reset_lock:" + email;
        // 락 키 생성
        String currentLock = redisTemplate.opsForValue().get(lockKey);
        // 현재 저장된 락 조회
        boolean isValid = irtLock != null && irtLock.equals(currentLock);
        // 검증 결과
        
        if (!isValid) {
            // 세션 락 검증 실패의 경우
            log.warn("Session lock validation failed for email: {}. Provided: {}, Current: {}", 
                    email, irtLock, currentLock);
            // 경고 로그 기록
        } else { 
            // 세션 락 검증 성공의 경우
            log.debug("Session lock validated successfully for email: {}", email);
            // 디버그 로그 기록
        } // if 끝
        
        return isValid;
        // 검증 결과 반환
    } // isLockValid 끝
    
    // =============== 이메일 코드 관리 ===============
    
    public void saveEmailCode(String email, String code, Map<String, Object> irtData, String lock) throws Exception {
        try {
            String codeKey = "email_code:" + email;
            
            Map<String, Object> codeData = new HashMap<>();
            codeData.put("code", code);
            codeData.put("user_id", irtData.get("user_id"));
            codeData.put("lock", lock);
            codeData.put("attempts", 0);
            codeData.put("created_at", Instant.now().getEpochSecond());
            
            String codeJson = objectMapper.writeValueAsString(codeData);
            redisTemplate.opsForValue().set(codeKey, codeJson, EMAIL_CODE_TTL_MINUTES, TimeUnit.MINUTES);
            
            log.info("Email code saved successfully for email: {} with {} minutes TTL", email, EMAIL_CODE_TTL_MINUTES);
        } catch (Exception e) {
            log.error("Failed to save email code for email: {}. Error: {}", email, e.getMessage());
            throw e;
        }
    }
    
    // =============== 카운터 관리 ===============

    // 카운터 증가 (기본 1시간 TTL 사용)
    public void incrementCounter(String key) {
        incrementCounter(key, DEFAULT_COUNTER_TTL_SECONDS);
    }
    
    // 카운터 증가 (사용자 정의 TTL)
    public void incrementCounter(String key, int expireSeconds) {
        try {
            String count = redisTemplate.opsForValue().get(key);
            if (count == null) {
                // 첫 번째 카운트
                redisTemplate.opsForValue().set(key, "1", expireSeconds, TimeUnit.SECONDS);
                log.debug("Counter initialized for key: {} with expiry: {} seconds", key, expireSeconds);
            } else {
                // 기존 카운트 증가
                Long newCount = redisTemplate.opsForValue().increment(key);
                log.debug("Counter incremented for key: {}, new value: {}", key, newCount);
            }
        } catch (Exception e) {
            log.error("Failed to increment counter for key: {}. Error: {}", key, e.getMessage());
        }
    }
    
    // 일일 카운터 증가 (24시간 TTL)
    public void incrementDailyCounter(String key) {
        incrementCounter(key, DAILY_COUNTER_TTL_SECONDS);
    }
    
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    public Long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }
    
    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    public void setValue(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
    
    // =============== IP 기반 표준 키 관리 (AccountRecoveryRateLimiter 호환) ===============
    
    public void savePasswordResetSession(String clientIp, String email, Map<String, Object> sessionData) throws Exception {
        try {
            // IP 기반 세션 데이터 저장
            String sessionKey = "pw_reset_session:" + clientIp;
            Map<String, Object> ipSessionData = new HashMap<>(sessionData);
            ipSessionData.put("email", email);
            ipSessionData.put("ip", clientIp);
            ipSessionData.put("created_at", Instant.now().getEpochSecond());
            
            String sessionJson = objectMapper.writeValueAsString(ipSessionData);
            redisTemplate.opsForValue().set(sessionKey, sessionJson, PASSWORD_RESET_SESSION_TTL_MINUTES, TimeUnit.MINUTES);
            
            log.info("Password reset session saved for IP: {}, email: {} with {} minutes TTL", 
                    clientIp, email, PASSWORD_RESET_SESSION_TTL_MINUTES);
        } catch (Exception e) {
            log.error("Failed to save password reset session for IP: {}, email: {}. Error: {}", 
                    clientIp, email, e.getMessage());
            throw e;
        }
    }
    
    public Map<String, Object> getPasswordResetSession(String clientIp) {
        try {
            String sessionKey = "pw_reset_session:" + clientIp;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            if (sessionJson == null) {
                log.debug("Password reset session not found for IP: {}", clientIp);
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(sessionJson, Map.class);
            log.debug("Password reset session retrieved successfully for IP: {}", clientIp);
            return result;
        } catch (Exception e) {
            log.error("Error retrieving password reset session for IP: {}. Error: {}", 
                    clientIp, e.getMessage());
            return null;
        }
    }
    
    public void savePasswordResetCode(String clientIp, String email, String code, Map<String, Object> sessionData) throws Exception {
        try {
            String codeKey = "pw_reset_code:" + clientIp;
            
            Map<String, Object> codeData = new HashMap<>();
            codeData.put("code", code);
            codeData.put("email", email);
            codeData.put("ip", clientIp);
            codeData.put("attempts", 0);
            codeData.put("created_at", Instant.now().getEpochSecond());
            
            // 추가 세션 데이터 병합
            if (sessionData != null) {
                codeData.putAll(sessionData);
            }
            
            String codeJson = objectMapper.writeValueAsString(codeData);
            redisTemplate.opsForValue().set(codeKey, codeJson, PASSWORD_RESET_CODE_TTL_MINUTES, TimeUnit.MINUTES);
            
            log.info("Password reset code saved for IP: {}, email: {} with {} minutes TTL", 
                    clientIp, email, PASSWORD_RESET_CODE_TTL_MINUTES);
        } catch (Exception e) {
            log.error("Failed to save password reset code for IP: {}, email: {}. Error: {}", 
                    clientIp, email, e.getMessage());
            throw e;
        }
    }

    /**
     * RT 토큰으로 reset session 조회 및 삭제 (단일 사용 보장)
     * GETDEL을 시뮬레이션하여 원자적 조회+삭제 수행
     * 
     * @param resetToken RT(Reset Token)
     * @return reset session 데이터 또는 null
     */
    public Map<String, Object> getAndDeleteResetSession(String resetToken) {
        try {
            String sessionKey = "reset_session:" + resetToken;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            
            if (sessionJson == null) {
                return null; // 토큰이 존재하지 않거나 만료됨
            }
            
            // 조회 후 즉시 삭제 (단일 사용 보장)
            redisTemplate.delete(sessionKey);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionData = objectMapper.readValue(sessionJson, Map.class);
            log.info("Reset session retrieved and deleted for token: {}", resetToken);
            
            return sessionData;
            
        } catch (Exception e) {
            log.error("Failed to get and delete reset session for token: {}. Error: {}", resetToken, e.getMessage());
            return null;
        }
    }

    /**
     * 현재 락 토큰 조회
     * 
     * @param email 사용자 이메일
     * @return 현재 락 토큰 또는 null
     */
    public String getCurrentLock(String email) {
        try {
            String lockKey = "reset_lock:" + email;
            String lockToken = redisTemplate.opsForValue().get(lockKey);
            
            log.debug("Current lock retrieved for email: {}", email);
            return lockToken;
            
        } catch (Exception e) {
            log.error("Failed to get current lock for email: {}. Error: {}", email, e.getMessage());
            return null;
        }
    }

    /**
     * 락 토큰 삭제
     * 
     * @param email 사용자 이메일
     */
    public void deleteLock(String email) {
        try {
            String lockKey = "reset_lock:" + email;
            redisTemplate.delete(lockKey);
            
            log.info("Lock deleted for email: {}", email);
            
        } catch (Exception e) {
            log.error("Failed to delete lock for email: {}. Error: {}", email, e.getMessage());
        }
    }
}