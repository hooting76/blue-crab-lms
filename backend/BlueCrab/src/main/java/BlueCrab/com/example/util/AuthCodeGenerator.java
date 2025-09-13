// 작업자 : 성태준
// 인증 코드 생성 유틸리티 클래스
// 이메일 인증에 사용되는 랜덤 인증 코드와 세션 ID 생성 기능을 담당

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.UUID;

/* 인증 코드 및 세션 ID 생성을 담당하는 유틸리티 클래스
 * 
 * 주요 기능:
 * - 기본 6자리 인증 코드 생성 (영문 대문자 + 숫자)
 * - 고유한 인증 세션 ID 생성
 * 
 * 설계 원칙:
 * - 단일 책임: 코드 생성에만 집중
 * - 보안성: SecureRandom 사용으로 예측 불가능한 코드 생성
 * - 재사용성: 다양한 컨트롤러에서 활용 가능
 */
@Component
@Slf4j
public class AuthCodeGenerator {
    
    // ========== 인증 코드 생성 설정 상수 ==========
    private static final String AUTH_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    // 인증 코드 문자 집합 (영문 대문자 + 숫자)
    private static final int AUTH_CODE_LENGTH = 6;
    // 인증 코드 길이 (6자리)
    
    // ========== 보안 랜덤 객체 ==========
    private static final SecureRandom secureRandom = new SecureRandom();
    // SecureRandom: 암호학적으로 안전한 난수 생성기
    // 일반 Random보다 예측하기 어려운 랜덤 값 생성
    
    public String generateAuthCode() {
        StringBuilder codeBuilder = new StringBuilder();
        
        // 6자리 랜덤 문자 선택
        for (int i = 0; i < AUTH_CODE_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(AUTH_CODE_CHARACTERS.length());
            char randomChar = AUTH_CODE_CHARACTERS.charAt(randomIndex);
            codeBuilder.append(randomChar);
        }
        
        return codeBuilder.toString();
    }
    
    public String generateAuthSessionId(String userEmail) {
        // 입력값 검증
        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.error("User email is null or empty for session ID generation");
            throw new IllegalArgumentException("사용자 이메일이 유효하지 않습니다.");
        }
        
        String trimmedEmail = userEmail.trim();
        long currentTimeMillis = System.currentTimeMillis();
        String uuid8Chars = UUID.randomUUID().toString().substring(0, 8);
        
        String sessionId = trimmedEmail + ":" + currentTimeMillis + ":" + uuid8Chars;
        
        log.debug("Generated auth session ID for user: {} (ID length: {})", trimmedEmail, sessionId.length());
        
        return sessionId;
    }
}
