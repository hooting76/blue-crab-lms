package BlueCrab.com.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 좌석 신청 응답 데이터를 전달하는 DTO 클래스
 * 좌석 예약 API의 응답 데이터로 사용
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-09-29
 */
public class SeatReserveResponseDto {
    
    /**
     * 예약된 좌석 번호
     */
    private Integer seatNumber;
    
    /**
     * 사용 시작 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * 사용 종료 예정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 사용 가능 시간 (분)
     */
    private Integer usageTimeMinutes;
    
    // 생성자
    public SeatReserveResponseDto() {}
    
    public SeatReserveResponseDto(Integer seatNumber, LocalDateTime startTime, LocalDateTime endTime) {
        this.seatNumber = seatNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.usageTimeMinutes = calculateUsageTime(startTime, endTime);
    }
    
    // Getter와 Setter
    public Integer getSeatNumber() {
        return seatNumber;
    }
    
    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        this.usageTimeMinutes = calculateUsageTime(startTime, endTime);
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        this.usageTimeMinutes = calculateUsageTime(startTime, endTime);
    }
    
    public Integer getUsageTimeMinutes() {
        return usageTimeMinutes;
    }
    
    public void setUsageTimeMinutes(Integer usageTimeMinutes) {
        this.usageTimeMinutes = usageTimeMinutes;
    }
    
    /**
     * 사용 시간을 분 단위로 계산합니다.
     * @param start 시작 시간
     * @param end 종료 시간
     * @return 사용 시간 (분)
     */
    private Integer calculateUsageTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return (int) java.time.Duration.between(start, end).toMinutes();
    }
    
    @Override
    public String toString() {
        return "SeatReserveResponseDto{" +
                "seatNumber=" + seatNumber +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", usageTimeMinutes=" + usageTimeMinutes +
                '}';
    }
}