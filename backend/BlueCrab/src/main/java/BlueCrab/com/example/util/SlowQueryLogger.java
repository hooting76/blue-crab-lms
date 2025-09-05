package BlueCrab.com.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 느린 쿼리 감지 및 로깅 유틸리티
 * 데이터베이스 성능 모니터링을 위한 클래스
 */
@Component
public class SlowQueryLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(SlowQueryLogger.class);
    
    // 느린 쿼리 임계값 (밀리초) - 1초
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000;
    
    // 매우 느린 쿼리 임계값 (밀리초) - 3초
    private static final long VERY_SLOW_QUERY_THRESHOLD_MS = 3000;
    
    /**
     * 쿼리 실행 시간을 측정하고 느린 쿼리를 로깅
     * 
     * @param queryName 쿼리 식별자 (메서드명 등)
     * @param executionTimeMs 실행 시간 (밀리초)
     * @param sql 실행된 SQL (선택사항)
     * @param params 쿼리 파라미터 (선택사항)
     */
    public static void logQueryTime(String queryName, long executionTimeMs, String sql, Object... params) {
        if (executionTimeMs > VERY_SLOW_QUERY_THRESHOLD_MS) {
            // 매우 느린 쿼리 - ERROR 레벨
            logger.error("🚨 매우 느린 쿼리 감지 - {}ms | 쿼리: {} | SQL: {} | 파라미터: {}", 
                executionTimeMs, queryName, sql, java.util.Arrays.toString(params));
        } else if (executionTimeMs > SLOW_QUERY_THRESHOLD_MS) {
            // 느린 쿼리 - WARN 레벨
            logger.warn("⚠️ 느린 쿼리 감지 - {}ms | 쿼리: {} | SQL: {} | 파라미터: {}", 
                executionTimeMs, queryName, sql, java.util.Arrays.toString(params));
        } else {
            // 정상 속도 - DEBUG 레벨 (개발시에만)
            logger.debug("✅ 쿼리 실행 완료 - {}ms | 쿼리: {}", executionTimeMs, queryName);
        }
    }
    
    /**
     * 간단한 형태의 느린 쿼리 로깅
     */
    public static void logQueryTime(String queryName, long executionTimeMs) {
        logQueryTime(queryName, executionTimeMs, null);
    }
    
    /**
     * Repository 메서드 실행 시간 측정을 위한 헬퍼 메서드
     */
    public static <T> T measureAndLog(String queryName, java.util.function.Supplier<T> queryExecution) {
        long startTime = System.currentTimeMillis();
        try {
            T result = queryExecution.get();
            long executionTime = System.currentTimeMillis() - startTime;
            logQueryTime(queryName, executionTime);
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("❌ 쿼리 실행 실패 - {}ms | 쿼리: {} | 오류: {}", 
                executionTime, queryName, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Repository 메서드 실행 시간 측정 (반환값 없는 메서드용)
     */
    public static void measureAndLog(String queryName, Runnable queryExecution) {
        long startTime = System.currentTimeMillis();
        try {
            queryExecution.run();
            long executionTime = System.currentTimeMillis() - startTime;
            logQueryTime(queryName, executionTime);
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("❌ 쿼리 실행 실패 - {}ms | 쿼리: {} | 오류: {}", 
                executionTime, queryName, e.getMessage());
            throw e;
        }
    }
}
