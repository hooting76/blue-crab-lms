package BlueCrab.com.example.entity;

import javax.persistence.*;

/**
 * 사용자 정보를 저장하는 JPA 엔티티 클래스
 * USER_TBL 테이블과 매핑되며, 사용자 등록, 로그인, 프로필 관리 등에 사용
 *
 * 주요 기능:
 * - 사용자 기본 정보 저장 (이메일, 비밀번호, 이름, 전화번호 등)
 * - 주소 정보 관리 (우편번호, 주소1, 주소2)
 * - 등록 정보 추적 (등록일, 등록 IP)
 * - 학생/교수 구분 (userStudent 필드)
 *
 * 데이터베이스 매핑:
 * - 테이블명: USER_TBL
 * - 기본키: USER_IDX (자동 생성)
 *
 * 보안 고려사항:
 * - 비밀번호는 해싱하여 저장 (현재 평문으로 저장되어 있으나, 보안 강화 필요)
 * - 개인정보 필드 (전화번호, 주소, 생년월일)는 암호화 고려
 * - 등록 IP는 보안 감사에 사용
 *
 * 사용 예시:
 * - 회원가입 시 새로운 UserTbl 객체 생성
 * - 로그인 시 이메일로 사용자 조회 및 비밀번호 검증
 * - 프로필 업데이트 시 필드 값 수정
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "USER_TBL")
public class UserTbl {
    
    /**
     * 사용자 고유 식별자
     * 데이터베이스에서 자동 생성되는 기본키
     * 사용자 조회, 수정, 삭제 시 사용
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_IDX")
    private Integer userIdx;
    
    /**
     * 사용자 이메일 주소
     * 로그인 시 사용자명으로 사용되며, 필수 입력 필드
     * 유니크 제약조건이 있어야 하나 현재 설정되지 않음
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 최대 길이: 200자
     *
     * 사용 예시: "student@university.edu"
     */
    @Column(name = "USER_EMAIL", nullable = false, length = 200)
    private String userEmail;
    
    /**
     * 사용자 비밀번호
     * 해싱되지 않은 평문 비밀번호 (보안 취약점)
     * 로그인 시 검증에 사용되며, 필수 입력 필드
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 최대 길이: 200자
     *
     * 보안 권장사항:
     * - BCrypt 또는 Argon2로 해싱하여 저장
     * - 솔트 사용
     * - 패스워드 정책 적용 (최소 길이, 복잡성)
     *
     * ⚠️ 현재 구현은 보안상 취약함 - 즉시 개선 필요
     */
    @Column(name = "USER_PW", nullable = false, length = 200)
    private String userPw;
    
    /**
     * 사용자 실명
     * 필수 입력 필드로, 사용자 식별에 사용
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 최대 길이: 50자
     *
     * 사용 예시: "홍길동"
     */
    @Column(name = "USER_NAME", nullable = false, length = 50)
    private String userName;
    
    /**
     * 사용자 학번/교수 코드
     * 학생의 경우 학번, 교수의 경우 교수 코드로 사용
     * 필수 입력 필드로, 사용자 식별에 사용
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 문자열형 (String)
     *
     * 사용 예시: "202500101000" (학생), "1001" (교수)
     */
    @Column(name = "USER_CODE", nullable = false)
    private String userCode;
    
    /**
     * 사용자 전화번호
     * 필수 입력 필드로, CHAR(11) 타입으로 저장
     * 하이픈 없이 숫자만 저장하는 것이 일반적
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 길이: 정확히 11자 (columnDefinition = "CHAR(11)")
     *
     * 사용 예시: "01012345678"
     *
     * 보안 고려사항:
     * - 개인정보이므로 암호화 저장 고려
     */
    @Column(name = "USER_PHONE", nullable = false, length = 11, columnDefinition = "CHAR(11)")
    private String userPhone;
    
    /**
     * 사용자 생년월일
     * 필수 입력 필드로, 날짜 형식의 문자열로 저장
     * 날짜 포맷은 애플리케이션에서 통일하여 사용
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 최대 길이: 100자 (날짜 문자열로 충분)
     *
     * 사용 예시: "1990-01-01"
     *
     * 보안 고려사항:
     * - 개인정보이므로 암호화 저장 고려
     * - 나이 계산 등에 사용될 수 있음
     */
    @Column(name = "USER_BIRTH", nullable = false, length = 100)
    private String userBirth;
    
    /**
     * 사용자 유형 구분자
     * 학생(0) 또는 교수(1) 구분에 사용
     * 필수 입력 필드
     *
     * 값 의미:
     * - 0: 학생
     * - 1: 교수
     * - 기타 값: 정의되지 않음 (확장 가능)
     *
     * 사용 예시:
     * - 권한 부여 시 사용
     * - UI 표시 분기 시 사용
     */
    @Column(name = "USER_STUDENT", nullable = false)
    private Integer userStudent;
    
    /**
     * 마지막 로그인 시간
     * 선택 입력 필드로, 로그인 시 업데이트
     * 사용자 활동 추적에 사용
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - 최대 길이: 100자
     *
     * 사용 예시: "2024-08-27 14:30:00"
     */
    @Column(name = "USER_LATEST", length = 100)
    private String userLatest;
    
    /**
     * 사용자 우편번호
     * 주소 정보의 일부로, 선택 입력
     * 주소 검색 및 배송 등에 사용
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     *
     * 사용 예시: 12345
     */
    @Column(name = "USER_ZIP")
    private Integer userZip;
    
    /**
     * 사용자 기본 주소
     * 선택 입력 필드로, 주소1 정보 저장
     * 최대 200자까지 저장 가능
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - 최대 길이: 200자
     *
     * 사용 예시: "서울특별시 강남구 테헤란로 123"
     *
     * 보안 고려사항:
     * - 개인정보이므로 암호화 저장 고려
     */
    @Column(name = "USER_FIRST_ADD", length = 200)
    private String userFirstAdd;
    
    /**
     * 사용자 상세 주소
     * 선택 입력 필드로, 주소2 정보 저장
     * 기본 주소에 대한 추가 정보
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - 최대 길이: 100자
     *
     * 사용 예시: "456호"
     *
     * 보안 고려사항:
     * - 개인정보이므로 암호화 저장 고려
     */
    @Column(name = "USER_LAST_ADD", length = 100)
    private String userLastAdd;
    
    /**
     * 사용자 등록 일시
     * 계정 생성 시 자동으로 설정되는 타임스탬프
     * 사용자 등록 순서 파악이나 감사에 사용
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - 최대 길이: 100자
     *
     * 사용 예시: "2024-01-01 10:00:00"
     */
    @Column(name = "USER_REG", length = 100)
    private String userReg;
    
    /**
     * 사용자 등록 IP 주소
     * 계정 생성 시 클라이언트 IP를 기록
     * 보안 감사 및 이상 탐지 시 사용
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - 최대 길이: 100자
     *
     * 사용 예시: "192.168.1.100"
     *
     * 보안 고려사항:
     * - IP 주소는 개인정보로 취급될 수 있음
     * - GDPR 등 개인정보 보호법 준수 필요
     */
    @Column(name = "USER_REG_IP", length = 100)
    private String userRegIp;
    
    /**
     * 프로필 이미지 MinIO 키
     * MinIO 객체 스토리지에 저장된 프로필 이미지의 키 값
     * 프로필 이미지 조회 시 사용
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - 최대 길이: 255자
     *
     * 사용 예시: "profiles/user_123/profile.jpg"
     */
    @Column(name = "PROFILE_IMAGE_KEY", length = 255)
    private String profileImageKey;
    
    /* 강의 평가 데이터 (JSON 배열)
     * 사용자가 작성한 강의 평가 정보를 JSON 형식으로 저장
     * LONGTEXT 타입으로 대용량 JSON 데이터 저장 가능
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - LONGTEXT 타입
     *
     * JSON 구조 예시:
     * [
     *   {
     *     "lectureIdx": 1,
     *     "evaluationDate": "2025-06-15",
     *     "ratings": {
     *       "content": 4,
     *       "material": 5,
     *       "pace": 3,
     *       "attitude": 4,
     *       "overall": 4
     *     },
     *     "comments": "좋은 강의였습니다."
     *   }
     * ]
     */
    @Column(name = "LECTURE_EVALUATIONS", columnDefinition = "LONGTEXT")
    private String lectureEvaluations;
    
    /**
     * 기본 생성자
     * JPA 엔티티 생성을 위해 필수
     * JSON 역직렬화 시에도 사용
     */
    public UserTbl() {}
    
    /**
     * 필수 필드 초기화 생성자
     * 회원가입 시 새로운 사용자 객체 생성에 사용
     *
     * @param userEmail 사용자 이메일 주소
     * @param userPw 사용자 비밀번호 (평문)
     * @param userName 사용자 실명
     * @param userCode 사용자 학번/교수 코드
     * @param userPhone 사용자 전화번호
     * @param userBirth 사용자 생년월일
     * @param userStudent 사용자 유형 (0:학생, 1:교수)
     */
    public UserTbl(String userEmail, String userPw, String userName, String userCode, String userPhone, String userBirth, Integer userStudent) {
        this.userEmail = userEmail;
        this.userPw = userPw;
        this.userName = userName;
        this.userCode = userCode;
        this.userPhone = userPhone;
        this.userBirth = userBirth;
        this.userStudent = userStudent;
    }
    
    // Getters and Setters
    public Integer getUserIdx() {
        return userIdx;
    }
    
    public void setUserIdx(Integer userIdx) {
        this.userIdx = userIdx;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getUserPw() {
        return userPw;
    }
    
    public void setUserPw(String userPw) {
        this.userPw = userPw;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }
    
    public String getUserPhone() {
        return userPhone;
    }
    
    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }
    
    public String getUserBirth() {
        return userBirth;
    }
    
    public void setUserBirth(String userBirth) {
        this.userBirth = userBirth;
    }
    
    public Integer getUserStudent() {
        return userStudent;
    }
    
    public void setUserStudent(Integer userStudent) {
        this.userStudent = userStudent;
    }
    
    public String getUserLatest() {
        return userLatest;
    }
    
    public void setUserLatest(String userLatest) {
        this.userLatest = userLatest;
    }
    
    public Integer getUserZip() {
        return userZip;
    }
    
    public void setUserZip(Integer userZip) {
        this.userZip = userZip;
    }
    
    public String getUserFirstAdd() {
        return userFirstAdd;
    }
    
    public void setUserFirstAdd(String userFirstAdd) {
        this.userFirstAdd = userFirstAdd;
    }
    
    public String getUserLastAdd() {
        return userLastAdd;
    }
    
    public void setUserLastAdd(String userLastAdd) {
        this.userLastAdd = userLastAdd;
    }
    
    public String getUserReg() {
        return userReg;
    }
    
    public void setUserReg(String userReg) {
        this.userReg = userReg;
    }
    
    public String getUserRegIp() {
        return userRegIp;
    }
    
    public void setUserRegIp(String userRegIp) {
        this.userRegIp = userRegIp;
    }
    
    public String getProfileImageKey() {
        return profileImageKey;
    }
    
    public void setProfileImageKey(String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }
    
    public String getLectureEvaluations() {
        return lectureEvaluations;
    }
    
    public void setLectureEvaluations(String lectureEvaluations) {
        this.lectureEvaluations = lectureEvaluations;
    }
    
    /**
     * 사용자 활성화 상태 확인 메서드
     * 현재는 항상 true를 반환하지만, 향후 계정 상태 관리에 사용 가능
     * 예: 계정 잠금, 휴면 계정 등 상태에 따라 false 반환
     *
     * @return 활성화 상태 (현재는 항상 true)
     */
    public boolean isActive() {
        return true; // 기본적으로 활성화, 필요시 비즈니스 로직 추가
    }
    
    /**
     * 사용자명 반환 메서드 (로그인용)
     * Spring Security 등에서 사용자명을 얻기 위해 사용
     * 이메일을 사용자명으로 사용
     *
     * @return 사용자 이메일 주소
     */
    public String getUsername() {
        return this.userEmail; // 이메일을 사용자명으로 사용
    }
    
    /**
     * 비밀번호 반환 메서드
     * Spring Security에서 비밀번호 검증 시 사용
     * ⚠️ 보안상 민감한 정보이므로 주의하여 사용
     *
     * @return 사용자 비밀번호 (평문)
     */
    public String getPassword() {
        return this.userPw;
    }
    
    /**
     * 객체의 문자열 표현을 반환
     * 디버깅 및 로깅 시 사용
     * 민감한 정보(비밀번호)는 포함하지 않음
     *
     * @return 사용자 주요 정보의 문자열 표현
     */
    @Override
    public String toString() {
        return "UserTbl{" +
                "userIdx=" + userIdx +
                ", userEmail='" + userEmail + '\'' +
                ", userName='" + userName + '\'' +
                ", userPhone='" + userPhone + '\'' +
                ", userBirth='" + userBirth + '\'' +
                ", userStudent=" + userStudent +
                '}';
    }
}
