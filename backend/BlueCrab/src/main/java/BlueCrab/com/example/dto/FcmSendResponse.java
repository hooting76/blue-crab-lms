package BlueCrab.com.example.dto;

import java.util.Map;

/**
 * FCM 알림 전송 응답 DTO
 */
public class FcmSendResponse {

    private String status;
    private Map<String, Boolean> sent; // platform -> success
    private Map<String, String> failedReasons; // platform -> reason

    public FcmSendResponse() {}

    public FcmSendResponse(String status, Map<String, Boolean> sent, Map<String, String> failedReasons) {
        this.status = status;
        this.sent = sent;
        this.failedReasons = failedReasons;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Boolean> getSent() {
        return sent;
    }

    public void setSent(Map<String, Boolean> sent) {
        this.sent = sent;
    }

    public Map<String, String> getFailedReasons() {
        return failedReasons;
    }

    public void setFailedReasons(Map<String, String> failedReasons) {
        this.failedReasons = failedReasons;
    }
}
