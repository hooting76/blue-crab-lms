/*
 * Spring MVC의 RESTful API 컨트롤러.
 * "/sendMail" : 메일 인증 코드를 생성하고 유저에게 메일 발송.
 * "/checkAuth" : 유저가 메일에서 전달 받은 인증 링크를 확인.
 * JWT 토큰 기반 인증, 로컬스토리지 이메일 활용
 */
package BlueCrab.com.example.controller;

import org.springframework.security.oauth2.jwt.Jwt;
/* JWT : Spring Security OAuth2에서 제공하는 JWT 토큰을 나타내는 클래스.
 * JWT(JSON Web Token)의 헤더, 페이로드, 서명 정보를 포함 하는 불변 객체.
 * getClaimAsString(), getClaimAsInstant() 등의 메서드로 클레임 정보 추출 가능.
 * 토큰의 유효성 검증은 Spring Security에서 자동으로 처리되므로 안전하게 사용 가능.
 * 예 : jwt.getClaimAsString("name"), jwt.getClaimAsString("email"), jwt.getSubject() 등
 */
import java.time.LocalDateTime;	// 날짜와 시간 API 핵심 클래스, 여기서는 인증 토큰의 생성 시간 기록과 만료 시간 계산에 사용
import java.util.UUID;
/* UUID : Universally Unique Identifier, 128비트 고유 식별자.
 * RFC 4122 표준을 따르고, 전 세계적으로 중복되지 않는 식별자 생성 보장.
 * 토큰 생성에 적합하고 예측 할 수 없는 고유성을 보장.
 */

import javax.servlet.http.HttpServletRequest;	// 클라이언트에서 서버로 보낸 HTTP 요청의 모든 정보를 캡슐화.
import javax.servlet.http.HttpSession;		
// HTTP 프로토콜의 stateless 특성을 보완하기 위한 세션 관리 인터페이스. 인증 토큰과 관련 보안 정보의 임시 저장소 기능.

import org.springframework.beans.factory.annotation.Autowired;
// Spring Framework의 핵심 DI 어노테이션. Spring IoC 컨테이너가 관리하는 Bean들 간의 의존성을 자동으로 주입.
import org.springframework.beans.factory.annotation.Value;
// Spring에서 외부의 설정값을 주입 받기 위한 어노테이션.
import org.springframework.http.HttpStatus;
/* HTTP 상태 코드를 나타내는 열거형 (enum) 클래스.
 * OK(200), CREATED(201), BAD_REQUEST(400), UNAUTHORIZED(401), FORBIDDEN(403), NOT_FOUND(404), INTERNAL_SERVER_ERROR(500) 등이 있다.
 * ResponseEntity와 함깨 사용되어 HTTP 응답의 상태 코드를 명확히 지정 하는대 사용.
 */
import org.springframework.http.ResponseEntity;	// Spring MVC에서 HTTP 응답 전체를 표현하는 제네릭 클래스.
import org.springframework.security.core.Authentication;	// 인증정보를 나타내는 인터페이스.
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;	// 암호화 알고리즘
import org.springframework.web.bind.annotation.GetMapping;	// HTTP GET 요청을 특정 핸들러 메서드에 매핑하는 어노테이션.
import org.springframework.web.bind.annotation.RequestParam;	// 
import org.springframework.web.bind.annotation.RestController;	// 본 클래스가 REST 컨트롤러임을 나타내는 어노테이션.

import BlueCrab.com.example.dto.AuthResponse;	// json형태로 응답하기 위한 DTO 임포트
import BlueCrab.com.example.service.EmailService;	// 이메일 발송 기능을 담당하는 서비스 클래스.
import BlueCrab.com.example.util.RequestUtils;	// 클라이언트 IP 추출 유틸리티 클래스.
import lombok.extern.slf4j.Slf4j;	// Lombok 제공의 로깅용 어노테이션.

@RestController
/* @RestController : 본 클래스가 Spring의 REST 컨트롤러임을 나타내는 어노테이션
 * 또한, @RestController=@Controller + @ResponseBody 이기도 함.
 * 본 클래스의 모든 메서드는 HTML 뷰를 반환하는 대신, HTTP 응답 본문에 직접 데이터를 씀.
 * (JSON, XML, 또는 일반 문자열 등)
 * 즉, 본 클래스의 메서드들은 주로 RESTful API 엔드포인트를 구현하는 데 사용됨.
 */
@Slf4j
/* @Slf4j : Lombok 에서 제공하는 어노테이션으로, 로깅용 어노테이션.
 * 개발자가 직접 logger 객체를 생성하지 않아도 log 객체를 사용할 수 있게 해줌.
 * log 객체는 다양한 로그 레벨(info, debug, error 등)을 지원.
 */
public class MailAuthCheckController {
/* MailAuthCheckController : 메일 인증을 처리하는 REST 컨트롤러 클래스.
 * "/sendMail"과 "/checkAuth" 두 개의 주요 엔드포인트를 제공.
 */
	
	@Autowired
	/* @Autowired : spring에게 의존성 주입의 자동처리 요청을 날리는 어노테이션
	 * spring 컨테이너가 자동으로 적합한 bean을 찾아서 연결해 주기에 "new"키워드를 일일히 쓸 필요가 없어진다.
	 */
	private EmailService emailService;
	/* EmailService : 이메일 발송 기능을 담당하는 서비스 클래스.
	 * 본 컨트롤러에서 emailService 객체를 통해 이메일 발송.
	 */
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	/* BCryptPasswordEncoder: 암호화 알고리즘 중 하나인 BCrypt를 사용해 암호화하는 클래스.
	 * bCryptPasswordEncoder 객체를 통해 암호화 및 검증.
	 */
	
	@Value("${app.domain}")
	private String domain;
	/* @Value("${app.domain}") : Spring의 프로퍼티 주입 어노테이션.
	 * application.properties의 app.domain 설정값을 읽어와 주입.
	 * 여기서는 domain= https://bluecrab.chickenkiller.com
	 * 간단히 말해, 일일히 하드코딩 하지 않아도 됨.
	 */
	
	private static final int AUTH_TOKEN_EXPIRY_MINUTES = 10;
	// 인증 토큰의 만료 시간의 정의. MINUTES는 분 이기에 10은 10분을 의미.
	
	@GetMapping("/sendMail")
	/* @GetMapping("/sendMail"): 본 메서드가 HTTP의 GET 요청을 "/sendMail" 경로로 처리하도록 하는 어노테이션.
	 * 본 메서드는 유저에게 인증 메일을 발송하는 기능을 함.
	 */
	public ResponseEntity<AuthResponse> sendMail(HttpSession session, HttpServletRequest request, Authentication authentication) {
		/* 
		 * ResponseEntity<String> : HTTP 응답 전체를 나타내는 클래스.
		 * 본 메서드는 ResponseEntity<String>를 반환하여, HTTP 상태 코드와 응답 본문(문자열)을 함께 전달.
		 * sendMail : 유저에게 인증 메일을 발송하는 메서드.
		 * HttpSession session : 현재 HTTP 세션을 나타내는 객체.
		 * 이 객체를 통해 서버 측에서 유저별로 데이터를 임시 저장하고 관리할 수 있음.
		 * 본 메서드에서는 인증 코드를 세션에 임시 저장하는 데 사용됨.
		 * HttpServletRequest request : 현재 HTTP 요청을 나타내는 객체.
		 * 이 객체를 통해 요청 헤더, 파라미터, 클라이언트 정보 등을 접근할 수 있음.
		 * 본 메서드에서는 클라이언트의 IP 주소를 로깅하는 데 사용됨.
		 * Principal principal : 현재 인증된 사용자를 나타내는 객체.
		 * Spring Security에 의해 자동으로 주입되며, 사용자의 이름(일반적으로 username)을 포함.
		 * 본 메서드에서는 인증메일을 요청한 사용자를 식별하는 데 사용됨.
		 */
		
		// JWT 토큰에서 사용자 이메일 추출 (로컬스토리지의 user.email과 동일)
		String userEmail = authentication.getName();
		/* String userEmail : 인증된 사용자의 이메일 주소를 저장하는 변수.
		 * authentication.getName() : Authentication 인터페이스의 기본 메서드로 인증된 사용자의 주요 식별자 반환.
		 * 여기서는 JWT 토큰 에서 email을 추출하는 데 사용됨.
		 */
		String clientIp = RequestUtils.getClientIp(request);
		/* String clientIp : 클라이언트 IP 주소를 저장하는 변수.
		 * RequestUtils.getClientIP(request) : 커스텀 유틸리티 메서드로 실제 클라이언트 IP 주소를 추출한다.
		 * 보안 로깅과 IP 기반 검증에 활용.
		 */
		
		log.info("이메일 인증 요청 from: {}, by: {}", clientIp, userEmail);
		/* 동작 확인용 로그
		 * "이메일 인증 요청 from: {}, by: {}" : 로그 메시지 양식
		 * clientIP : 클라이언트 IP 주소
		 * userEmail : 인증 요청한 사용자 이메일
		 * 개인정보가 포함되는 로그 이기에 보안에 요주의.
		 */
		
		ResponseEntity<AuthResponse> responseEntity = null;
		/* HTTP 응답 객체 변수 초기화:
		* - try-catch 블록 외부에 선언하여 블록 내외부에서 모두 접근 가능
		* - null 초기화로 컴파일러의 변수 초기화 검증 통과
		* - 메서드 마지막에 단일 변수로 응답을 반환하는 클린 코드 패턴
		* - 성공/실패 상황 모두 동일한 응답 형식으로 일관성 유지
		*/
		/* ResponseEntity<AuthResponse> : HTTP 응답 전체를 나타내는 제네릭 클래스.
		 * json형태로 응답하기 위한 DTO.
		 * responseEntity : HTTP 응답을 담을 변수 선언.
		 * 초기값은 null로 설정.
		 * 
		 */
			try {
			// JWT 토큰에서 사용자 이름 추출
			// authentication.getPrincipal()을 통해 JWT의 추가 정보에 접근 가능
			String userName = "학생"; // 기본값

			try {
				// JWT에 name 필드가 있는 경우 추출 시도
				if (authentication.getPrincipal() instanceof Jwt) {
					/* if (...) : 조건문 내에서 JWT 객체로 캐스팅 후 name 클레임 추출:
					 * authentication.getPrincipal() : 현제 인증된 유저의 상세 정보를 담고 있는 객체 반환.
					 * instanceof Jwt : 반환된 객체가 Jwt 타입인지 확인.
					 * JWT 기반 인증 에서는 JWT 객체가 반환되지만 100% 보장되지 않기에 방어적 코딩이 필요.
					 * ClassCastException 방지를 위해 반드시 체크해야 함.
					 */
					Jwt jwt = (Jwt) authentication.getPrincipal();
					/* Jwt jwt : Jwt 타입으로 캐스팅된 인증 객체를 저장하는 변수.
					 * (Jwt) authentication.getPrincipal() : authentication.getPrincipal()의 반환 타입은 Object.
					 * Jwt 타입으로 안전하게 캐스팅.
					 * Jwt 객체를 통해 JWT 토큰의 클레임 정보에 접근 가능.
					 */
					String jwtName = jwt.getClaimAsString("name");
					/* String jwtName : JWT 토큰 에서 추출한 사용자 이름을 저장하는 변수.
					 * jwt.getClaimAsString("name") : Jwt 객체에서 "name" 클레임 값을 문자열로 추출.
					 * 해당 클레임이 없으면 null을 반환.
					 */
					/* JWT 구조 예시:
					 * {
					 *   "sub": "user@example.com",
					 *   "name": "홍길동",        // <- 이 값을 추출
					 *   "email": "user@example.com",
					 *   "exp": 1234567890
					 * }
					 */
					if (jwtName != null && !jwtName.trim().isEmpty()) {
					/* jwtName != null && !jwtName.trim().isEmpty() : 추출된 이름이 null이 아님 AND 공백이 아님.
					 * 공백의 검증은 trim()과 isEmpty()를 조합하여 무의미한 공백 문자열도 필터링.
					 * 이를 만족할 경우 userName 변수에 실제 이름을 할당.
					 * 즉, JWT에 유효한 이름이 있을 때에만 userName의 기본값 "학생"을 해당 이름으로 대체.
					 */
						userName = jwtName;
					/* userName에 JWT에서 추출한 이름 할당
					 * 이후 이메일 본문에 인증 대상자의 실제 이름을 포함하는 데 사용.
					 */
					}
					// 원래라면 여기에 else 블록이 쓰이지만 기본값을 할당 했기에, if 조건을 만족하지 않으면 기본값을 유지.
				}
				// if (authentication.getPrincipal() instanceof Jwt) 블록 종료
			} catch (Exception e) {
				log.debug("JWT에서 사용자 이름 추출 실패, 기본값 사용: {}", e.getMessage());
				/* 실패시의 로그 처리:
				 * debug : info/warn/error 레벨과 달리 개발/디버깅 시에만 필요한 상세 정보.
				 * 
				 * 예외 유형 : 
				 * - ClassCastException: 타입 캐스팅 실패 (드물지만 가능)
				 * - IllegalArgumentException : getClaimAsString() 파싱 오류
				 * - NullPointerException: 예상치 못한 null 참조
				 * - RuntimeException: JSON 파싱 오류, 메모리 부족 등
				 *
				 * 방어적 프로그래밍 원칙 :
				 * - 부수적인 기능이 핵심 기능에 영향을 끼치지 않도록 설계
				 * - 예외 발생 시에도 userName은 기본값 "학생"으로 유지되어 정상 진행
				 * - 견고한 시스템 설계: 일부 실패가 전체 서비스 중단으로 이어지지 않음
				 */
			}
			
			// 기존 인증 토큰 정리
			session.removeAttribute("auth_token");
			session.removeAttribute("auth_user_email");
			session.removeAttribute("auth_client_ip");
			session.removeAttribute("auth_token_created");
			
			// UUID를 이용한 안전한 토큰 생성
			String authToken = UUID.randomUUID().toString().replace("-", "");
			LocalDateTime tokenCreatedTime = LocalDateTime.now();
			
			// 세션에 인증 정보 저장 (현재는 이메일 기반, 추후 USER_IDX 기반으로 변경 예정)
			session.setAttribute("auth_token", authToken);
			session.setAttribute("auth_user_email", userEmail);
			session.setAttribute("auth_client_ip", clientIp);
			session.setAttribute("auth_token_created", tokenCreatedTime);
			
			log.info("인증 토큰 생성 완료 - Email: {}, Name: {}, IP: {}, Token: {}", 
					userEmail, userName, clientIp, authToken.substring(0, 8) + "...");
			
			String encryptedToken = bCryptPasswordEncoder.encode(authToken);
			
			// TODO: 추후 DB에서 사용자 이름 조회로 변경 예정
			// 향후 변경사항: UserTbl user = userTblRepository.findByUserEmail(userEmail).get();
			// String userName = user.getUserName();
			// 현재는 JWT 토큰에서 추출한 이름 사용
			
			// 이메일 발송 (로컬스토리지의 이메일 주소 활용)
			String emailContent = createEmailContent(userName, encryptedToken);
			emailService.sendMIMEMessage("bluecrabacademy@gmail.com", userEmail, "BlueCrab 이메일 인증", emailContent);
			
			responseEntity = new ResponseEntity<>(
				new AuthResponse("인증메일이 발송되었습니다. 10분 이내에 인증을 완료해주세요."), 
				HttpStatus.OK);
			
		} catch (Exception e) {
			log.error("메일 발송 실패 - Email: {}, IP: {}, Error: {}", userEmail, clientIp, e.getMessage());
			
			// 오류 시 세션 정리
			session.removeAttribute("auth_token");
			session.removeAttribute("auth_user_email");
			session.removeAttribute("auth_client_ip");
			session.removeAttribute("auth_token_created");
			
			responseEntity = new ResponseEntity<>(
				new AuthResponse("메일 발송 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."), 
				HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return responseEntity;
	}
	
	@GetMapping("/checkAuth")
	/* @GetMapping("/checkAuth"): 본 메서드가 HTTP의 GET 요청을 "/checkAuth" 경로로 처리하도록 하는 어노테이션.
	 * 웹 브라우저의 주소 창에 URL을 직접 입력하여 접근 할 수 있음.
	 */
	public ResponseEntity<AuthResponse> checkAuth(@RequestParam("encryptedPW") String encryptedPW, HttpSession session, HttpServletRequest request){
		/* HttpServletRequest request : 현재 HTTP 요청을 나타내는 객체.
		 * 이 객체를 통해 요청 헤더, 파라미터, 클라이언트 정보 등을 접근할 수 있음.
		 * 본 메서드에서는 클라이언트의 IP 주소를 로깅하는 데 사용됨.
		 */

		String clientIp = RequestUtils.getClientIp(request);
		
		log.info("인증 확인 요청 from: {}, encryptedToken: {}", 
				clientIp, encryptedPW.substring(0, Math.min(8, encryptedPW.length())) + "...");

		ResponseEntity<AuthResponse> responseEntity = null;
		/* json형태로 응답하기 위한 DTO
		 * ResponseEntity<AuthResponse> responseEntity : HTTP 응답을 담을 변수 선언.
		 * 초기값은 null로 설정.
		 */

		try {
			// 파라미터 유효성 검사
			if (encryptedPW == null || encryptedPW.trim().isEmpty()) {
				log.warn("인증 토큰이 비어있음 - IP: {}", clientIp);
				responseEntity = new ResponseEntity<>(
					new AuthResponse("인증 토큰이 전송되지 않았습니다."), 
					HttpStatus.BAD_REQUEST);
				return responseEntity;
			}
			
			// 세션에서 인증 정보 추출
			String storedToken = (String) session.getAttribute("auth_token");
			String storedUserEmail = (String) session.getAttribute("auth_user_email");
			String storedClientIp = (String) session.getAttribute("auth_client_ip");
			LocalDateTime tokenCreatedTime = (LocalDateTime) session.getAttribute("auth_token_created");
			
			// 세션 유효성 검사
			if (storedToken == null || storedUserEmail == null || 
				storedClientIp == null || tokenCreatedTime == null) {
				log.warn("세션에 인증 정보가 없음 - IP: {}", clientIp);
				responseEntity = new ResponseEntity<>(
					new AuthResponse("인증 세션이 만료되었거나 존재하지 않습니다. 다시 인증을 요청해주세요."), 
					HttpStatus.UNAUTHORIZED);
				return responseEntity;
			}
			
			// 토큰 만료 시간 확인
			LocalDateTime now = LocalDateTime.now();
			if (now.isAfter(tokenCreatedTime.plusMinutes(AUTH_TOKEN_EXPIRY_MINUTES))) {
				log.warn("인증 토큰 만료 - Email: {}, IP: {}, 생성시간: {}", 
						storedUserEmail, clientIp, tokenCreatedTime);
				
				// 만료된 세션 정리
				session.removeAttribute("auth_token");
				session.removeAttribute("auth_user_email");
				session.removeAttribute("auth_client_ip");
				session.removeAttribute("auth_token_created");
				
				responseEntity = new ResponseEntity<>(
					new AuthResponse("인증 토큰이 만료되었습니다. 다시 인증을 요청해주세요."), 
					HttpStatus.UNAUTHORIZED);
				return responseEntity;
			}
			
			// TODO: 추후 DB 참조로 사용자 재검증 추가 예정
			// 향후 변경사항: DB에서 USER_IDX로 사용자 정보 재조회 및 상태 확인
			// Optional<UserTbl> optionalUser = userTblRepository.findById(storedUserId);
			// if (!optionalUser.isPresent() || !optionalUser.get().isActive()) { ... }
			
			// IP 주소 대조 (보안 강화)
			if (!storedClientIp.equals(clientIp)) {
				log.warn("IP 주소 불일치 - 요청IP: {}, 저장된IP: {}, Email: {}", 
						clientIp, storedClientIp, storedUserEmail);
				
				// 보안상 세션 정리
				session.removeAttribute("auth_token");
				session.removeAttribute("auth_user_email");
				session.removeAttribute("auth_client_ip");
				session.removeAttribute("auth_token_created");
				
				responseEntity = new ResponseEntity<>(
					new AuthResponse("보안상 이유로 인증이 거부되었습니다. 인증을 요청한 기기에서만 가능합니다."), 
					HttpStatus.FORBIDDEN);
				return responseEntity;
			}
			
			log.info("사용자 정보 대조 완료 - Email: {}, IP: {}", storedUserEmail, storedClientIp);
			
			// 토큰 검증
			boolean isTokenValid = bCryptPasswordEncoder.matches(storedToken, encryptedPW);
			
			if (isTokenValid) {
				log.info("이메일 인증 성공 - Email: {}, IP: {}", storedUserEmail, clientIp);
				
				// 성공 시 세션 정리
				session.removeAttribute("auth_token");
				session.removeAttribute("auth_user_email");
				session.removeAttribute("auth_client_ip");
				session.removeAttribute("auth_token_created");
				
				responseEntity = new ResponseEntity<>(
					new AuthResponse("이메일 인증이 성공적으로 완료되었습니다."), 
					HttpStatus.OK);
			} else {
				log.warn("인증 토큰 불일치 - Email: {}, IP: {}", storedUserEmail, clientIp);
				
				// 실패 시 세션 정리
				session.removeAttribute("auth_token");
				session.removeAttribute("auth_user_email");
				session.removeAttribute("auth_client_ip");
				session.removeAttribute("auth_token_created");
				
				responseEntity = new ResponseEntity<>(
					new AuthResponse("인증에 실패했습니다. 올바른 인증 링크를 사용해주세요."), 
					HttpStatus.UNAUTHORIZED);
			}
			
		} catch (Exception e) {
			log.error("인증 확인 중 오류 발생 - IP: {}, Error: {}", clientIp, e.getMessage());
			
			// 오류 시 세션 정리
			session.removeAttribute("auth_token");
			session.removeAttribute("auth_user_email");
			session.removeAttribute("auth_client_ip");
			session.removeAttribute("auth_token_created");
			
			responseEntity = new ResponseEntity<>(
				new AuthResponse("인증 처리 중 오류가 발생했습니다. 다시 시도해주세요."), 
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return responseEntity;
		// responseEntity값을 반환 하여 마무리.
	}

	/**
	 * 이메일 인증 HTML 내용 생성 (기본 스타일 사용)
	 */
	private String createEmailContent(String userName, String encryptedToken) {
		StringBuilder content = new StringBuilder();
		content.append("<div>");
		content.append("<h2>BlueCrab 이메일 인증</h2>");
		content.append("<p>안녕하세요, <strong>").append(userName).append("</strong>님!</p>");
		content.append("<p>BlueCrab 서비스의 이메일 인증을 진행해주세요.</p>");
		
		// 보안 안내
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
		content.append("<a href='").append(domain).append("/BlueCrab-1.0.0/checkAuth?encryptedPW=").append(encryptedToken).append("'>");
		content.append("이메일 인증하기</a>");
		content.append("</p>");
		
		content.append("<p><small>본 메일은 자동 발송되었습니다.</small></p>");
		content.append("</div>");
		
		return content.toString();
	}

}
