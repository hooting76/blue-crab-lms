package BlueCrab.com.example.config;

import BlueCrab.com.example.interceptor.RequestTrackingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 웹 설정
 * - 인터셉터 등록
 * - 정적 리소스 처리 등
 * 
 * 주의: CORS 설정은 SecurityConfig에서 처리됩니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private RequestTrackingInterceptor requestTrackingInterceptor;
    
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // RequestTrackingInterceptor 활성화 - MDC 로그 컨텍스트 추적용
        registry.addInterceptor(requestTrackingInterceptor)
                .addPathPatterns("/api/**") // API 경로에만 적용
                .excludePathPatterns("/api/health"); // 헬스체크만 제외
    }
}
