package BlueCrab.com.example.dto;

/**
 * 관리자 1차 로그인 응답을 위한 DTO 클래스
 * 관리자 ID/PW 인증 성공 후 이메일 인증 토큰을 포함한 응답 데이터를 담는 객체
 *
 * 📋 응답 데이터 구조:
 * {
 *   "message": "이메일 인증이 필요합니다. 이메일을 확인해주세요.",
 *   "emailVerificationToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "expiresIn": 600,
 *   "email": "admin@example.com"
 * }
 *
 * 🔗 API 응답 표준과의 관계:
 * - ApiResponse<AdminLoginResponse> 형태로 래핑되어 응답
 * - success: true (1차 인증 성공)
 * - message: "1차 인증이 완료되었습니다"
 * - data: 이 AdminLoginResponse 객체
 * - timestamp: 응답 생성 시간
 *
 * 💡 사용 시나리오:
 * 1. 관리자가 POST /api/admin/login으로 ID/PW 전송
 * 2. 서버에서 인증 성공 후 이메일 인증 토큰 생성
 * 3. 관리자 이메일로 인증 링크 발송
 * 4. 이 응답을 클라이언트에 반환
 * 5. 클라이언트는 이메일 인증 토큰으로 2차 인증 진행
 *
 * 🔒 보안 고려사항:
 * - 이메일 인증 토큰은 단시간 유효 (기본 10분)
 * - 토큰 사용 후 즉시 블랙리스트 처리
 * - 이메일 주소는 마스킹 처리하여 노출
 */
public class AdminLoginResponse {

    /**
     * 사용자에게 표시할 메시지
     * 이메일 인증 진행 안내 메시지
     */
    private String message;

    /**
     * 세션 토큰 (1차 로그인 성공 후 발급)
     * 2차 인증 코드 발급 요청 시 사용되는 일회용 토큰
     */
    private String sessionToken;

    /**
     * 토큰 만료 시간 (초 단위)
     * 클라이언트가 타이머 표시에 사용
     */
    private long expiresIn;

    /**
     * 인증 이메일이 발송된 이메일 주소 (마스킹 처리)
     * 사용자 확인용 (예: "ad***@example.com")
     */
    private String maskedEmail;

    /**
     * 다음 단계 안내 URL (선택사항)
     * 이메일 인증 페이지 URL
     */
    private String nextStepUrl;

    // 기본 생성자
    public AdminLoginResponse() {}

    // 생성자
    public AdminLoginResponse(String message, String sessionToken, long expiresIn) {
        this.message = message;
        this.sessionToken = sessionToken;
        this.expiresIn = expiresIn;
    }

    // 전체 생성자
    public AdminLoginResponse(String message, String sessionToken, long expiresIn, 
                             String maskedEmail, String nextStepUrl) {
        this.message = message;
        this.sessionToken = sessionToken;
        this.expiresIn = expiresIn;
        this.maskedEmail = maskedEmail;
        this.nextStepUrl = nextStepUrl;
    }

    // 이메일 마스킹 헬퍼 메서드
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***@" + domain;
        } else {
            return localPart.substring(0, 2) + "***@" + domain;
        }
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getMaskedEmail() {
        return maskedEmail;
    }

    public void setMaskedEmail(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }

    public String getNextStepUrl() {
        return nextStepUrl;
    }

    public void setNextStepUrl(String nextStepUrl) {
        this.nextStepUrl = nextStepUrl;
    }

    @Override
    public String toString() {
        return "AdminLoginResponse{" +
                "message='" + message + '\'' +
                ", sessionToken='[PROTECTED]'" +  // 토큰은 로그에 노출하지 않음
                ", expiresIn=" + expiresIn +
                ", maskedEmail='" + maskedEmail + '\'' +
                ", nextStepUrl='" + nextStepUrl + '\'' +
                '}';
    }
}
