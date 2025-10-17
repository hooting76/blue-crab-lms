package BlueCrab.com.example.dto;

import java.util.List;
import java.util.Map;

/**
 * 사용자별 Data-only 알림 전송 결과 요약.
 */
public class FcmDataOnlyUserResult {

    private String userCode;
    private Map<String, List<String>> tokensByPlatform;
    private List<String> missingPlatforms;
    private List<String> succeededTokens;
    private Map<String, String> failedTokens;
    private int successCount;
    private int failureCount;

    public FcmDataOnlyUserResult() {
    }

    public FcmDataOnlyUserResult(String userCode,
                                 Map<String, List<String>> tokensByPlatform,
                                 List<String> missingPlatforms,
                                 List<String> succeededTokens,
                                 Map<String, String> failedTokens,
                                 int successCount,
                                 int failureCount) {
        this.userCode = userCode;
        this.tokensByPlatform = tokensByPlatform;
        this.missingPlatforms = missingPlatforms;
        this.succeededTokens = succeededTokens;
        this.failedTokens = failedTokens;
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public Map<String, List<String>> getTokensByPlatform() {
        return tokensByPlatform;
    }

    public void setTokensByPlatform(Map<String, List<String>> tokensByPlatform) {
        this.tokensByPlatform = tokensByPlatform;
    }

    public List<String> getMissingPlatforms() {
        return missingPlatforms;
    }

    public void setMissingPlatforms(List<String> missingPlatforms) {
        this.missingPlatforms = missingPlatforms;
    }

    public List<String> getSucceededTokens() {
        return succeededTokens;
    }

    public void setSucceededTokens(List<String> succeededTokens) {
        this.succeededTokens = succeededTokens;
    }

    public Map<String, String> getFailedTokens() {
        return failedTokens;
    }

    public void setFailedTokens(Map<String, String> failedTokens) {
        this.failedTokens = failedTokens;
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
}
