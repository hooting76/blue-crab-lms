// 작업자 : 성태준
// 3단계 : 인증 코드 검증 요청 DTO
// 사용자가 입력한 인증 코드와 IRT 토큰을 통해 코드 검증을 요청하는 데이터 전송 객체

package BlueCrab.com.example.dto;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

// ========== 외부 라이브러리 ==========
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetCodeVerifyRequest {

    @NotBlank(message = "IRT 토큰은 필수입니다")
    private String irtToken;
    // IRT (Identity Request Token) 토큰
    // 1단계 본인확인에서 발급받은 토큰

    @NotBlank(message = "인증 코드는 필수입니다")
    @Size(min = 6, max = 6, message = "인증 코드는 6자리여야 합니다")
    private String authCode;
    // 사용자가 입력한 인증 코드
    // 6자리 영문 대문자 + 숫자 조합
}