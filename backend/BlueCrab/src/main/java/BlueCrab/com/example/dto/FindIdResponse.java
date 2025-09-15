package BlueCrab.com.example.dto;

/**
 * ID 찾기 응답 DTO
 * 사용자의 ID 찾기 요청에 대한 응답 정보를 담는 클래스
 *
 * 응답 유형:
 * 1. 성공: 마스킹된 이메일 주소 반환
 * 2. 실패: 에러 메시지 반환
 *
 * 보안 고려사항:
 * - 계정 존재 여부를 직접적으로 노출하지 않음
 * - 이메일 주소는 마스킹하여 일부만 노출
 * - 타이밍 공격 방지를 위한 일관된 응답 구조
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class FindIdResponse {
    
    /**
     * 요청 처리 성공 여부
     */
    private boolean success;
    
    /**
     * 마스킹된 이메일 주소
     * 성공 시에만 값이 설정됨
     * 예: "u****3@domain.com"
     */
    private String maskedEmail;
    
    /**
     * 응답 메시지
     * 성공/실패 모두에 포함되는 안내 메시지
     */
    private String message;
    
    // 기본 생성자
    public FindIdResponse() {}
    
    // 성공 응답 생성자
    public FindIdResponse(boolean success, String maskedEmail, String message) {
        this.success = success;
        this.maskedEmail = maskedEmail;
        this.message = message;
    }
    
    /**
     * 성공 응답 생성 정적 메서드
     * @param maskedEmail 마스킹된 이메일 주소
     * @return 성공 응답 객체
     */
    public static FindIdResponse success(String maskedEmail) {
        return new FindIdResponse(true, maskedEmail, "이메일 주소를 찾았습니다.");
    }
    
    /**
     * 실패 응답 생성 정적 메서드
     * @return 실패 응답 객체
     */
    public static FindIdResponse failure() {
        return new FindIdResponse(false, null, "해당 정보로 만들어진 Id가 존재 하지 않습니다.");
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMaskedEmail() {
        return maskedEmail;
    }
    
    public void setMaskedEmail(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "FindIdResponse{" +
                "success=" + success +
                ", maskedEmail='" + maskedEmail + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}