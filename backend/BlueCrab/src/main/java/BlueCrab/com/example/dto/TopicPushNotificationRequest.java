package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.Map;

/**
 * 관리자용 토픽 푸시 알림 전송 요청 DTO.
 */
public class TopicPushNotificationRequest {

    @NotBlank(message = "푸시 토픽을 입력해주세요.")
    @Size(max = 900, message = "토픽 이름은 900자를 넘을 수 없습니다.")
    @Pattern(regexp = "[a-zA-Z0-9-_.~%]+", message = "토픽 이름은 문자, 숫자, '-', '_', '.', '~', '%'만 사용할 수 있습니다.")
    private String topic;

    @NotBlank(message = "알림 제목을 입력해주세요.")
    @Size(max = 120, message = "알림 제목은 120자를 넘을 수 없습니다.")
    private String title;

    @NotBlank(message = "알림 내용을 입력해주세요.")
    @Size(max = 4000, message = "알림 내용은 4000자를 넘을 수 없습니다.")
    private String body;

    private Map<String, String> data;

    public TopicPushNotificationRequest() {
    }

    public TopicPushNotificationRequest(String topic, String title, String body, Map<String, String> data) {
        this.topic = topic;
        this.title = title;
        this.body = body;
        this.data = data;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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
