package BlueCrab.com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 열람실 사용 기록을 저장하는 JPA 엔티티 클래스
 * READING_USAGE_LOG 테이블과 매핑되며, 사용자별 열람실 이용 이력 관리에 사용
 *
 * 주요 기능:
 * - 좌석 사용 이력 기록
 * - 입실/퇴실 시간 추적
 * - 사용자별 이용 통계 제공
 *
 * 비즈니스 룰:
 * - 입실 시 기록 생성 (endTime = null)
 * - 퇴실 시 endTime 업데이트
 * - 자동 퇴실 시에도 기록 완료
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Entity
@Table(name = "LAMP_USAGE_LOG")
public class ReadingUsageLog {
    
    /**
     * 기록 고유 식별자
     * 자동 생성되는 기본키
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;
    
    /**
     * 사용한 좌석 번호
     * ReadingSeat의 seatNumber와 연결
     */
    @Column(name = "lamp_idx", nullable = false)
    private Integer seatNumber;
    
    /**
     * 사용자 학번/교번
     * UserTbl의 userCode와 연결 (외래키)
     */
    @Column(name = "user_code", nullable = false, length = 255)
    private String userCode;
    
    /**
     * 입실 시간
     * 좌석 신청 완료 시점
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    /**
     * 퇴실 시간
     * null: 사용중, 값 있음: 퇴실 완료
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    /**
     * 기록 생성 시간
     * 입실 시점의 타임스탬프
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // 생성자
    public ReadingUsageLog() {}
    
    public ReadingUsageLog(Integer seatNumber, String userCode) {
        this.seatNumber = seatNumber;
        this.userCode = userCode;
        this.startTime = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }
    
    // JPA 콜백 메서드
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.startTime == null) {
            this.startTime = LocalDateTime.now();
        }
    }
    
    // Getter와 Setter
    public Long getLogId() {
        return logId;
    }
    
    public void setLogId(Long logId) {
        this.logId = logId;
    }
    
    public Integer getSeatNumber() {
        return seatNumber;
    }
    
    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
    
    public String getUserCode() {
        return userCode;
    }
    
    public void setUserCode(String userCode) {
        this.userCode = userCode;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 퇴실 처리를 합니다.
     */
    public void checkout() {
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 실제 사용 시간을 분 단위로 계산합니다.
     * @return 사용 시간 (분), 아직 사용중이면 현재까지의 시간
     */
    public long getUsageTimeInMinutes() {
        LocalDateTime end = (endTime != null) ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMinutes();
    }
    
    /**
     * 현재 사용 중인지 확인합니다.
     * @return true: 사용중, false: 퇴실 완료
     */
    public boolean isActive() {
        return endTime == null;
    }
    
    @Override
    public String toString() {
        return "ReadingUsageLog{" +
                "logId=" + logId +
                ", seatNumber=" + seatNumber +
                ", userCode='" + userCode + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", createdAt=" + createdAt +
                '}';
    }
}