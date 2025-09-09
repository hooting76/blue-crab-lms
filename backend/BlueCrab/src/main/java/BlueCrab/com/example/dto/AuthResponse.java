// 작업자 : 성태준
package BlueCrab.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// ========== 인증 응답 DTO 클래스 ==========
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String message = "인증이 완료 되었습니다.";
    private Object data; // 추가 데이터를 위한 필드
    
    // ========== 메시지만 있는 생성자 ==========
    public AuthResponse(String message) {
        this.message = message;
        this.data = null;
    }
}