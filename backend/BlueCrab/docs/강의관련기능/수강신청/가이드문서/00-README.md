# 📚 수강신청 시스템 종합 가이드

학생 수강신청 관련 API 명세서 및 구현 가이드

작성일: 2025-10-22

---

## 📁 문서 구조

```
수강신청/
├── 가이드문서/
│   ├── 00-README.md                # 📖 이 문서 (종합 가이드)
│   ├── 01-API-SPECIFICATION.md     # 📋 API 상세 명세
│   ├── 02-ELIGIBILITY-LOGIC.md     # 🔍 수강 자격 검증 로직
│   └── 03-QUICK-START.md           # 🚀 빠른 시작 (브라우저 테스트)
│
└── 프런트팀의 요청/
    └── 백엔드답변/                  # 프론트엔드 팀 전용 응답
        ├── 00-요청검토결과.md
        ├── 01-필터링파라미터명세.md
        ├── 02-취소API사용법.md
        ├── 03-안내문API명세.md
        ├── 05-자동필터링원리.md
        ├── 06-학부학과코드매핑.md
        └── 07-안내문API구현가이드.md
```

---

## 🎯 시스템 개요

### 주요 기능

1. **수강 가능 강의 조회** (`/api/lectures/eligible`)
   - 학부/학과 기반 자동 필터링
   - JWT 토큰으로 학생 자동 식별
   - 수강 자격 검증 포함
   - 페이징 지원

2. **수강신청** (`/api/enrollments/enroll`)
   - 실시간 정원 체크
   - 중복 신청 방지
   - 수강 자격 검증

3. **수강목록 관리** (`/api/enrollments/list`, `/api/enrollments/{enrollmentIdx}`)
   - 현재 수강목록 조회
   - 수강신청 취소
   - 페이징 지원

4. **안내문 관리** (`/notice/course-apply/view`, `/notice/course-apply/save`)
   - 수강신청 안내문 조회 (공개)
   - 안내문 저장/수정 (관리자/교수)

---

## 🔑 핵심 로직

### 1. 자동 필터링 (Phase 9)

**백엔드가 모든 것을 자동 처리합니다!**

```javascript
// 프론트엔드는 studentId만 전달
fetch('/api/lectures/eligible', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` },
  body: JSON.stringify({ studentId: 15, page: 0, size: 10 })
});
```

**백엔드 처리 순서**:
1. JWT 토큰 → 학생 자동 식별
2. 학생의 학부/학과 자동 조회
3. 수강 가능 강의만 자동 필터링
4. 수강 자격 검증 결과 포함

**중요**: 백엔드는 **코드만 반환** (`lecMcode`, `lecMcodeDep`)
- 프론트엔드가 코드 → 이름 변환 수행
- 상세: `프런트팀의 요청/백엔드답변/06-학부학과코드매핑.md`

### 2. 0값 규칙 (전체 허용)

| 코드 | 의미 | 예시 |
|------|------|------|
| `lecMcode = "0"` | 모든 학부 허용 | 교양 과목 |
| `lecMcodeDep = "0"` | 모든 학과 허용 | 학부 공통 과목 |
| 둘 다 "0" | 전체 학생 허용 | 전교생 대상 |

### 3. 수강 자격 검증

**학생이 수강 가능한 경우**:
- 강의의 `lecMcode = "0"` (전체 학부 허용)
- 강의의 `lecMcodeDep = "0"` (전체 학과 허용)
- 학생의 전공(Mcode)이 강의의 `lecMcode`와 일치
- 학생의 부전공(McodeSub)이 강의의 `lecMcode`와 일치

상세: `02-ELIGIBILITY-LOGIC.md`

### 4. 안내문 시스템

**조회** (공개 - 인증 불필요):
```javascript
fetch('http://localhost:8090/notice/course-apply/view', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' }
});
// → { success: true, message: "안내문 내용", updatedAt, updatedBy }
```

**저장** (관리자/교수 - JWT 필요):
```javascript
fetch('http://localhost:8090/notice/course-apply/save', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({ message: "수강신청 안내 메시지" })
});
```

상세: `프런트팀의 요청/백엔드답변/07-안내문API구현가이드.md`

---

## 🔧 API 목록

| API | 메서드 | 엔드포인트 | 인증 | 설명 |
|-----|--------|------------|------|------|
| 수강 가능 강의 조회 | POST | `/api/lectures/eligible` | ✅ | 학부/학과 자동 필터링 |
| 수강신청 | POST | `/api/enrollments/enroll` | ✅ | 실시간 정원 체크 |
| 수강목록 조회 | POST | `/api/enrollments/list` | ✅ | 현재 수강목록 |
| 수강신청 취소 | DELETE | `/api/enrollments/{enrollmentIdx}` | ✅ | 수강 취소 |
| 안내문 조회 | POST | `/notice/course-apply/view` | ❌ | 공개 |
| 안내문 저장 | POST | `/notice/course-apply/save` | ✅ | 관리자/교수 |

---

## 🚀 빠른 시작

### 1단계: 인증
```javascript
const token = window.authToken; // 로그인 후 저장된 JWT 토큰
```

### 2단계: 수강 가능 강의 조회
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
console.log('총 개수:', data.totalCount);
```

### 3단계: 수강신청
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

### 4단계: 수강목록 확인
```javascript
const listResponse = await fetch('/api/enrollments/list', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    studentIdx: currentUser.userIdx,
    enrolled: true
  })
});

const myList = await listResponse.json();
console.log('내 수강목록:', myList.content);
```

### 5단계: 수강신청 취소
```javascript
const deleteResponse = await fetch(`/api/enrollments/${enrollmentIdx}`, {
  method: 'DELETE',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

if (deleteResponse.ok) {
  alert('수강신청이 취소되었습니다.');
}
```

상세: `03-QUICK-START.md`

---

## 📊 주요 데이터 구조

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
    ↓ (JWT 토큰 획득)
수강 가능 강의 조회
    ↓ (자동 필터링: 학부/학과)
강의 목록 표시
    ↓ (학생 선택)
수강신청 버튼 클릭
    ↓ (중복 체크, 정원 체크)
수강신청 처리
    ↓ (성공)
수강목록에 추가
    ↓
수강목록 조회/취소
```

---

## ⚠️ 주의사항

### 에러 처리

| 에러 | HTTP | 메시지 |
|------|------|--------|
| 정원 초과 | 400 | "강의 정원이 초과되었습니다." |
| 중복 신청 | 400 | "이미 수강신청한 강의입니다." |
| 자격 미달 | 400 | "수강 자격이 없습니다." |
| 인증 실패 | 401 | "인증이 필요합니다." |
| 권한 없음 | 403 | "권한이 없습니다." |

### 응답 형식
- **수강신청 성공**: 엔티티 직접 반환 (래퍼 없음)
- **에러 응답**: `{ success: false, message: "..." }` 형식

### 학부/학과 코드
- 백엔드는 **코드만 반환**: `lecMcode`, `lecMcodeDep`
- 프론트엔드가 **코드 → 이름 변환**
- 매핑 테이블: `프런트팀의 요청/백엔드답변/06-학부학과코드매핑.md`

---

## 📚 상세 문서

### 가이드문서

| 문서 | 설명 |
|------|------|
| [01-API-SPECIFICATION.md](./01-API-SPECIFICATION.md) | 📋 API 상세 명세 (요청/응답 예시) |
| [02-ELIGIBILITY-LOGIC.md](./02-ELIGIBILITY-LOGIC.md) | 🔍 수강 자격 검증 로직 상세 |
| [03-QUICK-START.md](./03-QUICK-START.md) | 🚀 브라우저 콘솔 테스트 |

### 프론트팀 전용

| 문서 | 설명 |
|------|------|
| [00-요청검토결과.md](../프런트팀의 요청/백엔드답변/00-요청검토결과.md) | 📋 요청사항 검토 결과 |
| [01-필터링파라미터명세.md](../프런트팀의 요청/백엔드답변/01-필터링파라미터명세.md) | 🔍 추가 필터링 파라미터 |
| [02-취소API사용법.md](../프런트팀의 요청/백엔드답변/02-취소API사용법.md) | 🗑️ 수강신청 취소 가이드 |
| [03-안내문API명세.md](../프런트팀의 요청/백엔드답변/03-안내문API명세.md) | 📢 안내문 API 설계 |
| [05-자동필터링원리.md](../프런트팀의 요청/백엔드답변/05-자동필터링원리.md) | ⭐ 자동 필터링 원리 (필독) |
| [06-학부학과코드매핑.md](../프런트팀의 요청/백엔드답변/06-학부학과코드매핑.md) | � 학부/학과 코드 매핑 (필수) |
| [07-안내문API구현가이드.md](../프런트팀의 요청/백엔드답변/07-안내문API구현가이드.md) | 📢 안내문 API 구현 완료 |

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
await testEnroll('CS101')

// 4. 수강목록 조회
await testMyEnrollments()

// 5. 수강신청 취소
await testCancelEnrollment(123)
```

상세: `03-QUICK-START.md`

---

## 🔐 보안

### JWT 인증
- 모든 API는 JWT Bearer Token 필요 (안내문 조회 제외)
- 토큰은 로그인 API로 획득
- Header: `Authorization: Bearer {token}`

### 권한
- **학생**: 수강 가능 강의 조회, 수강신청, 수강목록 조회, 취소
- **관리자/교수**: 안내문 저장/수정
- **공개**: 안내문 조회

---

## 📈 향후 계획

### Phase 10 (오늘 중 완료 예정)
- [ ] 추가 검색 옵션 구현 (`lecYear`, `lecSemester`, `lecMajor`)
- [x] 안내문 시스템 구현 완료

### Phase 11 (향후)
- [ ] 수강 정원 실시간 업데이트 (WebSocket)
- [ ] 수강신청 이력 관리
- [ ] 수강신청 기간 제한 기능
- [ ] 학점 상한 체크

---

## 💡 FAQ

**Q1. 왜 백엔드가 코드만 반환하나요?**
- A: 학부/학과명 출력은 별도 기능이 아닙니다. 프론트엔드에서 정적 매핑 테이블로 변환합니다.

**Q2. 0값 규칙이 뭔가요?**
- A: `lecMcode = "0"` 또는 `lecMcodeDep = "0"`이면 해당 조건은 무시하고 모든 학생이 수강 가능합니다.

**Q3. 강의명 검색은 지원하나요?**
- A: ❌ 로직이 복잡하여 지원하지 않습니다. `lecYear`, `lecSemester`, `lecMajor`만 지원합니다.

**Q4. 수강신청 취소는 어떻게 하나요?**
- A: `DELETE /api/enrollments/{enrollmentIdx}` - 상세: `프런트팀의 요청/백엔드답변/02-취소API사용법.md`

**Q5. 안내문은 어떻게 표시하나요?**
- A: `/notice/course-apply/view`로 조회 (공개) - 상세: `프런트팀의 요청/백엔드답변/07-안내문API구현가이드.md`

---

## 📞 문의

문서 관련 문의사항이나 오류 발견 시 백엔드 팀으로 연락주세요.

---

> 💡 **권장 읽기 순서**:
> 1. 이 문서 (00-README.md) - 전체 개요
> 2. `05-자동필터링원리.md` - 백엔드 자동 처리 이해
> 3. `06-학부학과코드매핑.md` - 코드 변환 구현
> 4. `01-API-SPECIFICATION.md` - API 상세 명세
> 5. `03-QUICK-START.md` - 실습

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
