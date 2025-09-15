package BlueCrab.com.example.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class PasswordResetRedisUtil {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    // =============== IRT 관련 ===============
    
    public Map<String, Object> getIRTData(String irtToken) {
        try {
            String irtKey = "irt:" + irtToken;
            String irtJson = redisTemplate.opsForValue().get(irtKey);
            if (irtJson == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(irtJson, Map.class);
            return result;
        } catch (Exception e) {
            return null;
        }
    }
    
    public boolean isLockValid(String email, String irtLock) {
        String lockKey = "reset_lock:" + email;
        String currentLock = redisTemplate.opsForValue().get(lockKey);
        return irtLock != null && irtLock.equals(currentLock);
    }
    
    // =============== 이메일 코드 관리 ===============
    
    public void saveEmailCode(String email, String code, Map<String, Object> irtData, String lock) throws Exception {
        String codeKey = "email_code:" + email;
        
        Map<String, Object> codeData = new HashMap<>();
        codeData.put("code", code);
        codeData.put("user_id", irtData.get("user_id"));
        codeData.put("lock", lock);
        codeData.put("attempts", 0);
        codeData.put("created_at", Instant.now().getEpochSecond());
        
        String codeJson = objectMapper.writeValueAsString(codeData);
        redisTemplate.opsForValue().set(codeKey, codeJson, 5, TimeUnit.MINUTES); // 5분 TTL
    }
    
    // =============== 카운터 관리 ===============

    public void incrementCounter(String key, int expireSeconds) {
        String count = redisTemplate.opsForValue().get(key);
        if (count == null) {
            // 첫 번째 카운트
            redisTemplate.opsForValue().set(key, "1", expireSeconds, TimeUnit.SECONDS);
        } else {
            // 기존 카운트 증가
            redisTemplate.opsForValue().increment(key);
        }
    }
    
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    public Long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }
    
    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    public void setValue(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
}