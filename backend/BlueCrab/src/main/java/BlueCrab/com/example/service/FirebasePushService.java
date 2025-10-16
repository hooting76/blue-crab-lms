package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.BatchSendResponse;
import BlueCrab.com.example.dto.TokenSendResult;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * Data-only 방식 배치 푸시 알림 전송 (중복 방지)
     * 여러 토큰에 동시에 Data-only 메시지를 전송합니다.
     *
     * @param tokens FCM 토큰 목록 (최대 500개)
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터
     * @return 배치 전송 결과
     */
    public BatchSendResponse sendDataOnlyNotificationBatch(
            List<String> tokens, String title, String body, Map<String, String> data) {

        try {
            // 중복 토큰 제거
            List<String> uniqueTokens = tokens.stream()
                    .distinct()
                    .collect(Collectors.toList());

            log.info("📤 Sending data-only batch notification to {} tokens (original: {})",
                    uniqueTokens.size(), tokens.size());

            // Data 페이로드 구성
            java.util.HashMap<String, String> messageData = new java.util.HashMap<>();
            messageData.put("title", title);
            messageData.put("body", body);
            messageData.put("type", "data-only-batch");
            messageData.put("timestamp", String.valueOf(System.currentTimeMillis()));

            if (data != null && !data.isEmpty()) {
                messageData.putAll(data);
            }

            // MulticastMessage 생성 (Data-only, Notification 없음)
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(uniqueTokens)
                    .putAllData(messageData)  // Data-only
                    .build();

            // Firebase 배치 전송
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            // 결과 분석
            List<TokenSendResult> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (int i = 0; i < response.getResponses().size(); i++) {
                SendResponse sendResponse = response.getResponses().get(i);
                String token = uniqueTokens.get(i);

                if (sendResponse.isSuccessful()) {
                    results.add(TokenSendResult.success(token, sendResponse.getMessageId()));
                    successCount++;
                } else {
                    String errorMsg = sendResponse.getException() != null
                            ? sendResponse.getException().getMessage()
                            : "Unknown error";
                    results.add(TokenSendResult.failure(token, errorMsg));
                    failureCount++;
                    log.warn("Failed to send to token {}: {}",
                            maskToken(token), errorMsg);
                }
            }

            log.info("✅ Batch notification sent: {} success, {} failed out of {} total",
                    successCount, failureCount, uniqueTokens.size());

            return new BatchSendResponse(successCount, failureCount, results);

        } catch (Exception e) {
            log.error("❌ Failed to send data-only batch notification", e);
            throw new RuntimeException("Data-only batch notification failed", e);
        }
    }

    /**
     * 토큰 마스킹 (로그 보안용)
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }

    /**
     * VAPID Public Key 반환 (프론트엔드에서 사용)
     */
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }
}