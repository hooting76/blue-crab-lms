package BlueCrab.com.example.dto.Lecture.Attendance;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 출석 승인 레코드 DTO
 * PUT /api/attendance/approve 요청 시 배열 요소로 사용
 * 
 * 교수가 여러 학생의 출석을 한 번에 승인/반려할 때 사용
 */
public class AttendanceApprovalRecordDto {

    @NotNull(message = "학생 USER_IDX는 필수입니다")
    private Integer studentIdx;         // 학생 USER_IDX

    @NotNull(message = "출석 상태는 필수입니다")
    @Pattern(regexp = "^(출|지|결)$", message = "출석 상태는 '출', '지', '결' 중 하나여야 합니다")
    private String status;              // 출석 상태 (출/지/결)

    private String rejectReason;        // 반려 사유 (결석 처리 시 선택사항)

    // Constructors

    public AttendanceApprovalRecordDto() {}

    public AttendanceApprovalRecordDto(Integer studentIdx, String status) {
        this.studentIdx = studentIdx;
        this.status = status;
    }

    public AttendanceApprovalRecordDto(Integer studentIdx, String status, String rejectReason) {
        this.studentIdx = studentIdx;
        this.status = status;
        this.rejectReason = rejectReason;
    }

    // Getters and Setters

    public Integer getStudentIdx() {
        return studentIdx;
    }

    public void setStudentIdx(Integer studentIdx) {
        this.studentIdx = studentIdx;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    @Override
    public String toString() {
        return "AttendanceApprovalRecordDto{" +
                "studentIdx=" + studentIdx +
                ", status='" + status + '\'' +
                ", rejectReason='" + rejectReason + '\'' +
                '}';
    }
}
