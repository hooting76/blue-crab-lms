// 작성자: AI Assistant
// 학부 Repository

package BlueCrab.com.example.repository.Lecture;

import BlueCrab.com.example.entity.Lecture.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Faculty 엔티티의 데이터베이스 액세스 레이어
 * Spring Data JPA를 사용한 CRUD 및 쿼리 메서드 제공
 *
 * @version 1.0.0
 * @since 2025-10-15
 */
@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Integer> {

    /**
     * 학부 코드로 학부 조회
     *
     * @param facultyCode 학부 코드 (두 자리 숫자 문자열)
     * @return 해당 학부 코드의 Faculty 객체 (Optional)
     */
    Optional<Faculty> findByFacultyCode(String facultyCode);

    /**
     * 학부 코드 존재 여부 확인
     *
     * @param facultyCode 학부 코드
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByFacultyCode(String facultyCode);
}
