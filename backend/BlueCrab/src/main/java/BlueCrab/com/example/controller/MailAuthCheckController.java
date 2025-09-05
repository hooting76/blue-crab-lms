/*
 * Spring MVC의 RESTful API 컨트롤러.
 * "/sendMail" : 메일 인증 코드를 생성하고 유저에게 메일 발송.
 * "/checkAuth" : 유저가 메일에서 전달 받은 인증 링크를 확인.
 */
package BlueCrab.com.example.controller;

import java.io.File;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import BlueCrab.com.example.service.EmailService;
import lombok.extern.slf4j.Slf4j;

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
	/* @Autowired : spring에게 의존성 주입의 자동처리 요청을 날리는 어노테이션
	 * spring 컨테이너가 자동으로 적합한 bean을 찾아서 연결해 주기에 "new"키워드를 일일히 쓸 필요가 없어진다.
	 */
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	/* BCryptPasswordEncoder: 암호화 알고리즘 중 하나인 BCrypt를 사용해 암호화하는 클래스.
	 * bCryptPasswordEncoder 객체를 통해 암호화 및 검증.
	 */
	
	@Value("${app.domain}")
	private String domain;
	
	@GetMapping("/sendMail")
	/* @GetMapping("/sendMail"): 본 메서드가 HTTP의 GET 요청을 "/sendMail" 경로로 처리하도록 하는 어노테이션.
	 * 본 메서드는 유저에게 인증 메일을 발송하는 기능을 함.
	 */
	public ResponseEntity<String> sendMail(HttpSession session){
		/* 
		 * ResponseEntity<String> : HTTP 응답 전체를 나타내는 클래스.
		 * 본 메서드는 ResponseEntity<String>를 반환하여, HTTP 상태 코드와 응답 본문(문자열)을 함께 전달.
		 * sendMail : 유저에게 인증 메일을 발송하는 메서드.
		 * HttpSession session : 현재 HTTP 세션을 나타내는 객체.
		 * 이 객체를 통해 서버 측에서 유저별로 데이터를 임시 저장하고 관리할 수 있음.
		 * 본 메서드에서는 인증 코드를 세션에 임시 저장하는 데 사용됨.
		 */

		/* 
		 * 
		 */
		
		log.info("인증메일 송부");
		// 동작 확인용 로그
		
		ResponseEntity<String> responseEntity = null;
		/* ResponseEntity<String> responseEntity : HTTP 응답을 담을 변수 선언.
		 * 초기값은 null로 설정.
		 */

		String hashcode = new Object().hashCode() + "";
		/* 메일 인증에 사용할 임의의 인증 코드를 생성.
		 * 시행 할 때 마다 다른 임의의 값이 생성됨.
		 * String hashcode: 생성된 인증 코드를 담는 변수.
		 * ++변수명이 "hashcode"인 이유는, Object의 hashCode() 메서드를 사용했기 때문.++
		 * new Object().hashCode(): 신규 object 인스턴스를 생성하고, 객체에 부여된 해시코드를 반환.
		 * ++ "new Object()" 부분이 시행 시 마다 메모리의 다른 위치에 새로운 객체를 생성하기 때문에,
		 * 결과적으로 매번 다른 해시코드가 생성됨.++
		 */

		session.setAttribute("hashcode", hashcode); 
		/* session: 사용자가 해당 웹 사이트를 이용하는 동안 유지되는 서버 측 저장 공간.
		 * 접속에 사용한 브라우저 에만 유효하며, 해당 브라우저를 닫거나 일정 시간이 지나 세션이 만료되면 삭제됨.
		 * setAttribute("hashcode", hashcode): session에 데이터를 저장 하기 위한 메서드.
		 * 이 중 첫번쩨 인자 "hashcode"는 데이터를 식별하는 이름(키)이고, 
		 * 두 번째 인자 hashcode는 저장할 실제 데이터(값) 이다.
		 * 원형은 setAttribute(String name, Object value) 으로, 여기서 Object value는 모든 타입의 데이터를 
		 * 저장할 수 있음을 의미.
		 * 이후 getAttribute("hashcode") 메서드를 사용해 세션에서 이 값을 꺼낼 수 있음.
		 */
		
		log.info("원본 패스워드:" + session.getAttribute("hashcode"));
		// 동작 확인용 로그
				

		// 암호화 알고리즘은 수정 필요.
		// String encryptedPW = bCryptPasswordEncoder.encode("java");
		String encryptedPW = bCryptPasswordEncoder.encode(hashcode);
		/* String encryptedPW: 암호화된 인증 코드를 저장할 변수를 선언.
		 * bCryptPasswordEncoder: Spring Security가 제공하는 암호화 도구.
		 * .encode(hashcode): hashcode 변수를 BCrypt 알고리즘으로 암호화.
		 * ++Bycrypt는 단방향 해시 해시 알고리즘으로, 암호화된 결과(encryptedPW)를 
		 * 다시 원본(hashcode)으로 되돌릴 수 없음고, 같은 원본 입력값 이라도 매번 다른 암호화 결과가 생성됨.++
		 */
		
		try {
			// 메일 발송 작업을 수행하는 부분
			emailService.sendMIMEMessage("shizuka1887@gmail.com", 
					"123joon@naver.com",
					"데모 인증 메일", 
					"<span style='background-color:yellow'>링크를 클릭해서 인증 </span>"
					+ "<a href='" + domain + "/BlueCrab-1.0.0/checkAuth?encryptedPW="+encryptedPW+"'>메일 인증 링크</a>");
					
			/* emailService: EmailService 클래스의 인스턴스로, @Autowired 어노테이션을 통해 주입된 객체, 발송 기능을 담당.
			 * sendMIMEMessage(...): HTML 형식의 이메일을 발송하는 메서드.
			 * from: 메일 발송자 주소.
			 * to: 메일 수신자 주소.
			 * subject: 메일 제목.
			 * "<span ...> + <a href='...'>...</a>": 메일 본문 내용으로, HTML 형식으로 작성.
			 * 본문에는 노란색 배경의 텍스트와 인증 링크가 포함.
			 * URL 부분 : /checkAuth 이라는 인증 확인 페이지로 이동 + 앞서 암호화 했었던 encryptedPW값을 쿼리 파라미터로 함께 전달.
			 */
			// http://localhost:8181/ 부분은 이후 수정 필요.
			
			// 메일 발송이 성공했을 때 클라이언트 에게 보낼 응답을 "준비".
			responseEntity = new ResponseEntity<>("인증메일 발송 성공", HttpStatus.OK);
			/* responseEntity: HTTP 응답을 담을 객체. 
			 * new ResponseEntity<>() : ResponseEntity 객체를 선언
			 * ("인증메일 발송 성공", HttpStatus.OK) : "응답 본문에 포함될 메세지", HTTP 상태코드(200).OK 
			 * 요청이 성공적으로 처리 되었음을 의미.
			 * 
			 * 여기까지의 모든 항목이 문제 없이 실행 되면, ResponseEntity 객체가 생성되고, responseEntity 변수에 할당됨.
			 * 이후 return을 통해 최종 전달됨.
			 */

		// try 블록 안에서 emailService.sendMIMEMessage 호출이 실패했을 때, 제어권이 catch블록으로 이양된다.
		} catch (Exception e) {
		/* catch : try 블록에서 예외가 발생 했을시, 이를 포착하는 키워드.
		 * (Exception e) : JAVA상의 모든 예외를 포함하는 최상위 클래스 Exception를 이용해서 잡아내 e 라는 변수에 담는다.
		 */
			
			log.error("메일 발송 실패: " + e);
			/* 에러 확인용 로그
			 * log.error() : @Slf4j 어노테이션으로 생성딘 로거 메서드. ERROR 수준의 로그를 남긴다.(주로 심각한 오류에 사용)
			 * ("메일 발송 실패: " + e) : 콘솔, 로그파일 등에 기록될 로그 메세지 구조. 메세지와 e에 담긴 예외정보를 기록
			 */
			responseEntity = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			/* ResponseEntity: HTTP 응답을 담는 객체
			 * HttpStatus.INTERNAL_SERVER_ERROR: HTTP 상태 코드 500.
			 * 상태 코드 500은 클라이언트 측의 요청은 유효했으나, 서버 내부의 예측하지 못 한 오류로 요청의 처리가
			 * 불가능함을 의미.
			 * 이로인해 메일 발송에 실패 했을 경우 HTTP 응답의 상태 코드를 500으로 설정 하게 된다.
			 */
		}
		// 최종적으로 ResponseEntity메서드의 값을 반환.
		return responseEntity;
		/* 
		 * try-catch 블록에서 결과에 따라,
		 * 성공 : "인증메일 발송 성공" 메시지 + 상태코드 200 응답.
		 * 실패 : 상태코드 500 응답.
		 */
	}
	
	@GetMapping("/checkAuth")
	/* @GetMapping("/checkAuth"): 본 메서드가 HTTP의 GET 요청을 "/checkAuth" 경로로 처리하도록 하는 어노테이션.
	 * 웹 브라우저의 주소 창에 URL을 직접 입력하여 접근 할 수 있음.
	 */
	public ResponseEntity<String> checkAuth(@RequestParam("encryptedPW") String encryptedPW, HttpSession session){
	/* ResponseEntity<String> : HTTP 응답 전체를 나타내는 클래스.
	 * 본 메서드는 ResponseEntity<String>를 반환하여, HTTP 상태 코드와 응답 본문(문자열)을 함께 전달.
	 * checkAuth : 유저가 메일에서 전달 받은 인증 링크를 확인하는 메서드.
	 * @RequestParam("encryptedPW") String encryptedPW : HTTP 요청의 쿼리 파라미터에서 
	 * "encryptedPW"라는 이름으로 전달된 값을 String 타입의 변수 encryptedPW에 매핑.
	 * HttpSession session : 현재 HTTP 세션을 나타내는 객체.
	 * 이 객체를 통해 서버 측에서 유저별로 데이터를 임시 저장하고 관리할 수 있음.
	 * 본 메서드에서는 이전에 세션에 저장된 인증 코드를 꺼내는 데 사용됨.
	 */
	/* public ResponseEntity<String> checkAuth(...):
	* 이 메서드는 HTTP 응답(상태 코드, 메시지 등)을 포함하는 'ResponseEntity' 객체를 반환합니다.
	* <String>은 응답 본문이 문자열 타입이라는 것을 의미합니다.
	* * @RequestParam("encryptedPW") String encryptedPW:
	* HTTP 요청의 쿼리 파라미터(URL의 '?' 뒤에 오는 데이터)에서 "encryptedPW"라는 이름의 값을
	* 가져와 'encryptedPW'라는 String 변수에 자동으로 할당해줍니다.
	* * HttpSession session:
	* 현재 사용자의 세션 정보를 관리하는 객체입니다. 'sendMail' 메서드에서 세션에 저장해두었던
	* 원본 인증 코드를 가져오기 위해 사용됩니다.
	*/
	/* public : 접근 제어자. 이 메서드가 클래스 외부에서도 호출될 수 있음을 의미.
	 * ResponseEntity<String> : HTTP 응답 전체를 나타내는 클래스.
	 * checkAuth(...) : 유저가 메일에서 전달 받은 인증 링크를 확인하는 메서드.
	 * (@RequestParam("encryptedPW") String encryptedPW, HttpSession session) :
	 * 
	 */

		log.info("encryptedPW:" + encryptedPW);
		
		ResponseEntity<String> responseEntity = null;
		
		try {
			
			if (encryptedPW.trim().equals("")) {
				
				responseEntity = new ResponseEntity<>("인증 패스워드가 전송되지 않았습니다.",HttpStatus.NO_CONTENT); // 컨텐츠가 없을 때
			
			} else {
				log.info("세션화 된 원본 패스워드:" + session.getAttribute("hashcode"));
				String hashcode = session.getAttribute("hashcode").toString();
				
				log.info("인증 성공여부: " + bCryptPasswordEncoder.matches(hashcode, encryptedPW));
				
				session.removeAttribute("hashcode"); // 임시 원본 코드 삭제
				
				// 인증 패스워드 점검
				if (bCryptPasswordEncoder.matches(hashcode, encryptedPW) == true) {
					
					responseEntity = new ResponseEntity<>("인증이 완료되었습니다.",HttpStatus.OK);
					
				} else {
					
					responseEntity = new ResponseEntity<>("인증에 실패했습니다.",HttpStatus.UNAUTHORIZED);
					
				}
								
			}
			
		} catch (Exception e) {
			
			log.error("패스워드 인증 오류: " + e);
			responseEntity = new ResponseEntity<>("서버에 문제가 있습니다.",HttpStatus.INTERNAL_SERVER_ERROR);
			
		}
		
		return responseEntity;
	}

}
