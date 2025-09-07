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
public class MailAuthCheckController {
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	
	@Value("${app.domain}")
	private String domain;
	
	private static final int AUTH_TOKEN_EXPIRY_MINUTES = 10;
	private static final String AUTH_SESSION_PREFIX = "email_auth:session:";
	
	@GetMapping("/sendMail")
	public ResponseEntity<AuthResponse> sendMail(HttpServletRequest request, Authentication authentication) {
		
		String userEmail = authentication.getName();
		String clientIp = RequestUtils.getClientIpAddress(request);
		
		log.info("이메일 인증 요청 from: {}, by: {}", clientIp, userEmail);
		
		try {
			String userName = extractUserNameFromJWT(authentication);
			String authSessionId = generateAuthSessionId(userEmail);
			String authToken = generateAuthToken();
			LocalDateTime tokenCreatedTime = LocalDateTime.now();
			
			saveAuthDataToRedis(authSessionId, authToken, userEmail, clientIp, tokenCreatedTime);
			
			String encryptedToken = bCryptPasswordEncoder.encode(authToken);
			String emailContent = createEmailContent(userName, encryptedToken, authSessionId);
			
			emailService.sendMIMEMessage("bluecrabacademy@gmail.com", userEmail, "BlueCrab 이메일 인증", emailContent);
			
			log.info("이메일 인증 발송 완료 - Email: {}, SessionId: {}", userEmail, authSessionId);
			
			return ResponseEntity.ok(new AuthResponse("인증메일이 발송되었습니다. 10분 이내에 인증을 완료해주세요."));
			
		} catch (Exception e) {
			log.error("메일 발송 실패 - Email: {}, IP: {}, Error: {}", userEmail, clientIp, e.getMessage());
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new AuthResponse("메일 발송 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
		}
	}
	
	@GetMapping("/checkAuth")
	public ResponseEntity<AuthResponse> checkAuth(
			@RequestParam("encryptedPW") String encryptedPW, 
			@RequestParam("sessionId") String authSessionId,
			HttpServletRequest request) {

		String clientIp = RequestUtils.getClientIpAddress(request);
		
		log.info("인증 확인 요청 from: {}, sessionId: {}", clientIp, authSessionId);

		try {
			if (isInvalidParameter(encryptedPW) || isInvalidParameter(authSessionId)) {
				return ResponseEntity.badRequest()
						.body(new AuthResponse("잘못된 인증 요청입니다."));
			}
			
			AuthData authData = getAuthDataFromRedis(authSessionId);
			
			if (authData == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new AuthResponse("인증 세션이 만료되었거나 존재하지 않습니다. 다시 인증을 요청해주세요."));
			}
			
			if (isTokenExpired(authData.getTokenCreatedTime())) {
				cleanupAuthTokens(authSessionId);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new AuthResponse("인증 토큰이 만료되었습니다. 다시 인증을 요청해주세요."));
			}
			
			if (!authData.getClientIp().equals(clientIp)) {
				cleanupAuthTokens(authSessionId);
				log.warn("IP 주소 불일치 - 요청IP: {}, 저장된IP: {}", clientIp, authData.getClientIp());
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(new AuthResponse("보안상 이유로 인증이 거부되었습니다. 인증을 요청한 기기에서만 가능합니다."));
			}
			
			if (bCryptPasswordEncoder.matches(authData.getAuthToken(), encryptedPW)) {
				cleanupAuthTokens(authSessionId);
				log.info("이메일 인증 성공 - Email: {}, IP: {}", authData.getUserEmail(), clientIp);
				return ResponseEntity.ok(new AuthResponse("이메일 인증이 성공적으로 완료되었습니다."));
			} else {
				cleanupAuthTokens(authSessionId);
				log.warn("인증 토큰 불일치 - Email: {}, IP: {}", authData.getUserEmail(), clientIp);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new AuthResponse("인증에 실패했습니다. 올바른 인증 링크를 사용해주세요."));
			}
			
		} catch (Exception e) {
			log.error("인증 확인 중 오류 발생 - SessionId: {}, IP: {}, Error: {}", authSessionId, clientIp, e.getMessage());
			cleanupAuthTokens(authSessionId);
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new AuthResponse("인증 처리 중 오류가 발생했습니다. 다시 시도해주세요."));
		}
	}

	private String extractUserNameFromJWT(Authentication authentication) {
		String userName = "학생";
		
		try {
			if (authentication.getPrincipal() instanceof Jwt) {
				Jwt jwt = (Jwt) authentication.getPrincipal();
				String jwtName = jwt.getClaimAsString("name");
				
				if (jwtName != null && !jwtName.trim().isEmpty()) {
					userName = jwtName;
				}
			}
		} catch (Exception e) {
			log.debug("JWT에서 사용자 이름 추출 실패, 기본값 사용: {}", e.getMessage());
		}
		
		return userName;
	}
	
	private String generateAuthSessionId(String userEmail) {
		return userEmail + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID().toString().substring(0, 8);
	}
	
	private String generateAuthToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	private boolean isInvalidParameter(String parameter) {
		return parameter == null || parameter.trim().isEmpty();
	}
	
	private boolean isTokenExpired(LocalDateTime tokenCreatedTime) {
		return LocalDateTime.now().isAfter(tokenCreatedTime.plusMinutes(AUTH_TOKEN_EXPIRY_MINUTES));
	}
	
	private void saveAuthDataToRedis(String authSessionId, String authToken, String userEmail, 
									String clientIp, LocalDateTime tokenCreatedTime) {
		String sessionKey = AUTH_SESSION_PREFIX + authSessionId;
		
		redisTemplate.opsForHash().putAll(sessionKey, Map.of(
			"authToken", authToken,
			"userEmail", userEmail,
			"clientIp", clientIp,
			"tokenCreatedTime", tokenCreatedTime.toString()
		));
		
		long ttlSeconds = AUTH_TOKEN_EXPIRY_MINUTES * 60;
		redisTemplate.expire(sessionKey, ttlSeconds, TimeUnit.SECONDS);
		
		log.debug("Redis에 인증 데이터 저장 완료 - SessionId: {}, TTL: {}초", authSessionId, ttlSeconds);
	}
	
	private AuthData getAuthDataFromRedis(String authSessionId) {
		try {
			String sessionKey = AUTH_SESSION_PREFIX + authSessionId;
			Map<Object, Object> authDataMap = redisTemplate.opsForHash().entries(sessionKey);
			
			if (authDataMap.isEmpty()) {
				return null;
			}
			
			String authToken = (String) authDataMap.get("authToken");
			String userEmail = (String) authDataMap.get("userEmail");
			String clientIp = (String) authDataMap.get("clientIp");
			String createdTimeStr = (String) authDataMap.get("tokenCreatedTime");
			
			if (authToken == null || userEmail == null || clientIp == null || createdTimeStr == null) {
				return null;
			}
			
			LocalDateTime tokenCreatedTime = LocalDateTime.parse(createdTimeStr);
			
			return new AuthData(authToken, userEmail, clientIp, tokenCreatedTime);
		} catch (Exception e) {
			log.error("Redis 인증 데이터 조회 실패 - SessionId: {}, Error: {}", authSessionId, e.getMessage());
			return null;
		}
	}
	
	private void cleanupAuthTokens(String authSessionId) {
		try {
			String sessionKey = AUTH_SESSION_PREFIX + authSessionId;
			redisTemplate.delete(sessionKey);
			
			log.debug("Redis 인증 토큰 정리 완료 - SessionId: {}", authSessionId);
		} catch (Exception e) {
			log.warn("Redis 인증 토큰 정리 실패 - SessionId: {}, Error: {}", authSessionId, e.getMessage());
		}
	}

	private String createEmailContent(String userName, String encryptedToken, String authSessionId) {
		StringBuilder content = new StringBuilder();
		content.append("<div>");
		content.append("<h2>BlueCrab 이메일 인증</h2>");
		content.append("<p>안녕하세요, <strong>").append(userName).append("</strong>님!</p>");
		content.append("<p>BlueCrab 서비스의 이메일 인증을 진행해주세요.</p>");
		
		content.append("<div>");
		content.append("<p><strong>보안 안내</strong></p>");
		content.append("<ul>");
		content.append("<li>본 인증 링크는 10분간만 유효합니다.</li>");
		content.append("<li>인증을 요청한 기기에서만 인증이 가능합니다.</li>");
		content.append("<li>타인과 공유하지 마세요.</li>");
		content.append("</ul>");
		content.append("</div>");
		
		content.append("<p>");
		content.append("<a href='").append(domain).append("/BlueCrab-1.0.0/checkAuth?encryptedPW=")
			   .append(encryptedToken).append("&sessionId=").append(authSessionId).append("'>");
		content.append("이메일 인증하기</a>");
		content.append("</p>");
		
		content.append("<p><small>본 메일은 자동 발송되었습니다.</small></p>");
		content.append("</div>");
		
		return content.toString();
	}
	
	private static class AuthData {
		private final String authToken;
		private final String userEmail;
		private final String clientIp;
		private final LocalDateTime tokenCreatedTime;
		
		public AuthData(String authToken, String userEmail, String clientIp, LocalDateTime tokenCreatedTime) {
			this.authToken = authToken;
			this.userEmail = userEmail;
			this.clientIp = clientIp;
			this.tokenCreatedTime = tokenCreatedTime;
		}
		
		public String getAuthToken() { return authToken; }
		public String getUserEmail() { return userEmail; }
		public String getClientIp() { return clientIp; }
		public LocalDateTime getTokenCreatedTime() { return tokenCreatedTime; }
	}
}