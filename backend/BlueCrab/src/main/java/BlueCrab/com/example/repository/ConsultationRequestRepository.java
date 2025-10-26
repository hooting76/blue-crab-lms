package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.ConsultationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상담 요청 정보 데이터베이스 접근을 위한 JPA 리포지토리 인터페이스
 */
@Repository
public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {

    // 요청자 기준 목록 조회 (학생이 보낸 요청)
    Page<ConsultationRequest> findByRequesterUserCodeOrderByCreatedAtDesc(String requesterUserCode, Pageable pageable);

    // 수신자 기준 목록 조회 (교수가 받은 요청)
    Page<ConsultationRequest> findByRecipientUserCodeOrderByCreatedAtDesc(String recipientUserCode, Pageable pageable);

    // 요청자 + 상태 필터
    Page<ConsultationRequest> findByRequesterUserCodeAndRequestStatusOrderByCreatedAtDesc(
        String requesterUserCode,
        String requestStatus,
        Pageable pageable
    );

    // 수신자 + 상태 필터
    Page<ConsultationRequest> findByRecipientUserCodeAndRequestStatusOrderByCreatedAtDesc(
        String recipientUserCode,
        String requestStatus,
        Pageable pageable
    );

    // 진행 중인 상담 조회 (학생/교수 모두)
    @Query("SELECT c FROM ConsultationRequest c " +
           "WHERE (c.requesterUserCode = :userCode OR c.recipientUserCode = :userCode) " +
           "AND c.consultationStatus IN ('SCHEDULED', 'IN_PROGRESS') " +
           "ORDER BY c.lastActivityAt DESC")
    Page<ConsultationRequest> findActiveConsultations(@Param("userCode") String userCode, Pageable pageable);

    // 자동 종료 대상 조회 (2시간 비활성)
    @Query("SELECT c FROM ConsultationRequest c " +
           "WHERE c.consultationStatus = 'IN_PROGRESS' " +
           "AND c.lastActivityAt < :threshold")
    List<ConsultationRequest> findInactiveConsultations(@Param("threshold") LocalDateTime threshold);

    // 자동 종료 대상 조회 (24시간 제한)
    @Query("SELECT c FROM ConsultationRequest c " +
           "WHERE c.consultationStatus = 'IN_PROGRESS' " +
           "AND c.startedAt < :threshold")
    List<ConsultationRequest> findLongRunningConsultations(@Param("threshold") LocalDateTime threshold);

    // 완료된 상담 이력 조회
    @Query("SELECT c FROM ConsultationRequest c " +
           "WHERE (c.requesterUserCode = :userCode OR c.recipientUserCode = :userCode) " +
           "AND c.consultationStatus = 'COMPLETED' " +
           "AND (:startDate IS NULL OR c.endedAt >= :startDate) " +
           "AND (:endDate IS NULL OR c.endedAt <= :endDate) " +
           "ORDER BY c.endedAt DESC")
    Page<ConsultationRequest> findCompletedConsultations(
        @Param("userCode") String userCode,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    // 읽지 않은 요청 개수 (교수용)
    @Query("SELECT COUNT(c) FROM ConsultationRequest c " +
           "WHERE c.recipientUserCode = :recipientUserCode " +
           "AND c.requestStatus = 'PENDING'")
    long countUnreadRequests(@Param("recipientUserCode") String recipientUserCode);

    // 읽지 않은 메시지가 있는 상담 조회
    @Query("SELECT c FROM ConsultationRequest c " +
           "WHERE c.consultationStatus = 'IN_PROGRESS' " +
           "AND ((c.requesterUserCode = :userCode AND (c.lastReadTimeStudent IS NULL OR c.lastReadTimeStudent < c.lastActivityAt)) " +
           "OR (c.recipientUserCode = :userCode AND (c.lastReadTimeProfessor IS NULL OR c.lastReadTimeProfessor < c.lastActivityAt)))")
    List<ConsultationRequest> findConsultationsWithUnreadMessages(@Param("userCode") String userCode);

    // 상태 기준 상담 목록 조회
    List<ConsultationRequest> findByConsultationStatus(String consultationStatus);

    // 참여자 확인 (권한 검증용)
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ConsultationRequest c " +
           "WHERE c.requestIdx = :requestIdx " +
           "AND (c.requesterUserCode = :userCode OR c.recipientUserCode = :userCode)")
    boolean isParticipant(@Param("requestIdx") Long requestIdx, @Param("userCode") String userCode);

    // 상대방 userCode 조회
    @Query("SELECT CASE " +
           "WHEN c.requesterUserCode = :userCode THEN c.recipientUserCode " +
           "ELSE c.requesterUserCode END " +
           "FROM ConsultationRequest c " +
           "WHERE c.requestIdx = :requestIdx")
    String findPartnerUserCode(@Param("requestIdx") Long requestIdx, @Param("userCode") String userCode);
}
