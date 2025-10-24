package BlueCrab.com.example.dto.Consultation;

import javax.validation.constraints.NotNull;

/**
 * 상담 메모 작성/수정을 위한 DTO
 */
public class ConsultationMemoDto {

    @NotNull(message = "요청 ID는 필수 입력 항목입니다.")
    private Long requestIdx;

    private String memo;

    public ConsultationMemoDto() {}

    public Long getRequestIdx() {
        return requestIdx;
    }

    public void setRequestIdx(Long requestIdx) {
        this.requestIdx = requestIdx;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
