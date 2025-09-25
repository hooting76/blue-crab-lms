package BlueCrab.com.example.service;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 객체 스토리지 서비스
 * 프로필 이미지 저장 및 조회를 위한 MinIO 클라이언트 래퍼
 *
 * 주요 기능:
 * - 프로필 이미지 파일의 Presigned URL 생성
 * - 이미지 파일 접근 권한 관리
 * - MinIO 서버 연결 및 설정 관리
 *
 * 사용 방식:
 * - 프로필 사진은 관리자가 업로드하므로 조회 기능만 제공
 * - Presigned URL을 통해 직접 이미지 접근 가능
 * - URL 유효기간 설정으로 보안 강화
 */
@Service
public class MinIOService {

    private static final Logger logger = LoggerFactory.getLogger(MinIOService.class);

    @Value("${app.minio.endpoint}")
    private String minioEndpoint;

    @Value("${app.minio.access-key}")
    private String minioAccessKey;

    @Value("${app.minio.secret-key}")
    private String minioSecretKey;

    @Value("${app.minio.bucket-name}")
    private String bucketName;

    @Value("${app.minio.profile-folder}")
    private String profileFolder;

    private MinioClient minioClient;

    /**
     * 이미지 키로부터 MinIO 객체 이름 생성
     * 프로필 폴더가 설정된 경우 폴더/키 형태로, 없으면 키만 사용
     */
    private String buildObjectName(String imageKey) {
        if (profileFolder == null || profileFolder.trim().isEmpty()) {
            return imageKey;
        }
        return profileFolder + "/" + imageKey;
    }

    /**
     * MinIO 클라이언트 초기화
     * 서비스 시작 시 MinIO 서버와 연결 설정
     */
    @PostConstruct
    public void initializeMinioClient() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(minioEndpoint)
                    .credentials(minioAccessKey, minioSecretKey)
                    .build();

            logger.info("MinIO 클라이언트 초기화 완료 - Endpoint: {}, Bucket: {}", minioEndpoint, bucketName);

        } catch (Exception e) {
            logger.error("MinIO 클라이언트 초기화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("MinIO 서비스 초기화에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 프로필 이미지의 Presigned URL 생성
     * 클라이언트가 직접 이미지에 접근할 수 있는 임시 URL을 생성
     *
     * @param imageKey 이미지 파일의 키 (PROFILE_VIEW의 profile_image_key)
     * @return 이미지에 접근할 수 있는 Presigned URL
     * @throws RuntimeException MinIO 연결 오류 시
     */
    public String getProfileImageUrl(String imageKey) {
        if (imageKey == null || imageKey.trim().isEmpty()) {
            logger.debug("프로필 이미지 키가 비어있음");
            return null;
        }

        try {
            // 프로필 폴더 내 이미지 경로 생성
            String objectName = buildObjectName(imageKey);

            logger.debug("프로필 이미지 URL 생성 요청 - Object: {}", objectName);

            // Presigned URL 생성 (24시간 유효)
            String presignedUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(24, TimeUnit.HOURS)  // 24시간 유효
                    .build()
            );

            logger.debug("프로필 이미지 URL 생성 성공 - Key: {}", imageKey);
            return presignedUrl;

        } catch (MinioException e) {
            logger.warn("MinIO 서버 오류로 이미지 URL 생성 실패 - Key: {}, Error: {}", imageKey, e.getMessage());
            return null;

        } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            logger.error("이미지 URL 생성 중 시스템 오류 - Key: {}, Error: {}", imageKey, e.getMessage(), e);
            return null;

        } catch (Exception e) {
            logger.error("예상하지 못한 오류로 이미지 URL 생성 실패 - Key: {}, Error: {}", imageKey, e.getMessage(), e);
            return null;
        }
    }


    /**
     * 이미지 파일 존재 여부 확인
     * 실제로 MinIO에 해당 이미지가 존재하는지 확인
     *
     * @param imageKey 확인할 이미지 키
     * @return 이미지 존재 여부
     */
    public boolean imageExists(String imageKey) {
        if (imageKey == null || imageKey.trim().isEmpty()) {
            return false;
        }

        try {
            String objectName = buildObjectName(imageKey);

            // 객체 정보 조회로 존재 여부 확인
            minioClient.statObject(
                io.minio.StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );

            return true;

        } catch (Exception e) {
            logger.debug("이미지 파일 존재하지 않음 - Key: {}", imageKey);
            return false;
        }
    }

    /**
     * MinIO 서버 연결 상태 확인
     *
     * @return 연결 상태
     */
    public boolean isMinioHealthy() {
        try {
            // 버킷 존재 여부 확인으로 연결 테스트
            boolean exists = minioClient.bucketExists(
                io.minio.BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );

            logger.debug("MinIO 서버 상태 확인 - 버킷 존재: {}", exists);
            return exists;

        } catch (Exception e) {
            logger.warn("MinIO 서버 연결 실패: {}", e.getMessage());
            return false;
        }
    }

    // Getter methods for configuration values
    public String getBucketName() {
        return bucketName;
    }

    public String getProfileFolder() {
        return profileFolder;
    }

    public String getMinioEndpoint() {
        return minioEndpoint;
    }

    /**
     * 프로필 이미지 파일을 직접 스트림으로 조회
     * 백엔드 프록시 방식으로 이미지를 전달할 때 사용
     *
     * @param imageKey 이미지 파일의 키
     * @return 이미지 파일 스트림
     * @throws Exception MinIO 연결 오류 또는 파일 없음
     */
    public InputStream getProfileImageStream(String imageKey) throws Exception {
        if (imageKey == null || imageKey.trim().isEmpty()) {
            throw new IllegalArgumentException("이미지 키가 비어있습니다.");
        }

        try {
            String objectName = buildObjectName(imageKey);

            logger.debug("프로필 이미지 스트림 조회 - Object: {}", objectName);

            InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );

            logger.debug("프로필 이미지 스트림 조회 성공 - Key: {}", imageKey);
            return inputStream;

        } catch (Exception e) {
            logger.warn("프로필 이미지 스트림 조회 실패 - Key: {}, Error: {}", imageKey, e.getMessage());
            throw new Exception("이미지 파일을 찾을 수 없습니다: " + imageKey, e);
        }
    }


    /**
     * 이미지 파일의 메타데이터 조회
     * Content-Type, 파일 크기 등 정보 획득
     *
     * @param imageKey 이미지 파일의 키
     * @return 파일 메타데이터
     * @throws Exception MinIO 연결 오류 또는 파일 없음
     */
    public StatObjectResponse getImageMetadata(String imageKey) throws Exception {
        if (imageKey == null || imageKey.trim().isEmpty()) {
            throw new IllegalArgumentException("이미지 키가 비어있습니다.");
        }

        try {
            String objectName = buildObjectName(imageKey);

            StatObjectResponse metadata = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );

            logger.debug("이미지 메타데이터 조회 성공 - Key: {}, Size: {}, Type: {}",
                        imageKey, metadata.size(), metadata.contentType());

            return metadata;

        } catch (Exception e) {
            logger.warn("이미지 메타데이터 조회 실패 - Key: {}, Error: {}", imageKey, e.getMessage());
            throw new Exception("이미지 파일 정보를 조회할 수 없습니다: " + imageKey, e);
        }
    }


    /**
     * 이미지 타입 추측 (파일 확장자 기반)
     *
     * @param imageKey 이미지 파일의 키
     * @return MIME 타입
     */
    public String guessContentType(String imageKey) {
        if (imageKey == null) {
            return "application/octet-stream";
        }

        String lowerKey = imageKey.toLowerCase();
        if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerKey.endsWith(".png")) {
            return "image/png";
        } else if (lowerKey.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerKey.endsWith(".webp")) {
            return "image/webp";
        } else {
            return "image/jpeg"; // 기본값
        }
    }
}