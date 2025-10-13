package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.AdminRefreshRequest;
import BlueCrab.com.example.dto.AuthResponse;
import BlueCrab.com.example.exception.UnauthorizedException;
import BlueCrab.com.example.service.AdminService;
import BlueCrab.com.example.util.AdminAuthResponseBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 관리자 토큰 재발급 컨트롤러
 */
@RestController
@RequestMapping("/api/admin/auth")
@Slf4j
public class AdminAuthTokenController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminAuthResponseBuilder adminAuthResponseBuilder;

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshAdminToken(@Valid @RequestBody AdminRefreshRequest request) {
        try {
            AdminService.AdminTokenRefreshResult result = adminService.refreshAdminToken(request.getRefreshToken());
            return adminAuthResponseBuilder.buildSuccessResponse(result.getTokenPair(), result.getAdmin());
        } catch (IllegalArgumentException ex) {
            log.warn("Admin refresh token validation failed: {}", ex.getMessage());
            return adminAuthResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (UnauthorizedException ex) {
            log.warn("Admin refresh token unauthorized: {}", ex.getMessage());
            return adminAuthResponseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            log.error("Unexpected error during admin refresh token processing", ex);
            return adminAuthResponseBuilder.buildErrorResponse(
                "토큰 재발급 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
