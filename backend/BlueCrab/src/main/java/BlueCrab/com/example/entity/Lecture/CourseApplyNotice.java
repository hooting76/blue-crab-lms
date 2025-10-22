package BlueCrab.com.example.entity.Lecture;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 수강신청 안내문 정보를 저장하는 JPA 엔티티 클래스
 * COURSE_APPLY_NOTICE 테이블과 매핑되며, 수강신청 기간 공지사항 관리에 사용
 *
 * 주요 기능:
 * - 수강신청 안내 메시지 저장
 * - 최신 안내문 1개만 유지 (단일 레코드 관리)
 * - 수정 이력 추적 (수정자, 수정 시각)
 *
 * 데이터베이스 매핑:
 * - 테이블명: COURSE_APPLY_NOTICE
 * - 기본키: NOTICE_IDX (자동 생성)
 *
 * 사용 예시:
 * - 관리자/교수가 안내문 작성 또는 수정
 * - 학생이 안내문 조회 (로그인 불필요)
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-22
 */
@Entity
@Table(name = "COURSE_APPLY_NOTICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseApplyNotice {

    /**
     * 안내문 고유 식별자
     * 데이터베이스에서 자동 생성되는 기본키
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTICE_IDX")
    private Integer noticeIdx;

    /**
     * 안내 메시지
     * 수강신청 기간, 주의사항 등을 포함하는 텍스트
     * TEXT 타입으로 여러 줄 입력 가능
     */
    @Column(name = "MESSAGE", columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * 수정 시각
     * 안내문이 작성되거나 수정된 시각
     * 자동 업데이트됨
     */
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 수정자
     * 안내문을 작성하거나 수정한 관리자/교수의 username
     */
    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    /**
     * 생성 시각
     * 안내문이 처음 생성된 시각
     * 수정 불가능 (updatable = false)
     */
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 엔티티가 저장되기 전에 호출되는 콜백 메소드
     * 생성 시각과 수정 시각을 자동으로 설정
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 엔티티가 업데이트되기 전에 호출되는 콜백 메소드
     * 수정 시각을 자동으로 업데이트
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
