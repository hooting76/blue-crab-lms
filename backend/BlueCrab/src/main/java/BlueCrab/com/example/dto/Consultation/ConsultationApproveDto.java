package BlueCrab.com.example.dto.Consultation;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 상담 요청 승인을 위한 DTO
 */
public class ConsultationApproveDto {

    @NotNull(message = "요청 ID는 필수 입력 항목입니다.")
    private Long requestIdx;

    @Size(max = 500, message = "수락 메시지는 500자 이하로 입력해주세요.")
    private String acceptMessage;

    public ConsultationApproveDto() {}

    public Long getRequestIdx() {
        return requestIdx;
    }

    public void setRequestIdx(Long requestIdx) {
        this.requestIdx = requestIdx;
    }

    public String getAcceptMessage() {
        return acceptMessage;
    }

    public void setAcceptMessage(String acceptMessage) {
        this.acceptMessage = acceptMessage;
    }
}
