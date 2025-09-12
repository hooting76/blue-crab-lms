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

// ========== Spring Data Redis 관련 임포트 ==========
import org.springframework.data.redis.core.RedisTemplate;

// ========== Java 표준 라이브러리 임포트 ==========
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.Random;
import java.util.Optional;

// ========== 외부 라이브러리 임포트 ==========
import lombok.extern.slf4j.Slf4j;

// ========== 프로젝트 내부 클래스 임포트 ==========
import BlueCrab.com.example.dto.AuthResponse;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.dto.AuthCodeVerifyRequest;
import BlueCrab.com.example.service.EmailService;
import BlueCrab.com.example.util.RequestUtils;

// ========== 사용하지 않는 주석 처리된 임포트 ==========
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@RestController
@Slf4j
public class MailAuthCheckController {
    
    // ========== 설정 상수 ==========
    private static final int AUTH_CODE_EXPIRY_MINUTES = 5; 
	// 인증 코드 유효 기간, 단위: 분
    private static final int AUTH_CODE_LENGTH = 6;	
	// 인증 코드 길이, 단위: 자리수
    private static final String AUTH_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	// 인증 코드에 사용될 문자 집합 (영문 대문자 + 숫자)
    private static final String AUTH_SESSION_PREFIX = "email_auth:code:";
	// Redis에 저장될 인증 세션 키 접두사, 사용자 이메일과 조합하여 고유 키 생성
    
    // ========== 의존성 주입 ==========
    @Autowired
    private EmailService emailService;
	// 이메일 발송 서비스, 인증 코드 이메일 전송에 사용
    
    // @Autowired
    // private BCryptPasswordEncoder bCryptPasswordEncoder;
	// 인증 방식을 코드 기반으로 변경하면서 불필요해짐
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
	// RedisTemplate을 사용하여 Redis에 인증 코드 및 세션 정보 저장/조회
    
    @Value("${app.domain}")
    private String domain;
	// 애플리케이션 도메인, 이메일 내용에 사용

	@Autowired
	private UserTblRepository userTblRepository;
	// 사용자 정보 조회를 위한 리포지토리
	// DB에서 이메일로 사용자 이름을 조회하는데 사용
    
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
    
    // ========== 인증 코드 발송 API (기존 엔드포인트 유지) ==========
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
            String userName = extractUserNameFromJWT(authentication);
			// JWT에서 사용자 이름 추출
            
            cleanupAuthData(userEmail);
            // 기존 인증 데이터 삭제 (중복 요청이 있을 경우의 대비용)
            
            String authSessionId = generateAuthSessionId(userEmail);
			// 고유한 인증 세션 ID 생성
            String authCode = generateAuthCode();
			// 랜덤 인증 코드 생성
            LocalDateTime codeCreatedTime = LocalDateTime.now();
			// 인증 코드 생성 시간 기록
            
            saveAuthDataToRedis(authSessionId, authCode, userEmail, clientIp, codeCreatedTime);
			// Redis에 인증 데이터 저장
            
            String emailContent = createAuthCodeEmailContent(userName, authCode);
			// 인증 코드 이메일 내용 생성
            emailService.sendMIMEMessage("bluecrabacademy@gmail.com", userEmail, 
                "BlueCrab 이메일 인증 코드", emailContent);
				// 이메일 발송 서비스 호출 (발신자, 수신자, 제목, 내용)

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
            if (isInvalidAuthCode(inputCode)) {
				// 입력된 인증 코드 유효성 검증
				// 유효하지 않은 경우
                log.warn("Invalid auth code format - User: {}, IP: {}", userEmail, clientIp);
                // 경고 로그 기록
                return ResponseEntity.badRequest()
                        .body(new AuthResponse("인증 코드가 올바르지 않습니다."));
					// HTTP 400 Bad Request 응답 반환
            } // 입력 파라미터 검증 끝
            
            AuthCodeData authData = getAuthDataFromRedis(userEmail);
			// AuthCodeData authData : Redis에서 인증 데이터 조회
			// getAuthDataFromRedis(userEmail) : 사용자 이메일로 Redis에서 인증 데이터 조회 
            if (authData == null) {
				// 인증 데이터가 없는 경우
                log.warn("No auth data found - User: {}, IP: {}", userEmail, clientIp);
                // 경고 로그 기록
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse("인증 코드가 만료되었거나 존재하지 않습니다. 새로운 코드를 요청해주세요."));
					// HTTP 401 Unauthorized 응답 반환
            } // Redis에서 인증 데이터 조회 끝
            
            if (isAuthCodeExpired(authData.getCodeCreatedTime())) {
				// 인증 코드가 만료된 경우
                cleanupAuthData(userEmail);
				// Redis에서 인증 데이터 정리
                log.warn("Auth code expired - User: {}, IP: {}", userEmail, clientIp);
                // 만료 경고 로그 기록
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse("인증 코드가 만료되었습니다. 새로운 코드를 요청해주세요."));
					// HTTP 401 Unauthorized 응답 반환
            } // 인증 코드 만료 검사 끝
            
            if (!authData.getClientIp().equals(clientIp)) {
				// 요청한 IP 주소와 저장된 IP 주소가 다른 경우 (보안상 이유)
                cleanupAuthData(userEmail);
				// Redis에서 인증 데이터 정리
                log.warn("IP address mismatch - Request IP: {}, Stored IP: {}", clientIp, authData.getClientIp());
				// IP 불일치 경고 로그 기록
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new AuthResponse("보안상 이유로 인증이 거부되었습니다. 인증을 요청한 기기에서만 가능합니다."));
					// HTTP 403 Forbidden 응답 반환
            } // IP 주소 불일치 검사 끝
            
            if (inputCode.toUpperCase().equals(authData.getAuthCode())) {
				// 입력된 인증 코드가 저장된 코드와 일치하는 경우
                cleanupAuthData(userEmail);
				// Redis에서 인증 데이터 정리
                log.info("Auth code verification successful - User: {}, IP: {}", userEmail, clientIp);
				// 성공 로그 기록
                return ResponseEntity.ok(new AuthResponse("인증이 성공적으로 완료되었습니다."));
				// HTTP 200 OK 응답과 함께 성공 메시지 반환
            } // 코드 일치 검사 끝
            else {
				// 인증 코드가 일치하지 않는 경우
                cleanupAuthData(userEmail);
				// Redis에서 인증 데이터 정리
                log.warn("Auth code mismatch - User: {}, IP: {}", userEmail, clientIp);
				// 불일치 경고 로그 기록
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse("인증 코드가 올바르지 않습니다. 인증코드는 영문 대문자와 숫자 조합의 %d자리 문자입니다.", AUTH_CODE_LENGTH));
						// HTTP 401 Unauthorized 응답 반환
            } // if-else 끝
            
        } catch (Exception e) {
			// 예외 발생 시
            log.error("Auth code verification error - User: {}, IP: {}, Error: {}", userEmail, clientIp, e.getMessage());
			// 오류 로그 기록
            cleanupAuthData(userEmail);
			// Redis에서 인증 데이터 정리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("인증 처리 중 오류가 발생했습니다. 다시 시도해주세요."));
				// HTTP 500 Internal Server Error 응답 반환
        } // try-catch 끝
    } // verifyAuthCode 끝
    
    // ========== 인증 코드 생성 ==========
    private String generateAuthCode() {
		// String generateAuthCode() : 랜덤 인증 코드 생성 메서드
        Random random = new Random();
		// Random random = new Random() : 랜덤 객체 생성 
        StringBuilder codeBuilder = new StringBuilder();
		// StringBuilder codeBuilder = new StringBuilder() : 코드 문자열 빌더 생성
        
        for (int i = 0; i < AUTH_CODE_LENGTH; i++) {
			// AUTH_CODE_LENGTH : 인증 코드 길이만큼 반복
            int index = random.nextInt(AUTH_CODE_CHARACTERS.length());
			// 랜덤 인덱스 생성 (문자 집합 길이 내)
            codeBuilder.append(AUTH_CODE_CHARACTERS.charAt(index));
			// 랜덤 문자 선택하여 코드 빌더에 추가
        } // for 끝
        
        return codeBuilder.toString();
		// 완성된 인증 코드 문자열 반환
    } // generateAuthCode 끝
    
    // ========== 세션 ID 생성 ==========
    private String generateAuthSessionId(String userEmail) {
		// String generateAuthSessionId(String userEmail) : 고유한 인증 세션 ID 생성 메서드
        return userEmail + ":" + System.currentTimeMillis() + ":" 
                            + UUID.randomUUID()
                            .toString()
                            .substring(0, 8);
		// 이메일, 현재 시간, UUID 조합으로 고유 세션 ID 생성
    } // generateAuthSessionId 끝
    
// ========== JWT에서 사용자 이름 추출 (이름은 DB 참조 버전) ==========
private String extractUserNameFromJWT(Authentication authentication) {
	// JWT 대신 DB에서 사용자 이름을 조회하는 메서드
	// JWT 토큰에 name 클레임이 없는 문제를 해결하기 위해 DB 직접 조회 방식 채택
    String userName = "학생";
	// 사용자 이름의 기본값을 설정, DB 조회 실패 시 안전하게 처리 가능
	// 기본값을 설정함으로서 예외 상황에서도 안전하게 처리 가능

	// try-catch 블록으로 예외 처리
    try {
        String userEmail = authentication.getName();
		// 인증된 사용자의 이메일 주소 추출
		// JWT 토큰의 subject(sub) 클레임에서 가져옴
        log.debug("Extracted user email from JWT: {}", userEmail);
        // 추출된 이메일 로그 기록
        
        if (userEmail != null && !userEmail.trim().isEmpty()) {
			// 이메일이 유효한 경우에만 DB 조회 수행
            Optional<UserTbl> userOptional = userTblRepository.findByUserEmail(userEmail);
			// 이메일로 데이터베이스에서 사용자 정보 조회
			// Optional을 사용하여 사용자가 존재하지 않을 경우 안전하게 처리
            log.debug("DB query executed for user email: {}", userEmail);
            // DB 조회 시도 로그 기록
            
            if (userOptional.isPresent()) {
				// 사용자가 존재하는 경우
                UserTbl user = userOptional.get();
				// Optional에서 실제 사용자 객체 추출
                String dbUserName = user.getUserName();
				// DB에서 조회한 사용자 이름
                
                if (dbUserName != null && !dbUserName.trim().isEmpty()) {
					// DB에서 조회한 이름이 유효한 경우
                    userName = dbUserName;
					// 조회한 이름을 사용자 이름으로 설정
                    log.debug("User name extracted from DB - Email: {}, Name: {}", userEmail, userName);
					// 성공 로그 기록
                } else {
					// DB에 이름이 없거나 빈 문자열인 경우
                    log.debug("Failed to extract user name from DB - Email: {}", userEmail);
					// 디버그 로그 기록
                } // if(DB 이름 유효성) 끝
            } else {
				// 사용자가 존재하지 않는 경우
                log.warn("User not found in DB - Email: {}, Using default", userEmail);
				// 경고 로그 기록
            } // if(사용자 존재) 끝
        } else {
			// 이메일이 null이거나 빈 문자열인 경우
            log.warn("No valid email found in JWT, using default value");
			// 경고 로그 기록
        } // if(이메일 유효성) 끝

	// 예외 발생 시 기본값 유지
    } catch (Exception e) {
        log.error("Error occurred while extracting user name from DB: {}", e.getMessage());
		// 오류 로그 기록
    } // try-catch 끝
    
    return userName;
	// 최종 사용자 이름 반환 (DB 조회 성공 시 실제 이름, 실패 시 기본값)
} // extractUserNameFromJWT 끝

    // ========== 인증 코드 유효성 검증 ==========
    private boolean isInvalidAuthCode(String authCode) {
		// 입력된 인증 코드의 유효성을 검증하는 메서드
        if (authCode == null || authCode.trim().isEmpty()) {
			// null 또는 빈 문자열인 경우
            log.warn("Auth code is null or empty.");
            // 경고 로그 기록
            return true;
			// 유효하지 않음
        } // if 끝
        
        String trimmedCode = authCode.trim().toUpperCase();
		// 공백 제거 및 대문자 변환
        
        if (trimmedCode.length() != AUTH_CODE_LENGTH) {
			// 길이가 올바르지 않은 경우
            log.warn("Auth code length is invalid: {}", trimmedCode.length());
            // 경고 로그 기록
            return true;
			// 유효하지 않음
        } // if 끝
        
        for (char c : trimmedCode.toCharArray()) {
			// 각 문자가 허용된 문자 집합에 속하는지 확인
            if (AUTH_CODE_CHARACTERS.indexOf(c) == -1) {
				// 허용되지 않은 문자가 포함된 경우
                log.warn("Auth code contains invalid character: {}", c);
                // 경고 로그 기록
                return true;
				// 유효하지 않음
            } // if 끝
        } // for 끝
        
        return false;
		// 모든 검증을 통과한 경우 유효함
    } // isInvalidAuthCode 끝
    
    // ========== 인증 코드 만료 확인 ==========
    private boolean isAuthCodeExpired(LocalDateTime codeCreatedTime) {
		// 인증 코드가 만료되었는지 확인하는 메서드
        log.debug("Checking if auth code is expired. Created at: {}, Now: {}", 
            codeCreatedTime, LocalDateTime.now());
            // 디버그 로그 기록
        return LocalDateTime.now().isAfter(codeCreatedTime.plusMinutes(AUTH_CODE_EXPIRY_MINUTES));
		// 현재 시간이 코드 생성 시간 + 유효 기간을 초과했는지 확인
    } // isAuthCodeExpired 끝
    
    // ========== Redis에 인증 데이터 저장 ==========
    private void saveAuthDataToRedis(String sessionId, String authCode, String userEmail, 
                                    String clientIp, LocalDateTime codeCreatedTime) {
		// Redis에 인증 데이터를 저장하는 메서드
        String sessionKey = AUTH_SESSION_PREFIX + userEmail;
		// Redis 키 생성 (접두사 + 사용자 이메일)
        
        redisTemplate.opsForHash().putAll(sessionKey, Map.of(
			// 인증 데이터 맵 생성 및 저장
            "authCode", authCode,
			// 인증 코드
            "userEmail", userEmail,
			// 사용자 이메일
            "clientIp", clientIp,
			// 클라이언트 IP
            "codeCreatedTime", codeCreatedTime.toString(),
			// 코드 생성 시간 (문자열로 저장)
            "sessionId", sessionId
			// 세션 ID
        )); // Redis에 해시맵 형태로 저장
        
        long ttlSeconds = AUTH_CODE_EXPIRY_MINUTES * 60;
		// TTL(만료 시간)을 초 단위로 계산, 분 단위에서 초 단위로 변환
        redisTemplate.expire(sessionKey, ttlSeconds, TimeUnit.SECONDS);
		// Redis 키에 TTL 설정 (초 단위)

        log.debug("Successfully saved auth data to Redis - User: {}, TTL: {} seconds", userEmail, ttlSeconds);
		// 저장 완료 디버그 로그 기록
    } // saveAuthDataToRedis 끝
    
    // ========== Redis에서 인증 데이터 조회 ==========
    private AuthCodeData getAuthDataFromRedis(String userEmail) {
		// Redis에서 인증 데이터를 조회하는 메서드

		// try-catch 블록으로 예외 처리
        try {
            String sessionKey = AUTH_SESSION_PREFIX + userEmail;
			// Redis 키 생성 (접두사 + 사용자 이메일)
            Map<Object, Object> authDataMap = redisTemplate.opsForHash().entries(sessionKey);
			// Redis에서 해시맵 형태로 인증 데이터 조회
            
            if (authDataMap.isEmpty()) {
				// 데이터가 없는 경우
                log.warn("No auth data found in Redis for user: {}", userEmail);
                // 경고 로그 기록
                return null;
				// null 반환
            } // if 끝
            
            String authCode = (String) authDataMap.get("authCode");
			// 저장된 인증 코드
            String email = (String) authDataMap.get("userEmail");
			// 저장된 사용자 이메일
            String clientIp = (String) authDataMap.get("clientIp");
			// 저장된 클라이언트 IP
            String createdTimeStr = (String) authDataMap.get("codeCreatedTime");
			// 저장된 코드 생성 시간 (문자열)
            
            if (authCode == null || email == null || clientIp == null || createdTimeStr == null) {
				// 필수 데이터가 누락된 경우
				// 인증코드, 이메일, IP, 생성시간 중 하나라도 null이면 데이터 무결성 문제
                log.error("Incomplete auth data in Redis for user: {}", userEmail);
                // 오류 로그 기록
                return null;
				// null 반환
            } // if 끝
            
            LocalDateTime codeCreatedTime = LocalDateTime.parse(createdTimeStr);
			// 문자열로 저장된 생성 시간을 LocalDateTime 객체로 변환

            log.debug("Retrieved auth data from Redis for user: {}", userEmail);
            // 조회 완료 디버그 로그 기록

            return new AuthCodeData(authCode, email, clientIp, codeCreatedTime);
			// AuthCodeData 객체 생성 및 반환
        
		// 예외 발생 시
        } catch (Exception e) {
            log.error("Redis 인증 데이터 조회 실패 - 사용자: {}, 오류: {}", userEmail, e.getMessage());
			// 오류 로그 기록
            return null;
			// null 반환
        } // try-catch 끝
    } // getAuthDataFromRedis 끝
    
    // ========== Redis 인증 데이터 정리 ==========
    private void cleanupAuthData(String userEmail) {
		// Redis에서 인증 데이터를 삭제하는 메서드

		// try-catch 블록으로 예외 처리
        try {
            String sessionKey = AUTH_SESSION_PREFIX + userEmail;
			// Redis 키 생성 (접두사 + 사용자 이메일)
            redisTemplate.delete(sessionKey);
			// Redis에서 키 삭제
            log.debug("Completed cleanup of Redis auth data - User: {}", userEmail);
			// 정리 완료 디버그 로그 기록
		// 예외 발생 시
        } catch (Exception e) {
            log.warn("Failed to cleanup Redis auth data - User: {}, Error: {}", userEmail, e.getMessage());
			// 경고 로그 기록
        } // try-catch 끝
    } // cleanupAuthData 끝
    
    // ========== 인증 코드 이메일 내용 생성 ==========
    private String createAuthCodeEmailContent(String userName, String authCode) {
		// 이메일 본문 내용을 HTML 형식으로 생성하는 메서드
        StringBuilder content = new StringBuilder();
		// StringBuilder content : 이메일 내용 빌더 생성
		// new StringBuilder() : 문자열을 효율적으로 조합하기 위해 StringBuilder 사용
        
		// ========== HTML 이메일 템플릿 ==========
        content.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        content.append("<h2 style='color: #2c5aa0;'>BlueCrab 이메일 인증</h2>");
        content.append("<p>안녕하세요, <strong>").append(userName).append("</strong>님!</p>");
        content.append("<p>아래 인증 코드를 입력하여 이메일 인증을 완료해주세요.</p>");
        
        content.append("<div style='background-color: #f8f9fa; border: 2px solid #2c5aa0; border-radius: 8px; ");
        content.append("padding: 20px; text-align: center; margin: 20px 0;'>");
        content.append("<h1 style='font-size: 36px; letter-spacing: 8px; margin: 0; color: #2c5aa0;'>");
        content.append(authCode);
        content.append("</h1>");
        content.append("</div>");
        
        content.append("<div style='background-color: #fff3cd; border: 1px solid #ffeaa7; border-radius: 4px; padding: 15px; margin: 20px 0;'>");
        content.append("<p style='margin: 0; font-weight: bold; color: #856404;'>보안 안내</p>");
        content.append("<ul style='margin: 10px 0; color: #856404;'>");
        content.append("<li>인증 코드는 ").append(AUTH_CODE_EXPIRY_MINUTES).append("분간만 유효합니다.</li>");
        content.append("<li>인증을 요청한 기기에서만 사용할 수 있습니다.</li>");
        content.append("<li>타인과 공유하지 마세요.</li>");
        content.append("</ul>");
        content.append("</div>");
        
        content.append("<p style='color: #6c757d; font-size: 14px;'>본 메일은 자동 발송되었습니다.</p>");
        content.append("</div>");
		
		// ========== HTML 이메일 템플릿 끝 ==========        
        
		return content.toString();
		// 완성된 이메일 내용 문자열 반환

    } // createAuthCodeEmailContent 끝
    
    // ========== 인증 코드 데이터 클래스 ==========
    private static class AuthCodeData {
		// Redis에 저장된 인증 코드 데이터 구조를 나타내는 내부 클래스
        private final String authCode;
		// 인증 코드
        // private final String userEmail;
		// 이메일은 현재 사용되지 않음
        private final String clientIp;
		// 클라이언트 IP 주소
        private final LocalDateTime codeCreatedTime;
		// 코드 생성 시간
        
        public AuthCodeData(String authCode, String userEmail, String clientIp, LocalDateTime codeCreatedTime) {
			// AuthCodeData() : 생성자
			// 인증 코드, 이메일, 클라이언트 IP, 생성 시간을 매개변수로 받음
			// 이메일은 JWT에서 이미 확인되었으므로 별도로 저장하지 않음 
            this.authCode = authCode;
			// 인증 코드 초기화
            // this.userEmail = userEmail;
			// 이메일 초기화 (현재 사용되지 않음)
            this.clientIp = clientIp;
			// 클라이언트 IP 초기화
            this.codeCreatedTime = codeCreatedTime;
			// 코드 생성 시간 초기화
        } // 생성자 끝
        
        public String getAuthCode() { return authCode; }
		// 인증 코드 Getter
        // public String getUserEmail() { return userEmail; }
		// 이메일 Getter (현재 사용되지 않음)
        public String getClientIp() { return clientIp; }
		// 클라이언트 IP Getter
        public LocalDateTime getCodeCreatedTime() { return codeCreatedTime; }
		// 코드 생성 시간 Getter
    } // AuthCodeData 끝
} // MailAuthCheckController 끝