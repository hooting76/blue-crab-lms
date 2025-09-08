package BlueCrab.com.example.service;

import BlueCrab.com.example.util.SHA256Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 통합 비밀번호 검증 서비스
 * BCrypt와 SHA256 방식을 모두 지원
 */
@Service
public class PasswordValidationService {

    @Autowired
    private PasswordEncoder passwordEncoder; // BCrypt 등

    /**
     * 비밀번호 검증 (자동 방식 감지)
     * 
     * @param plainPassword 평문 비밀번호
     * @param hashedPassword 저장된 해시값
     * @return boolean 일치하면 true
     */
    public boolean validatePassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }

        // SHA256 해시 형식인지 확인 (64자리 hex)
        if (SHA256Util.isValidSHA256Hash(hashedPassword)) {
            return SHA256Util.matches(plainPassword, hashedPassword);
        } else {
            // BCrypt 등 Spring Security PasswordEncoder 사용
            return passwordEncoder.matches(plainPassword, hashedPassword);
        }
    }

    /**
     * 관리자 비밀번호 검증 (SHA256 방식)
     */
    public boolean validateAdminPassword(String plainPassword, String sha256Hash) {
        return SHA256Util.matches(plainPassword, sha256Hash);
    }

    /**
     * 일반 사용자 비밀번호 검증 (BCrypt 방식)
     */
    public boolean validateUserPassword(String plainPassword, String bcryptHash) {
        return passwordEncoder.matches(plainPassword, bcryptHash);
    }
}
