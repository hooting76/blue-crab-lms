package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.CertIssueTbl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CERT_ISSUE_TBL에 대한 JPA Repository 인터페이스
 * 증명서 발급 이력 조회 및 저장을 위한 데이터 액세스 계층
 *
 * 주요 기능:
 * - 증명서 발급 이력 저장
 * - 사용자별 발급 이력 조회
 * - 증명서 유형별 필터링
 * - 최근 발급 이력 체크 (남발 방지)
 * - 발급 통계 데이터 제공
 *
 * 성능 최적화:
 * - Fetch Join으로 N+1 문제 방지
 * - 인덱스 활용 (USER_IDX, ISSUED_AT)
 * - 페이징 지원 (대량 데이터 처리)
 *
 * 보안 고려사항:
 * - 본인의 발급 이력만 조회 가능
 * - 관리자는 전체 발급 이력 조회 가능
 * - 민감한 정보(JSON 스냅샷)는 필요시에만 조회
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-13
 */
@Repository
public interface CertIssueRepository extends JpaRepository<CertIssueTbl, Integer> {

    /**
     * 사용자 이메일로 발급 이력 조회 (Fetch Join)
     * JWT 토큰에서 추출한 사용자 이메일을 이용하여 발급 이력을 조회
     *
     * 정렬: 최신 발급 순 (ISSUED_AT DESC)
     *
     * @param userEmail 사용자 이메일 주소
     * @return 발급 이력 목록 (최신순)
     *
     * 사용 예시:
     * List<CertIssueTbl> history = certIssueRepository.findAllByUserEmailOrderByIssuedAtDesc("student@univ.edu");
     */
    @Query("SELECT c FROM CertIssueTbl c " +
           "JOIN FETCH c.user u " +
           "WHERE u.userEmail = :userEmail " +
           "ORDER BY c.issuedAt DESC")
    List<CertIssueTbl> findAllByUserEmailOrderByIssuedAtDesc(@Param("userEmail") String userEmail);

    /**
     * 사용자 이메일과 증명서 유형으로 발급 이력 조회
     * 특정 증명서의 발급 이력만 필터링
     *
     * @param userEmail 사용자 이메일
     * @param certType 증명서 유형 (enrollment, graduation_expected 등)
     * @return 해당 유형의 발급 이력 목록 (최신순)
     *
     * 사용 예시:
     * List<CertIssueTbl> enrollmentHistory = certIssueRepository.findByUserEmailAndCertType("student@univ.edu", "enrollment");
     */
    @Query("SELECT c FROM CertIssueTbl c " +
           "JOIN FETCH c.user u " +
           "WHERE u.userEmail = :userEmail " +
           "AND c.certType = :certType " +
           "ORDER BY c.issuedAt DESC")
    List<CertIssueTbl> findByUserEmailAndCertType(
        @Param("userEmail") String userEmail,
        @Param("certType") String certType
    );

    /**
     * 사용자 이메일로 최근 발급 이력 조회 (1건)
     * 남발 방지 체크 시 사용
     *
     * @param userEmail 사용자 이메일
     * @return 가장 최근 발급 이력 (Optional)
     *
     * 사용 예시:
     * Optional<CertIssueTbl> latest = certIssueRepository.findLatestByUserEmail("student@univ.edu");
     * if (latest.isPresent()) {
     *     LocalDateTime lastIssued = latest.get().getIssuedAt();
     *     // 최근 발급 시간 체크
     * }
     */
    default Optional<CertIssueTbl> findLatestByUserEmail(String userEmail) {
        List<CertIssueTbl> results = findAllByUserEmailOrderByIssuedAtDesc(userEmail);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 사용자별 특정 증명서의 최근 발급 이력 조회 (1건)
     * 특정 증명서 남발 방지 체크 시 사용
     *
     * @param userEmail 사용자 이메일
     * @param certType 증명서 유형
     * @return 해당 증명서의 가장 최근 발급 이력 (Optional)
     */
    default Optional<CertIssueTbl> findLatestByUserEmailAndCertType(String userEmail, String certType) {
        List<CertIssueTbl> results = findByUserEmailAndCertType(userEmail, certType);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 특정 시간 이후 발급된 이력 조회
     * 남발 방지: "최근 N분 이내에 발급한 이력이 있는가?" 체크
     *
     * @param userEmail 사용자 이메일
     * @param afterTime 기준 시간 (이 시간 이후)
     * @return 기준 시간 이후 발급 이력 목록
     *
     * 사용 예시:
     * // 최근 5분 이내 발급 이력 체크
     * LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
     * List<CertIssueTbl> recent = certIssueRepository.findByUserEmailAndIssuedAtAfter("student@univ.edu", fiveMinutesAgo);
     * if (!recent.isEmpty()) {
     *     throw new RuntimeException("최근 5분 이내에 이미 발급되었습니다.");
     * }
     */
    @Query("SELECT c FROM CertIssueTbl c " +
           "JOIN FETCH c.user u " +
           "WHERE u.userEmail = :userEmail " +
           "AND c.issuedAt > :afterTime " +
           "ORDER BY c.issuedAt DESC")
    List<CertIssueTbl> findByUserEmailAndIssuedAtAfter(
        @Param("userEmail") String userEmail,
        @Param("afterTime") LocalDateTime afterTime
    );

    /**
     * 특정 시간 이후 특정 증명서 발급 이력 조회
     * 증명서 유형별 남발 방지
     *
     * @param userEmail 사용자 이메일
     * @param certType 증명서 유형
     * @param afterTime 기준 시간
     * @return 기준 시간 이후 해당 증명서 발급 이력 목록
     */
    @Query("SELECT c FROM CertIssueTbl c " +
           "JOIN FETCH c.user u " +
           "WHERE u.userEmail = :userEmail " +
           "AND c.certType = :certType " +
           "AND c.issuedAt > :afterTime " +
           "ORDER BY c.issuedAt DESC")
    List<CertIssueTbl> findByUserEmailAndCertTypeAndIssuedAtAfter(
        @Param("userEmail") String userEmail,
        @Param("certType") String certType,
        @Param("afterTime") LocalDateTime afterTime
    );

    /**
     * USER_IDX로 발급 이력 조회
     * 내부 시스템에서 사용
     *
     * @param userIdx 사용자 고유 식별자
     * @return 발급 이력 목록 (최신순)
     */
    @Query("SELECT c FROM CertIssueTbl c " +
           "JOIN FETCH c.user u " +
           "WHERE c.user.userIdx = :userIdx " +
           "ORDER BY c.issuedAt DESC")
    List<CertIssueTbl> findAllByUserIdxOrderByIssuedAtDesc(@Param("userIdx") Integer userIdx);

    /**
     * 증명서 유형별 발급 건수 조회
     * 통계 및 대시보드용
     *
     * @param certType 증명서 유형
     * @return 해당 유형의 총 발급 건수
     *
     * 사용 예시:
     * long enrollmentCount = certIssueRepository.countByCertType("enrollment");
     * long graduationCount = certIssueRepository.countByCertType("graduation_expected");
     */
    long countByCertType(String certType);

    /**
     * 특정 기간 내 발급 건수 조회
     * 통계 및 리포트용
     *
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 기간 내 총 발급 건수
     *
     * 사용 예시:
     * // 오늘 발급 건수
     * LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0);
     * LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59);
     * long todayCount = certIssueRepository.countByIssuedAtBetween(startOfDay, endOfDay);
     */
    long countByIssuedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 특정 IP에서 발급된 이력 조회
     * 보안 감사 및 이상 접근 탐지용
     *
     * @param issuedIp 발급 IP 주소
     * @return 해당 IP에서 발급된 이력 목록
     */
    @Query("SELECT c FROM CertIssueTbl c " +
           "JOIN FETCH c.user u " +
           "WHERE c.issuedIp = :issuedIp " +
           "ORDER BY c.issuedAt DESC")
    List<CertIssueTbl> findByIssuedIpOrderByIssuedAtDesc(@Param("issuedIp") String issuedIp);

    /**
     * 사용자의 총 발급 건수 조회
     * 발급 제한 정책 적용 시 사용
     *
     * @param userEmail 사용자 이메일
     * @return 총 발급 건수
     */
    @Query("SELECT COUNT(c) FROM CertIssueTbl c WHERE c.user.userEmail = :userEmail")
    long countByUserEmail(@Param("userEmail") String userEmail);

    /**
     * 사용자의 특정 증명서 총 발급 건수 조회
     *
     * @param userEmail 사용자 이메일
     * @param certType 증명서 유형
     * @return 해당 증명서 총 발급 건수
     */
    @Query("SELECT COUNT(c) FROM CertIssueTbl c WHERE c.user.userEmail = :userEmail AND c.certType = :certType")
    long countByUserEmailAndCertType(
        @Param("userEmail") String userEmail,
        @Param("certType") String certType
    );

    /**
     * 전체 발급 이력 조회 (관리자용)
     * 페이징 처리 권장
     *
     * @return 전체 발급 이력 목록 (최신순)
     */
    @Query("SELECT c FROM CertIssueTbl c " +
           "JOIN FETCH c.user u " +
           "ORDER BY c.issuedAt DESC")
    List<CertIssueTbl> findAllOrderByIssuedAtDesc();

    /**
     * 특정 기간 내 발급 이력 조회 (관리자용)
     *
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 기간 내 발급 이력 목록
     */
    @Query("SELECT c FROM CertIssueTbl c " +
           "JOIN FETCH c.user u " +
           "WHERE c.issuedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY c.issuedAt DESC")
    List<CertIssueTbl> findByIssuedAtBetween(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
