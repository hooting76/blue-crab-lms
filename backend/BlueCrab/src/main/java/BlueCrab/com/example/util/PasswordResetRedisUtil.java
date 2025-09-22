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

// ========== 내부 모델 ==========
import BlueCrab.com.example.model.PasswordResetCodeData; // 코드 데이터 모델


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
        // 분을 초로 변환하여 반환
    }
    
    public static int hoursToSeconds(int hours) {
        // 시간 단위 변환 편의 메서드: 시간을 초로
        return hours * 3600;
        // 시간을 초로 변환하여 반환
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
        // 이메일 인증 코드와 관련 데이터 저장
        try {
            String codeKey = "email_code:" + email;             // 이메일 코드 키 생성
            
            Map<String, Object> codeData = new HashMap<>();     // 저장할 데이터 맵 생성
            codeData.put("code", code);
            codeData.put("user_id", irtData.get("user_id"));
            codeData.put("lock", lock);
            codeData.put("attempts", 0);
            codeData.put("created_at", Instant.now().getEpochSecond());
            
            String codeJson = objectMapper.writeValueAsString(codeData);    // JSON으로 직렬화
            redisTemplate.opsForValue().set(codeKey, codeJson, EMAIL_CODE_TTL_MINUTES, TimeUnit.MINUTES);
            // Redis에 저장 (TTL 설정)
            
            log.info("Email code saved successfully for email: {} with {} minutes TTL", email, EMAIL_CODE_TTL_MINUTES);
            // 저장 성공 로그
        } catch (Exception e) {
            log.error("Failed to save email code for email: {}. Error: {}", email, e.getMessage());
            // 저장 실패 로그
            throw e;
            // 예외 재발생
        }
    }
    
    // =============== 카운터 관리 ===============

    // 카운터 증가 (기본 1시간 TTL 사용)
    // 효율성을 위해 동명의 메서드 오버로딩
    public void incrementCounter(String key) {
        incrementCounter(key, DEFAULT_COUNTER_TTL_SECONDS);
        // 기본 TTL 사용
    } // incrementCounter(String key) 끝
    
    // 카운터 증가 (사용자 정의 TTL)
    public void incrementCounter(String key, int expireSeconds) {
        // 지정된 키의 카운터를 증가시키고 TTL 설정

        // try-catch로 예외 처리
        try {
            String count = redisTemplate.opsForValue().get(key);
            // 현재 카운트 조회
            if (count == null) {
                // 첫 번째 카운트인 경우
                redisTemplate.opsForValue().set(key, "1", expireSeconds, TimeUnit.SECONDS);
                // 카운트 초기화 및 TTL 설정
                log.debug("Counter initialized for key: {} with expiry: {} seconds", key, expireSeconds);
                // 초기화 디버그 로그 기록
            } else {
                Long newCount = redisTemplate.opsForValue().increment(key);
                // 카운트 증가
                log.debug("Counter incremented for key: {}, new value: {}", key, newCount);
                // 증가 디버그 로그 기록
            }
        } catch (Exception e) {
            log.error("Failed to increment counter for key: {}. Error: {}", key, e.getMessage());
            // 에러 로그 기록
        } // catch 끝
    } // incrementCounter(String key, int expireSeconds) 끝
    
    // 일일 카운터 증가 (24시간 TTL)
    public void incrementDailyCounter(String key) {
        incrementCounter(key, DAILY_COUNTER_TTL_SECONDS);
        // 24시간 TTL 사용
    }

    // 키 존재 여부 확인
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        // 키 존재 여부 반환
    }
    
    // 키 만료 시간 조회
    public Long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
        // 키 만료 시간 반환
    }
    
    // 단순 값 조회
    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
        // 단순 값 조회
    }
    
    // 단순 값 설정 (TTL 포함)
    public void setValue(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
        // 단순 값 설정 (TTL 포함)
    }
    
    // =============== IP 기반 표준 키 관리 (AccountRecoveryRateLimiter 호환) ===============
    
    public void savePasswordResetSession(String clientIp, String email, Map<String, Object> sessionData) throws Exception {
        // 비밀번호 재설정 세션 저장

        // try-catch로 예외 처리
        try {
            String sessionKey = "pw_reset_session:" + clientIp;                     // IP 기반 세션 데이터 저장
            Map<String, Object> ipSessionData = new HashMap<>(sessionData);         // 기존 세션 데이터 복사
            ipSessionData.put("email", email);                                  // 이메일 추가
            ipSessionData.put("ip", clientIp);                                  // 클라이언트 IP 추가
            ipSessionData.put("created_at", Instant.now().getEpochSecond());    // 생성 시간 추가
            
            String sessionJson = objectMapper.writeValueAsString(ipSessionData);    // JSON으로 직렬화
            redisTemplate.opsForValue().set(sessionKey, sessionJson, PASSWORD_RESET_SESSION_TTL_MINUTES, TimeUnit.MINUTES);
            //  Redis에 저장 (TTL 설정)
            
            log.info("Password reset session saved for IP: {}, email: {} with {} minutes TTL", 
                    clientIp, email, PASSWORD_RESET_SESSION_TTL_MINUTES);
            // 저장 성공 로그
        } catch (Exception e) {
            log.error("Failed to save password reset session for IP: {}, email: {}. Error: {}", 
                    clientIp, email, e.getMessage());
            throw e;
            // 저장 실패 로그 및 예외 재발생
        } // catch 끝
    } // savePasswordResetSession 끝
    
    public Map<String, Object> getPasswordResetSession(String clientIp) {
        // IP 기반 비밀번호 재설정 세션 조회

        // try-catch로 예외 처리
        try {
            String sessionKey = "pw_reset_session:" + clientIp;                 // IP 기반 세션 키 생성
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);   // Redis에서 조회
            if (sessionJson == null) {
                // 세션이 없거나 만료된 경우
                log.debug("Password reset session not found for IP: {}", clientIp);
                // 디버그 로그 기록
                return null;
                // null 반환하여 세션이 없음을 나타냄
            } // if 끝
            @SuppressWarnings("unchecked") // 타입 안전성 경고 억제
            Map<String, Object> result = objectMapper.readValue(sessionJson, Map.class);
            // JSON을 Map으로 역직렬화
            log.debug("Password reset session retrieved successfully for IP: {}", clientIp);
            // 디버그 로그 기록
            return result;
            // 조회된 데이터 반환
        } catch (Exception e) {
            // 예외 발생 시
            log.error("Error retrieving password reset session for IP: {}. Error: {}", 
                    clientIp, e.getMessage());
            // 에러 로그 기록
            return null;
            // null 반환하여 실패를 나타냄
        } // catch 끝
    } // getPasswordResetSession 끝
    
    public void savePasswordResetCode(String clientIp, String email, String code, Map<String, Object> sessionData) throws Exception {
        // 비밀번호 재설정 코드 저장 (IP 기반)

        // try-catch로 예외 처리
        try {
            String codeKey = "pw_reset_code:" + clientIp;
            // IP 기반 코드 키 생성(요청한 IP 에서만 사용 가능 하도록 하기 위함)
            
            Map<String, Object> codeData = new HashMap<>(); // 저장할 데이터 맵 생성
            codeData.put("code", code);
            codeData.put("email", email);
            codeData.put("ip", clientIp);
            codeData.put("attempts", 0);
            codeData.put("created_at", Instant.now().getEpochSecond());
            
            // 추가 세션 데이터 병합
            if (sessionData != null) {
                // 세션 데이터가 있는경우
                codeData.putAll(sessionData);
                // 세션 데이터 병합
            }
            
            String codeJson = objectMapper.writeValueAsString(codeData);
            // JSON으로 직렬화
            redisTemplate.opsForValue().set(codeKey, codeJson, PASSWORD_RESET_CODE_TTL_MINUTES, TimeUnit.MINUTES);
            // Redis에 저장 (TTL 설정)
            
            log.info("Password reset code saved for IP: {}, email: {} with {} minutes TTL", 
                    clientIp, email, PASSWORD_RESET_CODE_TTL_MINUTES);
            // 저장 성공 로그
        } catch (Exception e) {
            log.error("Failed to save password reset code for IP: {}, email: {}. Error: {}", 
                    clientIp, email, e.getMessage());
            // 저장 실패 로그
            throw e;
            // 예외 재발생
        } // catch 끝
    } // savePasswordResetCode 끝

    // =============== RT 관련 ===============

    public Map<String, Object> getAndDeleteResetSession(String resetToken) {
        // RT 토큰으로 세션 데이터 조회 및 삭제 (단일 사용 보장)
        try {
            String sessionKey = "reset_session:" + resetToken;
            // RT 기반 세션 키 생성
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            // Redis에서 조회
            
            if (sessionJson == null) {
                // 세션이 없거나 만료된 경우
                return null;
                // null 반환
            }
            
            redisTemplate.delete(sessionKey);
            // 조회 후 즉시 삭제 (단일 사용 보장)
            
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionData = objectMapper.readValue(sessionJson, Map.class);
            // JSON을 Map으로 역직렬화
            log.info("Reset session retrieved and deleted for token: {}", resetToken);
            // 정보 로그
            
            return sessionData;
            // 조회된 세션 데이터 반환
            
        } catch (Exception e) {
            log.error("Failed to get and delete reset session for token: {}. Error: {}", resetToken, e.getMessage());
            // 에러 로그
            return null;
            // null 반환
        }
    } // getAndDeleteResetSession 끝

    public String getCurrentLock(String email) {
        // 현재 락 토큰 조회
        try {
            String lockKey = "reset_lock:" + email;
            // 락 키 생성
            String lockToken = redisTemplate.opsForValue().get(lockKey);
            // 현재 저장된 락 조회
            
            log.debug("Current lock retrieved for email: {}", email);
            // 디버그 로그
            return lockToken;
            // 현재 락 토큰 반환
            
        } catch (Exception e) {
            log.error("Failed to get current lock for email: {}. Error: {}", email, e.getMessage());
            // 에러 로그
            return null;
            // null 반환
        }
    }

    public void deleteLock(String email) {
        // 락 토큰 삭제
        try {
            String lockKey = "reset_lock:" + email;     // 락 키 생성
            redisTemplate.delete(lockKey);              // 락 키 삭제
            
            log.info("Lock deleted for email: {}", email);
            
        } catch (Exception e) {
            log.error("Failed to delete lock for email: {}. Error: {}", email, e.getMessage());
            // 에러 로그
        }
    }

    public boolean validateSessionLock(String email, String sessionLockToken) {
        // 세션 락 검증
        try {
            String currentLockToken = getCurrentLock(email);            // 현재 저장된 락 조회
            
            if (currentLockToken == null) {
                // 현재 락이 없는 경우
                log.warn("no current lock token: email={}", email);
                // 경고 로그
                return false;
            }
            
            boolean isValid = currentLockToken.equals(sessionLockToken);
            log.info("Session lock validation result: email={}, valid={}", email, isValid);
            
            return isValid;
            // 검증 결과 반환
            
        } catch (Exception e) {
            log.error("Error occurred during session lock validation: email={}", email, e);
            return false;
            // false 반환
        }
    }

    // Stage 3: Code Verification Methods

    public PasswordResetCodeData getPasswordResetCodeData(String clientIp) {
        // 패스워드 리셋 코드 데이터 조회 (IP 기반)
        // 2단계에서 저장한 IP 기반 코드 데이터를 3단계에서 조회
        try {
            String codeKey = "pw_reset_code:" + clientIp;   // IP 기반 코드 키 생성(요청한 IP 에서만 사용 가능 하도록 하기 위함)
            String jsonData = (String) redisTemplate.opsForValue().get(codeKey);    // Redis에서 조회
            
            if (jsonData == null) {
                // 데이터가 없거나 만료된 경우
                log.info("No password reset code data found for IP: {}", clientIp);
                return null;
            }
            
            return objectMapper.readValue(jsonData, PasswordResetCodeData.class);
            // JSON을 PasswordResetCodeData 객체로 역직렬화하여 반환
            
        } catch (Exception e) {
            log.error("Failed to get password reset code data for IP: {}. Error: {}", clientIp, e.getMessage());
            // 에러 로그
            return null;
        }
    }

    public void invalidatePasswordResetCode(String clientIp) {
        // 패스워드 리셋 코드 무효화 (삭제) - IP 기반
        try {
            String codeKey = "pw_reset_code:" + clientIp;       // IP 기반 코드 키 생성
            redisTemplate.delete(codeKey);       // 코드 키 삭제
            
            log.info("Password reset code invalidated for IP: {}", clientIp);
            
        } catch (Exception e) {
            log.error("Failed to invalidate password reset code for IP: {}. Error: {}", clientIp, e.getMessage());
        }
    }

    public PasswordResetCodeData incrementCodeVerificationAttempts(String clientIp) {
        // 코드 검증 시도 횟수 증가 (IP 기반)
        try {
            String codeKey = "pw_reset_code:" + clientIp;       // IP 기반 코드 키 생성
            String jsonData = (String) redisTemplate.opsForValue().get(codeKey);    // Redis에서 조회
            
            if (jsonData == null) {
                // 데이터가 없거나 만료된 경우
                log.warn("No password reset code data found for attempts increment: {}", clientIp);
                return null;
            }
            
            PasswordResetCodeData codeData = objectMapper.readValue(jsonData, PasswordResetCodeData.class);
            // JSON을 PasswordResetCodeData 객체로 역직렬화
            codeData.setVerificationAttempts(codeData.getVerificationAttempts() + 1);
            // 시도 횟수 +1 증가
            
            Long ttl = redisTemplate.getExpire(codeKey);
            // 기존 키의 남은 TTL 조회
            // 남은 TTL 유지
            String updatedJson = objectMapper.writeValueAsString(codeData);
            // 업데이트된 데이터를 JSON으로 직렬화
            
            if (ttl != null && ttl > 0) {
                // TTL이 유효한 경우
                redisTemplate.opsForValue().set(codeKey, updatedJson, ttl, TimeUnit.SECONDS);
                // 기존 TTL 유지
            } else {    // TTL이 없거나 만료된 경우 (안전장치)
                redisTemplate.opsForValue().set(codeKey, updatedJson, 300, TimeUnit.SECONDS); 
                // 기본 TTL 5분 설정
            }
            
            log.info("Code verification attempts incremented for IP: {}. New attempt count: {}", 
                    clientIp, codeData.getVerificationAttempts());
            
            return codeData;
            // 업데이트된 코드 데이터 반환
            
        } catch (Exception e) {
            log.error("Failed to increment code verification attempts for IP: {}. Error: {}", clientIp, e.getMessage());
            // 에러 로그
            return null;
            // null 반환
        }
    } // incrementCodeVerificationAttempts 끝
} // PasswordResetRedisUtil 끝