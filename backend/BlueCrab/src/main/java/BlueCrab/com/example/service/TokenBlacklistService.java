package BlueCrab.com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 토큰 블랙리스트 관리 서비스
 * Redis를 사용하여 무효화된 JWT 토큰을 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    private static final String ACCESS_TOKEN_PREFIX = "access:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    /**
     * Access Token을 블랙리스트에 추가
     * @param token JWT 토큰
     * @param expirationTime 토큰 만료 시간 (밀리초)
     */
    public void addAccessTokenToBlacklist(String token, long expirationTime) {
        try {
            String key = BLACKLIST_PREFIX + ACCESS_TOKEN_PREFIX + token;
            long ttlMillis = calculateTtlMillis(expirationTime);
            
            if (ttlMillis > 0) {
                redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(ttlMillis));
                log.info("Access token added to blacklist with TTL: {} ms", ttlMillis);
            } else {
                log.warn("Token expiration time is in the past, not adding to blacklist");
            }
        } catch (Exception e) {
            log.error("Failed to add access token to blacklist: {}", e.getMessage());
        }
    }

    /**
     * Refresh Token을 블랙리스트에 추가
     * @param token JWT 토큰
     * @param expirationTime 토큰 만료 시간 (밀리초)
     */
    public void addRefreshTokenToBlacklist(String token, long expirationTime) {
        try {
            String key = BLACKLIST_PREFIX + REFRESH_TOKEN_PREFIX + token;
            long ttlMillis = calculateTtlMillis(expirationTime);
            
            if (ttlMillis > 0) {
                redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(ttlMillis));
                log.info("Refresh token added to blacklist with TTL: {} ms", ttlMillis);
            } else {
                log.warn("Token expiration time is in the past, not adding to blacklist");
            }
        } catch (Exception e) {
            log.error("Failed to add refresh token to blacklist: {}", e.getMessage());
        }
    }

    /**
     * Access Token이 블랙리스트에 있는지 확인
     * @param token JWT 토큰
     * @return 블랙리스트에 있으면 true, 없으면 false
     */
    public boolean isAccessTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + ACCESS_TOKEN_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check access token blacklist status: {}", e.getMessage());
            return false; // Redis 오류 시 안전하게 false 반환
        }
    }

    /**
     * Refresh Token이 블랙리스트에 있는지 확인
     * @param token JWT 토큰
     * @return 블랙리스트에 있으면 true, 없으면 false
     */
    public boolean isRefreshTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + REFRESH_TOKEN_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check refresh token blacklist status: {}", e.getMessage());
            return false; // Redis 오류 시 안전하게 false 반환
        }
    }

    /**
     * 사용자의 모든 토큰을 블랙리스트에서 제거 (새 로그인 시 사용)
     * @param username 사용자명
     */
    public void removeUserTokensFromBlacklist(String username) {
        try {
            String pattern = BLACKLIST_PREFIX + "*";
            redisTemplate.delete(redisTemplate.keys(pattern));
            log.info("Removed all tokens from blacklist for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to remove user tokens from blacklist: {}", e.getMessage());
        }
    }

    /**
     * TTL 계산 (현재 시간부터 토큰 만료까지의 시간)
     * @param expirationTime 토큰 만료 시간 (밀리초)
     * @return TTL (밀리초)
     */
    private long calculateTtlMillis(long expirationTime) {
        long currentTime = System.currentTimeMillis();
        long ttlMillis = expirationTime - currentTime;
        return ttlMillis > 0 ? ttlMillis : 0;
    }

    /**
     * 블랙리스트 통계 조회 (디버깅 용도)
     * @return 블랙리스트에 있는 토큰 수
     */
    public long getBlacklistSize() {
        try {
            String pattern = BLACKLIST_PREFIX + "*";
            return redisTemplate.countExistingKeys(redisTemplate.keys(pattern));
        } catch (Exception e) {
            log.error("Failed to get blacklist size: {}", e.getMessage());
            return 0;
        }
    }
}
