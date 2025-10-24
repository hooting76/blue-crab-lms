package BlueCrab.com.example.dto.Consultation;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 상담 요청 취소를 위한 DTO
 */
public class ConsultationCancelDto {

    @NotNull(message = "요청 ID는 필수 입력 항목입니다.")
    private Long requestIdx;

    @NotBlank(message = "취소 사유는 필수 입력 항목입니다.")
    @Size(max = 500, message = "취소 사유는 500자 이하로 입력해주세요.")
    private String cancelReason;

    public ConsultationCancelDto() {}

    public Long getRequestIdx() {
        return requestIdx;
    }

    public void setRequestIdx(Long requestIdx) {
        this.requestIdx = requestIdx;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
