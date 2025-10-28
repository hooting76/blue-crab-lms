package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * FCM 알림 이력 Repository
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-28
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * 발송 이력 조회 (최신순)
     *
     * @param pageable 페이징 정보
     * @return 알림 이력 페이지
     */
    Page<NotificationEntity> findAllByOrderBySentAtDesc(Pageable pageable);

    /**
     * 특정 Admin이 발송한 이력 조회
     *
     * @param createdBy Admin ID
     * @return 알림 이력 리스트
     */
    List<NotificationEntity> findByCreatedByOrderBySentAtDesc(String createdBy);
}
