package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.FacilityTbl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

/**
 * 시설 정보 데이터베이스 접근을 위한 JPA 리포지토리 인터페이스
 */
@Repository
public interface FacilityRepository extends JpaRepository<FacilityTbl, Integer> {

    List<FacilityTbl> findByIsActiveTrue();

    @Query("SELECT f FROM FacilityTbl f WHERE f.facilityType = :facilityType AND f.isActive = true")
    List<FacilityTbl> findByFacilityTypeAndActive(@Param("facilityType") String facilityType);

    @Query("SELECT f FROM FacilityTbl f WHERE (f.facilityName LIKE CONCAT('%', :keyword, '%') " +
           "OR f.location LIKE CONCAT('%', :keyword, '%')) AND f.isActive = true")
    List<FacilityTbl> searchFacilities(@Param("keyword") String keyword);

    /**
     * 비관적 락을 사용하여 시설 정보를 조회합니다.
     * 동시성 제어가 필요한 예약 생성/승인 시 사용됩니다.
     *
     * @param facilityIdx 시설 ID
     * @return 락이 적용된 시설 정보 (Optional)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    @Query("SELECT f FROM FacilityTbl f WHERE f.facilityIdx = :facilityIdx")
    Optional<FacilityTbl> findByIdWithLock(@Param("facilityIdx") Integer facilityIdx);
}
