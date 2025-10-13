package BlueCrab.com.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 시설의 일일 예약 스케줄 응답 DTO
 * 하루 전체의 시간대별 예약 상태를 제공
 */
public class DailyScheduleDto {

    private Integer facilityIdx;
    private String facilityName;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    
    private List<TimeSlotStatus> slots;

    public DailyScheduleDto() {}

    public DailyScheduleDto(Integer facilityIdx, String facilityName, LocalDate date, List<TimeSlotStatus> slots) {
        this.facilityIdx = facilityIdx;
        this.facilityName = facilityName;
        this.date = date;
        this.slots = slots;
    }

    /**
     * 시간대별 상태 정보
     */
    public static class TimeSlotStatus {
        private String time;  // "09:00" 형식
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;
        
        private String status;  // "available", "reserved", "pending", "blocked"
        private String statusMessage;  // 상태 설명 메시지

        public TimeSlotStatus() {}

        public TimeSlotStatus(String time, LocalDateTime startTime, LocalDateTime endTime, String status, String statusMessage) {
            this.time = time;
            this.startTime = startTime;
            this.endTime = endTime;
            this.status = status;
            this.statusMessage = statusMessage;
        }

        // Getters and Setters
        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public void setStatusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
        }
    }

    // Getters and Setters
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<TimeSlotStatus> getSlots() {
        return slots;
    }

    public void setSlots(List<TimeSlotStatus> slots) {
        this.slots = slots;
    }
}
