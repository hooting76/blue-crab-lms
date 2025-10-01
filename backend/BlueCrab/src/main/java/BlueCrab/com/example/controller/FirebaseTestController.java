package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.service.FirebasePushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FirebaseTestController {

    @Autowired
    private FirebasePushService pushService;

    /**
     * Firebase 초기화 상태 확인 (인증 불필요)
     */
    @GetMapping("/firebase-status")
    public ResponseEntity<ApiResponse<String>> checkFirebaseStatus() {
        try {
            String publicKey = pushService.getVapidPublicKey();
            if (publicKey != null && !publicKey.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("Firebase initialized successfully", 
                    "VAPID Public Key: " + publicKey));
            } else {
                return ResponseEntity.ok(ApiResponse.failure("Firebase not properly initialized"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.failure("Firebase initialization failed: " + e.getMessage()));
        }
    }

    /**
     * VAPID 공개키 가져오기 (인증 불필요)
     */
    @GetMapping("/vapid-key")
    public ResponseEntity<ApiResponse<String>> getVapidKey() {
        try {
            String publicKey = pushService.getVapidPublicKey();
            return ResponseEntity.ok(ApiResponse.success("VAPID public key retrieved", publicKey));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Failed to get VAPID key: " + e.getMessage()));
        }
    }

    /**
     * 테스트용 푸시 알림 전송 (간단한 인증)
     */
    @PostMapping("/send-test-push")
    public ResponseEntity<ApiResponse<String>> sendTestPush(
            @RequestParam String token,
            @RequestParam(defaultValue = "테스트 알림") String title,
            @RequestParam(defaultValue = "Firebase 푸시 알림 테스트입니다") String body,
            @RequestParam(defaultValue = "test123") String testKey) {
        
        // 간단한 테스트 키 검증
        if (!"test123".equals(testKey)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Invalid test key"));
        }
        
        try {
            String response = pushService.sendPushNotification(token, title, body);
            return ResponseEntity.ok(ApiResponse.success("Test push notification sent successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Failed to send test push notification: " + e.getMessage()));
        }
    }

    /**
     * 토픽 테스트 푸시 전송
     */
    @PostMapping("/send-topic-test")
    public ResponseEntity<ApiResponse<String>> sendTopicTest(
            @RequestParam(defaultValue = "test-topic") String topic,
            @RequestParam(defaultValue = "전체 공지") String title,
            @RequestParam(defaultValue = "테스트 토픽 알림입니다") String body,
            @RequestParam(defaultValue = "test123") String testKey) {
        
        if (!"test123".equals(testKey)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Invalid test key"));
        }
        
        try {
            String response = pushService.sendPushNotificationToTopic(topic, title, body);
            return ResponseEntity.ok(ApiResponse.success("Test topic push sent successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Failed to send test topic push: " + e.getMessage()));
        }
    }
}