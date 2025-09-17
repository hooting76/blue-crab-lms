package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.PasswordResetIdentityRequest;
import BlueCrab.com.example.dto.PasswordResetIdentityResponse;
import BlueCrab.com.example.dto.PasswordResetCodeVerifyRequest;
import BlueCrab.com.example.dto.PasswordResetCodeVerifyResponse;
import BlueCrab.com.example.dto.ChangePasswordRequest;
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

// ========== PasswordResetEmailController (작성자: 성태준)에서 추가된 import ==========
import BlueCrab.com.example.service.EmailService; // 이메일 발송 서비스
import BlueCrab.com.example.util.AuthCodeGenerator; // 인증 코드 생성기
import BlueCrab.com.example.util.EmailTemplateUtils; // 이메일 템플릿 유틸리티
import BlueCrab.com.example.util.PasswordResetRedisUtil; // Redis 유틸리티
import BlueCrab.com.example.util.AccountRecoveryRateLimiter; // 표준 레이트 리미터

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

    // ========== PasswordResetEmailController (작성자: 성태준)에서 추가된 의존성 주입 ==========
    @Autowired
    private PasswordResetRedisUtil redisUtil;
    // 비밀번호 재설정 관련 Redis 유틸리티
    
    @Autowired
    private AccountRecoveryRateLimiter rateLimiter;
    // 표준 레이트 리미터 (ID/PW 찾기 통합)
    
    @Autowired
    private EmailService emailService;
    // 이메일 발송 서비스
    
    @Autowired
    private AuthCodeGenerator authCodeGenerator;
    // 인증 코드 생성기
    
    @Autowired
    private EmailTemplateUtils emailTemplateUtils;
    // 이메일 템플릿 유틸리티

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

            // 레이트리밋 응답인 경우 HTTP 429 반환
            if (!response.isSuccess() && response.getMessage().contains("요청이 너무 많습니다")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.rateLimitError(response.getMessage(), response));
            }

            // 일반 성공 응답 반환 (실제 성공/실패와 관계없이 항상 200 OK)
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

    // ========== PasswordResetEmailController (작성자: 성태준)에서 추가된 메서드 ==========
    /* 2단계: 이메일 발송 처리 (성태준 작성)
     * 인증 코드 이메일 발송 엔드포인트 (IP 기반 제한)
     */
    @PostMapping("/send-email")
    public ResponseEntity<?> sendResetEmail(@RequestBody Map<String, String> 
                request, HttpServletRequest httpRequest) {
        // ResponseEntity<?> : 다양한 응답 타입 지원
        // sendResetEmail : 비밀번호 재설정 이메일 발송 요청 처리
        // @RequestBody Map<String, String> request : 요청 본문에서 JSON 데이터를 Map으로 매핑
        // HttpServletRequest httpRequest : 클라이언트 IP, User-Agent 등 요청 메타데이터 접근용
        String irtToken = request.get("irtToken");
        // String irtToken : 요청에서 IRT 토큰 추출
        // request.get("irtToken") : JSON 요청에서 "irtToken" 키의 값을 가져옴  
        
        if (irtToken == null || irtToken.trim().isEmpty()) {
            // IRT 토큰이 없거나 빈 문자열인 경우
            logger.error("IRT token is missing in the request");
            // 에러 로그 기록
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "IRT token is required"));
            // 400 Bad Request 응답 반환
        } // if 끝
        

        String clientIp = getClientIpAddressFromEmailController(httpRequest);
        // IP 주소 추출
        String userAgent = httpRequest.getHeader("User-Agent");
        // 클라이언트 User-Agent
        
        // ========== 주요 처리 로직 ==========

        // try-catch 블록으로 예외 처리
        try {
            // 1. IP 기반 레이트 리밋 확인 (AccountRecoveryRateLimiter 직접 사용)
            if (!rateLimiter.isAllowedForFindPassword(clientIp, userAgent)) {
                // 레이트 리밋 초과의 경우
                long waitTime = rateLimiter.getRemainingWaitTime(clientIp, "find_pw");
                // 남은 대기 시간 조회
                Long retryAfter = waitTime > 0 ? waitTime : 60L;
                // 최소 60초로 설정
                logger.warn("Rate limit exceeded for IP: {}, User-Agent: {}. Retry after {} seconds", 
                        clientIp, userAgent, retryAfter);
                // 경고 로그 기록
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", String.valueOf(retryAfter))
                        .body(Map.of("error", "Rate limit exceeded", "retryAfter", retryAfter));
                // 429 Too Many Requests 응답 반환
            } // if 끝
            
            // 2. IRT 토큰 검증 (기존 로직 유지)
            Map<String, Object> irtData = redisUtil.getIRTData(irtToken);
            if (irtData == null) {
                // IRT 토큰이 없거나 만료된 경우
                rateLimiter.recordFailure(clientIp, "find_pw");
                // 실패 기록
                logger.warn("Invalid or expired IRT token from IP: {}, User-Agent: {}", 
                        clientIp, userAgent);
                // 경고 로그 기록
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "SESSION_EXPIRED"));
                // 401 Unauthorized 응답 반환
            } // if 끝
            
            String email = (String) irtData.get("email"); // 사용자 이메일
            String irtLock = (String) irtData.get("lock"); // 세션 락
            
            // 3. 세션 락 검증 (기존 로직 유지)
            if (!redisUtil.isLockValid(email, irtLock)) {
                // 세션 락이 유효하지 않은 경우
                logger.warn("Session lock mismatch for email: {}, IP: {}, User-Agent: {}", 
                        email, clientIp, userAgent);
                // 경고 로그 기록
                rateLimiter.recordFailure(clientIp, "find_pw");
                // 실패 기록
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "SESSION_REPLACED"));
                // 409 Conflict 응답 반환
            } // if 끝
            
            // 4. 인증 코드 생성 및 저장 (IP 기반)
            String authCode = authCodeGenerator.generateAuthCode();
            // 인증 코드 생성
            redisUtil.savePasswordResetCode(clientIp, email, authCode, irtData);
            // Redis에 인증 코드 저장
            
            // 5. 이메일 발송
            sendPasswordResetEmailFromEmailController(email, authCode);
            logger.info("Password reset email sent to {} for IP: {}, User-Agent: {}", 
                    email, clientIp, userAgent);
            
            // 6. 성공 기록 (IP 기반)
            rateLimiter.recordSuccess(clientIp, "find_pw");
            logger.info("Password reset request successful for IP: {}, User-Agent: {}", 
                    clientIp, userAgent);
            
            // 7. 성공 응답 반환 (최적화된 불변 Map 사용)
            return ResponseEntity.ok(ApiResponse.success(
                "비밀번호 재설정 인증 코드가 발송되었습니다.", 
                Map.of(
                    "maskedEmail", maskEmail(email),
                    "expiryMinutes", 5,
                    "sentAt", java.time.Instant.now().toString()
                )
            ));
            // 표준 ApiResponse 형식으로 응답 반환
            
        } catch (Exception e) {
            // 예외 처리
            rateLimiter.recordFailure(clientIp, "find_pw"); // 예외 발생 시도 실패로 기록
            logger.error("Error processing password reset request for IP: {}, User-Agent: {}. Error: {}", 
                    clientIp, userAgent, e.getMessage());
            // 에러 로그 기록
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
            // 표준 ApiResponse 형식으로 에러 응답 반환
        } // try-catch 끝
    } // sendResetEmail 끝

    // ========== PasswordResetEmailController (작성자: 성태준)에서 추가된 보조 메서드 ==========
    private String getClientIpAddressFromEmailController(HttpServletRequest request) {
        // getClientIpAddress(...) : 클라이언트 IP 주소 추출
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        // X-Forwarded-For 헤더에서 IP 주소 추출
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // xForwardedFor 값이 null이 아니고, 빈 문자열이 아니고, "unknown"이 아닌 경우
            logger.info("Client IP (X-Forwarded-For): {}", xForwardedFor.split(",")[0].trim());
            // 첫 번째 IP 주소 로그 기록
            return xForwardedFor.split(",")[0].trim();
            // 첫 번째 IP 주소 반환
        } // if 끝
        
        String xRealIp = request.getHeader("X-Real-IP");
        // X-Real-IP 헤더에서 IP 주소 추출
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            // xRealIp 값이 null이 아니고, 빈 문자열이 아니고, "unknown"이 아닌 경우
            logger.info("Client IP (X-Real-IP): {}", xRealIp);
            // X-Real-IP 로그 기록
            return xRealIp;
            // X-Real-IP 반환
        }
        
        logger.info("Client IP (RemoteAddr): {}", request.getRemoteAddr());
        // RemoteAddr 로그 기록

        return request.getRemoteAddr();
        // RemoteAddr 반환
    } // getClientIpAddress 끝
    
    private void sendPasswordResetEmailFromEmailController(String email, String code) throws Exception {
        // sendPasswordResetEmail(...) : 비밀번호 재설정 이메일 발송
        String emailContent = emailTemplateUtils.createAuthCodeEmailTemplate("사용자", code, 5);
        // 이메일 템플릿 생성 (5분 유효)
        
        logger.debug("Sending password reset email to: {}", email);
        // 디버그 로그 기록

        emailService.sendMIMEMessage(
            "bluecrabacademy@gmail.com", 
            email, 
            "비밀번호 재설정 인증코드", 
            emailContent
        );
    } // sendPasswordResetEmail 끝

    // ========== PasswordResetEmailController (작성자: 성태준)에서 추가된 메서드 끝 ==========

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

    // ========== 인증 코드 검증 단계(작성자 : 성태준) ==========

    /* 보안 특징:
     * - IRT 토큰 검증 (세션 무결성 확인)
     * - 코드 만료 확인 (5분 TTL)
     * - 최대 5회 시도 제한
     * - 검증 성공 시 코드 즉시 무효화
     * - Replace-on-new 원칙 적용
     * 
     * 에러 처리:
     * - 코드 불일치: 남은 시도 횟수 반환
     * - 만료: 새로운 코드 요청 안내
     * - 차단: 최대 시도 횟수 초과
     * - 세션 오류: 처음부터 다시 시도
     */
    @PostMapping("/verify-code") // 인증 코드 검증 엔드포인트
    public ResponseEntity<PasswordResetCodeVerifyResponse> verifyCode(
            @Valid @RequestBody PasswordResetCodeVerifyRequest request,
            HttpServletRequest httpRequest) {
            // ResponseEntity<PasswordResetCodeVerifyResponse> : 다양한 응답 지원
            // verifyCode : 인증 코드 검증 요청 처리
            // @RequestBody PasswordResetCodeVerifyRequest request : 요청 본문에서 JSON 데이터를 DTO로 매핑
            // HttpServletRequest httpRequest : IP 추출용
        
        String userIp = RequestUtils.getClientIpAddress(httpRequest);
        // 클라이언트 IP 주소 추출
        // RequestUtils.getClientIpAddress(...) : IP 추출 유틸리티 메서드
        logger.info("코드 검증 요청 수신: userIp={}", userIp);
        // 요청 수신 로그 기록
        
        try {
            // 1. 레이트 리미팅 확인 
            if (!rateLimiter.isAllowedForFindPassword(userIp, "Mozilla/5.0")) {
                // 레이트 리밋 초과의 경우
                long waitTime = rateLimiter.getRemainingWaitTime(userIp, "verify_code");
                // 남은 대기 시간 조회
                logger.warn("코드 검증 레이트 리미트 초과: userIp={}", userIp);
                // 경고 로그 기록
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(PasswordResetCodeVerifyResponse.failure(0, waitTime));
                // 429 Too Many Requests 응답 반환
            } // if 끝
            
            // 2. 서비스 레이어 호출
            PasswordResetCodeVerifyResponse response = passwordResetService.verifyCode(request, userIp);
            
            // 3. 레이트 리미터 업데이트
            if (response.isSuccess()) {
                // 검증 성공 시
                rateLimiter.recordSuccess(userIp, "verify_code");
                // 성공 기록
            } else {
                // 검증 실패 시
                rateLimiter.recordFailure(userIp, "verify_code");
                // 실패 기록
            }
            
            // 4. 응답 반환
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            
            // 특수한 경우의 상태 코드 처리
            if ("SESSION_ERROR".equals(response.getStatus())) {
                // 세션 오류의 경우 (IRT 토큰 문제)
                status = HttpStatus.UNAUTHORIZED;
                // 401 Unauthorized
            } else if ("BLOCKED".equals(response.getStatus())) {
                // 차단된 경우 (최대 시도 횟수 초과)
                status = HttpStatus.LOCKED;
                // 423 Locked
            } else if ("EXPIRED".equals(response.getStatus())) {
                // 코드 만료의 경우
                status = HttpStatus.GONE;
                // 410 Gone
            } // if-else 끝

            logger.info("코드 검증 응답: userIp={}, success={}, status={}", 
                        userIp, response.isSuccess(), response.getStatus());
            // 응답 로그 기록
            
            return ResponseEntity.status(status).body(response);
            // 최종 응답 반환
            
        } catch (Exception e) {
            // 예외 처리
                        
            rateLimiter.recordFailure(userIp, "verify_code");
            // 레이트 리미터 업데이트 (오류 시에도 실패로 기록)
            logger.error("코드 검증 중 예상하지 못한 오류 발생: userIp={}", userIp, e);
            // 에러 로그 기록
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PasswordResetCodeVerifyResponse.failure(0, 0L));
            // 500 Internal Server Error 응답 반환
        }
    } // verifyCode 끝

    // ========== 인증 코드 검증 단계(작성자 : 성태준) 끝 ==========

    /**
     * 4단계: 비밀번호 변경 처리
     * RT 토큰을 검증한 후 새로운 비밀번호로 변경
     * 
     * 보안 특징:
     * - RT 토큰 단일 사용 보장 (GETDEL 사용)
     * - 락 토큰 일치 확인 (Replace-on-new)
     * - BCrypt로 비밀번호 해싱
     * - 사용된 토큰들 정리
     *
     * @param resetToken RT(Reset Token) - 헤더에서 전달
     * @param request 새 비밀번호 데이터
     * @return ApiResponse<Object> 변경 결과
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            @RequestHeader("X-RT") String resetToken,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        try {
            logger.info("비밀번호 변경 요청 수신");
            
            // 서비스에서 비밀번호 변경 처리
            passwordResetService.changePassword(resetToken, request.getNewPassword());
            
            logger.info("비밀번호 변경 성공");
            return ResponseEntity.ok(ApiResponse.success(
                "비밀번호가 성공적으로 변경되었습니다.",
                Map.of("ok", true)
            ));
            
        } catch (IllegalArgumentException e) {
            logger.warn("비밀번호 변경 실패: {}", e.getMessage());
            
            if (e.getMessage().contains("SESSION_EXPIRED")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("세션이 만료되었습니다. 처음부터 다시 시도해주세요."));
            } else if (e.getMessage().contains("SESSION_REPLACED")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.failure("다른 곳에서 비밀번호 재설정을 진행중입니다."));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("잘못된 요청입니다."));
            }
            
        } catch (Exception e) {
            logger.error("비밀번호 변경 중 예상하지 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
    }
    
    /**
     * 이메일 주소 마스킹 처리
     * 보안상 이메일 주소를 부분적으로 숨김 처리
     *
     * @param email 원본 이메일 주소
     * @return 마스킹된 이메일 주소 (예: te***@gmail.com)
     */
    private String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "***@***.***";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***@***.***";
        }
        
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        
        // 로컬 부분 마스킹 (앞 2글자 보여주고 나머지는 *)
        String maskedLocal;
        if (localPart.length() <= 2) {
            maskedLocal = "*".repeat(localPart.length());
        } else {
            maskedLocal = localPart.substring(0, 2) + "*".repeat(localPart.length() - 2);
        }
        
        return maskedLocal + domainPart;
    }
}