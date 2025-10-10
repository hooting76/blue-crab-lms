package BlueCrab.com.example.dto;

import BlueCrab.com.example.enums.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 예약 정보 전송을 위한 DTO 클래스
 */
public class ReservationDto {

    private Integer reservationIdx;
    private Integer facilityIdx;
    private String facilityName;
    private String userCode;
    private String userName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    private Integer partySize;
    private String purpose;
    private String requestedEquipment;
    private ReservationStatus status;
    private String adminNote;
    private String rejectionReason;
    private String approvedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime approvedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public ReservationDto() {}

    public ReservationDto(Integer reservationIdx, Integer facilityIdx, String facilityName, String userCode,
                          String userName, LocalDateTime startTime, LocalDateTime endTime, Integer partySize,
                          String purpose, String requestedEquipment, ReservationStatus status, String adminNote,
                          String rejectionReason, String approvedBy, LocalDateTime approvedAt, LocalDateTime createdAt) {
        this.reservationIdx = reservationIdx;
        this.facilityIdx = facilityIdx;
        this.facilityName = facilityName;
        this.userCode = userCode;
        this.userName = userName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.partySize = partySize;
        this.purpose = purpose;
        this.requestedEquipment = requestedEquipment;
        this.status = status;
        this.adminNote = adminNote;
        this.rejectionReason = rejectionReason;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.createdAt = createdAt;
    }

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

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
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
}
