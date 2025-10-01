package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * FCM 알림 전송 요청 DTO
 */
public class FcmSendRequest {

    @NotBlank(message = "사용자 코드는 필수입니다")
    private String userCode;

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String body;

    private Map<String, String> data; // 선택사항

    public FcmSendRequest() {}

    public FcmSendRequest(String userCode, String title, String body) {
        this.userCode = userCode;
        this.title = title;
        this.body = body;
    }

    public FcmSendRequest(String userCode, String title, String body, Map<String, String> data) {
        this.userCode = userCode;
        this.title = title;
        this.body = body;
        this.data = data;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
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
}
