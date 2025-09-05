package BlueCrab.com.example.util;

import BlueCrab.com.example.config.AppConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 토큰 생성, 파싱, 검증을 담당하는 유틸리티 클래스
 * 액세스 토큰과 리프레시 토큰의 생성 및 관리 기능 제공
 */
@Component
public class JwtUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Autowired
    private AppConfig appConfig;

    /**
     * JWT 서명을 위한 비밀 키 생성
     * 설정된 비밀 문자열에서 HMAC SHA 키를 생성
     * 
     * @return HMAC SHA 비밀 키
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(appConfig.getJwt().getSecret().getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 액세스 토큰 생성
     * 짧은 유효기간의 액세스 토큰을 생성 (기본 15분)
     * 
     * @param username 사용자명 (이메일)
     * @param userId 사용자 ID
     * @return 생성된 액세스 토큰
     */
    public String generateAccessToken(String username, Integer userId) {
        logger.debug("Generating access token for user: {}", username);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "access");
        return createToken(claims, username, appConfig.getJwt().getAccessTokenExpiration());
    }

    /**
     * 리프레시 토큰 생성
     * 긴 유효기간의 리프레시 토큰을 생성 (기본 24시간)
     * 
     * @param username 사용자명 (이메일)
     * @param userId 사용자 ID
     * @return 생성된 리프레시 토큰
     */
    public String generateRefreshToken(String username, Integer userId) {
        logger.debug("Generating refresh token for user: {}", username);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        return createToken(claims, username, appConfig.getJwt().getRefreshTokenExpiration());
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    public Boolean isAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "access".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public Integer extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return Integer.valueOf(claims.get("userId").toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 토큰의 만료 시간을 밀리초로 반환
     * 
     * @param token JWT 토큰
     * @return 만료 시간 (밀리초)
     */
    public long getTokenExpiration(String token) {
        Date expiration = extractExpiration(token);
        return expiration.getTime();
    }

    /**
     * 토큰 유효성 검증
     * 토큰의 서명, 만료 시간 등을 검증
     * 
     * @param token 검증할 JWT 토큰
     * @return 토큰 유효성 여부
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT token unsupported: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.warn("JWT token malformed: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token illegal argument: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            logger.warn("JWT token validation error: {}", e.getMessage());
            return false;
        }
    }
}
