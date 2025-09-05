package BlueCrab.com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class BlueCrabApplication extends SpringBootServletInitializer {
    
    /**
     * WAR 배포를 위한 configure 메서드
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(BlueCrabApplication.class);
    }
    
    /**
     * 메인 메서드 (JAR 실행용)
     */
    public static void main(String[] args) {
        SpringApplication.run(BlueCrabApplication.class, args);
    }
}
