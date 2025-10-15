package BlueCrab.com.example.entity.Lecture;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * ATTENDANCE_REQUEST_TBL - 출석 사유 신청 테이블
 * 
 * 학생이 특정 회차의 결석/지각에 대해 사유를 신청하고
 * 교수가 개별적으로 승인/거부하는 시스템
 * 
 * 특징:
 * - 각 수강신청의 각 회차당 1회만 신청 가능 (UNIQUE 제약)
 * - 승인 시 ENROLLMENT_DATA의 출석 문자열 업데이트 ("결" → "공")
 * - 거부 시 출석 문자열은 그대로 유지
 */
@Entity
@Table(name = "ATTENDANCE_REQUEST_TBL",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_request",
           columnNames = {"ENROLLMENT_IDX", "SESSION_NUMBER"}
       ))
public class AttendanceRequestTbl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REQUEST_IDX")
    private Long requestIdx;

    @Column(name = "ENROLLMENT_IDX", nullable = false)
    private Integer enrollmentIdx;

    @Column(name = "SESSION_NUMBER", nullable = false)
    private Integer sessionNumber; // 1~80

    @Column(name = "REQUEST_REASON", nullable = false, length = 500)
    private String requestReason;

    @Column(name = "REQUESTED_AT", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "APPROVAL_STATUS", nullable = false, length = 20)
    private String approvalStatus = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "APPROVED_BY")
    private Integer approvedBy;

    @Column(name = "APPROVED_AT")
    private LocalDateTime approvedAt;

    @Column(name = "REJECT_REASON", length = 500)
    private String rejectReason;

    // ========== Constructors ==========
    
    public AttendanceRequestTbl() {}

    public AttendanceRequestTbl(Integer enrollmentIdx, Integer sessionNumber, String requestReason) {
        this.enrollmentIdx = enrollmentIdx;
        this.sessionNumber = sessionNumber;
        this.requestReason = requestReason;
        this.requestedAt = LocalDateTime.now();
        this.approvalStatus = "PENDING";
    }

    // ========== Business Methods ==========

    /**
     * 승인 처리
     */
    public void approve(Integer approvedBy) {
        this.approvalStatus = "APPROVED";
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 거부 처리
     */
    public void reject(Integer approvedBy, String rejectReason) {
        this.approvalStatus = "REJECTED";
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
        this.rejectReason = rejectReason;
    }

    /**
     * 대기 중인지 확인
     */
    public boolean isPending() {
        return "PENDING".equals(this.approvalStatus);
    }

    /**
     * 승인되었는지 확인
     */
    public boolean isApproved() {
        return "APPROVED".equals(this.approvalStatus);
    }

    /**
     * 거부되었는지 확인
     */
    public boolean isRejected() {
        return "REJECTED".equals(this.approvalStatus);
    }

    // ========== Getters and Setters ==========

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

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
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

    @Override
    public String toString() {
        return "AttendanceRequestTbl{" +
                "requestIdx=" + requestIdx +
                ", enrollmentIdx=" + enrollmentIdx +
                ", sessionNumber=" + sessionNumber +
                ", approvalStatus='" + approvalStatus + '\'' +
                '}';
    }
}
