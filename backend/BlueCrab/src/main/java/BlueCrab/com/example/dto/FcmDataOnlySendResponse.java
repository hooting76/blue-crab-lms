package BlueCrab.com.example.dto;

import java.util.List;

/**
 * Data-only 알림 전송 결과 응답 DTO.
 */
public class FcmDataOnlySendResponse {

    private int totalUsers;
    private int totalTokens;
    private int successCount;
    private int failureCount;
    private List<FcmDataOnlyUserResult> results;

    public FcmDataOnlySendResponse() {
    }

    public FcmDataOnlySendResponse(int totalUsers,
                                   int totalTokens,
                                   int successCount,
                                   int failureCount,
                                   List<FcmDataOnlyUserResult> results) {
        this.totalUsers = totalUsers;
        this.totalTokens = totalTokens;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.results = results;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
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

    public List<FcmDataOnlyUserResult> getResults() {
        return results;
    }

    public void setResults(List<FcmDataOnlyUserResult> results) {
        this.results = results;
    }
}
