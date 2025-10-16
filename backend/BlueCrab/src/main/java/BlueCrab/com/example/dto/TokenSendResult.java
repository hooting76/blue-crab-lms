package BlueCrab.com.example.dto;

/**
 * 개별 토큰 전송 결과를 나타내는 DTO.
 */
public class TokenSendResult {

    private String token;
    private boolean success;
    private String messageId;  // 성공 시 FCM 메시지 ID
    private String error;      // 실패 시 에러 메시지

    public TokenSendResult() {
    }

    public TokenSendResult(String token, boolean success, String messageId, String error) {
        this.token = token;
        this.success = success;
        this.messageId = messageId;
        this.error = error;
    }

    // 성공 결과 생성용 정적 메서드
    public static TokenSendResult success(String token, String messageId) {
        return new TokenSendResult(token, true, messageId, null);
    }

    // 실패 결과 생성용 정적 메서드
    public static TokenSendResult failure(String token, String error) {
        return new TokenSendResult(token, false, null, error);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
