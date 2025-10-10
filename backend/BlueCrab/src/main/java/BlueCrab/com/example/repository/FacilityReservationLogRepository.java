package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.FacilityReservationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 시설 예약 로그 데이터베이스 접근을 위한 JPA 리포지토리 인터페이스
 */
@Repository
public interface FacilityReservationLogRepository extends JpaRepository<FacilityReservationLog, Integer> {

    List<FacilityReservationLog> findByReservationIdxOrderByCreatedAtAsc(Integer reservationIdx);
}
