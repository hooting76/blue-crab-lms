package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.AdminLoginRequest;
import BlueCrab.com.example.dto.AdminLoginResponse;
import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import javax.servlet.http.HttpServletRequest;

/**
 * 관리자 전용 REST API 컨트롤러
 * 관리자 로그인, 이메일 인증 등을 처리
 *
 * 주요 기능:
 * - 관리자 1차 로그인 (ID/PW 검증)
 * - 이메일 인증 토큰 발급
 * - 2차 인증 처리
 *
 * 보안 정책:
 * - 1차 로그인은 permitAll (SecurityConfig 설정)
 * - 2차 인증 완료 후 JWT 토큰 발급
 * - 실패 시 상세한 에러 로그 기록
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 관리자 1차 로그인 (ID/PW 검증 + 세션 토큰 발급)
     * 
     * @param loginRequest 관리자 로그인 요청 (adminId, password)
     * @param request HTTP 요청 정보 (IP 추출용)
     * @return 세션 토큰 및 안내 메시지
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> adminLogin(
            @RequestBody AdminLoginRequest loginRequest,
            HttpServletRequest request) {

        try {
            // 클라이언트 IP 추출
            String clientIp = getClientIpAddress(request);
            
            // 실제 관리자 인증 서비스 호출
            AdminLoginResponse response = adminService.authenticateAndGenerateSessionToken(
                loginRequest, clientIp);

            return ResponseEntity.ok(
                ApiResponse.success("1차 인증이 완료되었습니다", response)
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.failure("관리자 로그인 처리 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     * 프록시 환경을 고려한 실제 IP 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 관리자 이메일 인증 (2차 인증)
     * 
     * @param token 이메일 인증 토큰
     * @return JWT 액세스 토큰 및 관리자 정보
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Object>> verifyEmail(@RequestParam String token) {
        
        try {
            // 실제 이메일 토큰 검증 및 JWT 발급
            Object jwtResponse = adminService.verifyEmailAndGenerateJwt(token);

            return ResponseEntity.ok(
                ApiResponse.success("이메일 인증이 완료되었습니다. JWT 토큰이 발급되었습니다.", jwtResponse)
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.failure("이메일 인증 처리 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
}
