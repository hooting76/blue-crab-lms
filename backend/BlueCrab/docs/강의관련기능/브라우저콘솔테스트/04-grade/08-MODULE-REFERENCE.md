# 🧩 모듈 참조 문서

각 모듈의 역할, 의존성, 함수 목록을 상세히 설명합니다.

---

## 📖 목차

- [모듈 구조](#-모듈-구조)
- [의존성 체인](#-의존성-체인)
- [모듈별 상세](#-모듈별-상세)

---

## 📁 모듈 구조

```
04-grade/
├── 01-grade-test-utils.js       # ① 공통 유틸리티 (183 lines)
├── 02-grade-phase1-tests.js     # ② Phase 1 테스트 (367 lines)
├── 03-grade-phase3-tests.js     # ③ Phase 3 테스트 (142 lines)
└── 04-grade-test-runner.js      # ④ 통합 러너 (223 lines)
```

**총 라인 수:** 915 lines (주석 포함)

---

## 🔗 의존성 체인

```
01-grade-test-utils.js (기초)
    ↓
    ├── 02-grade-phase1-tests.js (Phase 1)
    └── 03-grade-phase3-tests.js (Phase 3)
            ↓
        04-grade-test-runner.js (통합)
```

**중요:** 반드시 순서대로 로드!

---

## 1️⃣ `01-grade-test-utils.js`

### 역할
- 공통 유틸리티 제공
- 모든 모듈의 기초

### 의존성
- 없음 (기초 모듈)

### 글로벌 객체
```javascript
window.gradeTestUtils
```

### 주요 함수

#### `checkAuth()`
JWT 토큰 인증 체크

**반환:**
```javascript
"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**예외:**
```javascript
throw new Error("⚠️ 로그인이 필요합니다!")
```

---

#### `apiCall(method, endpoint, data)`
범용 API 호출 함수

**파라미터:**
- `method`: "GET" | "POST" | "PUT" | "DELETE"
- `endpoint`: "/api/enrollments/..."
- `data`: Request body (optional)

**반환:**
```javascript
{
    success: true,
    message: "...",
    data: {...}
}
```

---

#### `apiGet(endpoint)`
GET 요청 래퍼

**예시:**
```javascript
const result = await gradeTestUtils.apiGet('/api/enrollments/1/100/grade')
```

---

#### `apiPut(endpoint, data)`
PUT 요청 래퍼

**예시:**
```javascript
const result = await gradeTestUtils.apiPut(
    '/api/enrollments/1/attendance',
    { status: 'PRESENT', date: '2024-01-15' }
)
```

---

#### `testData`
기본 테스트 데이터 객체

**구조:**
```javascript
{
    lecIdx: 1,
    studentIdx: 100,
    professorIdx: 22,
    enrollmentIdx: 1,
    assignmentIdx: 1,
    passingThreshold: 60.0,
    attendanceMaxScore: 80,
    assignmentTotalMaxScore: 100,
    latePenaltyPerSession: 0.5,
    gradeDistribution: { "A+": 10, "A": 15, ... }
}
```

---

#### `setTestData(updates)`
테스트 데이터 동적 수정

**예시:**
```javascript
gradeTestUtils.setTestData({
    lecIdx: 2,
    studentIdx: 101
})
```

---

#### `getTestData()`
현재 테스트 데이터 조회

**반환:**
```javascript
{ lecIdx: 1, studentIdx: 100, ... }
```

---

## 2️⃣ `02-grade-phase1-tests.js`

### 역할
- Phase 1 핵심 기능 테스트 (5개)

### 의존성
- `01-grade-test-utils.js` 필수

### 글로벌 객체
```javascript
window.gradePhase1Tests
```

### 주요 함수

#### `testGradeConfig()`
성적 구성 설정 테스트

**API:** `POST /api/enrollments/grade-config`

**기능:**
- 출석 만점 설정
- 과제 만점 설정
- 지각 페널티 설정
- 등급 분포 설정

**예상 출력:**
```
✅ 성적 구성 설정 성공
📝 메시지: 성적 구성이 성공적으로 저장되었습니다.
```

---

#### `testStudentGradeInfo()`
학생 성적 조회 테스트

**API:** `GET /api/enrollments/{lecIdx}/{studentIdx}/grade`

**기능:**
- 출석 점수/비율 조회
- 과제 점수/비율 조회
- 총점/백분율 계산

**예상 출력:**
```
📊 성적 상세 정보:
  📅 출석: 18.5 / 80 (23.1%)
  📝 과제: 85.0 / 100 (85.0%)
  📈 총점: 103.5 (51.8%)
```

---

#### `testProfessorGradeView()`
교수용 성적 조회 테스트

**API:** `GET /api/enrollments/professor/grade`

**기능:**
- 전체 학생 성적 목록
- 통계 (평균, 최고점, 최저점, 순위)

---

#### `testGradeList()`
성적 목록 조회 테스트

**API:** `GET /api/enrollments/grade-list`

**기능:**
- 페이징 (page, size)
- 정렬 (percentage/grade, asc/desc)

---

#### `testGradeFinalize()`
최종 등급 배정 테스트

**API:** `POST /api/enrollments/finalize-grades`

**기능:**
- 60% 기준 합격/불합격
- 상대평가 (A+~D)
- 동점자 처리

**예상 출력:**
```
📊 등급 분포:
  A+: 10명 (10%)
  A: 15명 (15%)
  B+: 15명 (15%)
  ...
```

---

#### `runPhase1Tests()`
Phase 1 전체 실행 (5개)

**순서:**
1. config → 2. studentInfo → 3. professorView → 4. gradeList → 5. finalize

---

## 3️⃣ `03-grade-phase3-tests.js`

### 역할
- Phase 3 이벤트 시스템 테스트 (2개)

### 의존성
- `01-grade-test-utils.js` 필수

### 글로벌 객체
```javascript
window.gradePhase3Tests
```

### 주요 함수

#### `testAttendanceUpdate()`
출석 업데이트 + 이벤트 발행

**API:** `PUT /api/enrollments/{enrollmentIdx}/attendance`

**기능:**
- 출석/지각/결석 기록
- `GradeUpdateEvent(ATTENDANCE)` 발행
- 비동기 성적 재계산

**예상 출력:**
```
✅ 출석 업데이트 성공
🔔 이벤트 발행됨: ATTENDANCE
```

**Eclipse 로그:**
```
[GradeUpdateEventListener] 성적 업데이트 이벤트 수신
- 타입: ATTENDANCE
- 수강 ID: 1
```

---

#### `testAssignmentGrade()`
과제 채점 + 이벤트 발행

**API:** `PUT /api/assignments/{assignmentIdx}/grade`

**기능:**
- 과제 점수 입력
- `GradeUpdateEvent(ASSIGNMENT)` 발행
- 비동기 성적 재계산

**예상 출력:**
```
✅ 과제 채점 성공
🔔 이벤트 발행됨: ASSIGNMENT
```

**Eclipse 로그:**
```
[GradeUpdateEventListener] 성적 업데이트 이벤트 수신
- 타입: ASSIGNMENT
- 수강 ID: 1
```

---

#### `runPhase3Tests()`
Phase 3 전체 실행 (2개)

**순서:**
1. attendance → 2초 대기 → 2. assignment → 2초 대기

**대기 시간:** 이벤트 처리를 위한 비동기 대기

---

## 4️⃣ `04-grade-test-runner.js`

### 역할
- 모든 테스트 통합 및 오케스트레이션
- 사용자 친화적 인터페이스 제공

### 의존성
- `02-grade-phase1-tests.js` 필수
- `03-grade-phase3-tests.js` 필수

### 글로벌 객체
```javascript
window.gradeTests
```

### 통합 인터페이스

#### 개별 테스트 함수

```javascript
gradeTests.config()          // Phase 1-1
gradeTests.studentInfo()     // Phase 1-2
gradeTests.professorView()   // Phase 1-3
gradeTests.gradeList()       // Phase 1-4
gradeTests.finalize()        // Phase 1-5
gradeTests.attendance()      // Phase 3-1
gradeTests.assignment()      // Phase 3-2
```

#### Phase 실행 함수

```javascript
gradeTests.phase1()   // Phase 1 전체 (5개)
gradeTests.phase3()   // Phase 3 전체 (2개)
```

#### 전체 실행 함수

```javascript
gradeTests.runAll()   // Phase 1 + Phase 3 (7개)
```

**플로우:**
1. Phase 1 테스트 5개
2. 3초 대기
3. Phase 3 테스트 2개
4. 통계 출력

---

#### 시나리오 함수

```javascript
gradeTests.scenario()  // 6단계 워크플로우
```

**워크플로우:**
1. 성적 구성 설정
2. 출석 기록 (이벤트)
3. 과제 채점 (이벤트)
4. 학생 성적 확인
5. 전체 성적 목록
6. 최종 등급 배정

---

#### 데이터 관리 함수

```javascript
gradeTests.getData()         // 현재 데이터 조회
gradeTests.setData(updates)  // 데이터 수정
gradeTests.customTest()      // 커스텀 데이터로 테스트
```

---

## 📊 함수 요약표

### Utils 모듈

| 함수 | 기능 | 반환 |
|------|------|------|
| `checkAuth()` | JWT 토큰 체크 | string |
| `apiCall()` | 범용 API 호출 | object |
| `apiGet()` | GET 요청 | object |
| `apiPut()` | PUT 요청 | object |
| `getTestData()` | 데이터 조회 | object |
| `setTestData()` | 데이터 수정 | void |

### Phase 1 모듈

| 함수 | API | 기능 |
|------|-----|------|
| `testGradeConfig()` | POST /grade-config | 성적 구성 설정 |
| `testStudentGradeInfo()` | GET /{lecIdx}/{studentIdx}/grade | 학생 성적 조회 |
| `testProfessorGradeView()` | GET /professor/grade | 교수용 성적 조회 |
| `testGradeList()` | GET /grade-list | 성적 목록 조회 |
| `testGradeFinalize()` | POST /finalize-grades | 최종 등급 배정 |
| `runPhase1Tests()` | - | Phase 1 전체 실행 |

### Phase 3 모듈

| 함수 | API | 기능 | 이벤트 |
|------|-----|------|--------|
| `testAttendanceUpdate()` | PUT /{enrollmentIdx}/attendance | 출석 업데이트 | ATTENDANCE |
| `testAssignmentGrade()` | PUT /assignments/{idx}/grade | 과제 채점 | ASSIGNMENT |
| `runPhase3Tests()` | - | Phase 3 전체 실행 | - |

### Runner 모듈

| 함수 | 기능 |
|------|------|
| `config()` | Phase 1-1 실행 |
| `studentInfo()` | Phase 1-2 실행 |
| `professorView()` | Phase 1-3 실행 |
| `gradeList()` | Phase 1-4 실행 |
| `finalize()` | Phase 1-5 실행 |
| `attendance()` | Phase 3-1 실행 |
| `assignment()` | Phase 3-2 실행 |
| `phase1()` | Phase 1 전체 |
| `phase3()` | Phase 3 전체 |
| `runAll()` | 전체 7개 테스트 |
| `scenario()` | 6단계 워크플로우 |
| `getData()` | 데이터 조회 |
| `setData()` | 데이터 수정 |
| `customTest()` | 커스텀 테스트 |

---

## 🔄 로드 순서

### 올바른 순서

```javascript
// ① Utils (기초)
// 01-grade-test-utils.js 로드
window.gradeTestUtils  // ✅ 확인

// ② Phase 1 (의존: Utils)
// 02-grade-phase1-tests.js 로드
window.gradePhase1Tests  // ✅ 확인

// ③ Phase 3 (의존: Utils)
// 03-grade-phase3-tests.js 로드
window.gradePhase3Tests  // ✅ 확인

// ④ Runner (의존: Phase 1, Phase 3)
// 04-grade-test-runner.js 로드
window.gradeTests  // ✅ 확인
```

### 잘못된 순서

```javascript
// ❌ Utils 없이 Phase 1 로드
// 02-grade-phase1-tests.js 로드
// → Error: gradeTestUtils is not defined

// ❌ Phase 1/3 없이 Runner 로드
// 04-grade-test-runner.js 로드
// → Error: gradePhase1Tests is not defined
```

---

## 📚 다음 단계

- [사용법 가이드](./06-USAGE-GUIDE.md) - 실행 방법
- [테스트 가이드](./07-TESTING-GUIDE.md) - Phase 4 완전 가이드
- [문제 해결](./09-TROUBLESHOOTING.md) - 오류 해결
