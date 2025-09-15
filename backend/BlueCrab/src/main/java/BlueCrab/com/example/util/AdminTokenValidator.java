// 작업자 : 성태준
// 관리자 토큰 검증 유틸리티 클래스
// Bearer 토큰 검증 및 관리자 이메일 추출 기능을 담당

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========

// ========== 외부 라이브러리 ==========
import lombok.extern.slf4j.Slf4j;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class AdminTokenValidator {
    
    // ========== 의존성 주입 ==========
    @Autowired
    private BlueCrab.com.example.service.EmailVerificationService emailVerificationService;
    // 이메일 인증 서비스 (토큰에서 이메일 추출용)
    
    public String extractEmailFromBearerToken(String bearerToken) {
        // Bearer 토큰에서 관리자 이메일을 추출하는 메서드
        try {
            // Bearer 토큰 형식 검증
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                // 형식이 올바르지 않은 경우
                log.warn("Invalid bearer token format - token: {}", 
                    bearerToken != null ? bearerToken.substring(0, Math.min(20, bearerToken.length())) + "..." : "null");
                // 경고 로그 기록
                return null;
                // null 반환하여 실패를 나타냄
            } // if 끝
            
            String jwt = bearerToken.substring(7);
            // "Bearer " 부분 제거하여 JWT 추출
            log.debug("Extracted JWT token: {}", jwt.substring(0, Math.min(20, jwt.length())) + "...");
            // 디버그 로그 기록

            String email = emailVerificationService.extractAdminIdFromSessionToken(jwt);
            // EmailVerificationService를 통해 이메일 추출
            
            if (email != null) {
                // 이메일이 정상적으로 추출된 경우
                log.debug("Successfully extracted admin email from token");
                // 디버그 로그 기록
                return email;
                // 추출된 이메일 반환
            } else {
                // 이메일 추출 실패의 경우
                log.warn("Failed to extract admin email from token - email is null");
                // 경고 로그 기록
                return null;
                // null 반환하여 실패를 나타냄
            } // if 끝
            
        } catch (Exception e) {
            // 예외 처리
            log.error("Exception occurred while extracting email from bearer token: {}", e.getMessage(), e);
            // 에러 로그 기록
            return null;
            // null 반환하여 실패를 나타냄
        } // 메서드 끝
    } // 클래스 끝
    
    public boolean isValidBearerToken(String bearerToken) {
        // Bearer 토큰의 유효성을 검사하는 메서드
        return extractEmailFromBearerToken(bearerToken) != null;
        // 이메일이 추출되면 유효한 토큰으로 간주
    } // isValidBearerToken 메서드 끝

    public boolean validateAdminToken(String jwt) {
        // JWT 토큰의 유효성을 검사하는 메서드
        try {
            if (jwt == null || jwt.trim().isEmpty()) {
                // 토큰이 null이거나 빈 문자열인 경우
                log.warn("JWT token is null or empty");
                // 경고 로그 기록
                return false;
                // false 반환
            } // if 끝
            
            String email = emailVerificationService.extractAdminIdFromSessionToken(jwt);
            // EmailVerificationService를 통한 토큰 검증
            return email != null && !email.trim().isEmpty();
            // 이메일이 null이 아니고 빈 문자열이 아님을 반환 하여 유효성 판단
            
        } catch (Exception e) {
            // 예외 처리
            log.error("Exception occurred while validating admin token: {}", e.getMessage(), e);
            // 오류 로그 기록
            return false;
            // false 반환
        } 
    } // validateAdminToken 메서드 끝
} // AdminTokenValidator 클래스 끝