package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.RegistryTbl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REGIST_TABLE에 대한 JPA Repository 인터페이스
 * 학적 정보 조회 및 관리를 위한 데이터 액세스 계층
 *
 * 주요 기능:
 * - 최신 학적 정보 조회 (사용자별)
 * - 특정 시점 기준 학적 스냅샷 조회
 * - 학적 이력 조회
 * - 학적 상태별 필터링
 *
 * 성능 최적화:
 * - Fetch Join으로 N+1 문제 방지
 * - 인덱스 활용 (USER_IDX, ADMIN_REG)
 * - 필요한 컬럼만 조회 (DTO Projection 가능)
 *
 * 참고사항:
 * - 한 사용자당 여러 학적 이력 존재 가능
 * - 최신 학적 = ADMIN_REG DESC NULLS LAST, REG_IDX DESC 정렬 후 첫 번째 행
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-13
 */
@Repository
public interface RegistryRepository extends JpaRepository<RegistryTbl, Integer> {

    /**
     * 사용자 이메일로 최신 학적 정보 조회 (Fetch Join)
     * JWT 토큰에서 추출한 사용자 이메일을 이용하여 최신 학적 정보를 조회
     *
     * 정렬 기준:
     * 1. ADMIN_REG DESC NULLS LAST (처리일시 최신순, NULL은 마지막)
     * 2. REG_IDX DESC (생성순 최신순, 보조 기준)
     *
     * 성능 최적화:
     * - JOIN FETCH로 UserTbl을 즉시 로딩 (N+1 방지)
     * - 인덱스 활용: (USER_IDX, ADMIN_REG)
     *
     * @param userEmail 사용자 이메일 주소
     * @return 최신 학적 정보 (Optional)
     *
     * 사용 예시:
     * Optional<RegistryTbl> latest = registryRepository.findLatestByUserEmail("student@univ.edu");
     */
    @Query("SELECT r FROM RegistryTbl r " +
           "JOIN FETCH r.user u " +
           "WHERE u.userEmail = :userEmail " +
           "ORDER BY r.adminReg DESC NULLS LAST, r.regIdx DESC")
    List<RegistryTbl> findAllByUserEmailOrderByLatest(@Param("userEmail") String userEmail);

    /**
     * 최신 학적 정보 조회 (단건)
     * findAllByUserEmailOrderByLatest의 첫 번째 결과만 반환
     *
     * @param userEmail 사용자 이메일 주소
     * @return 최신 학적 정보 (Optional)
     */
    default Optional<RegistryTbl> findLatestByUserEmail(String userEmail) {
        List<RegistryTbl> results = findAllByUserEmailOrderByLatest(userEmail);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 사용자 IDX로 최신 학적 정보 조회
     * 내부 시스템에서 USER_IDX를 알고 있을 때 사용
     *
     * @param userIdx 사용자 고유 식별자
     * @return 최신 학적 정보 (Optional)
     */
    @Query("SELECT r FROM RegistryTbl r " +
           "JOIN FETCH r.user u " +
           "WHERE r.user.userIdx = :userIdx " +
           "ORDER BY r.adminReg DESC NULLS LAST, r.regIdx DESC")
    List<RegistryTbl> findAllByUserIdxOrderByLatest(@Param("userIdx") Integer userIdx);

    /**
     * USER_IDX로 최신 학적 정보 조회 (단건)
     *
     * @param userIdx 사용자 고유 식별자
     * @return 최신 학적 정보 (Optional)
     */
    default Optional<RegistryTbl> findLatestByUserIdx(Integer userIdx) {
        List<RegistryTbl> results = findAllByUserIdxOrderByLatest(userIdx);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 특정 시점 기준 학적 정보 조회 (As-Of Query)
     * 증명서 발급 시 특정 날짜 기준의 학적 상태를 조회할 때 사용
     *
     * 조건:
     * - ADMIN_REG <= asOfDate (처리일시가 기준일 이전)
     * - 정렬: ADMIN_REG DESC, REG_IDX DESC
     *
     * @param userEmail 사용자 이메일
     * @param asOfDate 기준 일시 (이 시점까지의 학적 반영)
     * @return 기준 시점의 학적 정보 (Optional)
     *
     * 사용 예시:
     * // 2025년 3월 1일 기준 학적 상태 조회
     * LocalDateTime asOf = LocalDateTime.of(2025, 3, 1, 23, 59, 59);
     * Optional<RegistryTbl> snapshot = registryRepository.findLatestByUserEmailAsOf("student@univ.edu", asOf);
     */
    @Query("SELECT r FROM RegistryTbl r " +
           "JOIN FETCH r.user u " +
           "WHERE u.userEmail = :userEmail " +
           "AND (r.adminReg IS NULL OR r.adminReg <= :asOfDate) " +
           "ORDER BY r.adminReg DESC NULLS LAST, r.regIdx DESC")
    List<RegistryTbl> findAllByUserEmailAsOf(
        @Param("userEmail") String userEmail,
        @Param("asOfDate") LocalDateTime asOfDate
    );

    /**
     * 특정 시점 기준 학적 정보 조회 (단건)
     *
     * @param userEmail 사용자 이메일
     * @param asOfDate 기준 일시
     * @return 기준 시점의 학적 정보 (Optional)
     */
    default Optional<RegistryTbl> findLatestByUserEmailAsOf(String userEmail, LocalDateTime asOfDate) {
        List<RegistryTbl> results = findAllByUserEmailAsOf(userEmail, asOfDate);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 사용자 이메일로 모든 학적 이력 조회
     * 학적 변경 히스토리 조회 시 사용
     *
     * @param userEmail 사용자 이메일
     * @return 학적 이력 목록 (최신순 정렬)
     */
    @Query("SELECT r FROM RegistryTbl r " +
           "JOIN FETCH r.user u " +
           "WHERE u.userEmail = :userEmail " +
           "ORDER BY r.adminReg DESC NULLS LAST, r.regIdx DESC")
    List<RegistryTbl> findAllHistoryByUserEmail(@Param("userEmail") String userEmail);

    /**
     * 특정 학적 상태인 사용자 수 조회
     * 통계 및 대시보드용
     *
     * @param stdStat 학적 상태 (재학, 휴학, 졸업 등)
     * @return 해당 상태의 사용자 수
     *
     * 주의:
     * - 중복 카운트 방지를 위해 최신 학적만 카운트하도록 서브쿼리 필요
     * - 현재는 단순 카운트이므로 개선 필요
     */
    long countByStdStat(String stdStat);

    /**
     * 학번/교번으로 최신 학적 조회
     * 관리자 페이지에서 학번으로 검색 시 사용
     *
     * @param userCode 학번/교번
     * @return 최신 학적 정보 (Optional)
     */
    @Query("SELECT r FROM RegistryTbl r " +
           "JOIN FETCH r.user u " +
           "WHERE r.userCode = :userCode " +
           "ORDER BY r.adminReg DESC NULLS LAST, r.regIdx DESC")
    List<RegistryTbl> findAllByUserCodeOrderByLatest(@Param("userCode") String userCode);

    /**
     * 학번/교번으로 최신 학적 조회 (단건)
     *
     * @param userCode 학번/교번
     * @return 최신 학적 정보 (Optional)
     */
    default Optional<RegistryTbl> findLatestByUserCode(String userCode) {
        List<RegistryTbl> results = findAllByUserCodeOrderByLatest(userCode);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 사용자 이메일로 학적 정보 존재 여부 확인
     * 학적 등록 여부 체크 시 사용
     *
     * @param userEmail 사용자 이메일
     * @return 학적 정보 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM RegistryTbl r " +
           "WHERE r.user.userEmail = :userEmail")
    boolean existsByUserEmail(@Param("userEmail") String userEmail);

    /**
     * 이수 학기 수 기준으로 사용자 조회
     * 졸업 예정자 필터링 등에 사용
     *
     * @param minTerms 최소 이수 학기 수
     * @return 조건에 맞는 학적 목록
     *
     * 사용 예시:
     * // 7학기 이상 이수자 조회 (졸업예정자)
     * List<RegistryTbl> graduationCandidates = registryRepository.findByCntTermGreaterThanEqual(7);
     */
    @Query("SELECT r FROM RegistryTbl r " +
           "JOIN FETCH r.user u " +
           "WHERE r.cntTerm >= :minTerms " +
           "ORDER BY r.adminReg DESC NULLS LAST, r.regIdx DESC")
    List<RegistryTbl> findByCntTermGreaterThanEqual(@Param("minTerms") Integer minTerms);
}
