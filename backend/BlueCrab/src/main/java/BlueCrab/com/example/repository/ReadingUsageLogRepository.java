package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.ReadingUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 열람실 사용 기록 관리를 위한 Repository 인터페이스
 * ReadingUsageLog 엔티티에 대한 데이터베이스 작업을 담당
 *
 * 주요 기능:
 * - 사용 기록 CRUD 작업
 * - 사용자별 이용 이력 조회
 * - 통계 데이터 제공
 * - 활성 세션 관리
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Repository
public interface ReadingUsageLogRepository extends JpaRepository<ReadingUsageLog, Long> {
    
    /**
     * 사용자의 현재 활성 세션을 조회합니다.
     * endTime이 null인 기록을 찾습니다.
     * @param userCode 사용자 학번/교번
     * @return 현재 사용 중인 기록
     */
    Optional<ReadingUsageLog> findByUserCodeAndEndTimeIsNull(String userCode);
    
    /**
     * 특정 좌석의 현재 활성 세션을 조회합니다.
     * @param seatNumber 좌석 번호
     * @return 현재 사용 중인 기록
     */
    Optional<ReadingUsageLog> findBySeatNumberAndEndTimeIsNull(Integer seatNumber);
    
    /**
     * 사용자별 전체 이용 기록을 조회합니다. (최신순)
     * @param userCode 사용자 학번/교번
     * @return 사용자 이용 기록 목록
     */
    List<ReadingUsageLog> findByUserCodeOrderByCreatedAtDesc(String userCode);
    
    /**
     * 특정 기간 동안의 사용자 이용 기록을 조회합니다.
     * @param userCode 사용자 학번/교번
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return 기간별 이용 기록
     */
    @Query("SELECT rul FROM ReadingUsageLog rul WHERE rul.userCode = :userCode " +
           "AND rul.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY rul.createdAt DESC")
    List<ReadingUsageLog> findByUserCodeAndDateRange(
            @Param("userCode") String userCode,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 특정 좌석의 전체 사용 기록을 조회합니다. (최신순)
     * @param seatNumber 좌석 번호
     * @return 좌석 사용 기록 목록
     */
    List<ReadingUsageLog> findBySeatNumberOrderByCreatedAtDesc(Integer seatNumber);
    
    /**
     * 특정 기간의 전체 사용 기록을 조회합니다.
     * 통계 및 리포트 생성에 사용
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return 기간별 전체 사용 기록
     */
    @Query("SELECT rul FROM ReadingUsageLog rul WHERE rul.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY rul.createdAt DESC")
    List<ReadingUsageLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 사용자의 총 이용 횟수를 조회합니다.
     * @param userCode 사용자 학번/교번
     * @return 총 이용 횟수
     */
    long countByUserCode(String userCode);
    
    /**
     * 특정 기간 동안 사용자의 이용 횟수를 조회합니다.
     * @param userCode 사용자 학번/교번
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return 기간별 이용 횟수
     */
    @Query("SELECT COUNT(rul) FROM ReadingUsageLog rul WHERE rul.userCode = :userCode " +
           "AND rul.createdAt BETWEEN :startDate AND :endDate")
    long countByUserCodeAndDateRange(
            @Param("userCode") String userCode,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 특정 좌석의 총 사용 횟수를 조회합니다.
     * @param seatNumber 좌석 번호
     * @return 좌석 총 사용 횟수
     */
    long countBySeatNumber(Integer seatNumber);
    
    /**
     * 현재 활성 세션 수를 조회합니다.
     * @return 현재 사용 중인 좌석 수
     */
    @Query("SELECT COUNT(rul) FROM ReadingUsageLog rul WHERE rul.endTime IS NULL")
    long countActiveSessions();
    
    /**
     * 사용자의 총 이용 시간을 분 단위로 계산합니다.
     * 완료된 세션만 계산 (endTime이 null이 아닌 것)
     * Native Query를 사용하여 MySQL의 TIMESTAMPDIFF 함수 활용
     * @param userCode 사용자 학번/교번
     * @return 총 이용 시간 (분)
     */
    @Query(value = "SELECT COALESCE(SUM(TIMESTAMPDIFF(MINUTE, start_time, end_time)), 0) " +
           "FROM LAMP_USAGE_LOG WHERE user_code = :userCode AND end_time IS NOT NULL", 
           nativeQuery = true)
    long getTotalUsageTimeInMinutes(@Param("userCode") String userCode);
    
    /**
     * 사용자가 현재 활성 세션을 가지고 있는지 확인합니다.
     * @param userCode 사용자 학번/교번
     * @return 활성 세션이 있으면 true, 없으면 false
     */
    boolean existsByUserCodeAndEndTimeIsNull(String userCode);

       @Modifying(clearAutomatically = true)
       @Query("UPDATE ReadingUsageLog rul SET rul.preNoticeSentAt = :sentAt, rul.preNoticeTokenCount = :tokenCount WHERE rul.logId = :logId")
       int markPreNoticeSent(@Param("logId") Long logId,
                                            @Param("sentAt") LocalDateTime sentAt,
                                            @Param("tokenCount") Integer tokenCount);
}