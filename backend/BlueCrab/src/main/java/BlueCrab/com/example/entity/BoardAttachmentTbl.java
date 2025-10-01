// 작성자 : 성태준
// 게시글 첨부파일 테이블 엔티티
// 첨부파일의 메타데이터 정보를 저장하는 엔티티

package BlueCrab.com.example.entity;

// ========== JPA 어노테이션 ==========
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Table;

// ========== Validation 어노테이션 ==========
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Entity
@Table(name = "BOARD_ATTACHMENT_TBL")
public class BoardAttachmentTbl {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ATTACHMENT_IDX", nullable = false)
    private Integer attachmentIdx;
    // 첨부파일 IDX (기본키, 자동생성)
    
    @NotNull(message = "게시글 IDX는 필수입니다")
    @Column(name = "BOARD_IDX", nullable = false)
    private Integer boardIdx;
    // 게시글 IDX (외래키)
    
    @NotBlank(message = "원본 파일명은 필수입니다")
    @Size(max = 255, message = "원본 파일명은 255자를 초과할 수 없습니다")
    @Column(name = "ORIGINAL_FILE_NAME", length = 255, nullable = false)
    private String originalFileName;
    // 원본 파일명
    
    @NotBlank(message = "파일 경로는 필수입니다")
    @Size(max = 500, message = "파일 경로는 500자를 초과할 수 없습니다")
    @Column(name = "FILE_PATH", length = 500, nullable = false)
    private String filePath;
    // 파일 경로 (변환된 파일명 포함)
    
    @NotNull(message = "파일 크기는 필수입니다")
    @Column(name = "FILE_SIZE", nullable = false)
    private Long fileSize;
    // 파일 크기 (bytes)
    
    @NotBlank(message = "MIME 타입은 필수입니다")
    @Size(max = 100, message = "MIME 타입은 100자를 초과할 수 없습니다")
    @Column(name = "MIME_TYPE", length = 100, nullable = false)
    private String mimeType;
    // 파일 MIME 타입
    
    @NotBlank(message = "업로드 일자는 필수입니다")
    @Size(max = 50, message = "업로드 일자는 50자를 초과할 수 없습니다")
    @Column(name = "UPLOAD_DATE", length = 50, nullable = false)
    private String uploadDate;
    // 업로드 일자
    
    @Size(max = 50, message = "만료일은 50자를 초과할 수 없습니다")
    @Column(name = "EXPIRY_DATE", length = 50, nullable = true)
    private String expiryDate;
    // 파일 만료일 (nullable)
    
    @NotNull(message = "활성화 여부는 필수입니다")
    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive = 1;
    // 활성화 여부 (1 : 활성 / 0 : 비활성)
    
    // ========== 생성자 ==========
    
    // 기본 생성자
    public BoardAttachmentTbl() {}
    
    // 전체 생성자 (ID 제외)
    public BoardAttachmentTbl(Integer boardIdx, String originalFileName, String filePath, 
                             Long fileSize, String mimeType, String uploadDate, String expiryDate, Integer isActive) {
        this.boardIdx = boardIdx;
        this.originalFileName = originalFileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.uploadDate = uploadDate;
        this.expiryDate = expiryDate;
        this.isActive = isActive;
    }
    
    // ========== Getter/Setter 메서드 ==========
    
    public Integer getAttachmentIdx() {
        return attachmentIdx;
    }
    
    public void setAttachmentIdx(Integer attachmentIdx) {
        this.attachmentIdx = attachmentIdx;
    }
    
    public Integer getBoardIdx() {
        return boardIdx;
    }
    
    public void setBoardIdx(Integer boardIdx) {
        this.boardIdx = boardIdx;
    }
    
    public String getOriginalFileName() {
        return originalFileName;
    }
    
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public String getUploadDate() {
        return uploadDate;
    }
    
    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }
    
    public String getExpiryDate() {
        return expiryDate;
    }
    
    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public Integer getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }
    
    // ========== 유틸리티 메서드 ==========
    
    @Override
    public String toString() {
        return "BoardAttachmentTbl{" +
                "attachmentIdx=" + attachmentIdx +
                ", boardIdx=" + boardIdx +
                ", originalFileName='" + originalFileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", mimeType='" + mimeType + '\'' +
                ", uploadDate='" + uploadDate + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
