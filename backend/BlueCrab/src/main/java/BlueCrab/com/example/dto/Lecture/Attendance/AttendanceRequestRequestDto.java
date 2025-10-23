package BlueCrab.com.example.dto.Lecture.Attendance;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 출석 인정 요청 Request DTO
 * POST /api/attendance/request
 * 
 * 학생이 출석 인정을 요청할 때 사용
 * JWT 토큰에서 studentIdx 추출하여 사용
 */
public class AttendanceRequestRequestDto {

    @NotNull(message = "강의 코드는 필수입니다")
    private String lecSerial;           // 강의 코드 (LEC_SERIAL)

    @NotNull(message = "회차 번호는 필수입니다")
    @Min(value = 1, message = "회차 번호는 1 이상이어야 합니다")
    @Max(value = 80, message = "회차 번호는 80 이하여야 합니다")
    private Integer sessionNumber;      // 회차 번호 (1~80)

    private String requestReason;       // 요청 사유 (선택사항)

    // Constructors

    public AttendanceRequestRequestDto() {}

    public AttendanceRequestRequestDto(String lecSerial, Integer sessionNumber, String requestReason) {
        this.lecSerial = lecSerial;
        this.sessionNumber = sessionNumber;
        this.requestReason = requestReason;
    }

    // Getters and Setters

    public String getLecSerial() {
        return lecSerial;
    }

    public void setLecSerial(String lecSerial) {
        this.lecSerial = lecSerial;
    }

    public Integer getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(Integer sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public String getRequestReason() {
        return requestReason;
    }

    public void setRequestReason(String requestReason) {
        this.requestReason = requestReason;
    }

    @Override
    public String toString() {
        return "AttendanceRequestRequestDto{" +
                "lecSerial='" + lecSerial + '\'' +
                ", sessionNumber=" + sessionNumber +
                ", requestReason='" + requestReason + '\'' +
                '}';
    }
}
