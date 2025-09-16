package BlueCrab.com.example.dto;

/**
 * 비밀번호 재설정 본인확인 응답 DTO
 * 성공/실패 여부와 관계없이 동일한 중립적 메시지를 반환하여 보안을 강화
 *
 * 보안 원칙:
 * - 계정 존재 여부를 노출하지 않음
 * - 성공/실패 시 모두 동일한 메시지 반환
 * - 내부적으로만 IRT 토큰 발급 여부 결정
 */
public class PasswordResetIdentityResponse {

    /**
     * 처리 성공 여부
     * 항상 true로 반환 (실제 성공/실패 여부 비노출)
     */
    private boolean success;

    /**
     * 사용자에게 표시할 메시지
     * 성공/실패 여부와 관계없이 동일한 중립적 메시지
     */
    private String message;

    /**
     * IRT (Identity Request Token)
     * 본인확인 성공 시에만 반환되는 토큰
     * 다음 단계(이메일 발송)에서 사용
     */
    private String identityToken;

    /**
     * 마스킹된 이메일 주소
     * 성공 시에만 반환하여 사용자가 어떤 이메일로 발송될지 확인할 수 있게 함
     */
    private String maskedEmail;

    // 기본 생성자
    public PasswordResetIdentityResponse() {}

    // 성공 응답 생성자
    public PasswordResetIdentityResponse(boolean success, String message, String identityToken, String maskedEmail) {
        this.success = success;
        this.message = message;
        this.identityToken = identityToken;
        this.maskedEmail = maskedEmail;
    }

    // 실패 응답 생성자 (토큰 없음)
    public PasswordResetIdentityResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * 성공 응답을 생성하는 정적 메서드
     */
    public static PasswordResetIdentityResponse success(String identityToken, String maskedEmail) {
        return new PasswordResetIdentityResponse(
            true,
            "요청이 접수되었습니다. 입력 정보가 정확하다면 이메일이 곧 도착합니다.",
            identityToken,
            maskedEmail
        );
    }

    /**
     * 실패 응답을 생성하는 정적 메서드
     * 성공과 동일한 메시지로 응답
     */
    public static PasswordResetIdentityResponse neutral() {
        return new PasswordResetIdentityResponse(
            true,
            "요청이 접수되었습니다. 입력 정보가 정확하다면 이메일이 곧 도착합니다."
        );
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIdentityToken() {
        return identityToken;
    }

    public void setIdentityToken(String identityToken) {
        this.identityToken = identityToken;
    }

    public String getMaskedEmail() {
        return maskedEmail;
    }

    public void setMaskedEmail(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }

    @Override
    public String toString() {
        return "PasswordResetIdentityResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", identityToken='" + (identityToken != null ? "[PRESENT]" : "[ABSENT]") + '\'' +
                ", maskedEmail='" + maskedEmail + '\'' +
                '}';
    }
}