package BlueCrab.com.example.dto;

import java.util.List;

/**
 * 열람실 전체 현황 정보를 전달하는 DTO 클래스
 * 좌석 현황 조회 API의 응답 데이터로 사용
 *
 * 포함 정보:
 * - 전체 좌석 목록 및 상태
 * - 좌석 사용 통계
 * - 시설 정보
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-09-29
 */
public class ReadingRoomStatusDto {
    
    /**
     * 좌석별 상태 정보 목록
     */
    private List<ReadingSeatDto> seats;
    
    /**
     * 전체 좌석 수
     */
    private Integer totalSeats;
    
    /**
     * 사용 가능한 좌석 수
     */
    private Integer availableSeats;
    
    /**
     * 사용 중인 좌석 수
     */
    private Integer occupiedSeats;
    
    /**
     * 사용률 (%)
     */
    private Double occupancyRate;
    
    // 생성자
    public ReadingRoomStatusDto() {}
    
    public ReadingRoomStatusDto(List<ReadingSeatDto> seats, Integer totalSeats, 
                               Integer availableSeats, Integer occupiedSeats) {
        this.seats = seats;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.occupiedSeats = occupiedSeats;
        this.occupancyRate = calculateOccupancyRate(totalSeats, occupiedSeats);
    }
    
    // Getter와 Setter
    public List<ReadingSeatDto> getSeats() {
        return seats;
    }
    
    public void setSeats(List<ReadingSeatDto> seats) {
        this.seats = seats;
    }
    
    public Integer getTotalSeats() {
        return totalSeats;
    }
    
    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
        this.occupancyRate = calculateOccupancyRate(totalSeats, occupiedSeats);
    }
    
    public Integer getAvailableSeats() {
        return availableSeats;
    }
    
    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }
    
    public Integer getOccupiedSeats() {
        return occupiedSeats;
    }
    
    public void setOccupiedSeats(Integer occupiedSeats) {
        this.occupiedSeats = occupiedSeats;
        this.occupancyRate = calculateOccupancyRate(totalSeats, occupiedSeats);
    }
    
    public Double getOccupancyRate() {
        return occupancyRate;
    }
    
    public void setOccupancyRate(Double occupancyRate) {
        this.occupancyRate = occupancyRate;
    }
    
    /**
     * 사용률을 계산합니다.
     * @param total 전체 좌석 수
     * @param occupied 사용 중인 좌석 수
     * @return 사용률 (%), 전체가 0이면 0.0
     */
    private Double calculateOccupancyRate(Integer total, Integer occupied) {
        if (total == null || total == 0 || occupied == null) {
            return 0.0;
        }
        return Math.round((occupied.doubleValue() / total.doubleValue()) * 100 * 100.0) / 100.0;
    }
    
    @Override
    public String toString() {
        return "ReadingRoomStatusDto{" +
                "totalSeats=" + totalSeats +
                ", availableSeats=" + availableSeats +
                ", occupiedSeats=" + occupiedSeats +
                ", occupancyRate=" + occupancyRate +
                ", seatsCount=" + (seats != null ? seats.size() : 0) +
                '}';
    }
}