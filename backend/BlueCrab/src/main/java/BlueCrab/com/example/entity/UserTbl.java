package BlueCrab.com.example.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

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
 * @version 1.1.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "USER_TBL")
@Getter
@Setter
public class UserTbl {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_IDX")
    private Integer userIdx;
    
    @Column(name = "USER_EMAIL", nullable = false, length = 200)
    private String userEmail;
    
    @Column(name = "USER_PW", nullable = false, length = 200)
    private String userPw;
    
    @Getter(AccessLevel.NONE)
    @Column(name = "USER_NAME", nullable = false, length = 50)
    private String userName;
    
    @Column(name = "USER_CODE", nullable = false)
    private String userCode;
    
    @Column(name = "USER_PHONE", nullable = false, length = 11, columnDefinition = "CHAR(11)")
    private String userPhone;
    
    @Column(name = "USER_BIRTH", nullable = false, length = 100)
    private String userBirth;
    
    @Column(name = "USER_STUDENT", nullable = false)
    private Integer userStudent;
    
    @Column(name = "USER_LATEST", length = 100)
    private String userLatest;
    
    @Column(name = "USER_ZIP")
    private Integer userZip;
    
    @Column(name = "USER_FIRST_ADD", length = 200)
    private String userFirstAdd;
    
    @Column(name = "USER_LAST_ADD", length = 100)
    private String userLastAdd;
    
    @Column(name = "USER_REG", length = 100)
    private String userReg;
    
    @Column(name = "USER_REG_IP", length = 100)
    private String userRegIp;
    
    @Column(name = "PROFILE_IMAGE_KEY", length = 255)
    private String profileImageKey;
    
    @Column(name = "LECTURE_EVALUATIONS", columnDefinition = "LONGTEXT")
    private String lectureEvaluations;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "USER_IDX", referencedColumnName = "USER_IDX")
    private SerialCodeTable serialCodeTable;

    @Transient
    private String majorFacultyCode;
    @Transient
    private String majorDeptCode;
    @Transient
    private String minorFacultyCode;
    @Transient
    private String minorDeptCode;

    public UserTbl() {}
    
    public UserTbl(String userEmail, String userPw, String userName, String userCode, String userPhone, String userBirth, Integer userStudent) {
        this.userEmail = userEmail;
        this.userPw = userPw;
        this.userName = userName;
        this.userCode = userCode;
        this.userPhone = userPhone;
        this.userBirth = userBirth;
        this.userStudent = userStudent;
    }
    
    public boolean isActive() {
        return true;
    }
    
    public String getUsername() {
        return this.userEmail;
    }
    
    public String getPassword() {
        return this.userPw;
    }

    public String getUserName() {
        return this.userName;
    }
    
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
