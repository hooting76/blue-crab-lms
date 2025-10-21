package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.SerialCodeTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SerialCodeTable 엔티티를 위한 Spring Data JPA 리포지토리
 * 학생의 주전공 및 부전공 정보를 관리
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Repository
public interface SerialCodeTableRepository extends JpaRepository<SerialCodeTable, Integer> {
    Optional<SerialCodeTable> findByUserIdx(Integer userIdx);
}
