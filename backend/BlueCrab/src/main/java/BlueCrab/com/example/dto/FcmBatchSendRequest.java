package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * FCM 일괄 알림 전송 요청 DTO
 */
public class FcmBatchSendRequest {

    @NotEmpty(message = "사용자 코드 목록은 필수입니다")
    private List<String> userCodes;

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String body;

    private List<String> platforms; // null이면 전체 플랫폼
    private Map<String, String> data; // 선택사항

    public FcmBatchSendRequest() {}

    public FcmBatchSendRequest(List<String> userCodes, String title, String body) {
        this.userCodes = userCodes;
        this.title = title;
        this.body = body;
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

    public List<String> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<String> platforms) {
        this.platforms = platforms;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
