package BlueCrab.com.example.dto.Consultation;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 상담 목록 통합 조회 요청 DTO.
 */
@Data
public class ConsultationListRequest {

    @NotNull(message = "조회 유형은 필수입니다.")
    private ViewType viewType;

    private String status;

    private LocalDate startDate;
    private LocalDate endDate;

    @Min(value = 0, message = "페이지는 0 이상이어야 합니다.")
    private int page = 0;

    @Min(value = 1, message = "사이즈는 1 이상이어야 합니다.")
    @Max(value = 100, message = "사이즈는 100 이하이어야 합니다.")
    private int size = 10;
}
