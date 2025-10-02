// 작성자 : 성태준
// 첨부파일 연결 요청 DTO
// 게시글에 첨부파일 IDX 목록을 연결하기 위한 데이터 전송 객체

package BlueCrab.com.example.dto;

// ========== Java 표준 라이브러리 ==========
import java.util.List;

// ========== Validation 어노테이션 ==========
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.NotEmpty;

public class AttachmentLinkRequest {
    
    @NotNull(message = "첨부파일 IDX 목록은 필수입니다")
    @NotEmpty(message = "첨부파일 IDX 목록은 비어있을 수 없습니다")
    @Size(min = 1, max = 5, message = "첨부파일은 1개 이상 5개 이하로 가능합니다")
    private List<Integer> attachmentIdx;
    
    // ========== 생성자 ==========
    
    // 기본 생성자
    public AttachmentLinkRequest() {}
    
    // 전체 생성자
    public AttachmentLinkRequest(List<Integer> attachmentIdx) {
        this.attachmentIdx = attachmentIdx;
    }
    
    // ========== Getter/Setter 메서드 ==========
    
    public List<Integer> getAttachmentIdx() {
        return attachmentIdx;
    }
    
    public void setAttachmentIdx(List<Integer> attachmentIdx) {
        this.attachmentIdx = attachmentIdx;
    }
    
    // ========== 유틸리티 메서드 ==========
    // 첨부파일 IDX 목록이 비어있는지 확인
    public boolean isEmpty() {
        return attachmentIdx == null || attachmentIdx.isEmpty();
    }

    // 첨부파일 개수 반환
    public int getAttachmentCount() {
        return attachmentIdx != null ? attachmentIdx.size() : 0;
    }
    
    @Override
    public String toString() {
        return "AttachmentLinkRequest{" +
                "attachmentIdx=" + attachmentIdx +
                ", count=" + getAttachmentCount() +
                '}';
    }
}