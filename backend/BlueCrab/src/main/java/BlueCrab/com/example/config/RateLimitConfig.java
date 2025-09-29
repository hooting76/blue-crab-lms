package BlueCrab.com.example.config;

import BlueCrab.com.example.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Rate Limit 인터셉터 설정
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {
    
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**") // API 경로에만 적용
                .excludePathPatterns("/api/auth/**"); // 인증 관련 API는 제외
    }
}