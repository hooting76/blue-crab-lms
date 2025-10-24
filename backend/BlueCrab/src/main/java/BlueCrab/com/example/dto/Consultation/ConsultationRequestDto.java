package BlueCrab.com.example.dto.Consultation;

import BlueCrab.com.example.enums.ConsultationType;
import BlueCrab.com.example.enums.RequestStatus;
import BlueCrab.com.example.enums.ConsultationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 상담 요청 정보 전송을 위한 DTO
 */
public class ConsultationRequestDto {

    private Long requestIdx;
    private String requesterUserCode;
    private String requesterName;
    private String recipientUserCode;
    private String recipientName;
    private ConsultationType consultationType;
    private String title;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime desiredDate;

    private RequestStatus requestStatus;
    private String acceptMessage;
    private String rejectionReason;
    private String cancelReason;

    private ConsultationStatus consultationStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledStartAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endedAt;

    private Integer durationMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActivityAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private Boolean hasUnreadMessages;

    public ConsultationRequestDto() {}

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

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRecipientUserCode() {
        return recipientUserCode;
    }

    public void setRecipientUserCode(String recipientUserCode) {
        this.recipientUserCode = recipientUserCode;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public ConsultationType getConsultationType() {
        return consultationType;
    }

    public void setConsultationType(ConsultationType consultationType) {
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

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
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

    public ConsultationStatus getConsultationStatus() {
        return consultationStatus;
    }

    public void setConsultationStatus(ConsultationStatus consultationStatus) {
        this.consultationStatus = consultationStatus;
    }

    public LocalDateTime getScheduledStartAt() {
        return scheduledStartAt;
    }

    public void setScheduledStartAt(LocalDateTime scheduledStartAt) {
        this.scheduledStartAt = scheduledStartAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getHasUnreadMessages() {
        return hasUnreadMessages;
    }

    public void setHasUnreadMessages(Boolean hasUnreadMessages) {
        this.hasUnreadMessages = hasUnreadMessages;
    }
}
