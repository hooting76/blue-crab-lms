package BlueCrab.com.example.exception;

/**
 * 인증되지 않은 접근 시도 시 발생하는 예외
 * HTTP 401 Unauthorized 상태코드로 응답됨
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 관리자 권한이 없는 경우
     */
    public static UnauthorizedException forAdmin() {
        return new UnauthorizedException("관리자 권한이 필요합니다.");
    }

    /**
     * 유효하지 않은 토큰인 경우
     */
    public static UnauthorizedException forInvalidToken() {
        return new UnauthorizedException("유효하지 않은 토큰입니다.");
    }
}
