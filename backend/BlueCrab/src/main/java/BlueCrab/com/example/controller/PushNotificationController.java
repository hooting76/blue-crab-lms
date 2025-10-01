package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
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
}