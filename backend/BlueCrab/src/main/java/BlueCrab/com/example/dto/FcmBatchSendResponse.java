package BlueCrab.com.example.dto;

import java.util.List;
import java.util.Map;

/**
 * FCM 일괄 알림 전송 응답 DTO
 */
public class FcmBatchSendResponse {

    private String status;
    private int totalUsers;
    private int successCount;
    private int failureCount;
    private List<UserSendDetail> details;

    public FcmBatchSendResponse() {}

    public FcmBatchSendResponse(String status, int totalUsers, int successCount, int failureCount, List<UserSendDetail> details) {
        this.status = status;
        this.totalUsers = totalUsers;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.details = details;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
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

    public List<UserSendDetail> getDetails() {
        return details;
    }

    public void setDetails(List<UserSendDetail> details) {
        this.details = details;
    }

    /**
     * 사용자별 전송 상세 정보
     */
    public static class UserSendDetail {
        private String userCode;
        private Map<String, Boolean> sent;
        private Map<String, String> failed;

        public UserSendDetail() {}

        public UserSendDetail(String userCode, Map<String, Boolean> sent, Map<String, String> failed) {
            this.userCode = userCode;
            this.sent = sent;
            this.failed = failed;
        }

        public String getUserCode() {
            return userCode;
        }

        public void setUserCode(String userCode) {
            this.userCode = userCode;
        }

        public Map<String, Boolean> getSent() {
            return sent;
        }

        public void setSent(Map<String, Boolean> sent) {
            this.sent = sent;
        }

        public Map<String, String> getFailed() {
            return failed;
        }

        public void setFailed(Map<String, String> failed) {
            this.failed = failed;
        }
    }
}
