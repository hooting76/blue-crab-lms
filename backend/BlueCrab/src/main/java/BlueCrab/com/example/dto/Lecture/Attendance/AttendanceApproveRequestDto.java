package BlueCrab.com.example.dto.Lecture.Attendance;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 출석 승인/반려 Request DTO
 * PUT /api/attendance/approve
 * 
 * 교수가 학생들의 출석 요청을 승인/반려할 때 사용
 * JWT 토큰에서 professorIdx 추출하여 approvedBy에 사용
 */
public class AttendanceApproveRequestDto {

    @NotNull(message = "강의 코드는 필수입니다")
    private String lecSerial;           // 강의 코드 (LEC_SERIAL)

    @NotNull(message = "회차 번호는 필수입니다")
    @Min(value = 1, message = "회차 번호는 1 이상이어야 합니다")
    @Max(value = 80, message = "회차 번호는 80 이하여야 합니다")
    private Integer sessionNumber;      // 회차 번호 (1~80)

    @NotEmpty(message = "출석 승인 레코드는 최소 1개 이상이어야 합니다")
    @Valid
    private List<AttendanceApprovalRecordDto> attendanceRecords;  // 출석 승인 레코드 배열

    // Constructors

    public AttendanceApproveRequestDto() {}

    public AttendanceApproveRequestDto(String lecSerial, Integer sessionNumber, 
                                       List<AttendanceApprovalRecordDto> attendanceRecords) {
        this.lecSerial = lecSerial;
        this.sessionNumber = sessionNumber;
        this.attendanceRecords = attendanceRecords;
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

    public List<AttendanceApprovalRecordDto> getAttendanceRecords() {
        return attendanceRecords;
    }

    public void setAttendanceRecords(List<AttendanceApprovalRecordDto> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    @Override
    public String toString() {
        return "AttendanceApproveRequestDto{" +
                "lecSerial='" + lecSerial + '\'' +
                ", sessionNumber=" + sessionNumber +
                ", attendanceRecords=" + attendanceRecords +
                '}';
    }
}
