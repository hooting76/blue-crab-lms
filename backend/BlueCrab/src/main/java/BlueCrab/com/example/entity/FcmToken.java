package BlueCrab.com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * FCM 토큰 정보를 저장하는 JPA 엔티티 클래스
 * FCM_TOKEN_TABLE 테이블과 매핑되며, 푸시알림 전송에 사용
 *
 * 주요 기능:
 * - 플랫폼별(Android, iOS, Web) FCM 토큰 관리
 * - 사용자당 각 플랫폼별 1개 기기 지원
 * - 토큰별 마지막 사용 시간 추적
 *
 * 데이터베이스 매핑:
 * - 테이블명: FCM_TOKEN_TABLE
 * - 기본키: FCM_IDX (자동 생성)
 * - 외래키: USER_IDX (USER_TBL 참조)
 */
@Entity
@Table(name = "FCM_TOKEN_TABLE", uniqueConstraints = {
    @UniqueConstraint(name = "uq_fcm_token_user", columnNames = "USER_IDX")
})
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FCM_IDX")
    private Integer fcmIdx;

    @Column(name = "USER_IDX", nullable = false, unique = true)
    private Integer userIdx;

    @Column(name = "USER_CODE", nullable = false, length = 255)
    private String userCode;

    @Column(name = "FCM_TOKEN_ANDROID", length = 255)
    private String fcmTokenAndroid;

    @Column(name = "FCM_TOKEN_IOS", length = 255)
    private String fcmTokenIos;

    @Column(name = "FCM_TOKEN_WEB", length = 255)
    private String fcmTokenWeb;

    @Column(name = "FCM_TOKEN_ANDROID_LAST_USED")
    private LocalDateTime fcmTokenAndroidLastUsed;

    @Column(name = "FCM_TOKEN_IOS_LAST_USED")
    private LocalDateTime fcmTokenIosLastUsed;

    @Column(name = "FCM_TOKEN_WEB_LAST_USED")
    private LocalDateTime fcmTokenWebLastUsed;

    @Column(name = "FCM_TOKEN_ANDROID_KEEP_SIGNED_IN")
    private Boolean fcmTokenAndroidKeepSignedIn;

    @Column(name = "FCM_TOKEN_IOS_KEEP_SIGNED_IN")
    private Boolean fcmTokenIosKeepSignedIn;

    @Column(name = "FCM_TOKEN_WEB_KEEP_SIGNED_IN")
    private Boolean fcmTokenWebKeepSignedIn;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public FcmToken() {}

    public FcmToken(Integer userIdx, String userCode) {
        this.userIdx = userIdx;
        this.userCode = userCode;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getFcmIdx() {
        return fcmIdx;
    }

    public void setFcmIdx(Integer fcmIdx) {
        this.fcmIdx = fcmIdx;
    }

    public Integer getUserIdx() {
        return userIdx;
    }

    public void setUserIdx(Integer userIdx) {
        this.userIdx = userIdx;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getFcmTokenAndroid() {
        return fcmTokenAndroid;
    }

    public void setFcmTokenAndroid(String fcmTokenAndroid) {
        this.fcmTokenAndroid = fcmTokenAndroid;
    }

    public String getFcmTokenIos() {
        return fcmTokenIos;
    }

    public void setFcmTokenIos(String fcmTokenIos) {
        this.fcmTokenIos = fcmTokenIos;
    }

    public String getFcmTokenWeb() {
        return fcmTokenWeb;
    }

    public void setFcmTokenWeb(String fcmTokenWeb) {
        this.fcmTokenWeb = fcmTokenWeb;
    }

    public LocalDateTime getFcmTokenAndroidLastUsed() {
        return fcmTokenAndroidLastUsed;
    }

    public void setFcmTokenAndroidLastUsed(LocalDateTime fcmTokenAndroidLastUsed) {
        this.fcmTokenAndroidLastUsed = fcmTokenAndroidLastUsed;
    }

    public LocalDateTime getFcmTokenIosLastUsed() {
        return fcmTokenIosLastUsed;
    }

    public void setFcmTokenIosLastUsed(LocalDateTime fcmTokenIosLastUsed) {
        this.fcmTokenIosLastUsed = fcmTokenIosLastUsed;
    }

    public LocalDateTime getFcmTokenWebLastUsed() {
        return fcmTokenWebLastUsed;
    }

    public void setFcmTokenWebLastUsed(LocalDateTime fcmTokenWebLastUsed) {
        this.fcmTokenWebLastUsed = fcmTokenWebLastUsed;
    }

    public Boolean getFcmTokenAndroidKeepSignedIn() {
        return fcmTokenAndroidKeepSignedIn;
    }

    public void setFcmTokenAndroidKeepSignedIn(Boolean fcmTokenAndroidKeepSignedIn) {
        this.fcmTokenAndroidKeepSignedIn = fcmTokenAndroidKeepSignedIn;
    }

    public Boolean getFcmTokenIosKeepSignedIn() {
        return fcmTokenIosKeepSignedIn;
    }

    public void setFcmTokenIosKeepSignedIn(Boolean fcmTokenIosKeepSignedIn) {
        this.fcmTokenIosKeepSignedIn = fcmTokenIosKeepSignedIn;
    }

    public Boolean getFcmTokenWebKeepSignedIn() {
        return fcmTokenWebKeepSignedIn;
    }

    public void setFcmTokenWebKeepSignedIn(Boolean fcmTokenWebKeepSignedIn) {
        this.fcmTokenWebKeepSignedIn = fcmTokenWebKeepSignedIn;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 플랫폼별 토큰 조회
     */
    public String getTokenByPlatform(String platform) {
        switch (platform.toUpperCase()) {
            case "ANDROID":
                return fcmTokenAndroid;
            case "IOS":
                return fcmTokenIos;
            case "WEB":
                return fcmTokenWeb;
            default:
                return null;
        }
    }

    /**
     * 플랫폼별 토큰 설정
     */
    public void setTokenByPlatform(String platform, String token) {
        switch (platform.toUpperCase()) {
            case "ANDROID":
                this.fcmTokenAndroid = token;
                break;
            case "IOS":
                this.fcmTokenIos = token;
                break;
            case "WEB":
                this.fcmTokenWeb = token;
                break;
        }
    }

    /**
     * 플랫폼별 마지막 사용 시간 조회
     */
    public LocalDateTime getLastUsedByPlatform(String platform) {
        switch (platform.toUpperCase()) {
            case "ANDROID":
                return fcmTokenAndroidLastUsed;
            case "IOS":
                return fcmTokenIosLastUsed;
            case "WEB":
                return fcmTokenWebLastUsed;
            default:
                return null;
        }
    }

    /**
     * 플랫폼별 마지막 사용 시간 설정
     */
    public void setLastUsedByPlatform(String platform, LocalDateTime lastUsed) {
        switch (platform.toUpperCase()) {
            case "ANDROID":
                this.fcmTokenAndroidLastUsed = lastUsed;
                break;
            case "IOS":
                this.fcmTokenIosLastUsed = lastUsed;
                break;
            case "WEB":
                this.fcmTokenWebLastUsed = lastUsed;
                break;
        }
    }

    /**
     * 플랫폼별 로그인 상태 유지 여부 조회
     */
    public Boolean getKeepSignedInByPlatform(String platform) {
        switch (platform.toUpperCase()) {
            case "ANDROID":
                return fcmTokenAndroidKeepSignedIn;
            case "IOS":
                return fcmTokenIosKeepSignedIn;
            case "WEB":
                return fcmTokenWebKeepSignedIn;
            default:
                return null;
        }
    }

    /**
     * 플랫폼별 로그인 상태 유지 여부 설정
     */
    public void setKeepSignedInByPlatform(String platform, Boolean keepSignedIn) {
        switch (platform.toUpperCase()) {
            case "ANDROID":
                this.fcmTokenAndroidKeepSignedIn = keepSignedIn;
                break;
            case "IOS":
                this.fcmTokenIosKeepSignedIn = keepSignedIn;
                break;
            case "WEB":
                this.fcmTokenWebKeepSignedIn = keepSignedIn;
                break;
        }
    }
}
