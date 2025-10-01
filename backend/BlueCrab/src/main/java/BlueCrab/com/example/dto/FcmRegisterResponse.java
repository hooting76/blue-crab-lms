package BlueCrab.com.example.dto;

import java.time.LocalDateTime;

/**
 * FCM 토큰 등록 응답 DTO
 */
public class FcmRegisterResponse {

    private String status; // registered, renewed, conflict
    private String message;
    private String platform;
    private LocalDateTime lastUsed;

    public FcmRegisterResponse() {}

    public FcmRegisterResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public FcmRegisterResponse(String status, String message, String platform, LocalDateTime lastUsed) {
        this.status = status;
        this.message = message;
        this.platform = platform;
        this.lastUsed = lastUsed;
    }

    // Static factory methods
    public static FcmRegisterResponse registered() {
        return new FcmRegisterResponse("registered", "알림이 활성화되었습니다");
    }

    public static FcmRegisterResponse renewed() {
        return new FcmRegisterResponse("renewed", "토큰이 갱신되었습니다");
    }

    public static FcmRegisterResponse conflict(String platform, LocalDateTime lastUsed) {
        return new FcmRegisterResponse("conflict", "이미 다른 기기에서 알림을 받고 있습니다", platform, lastUsed);
    }

    public static FcmRegisterResponse replaced() {
        return new FcmRegisterResponse("replaced", "기기가 변경되었습니다");
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }
}
