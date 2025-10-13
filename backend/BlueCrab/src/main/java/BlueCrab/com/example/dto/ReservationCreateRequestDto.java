package BlueCrab.com.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.Future;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 예약 생성 요청을 위한 DTO 클래스
 */
public class ReservationCreateRequestDto {

    @NotNull(message = "시설 ID는 필수 입력 항목입니다.")
    private Integer facilityIdx;

    @NotNull(message = "시작 시간은 필수 입력 항목입니다.")
    @Future(message = "시작 시간은 현재 시간 이후여야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "종료 시간은 필수 입력 항목입니다.")
    @Future(message = "종료 시간은 현재 시간 이후여야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @NotNull(message = "인원수는 필수 입력 항목입니다.")
    @Min(value = 1, message = "인원수는 최소 1명 이상이어야 합니다.")
    private Integer partySize;

    private String purpose;
    private String requestedEquipment;

    public ReservationCreateRequestDto() {}

    public ReservationCreateRequestDto(Integer facilityIdx, LocalDateTime startTime, LocalDateTime endTime,
                                        Integer partySize, String purpose, String requestedEquipment) {
        this.facilityIdx = facilityIdx;
        this.startTime = startTime;
        this.endTime = endTime;
        this.partySize = partySize;
        this.purpose = purpose;
        this.requestedEquipment = requestedEquipment;
    }

    public Integer getFacilityIdx() {
        return facilityIdx;
    }

    public void setFacilityIdx(Integer facilityIdx) {
        this.facilityIdx = facilityIdx;
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
}
