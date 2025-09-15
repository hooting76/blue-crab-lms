// 작업자 : 성태준
// 관리자 JWT 토큰 생성 유틸리티 클래스
// 관리자용 액세스/리프레시 토큰 생성 기능을 담당

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import javax.crypto.SecretKey;

// ========== 외부 라이브러리 ==========
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.AdminTbl;

/* 관리자용 JWT 토큰 생성을 담당하는 유틸리티 클래스
 * 
 * 주요 기능:
 * - 관리자용 액세스 토큰 생성
 * - 관리자용 리프레시 토큰 생성
 * - 토큰 쌍(액세스/리프레시) 생성
 * 
 * 설계 원칙:
 * - 단일 책임: JWT 토큰 생성에만 집중
 * - 재사용성: 다양한 관리자 컨트롤러에서 활용 가능
 * - 보안성: 일관된 토큰 생성 로직 보장
 */
@Component
@Slf4j
public class AdminJwtTokenBuilder {
    
    // ========== 토큰 설정 상수 ==========
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000L; // 15분
    private static final long REFRESH_TOKEN_EXPIRATION = 24 * 60 * 60 * 1000L; // 24시간
    
    // ========== 의존성 주입 ==========
    @Autowired
    private BlueCrab.com.example.config.AppConfig appConfig;
    // 애플리케이션 설정 (JWT 시크릿 키 포함)
    
    /* 토큰 쌍을 담는 내부 클래스 */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;
        private final long expiresIn; // 액세스 토큰 만료 시간 (초 단위)
        
        public TokenPair(String accessToken, String refreshToken, long expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
        
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public long getExpiresIn() { return expiresIn; }
    }
    
    /* 관리자용 토큰 쌍(액세스/리프레시) 생성
     * 
     * @param admin 관리자 정보
     * @return 생성된 토큰 쌍 객체
     */
    public TokenPair buildTokenPair(AdminTbl admin) {
        try {
            log.debug("Building token pair for admin: {}", admin.getAdminId());
            
            String accessToken = buildAccessToken(admin);
            String refreshToken = buildRefreshToken(admin);
            long expiresIn = ACCESS_TOKEN_EXPIRATION / 1000; // 초 단위로 변환
            
            log.info("Successfully generated token pair for adminId: {}", admin.getAdminId());
            return new TokenPair(accessToken, refreshToken, expiresIn);
            
        } catch (Exception e) {
            log.error("Failed to build token pair for admin: {}, Error: {}", admin.getAdminId(), e.getMessage(), e);
            throw new RuntimeException("토큰 생성 중 오류가 발생했습니다.", e);
        }
    }
    
    /* 관리자용 액세스 토큰 생성
     * 
     * @param admin 관리자 정보
     * @return 생성된 액세스 토큰
     */
    private String buildAccessToken(AdminTbl admin) {
        SecretKey key = Keys.hmacShaKeyFor(appConfig.getJwt().getSecret().getBytes());
        long now = System.currentTimeMillis();
        int adminSys = admin.getAdminSys() != null ? admin.getAdminSys() : 0;
        
        String accessToken = Jwts.builder()
            .claim("adminId", admin.getAdminId())
            .claim("adminSys", adminSys)
            .claim("adminName", admin.getName())
            .claim("type", "access")
            .subject(admin.getAdminId())
            .issuedAt(new java.util.Date(now))
            .expiration(new java.util.Date(now + ACCESS_TOKEN_EXPIRATION))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
            
        log.debug("Generated access token for adminId: {}", admin.getAdminId());
        return accessToken;
    }
    
    /* 관리자용 리프레시 토큰 생성
     * 
     * @param admin 관리자 정보
     * @return 생성된 리프레시 토큰
     */
    private String buildRefreshToken(AdminTbl admin) {
        SecretKey key = Keys.hmacShaKeyFor(appConfig.getJwt().getSecret().getBytes());
        long now = System.currentTimeMillis();
        int adminSys = admin.getAdminSys() != null ? admin.getAdminSys() : 0;
        
        String refreshToken = Jwts.builder()
            .claim("adminId", admin.getAdminId())
            .claim("adminSys", adminSys)
            .claim("adminName", admin.getName())
            .claim("type", "refresh")
            .subject(admin.getAdminId())
            .issuedAt(new java.util.Date(now))
            .expiration(new java.util.Date(now + REFRESH_TOKEN_EXPIRATION))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
            
        log.debug("Generated refresh token for adminId: {}", admin.getAdminId());
        return refreshToken;
    }
}