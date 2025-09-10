package BlueCrab.com.example.entity;

import javax.persistence.*;

/**
 * 관리자 정보를 저장하는 JPA 엔티티 클래스
 * ADMIN_TBL 테이블과 매핑되며, 관리자 로그인, 권한 관리 등에 사용
 *
 * 주요 기능:
 * - 관리자 기본 정보 저장 (관리자ID, 비밀번호, 이메일, 이름)
 * - 계정 상태 관리 (활성, 정지, 차단)
 * - 정지 정보 관리 (정지 종료일, 정지 사유)
 * - 등록 정보 추적 (등록일, 등록 IP)
 *
 * 데이터베이스 매핑:
 * - 테이블명: ADMIN_TBL
 * - 기본키: ADMIN_IDX (자동 생성)
 *
 * 보안 고려사항:
 * - 비밀번호는 BCrypt 해싱하여 저장
 * - 이메일 인증을 통한 2단계 로그인
 * - 계정 상태 관리를 통한 보안 강화
 */
@Entity
@Table(name = "ADMIN_TBL")
public class AdminTbl {
    // 성태준 추가: 관리자 이름 별도 필드
    @Column(name = "ADMIN_NAME", nullable = false, length = 100)
    private String adminName;

    // 성태준 추가: 관리자 이름 getter/setter
    public String getAdminName() {
        return adminName;
    }
    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }
    
    /**
     * 관리자 고유 식별자
     * 데이터베이스에서 자동 생성되는 기본키
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ADMIN_IDX")
    private Integer adminIdx;
    
    /**
     * 관리자 로그인 ID (이메일 형식)
     * 로그인 시 사용되는 고유 식별자이며 이메일 주소 형태
     */
    @Column(name = "ADMIN_ID", nullable = false, unique = true, length = 100)
    private String adminId;
    
    /**
     * 관리자 시스템 권한 (1: 시스템 관리자, 0: 일반 관리자)
     */
    @Column(name = "ADMIN_SYS", nullable = false)
    private Integer adminSys = 0;
    
    /**
     * 관리자 비밀번호 (해시)
     */
    @Column(name = "ADMIN_PW", nullable = false, length = 255)
    private String password;
    
    /**
     * 관리자 이름
     */
    @Column(name = "ADMIN_NAME", nullable = false, length = 100)
    private String name;
    
    /**
     * 관리자 전화번호
     */
    @Column(name = "ADMIN_PHONE", length = 11)
    private String adminPhone;
    
    /**
     * 관리자 사무실
     */
    @Column(name = "ADMIN_OFFICE", length = 11)
    private String adminOffice;
    
    /**
     * 마지막 접속 시간
     */
    @Column(name = "ADMIN_LATEST", length = 100)
    private String adminLatest;
    
    /**
     * 마지막 접속 IP
     */
    @Column(name = "ADMIN_LATEST_IP", length = 50)
    private String adminLatestIp;
    
    /**
     * 등록 일시
     */
    @Column(name = "ADMIN_REG", length = 100)
    private String adminReg;
    
    /**
     * 등록 IP
     */
    @Column(name = "ADMIN_REG_IP", length = 100)
    private String adminRegIp;

    // 기본 생성자
    public AdminTbl() {}

    // 생성자
    public AdminTbl(String adminId, String password, String name, String adminRegIp) {
        this.adminId = adminId;
        this.password = password;
        this.name = name;
        this.adminRegIp = adminRegIp;
        this.adminReg = java.time.LocalDateTime.now().toString();
    }

    // Getters and Setters
    public Integer getAdminIdx() {
        return adminIdx;
    }

    public void setAdminIdx(Integer adminIdx) {
        this.adminIdx = adminIdx;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getAdminSys() {
        return adminSys;
    }

    public void setAdminSys(Integer adminSys) {
        this.adminSys = adminSys;
    }

    /**
     * 이메일 주소 반환 (adminId가 이메일 역할)
     */
    public String getEmail() {
        return adminId;
    }

    /**
     * 이메일 설정 (adminId에 설정)
     */
    public void setEmail(String email) {
        this.adminId = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdminPhone() {
        return adminPhone;
    }

    public void setAdminPhone(String adminPhone) {
        this.adminPhone = adminPhone;
    }

    public String getAdminOffice() {
        return adminOffice;
    }

    public void setAdminOffice(String adminOffice) {
        this.adminOffice = adminOffice;
    }

    public String getAdminLatest() {
        return adminLatest;
    }

    public void setAdminLatest(String adminLatest) {
        this.adminLatest = adminLatest;
    }

    public String getAdminLatestIp() {
        return adminLatestIp;
    }

    public void setAdminLatestIp(String adminLatestIp) {
        this.adminLatestIp = adminLatestIp;
    }

    public String getAdminReg() {
        return adminReg;
    }

    public void setAdminReg(String adminReg) {
        this.adminReg = adminReg;
    }

    public String getAdminRegIp() {
        return adminRegIp;
    }

    public void setAdminRegIp(String adminRegIp) {
        this.adminRegIp = adminRegIp;
    }

    @Override
    public String toString() {
        return "AdminTbl{" +
                "adminIdx=" + adminIdx +
                ", adminId='" + adminId + '\'' +
                ", adminSys=" + adminSys +
                ", name='" + name + '\'' +
                ", adminPhone='" + adminPhone + '\'' +
                ", adminOffice='" + adminOffice + '\'' +
                ", adminLatest='" + adminLatest + '\'' +
                ", adminLatestIp='" + adminLatestIp + '\'' +
                ", adminReg='" + adminReg + '\'' +
                ", adminRegIp='" + adminRegIp + '\'' +
                '}';
    }
}
