package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.ProfileView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PROFILE_VIEW 뷰테이블에 대한 JPA Repository 인터페이스
 * 사용자 프로필 정보 조회를 위한 데이터 액세스 계층
 *
 * 주요 기능:
 * - 이메일을 통한 프로필 조회 (로그인 사용자 기준)
 * - 뷰테이블이므로 읽기 전용 작업만 제공
 *
 * 참고사항:
 * - ProfileView는 @Immutable 설정으로 수정 작업 불가
 * - 실제 데이터 수정은 개별 테이블(USER_TBL, REGIST_TABLE 등)을 통해 처리
 */
@Repository
public interface ProfileViewRepository extends JpaRepository<ProfileView, String> {

    /**
     * 사용자 이메일로 프로필 정보 조회
     * JWT 토큰에서 추출한 사용자 이메일을 이용하여 프로필 정보를 조회
     *
     * @param userEmail 사용자 이메일 주소
     * @return 해당 사용자의 프로필 정보 (Optional)
     */
    Optional<ProfileView> findByUserEmail(String userEmail);

    /**
     * 사용자 이메일로 프로필 정보 존재 여부 확인
     * 프로필 정보 접근 권한 체크 시 사용
     *
     * @param userEmail 사용자 이메일 주소
     * @return 프로필 정보 존재 여부
     */
    boolean existsByUserEmail(String userEmail);

    /**
     * 특정 사용자의 학적 상태 조회
     * 추가적인 권한 체크나 상태 확인 시 사용 가능
     *
     * @param userEmail 사용자 이메일 주소
     * @return 해당 사용자의 학적 상태
     */
    @Query("SELECT p.academicStatus FROM ProfileView p WHERE p.userEmail = :userEmail")
    Optional<String> findAcademicStatusByUserEmail(@Param("userEmail") String userEmail);

    /**
     * 특정 사용자의 사용자 유형 조회
     * 학생/교수 구분이 필요한 경우 사용
     *
     * @param userEmail 사용자 이메일 주소
     * @return 사용자 유형 (0: 학생, 1: 교수)
     */
    @Query("SELECT p.userType FROM ProfileView p WHERE p.userEmail = :userEmail")
    Optional<Integer> findUserTypeByUserEmail(@Param("userEmail") String userEmail);
}