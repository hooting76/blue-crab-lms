package BlueCrab.com.example.dto.Consultation;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 상담 상세 조회 요청 DTO.
 */
@Data
public class ConsultationDetailRequest {

    @NotNull(message = "상담 ID는 필수입니다.")
    private Long id;
}
