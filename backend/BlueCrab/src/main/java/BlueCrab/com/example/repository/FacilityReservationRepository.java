package BlueCrab.com.example.repository;

import BlueCrab.com.example.repository.projection.DashboardStatsProjection;
import BlueCrab.com.example.entity.FacilityReservationTbl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시설 예약 정보 데이터베이스 접근을 위한 JPA 리포지토리 인터페이스
 */
@Repository
public interface FacilityReservationRepository extends JpaRepository<FacilityReservationTbl, Integer> {

    List<FacilityReservationTbl> findByUserCodeOrderByCreatedAtDesc(String userCode);

    List<FacilityReservationTbl> findByUserCodeAndStatusOrderByCreatedAtDesc(String userCode, String status);

    @Query("SELECT r FROM FacilityReservationTbl r WHERE r.facilityIdx = :facilityIdx " +
           "AND r.status IN :statuses " +
           "AND (:excludeReservationIdx IS NULL OR r.reservationIdx <> :excludeReservationIdx) " +
           "AND ((r.startTime < :endTime AND r.endTime > :startTime))")
    List<FacilityReservationTbl> findConflictingReservations(
        @Param("facilityIdx") Integer facilityIdx,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("statuses") List<String> statuses,
        @Param("excludeReservationIdx") Integer excludeReservationIdx
    );

    @Query("SELECT r FROM FacilityReservationTbl r " +
           "LEFT JOIN FacilityTbl f ON r.facilityIdx = f.facilityIdx " +
           "LEFT JOIN UserTbl u ON r.userCode = u.userCode " +
           "WHERE r.status = :status " +
           "ORDER BY r.createdAt DESC")
    List<FacilityReservationTbl> findByStatusOrderByCreatedAtDesc(@Param("status") String status);

    @Query("SELECT r FROM FacilityReservationTbl r WHERE r.status = 'APPROVED' " +
           "AND r.endTime < :now ORDER BY r.endTime ASC")
    List<FacilityReservationTbl> findExpiredApprovedReservations(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(r) FROM FacilityReservationTbl r WHERE r.status = 'PENDING'")
    long countPendingReservations();

    @Query(value = "SELECT " +
           "COUNT(r.RESERVATION_IDX) as totalReservations, " +
           "SUM(CASE WHEN r.STATUS = 'PENDING' THEN 1 ELSE 0 END) as pendingCount, " +
           "SUM(CASE WHEN r.STATUS = 'APPROVED' THEN 1 ELSE 0 END) as approvedCount, " +
           "SUM(CASE WHEN r.STATUS = 'REJECTED' THEN 1 ELSE 0 END) as rejectedCount, " +
           "SUM(CASE WHEN r.STATUS = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledCount, " +
           "SUM(CASE WHEN r.STATUS = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount " +
           "FROM FACILITY_RESERVATION_TBL r " +
           "WHERE r.CREATED_AT BETWEEN :startDate AND :endDate", nativeQuery = true)
    DashboardStatsProjection getReservationStats(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    // 관리자용: 전체 예약 조회
       @Query("SELECT r FROM FacilityReservationTbl r ORDER BY r.createdAt DESC")
       List<FacilityReservationTbl> findAllOrderByCreatedAtDesc();

    // 관리자용: 시설별 예약 조회
    @Query("SELECT r FROM FacilityReservationTbl r WHERE r.facilityIdx = :facilityIdx " +
           "ORDER BY r.createdAt DESC")
    List<FacilityReservationTbl> findByFacilityIdxOrderByCreatedAtDesc(@Param("facilityIdx") Integer facilityIdx);

    // 관리자용: 상태 + 시설 필터
    @Query("SELECT r FROM FacilityReservationTbl r WHERE r.status = :status " +
           "AND r.facilityIdx = :facilityIdx ORDER BY r.createdAt DESC")
    List<FacilityReservationTbl> findByStatusAndFacilityIdxOrderByCreatedAtDesc(
        @Param("status") String status,
        @Param("facilityIdx") Integer facilityIdx
    );

       @Query("SELECT r FROM FacilityReservationTbl r " +
                 "LEFT JOIN FacilityTbl f ON r.facilityIdx = f.facilityIdx " +
                 "LEFT JOIN UserTbl u ON r.userCode = u.userCode " +
                 "WHERE (:status IS NULL OR r.status = :status) " +
                 "AND (:facilityIdx IS NULL OR r.facilityIdx = :facilityIdx) " +
                 "AND (:startDate IS NULL OR r.startTime >= :startDate) " +
                 "AND (:endDate IS NULL OR r.endTime <= :endDate) " +
                 "AND (:keyword IS NULL OR (" +
                 "LOWER(u.userName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                 "LOWER(u.userCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                 "LOWER(f.facilityName) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                 "))")
       Page<FacilityReservationTbl> searchReservations(
              @Param("status") String status,
              @Param("facilityIdx") Integer facilityIdx,
              @Param("startDate") LocalDateTime startDate,
              @Param("endDate") LocalDateTime endDate,
              @Param("keyword") String keyword,
              Pageable pageable
       );

    // Phase 2: 사용자별 활성 예약 수 조회
    @Query("SELECT COUNT(r) FROM FacilityReservationTbl r " +
           "WHERE r.userCode = :userCode " +
           "AND r.facilityIdx = :facilityIdx " +
           "AND r.status IN :statuses")
    long countByUserCodeAndFacilityIdxAndStatusIn(
        @Param("userCode") String userCode,
        @Param("facilityIdx") Integer facilityIdx,
        @Param("statuses") List<String> statuses
    );
}
