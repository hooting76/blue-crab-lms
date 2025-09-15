/* 작업자 : 성태준
 * 이메일 인증 코드 방식 처리 컨트롤러
 * "/sendMail" : 메일 인증 코드를 생성하고 유저에게 메일 발송.
 * "/verifyCode" : 유저가 입력한 인증 코드를 검증.
 * JWT 토큰 기반 인증을 사용하며, Redis를 활용해 인증 코드와 세션 정보를 관리.
 */

package BlueCrab.com.example.controller;

// ========== 임포트 구문 ==========

// ========== Spring Framework 관련 임포트 ==========
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

// ========== Java 표준 라이브러리 임포트 ==========
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

// ========== 외부 라이브러리 임포트 ==========
import lombok.extern.slf4j.Slf4j;

// ========== 프로젝트 내부 클래스 임포트 ==========
import BlueCrab.com.example.dto.AuthResponse;  // 공통 응답 DTO
import BlueCrab.com.example.dto.AuthCodeVerifyRequest;  // 인증 코드 검증 요청 DTO
import BlueCrab.com.example.service.EmailService;  // 이메일 발송 서비스
import BlueCrab.com.example.service.RedisService;  // Redis 기반 서비스 (코드 검증 시도 제한 등)
import BlueCrab.com.example.util.RequestUtils;  // 클라이언트 요청 유틸리티
import BlueCrab.com.example.util.MailAuthRateLimitUtils;  // 메일 인증 요청 빈도 제한 유틸리티
import BlueCrab.com.example.util.EmailTemplateUtils;  // 이메일 템플릿 생성 유틸리티
import BlueCrab.com.example.util.AuthCodeGenerator;  // 인증 코드 생성 유틸리티
import BlueCrab.com.example.util.AuthCodeValidator;  // 인증 코드 검증 유틸리티
import BlueCrab.com.example.util.UserNameExtractor;  // 사용자 이름 추출 유틸리티
import BlueCrab.com.example.util.RedisAuthDataManager;  // Redis 인증 데이터 관리 유틸리티

// ========== 컨트롤러 클래스 정의 ==========

@RestController  // RESTful 웹 서비스 컨트롤러
@Slf4j  // 로깅 지원
public class MailAuthCheckController {
    
    // ========== 설정 상수 ==========
    private static final int AUTH_CODE_EXPIRY_MINUTES = 5; 
	// 인증 코드 유효 기간, 단위: 분
    private static final int AUTH_CODE_LENGTH = 6;	
	// 인증 코드 길이, 단위: 자리수
    private static final String AUTH_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	// 인증 코드에 사용될 문자 집합 (영문 대문자 + 숫자)
    
    // ========== 의존성 주입 ==========
    @Autowired
    private EmailService emailService;
	// 이메일 발송 서비스, 인증 코드 이메일 전송에 사용
    
    // @Autowired
    // private BCryptPasswordEncoder bCryptPasswordEncoder;
	// 인증 방식을 코드 기반으로 변경하면서 불필요해짐

    @Autowired
    private MailAuthRateLimitUtils mailAuthRateLimit;
    // 메일 인증 요청 빈도 제한 유틸리티
    
    @Autowired
    private RedisService redisService;
    // Redis 기반 서비스 (코드 검증 시도 제한 등)
    
    @Autowired
    private EmailTemplateUtils emailTemplateUtils;
    // 이메일 템플릿 생성 유틸리티
    
    @Autowired
    private AuthCodeGenerator authCodeGenerator;
    // 인증 코드 생성 유틸리티
    
    @Autowired
    private AuthCodeValidator authCodeValidator;
    // 인증 코드 검증 유틸리티
    
    @Autowired
    private UserNameExtractor userNameExtractor;
    // 사용자 이름 추출 유틸리티
    
    @Autowired
    private RedisAuthDataManager redisAuthDataManager;
    // Redis 인증 데이터 관리 유틸리티
    
    @Value("${app.domain}")
    private String domain;
	// 애플리케이션 도메인, 이메일 내용에 사용
    
    // ========== 인증 설정 조회 API ==========
    @GetMapping("/api/auth/config")
    public ResponseEntity<AuthResponse> getAuthConfig() {
		// 인증 설정 정보를 클라이언트에 제공하는 API
		// 인증 코드 길이, 유효 기간, 사용 가능한 문자 집합 등을 반환
		
		// try-catch 블록으로 예외 처리
        try {
            Map<String, Object> configData = Map.of(
				// 설정 정보 맵 생성
                "authCodeExpiryMinutes", AUTH_CODE_EXPIRY_MINUTES,
                "authCodeLength", AUTH_CODE_LENGTH,
                "allowedCharacters", AUTH_CODE_CHARACTERS,
                "description", String.format("인증 코드는 %d자리 영문 대문자와 숫자 조합이며, %d분간 유효합니다.", 
                    AUTH_CODE_LENGTH, AUTH_CODE_EXPIRY_MINUTES)
            );
            
            AuthResponse response = new AuthResponse("인증 설정 정보를 성공적으로 조회했습니다.");
			// AuthResponse response : 응답 객체 생성
			// new AuthResponse() : 메시지와 데이터를 포함하는 응답 객체 생성
            response.setData(configData);
			// 설정 정보 맵을 응답 데이터로 설정
            log.info("Auth config retrieved: {}", configData);
            // 설정 정보 로그 기록
            return ResponseEntity.ok(response);
			// HTTP 200 OK 응답과 함께 응답 객체 반환
            
		// 예외 발생 시
        } catch (Exception e) {
            log.error("Failed to retrieve auth config - Error: {}", e.getMessage());
			// 오류 로그 기록
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("설정 정보 조회 중 오류가 발생했습니다."));
					// HTTP 500 Internal Server Error 응답 반환
        } // try-catch 끝
    } // getAuthConfig 끝
    
    // ========== 인증 코드 발송 API ==========
    @GetMapping("/sendMail")
	// @GetMapping("/sendMail") : 이메일 인증 코드 발송 엔드포인트
    public ResponseEntity<AuthResponse> sendAuthCode(HttpServletRequest request, Authentication authentication) {
		// ResponseEntity<AuthResponse> : HTTP 응답 본문에 AuthResponse 객체 포함
		// HttpServletRequest request : 클라이언트 요청 정보
		// Authentication authentication : 인증된 사용자 정보
        String userEmail = authentication.getName();
		// 인증된 사용자의 이메일 주소
        String clientIp = RequestUtils.getClientIpAddress(request);
		// 클라이언트 IP 주소 추출

        log.info("Request - User: {}, IP: {}", userEmail, clientIp);
		// 요청 로그 기록
        
		// try-catch 블록으로 예외 처리
        try {
            // Rate Limit 확인 (메일 발송)
            if (!mailAuthRateLimit.isSendMailAllowed(userEmail)) {
                // 허용되지 않는 경우 (빈도 제한 초과)
                int waitMinutes = mailAuthRateLimit.getRemainingWaitMinutes(userEmail);
                // 남은 대기 시간 조회
                String errorMessage = String.format("메일 발송 요청이 너무 빈번합니다. %d분 후 다시 시도해주세요.", waitMinutes);
                // 에러 메시지 생성
                log.warn("Send mail rate limit exceeded - User: {}, Wait minutes: {}", userEmail, waitMinutes);
                // 경고 로그 기록
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new AuthResponse(errorMessage));
                    // HTTP 429 Too Many Requests 응답 반환
                    // Rate Limit 초과 시 적절한 상태 코드와 메시지 반환
            }
            
            String userName = userNameExtractor.extractUserNameFromAuthentication(authentication);
			// UserNameExtractor를 사용하여 JWT에서 사용자 이름 추출
            
            redisAuthDataManager.cleanup(userEmail);
            // 기존 인증 데이터 삭제 (중복 요청이 있을 경우의 대비용)
            
            String authSessionId = authCodeGenerator.generateAuthSessionId(userEmail);
			// AuthCodeGenerator를 사용하여 고유한 인증 세션 ID 생성
            String authCode = authCodeGenerator.generateAuthCode();
			// AuthCodeGenerator를 사용하여 랜덤 인증 코드 생성
            LocalDateTime codeCreatedTime = LocalDateTime.now();
			// 인증 코드 생성 시간 기록
            
            RedisAuthDataManager.AuthCodeData authData = new RedisAuthDataManager.AuthCodeData(
                authCode, userEmail, clientIp, codeCreatedTime, authSessionId);
            redisAuthDataManager.saveAuthData(userEmail, authData);
			// Redis에 인증 데이터 저장
            
            String emailContent = emailTemplateUtils.createAuthCodeEmailTemplate(userName, authCode, AUTH_CODE_EXPIRY_MINUTES);
			// 이메일 템플릿 유틸리티를 사용하여 HTML 이메일 내용 생성
            emailService.sendMIMEMessage("bluecrabacademy@gmail.com", userEmail, 
                "BlueCrab 이메일 인증 코드", emailContent);
				// 이메일 발송 서비스 호출 (발신자, 수신자, 제목, 내용)

            mailAuthRateLimit.recordSendMailRequest(userEmail);
            // Rate Limit 카운터 증가 (발송 성공 시)

            log.info("Success - User: {}, Session ID: {}", userEmail, authSessionId);
			// 발송 완료 로그 기록
            
            return ResponseEntity.ok(new AuthResponse(
				// HTTP 200 OK 응답과 함께 성공 메시지 반환
                String.format("인증코드가 발송되었습니다. %d분 이내에 인증을 완료해주세요.", AUTH_CODE_EXPIRY_MINUTES)
				// 메시지에 인증 코드 유효 기간 포함
            ));
            
		// 예외 발생 시
        } catch (Exception e) {
            log.error("Failed to send auth code - User: {}, IP: {}, Error: {}", userEmail, clientIp, e.getMessage());
			// 오류 로그 기록
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("인증 코드 발송 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
					// HTTP 500 Internal Server Error 응답 반환
        } // try-catch 끝
    } // sendAuthCode 끝
    
    // ========== 인증 코드 확인 API ==========
    @PostMapping("/verifyCode")
	// @PostMapping("/verifyCode") : 인증 코드 확인 엔드포인트
    public ResponseEntity<AuthResponse> verifyAuthCode(
		// ResponseEntity<AuthResponse> : HTTP 응답 본문에 AuthResponse 객체 포함
            @RequestBody AuthCodeVerifyRequest request,
            HttpServletRequest httpRequest, 
            Authentication authentication) {
				// @RequestBody AuthCodeVerifyRequest request : 요청 본문에서 인증 코드 추출
				// HttpServletRequest httpRequest : 클라이언트 요청 정보
				// Authentication authentication : 인증된 사용자 정보
        
        String userEmail = authentication.getName();
		// 인증된 사용자의 이메일 주소
        String clientIp = RequestUtils.getClientIpAddress(httpRequest);
		// 클라이언트 IP 주소 추출
        String inputCode = request.getAuthCode();
		// 요청 본문에서 입력된 인증 코드 추출

        log.info("Auth code verification request - User: {}, IP: {}", userEmail, clientIp);
		// 요청 로그 기록
        
		// try-catch 블록으로 예외 처리
        try {
            // Rate Limit 확인 (코드 검증) - RedisService 사용
            try {
                redisService.checkEmailVerificationAttempts(userEmail);
                // 인증 코드 검증 시도 횟수 확인 및 증가
            } catch (RuntimeException e) {
                // 인증 시도 횟수 초과 시
                log.warn("Verify code rate limit exceeded - User: {}, Error: {}", userEmail, e.getMessage());
                // 경고 로그 기록
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new AuthResponse(e.getMessage()));
                    // HTTP 429 Too Many Requests 응답 반환
                    // Rate Limit 초과 시 적절한 상태 코드와 메시지 반환
            }
            
            if (authCodeValidator.isInvalidAuthCode(inputCode)) {
				// 입력된 인증 코드 형식이 올바르지 않은 경우
                // Rate Limit은 RedisService에서 자동으로 관리됨 (checkEmailVerificationAttempts 호출 시)
                log.warn("Invalid auth code format - User: {}, IP: {}", userEmail, clientIp);
                // 경고 로그 기록
                return ResponseEntity.badRequest()
                        .body(new AuthResponse("인증 코드가 올바르지 않습니다."));
					// HTTP 400 Bad Request 응답 반환
            } // 입력 파라미터 검증 끝
            
            RedisAuthDataManager.AuthCodeData authData = redisAuthDataManager.getAuthData(userEmail);
			// RedisAuthDataManager.AuthCodeData authData : Redis에서 인증 데이터 조회
			// redisAuthDataManager.getAuthData(userEmail) : 사용자 이메일로 Redis에서 인증 데이터 조회 
            if (authData == null) {
				// 인증 데이터가 없는 경우
                log.warn("No auth data found - User: {}, IP: {}", userEmail, clientIp);
                // 경고 로그 기록
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse("인증 코드가 만료되었거나 존재하지 않습니다. 새로운 코드를 요청해주세요."));
					// HTTP 401 Unauthorized 응답 반환
            } // Redis에서 인증 데이터 조회 끝
            
            if (authCodeValidator.isAuthCodeExpired(authData.getCodeCreatedTime())) {
				// 인증 코드가 만료된 경우
                redisAuthDataManager.cleanup(userEmail);
				// Redis에서 인증 데이터 정리
                log.warn("Auth code expired - User: {}, IP: {}", userEmail, clientIp);
                // 만료 경고 로그 기록
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse("인증 코드가 만료되었습니다. 새로운 코드를 요청해주세요."));
					// HTTP 401 Unauthorized 응답 반환
            } // 인증 코드 만료 검사 끝
            
            if (!authData.getClientIp().equals(clientIp)) {
				// 요청한 IP 주소와 저장된 IP 주소가 다른 경우 (보안상 이유)
                redisAuthDataManager.cleanup(userEmail);
				// Redis에서 인증 데이터 정리
                log.warn("IP address mismatch - Request IP: {}, Stored IP: {}", clientIp, authData.getClientIp());
				// IP 불일치 경고 로그 기록
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new AuthResponse("보안상 이유로 인증이 거부되었습니다. 인증을 요청한 기기에서만 가능합니다."));
					// HTTP 403 Forbidden 응답 반환
            } // IP 주소 불일치 검사 끝
            
            if (inputCode.toUpperCase().equals(authData.getAuthCode())) {
				// 입력된 인증 코드가 저장된 코드와 일치하는 경우
                redisService.clearEmailVerificationAttempts(userEmail);
                // RedisService에서 인증 시도 횟수 초기화 (성공 시)
                redisAuthDataManager.cleanup(userEmail);
				// Redis에서 인증 데이터 정리
                log.info("Auth code verification successful - User: {}, IP: {}", userEmail, clientIp);
				// 성공 로그 기록
                return ResponseEntity.ok(new AuthResponse("인증이 성공적으로 완료되었습니다."));
				// HTTP 200 OK 응답과 함께 성공 메시지 반환
            } // 코드 일치 검사 끝
            else {
				// 인증 코드가 일치하지 않는 경우
                // Rate Limit은 RedisService에서 자동으로 관리됨 (checkEmailVerificationAttempts 호출 시)
                redisAuthDataManager.cleanup(userEmail);
				// Redis에서 인증 데이터 정리
                log.warn("Auth code mismatch - User: {}, IP: {}", userEmail, clientIp);
				// 불일치 경고 로그 기록
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse(String.format("인증 코드가 올바르지 않습니다. 인증코드는 영문 대문자와 숫자 조합의 %d자리 문자입니다.", AUTH_CODE_LENGTH)));
						// HTTP 401 Unauthorized 응답 반환
            } // if-else 끝
            
        } catch (Exception e) {
			// 예외 발생 시
            log.error("Auth code verification error - User: {}, IP: {}, Error: {}", userEmail, clientIp, e.getMessage());
			// 오류 로그 기록
            redisAuthDataManager.cleanup(userEmail);
			// Redis에서 인증 데이터 정리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("인증 처리 중 오류가 발생했습니다. 다시 시도해주세요."));
				// HTTP 500 Internal Server Error 응답 반환
        } // try-catch 끝
    } // verifyAuthCode 끝
} // MailAuthCheckController 끝