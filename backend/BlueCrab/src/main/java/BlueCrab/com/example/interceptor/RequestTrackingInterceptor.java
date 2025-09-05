package BlueCrab.com.example.interceptor;

import BlueCrab.com.example.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * 요청 추적 인터셉터
 * 각 HTTP 요청에 대한 추적 및 모니터링을 수행하는 Spring MVC 인터셉터
 *
 * 주요 기능:
 * - 고유한 요청 ID 생성 및 추적
 * - 요청 처리 시간 측정 및 성능 모니터링
 * - MDC를 통한 로그 상관관계 추적
 * - 느린 요청 자동 감지 및 경고
 * - 클라이언트 정보 로깅 (IP, User-Agent)
 *
 * 작동 방식:
 * 1. 요청 시작 시: 요청 ID 생성, MDC 설정, 시작 시간 기록
 * 2. 요청 처리 중: 모든 로그에 MDC 컨텍스트 자동 포함
 * 3. 요청 완료 시: 소요 시간 계산, 성능 분석, MDC 정리
 *
 * MDC (Mapped Diagnostic Context) 활용:
 * - 모든 로그에 요청별 컨텍스트 정보 자동 포함
 * - 로그 상관관계 추적으로 디버깅 및 모니터링 용이
 * - 멀티스레드 환경에서도 안전한 컨텍스트 관리
 *
 * 성능 모니터링:
 * - 요청별 소요 시간 측정
 * - 1초 이상의 느린 요청 자동 감지
 * - 응답 상태 코드별 로그 레벨 조정
 *
 * 보안 고려사항:
 * - 로그에 민감한 정보 노출하지 않음
 * - MDC는 요청 완료 후 즉시 정리하여 메모리 누수 방지
 * - 클라이언트 IP는 보안 감사에 사용될 수 있으나, GDPR 등 고려
 *
 * 사용 예시:
 * - 모든 HTTP 요청에 자동 적용되어 성능 모니터링
 * - 로그에서 특정 요청 ID로 모든 관련 로그 검색 가능
 * - 느린 요청 감지로 DB 쿼리 최적화 포인트 식별
 *
 * 설정 방법:
 * WebMvcConfigurer에서 addInterceptors()로 등록
 * 또는 @Component로 자동 스캔되어 적용
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class RequestTrackingInterceptor implements HandlerInterceptor {
    
    /**
     * 인터셉터 전용 로거
     * 요청 추적 관련 로그를 별도 분리하여 모니터링 용이
     */
    private static final Logger logger = LoggerFactory.getLogger(RequestTrackingInterceptor.class);
    
    /**
     * MDC 키 상수 정의
     * 로그 컨텍스트 키를 상수로 정의하여 오타 방지 및 유지보수성 향상
     */
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String START_TIME_KEY = "startTime";
    private static final String REQUEST_URI_KEY = "requestUri";
    private static final String REQUEST_METHOD_KEY = "requestMethod";
    
    /**
     * 요청 전처리 메서드
     * 각 HTTP 요청 시작 시 호출되어 추적 준비 작업 수행
     *
     * 수행 작업:
     * 1. 고유한 요청 ID 생성 (짧은 UUID 형식)
     * 2. MDC에 요청 컨텍스트 정보 설정
     * 3. 요청 시작 시간 기록
     * 4. 요청 시작 로그 기록
     *
     * MDC 설정 정보:
     * - requestId: 요청 추적을 위한 고유 식별자
     * - requestUri: 요청 URI (엔드포인트 경로)
     * - requestMethod: HTTP 메서드 (GET, POST 등)
     *
     * 보안 고려사항:
     * - 요청 ID는 예측 불가능한 UUID로 생성하여 추적 방지
     * - 민감한 헤더 정보는 MDC에 포함하지 않음
     *
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param handler 요청을 처리할 핸들러 객체
     * @return true면 요청 계속 진행, false면 요청 중단
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        // 요청 ID 생성 (UUID 또는 단순 증가 ID)
        String requestId = generateRequestId();
        long startTime = System.currentTimeMillis();
        
        // MDC에 컨텍스트 정보 설정 (모든 로그에 자동 포함됨)
        MDC.put(REQUEST_ID_KEY, requestId);
        MDC.put(REQUEST_URI_KEY, request.getRequestURI());
        MDC.put(REQUEST_METHOD_KEY, request.getMethod());
        
        // 요청 속성에 시작 시간 저장
        request.setAttribute(START_TIME_KEY, startTime);
        
        // 디버깅을 위한 MDC 값 확인
        logger.info("🔍 MDC 설정 완료 - requestId: [{}], method: [{}], uri: [{}]", 
            MDC.get(REQUEST_ID_KEY), MDC.get(REQUEST_METHOD_KEY), MDC.get(REQUEST_URI_KEY));
        
        // 요청 시작 로그
        logger.info("요청 시작 - URI: {} {}, IP: {}, UserAgent: {}", 
            request.getMethod(), 
            request.getRequestURI(),
            RequestUtils.getClientIpAddress(request),
            request.getHeader("User-Agent"));
        
        return true;
    }
    
    /**
     * 요청 후처리 메서드
     * 각 HTTP 요청 완료 시 호출되어 추적 마무리 작업 수행
     *
     * 수행 작업:
     * 1. 요청 처리 시간 계산
     * 2. 응답 상태에 따른 로그 기록
     * 3. 느린 요청 감지 및 경고
     * 4. MDC 컨텍스트 정리 (메모리 누수 방지)
     *
     * 성능 분석 로직:
     * - 소요 시간이 1초를 초과하면 느린 요청으로 간주
     * - 4xx/5xx 응답은 경고 레벨로 로깅
     * - 정상 응답(2xx/3xx)은 정보 레벨로 로깅
     *
     * 메모리 관리:
     * - MDC.clear()를 finally 블록에서 호출하여 반드시 실행
     * - 스레드 로컬 변수 누수 방지
     *
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param handler 요청을 처리한 핸들러 객체
     * @param ex 요청 처리 중 발생한 예외 (없으면 null)
     */
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                               @NonNull Object handler, @Nullable Exception ex) {
        try {
            // 요청 처리 시간 계산
            Long startTime = (Long) request.getAttribute(START_TIME_KEY);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                
                // 응답 상태에 따른 로그 레벨 결정
                if (response.getStatus() >= 400) {
                    logger.warn("요청 완료 - 상태: {}, 소요시간: {}ms, 예외: {}", 
                        response.getStatus(), duration, ex != null ? ex.getMessage() : "없음");
                } else {
                    logger.info("요청 완료 - 상태: {}, 소요시간: {}ms", response.getStatus(), duration);
                }
                
                // 성능 모니터링을 위한 느린 요청 감지 (1초 이상)
                if (duration > 1000) {
                    logger.warn("느린 요청 감지 - 소요시간: {}ms, URI: {} {}", 
                        duration, request.getMethod(), request.getRequestURI());
                }
            }
        } finally {
            // MDC 정리 (메모리 누수 방지)
            MDC.clear();
        }
    }
    
    /**
     * 요청 ID 생성 메서드
     * 각 요청에 대한 고유 식별자를 생성
     *
     * 생성 방식:
     * - UUID를 생성한 후 앞 8자리만 사용
     * - 대문자로 변환하여 가독성 향상
     *
     * 장점:
     * - 충돌 가능성이 극히 낮음
     * - 로그에서 식별하기 쉬운 적절한 길이
     * - 예측 불가능하여 보안성 향상
     *
     * 예시 출력: "A1B2C3D4"
     *
     * @return 8자리 대문자 요청 ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
