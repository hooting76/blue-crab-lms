// 작업자 : 성태준
// 인증 코드 검증 유틸리티 클래스
// 이메일 인증 코드의 형식 검증과 만료 검증 기능을 담당

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.time.LocalDateTime;

// ========== 외부 라이브러리 ==========
import lombok.extern.slf4j.Slf4j;

// ========== Spring Framework ==========
import org.springframework.stereotype.Component;


@Component // Spring의 컴포넌트 스캔에 의해 빈으로 등록
@Slf4j // Lombok을 사용한 로깅 지원
public class AuthCodeValidator {
    // 인증 코드 검증 메서드
    
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
            // true를 반환하여 유효하지 않음을 나타냄
        } // if 끝
        
        String trimmedCode = authCode.trim().toUpperCase();
        // 공백 제거 및 대문자 변환
        
        if (trimmedCode.length() != AUTH_CODE_LENGTH) {
            // 길이가 올바르지 않은 경우
            log.warn("Auth code length is invalid: {}", trimmedCode.length());
            // 경고 로그 기록
            return true;
            // true를 반환하여 유효하지 않음을 나타냄
        } // if 끝
        
        for (char c : trimmedCode.toCharArray()) {
            // 각 문자가 허용된 문자 집합에 속하는지 확인
            if (AUTH_CODE_CHARACTERS.indexOf(c) == -1) {
                // 허용되지 않은 문자가 포함된 경우
                log.warn("Auth code contains invalid character: {}", c);
                // 경고 로그 기록
                return true;
                // true를 반환하여 유효하지 않음을 나타냄
            } // if 끝
        } // for 끝
        
        return false;
        // false를 반환하여 유효함을 나타냄
    } // isInvalidAuthCode 끝
    
    public boolean isAuthCodeExpired(LocalDateTime codeCreatedTime) {
        // 인증 코드가 만료되었는지 확인하는 메서드
        log.debug("Checking if auth code is expired. Created at: {}, Now: {}", 
            codeCreatedTime, LocalDateTime.now());
        // 디버그 로그 기록
        return LocalDateTime.now().isAfter(codeCreatedTime.plusMinutes(AUTH_CODE_EXPIRY_MINUTES));
        // 현재 시간이 코드 생성 시간 + 유효 기간을 초과했는지 확인
    } // isAuthCodeExpired 끝
    
    public ValidationResult validateAuthCode(String authCode, LocalDateTime codeCreatedTime) {
        // 형식 검증
        if (isInvalidAuthCode(authCode)) {
            // 형식이 올바르지 않은 경우
            return ValidationResult.invalid("인증 코드가 올바르지 않습니다. 인증코드는 영문 대문자와 숫자 조합의 " + AUTH_CODE_LENGTH + "자리 문자입니다.");
            // 반환하여 유효하지 않음을 나타냄
        } // if 끝
        
        // 만료 검증
        if (isAuthCodeExpired(codeCreatedTime)) {
            // 인증 코드가 만료된 경우
            return ValidationResult.expired("인증 코드가 만료되었습니다. 새로운 코드를 요청해주세요.");
            // 반환하여 만료되었음을 나타냄
        } // if 끝
        
        return ValidationResult.valid();
        // 모든 검증을 통과한 경우 유효함을 나타냄
    } // validateAuthCode 끝
    
    // 검증 결과를 나타내는 내부 클래스
    public static class ValidationResult {
        // 검증 결과 상태
        private final boolean valid; // 유효 여부
        private final boolean expired; // 만료 여부
        private final String message; // 상세 메시지
        
        private ValidationResult(boolean valid, boolean expired, String message) {
            // 생성자
            this.valid = valid; // 유효 여부 초기화
            this.expired = expired; // 만료 여부 초기화
            this.message = message; // 상세 메시지 초기화
        } // 생성자 끝
        
        public static ValidationResult valid() {
            // 유효한 경우
            return new ValidationResult(true, false, "유효한 인증 코드입니다.");
            // 반환하여 유효함을 나타냄
        } // valid 끝
        
        public static ValidationResult invalid(String message) {
            // 유효하지 않은 경우
            return new ValidationResult(false, false, message);
            // 반환하여 유효하지 않음을 나타냄
        } // invalid 끝
        
        public static ValidationResult expired(String message) {
            // 만료된 경우
            return new ValidationResult(false, true, message);
            // 반환하여 만료되었음을 나타냄
        } // expired 끝
        
        public boolean isValid() {
            // 유효 여부를 확인하는 메서드
            return valid;
            // valid를 반환하여 유효함을 나타냄
        } // isValid 끝
        
        public boolean isExpired() {
            // 만료 여부를 확인하는 메서드
            return expired;
            // expired를 반환하여 만료되었음을 나타냄
        } // isExpired 끝
        
        public String getMessage() {
            // 메세지를 반환하는 메서드
            return message;
            // 메시지 반환
        } // getMessage 끝
        
        public org.springframework.http.HttpStatus getHttpStatus() {
            // 검증 결과에 따른 HTTP 상태 코드를 반환하는 메서드
            if (expired) {
                // 만료된 경우
                return org.springframework.http.HttpStatus.UNAUTHORIZED;
                // 401 상태 코드 반환
            } else if (!valid) {
                // 유효하지 않은 경우
                return org.springframework.http.HttpStatus.BAD_REQUEST;
                // 400 상태 코드 반환
            } else {
                // 유효한 경우
                return org.springframework.http.HttpStatus.OK;
                // 200 상태 코드 반환
            } // if 끝
        } // getHttpStatus 끝
    } // ValidationResult 클래스 끝
    
    public int getAuthCodeLength() {
        // 현재 설정된 인증 코드 길이를 반환하는 메서드
        return AUTH_CODE_LENGTH;
        // AUTH_CODE_LENGTH 반환
    } // getAuthCodeLength 끝
    
    public String getAuthCodeCharacters() {
        // 현재 설정된 인증 코드 문자 집합을 반환하는 메서드
        return AUTH_CODE_CHARACTERS;
        // AUTH_CODE_CHARACTERS 반환
    } // getAuthCodeCharacters 끝
    
    public int getAuthCodeExpiryMinutes() {
        // 현재 설정된 인증 코드 유효 기간(분)을 반환하는 메서드
        return AUTH_CODE_EXPIRY_MINUTES;
        // AUTH_CODE_EXPIRY_MINUTES 반환
    } // getAuthCodeExpiryMinutes 끝
} // AuthCodeValidator 클래스 끝
