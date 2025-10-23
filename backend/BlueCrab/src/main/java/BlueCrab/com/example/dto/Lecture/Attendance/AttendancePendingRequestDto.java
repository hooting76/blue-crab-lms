package BlueCrab.com.example.dto.Lecture.Attendance;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 출석 대기 요청 DTO
 * ENROLLMENT_DATA.attendance.pendingRequests[] 배열 요소
 * 
 * 교수 승인 대기 중인 출석 요청
 */
public class AttendancePendingRequestDto {

    private Integer sessionNumber;                          // 회차 번호 (1~80)
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestDate;                      // 요청 일시
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;                        // 만료 일시 (requestDate + 7일)
    
    private Boolean tempApproved;                           // 자동승인 예약 여부

    // Constructors

    public AttendancePendingRequestDto() {}

    public AttendancePendingRequestDto(Integer sessionNumber, LocalDateTime requestDate, 
                                       LocalDateTime expiresAt, Boolean tempApproved) {
        this.sessionNumber = sessionNumber;
        this.requestDate = requestDate;
        this.expiresAt = expiresAt;
        this.tempApproved = tempApproved;
    }

    // Getters and Setters

    public Integer getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(Integer sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getTempApproved() {
        return tempApproved;
    }

    public void setTempApproved(Boolean tempApproved) {
        this.tempApproved = tempApproved;
    }

    @Override
    public String toString() {
        return "AttendancePendingRequestDto{" +
                "sessionNumber=" + sessionNumber +
                ", requestDate=" + requestDate +
                ", expiresAt=" + expiresAt +
                ", tempApproved=" + tempApproved +
                '}';
    }
}
