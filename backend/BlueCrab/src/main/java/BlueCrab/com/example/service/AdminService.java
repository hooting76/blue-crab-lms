package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.AdminLoginRequest;
import BlueCrab.com.example.dto.AdminLoginResponse;
import BlueCrab.com.example.entity.AdminTbl;
import BlueCrab.com.example.repository.AdminTblRepository;
import BlueCrab.com.example.util.SHA256Util;
import BlueCrab.com.example.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 관리자 인증 및 관리 서비스 클래스
 * 관리자 로그인, 계정 상태 관리, 보안 정책 적용을 담당
 *
 * 주요 기능:
 * - 관리자 1차 인증 (ID/PW 검증)
 * - 계정 상태 확인 및 관리 (활성/정지/차단)
 * - 이메일 인증 토큰 발급
 * - 로그인 이력 관리
 * - 보안 정책 적용
 *
 * 보안 정책:
 * - BCrypt 패스워드 해싱
 * - 계정 상태별 접근 제어
 * - 정지 기간 자동 관리
 * - 로그인 시도 제한 (RedisService 연동)
 */
@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    @Autowired
    private AdminTblRepository adminRepository;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /**
     * 관리자 1차 인증 (ID/PW 검증 + 세션 토큰 발급)
     * 
     * @param request 관리자 로그인 요청 정보
     * @param clientIp 클라이언트 IP 주소
     * @return AdminLoginResponse 세션 토큰 포함 응답
     * @throws RuntimeException 인증 실패 시
     */
    public AdminLoginResponse authenticateAndGenerateSessionToken(AdminLoginRequest request, String clientIp) {
        try {
            // 로그인 시도 횟수 제한 확인
            String identifier = clientIp + ":" + request.getAdminId();
            redisService.checkLoginAttempts(identifier);

            // 1. 관리자 계정 조회 (adminId가 이메일 주소)
            Optional<AdminTbl> optionalAdmin = adminRepository.findByAdminId(request.getAdminId());
            if (!optionalAdmin.isPresent()) {
                logger.warn("Admin login failed - admin not found: {}", request.getAdminId());
                throw new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }

            AdminTbl admin = optionalAdmin.get();

            // 2. 계정 상태 검증
            Map<String, Object> statusValidation = redisService.validateAdminStatus(admin.getAdminId());
            String cachedStatus = (String) statusValidation.get("cachedStatus");
            
            // 정지된 계정 확인
            if ("suspended".equals(cachedStatus)) {
                Boolean isStillSuspended = (Boolean) statusValidation.get("isStillSuspended");
                if (Boolean.TRUE.equals(isStillSuspended)) {
                    Map<String, String> suspendInfo = (Map<String, String>) statusValidation.get("suspendInfo");
                    String reason = suspendInfo != null ? suspendInfo.get("reason") : "관리자에 의해 정지";
                    Long remainingTime = (Long) statusValidation.get("remainingTime");
                    long remainingMinutes = remainingTime != null ? remainingTime / (1000 * 60) : 0;
                    
                    logger.warn("Suspended admin login attempt: {}, remaining: {}min", request.getAdminId(), remainingMinutes);
                    throw new RuntimeException("계정이 정지되었습니다. 사유: " + reason + " (남은 시간: " + remainingMinutes + "분)");
                }
            }
            
            // 차단된 계정 확인
            if ("banned".equals(cachedStatus)) {
                logger.warn("Banned admin login attempt: {}", request.getAdminId());
                throw new RuntimeException("계정이 영구 차단되었습니다. 관리자에게 문의하세요.");
            }

            // 3. 비밀번호 검증 (SHA256)
            if (!SHA256Util.matches(request.getPassword(), admin.getPassword())) {
                logger.warn("Admin login failed - invalid password for: {}", request.getAdminId());
                throw new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }

            // 3. 세션 토큰 생성 (인증 코드 발급용)
            String sessionToken = emailVerificationService.generateSessionToken(admin.getAdminId());

            logger.info("Admin 1st authentication successful for: {}", admin.getAdminId());

            // 로그인 성공 시 시도 횟수 초기화
            redisService.clearLoginAttempts(identifier);

            // 4. 응답 생성 (세션 토큰 발급)
            return new AdminLoginResponse(
                "1차 인증이 완료되었습니다. 인증 코드 발급 버튼을 눌러 2차 인증을 진행해주세요.",
                sessionToken,
                600L, // 10분 (세션 토큰 유효시간)
                AdminLoginResponse.maskEmail(admin.getAdminId()),
                null  // 인증 URL 불필요
            );

        } catch (RuntimeException e) {
            // 비즈니스 로직 예외는 그대로 재발생
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during admin authentication", e);
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 관리자 ID로 관리자 정보 조회
     * 
     * @param adminId 관리자 ID
     * @return Optional<AdminTbl> 관리자 정보
     */
    public Optional<AdminTbl> findByAdminId(String adminId) {
        return adminRepository.findByAdminId(adminId);
    }

    /**
     * 관리자 로그인 이력 업데이트
     * 2차 인증 성공 후 호출
     * 
     * @param adminId 관리자 ID
     * @param loginIp 로그인 IP
     */
    public void updateLoginHistory(String adminId, String loginIp) {
        try {
            Optional<AdminTbl> optionalAdmin = adminRepository.findByAdminId(adminId);
            if (optionalAdmin.isPresent()) {
                AdminTbl admin = optionalAdmin.get();
                admin.setAdminLatest(java.time.LocalDateTime.now().toString());
                admin.setAdminLatestIp(loginIp);
                adminRepository.save(admin);
                
                logger.info("Login history updated for admin: {}, IP: {}", adminId, loginIp);
            }
        } catch (Exception e) {
            logger.error("Error updating login history for admin: " + adminId, e);
        }
    }

    /**
     * 이메일 인증 토큰 검증 및 JWT 발급
     * 2차 인증 처리
     * 
     * @param token 이메일 인증 토큰
     * @return JWT 토큰 정보
     * @throws RuntimeException 인증 실패 시
     */
    public Object verifyEmailAndGenerateJwt(String token) {
        try {
            // 1. 이메일 인증 시도 횟수 확인
            redisService.checkEmailVerificationAttempts(token);

            // 2. 토큰 유효성 검증 및 관리자 ID 추출
            String adminId = emailVerificationService.validateEmailVerificationToken(token);
            if (adminId == null) {
                throw new RuntimeException("유효하지 않거나 만료된 인증 토큰입니다.");
            }

            // 3. 관리자 정보 조회
            Optional<AdminTbl> optionalAdmin = adminRepository.findByAdminId(adminId);
            if (!optionalAdmin.isPresent()) {
                throw new RuntimeException("관리자 정보를 찾을 수 없습니다.");
            }

            AdminTbl admin = optionalAdmin.get();

            // 5. JWT 토큰 생성
            String accessToken = jwtUtil.generateAccessToken(admin.getAdminId(), admin.getAdminIdx());
            String refreshToken = jwtUtil.generateRefreshToken(admin.getAdminId(), admin.getAdminIdx());

            // 6. 이메일 토큰 블랙리스트 처리
            emailVerificationService.blacklistEmailVerificationToken(token);

            // 7. 로그인 이력 업데이트 (IP는 추후 추가 가능)
            updateLoginHistory(admin.getAdminId(), "127.0.0.1");

            logger.info("Admin 2nd authentication successful for: {}", admin.getAdminId());

            // 8. JWT 응답 생성
            Map<String, Object> jwtResponse = new HashMap<>();
            jwtResponse.put("accessToken", accessToken);
            jwtResponse.put("refreshToken", refreshToken);
            jwtResponse.put("adminInfo", Map.of(
                "adminId", admin.getAdminId(),
                "name", admin.getName(),
                "email", admin.getEmail()
            ));

            return jwtResponse;

        } catch (RuntimeException e) {
            // 비즈니스 로직 예외는 그대로 재발생
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during email verification", e);
            throw new RuntimeException("이메일 인증 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
