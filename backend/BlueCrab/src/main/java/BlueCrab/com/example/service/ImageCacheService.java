package BlueCrab.com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * 이미지 캐싱 서비스
 * Redis를 활용하여 프로필 이미지를 캐싱하여 성능 향상
 *
 * 주요 기능:
 * - 이미지 바이트 배열 캐싱
 * - 캐시 무효화 (이미지 업데이트 시)
 * - 캐시 히트/미스 통계
 *
 * 캐시 전략:
 * - 24시간 TTL (설정 가능)
 * - 최대 100개 이미지 캐싱 (설정 가능)
 * - LRU 방식으로 오래된 캐시 자동 제거
 */
@Service
@CacheConfig(cacheNames = "profile-images")
public class ImageCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ImageCacheService.class);

    @Value("${app.image.cache-enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.image.cache-max-size:100}")
    private int cacheMaxSize;

    @Value("${app.image.cache-expire-hours:24}")
    private int cacheExpireHours;

    /**
     * 이미지 바이트 배열 캐시 조회
     * Redis에서 이미지를 찾고, 없으면 MinIO에서 조회 후 캐싱
     *
     * @param imageKey 이미지 키
     * @param imageStream MinIO에서 가져온 이미지 스트림 (캐시 미스 시 사용)
     * @return 캐시된 이미지 바이트 배열
     */
    @Cacheable(key = "#imageKey", condition = "#root.target.cacheEnabled")
    public byte[] getCachedImageBytes(String imageKey, InputStream imageStream) {
        try {
            logger.debug("캐시 미스 - MinIO에서 이미지 로딩: {}", imageKey);

            // InputStream을 byte[]로 변환
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = imageStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] imageBytes = outputStream.toByteArray();

            logger.info("이미지 캐시 저장 완료 - Key: {}, Size: {} bytes", imageKey, imageBytes.length);

            return imageBytes;

        } catch (IOException e) {
            logger.error("이미지 바이트 변환 실패 - Key: {}, Error: {}", imageKey, e.getMessage());
            throw new RuntimeException("이미지 데이터 변환 실패", e);
        } finally {
            try {
                imageStream.close();
            } catch (IOException e) {
                logger.warn("이미지 스트림 닫기 실패: {}", e.getMessage());
            }
        }
    }

    /**
     * 캐시된 이미지 조회만 (MinIO 접근 없음)
     * 캐시에 있으면 반환, 없으면 Empty 반환
     *
     * @param imageKey 이미지 키
     * @return 캐시된 이미지 바이트 배열 (Optional)
     */
    public Optional<byte[]> getCachedImageBytesOnly(String imageKey) {
        if (!cacheEnabled) {
            return Optional.empty();
        }

        try {
            // TODO: Redis에서 직접 조회하는 로직 구현
            // 현재는 @Cacheable을 통해서만 캐시 접근 가능
            logger.debug("캐시 전용 조회 요청 - Key: {}", imageKey);
            return Optional.empty();

        } catch (Exception e) {
            logger.warn("캐시 조회 실패 - Key: {}, Error: {}", imageKey, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 특정 이미지 캐시 무효화
     * 이미지가 업데이트된 경우 호출
     *
     * @param imageKey 무효화할 이미지 키
     */
    @CacheEvict(key = "#imageKey")
    public void evictImageCache(String imageKey) {
        if (cacheEnabled) {
            logger.info("이미지 캐시 무효화 - Key: {}", imageKey);
        }
    }

    /**
     * 특정 사용자의 모든 이미지 캐시 무효화
     * 사용자 프로필 이미지가 변경된 경우 호출
     *
     * @param userEmail 사용자 이메일
     */
    @CacheEvict(allEntries = true, condition = "#userEmail != null")
    public void evictUserImageCache(String userEmail) {
        if (cacheEnabled) {
            logger.info("사용자 이미지 캐시 전체 무효화 - User: {}", userEmail);
        }
    }

    /**
     * 전체 이미지 캐시 무효화
     * 시스템 유지보수나 긴급 상황 시 사용
     */
    @CacheEvict(allEntries = true)
    public void evictAllImageCache() {
        if (cacheEnabled) {
            logger.info("전체 이미지 캐시 무효화 실행");
        }
    }

    /**
     * 캐시 설정 정보 반환
     *
     * @return 캐시 설정 Map
     */
    public java.util.Map<String, Object> getCacheInfo() {
        return java.util.Map.of(
            "cacheEnabled", cacheEnabled,
            "cacheMaxSize", cacheMaxSize,
            "cacheExpireHours", cacheExpireHours,
            "cacheName", "profile-images"
        );
    }

    /**
     * 이미지 스트림을 바이트 배열로 변환
     * 캐시 없이 직접 변환하는 유틸리티 메서드
     *
     * @param imageStream 이미지 입력 스트림
     * @return 이미지 바이트 배열
     * @throws IOException 스트림 읽기 실패 시
     */
    public static byte[] streamToBytes(InputStream imageStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = imageStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        return outputStream.toByteArray();
    }
}