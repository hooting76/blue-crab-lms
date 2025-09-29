package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.ReadingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 열람실 좌석 현황 관리를 위한 Repository 인터페이스
 * ReadingSeat 엔티티에 대한 데이터베이스 작업을 담당
 *
 * 주요 기능:
 * - 좌석 현황 조회 (전체, 개별)
 * - 사용자별 예약 좌석 조회
 * - 만료된 좌석 조회 및 정리
 * - 통계 정보 제공
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Repository
public interface ReadingSeatRepository extends JpaRepository<ReadingSeat, Integer> {
    
    /**
     * 좌석 번호로 좌석 정보를 조회합니다.
     * @param seatNumber 좌석 번호
     * @return 좌석 정보
     */
    Optional<ReadingSeat> findBySeatNumber(Integer seatNumber);
    
    /**
     * 사용자 코드로 현재 사용 중인 좌석을 조회합니다.
     * 1인 1좌석 제한 검증에 사용
     * @param userCode 사용자 학번/교번
     * @return 사용 중인 좌석 정보
     */
    Optional<ReadingSeat> findByUserCodeAndIsOccupied(String userCode, Integer isOccupied);
    
    /**
     * 좌석 사용 상태로 좌석 목록을 조회합니다.
     * @param isOccupied 사용 상태 (0: 빈좌석, 1: 사용중)
     * @return 해당 상태의 좌석 목록
     */
    List<ReadingSeat> findByIsOccupied(Integer isOccupied);
    
    /**
     * 만료된 좌석들을 조회합니다.
     * 자동 퇴실 처리를 위해 사용
     * @param currentTime 현재 시간
     * @return 만료된 좌석 목록
     */
    @Query("SELECT rs FROM ReadingSeat rs WHERE rs.isOccupied = 1 AND rs.endTime < :currentTime")
    List<ReadingSeat> findExpiredSeats(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 전체 좌석 수를 조회합니다.
     * @return 전체 좌석 수
     */
    @Query("SELECT COUNT(rs) FROM ReadingSeat rs")
    long countTotalSeats();
    
    /**
     * 특정 상태의 좌석 수를 조회합니다.
     * @param isOccupied 사용 상태 (0: 빈좌석, 1: 사용중)
     * @return 해당 상태의 좌석 수
     */
    long countByIsOccupied(Integer isOccupied);
    
    /**
     * 특정 사용자가 현재 좌석을 사용 중인지 확인합니다.
     * @param userCode 사용자 학번/교번
     * @param isOccupied 사용 상태 (1: 사용중)
     * @return 사용 중이면 true, 아니면 false
     */
    boolean existsByUserCodeAndIsOccupied(String userCode, Integer isOccupied);
    
    /**
     * 좌석 번호 범위로 좌석들을 조회합니다.
     * 초기 데이터 생성 시 사용
     * @param startSeat 시작 좌석 번호
     * @param endSeat 종료 좌석 번호
     * @return 범위 내 좌석 목록
     */
    @Query("SELECT rs FROM ReadingSeat rs WHERE rs.seatNumber BETWEEN :startSeat AND :endSeat ORDER BY rs.seatNumber")
    List<ReadingSeat> findBySeatNumberBetween(@Param("startSeat") Integer startSeat, @Param("endSeat") Integer endSeat);
    
    /**
     * 좌석 번호순으로 정렬된 전체 좌석 목록을 조회합니다.
     * @return 좌석 번호순 전체 좌석 목록
     */
    List<ReadingSeat> findAllByOrderBySeatNumber();
}