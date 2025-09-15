// 작업자 : 성태준
package BlueCrab.com.example.dto;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

// ========== 인증 코드 확인 요청 DTO ==========
public class AuthCodeVerifyRequest {
    
    @NotBlank(message = "인증 코드는 필수입니다.")
    // 공백이 아닌지 확인, 공백일 경우 "인증 코드는 필수입니다." 메시지 반환
    @Size(min = 6, max = 6, message = "인증 코드는 6자리여야 합니다." )
    // 길이가 6자리인지 확인, 아닐 경우 "인증 코드는 6자리여야 합니다." 메시지 반환
    // min = 6, max = 6 으로 조절 가능하지만, 메세지는 연동방식이 불가능 하기에, 수동으로 수정 필요.
    @Pattern(regexp = "^[A-Z0-9]{6}$", message = "인증 코드는 영문 대문자와 숫자만 허용됩니다.")
    // 영문 대문자와 숫자 6자리 인지 확인, 아닐 경우 "인증 코드는 영문 대문자와 숫자만 허용됩니다." 메시지 반환
    private String authCode;
    // 인증 코드 필드
    
    // ========== 생성자 ==========
    public AuthCodeVerifyRequest() {}
    // 기본 생성자
    
    public AuthCodeVerifyRequest(String authCode) {
        // 매개변수 있는 생성자
        this.authCode = authCode;
        // 자동으로 대문자 변환
    }
    
    // ========== Getter/Setter ==========
    public String getAuthCode() { 
        // 자동으로 대문자 변환
        return authCode; 
        // 대문자로 변환된 인증 코드 반환
    }
    
    public void setAuthCode(String authCode) { 
        // 자동으로 대문자 변환
        this.authCode = authCode != null ? authCode.trim().toUpperCase() : null; 
        // 공백 제거 및 대문자 변환 후 설정
    }
    
    // ========== toString (보안상 코드 마스킹) ==========
    @Override // toString 메서드 재정의
    public String toString() {
        // 보안상 인증 코드는 마스킹 처리하여 출력
        return "AuthCodeVerifyRequest{" +
                "authCode='" + (authCode != null ? "******" : "null") + '\'' +
                '}';
        // 문자열 표현 반환 
    }
} // AuthCodeVerifyRequest 끝