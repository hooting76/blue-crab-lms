package BlueCrab.com.example.entity;

import BlueCrab.com.example.enums.ReservationStatus;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 시설 예약 정보를 저장하는 JPA 엔티티 클래스
 * FACILITY_RESERVATION_TBL 테이블과 매핑
 */
@Entity
@Table(name = "FACILITY_RESERVATION_TBL")
public class FacilityReservationTbl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RESERVATION_IDX")
    private Integer reservationIdx;

    @Column(name = "FACILITY_IDX", nullable = false)
    private Integer facilityIdx;

    @Column(name = "USER_CODE", nullable = false, length = 50)
    private String userCode;

    @Column(name = "USER_EMAIL", length = 255)
    private String userEmail;

    @Column(name = "START_TIME", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "END_TIME", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "PARTY_SIZE")
    private Integer partySize;

    @Column(name = "PURPOSE", columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "REQUESTED_EQUIPMENT", columnDefinition = "TEXT")
    private String requestedEquipment;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "ADMIN_NOTE", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "REJECTION_REASON", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "APPROVED_BY", length = 50)
    private String approvedBy;

    @Column(name = "APPROVED_AT")
    private LocalDateTime approvedAt;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public FacilityReservationTbl() {}

    public Integer getReservationIdx() {
        return reservationIdx;
    }

    public void setReservationIdx(Integer reservationIdx) {
        this.reservationIdx = reservationIdx;
    }

    public Integer getFacilityIdx() {
        return facilityIdx;
    }

    public void setFacilityIdx(Integer facilityIdx) {
        this.facilityIdx = facilityIdx;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getPartySize() {
        return partySize;
    }

    public void setPartySize(Integer partySize) {
        this.partySize = partySize;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getRequestedEquipment() {
        return requestedEquipment;
    }

    public void setRequestedEquipment(String requestedEquipment) {
        this.requestedEquipment = requestedEquipment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ReservationStatus getStatusEnum() {
        return ReservationStatus.fromDbValue(this.status);
    }

    public void setStatusEnum(ReservationStatus status) {
        this.status = status != null ? status.toDbValue() : null;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
