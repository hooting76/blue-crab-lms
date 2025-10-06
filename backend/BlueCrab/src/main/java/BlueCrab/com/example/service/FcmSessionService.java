package BlueCrab.com.example.service;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FCM 임시 세션 관리 서비스 (Redis)
 *
 * 용도:
 * - 로그인 중이지만 DB에 등록되지 않은 기기의 임시 FCM 토큰 저장
 * - 사용자가 기기 변경을 거부했지만 로그인은 성공한 경우
 * - 로그아웃 시 자동 삭제
 */
@Service
public class FcmSessionService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration TEMP_TOKEN_TTL = Duration.ofHours(24);

    public FcmSessionService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 임시 FCM 토큰 추가 (로그인 중에만 유효)
     *
     * @param userIdx 사용자 인덱스
     * @param platform 플랫폼 (ANDROID, IOS, WEB)
     * @param fcmToken FCM 토큰
     */
    public void addTemporaryToken(Integer userIdx, String platform, String fcmToken) {
        String key = buildKey(userIdx, platform);
        redisTemplate.opsForValue().set(key, fcmToken, TEMP_TOKEN_TTL);
    }

    /**
     * 임시 FCM 토큰 제거 (로그아웃 시)
     *
     * @param userIdx 사용자 인덱스
     * @param platform 플랫폼
     */
    public void removeTemporaryToken(Integer userIdx, String platform) {
        String key = buildKey(userIdx, platform);
        redisTemplate.delete(key);
    }

    /**
     * 임시 FCM 토큰 조회
     *
     * @param userIdx 사용자 인덱스
     * @param platform 플랫폼
     * @return FCM 토큰 (없으면 null)
     */
    public String getTemporaryToken(Integer userIdx, String platform) {
        String key = buildKey(userIdx, platform);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 임시 토큰이 존재하는지 확인
     *
     * @param userIdx 사용자 인덱스
     * @param platform 플랫폼
     * @return 존재 여부
     */
    public boolean hasTemporaryToken(Integer userIdx, String platform) {
        String key = buildKey(userIdx, platform);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 사용자의 모든 임시 토큰 제거 (전체 로그아웃 시)
     *
     * @param userIdx 사용자 인덱스
     */
    public void removeAllTemporaryTokens(Integer userIdx) {
        String pattern = "fcm:temp:" + userIdx + ":*";
        Set<String> keys = findKeysByPattern(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 사용자의 임시 토큰이 있는 플랫폼 목록 조회
     *
     * @param userIdx 사용자 인덱스
     * @return 플랫폼 목록 (ANDROID, IOS, WEB)
     */
    public Set<String> getTemporaryPlatforms(Integer userIdx) {
        String pattern = "fcm:temp:" + userIdx + ":*";
        Set<String> keys = findKeysByPattern(pattern);

        if (keys == null || keys.isEmpty()) {
            return Collections.emptySet();
        }

        return keys.stream()
                .map(key -> key.split(":")[3]) // "fcm:temp:123:WEB" -> "WEB"
                .collect(Collectors.toSet());
    }

    /**
     * Redis 키 생성
     *
     * @param userIdx 사용자 인덱스
     * @param platform 플랫폼
     * @return Redis 키
     */
    private String buildKey(Integer userIdx, String platform) {
        return "fcm:temp:" + userIdx + ":" + platform.toUpperCase();
    }

    private Set<String> findKeysByPattern(String pattern) {
        Set<String> keys = redisTemplate.execute((RedisConnection connection) -> {
            if (connection == null) {
                return Collections.<String>emptySet();
            }

            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
            RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
            Set<String> result = new HashSet<>();

            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    String deserialized = serializer.deserialize(cursor.next());
                    if (deserialized != null) {
                        result.add(deserialized);
                    }
                }
            }

            return result;
        });

        return keys == null ? Collections.emptySet() : keys;
    }
}
