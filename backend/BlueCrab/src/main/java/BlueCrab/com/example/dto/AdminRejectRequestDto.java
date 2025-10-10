package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 관리자의 예약 반려 요청을 위한 DTO 클래스
 */
public class AdminRejectRequestDto {

    @NotNull(message = "예약 ID는 필수 입력 항목입니다.")
    private Integer reservationIdx;

    @NotBlank(message = "반려 사유는 필수 입력 항목입니다.")
    private String rejectionReason;

    public AdminRejectRequestDto() {}

    public AdminRejectRequestDto(Integer reservationIdx, String rejectionReason) {
        this.reservationIdx = reservationIdx;
        this.rejectionReason = rejectionReason;
    }

    public Integer getReservationIdx() {
        return reservationIdx;
    }

    public void setReservationIdx(Integer reservationIdx) {
        this.reservationIdx = reservationIdx;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
