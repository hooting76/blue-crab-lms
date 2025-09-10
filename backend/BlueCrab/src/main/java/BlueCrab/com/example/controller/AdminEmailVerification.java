// 작업자 : 성태준
// 관리자 계정용 메일 인증 시스템 컨트롤러

package BlueCrab.com.example.controller;

// ========== 임포트 구문 ==========

// ========== Spring Framework 관련 임포트 ==========
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

// ========== spring Data Redis 관련 임포트 ==========
import org.springframework.data.redis.core.RedisTemplate;

// ========== Java 표준 라이브러리 관련 임포트 ==========
import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

// ========== 외부 라이브러리 임포트 ==========
import lombok.extern.slf4j.Slf4j;

// ========= 프로젝트 내부 임포트 ==========
import BlueCrab.com.example.dto.AuthResponse;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.dto.AuthCodeVerifyRequest;
import BlueCrab.com.example.service.EmailService;
import BlueCrab.com.example.util.RequestUtils;

@RestController
@Slf4j
public class AdminEmailVerification {
    // ========== 상수 정의 ==========
    private static final int AUTH_CODE_LENGTH = 6;
    private static final int AUTH_CODE_EXPIRY_MINUTES = 5;
    private static final String AUTH_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String AUTH_SESSION_PREFIX = "admin_email_auth_code:";
    
    // ========== 의존성 주입 ==========
    @Autowired
    private EmailService emailService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private UserTblRepository userTblRepository;
    @Value("${app.domain}")
    private String domain;

    // 인증코드 생성용 시큐어 랜덤 객체
    private static final SecureRandom secureRandom = new SecureRandom();
    // SecureRandom secureRandom : 시큐어 랜덤 객체로, 인증코드 생성에 사용됨.
    // new SecureRandom() : 시큐어 랜덤 객체를 생성. 보안성이 높은 난수 생성에 사용됨.

    // ========== 1. 인증코드 요청 (임시토큰 기반) ==========
    @PostMapping("/api/admin/email-auth/request")
    // 관리자 이메일 인증코드 요청 엔드포인트
    public ResponseEntity<AuthResponse> requestAdminEmailAuthCode
                                        (Authentication authentication, HttpServletRequest request) {
            // ResponseEntity<AuthResponse>  : 인증 요청 결과를 담아 반환하는 HTTP 응답 객체
            // requestAdminEmailAuthCode : 관리자 이메일 인증코드 요청 처리 메서드
            // Authentication authentication : 현재 인증된 사용자의 정보를 담고 있는 객체
            // HttpServletRequest request : 현재 HTTP 요청에 대한 정보를 담고 있는 객체 
        String email = authentication.getName(); // 임시토큰에서 추출
        String clientIp = RequestUtils.getClientIpAddress(request); // 요청 IP 
        Optional<UserTbl> adminOpt = userTblRepository.findByUserEmail(email); // DB 조회

        if (!adminOpt.isPresent()) {
            // 관리자 계정이 없으면
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse("관리자 계정을 찾을 수 없습니다."));
            // 401 응답을 반환
            // AuthResponse : 응답 메시지를 담는 DTO
        } // if 문 끝

        String authCode = generateAuthCode();
        // generateAuthCode() : 인증코드 생성 메서드 호출
        LocalDateTime codeCreatedTime = LocalDateTime.now();
        // 인증코드 생성 시간 기록
        // LocalDateTime.now() : 현재 날짜와 시간을 가져오는 메서드 호출
        saveAuthCodeToRedis(email, authCode, clientIp, codeCreatedTime);
        // saveAuthCodeToRedis : Redis에 인증코드와 관련 정보 저장 메서드 호출
        String emailContent = createEmailCodeContent(adminOpt.get().getUserName(), authCode);
        // createEmailCodeContent : 이메일 본문 생성 메서드 호출
        // adminOpt.get().getUserName() : 관리자 이름 조회
        emailService.sendMIMEMessage
        ("bluecrabacademy@gmail.com", email, "관리자 이메일 인증코드", emailContent);
        // emailService.sendMIMEMessage : 이메일 발송 메서드 호출
        // (발신자 주소, 수신자 주소, 제목, 본문 내용 전달)의 구조

        log.info("관리자 인증코드 발송 - Email: {}, Code: {}", email, authCode);
        // 인증코드 발송 로그 기록 (실제 운영시 코드 노출 주의)

        return ResponseEntity.ok(new AuthResponse
        ("인증코드가 발송되었습니다. %d분 이내에 인증을 완료해주세요.", AUTH_CODE_EXPIRY_MINUTES));
        // 200 OK 응답과 함께 발송 완료 메시지 반환
    } // requestAdminEmailAuthCode 메서드 끝

    // ========== 2. 인증코드 검증 ==========
    @PostMapping("/api/admin/email-auth/verify")
    // 인증코드 검증 요청 엔드포인트
    public ResponseEntity<AuthResponse> verifyAdminEmailAuthCode
        (@RequestBody AuthCodeVerifyRequest req, 
        Authentication authentication, 
        HttpServletRequest request) {
        
        String email = authentication.getName();
        String clientIp = RequestUtils.getClientIpAddress(request);
        AuthCodeData codeData = getAuthCodeDataFromRedis(email);
        
        if (codeData == null || isCodeExpired(codeData.getCodeCreatedTime())) {
            // 인증코드가 없거나 만료된 경우
            cleanupAuthCode(email);
            // Redis에서 인증코드 데이터 삭제

            log.info("Admin email auth failed - session expired for email: {}", email);
            // 인증 세션 만료 로그 기록
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse("인증 세션이 만료되었습니다."));
            // 401 응답 반환
            // AuthResponse : 응답 메시지를 담는 DTO
        } // if 인증코드가 없거나 만료된 경우 끝

        if (!codeData.getClientIp().equals(clientIp)) {
            // 요청 IP가 저장된 IP와 다른 경우
            cleanupAuthCode(email);
            // Redis에서 인증코드 데이터 삭제
            log.info("Admin email auth failed - IP mismatch for email: {}", email);
            // IP 불일치 로그 기록
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new AuthResponse("IP가 일치하지 않습니다."));
            // 403 응답 반환
            // AuthResponse : 응답 메시지를 담는 DTO
        } // if 요청 IP가 저장된 IP와 다른 경우 끝

        if (codeData.getAuthCode().equalsIgnoreCase(req.getAuthCode().trim())) {
            // 인증코드가 일치하는 경우
            cleanupAuthCode(email);
            // Redis에서 인증코드 데이터 삭제
            log.info("Admin email auth succeeded for email: {}", email);
            // 인증 성공 로그 기록
            // 여기서 정식 토큰 발행 로직 호출(별도 서비스/담당자 구현)
            return ResponseEntity.ok(new AuthResponse("이메일 인증 성공!"));
            // 200 OK 응답과 함께 인증 성공 메시지 반환
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse("인증코드가 올바르지 않습니다."));
        }
    }

    // 인증코드 생성 (숫자+영문 대문자 6자리)
    private String generateAuthCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < AUTH_CODE_LENGTH; i++) {
            code.append(AUTH_CODE_CHARACTERS.charAt(secureRandom.nextInt(AUTH_CODE_CHARACTERS.length())));
        }
        return code.toString();
    }

    // Redis에 인증코드 저장
    private void saveAuthCodeToRedis(String email, String authCode, String clientIp, LocalDateTime codeCreatedTime) {
        String key = AUTH_SESSION_PREFIX + email;
        redisTemplate.opsForHash().putAll(key, Map.of(
            "authCode", authCode,
            "clientIp", clientIp,
            "codeCreatedTime", codeCreatedTime.toString()
        ));
        redisTemplate.expire(key, AUTH_CODE_EXPIRY_MINUTES * 60, TimeUnit.SECONDS);
    }

    // Redis에서 인증코드 데이터 조회
    private AuthCodeData getAuthCodeDataFromRedis(String email) {
        String key = AUTH_SESSION_PREFIX + email;
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        if (map.isEmpty()) return null;
        String authCode = (String) map.get("authCode");
        String clientIp = (String) map.get("clientIp");
        String createdTimeStr = (String) map.get("codeCreatedTime");
        if (authCode == null || clientIp == null || createdTimeStr == null) return null;
        LocalDateTime codeCreatedTime = LocalDateTime.parse(createdTimeStr);
        return new AuthCodeData(authCode, clientIp, codeCreatedTime);
    }

    // 인증코드 만료 검사
    private boolean isCodeExpired(LocalDateTime codeCreatedTime) {
        return LocalDateTime.now().isAfter(codeCreatedTime.plusMinutes(AUTH_CODE_EXPIRY_MINUTES));
    }

    // 인증코드 삭제
    private void cleanupAuthCode(String email) {
        String key = AUTH_SESSION_PREFIX + email;
        redisTemplate.delete(key);
    }

    // 인증코드 이메일 본문 생성
    private String createEmailCodeContent(String userName, String authCode) {
        StringBuilder content = new StringBuilder();
        content.append("<div>");
        content.append("<h2>관리자 이메일 인증코드</h2>");
        content.append("<p>안녕하세요, <strong>").append(userName).append("</strong>님!</p>");
        content.append("<p>아래 인증코드를 입력해 주세요.</p>");
        content.append("<div style='font-size:2em; font-weight:bold; color:#2a5ada; margin:16px 0;'>").append(authCode).append("</div>");
        content.append("<ul>");
        content.append(String.format("<li>인증코드는 %d분간만 유효합니다.</li>", AUTH_CODE_EXPIRY_MINUTES));
        content.append("<li>인증을 요청한 기기에서만 인증이 가능합니다.</li>");
        content.append("<li>타인과 공유하지 마세요.</li>");
        content.append("</ul>");
        content.append("<p><small>본 메일은 자동 발송되었습니다.</small></p>");
        content.append("</div>");
        return content.toString();
    }

    // 인증코드 데이터 클래스
    private static class AuthCodeData {
        private final String authCode;
        private final String clientIp;
        private final LocalDateTime codeCreatedTime;
        public AuthCodeData(String authCode, String clientIp, LocalDateTime codeCreatedTime) {
            this.authCode = authCode;
            this.clientIp = clientIp;
            this.codeCreatedTime = codeCreatedTime;
        }
        public String getAuthCode() { return authCode; }
        public String getClientIp() { return clientIp; }
        public LocalDateTime getCodeCreatedTime() { return codeCreatedTime; }
    }
}