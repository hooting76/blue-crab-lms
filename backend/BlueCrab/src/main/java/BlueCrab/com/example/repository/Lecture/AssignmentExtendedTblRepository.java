package BlueCrab.com.example.repository.Lecture;

import BlueCrab.com.example.entity.Lecture.AssignmentExtendedTbl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/* 과제 정보 데이터베이스 접근을 위한 JPA 리포지토리 인터페이스
 * AssignmentExtendedTbl 엔티티에 대한 CRUD 작업 및 커스텀 쿼리를 제공
 *
 * 주요 기능:
 * - 과제 정보 조회 (강의별)
 * - 과제 제출 및 채점 관리
 * - 과제 통계 정보 제공
 * - 과제 상태 관리
 *
 * 사용되는 주요 시나리오:
 * - 교수가 강의별 과제 목록 조회
 * - 학생이 수강 중인 강의의 과제 목록 조회
 * - 과제 등록, 수정, 삭제
 * - 과제 제출 현황 확인
 * - 과제 채점 및 피드백 관리
 *
 * JSON 데이터 활용:
 * - ASSIGNMENT_DATA 필드에 과제 정보, 제출 목록, 채점 정보를 JSON 형식으로 저장
 * - JSON 데이터 구조는 Entity 클래스의 주석 참조
 *
 * Spring Data JPA의 메서드 네이밍 규칙을 활용한 쿼리 자동 생성
 * 복잡한 쿼리의 경우 @Query 어노테이션으로 JPQL 직접 작성
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-12
 */
@Repository
public interface AssignmentExtendedTblRepository extends JpaRepository<AssignmentExtendedTbl, Integer> {

    // ========== 기본 조회 메서드 ==========

    /* 강의 IDX로 과제 목록 조회
     * 교수가 강의의 모든 과제를 관리하거나 학생이 과제 목록을 확인할 때 사용
     * 사용 예시: List<AssignmentExtendedTbl> assignments = assignmentRepository.findByLecIdx(456);
     */
    List<AssignmentExtendedTbl> findByLecIdx(Integer lecIdx);

    /* 강의 IDX로 과제 목록 조회 (페이징)
     * 사용 예시: Page<AssignmentExtendedTbl> assignments = assignmentRepository.findByLecIdx(456, PageRequest.of(0, 10));
     */
    Page<AssignmentExtendedTbl> findByLecIdx(Integer lecIdx, Pageable pageable);

    // ========== 강의 정보 포함 조회 메서드 ==========

    /* 강의의 과제 목록 조회 (강의 정보 포함)
     * JOIN FETCH를 사용하여 N+1 문제 방지
     * 사용 예시: List<AssignmentExtendedTbl> assignments = assignmentRepository.findAssignmentsWithLecture(456);
     */
    @Query("SELECT a FROM AssignmentExtendedTbl a JOIN FETCH a.lecture WHERE a.lecIdx = :lecIdx")
    List<AssignmentExtendedTbl> findAssignmentsWithLecture(@Param("lecIdx") Integer lecIdx);

    /* 강의의 과제 목록 조회 (페이징, 강의 정보 포함)
     */
    @Query("SELECT a FROM AssignmentExtendedTbl a JOIN FETCH a.lecture WHERE a.lecIdx = :lecIdx")
    Page<AssignmentExtendedTbl> findAssignmentsWithLecture(@Param("lecIdx") Integer lecIdx, Pageable pageable);

    // ========== 복합 조회 메서드 ==========

    /* 여러 강의 IDX로 과제 목록 조회
     * N+1 문제 방지를 위한 배치 페치 메서드
     * 학생이 수강 중인 여러 강의의 과제를 한 번에 조회할 때 사용
     */
    List<AssignmentExtendedTbl> findAllByLecIdxIn(List<Integer> lecIdxList);

    /* 여러 강의의 과제 목록 조회 (강의 정보 포함)
     */
    @Query("SELECT a FROM AssignmentExtendedTbl a JOIN FETCH a.lecture WHERE a.lecIdx IN :lecIdxList")
    List<AssignmentExtendedTbl> findAllByLecIdxInWithLecture(@Param("lecIdxList") List<Integer> lecIdxList);

    // ========== 통계 관련 메서드 ==========

    /* 강의의 과제 수 조회
     * 사용 예시: long assignmentCount = assignmentRepository.countByLecIdx(456);
     */
    long countByLecIdx(Integer lecIdx);

    /* 전체 과제 수 조회
     */
    @Query("SELECT COUNT(a) FROM AssignmentExtendedTbl a")
    long countAllAssignments();

    // ========== 존재 여부 확인 메서드 ==========

    /* 특정 강의에 과제가 있는지 확인
     * 사용 예시: if (assignmentRepository.existsByLecIdx(456)) { // 과제가 있는 강의 }
     */
    boolean existsByLecIdx(Integer lecIdx);

    // ========== 삭제 관련 메서드 ==========

    /* 강의의 모든 과제 삭제
     * 강의 삭제 시 등에 사용 (CASCADE 설정으로 자동 삭제되지만 명시적 메서드 제공)
     * 사용 예시: int deletedCount = assignmentRepository.deleteByLecIdx(456);
     */
    int deleteByLecIdx(Integer lecIdx);

    // ========== JSON 데이터 기반 조회 메서드 (향후 확장) ==========

    /* JSON 데이터를 활용한 고급 쿼리는 Service 레이어에서 처리
     * - 마감일 기준 조회 (마감 임박, 마감 지난 과제 등)
     * - 제출 상태별 조회 (제출 완료, 미제출)
     * - 채점 상태별 조회 (채점 완료, 채점 대기)
     * - 특정 학생의 제출 여부 확인
     * Repository에서 기본 데이터를 조회한 후, Service에서 Jackson ObjectMapper를 사용하여 JSON 파싱 및 필터링
     */

    // ========== 정렬 메서드 ==========

    /* 강의의 과제 목록 조회 (최신순)
     * assignment_idx 기준 내림차순 정렬
     */
    List<AssignmentExtendedTbl> findByLecIdxOrderByAssignmentIdxDesc(Integer lecIdx);

    /* 강의의 과제 목록 조회 (오래된 순)
     * assignment_idx 기준 오름차순 정렬
     */
    List<AssignmentExtendedTbl> findByLecIdxOrderByAssignmentIdxAsc(Integer lecIdx);

    // ========== 커스텀 쿼리 메서드 (향후 확장) ==========

    /* 특정 기간 내에 생성된 과제 조회
     * 관리자 대시보드에서 최근 생성된 과제 목록을 보여줄 때 사용
     * 참고: JSON 데이터의 createdAt 필드를 활용하려면 Service 레이어에서 처리 또는 별도 컬럼으로 분리 고려
     */

    /* 마감일이 임박한 과제 조회
     * 참고: JSON 데이터의 dueDate 필드를 활용하려면 Service 레이어에서 처리
     * 또는 별도 컬럼으로 분리하여 다음과 같은 쿼리 작성 가능:
     * @Query("SELECT a FROM AssignmentExtendedTbl a WHERE a.lecIdx = :lecIdx AND a.dueDate BETWEEN :startDate AND :endDate")
     * List<AssignmentExtendedTbl> findUpcomingAssignments(@Param("lecIdx") Integer lecIdx,
     *                                                      @Param("startDate") LocalDateTime startDate,
     *                                                      @Param("endDate") LocalDateTime endDate);
     */
}
