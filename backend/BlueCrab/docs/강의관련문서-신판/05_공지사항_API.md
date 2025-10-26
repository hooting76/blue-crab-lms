# 공지사항 API 문서

## 📋 개요

수강신청 안내문 조회 및 저장 기능을 제공하는 API 문서입니다.

**컨트롤러**: `NoticeController.java`  
**기본 경로**: `/notice/course-apply`  
**관련 DB 테이블**: `COURSE_APPLY_NOTICE`

---

## 🔍 API 목록

### 1. 안내문 조회 (공개)

**엔드포인트**: `POST /notice/course-apply/view`

**권한**: 없음 (공개, 로그인 불필요)

**설명**: 현재 수강신청 안내문을 조회합니다. 최신 안내문 1개만 반환됩니다.

#### Request Body
```json
{}
```

빈 객체를 전송하거나 body 없이 요청 가능합니다.

#### 응답 예시 (성공)
```json
{
  "success": true,
  "message": "<p>방학/학기중/수강신청기간의 시기 구분 기능은 미구현</p>",
  "updatedAt": "2025-10-23T13:56:11",
  "updatedBy": "bluecrabtester9@gmail.com"
}
```

#### 응답 예시 (안내문 없음)
```json
{
  "success": false,
  "message": "안내문이 없습니다."
}
```

**HTTP Status**: 
- 성공: `200 OK`
- 안내문 없음: `404 NOT FOUND`
- 오류: `500 INTERNAL SERVER ERROR`

---

### 2. 안내문 저장 (관리자/교수)

**엔드포인트**: `POST /notice/course-apply/save`

**권한**: 관리자 또는 교수

**설명**: 수강신청 안내문을 작성하거나 수정합니다. 기존 안내문이 있으면 수정하고, 없으면 새로 생성합니다.

#### Request Body
```json
{
  "message": "<p>2025학년도 1학기 수강신청 일정 안내</p><ul><li>신청 기간: 2025-01-10 ~ 2025-01-15</li><li>정정 기간: 2025-01-16 ~ 2025-01-20</li></ul>"
}
```

**필드 설명**:
- `message` (String, 필수): 안내문 내용 (HTML 형식 지원)

#### 응답 예시 (성공)
```json
{
  "success": true,
  "message": "안내문이 저장되었습니다.",
  "noticeIdx": 1,
  "updatedAt": "2025-10-26T10:30:00",
  "updatedBy": "admin@bluecrab.ac.kr"
}
```

#### 응답 예시 (권한 없음)
```json
{
  "success": false,
  "message": "권한이 없습니다."
}
```

**HTTP Status**: 
- 성공: `200 OK`
- 권한 없음: `403 FORBIDDEN`
- 오류: `500 INTERNAL SERVER ERROR`

---

## 📊 DTO 구조

### NoticeViewResponse
```java
{
  "success": Boolean,
  "message": String,          // 안내문 내용 (HTML)
  "updatedAt": String,        // ISO-8601 형식
  "updatedBy": String         // 작성자 이메일
}
```

### NoticeSaveRequest
```java
{
  "message": String           // 안내문 내용 (HTML)
}
```

### NoticeSaveResponse
```java
{
  "success": Boolean,
  "message": String,          // 응답 메시지
  "noticeIdx": Integer,       // 안내문 ID
  "updatedAt": String,        // ISO-8601 형식
  "updatedBy": String         // 작성자 이메일
}
```

---

## 🔗 관련 테이블

### COURSE_APPLY_NOTICE
- **기본 키**: `notice_idx`
- **주요 컬럼**:
  - `message`: 안내문 내용 (TEXT)
  - `updated_at`: 최종 수정 일시
  - `updated_by`: 최종 수정자 (이메일)
  - `created_at`: 생성 일시

### 샘플 데이터
```sql
INSERT INTO COURSE_APPLY_NOTICE VALUES
(1, '<p>방학/학기중/수강신청기간의 시기 구분 기능은 미구현</p>', '2025-10-23 13:56:11', 'bluecrabtester9@gmail.com', '2025-10-22 02:13:19');
```

---

## 📈 비즈니스 로직

### 안내문 저장/수정 프로세스
1. 인증 확인 (JWT 토큰)
2. 권한 확인 (관리자 또는 교수)
3. 기존 안내문 조회
4. 있으면 수정, 없으면 신규 생성
5. `updated_by`에 현재 사용자 이메일 저장
6. `updated_at` 자동 갱신
7. 응답 반환

### 안내문 조회 프로세스
1. 최신 안내문 1개 조회 (`ORDER BY updated_at DESC`)
2. 없으면 404 응답
3. 있으면 내용 반환

---

## 💡 사용 예시

### 프론트엔드 예시 (JavaScript)

#### 안내문 조회 (공개)
```javascript
fetch('/notice/course-apply/view', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({})
})
.then(res => res.json())
.then(data => {
  if (data.success) {
    document.getElementById('notice').innerHTML = data.message;
    document.getElementById('updated-at').textContent = 
      `최종 수정: ${data.updatedAt} (${data.updatedBy})`;
  } else {
    document.getElementById('notice').textContent = '안내문이 없습니다.';
  }
});
```

#### 안내문 저장 (관리자/교수)
```javascript
fetch('/notice/course-apply/save', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${jwtToken}`
  },
  body: JSON.stringify({
    message: '<p>수강신청 일정이 변경되었습니다.</p>'
  })
})
.then(res => res.json())
.then(data => {
  if (data.success) {
    alert('안내문이 저장되었습니다.');
  } else {
    alert(data.message);
  }
});
```

---

## ⚠️ 주의사항

1. **HTML 입력**: `message` 필드는 HTML 형식을 지원하지만 XSS 방지를 위해 sanitize 필요
2. **권한 검증**: 안내문 저장은 관리자/교수만 가능
3. **단일 안내문**: 항상 최신 1개의 안내문만 조회됨
4. **공개 API**: 조회 API는 인증 불필요 (누구나 접근 가능)

---

## 🔒 보안 고려사항

### XSS 방지
프론트엔드에서 안내문을 렌더링할 때 HTML sanitize 필요:
```javascript
import DOMPurify from 'dompurify';

const cleanHTML = DOMPurify.sanitize(data.message);
document.getElementById('notice').innerHTML = cleanHTML;
```

### 권한 검증
```java
// Controller에서 Spring Security 인증 확인
if (authentication == null || !authentication.isAuthenticated()) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(...);
}

// 관리자 또는 교수 권한 확인
String userEmail = authentication.getName();
UserTbl user = userRepository.findByUserEmail(userEmail).orElse(null);
if (user == null || (user.getUserStudent() != 0 && user.getUserStudent() != 1)) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(...);
}
```

---

## 📝 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0.0 | 2025-10-22 | 초기 버전 생성 |
| 1.0.1 | 2025-10-23 | 안내문 수정 기능 추가 |

---

© 2025 BlueCrab LMS Development Team
