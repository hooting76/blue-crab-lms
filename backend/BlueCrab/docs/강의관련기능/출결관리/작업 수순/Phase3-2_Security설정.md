# Phase 3-2: Security 설정

## 📋 작업 개요

**목표**: Spring Security 권한 설정 및 CORS 설정  
**예상 소요 시간**: 0.5시간  
**상태**: ✅ 완료 (2025-10-23)

---

## 🎯 작업 내용

### 1. SecurityConfig 권한 설정

**파일**: `src/main/java/BlueCrab/com/example/config/SecurityConfig.java`

```java
// 출석 API 권한 설정 추가
.requestMatchers("/api/attendance/request").authenticated()
.requestMatchers("/api/attendance/approve").hasAnyRole("PROFESSOR", "ADMIN")
.requestMatchers("/api/attendance/student/view").authenticated()
.requestMatchers("/api/attendance/professor/view").hasAnyRole("PROFESSOR", "ADMIN")
```

### 2. CORS 설정 (web.xml)

기존 CORS 필터 매핑 `/api/*`가 출석 API를 모두 커버하므로 추가 설정 불필요:
```xml
<filter-mapping>
    <filter-name>CorsFilter</filter-name>
    <url-pattern>/api/*</url-pattern>
</filter-mapping>
```

---

## 📋 체크리스트

- [x] SecurityConfig에 권한 규칙 추가
- [x] OPTIONS 요청 허용 (이미 설정됨)
- [x] CORS 필터 매핑 확인 (이미 설정됨)
- [x] 권한 규칙 검증

---

## 🎯 다음 단계

**Phase 4-1: 자동 승인 스케줄러 구현**

---

## 📚 참고 문서

- SecurityConfig.java
- web.xml
