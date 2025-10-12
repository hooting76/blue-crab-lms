package BlueCrab.com.example.repository.Lecture;

import BlueCrab.com.example.entity.Lecture.LecTbl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/* 강의 정보 데이터베이스 접근을 위한 JPA 리포지토리 인터페이스
 * LecTbl 엔티티에 대한 CRUD 작업 및 커스텀 쿼리를 제공
 *
 * 주요 기능:
 * - 강의 조회 (강의코드, 교수명, 학년, 학기 등)
 * - 강의 검색 및 필터링 (전공/교양, 필수/선택, 수강신청 상태)
 * - 강의 통계 정보 제공 (전체 수, 개설 상태별 수)
 * - 수강 인원 관리 (증가/감소)
 *
 * 사용되는 주요 시나리오:
 * - 학생이 수강신청 가능한 강의 목록 조회
 * - 교수가 담당 강의 목록 조회
 * - 관리자가 강의 개설 및 관리
 * - 수강신청 시 수강 인원 업데이트
 * - 강의 검색 및 필터링
 *
 * Spring Data JPA의 메서드 네이밍 규칙을 활용한 쿼리 자동 생성
 * 복잡한 쿼리의 경우 @Query 어노테이션으로 JPQL 직접 작성
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-12
 */
@Repository
public interface LecTblRepository extends JpaRepository<LecTbl, Integer> {

    // ========== 기본 조회 메서드 ==========

    /* 강의 코드로 특정 강의 조회
     * 강의 검색, 중복 확인 등에 사용
     * 사용 예시: Optional<LecTbl> lecture = lecTblRepository.findByLecSerial("CS101");
     */
    Optional<LecTbl> findByLecSerial(String lecSerial);

    /* 담당 교수명으로 강의 목록 조회
     * 교수가 담당하는 모든 강의를 조회할 때 사용
     * 사용 예시: List<LecTbl> lectures = lecTblRepository.findByLecProf("김교수");
     */
    List<LecTbl> findByLecProf(String lecProf);

    /* 강의명에 특정 문자열이 포함된 강의 목록 조회
     * 강의 검색 기능에서 활용
     * 사용 예시: List<LecTbl> lectures = lecTblRepository.findByLecTitContaining("프로그래밍");
     */
    List<LecTbl> findByLecTitContaining(String lecTit);

    // ========== 수강신청 관련 조회 메서드 ==========

    /* 수강신청 상태별 강의 목록 조회 (페이징)
     * lecOpen: 0=닫힘, 1=열림
     * 사용 예시: Page<LecTbl> lectures = lecTblRepository.findByLecOpen(1, PageRequest.of(0, 10));
     */
    Page<LecTbl> findByLecOpen(Integer lecOpen, Pageable pageable);

    /* 수강신청 상태별 강의 목록 조회
     */
    List<LecTbl> findByLecOpen(Integer lecOpen);

    /* 수강신청 열려있고 정원이 남은 강의 목록 조회
     * 학생이 수강신청 가능한 강의 목록을 보여줄 때 사용
     * 사용 예시: Page<LecTbl> lectures = lecTblRepository.findAvailableLectures(1, PageRequest.of(0, 20));
     */
    @Query("SELECT l FROM LecTbl l WHERE l.lecOpen = :lecOpen AND l.lecCurrent < l.lecMany")
    Page<LecTbl> findAvailableLectures(@Param("lecOpen") Integer lecOpen, Pageable pageable);

    // ========== 학년/학기별 조회 메서드 ==========

    /* 대상 학년으로 강의 목록 조회
     * 사용 예시: List<LecTbl> lectures = lecTblRepository.findByLecYear(1); // 1학년 대상 강의
     */
    List<LecTbl> findByLecYear(Integer lecYear);

    /* 학기로 강의 목록 조회
     * 사용 예시: List<LecTbl> lectures = lecTblRepository.findByLecSemester(1); // 1학기 강의
     */
    List<LecTbl> findByLecSemester(Integer lecSemester);

    /* 학년과 학기로 강의 목록 조회
     * 사용 예시: Page<LecTbl> lectures = lecTblRepository.findByLecYearAndLecSemester(1, 1, PageRequest.of(0, 10));
     */
    Page<LecTbl> findByLecYearAndLecSemester(Integer lecYear, Integer lecSemester, Pageable pageable);

    // ========== 전공/교양 및 필수/선택 조회 메서드 ==========

    /* 전공/교양 구분으로 강의 목록 조회
     * lecMajor: 0=교양, 1=전공
     * 사용 예시: Page<LecTbl> majorLectures = lecTblRepository.findByLecMajor(1, PageRequest.of(0, 10));
     */
    Page<LecTbl> findByLecMajor(Integer lecMajor, Pageable pageable);

    // ========== 복합 검색 메서드 ==========

    /* 교수명과 학기로 강의 목록 조회
     * 특정 교수가 특정 학기에 개설한 강의를 조회
     */
    List<LecTbl> findByLecProfAndLecSemester(String lecProf, Integer lecSemester);

    /* 학년, 학기, 전공/교양, 수강신청 상태로 강의 검색
     * 학생이 수강신청할 수 있는 강의를 필터링할 때 사용
     */
    @Query("SELECT l FROM LecTbl l WHERE " +
           "(:lecYear IS NULL OR l.lecYear = :lecYear) AND " +
           "(:lecSemester IS NULL OR l.lecSemester = :lecSemester) AND " +
           "(:lecMajor IS NULL OR l.lecMajor = :lecMajor) AND " +
           "(:lecOpen IS NULL OR l.lecOpen = :lecOpen)")
    Page<LecTbl> searchLectures(@Param("lecYear") Integer lecYear,
                                 @Param("lecSemester") Integer lecSemester,
                                 @Param("lecMajor") Integer lecMajor,
                                 @Param("lecOpen") Integer lecOpen,
                                 Pageable pageable);

    // ========== 수강 인원 관리 메서드 ==========

    /* 수강 인원 증가
     * 학생이 수강신청할 때 사용
     * lecMany가 정원(capacity), lecCurrent가 현재 수강 인원
     */
    @Modifying
    @Query("UPDATE LecTbl l SET l.lecCurrent = l.lecCurrent + 1 WHERE l.lecIdx = :lecIdx AND l.lecCurrent < l.lecMany")
    int incrementLecCurrent(@Param("lecIdx") Integer lecIdx);

    /* 수강 인원 감소
     * 학생이 수강 취소할 때 사용
     */
    @Modifying
    @Query("UPDATE LecTbl l SET l.lecCurrent = l.lecCurrent - 1 WHERE l.lecIdx = :lecIdx AND l.lecCurrent > 0")
    int decrementLecCurrent(@Param("lecIdx") Integer lecIdx);

    // ========== 통계 관련 메서드 ==========

    /* 전체 강의 수 조회
     */
    @Query("SELECT COUNT(l) FROM LecTbl l")
    long countAllLectures();

    /* 수강신청 상태별 강의 수 조회
     */
    long countByLecOpen(Integer lecOpen);

    /* 특정 교수의 강의 수 조회
     */
    long countByLecProf(String lecProf);

    /* 전공/교양별 강의 수 조회
     */
    long countByLecMajor(Integer lecMajor);

    // ========== 존재 여부 확인 메서드 ==========

    /* 강의 코드의 존재 여부 확인
     * 강의 등록 시 중복 확인에 사용
     */
    boolean existsByLecSerial(String lecSerial);

    /* 정원이 남았는지 확인
     * lecMany가 정원, lecCurrent가 현재 수강 인원
     */
    @Query("SELECT CASE WHEN l.lecCurrent < l.lecMany THEN true ELSE false END FROM LecTbl l WHERE l.lecIdx = :lecIdx")
    boolean hasAvailableSeats(@Param("lecIdx") Integer lecIdx);
}