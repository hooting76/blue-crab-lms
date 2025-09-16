// 작업자 : 성태준
// 3단계 : 인증 코드 검증 응답 DTO

package BlueCrab.com.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetCodeVerifyResponse {


    private boolean success; // 검증 성공 여부

    private String message; // 응답 메시지

    private String resetToken; // 비밀번호 재설정 토큰 - 성공 시에만 제공

    private String maskedEmail; // 마스킹된 이메일 - 성공 시에만 제공

    private Integer remainingAttempts; // 남은 시도 횟수 - 실패 시에만 제공

    private Long expiresInSeconds; // 코드 만료까지 남은 시간(초) - 실패 시에만 제공

    private String status; // 응답 상태 코드

    // ========== 정적 팩토리 메서드 ==========

    public static PasswordResetCodeVerifyResponse success(String resetToken, String maskedEmail) {
        // 코드 검증 성공 응답
        return new PasswordResetCodeVerifyResponse(
            true,
            "코드 검증이 완료되었습니다. 이제 새로운 비밀번호를 설정할 수 있습니다.",
            resetToken,
            maskedEmail,
            null,
            null,
            "SUCCESS"
        ); // 반환하여 유효함을 나타냄
    }

    public static PasswordResetCodeVerifyResponse failure(int remainingAttempts, long expiresInSeconds) {
        // 코드 불일치 실패 응답
        return new PasswordResetCodeVerifyResponse(
            false,
            String.format("인증 코드가 일치하지 않습니다. 남은 시도 횟수: %d회", remainingAttempts),
            null,
            null,
            remainingAttempts,
            expiresInSeconds,
            "FAILURE"
        ); // 반환하여 유효하지 않음을 나타냄
    } 

    public static PasswordResetCodeVerifyResponse expired() {
        // 코드 만료 응답
        return new PasswordResetCodeVerifyResponse(
            false,
            "인증 코드가 만료되었습니다. 새로운 인증 코드를 요청해주세요.",
            null,
            null,
            null,
            null,
            "EXPIRED"
        ); // 반환하여 유효하지 않음을 나타냄
    }

    public static PasswordResetCodeVerifyResponse blocked() {
        // 시도 횟수 초과로 차단된 응답
        return new PasswordResetCodeVerifyResponse(
            false,
            "최대 시도 횟수를 초과하여 인증 코드가 차단되었습니다. 새로운 인증 코드를 요청해주세요.",
            null,
            null,
            0,
            null,
            "BLOCKED"
        ); // 반환하여 유효하지 않음을 나타냄   
    }

    public static PasswordResetCodeVerifyResponse sessionError() {
        // 세션 오류 응답
        return new PasswordResetCodeVerifyResponse(
            false,
            "세션이 만료되거나 유효하지 않습니다. 처음부터 다시 시도해주세요.",
            null,
            null,
            null,
            null,
            "SESSION_ERROR"
        ); // 반환하여 유효하지 않음을 나타냄
    }
}