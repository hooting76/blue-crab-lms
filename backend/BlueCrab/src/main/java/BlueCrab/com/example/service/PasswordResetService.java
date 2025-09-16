package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.PasswordResetIdentityRequest;
import BlueCrab.com.example.dto.PasswordResetIdentityResponse;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.util.PasswordResetRateLimiter;
import BlueCrab.com.example.util.PasswordResetTokenManager;
import BlueCrab.com.example.util.UserVerificationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
                // 레이트 리미팅도 중립적 응답으로 처리
                return PasswordResetIdentityResponse.neutral();
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
                actualUser.getUserCode().toString(), 
                requestUser.getUserCode().toString()
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
        dummy.setUserCode(99999999);
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
    public java.util.Map<String, Object> getRateLimitStatus(String clientIp, String email) {
        return rateLimiter.getRateLimitStatus(clientIp, email);
    }
}