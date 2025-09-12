// 작업자 : 성태준
// 관리자 계정용 메일 인증 시스템 컨트롤러

package BlueCrab.com.example.controller;

// ========== 임포트 구문 ==========

// ========== Spring Framework 관련 임포트 ==========
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
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000L; // 15분
    private static final long REFRESH_TOKEN_EXPIRATION = 24 * 60 * 60 * 1000L; // 24시간
    
    // ========== 의존성 주입 ==========
    @Autowired
    private EmailService emailService;
    // EmailService : 이메일 발송 기능을 담당하는 서비스 클래스
    // Spring 컨테이너가 관리하는 EmailService bean이 자동으로 주입됨

    @Autowired
    private BlueCrab.com.example.service.EmailVerificationService emailVerificationService;
    // EmailVerificationService : 이메일 인증 기능을 담당하는 서비스 클래스
    // Spring 컨테이너가 관리하는 EmailVerificationService bean이 자동으로 주입됨

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    // RedisTemplate<String, Object> : Redis와 상호작용하는 템플릿 클래스
    // String : Redis 키의 타입
    // Object : Redis 값의 타입

    @Autowired
    private AdminTblRepository adminTblRepository;
    // UserTblRepository : 사용자 테이블과 상호작용하는 JPA 리포지토리 인터페이스
    // Spring Data JPA가 자동으로 구현체를 생성하여 주입함

    @Autowired
    private BlueCrab.com.example.config.AppConfig appConfig;
    // AppConfig : 애플리케이션 설정 정보를 담는 설정 클래스

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
        
        try {
            log.info("Admin email auth code request started");
            log.info("AppConfig status: {}", appConfig != null ? "OK" : "NULL");
            log.info("EmailVerificationService status: {}", emailVerificationService != null ? "OK" : "NULL");
            
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
        // sessionToken.substring(7) : "Bearer " 부분 제거
        log.info("Extracted JWT token: {}", jwt.substring(0, Math.min(20, jwt.length())) + "...");
        
        String email = null;
        try {
            email = emailVerificationService.extractAdminIdFromSessionToken(jwt);
            log.info("Email extraction result: {}", email);
        } catch (Exception e) {
            log.error("Email extraction failed with exception: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("토큰 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
        // JWT 토큰에서 관리자 이메일 추출 (EmailVerificationService 사용)
        // 본 컨트롤러의 핵심이자 근간, 첫 단추.

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
        
    String emailContent = createEmailCodeContent(admin.getName(), authCode);
        // 이메일 본문 내용 생성
        
        emailService.sendMIMEMessage("bluecrabacademy@gmail.com", email, "관리자 이메일 인증코드", emailContent);
        // 이메일 발송
        // (발신자, 수신자, 제목, 본문 내용)

        log.info("successfully sent admin email auth code - Email: {}", email);
        // 인증코드 발송 로그
        
        return ResponseEntity.ok(new AuthResponse("인증코드가 발송되었습니다. %d분 이내에 인증을 완료해주세요.", AUTH_CODE_EXPIRY_MINUTES));
        // 200 응답 반환
        // AuthResponse : 응답 메시지를 담는 DTO
        
        } catch (Exception e) {
            log.error("Unexpected error in requestAdminEmailAuthCode: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse("서버 내부 오류가 발생했습니다: " + e.getMessage()));
        }
    } // requestAdminEmailAuthCode 메서드 끝

    // ========== 2. 인증코드 검증 ==========
    @PostMapping("/api/admin/email-auth/verify")
    // 인증코드 검증 요청 엔드포인트
    public ResponseEntity<AuthResponse> verifyAdminEmailAuthCode
        (@RequestBody AuthCodeVerifyRequest req,
        @RequestHeader("Authorization") String sessionToken,
        HttpServletRequest request) {
        // 인증코드 검증 요청에서 임시토큰(JWT)을 헤더로 받아 adminId 추출
        if (sessionToken == null || !sessionToken.startsWith("Bearer ")) {
            // 임시토큰이 없거나 유효하지 않은 경우
            log.info("Admin email auth verify failed - missing or invalid session token");
            // 임시토큰 누락 로그 기록
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse("임시토큰이 필요합니다."));
                // 401 응답 반환
                // AuthResponse : 응답 메시지를 담는 DTO
        } // if 임시토큰이 없거나 유효하지 않은 경우 끝

        String jwt = sessionToken.substring(7);
        // "Bearer "를 제거한 JWT 토큰 문자열
        String email = emailVerificationService.extractAdminIdFromSessionToken(jwt);
        // 이메일 인증 토큰에서 관리자 이메일 추출

        if (email == null) {
            // 이메일이 추출되지 않는 경우
            log.info("Admin email auth verify failed - invalid session token");
            // 임시토큰 유효하지 않음 로그 기록
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse("유효하지 않은 임시토큰입니다."));
                // 401 응답 반환
                // AuthResponse : 응답 메시지를 담는 DTO
        } // if 이메일이 추출되지 않는 경우 끝

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
            // Redis에서 인증코드 데이터 삭제
            log.info("Admin email auth succeeded for email: {}", email);
            // 인증 성공 로그 기록

            // 관리자 정보 조회
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
            int adminSys = admin.getAdminSys() != null ? admin.getAdminSys() : 0;
            // adminSys : 관리자 시스템 권한 (null인 경우 0으로 기본 설정)
            // admin.getAdminSys() : 관리자 시스템 권한 조회
            // != null ? ... : ... : null인 경우 기본값 0 설정
            SecretKey key = Keys.hmacShaKeyFor(appConfig.getJwt().getSecret().getBytes());
            // SecretKey key : JWT 서명에 사용할 비밀 키
            // Keys.hmacShaKeyFor(appConfig.getJwt().getSecret().getBytes()) : AppConfig에서 비밀 키 생성
            long now = System.currentTimeMillis();
            // now : 현재 시간 (밀리초 단위)
            String accessToken = Jwts.builder()
                // accessToken : 액세스 토큰 문자열
                .claim("adminId", admin.getAdminId())
                // adminId 클레임 설정
                .claim("adminSys", adminSys)
                // adminSys 클레임 설정
                .claim("adminName", admin.getName())
                // adminName 클레임 설정
                .claim("type", "access")
                // type 클레임 설정
                .subject(admin.getAdminId())
                // 토큰 주체(subject) 설정
                .issuedAt(new java.util.Date(now))
                // 발급 시간 설정
                .expiration(new java.util.Date(now + ACCESS_TOKEN_EXPIRATION))
                // 만료 시간 설정
                .signWith(key, Jwts.SIG.HS256)
                // HS256 알고리즘으로 서명
                .compact();
                // 토큰 생성
            log.info("Generated access token for adminId: {}", admin.getAdminId());
            // 액세스 토큰 발급 로그

            String refreshToken = Jwts.builder()
                // refreshToken : 리프레시 토큰 문자열
                .claim("adminId", admin.getAdminId())
                // adminId 클레임 설정
                .claim("adminSys", adminSys)
                // adminSys 클레임 설정
                .claim("adminName", admin.getName())
                // adminName 클레임 설정
                .claim("type", "refresh")
                // type 클레임 설정
                .subject(admin.getAdminId())
                // 토큰 주체(subject) 설정
                .issuedAt(new java.util.Date(now))
                // 발급 시간 설정
                .expiration(new java.util.Date(now + REFRESH_TOKEN_EXPIRATION))
                // 만료 시간 설정
                .signWith(key, Jwts.SIG.HS256)
                // HS256 알고리즘으로 서명
                .compact();
                // 토큰 생성
            log.info("Generated refresh token for adminId: {}", admin.getAdminId());
            // 리프레시 토큰 발급 로그

            Map<String, Object> responseData = new java.util.HashMap<>();
            // responseData : 응답 데이터 맵
            responseData.put("accessToken", accessToken);
            // 액세스 토큰
            responseData.put("refreshToken", refreshToken);
            // 리프레시 토큰
            responseData.put("adminSys", adminSys);
            // 관리자 시스템 권한
            responseData.put("expiresIn", ACCESS_TOKEN_EXPIRATION / 1000);
            // 액세스 토큰 만료 시간(초 단위)
            responseData.put("adminId", admin.getAdminId());
            // 관리자 ID(이메일)
            responseData.put("adminName", admin.getName());
            // 관리자 이름

            AuthResponse response = new AuthResponse("이메일 인증 성공! 토큰이 발급되었습니다.");
            // 인증 성공 응답 메시지
            response.setData(responseData);
            // 응답 데이터 설정
            return ResponseEntity.ok(response);
            // 200 응답 반환

        } // if 인증코드가 일치하는 경우 끝
        else {
            // 인증코드가 일치하지 않는 경우
            log.info("Admin email auth failed - invalid code for email: {}", email);
            // 인증코드 불일치 로그 기록
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthResponse("인증코드가 올바르지 않습니다."));
                // 401 응답 반환
                // AuthResponse : 응답 메시지를 담는 DTO
        } // else 끝

    } // verifyAdminEmailAuthCode 메서드 끝

    // ========== 내부 유틸리티 메서드 ==========

    // 3. 인증코드 생성(숫자+영문 대문자 6자리)
    private String generateAuthCode() {
        // String generateAuthCode() : 인증코드를 생성하여 반환하는 메서드
        StringBuilder code = new StringBuilder();
        // StringBuilder code : 인증코드를 효율적으로 생성하기 위한 StringBuilder 객체
        for (int i = 0; i < AUTH_CODE_LENGTH; i++) {
            // AUTH_CODE_LENGTH : 인증코드 길이, 상수 정의 참조
            code.append(AUTH_CODE_CHARACTERS.charAt(secureRandom.nextInt(AUTH_CODE_CHARACTERS.length())));
            // AUTH_CODE_CHARACTERS : 인증코드에 사용할 문자 집합
            // secureRandom.nextInt(...) : 시큐어 랜덤으로 인덱스 생성
        } // for 루프 끝
        return code.toString();
        // 생성된 인증코드를 문자열로 반환

    } // generateAuthCode 메서드 끝

    // 4. Redis에 인증코드 저장
    private void saveAuthCodeToRedis(String email, String authCode, String clientIp, LocalDateTime codeCreatedTime) {
        // (메일, 인증코드, 클라이언트 IP, 생성 시각)
        String key = AUTH_SESSION_PREFIX + email;
        // String key : Redis에 저장할 키
        // AUTH_SESSION_PREFIX + email : 접두사 + 사용자 이메일
        redisTemplate.opsForHash().putAll(key, Map.of(
            // Redis에 해시맵 형태로 데이터 저장
            // putAll : 여러 필드를 한꺼번에 저장
            "authCode", authCode,   // 인증코드
            "clientIp", clientIp,   // 클라이언트 IP
            "codeCreatedTime", codeCreatedTime.toString()   // 생성 시각 (문자열로 저장)
        )); 
        redisTemplate.expire(key, AUTH_CODE_EXPIRY_MINUTES * 60, TimeUnit.SECONDS);
        // 키 만료 시간 설정 (5분)
        log.debug("Saved auth code data to Redis for email: {}", email);
        // 저장 로그 기록
    } // saveAuthCodeToRedis 메서드 끝

    // 5. Redis에서 인증코드 데이터 조회
    private AuthCodeData getAuthCodeDataFromRedis(String email) {
        // AuthCodeData : 인증코드 데이터 클래스
        // String email : 사용자 이메일
        String key = AUTH_SESSION_PREFIX + email;
        // String key : Redis에서 조회할 키
        // AUTH_SESSION_PREFIX + email : 접두사 + 사용자 이메일
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        // Redis에서 해시맵 형태로 데이터 조회
        if (map.isEmpty()) return null;
        // 데이터가 없으면 null 반환

        String authCode = (String) map.get("authCode");
        // 조회된 인증코드
        String clientIp = (String) map.get("clientIp");
        // 조회된 클라이언트 IP
        String createdTimeStr = (String) map.get("codeCreatedTime");
        // 조회된 생성 시각 (문자열)

        if (authCode == null || clientIp == null || createdTimeStr == null) return null;
        // 필수 데이터가 없으면 null 반환
        LocalDateTime codeCreatedTime = LocalDateTime.parse(createdTimeStr);
        // 문자열을 LocalDateTime으로 변환
        return new AuthCodeData(authCode, clientIp, codeCreatedTime);
        // AuthCodeData 객체 생성하여 반환

    } // getAuthCodeDataFromRedis 메서드 끝

    // 6. 인증코드 만료 검사
    private boolean isCodeExpired(LocalDateTime codeCreatedTime) {
        // LocalDateTime codeCreatedTime : 인증코드 생성 시각
        log.debug("Checking code expiry - Created: {}, Now: {}", codeCreatedTime, LocalDateTime.now());
        // 인증코드 생성 시간과 현재 시간 비교
        return LocalDateTime.now().isAfter(codeCreatedTime.plusMinutes(AUTH_CODE_EXPIRY_MINUTES));
        // 현재 시간이 생성 시간 + 만료 시간(5분) 이후인지 검사
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
        // 인증코드
        private final String clientIp;
        // 클라이언트 IP
        private final LocalDateTime codeCreatedTime;
        // 인증코드 생성 시각
        public AuthCodeData(String authCode, String clientIp, LocalDateTime codeCreatedTime) {
            // 생성자
            this.authCode = authCode;   // 인증코드
            this.clientIp = clientIp;   // 클라이언트 IP
            this.codeCreatedTime = codeCreatedTime;  // 생성 시각
        } // 생성자 끝
        public String getAuthCode() { return authCode; }    // 인증코드 반환
        public String getClientIp() { return clientIp; }    // 클라이언트 IP 반환
        public LocalDateTime getCodeCreatedTime() { return codeCreatedTime; }   // 생성 시각 반환

    } // AuthCodeData 클래스 끝

} // AdminEmailVerification 클래스 끝