package BlueCrab.com.example.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.location:}")
    private String credentialsLocation;

    @Value("${firebase.credentials.json:}")
    private String credentialsJson;

    @Value("${firebase.database.url:}")
    private String databaseUrl;

    @Value("${firebase.vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${firebase.vapid.private-key:}")
    private String vapidPrivateKey;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(loadCredentials());

        if (StringUtils.hasText(databaseUrl)) {
            builder.setDatabaseUrl(databaseUrl);
        }

        FirebaseOptions options = builder.build();

        List<FirebaseApp> apps = FirebaseApp.getApps();
        if (apps != null) {
            for (FirebaseApp app : apps) {
                if (FirebaseApp.DEFAULT_APP_NAME.equals(app.getName())) {
                    log.debug("Reusing existing FirebaseApp instance");
                    return app;
                }
            }
        }

        return FirebaseApp.initializeApp(options);
    }

    private GoogleCredentials loadCredentials() throws IOException {
        // 1순위: JSON 문자열로 직접 제공된 credentials 사용
        if (StringUtils.hasText(credentialsJson)) {
            try (InputStream serviceAccount = new ByteArrayInputStream(credentialsJson.getBytes())) {
                log.info("FirebaseApp initialized using credentials from JSON string");
                return GoogleCredentials.fromStream(serviceAccount);
            }
        }
        
        // 2순위: 파일 경로로 제공된 credentials 사용
        if (StringUtils.hasText(credentialsLocation)) {
            Path path = Path.of(credentialsLocation).toAbsolutePath();
            if (!Files.exists(path)) {
                throw new IOException("Firebase credentials file not found at: " + path);
            }

            try (InputStream serviceAccount = Files.newInputStream(path)) {
                log.info("FirebaseApp initialized using credentials from file: {}", credentialsLocation);
                return GoogleCredentials.fromStream(serviceAccount);
            }
        }

        // 3순위: Application Default Credentials (ADC) 사용
        log.info("FirebaseApp initialized using Application Default Credentials");
        return GoogleCredentials.getApplicationDefault();
    }
}
