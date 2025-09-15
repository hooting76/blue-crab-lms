// 성태준 추가
// 비밀번호 재설정 기능

package BlueCrab.com.example.controller;

// ========== Java 표준 라이브러리 ==========
import java.util.Map; // Map 인터페이스

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired; // 의존성 주입
import org.springframework.http.HttpStatus; // HTTP 상태 코드
import org.springframework.http.ResponseEntity; // HTTP 응답 객체
import org.springframework.web.bind.annotation.*; // REST 컨트롤러 및 매핑

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.service.EmailService; // 이메일 발송 서비스
import BlueCrab.com.example.util.AuthCodeGenerator; // 인증 코드 생성기
import BlueCrab.com.example.util.EmailTemplateUtils; // 이메일 템플릿 유틸리티
import BlueCrab.com.example.util.PasswordResetRedisUtil; // Redis 유틸리티
import BlueCrab.com.example.util.PasswordResetRateLimitUtil; // 레이트 리밋 유틸리티

@RestController
@RequestMapping("/api/password-reset")
public class PasswordResetController {
    // ========== 의존성 주입 ==========

    @Autowired // @Autowired : 의존성 주입 어노테이션
    private PasswordResetRedisUtil redisUtil;
    // 비밀번호 재설정 관련 Redis 유틸리티
    
    @Autowired
    private PasswordResetRateLimitUtil rateLimitUtil;
    // 비밀번호 재설정 관련 레이트 리밋 유틸리티
    
    @Autowired
    private EmailService emailService;
    // 이메일 발송 서비스
    
    @Autowired
    private AuthCodeGenerator authCodeGenerator;
    // 인증 코드 생성기
    
    @Autowired
    private EmailTemplateUtils emailTemplateUtils;
    // 이메일 템플릿 유틸리티

    @PostMapping("/send-reset-email")
    public ResponseEntity<?> sendResetEmail(@RequestBody Map<String, String> request) {
        String irtToken = request.get("irtToken");
        
        if (irtToken == null || irtToken.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "IRT token is required"));
        }
        
        try {
            Map<String, Object> irtData = redisUtil.getIRTData(irtToken);
            if (irtData == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "SESSION_EXPIRED"));
            }
            
            String email = (String) irtData.get("email");
            String irtLock = (String) irtData.get("lock");
            
            if (!redisUtil.isLockValid(email, irtLock)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "SESSION_REPLACED"));
            }
            
            Long retryAfter = rateLimitUtil.getRateLimitRetryAfter(email);
            if (retryAfter != null) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", String.valueOf(retryAfter))
                        .body(Map.of("error", "Rate limit exceeded", "retryAfter", retryAfter));
            }
            
            String authCode = authCodeGenerator.generateAuthCode();
            redisUtil.saveEmailCode(email, authCode, irtData, irtLock);
            
            sendPasswordResetEmail(email, authCode);
            
            rateLimitUtil.updateRateLimit(email);
            
            return ResponseEntity.ok(Map.of("ok", true));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }
    
    private void sendPasswordResetEmail(String email, String code) throws Exception {
        String emailContent = emailTemplateUtils.createAuthCodeEmailTemplate("사용자", code, 5);
        
        emailService.sendMIMEMessage(
            "noreply@bluecrab.com", 
            email, 
            "BlueCrab 비밀번호 재설정 인증코드", 
            emailContent
        );
    }
}