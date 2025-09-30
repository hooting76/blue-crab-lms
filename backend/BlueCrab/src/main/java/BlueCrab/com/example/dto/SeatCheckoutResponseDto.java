package BlueCrab.com.example.dto;

/**
 * 퇴실 처리 응답 데이터를 전달하는 DTO 클래스
 * 퇴실 API의 응답 데이터로 사용
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-09-29
 */
public class SeatCheckoutResponseDto {
    
    /**
     * 퇴실한 좌석 번호
     */
    private Integer seatNumber;
    
    /**
     * 실제 사용 시간 (분)
     */
    private Long usageTime;
    
    // 생성자
    public SeatCheckoutResponseDto() {}
    
    public SeatCheckoutResponseDto(Integer seatNumber, Long usageTime) {
        this.seatNumber = seatNumber;
        this.usageTime = usageTime;
    }
    
    // Getter와 Setter
    public Integer getSeatNumber() {
        return seatNumber;
    }
    
    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
    
    public Long getUsageTime() {
        return usageTime;
    }
    
    public void setUsageTime(Long usageTime) {
        this.usageTime = usageTime;
    }
    
    @Override
    public String toString() {
        return "SeatCheckoutResponseDto{" +
                "seatNumber=" + seatNumber +
                ", usageTime=" + usageTime +
                '}';
    }
}