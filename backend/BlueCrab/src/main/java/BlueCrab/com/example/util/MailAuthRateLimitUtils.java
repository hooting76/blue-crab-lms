// 작업자 : 성태준
// 이메일 인증 Rate Limiting 유틸리티 클래스
// Redis를 활용한 요청 빈도 제한 기능
// 메일 발송과 코드 검증 요청에 대한 독립적인 스팸 방지 및 보안 강화

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========

// =========== Java 표준 라이브러리 ==========
import java.util.concurrent.TimeUnit;

// ========= 외부 라이브러리 ==========
import lombok.extern.slf4j.Slf4j;

// ========= Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MailAuthRateLimitUtils {
    
    // ========== Rate Limiting 설정 상수 ==========
    private static final int INTERVAL_MINUTES = 1;
    // 요청 간격 제한 (분 단위) - 연속 요청 방지
    private static final int HOURLY_LIMIT = 3;
    // 시간당 최대 요청 횟수
    private static final int DAILY_LIMIT = 5;
    // 일일 최대 요청 횟수

    // ========== Redis 키 접두사 ==========
    private static final String SENDMAIL_PREFIX = "rate_limit:sendmail:";
    // 메일 발송 요청 관련 Redis 키 접두사
    private static final String LAST_REQ_PREFIX = "last_req:sendmail:";
    // 마지막 메일 발송 요청 시간 저장용 Redis 키 접두사
    
    // ========== 의존성 주입 ==========
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    // Redis 템플릿을 사용하여 카운터 및 제한 정보 저장/조회
    
    // ========== 메일 발송 요청 허용 여부 확인 ==========
    public boolean isSendMailAllowed(String userEmail) {
        // 메일 발송 요청이 허용되는지 확인하는 메서드
        return checkRateLimit(userEmail, SENDMAIL_PREFIX, "sendmail");
        // 요청 허용 여부를 반환
    } // isSendMailAllowed 끝

    // ========== 메일 발송 요청 기록 ==========
    public void recordSendMailRequest(String userEmail) {
        // 메일 발송 요청을 카운터에 기록하는 메서드
        incrementCounter(userEmail, SENDMAIL_PREFIX, "sendmail");
        // 카운터 증가 및 마지막 요청 시간 업데이트
    } // recordSendMailRequest 끝
    
    // ========== Rate Limit 확인 (내부 메서드) ==========
    private boolean checkRateLimit(String userEmail, String prefix, String requestType) {
        // 공통 Rate Limit 확인 로직
        try {
            // 1. 간격 제한 확인 (연속 요청 방지)
            if (!checkInterval(userEmail, requestType)) {
                // 간격 제한 초과의 경우
                log.warn("Interval limit exceeded for {} - User: {}", requestType, userEmail);
                // 경고 로그 기록
                return false;
                // 요청 거부를 반환
            } // 간격 제한 초과의 경우 끝
            
            // 2. 시간당 제한 확인
            if (!checkHourlyLimit(userEmail, prefix)) {
                // 시간당 제한 초과의 경우
                log.warn("Hourly limit exceeded for {} - User: {}", requestType, userEmail);
                // 경고 로그 기록
                return false;
                // 요청 거부를 반환
            } // 시간당 제한 초과의 경우 끝
            
            // 3. 일일 제한 확인
            if (!checkDailyLimit(userEmail, prefix)) {
                // 일일 제한 초과의 경우
                log.warn("Daily limit exceeded for {} - User: {}", requestType, userEmail);
                // 경고 로그 기록
                return false;
                // 요청 거부를 반환
            } // 일일 제한 초과의 경우 끝
            
            return true;
            // 모든 제한을 통과한 경우 요청 허용을 반환
            
        } catch (Exception e) {
            // 오류 처리
            log.error("Rate limit check error for {} - User: {}, Error: {}", 
                    requestType, userEmail, e.getMessage());
                // 오류 로그 기록
            return true;
            // 요청 허용 반환(서비스 연속성 보장)
        }
    } // checkRateLimit 끝
    
    // ========== 간격 제한 확인 ==========
    private boolean checkInterval(String userEmail, String requestType) {
        // 간격 제한 확인 로직
        String intervalKey = LAST_REQ_PREFIX + userEmail;
        // 마지막 요청 시간 조회
        Object lastTime = redisTemplate.opsForValue().get(intervalKey);
        // Redis에서 마지막 요청 시간 조회
        
        if (lastTime == null) {
            // 첫 요청인 경우
            return true;
            // 요청 허용 반환(첫 요청 허용)
        } // 첫 요청인 경우 끝
        
        try { // 예외 처리 블록
            long lastRequestTime = Long.parseLong(lastTime.toString());
            // 마지막 요청 시간 파싱
            long currentTime = System.currentTimeMillis();
            // 현재 시간 조회
            long timeDiff = currentTime - lastRequestTime;
            // 시간 차이 계산
            long intervalMillis = INTERVAL_MINUTES * 60 * 1000;
            // 설정된 간격 제한을 밀리초로 변환
            
            return timeDiff >= intervalMillis;
            // 설정된 간격 이상 경과했으면 허용을 반환

        } // 예외 처리 블록 끝
        catch (Exception e) {
            // 오류 처리
            log.warn("Error checking interval for {} - User: {}", requestType, userEmail);
            // 오류 로그 기록
            return true;
            // 허용을 반환(오류 시 허용)
        }
    } // checkInterval 끝
    
    // ========== 시간당 제한 확인 ==========
    private boolean checkHourlyLimit(String userEmail, String prefix) {
        // 시간당 제한 확인 로직
        String hourlyKey = prefix + "hourly:" + userEmail;
        // Redis 키 구성
        Object countObj = redisTemplate.opsForValue().get(hourlyKey);
        // 시간당 요청 카운터 조회
        
        if (countObj == null) {
            // 첫 요청인 경우
            return true;
            // 요청 허용 반환(첫 요청 허용)
        } // 첫 요청인 경우 끝
        
        try { // 예외 처리 블록 시작
            int currentCount = Integer.parseInt(countObj.toString());
            // 현재 카운터 파싱
            return currentCount < HOURLY_LIMIT;
            // 제한 내이면 허용 반환
            
        } // 예외 처리 블록 끝 
        catch (Exception e) { 
            // 오류 처리
            return true;
            // 허용을 반환(오류 시 허용)
        }
    } // checkHourlyLimit 끝
    
    // ========== 일일 제한 확인 ==========
    private boolean checkDailyLimit(String userEmail, String prefix) {
        // 일일 제한 확인 로직
        String dailyKey = prefix + "daily:" + userEmail;
        // Redis 키 구성
        Object countObj = redisTemplate.opsForValue().get(dailyKey);
        // 일일 요청 카운터 조회
        
        if (countObj == null) {
            // 첫 요청인 경우
            return true; 
            // 요청 허용 반환(첫 요청 허용)
        } // 첫 요청인 경우 끝
        
        try {
            // 예외 처리 블록 시작
            int currentCount = Integer.parseInt(countObj.toString());
            // 현재 카운터 파싱
            return currentCount < DAILY_LIMIT;
            // 제한 내이면 허용 반환
            
        } // 예외 처리 블록 끝 
        catch (Exception e) {
            // 오류 처리
            return true;
            // 허용을 반환(오류 시 허용)
        }
    } // checkDailyLimit 끝
    
    // ========== 카운터 증가 (내부 메서드) ==========
    private void incrementCounter(String userEmail, String prefix, String requestType) {
        // 공통 카운터 증가 로직
        try {
            String hourlyKey = prefix + "hourly:" + userEmail;
            // 시간당 카운터 Redis 키
            String dailyKey = prefix + "daily:" + userEmail;
            // 일일 카운터 Redis 키
            String intervalKey = LAST_REQ_PREFIX + userEmail;
            // 마지막 요청 시간 Redis 키

            Long hourlyCount = redisTemplate.opsForValue().increment(hourlyKey);
            // 시간당 카운터 증가
            if (hourlyCount != null && hourlyCount == 1) {
                // 첫 요청인 경우
                redisTemplate.expire(hourlyKey, 1, TimeUnit.HOURS);
                // 1시간 TTL 설정
            } // 첫 요청인 경우 끝
            
            Long dailyCount = redisTemplate.opsForValue().increment(dailyKey);
            // 일일 카운터 증가
            if (dailyCount != null && dailyCount == 1) {
                // 첫 요청인 경우
                long secondsUntilMidnight = getSecondsUntilMidnight();
                // 자정까지 남은 시간 계산
                redisTemplate.expire(dailyKey, secondsUntilMidnight, TimeUnit.SECONDS);
                // 자정까지 TTL 설정
            }// 첫 요청인 경우 끝
        
            redisTemplate.opsForValue().set(intervalKey, 
                                        String.valueOf(System.currentTimeMillis()), 
                                        INTERVAL_MINUTES, TimeUnit.MINUTES);
                // 마지막 요청 시간 저장
                // intervalKey에 현재 시간과 TTL 설정
                // String.valueOf() : long를 문자열로 변환
                // INTERVAL_MINUTES : 마지막 요청 시간의 TTL (분 단위)

            log.debug("Updated counters for {} - User: {}, Hourly: {}, Daily: {}", 
                    requestType, userEmail, hourlyCount, dailyCount);
            // 디버그 로그 기록
                    
        } catch (Exception e) {
            log.error("Failed to increment counter for {} - User: {}, Error: {}", 
                    requestType, userEmail, e.getMessage());
            // 오류 로그 기록
            // 오류 발생 시에도 서비스 연속성 보장을 위해 예외를 던지지 않음
        }
    } // incrementCounter 끝
    
    // ========== 자정까지 남은 시간 계산 ==========
    private long getSecondsUntilMidnight() {
        // 자정까지 남은 시간을 초 단위로 계산하는 메서드
        long currentTime = System.currentTimeMillis();  // 현재 시간 밀리초
        long oneDay = 24 * 60 * 60 * 1000;  // 24시간을 밀리초로
        long millisSinceMidnight = currentTime % oneDay;  // 자정 이후 경과 시간
        long millisUntilMidnight = oneDay - millisSinceMidnight;  // 자정까지 남은 시간
        return millisUntilMidnight / 1000;  // 초 단위로 반환
    } // getSecondsUntilMidnight 끝
    
    // ========== 남은 대기 시간 조회 (분 단위) ==========
    public int getRemainingWaitMinutes(String userEmail) {
        // 대기 시간 조회 메서드
        try {
            String intervalKey = LAST_REQ_PREFIX + userEmail;
            // 마지막 요청 시간 조회
            Object lastTime = redisTemplate.opsForValue().get(intervalKey);
            // Redis에서 마지막 요청 시간 조회
            
            if (lastTime == null) {
                // 첫 요청인 경우
                return 0; 
                // 대기 시간 없음을 반환
            } // 첫 요청인 경우 끝
            
            long lastRequestTime = Long.parseLong(lastTime.toString());
            // 마지막 요청 시간 파싱
            long currentTime = System.currentTimeMillis();
            // 현재 시간 조회
            long timeDiff = currentTime - lastRequestTime;
            // 시간 차이 계산
            long intervalMillis = INTERVAL_MINUTES * 60 * 1000;
            // 설정된 간격 제한을 밀리초로 변환
            
            if (timeDiff >= intervalMillis) {
                // 간격 제한을 초과한 경우
                return 0; 
                // 대기 시간 없음을 반환
            } // 간격 제한을 초과한 경우 끝 
            
            long remainingMillis = intervalMillis - timeDiff;
            // 남은 대기 시간 계산
            return (int) Math.ceil(remainingMillis / (60.0 * 1000)); 
            // 분 단위로 올림

        } // try 끝
        catch (Exception e) {
            // 오류 처리
            log.error("Error getting remaining wait time for sendmail - User: {}", userEmail);
            // 오류 로그 기록
            return 0;
            // 오류 시 대기 시간 없음을 반환
        } // catch 끝
    } // getRemainingWaitMinutes 끝
} // MailAuthRateLimitUtils 클래스 끝
