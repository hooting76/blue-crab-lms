package BlueCrab.com.example.dto;

import java.util.List;

/**
 * FCM 브로드캐스트 알림 전송 응답 DTO
 */
public class FcmBroadcastResponse {

    private String status;
    private int totalTokens;
    private int successCount;
    private int failureCount;
    private List<String> invalidTokens;

    public FcmBroadcastResponse() {}

    public FcmBroadcastResponse(String status, int totalTokens, int successCount, int failureCount, List<String> invalidTokens) {
        this.status = status;
        this.totalTokens = totalTokens;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.invalidTokens = invalidTokens;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<String> getInvalidTokens() {
        return invalidTokens;
    }

    public void setInvalidTokens(List<String> invalidTokens) {
        this.invalidTokens = invalidTokens;
    }
}
