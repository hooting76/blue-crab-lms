package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;

/**
 * 관리자 리프레시 토큰 요청 DTO
 * 액세스 토큰 재발급을 위한 리프레시 토큰을 전달한다.
 */
public class AdminRefreshRequest {

    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;

    public AdminRefreshRequest() {}

    public AdminRefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
