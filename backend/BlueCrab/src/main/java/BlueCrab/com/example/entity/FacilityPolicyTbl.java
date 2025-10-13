package BlueCrab.com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 시설별 예약 정책을 저장하는 JPA 엔티티 클래스
 * FACILITY_POLICY_TBL 테이블과 매핑
 *
 * NULL 값은 글로벌 기본값 사용을 의미함:
 * - MIN_DURATION_MINUTES: NULL → 30분 기본값
 * - MAX_DURATION_MINUTES: NULL → 480분 기본값
 * - MIN_DAYS_IN_ADVANCE: NULL → 0일 (즉시 예약 가능)
 * - MAX_DAYS_IN_ADVANCE: NULL → 30일 기본값
 * - CANCELLATION_DEADLINE_HOURS: NULL → 24시간 기본값
 * - MAX_RESERVATIONS_PER_USER: NULL → 제한없음
 * - ALLOW_WEEKEND_BOOKING: NULL → true (주말 예약 허용)
 */
@Entity
@Table(name = "FACILITY_POLICY_TBL")
public class FacilityPolicyTbl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "POLICY_IDX")
    private Integer policyIdx;

    @Column(name = "FACILITY_IDX", nullable = false, unique = true)
    private Integer facilityIdx;

    @Column(name = "REQUIRES_APPROVAL", nullable = false)
    private Boolean requiresApproval = true;

    @Column(name = "MIN_DURATION_MINUTES")
    private Integer minDurationMinutes;

    @Column(name = "MAX_DURATION_MINUTES")
    private Integer maxDurationMinutes;

    @Column(name = "MIN_DAYS_IN_ADVANCE")
    private Integer minDaysInAdvance;

    @Column(name = "MAX_DAYS_IN_ADVANCE")
    private Integer maxDaysInAdvance;

    @Column(name = "CANCELLATION_DEADLINE_HOURS")
    private Integer cancellationDeadlineHours;

    @Column(name = "MAX_RESERVATIONS_PER_USER")
    private Integer maxReservationsPerUser;

    @Column(name = "ALLOW_WEEKEND_BOOKING")
    private Boolean allowWeekendBooking;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public FacilityPolicyTbl() {}

    // Getters and Setters

    public Integer getPolicyIdx() {
        return policyIdx;
    }

    public void setPolicyIdx(Integer policyIdx) {
        this.policyIdx = policyIdx;
    }

    public Integer getFacilityIdx() {
        return facilityIdx;
    }

    public void setFacilityIdx(Integer facilityIdx) {
        this.facilityIdx = facilityIdx;
    }

    public Boolean getRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(Boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public Integer getMinDurationMinutes() {
        return minDurationMinutes;
    }

    public void setMinDurationMinutes(Integer minDurationMinutes) {
        this.minDurationMinutes = minDurationMinutes;
    }

    public Integer getMaxDurationMinutes() {
        return maxDurationMinutes;
    }

    public void setMaxDurationMinutes(Integer maxDurationMinutes) {
        this.maxDurationMinutes = maxDurationMinutes;
    }

    public Integer getMinDaysInAdvance() {
        return minDaysInAdvance;
    }

    public void setMinDaysInAdvance(Integer minDaysInAdvance) {
        this.minDaysInAdvance = minDaysInAdvance;
    }

    public Integer getMaxDaysInAdvance() {
        return maxDaysInAdvance;
    }

    public void setMaxDaysInAdvance(Integer maxDaysInAdvance) {
        this.maxDaysInAdvance = maxDaysInAdvance;
    }

    public Integer getCancellationDeadlineHours() {
        return cancellationDeadlineHours;
    }

    public void setCancellationDeadlineHours(Integer cancellationDeadlineHours) {
        this.cancellationDeadlineHours = cancellationDeadlineHours;
    }

    public Integer getMaxReservationsPerUser() {
        return maxReservationsPerUser;
    }

    public void setMaxReservationsPerUser(Integer maxReservationsPerUser) {
        this.maxReservationsPerUser = maxReservationsPerUser;
    }

    public Boolean getAllowWeekendBooking() {
        return allowWeekendBooking;
    }

    public void setAllowWeekendBooking(Boolean allowWeekendBooking) {
        this.allowWeekendBooking = allowWeekendBooking;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 최소 예약 시간(분) - NULL이면 글로벌 기본값 30분 사용
     */
    public int getEffectiveMinDurationMinutes() {
        return minDurationMinutes != null ? minDurationMinutes : 30;
    }

    /**
     * 최대 예약 시간(분) - NULL이면 글로벌 기본값 480분(8시간) 사용
     */
    public int getEffectiveMaxDurationMinutes() {
        return maxDurationMinutes != null ? maxDurationMinutes : 480;
    }

    /**
     * 최소 사전 예약 일수 - NULL이면 즉시 예약 가능 (0일)
     * 예: 3이면 최소 3일 전까지만 예약 가능 (당일/1일전/2일전 불가)
     */
    public int getEffectiveMinDaysInAdvance() {
        return minDaysInAdvance != null ? minDaysInAdvance : 0;
    }

    /**
     * 최대 사전 예약 일수 - NULL이면 글로벌 기본값 30일 사용
     */
    public int getEffectiveMaxDaysInAdvance() {
        return maxDaysInAdvance != null ? maxDaysInAdvance : 30;
    }

    /**
     * 취소 마감 시간(시간) - NULL이면 글로벌 기본값 24시간 사용
     */
    public int getEffectiveCancellationDeadlineHours() {
        return cancellationDeadlineHours != null ? cancellationDeadlineHours : 24;
    }

    /**
     * 사용자당 최대 예약 수 - NULL이면 제한없음
     */
    public Integer getEffectiveMaxReservationsPerUser() {
        return maxReservationsPerUser;
    }

    /**
     * 주말 예약 허용 여부 - NULL이면 글로벌 기본값 true(허용) 사용
     */
    public boolean isEffectiveAllowWeekendBooking() {
        return allowWeekendBooking != null ? allowWeekendBooking : true;
    }
}
