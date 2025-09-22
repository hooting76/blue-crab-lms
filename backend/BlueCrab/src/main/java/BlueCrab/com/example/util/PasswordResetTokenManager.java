package BlueCrab.com.example.util;

import BlueCrab.com.example.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 비밀번호 재설정 토큰 관리 유틸리티
 * Replace-on-new 방식의 락 토큰 기반 토큰 관리 시스템
 *
 * 토큰 종류:
 * - 락 토큰 (Lock Token): 세션 무효화의 기준이 되는 토큰
 * - IRT (Identity Request Token): 본인확인 후 발급되는 토큰
 * - RT (Reset Token): 코드 검증 후 발급되는 토큰
 *
 * Replace-on-new 원칙:
 * - 새로운 본인확인이 성공하면 락 토큰을 교체
 * - 이전 락 토큰과 바인딩된 모든 토큰들이 자동으로 무효화
 */
@Component
public class PasswordResetTokenManager {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenManager.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RedisService redisService;

    // Redis 키 프리픽스
    private static final String RESET_LOCK_PREFIX = "reset_lock:";
    private static final String IRT_PREFIX = "irt:";
    private static final String RESET_SESSION_PREFIX = "reset_session:";

    // TTL 설정 (분 단위)
    private static final int LOCK_TTL_MINUTES = 30;     // 락 토큰 TTL
    private static final int IRT_TTL_MINUTES = 10;      // IRT TTL
    private static final int RT_TTL_MINUTES = 15;       // RT TTL

    /**
     * 새로운 락 토큰을 생성하고 기존 락을 교체
     * Replace-on-new 방식의 핵심 메서드
     *
     * @param email 사용자 이메일
     * @return 새로 생성된 락 토큰
     */
    public String generateAndReplaceLockToken(String email) {
        String lockToken = generateSecureToken();
        String lockKey = RESET_LOCK_PREFIX + email;

        try {
            // 새 락 토큰으로 교체
            redisService.storeValue(lockKey, lockToken, LOCK_TTL_MINUTES);
            logger.info("Lock token replaced for email: {}", email);
            return lockToken;
        } catch (Exception e) {
            logger.error("Failed to generate lock token for email: {}", email, e);
            throw new RuntimeException("락 토큰 생성에 실패했습니다", e);
        }
    }

    /**
     * IRT (Identity Request Token) 생성 및 저장
     *
     * @param email 사용자 이메일
     * @param userId 사용자 ID
     * @param lockToken 현재 락 토큰
     * @return 생성된 IRT 토큰
     */
    public String generateIRT(String email, Integer userId, String lockToken) {
        String irtToken = generateSecureToken();
        String irtKey = IRT_PREFIX + irtToken;

        Map<String, Object> irtData = new HashMap<>();
        irtData.put("email", email);
        irtData.put("userId", userId);
        irtData.put("lock", lockToken);
        irtData.put("createdAt", LocalDateTime.now().toString());

        try {
            String irtDataJson = objectMapper.writeValueAsString(irtData);
            redisService.storeValue(irtKey, irtDataJson, IRT_TTL_MINUTES);
            logger.info("IRT generated for email: {}", email);
            return irtToken;
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize IRT data for email: {}", email, e);
            throw new RuntimeException("IRT 생성에 실패했습니다", e);
        }
    }

    /**
     * IRT 토큰 검증 및 데이터 조회
     *
     * @param irtToken IRT 토큰
     * @return IRT 데이터 또는 null (무효한 경우)
     */
    public IRTData validateIRT(String irtToken) {
        if (irtToken == null || irtToken.trim().isEmpty()) {
            return null;
        }

        String irtKey = IRT_PREFIX + irtToken;
        try {
            String irtDataJson = redisService.getValue(irtKey);
            if (irtDataJson == null) {
                logger.warn("IRT not found or expired: {}", irtToken);
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> irtDataMap = objectMapper.readValue(irtDataJson, Map.class);
            return new IRTData(
                (String) irtDataMap.get("email"),
                (Integer) irtDataMap.get("userId"),
                (String) irtDataMap.get("lock"),
                (String) irtDataMap.get("createdAt")
            );
        } catch (Exception e) {
            logger.error("Failed to validate IRT: {}", irtToken, e);
            return null;
        }
    }

    /**
     * 현재 락 토큰 조회
     *
     * @param email 사용자 이메일
     * @return 현재 락 토큰 또는 null
     */
    public String getCurrentLockToken(String email) {
        String lockKey = RESET_LOCK_PREFIX + email;
        return redisService.getValue(lockKey);
    }

    /**
     * 락 토큰 일치 여부 확인
     * Replace-on-new 방식에서 핵심적인 검증 로직
     *
     * @param email 사용자 이메일
     * @param expectedLock 예상되는 락 토큰
     * @return 일치 여부
     */
    public boolean isLockTokenValid(String email, String expectedLock) {
        if (expectedLock == null) {
            return false;
        }

        String currentLock = getCurrentLockToken(email);
        return expectedLock.equals(currentLock);
    }

    /**
     * RT (Reset Token) 생성 및 저장
     *
     * @param email 사용자 이메일
     * @param userId 사용자 ID
     * @param lockToken 현재 락 토큰
     * @return 생성된 RT 토큰
     */
    public String generateRT(String email, Integer userId, String lockToken) {
        String rtToken = generateSecureToken();
        String rtKey = RESET_SESSION_PREFIX + rtToken;

        Map<String, Object> rtData = new HashMap<>();
        rtData.put("email", email);
        rtData.put("userId", userId);
        rtData.put("lock", lockToken);
        rtData.put("createdAt", LocalDateTime.now().toString());

        try {
            String rtDataJson = objectMapper.writeValueAsString(rtData);
            redisService.storeValue(rtKey, rtDataJson, RT_TTL_MINUTES);
            logger.info("RT generated for email: {}", email);
            return rtToken;
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize RT data for email: {}", email, e);
            throw new RuntimeException("RT 생성에 실패했습니다", e);
        }
    }

    /**
     * RT 토큰 소비 (단일 사용 보장)
     * GETDEL 명령어를 사용하여 원자적으로 조회 후 삭제
     *
     * @param rtToken RT 토큰
     * @return RT 데이터 또는 null (무효한 경우)
     */
    public RTData consumeRT(String rtToken) {
        if (rtToken == null || rtToken.trim().isEmpty()) {
            return null;
        }

        String rtKey = RESET_SESSION_PREFIX + rtToken;
        try {
            String rtDataJson = redisService.getAndDelete(rtKey);
            if (rtDataJson == null) {
                logger.warn("RT not found or already consumed: {}", rtToken);
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rtDataMap = objectMapper.readValue(rtDataJson, Map.class);
            return new RTData(
                (String) rtDataMap.get("email"),
                (Integer) rtDataMap.get("userId"),
                (String) rtDataMap.get("lock"),
                (String) rtDataMap.get("createdAt")
            );
        } catch (Exception e) {
            logger.error("Failed to consume RT: {}", rtToken, e);
            return null;
        }
    }

    /**
     * 안전한 토큰 생성
     * UUID + 추가 랜덤 바이트를 조합하여 높은 엔트로피 확보
     *
     * @return 생성된 토큰
     */
    private String generateSecureToken() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return uuid + randomPart;
    }

    /**
     * IRT 데이터를 담는 내부 클래스
     * Jackson JSON 직렬화/역직렬화를 위해 기본 생성자와 setter 추가
     */
    public static class IRTData {
        private String email;
        private Integer userId;
        private String lock;
        private String createdAt;

        // Jackson을 위한 기본 생성자
        public IRTData() {
        }

        public IRTData(String email, Integer userId, String lock, String createdAt) {
            this.email = email;
            this.userId = userId;
            this.lock = lock;
            this.createdAt = createdAt;
        }

        // Getter 메서드들
        public String getEmail() { return email; }
        public Integer getUserId() { return userId; }
        public String getLock() { return lock; }
        public String getCreatedAt() { return createdAt; }

        // Jackson을 위한 Setter 메서드들
        public void setEmail(String email) { this.email = email; }
        public void setUserId(Integer userId) { this.userId = userId; }
        public void setLock(String lock) { this.lock = lock; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // ========== 3 단계 : Code Verification용 메서드들 작업자 : 성태준 ==========
    public Map<String, Object> extractIRTTokenData(String irtToken) {
        // IRT 토큰에서 데이터 추출

        // try-catch로 예외 처리
        try {
            String irtData = redisService.getValue(IRT_PREFIX + irtToken);
            // IRT 데이터 조회
            if (irtData == null) {
                // null 또는 만료된 경우
                logger.warn("IRT token not found or expired: {}", irtToken);
                //경고 로그
                return null;
                // null 반환
            } // null 또는 만료된 경우 끝

            IRTData data = objectMapper.readValue(irtData, IRTData.class);
            // JSON을 IRTData 객체로 역직렬화
            
            Map<String, Object> result = new HashMap<>();       // 결과 맵 생성
            result.put("email", data.getEmail());           // 이메일 추가
            result.put("userId", data.getUserId());         // 사용자 ID 추가
            result.put("sessionLockToken", data.getLock()); // 세션 잠금 토큰 추가

            return result;
            // AuthCodeData 객체 생성 및 반환
            
        } catch (Exception e) {
            logger.error("IRT token data extraction failed: {}", irtToken, e);
            // 오류 로그
            return null;
            // null 반환
        } // try-catch 끝
    } // extractIRTTokenData 끝

    public String generateRTToken(String email, String sessionLockToken) {
        // RT 토큰 생성 및 저장

        // try-catch로 예외 처리
        try {
            // 1. RT 토큰 생성
            String rtToken = UUID.randomUUID().toString();
            
            // 2. RT 데이터 생성
            RTData rtData = new RTData(
                email, 
                null, // userId는 RT 토큰에서 불필요
                sessionLockToken,   // 현재 세션 락 토큰
                LocalDateTime.now().toString()  // 생성 일시
            );
            
            // 3. Redis에 저장
            String rtDataJson = objectMapper.writeValueAsString(rtData);
            // RT 데이터 JSON 직렬화
            redisService.storeValue(
                RESET_SESSION_PREFIX + rtToken,     // RT 키
                rtDataJson,     // RT 데이터
                RT_TTL_MINUTES  // RT TTL
            ); // Redis에 RT 데이터 저장

            logger.info("RT token creation completed: email={}", email);
            // 생성 완료 로그
            return rtToken;
            // 생성된 RT 토큰 반환
            
        } catch (Exception e) {
            logger.error("RT token creation failed: email={}", email, e);
            // 오류 로그
            return null;
            // null 반환
        } // try-catch 끝
    } // generateRTToken 끝
    // ========== 3 단계 : Code Verification용 메서드들 작업자 : 성태준 끝 ==========

    /**
     * RT 데이터를 담는 내부 클래스
     * Jackson JSON 직렬화/역직렬화를 위해 기본 생성자와 setter 추가
     */
    public static class RTData {
        private String email;
        private Integer userId;
        private String lock;
        private String createdAt;

        // Jackson을 위한 기본 생성자
        public RTData() {
        }

        public RTData(String email, Integer userId, String lock, String createdAt) {
            this.email = email;
            this.userId = userId;
            this.lock = lock;
            this.createdAt = createdAt;
        }

        // Getter 메서드들
        public String getEmail() { return email; }
        public Integer getUserId() { return userId; }
        public String getLock() { return lock; }
        public String getCreatedAt() { return createdAt; }

        // Jackson을 위한 Setter 메서드들
        public void setEmail(String email) { this.email = email; }
        public void setUserId(Integer userId) { this.userId = userId; }
        public void setLock(String lock) { this.lock = lock; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}