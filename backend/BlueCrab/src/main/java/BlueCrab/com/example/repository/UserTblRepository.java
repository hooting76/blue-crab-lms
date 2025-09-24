package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.UserTbl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 정보 데이터베이스 접근을 위한 JPA 리포지토리 인터페이스
 * UserTbl 엔티티에 대한 CRUD 작업 및 커스텀 쿼리를 제공
 *
 * 주요 기능:
 * - 사용자 조회 (이메일, 이름, 키워드 등)
 * - 사용자 존재 여부 확인 (중복 체크용)
 * - 사용자 통계 정보 제공 (전체 수, 학생/교수 수)
 * - 생년월일 범위 검색
 *
 * 사용되는 주요 시나리오:
 * - 로그인 시 이메일로 사용자 조회
 * - 회원가입 시 이메일/전화번호 중복 확인
 * - 사용자 검색 및 필터링
 * - 관리자 대시보드의 통계 정보
 * - 계정 찾기 기능
 *
 * Spring Data JPA의 메서드 네이밍 규칙을 활용한 쿼리 자동 생성
 * 복잡한 쿼리의 경우 @Query 어노테이션으로 JPQL 직접 작성
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface UserTblRepository extends JpaRepository<UserTbl, Integer> {
    
    /**
     * 이메일 주소로 특정 사용자 조회
     * 로그인, 프로필 조회, 계정 찾기 등에 사용
     *
     * Spring Data JPA의 메서드 네이밍 규칙에 따라 자동으로
     * "SELECT u FROM UserTbl u WHERE u.userEmail = ?1" 쿼리 생성
     *
     * @param userEmail 조회할 사용자의 이메일 주소
     * @return Optional<UserTbl> - 사용자가 존재하면 UserTbl 객체, 없으면 empty
     *
     * 사용 예시:
     * Optional<UserTbl> user = userRepository.findByUserEmail("student@university.edu");
     * if (user.isPresent()) {
     *     // 로그인 처리
     * } else {
     *     // 계정 없음 처리
     * }
     */
    Optional<UserTbl> findByUserEmail(String userEmail);
    
    /**
     * 사용자 이름에 특정 문자열이 포함된 사용자 목록 조회
     * 사용자 검색 기능에서 활용
     *
     * Spring Data JPA의 Containing 키워드에 따라 자동으로
     * "SELECT u FROM UserTbl u WHERE u.userName LIKE %?1%" 쿼리 생성
     *
     * @param userName 검색할 이름의 일부 문자열
     * @return List<UserTbl> - 이름에 검색어가 포함된 사용자 목록
     *
     * 사용 예시:
     * List<UserTbl> users = userRepository.findByUserNameContaining("김");
     * // "김철수", "김영희", "박김수" 등이 검색됨
     */
    List<UserTbl> findByUserNameContaining(String userName);
    
    /**
     * 학생/교수 구분에 따라 사용자 목록 조회
     * 학사 관리 시스템에서 학생 또는 교수 목록을 분리하여 조회할 때 사용
     *
     * @param userStudent 구분 값 (1: 학생, 0: 교수)
     * @return List<UserTbl> - 해당 구분에 속하는 사용자 목록
     *
     * 사용 예시:
     * List<UserTbl> students = userRepository.findByUserStudent(1);
     * List<UserTbl> professors = userRepository.findByUserStudent(0);
     */
    List<UserTbl> findByUserStudent(Integer userStudent);
    
    /**
     * 데이터베이스의 전체 사용자 수 조회
     * 관리자 대시보드의 통계 정보에 사용
     *
     * @return long - 등록된 전체 사용자 수
     *
     * 사용 예시:
     * long totalUsers = userRepository.countAllUsers();
     * System.out.println("총 사용자 수: " + totalUsers);
     */
    @Query("SELECT COUNT(u) FROM UserTbl u")
    long countAllUsers();
    
    /**
     * 학생 수 조회
     * 학사 통계 및 대시보드에 사용
     *
     * @return long - 등록된 학생 수 (userStudent = 1)
     *
     * 사용 예시:
     * long studentCount = userRepository.countStudents();
     */
    @Query("SELECT COUNT(u) FROM UserTbl u WHERE u.userStudent = 1")
    long countStudents();
    
    /**
     * 교수 수 조회
     * 학사 통계 및 대시보드에 사용
     *
     * @return long - 등록된 교수 수 (userStudent = 0)
     *
     * 사용 예시:
     * long professorCount = userRepository.countProfessors();
     */
    @Query("SELECT COUNT(u) FROM UserTbl u WHERE u.userStudent = 0")
    long countProfessors();
    
    /**
     * 이메일 주소의 존재 여부 확인
     * 회원가입 시 이메일 중복 체크에 사용
     *
     * Spring Data JPA의 Exists 키워드에 따라 자동으로
     * "SELECT COUNT(u) > 0 FROM UserTbl u WHERE u.userEmail = ?1" 쿼리 생성
     *
     * @param userEmail 확인할 이메일 주소
     * @return boolean - 이메일이 존재하면 true, 없으면 false
     *
     * 사용 예시:
     * if (userRepository.existsByUserEmail("newuser@example.com")) {
     *     throw new DuplicateResourceException("이미 사용중인 이메일입니다.");
     * }
     */
    boolean existsByUserEmail(String userEmail);
    
    /**
     * 전화번호의 존재 여부 확인
     * 회원가입 시 전화번호 중복 체크에 사용
     *
     * @param userPhone 확인할 전화번호
     * @return boolean - 전화번호가 존재하면 true, 없으면 false
     *
     * 사용 예시:
     * if (userRepository.existsByUserPhone("01012345678")) {
     *     throw new DuplicateResourceException("이미 사용중인 전화번호입니다.");
     * }
     */
    boolean existsByUserPhone(String userPhone);
    
    /**
     * 이름 또는 이메일에 키워드가 포함된 사용자 검색
     * 통합 검색 기능에서 사용
     *
     * JPQL 쿼리 설명:
     * - LIKE 연산자로 부분 일치 검색
     * - OR 조건으로 이름과 이메일 모두 검색
     * - %:keyword% 형태로 앞뒤 와일드카드 적용
     *
     * @param keyword 검색할 키워드 (이름이나 이메일의 일부)
     * @return List<UserTbl> - 키워드가 포함된 사용자 목록
     *
     * 사용 예시:
     * List<UserTbl> results = userRepository.findByKeyword("김");
     * // 이름에 "김"이 들어가거나 이메일에 "김"이 들어간 사용자 검색
     */
    @Query("SELECT u FROM UserTbl u WHERE u.userName LIKE %:keyword% OR u.userEmail LIKE %:keyword%")
    List<UserTbl> findByKeyword(@Param("keyword") String keyword);
    
    /**
     * 생년월일 범위로 사용자 검색
     * 연령대별 사용자 조회나 생일자 찾기 기능에 사용
     *
     * JPQL 쿼리 설명:
     * - BETWEEN 연산자로 범위 검색
     * - 생년월일 필드가 String 타입이므로 문자열 비교
     * - 날짜 포맷은 애플리케이션 전체에서 통일하여 사용
     *
     * @param startDate 시작 날짜 (포함, 예: "1990-01-01")
     * @param endDate 종료 날짜 (포함, 예: "1999-12-31")
     * @return List<UserTbl> - 생년월일이 범위 내에 있는 사용자 목록
     *
     * 사용 예시:
     * List<UserTbl> users = userRepository.findByUserBirthBetween("1990-01-01", "1999-12-31");
     * // 1990년대 생 사용자 검색
     */
    @Query("SELECT u FROM UserTbl u WHERE u.userBirth BETWEEN :startDate AND :endDate")
    List<UserTbl> findByUserBirthBetween(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 학번, 이름, 전화번호로 사용자 조회
     * ID 찾기, 비밀번호 찾기 등에서 사용자 신원 확인 시 사용
     *
     * 세 정보가 모두 정확히 일치하는 사용자를 검색
     * 보안을 위해 부분 일치는 허용하지 않음
     *
     * @param userCode 학번/교수 코드
     * @param userName 사용자 이름 (정확한 일치)
     * @param userPhone 전화번호 (정확한 일치)
     * @return Optional<UserTbl> - 일치하는 사용자가 있으면 반환, 없으면 empty
     *
     * 사용 예시:
     * Optional<UserTbl> user = userRepository.findByUserCodeAndUserNameAndUserPhone("202012345", "홍길동", "01012345678");
     */
    Optional<UserTbl> findByUserCodeAndUserNameAndUserPhone(String userCode, String userName, String userPhone);

    /**
     * 이메일, 학번, 이름, 전화번호로 사용자 조회 (비밀번호 재설정용 - 강화된 보안)
     * 4개 필드가 모두 정확히 일치하는 사용자를 검색
     * 보안을 위해 부분 일치는 허용하지 않음
     *
     * @param userEmail 사용자 이메일 (정확한 일치)
     * @param userCode 학번/교수 코드
     * @param userName 사용자 이름 (정확한 일치)
     * @param userPhone 전화번호 (정확한 일치)
     * @return Optional<UserTbl> - 일치하는 사용자가 있으면 반환, 없으면 empty
     *
     * 사용 예시:
     * Optional<UserTbl> user = userRepository.findByUserEmailAndUserCodeAndUserNameAndUserPhone(
     *     "student@university.edu", "202012345", "홍길동", "01012345678");
     */
    Optional<UserTbl> findByUserEmailAndUserCodeAndUserNameAndUserPhone(
        String userEmail, String userCode, String userName, String userPhone);
}
