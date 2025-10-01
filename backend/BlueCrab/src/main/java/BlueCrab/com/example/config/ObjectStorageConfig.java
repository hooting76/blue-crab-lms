// 작성자 : 성태준
// 게시글 첨부파일 전용 객체 스토리지 설정 클래스
// board-attached 버킷을 사용하여 첨부파일을 저장하고 관리
// 첨부파일 전용 MinIO 클라이언트 빈을 생성하고 관리

package BlueCrab.com.example.config;

// ========== 임포트 구문 ==========

// ========== MinIO 라이브러리 ==========
import io.minio.MinioClient;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Configuration
public class ObjectStorageConfig {

    private static final Logger logger = LoggerFactory.getLogger(ObjectStorageConfig.class);
    // 로그 설정

    // ========== MinIO 연결 설정 ==========
    @Value("${app.minio.endpoint}") // application.properties에서 설정값 주입
    private String endpoint; // MinIO 서버 엔드포인트

    @Value("${app.minio.access-key}") 
    private String accessKey; // MinIO 접근 키

    @Value("${app.minio.secret-key}")
    private String secretKey; // MinIO 비밀 키

    // 게시글 첨부파일 전용 MinIO 클라이언트 빈
    @Bean
    @Primary
    public MinioClient boardAttachmentMinioClient() {
        logger.info("Creating board attachment MinIO client with endpoint: {}", endpoint);
        // 로그 설정
        
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
                // MinIO 클라이언트 생성
            
            logger.info("Board attachment MinIO client created successfully");
            // MinIO 클라이언트 생성 성공 로그
            return client;
            // 생성된 MinIO 클라이언트 반환
            
        } catch (Exception e) {
            logger.error("Failed to create board attachment MinIO client: {}", e.getMessage(), e);
            // 에러 로그
            throw new RuntimeException("게시글 첨부파일용 MinIO 클라이언트 생성 실패", e);
            // 예외 처리
        }
    }
}