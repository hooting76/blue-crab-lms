/* 작업자 : 성태준
 * Redis 인증 데이터 관리 전용 유틸리티 클래스
 * 이메일 인증 코드와 관련된 데이터의 저장, 조회, 삭제를 담당
 * MailAuthCheckController와 AdminEmailAuthController에서 공통으로 사용 가능한 범용 유틸리티
 */

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisAuthDataManager {
    
    // ========== 기본 설정 상수 ==========
    private static final String DEFAULT_KEY_PREFIX = "email_auth:code:";
    // 기본 Redis 키 접두사 (일반 사용자용)
    private static final int DEFAULT_EXPIRY_MINUTES = 5;
    // 기본 만료 시간 (5분)
    
    // ========== 의존성 주입 ==========
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    // Redis 템플릿을 사용하여 인증 데이터 저장/조회/삭제
    
    /* 인증 데이터를 Redis에 저장 (기본 설정 사용)
     * 일반적인 이메일 인증에서 사용
     * 
     * @param userEmail 사용자 이메일 주소 (키 식별자로 사용)
     * @param authCodeData 저장할 인증 데이터
     */
    public void saveAuthData(String userEmail, AuthCodeData authCodeData) {
        saveAuthData(DEFAULT_KEY_PREFIX, userEmail, authCodeData, DEFAULT_EXPIRY_MINUTES);
    }
    
    /* 인증 데이터를 Redis에서 조회 (기본 설정 사용)
     * 일반적인 이메일 인증에서 사용
     * 
     * @param userEmail 사용자 이메일 주소 (키 식별자)
     * @return AuthCodeData 조회된 인증 데이터 (없으면 null)
     */
    public AuthCodeData getAuthData(String userEmail) {
        return getAuthData(DEFAULT_KEY_PREFIX, userEmail);
    }
    
    /* 인증 데이터를 Redis에서 삭제 (기본 설정 사용)
     * 일반적인 이메일 인증에서 사용
     * 
     * @param userEmail 사용자 이메일 주소 (키 식별자)
     */
    public void cleanup(String userEmail) {
        cleanup(DEFAULT_KEY_PREFIX, userEmail);
    }
    
    /* 인증 데이터를 Redis에 저장 (커스터마이징 가능)
     * AdminEmailAuthController 등에서 다른 키 접두사 사용 시 활용
     * 
     * @param keyPrefix Redis 키 접두사
     * @param identifier 식별자 (이메일 등)
     * @param authCodeData 저장할 인증 데이터
     * @param expiryMinutes 만료 시간 (분)
     */
    public void saveAuthData(String keyPrefix, String identifier, AuthCodeData authCodeData, int expiryMinutes) {
        try {
            String sessionKey = keyPrefix + identifier;
            // Redis 키 생성 (접두사 + 식별자)
            
            redisTemplate.opsForHash().putAll(sessionKey, Map.of(
                // 인증 데이터 맵 생성 및 저장
                "authCode", authCodeData.getAuthCode(),
                // 인증 코드
                "userEmail", authCodeData.getUserEmail(),
                // 사용자 이메일 (또는 관리자 ID)
                "clientIp", authCodeData.getClientIp(),
                // 클라이언트 IP
                "codeCreatedTime", authCodeData.getCodeCreatedTime().toString(),
                // 코드 생성 시간 (문자열로 저장)
                "sessionId", authCodeData.getSessionId()
                // 세션 ID
            )); // Redis에 해시맵 형태로 저장
            
            long ttlSeconds = expiryMinutes * 60;
            // TTL(만료 시간)을 초 단위로 계산
            redisTemplate.expire(sessionKey, ttlSeconds, TimeUnit.SECONDS);
            // Redis 키에 TTL 설정
            
            log.debug("Successfully saved auth data to Redis - Identifier: {}, TTL: {} seconds", identifier, ttlSeconds);
            // 저장 완료 디버그 로그 기록
            
        } catch (Exception e) {
            log.error("Failed to save auth data to Redis - Identifier: {}, Error: {}", identifier, e.getMessage());
            // 저장 실패 오류 로그 기록
        }
    }
    
    /* 인증 데이터를 Redis에서 조회 (커스터마이징 가능)
     * AdminEmailAuthController 등에서 다른 키 접두사 사용 시 활용
     * 
     * @param keyPrefix Redis 키 접두사
     * @param identifier 식별자 (이메일 등)
     * @return AuthCodeData 조회된 인증 데이터 (없으면 null)
     */
    public AuthCodeData getAuthData(String keyPrefix, String identifier) {
        try {
            String sessionKey = keyPrefix + identifier;
            // Redis 키 생성 (접두사 + 식별자)
            Map<Object, Object> authDataMap = redisTemplate.opsForHash().entries(sessionKey);
            // Redis에서 해시맵 형태로 인증 데이터 조회
            
            if (authDataMap.isEmpty()) {
                // 데이터가 없는 경우
                log.warn("No auth data found in Redis for identifier: {}", identifier);
                // 경고 로그 기록
                return null;
                // null 반환
            }
            
            String authCode = (String) authDataMap.get("authCode");
            // 저장된 인증 코드
            String email = (String) authDataMap.get("userEmail");
            // 저장된 사용자 이메일 (또는 관리자 ID)
            String clientIp = (String) authDataMap.get("clientIp");
            // 저장된 클라이언트 IP
            String createdTimeStr = (String) authDataMap.get("codeCreatedTime");
            // 저장된 코드 생성 시간 (문자열)
            String sessionId = (String) authDataMap.get("sessionId");
            // 저장된 세션 ID
            
            if (authCode == null || email == null || clientIp == null || createdTimeStr == null || sessionId == null) {
                // 필수 데이터가 누락된 경우
                log.error("Incomplete auth data in Redis for identifier: {}", identifier);
                // 오류 로그 기록
                return null;
                // null 반환
            }
            
            LocalDateTime codeCreatedTime = LocalDateTime.parse(createdTimeStr);
            // 문자열로 저장된 생성 시간을 LocalDateTime 객체로 변환
            
            log.debug("Retrieved auth data from Redis for identifier: {}", identifier);
            // 조회 완료 디버그 로그 기록
            
            return new AuthCodeData(authCode, email, clientIp, codeCreatedTime, sessionId);
            // AuthCodeData 객체 생성 및 반환
            
        } catch (Exception e) {
            // 예외 발생 시
            log.error("Redis 인증 데이터 조회 실패 - 식별자: {}, 오류: {}", identifier, e.getMessage());
            // 오류 로그 기록
            return null;
            // null 반환
        }
    }
    
    /* 인증 데이터를 Redis에서 삭제 (커스터마이징 가능)
     * AdminEmailAuthController 등에서 다른 키 접두사 사용 시 활용
     * 
     * @param keyPrefix Redis 키 접두사
     * @param identifier 식별자 (이메일 등)
     */
    public void cleanup(String keyPrefix, String identifier) {
        try {
            String sessionKey = keyPrefix + identifier;
            // Redis 키 생성 (접두사 + 식별자)
            redisTemplate.delete(sessionKey);
            // Redis에서 키 삭제
            log.debug("Completed cleanup of Redis auth data - Identifier: {}", identifier);
            // 정리 완료 디버그 로그 기록
        } catch (Exception e) {
            // 예외 발생 시
            log.warn("Failed to cleanup Redis auth data - Identifier: {}, Error: {}", identifier, e.getMessage());
            // 경고 로그 기록
        }
    }
    
    /* Redis 인증 데이터 구조를 나타내는 데이터 클래스
     * Redis에 저장되는 인증 코드 관련 모든 정보를 담음
     */
    public static class AuthCodeData {
        private final String authCode;
        // 인증 코드
        private final String userEmail;
        // 사용자 이메일 (또는 관리자 ID)
        private final String clientIp;
        // 클라이언트 IP 주소
        private final LocalDateTime codeCreatedTime;
        // 코드 생성 시간
        private final String sessionId;
        // 세션 ID
        
        public AuthCodeData(String authCode, String userEmail, String clientIp, LocalDateTime codeCreatedTime, String sessionId) {
            // AuthCodeData 생성자
            this.authCode = authCode;
            // 인증 코드 초기화
            this.userEmail = userEmail;
            // 사용자 이메일 초기화
            this.clientIp = clientIp;
            // 클라이언트 IP 초기화
            this.codeCreatedTime = codeCreatedTime;
            // 코드 생성 시간 초기화
            this.sessionId = sessionId;
            // 세션 ID 초기화
        }
        
        public String getAuthCode() { return authCode; }
        // 인증 코드 Getter
        public String getUserEmail() { return userEmail; }
        // 사용자 이메일 Getter
        public String getClientIp() { return clientIp; }
        // 클라이언트 IP Getter
        public LocalDateTime getCodeCreatedTime() { return codeCreatedTime; }
        // 코드 생성 시간 Getter
        public String getSessionId() { return sessionId; }
        // 세션 ID Getter
    }
}
