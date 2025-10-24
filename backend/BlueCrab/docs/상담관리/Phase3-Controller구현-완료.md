# Phase 3: Controller Layer 구현 완료 보고서

**작업일**: 2025-10-24
**담당자**: BlueCrab Development Team
**상태**: ✅ 완료

---

## 📋 작업 개요

상담 요청/진행 관리 시스템의 Controller Layer를 구현하고 Spring Security 설정을 완료했습니다.

### 주요 산출물

1. **ConsultationController** - REST API Controller (14개 엔드포인트)
2. **SecurityConfig 업데이트** - 상담 API 권한 규칙 추가

---

## 🎯 구현 완료 항목

### 1. REST API 엔드포인트 (14개)

**파일**: [ConsultationController.java](../../src/main/java/BlueCrab/com/example/controller/ConsultationController.java) (620줄)

#### 상담 요청 관리 (4개)

| 메서드 | 엔드포인트 | 권한 | 설명 |
|--------|-----------|------|------|
| POST | `/api/consultation/request` | authenticated | 상담 요청 생성 (학생) |
| POST | `/api/consultation/approve` | PROFESSOR, ADMIN | 요청 승인 (교수) |
| POST | `/api/consultation/reject` | PROFESSOR, ADMIN | 요청 반려 (교수) |
| POST | `/api/consultation/cancel` | authenticated | 요청 취소 (학생) |

#### 상담 진행 관리 (3개)

| 메서드 | 엔드포인트 | 권한 | 설명 |
|--------|-----------|------|------|
| POST | `/api/consultation/start` | authenticated | 상담 시작 |
| POST | `/api/consultation/end` | authenticated | 상담 종료 |
| POST | `/api/consultation/memo` | PROFESSOR, ADMIN | 메모 작성/수정 (교수) |

#### 상담 조회 (5개)

| 메서드 | 엔드포인트 | 권한 | 설명 |
|--------|-----------|------|------|
| POST | `/api/consultation/my-requests` | authenticated | 내가 보낸 요청 목록 (학생) |
| POST | `/api/consultation/received` | PROFESSOR, ADMIN | 받은 요청 목록 (교수) |
| POST | `/api/consultation/active` | authenticated | 진행 중인 상담 목록 |
| POST | `/api/consultation/history` | authenticated | 완료된 상담 이력 |
| GET | `/api/consultation/{id}` | authenticated | 상담 상세 조회 |

#### 기타 (2개)

| 메서드 | 엔드포인트 | 권한 | 설명 |
|--------|-----------|------|------|
| GET | `/api/consultation/unread-count` | PROFESSOR, ADMIN | 읽지 않은 요청 개수 (교수) |
| POST | `/api/consultation/read` | authenticated | 읽음 시간 업데이트 |

---

## 📊 API 상세 명세

### 1. 상담 요청 생성

**엔드포인트**: `POST /api/consultation/request`

**Request Body**:
```json
{
  "recipientUserCode": "P001",
  "consultationType": "ACADEMIC",
  "title": "학점 상담 요청",
  "content": "학점 관리에 대해 상담받고 싶습니다.",
  "desiredDate": "2025-10-25T14:00:00"
}
```

**Response** (200 OK):
```json
{
  "requestIdx": 1,
  "requesterUserCode": "202012345",
  "requesterName": "홍길동",
  "recipientUserCode": "P001",
  "recipientName": "김교수",
  "consultationType": "ACADEMIC",
  "title": "학점 상담 요청",
  "content": "학점 관리에 대해 상담받고 싶습니다.",
  "desiredDate": "2025-10-25T14:00:00",
  "requestStatus": "PENDING",
  "createdAt": "2025-10-24T10:00:00"
}
```

**특징**:
- ✅ JWT 토큰에서 requesterUserCode 자동 추출
- ✅ 사용자 존재 여부 검증
- ✅ Validation 적용 (@Valid)

---

### 2. 상담 요청 승인

**엔드포인트**: `POST /api/consultation/approve`

**Request Body**:
```json
{
  "requestIdx": 1,
  "acceptMessage": "오후 2시에 연구실로 오세요."
}
```

**Response** (200 OK):
```json
{
  "requestIdx": 1,
  "requestStatus": "APPROVED",
  "consultationStatus": "SCHEDULED",
  "acceptMessage": "오후 2시에 연구실로 오세요.",
  "updatedAt": "2025-10-24T11:00:00"
}
```

**권한 검증**:
- ✅ 교수 권한 확인 (userStudent = 1)
- ❌ 학생 접근 시 403 Forbidden

---

### 3. 내가 보낸 요청 목록 조회

**엔드포인트**: `POST /api/consultation/my-requests?page=0&size=20&status=PENDING`

**Query Parameters**:
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 20)
- `status`: 요청 상태 필터 (선택, PENDING/APPROVED/REJECTED/CANCELLED)

**Response** (200 OK):
```json
{
  "content": [
    {
      "requestIdx": 1,
      "recipientName": "김교수",
      "consultationType": "ACADEMIC",
      "title": "학점 상담 요청",
      "requestStatus": "PENDING",
      "createdAt": "2025-10-24T10:00:00"
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**특징**:
- ✅ JWT에서 requesterUserCode 자동 추출
- ✅ 페이징 처리
- ✅ 상태별 필터링 지원

---

### 4. 상담 상세 조회

**엔드포인트**: `GET /api/consultation/{requestIdx}`

**Response** (200 OK):
```json
{
  "requestIdx": 1,
  "requesterUserCode": "202012345",
  "requesterName": "홍길동",
  "recipientUserCode": "P001",
  "recipientName": "김교수",
  "consultationType": "ACADEMIC",
  "title": "학점 상담 요청",
  "content": "학점 관리에 대해 상담받고 싶습니다.",
  "desiredDate": "2025-10-25T14:00:00",
  "requestStatus": "APPROVED",
  "acceptMessage": "오후 2시에 연구실로 오세요.",
  "consultationStatus": "SCHEDULED",
  "createdAt": "2025-10-24T10:00:00",
  "hasUnreadMessages": false
}
```

**권한 검증**:
- ✅ 참여자만 조회 가능 (requester 또는 recipient)
- ❌ 제3자 접근 시 403 Forbidden

---

### 5. 읽지 않은 요청 개수 조회

**엔드포인트**: `GET /api/consultation/unread-count`

**Response** (200 OK):
```json
{
  "recipientUserCode": "P001",
  "unreadCount": 5
}
```

**특징**:
- ✅ 교수 전용 API
- ✅ PENDING 상태 요청만 카운트

---

## 🔒 Security 설정

**파일**: [SecurityConfig.java](../../src/main/java/BlueCrab/com/example/config/SecurityConfig.java)

### 추가된 권한 규칙 (14개)

```java
// 💬 상담 요청/관리 API (인증 및 권한 필요)
.requestMatchers("/api/consultation/request").authenticated()              // 학생 상담 요청
.requestMatchers("/api/consultation/approve").hasAnyRole("PROFESSOR", "ADMIN")  // 교수 승인
.requestMatchers("/api/consultation/reject").hasAnyRole("PROFESSOR", "ADMIN")   // 교수 반려
.requestMatchers("/api/consultation/cancel").authenticated()               // 학생 취소
.requestMatchers("/api/consultation/start").authenticated()                // 상담 시작
.requestMatchers("/api/consultation/end").authenticated()                  // 상담 종료
.requestMatchers("/api/consultation/memo").hasAnyRole("PROFESSOR", "ADMIN")     // 교수 메모
.requestMatchers("/api/consultation/my-requests").authenticated()          // 내 요청 목록
.requestMatchers("/api/consultation/received").hasAnyRole("PROFESSOR", "ADMIN") // 받은 요청 목록
.requestMatchers("/api/consultation/active").authenticated()               // 진행 중인 상담
.requestMatchers("/api/consultation/history").authenticated()              // 상담 이력
.requestMatchers("/api/consultation/{id}").authenticated()                 // 상담 상세
.requestMatchers("/api/consultation/unread-count").hasAnyRole("PROFESSOR", "ADMIN") // 읽지 않은 개수
.requestMatchers("/api/consultation/read").authenticated()                 // 읽음 처리
```

### 권한 체계

| 권한 | 설명 | 확인 방법 |
|------|------|-----------|
| `authenticated` | 로그인한 모든 사용자 (학생/교수) | JWT 토큰 검증 |
| `PROFESSOR` | 교수 권한 | userStudent = 1 |
| `ADMIN` | 관리자 권한 | 역할 기반 |

---

## 💡 구현 특징

### 1. JWT 인증 통합

```java
// Authentication에서 사용자 정보 자동 추출
String userEmail = authentication.getName();
UserTbl user = userRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

// DTO에 자동 설정
createDto.setRequesterUserCode(user.getUserCode());
```

### 2. 권한 검증 Helper 메서드

```java
/**
 * 인증 확인 (학생/교수 공통)
 */
private UserTbl validateAuth(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
        throw new SecurityException("인증이 필요합니다.");
    }

    String userEmail = authentication.getName();
    return userRepository.findByUserEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
}

/**
 * 교수 권한 확인
 */
private UserTbl validateProfessorAuth(Authentication authentication) {
    UserTbl user = validateAuth(authentication);

    if (user.getUserStudent() != 1) {
        throw new SecurityException("교수 권한이 필요합니다.");
    }

    return user;
}
```

### 3. 통일된 에러 응답

```java
private Map<String, Object> createErrorResponse(String message) {
    Map<String, Object> error = new HashMap<>();
    error.put("success", false);
    error.put("message", message);
    error.put("timestamp", System.currentTimeMillis());
    return error;
}
```

**에러 응답 예시**:
```json
{
  "success": false,
  "message": "교수 권한이 필요합니다.",
  "timestamp": 1698134400000
}
```

### 4. 로깅 전략

```java
// 요청 시작 로그
log.info("상담 요청 생성: requester={}, recipient={}",
         createDto.getRequesterUserCode(), createDto.getRecipientUserCode());

// 성공 로그
log.info("상담 요청 생성 완료: requestIdx={}", result.getRequestIdx());

// 경고 로그
log.warn("권한 없음: {}", e.getMessage());

// 에러 로그
log.error("상담 요청 생성 실패", e);
```

---

## 📊 코드 통계

| 항목 | 값 |
|------|-----|
| Controller 메서드 수 | 14개 |
| Helper 메서드 수 | 3개 |
| 총 라인 수 | 620줄 |
| Security 규칙 추가 | 14개 |
| 지원 HTTP 메서드 | POST (12개), GET (2개) |
| 권한 레벨 | 2개 (authenticated, PROFESSOR) |

---

## 🧪 테스트 시나리오

### 1. 인증 테스트

**TC-001: 인증 없이 API 호출**
```bash
# Request
curl -X POST http://localhost:8080/api/consultation/request

# Expected Response: 401 Unauthorized
{
  "success": false,
  "message": "인증이 필요합니다.",
  "timestamp": 1698134400000
}
```

**TC-002: 유효한 JWT로 API 호출**
```bash
# Request
curl -X POST http://localhost:8080/api/consultation/request \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{...}'

# Expected Response: 200 OK
```

---

### 2. 권한 테스트

**TC-003: 학생이 교수 전용 API 호출**
```bash
# Request (학생 토큰)
curl -X POST http://localhost:8080/api/consultation/approve \
  -H "Authorization: Bearer [STUDENT_TOKEN]"

# Expected Response: 403 Forbidden
{
  "success": false,
  "message": "교수 권한이 필요합니다.",
  "timestamp": 1698134400000
}
```

**TC-004: 교수가 교수 전용 API 호출**
```bash
# Request (교수 토큰)
curl -X POST http://localhost:8080/api/consultation/approve \
  -H "Authorization: Bearer [PROFESSOR_TOKEN]" \
  -d '{"requestIdx": 1, "acceptMessage": "승인합니다."}'

# Expected Response: 200 OK
```

---

### 3. 기능 테스트

**TC-005: 상담 요청 생성**
```bash
curl -X POST http://localhost:8080/api/consultation/request \
  -H "Authorization: Bearer [TOKEN]" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientUserCode": "P001",
    "consultationType": "ACADEMIC",
    "title": "학점 상담",
    "content": "학점 관리 방법을 알고 싶습니다.",
    "desiredDate": "2025-10-25T14:00:00"
  }'
```

**TC-006: 페이징 조회**
```bash
curl -X POST "http://localhost:8080/api/consultation/my-requests?page=0&size=10&status=PENDING" \
  -H "Authorization: Bearer [TOKEN]"
```

**TC-007: 상세 조회 (권한 검증)**
```bash
# 참여자가 아닌 사용자
curl -X GET http://localhost:8080/api/consultation/1 \
  -H "Authorization: Bearer [OTHER_USER_TOKEN]"

# Expected: 403 Forbidden
```

---

## 🔍 에러 처리

### HTTP 상태 코드

| 상태 코드 | 시나리오 | 응답 예시 |
|-----------|---------|-----------|
| 200 OK | 정상 처리 | `ConsultationRequestDto` |
| 400 Bad Request | Validation 실패, 비즈니스 로직 오류 | `{"success": false, "message": "..."}` |
| 401 Unauthorized | 인증 실패 (토큰 없음/만료) | `{"success": false, "message": "인증이 필요합니다."}` |
| 403 Forbidden | 권한 부족 | `{"success": false, "message": "교수 권한이 필요합니다."}` |
| 404 Not Found | 리소스 없음 | `{"success": false, "message": "상담 요청을 찾을 수 없습니다."}` |
| 500 Internal Server Error | 서버 오류 | `{"success": false, "message": "서버 오류가 발생했습니다."}` |

### Exception 처리 흐름

```java
try {
    // 1. 인증 확인
    UserTbl user = validateAuth(authentication);

    // 2. Service 호출
    ConsultationRequestDto result = consultationService.createRequest(createDto);

    // 3. 성공 응답
    return ResponseEntity.ok(result);

} catch (SecurityException e) {
    // 권한 오류
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(createErrorResponse(e.getMessage()));

} catch (Exception e) {
    // 일반 오류
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse("..."));
}
```

---

## 📝 프론트엔드 연동 가이드

### 1. API 호출 예시 (JavaScript)

```javascript
// 상담 요청 생성
async function createConsultationRequest(data) {
  const response = await fetch('/api/consultation/request', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${getToken()}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      recipientUserCode: data.professorCode,
      consultationType: data.type,
      title: data.title,
      content: data.content,
      desiredDate: data.desiredDate
    })
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }

  return await response.json();
}

// 내 요청 목록 조회
async function getMyRequests(page = 0, size = 20, status = null) {
  const params = new URLSearchParams({ page, size });
  if (status) params.append('status', status);

  const response = await fetch(`/api/consultation/my-requests?${params}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${getToken()}`
    }
  });

  return await response.json();
}

// 상담 요청 승인 (교수)
async function approveRequest(requestIdx, message) {
  const response = await fetch('/api/consultation/approve', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${getToken()}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      requestIdx: requestIdx,
      acceptMessage: message
    })
  });

  return await response.json();
}
```

### 2. 에러 처리

```javascript
try {
  const result = await createConsultationRequest(formData);
  alert('상담 요청이 완료되었습니다.');
} catch (error) {
  if (error.message.includes('인증')) {
    // 로그인 페이지로 이동
    window.location.href = '/login';
  } else if (error.message.includes('권한')) {
    alert('권한이 없습니다.');
  } else {
    alert('오류가 발생했습니다: ' + error.message);
  }
}
```

---

## 🚀 다음 단계: Phase 4

### Scheduler 구현 (예정)

**목표**: 자동 종료 기능 스케줄러 구현

**주요 작업**:

1. **ConsultationScheduler 클래스 생성**
   - `@Scheduled` 어노테이션 활용
   - Cron 표현식으로 실행 주기 설정

2. **자동 종료 작업**
   - 2시간 비활성 상담 종료 (매 1시간마다)
   - 24시간 장시간 실행 상담 종료 (매일 오전 5시)

3. **스케줄러 설정**
```java
@Scheduled(cron = "0 0 * * * *") // 매 시간
public void autoEndInactiveConsultations() {
    int count = consultationService.autoEndInactiveConsultations();
    log.info("비활성 상담 {}건 자동 종료", count);
}

@Scheduled(cron = "0 0 5 * * *") // 매일 오전 5시
public void autoEndLongRunningConsultations() {
    int count = consultationService.autoEndLongRunningConsultations();
    log.info("장시간 실행 상담 {}건 자동 종료", count);
}
```

**예상 소요 시간**: 1-2시간

---

## ✅ 검증 완료 사항

### 컴파일 및 빌드
- [x] Controller 컴파일 성공
- [x] SecurityConfig 업데이트 성공
- [x] 순환 의존성 없음
- [x] Import 오류 없음

### 코드 품질
- [x] 모든 public 메서드 JavaDoc 주석
- [x] 로깅 처리 (INFO, WARN, ERROR)
- [x] 예외 처리 및 에러 응답
- [x] Helper 메서드로 중복 코드 제거

### 보안
- [x] JWT 인증 통합
- [x] 권한 검증 (학생/교수 구분)
- [x] Security 규칙 14개 추가
- [x] 참여자 권한 검증

### API 설계
- [x] RESTful 규칙 준수
- [x] 일관된 응답 포맷
- [x] 페이징 지원
- [x] 필터링 지원 (상태별)

---

## 📌 참고 사항

### Controller 패턴
- **Request Body**: `@Valid @RequestBody` - Validation 적용
- **Query Parameter**: `@RequestParam(defaultValue = "0")` - 기본값 설정
- **Path Variable**: `@PathVariable Long requestIdx` - URL 경로에서 추출

### HTTP 메서드 선택
- **POST**: 대부분의 API (일관성 유지, Request Body 활용)
- **GET**: 상세 조회, 개수 조회 (캐싱 가능, 멱등성)

### 보안 고려사항
- JWT에서 추출한 사용자 정보만 신뢰
- Request Body의 userCode는 무시하고 JWT 우선
- 참여자 권한 검증은 Service Layer에서 처리

---

## 📞 문의

**작성자**: BlueCrab Development Team
**작성일**: 2025-10-24
**버전**: 1.0.0
