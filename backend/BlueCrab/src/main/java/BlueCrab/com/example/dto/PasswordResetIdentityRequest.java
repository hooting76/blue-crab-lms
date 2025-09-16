package BlueCrab.com.example.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 비밀번호 재설정을 위한 본인확인 요청 DTO
 * 사용자가 이메일, 학번, 이름을 입력하여 본인 신원을 확인하는 1단계에서 사용
 *
 * 보안 고려사항:
 * - 세 필드 모두 정확히 일치해야만 성공 처리
 * - 실패 시 계정 존재 여부를 노출하지 않음
 * - 중립적인 메시지로 응답
 */
public class PasswordResetIdentityRequest {

    /**
     * 사용자 이메일 주소
     * 로그인에 사용되는 이메일과 동일해야 함
     */
    @NotBlank(message = "이메일은 필수 입력입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    /**
     * 사용자 학번 또는 교수 코드
     * 가입 시 입력한 코드와 정확히 일치해야 함
     */
    @NotNull(message = "학번/교수코드는 필수 입력입니다")
    private Integer userCode;

    /**
     * 사용자 실명
     * 가입 시 입력한 이름과 정확히 일치해야 함
     */
    @NotBlank(message = "이름은 필수 입력입니다")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]{2,50}$", message = "이름은 2-50자의 한글, 영문, 공백만 입력 가능합니다")
    private String userName;

    /**
     * 사용자 전화번호
     * 가입 시 입력한 전화번호와 정확히 일치해야 함
     * 하이픈 없이 숫자만 11자리 (01012345678 형식)
     */
    @NotBlank(message = "전화번호는 필수 입력입니다")
    @Pattern(regexp = "^010[0-9]{8}$", message = "전화번호는 010으로 시작하는 11자리 숫자여야 합니다")
    private String userPhone;

    // 기본 생성자
    public PasswordResetIdentityRequest() {}

    // 전체 필드 생성자
    public PasswordResetIdentityRequest(String email, Integer userCode, String userName, String userPhone) {
        this.email = email;
        this.userCode = userCode;
        this.userName = userName;
        this.userPhone = userPhone;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getUserCode() {
        return userCode;
    }

    public void setUserCode(Integer userCode) {
        this.userCode = userCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    @Override
    public String toString() {
        return "PasswordResetIdentityRequest{" +
                "email='" + email + '\'' +
                ", userCode=" + userCode +
                ", userName='" + userName + '\'' +
                ", userPhone='" + userPhone + '\'' +
                '}';
    }
}