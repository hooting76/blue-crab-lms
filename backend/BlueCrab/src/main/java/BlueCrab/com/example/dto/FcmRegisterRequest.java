package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * FCM 토큰 등록 요청 DTO
 */
public class FcmRegisterRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다")
    private String fcmToken;

    @NotBlank(message = "플랫폼은 필수입니다")
    @Pattern(regexp = "(?i)^(ANDROID|IOS|WEB)$", message = "플랫폼은 ANDROID, IOS, WEB 중 하나여야 합니다 (대소문자 구분 없음)")
    private String platform;

    private Boolean keepSignedIn;

    private Boolean temporaryOnly; // 충돌 시 DB 덮어쓰기 거부하고 임시 등록만

    public FcmRegisterRequest() {}

    public FcmRegisterRequest(String fcmToken, String platform) {
        this.fcmToken = fcmToken;
        this.platform = platform;
    }

    public FcmRegisterRequest(String fcmToken, String platform, Boolean keepSignedIn) {
        this.fcmToken = fcmToken;
        this.platform = platform;
        this.keepSignedIn = keepSignedIn;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Boolean getKeepSignedIn() {
        return keepSignedIn;
    }

    public void setKeepSignedIn(Boolean keepSignedIn) {
        this.keepSignedIn = keepSignedIn;
    }

    public Boolean getTemporaryOnly() {
        return temporaryOnly;
    }

    public void setTemporaryOnly(Boolean temporaryOnly) {
        this.temporaryOnly = temporaryOnly;
    }
}
