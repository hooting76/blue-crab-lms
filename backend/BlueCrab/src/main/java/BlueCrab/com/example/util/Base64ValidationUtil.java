// 작성자 : 성태준
// Base64 인코딩 전 원문 길이 검증 유틸리티

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class Base64ValidationUtil {
    
    // ========== 상수 정의 ==========
    
    // 원문 길이 제한
    public static final int MAX_TITLE_LENGTH = 50;         // 제목 원문 최대 50자
    public static final int MAX_CONTENT_LENGTH = 10000;    // 본문 원문 최대 10,000자
    
    // ========== 원문 길이 검증 메서드 ==========
    
    // 제목 원문을 받아오는 메서드
    public static String validateAndEncodeTitleIfValid(String originalTitle) {
        if (isValidTitleLength(originalTitle)) {
            return encodeBase64(originalTitle);
        }
        return null;
    }
    
    // 본문 원문을 받아오는 메서드
    public static String validateAndEncodeContentIfValid(String originalContent) {
        if (isValidContentLength(originalContent)) {
            return encodeBase64(originalContent);
        }
        return null;
    }

    // 제목 원문 길이 검증
    public static boolean isValidTitleLength(String originalTitle) {
        if (originalTitle == null) {
            return false;
        }
        return originalTitle.length() <= MAX_TITLE_LENGTH;
    }
    
    // 본문 원문 길이 검증
    public static boolean isValidContentLength(String originalContent) {
        if (originalContent == null) {
            return false;
        }
        return originalContent.length() <= MAX_CONTENT_LENGTH;
    }
    
    // ========== Base64 인코딩 메서드 ==========
    
    public static String encodeBase64(String originalString) {
        if (originalString == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(originalString.getBytes(StandardCharsets.UTF_8));
    }
    
    // ========== 에러 메시지 생성 메서드 ==========
    
    // 제목 길이 초과 에러 메시지 생성
    public static String getTitleLengthErrorMessage(int actualLength) {
        return String.format("제목은 %d자를 초과할 수 없습니다. (현재: %d자)", 
                            MAX_TITLE_LENGTH, actualLength);
    }

    // 본문 길이 초과 에러 메시지 생성
    public static String getContentLengthErrorMessage(int actualLength) {
        return String.format("본문은 %d자를 초과할 수 없습니다. (현재: %d자)", 
                            MAX_CONTENT_LENGTH, actualLength);
    }
    
}