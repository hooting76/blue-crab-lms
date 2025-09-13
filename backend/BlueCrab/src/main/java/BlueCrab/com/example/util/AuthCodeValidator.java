// 작업자 : 성태준
// 인증 코드 검증 유틸리티 클래스
// 이메일 인증 코드의 형식 검증과 만료 검증 기능을 담당

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;


@Component
@Slf4j
public class AuthCodeValidator {
    
    // ========== 인증 코드 검증 설정 상수 ==========
    private static final int AUTH_CODE_LENGTH = 6;
    // 인증 코드 길이 (6자리)
    private static final String AUTH_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    // 인증 코드 문자 집합 (영문 대문자 + 숫자)
    private static final int AUTH_CODE_EXPIRY_MINUTES = 5;
    // 인증 코드 유효 기간 (5분)
    
    public boolean isInvalidAuthCode(String authCode) {
        // 입력된 인증 코드의 유효성을 검증하는 메서드
        if (authCode == null || authCode.trim().isEmpty()) {
            // null 또는 빈 문자열인 경우
            log.warn("Auth code is null or empty.");
            // 경고 로그 기록
            return true;
            // 유효하지 않음
        }
        
        String trimmedCode = authCode.trim().toUpperCase();
        // 공백 제거 및 대문자 변환
        
        if (trimmedCode.length() != AUTH_CODE_LENGTH) {
            // 길이가 올바르지 않은 경우
            log.warn("Auth code length is invalid: {}", trimmedCode.length());
            // 경고 로그 기록
            return true;
            // 유효하지 않음
        }
        
        for (char c : trimmedCode.toCharArray()) {
            // 각 문자가 허용된 문자 집합에 속하는지 확인
            if (AUTH_CODE_CHARACTERS.indexOf(c) == -1) {
                // 허용되지 않은 문자가 포함된 경우
                log.warn("Auth code contains invalid character: {}", c);
                // 경고 로그 기록
                return true;
                // 유효하지 않음
            }
        }
        
        return false;
        // 모든 검증을 통과한 경우 유효함
    }
    
    public boolean isAuthCodeExpired(LocalDateTime codeCreatedTime) {
        // 인증 코드가 만료되었는지 확인하는 메서드
        log.debug("Checking if auth code is expired. Created at: {}, Now: {}", 
            codeCreatedTime, LocalDateTime.now());
        // 디버그 로그 기록
        return LocalDateTime.now().isAfter(codeCreatedTime.plusMinutes(AUTH_CODE_EXPIRY_MINUTES));
        // 현재 시간이 코드 생성 시간 + 유효 기간을 초과했는지 확인
    }
    
    public ValidationResult validateAuthCode(String authCode, LocalDateTime codeCreatedTime) {
        // 형식 검증
        if (isInvalidAuthCode(authCode)) {
            return ValidationResult.invalid("인증 코드가 올바르지 않습니다. 인증코드는 영문 대문자와 숫자 조합의 " + AUTH_CODE_LENGTH + "자리 문자입니다.");
        }
        
        // 만료 검증
        if (isAuthCodeExpired(codeCreatedTime)) {
            return ValidationResult.expired("인증 코드가 만료되었습니다. 새로운 코드를 요청해주세요.");
        }
        
        return ValidationResult.valid();
    }
    
    /* 검증 결과를 나타내는 내부 클래스
     */
    public static class ValidationResult {
        private final boolean valid;
        private final boolean expired;
        private final String message;
        
        private ValidationResult(boolean valid, boolean expired, String message) {
            this.valid = valid;
            this.expired = expired;
            this.message = message;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, false, "유효한 인증 코드입니다.");
        }
        
        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, false, message);
        }
        
        public static ValidationResult expired(String message) {
            return new ValidationResult(false, true, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public boolean isExpired() {
            return expired;
        }
        
        public String getMessage() {
            return message;
        }
        
        public org.springframework.http.HttpStatus getHttpStatus() {
            if (expired) {
                return org.springframework.http.HttpStatus.UNAUTHORIZED;
            } else if (!valid) {
                return org.springframework.http.HttpStatus.BAD_REQUEST;
            } else {
                return org.springframework.http.HttpStatus.OK;
            }
        }
    }
    
    /* 현재 설정된 인증 코드 길이 반환
     * 
     * @return 인증 코드 길이
     */
    public int getAuthCodeLength() {
        return AUTH_CODE_LENGTH;
    }
    
    /* 현재 설정된 인증 코드 문자 집합 반환
     * 
     * @return 인증 코드 문자 집합
     */
    public String getAuthCodeCharacters() {
        return AUTH_CODE_CHARACTERS;
    }
    
    /* 현재 설정된 인증 코드 유효 기간 반환
     * 
     * @return 인증 코드 유효 기간 (분)
     */
    public int getAuthCodeExpiryMinutes() {
        return AUTH_CODE_EXPIRY_MINUTES;
    }
}
