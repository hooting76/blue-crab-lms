package BlueCrab.com.example.repository.Lecture;

import BlueCrab.com.example.entity.Lecture.CourseApplyNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CourseApplyNotice 엔티티의 데이터베이스 액세스 레이어
 * Spring Data JPA를 사용한 CRUD 및 쿼리 메서드 제공
 *
 * 주요 기능:
 * - 안내문 CRUD 작업
 * - 최신 안내문 조회 (단일 레코드 관리)
 *
 * @version 1.0.0
 * @since 2025-10-22
 */
@Repository
public interface CourseApplyNoticeRepository extends JpaRepository<CourseApplyNotice, Integer> {

    /**
     * 가장 최근에 수정된 안내문 1개를 조회
     * 단일 레코드 관리를 위해 항상 최신 안내문만 반환
     *
     * @return 최신 안내문 (Optional)
     */
    Optional<CourseApplyNotice> findTopByOrderByUpdatedAtDesc();
}
