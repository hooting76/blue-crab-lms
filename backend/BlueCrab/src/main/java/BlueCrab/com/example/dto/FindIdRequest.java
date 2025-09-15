package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * ID 찾기 요청 DTO
 * 사용자가 학번, 이름, 전화번호를 입력하여 이메일 주소를 찾을 때 사용
 *
 * 입력 필드:
 * - userCode: 학번/교수 코드
 * - userName: 사용자 실명
 * - userPhone: 전화번호 (11자리, 하이픈 없이)
 *
 * 유효성 검증:
 * - 모든 필드 필수 입력
 * - 이름은 50자 이하
 * - 전화번호는 11자리 숫자
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class FindIdRequest {
    
    /**
     * 학번 또는 교수 코드
     * 학생의 경우 학번, 교수의 경우 교수 코드
     * 문자열로 받아서 Integer로 변환 처리
     */
    @NotBlank(message = "학번/교수 코드는 필수입니다.")
    @Pattern(regexp = "^\\d+$", message = "학번/교수 코드는 숫자여야 합니다.")
    private String userCode;
    
    /**
     * 사용자 실명
     * 데이터베이스에 저장된 이름과 정확히 일치해야 함
     */
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String userName;
    
    /**
     * 전화번호
     * 11자리 숫자로 입력 (하이픈 없이)
     * 예: 01012345678
     */
    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^\\d{11}$", message = "전화번호는 11자리 숫자여야 합니다.")
    private String userPhone;
    
    // 기본 생성자
    public FindIdRequest() {}
    
    // 전체 필드 생성자
    public FindIdRequest(String userCode, String userName, String userPhone) {
        this.userCode = userCode;
        this.userName = userName;
        this.userPhone = userPhone;
    }
    
    // Getters and Setters
    public String getUserCode() {
        return userCode;
    }
    
    public void setUserCode(String userCode) {
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
        return "FindIdRequest{" +
                "userCode=" + userCode +
                ", userName='" + userName + '\'' +
                ", userPhone='" + (userPhone != null ? userPhone.replaceAll("\\d(?=\\d{4})", "*") : null) + '\'' +
                '}';
    }
}