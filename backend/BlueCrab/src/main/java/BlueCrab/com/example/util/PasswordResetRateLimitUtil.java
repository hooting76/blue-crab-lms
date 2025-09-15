package BlueCrab.com.example.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class PasswordResetRateLimitUtil {

    @Autowired
    private PasswordResetRedisUtil redisUtil;
    
    // 레이트 리밋 상수
    private static final int RESEND_COOLDOWN_SECONDS = 60;        // 재전송 쿨타임: 60초
    private static final int HOURLY_LIMIT = 5;                    // 시간당 제한: 5회
    private static final int DAILY_LIMIT = 10;                    // 일일 제한: 10회
    private static final int HOURLY_EXPIRE_SECONDS = 3600;        // 1시간
    private static final int DAILY_EXPIRE_SECONDS = 86400;        // 24시간
    
    public Long getRateLimitRetryAfter(String email) {
        // 1. 재전송 쿨타임 확인 (60초)
        String resendKey = "mail_resend:" + email;
        if (redisUtil.hasKey(resendKey)) {
            Long ttl = redisUtil.getExpire(resendKey, TimeUnit.SECONDS);
            return ttl > 0 ? ttl : RESEND_COOLDOWN_SECONDS;
        }
        
        // 2. 시간당 5회 제한 확인
        String hourlyKey = "mail_send:h1:" + email;
        String hourlyCount = redisUtil.getValue(hourlyKey);
        if (hourlyCount != null && Integer.parseInt(hourlyCount) >= HOURLY_LIMIT) {
            Long ttl = redisUtil.getExpire(hourlyKey, TimeUnit.SECONDS);
            return ttl > 0 ? ttl : HOURLY_EXPIRE_SECONDS;
        }
        
        // 3. 일일 10회 제한 확인
        String dailyKey = "mail_send:d1:" + email;
        String dailyCount = redisUtil.getValue(dailyKey);
        if (dailyCount != null && Integer.parseInt(dailyCount) >= DAILY_LIMIT) {
            Long ttl = redisUtil.getExpire(dailyKey, TimeUnit.SECONDS);
            return ttl > 0 ? ttl : DAILY_EXPIRE_SECONDS;
        }
        
        return null; // 제한 없음
    }
    
    public void updateRateLimit(String email) {
        // 1. 재전송 쿨타임 설정 (60초)
        String resendKey = "mail_resend:" + email;
        redisUtil.setValue(resendKey, "1", RESEND_COOLDOWN_SECONDS, TimeUnit.SECONDS);
        
        // 2. 시간당 카운트 증가 (발송 성공 시에만)
        String hourlyKey = "mail_send:h1:" + email;
        redisUtil.incrementCounter(hourlyKey, HOURLY_EXPIRE_SECONDS);
        
        // 3. 일일 카운트 증가 (발송 성공 시에만)
        String dailyKey = "mail_send:d1:" + email;
        redisUtil.incrementCounter(dailyKey, DAILY_EXPIRE_SECONDS);
    }
    
    public boolean isRateLimited(String email) {
        return getRateLimitRetryAfter(email) != null;
    }
    
    public RateLimitStatus getRateLimitStatus(String email) {
        String resendKey = "mail_resend:" + email;
        String hourlyKey = "mail_send:h1:" + email;
        String dailyKey = "mail_send:d1:" + email;
        
        boolean resendBlocked = redisUtil.hasKey(resendKey);
        Long resendTtl = resendBlocked ? redisUtil.getExpire(resendKey, TimeUnit.SECONDS) : null;
        
        String hourlyCountStr = redisUtil.getValue(hourlyKey);
        int hourlyCount = hourlyCountStr != null ? Integer.parseInt(hourlyCountStr) : 0;
        Long hourlyTtl = hourlyCount > 0 ? redisUtil.getExpire(hourlyKey, TimeUnit.SECONDS) : null;
        
        String dailyCountStr = redisUtil.getValue(dailyKey);
        int dailyCount = dailyCountStr != null ? Integer.parseInt(dailyCountStr) : 0;
        Long dailyTtl = dailyCount > 0 ? redisUtil.getExpire(dailyKey, TimeUnit.SECONDS) : null;
        
        return new RateLimitStatus(
            resendBlocked, resendTtl,
            hourlyCount, HOURLY_LIMIT, hourlyTtl,
            dailyCount, DAILY_LIMIT, dailyTtl
        );
    }
    
    public static class RateLimitStatus {
        public final boolean resendBlocked;
        public final Long resendTtl;
        public final int hourlyCount;
        public final int hourlyLimit;
        public final Long hourlyTtl;
        public final int dailyCount;
        public final int dailyLimit;
        public final Long dailyTtl;
        
        public RateLimitStatus(boolean resendBlocked, Long resendTtl,
                             int hourlyCount, int hourlyLimit, Long hourlyTtl,
                             int dailyCount, int dailyLimit, Long dailyTtl) {
            this.resendBlocked = resendBlocked;
            this.resendTtl = resendTtl;
            this.hourlyCount = hourlyCount;
            this.hourlyLimit = hourlyLimit;
            this.hourlyTtl = hourlyTtl;
            this.dailyCount = dailyCount;
            this.dailyLimit = dailyLimit;
            this.dailyTtl = dailyTtl;
        }
        
        @Override
        public String toString() {
            return String.format(
                "RateLimit[resend=%s(%ds), hourly=%d/%d(%ds), daily=%d/%d(%ds)]",
                resendBlocked, resendTtl, hourlyCount, hourlyLimit, hourlyTtl,
                dailyCount, dailyLimit, dailyTtl
            );
        }
    }
}