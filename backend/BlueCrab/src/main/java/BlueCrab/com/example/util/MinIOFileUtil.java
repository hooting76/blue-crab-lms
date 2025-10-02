// 작성자 : 성태준
// MinIO 파일 연동 유틸리티 클래스
// 파일 업로드, 삭제 등 MinIO 관련 공통 기능 제공

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.io.IOException;
import java.io.InputStream;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

// ========== MinIO ==========
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.ObjectWriteResponse;
import io.minio.errors.MinioException;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MinIOFileUtil {

    private static final Logger logger = LoggerFactory.getLogger(MinIOFileUtil.class);

    // ========== 설정값 주입 ==========
    
    @Value("${app.minio.board-bucket-name}")
    private String bucketName;

    // ========== 의존성 주입 ==========
    
    @Autowired
    private MinioClient minioClient;

    // ========== 파일 업로드 메서드 ==========

    /**
     * MinIO에 파일 업로드
     * @param file 업로드할 파일
     * @param filePath 저장할 경로
     * @return 업로드 응답 정보
     */
    public ObjectWriteResponse uploadFile(MultipartFile file, String filePath) {
        
        try (InputStream inputStream = file.getInputStream()) {
            
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filePath)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            
            ObjectWriteResponse response = minioClient.putObject(putObjectArgs);
            
            logger.debug("MinIO 업로드 완료 - 버킷: {}, 경로: {}, ETag: {}", 
                        bucketName, filePath, response.etag());
            
            return response;
            
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 오류: " + e.getMessage(), e);
        } catch (MinioException e) {
            throw new RuntimeException("MinIO 업로드 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("파일 업로드 중 예상치 못한 오류: " + e.getMessage(), e);
        }
    }

    // ========== 파일 삭제 메서드 ==========

    /**
     * MinIO에서 파일 삭제
     * @param filePath 삭제할 파일 경로
     */
    public void deleteFile(String filePath) {
        
        try {
            RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filePath)
                    .build();
            
            minioClient.removeObject(removeObjectArgs);
            
            logger.debug("MinIO 파일 삭제 완료 - 버킷: {}, 경로: {}", bucketName, filePath);
            
        } catch (MinioException e) {
            throw new RuntimeException("MinIO 파일 삭제 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("파일 삭제 중 예상치 못한 오류: " + e.getMessage(), e);
        }
    }

    // ========== 유틸리티 메서드 ==========

    /**
     * 버킷명 조회
     * @return 설정된 버킷명
     */
    public String getBucketName() {
        return bucketName;
    }
}