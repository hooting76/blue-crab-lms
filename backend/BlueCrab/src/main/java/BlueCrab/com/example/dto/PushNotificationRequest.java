package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.Map;

/**
 * 관리자용 단일 디바이스 푸시 알림 전송 요청 DTO.
 */
public class PushNotificationRequest {

    @NotBlank(message = "FCM 등록 토큰은 필수입니다.")
    @Size(max = 4096, message = "FCM 등록 토큰은 4096자를 넘을 수 없습니다.")
    private String token;

    @NotBlank(message = "알림 제목을 입력해주세요.")
    @Size(max = 120, message = "알림 제목은 120자를 넘을 수 없습니다.")
    private String title;

    @NotBlank(message = "알림 내용을 입력해주세요.")
    @Size(max = 4000, message = "알림 내용은 4000자를 넘을 수 없습니다.")
    private String body;

    private Map<String, String> data;

    public PushNotificationRequest() {
    }

    public PushNotificationRequest(String token, String title, String body, Map<String, String> data) {
        this.token = token;
        this.title = title;
        this.body = body;
        this.data = data;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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
        return data == null ? Collections.emptyMap() : data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
