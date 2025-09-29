package BlueCrab.com.example.interceptor;

import BlueCrab.com.example.annotation.RateLimit;
import BlueCrab.com.example.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 요청 빈도 제한 인터셉터
 * @RateLimit 어노테이션이 붙은 메서드에 대해 요청 빈도를 제한합니다.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    // Redis가 없는 경우 메모리 기반 저장소 사용
    private final Map<String, RequestInfo> requestStore = new ConcurrentHashMap<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        
        if (rateLimit == null) {
            return true; // Rate Limit 어노테이션이 없으면 통과
        }
        
        String key = generateKey(request, rateLimit.keyType());
        if (key == null) {
            return true; // 키 생성 실패 시 통과 (인증 오류는 다른 곳에서 처리)
        }
        
        if (isRateLimited(key, rateLimit)) {
            // 요청 제한 응답
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", rateLimit.message());
            errorResponse.put("errorCode", "RATE_LIMIT_EXCEEDED");
            errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            
            logger.warn("Rate limit exceeded for key: {} on endpoint: {}", key, request.getRequestURI());
            return false;
        }
        
        return true;
    }
    
    /**
     * 요청 제한 여부 확인 및 카운트 증가
     */
    private boolean isRateLimited(String key, RateLimit rateLimit) {
        long currentTime = System.currentTimeMillis();
        long timeWindow = rateLimit.timeWindow() * 1000L; // 초를 밀리초로 변환
        
        RequestInfo requestInfo = requestStore.get(key);
        
        if (requestInfo == null) {
            // 첫 번째 요청
            requestStore.put(key, new RequestInfo(currentTime, 1));
            return false;
        }
        
        // 시간 윈도우가 지났으면 리셋
        if (currentTime - requestInfo.firstRequestTime > timeWindow) {
            requestStore.put(key, new RequestInfo(currentTime, 1));
            return false;
        }
        
        // 최대 요청 횟수 초과 확인
        if (requestInfo.requestCount >= rateLimit.maxRequests()) {
            return true; // 제한됨
        }
        
        // 카운트 증가
        requestInfo.requestCount++;
        return false;
    }
    
    /**
     * Rate Limit 키 생성
     */
    private String generateKey(HttpServletRequest request, RateLimit.KeyType keyType) {
        String endpoint = request.getRequestURI();
        
        switch (keyType) {
            case IP:
                String clientIp = getClientIpAddress(request);
                return "rate_limit:ip:" + clientIp + ":" + endpoint;
                
            case USER:
                String userCode = getUserCodeFromRequest(request);
                if (userCode == null) {
                    return null; // 인증되지 않은 사용자
                }
                return "rate_limit:user:" + userCode + ":" + endpoint;
                
            default:
                return null;
        }
    }
    
    /**
     * JWT 토큰에서 사용자 코드 추출
     */
    private String getUserCodeFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.validateToken(token)) {
                    // JWT에서 사용자 정보 추출 (기존 방식 활용)
                    String username = jwtUtil.extractUsername(token);
                    return username; // username을 userCode로 사용
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract user code from token", e);
        }
        return null;
    }
    
    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 요청 정보 저장 클래스
     */
    private static class RequestInfo {
        long firstRequestTime;
        int requestCount;
        
        RequestInfo(long firstRequestTime, int requestCount) {
            this.firstRequestTime = firstRequestTime;
            this.requestCount = requestCount;
        }
    }
}