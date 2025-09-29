package BlueCrab.com.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 요청 빈도 제한을 위한 어노테이션
 * 지정된 시간 내에 최대 요청 횟수를 제한합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 시간 윈도우 (초)
     * @return 시간 윈도우
     */
    int timeWindow() default 60;
    
    /**
     * 시간 윈도우 내 최대 요청 횟수
     * @return 최대 요청 횟수
     */
    int maxRequests() default 10;
    
    /**
     * 제한 키 타입 (IP 기반 또는 사용자 기반)
     * @return 키 타입
     */
    KeyType keyType() default KeyType.USER;
    
    /**
     * 커스텀 에러 메시지
     * @return 에러 메시지
     */
    String message() default "요청이 너무 빈번합니다. 잠시 후 다시 시도해주세요.";
    
    enum KeyType {
        IP,    // IP 주소 기반 제한
        USER   // 사용자 기반 제한 (JWT 토큰)
    }
}