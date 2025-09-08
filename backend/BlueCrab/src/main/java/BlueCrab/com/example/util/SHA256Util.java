package BlueCrab.com.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA256 해싱 유틸리티 클래스
 * 관리자 비밀번호 검증을 위한 SHA256 해시 처리
 */
public class SHA256Util {

    private static final Logger logger = LoggerFactory.getLogger(SHA256Util.class);

    /**
     * 평문 비밀번호를 SHA256으로 해싱
     * 
     * @param plainPassword 평문 비밀번호
     * @return String SHA256 해시값 (소문자 hex)
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            
            // byte 배열을 hex 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    /**
     * 평문 비밀번호와 저장된 해시값 비교
     * 
     * @param plainPassword 평문 비밀번호
     * @param hashedPassword 저장된 해시값
     * @return boolean 일치하면 true
     */
    public static boolean matches(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }

        try {
            String inputHash = hash(plainPassword);
            return inputHash.equals(hashedPassword.toLowerCase());
        } catch (Exception e) {
            logger.error("Error comparing passwords", e);
            return false;
        }
    }

    /**
     * 해시값이 유효한 SHA256 형식인지 확인
     * 
     * @param hash 해시값
     * @return boolean 유효하면 true
     */
    public static boolean isValidSHA256Hash(String hash) {
        if (hash == null) {
            return false;
        }
        
        // SHA256 해시는 64자의 hex 문자열
        return hash.matches("^[a-fA-F0-9]{64}$");
    }

    /**
     * 테스트용 메인 메서드
     * 비밀번호 해싱 결과 확인용
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            String password = args[0];
            String hashed = hash(password);
            System.out.println("Password: " + password);
            System.out.println("SHA256 Hash: " + hashed);
            System.out.println("Hash Length: " + hashed.length());
            System.out.println("Is Valid SHA256: " + isValidSHA256Hash(hashed));
            
            // 검증 테스트
            boolean matches = matches(password, hashed);
            System.out.println("Verification Test: " + matches);
        } else {
            System.out.println("Usage: java SHA256Util <password>");
            System.out.println("Example hashes:");
            System.out.println("'password123' -> " + hash("password123"));
            System.out.println("'admin123' -> " + hash("admin123"));
        }
    }
}
