package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 관리자 로그인 요청을 위한 DTO 클래스
 * 관리자가 1차 인증(ID/PW 검증)을 위해 보내는 요청 데이터를 담는 데이터 전송 객체
 *
 * 📋 요청 데이터 구조:
 * {
 *   "adminId": "admin@test.com",      // 관리자 이메일 주소
 *   "password": "password123"         // 관리자 비밀번호 (평문)
 * }
 *
 * 🔗 API 응답 표준과의 관계:
 * - 이 클래스는 관리자 1차 로그인 요청 데이터를 담음
 * - AuthController에서 이 데이터를 받아 처리
 * - 성공 시 ApiResponse<AdminLoginResponse> 형태로 이메일 인증 토큰 응답
 * - 실패 시 ApiResponse<Void> 형태로 에러 응답
 *
 * 💡 사용 예시:
 * POST /api/admin/login
 * Content-Type: application/json
 * {
 *   "adminId": "admin01@test.com",
 *   "password": "secureAdminPassword123"
 * }
 *
 * 🔒 보안 고려사항:
 * - 반드시 HTTPS 프로토콜에서만 사용
 * - 비밀번호는 평문으로 전송되므로 SSL/TLS 암호화 필수
 * - 로그인 시도 횟수 제한 적용
 * - IP 기반 Rate Limiting 적용
 */
public class AdminLoginRequest {

    /**
     * 관리자 이메일 주소 (로그인 ID)
     * 필수 입력 필드로, 공백이나 null 값 허용하지 않음
     * 이메일 형식으로 입력 (예: admin@test.com)
     */
    @NotBlank(message = "관리자 이메일은 필수 입력 항목입니다.")
    @Size(min = 5, max = 100, message = "관리자 이메일은 5자 이상 100자 이하로 입력해주세요.")
    private String adminId;

    /**
     * 관리자 비밀번호
     * 필수 입력 필드로, 공백이나 null 값 허용하지 않음
     * 최소 8자 이상, 최대 100자 이하
     */
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해주세요.")
    private String password;

    // 기본 생성자
    public AdminLoginRequest() {}

    // 생성자
    public AdminLoginRequest(String adminId, String password) {
        this.adminId = adminId;
        this.password = password;
    }

    // Getters and Setters
    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "AdminLoginRequest{" +
                "adminId='" + adminId + '\'' +
                ", password='[PROTECTED]'" +  // 비밀번호는 로그에 노출하지 않음
                '}';
    }
}
