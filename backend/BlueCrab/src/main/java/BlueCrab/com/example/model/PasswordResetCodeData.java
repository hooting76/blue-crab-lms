// 작업자 : 성태준
// 3단계 : 비밀번호 재설정 코드 데이터 모델

package BlueCrab.com.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordResetCodeData {
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("created_at")
    private Long createdAt;
    
    @JsonProperty("attempts")
    private Integer verificationAttempts;
    
    @JsonProperty("irtToken")
    private String irtToken;
    
    public PasswordResetCodeData() {} // 기본 생성자
    
    public PasswordResetCodeData(String email, String code, String irtToken) {
        // 생성자
        this.email = email;
        this.code = code;
        this.irtToken = irtToken;
        this.createdAt = System.currentTimeMillis();
        this.verificationAttempts = 0;
    }
    
    public String getEmail() {
        // 이메일 반환 메서드
        return email;
    }
    
    public void setEmail(String email) {
        // 이메일 설정 메서드
        this.email = email;
    }
    
    public String getCode() {
        // 코드 반환 메서드
        return code;
    }
    
    public void setCode(String code) {
        // 코드 설정 메서드
        this.code = code;
    }
    
    public Long getCreatedAt() {
        // 생성 시간 반환 메서드
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        // 생성 시간 설정 메서드
        this.createdAt = createdAt;
    }
    
    public Integer getVerificationAttempts() {
        // 시도 횟수 반환 메서드
        return verificationAttempts;
    }
    
    public void setVerificationAttempts(Integer verificationAttempts) {
        // 시도 횟수 설정 메서드
        this.verificationAttempts = verificationAttempts;
    }
    
    public String getIrtToken() {
        // IRT 토큰 반환 메서드
        return irtToken;
    }
    
    public void setIrtToken(String irtToken) {
        // IRT 토큰 설정 메서드
        this.irtToken = irtToken;
    }
    
    public boolean isExpired() {
        // 코드가 만료되었는지 확인 (5분 = 300초)
        // createdAt이 초 단위이므로 현재 시간도 초 단위로 변환하여 비교
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        return (currentTimeSeconds - createdAt) > 300; // 300초 = 5분
    }
    
    public boolean isMaxAttemptsExceeded() {
        // 최대 시도 횟수를 초과했는지 확인 (5회)
        return verificationAttempts >= 5;
    }
}