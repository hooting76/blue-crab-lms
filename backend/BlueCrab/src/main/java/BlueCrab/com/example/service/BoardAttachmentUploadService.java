// 작성자 : 성태준
// 게시글 첨부파일 업로드 전용 서비스
// 파일 업로드, 검증 관련 비즈니스 로직 처리

package BlueCrab.com.example.service;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.BoardAttachmentTbl;
import BlueCrab.com.example.repository.BoardAttachmentRepository;
import BlueCrab.com.example.util.MinIOFileUtil;

@Service
public class BoardAttachmentUploadService {

    private static final Logger logger = LoggerFactory.getLogger(BoardAttachmentUploadService.class);

    // ========== 설정값 주입 ==========
    
    @Value("${app.file.allowed-extensions}")
    private String allowedExtensions;
    
    @Value("${app.file.max-per-post}")
    private Integer maxFilesPerPost;
    
    @Value("${app.file.expire-days}")
    private Integer expireDays;

    // ========== 의존성 주입 ==========
    
    @Autowired
    private MinIOFileUtil minIOFileUtil;
    
    @Autowired
    private BoardAttachmentRepository attachmentRepository;

    // ========== 파일 업로드 메서드 ==========

    /**
     * 다중 파일 업로드 처리
     * @param boardIdx 게시글 IDX
     * @param files 업로드할 파일들
     * @return 업로드된 첨부파일 엔티티 목록
     */
    public List<BoardAttachmentTbl> uploadFiles(Integer boardIdx, List<MultipartFile> files) {
        
        logger.info("다중 파일 업로드 시작 - 게시글 IDX: {}, 파일 수: {}", boardIdx, files.size());
        
        validateFileCount(boardIdx, files.size());
        
        List<BoardAttachmentTbl> uploadedFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                BoardAttachmentTbl attachment = uploadSingleFile(boardIdx, file);
                uploadedFiles.add(attachment);
                
            } catch (Exception e) {
                logger.error("파일 업로드 실패 - 파일명: {}, 오류: {}", file.getOriginalFilename(), e.getMessage());
                rollbackUploadedFiles(uploadedFiles);
                throw new RuntimeException("파일 업로드 중 오류 발생: " + e.getMessage(), e);
            }
        }
        
        logger.info("다중 파일 업로드 완료 - 게시글 IDX: {}, 성공 파일 수: {}", boardIdx, uploadedFiles.size());
        return uploadedFiles;
    }

    /**
     * 단일 파일 업로드 처리
     * @param boardIdx 게시글 IDX
     * @param file 업로드할 파일
     * @return 업로드된 첨부파일 엔티티
     */
    public BoardAttachmentTbl uploadSingleFile(Integer boardIdx, MultipartFile file) {
        
        logger.info("단일 파일 업로드 시작 - 게시글 IDX: {}, 파일명: {}", boardIdx, file.getOriginalFilename());
        
        try {
            validateFile(file);
            String filePath = generateFilePath(file.getOriginalFilename());
            
            minIOFileUtil.uploadFile(file, filePath);
            
            BoardAttachmentTbl attachment = createAttachmentEntity(boardIdx, file, filePath);
            BoardAttachmentTbl savedAttachment = attachmentRepository.save(attachment);
            
            logger.info("단일 파일 업로드 완료 - 첨부파일 IDX: {}, 파일 경로: {}", 
                       savedAttachment.getAttachmentIdx(), filePath);
            
            return savedAttachment;
            
        } catch (Exception e) {
            logger.error("단일 파일 업로드 실패 - 파일명: {}, 오류: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("파일 업로드 실패: " + e.getMessage(), e);
        }
    }

    // ========== 파일 검증 메서드 ==========

    /**
     * 파일 개수 제한 검증
     */
    private void validateFileCount(Integer boardIdx, int newFileCount) {
        int existingCount = attachmentRepository.countActiveAttachmentsByBoardIdx(boardIdx);
        int totalCount = existingCount + newFileCount;
        
        if (totalCount > maxFilesPerPost) {
            throw new IllegalArgumentException(
                String.format("첨부파일 수 제한 초과 (현재: %d개, 추가: %d개, 최대: %d개)", 
                            existingCount, newFileCount, maxFilesPerPost)
            );
        }
        
        logger.debug("파일 수 검증 통과 - 게시글 IDX: {}, 기존: {}개, 추가: {}개, 총: {}개", 
                    boardIdx, existingCount, newFileCount, totalCount);
    }

    /**
     * 단일 파일 검증
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }
        
        validateFileExtension(originalFilename);
        
        logger.debug("파일 검증 완료 - 파일명: {}, 크기: {}bytes", originalFilename, file.getSize());
    }

    /**
     * 파일 확장자 검증
     */
    private void validateFileExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        List<String> allowedExtList = Arrays.asList(allowedExtensions.toLowerCase().split(","));
        
        if (!allowedExtList.contains(extension)) {
            throw new IllegalArgumentException(
                String.format("허용되지 않은 파일 형식입니다. (현재: %s, 허용: %s)", 
                            extension, allowedExtensions)
            );
        }
    }

    // ========== 파일 경로 생성 ==========

    /**
     * 파일 저장 경로 생성
     */
    private String generateFilePath(String originalFilename) {
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalFilename);
        
        String uniqueFilename = String.format("%s-%s.%s", timeStamp, uuid, extension);
        String filePath = dateFolder + "/" + uniqueFilename;
        
        logger.debug("파일 경로 생성 - 원본: {}, 생성: {}", originalFilename, filePath);
        return filePath;
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("파일 확장자를 찾을 수 없습니다: " + filename);
        }
        return filename.substring(lastDotIndex + 1);
    }

    // ========== 엔티티 생성 ==========

    /**
     * 첨부파일 엔티티 생성
     */
    private BoardAttachmentTbl createAttachmentEntity(Integer boardIdx, MultipartFile file, String filePath) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusDays(expireDays);
        
        BoardAttachmentTbl attachment = new BoardAttachmentTbl();
        attachment.setBoardIdx(boardIdx);
        attachment.setOriginalFileName(file.getOriginalFilename());
        attachment.setFilePath(filePath);
        attachment.setFileSize(file.getSize());
        attachment.setMimeType(file.getContentType());
        attachment.setUploadDate(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        attachment.setExpiryDate(expiryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        attachment.setIsActive(1);
        
        logger.debug("첨부파일 엔티티 생성 - 게시글 IDX: {}, 파일명: {}", boardIdx, file.getOriginalFilename());
        return attachment;
    }

    // ========== 롤백 처리 ==========

    /**
     * 업로드된 파일들 롤백 처리
     */
    private void rollbackUploadedFiles(List<BoardAttachmentTbl> uploadedFiles) {
        logger.warn("파일 업로드 롤백 시작 - 롤백 대상: {}개", uploadedFiles.size());
        
        for (BoardAttachmentTbl attachment : uploadedFiles) {
            try {
                if (attachment.getAttachmentIdx() != null) {
                    attachmentRepository.delete(attachment);
                    logger.debug("롤백 완료 - 첨부파일 IDX: {}", attachment.getAttachmentIdx());
                }
            } catch (Exception e) {
                logger.error("롤백 실패 - 첨부파일 IDX: {}, 오류: {}", 
                           attachment.getAttachmentIdx(), e.getMessage());
            }
        }
        
        logger.warn("파일 업로드 롤백 완료");
    }
}