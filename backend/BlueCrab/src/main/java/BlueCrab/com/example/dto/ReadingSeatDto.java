package BlueCrab.com.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 열람실 좌석 정보를 전달하는 DTO 클래스
 * API 응답에서 좌석별 상태 정보를 담는데 사용
 *
 * 주요 용도:
 * - 좌석 현황 조회 API 응답
 * - 좌석 상태 변경 시 응답 데이터
 * - 프론트엔드에서 좌석 표시용 데이터
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-09-29
 */
public class ReadingSeatDto {
    
    /**
     * 좌석 번호 (1~80)
     */
    private Integer seatNumber;
    
    /**
     * 좌석 사용 여부
     * false: 예약 가능, true: 사용 중
     */
    private Boolean isOccupied;
    
    /**
     * 예정 종료 시간
     * 사용 중인 좌석만 값이 있음
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 남은 시간 (분)
     * 사용 중인 좌석의 잔여 시간
     */
    private Long remainingMinutes;
    
    // 생성자
    public ReadingSeatDto() {}
    
    public ReadingSeatDto(Integer seatNumber, Boolean isOccupied, LocalDateTime endTime) {
        this.seatNumber = seatNumber;
        this.isOccupied = isOccupied;
        this.endTime = endTime;
        this.remainingMinutes = calculateRemainingMinutes(endTime);
    }
    
    // Getter와 Setter
    public Integer getSeatNumber() {
        return seatNumber;
    }
    
    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
    
    public Boolean getIsOccupied() {
        return isOccupied;
    }
    
    public void setIsOccupied(Boolean isOccupied) {
        this.isOccupied = isOccupied;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        this.remainingMinutes = calculateRemainingMinutes(endTime);
    }
    
    public Long getRemainingMinutes() {
        return remainingMinutes;
    }
    
    public void setRemainingMinutes(Long remainingMinutes) {
        this.remainingMinutes = remainingMinutes;
    }
    
    /**
     * 남은 시간을 계산합니다.
     * @param endTime 종료 예정 시간
     * @return 남은 시간 (분), 종료시간이 null이거나 지났으면 0
     */
    private Long calculateRemainingMinutes(LocalDateTime endTime) {
        if (endTime == null) {
            return null;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            return 0L;
        }
        
        return java.time.Duration.between(now, endTime).toMinutes();
    }
    
    @Override
    public String toString() {
        return "ReadingSeatDto{" +
                "seatNumber=" + seatNumber +
                ", isOccupied=" + isOccupied +
                ", endTime=" + endTime +
                ", remainingMinutes=" + remainingMinutes +
                '}';
    }
}