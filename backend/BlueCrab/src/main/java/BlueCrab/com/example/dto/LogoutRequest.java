package BlueCrab.com.example.dto;

/**
 * 로그아웃 요청을 위한 DTO 클래스
 * RefreshToken을 포함하여 완전한 토큰 무효화 처리를 위해 사용
 * 
 * AccessToken은 Authorization 헤더에서, RefreshToken은 요청 바디에서 전달받음
 */
public class LogoutRequest {
    
    private String refreshToken;
    
    public LogoutRequest() {}
    
    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    /**
     * 리프레시 토큰 getter
     * @return 리프레시 토큰 문자열
     */
    public String getRefreshToken() {
        return refreshToken;
    }
    
    /**
     * 리프레시 토큰 setter
     * @param refreshToken 리프레시 토큰 문자열
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    @Override
    public String toString() {
        return "LogoutRequest{" +
                "refreshToken='" + (refreshToken != null ? "[PROVIDED]" : "null") + '\'' +
                '}';
    }
}
