package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.PasswordResetIdentityRequest;
import BlueCrab.com.example.dto.PasswordResetIdentityResponse;
import BlueCrab.com.example.dto.PasswordResetCodeVerifyRequest;
import BlueCrab.com.example.dto.PasswordResetCodeVerifyResponse;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.model.PasswordResetCodeData;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.util.PasswordResetRateLimiter;
import BlueCrab.com.example.util.PasswordResetTokenManager;
import BlueCrab.com.example.util.UserVerificationUtils;
import BlueCrab.com.example.util.PasswordResetRedisUtil;
import BlueCrab.com.example.util.SHA256Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Map;

/**
 * 비밀번호 재설정 서비스
 * 4단계 비밀번호 재설정 플로우의 비즈니스 로직을 처리
 *
 * 처리 단계:
 * 1. 본인확인 (이메일 + 학번 + 이름 + 전화번호)
 * 2. 이메일 발송 (IRT 기반)
 * 3. 코드 검증 (Replace-on-new 방식)
 * 4. 비밀번호 변경 (RT 단일 사용)
 *
 * 보안 원칙:
 * - Replace-on-new: 새 본인확인 시 이전 토큰들 무효화
 * - 중립적 응답: 계정 존재 여부 비노출
 * - 레이트 리미팅: IP/이메일별 요청 제한
 * - 상수시간 비교: 타이밍 어택 방지
 * - 4개 필드 검증: 이메일, 학번, 이름, 전화번호 모두 일치 필요
 */
@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    @Autowired
    private UserTblRepository userTblRepository;

    @Autowired
    private PasswordResetTokenManager tokenManager;

    @Autowired
    private PasswordResetRateLimiter rateLimiter;

    @Autowired
    private UserVerificationUtils userVerificationUtils;

    @Autowired
    private PasswordResetRedisUtil redisUtil;

    /**
     * 1단계: 본인확인 처리
     * 이메일, 학번, 이름, 전화번호를 검증하여 본인 여부를 확인
     *
     * @param request 본인확인 요청 데이터 (이메일, 학번, 이름, 전화번호)
     * @param clientIp 클라이언트 IP 주소
     * @return 본인확인 응답 (성공/실패 무관하게 중립적 메시지)
     */
    public PasswordResetIdentityResponse verifyIdentity(PasswordResetIdentityRequest request, String clientIp) {
        String email = request.getEmail();
        
        try {
            // 1. 레이트 리미팅 확인
            if (!rateLimiter.isIdentityVerificationAllowed(clientIp, email)) {
                logger.warn("Password reset rate limit exceeded - IP: {}, Email: {}", clientIp, email);
                // 레이트 리미팅은 구별 가능한 응답으로 처리
                return PasswordResetIdentityResponse.rateLimitExceeded();
            }

            // 2. 시도 기록 (성공/실패 무관하게 기록)
            rateLimiter.recordIdentityVerificationAttempt(clientIp, email);

            // 3. 사용자 정보 검증 (상수시간 비교)
            boolean isValid = verifyUserIdentityConstantTime(request);

            if (isValid) {
                // 4. 성공 시 락 토큰 생성/교체 (Replace-on-new)
                String lockToken = tokenManager.generateAndReplaceLockToken(email);
                
                // 5. 사용자 정보 조회 (검증 후이므로 안전)
                Optional<UserTbl> userOpt = userTblRepository.findByUserEmailAndUserCodeAndUserNameAndUserPhone(
                    email, request.getUserCode(), request.getUserName(), request.getUserPhone()
                );
                
                if (userOpt.isPresent()) {
                    UserTbl user = userOpt.get();
                    
                    // 6. IRT 토큰 생성
                    String irtToken = tokenManager.generateIRT(email, user.getUserIdx(), lockToken);
                    
                    // 7. 실패 카운터 초기화
                    rateLimiter.clearIdentityVerificationFailures(email);
                    
                    // 8. 이메일 마스킹 처리
                    String maskedEmail = userVerificationUtils.maskEmail(email);
                    
                    logger.info("Password reset identity verification successful - Email: {}", email);
                    return PasswordResetIdentityResponse.success(irtToken, maskedEmail);
                }
            }

            // 9. 실패 시 실패 카운터 증가
            rateLimiter.recordIdentityVerificationFailure(email);
            
            logger.info("Password reset identity verification failed - Email: {}", email);
            return PasswordResetIdentityResponse.neutral();

        } catch (Exception e) {
            logger.error("Error during password reset identity verification - Email: {}", email, e);
            
            // 오류 시에도 실패로 기록
            rateLimiter.recordIdentityVerificationFailure(email);
            
            // 예외 발생 시에도 중립적 응답
            return PasswordResetIdentityResponse.neutral();
        }
    }

    /**
     * 사용자 신원 정보를 상수시간으로 검증
     * 타이밍 어택을 방지하기 위해 항상 동일한 시간이 소요되도록 구현
     *
     * @param request 본인확인 요청 데이터 (4개 필드)
     * @return 검증 결과
     */
    private boolean verifyUserIdentityConstantTime(PasswordResetIdentityRequest request) {
        try {
            // 1. 데이터베이스에서 사용자 조회
            Optional<UserTbl> userOpt = userTblRepository.findByUserEmailAndUserCodeAndUserNameAndUserPhone(
                request.getEmail(), request.getUserCode(), request.getUserName(), request.getUserPhone()
            );

            // 2. 상수시간 비교를 위한 더미 데이터 준비
            UserTbl actualUser = userOpt.orElse(createDummyUser());
            UserTbl requestUser = createUserFromRequest(request);

            // 3. 상수시간 비교 수행 (4개 필드)
            boolean emailMatch = constantTimeEquals(actualUser.getUserEmail(), requestUser.getUserEmail());
            boolean codeMatch = constantTimeEquals(
                actualUser.getUserCode(),
                requestUser.getUserCode()
            );
            boolean nameMatch = constantTimeEquals(actualUser.getUserName(), requestUser.getUserName());
            boolean phoneMatch = constantTimeEquals(actualUser.getUserPhone(), requestUser.getUserPhone());

            // 4. 실제 사용자가 존재하고 모든 필드가 일치하는지 확인
            return userOpt.isPresent() && emailMatch && codeMatch && nameMatch && phoneMatch;

        } catch (Exception e) {
            logger.error("Error in constant-time user verification", e);
            return false;
        }
    }

    /**
     * 상수시간 문자열 비교
     * 타이밍 어택을 방지하기 위해 문자열 길이와 관계없이 항상 동일한 시간 소요
     *
     * @param a 첫 번째 문자열
     * @param b 두 번째 문자열
     * @return 비교 결과
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        // 길이를 맞춰서 비교 (더 긴 문자열 기준)
        int maxLength = Math.max(a.length(), b.length());
        String aPadded = String.format("%-" + maxLength + "s", a);
        String bPadded = String.format("%-" + maxLength + "s", b);

        int result = 0;
        for (int i = 0; i < maxLength; i++) {
            result |= aPadded.charAt(i) ^ bPadded.charAt(i);
        }

        return result == 0 && a.length() == b.length();
    }

    /**
     * 더미 사용자 객체 생성 (상수시간 비교용)
     */
    private UserTbl createDummyUser() {
        UserTbl dummy = new UserTbl();
        dummy.setUserEmail("dummy@example.com");
        dummy.setUserCode("99999999");
        dummy.setUserName("더미사용자");
        dummy.setUserPhone("01099999999");
        return dummy;
    }

    /**
     * 요청 데이터로부터 사용자 객체 생성 (상수시간 비교용)
     */
    private UserTbl createUserFromRequest(PasswordResetIdentityRequest request) {
        UserTbl user = new UserTbl();
        user.setUserEmail(request.getEmail());
        user.setUserCode(request.getUserCode());
        user.setUserName(request.getUserName());
        user.setUserPhone(request.getUserPhone());
        return user;
    }

    /**
     * 디버깅을 위한 레이트 리미팅 상태 조회
     *
     * @param clientIp 클라이언트 IP
     * @param email 사용자 이메일
     * @return 레이트 리미팅 상태
     */
    public Map<String, Object> getRateLimitStatus(String clientIp, String email) {
        return rateLimiter.getRateLimitStatus(clientIp, email);
    }

    /**
     * 4단계: 비밀번호 변경 처리
     * RT 토큰을 검증한 후 새로운 비밀번호로 변경
     * 
     * 보안 특징:
     * - RT 토큰 단일 사용 보장 (GETDEL 사용)
     * - 락 토큰 일치 확인 (Replace-on-new)
     * - SHA-256으로 비밀번호 해싱 (기존 시스템 호환성)
     * - 사용된 토큰들 정리
     *
     * @param resetToken RT(Reset Token)
     * @param newPassword 새 비밀번호 (이미 정규식 검증 완료)
     * @throws IllegalArgumentException 토큰 무효/만료/락 불일치 시
     */
    public void changePassword(String resetToken, String newPassword) {
        logger.info("비밀번호 변경 처리 시작");

        try {
            // 1. RT 토큰을 GETDEL로 가져오기 (단일 사용 보장)
            Map<String, Object> resetSession = redisUtil.getAndDeleteResetSession(resetToken);
            if (resetSession == null) {
                logger.warn("RT 토큰이 존재하지 않음 또는 만료됨: {}", resetToken);
                throw new IllegalArgumentException("SESSION_EXPIRED");
            }

            String email = (String) resetSession.get("email");
            String userId = (String) resetSession.get("user_id");
            String rtLock = (String) resetSession.get("lock");

            // 2. 현재 락 토큰과 일치 확인 (Replace-on-new)
            String currentLock = redisUtil.getCurrentLock(email);
            if (currentLock == null || !currentLock.equals(rtLock)) {
                logger.warn("락 토큰 불일치. 다른 세션에서 재설정 진행 중: email={}", email);
                throw new IllegalArgumentException("SESSION_REPLACED");
            }

            // 3. 사용자 조회
            Optional<UserTbl> userOptional = userTblRepository.findByUserEmail(email);
            if (!userOptional.isPresent()) {
                logger.error("사용자를 찾을 수 없음: email={}", email);
                throw new IllegalArgumentException("USER_NOT_FOUND");
            }

            UserTbl user = userOptional.get();

            // 4. 비밀번호 해싱 후 저장 (SHA-256 사용)
            String hashedPassword = SHA256Util.hash(newPassword);
            user.setUserPw(hashedPassword);
            userTblRepository.save(user);

            // 5. 정리 작업 - 락 토큰 삭제 (선택적)
            redisUtil.deleteLock(email);

            logger.info("비밀번호 변경 완료: userId={}", userId);

        } catch (IllegalArgumentException e) {
            // 예상된 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            logger.error("비밀번호 변경 중 예상하지 못한 오류 발생", e);
            throw new RuntimeException("PASSWORD_CHANGE_FAILED", e);
        }
    }

    // ========== 3 단계 : 인증 코드 검증 작업자 : 성태준 ==========

    public PasswordResetCodeVerifyResponse verifyCode(PasswordResetCodeVerifyRequest request, String userIp) {
        String irtToken = request.getIrtToken();
        String authCode = request.getAuthCode();
        
        logger.info("코드 검증 시작: userIp={}", userIp);
        
        try {
            // 1. IRT 토큰 검증 및 이메일 추출
            Map<String, Object> tokenData = tokenManager.extractIRTTokenData(irtToken);
            if (tokenData == null) {
                logger.warn("유효하지 않은 IRT 토큰: userIp={}", userIp);
                return PasswordResetCodeVerifyResponse.sessionError();
            }
            
            String email = (String) tokenData.get("email");
            String sessionLockToken = (String) tokenData.get("sessionLockToken");
            
            // 2. 세션 락 검증
            if (!redisUtil.validateSessionLock(email, sessionLockToken)) {
                logger.warn("세션 락 검증 실패: email={}, userIp={}", email, userIp);
                return PasswordResetCodeVerifyResponse.sessionError();
            }
            
            // 3. 저장된 코드 데이터 조회 (IP 기반)
            PasswordResetCodeData codeData = redisUtil.getPasswordResetCodeData(userIp);
            if (codeData == null) {
                logger.warn("코드 데이터를 찾을 수 없음: userIp={}", userIp);
                return PasswordResetCodeVerifyResponse.expired();
            }
            
            // 4. 코드 만료 확인
            if (codeData.isExpired()) {
                logger.warn("코드 만료: userIp={}", userIp);
                redisUtil.invalidatePasswordResetCode(userIp);
                return PasswordResetCodeVerifyResponse.expired();
            }
            
            // 5. 최대 시도 횟수 확인
            if (codeData.isMaxAttemptsExceeded()) {
                logger.warn("최대 시도 횟수 초과: attempts={}, userIp={}", 
                           codeData.getVerificationAttempts(), userIp);
                redisUtil.invalidatePasswordResetCode(userIp);
                return PasswordResetCodeVerifyResponse.blocked();
            }
            
            // 6. 코드 검증
            if (!codeData.getCode().equals(authCode)) {
                // 시도 횟수 증가 (IP 기반)
                PasswordResetCodeData updatedData = redisUtil.incrementCodeVerificationAttempts(userIp);
                
                int remainingAttempts = 5;
                if (updatedData != null) {
                    remainingAttempts = Math.max(0, 5 - updatedData.getVerificationAttempts());
                }
                
                logger.warn("코드 불일치: email={}, attempts={}, remaining={}, userIp={}", 
                           email, updatedData != null ? updatedData.getVerificationAttempts() : -1, 
                           remainingAttempts, userIp);
                
                if (remainingAttempts <= 0) {
                    redisUtil.invalidatePasswordResetCode(userIp);
                    return PasswordResetCodeVerifyResponse.blocked();
                }
                
                return PasswordResetCodeVerifyResponse.failure(remainingAttempts, 300L); // 5분 = 300초
            }
            
            // 7. 검증 성공 - RT 토큰 생성
            String rtToken = tokenManager.generateRTToken(email, sessionLockToken);
            
            // 8. 코드 무효화
            redisUtil.invalidatePasswordResetCode(userIp);
            
            logger.info("코드 검증 성공: email={}, userIp={}", email, userIp);
            
            // 마스킹된 이메일 생성
            String maskedEmail = maskEmail(email);
            
            return PasswordResetCodeVerifyResponse.success(rtToken, maskedEmail);
            
        } catch (Exception e) {
            logger.error("코드 검증 중 오류 발생: userIp={}", userIp, e);
            return PasswordResetCodeVerifyResponse.failure(0, 0L);
        }
    }

    // ========== 3 단계 : 인증 코드 검증 작업자 : 성태준 끝 ==========

    /**
     * 이메일 마스킹 유틸리티 메서드
     * 
     * @param email 원본 이메일
     * @return 마스킹된 이메일
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return email; // 너무 짧으면 마스킹하지 않음
        }
        
        String maskedLocal = localPart.substring(0, 2) + "***";
        return maskedLocal + "@" + domain;
    }
}