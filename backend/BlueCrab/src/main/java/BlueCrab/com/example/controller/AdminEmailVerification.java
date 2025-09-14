// 작업자 : 성태준
// 관리자 계정용 메일 인증 시스템 컨트롤러

package BlueCrab.com.example.controller;

// ========== 임포트 구문 ==========

// ========== Spring Framework 관련 임포트 ==========
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

// ========== Java 표준 라이브러리 관련 임포트 ==========
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

// ========== 외부 라이브러리 임포트 ==========
import lombok.extern.slf4j.Slf4j;

// ========= 프로젝트 내부 임포트 ==========
import BlueCrab.com.example.dto.AuthResponse;
import BlueCrab.com.example.entity.AdminTbl;
import BlueCrab.com.example.repository.AdminTblRepository;
import BlueCrab.com.example.dto.AuthCodeVerifyRequest;
import BlueCrab.com.example.service.EmailService;
import BlueCrab.com.example.util.RequestUtils;
import BlueCrab.com.example.util.AuthCodeGenerator;
import BlueCrab.com.example.util.RedisAuthDataManager;
import BlueCrab.com.example.util.AuthCodeValidator;
import BlueCrab.com.example.util.EmailTemplateUtils;
import BlueCrab.com.example.util.AdminTokenValidator;
import BlueCrab.com.example.util.AdminJwtTokenBuilder;
import BlueCrab.com.example.util.AdminAuthResponseBuilder;

@RestController
@Slf4j
public class AdminEmailVerification {
    // ========== 상수 정의 ==========
    private static final int AUTH_CODE_EXPIRY_MINUTES = 5;
    // 수정 시 AuthCodeVerifyRequest.java의 @Size 어노테이션 내용도 함께 수정 필요
    private static final String ADMIN_AUTH_KEY_PREFIX = "admin_email_auth_code:";
    
    // ========== 의존성 주입 ==========
    @Autowired
    private EmailService emailService;
    // EmailService : 이메일 발송 기능을 담당하는 서비스 클래스

    @Autowired
    private AdminTblRepository adminTblRepository;
    // 관리자 테이블과 상호작용하는 JPA 리포지토리 인터페이스

    @Value("${app.domain}")
    // @Value : application.properties 또는 application.yml 파일에서 설정 값을 주입받는 어노테이션
    private String domain;
    // domain : 애플리케이션의 도메인 이름을 저장하는 필드

    // ========== 유틸리티 클래스 의존성 주입 ==========
    @Autowired
    private AuthCodeGenerator authCodeGenerator;
    // 인증 코드 생성 유틸리티
    
    @Autowired
    private RedisAuthDataManager redisAuthDataManager;
    // Redis 인증 데이터 관리 유틸리티
    
    @Autowired
    private AuthCodeValidator authCodeValidator;
    // 인증 코드 검증 유틸리티
    
    @Autowired
    private EmailTemplateUtils emailTemplateUtils;
    // 이메일 템플릿 생성 유틸리티
    
    @Autowired
    private AdminTokenValidator adminTokenValidator;
    // 관리자 토큰 검증 유틸리티
    
    @Autowired
    private AdminJwtTokenBuilder adminJwtTokenBuilder;
    // 관리자 JWT 토큰 생성 유틸리티
    
    @Autowired
    private AdminAuthResponseBuilder adminAuthResponseBuilder;
    // 관리자 인증 응답 생성 유틸리티

    // ========== 1. 인증코드 요청 (임시토큰 기반) ==========
    @PostMapping("/api/admin/email-auth/request")
    public ResponseEntity<AuthResponse> requestAdminEmailAuthCode(
        @RequestHeader("Authorization") String sessionToken,
        HttpServletRequest request) {
        
        try {
            log.info("Admin email auth code request started");
            
            // AdminTokenValidator를 사용한 토큰 검증 및 이메일 추출
            // Bearer 토큰 형식 검증, JWT 파싱, 관리자 이메일 추출을 한번에 처리
            String email = adminTokenValidator.extractEmailFromBearerToken(sessionToken);
            if (email == null) {
                log.info("Admin email auth failed - invalid or missing session token");
                // AdminAuthResponseBuilder를 사용한 에러 응답 생성
                // 일관된 응답 형식과 HTTP 상태 코드 관리
                return adminAuthResponseBuilder.buildErrorResponse("유효하지 않은 임시토큰입니다.", HttpStatus.UNAUTHORIZED);
            }

            // 클라이언트 IP 추출
            String clientIp = RequestUtils.getClientIpAddress(request);
            
            // 관리자 정보 조회
            Optional<AdminTbl> adminOpt = adminTblRepository.findByAdminId(email);
            if (!adminOpt.isPresent()) {
                log.info("Admin email auth failed - admin account not found for email: {}", email);
                // AdminAuthResponseBuilder를 사용한 에러 응답 생성
                return adminAuthResponseBuilder.buildErrorResponse("관리자 계정을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED);
            }

            AdminTbl admin = adminOpt.get();
            
            // AuthCodeGenerator를 사용한 인증코드 생성
            // 기존 MailAuthCheckController와 동일한 생성 로직 재사용 (영문 대문자 + 숫자 6자리)
            String authCode = authCodeGenerator.generateAuthCode();
            LocalDateTime codeCreatedTime = LocalDateTime.now();

            // RedisAuthDataManager를 사용한 Redis 저장
            // 기존 MailAuthCheckController와 동일한 Redis 관리 로직 재사용
            // 관리자용 전용 키 프리픽스 사용으로 일반 사용자와 구분
            RedisAuthDataManager.AuthCodeData authData = new RedisAuthDataManager.AuthCodeData(
                authCode, email, clientIp, codeCreatedTime, "admin_session_" + System.currentTimeMillis());
            redisAuthDataManager.saveAuthData(ADMIN_AUTH_KEY_PREFIX, email, authData, AUTH_CODE_EXPIRY_MINUTES);
            
            // EmailTemplateUtils를 사용한 이메일 내용 생성
            // 관리자 전용 이메일 템플릿 사용 (일반 사용자용과 차별화된 디자인)
            String emailContent = emailTemplateUtils.createAuthCodeEmailTemplate(admin.getName(), authCode, AUTH_CODE_EXPIRY_MINUTES);
            
            // 이메일 발송
            emailService.sendMIMEMessage("bluecrabacademy@gmail.com", email, "관리자 이메일 인증코드", emailContent);

            log.info("Successfully sent admin email auth code - Email: {}", email);
            
            // AdminAuthResponseBuilder를 사용한 성공 응답 생성
            // 표준화된 응답 형식으로 인증코드 발송 완료 메시지 반환
            return adminAuthResponseBuilder.buildCodeSentResponse(AUTH_CODE_EXPIRY_MINUTES);
            
        } catch (Exception e) {
            log.error("Unexpected error in requestAdminEmailAuthCode: {}", e.getMessage(), e);
            // AdminAuthResponseBuilder를 사용한 예외 상황 에러 응답 생성
            // 일관된 에러 처리와 응답 형식 보장
            return adminAuthResponseBuilder.buildErrorResponse("서버 내부 오류가 발생했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    } // requestAdminEmailAuthCode 메서드 끝

    // ========== 2. 인증코드 검증 ==========
    @PostMapping("/api/admin/email-auth/verify")
    public ResponseEntity<AuthResponse> verifyAdminEmailAuthCode(
        @RequestBody AuthCodeVerifyRequest req,
        @RequestHeader("Authorization") String sessionToken,
        HttpServletRequest request) {
        
        try {
            // AdminTokenValidator를 사용한 토큰 검증 및 이메일 추출
            // requestAdminEmailAuthCode와 동일한 토큰 처리 로직 재사용
            String email = adminTokenValidator.extractEmailFromBearerToken(sessionToken);
            if (email == null) {
                log.info("Admin email auth verify failed - invalid or missing session token");
                // AdminAuthResponseBuilder를 사용한 에러 응답 생성
                return adminAuthResponseBuilder.buildErrorResponse("유효하지 않은 임시토큰입니다.", HttpStatus.UNAUTHORIZED);
            }

            String clientIp = RequestUtils.getClientIpAddress(request);
            
            // RedisAuthDataManager를 사용한 인증 데이터 조회
            // 관리자 전용 키 프리픽스로 Redis에서 인증 데이터 조회
            RedisAuthDataManager.AuthCodeData codeData = redisAuthDataManager.getAuthData(ADMIN_AUTH_KEY_PREFIX, email);
            
            // AuthCodeValidator를 사용한 만료 검사
            // 기존 MailAuthCheckController와 동일한 만료 검증 로직 재사용
            if (codeData == null || authCodeValidator.isAuthCodeExpired(codeData.getCodeCreatedTime())) {
                redisAuthDataManager.cleanup(ADMIN_AUTH_KEY_PREFIX, email);
                log.info("Admin email auth failed - session expired for email: {}", email);
                // AdminAuthResponseBuilder를 사용한 만료 에러 응답 생성
                return adminAuthResponseBuilder.buildErrorResponse("인증 세션이 만료되었습니다.", HttpStatus.UNAUTHORIZED);
            }

            // IP 일치 검사 (보안 강화)
            if (!codeData.getClientIp().equals(clientIp)) {
                redisAuthDataManager.cleanup(ADMIN_AUTH_KEY_PREFIX, email);
                log.info("Admin email auth failed - IP mismatch for email: {}", email);
                // AdminAuthResponseBuilder를 사용한 보안 에러 응답 생성
                return adminAuthResponseBuilder.buildErrorResponse("IP가 일치하지 않습니다.", HttpStatus.FORBIDDEN);
            }

            // 인증코드 검증
            if (codeData.getAuthCode().equalsIgnoreCase(req.getAuthCode().trim())) {
                redisAuthDataManager.cleanup(ADMIN_AUTH_KEY_PREFIX, email);
                log.info("Admin email auth succeeded for email: {}", email);

                // 관리자 정보 조회
                Optional<AdminTbl> adminOpt = adminTblRepository.findByAdminId(email);
                if (!adminOpt.isPresent()) {
                    log.info("Admin email auth failed - admin account not found for email: {}", email);
                    // AdminAuthResponseBuilder를 사용한 계정 없음 에러 응답 생성
                    return adminAuthResponseBuilder.buildErrorResponse("관리자 계정을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED);
                }

                AdminTbl admin = adminOpt.get();
                
                // AdminJwtTokenBuilder를 사용한 토큰 생성
                // 관리자 전용 JWT 토큰 쌍(액세스/리프레시) 생성
                // 기존의 복잡한 JWT 생성 로직을 유틸리티로 단순화
                AdminJwtTokenBuilder.TokenPair tokens = adminJwtTokenBuilder.buildTokenPair(admin);
                
                // AdminAuthResponseBuilder를 사용한 성공 응답 생성
                // 토큰 정보와 관리자 정보를 포함한 표준화된 성공 응답 반환
                return adminAuthResponseBuilder.buildSuccessResponse(tokens, admin);

            } else {
                log.info("Admin email auth failed - invalid code for email: {}", email);
                // AdminAuthResponseBuilder를 사용한 인증코드 불일치 에러 응답 생성
                return adminAuthResponseBuilder.buildErrorResponse("인증코드가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error in verifyAdminEmailAuthCode: {}", e.getMessage(), e);
            return adminAuthResponseBuilder.buildErrorResponse("서버 내부 오류가 발생했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    } // verifyAdminEmailAuthCode 메서드 끝

} // AdminEmailVerification 클래스 끝