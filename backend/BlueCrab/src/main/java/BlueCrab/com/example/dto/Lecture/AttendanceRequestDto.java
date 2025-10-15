package BlueCrab.com.example.dto.Lecture;

import BlueCrab.com.example.entity.Lecture.AttendanceRequestTbl;

import java.time.LocalDateTime;

/**
 * 출석 인정 요청 DTO
 * 학생의 출석 인정 신청 및 교수의 승인/반려 처리에 사용
 */
public class AttendanceRequestDto {

    private Long requestIdx;            // 요청 IDX
    private Integer enrollmentIdx;      // 수강신청 IDX
    private Integer sessionNumber;      // 회차 번호 (1~80)
    private String requestReason;       // 신청 사유
    private String approvalStatus;      // 승인 상태 (PENDING/APPROVED/REJECTED)
    private Integer approvedBy;         // 승인자 IDX
    private LocalDateTime approvedAt;   // 승인 일시
    private String rejectReason;        // 반려 사유
    private LocalDateTime requestedAt;  // 신청 일시

    // 학생 정보 (조인 시)
    private String studentName;         // 학생 이름
    private String studentId;           // 학번

    public AttendanceRequestDto() {}

    // Entity → DTO 변환
    public AttendanceRequestDto(AttendanceRequestTbl entity) {
        this.requestIdx = entity.getRequestIdx();
        this.enrollmentIdx = entity.getEnrollmentIdx();
        this.sessionNumber = entity.getSessionNumber();
        this.requestReason = entity.getRequestReason();
        this.approvalStatus = entity.getApprovalStatus();
        this.approvedBy = entity.getApprovedBy();
        this.approvedAt = entity.getApprovedAt();
        this.rejectReason = entity.getRejectReason();
        this.requestedAt = entity.getRequestedAt();
    }

    // Getters and Setters

    public Long getRequestIdx() {
        return requestIdx;
    }

    public void setRequestIdx(Long requestIdx) {
        this.requestIdx = requestIdx;
    }

    public Integer getEnrollmentIdx() {
        return enrollmentIdx;
    }

    public void setEnrollmentIdx(Integer enrollmentIdx) {
        this.enrollmentIdx = enrollmentIdx;
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

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
