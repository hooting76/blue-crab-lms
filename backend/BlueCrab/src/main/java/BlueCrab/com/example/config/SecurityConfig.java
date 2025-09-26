package BlueCrab.com.example.config;

import BlueCrab.com.example.security.JwtAuthenticationEntryPoint;
import BlueCrab.com.example.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// 성태준 추가 임포트
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Spring Security 설정 클래스
 * JWT 기반 인증을 구성하는 중앙 보안 설정 클래스
 * 
 * 주요 기능:
 * - JWT 토큰 기반 인증 시스템 구성
 * - 비밀번호 암호화 알고리즘 설정
 * - 세션리스(stateless) 인증 방식 적용
 * - 역할 기반 접근 제어 및 권한 관리
 * 
 * 보안 아키텍처:
 * 1. JwtAuthenticationFilter: 모든 요청에서 JWT 토큰 검증
 * 2. JwtAuthenticationEntryPoint: 인증 실패 시 401 응답
 * 3. PasswordEncoder: 사용자 비밀번호 암호화
 * 4. SecurityFilterChain: HTTP 보안 규칙 체인 구성
 * 
 * ⚠️ 참고: 
 * - CORS는 Tomcat web.xml 필터에서 처리
 * - MessageDigestPasswordEncoder 사용으로 인한 deprecation 경고는
 *   클래스 레벨에서 억제 처리됨 (향후 업그레이드 예정)
 * 
 * 🔄 미래 업그레이드 계획:
 * - Spring Security 6.0+ 호환성 확보
 * - BCryptPasswordEncoder로 암호화 알고리즘 업그레이드
 * - OAuth2 지원 추가 검토
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@SuppressWarnings("deprecation") // MessageDigestPasswordEncoder 경고 억제
public class SecurityConfig {

    /**
     * JWT 인증 실패 시 처리할 엔트리 포인트
     * 401 Unauthorized 응답을 반환하는 역할을 담당
     */
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * JWT 토큰을 검증하고 인증 정보를 설정하는 필터
     * 모든 요청에서 JWT 토큰을 추출하여 유효성을 검증
     */
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * 애플리케이션 설정 정보를 담은 구성 객체
     * JWT, 보안, 데이터베이스 등의 설정값을 제공
     */
    @Autowired
    private AppConfig appConfig;

    /**
     * 비밀번호 인코더 빈 등록
     * 현재는 SHA-256 사용 (개발 및 테스트용)
     * 
     * ⚠️ DEPRECATED 경고: MessageDigestPasswordEncoder는 Spring Security 6.0+에서 제거 예정
     * 
     * 🔄 업그레이드 계획:
     * - Phase 1: BCryptPasswordEncoder로 전환 (보안성 향상)
     * - Phase 2: Argon2PasswordEncoder로 최종 업그레이드 (최신 보안 표준)
     * 
     * 현재 SHA-256을 사용하는 이유:
     * - 기존 사용자 데이터와의 호환성 유지
     * - 점진적 마이그레이션을 위한 과도기적 사용
     * 
     * @return 설정된 암호화 알고리즘에 기반한 PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        String algorithm = appConfig.getSecurity().getPasswordEncodingAlgorithm();
        // TODO: Spring Security 6.0 업그레이드 시 BCryptPasswordEncoder로 교체
        return new MessageDigestPasswordEncoder(algorithm);
    }

    /**
     * 인증 매니저 빈 등록
     * Spring Security의 인증 처리를 담당
     * 
     * @param config 인증 설정 객체
     * @return AuthenticationManager 인스턴스
     * @throws Exception 인증 매니저 생성 오류
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // REST API이므로 CSRF 보호 비활성화
            // CORS는 Tomcat web.xml에서 처리하므로 완전히 비활성화
            .cors(cors -> cors.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)) // 인증 실패 시 처리
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션리스 JWT 방식 사용
            .authorizeHttpRequests(auth -> auth
                // 🔓 공개 엔드포인트 (인증 불필요)
                .requestMatchers("/api/auth/**").permitAll() // 로그인, 회원가입, 토큰 갱신 등
                .requestMatchers("/api/auth/password-reset/**").permitAll() // 비밀번호 재설정 시스템 (본인확인, 이메일발송, 코드검증, 비밀번호변경)
                .requestMatchers("/api/account/FindId/**").permitAll() // ID 찾기 기능 (인증 불필요)
                .requestMatchers("/api/admin/login").permitAll() // 어드민 1차 로그인 허용
                .requestMatchers("/api/admin/verify-email").permitAll() // 어드민 이메일 인증 허용
                .requestMatchers("/api/admin/email-auth/**").permitAll() // 어드민 이메일 인증코드 시스템 허용
                .requestMatchers("/api/ping").permitAll() // 연결 테스트 엔드포인트
                .requestMatchers("/", "/status").permitAll() // 메인 페이지 및 상태 페이지
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll() // 정적 리소스
                
                // � 게시판 조회 API (임시로 모두 허용 - 디버깅용)
                .requestMatchers("/api/boards/**").permitAll() //
                
                // 🌐 CORS Preflight 요청 허용 (중요!)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // 모든 OPTIONS 요청 허용
                
                // � 프로필 API (인증된 사용자만 접근 가능)
                .requestMatchers(HttpMethod.POST, "/api/profile/me").authenticated() // 프로필 조회
                .requestMatchers(HttpMethod.POST, "/api/profile/me/completeness").authenticated() // 프로필 완성도 체크
                .requestMatchers(HttpMethod.GET, "/api/profile/me/image/**").authenticated() // 프로필 이미지 조회
                
                // �🔧 관리자 전용 엔드포인트 (현재 임시로 개방)
                .requestMatchers("/admin/logs/**").permitAll() // 로그 모니터링 (TODO: ADMIN 권한 필요)
                .requestMatchers("/admin/metrics/**").permitAll() // 메트릭 정보 (TODO: ADMIN 권한 필요)
                
                // 🔒 보호된 엔드포인트 (인증 필수)
                .requestMatchers("/api/**").authenticated() // 모든 API 요청은 인증 필요
                
                
                // 성태준 추가, 이메일 인증 관련 API 보호
                // "/sendMail" 엔드포인트는 인증된 사용자만 접근 가능하도록 설정
                .requestMatchers("/sendMail").authenticated() // 이메일 인증 API 보호
                .requestMatchers("/BlueCrab-1.0.0/sendMail").authenticated() // 이메일 인증 API 보호
                .requestMatchers("/verifyCode").authenticated() // 인증 코드 확인 API 보호
                .requestMatchers("/BlueCrab-1.0.0/verifyCode").authenticated() // 인증 코드 확인 API 보호
                
                // 성태준 추가, 비밀번호 재설정 기능 
                // ⚠️ 보안 정책: 구체적 경로만 허용하여 의도하지 않은 엔드포인트 노출 방지
                // 심플하게 말 해 permitAll()에 있어서는 "/**" 같은 경로는 쓰지 말라는 뜻
                .requestMatchers("/api/auth/password-reset/send-email").permitAll() // 인증코드 발송 (인증 불필요)
                .requestMatchers("/api/auth/password-reset/verify-identity").permitAll() // 신원 확인 (미구현, 향후 추가용)
                // 📝 아직 미 작성 된 단계에서 쓰일 엔드포인트:
                // .requestMatchers("/api/password-reset/verify-code").permitAll() // 인증코드 검증
                .requestMatchers("/api/auth/password-reset/change-password").permitAll() // 비밀번호 변경



                // 🌐 기타 요청 처리
                .anyRequest().permitAll() // 위에서 지정하지 않은 모든 요청 허용
            );

        // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 전에 실행
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 성태준 추가, BCryptPasswordEncoder 빈 등록
    // 다른 곳에서 BCrypt가 필요한 경우를 위해 유지
    @Bean
    @Qualifier("bcryptEncoder")
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 설정 소스 빈 등록 (현재 비활성화)
     * 
     * Tomcat web.xml에서 CORS를 처리하므로 Spring Boot에서는 비활성화합니다.
     * 중복된 CORS 헤더 방지를 위해 주석 처리되었습니다.
     * 
     * @return 구성된 CORS 설정 소스
     */
    /*
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 프론트엔드 도메인만 명시적으로 허용 (보안 강화)
        configuration.setAllowedOrigins(Arrays.asList(
            "https://dtmch.synology.me:56000",        // 프론트엔드 웹주소
            "https://bluecrab.chickenkiller.com"      // 백엔드 도메인 (테스트 페이지용)
        ));
        
        // 허용할 HTTP 메서드 설정 (REST API에 필요한 모든 메서드)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 허용할 요청 헤더 설정 (Authorization 명시적 포함)
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",    // JWT 토큰 헤더 (반드시 명시)
            "Content-Type",     // 요청 본문 타입
            "X-Requested-With", // AJAX 요청 식별
            "Accept",          // 응답 타입 선호도
            "Origin"           // 출처 정보
        ));
        
        // JWT 기반 인증이므로 credentials는 false (쿠키 사용 안함)
        configuration.setAllowCredentials(false);
        
        // CORS 정책 캐시 시간 설정 (브라우저에서 재검증 없이 사용)
        configuration.setMaxAge(3600L); // 1시간 캐시
        
        // 모든 경로에 CORS 정책 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    */
}
