package BlueCrab.com.example.dto;

/**
 * API 응답을 위한 공통 DTO 클래스
 * 모든 REST 컨트롤러에서 일관된 응답 형식을 제공하기 위해 사용
 *
 * 📋 응답 구조:
 * {
 *   "success": true/false,           // 요청 성공/실패 여부
 *   "message": "처리 결과 메시지",     // 사용자에게 표시할 메시지 (한국어)
 *   "data": { ... },                // 실제 응답 데이터 (제네릭 타입)
 *   "timestamp": "2025-08-27T..."   // 응답 생성 시간 (ISO-8601)
 * }
 *
 * 🎯 사용 목적:
 * - 모든 API 응답의 일관성 보장
 * - 클라이언트가 예측 가능한 응답 형식 제공
 * - 성공/실패 상태를 명확히 구분
 * - 디버깅을 위한 타임스탬프 포함
 *
 * 💡 사용 예시:
 *
 * // 성공 응답 (데이터 포함)
 * return ResponseEntity.ok(ApiResponse.success("사용자 조회 성공", user));
 *
 * // 성공 응답 (데이터 없음)
 * return ResponseEntity.ok(ApiResponse.success("로그아웃 완료"));
 *
 * // 실패 응답
 * return ResponseEntity.badRequest().body(ApiResponse.failure("입력값이 유효하지 않습니다"));
 *
 * // 에러 응답 (상세 데이터 포함)
 * return ResponseEntity.status(500).body(ApiResponse.failure("서버 오류", errorDetails));
 *
 * 🔗 연동 클래스:
 * - 모든 Controller 클래스에서 사용
 * - GlobalExceptionHandler에서 예외 응답으로 활용
 * - 프론트엔드에서 일관된 응답 파싱 가능
 *
 * @param <T> 응답 데이터의 타입 (UserTbl, List<User>, String 등)
 * @author BlueCrab Development Team
 * @version 1.0.0
 */
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private String timestamp;
    private String errorCode;  // 에러 코드 필드 추가
    
    /**
     * 기본 생성자
     */
    public ApiResponse() {
        this.timestamp = java.time.Instant.now().toString();
    }
    
    /**
     * 성공 응답 생성자
     * @param success 성공 여부
     * @param message 응답 메시지
     * @param data 응답 데이터
     */
    public ApiResponse(boolean success, String message, T data) {
        this();
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = null;
    }

    /**
     * 에러 응답 생성자 (에러 코드 포함)
     * @param success 성공 여부
     * @param message 응답 메시지
     * @param data 응답 데이터
     * @param errorCode 에러 코드
     */
    public ApiResponse(boolean success, String message, T data, String errorCode) {
        this();
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
    }
    
    /**
     * 성공 응답 생성 (데이터 포함)
     * 컨트롤러에서 데이터를 반환할 때 사용
     *
     * @param message 성공 메시지 (사용자에게 표시)
     * @param data 실제 응답 데이터
     * @return 구성된 ApiResponse 객체
     *
     * 사용 예시:
     * return ResponseEntity.ok(ApiResponse.success("사용자 조회 성공", userList));
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
    
    /**
     * 성공 응답 생성 (데이터 없음)
     * 단순 확인 응답이나 상태 변경 성공 시 사용
     *
     * @param message 성공 메시지
     * @return 구성된 ApiResponse 객체
     *
     * 사용 예시:
     * return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다"));
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }
    
    /**
     * 실패 응답 생성
     * 에러 상황에서 사용자에게 전달할 메시지만 포함
     *
     * @param message 실패 메시지
     * @return 구성된 ApiResponse 객체
     *
     * 사용 예시:
     * return ResponseEntity.badRequest().body(ApiResponse.failure("입력값이 올바르지 않습니다"));
     */
    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, message, null);
    }
    
    /**
     * 실패 응답 생성 (상세 데이터 포함)
     * 에러 상세 정보까지 포함해서 반환할 때 사용
     *
     * @param message 실패 메시지
     * @param data 에러 상세 정보
     * @return 구성된 ApiResponse 객체
     *
     * 사용 예시:
     * return ResponseEntity.status(500).body(ApiResponse.failure("서버 오류", errorDetails));
     */
    public static <T> ApiResponse<T> failure(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }

    /**
     * 레이트리밋 에러 응답 생성
     * HTTP 429와 함께 사용하여 프론트엔드에서 구분 가능
     *
     * @param message 레이트리밋 메시지
     * @return 구성된 ApiResponse 객체 (errorCode: "RATE_LIMIT_EXCEEDED")
     *
     * 사용 예시:
     * return ResponseEntity.status(429).body(ApiResponse.rateLimitError("요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
     */
    public static <T> ApiResponse<T> rateLimitError(String message) {
        return new ApiResponse<>(false, message, null, "RATE_LIMIT_EXCEEDED");
    }

    /**
     * 레이트리밋 에러 응답 생성 (추가 데이터 포함)
     * 대기 시간 등의 정보를 포함할 때 사용
     *
     * @param message 레이트리밋 메시지
     * @param data 추가 정보 (예: 대기 시간)
     * @return 구성된 ApiResponse 객체 (errorCode: "RATE_LIMIT_EXCEEDED")
     */
    public static <T> ApiResponse<T> rateLimitError(String message, T data) {
        return new ApiResponse<>(false, message, data, "RATE_LIMIT_EXCEEDED");
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}