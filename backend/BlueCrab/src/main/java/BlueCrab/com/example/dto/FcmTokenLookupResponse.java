package BlueCrab.com.example.dto;

import java.util.List;
import java.util.Map;

/**
 * 사용자별 FCM 토큰 목록 응답 DTO.
 */
public class FcmTokenLookupResponse {

    private String userCode;
    private Map<String, List<String>> tokensByPlatform;
    private List<String> missingPlatforms;

    public FcmTokenLookupResponse() {
    }

    public FcmTokenLookupResponse(String userCode,
                                  Map<String, List<String>> tokensByPlatform,
                                  List<String> missingPlatforms) {
        this.userCode = userCode;
        this.tokensByPlatform = tokensByPlatform;
        this.missingPlatforms = missingPlatforms;
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
}
