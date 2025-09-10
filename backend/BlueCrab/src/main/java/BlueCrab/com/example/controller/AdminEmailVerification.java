// 작업자 : 성태준
// 관리자 계정용 메일 인증 시스템 컨트롤러

package BlueCrab.com.example.controller;

// ========== 임포트 구문 ==========

// ========== Spring Framework 관련 임포트 ==========
import org.springframework.security.core.Authentication;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
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
import BlueCrab.com.example.entity.AdminTbl;
import BlueCrab.com.example.repository.AdminTblRepository;
import BlueCrab.com.example.dto.AuthCodeVerifyRequest;
import BlueCrab.com.example.service.EmailService;
import BlueCrab.com.example.util.RequestUtils;

@RestController
@Slf4j
public class AdminEmailVerification {
    // ========== 상수 정의 ==========
    private static final int AUTH_CODE_LENGTH = 6;
    // 수정 시 AuthCodeVerifyRequest.java의 @Size 어노테이션 내용도 함께 수정 필요
    private static final int AUTH_CODE_EXPIRY_MINUTES = 5;
    private static final String AUTH_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String AUTH_SESSION_PREFIX = "admin_email_auth_code:";
    private static final String JWT_SECRET = "your-session-token-secret-key-should-be-at-least-256-bits-for-security";
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000L; // 15분
    private static final long REFRESH_TOKEN_EXPIRATION = 24 * 60 * 60 * 1000L; // 24시간
    
    // ========== 의존성 주입 ==========
    @Autowired
    private EmailService emailService;

    @Autowired
    private BlueCrab.com.example.service.EmailVerificationService emailVerificationService;
    // EmailService : 이메일 발송 기능을 담당하는 서비스 클래스
    // Spring 컨테이너가 관리하는 EmailService bean이 자동으로 주입됨

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    // RedisTemplate<String, Object> : Redis와 상호작용하는 템플릿 클래스
    // String : Redis 키의 타입
    // Object : Redis 값의 타입

    @Autowired
    private AdminTblRepository adminTblRepository;
    // UserTblRepository : 사용자 테이블과 상호작용하는 JPA 리포지토리 인터페이스
    // Spring Data JPA가 자동으로 구현체를 생성하여 주입함

    @Value("${app.domain}")
    // @Value : application.properties 또는 application.yml 파일에서 설정 값을 주입받는 어노테이션
    // "${app.domain}" : 설정 파일에서 "app.domain" 키에 해당하는 값을 주입받음
    private String domain;
    // domain : 애플리케이션의 도메인 이름을 저장하는 필드

    // 인증코드 생성용 시큐어 랜덤 객체
    private static final SecureRandom secureRandom = new SecureRandom();
    // SecureRandom secureRandom : 시큐어 랜덤 객체로, 인증코드 생성에 사용됨.
    // new SecureRandom() : 시큐어 랜덤 객체를 생성. 보안성이 높은 난수 생성에 사용됨.

    // ========== 1. 인증코드 요청 (임시토큰 기반) ==========
    @PostMapping("/api/admin/email-auth/request")
    // 관리자 이메일 인증코드 요청 엔드포인트
    public ResponseEntity<AuthResponse> requestAdminEmailAuthCode(
        // ResponseEntity<AuthResponse> : 인증 요청 결과를 담아 반환하는 HTTP 응답 객체
        // requestAdminEmailAuthCode : 관리자 이메일 인증코드 요청 처리 메서드
        @RequestHeader("Authorization") String sessionToken,
        HttpServletRequest request) {
        // sessionToken: "Bearer <JWT>" 형식일 수 있으므로 "Bearer " 제거
        // HttpServletRequest request : 현재 HTTP 요청에 대한 정보를 담고 있는 객체
        if (sessionToken == null || !sessionToken.startsWith("Bearer ")) {
            // 임시토큰이 없거나 유효하지 않은 경우
            log.info("Admin email auth failed - missing or invalid session token");
            // 임시토큰 누락 로그 기록
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse("임시토큰이 필요합니다."));
                // 401 응답 반환
                // AuthResponse : 응답 메시지를 담는 DTO
        } // if 임시토큰이 없거나 유효하지 않은 경우 끝

        String jwt = sessionToken.substring(7); 
        // EmailVerificationService에서 adminId 추출
        String email = emailVerificationService.extractAdminIdFromSessionToken(jwt);
        // 이메일 인증 토큰에서 관리자 이메일 추출

        if (email == null) {
            // 이메일이 추출되지 않는 경우
            log.info("email extraction failed");
            // 이메일 추출 실패 로그 기록
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse("유효하지 않은 임시토큰입니다."));
                // 401 응답 반환
                // AuthResponse : 응답 메시지를 담는 DTO
        } // if 이메일이 추출되지 않는 경우 끝

        String clientIp = RequestUtils.getClientIpAddress(request);
        // RequestUtils.getClientIpAddress(request) : 요청의 클라이언트 IP 주소를 가져옴
        Optional<AdminTbl> adminOpt = adminTblRepository.findByAdminId(email);
        // adminOpt : 이메일로 조회한 관리자 정보(Optional)
        // adminTblRepository.findByAdminId(email) : 이메일로 관리자 정보를 조회하는 메서드 호출

        if (!adminOpt.isPresent()) {
            // 관리자 계정이 없는 경우
            log.info("Admin email auth failed - admin account not found for email: {}", email);
            // 관리자 계정 없음 로그 기록
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse("관리자 계정을 찾을 수 없습니다."));
                // 401 응답 반환
                // AuthResponse : 응답 메시지를 담는 DTO
        } // if 관리자 계정이 없는 경우 끝

        AdminTbl admin = adminOpt.get();
        // admin : 조회된 관리자 정보
        // adminOpt.get() : Optional에서 실제 AdminTbl 객체를 꺼냄
        String authCode = generateAuthCode();
        // authCode : 생성된 인증코드
        // generateAuthCode() : 인증코드 생성 메서드 호출
        LocalDateTime codeCreatedTime = LocalDateTime.now();
        // codeCreatedTime : 인증코드 생성 시각
        // LocalDateTime.now() : 현재 시각을 가져옴

        saveAuthCodeToRedis(email, authCode, clientIp, codeCreatedTime);
        // Redis에 인증코드 저장
        // (이메일, 인증코드, 클라이언트 IP, 생성 시각)
        
    String emailContent = createEmailCodeContent(admin.getAdminName(), authCode);
        // 이메일 본문 내용 생성
        
        emailService.sendMIMEMessage("bluecrabacademy@gmail.com", email, "관리자 이메일 인증코드", emailContent);
        
        log.info("Admin email auth code sent - Email: {}, Code: {}", email, authCode);
        
        return ResponseEntity.ok(new AuthResponse("인증코드가 발송되었습니다. %d분 이내에 인증을 완료해주세요.", AUTH_CODE_EXPIRY_MINUTES));
    } // requestAdminEmailAuthCode 메서드 끝

    // ========== 2. 인증코드 검증 ==========
    @PostMapping("/api/admin/email-auth/verify")
    // 인증코드 검증 요청 엔드포인트
    public ResponseEntity<AuthResponse> verifyAdminEmailAuthCode
        (@RequestBody AuthCodeVerifyRequest req, 
        Authentication authentication, 
        HttpServletRequest request) {
        // ResponseEntity<AuthResponse> : 인증 검증 결과를 담아 반환하는 HTTP 응답 객체
        // verifyAdminEmailAuthCode : 관리자 이메일 인증코드 검증 처리 메서드
        // @RequestBody AuthCodeVerifyRequest req : 요청 본문에서 인증코드 정보를 담은 DTO
        // Authentication authentication : 현재 인증된 사용자의 정보를 담고 있는 객체
        // HttpServletRequest request : 현재 HTTP 요청에 대한 정보를 담고 있는 객체
        
        String email = authentication.getName();
        // authentication.getName() : 현재 인증된 사용자의 이름(이메일)을 가져옴
        String clientIp = RequestUtils.getClientIpAddress(request);
        // RequestUtils.getClientIpAddress(request) : 요청의 클라이언트 IP 주소를 가져옴
        AuthCodeData codeData = getAuthCodeDataFromRedis(email);
        // getAuthCodeDataFromRedis : Redis에서 인증코드 데이터 조회 메서드 호출
        
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
            log.info("Admin email auth succeeded for email: {}", email);

            // 관리자 정보 조회
            Optional<AdminTbl> adminOpt = adminTblRepository.findByAdminId(email);
            if (!adminOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("관리자 계정을 찾을 수 없습니다."));
            }
            AdminTbl admin = adminOpt.get();
            int adminSys = admin.getAdminSys() != null ? admin.getAdminSys() : 0;
            SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
            long now = System.currentTimeMillis();
            String accessToken = Jwts.builder()
                .claim("adminId", admin.getAdminId())
                .claim("adminSys", adminSys)
                .claim("name", admin.getAdminName())
                .claim("type", "access")
                .subject(admin.getAdminId())
                .issuedAt(new java.util.Date(now))
                .expiration(new java.util.Date(now + ACCESS_TOKEN_EXPIRATION))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

            String refreshToken = Jwts.builder()
                .claim("adminId", admin.getAdminId())
                .claim("adminSys", adminSys)
                .claim("name", admin.getAdminName())
                .claim("type", "refresh")
                .subject(admin.getAdminId())
                .issuedAt(new java.util.Date(now))
                .expiration(new java.util.Date(now + REFRESH_TOKEN_EXPIRATION))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

            Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("accessToken", accessToken);
            responseData.put("refreshToken", refreshToken);
            responseData.put("adminSys", adminSys);
            responseData.put("expiresIn", ACCESS_TOKEN_EXPIRATION / 1000);
            responseData.put("adminId", admin.getAdminId());
            responseData.put("name", admin.getAdminName());

            AuthResponse response = new AuthResponse("이메일 인증 성공! 토큰이 발급되었습니다.");
            response.setData(responseData);
            return ResponseEntity.ok(response);
        } else {
            // 인증코드가 일치하지 않는 경우
            log.info("Admin email auth failed - invalid code for email: {}", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse("인증코드가 올바르지 않습니다."));
        }

    } // verifyAdminEmailAuthCode 메서드 끝

    // ========== 내부 유틸리티 메서드 ==========

    // 3. 인증코드 생성(숫자+영문 대문자 6자리)
    private String generateAuthCode() {
        // String generateAuthCode() : 인증코드를 생성하여 반환하는 메서드
        StringBuilder code = new StringBuilder();
        // StringBuilder code : 인증코드를 효율적으로 생성하기 위한 StringBuilder 객체
        for (int i = 0; i < AUTH_CODE_LENGTH; i++) {
            code.append(AUTH_CODE_CHARACTERS.charAt(secureRandom.nextInt(AUTH_CODE_CHARACTERS.length())));
        }
        return code.toString();
    } // generateAuthCode 메서드 끝

    // 4. Redis에 인증코드 저장
    private void saveAuthCodeToRedis(String email, String authCode, String clientIp, LocalDateTime codeCreatedTime) {
        String key = AUTH_SESSION_PREFIX + email;
        redisTemplate.opsForHash().putAll(key, Map.of(
            "authCode", authCode,
            "clientIp", clientIp,
            "codeCreatedTime", codeCreatedTime.toString()
        ));
        redisTemplate.expire(key, AUTH_CODE_EXPIRY_MINUTES * 60, TimeUnit.SECONDS);
    } // saveAuthCodeToRedis 메서드 끝

    // 5. Redis에서 인증코드 데이터 조회
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
    } // getAuthCodeDataFromRedis 메서드 끝

    // 6. 인증코드 만료 검사
    private boolean isCodeExpired(LocalDateTime codeCreatedTime) {
    log.debug("Checking code expiry - Created: {}, Now: {}", codeCreatedTime, LocalDateTime.now());
        // 인증코드 생성 시간과 현재 시간 비교
        return LocalDateTime.now().isAfter(codeCreatedTime.plusMinutes(AUTH_CODE_EXPIRY_MINUTES));
    } // isCodeExpired 메서드 끝

    // 7. 인증코드 삭제
    private void cleanupAuthCode(String email) {
        String key = AUTH_SESSION_PREFIX + email;
        // String key : Redis에 저장할 키
        // AUTH_SESSION_PREFIX + email : 접두사 + 사용자 이메일
        redisTemplate.delete(key);
        // Redis에서 인증코드 데이터 삭제
    log.debug("Deleted auth code data from Redis for email: {}", email);
        // 삭제 로그 기록
    } // cleanupAuthCode 메서드 끝

    // 8. 인증코드 이메일 본문 생성
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
    } // createEmailCodeContent 메서드 끝

    // 9. 인증코드 데이터 클래스
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
    } // AuthCodeData 클래스 끝

} // AdminEmailVerification 클래스 끝