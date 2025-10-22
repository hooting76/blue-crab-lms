package BlueCrab.com.example.security;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 순수 SHA-256 해시만 생성하는 PasswordEncoder
 * Salt 없이, 중괄호 prefix 없이, 순수 HEX 해시만 반환
 *
 * 주의: 이 방식은 보안이 약하므로 테스트/레거시 호환용으로만 사용
 * 향후 BCryptPasswordEncoder로 마이그레이션 예정
 */
public class PlainSha256PasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        // 인코딩된 비밀번호와 비교
        String rawPasswordHash = encode(rawPassword);
        return rawPasswordHash.equals(encodedPassword);
    }

    /**
     * 바이트 배열을 HEX 문자열로 변환
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
