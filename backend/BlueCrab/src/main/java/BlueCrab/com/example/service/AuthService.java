package BlueCrab.com.example.service;

import BlueCrab.com.example.config.AppConfig;
import BlueCrab.com.example.dto.LoginRequest;
import BlueCrab.com.example.dto.LoginResponse;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 인증 및 로그인 처리를 담당하는 서비스 클래스
 * JWT 토큰 기반의 로그인 및 토큰 갱신 기능을 제공
 *
 * 주요 기능:
 * - 사용자 로그인 인증 및 JWT 토큰 발급
 * - 리프레시 토큰을 이용한 액세스 토큰 재발급
 * - 사용자 정보 조회 및 검증
 * - 보안 로깅 및 감사 추적
 *
 * 인증 플로우:
 * 1. 클라이언트에서 로그인 요청 (이메일 + 비밀번호)
 * 2. 사용자 정보 조회 및 존재 확인
 * 3. 비밀번호 해싱 검증
 * 4. JWT 액세스 토큰 및 리프레시 토큰 생성
 * 5. 토큰 정보와 사용자 정보를 포함한 응답 반환
 *
 * 토큰 재발급 플로우:
 * 1. 클라이언트에서 리프레시 토큰 전송
 * 2. 리프레시 토큰 유효성 및 타입 검증
 * 3. 토큰에서 사용자 정보 추출
 * 4. 사용자 존재 재확인
 * 5. 새로운 JWT 토큰 쌍 생성 및 반환
 *
 * 보안 고려사항:
 * - 비밀번호는 해싱하여 저장된 값과 비교 (평문 저장 금지)
 * - JWT 토큰에는 민감한 정보 포함하지 않음
 * - 로그인 실패 시 구체적인 실패 이유 노출하지 않음
 * - 모든 인증 시도는 로깅하여 감사 추적
 *
 * 예외 처리:
 * - 사용자 없음: RuntimeException("Invalid username or password")
 * - 비밀번호 불일치: RuntimeException("Invalid username or password")
 * - 토큰 검증 실패: RuntimeException("Invalid refresh token")
 * - 시스템 오류: RuntimeException("Authentication failed: " + message)
 *
 * 의존성:
 * - UserTblRepository: 사용자 정보 조회
 * - JwtUtil: JWT 토큰 생성 및 검증
 * - PasswordEncoder: 비밀번호 해싱 및 검증
 * - AppConfig: JWT 설정 정보 (토큰 만료시간 등)
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
public class AuthService {
    
    /**
     * 인증 서비스 전용 로거
     * 로그인 시도, 성공, 실패 등 모든 인증 관련 이벤트를 로깅
     * 보안 감사 및 모니터링에 사용
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    /**
     * 사용자 정보 리포지토리
     * 이메일로 사용자 조회 및 존재 확인에 사용
     */
    @Autowired
    private UserTblRepository userTblRepository;

    /**
     * JWT 유틸리티 클래스
     * 액세스 토큰 및 리프레시 토큰 생성, 검증에 사용
     */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 비밀번호 인코더
     * 비밀번호 해싱 및 검증에 사용
     * Spring Security의 BCryptPasswordEncoder 등 구현체 주입
     */
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 애플리케이션 설정 정보
     * JWT 토큰 만료시간 등 설정 값 가져오기에 사용
     */
    @Autowired
    private AppConfig appConfig;

    /**
     * 토큰 블랙리스트 서비스
     * 로그아웃 시 토큰 무효화에 사용
     */
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 사용자 로그인 인증 처리 메서드
     * 이메일과 비밀번호를 검증하여 JWT 토큰을 발급하는 핵심 인증 로직
     *
     * 처리 단계:
     * 1. 요청 데이터 추출 (이메일, 비밀번호)
     * 2. 사용자 정보 조회 (이메일로 DB 검색)
     * 3. 사용자 존재 확인 (없으면 예외 발생)
     * 4. 비밀번호 검증 (해싱된 비밀번호 비교)
     * 5. JWT 토큰 생성 (액세스 토큰 + 리프레시 토큰)
     * 6. 사용자 정보 객체 생성
     * 7. 토큰 만료시간 설정 정보 가져오기
     * 8. 응답 객체 생성 및 반환
     *
     * 보안 처리:
     * - 비밀번호는 즉시 검증 후 메모리에서 제거
     * - 로그인 실패 시 동일한 에러 메시지 반환 (정보 유출 방지)
     * - 모든 인증 시도는 debug/warn 레벨로 로깅
     *
     * @param loginRequest 로그인 요청 정보 (이메일, 비밀번호)
     * @return LoginResponse JWT 토큰과 사용자 정보 포함한 응답 객체
     * @throws RuntimeException 사용자 없음 또는 비밀번호 불일치 시
     * @throws RuntimeException 시스템 오류 (토큰 생성 실패 등) 시
     *
     * 사용 예시:
     * LoginRequest request = new LoginRequest("user@example.com", "password123");
     * LoginResponse response = authService.authenticate(request);
     * // response.getAccessToken(), response.getRefreshToken() 사용
     */
    public LoginResponse authenticate(LoginRequest loginRequest) {
        try {
            // 요청 데이터 추출
            String email = loginRequest.getUsername();
            String password = loginRequest.getPassword();
            
            // 로그인 시도 로깅 (보안 감사용)
            logger.debug("인증 시도: {}", email);
            
            // 단계 1: 사용자 조회
            // 이메일로 데이터베이스에서 사용자 정보 검색
            // 존재하지 않으면 예외 발생 (보안: 구체적인 이유 노출하지 않음)
            UserTbl user = userTblRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

            // 단계 2: 비밀번호 검증
            // 클라이언트에서 받은 평문 비밀번호를 해싱하여 저장된 값과 비교
            // matches() 메서드는 내부적으로 해싱 후 비교 수행
            if (!passwordEncoder.matches(password, user.getUserPw())) {
                logger.warn("비밀번호 불일치: {}", email);
                throw new RuntimeException("Invalid username or password");
            }

            // 단계 3: JWT 토큰 생성
            // 사용자 이메일과 ID를 기반으로 액세스 토큰 생성
            String accessToken = jwtUtil.generateAccessToken(user.getUserEmail(), user.getUserIdx());
            
            // 리프레시 토큰도 동일한 정보로 생성
            String refreshToken = jwtUtil.generateRefreshToken(user.getUserEmail(), user.getUserIdx());

            // 단계 4: 사용자 정보 객체 생성
            // 응답에 포함할 사용자 정보 생성 (userStudent 포함)
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getUserIdx(), user.getUserName(), user.getUserEmail(), user.getUserStudent()
            );

            // 단계 5: 토큰 만료시간 설정 가져오기
            // AppConfig에서 액세스 토큰 만료시간을 가져와 초 단위로 변환
            long expiresIn = appConfig.getJwt().getAccessTokenExpiration() / 1000;
            
            // 인증 성공 로깅
            logger.info("인증 성공: {}", email);
            
            // 최종 응답 객체 생성 및 반환
            return new LoginResponse(accessToken, refreshToken, expiresIn, userInfo);

        } catch (AuthenticationException e) {
            // Spring Security 관련 인증 예외 처리
            logger.error("인증 실패: {}", e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * JWT 리프레시 토큰을 사용하여 새로운 액세스 토큰 발급 메서드
     * 만료된 액세스 토큰을 갱신하기 위한 보안 메커니즘
     *
     * 처리 단계:
     * 1. 리프레시 토큰 유효성 검증
     * 2. 리프레시 토큰 타입 확인 (액세스 토큰과의 구분)
     * 3. 토큰에서 사용자 정보 추출 (이메일, 사용자 ID)
     * 4. 사용자 존재 재확인 (보안 강화)
     * 5. 새로운 JWT 토큰 쌍 생성
     * 6. 사용자 정보 객체 재생성
     * 7. 토큰 만료시간 설정 적용
     * 8. 새로운 토큰 정보 반환
     *
     * 보안 처리:
     * - 리프레시 토큰도 만료시간이 있으므로 주기적 갱신 유도
     * - 토큰에서 추출한 사용자 정보로 DB 재조회 (토큰 변조 방지)
     * - 새로운 토큰 쌍을 모두 재발급하여 토큰 탈취 위험 감소
     *
     * @param refreshToken 클라이언트에서 전송한 리프레시 토큰
     * @return LoginResponse 새로운 JWT 토큰과 사용자 정보 포함한 응답 객체
     * @throws RuntimeException 토큰 유효하지 않음, 사용자 없음, 토큰 생성 실패 시
     *
     * 사용 예시:
     * String refreshToken = "eyJhbGciOiJIUzI1NiJ9...";
     * LoginResponse response = authService.refreshToken(refreshToken);
     * // response.getAccessToken()를 새로운 액세스 토큰으로 사용
     */
    public LoginResponse refreshToken(String refreshToken) {
        try {
            // 단계 1: 리프레시 토큰 유효성 검증
            // 토큰이 유효한지, 만료되지 않았는지 확인
            if (!jwtUtil.isTokenValid(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }
            
            // 단계 2: 리프레시 토큰 타입 확인
            // 해당 토큰이 실제 리프레시 토큰인지 확인 (액세스 토큰과의 구분)
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }

            // 단계 3: 토큰에서 사용자 정보 추출
            // JWT 토큰에서 이메일과 사용자 ID 추출
            String username = jwtUtil.extractUsername(refreshToken);
            Integer userId = jwtUtil.extractUserId(refreshToken);
            
            // 토큰 갱신 요청 로깅
            logger.debug("토큰 갱신 요청: {}", username);

            // 단계 4: 사용자 존재 재확인
            // 토큰이 유효하더라도 DB에서 사용자 존재 재확인
            // 계정 삭제된 경우 등 보안 강화 목적
            UserTbl user = userTblRepository.findByUserEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // 단계 5: 새로운 액세스 토큰 생성
            // 추출한 사용자 정보로 새로운 액세스 토큰 생성
            String newAccessToken = jwtUtil.generateAccessToken(username, userId);
            
            // 새로운 리프레시 토큰도 생성 (토큰 로테이션)
            String newRefreshToken = jwtUtil.generateRefreshToken(username, userId);

            // 단계 6: 사용자 정보 객체 재생성 (userStudent 포함)
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getUserIdx(), user.getUserName(), user.getUserEmail(), user.getUserStudent()
            );

            // 단계 7: 토큰 만료시간 설정 가져오기
            long expiresIn = appConfig.getJwt().getAccessTokenExpiration() / 1000;
            
            // 토큰 갱신 성공 로깅
            logger.info("토큰 갱신 성공: {}", username);
            
            // 새로운 토큰 정보 반환
            return new LoginResponse(newAccessToken, newRefreshToken, expiresIn, userInfo);

        } catch (Exception e) {
            // 모든 예외를 포괄적으로 처리
            logger.error("토큰 갱신 실패: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed: " + e.getMessage());
        }
    }

    /**
     * 완전한 로그아웃 처리
     * - AccessToken을 블랙리스트에 추가하여 무효화
     * - RefreshToken을 블랙리스트에 추가하여 무효화
     * - 보안상 두 토큰을 모두 무효화하여 완전한 세션 종료 보장
     * 
     * @param accessToken 무효화할 액세스 토큰 (필수)
     * @param refreshToken 무효화할 리프레시 토큰 (필수)
     * @param username 사용자명
     */
    public void logout(String accessToken, String refreshToken, String username) {
        try {
            // 1. AccessToken 유효성 검증 및 블랙리스트 추가
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                try {
                    // AccessToken에서 만료 시간 추출
                    long accessTokenExpiration = jwtUtil.getTokenExpiration(accessToken);
                    tokenBlacklistService.addAccessTokenToBlacklist(accessToken, accessTokenExpiration);
                    logger.info("AccessToken 블랙리스트 추가 완료 - 사용자: {}", username);
                } catch (Exception e) {
                    logger.warn("AccessToken 블랙리스트 추가 실패 - 사용자: {}, 오류: {}", username, e.getMessage());
                    // AccessToken 처리 실패해도 로그아웃 계속 진행
                }
            } else {
                logger.warn("AccessToken이 null이거나 비어있음 - 사용자: {}", username);
            }
            
            // 2. RefreshToken 무효화 (필수)
            if (refreshToken != null && !refreshToken.trim().isEmpty()) {
                try {
                    // RefreshToken에서 사용자명 추출하여 검증
                    String refreshTokenUsername = jwtUtil.extractUsername(refreshToken);
                    if (jwtUtil.validateToken(refreshToken, refreshTokenUsername)) {
                        // RefreshToken 만료 시간 추출 및 블랙리스트 추가
                        long refreshTokenExpiration = jwtUtil.getTokenExpiration(refreshToken);
                        tokenBlacklistService.addRefreshTokenToBlacklist(refreshToken, refreshTokenExpiration);
                        logger.info("RefreshToken 블랙리스트 추가 완료 - 사용자: {}", username);
                    } else {
                        logger.warn("RefreshToken이 유효하지 않음 - 사용자: {}", username);
                        // 유효하지 않아도 로그아웃은 진행 (AccessToken 무효화 완료됨)
                    }
                } catch (Exception e) {
                    logger.warn("RefreshToken 무효화 실패 - 사용자: {}, 오류: {}", username, e.getMessage());
                    // RefreshToken 오류는 치명적이지 않음 (AccessToken 무효화가 완료됨)
                }
            } else {
                logger.error("RefreshToken이 null이거나 비어있음 - 사용자: {}", username);
                throw new RuntimeException("RefreshToken is required for complete logout");
            }
            
            // 3. 로그아웃 로그 기록
            logger.info("완전한 로그아웃 완료 - 사용자: {}, 시간: {}", username, java.time.Instant.now());
            
        } catch (Exception e) {
            logger.error("로그아웃 처리 중 오류 - 사용자: {}, 오류: {}", username, e.getMessage(), e);
            throw new RuntimeException("로그아웃 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * JWT 토큰에서 사용자명 추출
     * @param token JWT 토큰
     * @return 사용자명
     */
    public String extractUsernameFromToken(String token) {
        return jwtUtil.extractUsername(token);
    }
}
