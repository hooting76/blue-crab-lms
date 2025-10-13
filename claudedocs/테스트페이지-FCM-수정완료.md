# 테스트 페이지 및 FCM API 수정 완료 보고서

**작업 일시**: 2025-10-13
**작업 범위**: FCM 전송 API 및 API 테스트 페이지 오류 수정

---

## 1. FCM 전송 API 수정 (스크린샷 문제 해결)

### 문제점
테스트 페이지에서 FCM 전송 API 호출 시 400 에러 발생:
```json
"JSON parse error: Unrecognized field \"targetType\"
(class BlueCrab.com.example.dto.FcmSendRequest),
not marked as ignorable"
```

### 원인
- 테스트 페이지: `{ targetType: "USER", targeta: ["user@example.com"] }` 형식 사용
- 기존 DTO: `userCode` 필드만 지원 (단일 사용자)

### 해결 방법

#### 1.1. FcmSendRequest DTO 확장
**파일**: [backend/BlueCrab/src/main/java/BlueCrab/com/example/dto/FcmSendRequest.java](../backend/BlueCrab/src/main/java/BlueCrab/com/example/dto/FcmSendRequest.java)

**수정 내용**:
```java
// 기존 방식: 단일 사용자 코드 (선택사항)
private String userCode;

// 새로운 방식: 대상 타입 및 대상 목록 (선택사항)
private String targetType; // "USER", "ROLE", "ALL" 등
private List<String> targeta; // 대상 목록 (사용자 코드 배열)

// getter/setter 추가
public String getTargetType() { return targetType; }
public void setTargetType(String targetType) { this.targetType = targetType; }
public List<String> getTargeta() { return targeta; }
public void setTargeta(List<String> targeta) { this.targeta = targeta; }
```

**특징**:
- 하위 호환성 유지: 기존 `userCode` 방식 계속 지원
- 새로운 방식: `targetType`과 `targeta`로 여러 사용자에게 전송 가능
- 유연한 API 설계

#### 1.2. FcmTokenService 수정
**파일**: [backend/BlueCrab/src/main/java/BlueCrab/com/example/service/FcmTokenService.java](../backend/BlueCrab/src/main/java/BlueCrab/com/example/service/FcmTokenService.java:327)

**수정 내용**:
```java
public FcmSendResponse sendNotification(FcmSendRequest request) {
    // 새로운 방식: targetType과 targeta 사용
    if (request.getTargetType() != null &&
        request.getTargeta() != null &&
        !request.getTargeta().isEmpty()) {

        if ("USER".equalsIgnoreCase(request.getTargetType())) {
            return sendToMultipleUsers(request);
        } else {
            throw new IllegalArgumentException(
                "지원하지 않는 targetType입니다: " + request.getTargetType()
            );
        }
    }

    // 기존 방식: userCode 사용
    // ... (기존 로직 유지)
}

// 여러 사용자에게 전송하는 새로운 private 메서드 추가
private FcmSendResponse sendToMultipleUsers(FcmSendRequest request) {
    // 각 사용자에게 순회하며 전송
    // 모든 플랫폼(ANDROID, IOS, WEB)으로 전송
}
```

**처리 로직**:
1. `targetType`과 `targeta` 확인 → 새로운 방식
2. `userCode` 확인 → 기존 방식
3. 둘 다 없으면 → 에러

---

## 2. API 테스트 페이지 수정

### 문제점
[docs/test-page-review.md](../docs/test-page-review.md)에서 지적된 "권한 헤더 누락으로 다수 엔드포인트 401 발생" 문제

### 원인
```javascript
// 기존 코드 (잘못된 로직)
if (endpoint !== 'ping' && endpoint !== 'health' && accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
}
```

- `endpoint` 변수는 선택된 옵션 값일 뿐
- 커스텀 URL을 사용할 경우 엔드포인트 이름과 실제 URL이 일치하지 않음
- 대부분의 `/api/` 엔드포인트가 401 에러 발생

### 해결 방법

#### 2.1. Authorization 헤더 로직 개선
**파일**: [backend/BlueCrab/src/main/resources/static/js/api-test-standalone.js](../backend/BlueCrab/src/main/resources/static/js/api-test-standalone.js:244)

**수정 내용**:
```javascript
// Authorization 헤더 추가 로직 개선
// permitAll 엔드포인트를 제외한 모든 /api/ 요청에 토큰 추가
const permitAllEndpoints = ['ping', 'health', 'login', 'register'];
const shouldAddAuth = !permitAllEndpoints.includes(endpoint) &&
                      url.includes('/api/') &&
                      accessToken;

if (shouldAddAuth) {
    headers['Authorization'] = `Bearer ${accessToken}`;
}
```

**개선 사항**:
- permitAll 목록을 명시적으로 정의
- 실제 URL에 `/api/`가 포함되는지 확인
- 더 안전하고 정확한 헤더 추가

---

## 3. 테스트 및 검증

### 3.1. FCM API 테스트
**엔드포인트**: `POST /api/fcm/send`

**새로운 방식 (여러 사용자)**:
```json
{
  "targetType": "USER",
  "targeta": ["user1@example.com", "user2@example.com"],
  "title": "테스트 알림",
  "body": "여러 사용자에게 전송",
  "data": {
    "type": "TEST"
  }
}
```

**기존 방식 (단일 사용자, 하위 호환)**:
```json
{
  "userCode": "user1@example.com",
  "title": "테스트 알림",
  "body": "단일 사용자에게 전송"
}
```

### 3.2. 테스트 페이지 동작 확인
1. 로그인 후 토큰 저장 확인
2. `/api/profile`, `/api/users` 등 인증 필요 엔드포인트 호출
3. Authorization 헤더가 자동으로 추가되는지 확인
4. 401 에러 없이 정상 응답 확인

---

## 4. 남은 작업 (선택사항)

### 4.1. attachment-test.html 수정 (보고서 이슈 #3, #4)
**위치**: `backend/BlueCrab/attachment-test.html`

**문제점**:
- 업로드 URL: `POST /api/attachments/upload` (잘못됨)
- 실제 URL: `POST /api/board-attachments/upload/{boardIdx}`
- FormData 키: `file` → `files`로 수정 필요
- 필드명: `attachmentIds` → `attachmentIdx`로 수정 필요

**권장 조치**:
필요시 별도로 수정 (현재는 FCM 관련 문제만 해결)

### 4.2. 추가 개선 사항
- permitAll 엔드포인트를 서버와 동기화하는 설정 파일 생성
- 토큰 만료 시 자동 갱신 로직 추가
- 에러 응답 시 더 상세한 디버깅 정보 표시

---

## 5. 요약

### 수정 파일 목록
1. ✅ `backend/BlueCrab/src/main/java/BlueCrab/com/example/dto/FcmSendRequest.java`
2. ✅ `backend/BlueCrab/src/main/java/BlueCrab/com/example/service/FcmTokenService.java`
3. ✅ `backend/BlueCrab/src/main/resources/static/js/api-test-standalone.js`

### 주요 개선 사항
- **FCM API**: 단일 사용자 + 여러 사용자 전송 지원
- **하위 호환성**: 기존 코드 영향 없음
- **테스트 페이지**: Authorization 헤더 자동 추가 개선
- **안정성**: 더 정확한 인증 로직

### 다음 단계
1. 서버 재시작 및 컴파일
2. 테스트 페이지에서 FCM 전송 API 테스트
3. 다른 인증 필요 엔드포인트 정상 동작 확인
4. 필요시 추가 엔드포인트에도 동일 패턴 적용
