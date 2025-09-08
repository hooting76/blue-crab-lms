package BlueCrab.com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

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
     * 관리자 비밀번호 (BCrypt 해시)
     */
    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;
    
    /**
     * 관리자 이메일 주소
     * 이메일 인증에 사용
     */
    @Column(name = "EMAIL", nullable = false, unique = true, length = 100)
    private String email;
    
    /**
     * 관리자 이름
     */
    @Column(name = "NAME", nullable = false, length = 50)
    private String name;
    
    /**
     * 계정 상태
     * - active: 활성
     * - suspended: 일시 정지
     * - banned: 영구 차단
     */
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "active";
    
    /**
     * 정지 종료일
     * status가 'suspended'일 때 사용
     */
    @Column(name = "SUSPEND_UNTIL")
    private LocalDateTime suspendUntil;
    
    /**
     * 정지 사유
     */
    @Column(name = "SUSPEND_REASON", length = 500)
    private String suspendReason;
    
    /**
     * 등록일
     */
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 등록 IP 주소
     */
    @Column(name = "CREATED_IP", length = 45)
    private String createdIp;
    
    /**
     * 최종 로그인 일시
     */
    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;
    
    /**
     * 최종 로그인 IP
     */
    @Column(name = "LAST_LOGIN_IP", length = 45)
    private String lastLoginIp;

    // 기본 생성자
    public AdminTbl() {}

    // 생성자
    public AdminTbl(String adminId, String password, String email, String name, String createdIp) {
        this.adminId = adminId;
        this.password = password;
        this.email = email;
        this.name = name;
        this.createdIp = createdIp;
        this.createdAt = LocalDateTime.now();
        this.status = "active";
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSuspendUntil() {
        return suspendUntil;
    }

    public void setSuspendUntil(LocalDateTime suspendUntil) {
        this.suspendUntil = suspendUntil;
    }

    public String getSuspendReason() {
        return suspendReason;
    }

    public void setSuspendReason(String suspendReason) {
        this.suspendReason = suspendReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedIp() {
        return createdIp;
    }

    public void setCreatedIp(String createdIp) {
        this.createdIp = createdIp;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    @Override
    public String toString() {
        return "AdminTbl{" +
                "adminIdx=" + adminIdx +
                ", adminId='" + adminId + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
