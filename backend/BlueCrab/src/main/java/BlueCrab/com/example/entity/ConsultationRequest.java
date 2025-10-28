package BlueCrab.com.example.entity;

import BlueCrab.com.example.enums.ConsultationStatus;
import BlueCrab.com.example.enums.ConsultationType;
import BlueCrab.com.example.enums.RequestStatus;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 상담 요청 및 진행 정보를 저장하는 JPA 엔티티 클래스.
 * <p>
 * v2 리팩토링에서는 단일 status 필드를 도입했으나,
 * 기존 서비스/컨트롤러 코드와의 하위 호환을 위해 requestStatus / consultationStatus 등의
 * 레거시 필드를 유지하며 상호 변환 로직을 제공한다.
 */
@Entity
@Table(name = "CONSULTATION_REQUEST_TBL")
public class ConsultationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_idx")
    private Long requestIdx;

    // ===== 요청 정보 =====
    @Column(name = "requester_user_code", nullable = false, length = 20)
    private String requesterUserCode;

    @Column(name = "recipient_user_code", nullable = false, length = 20)
    private String recipientUserCode;

    @Column(name = "consultation_type", nullable = false, length = 50)
    private String consultationType;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", length = 1000)
    private String content;

    @Column(name = "desired_date")
    private LocalDateTime desiredDate;

    // ===== 통합 상태 =====
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "status_changed_by", length = 20)
    private String statusChangedBy;

    // ===== 레거시 상태 (하위 호환용) =====
    @Deprecated
    @Column(name = "request_status", length = 20)
    private String requestStatus = "PENDING";

    @Deprecated
    @Column(name = "consultation_status", length = 20)
    private String consultationStatus;

    @Deprecated
    @Column(name = "accept_message", length = 500)
    private String acceptMessage;

    @Deprecated
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Deprecated
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Deprecated
    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    // ===== 진행 정보 =====
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    // ===== 읽음 정보 =====
    @Column(name = "last_read_time_student")
    private LocalDateTime lastReadTimeStudent;

    @Column(name = "last_read_time_professor")
    private LocalDateTime lastReadTimeProfessor;

    // ===== 타임스탬프 =====
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== Lifecycle =====
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = "PENDING";
        }
        if (this.statusChangedAt == null) {
            this.statusChangedAt = now;
        }
        syncLegacyStatusFromUnified(this.status);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PostLoad
    private void onLoad() {
        syncUnifiedStatusFromLegacy();
    }

    // ===== Constructors =====
    public ConsultationRequest() {}

    // ===== 기본 Getter/Setter =====
    public Long getRequestIdx() {
        return requestIdx;
    }

    public void setRequestIdx(Long requestIdx) {
        this.requestIdx = requestIdx;
    }

    public String getRequesterUserCode() {
        return requesterUserCode;
    }

    public void setRequesterUserCode(String requesterUserCode) {
        this.requesterUserCode = requesterUserCode;
    }

    public String getRecipientUserCode() {
        return recipientUserCode;
    }

    public void setRecipientUserCode(String recipientUserCode) {
        this.recipientUserCode = recipientUserCode;
    }

    public ConsultationType getConsultationTypeEnum() {
        return ConsultationType.fromDbValue(this.consultationType);
    }

    public void setConsultationTypeEnum(ConsultationType consultationType) {
        this.consultationType = consultationType != null ? consultationType.toDbValue() : null;
    }

    public String getConsultationType() {
        return consultationType;
    }

    public void setConsultationType(String consultationType) {
        this.consultationType = consultationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getDesiredDate() {
        return desiredDate;
    }

    public void setDesiredDate(LocalDateTime desiredDate) {
        this.desiredDate = desiredDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        syncLegacyStatusFromUnified(status);
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
        if (statusReason == null) {
            this.acceptMessage = null;
            this.rejectionReason = null;
            this.cancelReason = null;
            return;
        }
        switch (this.status) {
            case "APPROVED":
                this.acceptMessage = statusReason;
                this.rejectionReason = null;
                this.cancelReason = null;
                break;
            case "REJECTED":
                this.rejectionReason = statusReason;
                this.acceptMessage = null;
                this.cancelReason = null;
                break;
            case "CANCELLED":
                this.cancelReason = statusReason;
                this.acceptMessage = null;
                this.rejectionReason = null;
                break;
            default:
                break;
        }
    }

    public LocalDateTime getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(LocalDateTime statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    public String getStatusChangedBy() {
        return statusChangedBy;
    }

    public void setStatusChangedBy(String statusChangedBy) {
        this.statusChangedBy = statusChangedBy;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public LocalDateTime getLastReadTimeStudent() {
        return lastReadTimeStudent;
    }

    public void setLastReadTimeStudent(LocalDateTime lastReadTimeStudent) {
        this.lastReadTimeStudent = lastReadTimeStudent;
    }

    public LocalDateTime getLastReadTimeProfessor() {
        return lastReadTimeProfessor;
    }

    public void setLastReadTimeProfessor(LocalDateTime lastReadTimeProfessor) {
        this.lastReadTimeProfessor = lastReadTimeProfessor;
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

    // ===== 하위 호환 메서드 =====
    @Deprecated
    public String getRequestStatus() {
        return this.requestStatus != null ? this.requestStatus : this.status;
    }

    @Deprecated
    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
        syncUnifiedStatusFromLegacy();
    }

    @Deprecated
    public RequestStatus getRequestStatusEnum() {
        String value = getRequestStatus();
        return value != null ? RequestStatus.fromDbValue(value) : null;
    }

    @Deprecated
    public void setRequestStatusEnum(RequestStatus requestStatus) {
        setRequestStatus(requestStatus != null ? requestStatus.toDbValue() : null);
    }

    @Deprecated
    public String getConsultationStatus() {
        if (this.consultationStatus != null) {
            return this.consultationStatus;
        }
        return deriveConsultationStatusFromUnified(this.status);
    }

    @Deprecated
    public void setConsultationStatus(String consultationStatus) {
        this.consultationStatus = consultationStatus;
        syncUnifiedStatusFromLegacy();
    }

    @Deprecated
    public ConsultationStatus getConsultationStatusEnum() {
        String value = getConsultationStatus();
        return value != null ? ConsultationStatus.fromDbValue(value) : null;
    }

    @Deprecated
    public void setConsultationStatusEnum(ConsultationStatus consultationStatus) {
        setConsultationStatus(consultationStatus != null ? consultationStatus.toDbValue() : null);
    }

    @Deprecated
    public String getAcceptMessage() {
        return acceptMessage;
    }

    @Deprecated
    public void setAcceptMessage(String acceptMessage) {
        this.acceptMessage = acceptMessage;
        if (acceptMessage != null && ("APPROVED".equals(this.status) || this.status == null)) {
            this.statusReason = acceptMessage;
        }
    }

    @Deprecated
    public String getRejectionReason() {
        return rejectionReason;
    }

    @Deprecated
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
        if (rejectionReason != null && ("REJECTED".equals(this.status) || this.status == null)) {
            this.statusReason = rejectionReason;
        }
    }

    @Deprecated
    public String getCancelReason() {
        return cancelReason;
    }

    @Deprecated
    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
        if (cancelReason != null && ("CANCELLED".equals(this.status) || this.status == null)) {
            this.statusReason = cancelReason;
        }
    }

    @Deprecated
    public String getMemo() {
        return memo;
    }

    @Deprecated
    public void setMemo(String memo) {
        this.memo = memo;
    }

    // ===== Helper =====
    private void syncUnifiedStatusFromLegacy() {
        if (this.requestStatus == null && this.consultationStatus == null) {
            return;
        }

        if (this.requestStatus != null) {
            switch (this.requestStatus) {
                case "APPROVED":
                    if ("IN_PROGRESS".equals(this.consultationStatus)) {
                        this.status = "IN_PROGRESS";
                    } else if ("COMPLETED".equals(this.consultationStatus)) {
                        this.status = "COMPLETED";
                    } else {
                        this.status = "APPROVED";
                    }
                    return;
                case "REJECTED":
                case "CANCELLED":
                case "PENDING":
                    this.status = this.requestStatus;
                    return;
                default:
                    this.status = this.requestStatus;
                    return;
            }
        }

        if (this.consultationStatus != null) {
            switch (this.consultationStatus) {
                case "IN_PROGRESS":
                    this.status = "IN_PROGRESS";
                    break;
                case "COMPLETED":
                    this.status = "COMPLETED";
                    break;
                case "SCHEDULED":
                    this.status = "APPROVED";
                    break;
                default:
                    break;
            }
        }
    }

    private void syncLegacyStatusFromUnified(String unifiedStatus) {
        if (unifiedStatus == null) {
            return;
        }

        switch (unifiedStatus) {
            case "PENDING":
                this.requestStatus = "PENDING";
                this.consultationStatus = null;
                break;
            case "APPROVED":
                this.requestStatus = "APPROVED";
                this.consultationStatus = "SCHEDULED";
                break;
            case "IN_PROGRESS":
                this.requestStatus = "APPROVED";
                this.consultationStatus = "IN_PROGRESS";
                break;
            case "COMPLETED":
                this.requestStatus = "APPROVED";
                this.consultationStatus = "COMPLETED";
                break;
            case "REJECTED":
                this.requestStatus = "REJECTED";
                this.consultationStatus = null;
                break;
            case "CANCELLED":
                this.requestStatus = "CANCELLED";
                this.consultationStatus = null;
                break;
            default:
                this.requestStatus = unifiedStatus;
                break;
        }
    }

    private String deriveConsultationStatusFromUnified(String unifiedStatus) {
        if (unifiedStatus == null) {
            return null;
        }
        switch (unifiedStatus) {
            case "IN_PROGRESS":
                return "IN_PROGRESS";
            case "COMPLETED":
                return "COMPLETED";
            case "APPROVED":
                return "SCHEDULED";
            default:
                return null;
        }
    }

    // ===== Convenience =====
    public boolean isTerminal() {
        return "REJECTED".equals(status) ||
               "CANCELLED".equals(status) ||
               "COMPLETED".equals(status);
    }

    public boolean canStart() {
        return "APPROVED".equals(status);
    }

    public boolean canComplete() {
        return "IN_PROGRESS".equals(status);
    }

    public boolean canCancel() {
        return "PENDING".equals(status) ||
               "APPROVED".equals(status) ||
               "IN_PROGRESS".equals(status);
    }

    @Override
    public String toString() {
        return "ConsultationRequest{" +
            "requestIdx=" + requestIdx +
            ", requesterUserCode='" + requesterUserCode + '\'' +
            ", recipientUserCode='" + recipientUserCode + '\'' +
            ", status='" + status + '\'' +
            ", createdAt=" + createdAt +
            '}';
    }
}
