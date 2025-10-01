package BlueCrab.com.example.exception;

/**
 * 잘못된 FCM 플랫폼 값이 전달된 경우 발생하는 예외
 * API 명세서에 정의된 ANDROID, IOS, WEB 이외의 값은 허용하지 않는다.
 */
public class InvalidPlatformException extends RuntimeException {

    public InvalidPlatformException(String platform) {
        super("지원하지 않는 플랫폼입니다: " + platform);
    }
}