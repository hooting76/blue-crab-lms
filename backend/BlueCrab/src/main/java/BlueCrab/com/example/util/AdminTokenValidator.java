// 작업자 : 성태준
// 관리자 토큰 검증 유틸리티 클래스
// Bearer 토큰 검증 및 관리자 이메일 추출 기능을 담당

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/* 관리자 토큰 검증과 이메일 추출을 담당하는 유틸리티 클래스
 * 
 * 주요 기능:
 * - Bearer 토큰 형식 검증
 * - JWT에서 관리자 이메일 추출
 * - 토큰 유효성 검사
 * 
 * 설계 원칙:
 * - 단일 책임: 관리자 토큰 처리에만 집중
 * - 재사용성: 다양한 관리자 컨트롤러에서 활용 가능
 * - 보안성: 토큰 검증 로직의 일관성 보장
 */
@Component
@Slf4j
public class AdminTokenValidator {
    
    // ========== 의존성 주입 ==========
    @Autowired
    private BlueCrab.com.example.service.EmailVerificationService emailVerificationService;
    // 이메일 인증 서비스 (토큰에서 이메일 추출용)
    
    /* Bearer 토큰에서 관리자 이메일을 추출하는 메서드
     * 
     * @param bearerToken "Bearer <JWT>" 형식의 토큰 문자열
     * @return 추출된 관리자 이메일 주소 (실패 시 null)
     */
    public String extractEmailFromBearerToken(String bearerToken) {
        try {
            // Bearer 토큰 형식 검증
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                log.warn("Invalid bearer token format - token: {}", 
                    bearerToken != null ? bearerToken.substring(0, Math.min(20, bearerToken.length())) + "..." : "null");
                return null;
            }
            
            // "Bearer " 부분 제거하여 JWT 추출
            String jwt = bearerToken.substring(7);
            log.debug("Extracted JWT token: {}", jwt.substring(0, Math.min(20, jwt.length())) + "...");
            
            // EmailVerificationService를 통해 이메일 추출
            String email = emailVerificationService.extractAdminIdFromSessionToken(jwt);
            
            if (email != null) {
                log.debug("Successfully extracted admin email from token");
                return email;
            } else {
                log.warn("Failed to extract admin email from token - email is null");
                return null;
            }
            
        } catch (Exception e) {
            log.error("Exception occurred while extracting email from bearer token: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /* Bearer 토큰의 유효성을 검사하는 메서드
     * 
     * @param bearerToken "Bearer <JWT>" 형식의 토큰 문자열
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValidBearerToken(String bearerToken) {
        return extractEmailFromBearerToken(bearerToken) != null;
    }
    
    /* JWT 토큰 자체의 유효성을 검사하는 메서드
     * 
     * @param jwt JWT 토큰 문자열 ("Bearer " 제거된 상태)
     * @return JWT가 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateAdminToken(String jwt) {
        try {
            if (jwt == null || jwt.trim().isEmpty()) {
                log.warn("JWT token is null or empty");
                return false;
            }
            
            // EmailVerificationService를 통한 토큰 검증
            String email = emailVerificationService.extractAdminIdFromSessionToken(jwt);
            return email != null && !email.trim().isEmpty();
            
        } catch (Exception e) {
            log.error("Exception occurred while validating admin token: {}", e.getMessage(), e);
            return false;
        }
    }
}