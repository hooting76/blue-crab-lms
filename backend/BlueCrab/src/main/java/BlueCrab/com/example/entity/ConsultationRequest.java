package BlueCrab.com.example.entity;

import BlueCrab.com.example.enums.ConsultationType;
import BlueCrab.com.example.enums.RequestStatus;
import BlueCrab.com.example.enums.ConsultationStatus;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 상담 요청 및 진행 정보를 저장하는 JPA 엔티티 클래스
 * CONSULTATION_REQUEST_TBL 테이블과 매핑
 * v2.0: 단일 테이블 통합 전략 (요청 + 진행)
 */
@Entity
@Table(name = "CONSULTATION_REQUEST_TBL")
public class ConsultationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_idx")
    private Long requestIdx;

    // [요청 정보]
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

    // [요청 처리]
    @Column(name = "request_status", nullable = false, length = 20)
    private String requestStatus = "PENDING";

    @Column(name = "accept_message", length = 500)
    private String acceptMessage;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    // [상담 진행]
    @Column(name = "consultation_status", length = 20)
    private String consultationStatus;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    // [읽음 처리]
    @Column(name = "last_read_time_student")
    private LocalDateTime lastReadTimeStudent;

    @Column(name = "last_read_time_professor")
    private LocalDateTime lastReadTimeProfessor;

    // [메모]
    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    // [타임스탬프]
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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

    // Constructors
    public ConsultationRequest() {}

    // Getters and Setters
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

    public String getConsultationType() {
        return consultationType;
    }

    public void setConsultationType(String consultationType) {
        this.consultationType = consultationType;
    }

    public ConsultationType getConsultationTypeEnum() {
        return ConsultationType.fromDbValue(this.consultationType);
    }

    public void setConsultationTypeEnum(ConsultationType consultationType) {
        this.consultationType = consultationType != null ? consultationType.toDbValue() : null;
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

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public RequestStatus getRequestStatusEnum() {
        return RequestStatus.fromDbValue(this.requestStatus);
    }

    public void setRequestStatusEnum(RequestStatus requestStatus) {
        this.requestStatus = requestStatus != null ? requestStatus.toDbValue() : null;
    }

    public String getAcceptMessage() {
        return acceptMessage;
    }

    public void setAcceptMessage(String acceptMessage) {
        this.acceptMessage = acceptMessage;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getConsultationStatus() {
        return consultationStatus;
    }

    public void setConsultationStatus(String consultationStatus) {
        this.consultationStatus = consultationStatus;
    }

    public ConsultationStatus getConsultationStatusEnum() {
        return ConsultationStatus.fromDbValue(this.consultationStatus);
    }

    public void setConsultationStatusEnum(ConsultationStatus consultationStatus) {
        this.consultationStatus = consultationStatus != null ? consultationStatus.toDbValue() : null;
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

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
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
