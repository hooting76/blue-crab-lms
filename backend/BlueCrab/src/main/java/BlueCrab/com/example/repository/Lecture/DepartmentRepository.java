// 작성자: AI Assistant
// 학과 Repository

package BlueCrab.com.example.repository.Lecture;

import BlueCrab.com.example.entity.Lecture.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Department 엔티티의 데이터베이스 액세스 레이어
 * Spring Data JPA를 사용한 CRUD 및 쿼리 메서드 제공
 *
 * @version 1.0.0
 * @since 2025-10-15
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    /**
     * 학부 ID와 학과 코드로 학과 조회
     * 
     * @param facultyId 학부 ID
     * @param deptCode 학과 코드 (두 자리 숫자 문자열)
     * @return 해당 학과의 Department 객체 (Optional)
     */
    Optional<Department> findByFacultyIdAndDeptCode(Integer facultyId, String deptCode);

    /**
     * 학부 ID로 해당 학부의 모든 학과 조회
     *
     * @param facultyId 학부 ID
     * @return 해당 학부의 학과 리스트
     */
    List<Department> findByFacultyId(Integer facultyId);

    /**
     * 학부 ID와 학과 코드 존재 여부 확인
     *
     * @param facultyId 학부 ID
     * @param deptCode 학과 코드
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByFacultyIdAndDeptCode(Integer facultyId, String deptCode);
}
