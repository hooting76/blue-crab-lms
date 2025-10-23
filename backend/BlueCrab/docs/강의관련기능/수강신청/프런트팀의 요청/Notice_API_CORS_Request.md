# 🚀 수강신청 안내문 API — CORS 허용 요청 문서

## 🎯 요청 배경

프론트엔드(`https://bluecrab-front-test.chickenkiller.com`)에서
백엔드(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/notice/course-apply/view`)로
**POST 요청 시 CORS preflight(OPTIONS)** 가 403 Forbidden으로 차단되는 현상이 발생하고 있습니다.

브라우저는 `POST` 요청을 보내기 전 자동으로 `OPTIONS` 사전 요청(preflight)을 수행하며,  
이 요청이 허용되지 않으면 실제 API 호출이 불가능합니다.

---

## 🧭 요청 목표

백엔드(Spring Boot)에서 다음 설정을 추가하여 CORS 문제를 해결하고,  
프론트엔드에서 정상적으로 수강신청 안내문을 조회/저장할 수 있도록 해주세요.

---

## ✅ 요청 사항 요약

| 항목 | 내용 |
|------|------|
| 대상 경로 | `/notice/**` |
| 허용 Origin | `https://bluecrab-front-test.chickenkiller.com` |
| 허용 메서드 | `POST`, `OPTIONS` |
| 허용 헤더 | `Content-Type`, `Authorization` |
| Credentials | `true` (필요 시) |
| Preflight 캐시 | `maxAge: 3600` 이상 |
| 보안 설정 | `http.cors()` 활성화 및 `OPTIONS` 허용 |

---

## ⚙️ 적용 코드 예시

### 1️⃣ CORS 전역 설정

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
            "https://bluecrab-front-test.chickenkiller.com"
        ));
        cfg.setAllowedMethods(List.of("POST", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/notice/**", cfg);
        return source;
    }
}
```

---

### 2️⃣ Security 설정 보완

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/notice/course-apply/view").permitAll()
                .requestMatchers("/notice/course-apply/save").hasAnyRole("ADMIN", "PROFESSOR")
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

---

### 3️⃣ (임시 대응) Controller 단 @CrossOrigin

> ※ 전역 설정이 적용되지 않는 환경에서는 임시로 아래 추가 가능

```java
@RestController
@RequestMapping("/notice/course-apply")
@CrossOrigin(
    origins = { "https://bluecrab-front-test.chickenkiller.com" },
    methods = { RequestMethod.POST },
    allowedHeaders = { "Content-Type", "Authorization" }
)
public class NoticeController {
    // 기존 코드 그대로 유지
}
```

---

## 🔍 적용 후 확인 절차

1. 서버 재시작 후 브라우저 캐시 삭제 (Disable Cache 체크)
2. `POST /notice/course-apply/view` 호출 → Status 200 확인
3. 응답 헤더에서 `Access-Control-Allow-Origin` 값 확인
4. 관리자 권한으로 `POST /notice/course-apply/save` 호출 테스트

---

## 🧩 참고사항

- 프론트는 모든 API를 **POST 기반 통신**으로 유지 중
- `/view` 는 공개(permitAll), `/save` 는 관리자/교수 권한 제한
- 추가적인 GET 방식 엔드포인트는 필요하지 않음

---

## 📞 담당자 메모

> 프론트엔드에서 발생하는 CORS 오류는 브라우저의 보안 정책으로 인한 것으로,  
> 백엔드에서 `OPTIONS` 요청을 허용해야 해결됩니다.  
> 프론트 코드 수정으로는 해결 불가하므로, 본 설정 적용이 필수입니다.
