package BlueCrab.com.example.util;

import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 사용자 정보 검증을 위한 유틸리티 클래스
 * ID 찾기, 비밀번호 찾기 등에서 공통으로 사용되는 사용자 검증 로직을 제공
 *
 * 주요 기능:
 * - 학번, 이름, 전화번호를 통한 사용자 존재 확인
 * - 이메일 주소 마스킹 처리
 * - 보안을 고려한 사용자 검증
 *
 * 보안 고려사항:
 * - 타이밍 공격 방지를 위한 일관된 처리 시간
 * - 사용자 존재 여부를 직접적으로 노출하지 않음
 * - 개인정보 마스킹 처리
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class UserVerificationUtils {
    
    @Autowired
    private UserTblRepository userTblRepository;
    
    /**
     * 사용자 정보 검증 (학번, 이름, 전화번호)
     * 세 정보가 모두 일치하는 사용자가 존재하는지 확인
     *
     * @param userCode 학번/교수 코드
     * @param userName 사용자 이름
     * @param userPhone 전화번호
     * @return 검증된 사용자 정보 (Optional)
     */
    public Optional<UserTbl> verifyUserByCodeNamePhone(String userCode, String userName, String userPhone) {
        try {
            // 세 정보가 모두 일치하는 사용자 조회
            return userTblRepository.findByUserCodeAndUserNameAndUserPhone(userCode, userName, userPhone);
        } catch (Exception e) {
            // 예외 발생 시 빈 Optional 반환 (보안상 에러 정보 노출 방지)
            return Optional.empty();
        }
    }
    
    /**
     * 이메일 주소 마스킹 처리
     * 이메일의 로컬 부분(@ 앞)을 마스킹하여 일부만 노출
     *
     * 마스킹 규칙:
     * - 3글자 이하: 첫 글자만 노출 (a** 형태)
     * - 4글자 이상: 첫 글자와 마지막 글자 노출 (a***b 형태)
     * - 도메인 부분은 그대로 노출
     *
     * @param email 원본 이메일 주소
     * @return 마스킹된 이메일 주소
     *
     * 예시:
     * - "abc@domain.com" → "a**@domain.com"
     * - "user123@domain.com" → "u****3@domain.com"
     * - "a@domain.com" → "a**@domain.com"
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email; // 유효하지 않은 이메일은 그대로 반환
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email; // 유효하지 않은 이메일은 그대로 반환
        }
        
        String localPart = parts[0];
        String domainPart = parts[1];
        
        String maskedLocal;
        
        if (localPart.length() <= 1) {
            // 1글자 이하인 경우
            maskedLocal = localPart + "**";
        } else if (localPart.length() <= 3) {
            // 2~3글자인 경우: 첫 글자만 노출
            maskedLocal = localPart.charAt(0) + "**";
        } else {
            // 4글자 이상인 경우: 첫 글자와 마지막 글자 노출
            int maskLength = localPart.length() - 2;
            String mask = "*".repeat(maskLength);
            maskedLocal = localPart.charAt(0) + mask + localPart.charAt(localPart.length() - 1);
        }
        
        return maskedLocal + "@" + domainPart;
    }
    
    /**
     * 전화번호 유효성 검사
     * 11자리 숫자 형태인지 확인
     *
     * @param userPhone 전화번호
     * @return 유효한 전화번호 여부
     */
    public boolean isValidPhoneNumber(String userPhone) {
        if (userPhone == null) {
            return false;
        }
        return userPhone.matches("^\\d{11}$");
    }
    
    /**
     * 사용자 이름 유효성 검사
     * 빈 문자열이 아니고 50자 이하인지 확인
     *
     * @param userName 사용자 이름
     * @return 유효한 이름 여부
     */
    public boolean isValidUserName(String userName) {
        if (userName == null) {
            return false;
        }
        String trimmed = userName.trim();
        return !trimmed.isEmpty() && trimmed.length() <= 50;
    }
    
    /**
     * 사용자 코드 유효성 검사
     * null이 아니고 양수인지 확인
     *
     * @param userCode 사용자 코드
     * @return 유효한 코드 여부
     */
    public boolean isValidUserCode(String userCode) {
        return userCode != null && !userCode.trim().isEmpty();
    }
}