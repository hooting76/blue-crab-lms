// 작업자 : 성태준  
// 이메일 템플릿 생성 유틸리티 클래스
// HTML 형식의 이메일 본문 내용을 생성하는 기능을 담당

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========

// ========== 외부 라이브러리 ==========
import lombok.extern.slf4j.Slf4j;

// ========== Spring Framework ==========
import org.springframework.stereotype.Component;


@Component // Spring의 컴포넌트 스캔에 의해 빈으로 등록
@Slf4j // Lombok을 사용한 로깅 지원
public class EmailTemplateUtils {
    // 이메일 템플릿 생성 메서드
    
    // ========== 이메일 템플릿 설정 상수 ==========
    private static final String BRAND_NAME = "BlueCrab";
    // 브랜드명 - 이메일 제목과 본문에 사용
    private static final String BRAND_COLOR = "#2c5aa0";
    // 브랜드 주색상 - 제목과 인증코드 박스에 사용
    private static final String BACKGROUND_COLOR = "#f8f9fa";
    // 인증코드 박스 배경색
    private static final String WARNING_BACKGROUND = "#fff3cd";
    // 경고/안내 섹션 배경색
    private static final String WARNING_BORDER = "#ffeaa7";
    // 경고/안내 섹션 테두리색
    private static final String WARNING_TEXT = "#856404";
    // 경고/안내 텍스트 색상
    private static final String SECONDARY_TEXT = "#6c757d";
    // 보조 텍스트 색상 (자동발송 안내 등)
    
    // 인증 코드 이메일 HTML 템플릿 생성
    // (EmailAuthController에서 활용)
    public String createAuthCodeEmailTemplate(String userName, String authCode, int expiryMinutes) {

        // ========== 입력값 검증 ==========
        if (userName == null || userName.trim().isEmpty()) {
            // 사용자 이름이 없으면 기본값 사용
            userName = "학생";
            // 기본 값 : "학생"
            log.debug("User name is null or empty, using default: {}", userName);
            // 디버그 로그 기록
        } // if 끝
        
        if (authCode == null || authCode.trim().isEmpty()) {
            // 인증 코드가 없으면 예외 발생
            log.error("Auth code is null or empty - cannot generate email template");
            // 에러 로그 기록
            throw new IllegalArgumentException("인증 코드가 유효하지 않습니다.");
            // 예외 발생
        } // if 끝
        
        if (expiryMinutes <= 0) {
            // 유효 시간이 올바르지 않으면 기본값 사용
            log.warn("Invalid expiry minutes: {}, using default: 5", expiryMinutes);
            // 경고 로그 기록
            expiryMinutes = 5;
            // 기본 값 : 5분
        } // if 끝
        
        log.debug("Generating auth code email template - User: {}, Code: {}, Expiry: {} minutes", 
                userName, authCode, expiryMinutes);
        // 디버그 로그 기록
        
        StringBuilder content = new StringBuilder();
        // HTML 이메일 본문 내용을 구성하기 위한 StringBuilder
        
        // ========== HTML 이메일 템플릿 구성 ==========
        
        // 메인 컨테이너 시작
        content.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        // 메인 컨테이너
        buildHeaderSection(content);
        // 헤더 섹션
        buildGreetingSection(content, userName);
        // 인사말 섹션
        buildAuthCodeSection(content, authCode);
        // 인증 코드 박스
        buildSecurityNoticeSection(content, expiryMinutes);
        // 보안 안내 섹션
        buildFooterSection(content);
        // 푸터 섹션
        content.append("</div>");
        // 메인 컨테이너 닫기
        log.debug("Successfully generated auth code email template for user: {}", userName);
        // 성공 로그 기록
        return content.toString();
        // 완성된 HTML 문자열 반환
    } // createAuthCodeEmailTemplate 끝
    
    // 헤더 섹션 구성 - 브랜드명과 제목
    private void buildHeaderSection(StringBuilder content) {
        content.append("<h2 style='color: ").append(BRAND_COLOR).append(";'>")
               .append(BRAND_NAME).append(" 이메일 인증</h2>");
    } // buildHeaderSection 끝
    
    // 인사말 섹션 구성 - 사용자명과 안내 메시지
    private void buildGreetingSection(StringBuilder content, String userName) {
        content.append("<p>안녕하세요, <strong>").append(userName).append("</strong>님!</p>")
               .append("<p>아래 인증 코드를 입력하여 이메일 인증을 완료해주세요.</p>");
    } // buildGreetingSection 끝
    
    // 인증 코드 박스 섹션 구성 - 강조된 인증 코드 표시
    private void buildAuthCodeSection(StringBuilder content, String authCode) {
        content.append("<div style='background-color: ").append(BACKGROUND_COLOR)
               .append("; border: 2px solid ").append(BRAND_COLOR)
               .append("; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0;'>")
               .append("<h1 style='font-size: 36px; letter-spacing: 8px; margin: 0; color: ")
               .append(BRAND_COLOR).append(";'>")
               .append(authCode)
               .append("</h1>")
               .append("</div>");
    } // buildAuthCodeSection 끝
    
    // 보안 안내 섹션 구성 - 유효시간 및 보안 주의사항
    private void buildSecurityNoticeSection(StringBuilder content, int expiryMinutes) {
        content.append("<div style='background-color: ").append(WARNING_BACKGROUND)
               .append("; border: 1px solid ").append(WARNING_BORDER)
               .append("; border-radius: 4px; padding: 15px; margin: 20px 0;'>")
               .append("<p style='margin: 0; font-weight: bold; color: ").append(WARNING_TEXT)
               .append(";'>보안 안내</p>")
               .append("<ul style='margin: 10px 0; color: ").append(WARNING_TEXT).append(";'>")
               .append("<li>인증 코드는 ").append(expiryMinutes).append("분간만 유효합니다.</li>")
               .append("<li>인증을 요청한 기기에서만 사용할 수 있습니다.</li>")
               .append("<li>타인과 공유하지 마세요.</li>")
               .append("</ul>")
               .append("</div>");
    } // buildSecurityNoticeSection 끝
    
    // 푸터 섹션 구성 - 자동발송 안내
    private void buildFooterSection(StringBuilder content) {
        content.append("<p style='color: ").append(SECONDARY_TEXT)
               .append("; font-size: 14px;'>본 메일은 자동 발송되었습니다.</p>");
    } // buildFooterSection 끝
    
    // 관리자용 인증 코드 이메일 HTML 템플릿 생성
    // (AdminEmailAuthController에서 활용)
    public String createAdminAuthCodeEmailTemplate(String adminName, String authCode, int expiryMinutes) {
        // 입력값 검증 (동일한 로직)
        if (adminName == null || adminName.trim().isEmpty()) {
            // 관리자 이름이 없으면 기본값 사용
            adminName = "관리자";
            // 기본 값 : "관리자"
            log.debug("Admin name is null or empty, using default: {}", adminName);
            // 디버그 로그 기록
        } // if 끝
        
        if (authCode == null || authCode.trim().isEmpty()) {
            // 인증 코드가 없으면 예외 발생
            log.error("Auth code is null or empty - cannot generate admin email template");
            // 에러 로그 기록
            throw new IllegalArgumentException("인증 코드가 유효하지 않습니다.");
            // 예외 발생
        } // if 끝
        
        if (expiryMinutes <= 0) {
            // 유효 시간이 올바르지 않으면 기본값 사용
            log.warn("Invalid expiry minutes: {}, using default: 5", expiryMinutes);
            // 경고 로그 기록
            expiryMinutes = 5;
            // 기본 값 : 5분
        } // if 끝
        
        log.debug("Generating admin auth code email template - Admin: {}, Code: {}, Expiry: {} minutes", 
                adminName, authCode, expiryMinutes);
        // 디버그 로그 기록
        StringBuilder content = new StringBuilder();
        // HTML 이메일 본문 내용을 구성하기 위한 StringBuilder
        
        // ========== 관리자용 HTML 이메일 템플릿 구성 ==========
        
        // 메인 컨테이너
        content.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        
        // 관리자용 헤더 섹션
        content.append("<h2 style='color: ").append(BRAND_COLOR).append(";'>")
               .append("관리자 이메일 인증코드</h2>");
        
        // 관리자 인사말 섹션
        content.append("<p>안녕하세요, <strong>").append(adminName).append("</strong>님!</p>")
               .append("<p>아래 인증코드를 입력해 주세요.</p>");
        
        // 인증 코드 박스 (동일한 디자인)
        buildAuthCodeSection(content, authCode);
        
        // 관리자용 보안 안내 섹션
        content.append("<div style='background-color: ").append(WARNING_BACKGROUND)
               .append("; border: 1px solid ").append(WARNING_BORDER)
               .append("; border-radius: 4px; padding: 15px; margin: 20px 0;'>")
               .append("<ul style='margin: 10px 0; color: ").append(WARNING_TEXT).append(";'>")
               .append("<li>인증코드는 ").append(expiryMinutes).append("분간만 유효합니다.</li>")
               .append("<li>인증을 요청한 기기에서만 인증이 가능합니다.</li>")
               .append("<li>타인과 공유하지 마세요.</li>")
               .append("</ul>")
               .append("</div>");
        
        // 푸터 섹션 (동일)
        buildFooterSection(content);
        
        // 메인 컨테이너 닫기
        content.append("</div>");
        
        log.debug("Successfully generated admin auth code email template for admin: {}", adminName);
        // 성공 로그 기록
        
        return content.toString();
        // 완성된 HTML 문자열 반환

    } // createAdminAuthCodeEmailTemplate 끝
    
    public String getBrandColor() {
        // 브랜드 주색상을 반환하는 메서드
        return BRAND_COLOR;
        // 브랜드 주색상 반환
    } // getBrandColor 끝

    public String getBrandName() {
        // 브랜드명을 반환하는 메서드
        return BRAND_NAME;
        // 브랜드명 반환
    } // getBrandName 끝
} // EmailTemplateUtils 클래스 끝
