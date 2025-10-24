package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 기본 정보 업데이트 요청 DTO
 * POST /api/profile/basic-info/update 요청 시 사용
 *
 * 프론트엔드 플로우:
 * 1. 사용자가 마이페이지에서 이름/전화번호 수정
 * 2. 수정한 정보를 이 DTO 형식으로 백엔드에 전송
 * 3. 백엔드에서 검증 후 USER_TBL 테이블에 저장
 *
 * 데이터베이스 매핑:
 * - userName → USER_NAME (VARCHAR 50)
 * - userPhone → USER_PHONE (CHAR 11)
 *
 * 요청 예시:
 * {
 *   "userName": "홍길동",
 *   "userPhone": "01012345678"
 * }
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-24
 */
public class BasicInfoUpdateRequest {

    /**
     * 사용자 이름
     * 예시: "홍길동", "John Smith", "이 영 희"
     *
     * 검증 규칙:
     * - 필수 입력
     * - 최대 50자 (DB 컬럼 제약)
     * - 한글, 영문, 공백만 허용 (숫자, 특수문자 차단)
     */
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]+$",
             message = "이름은 한글, 영문, 공백만 사용 가능합니다")
    private String userName;

    /**
     * 전화번호 (11자리 숫자, 하이픈 없이)
     * 예시: "01012345678", "01087654321"
     *
     * 검증 규칙:
     * - 필수 입력
     * - 010으로 시작하는 11자리 숫자
     * - 하이픈 없이 숫자만 입력
     */
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^010[0-9]{8}$",
             message = "전화번호는 010으로 시작하는 11자리 숫자여야 합니다")
    private String userPhone;

    // ========== Constructors ==========

    /**
     * 기본 생성자 (Jackson deserialization용)
     */
    public BasicInfoUpdateRequest() {}

    /**
     * 전체 필드 생성자
     *
     * @param userName 사용자 이름
     * @param userPhone 전화번호 (11자리)
     */
    public BasicInfoUpdateRequest(String userName, String userPhone) {
        this.userName = userName;
        this.userPhone = userPhone;
    }

    // ========== Getters and Setters ==========

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
        return "BasicInfoUpdateRequest{" +
                "userName='" + userName + '\'' +
                ", userPhone='" + userPhone + '\'' +
                '}';
    }
}
