package BlueCrab.com.example.repository.Lecture;

import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/* 수강신청 정보 데이터베이스 접근을 위한 JPA 리포지토리 인터페이스
 * EnrollmentExtendedTbl 엔티티에 대한 CRUD 작업 및 커스텀 쿼리를 제공
 *
 * 주요 기능:
 * - 수강신청 정보 조회 (학생별, 강의별)
 * - 수강 상태 관리 (수강중, 수강취소)
 * - 출결 및 성적 정보 조회
 * - 수강 통계 정보 제공
 *
 * 사용되는 주요 시나리오:
 * - 학생이 수강신청한 강의 목록 조회
 * - 교수가 강의를 듣는 학생 목록 조회
 * - 출결 및 성적 관리
 * - 수강 신청/취소 처리
 * - 학생별/강의별 수강 통계
 *
 * JSON 데이터 활용:
 * - ENROLLMENT_DATA 필드에 수강신청 정보, 출결, 성적 데이터를 JSON 형식으로 저장
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
public interface EnrollmentExtendedTblRepository extends JpaRepository<EnrollmentExtendedTbl, Integer> {

    // ========== 기본 조회 메서드 ==========

    /* 학생 IDX로 수강신청 목록 조회
     * 학생이 수강신청한 모든 강의를 조회할 때 사용
     * 사용 예시: List<EnrollmentExtendedTbl> enrollments = enrollmentRepository.findByStudentIdx(123);
     */
    List<EnrollmentExtendedTbl> findByStudentIdx(Integer studentIdx);

    /* 학생 IDX로 수강신청 목록 조회 (페이징)
     */
    Page<EnrollmentExtendedTbl> findByStudentIdx(Integer studentIdx, Pageable pageable);

    /* 강의 IDX로 수강신청 목록 조회
     * 교수가 강의를 듣는 학생 목록을 조회할 때 사용
     * 사용 예시: List<EnrollmentExtendedTbl> enrollments = enrollmentRepository.findByLecIdx(456);
     */
    List<EnrollmentExtendedTbl> findByLecIdx(Integer lecIdx);

    /* 강의 IDX로 수강신청 목록 조회 (페이징)
     */
    Page<EnrollmentExtendedTbl> findByLecIdx(Integer lecIdx, Pageable pageable);

    /* 학생과 강의로 수강신청 정보 조회
     * 특정 학생이 특정 강의를 수강하는지 확인할 때 사용
     * 사용 예시: Optional<EnrollmentExtendedTbl> enrollment = enrollmentRepository.findByStudentIdxAndLecIdx(123, 456);
     */
    Optional<EnrollmentExtendedTbl> findByStudentIdxAndLecIdx(Integer studentIdx, Integer lecIdx);

    // ========== 학생별 수강 정보 조회 메서드 ==========

    /* 학생의 현재 수강 중인 강의 목록 조회 (JOIN)
     * 학생 대시보드에서 현재 수강 중인 강의 정보를 보여줄 때 사용
     * 사용 예시: List<EnrollmentExtendedTbl> currentEnrollments = enrollmentRepository.findEnrolledLecturesByStudent(123);
     */
    @Query("SELECT e FROM EnrollmentExtendedTbl e JOIN FETCH e.lecture WHERE e.studentIdx = :studentIdx")
    List<EnrollmentExtendedTbl> findEnrolledLecturesByStudent(@Param("studentIdx") Integer studentIdx);

    /* 학생의 수강 이력 조회 (강의 정보 + 학생 정보 모두 포함)
     * DTO 변환을 위해 lecture와 student를 모두 JOIN FETCH
     * countQuery를 별도로 지정하여 JOIN FETCH와 COUNT 쿼리 충돌 방지
     */
    @Query(value = "SELECT DISTINCT e FROM EnrollmentExtendedTbl e " +
                   "JOIN FETCH e.lecture " +
                   "JOIN FETCH e.student " +
                   "WHERE e.studentIdx = :studentIdx",
           countQuery = "SELECT COUNT(e) FROM EnrollmentExtendedTbl e WHERE e.studentIdx = :studentIdx")
    Page<EnrollmentExtendedTbl> findEnrollmentHistoryByStudent(@Param("studentIdx") Integer studentIdx, Pageable pageable);

    // ========== 강의별 수강 정보 조회 메서드 ==========

    /* 강의의 수강생 목록 조회 (학생 정보 포함)
     * 교수가 강의를 듣는 학생 명단을 확인할 때 사용
     * 사용 예시: List<EnrollmentExtendedTbl> students = enrollmentRepository.findStudentsByLecture(456);
     */
    @Query("SELECT e FROM EnrollmentExtendedTbl e JOIN FETCH e.student WHERE e.lecIdx = :lecIdx")
    List<EnrollmentExtendedTbl> findStudentsByLecture(@Param("lecIdx") Integer lecIdx);

    /* 강의의 수강생 목록 조회 (페이징, 강의 정보 + 학생 정보 모두 포함)
     * DTO 변환을 위해 lecture와 student를 모두 JOIN FETCH
     * countQuery를 별도로 지정하여 JOIN FETCH와 COUNT 쿼리 충돌 방지
     */
    @Query(value = "SELECT DISTINCT e FROM EnrollmentExtendedTbl e " +
                   "JOIN FETCH e.lecture " +
                   "JOIN FETCH e.student " +
                   "WHERE e.lecIdx = :lecIdx",
           countQuery = "SELECT COUNT(e) FROM EnrollmentExtendedTbl e WHERE e.lecIdx = :lecIdx")
    Page<EnrollmentExtendedTbl> findStudentsByLecture(@Param("lecIdx") Integer lecIdx, Pageable pageable);

    // ========== 통계 관련 메서드 ==========

    /* 학생의 총 수강신청 수 조회
     * 사용 예시: long enrollmentCount = enrollmentRepository.countByStudentIdx(123);
     */
    long countByStudentIdx(Integer studentIdx);

    /* 강의의 수강생 수 조회
     * 수강 정원 확인 등에 사용
     * 사용 예시: long studentCount = enrollmentRepository.countByLecIdx(456);
     */
    long countByLecIdx(Integer lecIdx);

    /* 전체 수강신청 건수 조회
     */
    @Query("SELECT COUNT(e) FROM EnrollmentExtendedTbl e")
    long countAllEnrollments();

    // ========== 존재 여부 확인 메서드 ==========

    /* 학생이 특정 강의를 수강하는지 확인
     * 중복 수강신청 방지 등에 사용
     * 사용 예시: if (enrollmentRepository.existsByStudentIdxAndLecIdx(123, 456)) { throw new Exception("중복"); }
     */
    boolean existsByStudentIdxAndLecIdx(Integer studentIdx, Integer lecIdx);

    // ========== 복합 조회 메서드 ==========

    /* 여러 강의 IDX로 수강신청 목록 조회
     * N+1 문제 방지를 위한 배치 페치 메서드
     */
    List<EnrollmentExtendedTbl> findAllByLecIdxIn(List<Integer> lecIdxList);

    /* 여러 학생 IDX로 수강신청 목록 조회
     */
    List<EnrollmentExtendedTbl> findAllByStudentIdxIn(List<Integer> studentIdxList);

    // ========== 출석 관리 관련 메서드 (출석 요청/승인 시스템용) ==========

    /**
     * 강의 코드(LEC_SERIAL)로 수강생 목록 조회 (강의 정보 + 학생 정보 포함)
     * 출석 요청/승인 시스템에서 사용
     * 
     * @param lecSerial 강의 코드 (예: "CS101")
     * @return 수강생 목록 (강의 정보 및 학생 정보 포함)
     * 
     * 사용 시나리오:
     * - 교수가 강의의 전체 학생 출석 현황을 조회할 때
     * - 스케줄러가 특정 강의의 대기 요청을 처리할 때
     */
    @Query("SELECT e FROM EnrollmentExtendedTbl e " +
           "JOIN FETCH e.lecture l " +
           "JOIN FETCH e.student s " +
           "WHERE l.lecSerial = :lecSerial")
    List<EnrollmentExtendedTbl> findByLecSerialWithDetails(@Param("lecSerial") String lecSerial);

    /**
     * 강의 코드와 학생 IDX로 수강 정보 조회
     * 출석 요청/승인 시스템에서 특정 학생의 출석 데이터 조회에 사용
     * 
     * ⚠️ 중요: lecSerial을 파라미터로 받지만 내부적으로 LEC_IDX로 조인됨
     * 
     * 동작 원리:
     * 1. 프론트엔드에서 lecSerial (예: "CS101") 전달
     * 2. Repository가 LEC_TBL과 JOIN하여 LEC_IDX 매핑
     * 3. LEC_IDX로 ENROLLMENT_EXTENDED_TBL 조회
     * 
     * DB 스키마:
     * - LEC_TBL.LEC_SERIAL (사용자 친화적 코드, 예: "CS101")
     * - LEC_TBL.LEC_IDX (기본키, 예: 6)
     * - ENROLLMENT_EXTENDED_TBL.LEC_IDX (외래키, LEC_TBL.LEC_IDX 참조)
     * 
     * 쿼리 흐름:
     * lecSerial "CS101" 
     * → LEC_TBL JOIN (WHERE lecSerial = "CS101") 
     * → LEC_IDX = 6 추출
     * → ENROLLMENT_EXTENDED_TBL 조회 (WHERE LEC_IDX = 6 AND STUDENT_IDX = studentIdx)
     * 
     * @param lecSerial 강의 코드 (예: "CS101") - 사용자 친화적 식별자
     * @param studentIdx 학생 USER_IDX
     * @return 수강 정보 (Optional)
     * 
     * 사용 시나리오:
     * - 학생이 출석 요청을 할 때 (수강 여부 확인)
     * - 학생이 자신의 출석 현황을 조회할 때
     * - 교수가 특정 학생의 출석을 승인할 때
     */
    @Query("SELECT e FROM EnrollmentExtendedTbl e " +
           "JOIN FETCH e.lecture l " +
           "WHERE l.lecSerial = :lecSerial AND e.studentIdx = :studentIdx")
    Optional<EnrollmentExtendedTbl> findByLecSerialAndStudentIdx(
        @Param("lecSerial") String lecSerial,
        @Param("studentIdx") Integer studentIdx
    );

    /**
     * 강의 코드와 교수 USER_IDX로 강의 수강생 목록 조회 (권한 검증용)
     * 교수가 해당 강의의 담당 교수인지 검증하는 데 사용
     * 
     * ⚠️ 중요: lecSerial을 파라미터로 받지만 내부적으로 LEC_IDX로 조인됨
     * 
     * 동작 원리:
     * 1. 프론트엔드에서 lecSerial (예: "CS101") 전달
     * 2. Repository가 LEC_TBL과 JOIN하여 LEC_IDX 매핑
     * 3. LEC_IDX로 ENROLLMENT_EXTENDED_TBL 전체 수강생 조회
     * 4. 교수 권한 검증 (LEC_TBL.LEC_PROF = professorIdx의 USER_CODE)
     * 
     * @param lecSerial 강의 코드 (예: "CS101") - 사용자 친화적 식별자
     * @param professorIdx 교수 USER_IDX
     * @return 수강생 목록 (담당 강의가 맞으면 1개 이상 반환, 아니면 빈 리스트)
     * 
     * 사용 시나리오:
     * - 교수가 출석 승인 API를 호출할 때 권한 검증
     * - 교수가 강의의 출석 현황을 조회할 때 권한 검증
     * 
     * 권한 검증 예시:
     * <pre>
     * List<EnrollmentExtendedTbl> enrollments = 
     *     repository.findByLecSerialAndProfessorIdx(lecSerial, professorIdx);
     * if (enrollments.isEmpty()) {
     *     throw new AccessDeniedException("해당 강의의 담당 교수가 아닙니다.");
     * }
     * </pre>
     */
    @Query("SELECT e FROM EnrollmentExtendedTbl e " +
           "JOIN FETCH e.lecture l " +
           "JOIN FETCH e.student s " +
           "WHERE l.lecSerial = :lecSerial AND " +
           "EXISTS (SELECT 1 FROM UserTbl u WHERE u.userIdx = :professorIdx AND u.userCode = l.lecProf)")
    List<EnrollmentExtendedTbl> findByLecSerialAndProfessorIdx(
        @Param("lecSerial") String lecSerial,
        @Param("professorIdx") Integer professorIdx
    );

    // ========== JSON 데이터 기반 조회 메서드 (향후 확장) ==========

    /* JSON 데이터를 활용한 고급 쿼리는 Service 레이어에서 처리
     * - 출결 상태별 조회
     * - 성적 범위별 조회
     * - 수강 상태별 조회 등
     * Repository에서 기본 데이터를 조회한 후, Service에서 Jackson ObjectMapper를 사용하여 JSON 파싱 및 필터링
     */

    // ========== 삭제 관련 메서드 ==========

    /* 학생의 모든 수강신청 삭제
     * 학생 탈퇴 시 등에 사용 (CASCADE 설정으로 자동 삭제되지만 명시적 메서드 제공)
     */
    int deleteByStudentIdx(Integer studentIdx);

    /* 강의의 모든 수강신청 삭제
     * 강의 삭제 시 등에 사용 (CASCADE 설정으로 자동 삭제되지만 명시적 메서드 제공)
     */
    int deleteByLecIdx(Integer lecIdx);

    /* 특정 학생의 특정 강의 수강신청 삭제
     * 수강 취소 시 사용
     */
    int deleteByStudentIdxAndLecIdx(Integer studentIdx, Integer lecIdx);
}
