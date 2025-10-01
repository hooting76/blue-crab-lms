package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.*;
import BlueCrab.com.example.service.FcmTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

/**
 * FCM 토큰 관리 및 알림 전송 컨트롤러
 */
@RestController
@RequestMapping("/api/fcm")
public class FcmTokenController {
    @Autowired
    private FcmTokenService fcmTokenService;

    /**
     * FCM 토큰 등록 (충돌 감지)
     * POST /api/fcm/register
     */
    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FcmRegisterResponse>> register(
            @Valid @RequestBody FcmRegisterRequest request,
            Authentication authentication) {

        FcmRegisterResponse result = fcmTokenService.register(authentication, request);
        ApiResponse<FcmRegisterResponse> response = ApiResponse.success(result.getMessage(), result);
        return ResponseEntity.ok(response);
    }

    /**
     * FCM 토큰 강제 변경
     * POST /api/fcm/register/force
     */
    @PostMapping("/register/force")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FcmRegisterResponse>> registerForce(
            @Valid @RequestBody FcmRegisterRequest request,
            Authentication authentication) {

        FcmRegisterResponse result = fcmTokenService.registerForce(authentication, request);
        ApiResponse<FcmRegisterResponse> response = ApiResponse.success(result.getMessage(), result);
        return ResponseEntity.ok(response);
    }

    /**
     * FCM 토큰 삭제 (로그아웃용)
     * DELETE /api/fcm/unregister
     */
    @DeleteMapping("/unregister")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> unregister(
            @Valid @RequestBody FcmUnregisterRequest request,
            Authentication authentication) {

        fcmTokenService.unregister(authentication, request);
        ApiResponse<String> response = ApiResponse.success("로그아웃되었습니다.", "success");
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자에게 알림 전송 (관리자용)
     * POST /api/fcm/send
     */
    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FcmSendResponse>> send(
            @Valid @RequestBody FcmSendRequest request) {

        FcmSendResponse result = fcmTokenService.sendNotification(request);
        ApiResponse<FcmSendResponse> response = ApiResponse.success("알림 전송이 완료되었습니다.", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 여러 사용자에게 일괄 알림 전송 (관리자용)
     * POST /api/fcm/send/batch
     */
    @PostMapping("/send/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FcmBatchSendResponse>> sendBatch(
            @Valid @RequestBody FcmBatchSendRequest request) {

        FcmBatchSendResponse result = fcmTokenService.sendBatchNotification(request);
        ApiResponse<FcmBatchSendResponse> response = ApiResponse.success("일괄 알림 전송이 완료되었습니다.", result);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 사용자에게 브로드캐스트 알림 전송 (관리자용)
     * POST /api/fcm/send/broadcast
     */
    @PostMapping("/send/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FcmBroadcastResponse>> sendBroadcast(
            @Valid @RequestBody FcmBroadcastRequest request) {

        FcmBroadcastResponse result = fcmTokenService.sendBroadcast(request);
        ApiResponse<FcmBroadcastResponse> response = ApiResponse.success("브로드캐스트 알림 전송이 완료되었습니다.", result);
        return ResponseEntity.ok(response);
    }

    /**
     * FCM 토큰 통계 조회 (관리자용)
     * GET /api/fcm/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FcmStatsResponse>> getStats() {

        FcmStatsResponse result = fcmTokenService.getStats();
        ApiResponse<FcmStatsResponse> response = ApiResponse.success("통계 조회가 완료되었습니다.", result);
        return ResponseEntity.ok(response);
    }
}
