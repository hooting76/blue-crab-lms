package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.AdminLoginRequest;
import BlueCrab.com.example.dto.AdminLoginResponse;
import BlueCrab.com.example.dto.ApiResponse;
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

    /**
     * 관리자 1차 로그인 (ID/PW 검증 + 이메일 인증 토큰 발급)
     * 
     * @param loginRequest 관리자 로그인 요청 (adminId, password)
     * @param request HTTP 요청 정보 (IP 추출용)
     * @return 이메일 인증 토큰 및 안내 메시지
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> adminLogin(
            @RequestBody AdminLoginRequest loginRequest,
            HttpServletRequest request) {

        try {
            // TODO: 실제 관리자 인증 서비스 구현 필요
            // 1. AdminService.authenticate(adminId, password)
            // 2. 이메일 인증 토큰 생성
            // 3. 이메일 발송
            // 4. AdminLoginResponse 반환

            // 임시 응답 (테스트용)
            AdminLoginResponse response = new AdminLoginResponse(
                "이메일 인증이 필요합니다. 이메일을 확인해주세요.",
                "temp-email-verification-token-12345",
                600L  // 10분
            );

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
     * 관리자 이메일 인증 (2차 인증)
     * 
     * @param token 이메일 인증 토큰
     * @return JWT 액세스 토큰 및 관리자 정보
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Object>> verifyEmail(@RequestParam String token) {
        
        try {
            // TODO: 실제 이메일 토큰 검증 로직 구현 필요
            // 1. Redis에서 토큰 검증
            // 2. 토큰 만료 확인
            // 3. JWT AccessToken 생성
            // 4. 토큰 블랙리스트 처리

            // 임시 응답 (테스트용)
            return ResponseEntity.ok(
                ApiResponse.success("이메일 인증이 완료되었습니다. JWT 토큰이 발급되었습니다.", null)
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.failure("이메일 인증 처리 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }
}
