// 작업자 : 성태준
package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;

// ========== 인증 코드 확인 요청 DTO ==========
public class AuthCodeVerifyRequest {
    
    @NotBlank(message = "인증 코드는 필수입니다.")
    @Size(min = 6, max = 6, message = "인증 코드는 6자리여야 합니다.")
    @Pattern(regexp = "^[A-Z0-9]{6}$", message = "인증 코드는 영문 대문자와 숫자만 허용됩니다.")
    private String authCode;
    
    // ========== 생성자 ==========
    public AuthCodeVerifyRequest() {}
    
    public AuthCodeVerifyRequest(String authCode) {
        this.authCode = authCode;
    }
    
    // ========== Getter/Setter ==========
    public String getAuthCode() { 
        return authCode; 
    }
    
    public void setAuthCode(String authCode) { 
        // 자동으로 대문자 변환
        this.authCode = authCode != null ? authCode.trim().toUpperCase() : null; 
    }
    
    // ========== toString (보안상 코드 마스킹) ==========
    @Override
    public String toString() {
        return "AuthCodeVerifyRequest{" +
                "authCode='" + (authCode != null ? "******" : "null") + '\'' +
                '}';
    }
}