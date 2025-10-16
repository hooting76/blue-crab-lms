package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Data-only 방식 배치 푸시 알림 전송 요청 DTO.
 * 여러 FCM 토큰에 동시에 Data-only 메시지를 전송합니다.
 */
public class PushNotificationBatchRequest {

    @NotEmpty(message = "FCM 토큰 목록은 필수입니다.")
    @Size(min = 1, max = 500, message = "토큰은 1개 이상 500개 이하로 전송 가능합니다.")
    private List<String> tokens;

    @NotBlank(message = "알림 제목을 입력해주세요.")
    @Size(max = 120, message = "알림 제목은 120자를 넘을 수 없습니다.")
    private String title;

    @NotBlank(message = "알림 내용을 입력해주세요.")
    @Size(max = 4000, message = "알림 내용은 4000자를 넘을 수 없습니다.")
    private String body;

    private Map<String, String> data;

    public PushNotificationBatchRequest() {
    }

    public PushNotificationBatchRequest(List<String> tokens, String title, String body, Map<String, String> data) {
        this.tokens = tokens;
        this.title = title;
        this.body = body;
        this.data = data;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
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
