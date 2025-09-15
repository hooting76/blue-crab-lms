// 작업자 : 성태준
// 인증 코드 생성 유틸리티 클래스
// 이메일 인증에 사용되는 랜덤 인증 코드와 세션 ID 생성 기능을 담당

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.security.SecureRandom;
import java.util.UUID;

// ========== 외부 라이브러리 ==========
import lombok.extern.slf4j.Slf4j;

// ========== Spring Framework ==========
import org.springframework.stereotype.Component;

@Component // Spring의 컴포넌트 스캔에 의해 빈으로 등록
@Slf4j // Lombok을 사용한 로깅 지원
public class AuthCodeGenerator {
    // 인증 코드 생성 메서드
    
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
        // generateAuthCode() : 6자리 인증 코드 생성
        StringBuilder codeBuilder = new StringBuilder();
        // StringBuilder codeBuilder : 문자열을 효율적으로 생성하기 위한 클래스
        // codeBuilder.append() 메서드를 사용하여 문자열을 추가할 수 있다.
        
        // 6자리 랜덤 문자 선택
        for (int i = 0; i < AUTH_CODE_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(AUTH_CODE_CHARACTERS.length());
            // 0부터 AUTH_CODE_CHARACTERS.length() - 1 사이의 랜덤 인덱스 생성
            char randomChar = AUTH_CODE_CHARACTERS.charAt(randomIndex);
            // 랜덤 인덱스에 해당하는 문자 선택
            codeBuilder.append(randomChar);
            // 선택된 문자를 코드에 추가
        } // for 끝
        
        return codeBuilder.toString(); // 생성된 인증 코드를 문자열로 반환
    } // generateAuthCode 끝
    
    public String generateAuthSessionId(String userEmail) {
        // 입력값 검증
        if (userEmail == null || userEmail.trim().isEmpty()) {
            // null 또는 빈 문자열인 경우
            log.error("User email is null or empty for session ID generation");
            // 에러 로그 기록
            throw new IllegalArgumentException("사용자 이메일이 유효하지 않습니다.");
            // 예외 발생
        } // if 끝
        
        String trimmedEmail = userEmail.trim();
        // 이메일 앞뒤 공백 제거
        long currentTimeMillis = System.currentTimeMillis();
        // 현재 시간 밀리초
        String uuid8Chars = UUID.randomUUID().toString().substring(0, 8);
        // UUID의 앞 8자리 사용
        String sessionId = trimmedEmail + ":" + currentTimeMillis + ":" + uuid8Chars;
        // "이메일:타임스탬프:UUID8" 형식의 세션 ID 생성
        log.debug("Generated auth session ID for user: {} (ID length: {})", trimmedEmail, sessionId.length());
        // 디버그 로그 기록
        return sessionId;
        // 생성된 세션 ID 반환
    } // generateAuthSessionId 끝
} // AuthCodeGenerator 클래스 끝
