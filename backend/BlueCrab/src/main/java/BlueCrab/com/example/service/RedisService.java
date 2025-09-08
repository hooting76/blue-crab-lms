package BlueCrab.com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.Cursor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis를 활용한 서비스 클래스
 * 로그인 시도 제한, 이메일 인증 토큰 관리, 토큰 블랙리스트 등을 처리
 *
 * 주요 기능:
 * - 로그인 시도 횟수 제한 (Rate Limiting)
 * - 이메일 인증 시도 횟수 제한
 * - 토큰 블랙리스트 관리
 * - TTL을 활용한 자동 만료 처리
 *
 * Redis Key 구조:
 * - login_attempts:{identifier} : 로그인 시도 횟수
 * - email_verify_attempts:{token} : 이메일 인증 시도 횟수
 * - blacklist:access:{token} : 액세스 토큰 블랙리스트
 * - email_token:{token} : 이메일 인증 토큰 정보
 * - admin_status:{adminId} : 관리자 계정 상태 캐시
 * - admin_suspend:{adminId} : 관리자 정지 정보
 */
@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 로그인 시도 제한 설정
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOGIN_LOCKOUT_MINUTES = 15;
    
    // 이메일 인증 시도 제한 설정
    private static final int MAX_EMAIL_VERIFY_ATTEMPTS = 3;
    private static final int EMAIL_VERIFY_LOCKOUT_MINUTES = 10;

    /**
     * 로그인 시도 횟수 확인 및 증가
     * 최대 시도 횟수 초과 시 예외 발생
     * 
     * @param identifier 식별자 (IP + 관리자ID)
     * @throws RuntimeException 최대 시도 횟수 초과 시
     */
    public void checkLoginAttempts(String identifier) {
        String key = "login_attempts:" + identifier;
        String attemptsStr = redisTemplate.opsForValue().get(key);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            Long ttl = redisTemplate.getExpire(key);
            throw new RuntimeException("로그인 시도 횟수를 초과했습니다. " + ttl + "초 후에 다시 시도하세요.");
        }
        
        // 시도 횟수 증가 (15분 TTL)
        redisTemplate.opsForValue().set(key, String.valueOf(attempts + 1), Duration.ofMinutes(LOGIN_LOCKOUT_MINUTES));
        
        logger.info("Login attempt recorded for {}: {}/{}", identifier, attempts + 1, MAX_LOGIN_ATTEMPTS);
    }

    /**
     * 로그인 시도 횟수 초기화
     * 로그인 성공 시 호출
     * 
     * @param identifier 식별자 (IP + 관리자ID)
     */
    public void clearLoginAttempts(String identifier) {
        String key = "login_attempts:" + identifier;
        redisTemplate.delete(key);
        logger.info("Login attempts cleared for {}", identifier);
    }

    /**
     * 이메일 인증 시도 횟수 확인 및 증가
     * 최대 시도 횟수 초과 시 예외 발생
     * 
     * @param token 이메일 인증 토큰
     * @throws RuntimeException 최대 시도 횟수 초과 시
     */
    public void checkEmailVerificationAttempts(String token) {
        String key = "email_verify_attempts:" + token;
        String attemptsStr = redisTemplate.opsForValue().get(key);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        
        if (attempts >= MAX_EMAIL_VERIFY_ATTEMPTS) {
            Long ttl = redisTemplate.getExpire(key);
            throw new RuntimeException("이메일 인증 시도 횟수를 초과했습니다. " + ttl + "초 후에 다시 시도하세요.");
        }
        
        // 시도 횟수 증가 (10분 TTL)
        redisTemplate.opsForValue().set(key, String.valueOf(attempts + 1), Duration.ofMinutes(EMAIL_VERIFY_LOCKOUT_MINUTES));
        
        logger.info("Email verification attempt recorded for token: {}/{}", attempts + 1, MAX_EMAIL_VERIFY_ATTEMPTS);
    }

    /**
     * 이메일 인증 시도 횟수 초기화
     * 이메일 인증 성공 시 호출
     * 
     * @param token 이메일 인증 토큰
     */
    public void clearEmailVerificationAttempts(String token) {
        String key = "email_verify_attempts:" + token;
        redisTemplate.delete(key);
        logger.info("Email verification attempts cleared for token");
    }

    /**
     * 액세스 토큰을 블랙리스트에 추가
     * 관리자 계정 정지 시 현재 토큰 무효화용
     * 
     * @param token 액세스 토큰
     * @param expirationSeconds 토큰 만료까지 남은 시간 (초)
     */
    public void blacklistAccessToken(String token, long expirationSeconds) {
        String key = "blacklist:access:" + token;
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(expirationSeconds));
        logger.info("Access token blacklisted for {} seconds", expirationSeconds);
    }

    /**
     * 액세스 토큰이 블랙리스트에 있는지 확인
     * 
     * @param token 액세스 토큰
     * @return boolean 블랙리스트에 있으면 true
     */
    public boolean isAccessTokenBlacklisted(String token) {
        String key = "blacklist:access:" + token;
        return redisTemplate.hasKey(key);
    }

    /**
     * 이메일 인증 토큰 저장
     * 
     * @param token 이메일 인증 토큰
     * @param adminId 관리자 ID
     * @param expirationMinutes 만료 시간 (분)
     */
    public void storeEmailVerificationToken(String token, String adminId, int expirationMinutes) {
        String key = "email_token:" + token;
        redisTemplate.opsForValue().set(key, adminId, Duration.ofMinutes(expirationMinutes));
        logger.info("Email verification token stored for admin: {}", adminId);
    }

    /**
     * 이메일 인증 토큰으로 관리자 ID 조회
     * 
     * @param token 이메일 인증 토큰
     * @return String 관리자 ID (토큰이 유효하지 않으면 null)
     */
    public String getAdminIdByEmailToken(String token) {
        String key = "email_token:" + token;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 이메일 인증 토큰 삭제 (블랙리스트 처리)
     * 
     * @param token 이메일 인증 토큰
     */
    public void deleteEmailVerificationToken(String token) {
        String key = "email_token:" + token;
        redisTemplate.delete(key);
        logger.info("Email verification token deleted (blacklisted)");
    }

    /**
     * Redis 연결 상태 확인
     * 
     * @return boolean Redis 연결 가능하면 true
     */
    public boolean isRedisAvailable() {
        try {
            redisTemplate.opsForValue().get("health_check");
            return true;
        } catch (Exception e) {
            logger.error("Redis connection failed", e);
            return false;
        }
    }

    /**
     * 키의 TTL(만료 시간) 조회
     * 
     * @param key Redis 키
     * @return long TTL(초), 키가 없거나 만료시간이 없으면 -1
     */
    public long getKeyTTL(String key) {
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            logger.error("Error getting TTL for key: " + key, e);
            return -1;
        }
    }

    // ===== Redis 관리 기능 =====

    /**
     * 모든 키 조회 (패턴 기반)
     * 
     * @param pattern 키 패턴 (기본값: "*")
     * @param limit 최대 조회 개수 (기본값: 100)
     * @return List<String> 키 목록
     */
    public List<String> getAllKeys(String pattern, int limit) {
        List<String> keys = new ArrayList<>();
        try {
            if (pattern == null || pattern.trim().isEmpty()) {
                pattern = "*";
            }
            
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(limit)
                    .build();
            
            Cursor<String> cursor = redisTemplate.scan(options);
            int count = 0;
            while (cursor.hasNext() && count < limit) {
                keys.add(cursor.next());
                count++;
            }
            cursor.close();
            
        } catch (Exception e) {
            logger.error("Error getting keys with pattern: " + pattern, e);
        }
        return keys;
    }

    /**
     * 키별 상세 정보 조회
     * 
     * @param key Redis 키
     * @return Map<String, Object> 키 정보 (타입, 값, TTL 등)
     */
    public Map<String, Object> getKeyDetails(String key) {
        Map<String, Object> details = new HashMap<>();
        try {
            if (!redisTemplate.hasKey(key)) {
                details.put("exists", false);
                return details;
            }

            details.put("exists", true);
            details.put("key", key);
            
            // 키 타입 조회
            String type = redisTemplate.type(key).name();
            details.put("type", type);
            
            // TTL 조회
            long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            details.put("ttl", ttl);
            details.put("ttlFormatted", ttl > 0 ? ttl + "초" : (ttl == -1 ? "영구" : "만료됨"));
            
            // 값 조회 (타입별로 다르게 처리)
            Object value = null;
            switch (type) {
                case "STRING":
                    value = redisTemplate.opsForValue().get(key);
                    break;
                case "HASH":
                    value = redisTemplate.opsForHash().entries(key);
                    break;
                case "LIST":
                    value = redisTemplate.opsForList().range(key, 0, -1);
                    break;
                case "SET":
                    value = redisTemplate.opsForSet().members(key);
                    break;
                case "ZSET":
                    value = redisTemplate.opsForZSet().range(key, 0, -1);
                    break;
            }
            details.put("value", value);
            
            // 값의 크기 정보
            if (value != null) {
                if (value instanceof String) {
                    details.put("size", ((String) value).length() + " 문자");
                } else if (value instanceof Collection) {
                    details.put("size", ((Collection<?>) value).size() + " 개 요소");
                } else if (value instanceof Map) {
                    details.put("size", ((Map<?, ?>) value).size() + " 개 필드");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error getting details for key: " + key, e);
            details.put("error", "키 정보 조회 실패: " + e.getMessage());
        }
        return details;
    }

    /**
     * Redis 전체 정보 조회
     * 
     * @return Map<String, Object> Redis 서버 정보
     */
    public Map<String, Object> getRedisInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            // 기본 연결 상태
            info.put("connected", isRedisAvailable());
            
            // 키 통계
            List<String> allKeys = getAllKeys("*", 1000);
            info.put("totalKeys", allKeys.size());
            
            // 키 패턴별 분류
            Map<String, Integer> keyPatterns = new HashMap<>();
            keyPatterns.put("login_attempts", 0);
            keyPatterns.put("email_verify_attempts", 0);
            keyPatterns.put("blacklist", 0);
            keyPatterns.put("email_token", 0);
            keyPatterns.put("others", 0);
            
            for (String key : allKeys) {
                if (key.startsWith("login_attempts:")) {
                    keyPatterns.put("login_attempts", keyPatterns.get("login_attempts") + 1);
                } else if (key.startsWith("email_verify_attempts:")) {
                    keyPatterns.put("email_verify_attempts", keyPatterns.get("email_verify_attempts") + 1);
                } else if (key.startsWith("blacklist:")) {
                    keyPatterns.put("blacklist", keyPatterns.get("blacklist") + 1);
                } else if (key.startsWith("email_token:")) {
                    keyPatterns.put("email_token", keyPatterns.get("email_token") + 1);
                } else {
                    keyPatterns.put("others", keyPatterns.get("others") + 1);
                }
            }
            info.put("keysByPattern", keyPatterns);
            
            // 현재 시간
            info.put("timestamp", new Date());
            
        } catch (Exception e) {
            logger.error("Error getting Redis info", e);
            info.put("error", "Redis 정보 조회 실패: " + e.getMessage());
        }
        return info;
    }

    /**
     * 키 삭제
     * 
     * @param keys 삭제할 키 목록
     * @return long 실제 삭제된 키 개수
     */
    public long deleteKeys(String... keys) {
        try {
            long deleted = redisTemplate.delete(Arrays.asList(keys));
            logger.info("Deleted {} keys: {}", deleted, Arrays.toString(keys));
            return deleted;
        } catch (Exception e) {
            logger.error("Error deleting keys: " + Arrays.toString(keys), e);
            return 0;
        }
    }

    /**
     * 패턴으로 키 삭제
     * 
     * @param pattern 키 패턴
     * @param limit 최대 삭제 개수
     * @return long 실제 삭제된 키 개수
     */
    public long deleteKeysByPattern(String pattern, int limit) {
        try {
            List<String> keys = getAllKeys(pattern, limit);
            if (keys.isEmpty()) {
                return 0;
            }
            
            long deleted = redisTemplate.delete(keys);
            logger.info("Deleted {} keys with pattern '{}': {}", deleted, pattern, keys);
            return deleted;
        } catch (Exception e) {
            logger.error("Error deleting keys by pattern: " + pattern, e);
            return 0;
        }
    }

    /**
     * 키의 TTL 설정
     * 
     * @param key Redis 키
     * @param seconds TTL (초)
     * @return boolean 설정 성공 여부
     */
    public boolean setKeyTTL(String key, long seconds) {
        try {
            return redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Error setting TTL for key: " + key, e);
            return false;
        }
    }

    /**
     * Redis 메모리 정리 (만료된 키 정리)
     * 
     * @return Map<String, Object> 정리 결과
     */
    public Map<String, Object> cleanupExpiredKeys() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 현재 키 개수
            List<String> beforeKeys = getAllKeys("*", 1000);
            int beforeCount = beforeKeys.size();
            
            // 수동으로 만료된 키들 확인하여 삭제
            List<String> expiredKeys = new ArrayList<>();
            for (String key : beforeKeys) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                if (ttl != null && ttl == -2) { // -2는 키가 만료되었음을 의미
                    expiredKeys.add(key);
                }
            }
            
            long cleaned = 0;
            if (!expiredKeys.isEmpty()) {
                cleaned = deleteKeys(expiredKeys.toArray(new String[0]));
            }
            
            // 정리 후 키 개수
            List<String> afterKeys = getAllKeys("*", 1000);
            int afterCount = afterKeys.size();
            
            result.put("beforeCount", beforeCount);
            result.put("afterCount", afterCount);
            result.put("cleanedKeys", cleaned);
            result.put("expiredKeys", expiredKeys);
            result.put("timestamp", new Date());
            
        } catch (Exception e) {
            logger.error("Error cleaning up expired keys", e);
            result.put("error", "만료 키 정리 실패: " + e.getMessage());
        }
        return result;
    }

    /**
     * 관리자 계정 상태를 Redis에 캐시
     * 
     * @param adminId 관리자 ID
     * @param status 계정 상태 (active, suspended, banned)
     * @param cacheDurationMinutes 캐시 유지 시간 (분)
     */
    public void cacheAdminStatus(String adminId, String status, int cacheDurationMinutes) {
        try {
            String key = "admin_status:" + adminId;
            redisTemplate.opsForValue().set(key, status, Duration.ofMinutes(cacheDurationMinutes));
            logger.debug("Admin status cached: {} = {}", adminId, status);
        } catch (Exception e) {
            logger.error("Error caching admin status for: " + adminId, e);
        }
    }

    /**
     * Redis에서 관리자 계정 상태 조회
     * 
     * @param adminId 관리자 ID
     * @return String 계정 상태 (캐시되지 않은 경우 null)
     */
    public String getCachedAdminStatus(String adminId) {
        try {
            String key = "admin_status:" + adminId;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("Error getting cached admin status for: " + adminId, e);
            return null;
        }
    }

    /**
     * 관리자 정지 정보를 Redis에 저장
     * 
     * @param adminId 관리자 ID
     * @param suspendUntilTimestamp 정지 종료 시간 (타임스탬프)
     * @param reason 정지 사유
     */
    public void cacheAdminSuspendInfo(String adminId, long suspendUntilTimestamp, String reason) {
        try {
            String key = "admin_suspend:" + adminId;
            Map<String, String> suspendInfo = new HashMap<>();
            suspendInfo.put("suspendUntil", String.valueOf(suspendUntilTimestamp));
            suspendInfo.put("reason", reason != null ? reason : "");
            suspendInfo.put("cachedAt", String.valueOf(System.currentTimeMillis()));
            
            redisTemplate.opsForHash().putAll(key, suspendInfo);
            
            // 정지 종료 시간까지 TTL 설정 (여유 시간 1시간 추가)
            long ttlSeconds = (suspendUntilTimestamp - System.currentTimeMillis()) / 1000 + 3600;
            if (ttlSeconds > 0) {
                redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
            }
            
            logger.debug("Admin suspend info cached: {}", adminId);
        } catch (Exception e) {
            logger.error("Error caching admin suspend info for: " + adminId, e);
        }
    }

    /**
     * Redis에서 관리자 정지 정보 조회
     * 
     * @param adminId 관리자 ID
     * @return Map<String, String> 정지 정보 (suspendUntil, reason, cachedAt)
     */
    public Map<String, String> getCachedAdminSuspendInfo(String adminId) {
        try {
            String key = "admin_suspend:" + adminId;
            Map<Object, Object> rawData = redisTemplate.opsForHash().entries(key);
            
            if (rawData.isEmpty()) {
                return null;
            }
            
            Map<String, String> suspendInfo = new HashMap<>();
            for (Map.Entry<Object, Object> entry : rawData.entrySet()) {
                suspendInfo.put(entry.getKey().toString(), entry.getValue().toString());
            }
            
            return suspendInfo;
        } catch (Exception e) {
            logger.error("Error getting cached admin suspend info for: " + adminId, e);
            return null;
        }
    }

    /**
     * 관리자 계정 상태 캐시 삭제
     * 계정 상태 변경 시 호출
     * 
     * @param adminId 관리자 ID
     */
    public void clearAdminStatusCache(String adminId) {
        try {
            String statusKey = "admin_status:" + adminId;
            String suspendKey = "admin_suspend:" + adminId;
            
            redisTemplate.delete(statusKey);
            redisTemplate.delete(suspendKey);
            
            logger.debug("Admin status cache cleared: {}", adminId);
        } catch (Exception e) {
            logger.error("Error clearing admin status cache for: " + adminId, e);
        }
    }

    /**
     * 관리자 계정 상태 검증
     * Redis 캐시를 먼저 확인하고, 없으면 DB 조회 필요함을 알림
     * 
     * @param adminId 관리자 ID
     * @return Map<String, Object> 상태 정보 및 캐시 여부
     */
    public Map<String, Object> validateAdminStatus(String adminId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 기본 상태 캐시 확인
            String cachedStatus = getCachedAdminStatus(adminId);
            result.put("cachedStatus", cachedStatus);
            result.put("cacheHit", cachedStatus != null);
            
            if ("suspended".equals(cachedStatus)) {
                // 2. 정지 정보 캐시 확인
                Map<String, String> suspendInfo = getCachedAdminSuspendInfo(adminId);
                if (suspendInfo != null) {
                    long suspendUntil = Long.parseLong(suspendInfo.get("suspendUntil"));
                    long currentTime = System.currentTimeMillis();
                    
                    result.put("suspendInfo", suspendInfo);
                    result.put("isStillSuspended", currentTime < suspendUntil);
                    result.put("remainingTime", Math.max(0, suspendUntil - currentTime));
                }
            }
            
            result.put("needsDbCheck", cachedStatus == null);
            
        } catch (Exception e) {
            logger.error("Error validating admin status for: " + adminId, e);
            result.put("error", e.getMessage());
            result.put("needsDbCheck", true);
        }
        
        return result;
    }
}
