package BlueCrab.com.example.dto;

import javax.validation.constraints.NotNull;

/**
 * 관리자의 예약 승인 요청을 위한 DTO 클래스
 */
public class AdminApproveRequestDto {

    @NotNull(message = "예약 ID는 필수 입력 항목입니다.")
    private Integer reservationIdx;

    private String adminNote;

    public AdminApproveRequestDto() {}

    public AdminApproveRequestDto(Integer reservationIdx, String adminNote) {
        this.reservationIdx = reservationIdx;
        this.adminNote = adminNote;
    }

    public Integer getReservationIdx() {
        return reservationIdx;
    }

    public void setReservationIdx(Integer reservationIdx) {
        this.reservationIdx = reservationIdx;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }
}
