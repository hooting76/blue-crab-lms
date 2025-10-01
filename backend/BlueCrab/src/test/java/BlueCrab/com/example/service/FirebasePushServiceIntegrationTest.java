package BlueCrab.com.example.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Firebase Admin SDK와 실제 FCM 환경을 검증하는 통합 테스트입니다.
 * <p>
 * 실행 전에 다음 환경 변수를 설정하면 실제 디바이스/브라우저로 알림을 전송합니다.
 * <ul>
 *   <li>{@code FIREBASE_CREDENTIALS_PATH} 또는 {@code FIREBASE_CREDENTIALS_JSON}</li>
 *   <li>{@code FIREBASE_DATABASE_URL} (선택)</li>
 *   <li>{@code FCM_TEST_TOKEN} (디바이스 토큰 검증용, 선택)</li>
 *   <li>{@code FCM_TEST_TOPIC} (토픽 전송 검증용, 선택)</li>
 * </ul>
 * 값이 비어 있으면 해당 테스트는 자동으로 건너뜁니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirebasePushServiceIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(FirebasePushServiceIntegrationTest.class);

	private static final String CREDENTIALS_PATH = System.getenv("FIREBASE_CREDENTIALS_PATH");
	private static final String CREDENTIALS_JSON = System.getenv("FIREBASE_CREDENTIALS_JSON");
	private static final String DATABASE_URL = System.getenv("FIREBASE_DATABASE_URL");
	private static final String TEST_TOKEN = System.getenv("FCM_TEST_TOKEN");
	private static final String TEST_TOPIC = System.getenv("FCM_TEST_TOPIC");

	private FirebaseApp firebaseApp;
	private boolean ownsFirebaseApp;
	private FirebasePushService firebasePushService;

	@BeforeAll
	void setUpFirebase() throws IOException {
		Assumptions.assumeTrue(hasCredentials(),
				() -> "Firebase 인증 정보가 설정되지 않아 통합 테스트를 건너뜁니다.");

		GoogleCredentials credentials = loadCredentials();
		firebaseApp = ensureFirebaseApp(credentials);
		firebasePushService = new FirebasePushService();

		log.info("Firebase integration test initialized (app={}, hasToken={}, hasTopic={})",
				firebaseApp.getName(),
				StringUtils.hasText(TEST_TOKEN),
				StringUtils.hasText(TEST_TOPIC));
	}

	@AfterAll
	void tearDownFirebase() {
		if (ownsFirebaseApp && firebaseApp != null) {
			firebaseApp.delete();
			log.info("FirebaseApp deleted after integration test");
		}
	}

	@DisplayName("디바이스 토큰으로 푸시 알림을 전송하면 메시지 ID를 반환한다")
	@Test
	void sendPushNotification_withValidToken_returnsMessageId() {
		Assumptions.assumeTrue(StringUtils.hasText(TEST_TOKEN),
				() -> "FCM_TEST_TOKEN 이 없어 토큰 전송 테스트를 건너뜁니다.");

	String messageId = firebasePushService.sendPushNotification(
		TEST_TOKEN,
		"[DEV] 블루크랩 테스트 알림",
		"FirebasePushServiceIntegrationTest 에서 발송된 메시지입니다.",
		Map.of("source", "integration-test", "audience", "admin-token")
	);

		assertNotNull(messageId, "Firebase Messaging 응답이 null 이면 안 됩니다");
		assertFalse(messageId.isBlank(), "Firebase Messaging 응답이 비어 있으면 안 됩니다");
		log.info("FCM push message id (token): {}", messageId);
	}

	@DisplayName("토픽으로 푸시 알림을 전송하면 메시지 ID를 반환한다")
	@Test
	void sendPushNotificationToTopic_withValidTopic_returnsMessageId() {
		Assumptions.assumeTrue(StringUtils.hasText(TEST_TOPIC),
				() -> "FCM_TEST_TOPIC 이 없어 토픽 전송 테스트를 건너뜁니다.");

	String messageId = firebasePushService.sendPushNotificationToTopic(
		TEST_TOPIC,
		"[DEV] 블루크랩 토픽 테스트",
		"FirebasePushServiceIntegrationTest 에서 토픽으로 발송된 메시지입니다.",
		Map.of("source", "integration-test", "audience", "topic")
	);

		assertNotNull(messageId, "Firebase Messaging 응답이 null 이면 안 됩니다");
		assertFalse(messageId.isBlank(), "Firebase Messaging 응답이 비어 있으면 안 됩니다");
		log.info("FCM push message id (topic): {}", messageId);
	}

	private boolean hasCredentials() {
		return StringUtils.hasText(CREDENTIALS_PATH) || StringUtils.hasText(CREDENTIALS_JSON);
	}

	private GoogleCredentials loadCredentials() throws IOException {
		if (StringUtils.hasText(CREDENTIALS_JSON)) {
			try (InputStream jsonStream = new ByteArrayInputStream(CREDENTIALS_JSON.getBytes(StandardCharsets.UTF_8))) {
				log.info("Firebase credentials loaded from FIREBASE_CREDENTIALS_JSON");
				return GoogleCredentials.fromStream(jsonStream);
			}
		}

		Path path = Path.of(CREDENTIALS_PATH).toAbsolutePath();
		if (!Files.exists(path)) {
			throw new IOException("Firebase credentials file not found: " + path);
		}

		try (InputStream serviceAccount = Files.newInputStream(path)) {
			log.info("Firebase credentials loaded from FIREBASE_CREDENTIALS_PATH: {}", path);
			return GoogleCredentials.fromStream(serviceAccount);
		}
	}

	private FirebaseApp ensureFirebaseApp(GoogleCredentials credentials) throws IOException {
		if (FirebaseApp.getApps().isEmpty()) {
			FirebaseOptions.Builder builder = FirebaseOptions.builder()
					.setCredentials(credentials);

			if (StringUtils.hasText(DATABASE_URL)) {
				builder.setDatabaseUrl(DATABASE_URL);
			}

			ownsFirebaseApp = true;
			return FirebaseApp.initializeApp(builder.build());
		}

		ownsFirebaseApp = false;
		return FirebaseApp.getInstance();
	}
}
