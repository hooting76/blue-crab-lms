package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.FacilityBlockTbl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시설 차단 정보 데이터베이스 접근을 위한 JPA 리포지토리 인터페이스
 */
@Repository
public interface FacilityBlockRepository extends JpaRepository<FacilityBlockTbl, Integer> {

    /**
     * 특정 시설의 활성 차단 목록 조회
     */
    @Query("SELECT b FROM FacilityBlockTbl b WHERE b.facilityIdx = :facilityIdx " +
           "AND b.blockEnd > :now ORDER BY b.blockStart ASC")
    List<FacilityBlockTbl> findActiveFacilityBlocks(
        @Param("facilityIdx") Integer facilityIdx,
        @Param("now") LocalDateTime now
    );

    /**
     * 특정 기간과 겹치는 차단 정보 조회
     */
    @Query("SELECT b FROM FacilityBlockTbl b WHERE b.facilityIdx = :facilityIdx " +
           "AND ((b.blockStart < :endTime AND b.blockEnd > :startTime))")
    List<FacilityBlockTbl> findConflictingBlocks(
        @Param("facilityIdx") Integer facilityIdx,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 시설의 모든 차단 목록 조회
     */
    List<FacilityBlockTbl> findByFacilityIdxOrderByBlockStartDesc(Integer facilityIdx);
}
