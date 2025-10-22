# 📚 수강신청 시스템

학생 수강신청 관련 API 명세서 및 가이드

---

## 📁 문서 구조

```
수강신청/
├── 00-README.md                    # 이 문서
├── 01-API-SPECIFICATION.md         # API 명세서
├── 02-ELIGIBILITY-LOGIC.md         # 수강 자격 검증 로직
├── 03-QUICK-START.md               # 빠른 시작 가이드
└── 04-ENROLLMENT-FLOW.drawio       # 수강신청 플로우 다이어그램
```

---

## 🎯 시스템 개요

### 주요 기능
1. **수강 가능 강의 조회** - 학부/학과 기반 필터링
2. **수강신청 처리** - 실시간 정원 체크 및 등록
3. **수강목록 관리** - 조회, 취소
4. **수강 자격 검증** - 0값 규칙 및 전공 매칭

### 핵심 로직: 0값 규칙
- `lecMcode = "0"` 또는 `lecMcodeDep = "0"`: **모든 학생 수강 가능**
- 학생의 전공(Mcode) 또는 부전공(McodeSub)이 강의 코드와 일치해야 수강 가능

---

## 🔧 API 목록

| 순번 | API | 메서드 | 엔드포인트 | 설명 |
|------|-----|--------|------------|------|
| 1 | 수강 가능 강의 조회 | POST | `/api/lectures/eligible` | 학부/학과 필터링 |
| 2 | 수강신청 | POST | `/api/enrollments/enroll` | 실시간 정원 체크 |
| 3 | 수강목록 조회 | POST | `/api/enrollments/list` | 현재 수강중인 강의 |
| 4 | 수강신청 여부 확인 | POST | `/api/enrollments/list` | 중복 신청 방지 |

---

## 🚀 빠른 시작

### 1. 인증 토큰 획득
```javascript
// 학생 계정으로 로그인
const token = window.authToken;
```

### 2. 수강 가능 강의 조회
```javascript
const response = await fetch('/api/lectures/eligible', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    studentId: currentUser.userIdx,
    page: 0,
    size: 10
  })
});

const data = await response.json();
console.log('수강 가능 강의:', data.eligibleLectures);
```

### 3. 수강신청
```javascript
const enrollResponse = await fetch('/api/enrollments/enroll', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    studentIdx: currentUser.userIdx,
    lecSerial: 'CS101'
  })
});

if (enrollResponse.ok) {
  alert('수강신청 완료!');
}
```

---

## 📊 데이터 구조

### 수강 가능 강의 응답
```json
{
  "eligibleLectures": [
    {
      "lecSerial": "CS101",
      "lecTit": "자료구조",
      "lecProf": "22",
      "lecPoint": 3,
      "lecTime": "월1수1",
      "lecCurrent": 25,
      "lecMany": 30,
      "lecMcode": "01",
      "lecMcodeDep": "001",
      "isEligible": true,
      "eligibilityReason": "수강 가능 (전공 일치: 01-001)"
    }
  ],
  "totalCount": 45,
  "eligibleCount": 30,
  "ineligibleCount": 15,
  "studentInfo": {
    "userIdx": 100,
    "userName": "홍길동",
    "majorFacultyCode": "01",
    "majorDeptCode": "001"
  }
}
```

### 수강신청 응답
```json
{
  "enrollmentIdx": 1,
  "lecIdx": 1,
  "studentIdx": 100,
  "enrollmentData": "{\"enrollment\":{\"status\":\"ENROLLED\",\"enrollmentDate\":\"2025-03-01T09:00:00\"}}"
}
```

---

## 🔄 수강신청 플로우

```
학생 로그인
    ↓
수강 가능 강의 조회
    ↓ (학부/학과 필터링)
강의 선택
    ↓
수강신청 여부 확인
    ↓ (미신청 시)
정원 확인
    ↓
수강신청 처리
    ↓
수강목록 조회
```

---

## ⚠️ 주의사항

### 에러 처리
1. **정원 초과**: "강의 정원이 초과되었습니다."
2. **중복 신청**: "이미 수강신청한 강의입니다."
3. **자격 미달**: "수강 자격이 없습니다."

### 응답 형식
- 일부 API는 `success`/`message` 래퍼 없이 엔티티를 직접 반환
- 에러 응답만 표준 형식 사용

### 0값 규칙
- `lecMcode = "0"`: 모든 학부 허용
- `lecMcodeDep = "0"`: 모든 학과 허용
- 둘 다 "0"이면 전체 학생 수강 가능

---

## 📚 상세 문서

| 문서 | 설명 |
|------|------|
| [01-API-SPECIFICATION.md](./01-API-SPECIFICATION.md) | 📋 API 명세서 |
| [02-ELIGIBILITY-LOGIC.md](./02-ELIGIBILITY-LOGIC.md) | 🔍 수강 자격 검증 |
| [03-QUICK-START.md](./03-QUICK-START.md) | 🚀 빠른 시작 |
| [04-ENROLLMENT-FLOW.drawio](./04-ENROLLMENT-FLOW.drawio) | 📊 플로우 다이어그램 |

---

## 🧪 테스트

### 브라우저 콘솔 테스트
- **위치**: `브라우저콘솔테스트/02-student/`
- **파일**: 
  - `lecture-test-2a-student-enrollment.js` - 수강신청 테스트
  - `lecture-test-2b-student-my-courses.js` - 수강목록 관리

### 실행 방법
```javascript
// 1. 로그인
await login()  // 학생 계정

// 2. 수강 가능 강의 조회
await testEligibleLectures()

// 3. 수강신청
await testEnrollment('CS101')

// 4. 수강목록 조회
await testMyEnrollments()
```

---

## 💡 프론트엔드 개발 팁

### 1. 실시간 정원 확인
```javascript
const availableSeats = lecture.lecMany - lecture.lecCurrent;
if (availableSeats <= 0) {
  button.disabled = true;
  button.textContent = '정원 마감';
}
```

### 2. 중복 신청 방지
```javascript
const isEnrolled = await checkEnrollment(studentIdx, lecSerial);
if (isEnrolled) {
  button.textContent = '신청 완료';
  button.disabled = true;
}
```

### 3. 수강 자격 표시
```javascript
const badge = lecture.isEligible 
  ? '<span class="badge-success">수강 가능</span>'
  : '<span class="badge-danger">수강 불가</span>';
```

---

> **현재 상태**: Phase 2 구현 완료 (100%)
> **다음 단계**: Phase 3 (학기 진행) 참조
