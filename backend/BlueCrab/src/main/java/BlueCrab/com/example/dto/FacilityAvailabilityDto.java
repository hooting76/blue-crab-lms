package BlueCrab.com.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시설 가용성 정보를 위한 DTO 클래스
 */
public class FacilityAvailabilityDto {

    private Integer facilityIdx;
    private String facilityName;
    private Boolean isAvailable;
    private List<TimeSlot> conflictingReservations;

    public FacilityAvailabilityDto() {}

    public FacilityAvailabilityDto(Integer facilityIdx, String facilityName, Boolean isAvailable,
                                     List<TimeSlot> conflictingReservations) {
        this.facilityIdx = facilityIdx;
        this.facilityName = facilityName;
        this.isAvailable = isAvailable;
        this.conflictingReservations = conflictingReservations;
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

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public List<TimeSlot> getConflictingReservations() {
        return conflictingReservations;
    }

    public void setConflictingReservations(List<TimeSlot> conflictingReservations) {
        this.conflictingReservations = conflictingReservations;
    }

    public static class TimeSlot {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startTime;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime endTime;

        public TimeSlot() {}

        public TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
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
    }
}
