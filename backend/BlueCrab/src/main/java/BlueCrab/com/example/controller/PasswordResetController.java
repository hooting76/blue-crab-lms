package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.PasswordResetIdentityRequest;
import BlueCrab.com.example.dto.PasswordResetIdentityResponse;
import BlueCrab.com.example.service.PasswordResetService;
import BlueCrab.com.example.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

/**
 * 비밀번호 재설정 처리를 위한 REST API 컨트롤러
 * 4단계 비밀번호 재설정 플로우를 제공
 *
 * 보안 설계 원칙:
 * - Replace-on-new: 새로운 본인확인 시 이전 토큰들 자동 무효화
 * - 중립적 응답: 계정 존재 여부를 노출하지 않음
 * - 레이트 리미팅: IP/이메일별 요청 횟수 제한
 * - 타이밍 어택 방지: 상수시간 비교
 *
 * API 응답 형식:
 * - 모든 응답은 ApiResponse<T> 형식으로 통일
 * - 성공/실패 여부와 관계없이 일관된 메시지 제공
 * - 보안을 위해 내부 오류 정보를 클라이언트에 노출하지 않음
 *
 * 플로우:
 * 1. POST /api/auth/password-reset/verify-identity - 본인확인
 * 2. POST /api/auth/password-reset/send-email - 이메일 발송
 * 3. POST /api/auth/password-reset/verify-code - 코드 검증
 * 4. POST /api/auth/password-reset/change-password - 비밀번호 변경
 */
@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * 1단계: 본인확인 처리
     * 사용자가 이메일, 학번, 이름, 전화번호를 입력하여 본인 여부를 확인
     * 
     * 보안 특징:
     * - 성공/실패 시 모두 동일한 중립적 메시지 반환
     * - 내부적으로만 IRT 토큰 발급 여부 결정
     * - 레이트 리미팅으로 무차별 대입 공격 방지
     * - Replace-on-new 방식으로 이전 토큰들 무효화
     * - 4개 필드 모두 일치해야 성공 (이메일, 학번, 이름, 전화번호)
     *
     * @param request 본인확인 요청 데이터 (이메일, 학번, 이름, 전화번호)
     * @param httpRequest HTTP 요청 객체 (IP 추출용)
     * @return ApiResponse<PasswordResetIdentityResponse> 본인확인 결과
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "요청이 접수되었습니다. 입력 정보가 정확하다면 이메일이 곧 도착합니다.",
     *   "data": {
     *     "success": true,
     *     "message": "요청이 접수되었습니다. 입력 정보가 정확하다면 이메일이 곧 도착합니다.",
     *     "identityToken": "IRT_TOKEN_HERE", // 성공 시에만 포함
     *     "maskedEmail": "u****@example.com" // 성공 시에만 포함
     *   },
     *   "timestamp": "2024-03-20T10:30:00Z"
     * }
     */
    @PostMapping("/verify-identity")
    public ResponseEntity<ApiResponse<PasswordResetIdentityResponse>> verifyIdentity(
            @Valid @RequestBody PasswordResetIdentityRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // 클라이언트 IP 주소 추출
            String clientIp = RequestUtils.getClientIpAddress(httpRequest);
            
            logger.info("Password reset identity verification started - Email: {}, IP: {}", 
                request.getEmail(), clientIp);

            // 본인확인 처리
            PasswordResetIdentityResponse response = passwordResetService.verifyIdentity(request, clientIp);

            // 성공 응답 반환 (실제 성공/실패와 관계없이 항상 200 OK)
            return ResponseEntity.ok(ApiResponse.success(
                "본인확인 요청이 처리되었습니다",
                response
            ));

        } catch (Exception e) {
            logger.error("Unexpected error during password reset identity verification", e);
            
            // 예외 발생 시에도 중립적 응답
            PasswordResetIdentityResponse neutralResponse = PasswordResetIdentityResponse.neutral();
            return ResponseEntity.ok(ApiResponse.success(
                "본인확인 요청이 처리되었습니다",
                neutralResponse
            ));
        }
    }

    /**
     * 디버깅을 위한 레이트 리미팅 상태 조회 엔드포인트
     * 개발/테스트 환경에서만 사용하며, 운영 환경에서는 비활성화 권장
     *
     * @param email 조회할 이메일 (선택적)
     * @param httpRequest HTTP 요청 객체 (IP 추출용)
     * @return 레이트 리미팅 상태 정보
     */
    @GetMapping("/rate-limit-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateLimitStatus(
            @RequestParam(required = false) String email,
            HttpServletRequest httpRequest) {
        
        try {
            String clientIp = RequestUtils.getClientIpAddress(httpRequest);
            String targetEmail = email != null ? email : "anonymous@example.com";
            
            Map<String, Object> status = passwordResetService.getRateLimitStatus(clientIp, targetEmail);
            
            return ResponseEntity.ok(ApiResponse.success(
                "레이트 리미팅 상태 조회 완료",
                status
            ));
            
        } catch (Exception e) {
            logger.error("Error getting rate limit status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("상태 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 전역 예외 처리
     * 예상하지 못한 오류 발생 시 중립적 응답 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<PasswordResetIdentityResponse>> handleException(Exception e) {
        logger.error("Unhandled exception in password reset controller", e);
        
        // 모든 예외를 중립적 응답으로 처리
        PasswordResetIdentityResponse neutralResponse = PasswordResetIdentityResponse.neutral();
        return ResponseEntity.ok(ApiResponse.success(
            "요청이 처리되었습니다",
            neutralResponse
        ));
    }
}