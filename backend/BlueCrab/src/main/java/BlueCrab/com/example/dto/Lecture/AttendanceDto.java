// 작성자: 성태준

package BlueCrab.com.example.dto.Lecture;

/**
 * 출결 정보 전송을 위한 DTO 클래스
 * 출결 조회, 출결 체크 시 사용
 */
public class AttendanceDto {

    private String date;                // 출결 날짜 (YYYY-MM-DD)
    private String status;              // 출결 상태 (PRESENT, LATE, ABSENT, EXCUSED)
    private String requestReason;       // 신청 사유 (예: 병원 진료)
    private String approvalStatus;      // 승인 상태 (PENDING, APPROVED, REJECTED)
    private Integer approvedBy;         // 승인자 IDX
    private String approvedAt;          // 승인 일시

    public AttendanceDto() {}

    public AttendanceDto(String date, String status) {
        this.date = date;
        this.status = status;
    }

    public AttendanceDto(String date, String status, String requestReason, String approvalStatus,
                         Integer approvedBy, String approvedAt) {
        this.date = date;
        this.status = status;
        this.requestReason = requestReason;
        this.approvalStatus = approvalStatus;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
    }

    // Getters and Setters

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestReason() {
        return requestReason;
    }

    public void setRequestReason(String requestReason) {
        this.requestReason = requestReason;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Integer getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Integer approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(String approvedAt) {
        this.approvedAt = approvedAt;
    }
}
