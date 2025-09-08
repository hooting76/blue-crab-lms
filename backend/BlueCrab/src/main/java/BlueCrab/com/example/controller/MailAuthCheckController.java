/*
 * Spring MVC의 RESTful API 컨트롤러.
 * "/sendMail" : 메일 인증 코드를 생성하고 유저에게 메일 발송.
 * "/checkAuth" : 유저가 메일에서 전달 받은 인증 링크를 확인.
 * JWT 토큰 기반 인증, Redis를 활용한 분산 환경 지원
 */
package BlueCrab.com.example.controller;

// ========== Spring Security & JWT 관련 ==========
import org.springframework.security.oauth2.jwt.Jwt;	
// JWT 토큰 객체를 다루기 위한 클래스, JWT의 클레임(사용자정보)의 추출에 사용
import org.springframework.security.core.Authentication;	
// 현제 로그인 한 유저의 인증 정보를 담는 인터페이스, 유저 이메일, 권한 등을 확인 하는대 사용.
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// 비밀번호의 암호화/복호화를 위한 클래스, 인증 토큰을 해싱 하여 보안성을 강화한다.

// ========== Spring Web MVC 관련 ==========
import org.springframework.web.bind.annotation.RestController;
// 이 클래스가 REST API 컨트롤러 임을 표시, JSON 응답을 자동생성.
import org.springframework.web.bind.annotation.GetMapping;
// GET HTTP 요청을 위한 어노테이션, 여기서는 "/sendMail", "/checkAuth" 엔드포인트 생성.
import org.springframework.web.bind.annotation.RequestParam;
// URL 파라미터(?encryptedPW=xxx&sessionID=yyy) 를 매개변수로 받기 위한 어노테이션.
import org.springframework.http.ResponseEntity;
// HTTP 상태코드를 포함해 응답 전체를 제어, 500 Internal Server Error, 400 bad request, 404 not founr, 200 ok 등.
import org.springframework.http.HttpStatus;
// HTTP 상태코드 상수들(OK, UNAUTHORIZED, FORBIDDEN 등), ResponseEntity 와 함께 사용.
// 이로인해 상태코드를 직관적으로 작성 가능.

// ========== Redis 데이터베이스 관련 ==========
import org.springframework.data.redis.core.RedisTemplate;
// Redis DB 조작을 위한 중추 클래스, Hash, String, Set 등 다양한 데이터 타입을 저장/조회 가능.
import java.util.concurrent.TimeUnit;
// 시간 단위 설정(SECONDS, MINUTES, HOURS 등), Redis 만료시간(TTL) 설정에 사용.

// ========== Spring DI(의존성 주입) 관련 ==========
import org.springframework.beans.factory.annotation.Autowired;
// Spring이 자동으로 객체를 주입해주도록 하는 어노테이션, 즉, new 키워드 없이 객체 사용 가능.
import org.springframework.beans.factory.annotation.Value;
// application.properties 파일의 설정값을 주입, ${app.domain}과 같은 외부 설정을 가져올 때 사용.

// ========== HTTP & 서블릿 관련 ==========
import javax.servlet.http.HttpServletRequest;
// 클라이언트의 HTTP 요청 정보를 담는 객체, IP 주소, 헤더 정보 등을 추출하는데 사용된다.

// ========== Java 유틸리티 & 시간 관련 ==========
import java.time.LocalDateTime;
// 날짜와 시간을 취급하는 클래스, 인증토큰을 생성한 시간을 기록하고, 만료시간을 계산하느대 쓴다.
import java.util.UUID;
// 범용 고유 식별자 생성 클래스, 인증 토큰과 세션 ID를 생성하는데 사용.
import java.util.Map;
// 키와 값의 쌍을 다루는 인터페이스, Redis에 인증데이터를 저장할 때 사용.

// ========== 로깅 관련 ==========
import lombok.extern.slf4j.Slf4j;
// Lombok의 로깅 어노테이션, log 객체를 자동으로 생성.

// ========== 프로젝트 내부 클래스들 ==========
import BlueCrab.com.example.dto.AuthResponse;
// 인증 응답을 위한 DTO, JSON 형태로 클라이언트에게 응답 메시지 전달
import BlueCrab.com.example.service.EmailService;
// 이메일 발송 기능을 담당하는 서비스 클래스, SMTP를 통한 실제 메일 발송 처리
import BlueCrab.com.example.util.RequestUtils;
// HTTP 요청 관련 유틸리티 클래스, 클라이언트 IP 주소 추출 등의 기능 제공


// ========== 이메일 인증 처리 컨트롤러 ==========
@RestController
// 이 클래스가 REST API 컨트롤러임을 표시. JSON 응답을 자동으로 생성.
@Slf4j
// Lombok의 로깅 어노테이션

// 이메일 인증 처리 컨트롤러 클래스
public class MailAuthCheckController {
	
	@Autowired
	private EmailService emailService;
	// 이메일 발송 서비스 객체, SMTP를 통한 실제 메일 발송 처리 담당
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	// 비밀번호 암호화 객체, 널리 쓰이는 암호화 알고리즘의 하나.
	
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	// Redis DB를 조작하는 객체, 인증 토큰과 세션 데이터를 저장/조회/삭제 하는대 사용.
	
	@Value("${app.domain}")
	private String domain;
	// application.properties 에서 지정한 도메인 값을 주입, 이메일 내 인증 링크 생성에 사용.
	
	private static final int AUTH_TOKEN_EXPIRY_MINUTES = 10;
	// 인증 토큰의 만료 시간(분 단위), 10분으로 설정.
	private static final String AUTH_SESSION_PREFIX = "email_auth:session:";
	// Redis 세션 키의 접두사, 모든 인증 세션 키는 이 접두사로 시작.

	
	@GetMapping("/sendMail")
	// @GetMapping : GET HTTP 요청을 위한 어노테이션, "/sendMail" 엔드포인트 생성.
	// 이메일 인증 코드를 생성하고 유저에게 메일 발송
	public ResponseEntity<AuthResponse> sendMail(HttpServletRequest request, Authentication authentication) {
		// sendMail 메서드 : 이메일 인증 요청을 처리
		// HttpServletRequest request : 클라이언트의 HTTP 요청을 담는 객체 request
		// Authentication authentication : 현재 로그인한 유저의 인증 정보를 담는 객체 authentication
		String userEmail = authentication.getName();
		// 유저의 이메일 주소를 추출, JWT 토큰의 "sub" 클레임에 해당
		String clientIp = RequestUtils.getClientIpAddress(request);
		// 클라이언트의 IP 주소를 추출, 보안 검증에 사용
		log.info("이메일 인증 요청 from: {}, by: {}", clientIp, userEmail);
		// 동작 확인용 로그
		// 이메일 인증 요청 로그 기록, IP 주소와 이메일 포함
		
		// try-catch 블록, 메일 발송 및 Redis 저장 과정에서 발생할 수 있는 예외 처리
		try {
			String userName = extractUserNameFromJWT(authentication);
			// JWT 토큰 에서 사용자 이름을 추출, 이메일 내용에 사용 한다.
			String authSessionId = generateAuthSessionId(userEmail);
			// 인증 세션 ID를 생성, 이메일 링크와 Redis 키에 사용 한다.
			String authToken = generateAuthToken();
			// 인증 토큰을 생성, 이메일 링크와 Redis에 저장할 해시값에 사용 한다.
			// UUID를 사용해 고유한 토큰 생성, 보안성을 위해 해싱 처리.
			LocalDateTime tokenCreatedTime = LocalDateTime.now();
			// 토큰의 생성 시간을 기록, 만료 시간 계산에 사용한다.
			
			saveAuthDataToRedis(authSessionId, authToken, userEmail, clientIp, tokenCreatedTime);
			// 인증 데이터를 Redis에 저장, 세션 ID를 키로 사용한다.
			
			String encryptedToken = bCryptPasswordEncoder.encode(authToken);
			// String encryptedToken : 인증토큰을 해싱 처리, 이메일 링크에 사용.
			// bCryptPasswordEncoder.encode() : BCrypt 알고리즘을 이용해 토큰을 해싱.
			String emailContent = createEmailContent(userName, encryptedToken, authSessionId);
			// String emailContent : 이메일 본문 내용을 생성한다.
			// createEmailContent() : HTML 형식의 이메일 본문을 생성하는 메서드, (사용자 이름, 해싱된 토큰, 세션 ID)

			emailService.sendMIMEMessage("bluecrabacademy@gmail.com", userEmail, "BlueCrab 이메일 인증", emailContent);
			// emailService.sendMIMEMessage() : 이메일 발송 메서드 호출
			// (메일 발신자, 메일 수신자, 메일 제목, 메일 본문 내용)
			log.info("이메일 인증 발송 완료 - Email: {}, SessionId: {}", userEmail, authSessionId);
			// 이메일 발송 성공 경우의 로그 기록

			return ResponseEntity.ok(new AuthResponse("인증메일이 발송되었습니다. 10분 이내에 인증을 완료해주세요."));
			// ResponseEntity.ok() : HTTP 200 OK 상태코드와 함께 응답
			// AuthResponse : 인증 응답 DTO, JSON 형태로 클라이언트에게 응답 메시지 전달

		} catch (Exception e) {
			// 예외 처리 블록, 메일 발송 또는 Redis 저장 중 오류 발생 시의 처리
			log.error("메일 발송 실패 - Email: {}, IP: {}, Error: {}", userEmail, clientIp, e.getMessage());
			// 인증 메일 "발송" 실패시의 로그 처리, 이메일, IP, 오류 메시지 포함

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new AuthResponse("메일 발송 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
				// ResponseEntity.status() : HTTP 500 Internal Server Error 상태코드와 함께 응답
				// AuthResponse() : 인증 응답 DTO, JSON 형태로 클라이언트에게 오류 메시지 전달 
		} // try-catch 끝
	} // sendMail 메서드 끝
	
	@GetMapping("/checkAuth")
	// @GetMapping : GET HTTP 요청을 위한 어노테이션, "/checkAuth" 엔드포인트 생성.
	// 유저가 메일에서 전달 받은 인증 링크를 확인
	public ResponseEntity<AuthResponse> checkAuth(
			@RequestParam("encryptedPW") String encryptedPW, 
			@RequestParam("sessionId") String authSessionId,
			HttpServletRequest request) {
		// checkAuth 메서드 : 이메일 인증 확인 요청을 처리
		// @RequestParam : URL 파라미터를 매개변수로 받기 위한 어노테이션
		// String encryptedPW : 이메일 링크에서 전달된 해싱된 인증 토큰
		// String authSessionId : 이메일 링크에서 전달된 인증 세션 ID
		// HttpServletRequest request : 클라이언트의 HTTP 요청을 담는 객체 request

		String clientIp = RequestUtils.getClientIpAddress(request);
		// String clientIp : 클라이언트의 IP 주소를 추출, 보안 검증에 사용 한다.
		// RequestUtils.getClientIpAddress() : 유틸리티 메서드로 IP 주소 추출
		// IP 주소는 인증 요청의 출처를 확인하는데 사용된다.
		
		log.info("인증 확인 요청 from: {}, sessionId: {}", clientIp, authSessionId);
		// 인증 확인 요청 로그 기록, IP 주소와 세션 ID 포함

		// try-catch 블록, 인증 확인 과정에서 발생할 수 있는 예외 처리
		// 인증 토큰 검증, 세션 유효성 검사, IP 주소 확인 등을 수행
		try {
			// 입력 파라미터 검증
			if (isInvalidParameter(encryptedPW) || isInvalidParameter(authSessionId)) {
				// isInvalidParameter() : 파라미터가 null 이거나 빈 문자열인지 검사하는 메서드, 
				// encryptedPW 또는 authSessionId 가 유효하지 않으면
				return ResponseEntity.badRequest()
				// ResponseEntity.badRequest() : HTTP 400 Bad Request 상태코드와 함께 응답
						.body(new AuthResponse("잘못된 인증 요청입니다."));
						// AuthResponse() : 인증 응답 DTO, JSON 형태로 클라이언트에게 오류 메시지 전달
			} // 입력 파라미터 검증 끝
			
			AuthData authData = getAuthDataFromRedis(authSessionId);
			// AuthData authData : Redis에서 인증 데이터를 조회하는 메서드 호출
			// get AuthDataFromRedis() :Redis 에서 세션 ID로 인증 데이터를 조회하는 메서드
			// authData : 인증 토큰, 유저 이메일, 클라이언트 IP, 토큰 생성 시간 포함  

			// Redis에서 인증 데이터 조회
			if (authData == null) {
				// authData 가 null 이면, 즉 세션이 없거나 만료된 경우,
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new AuthResponse("인증 세션이 만료되었거나 존재하지 않습니다. 다시 인증을 요청해주세요."));
				// ResponseEntity.status() : HTTP 401 unauthorized 상태코드와 함께 응답
				// AuthResponse() : 인증 응답 DTO, JSON 형태로 클라이언트에게 오류 메시지 전달
			} // Redis에서 인증 데이터 조회 끝
			
			// 토큰 만료 시간, IP 주소 일치 여부 검사
			if (isTokenExpired(authData.getTokenCreatedTime())) {
				// isTokenExpired() :  토큰이 만료 되었는지 검사하는 메서드
				// authData.getTokenCreatedTime() : 토큰이 생성된 시간을 가져온다.
				// 토큰이 만료 되었으면,
				cleanupAuthTokens(authSessionId);
				// cleanupAuthTokens() : Redis에서 인증 데이터를 삭제하는 메서드 호출
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new AuthResponse("인증 토큰이 만료되었습니다. 다시 인증을 요청해주세요."));
					// ResponseEntity.status() : HTTP 401 unauthorized 상태코드와 함께 응답
					// AuthResponse() : 인증응답 DTO, JSON 형태로 클라이언트에게 오류 메시지 전달
			} // 토큰 만료 시간, IP 주소 일치 여부 검사 끝 
			
			// IP 주소 불일치 검사
			if (!authData.getClientIp().equals(clientIp)) {
				// authData.getClientIp() : Redis에 저장된 클라이언트 IP 주소를 가져온다.
				// clientIp : 현재 요청의 클라이언트 IP 주소
				// 즉, IP 주소가 일치하지 않으면,
				cleanupAuthTokens(authSessionId);
				// cleanupAuthTokens() : Redis 에서 인증 데이터를 삭제하는 메서드 호출
				log.warn("IP 주소 불일치 - 요청IP: {}, 저장된IP: {}", clientIp, authData.getClientIp());
				// IP 주소 불일치 로그 기록
				// 요청 IP 주소와 저장된 IP 주소를 함께 기록
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(new AuthResponse("보안상 이유로 인증이 거부되었습니다. 인증을 요청한 기기에서만 가능합니다."));
					// ResponseEntity.status() : HTTP 403 Forbidden 상태코드와 함께 응답
					// AuthResponse() : 인증 응답 DTO, JSON 형태로 클라이언트에게 오류 메시지 전달
			} // IP 주소 불일치 검사 끝
			
			// 인증 토큰 일치 여부 검사
			if (bCryptPasswordEncoder.matches(authData.getAuthToken(), encryptedPW)) {
				// bCryptPasswordEncoder.matches() : 해싱된 토큰과 Redis에 저장된 토큰이 일치하는지 검사
				// authData.getAuthToken() : Redis에 저장된 원본 인증 토큰 가져오기
				// encryptedPW : 이메일 링크에서 전달된 해싱된 인증 토큰
				// 즉, 해싱된 토큰과 Redis에 저장된 토큰이 일치하면,
				cleanupAuthTokens(authSessionId);
				// cleanupAuthTokens() : Redis에서 인증 데이터를 삭제하는 메서드 호출
				// (인증이 완료되었으므로 더 이상 필요하지 않다.)
				log.info("이메일 인증 성공 - Email: {}, IP: {}", authData.getUserEmail(), clientIp);
				// 이메일 인증 성공 로그 기록, 유저 이메일과 IP 주소 포함
				return ResponseEntity.ok(new AuthResponse("이메일 인증이 성공적으로 완료되었습니다."));
				// ResponseEntity.ok() : HTTP 200 OK 상태코드와 함께 응답
				// AuthResponse() : 인증 응답 DTO, JSON 형태로 클라이언트에게 성공 메시지 전달
			} else {
				// 그 외의 경우, 즉 토큰이 일치하지 않으면,
				cleanupAuthTokens(authSessionId);
				// cleanupAuthTokens() : Redis에서 인증 데이터를 삭제하는 메서드 호출
				// (보안상 이유로 인증 실패시에도 토큰을 삭제한다.)
				log.warn("인증 토큰 불일치 - Email: {}, IP: {}", authData.getUserEmail(), clientIp);
				// 인증 토큰 불일치 로그 기록, 유저 이메일과 IP 주소 포함
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new AuthResponse("인증에 실패했습니다. 올바른 인증 링크를 사용해주세요."));
					// ResponseEntity.status() : HTTP 401 Unauthorized 상태코드와 함께 응답
					// AuthResponse() : 인증 응답 DTO, JSON 형태로 클라이언트에게 오류 메시지 전달
			} // 인증 토큰 일치 여부 검사 끝

		// 예외 처리
		} catch (Exception e) {
			// 인증 확인 처리 중에 예외가 발생한 경우의 처리
			log.error("인증 확인 중 오류 발생 - SessionId: {}, IP: {}, Error: {}", authSessionId, clientIp, e.getMessage());
			// 인증 확인 중 오류 발생 로그 기록, 세션 ID, IP 주소, 오류 메시지 포함
			cleanupAuthTokens(authSessionId);
			// cleanupAuthTokens() : Redis에서 인증 데이터를 삭제하는 메서드 호출
			// (오류가 발생했을 때도 토큰을 삭제하여 보안성을 유지한다.)
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new AuthResponse("인증 처리 중 오류가 발생했습니다. 다시 시도해주세요."));
				// ResponseEntity.status() : HTTP 500 Internal Server Error 상태코드와 함께 응답
				// AuthResponse() : 인증 응답 DTO, JSON 형태로 클라이언트에게 오류 메시지 전달
		} // try-catch 끝
	} // checkAuth 메서드 끝

	// ========== 유틸리티 메서드들 ==========
	private String extractUserNameFromJWT(Authentication authentication) {
		// String extractUserNameFromJWT() : JWT 토큰에서 사용자 이름을 추출하는 메서드
		// Authentication authentication : 현제 로그인 한 유저의 인증 정보를 담는 객체 authentication

		String userName = "학생";
		// String userName: JWT 에서 추출한 사용자 이름
		// "학생" : 기본값, JWT 에서 이름을 추출하지 못 할 경우의 에러 방지용

		// try-catch 블록, JWT 토큰에서 사용자 이름을 추출하는 과정에서 발생할 수 있는 예외 처리
		try {
			if (authentication.getPrincipal() instanceof Jwt) {
				// authentication.getPrincipal() : 인증된 사용자의 주체(Principal) 정보를 가져온다.
				// instanceof Jwt : 주체 정보가 Jwt 타입인지 확인
				// 즉, 주체 정보가 JWT 토큰이면,

				Jwt jwt = (Jwt) authentication.getPrincipal();
				// Jwt jwt : 주체 정보를 Jwt 객체로 캐스팅
				// (Jwt) : 명시적 타입 캐스팅
				// authentication.getPrincipal() : 인증된 사용자의 주체 정보를 다시 가져온다.

				String jwtName = jwt.getClaimAsString("name");
				// String jwtName : JWT 토큰에서 추출한 사용자 이름을 수용하는 변수
				// jwt.getClaimAsString("name") : JWT 토큰의 "name" 클레임에서 사용자 이름을 문자열로 추출
				// 해당 클레임이 존재하지 않으면 null 반환
				
				if (jwtName != null && !jwtName.trim().isEmpty()) {
					// jwtName 이 null 이 아니고, 빈 문자열이 아니면,
					userName = jwtName;
					// userName 에 추출한 이름을 저장
				}
			} // if (authentication.getPrincipal() instanceof Jwt) 끝
		} catch (Exception e) {
			// JWT에서 사용자 이름 추출 중에 예외가 발생한 경우의 처리
			log.debug("JWT에서 사용자 이름 추출 실패, 기본값 사용: {}", e.getMessage());
			// JWT에서 사용자 이름 추출 실패 로그 기록, 오류 메시지 포함
			// 기본값인 "학생" 사용
		} // try-catch 끝
		
		return userName;
		// 추출한 사용자 이름 반환
	} // extractUserNameFromJWT 메서드 끝
	
	// 인증 세션 ID 생성
	private String generateAuthSessionId(String userEmail) {
		// String generateAuthSessionId() : 인증 세션 ID를 생성하는 메서드
		// String userEmail : 유저의 이메일 주소, 세션 ID 생성에 사용
		return userEmail + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID().toString().substring(0, 8);
		// userEmail : 유저의 이메일 주소
		// System.currentTimeMillis() : 현제 시간을 밀리초 단위로 반환
		// UUID.randomUUID().toString().substring(0, 8) : UUID를 생성하고 문자열로 변환한 후, 앞의 8글자만 사용
		// 즉, 이메일, 현재 시간, 랜덤 문자열을 조합해 고유한 세션 ID 생성
		
	} // generateAuthSessionId 메서드 끝
	
	// 인증 토큰 생성
	private String generateAuthToken() {
		// String generateAuthToken() : 인증 토큰을 생성하는 메서드
		return UUID.randomUUID().toString().replace("-", "");
		// UUID.randomUUID() : 범용 고유 식별자(UUID)를 생성
		// toString() : UUID를 문자열로 변환
		// replace("-", "") : 문자열에서 하이픈(-)을 제거
		// 즉, 하이픈이 제거된 고유한 인증 토큰 생성
	} // generateAuthToken 메서드 끝
	
	// 입력 파라미터 검증
	private boolean isInvalidParameter(String parameter) {
		// boolean isInvalidParameter() : 파라미터가 null 이거나 빈 문자열인지 검사하는 메서드
		// String parameter : 검사할 문자열 파라미터
		return parameter == null || parameter.trim().isEmpty();
		// parameter == null : 파라미터가 null 인지 검사
		// parameter.trim().isEmpty() : 파라미터가 빈 문자열인지 검사
	} // isInvalidParameter 메서드 끝
	
	// 토큰 만료 시간 검사
	private boolean isTokenExpired(LocalDateTime tokenCreatedTime) {
		// boolean isTokenExpired() : 토큰이 만료 되었는지 검사하는 메서드
		// LocalDateTime tokenCreatedTime : 토큰이 생성된 시간
		return LocalDateTime.now().isAfter(tokenCreatedTime.plusMinutes(AUTH_TOKEN_EXPIRY_MINUTES));
		// LocalDateTime.now() : 현제 시간을 가져온다.
		// isAfter() : 현제 시간이 토큰 만료 시간 이후인지 검사
		// tokenCreatedTime.plusMinutes(AUTH_TOKEN_EXPIRY_MINUTES) : 토큰 생성 시간에 만료 시간을 더한다.
		// 즉, 현제 시간이 토큰 생성 시간 + 만료 시간 이후이면 true 반환
	} // isTokenExpired 메서드 끝
	
	// Redis에 인증 데이터 저장
	private void saveAuthDataToRedis(String authSessionId, String authToken, String userEmail, 
									String clientIp, LocalDateTime tokenCreatedTime) {
		// void saveAuthDataToRedis() : 인증 데이터를 Redis에 저장하는 메서드
		// String authSessionId : 인증 세션 ID, Redis 키에 사용
		// String authToken : 인증 토큰, Redis 해시값에 사용
		// String userEmail : 유저 이메일, Redis 해시값에 사용
		// String clientIp : 클라이언트 IP 주소, Redis 해시값에 사용
		// LocalDateTime tokenCreatedTime : 토큰 생성 시간, Redis 해시값에 사용

		String sessionKey = AUTH_SESSION_PREFIX + authSessionId;
		// String sessionKey : Redis에 저장할 키, 접두사 + 세션 ID 조합
		// AUTH_SESSION_PREFIX : "email_auth:session:"
		// 즉, 모든 인증 세션 키는 "email_auth:session:"로 시작
		
		redisTemplate.opsForHash().putAll(sessionKey, Map.of(
			// redisTemplate.opsForHash() : Redis의 해시(Hash) 자료구조를 조작하는 메서드 체인
			// putAll() : 여러 개의 키-값 쌍을 한 번에 저장하는 메서드
			// sessionKey : Redis에 저장할 키
			"authToken", authToken,
			"userEmail", userEmail,
			"clientIp", clientIp,
			"tokenCreatedTime", tokenCreatedTime.toString()
			// Map.of() : 키-값 쌍을 간편하게 생성하는 메서드
			// "authToken" : 인증 토큰
			// "userEmail" : 유저 이메일
			// "clientIp" : 클라이언트 IP 주소
			// "tokenCreatedTime" : 토큰 생성 시간 (문자열로 저장)
		));
		
		long ttlSeconds = AUTH_TOKEN_EXPIRY_MINUTES * 60;
		// long ttlSeconds : 토큰의 만료 시간(초 단위)
		// AUTH_TOKEN_EXPIRY_MINUTES * 60 : 분 단위의 만료 시간을 초 단위로 변환하기 위해 60을 곱함
		redisTemplate.expire(sessionKey, ttlSeconds, TimeUnit.SECONDS);
		// redisTemplate.expire() : Redis 키의 만료 시간을 설정하는 메서드
		// sessionKey : 만료 시간을 설정할 Redis 키
		// ttlSeconds : 만료 시간(초 단위)
		// TimeUnit.SECONDS : 시간 단위를 초(SECONDS)로 지정
		
		log.debug("Redis에 인증 데이터 저장 완료 - SessionId: {}, TTL: {}초", authSessionId, ttlSeconds);
		// Redis에 인증 데이터 저장 완료 로그 기록, 세션 ID와 만료 시간(초) 포함
	} // saveAuthDataToRedis 메서드 끝
	
	// Redis에서 인증 데이터 조회
	private AuthData getAuthDataFromRedis(String authSessionId) {
		// AuthData getAuthDataFromRedis() : Redis 에서 세션 ID로 인증 데이터를 조회하는 메서드
		// String authSessionId : 인증 세션 ID, Redis 키에 사용

		// try-catch 블록, Redis 조회 과정에서 발생할 수 있는 예외 처리
		try {
			String sessionKey = AUTH_SESSION_PREFIX + authSessionId;
			// String sessionKey : Redis에 저장된 키, 접두사 + 세션 ID 조합
			// AUTH_SESSION_PREFIX : "email_auth:session:"
			// 즉, 모든 인증 세션 키는 "email_auth:session:"로 시작
			Map<Object, Object> authDataMap = redisTemplate.opsForHash().entries(sessionKey);
			// Map<Object, Object> authDataMap : Redis에서 조회한 인증 데이터 맵
			// redisTemplate.opsForHash().entries(sessionKey) : Redis 해시에서 모든 키-값 쌍을 조회하는 메서드
			// sessionKey : 조회할 Redis 키

			if (authDataMap.isEmpty()) {
				// 조회한 맵이 비어 있으면, 즉 세션이 없거나 만료된 경우,
				return null;
				// null 반환
			}
			
			String authToken = (String) authDataMap.get("authToken");
			// String authToken : Redis에서 조회한 인증 토큰
			// (String) : 명시적 타입 캐스팅
			String userEmail = (String) authDataMap.get("userEmail");
			// String userEmail : Redis에서 조회한 유저 이메일
			// (String) : 명시적 타입 캐스팅
			String clientIp = (String) authDataMap.get("clientIp");
			// String clientIp : Redis에서 조회한 클라이언트 IP 주소
			// (String) : 명시적 타입 캐스팅
			String createdTimeStr = (String) authDataMap.get("tokenCreatedTime");
			// String createdTimeStr : Redis에서 조회한 토큰 생성 시간(문자열)
			// (String) : 명시적 타입 캐스팅
			
			if (authToken == null || userEmail == null || clientIp == null || createdTimeStr == null) {
				// 조회한 값들 중 하나라도 null 이면,
				return null;
				// null 반환
			}
			
			LocalDateTime tokenCreatedTime = LocalDateTime.parse(createdTimeStr);
			// LocalDateTime tokenCreatedTime : 문자열로 저장된 토큰 생성 시간을 LocalDateTime 객체로 변환
			// LocalDateTime.parse(createdTimeStr) : 문자열을 LocalDateTime 객체로 파싱
						
			return new AuthData(authToken, userEmail, clientIp, tokenCreatedTime);
			// return new AuthData() : AuthData 객체 생성 및 반환
			// AuthData : 내부 정적 클래스, "인증 토큰, 유저 이메일, 클라이언트 IP, 토큰 생성 시간"을 포함

		} catch (Exception e) {
			// Redis에서 인증 데이터 조회 중에 예외가 발생한 경우의 처리
			log.error("Redis 인증 데이터 조회 실패 - SessionId: {}, Error: {}", authSessionId, e.getMessage());
			// Redis 인증 데이터 조회 실패 로그 기록, 세션 ID와 오류 메시지 포함
			return null;
			// null 반환
		} // try-catch 끝
	} // getAuthDataFromRedis 메서드 끝
	
	// Redis에서 인증 데이터 삭제
	private void cleanupAuthTokens(String authSessionId) {
		// void cleanupAuthTokens() : Redis에서 인증 데이터를 삭제하는 메서드
		// String authSessionId : 인증 세션 ID
		
		// try-catch 블록, Redis 삭제 과정에서 발생할 수 있는 예외 처리
		try {
			String sessionKey = AUTH_SESSION_PREFIX + authSessionId;
			// String sessionKey : Redis에 저장된 키, 접두사 + 세션 ID 조합
			redisTemplate.delete(sessionKey);
			// redisTemplate.delete() : Redis 키를 삭제하는 메서드
			// sessionKey : 삭제할 Redis 키
			
			log.debug("Redis 인증 토큰 정리 완료 - SessionId: {}", authSessionId);
			// Redis 인증 토큰 정리 완료 로그 기록, 세션 ID 포함
		} catch (Exception e) {
			// Redis에서 인증 데이터 삭제 중에 예외가 발생한 경우의 처리
			log.warn("Redis 인증 토큰 정리 실패 - SessionId: {}, Error: {}", authSessionId, e.getMessage());
			// Redis 인증 토큰 정리 실패 로그 기록, 세션 ID와 오류 메시지 포함
		}
	} // cleanupAuthTokens 메서드 끝

	// 이메일 본문 내용 생성
	private String createEmailContent(String userName, String encryptedToken, String authSessionId) {
		// String createEmailContent() : 이메일 본문 내용을 생성하는 메서드
		// () : 사용자 이름, 해싱된 토큰, 세션 ID를 매개변수로 받음
		StringBuilder content = new StringBuilder();
		// StringBuilder content : 이메일 본문 내용을 동적으로 생성하기 위한 StringBuilder 객체

		// HTML 형식의 이메일 본문 생성
		// 사용자 이름 포함 인사말
		content.append("<div>");
		content.append("<h2>BlueCrab 이메일 인증</h2>");
		content.append("<p>안녕하세요, <strong>").append(userName).append("</strong>님!</p>");
		content.append("<p>BlueCrab 서비스의 이메일 인증을 진행해주세요.</p>");
		
		// 인증 안내 및 보안 주의사항
		content.append("<div>");
		content.append("<p><strong>보안 안내</strong></p>");
		content.append("<ul>");
		content.append("<li>본 인증 링크는 10분간만 유효합니다.</li>");
		content.append("<li>인증을 요청한 기기에서만 인증이 가능합니다.</li>");
		content.append("<li>타인과 공유하지 마세요.</li>");
		content.append("</ul>");
		content.append("</div>");
		
		// 인증 링크
		content.append("<p>");
		content.append("<a href='").append(domain).append("/BlueCrab-1.0.0/checkAuth?encryptedPW=")
			   .append(encryptedToken).append("&sessionId=").append(authSessionId).append("'>");
		content.append("이메일 인증하기</a>");
		content.append("</p>");
		
		// 마무리 인사말
		content.append("<p><small>본 메일은 자동 발송되었습니다.</small></p>");
		content.append("</div>");
		// HTML 형식의 이메일 본문 생성 끝
		
		return content.toString();
		// 생성된 이메일 본문 내용을 문자열로 반환
	}
	
	// ========== 내부 정적 클래스 : 인증 데이터 저장용 ==========
	// Redis에서 조회한 인증 데이터를 담는 용도의 내부 클래스
	private static class AuthData {
		// 인증 토큰, 유저 이메일, 클라이언트 IP, 토큰 생성 시간을 포함
		private final String authToken;
		private final String userEmail;
		private final String clientIp;
		private final LocalDateTime tokenCreatedTime;

		// AuthData 생성자
		public AuthData(String authToken, String userEmail, String clientIp, LocalDateTime tokenCreatedTime) {
			this.authToken = authToken;
			// this.authToken : 인증 토큰
			// authToken : 생성자 매개변수로 전달된 인증 토큰
			this.userEmail = userEmail;
			this.clientIp = clientIp;
			this.tokenCreatedTime = tokenCreatedTime;
		} // AuthData 생성자 끝
		
		// Getter 메서드들
		// private 필드에 접근하기 위한 메서드들
		public String getAuthToken() { return authToken; }
		// 인증 토큰 반환
		public String getUserEmail() { return userEmail; }
		// 유저 이메일 반환
		public String getClientIp() { return clientIp; }
		// 클라이언트 IP 반환
		public LocalDateTime getTokenCreatedTime() { return tokenCreatedTime; }
		// 토큰 생성 시간 반환
	} // AuthData 클래스 끝
} // MailAuthCheckController 클래스 끝