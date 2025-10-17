package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * 사용자 코드 기반 Data-only 알림 전송 요청 DTO.
 */
public class FcmDataOnlySendRequest {

    @NotEmpty(message = "userCodes 목록은 필수입니다")
    private List<String> userCodes;

    @NotBlank(message = "title은 필수입니다")
    private String title;

    @NotBlank(message = "body는 필수입니다")
    private String body;

    private Map<String, String> data;

    private List<String> platforms;

    private Boolean includeTemporary;

    public FcmDataOnlySendRequest() {
    }

    public FcmDataOnlySendRequest(List<String> userCodes,
                                  String title,
                                  String body,
                                  Map<String, String> data,
                                  List<String> platforms,
                                  Boolean includeTemporary) {
        this.userCodes = userCodes;
        this.title = title;
        this.body = body;
        this.data = data;
        this.platforms = platforms;
        this.includeTemporary = includeTemporary;
    }

    public List<String> getUserCodes() {
        return userCodes;
    }

    public void setUserCodes(List<String> userCodes) {
        this.userCodes = userCodes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
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
