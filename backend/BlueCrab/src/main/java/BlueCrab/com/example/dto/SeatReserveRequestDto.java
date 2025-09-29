package BlueCrab.com.example.dto;

/**
 * 좌석 신청 요청 데이터를 전달하는 DTO 클래스
 * 좌석 예약 API의 요청 데이터로 사용
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-09-29
 */
public class SeatReserveRequestDto {
    
    /**
     * 신청할 좌석 번호 (1~80)
     */
    private Integer seatNumber;
    
    // 생성자
    public SeatReserveRequestDto() {}
    
    public SeatReserveRequestDto(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
    
    // Getter와 Setter
    public Integer getSeatNumber() {
        return seatNumber;
    }
    
    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
    
    @Override
    public String toString() {
        return "SeatReserveRequestDto{" +
                "seatNumber=" + seatNumber +
                '}';
    }
}