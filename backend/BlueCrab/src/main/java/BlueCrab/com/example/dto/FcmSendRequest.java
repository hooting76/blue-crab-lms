package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * FCM 알림 전송 요청 DTO
 * 단일 사용자 또는 여러 사용자에게 전송 가능
 */
public class FcmSendRequest {

    // 기존 방식: 단일 사용자 코드 (선택사항)
    private String userCode;

    // 새로운 방식: 대상 타입 및 대상 목록 (선택사항)
    private String targetType; // "USER", "ROLE", "ALL" 등
    private List<String> targeta; // 대상 목록 (사용자 코드 배열)

    // 제목과 내용은 필수
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

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public List<String> getTargeta() {
        return targeta;
    }

    public void setTargeta(List<String> targeta) {
        this.targeta = targeta;
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
