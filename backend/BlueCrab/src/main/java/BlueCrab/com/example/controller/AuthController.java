package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.LoginRequest;
import BlueCrab.com.example.dto.LoginResponse;
import BlueCrab.com.example.dto.LogoutRequest;
import BlueCrab.com.example.dto.RefreshTokenRequest;
import BlueCrab.com.example.service.AuthService;
import BlueCrab.com.example.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

/**
 * 인증 및 인가 처리를 위한 REST API 컨트롤러
 * JWT 기반 로그인, 로그아웃, 토큰 갱신 기능을 제공
 * 
 * 모든 응답은 ApiResponse<T> 형식으로 통일되어 반환됩니다:
 * - success: 요청 성공/실패 여부
 * - message: 사용자에게 표시할 메시지 (한국어)
 * - data: 실제 응답 데이터 (LoginResponse, Map 등)
 * - timestamp: 응답 생성 시간 (ISO-8601 형식)
 * 
 * 인증 실패나 예외 발생 시 GlobalExceptionHandler에서 일관된 형식으로 처리됩니다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * 사용자 로그인 처리
     * 사용자명(이메일)과 비밀번호를 검증하여 JWT 토큰을 발급
     * 
     * @param loginRequest 로그인 요청 정보 (사용자명, 비밀번호)
     * @return ApiResponse 형식의 JWT 액세스 토큰, 리프레시 토큰 및 사용자 정보
     * 
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "로그인에 성공했습니다.",
     *   "data": {
     *     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *     "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *     "tokenType": "Bearer",
     *     "expiresIn": 900,
     *     "user": {
     *       "userIdx": 1,
     *       "userEmail": "user@example.com",
     *       "userName": "홍길동",
     *       "email": "user@example.com"
     *     }
     *   },
     *   "timestamp": "2025-08-26T12:00:00Z"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                          HttpServletRequest request) {
        String username = loginRequest.getUsername();
        String clientIp = RequestUtils.getClientIpAddress(request);
        
        logger.debug("로그인 요청 수신 - 사용자: {}, IP: {}, 타임스탬프: {}", 
            username, clientIp, System.currentTimeMillis());
        logger.info("Login attempt: username={}, clientIp={}", username, clientIp);
        
        try {
            LoginResponse response = authService.authenticate(loginRequest);
            
            // 성공 로그 (민감 정보 제외)
            if (response != null && response.getUser() != null) {
                logger.info("Login successful: username={}, userId={}, clientIp={}", 
                    username, response.getUser().getId(), clientIp);
            }
            
            ApiResponse<LoginResponse> apiResponse = ApiResponse.success("로그인에 성공했습니다.", response);
            return ResponseEntity.ok(apiResponse);
            
        } catch (Exception e) {
            // 실패 로그 (예외 정보는 GlobalExceptionHandler에서 처리)
            logger.warn("Login failed: username={}, clientIp={}, reason={}", 
                username, clientIp, e.getMessage());
            throw e; // GlobalExceptionHandler가 처리하도록 재던짐
        }
    }

    /**
     * JWT 토큰 갱신 처리
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급
     * 
     * @param refreshTokenRequest 리프레시 토큰 요청 정보
     * @return ApiResponse 형식의 새로운 JWT 액세스 토큰과 리프레시 토큰
     * 
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "토큰이 성공적으로 갱신되었습니다.",
     *   "data": {
     *     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *     "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *     "tokenType": "Bearer",
     *     "expiresIn": 900,
     *     "user": { ... }
     *   },
     *   "timestamp": "2025-08-26T12:00:00Z"
     * }
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        LoginResponse response = authService.refreshToken(refreshTokenRequest.getRefreshToken());
        ApiResponse<LoginResponse> apiResponse = ApiResponse.success("토큰이 성공적으로 갱신되었습니다.", response);
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * 사용자 로그아웃 처리
     * JWT는 stateless이므로 서버에서 토큰을 무효화할 수 없음
     * 클라이언트에서 토큰을 삭제하도록 안내
     * 
     * @param request HTTP 요청 정보
     * @return 로그아웃 성공 메시지 및 토큰 삭제 안내
     * 
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "로그아웃이 성공적으로 처리되었습니다.",
     *   "data": {
     *     "message": "Logged out successfully",
     *     "instruction": "Please remove the tokens from client storage"
     *   },
     *   "timestamp": "2025-08-26T12:00:00Z"
     * }
     */

    /**
     * JWT 토큰 유효성 검증
     * JWT 인증 필터에서 이미 토큰 검증이 완료된 상태
     * 이 엔드포인트에 도달했다면 토큰이 유효함
     * 
     * @param request HTTP 요청 정보
     * @return 토큰 유효성 및 메시지
     * 
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "토큰이 유효합니다.",
     *   "data": {
     *     "valid": true,
     *     "message": "Token is valid"
     *   },
     *   "timestamp": "2025-08-26T12:00:00Z"
     * }
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(HttpServletRequest request) {
        Map<String, Object> data = Map.of(
            "valid", true,
            "message", "Token is valid"
        );
        ApiResponse<Map<String, Object>> response = ApiResponse.success("토큰이 유효합니다.", data);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 로그아웃 처리
     * AccessToken과 RefreshToken을 모두 무효화하여 완전한 로그아웃 처리
     * 
     * @param logoutRequest 로그아웃 요청 정보 (RefreshToken 포함)
     * @param request HTTP 요청 정보
     * @return 로그아웃 처리 결과
     * 
     * 요청 예시:
     * Headers: Authorization: Bearer <access-token>
     * Body: { "refreshToken": "<refresh-token>" }
     * 
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "로그아웃이 성공적으로 처리되었습니다.",
     *   "data": {
     *     "status": "SUCCESS",
     *     "message": "Logged out successfully",
     *     "instruction": "Please remove the tokens from client storage",
     *     "logoutTime": "2025-09-05T12:00:00Z",
     *     "username": "user@example.com",
     *     "tokensInvalidated": {
     *       "accessToken": true,
     *       "refreshToken": true
     *     }
     *   },
     *   "timestamp": "2025-09-05T12:00:00Z"
     * }
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(
            @RequestBody LogoutRequest logoutRequest,
            HttpServletRequest request) {
        
        String clientIp = RequestUtils.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        try {
            // 1. Authorization 헤더에서 AccessToken 추출
            String accessToken = extractTokenFromRequest(request);
            
            // 2. 요청 바디에서 RefreshToken 추출 (필수)
            String refreshToken = null;
            if (logoutRequest == null || logoutRequest.getRefreshToken() == null || 
                logoutRequest.getRefreshToken().trim().isEmpty()) {
                
                logger.warn("로그아웃 요청에 RefreshToken이 없음 - IP: {}, UserAgent: {}", clientIp, userAgent);
                
                Map<String, Object> errorData = Map.of(
                    "status", "REFRESH_TOKEN_MISSING",
                    "errorCode", "LOGOUT_004",
                    "message", "RefreshToken is required for complete logout",
                    "instruction", "Please provide both accessToken and refreshToken",
                    "details", "RefreshToken not found in request body"
                );
                
                ApiResponse<Map<String, Object>> response = ApiResponse.failure("완전한 로그아웃을 위해 RefreshToken이 필요합니다.", errorData);
                return ResponseEntity.badRequest().body(response);
            }
            
            refreshToken = logoutRequest.getRefreshToken();
            
            // 3. AccessToken 검증 (필수)
            if (accessToken == null || accessToken.trim().isEmpty()) {
                logger.warn("로그아웃 요청에 AccessToken이 없음 - IP: {}, UserAgent: {}", clientIp, userAgent);
                
                Map<String, Object> errorData = Map.of(
                    "status", "TOKEN_MISSING",
                    "errorCode", "LOGOUT_001",
                    "message", "Access token not found in Authorization header",
                    "instruction", "Please provide valid authorization token",
                    "details", "No Bearer token found in Authorization header"
                );
                
                ApiResponse<Map<String, Object>> response = ApiResponse.failure("로그아웃 처리 중 오류가 발생했습니다.", errorData);
                return ResponseEntity.badRequest().body(response);
            }
            
            String username = null;
            try {
                // 4. AccessToken에서 사용자명 추출 및 검증
                username = authService.extractUsernameFromToken(accessToken);
                logger.info("로그아웃 요청 - 사용자: {}, IP: {}, UserAgent: {}, RefreshToken 제공: 예", 
                           username, clientIp, userAgent);
                
                // 5. AuthService에서 토큰 무효화 처리 (둘 다 필수)
                authService.logout(accessToken, refreshToken, username);
                
            } catch (Exception tokenException) {
                // AccessToken이 유효하지 않은 경우
                logger.warn("로그아웃 시 AccessToken 검증 실패 - IP: {}, Error: {}", clientIp, tokenException.getMessage());
                
                Map<String, Object> errorData = Map.of(
                    "status", "TOKEN_INVALID",
                    "errorCode", "LOGOUT_002",
                    "message", "Invalid or expired access token",
                    "instruction", "Please login again to get a valid token",
                    "details", tokenException.getMessage()
                );
                
                ApiResponse<Map<String, Object>> response = ApiResponse.failure("유효하지 않은 토큰입니다.", errorData);
                return ResponseEntity.status(401).body(response);
            }
            
            // 6. 로그아웃 성공 응답
            Map<String, Object> successData = Map.of(
                "status", "SUCCESS",
                "message", "Logged out successfully",
                "instruction", "Please remove the tokens from client storage",
                "logoutTime", java.time.Instant.now().toString(),
                "username", username != null ? username : "unknown",
                "tokensInvalidated", Map.of(
                    "accessToken", true,
                    "refreshToken", true // 이제 항상 true (필수이므로)
                )
            );
            
            logger.info("로그아웃 처리 완료 - 사용자: {}, IP: {}, 무효화된 토큰: Access=true, Refresh=true", 
                       username, clientIp);
            
            ApiResponse<Map<String, Object>> response = ApiResponse.success("로그아웃이 성공적으로 처리되었습니다.", successData);
            return ResponseEntity.ok(response);
            
        } catch (Exception systemException) {
            // 시스템 오류 발생 시
            logger.error("로그아웃 처리 중 시스템 오류 발생 - IP: {}, Error: {}", clientIp, systemException.getMessage(), systemException);
            
            Map<String, Object> errorData = Map.of(
                "status", "SYSTEM_ERROR",
                "errorCode", "LOGOUT_003",
                "message", "System error occurred during logout",
                "instruction", "Please try again later or contact support",
                "details", systemException.getMessage()
            );
            
            ApiResponse<Map<String, Object>> response = ApiResponse.failure("시스템 오류가 발생했습니다.", errorData);
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     * @param request HTTP 요청
     * @return JWT 토큰 (Bearer 제거된 순수 토큰)
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
