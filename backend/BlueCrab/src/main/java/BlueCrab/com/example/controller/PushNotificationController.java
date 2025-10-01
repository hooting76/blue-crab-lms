package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.service.FirebasePushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push")
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class PushNotificationController {

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
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendPushNotification(
            @RequestParam String token,
            @RequestParam String title,
            @RequestParam String body) {
        
        try {
            String response = pushService.sendPushNotification(token, title, body);
            return ResponseEntity.ok(ApiResponse.success("Push notification sent successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Failed to send push notification: " + e.getMessage()));
        }
    }

    /**
     * 토픽으로 푸시 알림 전송
     */
    @PostMapping("/send-to-topic")
    public ResponseEntity<ApiResponse<String>> sendPushNotificationToTopic(
            @RequestParam String topic,
            @RequestParam String title,
            @RequestParam String body) {
        
        try {
            String response = pushService.sendPushNotificationToTopic(topic, title, body);
            return ResponseEntity.ok(ApiResponse.success("Push notification sent to topic successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Failed to send push notification to topic: " + e.getMessage()));
        }
    }
}