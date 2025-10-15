package BlueCrab.com.example.repository.Lecture;

import BlueCrab.com.example.entity.Lecture.AttendanceRequestTbl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ATTENDANCE_REQUEST_TBL Repository
 * 
 * 출석 사유 신청 데이터 CRUD 및 조회 기능 제공
 */
@Repository
public interface AttendanceRequestRepository extends JpaRepository<AttendanceRequestTbl, Long> {

    /**
     * 특정 수강신청의 모든 요청 조회
     */
    List<AttendanceRequestTbl> findByEnrollmentIdx(Integer enrollmentIdx);

    /**
     * 특정 수강신청의 특정 회차 요청 조회
     */
    Optional<AttendanceRequestTbl> findByEnrollmentIdxAndSessionNumber(
            Integer enrollmentIdx, Integer sessionNumber);

    /**
     * 특정 수강신청의 대기 중인 요청 조회
     */
    List<AttendanceRequestTbl> findByEnrollmentIdxAndApprovalStatus(
            Integer enrollmentIdx, String approvalStatus);

    /**
     * 특정 강의의 모든 대기 중인 요청 조회 (교수용)
     */
    @Query("SELECT ar FROM AttendanceRequestTbl ar " +
           "WHERE ar.enrollmentIdx IN " +
           "(SELECT e.enrollmentIdx FROM EnrollmentExtendedTbl e WHERE e.lecIdx = :lecIdx) " +
           "AND ar.approvalStatus = 'PENDING' " +
           "ORDER BY ar.requestedAt ASC")
    Page<AttendanceRequestTbl> findPendingRequestsByLecture(
            @Param("lecIdx") Integer lecIdx, Pageable pageable);

    /**
     * 특정 강의의 모든 요청 조회 (교수용 - 모든 상태)
     */
    @Query("SELECT ar FROM AttendanceRequestTbl ar " +
           "WHERE ar.enrollmentIdx IN " +
           "(SELECT e.enrollmentIdx FROM EnrollmentExtendedTbl e WHERE e.lecIdx = :lecIdx) " +
           "ORDER BY ar.requestedAt DESC")
    Page<AttendanceRequestTbl> findAllRequestsByLecture(
            @Param("lecIdx") Integer lecIdx, Pageable pageable);

    /**
     * 이미 요청했는지 확인
     */
    boolean existsByEnrollmentIdxAndSessionNumber(
            Integer enrollmentIdx, Integer sessionNumber);

    /**
     * 특정 강의의 대기 중인 요청 개수
     */
    @Query("SELECT COUNT(ar) FROM AttendanceRequestTbl ar " +
           "WHERE ar.enrollmentIdx IN " +
           "(SELECT e.enrollmentIdx FROM EnrollmentExtendedTbl e WHERE e.lecIdx = :lecIdx) " +
           "AND ar.approvalStatus = 'PENDING'")
    long countPendingRequestsByLecture(@Param("lecIdx") Integer lecIdx);

    /**
     * 승인 상태별 요청 조회
     */
    Page<AttendanceRequestTbl> findByApprovalStatus(String approvalStatus, Pageable pageable);
}
