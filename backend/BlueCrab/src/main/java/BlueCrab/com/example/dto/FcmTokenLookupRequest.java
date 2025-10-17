package BlueCrab.com.example.dto;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 사용자 코드 기반으로 FCM 토큰 목록을 조회하기 위한 요청 DTO.
 */
public class FcmTokenLookupRequest {

    @NotEmpty(message = "userCodes 목록은 필수입니다")
    private List<String> userCodes;

    private List<String> platforms;

    private Boolean includeTemporary;

    public FcmTokenLookupRequest() {
    }

    public FcmTokenLookupRequest(List<String> userCodes, List<String> platforms, Boolean includeTemporary) {
        this.userCodes = userCodes;
        this.platforms = platforms;
        this.includeTemporary = includeTemporary;
    }

    public List<String> getUserCodes() {
        return userCodes;
    }

    public void setUserCodes(List<String> userCodes) {
        this.userCodes = userCodes;
    }

    public List<String> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<String> platforms) {
        this.platforms = platforms;
    }

    public Boolean getIncludeTemporary() {
        return includeTemporary;
    }

    public void setIncludeTemporary(Boolean includeTemporary) {
        this.includeTemporary = includeTemporary;
    }
}
