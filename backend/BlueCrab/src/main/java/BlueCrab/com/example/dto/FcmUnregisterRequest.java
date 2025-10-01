package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * FCM 토큰 삭제 요청 DTO (로그아웃용)
 */
public class FcmUnregisterRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다")
    private String fcmToken;

    @NotBlank(message = "플랫폼은 필수입니다")
    @Pattern(regexp = "^(ANDROID|IOS|WEB)$", message = "플랫폼은 ANDROID, IOS, WEB 중 하나여야 합니다")
    private String platform;

    public FcmUnregisterRequest() {}

    public FcmUnregisterRequest(String fcmToken, String platform) {
        this.fcmToken = fcmToken;
        this.platform = platform;
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
}
