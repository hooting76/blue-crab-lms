package BlueCrab.com.example.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 프로필 이미지 키 생성 유틸리티
 * 
 * MinIO 스토리지에 저장될 프로필 이미지 키를 생성하고 관리
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-14
 */
@Component
public class ProfileImageKeyGenerator {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * 기본 프로필 이미지 목록
     * MinIO에 미리 업로드되어 있어야 함
     */
    private static final String[] DEFAULT_PROFILE_IMAGES = {
        "default/profile_default_1.jpg",
        "default/profile_default_2.jpg",
        "default/profile_default_3.jpg",
        "default/profile_default_4.jpg",
        "default/profile_default_5.jpg",
        "default/profile_avatar_1.png",
        "default/profile_avatar_2.png",
        "default/profile_avatar_3.png",
        "default/profile_avatar_4.png",
        "default/profile_avatar_5.png"
    };
    
    /**
     * 기본 프로필 이미지 키 랜덤 반환
     * 신규 사용자 등록 시 사용
     * 
     * @return 기본 프로필 이미지 키
     */
    public String getDefaultProfileImageKey() {
        return DEFAULT_PROFILE_IMAGES[RANDOM.nextInt(DEFAULT_PROFILE_IMAGES.length)];
    }
    
    /**
     * 성별 기반 기본 프로필 이미지 키 반환
     * 
     * @param isMale 남성 여부
     * @return 성별에 맞는 기본 프로필 이미지 키
     */
    public String getDefaultProfileImageKeyByGender(boolean isMale) {
        if (isMale) {
            String[] maleImages = {
                "default/profile_male_1.jpg",
                "default/profile_male_2.jpg",
                "default/profile_male_3.jpg"
            };
            return maleImages[RANDOM.nextInt(maleImages.length)];
        } else {
            String[] femaleImages = {
                "default/profile_female_1.jpg",
                "default/profile_female_2.jpg",
                "default/profile_female_3.jpg"
            };
            return femaleImages[RANDOM.nextInt(femaleImages.length)];
        }
    }
    
    /**
     * 사용자 코드 기반 프로필 이미지 키 생성
     * 실제 이미지 업로드 시 사용
     * 
     * @param userCode 사용자 코드 (학번/교번)
     * @param extension 파일 확장자 (jpg, png 등)
     * @return 생성된 프로필 이미지 키
     * 
     * 예: "2025-001-01-847_20251014123456.jpg"
     */
    public String generateProfileImageKey(String userCode, String extension) {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        );
        
        // 파일명에서 사용할 수 없는 문자 제거 (하이픈은 언더스코어로 변경)
        String sanitizedUserCode = userCode.replace("-", "");
        
        return String.format("%s_%s.%s", sanitizedUserCode, timestamp, extension);
    }
    
    /**
     * 사용자 코드와 원본 파일명 기반 프로필 이미지 키 생성
     * 
     * @param userCode 사용자 코드
     * @param originalFileName 원본 파일명
     * @return 생성된 프로필 이미지 키
     */
    public String generateProfileImageKeyFromFileName(String userCode, String originalFileName) {
        String extension = extractFileExtension(originalFileName);
        return generateProfileImageKey(userCode, extension);
    }
    
    /**
     * 파일명에서 확장자 추출
     * 
     * @param fileName 파일명
     * @return 확장자 (소문자)
     */
    private String extractFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "jpg"; // 기본값
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
        
        // 지원하는 확장자만 허용
        if (extension.matches("jpg|jpeg|png|gif|webp")) {
            return extension;
        }
        
        return "jpg"; // 기본값
    }
    
    /**
     * 프로필 이미지 키가 기본 이미지인지 확인
     * 
     * @param imageKey 이미지 키
     * @return 기본 이미지 여부
     */
    public boolean isDefaultImage(String imageKey) {
        if (imageKey == null) {
            return false;
        }
        
        return imageKey.startsWith("default/");
    }
    
    /**
     * 사용자 지정 프로필 이미지 키인지 확인
     * 
     * @param imageKey 이미지 키
     * @return 사용자 지정 이미지 여부
     */
    public boolean isCustomImage(String imageKey) {
        return imageKey != null && !isDefaultImage(imageKey);
    }
    
    /**
     * 프로필 이미지 URL 생성
     * MinIO 서비스와 연동하여 실제 URL 생성
     * 
     * @param imageKey 이미지 키
     * @param bucketName 버킷명
     * @return 이미지 URL
     */
    public String generateImageUrl(String imageKey, String bucketName) {
        if (imageKey == null || imageKey.isEmpty()) {
            // 기본 이미지 URL 반환
            return getDefaultProfileImageKey();
        }
        
        // MinIO URL 형식
        return String.format("profiles/%s", imageKey);
    }
}
