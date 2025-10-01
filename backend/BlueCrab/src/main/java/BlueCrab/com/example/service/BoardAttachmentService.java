// 작성자 : 성태준
// 게시글 첨부파일 서비스 - 업로드 기능 우선 구현
// 파일 업로드, 검증, MinIO 연동 등 업로드 관련 비즈니스 로직만 처리

package BlueCrab.com.example.service;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.io.IOException;
import java.io.InputStream;
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

// ========== MinIO ==========
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.ObjectWriteResponse;
import io.minio.errors.MinioException;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.BoardAttachmentTbl;
import BlueCrab.com.example.repository.BoardAttachmentRepository;

@Service
public class BoardAttachmentService {

    private static final Logger logger = LoggerFactory.getLogger(BoardAttachmentService.class);

    // ========== 설정값 주입 ==========
    
    @Value("${app.minio.board-bucket-name}")
    private String bucketName;
    
    @Value("${app.file.allowed-extensions}")
    private String allowedExtensions;
    
    @Value("${app.file.max-per-post}")
    private Integer maxFilesPerPost;
    
    @Value("${app.file.expire-days}")
    private Integer expireDays;

    // ========== 의존성 주입 ==========
    
    @Autowired
    private MinioClient minioClient;  // @Primary Bean인 boardAttachmentMinioClient 자동 주입
    
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
        
        // 1. 파일 수 제한 검증
        validateFileCount(boardIdx, files.size());
        
        List<BoardAttachmentTbl> uploadedFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                // 2. 개별 파일 업로드 처리
                BoardAttachmentTbl attachment = uploadSingleFile(boardIdx, file);
                uploadedFiles.add(attachment);
                
            } catch (Exception e) {
                logger.error("파일 업로드 실패 - 파일명: {}, 오류: {}", file.getOriginalFilename(), e.getMessage());
                
                // 이미 업로드된 파일들 롤백 처리
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
            // 1. 파일 검증
            validateFile(file);
            
            // 2. 파일 경로 생성
            String filePath = generateFilePath(file.getOriginalFilename());
            
            // 3. MinIO에 파일 업로드
            uploadToMinIO(file, filePath, boardIdx);
            
            // 4. 데이터베이스에 첨부파일 정보 저장
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
     * @param boardIdx 게시글 IDX
     * @param newFileCount 새로 업로드할 파일 수
     */
    private void validateFileCount(Integer boardIdx, int newFileCount) {
        
        // 기존 첨부파일 수 조회
        int existingCount = attachmentRepository.countActiveAttachmentsByBoardIdx(boardIdx);
        
        // 총 파일 수 검증
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
     * @param file 검증할 파일
     */
    private void validateFile(MultipartFile file) {
        
        // 1. 파일 존재 여부
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        
        // 2. 파일명 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }
        
        // 3. 파일 확장자 검증
        validateFileExtension(originalFilename);
        
        logger.debug("파일 검증 완료 - 파일명: {}, 크기: {}bytes", originalFilename, file.getSize());
    }

    /**
     * 파일 확장자 검증
     * @param filename 파일명
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

    // ========== 파일 경로 및 이름 생성 ==========

    /**
     * 파일 저장 경로 생성
     * @param originalFilename 원본 파일명
     * @return 생성된 파일 경로
     */
    private String generateFilePath(String originalFilename) {
        
        // 날짜별 폴더 구조: YYYYMMDD/
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 고유 파일명 생성: YYYYMMDD-HHMMSS-UUID.확장자
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
     * @param filename 파일명
     * @return 확장자 (점 제외)
     */
    private String getFileExtension(String filename) {
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("파일 확장자를 찾을 수 없습니다: " + filename);
        }
        
        return filename.substring(lastDotIndex + 1);
    }

    // ========== MinIO 연동 메서드 ==========

    /**
     * MinIO에 파일 업로드
     * @param file 업로드할 파일
     * @param filePath 저장할 경로
     * @param boardIdx 게시글 IDX (태그용)
     */
    private void uploadToMinIO(MultipartFile file, String filePath, Integer boardIdx) {
        
        try (InputStream inputStream = file.getInputStream()) {
            
            // MinIO 업로드 요청 생성
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filePath)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            
            // MinIO에 업로드 실행
            ObjectWriteResponse response = minioClient.putObject(putObjectArgs);
            
            logger.debug("MinIO 업로드 완료 - 버킷: {}, 경로: {}, ETag: {}", 
                        bucketName, filePath, response.etag());
            
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 오류: " + e.getMessage(), e);
        } catch (MinioException e) {
            throw new RuntimeException("MinIO 업로드 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("파일 업로드 중 예상치 못한 오류: " + e.getMessage(), e);
        }
    }

    // ========== 엔티티 생성 메서드 ==========

    /**
     * 첨부파일 엔티티 생성
     * @param boardIdx 게시글 IDX
     * @param file 업로드된 파일
     * @param filePath 저장된 파일 경로
     * @return 첨부파일 엔티티
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

    // ========== 롤백 처리 메서드 ==========

    /**
     * 업로드된 파일들 롤백 처리 (간단 버전)
     * @param uploadedFiles 롤백할 첨부파일 목록
     */
    private void rollbackUploadedFiles(List<BoardAttachmentTbl> uploadedFiles) {
        
        logger.warn("파일 업로드 롤백 시작 - 롤백 대상: {}개", uploadedFiles.size());
        
        for (BoardAttachmentTbl attachment : uploadedFiles) {
            try {
                // 데이터베이스에서 삭제 (간단하게 처리)
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

    // ========== 첨부파일 조회 메서드 ==========

    /**
     * 게시글별 활성화된 첨부파일 목록 조회
     * @param boardIdx 게시글 IDX
     * @return 활성화된 첨부파일 목록
     */
    public List<BoardAttachmentTbl> getActiveAttachmentsByBoardId(Integer boardIdx) {
        
        logger.debug("게시글별 활성화된 첨부파일 목록 조회 - 게시글 IDX: {}", boardIdx);
        
        try {
            List<BoardAttachmentTbl> attachments = attachmentRepository.findByBoardIdxAndIsActive(boardIdx, 1);
            logger.info("게시글별 첨부파일 목록 조회 완료 - 게시글 IDX: {}, 첨부파일 수: {}", boardIdx, attachments.size());
            return attachments;
            
        } catch (Exception e) {
            logger.error("게시글별 첨부파일 목록 조회 실패 - 게시글 IDX: {}, 오류: {}", boardIdx, e.getMessage(), e);
            throw new RuntimeException("첨부파일 목록 조회 실패: " + e.getMessage(), e);
        }
    }
}