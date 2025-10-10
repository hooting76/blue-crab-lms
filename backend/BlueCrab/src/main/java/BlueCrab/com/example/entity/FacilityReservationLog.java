package BlueCrab.com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 시설 예약 로그를 저장하는 JPA 엔티티 클래스
 * FACILITY_RESERVATION_LOG 테이블과 매핑
 */
@Entity
@Table(name = "FACILITY_RESERVATION_LOG")
public class FacilityReservationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOG_IDX")
    private Integer logIdx;

    @Column(name = "RESERVATION_IDX", nullable = false)
    private Integer reservationIdx;

    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    private String eventType;

    @Column(name = "ACTOR_TYPE", length = 20)
    private String actorType;

    @Column(name = "ACTOR_CODE", length = 50)
    private String actorCode;

    @Column(name = "PAYLOAD", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public FacilityReservationLog() {}

    public FacilityReservationLog(Integer reservationIdx, String eventType, String actorType,
                                    String actorCode, String payload) {
        this.reservationIdx = reservationIdx;
        this.eventType = eventType;
        this.actorType = actorType;
        this.actorCode = actorCode;
        this.payload = payload;
    }

    public Integer getLogIdx() {
        return logIdx;
    }

    public void setLogIdx(Integer logIdx) {
        this.logIdx = logIdx;
    }

    public Integer getReservationIdx() {
        return reservationIdx;
    }

    public void setReservationIdx(Integer reservationIdx) {
        this.reservationIdx = reservationIdx;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public String getActorCode() {
        return actorCode;
    }

    public void setActorCode(String actorCode) {
        this.actorCode = actorCode;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
