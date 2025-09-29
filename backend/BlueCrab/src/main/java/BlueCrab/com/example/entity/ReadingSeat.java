package BlueCrab.com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 열람실 좌석 현황을 저장하는 JPA 엔티티 클래스
 * READING_SEAT 테이블과 매핑되며, 실시간 좌석 상태 관리에 사용
 *
 * 주요 기능:
 * - 좌석별 사용 현황 관리 (사용중/빈자리)
 * - 사용자별 좌석 할당 정보
 * - 사용 시간 관리 (시작시간, 종료예정시간)
 *
 * 비즈니스 룰:
 * - 1인 1좌석 제한
 * - 기본 사용시간: 2시간
 * - 자동 퇴실: 종료시간 경과 시
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Entity
@Table(name = "LAMP_TBL")
public class ReadingSeat {
    
    /**
     * 좌석 번호 (1~80)
     * 기본키로 사용되며, 물리적 좌석 번호와 일치
     */
    @Id
    @Column(name = "LAMP_IDX")
    private Integer seatNumber;
    
    /**
     * 좌석 사용 여부
     * 0: 빈자리, 1: 사용중
     */
    @Column(name = "LAMP_ON", nullable = false)
    private Integer isOccupied = 0;
    
    /**
     * 현재 사용자의 학번/교번
     * UserTbl의 userCode와 연결 (외래키)
     * 빈자리일 경우 null
     */
    @Column(name = "USER_CODE", length = 255)
    private String userCode;
    
    /**
     * 사용 시작 시간
     * 좌석 신청 완료 시점
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    /**
     * 예정 종료 시간
     * 시작시간 + 2시간
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    /**
     * 최종 업데이트 시간
     * 좌석 상태 변경 시마다 갱신
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 생성자
    public ReadingSeat() {}
    
    public ReadingSeat(Integer seatNumber) {
        this.seatNumber = seatNumber;
        this.isOccupied = 0;
        this.updatedAt = LocalDateTime.now();
    }
    
    // JPA 콜백 메서드
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getter와 Setter
    public Integer getSeatNumber() {
        return seatNumber;
    }
    
    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
    
    public Integer getIsOccupied() {
        return isOccupied;
    }
    
    public void setIsOccupied(Integer isOccupied) {
        this.isOccupied = isOccupied;
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
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 좌석을 예약합니다.
     * @param userCode 사용자 학번/교번
     */
    public void reserve(String userCode) {
        this.isOccupied = 1;
        this.userCode = userCode;
        this.startTime = LocalDateTime.now();
        this.endTime = this.startTime.plusHours(2);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 좌석을 해제합니다.
     */
    public void release() {
        this.isOccupied = 0;
        this.userCode = null;
        this.startTime = null;
        this.endTime = null;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 사용시간이 만료되었는지 확인합니다.
     * @return true: 만료됨, false: 아직 사용 가능
     */
    public boolean isExpired() {
        return endTime != null && LocalDateTime.now().isAfter(endTime);
    }
    
    @Override
    public String toString() {
        return "ReadingSeat{" +
                "seatNumber=" + seatNumber +
                ", isOccupied=" + isOccupied +
                ", userCode='" + userCode + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", updatedAt=" + updatedAt +
                '}';
    }
}