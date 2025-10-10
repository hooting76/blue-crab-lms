package BlueCrab.com.example.exception;

import BlueCrab.com.example.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.persistence.PessimisticLockException;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리를 위한 핸들러 클래스
 * 모든 컨트롤러에서 발생하는 예외를 일관되게 처리
 *
 * 주요 기능:
 * - 다양한 예외 타입별로 적절한 HTTP 상태코드와 에러 메시지 반환
 * - 표준화된 ApiResponse 형식으로 에러 응답 통일
 * - 예외 발생 시 로깅을 통한 모니터링 및 디버깅 지원
 * - 보안 관련 예외(인증 실패, 토큰 오류 등) 우선 처리
 *
 * 처리하는 예외 유형:
 * - RuntimeException: 일반 비즈니스 로직 예외 (400 Bad Request)
 * - BadCredentialsException: 인증 실패 (401 Unauthorized)
 * - JWT 관련 예외: 토큰 만료/서명/형식 오류 (401/400)
 * - 유효성 검사 예외: @Valid 실패 (400 Bad Request)
 * - ResourceNotFoundException: 리소스 없음 (404 Not Found)
 * - DuplicateResourceException: 리소스 중복 (409 Conflict)
 * - Exception: 예상치 못한 시스템 오류 (500 Internal Server Error)
 *
 * 응답 형식 표준화:
 * 모든 에러 응답은 ApiResponse.failure()를 사용하여 일관된 JSON 구조 유지
 * {
 *   "success": false,
 *   "message": "에러 메시지",
 *   "data": null 또는 상세 에러 정보,
 *   "timestamp": "에러 발생 시간"
 * }
 *
 * 보안 고려사항:
 * - 민감한 정보(예외 스택 트레이스)는 클라이언트에게 노출하지 않음
 * - 인증 실패 시 구체적인 실패 이유 노출 제한 (보안)
 * - 로그에는 상세 정보 기록하여 내부 모니터링
 *
 * 사용 예시:
 * - 컨트롤러에서 예외 발생 시 자동으로 이 핸들러가 처리
 * - 클라이언트는 일관된 에러 응답 형식으로 에러 처리 가능
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 로깅을 위한 Logger 인스턴스
     * 예외 발생 시 warn/error 레벨로 로그 기록
     * 운영 환경 모니터링 및 디버깅에 사용
     */
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 비즈니스 로직 예외 처리
     * RuntimeException을 상속한 사용자 정의 예외들을 처리
     *
     * 처리 대상:
     * - DuplicateResourceException (중복 리소스)
     * - ResourceNotFoundException (리소스 없음)
     * - 기타 비즈니스 로직 예외
     *
     * HTTP 응답: 400 Bad Request
     * 로그 레벨: WARN (비즈니스 로직 오류는 경고로 기록)
     *
     * 사용 예시:
     * 서비스에서 throw new RuntimeException("잘못된 요청입니다.");
     * → 클라이언트에 400 응답과 함께 메시지 반환
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        logger.warn("Runtime exception occurred: {} at {}", ex.getMessage(), request.getRequestURI());
        
        ApiResponse<Object> response = ApiResponse.failure(ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 인증 실패 예외 처리
     * 로그인 실패 등 인증 관련 예외를 처리
     *
     * 처리 대상:
     * - 잘못된 사용자명/비밀번호
     * - 계정 잠금 상태
     * - 기타 Spring Security 인증 실패
     *
     * HTTP 응답: 401 Unauthorized
     * 로그 레벨: WARN (인증 실패는 보안 모니터링을 위해 기록)
     *
     * 보안 고려사항:
     * - 구체적인 실패 이유 노출하지 않음 (브루트 포스 공격 방지)
     * - 로그에는 상세 정보 기록하여 내부 분석
     *
     * 사용 예시:
     * 로그인 시 잘못된 비밀번호 입력
     * → 클라이언트에 401 응답과 일반적인 메시지 반환
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        
        logger.warn("Authentication failed: {} at {}", ex.getMessage(), request.getRequestURI());
        
        ApiResponse<Object> response = ApiResponse.failure("인증에 실패했습니다. 사용자명과 비밀번호를 확인해주세요.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * JWT 토큰 만료 예외 처리
     * 만료된 액세스 토큰에 대한 예외를 처리
     *
     * 처리 대상:
     * - io.jsonwebtoken.ExpiredJwtException
     *
     * HTTP 응답: 401 Unauthorized
     * 로그 레벨: WARN (토큰 만료는 정상적인 상황이나 모니터링 필요)
     *
     * 사용 예시:
     * 만료된 토큰으로 API 호출
     * → 클라이언트에 401 응답과 리프레시 토큰 사용 안내 메시지 반환
     */
    @ExceptionHandler({io.jsonwebtoken.ExpiredJwtException.class})
    public ResponseEntity<ApiResponse<Object>> handleExpiredJwtException(
            io.jsonwebtoken.ExpiredJwtException ex, HttpServletRequest request) {
        
        logger.warn("JWT token expired: {} at {}", ex.getMessage(), request.getRequestURI());
        
        ApiResponse<Object> response = ApiResponse.failure(
            "토큰이 만료되었습니다. 리프레시 토큰을 사용하여 새 토큰을 발급받아주세요."
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * JWT 토큰 서명 검증 실패 예외 처리
     * 잘못된 서명의 토큰에 대한 예외를 처리
     *
     * 처리 대상:
     * - io.jsonwebtoken.security.SignatureException
     *
     * HTTP 응답: 401 Unauthorized
     * 로그 레벨: WARN (토큰 위조 시도는 보안 위협으로 간주)
     *
     * 보안 고려사항:
     * - 토큰 위조 시도 감지 및 로깅
     * - 클라이언트에게는 일반적인 유효하지 않은 토큰 메시지 반환
     *
     * 사용 예시:
     * 변조된 토큰으로 API 호출
     * → 클라이언트에 401 응답과 재로그인 안내 메시지 반환
     */
    @ExceptionHandler({io.jsonwebtoken.security.SignatureException.class})
    public ResponseEntity<ApiResponse<Object>> handleJwtSignatureException(
            Exception ex, HttpServletRequest request) {
        
        logger.warn("JWT signature validation failed: {} at {}", ex.getMessage(), request.getRequestURI());
        
        ApiResponse<Object> response = ApiResponse.failure(
            "유효하지 않은 토큰입니다. 다시 로그인해주세요."
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * JWT 토큰 형식 오류 예외 처리
     * 잘못된 형식의 토큰에 대한 예외를 처리
     *
     * 처리 대상:
     * - io.jsonwebtoken.MalformedJwtException (형식 오류)
     * - io.jsonwebtoken.UnsupportedJwtException (지원하지 않는 형식)
     *
     * HTTP 응답: 400 Bad Request
     * 로그 레벨: WARN (클라이언트 측 토큰 생성 오류)
     *
     * 사용 예시:
     * 잘못된 형식의 토큰 전송
     * → 클라이언트에 400 응답과 토큰 형식 오류 메시지 반환
     */
    @ExceptionHandler({io.jsonwebtoken.MalformedJwtException.class, io.jsonwebtoken.UnsupportedJwtException.class})
    public ResponseEntity<ApiResponse<Object>> handleJwtFormatException(
            Exception ex, HttpServletRequest request) {
        
        logger.warn("JWT format error: {} at {}", ex.getMessage(), request.getRequestURI());
        
        ApiResponse<Object> response = ApiResponse.failure(
            "토큰 형식이 올바르지 않습니다. 다시 로그인해주세요."
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 유효성 검사 실패 예외 처리 (@Valid)
     * 요청 데이터의 유효성 검사 실패 시 처리
     *
     * 처리 대상:
     * - MethodArgumentNotValidException (@Valid 실패)
     * - BindException (데이터 바인딩 실패)
     *
     * HTTP 응답: 400 Bad Request
     * 로그 레벨: WARN (클라이언트 입력 오류)
     *
     * 응답 형식:
     * {
     *   "success": false,
     *   "message": "입력 데이터가 올바르지 않습니다.",
     *   "data": {
     *     "fieldName": "에러 메시지",
     *     "anotherField": "다른 에러 메시지"
     *   }
     * }
     *
     * 사용 예시:
     * 필수 필드 누락 또는 잘못된 형식으로 요청
     * → 클라이언트에 400 응답과 필드별 상세 에러 정보 반환
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            Exception ex, HttpServletRequest request) {
        
        logger.warn("Validation failed at {}: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        
        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException validEx = (MethodArgumentNotValidException) ex;
            validEx.getBindingResult().getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });
        } else if (ex instanceof BindException) {
            BindException bindEx = (BindException) ex;
            bindEx.getBindingResult().getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });
        }
        
        ApiResponse<Map<String, String>> response = ApiResponse.failure("입력 데이터가 올바르지 않습니다.", errors);
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 인증되지 않은 접근 예외 처리
     * UnauthorizedException을 처리하여 401 응답 반환
     *
     * 처리 대상:
     * - 관리자 권한 없음
     * - 유효하지 않은 토큰
     *
     * HTTP 응답: 401 Unauthorized
     * 로그 레벨: WARN (보안 모니터링)
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(
            UnauthorizedException ex, HttpServletRequest request) {

        logger.warn("Unauthorized access: {} at {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.failure(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 리소스를 찾을 수 없는 경우 예외 처리
     * ResourceNotFoundException을 처리하여 404 응답 반환
     *
     * 처리 대상:
     * - 존재하지 않는 리소스 조회
     * - 잘못된 ID로 조회
     *
     * HTTP 응답: 404 Not Found
     * 로그 레벨: WARN (클라이언트 요청 오류)
     *
     * 사용 예시:
     * 존재하지 않는 사용자 ID로 조회
     * → 클라이언트에 404 응답과 리소스 없음 메시지 반환
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        logger.warn("Resource not found: {} at {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.failure(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * 중복 리소스 예외 처리
     * DuplicateResourceException을 처리하여 409 응답 반환
     *
     * 처리 대상:
     * - 중복된 이메일로 회원가입 시도
     * - 중복된 데이터 삽입
     *
     * HTTP 응답: 409 Conflict
     * 로그 레벨: WARN (비즈니스 규칙 위반)
     *
     * 사용 예시:
     * 이미 존재하는 이메일로 회원가입
     * → 클라이언트에 409 응답과 중복 리소스 메시지 반환
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {

        logger.warn("Duplicate resource: {} at {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.failure(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * 비관적 락 타임아웃 예외 처리
     * 동시성 제어 중 락 획득 타임아웃 발생 시 처리
     *
     * 처리 대상:
     * - 시설 예약 생성/승인 시 락 대기 타임아웃
     * - 다른 트랜잭션이 락을 보유하여 대기 시간 초과
     *
     * HTTP 응답: 409 Conflict
     * 로그 레벨: WARN (동시 접근으로 인한 충돌)
     *
     * 사용 예시:
     * 여러 사용자가 동시에 같은 시설 예약 시도
     * → 클라이언트에 409 응답과 재시도 안내 메시지 반환
     */
    @ExceptionHandler(PessimisticLockException.class)
    public ResponseEntity<ApiResponse<Object>> handlePessimisticLockException(
            PessimisticLockException ex, HttpServletRequest request) {

        logger.warn("Pessimistic lock timeout: {} at {}", ex.getMessage(), request.getRequestURI());

        ApiResponse<Object> response = ApiResponse.failure(
            "요청이 집중되어 처리할 수 없습니다. 잠시 후 다시 시도해주세요."
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    /**
     * 예상하지 못한 모든 예외 처리
     * 처리되지 않은 모든 예외를 시스템 오류로 처리
     *
     * 처리 대상:
     * - 데이터베이스 연결 오류
     * - 외부 API 호출 실패
     * - 시스템 리소스 부족
     * - 기타 예상치 못한 예외
     *
     * HTTP 응답: 500 Internal Server Error
     * 로그 레벨: ERROR (시스템 오류로 상세 로깅 필요)
     *
     * 보안 고려사항:
     * - 클라이언트에게는 일반적인 시스템 오류 메시지만 노출
     * - 로그에는 스택 트레이스 포함하여 내부 디버깅
     *
     * 사용 예시:
     * 데이터베이스 연결 실패
     * → 클라이언트에 500 응답과 일반적인 시스템 오류 메시지 반환
     * → 로그에 상세 오류 정보 기록
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        logger.error("Unexpected error occurred: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
        
        ApiResponse<Object> response = ApiResponse.failure("시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}