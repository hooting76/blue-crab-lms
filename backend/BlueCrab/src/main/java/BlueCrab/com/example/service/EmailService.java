/* 작업자 : 성태준
 * 실제 메일 발송하는 로직을 담고 있는 클래스.
 * 총 3가지 메소드로 구성되어 있음.
*/
package BlueCrab.com.example.service;

import javax.mail.internet.MimeMessage;
import org.springframework.lang.NonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;


@Service
public class EmailService {

	@Autowired
	/* @Autowired : spring에게 의존성 주입의 자동처리 요청을 날리는 어노테이션
	 * spring 컨테이너가 자동으로 적합한 bean을 찾아서 연결해 주기에 "new"키워드를 일일히 쓸 필요가 없어진다.
	 */
	private JavaMailSender emailSender;
	/* JavaMailSender : Spring이 제공하는 "이메일 발송"의 핵심 인터페이스
	 * 단독으로는 기능 하지 않으나, xml설정파일에 의해 정의된 SMTP정보(host, port, username, password 등)를 
	 * 기반으로 동작.
	 */
	/* "@Autowired" 어노테이션이 "emailSender"객체(JavaMailSender 인터페이스에서 정의된)를 찾아서 알아서
	 * 연결해 주기에 작업시 "emailSender"객체를 통해 send()메서드 같은 것을 호출 하기만 하면 된다.
	 */
	
	// 단순한 텍스트메일
	// 현재 미사용이지만 향후 확장성을 위해 유지
	public void sendSimpleMessage(String from, String to, String subject, String text) {
		// sendSimpleMessage : 단순한 평문(Plain text)으로 된 이메일을 발송하는 메서드.
		
        SimpleMailMessage message = new SimpleMailMessage(); 
		// sendSimpleMessage : 단순한 평문(Plain text)으로 된 이메일을 나타내는 객체
		// 당연히 제목, 발신자, 수신자, 내용을 설정 할 수 있다.
		// 첨부파일은 포함되지 않는다.
        message.setFrom(from);
		// message.setFrom(from) : 메일 발송자의 주소를 설정.
        message.setTo(to); 
		// message.setTo(to) : 메일 수신자의 주소를 설정.
        message.setSubject(subject); 
		// message.setSubject(subject) : 메일의 제목을 설정.
        message.setText(text);
		// message.setText(text) : 메일의 본문을 설정.
        emailSender.send(message);
		// emailSender.send(message) : 이상 설정에 따라서 메일 내용을 실제로 발송.

    }
	
	// HTML, 첨부파일 등 복잡한 메일
	// 현재 미사용이지만 향후 첨부파일 기능 확장을 위해 유지
	// https://docs.spring.io/spring-framework/reference/6.0/integration/email.html#mail-usage-mime
	public void sendMIMEMessage(String from, String to, String subject, String text, FileSystemResource file) {
		
		MimeMessagePreparator preparator = new MimeMessagePreparator() {
			// MimeMessagePreparator : 메일을 발송하는 "준비"를 하는 인터페이스.
			
			@Override
			public void prepare(@NonNull MimeMessage message) throws Exception {
				// prepare : MimeMessage 객체를 받아서 메세지를 구성하는 메서드.

				final MimeMessageHelper mailHelper = new MimeMessageHelper(message, true, "UTF-8");
				/* MimeMessageHelper : MimeMassage의 취급을 보조하는 헬퍼 클래스
				 * 생성자 new MimeMessageHelper(message, true, "UTF-8") 에서 
				 * 첫번째 인자는 MimeMessage 객체. 
				 * 두번쩨 인자인 "true"부분이 멀티파트 메세지를 지원함 을 의미하는 부분으로, 
				 * 즉, 첨부파일을 포함할 수 있음을 의미.(HTML형식도 포함)
				 * 세번째 인자는 문자 인코딩 설정으로 여기서는 "UTF-8"로 설정해서 한글을 사용 가능하도록 한다.
				 */
				
				mailHelper.setFrom(from);
				// mailHelper.setFrom(from) : 메일 발송자의 주소를 설정.
				mailHelper.setTo(to); 
				// mailHelper.setTo(to) : 메일 수신자의 주소를 설정.
				mailHelper.setSubject(subject); 
				// mailHelper.setSubject(subject) : 메일의 제목을 설정.
				mailHelper.setText(text, true);
				// mailHelper.setText(text, true) : 메일의 본문을 설정. 두번째 인자가 true이면 HTML로 인식.
				
				// 메일 첨부				
				mailHelper.addAttachment(file.getFilename(), file);
				/* mailHelper.addAttachment(file.getFilename(), file) : 메일에 첨부파일을 추가.
				 * 첫번째 인자는 첨부파일의 이름, 두번째 인자는 실제 파일 객체.
				 * FileSystemResource는 스프링에서 제공하는 파일 객체로, 파일 시스템의 파일을 나타낸다.
				 */
			} //
		
		};
		
        emailSender.send(preparator);
		// emailSender.send(preparator) : 이상의 설정에 따라서 메일을 실제로 발송한다.
    } //
	
	// HTML, 첨부파일 없는 메일
	// 현재 AdminEmailAuthController, MailAuthCheckController에서 사용 중 (인증코드 메일 발송용)
	// https://docs.spring.io/spring-framework/reference/6.0/integration/email.html#mail-usage-mime
	public void sendMIMEMessage(String from, String to, String subject, String text) {
		
		MimeMessagePreparator preparator = new MimeMessagePreparator() {
			/* MimeMessagePreparator : 메일을 발송하는 "준비"를 하는 인터페이스.
			 */

			@Override
			public void prepare(@NonNull MimeMessage message) throws Exception {

				final MimeMessageHelper mailHelper = new MimeMessageHelper(message, true, "UTF-8");
				/* MimeMessageHelper : MimeMassage의 취급을 보조하는 헬퍼 클래스
				 * 생성자 new MimeMessageHelper(message, true, "UTF-8") 에서 
				 * 첫번째 인자는 MimeMessage 객체. 
				 * 두번쩨 인자인 "true"부분이 멀티파트 메세지를 지원함 을 의미하는 부분으로, 
				 * 즉, 첨부파일을 포함할 수 있음을 의미.(HTML형식도 포함)
				 * 세번째 인자는 문자 인코딩 설정으로 여기서는 "UTF-8"로 설정해서 한글을 사용 가능하도록 한다.
				 */
				mailHelper.setFrom(from);
				// mailHelper.setFrom(from) : 메일 발송자의 주소를 설정.
				mailHelper.setTo(to); 
				// mailHelper.setTo(to) : 메일 수신자의 주소를 설정.
				mailHelper.setSubject(subject); 
				// mailHelper.setSubject(subject) : 메일의 제목을 설정.
				mailHelper.setText(text, true); 
				// mailHelper.setText(text, true) : 메일의 본문을 설정. 두번째 인자가 true이면 HTML로 인식.
				
			} //
		
		};
		
        emailSender.send(preparator);
		// emailSender.send(preparator) : 이상의 설정에 따라서 메일을 실제로 발송한다.
    } //

}