package BlueCrab.com.example.dto.Consultation;

import javax.validation.constraints.NotNull;

/**
 * 상담 ID만 필요한 요청을 위한 공통 DTO
 * (시작, 종료, 상세 조회 등)
 */
public class ConsultationIdDto {

    @NotNull(message = "요청 ID는 필수 입력 항목입니다.")
    private Long requestIdx;

    public ConsultationIdDto() {}

    public Long getRequestIdx() {
        return requestIdx;
    }

    public void setRequestIdx(Long requestIdx) {
        this.requestIdx = requestIdx;
    }
}
