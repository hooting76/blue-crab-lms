package BlueCrab.com.example.exception;

/**
 * 리소스가 중복되는 경우 발생하는 예외
 * HTTP 409 상태코드로 응답됨
 */
public class DuplicateResourceException extends RuntimeException {
    
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 특정 필드값이 중복되는 경우
     * @param resourceName 리소스 이름
     * @param fieldName 필드 이름
     * @param fieldValue 중복된 필드값
     * @return DuplicateResourceException 인스턴스
     */
    public static DuplicateResourceException forField(String resourceName, String fieldName, Object fieldValue) {
        return new DuplicateResourceException(String.format("이미 존재하는 %s입니다. %s: %s", resourceName, fieldName, fieldValue));
    }
}