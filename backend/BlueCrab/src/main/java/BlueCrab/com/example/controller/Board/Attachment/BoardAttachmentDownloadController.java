// 작성자 : 성태준
// 게시글 첨부파일 다운로드 전용 컨트롤러
// MinIO에서 파일을 스트리밍하여 다운로드 제공

package BlueCrab.com.example.controller.Board.Attachment;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ========== MinIO ==========
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.Board.Attachment.BoardAttachmentTbl;
import BlueCrab.com.example.service.Board.Attachment.BoardAttachmentQueryService;

@RestController
@RequestMapping("/api/board-attachments")
public class BoardAttachmentDownloadController {
    
    private static final Logger logger = LoggerFactory.getLogger(BoardAttachmentDownloadController.class);
    
    // ========== 의존성 주입 ==========
    
    @Autowired
    private MinioClient minioClient;
    
    @Autowired
    private BoardAttachmentQueryService queryService;
    
    // ========== 설정값 주입 ==========
    
    @Value("${app.minio.board-bucket-name}")
    private String bucketName;
    
    // ========== 다운로드 API ==========
    
    /**
     * 첨부파일 다운로드
     * @param request 요청 (attachmentIdx 포함)
     * @return 파일 스트림
     */
    @PostMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestBody java.util.Map<String, Integer> request) {
        Integer attachmentIdx = request.get("attachmentIdx");
        
        logger.info("첨부파일 다운로드 요청 - attachmentIdx: {}", attachmentIdx);
        
        try {
            // 1. 첨부파일 정보 조회
            BoardAttachmentTbl attachment = queryService.getAttachmentById(attachmentIdx);
            
            // 2. 활성화 상태 확인
            if (attachment.getIsActive() != 1) {
                logger.warn("비활성화된 첨부파일 접근 시도 - attachmentIdx: {}", attachmentIdx);
                return ResponseEntity.status(HttpStatus.GONE).build(); // 410 Gone
            }
            
            // 3. MinIO에서 파일 스트림 가져오기
            InputStream fileStream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(attachment.getFilePath())
                    .build()
            );
            
            // 4. HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            
            // Content-Type 설정
            String mimeType = attachment.getMimeType();
            if (mimeType != null && !mimeType.isEmpty()) {
                headers.setContentType(MediaType.parseMediaType(mimeType));
            } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }
            
            // 파일명 설정 (한글 파일명 지원)
            String originalFileName = attachment.getOriginalFileName();
            String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20"); // 공백 처리
            
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + originalFileName + "\"; filename*=UTF-8''" + encodedFileName);
            
            // 파일 크기 설정
            headers.setContentLength(attachment.getFileSize());
            
            // 5. 응답 생성
            InputStreamResource resource = new InputStreamResource(fileStream);
            
            logger.info("첨부파일 다운로드 시작 - attachmentIdx: {}, 파일명: {}, 크기: {} bytes", 
                       attachmentIdx, originalFileName, attachment.getFileSize());
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
                
        } catch (MinioException e) {
            logger.error("MinIO 오류 - attachmentIdx: {}, 오류: {}", attachmentIdx, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            
        } catch (Exception e) {
            logger.error("첨부파일 다운로드 실패 - attachmentIdx: {}, 오류: {}", attachmentIdx, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ========== 헬스체크 API (테스트용) ==========
    
    /**
     * 다운로드 서비스 상태 확인
     * @return 서비스 상태
     */
    @PostMapping("/download/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Download service is running");
    }
    
    // ========== 파일 정보 조회 API ==========
    
    /**
     * 첨부파일 메타데이터 조회 (다운로드 전 확인용)
     * @param request 요청 (attachmentIdx 포함)
     * @return 첨부파일 정보
     */
    @PostMapping("/info")
    public ResponseEntity<?> getFileInfo(@RequestBody java.util.Map<String, Integer> request) {
        Integer attachmentIdx = request.get("attachmentIdx");
        
        logger.debug("첨부파일 정보 조회 - attachmentIdx: {}", attachmentIdx);
        
        try {
            BoardAttachmentTbl attachment = queryService.getAttachmentById(attachmentIdx);
            
            if (attachment.getIsActive() != 1) {
                return ResponseEntity.status(HttpStatus.GONE).build();
            }
            
            // 파일 정보만 반환 (민감한 정보 제외)
            return ResponseEntity.ok(new FileInfo(
                attachment.getAttachmentIdx(),
                attachment.getOriginalFileName(),
                attachment.getFileSize(),
                attachment.getMimeType(),
                attachment.getUploadDate()
            ));
            
        } catch (Exception e) {
            logger.error("첨부파일 정보 조회 실패 - attachmentIdx: {}, 오류: {}", attachmentIdx, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ========== 내부 클래스 ==========
    
    /**
     * 파일 정보 응답용 DTO
     */
    public static class FileInfo {
        private Integer attachmentIdx;
        private String originalFileName;
        private Long fileSize;
        private String mimeType;
        private String uploadDate;
        
        public FileInfo(Integer attachmentIdx, String originalFileName, Long fileSize, String mimeType, String uploadDate) {
            this.attachmentIdx = attachmentIdx;
            this.originalFileName = originalFileName;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
            this.uploadDate = uploadDate;
        }
        
        // Getters
        public Integer getAttachmentIdx() { return attachmentIdx; }
        public String getOriginalFileName() { return originalFileName; }
        public Long getFileSize() { return fileSize; }
        public String getMimeType() { return mimeType; }
        public String getUploadDate() { return uploadDate; }
    }
}