# 🗑️ 수강신청 취소 API 사용법

이미 구현된 DELETE 방식 API 활용 가이드

---

## ✅ 현재 구현 상태

수강신청 취소 기능은 **이미 완전히 구현**되어 있습니다!

**엔드포인트**: `DELETE /enrollments/{enrollmentIdx}`

---

## 🚀 빠른 시작

### 1️⃣ 수강 목록에서 enrollmentIdx 조회

```javascript
const response = await fetch('/api/enrollments/list', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    studentIdx: 15,
    page: 0,
    size: 100
  })
});

const data = await response.json();
const myEnrollments = data.content;

// 특정 강의 찾기
const target = myEnrollments.find(e => e.lecSerial === 'CS101');
console.log('enrollmentIdx:', target.enrollmentIdx);
```

### 2️⃣ 취소 요청

```javascript
const response = await fetch(`/api/enrollments/${target.enrollmentIdx}`, {
  method: 'DELETE',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

if (response.ok) {
  const result = await response.json();
  console.log('취소 완료:', result.message);
} else {
  console.error('취소 실패:', response.status);
}
```

---

## 📋 API 상세 명세

### DELETE /enrollments/{enrollmentIdx}

**설명**: 수강신청을 취소합니다.

**Path Parameter**:
- `enrollmentIdx`: 수강신청 ID (Integer, 필수)

**Headers**:
```http
Authorization: Bearer {JWT_TOKEN}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "수강이 취소되었습니다.",
  "timestamp": "2025-10-22T10:00:00Z"
}
```

**Response (404 Not Found)**:
```json
{
  "success": false,
  "message": "수강 정보를 찾을 수 없습니다.",
  "timestamp": "2025-10-22T10:00:00Z"
}
```

**Response (403 Forbidden)**:
```json
{
  "success": false,
  "message": "본인의 수강신청만 취소할 수 있습니다.",
  "timestamp": "2025-10-22T10:00:00Z"
}
```

---

## 💻 프론트엔드 통합 예시

### React 컴포넌트

```jsx
import React, { useState } from 'react';

function CourseList() {
  const [enrollments, setEnrollments] = useState([]);
  const token = localStorage.getItem('authToken');

  // 수강 목록 조회
  const loadEnrollments = async () => {
    const response = await fetch('/api/enrollments/list', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        studentIdx: 15,
        page: 0,
        size: 100
      })
    });
    
    const data = await response.json();
    setEnrollments(data.content);
  };

  // 수강 취소
  const cancelEnrollment = async (enrollmentIdx) => {
    if (!confirm('정말 취소하시겠습니까?')) return;

    try {
      const response = await fetch(`/api/enrollments/${enrollmentIdx}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        alert('수강신청이 취소되었습니다.');
        loadEnrollments(); // 목록 새로고침
      } else {
        const error = await response.json();
        alert(error.message || '취소 실패');
      }
    } catch (error) {
      console.error('에러:', error);
      alert('취소 중 오류가 발생했습니다.');
    }
  };

  return (
    <div>
      <h2>내 수강 목록</h2>
      {enrollments.map(enrollment => (
        <div key={enrollment.enrollmentIdx}>
          <h3>{enrollment.lecTit} ({enrollment.lecSerial})</h3>
          <p>교수: {enrollment.lecProfName}</p>
          <button onClick={() => cancelEnrollment(enrollment.enrollmentIdx)}>
            취소
          </button>
        </div>
      ))}
    </div>
  );
}

export default CourseList;
```

---

## 🔄 프론트팀 요청과의 차이

### 요청된 방식 (POST)
```http
POST /enrollments/cancel
{
  "studentIdx": 15,
  "lecSerial": "CS101"
}
```

### 현재 구현 (DELETE)
```http
DELETE /enrollments/{enrollmentIdx}
```

---

## 💡 DELETE 방식의 장점

### 1️⃣ REST 표준 준수
- DELETE는 리소스 삭제의 표준 메서드
- RESTful API 원칙에 부합

### 2️⃣ 명확한 리소스 식별
- enrollmentIdx로 직접 삭제 대상 지정
- 중복 조회 로직 불필요

### 3️⃣ 성능 최적화
- 단일 쿼리로 삭제 처리
- studentIdx + lecSerial 조합 조회 불필요

---

## 🔧 POST 방식도 필요하신가요?

만약 POST 방식 엔드포인트가 꼭 필요하시다면, 다음과 같이 추가 구현 가능합니다:

### 추가 구현 예시
```java
@PostMapping("/enrollments/cancel")
public ResponseEntity<?> cancelByLecSerial(@RequestBody CancelRequest request) {
    // 1. studentIdx + lecSerial로 enrollmentIdx 조회
    Enrollment enrollment = enrollmentRepository
        .findByStudentIdxAndLecSerial(request.getStudentIdx(), request.getLecSerial())
        .orElseThrow(() -> new NotFoundException("수강 정보를 찾을 수 없습니다."));
    
    // 2. 기존 DELETE 로직 호출
    return deleteEnrollment(enrollment.getEnrollmentIdx());
}
```

**예상 추가 시간**: 30분~1시간

---

## 🧪 테스트 코드

### 브라우저 콘솔 테스트

**위치**: `브라우저콘솔테스트/02-student/lecture-test-2b-student-my-courses.js`

**함수**: `cancelEnrollment()`

**실행 방법**:
```javascript
// 1. 로그인
await login() // 학생 계정

// 2. 스크립트 로드
// lecture-test-2b-student-my-courses.js 내용을 콘솔에 복사

// 3. 수강 목록 조회
await getMyEnrollments()

// 4. 취소 실행
await cancelEnrollment()
// enrollmentIdx 입력 프롬프트 → yes 확인
```

---

## ⚠️ 주의사항

### 1. 권한 검증
- 본인의 수강신청만 취소 가능
- JWT 토큰의 userIdx와 enrollment의 studentIdx 일치 확인

### 2. 강의 정원 자동 감소
- 취소 시 `lecCurrent` 자동 -1 처리
- 트랜잭션으로 안전하게 처리됨

### 3. 취소 제한 (향후 추가 가능)
```java
// 개강 전까지만 취소 가능 (예시)
if (LocalDate.now().isAfter(lecture.getStartDate())) {
    throw new BadRequestException("개강 후에는 취소할 수 없습니다.");
}
```

---

## 📊 동작 흐름

```
학생
  ↓
1. GET /enrollments/list
   (수강 목록 조회)
  ↓
2. enrollmentIdx 확인
  ↓
3. DELETE /enrollments/{enrollmentIdx}
   (취소 요청)
  ↓
백엔드
  ↓
4. JWT 검증
  ↓
5. 권한 확인 (본인 여부)
  ↓
6. lecCurrent--
  ↓
7. enrollment 삭제
  ↓
8. 성공 응답
```

---

## 🔗 관련 문서

- [가이드문서/01-API-SPECIFICATION.md](../../가이드문서/01-API-SPECIFICATION.md)
- [테스트 코드](../../../브라우저콘솔테스트/02-student/lecture-test-2b-student-my-courses.js)

---

## 📞 추가 문의

POST 방식 엔드포인트 추가가 필요하시거나, 다른 요구사항이 있으시면 말씀해주세요!

---

> **즉시 사용 가능**: DELETE 방식 API는 이미 완전히 구현되어 있습니다.
