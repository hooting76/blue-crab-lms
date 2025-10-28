package BlueCrab.com.example.dto.Consultation;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 상담 상태 변경 요청 DTO.
 */
@Data
public class ConsultationStatusRequest {

    @NotNull(message = "상담 ID는 필수입니다.")
    private Long id;

    @NotBlank(message = "변경할 상태는 필수입니다.")
    private String status;

    @Size(max = 500, message = "사유는 500자 이내로 입력해주세요.")
    private String reason;
}
