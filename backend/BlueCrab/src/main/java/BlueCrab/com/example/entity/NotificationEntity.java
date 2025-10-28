package BlueCrab.com.example.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * FCM 알림 발송 이력 엔티티
 *
 * NOTIFICATION_TBL 테이블과 매핑
 * 관리자가 발송한 FCM 알림의 이력을 저장
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-28
 */
@Entity
@Table(name = "NOTIFICATION_TBL")
@Getter
@Setter
public class NotificationEntity {

    /**
     * 알림 고유 식별자
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTIFICATION_IDX")
    private Long id;

    /**
     * 알림 제목
     */
    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    /**
     * 알림 내용
     */
    @Column(name = "BODY", nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * 필터 조건 (JSON 형식)
     * UserFilterCriteria를 JSON으로 직렬화하여 저장
     */
    @Column(name = "FILTER_CRITERIA", columnDefinition = "TEXT")
    private String filterCriteriaJson;

    /**
     * 대상 수
     */
    @Column(name = "TARGET_COUNT")
    private Integer targetCount;

    /**
     * 성공 수
     */
    @Column(name = "SUCCESS_COUNT")
    private Integer successCount;

    /**
     * 실패 수
     */
    @Column(name = "FAILURE_COUNT")
    private Integer failureCount;

    /**
     * 발송 시간
     */
    @Column(name = "SENT_AT")
    private LocalDateTime sentAt;

    /**
     * 발송한 관리자 ID (이메일)
     * USER_TBL.USER_EMAIL과 동일한 길이 (200)
     */
    @Column(name = "CREATED_BY", length = 200)
    private String createdBy;

    /**
     * 생성 시간
     */
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 엔티티 생성 시 자동으로 생성 시간 설정
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
