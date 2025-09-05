package BlueCrab.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/* 
 * 인증 요청에 대한 응답을 담는 DTO 클래스.
 */
@Getter
@Setter
@AllArgsConstructor
// @AllArgsConstructor : 모든 필드를 매개변수로 받는 생성자를 자동 생성하는 Lombok 어노테이션
public class AuthResponse {
    // 응답 메시지
    private String message = "인증이 완료 되었습니다.";
}
