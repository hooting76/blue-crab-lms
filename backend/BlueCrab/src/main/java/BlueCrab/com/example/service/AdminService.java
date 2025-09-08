package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.AdminLoginRequest;
import BlueCrab.com.example.dto.AdminLoginResponse;
import BlueCrab.com.example.entity.AdminTbl;
import BlueCrab.com.example.repository.AdminTblRepository;
import BlueCrab.com.example.util.SHA256Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /**
     * 관리자 1차 인증 (ID/PW 검증 + 이메일 인증 토큰 발급)
     * 
     * @param request 관리자 로그인 요청 정보
     * @return AdminLoginResponse 이메일 인증 토큰 포함 응답
     * @throws RuntimeException 인증 실패 시
     */
    public AdminLoginResponse authenticateAndGenerateEmailToken(AdminLoginRequest request) {
        try {
            // 1. 관리자 계정 조회 (adminId가 이메일 주소)
            Optional<AdminTbl> optionalAdmin = adminRepository.findByAdminId(request.getAdminId());
            if (!optionalAdmin.isPresent()) {
                logger.warn("Admin login failed - admin not found: {}", request.getAdminId());
                throw new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }

            AdminTbl admin = optionalAdmin.get();

            // 2. 계정 상태 확인 및 처리
            checkAndUpdateAccountStatus(admin);

            // 3. 비밀번호 검증 (SHA256)
            if (!SHA256Util.matches(request.getPassword(), admin.getPassword())) {
                logger.warn("Admin login failed - invalid password for: {}", request.getAdminId());
                throw new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }

            // 4. 이메일 인증 토큰 생성
            String emailToken = emailVerificationService.generateEmailVerificationToken(admin.getAdminId());

            // 5. 이메일 발송 (adminId가 이메일 주소이므로 바로 사용)
            boolean emailSent = emailVerificationService.sendVerificationEmail(
                admin.getAdminId(),  // adminId가 이메일 주소
                admin.getName(), 
                emailToken, 
                frontendBaseUrl
            );

            if (!emailSent) {
                logger.error("Failed to send verification email for admin: {}", admin.getAdminId());
                // 이메일 발송 실패 시에도 토큰은 발급 (수동 입력 가능하도록)
            }

            logger.info("Admin 1st authentication successful for: {}", admin.getAdminId());

            // 6. 응답 생성
            return new AdminLoginResponse(
                "이메일 인증이 필요합니다. " + AdminLoginResponse.maskEmail(admin.getAdminId()) + "으로 발송된 인증 링크를 확인해주세요.",
                emailToken,
                600, // 10분
                AdminLoginResponse.maskEmail(admin.getAdminId()),
                frontendBaseUrl + "/admin/verify-email"
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
     * 계정 상태 확인 및 업데이트
     * Redis 캐시를 활용하여 성능 최적화
     * 정지 기간 만료 시 자동 복구 처리
     * 
     * @param admin 관리자 정보
     * @throws RuntimeException 계정이 차단되었거나 정지 중인 경우
     */
    private void checkAndUpdateAccountStatus(AdminTbl admin) {
        String adminId = admin.getAdminId();
        String status = admin.getStatus();

        // Redis 캐시 상태 확인
        Map<String, Object> statusValidation = redisService.validateAdminStatus(adminId);
        String cachedStatus = (String) statusValidation.get("cachedStatus");
        boolean cacheHit = (Boolean) statusValidation.getOrDefault("cacheHit", false);

        logger.debug("Admin status validation - adminId: {}, cacheHit: {}, cachedStatus: {}, dbStatus: {}", 
            adminId, cacheHit, cachedStatus, status);

        // 캐시된 상태가 있고 DB 상태와 일치하는 경우 캐시 우선 사용
        String currentStatus = cacheHit && cachedStatus.equals(status) ? cachedStatus : status;

        // 영구 차단 계정
        if ("banned".equals(currentStatus)) {
            logger.warn("Banned admin attempted login: {}", adminId);
            // 차단 상태 캐시 (24시간)
            redisService.cacheAdminStatus(adminId, "banned", 24 * 60);
            throw new RuntimeException("계정이 영구적으로 차단되었습니다. 관리자에게 문의하세요.");
        }

        // 일시 정지 계정
        if ("suspended".equals(currentStatus)) {
            LocalDateTime suspendUntil = admin.getSuspendUntil();
            
            if (suspendUntil != null && suspendUntil.isAfter(LocalDateTime.now())) {
                // 정지 기간이 아직 남음
                long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), suspendUntil).toMinutes();
                
                // 정지 정보 Redis에 캐시
                long suspendUntilTimestamp = suspendUntil.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                redisService.cacheAdminStatus(adminId, "suspended", (int) remainingMinutes + 10); // 여유 시간 추가
                redisService.cacheAdminSuspendInfo(adminId, suspendUntilTimestamp, admin.getSuspendReason());
                
                logger.warn("Suspended admin attempted login: {}, remaining: {} minutes", adminId, remainingMinutes);
                throw new RuntimeException("계정이 정지되었습니다. " + remainingMinutes + "분 후에 다시 시도하세요. 사유: " + 
                    (admin.getSuspendReason() != null ? admin.getSuspendReason() : "관리자 조치"));
            } else {
                // 정지 기간 만료 - 자동 복구
                admin.setStatus("active");
                admin.setSuspendUntil(null);
                admin.setSuspendReason(null);
                adminRepository.save(admin);
                
                // Redis 캐시 업데이트
                redisService.clearAdminStatusCache(adminId);
                redisService.cacheAdminStatus(adminId, "active", 60); // 1시간 캐시
                
                logger.info("Admin account auto-reactivated: {}", adminId);
            }
        }

        // 활성 계정이 아닌 경우
        if (!"active".equals(admin.getStatus())) {
            logger.warn("Inactive admin attempted login: {}, status: {}", adminId, admin.getStatus());
            redisService.cacheAdminStatus(adminId, admin.getStatus(), 30); // 30분 캐시
            throw new RuntimeException("계정 상태가 올바르지 않습니다. 관리자에게 문의하세요.");
        }

        // 활성 계정 상태 캐시 (30분)
        if (!cacheHit || !"active".equals(cachedStatus)) {
            redisService.cacheAdminStatus(adminId, "active", 30);
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
     * 관리자 계정 정지
     * 
     * @param adminId 관리자 ID
     * @param suspendUntil 정지 종료 시간
     * @param reason 정지 사유
     * @return boolean 정지 처리 성공 여부
     */
    public boolean suspendAdmin(String adminId, LocalDateTime suspendUntil, String reason) {
        try {
            Optional<AdminTbl> optionalAdmin = adminRepository.findByAdminId(adminId);
            if (!optionalAdmin.isPresent()) {
                logger.warn("Admin not found for suspension: {}", adminId);
                return false;
            }

            AdminTbl admin = optionalAdmin.get();
            admin.setStatus("suspended");
            admin.setSuspendUntil(suspendUntil);
            admin.setSuspendReason(reason);
            adminRepository.save(admin);

            // Redis 캐시 업데이트
            long suspendUntilTimestamp = suspendUntil.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), suspendUntil).toMinutes();
            
            redisService.clearAdminStatusCache(adminId);
            redisService.cacheAdminStatus(adminId, "suspended", (int) remainingMinutes + 10); // 여유 시간 추가
            redisService.cacheAdminSuspendInfo(adminId, suspendUntilTimestamp, reason);

            logger.info("Admin suspended: {}, until: {}, reason: {}", adminId, suspendUntil, reason);
            return true;

        } catch (Exception e) {
            logger.error("Error suspending admin: " + adminId, e);
            return false;
        }
    }

    /**
     * 관리자 계정 영구 차단
     * 
     * @param adminId 관리자 ID
     * @param reason 차단 사유
     * @return boolean 차단 처리 성공 여부
     */
    public boolean banAdmin(String adminId, String reason) {
        try {
            Optional<AdminTbl> optionalAdmin = adminRepository.findByAdminId(adminId);
            if (!optionalAdmin.isPresent()) {
                logger.warn("Admin not found for banning: {}", adminId);
                return false;
            }

            AdminTbl admin = optionalAdmin.get();
            admin.setStatus("banned");
            admin.setSuspendReason(reason);
            adminRepository.save(admin);

            // Redis 캐시 업데이트
            redisService.clearAdminStatusCache(adminId);
            redisService.cacheAdminStatus(adminId, "banned", 24 * 60); // 24시간 캐시

            logger.info("Admin banned: {}, reason: {}", adminId, reason);
            return true;

        } catch (Exception e) {
            logger.error("Error banning admin: " + adminId, e);
            return false;
        }
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
                admin.setLastLoginAt(LocalDateTime.now());
                admin.setLastLoginIp(loginIp);
                adminRepository.save(admin);
                
                logger.info("Login history updated for admin: {}, IP: {}", adminId, loginIp);
            }
        } catch (Exception e) {
            logger.error("Error updating login history for admin: " + adminId, e);
        }
    }
}
