// 작성자 : 성태준
// 비밀번호 재설정 기능 통합 컨트롤러

package BlueCrab.com.example.controller;

// ========== Java 표준 라이브러리 ==========
import java.util.Map; // Map 인터페이스

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired; // 의존성 주입
import org.springframework.http.HttpStatus; // HTTP 상태 코드
import org.springframework.http.ResponseEntity; // HTTP 응답 객체
import org.springframework.web.bind.annotation.*; // REST 컨트롤러 및 매핑

// ========== Servlet API ==========
import javax.servlet.http.HttpServletRequest; // HTTP 요청 객체

// ========= 외부 라이브러리 ==========
import lombok.extern.slf4j.Slf4j; // 로깅

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.service.EmailService; // 이메일 발송 서비스
import BlueCrab.com.example.util.AuthCodeGenerator; // 인증 코드 생성기
import BlueCrab.com.example.util.EmailTemplateUtils; // 이메일 템플릿 유틸리티
import BlueCrab.com.example.util.PasswordResetRedisUtil; // Redis 유틸리티
import BlueCrab.com.example.util.AccountRecoveryRateLimiter; // 표준 레이트 리미터

@RestController
@RequestMapping("/api/password-reset")
@Slf4j // 로깅을 위한 어노테이션 추가
public class PasswordResetController {
    // ========== 의존성 주입 ==========

    @Autowired
    private PasswordResetRedisUtil redisUtil;
    // 비밀번호 재설정 관련 Redis 유틸리티
    
    @Autowired
    private AccountRecoveryRateLimiter rateLimiter;
    // 표준 레이트 리미터 (ID/PW 찾기 통합)
    
    @Autowired
    private EmailService emailService;
    // 이메일 발송 서비스
    
    @Autowired
    private AuthCodeGenerator authCodeGenerator;
    // 인증 코드 생성기
    
    @Autowired
    private EmailTemplateUtils emailTemplateUtils;
    // 이메일 템플릿 유틸리티

    @PostMapping("/send-reset-email") // 인증 코드 이메일 발송 엔드포인트 (IP 기반 제한)
    public ResponseEntity<?> sendResetEmail(@RequestBody Map<String, String> 
                request, HttpServletRequest httpRequest) {
        // ResponseEntity<?> : 다양한 응답 타입 지원
        // sendResetEmail : 비밀번호 재설정 이메일 발송 요청 처리
        // @RequestBody Map<String, String> request : 요청 본문에서 JSON 데이터를 Map으로 매핑
        // HttpServletRequest httpRequest : 클라이언트 IP, User-Agent 등 요청 메타데이터 접근용
        String irtToken = request.get("irtToken");
        // String irtToken : 요청에서 IRT 토큰 추출
        // request.get("irtToken") : JSON 요청에서 "irtToken" 키의 값을 가져옴  
        
        if (irtToken == null || irtToken.trim().isEmpty()) {
            // IRT 토큰이 없거나 빈 문자열인 경우
            log.error("IRT token is missing in the request");
            // 에러 로그 기록
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "IRT token is required"));
            // 400 Bad Request 응답 반환
        } // if 끝
        

        String clientIp = getClientIpAddress(httpRequest);
        // IP 주소 추출
        String userAgent = httpRequest.getHeader("User-Agent");
        // 클라이언트 User-Agent
        
        // ========== 주요 처리 로직 ==========

        // try-catch 블록으로 예외 처리
        try {
            // 1. IP 기반 레이트 리밋 확인 (AccountRecoveryRateLimiter 직접 사용)
            if (!rateLimiter.isAllowedForFindPassword(clientIp, userAgent)) {
                // 레이트 리밋 초과의 경우
                long waitTime = rateLimiter.getRemainingWaitTime(clientIp, "find_pw");
                // 남은 대기 시간 조회
                Long retryAfter = waitTime > 0 ? waitTime : 60L;
                // 최소 60초로 설정
                log.warn("Rate limit exceeded for IP: {}, User-Agent: {}. Retry after {} seconds", 
                        clientIp, userAgent, retryAfter);
                // 경고 로그 기록
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", String.valueOf(retryAfter))
                        .body(Map.of("error", "Rate limit exceeded", "retryAfter", retryAfter));
                // 429 Too Many Requests 응답 반환
            } // if 끝
            
            // 2. IRT 토큰 검증 (기존 로직 유지)
            Map<String, Object> irtData = redisUtil.getIRTData(irtToken);
            if (irtData == null) {
                // IRT 토큰이 없거나 만료된 경우
                rateLimiter.recordFailure(clientIp, "find_pw");
                // 실패 기록
                log.warn("Invalid or expired IRT token from IP: {}, User-Agent: {}", 
                        clientIp, userAgent);
                // 경고 로그 기록
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "SESSION_EXPIRED"));
                // 401 Unauthorized 응답 반환
            } // if 끝
            
            String email = (String) irtData.get("email"); // 사용자 이메일
            String irtLock = (String) irtData.get("lock"); // 세션 락
            
            // 3. 세션 락 검증 (기존 로직 유지)
            if (!redisUtil.isLockValid(email, irtLock)) {
                // 세션 락이 유효하지 않은 경우
                log.warn("Session lock mismatch for email: {}, IP: {}, User-Agent: {}", 
                        email, clientIp, userAgent);
                // 경고 로그 기록
                rateLimiter.recordFailure(clientIp, "find_pw");
                // 실패 기록
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "SESSION_REPLACED"));
                // 409 Conflict 응답 반환
            } // if 끝
            
            // 4. 인증 코드 생성 및 저장 (IP 기반)
            String authCode = authCodeGenerator.generateAuthCode();
            // 인증 코드 생성
            redisUtil.savePasswordResetCode(clientIp, email, authCode, irtData);
            // Redis에 인증 코드 저장
            
            // 5. 이메일 발송
            sendPasswordResetEmail(email, authCode);
            log.info("Password reset email sent to {} for IP: {}, User-Agent: {}", 
                    email, clientIp, userAgent);
            
            // 6. 성공 기록 (IP 기반)
            rateLimiter.recordSuccess(clientIp, "find_pw");
            log.info("Password reset request successful for IP: {}, User-Agent: {}", 
                    clientIp, userAgent);
            
            return ResponseEntity.ok(Map.of("ok", true));
            // 200 OK 응답 반환
            
        } catch (Exception e) {
            // 예외 처리
            rateLimiter.recordFailure(clientIp, "find_pw"); // 예외 발생 시도 실패로 기록
            log.error("Error processing password reset request for IP: {}, User-Agent: {}. Error: {}", 
                    clientIp, userAgent, e.getMessage());
            // 에러 로그 기록
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
            // 500 Internal Server Error 응답 반환
        } // try-catch 끝
    } // sendResetEmail 끝
    
    // ========== 보조 메서드 ==========
    private String getClientIpAddress(HttpServletRequest request) {
        // getClientIpAddress(...) : 클라이언트 IP 주소 추출
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        // X-Forwarded-For 헤더에서 IP 주소 추출
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // xForwardedFor 값이 null이 아니고, 빈 문자열이 아니고, "unknown"이 아닌 경우
            log.info("Client IP (X-Forwarded-For): {}", xForwardedFor.split(",")[0].trim());
            // 첫 번째 IP 주소 로그 기록
            return xForwardedFor.split(",")[0].trim();
            // 첫 번째 IP 주소 반환
        } // if 끝
        
        String xRealIp = request.getHeader("X-Real-IP");
        // X-Real-IP 헤더에서 IP 주소 추출
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            // xRealIp 값이 null이 아니고, 빈 문자열이 아니고, "unknown"이 아닌 경우
            log.info("Client IP (X-Real-IP): {}", xRealIp);
            // X-Real-IP 로그 기록
            return xRealIp;
            // X-Real-IP 반환
        }
        
        log.info("Client IP (RemoteAddr): {}", request.getRemoteAddr());
        // RemoteAddr 로그 기록

        return request.getRemoteAddr();
        // RemoteAddr 반환
    } // getClientIpAddress 끝
    
    private void sendPasswordResetEmail(String email, String code) throws Exception {
        // sendPasswordResetEmail(...) : 비밀번호 재설정 이메일 발송
        String emailContent = emailTemplateUtils.createAuthCodeEmailTemplate("사용자", code, 5);
        // 이메일 템플릿 생성 (5분 유효)
        
        log.debug("Sending password reset email to: {}", email);
        // 디버그 로그 기록

        emailService.sendMIMEMessage(
            "bluecrabacademy@gmail.com", 
            email, 
            "비밀번호 재설정 인증코드", 
            emailContent
        );
    } // sendPasswordResetEmail 끝
} // PasswordResetController 끝