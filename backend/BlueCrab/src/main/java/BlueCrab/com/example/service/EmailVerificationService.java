package BlueCrab.com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * 이메일 인증 관련 서비스 클래스
 * 관리자 2단계 인증에서 이메일 인증 토큰 생성, 검증, 관리를 담당
 *
 * 주요 기능:
 * - 이메일 인증 토큰 생성 및 저장
 * - 토큰 유효성 검증
 * - 토큰 블랙리스트 처리
 * - 토큰 만료 관리
 *
 * 토큰 정책:
 * - 토큰 유효시간: 10분
 * - 토큰 길이: UUID 기반 + 추가 랜덤 문자열
 * - 일회용 토큰 (사용 후 즉시 블랙리스트)
 * - Redis를 통한 토큰 저장 및 TTL 관리
 */
@Service
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    @Autowired
    private RedisService redisService;

    // 이메일 인증 토큰 설정
    private static final int EMAIL_TOKEN_EXPIRATION_MINUTES = 10;
    private static final int SESSION_TOKEN_EXPIRATION_MINUTES = 10;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * 세션 토큰 생성 및 저장 (1차 로그인 성공 후)
     * 인증 코드 발급 요청용 임시 토큰
     * 
     * @param adminId 관리자 ID
     * @return String 생성된 세션 토큰
     */
    public String generateSessionToken(String adminId) {
        // 안전한 토큰 생성 (UUID + 추가 랜덤 문자열)
        String baseToken = UUID.randomUUID().toString().replace("-", "");
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        String additionalRandom = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        String sessionToken = baseToken + additionalRandom;
        
        // Redis에 세션 토큰 저장 (10분 TTL)
        redisService.storeSessionToken(sessionToken, adminId, SESSION_TOKEN_EXPIRATION_MINUTES);
        
        logger.info("Session token generated for admin: {}", adminId);
        return sessionToken;
    }

    /**
     * 이메일 인증 토큰 생성 및 저장
     * 관리자 1차 로그인 성공 후 호출
     * 
     * @param adminId 관리자 ID
     * @return String 생성된 이메일 인증 토큰
     */
    public String generateEmailVerificationToken(String adminId) {
        // 안전한 토큰 생성 (UUID + 추가 랜덤 문자열)
        String baseToken = UUID.randomUUID().toString().replace("-", "");
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        String additionalRandom = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        String emailToken = baseToken + additionalRandom;
        
        // Redis에 토큰 저장 (10분 TTL)
        redisService.storeEmailVerificationToken(emailToken, adminId, EMAIL_TOKEN_EXPIRATION_MINUTES);
        
        logger.info("Email verification token generated for admin: {}", adminId);
        return emailToken;
    }

    /**
     * 이메일 인증 토큰 유효성 검증
     * 2차 인증 시 호출
     * 
     * @param token 이메일 인증 토큰
     * @return String 토큰이 유효하면 관리자 ID 반환, 무효하면 null
     */
    public String validateEmailVerificationToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Empty email verification token provided");
            return null;
        }

        // Redis에서 토큰으로 관리자 ID 조회
        String adminId = redisService.getAdminIdByEmailToken(token);
        
        if (adminId != null) {
            logger.info("Valid email verification token found for admin: {}", adminId);
            return adminId;
        } else {
            logger.warn("Invalid or expired email verification token");
            return null;
        }
    }

    /**
     * 이메일 인증 토큰 블랙리스트 처리
     * 토큰 사용 후 즉시 호출하여 재사용 방지
     * 
     * @param token 이메일 인증 토큰
     */
    public void blacklistEmailVerificationToken(String token) {
        redisService.deleteEmailVerificationToken(token);
        logger.info("Email verification token blacklisted");
    }

    /**
     * 관리자에게 이메일 인증 메일 발송
     * 
     * @param email 관리자 이메일 주소
     * @param adminName 관리자 이름
     * @param verificationToken 이메일 인증 토큰
     * @param frontendBaseUrl 프론트엔드 기본 URL
     * @return boolean 이메일 발송 성공 여부
     */
    public boolean sendVerificationEmail(String email, String adminName, String verificationToken, String frontendBaseUrl) {
        try {
            String verificationUrl = frontendBaseUrl + "/admin/verify-email?token=" + verificationToken;
            String htmlContent = buildVerificationEmailContent(adminName, verificationUrl, EMAIL_TOKEN_EXPIRATION_MINUTES);
            
            // 기존 EmailService의 메서드 확인 후 적절한 메서드 호출
            // boolean sent = emailService.sendEmail(email, subject, htmlContent);
            
            // TODO: EmailService의 실제 메서드명 확인 후 구현
            logger.info("Verification email would be sent to: {}", email);
            logger.debug("Email content: {}", htmlContent);
            
            // 현재는 항상 true 반환 (실제 구현 시 수정 필요)
            return true;
        } catch (Exception e) {
            logger.error("Error sending verification email to: " + email, e);
            return false;
        }
    }

    /**
     * 이메일 인증 메일 HTML 내용 생성
     * 
     * @param adminName 관리자 이름
     * @param verificationUrl 인증 URL
     * @param expirationMinutes 만료 시간 (분)
     * @return String HTML 이메일 내용
     */
    private String buildVerificationEmailContent(String adminName, String verificationUrl, int expirationMinutes) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<title>관리자 로그인 이메일 인증</title>" +
                "<style>" +
                ".container { max-width: 600px; margin: 0 auto; font-family: Arial, sans-serif; }" +
                ".header { background-color: #007bff; color: white; padding: 20px; text-align: center; }" +
                ".content { padding: 30px; background-color: #f8f9fa; }" +
                ".button { display: inline-block; padding: 12px 24px; background-color: #28a745; " +
                "color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                ".warning { background-color: #fff3cd; border: 1px solid #ffeaa7; " +
                "padding: 15px; border-radius: 5px; margin: 20px 0; }" +
                ".footer { text-align: center; padding: 20px; color: #6c757d; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">" +
                "<h1>BlueCrab LMS 관리자 로그인</h1>" +
                "</div>" +
                "<div class=\"content\">" +
                "<h2>안녕하세요, " + adminName + "님</h2>" +
                "<p>관리자 계정으로 로그인 요청이 있었습니다.</p>" +
                "<p>로그인을 완료하려면 아래 버튼을 클릭하여 이메일 인증을 진행해주세요.</p>" +
                "<div style=\"text-align: center;\">" +
                "<a href=\"" + verificationUrl + "\" class=\"button\">이메일 인증하기</a>" +
                "</div>" +
                "<div class=\"warning\">" +
                "<strong>⚠️ 보안 안내</strong><br>" +
                "• 이 링크는 " + expirationMinutes + "분 후에 만료됩니다.<br>" +
                "• 본인이 요청하지 않은 경우 이 메일을 무시하세요.<br>" +
                "• 링크는 일회용이며, 사용 후 즉시 무효화됩니다." +
                "</div>" +
                "<p>만약 버튼이 작동하지 않는다면, 아래 링크를 복사하여 브라우저에 직접 입력하세요:</p>" +
                "<p style=\"word-break: break-all; background-color: #e9ecef; padding: 10px; border-radius: 3px;\">" +
                verificationUrl +
                "</p>" +
                "</div>" +
                "<div class=\"footer\">" +
                "<p>이 메일은 자동으로 발송된 메일입니다. 회신하지 마세요.</p>" +
                "<p>&copy; 2024 BlueCrab LMS. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * 토큰 만료 시간 확인
     * 
     * @param token 이메일 인증 토큰
     * @return long 남은 시간 (초), 토큰이 없으면 -1
     */
    public long getTokenRemainingTime(String token) {
        try {
            String key = "email_token:" + token;
            return redisService.getKeyTTL(key);
        } catch (Exception e) {
            logger.error("Error getting token remaining time", e);
            return -1;
        }
    }
}
