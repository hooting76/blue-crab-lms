package BlueCrab.com.example.dto.Lecture.Attendance;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 출석 세션 정보 DTO
 * ENROLLMENT_DATA.attendance.sessions[] 배열 요소
 * 
 * 확정된 출석 기록 (출/지/결)
 */
public class AttendanceSessionDto {

    private Integer sessionNumber;                          // 회차 번호 (1~80)
    private String status;                                  // 출석 상태 (출/지/결)
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestDate;                      // 요청 일시
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedDate;                     // 승인 일시
    
    private Integer approvedBy;                             // 승인자 USER_IDX (교수)
    private Boolean tempApproved;                           // 자동승인 여부 (true: 자동, false: 수동)

    // Constructors

    public AttendanceSessionDto() {}

    public AttendanceSessionDto(Integer sessionNumber, String status, LocalDateTime requestDate, 
                                LocalDateTime approvedDate, Integer approvedBy, Boolean tempApproved) {
        this.sessionNumber = sessionNumber;
        this.status = status;
        this.requestDate = requestDate;
        this.approvedDate = approvedDate;
        this.approvedBy = approvedBy;
        this.tempApproved = tempApproved;
    }

    // Getters and Setters

    public Integer getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(Integer sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public Integer getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Integer approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Boolean getTempApproved() {
        return tempApproved;
    }

    public void setTempApproved(Boolean tempApproved) {
        this.tempApproved = tempApproved;
    }

    @Override
    public String toString() {
        return "AttendanceSessionDto{" +
                "sessionNumber=" + sessionNumber +
                ", status='" + status + '\'' +
                ", requestDate=" + requestDate +
                ", approvedDate=" + approvedDate +
                ", approvedBy=" + approvedBy +
                ", tempApproved=" + tempApproved +
                '}';
    }
}
