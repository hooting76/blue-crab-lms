// 작업자 : 성태준
package BlueCrab.com.example.dto;

// ========== 임포트 구문 ==========

// ========== 외부 라이브러리 ==========
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// ========== 인증 응답 DTO 클래스 ==========
@Getter // @Getter 자동 생성
@Setter // @Setter 자동 생성
@NoArgsConstructor // @NoArgsConstructor 자동 생성
public class AuthResponse {
    private String message = "인증이 완료 되었습니다."; // 기본 메시지 설정
    private boolean success = true; // 성공/실패 상태를 나타내는 필드 추가
    private Object data; // 추가 데이터를 위한 필드
    
    // ========== 1. 메시지만 있는 생성자 (하위 호환성 유지) ==========
    public AuthResponse(String message) {
        this.message = message; // 메시지 설정
        this.success = true; // 기본값을 true로 설정
        this.data = null; // 데이터는 null로 초기화
    }
    
    // ========== 2. 메시지와 성공 여부를 모두 설정하는 생성자 ==========
    public AuthResponse(String message, boolean success) {
        this.message = message; // 메시지 설정
        this.success = success; // 성공 여부 설정
        this.data = null; // 데이터는 null로 초기화
    }
    
    // ========== 3. 모든 필드를 설정하는 생성자 ==========
    public AuthResponse(String message, boolean success, Object data) {
        this.message = message; // 메시지 설정
        this.success = success; // 성공 여부 설정
        this.data = data; // 데이터 설정
    }
}