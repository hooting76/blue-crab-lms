package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * FCM 브로드캐스트 알림 전송 요청 DTO
 */
public class FcmBroadcastRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String body;

    private List<String> platforms; // null이면 전체 플랫폼
    private Map<String, String> data; // 선택사항
    private FilterOptions filter; // 필터링 옵션

    public FcmBroadcastRequest() {}

    public FcmBroadcastRequest(String title, String body) {
        this.title = title;
        this.body = body;
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

    public FilterOptions getFilter() {
        return filter;
    }

    public void setFilter(FilterOptions filter) {
        this.filter = filter;
    }

    /**
     * 브로드캐스트 필터링 옵션
     */
    public static class FilterOptions {
        private String userType; // STUDENT, PROFESSOR, ALL

        public FilterOptions() {}

        public FilterOptions(String userType) {
            this.userType = userType;
        }

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }
    }
}
