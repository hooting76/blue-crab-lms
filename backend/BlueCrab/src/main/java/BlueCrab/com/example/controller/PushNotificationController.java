package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.BatchSendResponse;
import BlueCrab.com.example.dto.PushNotificationBatchRequest;
import BlueCrab.com.example.dto.PushNotificationRequest;
import BlueCrab.com.example.dto.TopicPushNotificationRequest;
import BlueCrab.com.example.service.FirebasePushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/push")
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
@Validated
public class PushNotificationController {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationController.class);

    @Autowired
    private FirebasePushService pushService;

    /**
     * VAPID Public Key 반환 (프론트엔드에서 사용)
     */
    @GetMapping("/vapid-key")
    public ResponseEntity<ApiResponse<String>> getVapidPublicKey() {
        String publicKey = pushService.getVapidPublicKey();
        return ResponseEntity.ok(ApiResponse.success("VAPID public key retrieved", publicKey));
    }

    /**
     * 특정 토큰으로 푸시 알림 전송
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendPushNotification(
            @Valid @RequestBody PushNotificationRequest request) {
        try {
            String response = pushService.sendPushNotification(
                    request.getToken(),
                    request.getTitle(),
                    request.getBody(),
                    request.getData());
            return ResponseEntity.ok(ApiResponse.success("Push notification sent successfully", response));
        } catch (Exception e) {
            log.error("Failed to send push notification", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Failed to send push notification: " + e.getMessage()));
        }
    }

    /**
     * 토픽으로 푸시 알림 전송
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/send-to-topic")
    public ResponseEntity<ApiResponse<String>> sendPushNotificationToTopic(
            @Valid @RequestBody TopicPushNotificationRequest request) {
        try {
            String response = pushService.sendPushNotificationToTopic(
                    request.getTopic(),
                    request.getTitle(),
                    request.getBody(),
                    request.getData());
            return ResponseEntity.ok(ApiResponse.success("Push notification sent to topic successfully", response));
        } catch (Exception e) {
            log.error("Failed to send push notification to topic {}", request.getTopic(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Failed to send push notification to topic: " + e.getMessage()));
        }
    }

    /**
     * 🧪 Data-only 방식 테스트 (관리자 전용)
     * 중복 알림 방지 확인용 - Notification 페이로드 없이 Data만 전송
     *
     * ⚠️ 제한사항:
     * - 앱 실행 중일 때만 알림 표시
     * - 앱 종료 시 알림 전달 안됨
     * - 재부팅 후 알림 전달 안됨
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/send-data-only")
    public ResponseEntity<ApiResponse<String>> sendDataOnlyNotification(
            @Valid @RequestBody PushNotificationRequest request) {
        try {
            String response = pushService.sendDataOnlyNotification(
                    request.getToken(),
                    request.getTitle(),
                    request.getBody(),
                    request.getData());
            return ResponseEntity.ok(ApiResponse.success(
                "✅ Data-only notification sent (중복 방지됨)",
                response
            ));
        } catch (Exception e) {
            log.error("Failed to send data-only notification", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Failed to send data-only notification: " + e.getMessage()));
        }
    }

    /**
     * 🚀 Data-only 방식 배치 전송 (관리자 전용)
     * 여러 토큰에 동시에 Data-only 메시지를 전송합니다.
     *
     * ⚠️ 제한사항:
     * - 최대 500개 토큰까지 전송 가능
     * - 앱 실행 중일 때만 알림 표시
     * - 앱 종료 시 알림 전달 안됨
     * - 재부팅 후 알림 전달 안됨
     *
     * 📊 응답 형식:
     * {
     *   "successCount": 3,
     *   "failureCount": 1,
     *   "responses": [
     *     {"token": "...", "success": true, "messageId": "..."},
     *     {"token": "...", "success": false, "error": "..."}
     *   ]
     * }
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/send-data-only-batch")
    public ResponseEntity<ApiResponse<BatchSendResponse>> sendDataOnlyBatch(
            @Valid @RequestBody PushNotificationBatchRequest request) {
        try {
            BatchSendResponse response = pushService.sendDataOnlyNotificationBatch(
                    request.getTokens(),
                    request.getTitle(),
                    request.getBody(),
                    request.getData());

            String message = String.format(
                "✅ Batch notification sent: %d success, %d failed out of %d total",
                response.getSuccessCount(),
                response.getFailureCount(),
                response.getTotalCount()
            );

            return ResponseEntity.ok(ApiResponse.success(message, response));
        } catch (Exception e) {
            log.error("Failed to send data-only batch notification", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Failed to send data-only batch notification: " + e.getMessage()));
        }
    }
}