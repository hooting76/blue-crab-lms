# 수강신청 API HTTP 400 에러 수정 가이드

## 🔴 문제 상황

### 에러 발생
- **엔드포인트**: `GET /api/enrollments?studentIdx=6&page=0&size=10`
- **HTTP 상태**: 400 (Bad Request)
- **에러 메시지**: 
```
Could not write JSON: could not initialize proxy [BlueCrab.com.example.entity.Lecture.LecTbl#6] 
- no Session (through reference chain: org.springframework.data.domain.PageImpl["content"]
->java.util.Collections$UnmodifiableRandomAccessList[0]
->BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl["lecture"]
->BlueCrab.com.example.entity.Lecture.LecTbl$HibernateProxy$1ytZLU91["lecProf"])
```

### 원인 분석
1. `EnrollmentExtendedTbl` 엔티티가 직접 JSON으로 직렬화됨
2. `@ManyToOne(fetch = FetchType.LAZY)` 관계의 `lecture`, `student` 필드
3. Hibernate 세션이 닫힌 후 Lazy Loading 시도
4. Jackson이 Hibernate Proxy 객체를 직렬화하지 못함

---

## ✅ 해결 방법 (3가지 옵션)

### 방법 1: DTO 패턴 사용 (권장 ⭐)

#### 1-1. EnrollmentDto 활용
기존 `EnrollmentDto.java`가 이미 존재하므로 이를 활용:

```java
// EnrollmentController.java 수정
@GetMapping
public ResponseEntity<?> getEnrollments(
        @RequestParam(required = false) Integer studentIdx,
        @RequestParam(required = false) Integer lecIdx,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    try {
        // 4. 학생별 수강 목록 (페이징)
        if (studentIdx != null && !enrolled) {
            Pageable pageable = PageRequest.of(page, size);
            Page<EnrollmentExtendedTbl> enrollments = 
                    enrollmentService.getEnrollmentsByStudentPaged(studentIdx, pageable);
            
            // DTO로 변환 추가
            Page<EnrollmentDto> dtoPage = enrollments.map(this::convertToDto);
            return ResponseEntity.ok(dtoPage);
        }
        // ... 나머지 케이스도 동일하게 DTO 변환
    } catch (Exception e) {
        // ...
    }
}

// DTO 변환 메서드 추가
private EnrollmentDto convertToDto(EnrollmentExtendedTbl entity) {
    return new EnrollmentDto(
        entity.getEnrollmentIdx(),
        entity.getLecIdx(),
        entity.getLecture() != null ? entity.getLecture().getLecSerial() : null,
        entity.getLecture() != null ? entity.getLecture().getLecTit() : null,
        entity.getLecture() != null ? entity.getLecture().getLecProf() : null,
        entity.getStudentIdx(),
        entity.getStudent() != null ? entity.getStudent().getUserName() : null,
        entity.getEnrollmentReg(),
        entity.getEnrollmentData()
    );
}
```

**장점**: 
- API 응답 데이터 제어 가능
- 불필요한 필드 노출 방지
- 성능 최적화 가능

**단점**: 
- 변환 코드 추가 필요

---

### 방법 2: @JsonIgnoreProperties 사용

#### 2-1. 엔티티에 어노테이션 추가

```java
// EnrollmentExtendedTbl.java 수정
@Entity
@Table(name = "ENROLLMENT_EXTENDED_TBL")
@JsonIgnoreProperties({"lecture", "student", "hibernateLazyInitializer", "handler"})
public class EnrollmentExtendedTbl {
    // ... 기존 코드
}
```

**장점**: 
- 간단한 수정
- 즉시 적용 가능

**단점**: 
- `lecture`, `student` 정보가 JSON에 포함되지 않음
- 프론트엔드에서 강의/학생 정보 조회 불가

---

### 방법 3: Jackson Hibernate5 Module 설정

#### 3-1. 의존성 추가 (pom.xml)

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-hibernate5</artifactId>
</dependency>
```

#### 3-2. Configuration 클래스 추가

```java
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    
    @Bean
    public Hibernate5Module hibernate5Module() {
        Hibernate5Module module = new Hibernate5Module();
        // Lazy Loading된 필드는 null로 직렬화
        module.disable(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION);
        module.enable(Hibernate5Module.Feature.FORCE_LAZY_LOADING);
        return module;
    }
}
```

또는 `application.properties`에 추가:

```properties
spring.jackson.serialization.fail-on-empty-beans=false
```

**장점**: 
- 전역 설정으로 모든 엔티티에 적용
- Lazy Loading 필드 자동 처리

**단점**: 
- 성능 이슈 가능 (FORCE_LAZY_LOADING 사용 시)
- N+1 쿼리 문제 발생 가능

---

## 🎯 권장 해결 방법

### 최종 권장: **방법 1 (DTO 패턴) + 방법 2 (임시 조치)**

1. **즉시 조치** (방법 2):
   ```java
   @JsonIgnoreProperties({"lecture", "student", "hibernateLazyInitializer"})
   ```
   → 400 에러 즉시 해결

2. **장기 개선** (방법 1):
   - Service Layer에서 DTO 변환
   - 필요한 정보만 조회하여 반환
   - 성능 최적화

---

## 📝 테스트 방법

### 수정 전 테스트
```javascript
// 브라우저 콘솔에서 실행
await getMyEnrollments()
// → HTTP 400 에러 발생
```

### 수정 후 테스트
```javascript
// 브라우저 콘솔에서 실행
await getMyEnrollments()
// → HTTP 200 성공, 수강 목록 조회됨
```

---

## 🔍 관련 파일

- **Controller**: `EnrollmentController.java`
- **Entity**: `EnrollmentExtendedTbl.java`
- **DTO**: `EnrollmentDto.java` (이미 존재)
- **Service**: `EnrollmentService.java`
- **프론트 테스트**: `lecture-test-2-student-enrollment.js`

---

## 📚 참고 자료

- [Jackson Hibernate Module 문서](https://github.com/FasterXML/jackson-datatype-hibernate)
- [Spring Data JPA DTO Projection](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#projections)
- [Hibernate Lazy Loading Best Practices](https://vladmihalcea.com/the-best-way-to-handle-the-lazyinitializationexception/)

---

## ✅ 체크리스트

- [ ] 방법 선택 (1, 2, 또는 3)
- [ ] 코드 수정
- [ ] 로컬 테스트
- [ ] 빌드 확인
- [ ] 프론트엔드 테스트
- [ ] 커밋 & 푸시

---

작성일: 2025-10-14  
작성자: GitHub Copilot
