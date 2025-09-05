package BlueCrab.com.example.exception;

/**
 * 리소스를 찾을 수 없는 경우 발생하는 예외
 * HTTP 404 상태코드로 응답됨
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 특정 ID의 리소스를 찾을 수 없는 경우
     * @param resourceName 리소스 이름
     * @param id 리소스 ID
     * @return ResourceNotFoundException 인스턴스
     */
    public static ResourceNotFoundException forId(String resourceName, Object id) {
        return new ResourceNotFoundException(String.format("%s을(를) 찾을 수 없습니다. ID: %s", resourceName, id));
    }
    
    /**
     * 특정 필드값으로 리소스를 찾을 수 없는 경우
     * @param resourceName 리소스 이름
     * @param fieldName 필드 이름
     * @param fieldValue 필드값
     * @return ResourceNotFoundException 인스턴스
     */
    public static ResourceNotFoundException forField(String resourceName, String fieldName, Object fieldValue) {
        return new ResourceNotFoundException(String.format("%s을(를) 찾을 수 없습니다. %s: %s", resourceName, fieldName, fieldValue));
    }
}