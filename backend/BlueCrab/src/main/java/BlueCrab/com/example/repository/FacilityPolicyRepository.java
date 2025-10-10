package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.FacilityPolicyTbl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 시설별 예약 정책 데이터베이스 접근을 위한 JPA 리포지토리 인터페이스
 */
@Repository
public interface FacilityPolicyRepository extends JpaRepository<FacilityPolicyTbl, Integer> {

    /**
     * 특정 시설의 예약 정책을 조회합니다.
     *
     * @param facilityIdx 시설 ID
     * @return 예약 정책 정보 (Optional)
     */
    Optional<FacilityPolicyTbl> findByFacilityIdx(Integer facilityIdx);
}
