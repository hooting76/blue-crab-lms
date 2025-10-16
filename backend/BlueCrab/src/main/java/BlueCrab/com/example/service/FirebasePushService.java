package BlueCrab.com.example.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FirebasePushService {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushService.class);

    @Value("${firebase.vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${firebase.vapid.private-key:}")
    private String vapidPrivateKey;

    /**
     * 특정 토큰으로 푸시 알림 전송
     */
    public String sendPushNotification(String token, String title, String body) {
    return sendPushNotification(token, title, body, null);
    }

    public String sendPushNotification(String token, String title, String body, Map<String, String> data) {
        try {
        Message.Builder builder = Message.builder()
            .setToken(token)
            .setWebpushConfig(buildWebpushConfig(title, body));

        if (data != null && !data.isEmpty()) {
        builder.putAllData(data);
        }

        Message message = builder.build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent push notification: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to send push notification", e);
            throw new RuntimeException("Push notification failed", e);
        }
    }

    /**
     * 토픽으로 푸시 알림 전송
     */
    public String sendPushNotificationToTopic(String topic, String title, String body) {
    return sendPushNotificationToTopic(topic, title, body, null);
    }

    public String sendPushNotificationToTopic(String topic, String title, String body, Map<String, String> data) {
        try {
        Message.Builder builder = Message.builder()
            .setTopic(topic)
            .setWebpushConfig(buildWebpushConfig(title, body));

        if (data != null && !data.isEmpty()) {
        builder.putAllData(data);
        }

        Message message = builder.build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent push notification to topic {}: {}", topic, response);
            return response;
        } catch (Exception e) {
            log.error("Failed to send push notification to topic: {}", topic, e);
            throw new RuntimeException("Push notification to topic failed", e);
        }
    }

    private WebpushConfig buildWebpushConfig(String title, String body) {
    WebpushNotification notification = WebpushNotification.builder()
        .setTitle(title)
        .setBody(body)
        .build();

    return WebpushConfig.builder()
        .setNotification(notification)
        .build();
    }

    /**
     * Data-only 방식 푸시 알림 전송 (중복 방지 테스트용)
     * Notification 페이로드 없이 Data만 전송
     */
    public String sendDataOnlyNotification(String token, String title, String body, Map<String, String> data) {
        try {
            // Data 페이로드에 title, body 포함
            java.util.HashMap<String, String> messageData = new java.util.HashMap<>();
            messageData.put("title", title);
            messageData.put("body", body);
            messageData.put("type", "data-only");
            messageData.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            if (data != null && !data.isEmpty()) {
                messageData.putAll(data);
            }

            // ✅ Data만 전송 (Notification 페이로드 없음)
            Message message = Message.builder()
                .setToken(token)
                .putAllData(messageData)  // Data-only
                .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ Data-only notification sent: {}", response);
            return response;
        } catch (Exception e) {
            log.error("❌ Failed to send data-only notification", e);
            throw new RuntimeException("Data-only notification failed", e);
        }
    }

    /**
     * VAPID Public Key 반환 (프론트엔드에서 사용)
     */
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }
}