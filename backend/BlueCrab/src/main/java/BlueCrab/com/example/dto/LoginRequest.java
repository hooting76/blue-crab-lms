package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;

/**
 * 로그인 요청을 위한 DTO 클래스
 * 사용자가 시스템에 인증하기 위해 보내는 요청 데이터를 담는 데이터 전송 객체
 *
 * 📋 요청 데이터 구조:
 * {
 *   "username": "user@example.com",  // 사용자 식별자 (이메일)
 *   "password": "password123"        // 사용자 비밀번호 (평문)
 * }
 *
 * 🔗 API 응답 표준과의 관계:
 * - 이 클래스는 요청 데이터를 담음
 * - AuthController에서 이 데이터를 받아 처리
 * - 성공 시 ApiResponse<LoginResponse> 형태로 응답
 * - 실패 시 ApiResponse<Void> 형태로 에러 응답
 *
 * 💡 사용 예시:
 * POST /api/auth/login
 * Content-Type: application/json
 * {
 *   "username": "student@university.edu",
 *   "password": "securePassword123"
 * }
 *
 * 🔒 보안 고려사항:
 * - 반드시 HTTPS 프로토콜에서만 사용
 * - 패스워드는 평문으로 전송되므로 즉시 해싱하여 검증
 * - 로그인 실패 시 사용자명 존재 여부 노출하지 않음 (보안)
 * - 브루트 포스 공격 방지를 위한 Rate Limiting 적용 권장
 * - 패스워드 정책 준수 (최소 길이, 복잡성 등)
 *
 * ⚠️ 주의사항:
 * - 클라이언트에서 패스워드를 안전하게 입력받을 것
 * - 서버에서는 이 데이터를 즉시 폐기하고 해싱된 값과 비교
 * - 로그에 패스워드 값 기록 금지
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class LoginRequest {
    
    /**
     * 사용자 식별자 필드
     * 이메일 주소 형식의 사용자명을 저장
     *
     * 유효성 검증:
     * - 필수 입력 (@NotBlank)
     * - null, 빈 문자열, 공백만으로 구성된 문자열 모두 거부
     *
     * 보안 고려사항:
     * - 이메일 형식 검증은 별도 어노테이션으로 추가 가능
     * - 대소문자 구분하여 저장 (보통 소문자로 정규화)
     * - 로그인 실패 시 존재 여부 노출하지 않음
     *
     * 사용 예시:
     * "student@university.edu"
     * "professor@school.ac.kr"
     */
    @NotBlank(message = "Username is required")
    private String username;
    
    /**
     * 사용자 비밀번호 필드
     * 평문 비밀번호를 임시로 저장 (즉시 검증 후 폐기)
     *
     * 유효성 검증:
     * - 필수 입력 (@NotBlank)
     * - null, 빈 문자열, 공백만으로 구성된 문자열 모두 거부
     *
     * 보안 고려사항:
     * - 절대 로그에 기록하지 않음
     * - 메모리에 최소 시간만 유지
     * - 서버에서는 즉시 해싱하여 데이터베이스의 해시값과 비교
     * - 비교 후 즉시 메모리에서 제거
     * - 패스워드 정책 검증은 별도 서비스에서 수행
     *
     * ⚠️ 중요: 이 필드의 값은 절대 저장되지 않으며,
     * 검증 목적으로만 일시적으로 사용됨
     *
     * 사용 예시:
     * "securePassword123" (평문, 검증 즉시 폐기)
     */
    @NotBlank(message = "Password is required")
    private String password;
    
    /**
     * 기본 생성자
     * JSON 역직렬화나 빈 객체 생성 시 사용
     */
    public LoginRequest() {}
    
    /**
     * 모든 필드를 초기화하는 생성자
     * 프로그래밍 방식으로 LoginRequest 객체를 생성할 때 사용
     *
     * @param username 사용자 식별자 (이메일)
     * @param password 사용자 비밀번호 (평문)
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    /**
     * 사용자명 getter
     * @return 사용자 식별자 (이메일 주소)
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * 사용자명 setter
     * @param username 설정할 사용자 식별자
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * 비밀번호 getter
     * ⚠️ 보안 주의: 이 메서드는 디버깅 목적으로만 사용
     * 실제 운영에서는 패스워드 값에 접근하지 않도록 권장
     *
     * @return 사용자 비밀번호 (평문)
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * 비밀번호 setter
     * @param password 설정할 사용자 비밀번호
     */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /*
     * ===========================================
     * 전체 로그인 플로우 설명
     * ===========================================
     *
     * 1. 클라이언트 요청
     *    POST /api/auth/login
     *    Content-Type: application/json
     *    Body: { "username": "...", "password": "..." }
     *
     * 2. 컨트롤러에서 이 객체로 매핑
     *    @PostMapping("/login")
     *    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
     *        // 유효성 검증 (@Valid 어노테이션에 의해 자동 실행)
     *        // @NotBlank 검증 실패 시 MethodArgumentNotValidException 발생
     *    }
     *
     * 3. 서비스에서 비즈니스 로직 처리
     *    - username으로 사용자 조회
     *    - password를 해싱하여 저장된 해시와 비교
     *    - 성공 시 JWT 토큰 생성
     *
     * 4. 응답 생성
     *    - 성공: ApiResponse.success("로그인 성공", LoginResponse(토큰, 사용자정보))
     *    - 실패: ApiResponse.failure("로그인 실패: 잘못된 사용자명 또는 비밀번호")
     *
     * 🔒 보안 플로우:
     * - 클라이언트 → HTTPS → 서버
     * - 서버에서 즉시 패스워드 해싱 및 비교
     * - 비교 후 메모리에서 패스워드 제거
     * - JWT 토큰 발급 및 반환
     *
     * ⚠️ 보안 주의사항:
     * - 패스워드는 절대 로그에 기록하지 않음
     * - 로그인 실패 시 구체적인 실패 이유 노출하지 않음
     * - Rate Limiting 적용으로 브루트 포스 공격 방지
     * - 계정 잠금 정책 적용 고려
     */
}
