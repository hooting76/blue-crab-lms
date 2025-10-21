# 📚 API 참조 문서# 📚 API 참조 문서# 🧩 모듈 참조 문서



성적 관리 시스템의 모든 API와 함수 목록입니다.



------성적 관리 시스템의 모든 함수와 사용법입니다.각 모듈의 역할, 의존성, 함수 목록을 상세히 설명합니다.



## 📖 목차



- [Phase 1 API](#phase-1-api)------

- [Phase 3 API](#phase-3-api)



------

## 📖 목차## 📖 목차

## Phase 1 API



### 설정 함수

- [전역 객체](#-전역-객체)- [모듈 구조](#-모듈-구조)

**`setLecture(lecSerial, studentIdx)`**

- [설정 함수](#-설정-함수)- [의존성 체인](#-의존성-체인)

강의 코드와 학생 IDX를 설정합니다.

- [테스트 함수](#-테스트-함수)- [모듈별 상세](#-모듈별-상세)

```javascript

gradePhase1.setLecture('ETH201')       // 강의만

gradePhase1.setLecture('ETH201', 6)    // 강의 + 학생

```------



**`quickAttendanceConfig(maxScore, latePenalty)`**



출석 만점과 지각 페널티를 빠르게 설정합니다.## 🌐 전역 객체## 📁 모듈 구조



```javascript

gradePhase1.quickAttendanceConfig(80, 0.5)

```### `gradePhase1````



**`getConfig()`**04-grade/



현재 설정을 조회합니다.01-grade-phase1-tests.js 로드 시 생성되는 전역 객체입니다.├── 01-grade-test-utils.js       # ① 공통 유틸리티 (183 lines)



```javascript├── 02-grade-phase1-tests.js     # ② Phase 1 테스트 (367 lines)

const config = gradePhase1.getConfig()

```**확인:**├── 03-grade-phase3-tests.js     # ③ Phase 3 테스트 (142 lines)



### 테스트 함수```javascript└── 04-grade-test-runner.js      # ④ 통합 러너 (223 lines)



**`config()`** - 성적 구성 설정typeof gradePhase1  // → "object"```



```javascript```

API: POST /api/enrollments/grade-config

gradePhase1.setLecture('ETH201')**총 라인 수:** 915 lines (주석 포함)

await gradePhase1.config()

```---



**`studentInfo()`** - 학생 성적 조회---



```javascript## ⚙️ 설정 함수

API: POST /api/enrollments/grade-info (action: get-grade)

gradePhase1.setLecture('ETH201', 6)## 🔗 의존성 체인

await gradePhase1.studentInfo()

```### `setLecture(lecSerial, studentIdx)`



**`professorView()`** - 교수용 성적 조회```



```javascript강의 코드와 학생 IDX를 설정합니다.01-grade-test-utils.js (기초)

API: POST /api/enrollments/grade-info (action: professor-view)

gradePhase1.setLecture('ETH201', 6)    ↓

await gradePhase1.professorView()

```**파라미터:**    ├── 02-grade-phase1-tests.js (Phase 1)



**`gradeList()`** - 성적 목록 조회- `lecSerial` (string): 강의 코드 (예: "ETH201")    └── 03-grade-phase3-tests.js (Phase 3)



```javascript- `studentIdx` (number, optional): 학생 IDX (예: 6)            ↓

API: POST /api/enrollments/grade-list (action: list-all)

gradePhase1.setLecture('ETH201')        04-grade-test-runner.js (통합)

await gradePhase1.gradeList()

```**예시:**```



**`finalize()`** - 최종 등급 배정```javascript



```javascript// 강의만 설정**중요:** 반드시 순서대로 로드!

API: POST /api/enrollments/grade-finalize (action: finalize)

gradePhase1.setLecture('ETH201')gradePhase1.setLecture('ETH201')

await gradePhase1.finalize()

```---



**`runAll()`** - 전체 테스트// 강의 + 학생 설정



```javascriptgradePhase1.setLecture('ETH201', 6)## 1️⃣ `01-grade-test-utils.js`

gradePhase1.setLecture('ETH201')

await gradePhase1.runAll()```

```

### 역할

------

---- **테스트 도구 모음** (🧰 공구함)

## Phase 3 API

- ❌ 이 파일 자체로는 테스트를 실행하지 않음

### 설정 함수

### `quickAttendanceConfig(maxScore, latePenalty)`- ✅ 다른 테스트 파일(02, 03)이 사용할 도구만 제공

**`setLecture(lecSerial, studentIdx, assignmentIdx)`**

- 모든 모듈의 기초

강의 코드, 학생 IDX, 과제 IDX를 설정합니다.

출석 만점과 지각 페널티를 빠르게 설정합니다.

```javascript

gradePhase3.setLecture('ETH201', 6)       // 강의 + 학생### 의존성

gradePhase3.setLecture('ETH201', 6, 1)    // 강의 + 학생 + 과제

```**파라미터:**- 없음 (기초 모듈)



**`listStudents(page, size)`**- `maxScore` (number): 출석 만점 (기본값: 80)



수강생 목록을 조회합니다.- `latePenalty` (number): 지각 페널티/회 (기본값: 0.5)### 글로벌 객체



```javascript```javascript

API: POST /api/enrollments/list

gradePhase3.setLecture('ETH201')**반환:**window.gradeTestUtils

await gradePhase3.listStudents()       // 기본: page=0, size=20

await gradePhase3.listStudents(1, 10)  // 커스텀설정된 config 객체```

```



### 테스트 함수

**예시:**### 주요 함수

**`attendance()`** - 출석 업데이트 → 성적 재계산

```javascript

```javascript

API: POST /api/enrollments/grade-info (enrollmentIdx 자동 조회)gradePhase1.quickAttendanceConfig(80, 0.5)#### `checkAuth()`

API: PUT /api/enrollments/{enrollmentIdx}/attendance

gradePhase3.setLecture('ETH201', 6)gradePhase1.quickAttendanceConfig(90, 1.0)  // 출석 만점 90, 지각 1점JWT 토큰 인증 체크

await gradePhase3.attendance()

``````



**`assignment()`** - 과제 점수 → 성적 재계산**반환:**



```javascript---```javascript

API: POST /api/enrollments/grade-info (enrollmentIdx 자동 조회)

API: PUT /api/assignments/{assignmentIdx}/grade"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

gradePhase3.setLecture('ETH201', 6, 1)

await gradePhase3.assignment()### `promptConfig()````

```



**`runAll()`** - 전체 테스트

대화형 프롬프트로 성적 구성을 설정합니다.**예외:**

```javascript

gradePhase3.setLecture('ETH201', 6)```javascript

await gradePhase3.runAll()

```**입력 항목:**throw new Error("⚠️ 로그인이 필요합니다!")



------1. 출석 만점 (기본값: 80)```



## 📚 관련 문서2. 지각 페널티/회 (기본값: 0.5)



- [빠른 시작](./05-QUICK-START.md) - 5분 퀵 스타트3. 등급 분포 - A% (기본값: 30)---

- [사용법 가이드](./06-USAGE-GUIDE.md) - 전체 기능 설명

- [문제 해결](./09-TROUBLESHOOTING.md) - 오류 해결4. 등급 분포 - B% (기본값: 40)


5. 등급 분포 - C% (기본값: 20)#### `apiCall(method, endpoint, data)`

6. 등급 분포 - D% (기본값: 10)범용 API 호출 함수



**참고:****파라미터:**

- 과제 총점은 입력 불가 (자동 누적)- `method`: "GET" | "POST" | "PUT" | "DELETE"

- 등급 합계 100% 검증- `endpoint`: "/api/enrollments/..."

- `data`: Request body (optional)

**예시:**

```javascript**반환:**

gradePhase1.promptConfig()```javascript

```{

    success: true,

---    message: "...",

    data: {...}

### `updateConfig(partialConfig)`}

```

config 객체를 직접 수정합니다.

---

**파라미터:**

- `partialConfig` (object): 수정할 설정 (일부만 가능)#### `apiGet(endpoint)`

GET 요청 래퍼

**경고:**

- `assignmentTotalMaxScore` 설정 시 경고 출력**예시:**

```javascript

**예시:**const result = await gradeTestUtils.apiGet('/api/enrollments/1/100/grade')

```javascript```

// 출석 만점만 변경

gradePhase1.updateConfig({ attendanceMaxScore: 90 })---



// 지각 페널티 + 등급 분포 변경#### `apiPut(endpoint, data)`

gradePhase1.updateConfig({PUT 요청 래퍼

    latePenaltyPerSession: 1.0,

    gradeDistribution: { A: 25, B: 45, C: 20, D: 10 }**예시:**

})```javascript

```const result = await gradeTestUtils.apiPut(

    '/api/enrollments/1/attendance',

---    { status: 'PRESENT', date: '2024-01-15' }

)

### `getConfig()````



현재 설정된 config 객체를 조회합니다.---



**반환:**#### `testData`

```javascript기본 테스트 데이터 객체

{

    passingThreshold: 60,**구조:**

    attendanceMaxScore: 80,```javascript

    assignmentTotalMaxScore: 0,  // 🔒 자동 계산{

    latePenaltyPerSession: 0.5,    lecIdx: 1,

    gradeDistribution: { A: 30, B: 40, C: 20, D: 10 }    studentIdx: 100,

}    professorIdx: 22,

```    enrollmentIdx: 1,

    assignmentIdx: 1,

**예시:**    passingThreshold: 60.0,

```javascript    attendanceMaxScore: 80,

const config = gradePhase1.getConfig()    assignmentTotalMaxScore: 100,

console.log(config.attendanceMaxScore)  // 80    latePenaltyPerSession: 0.5,

```    gradeDistribution: { "A+": 10, "A": 15, ... }

}

---```



## 🧪 테스트 함수---



### `runAll()`#### `setTestData(updates)`

테스트 데이터 동적 수정

Phase 1 전체 테스트를 순차 실행합니다.

**예시:**

**실행 순서:**```javascript

1. 성적 구성 설정gradeTestUtils.setTestData({

2. 학생 성적 조회    lecIdx: 2,

3. 교수용 성적 조회    studentIdx: 101

4. 성적 목록 조회})

5. 최종 등급 배정```



**예시:**---

```javascript

gradePhase1.setLecture('ETH201')#### `getTestData()`

await gradePhase1.runAll()현재 테스트 데이터 조회

```

**반환:**

**출력:**```javascript

```{ lecIdx: 1, studentIdx: 100, ... }

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━```

🎉 전체 테스트 완료!

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━---

📊 최종 통계:

   총 테스트: 5개## 2️⃣ `02-grade-phase1-tests.js`

   성공: 5개

   실패: 0개### 역할

   성공률: 100%- Phase 1 핵심 기능 테스트 (5개)

```

### 의존성

---- `01-grade-test-utils.js` 필수



### `config()`### 글로벌 객체

```javascript

성적 구성 설정 API를 호출합니다.window.gradePhase1Tests

```

**API:**

```### 주요 함수

POST /api/enrollments/grade-config?lecSerial={lecSerial}

```#### `testGradeConfig()`

성적 구성 설정 테스트

**요청 데이터:**

```json**API:** `POST /api/enrollments/grade-config`

{

  "passingThreshold": 60,**기능:**

  "attendanceMaxScore": 80,- 출석 만점 설정

  "latePenaltyPerSession": 0.5,- 과제 만점 설정

  "gradeDistribution": { "A": 30, "B": 40, "C": 20, "D": 10 }- 지각 페널티 설정

}- 등급 분포 설정

```

**예상 출력:**

**참고:** `assignmentTotalMaxScore`는 제외됨 (서버 자동 계산)```

✅ 성적 구성 설정 성공

**예시:**📝 메시지: 성적 구성이 성공적으로 저장되었습니다.

```javascript```

gradePhase1.setLecture('ETH201')

await gradePhase1.config()---

```

#### `testStudentGradeInfo()`

---학생 성적 조회 테스트



### `studentInfo()`**API:** `GET /api/enrollments/{lecIdx}/{studentIdx}/grade`



학생 성적 정보를 조회합니다.**기능:**

- 출석 점수/비율 조회

**API:**- 과제 점수/비율 조회

```- 총점/백분율 계산

GET /api/enrollments/{lecSerial}/{studentIdx}/grade

```**예상 출력:**

```

**응답 예시:**📊 성적 상세 정보:

```json  📅 출석: 18.5 / 80 (23.1%)

{  📝 과제: 85.0 / 100 (85.0%)

  "attendanceScore": 75.5,  📈 총점: 103.5 (51.8%)

  "attendanceMaxScore": 80,```

  "attendancePercentage": 94.4,

  "assignmentScore": 91.0,---

  "assignmentMaxScore": 100,

  "assignmentPercentage": 91.0,#### `testProfessorGradeView()`

  "totalScore": 166.5,교수용 성적 조회 테스트

  "totalMaxScore": 180,

  "percentage": 92.5**API:** `GET /api/enrollments/professor/grade`

}

```**기능:**

- 전체 학생 성적 목록

**예시:**- 통계 (평균, 최고점, 최저점, 순위)

```javascript

gradePhase1.setLecture('ETH201', 6)---

await gradePhase1.studentInfo()

```#### `testGradeList()`

성적 목록 조회 테스트

---

**API:** `GET /api/enrollments/grade-list`

### `professorView()`

**기능:**

교수용 성적 조회 (학생 성적 + 반 통계)를 가져옵니다.- 페이징 (page, size)

- 정렬 (percentage/grade, asc/desc)

**API:**

```---

GET /api/enrollments/professor/grade?lecSerial={lecSerial}&studentIdx={studentIdx}

```#### `testGradeFinalize()`

최종 등급 배정 테스트

**응답 예시:**

```json**API:** `POST /api/enrollments/finalize-grades`

{

  "studentGrade": {**기능:**

    "totalScore": 75.5,- 60% 기준 합격/불합격

    "percentage": 94.4- 상대평가 (A+~D)

  },- 동점자 처리

  "classStats": {

    "averageScore": 68.2,**예상 출력:**

    "averagePercentage": 85.3,```

    "highestScore": 88.0,📊 등급 분포:

    "highestPercentage": 97.8,  A+: 10명 (10%)

    "studentRank": 3,  A: 15명 (15%)

    "totalStudents": 25  B+: 15명 (15%)

  }  ...

}```

```

---

**예시:**

```javascript#### `runPhase1Tests()`

gradePhase1.setLecture('ETH201', 6)Phase 1 전체 실행 (5개)

await gradePhase1.professorView()

```**순서:**

1. config → 2. studentInfo → 3. professorView → 4. gradeList → 5. finalize

---

---

### `gradeList()`

## 3️⃣ `03-grade-phase3-tests.js`

전체 학생 성적 목록을 조회합니다.

### 역할

**API:**- Phase 3 이벤트 시스템 테스트 (2개)

```

GET /api/enrollments/grade-list?lecSerial={lecSerial}&page=0&size=10&sort=percentage,desc### 의존성

```- `01-grade-test-utils.js` 필수



**응답 예시:**### 글로벌 객체

```json```javascript

{window.gradePhase3Tests

  "content": [```

    {

      "studentName": "홍길동",### 주요 함수

      "studentIdx": 100,

      "totalScore": 176.0,#### `testAttendanceUpdate()`

      "percentage": 97.8,출석 업데이트 + 이벤트 발행

      "finalGrade": "A",

      "rank": 1**API:** `PUT /api/enrollments/{enrollmentIdx}/attendance`

    },

    ...**기능:**

  ],- 출석/지각/결석 기록

  "totalElements": 25- `GradeUpdateEvent(ATTENDANCE)` 발행

}- 비동기 성적 재계산

```

**예상 출력:**

**예시:**```

```javascript✅ 출석 업데이트 성공

gradePhase1.setLecture('ETH201')🔔 이벤트 발행됨: ATTENDANCE

await gradePhase1.gradeList()```

```

**Eclipse 로그:**

---```

[GradeUpdateEventListener] 성적 업데이트 이벤트 수신

### `finalize()`- 타입: ATTENDANCE

- 수강 ID: 1

최종 등급을 배정합니다.```



**API:**---

```

POST /api/enrollments/finalize-grades?lecSerial={lecSerial}#### `testAssignmentGrade()`

```과제 채점 + 이벤트 발행



**로직:****API:** `PUT /api/assignments/{assignmentIdx}/grade`

1. 60% 이상 → 합격 (상대평가 대상)

2. 60% 미만 → F등급 확정**기능:**

3. 등급 분포에 따라 A/B/C/D 배정 (하위 침범 방식)- 과제 점수 입력

- `GradeUpdateEvent(ASSIGNMENT)` 발행

**응답 예시:**- 비동기 성적 재계산

```json

{**예상 출력:**

  "message": "등급 배정이 완료되었습니다.",```

  "gradeDistribution": {✅ 과제 채점 성공

    "A": 8,🔔 이벤트 발행됨: ASSIGNMENT

    "B": 10,```

    "C": 5,

    "D": 2,**Eclipse 로그:**

    "F": 0```

  }[GradeUpdateEventListener] 성적 업데이트 이벤트 수신

}- 타입: ASSIGNMENT

```- 수강 ID: 1

```

**예시:**

```javascript---

gradePhase1.setLecture('ETH201')

await gradePhase1.finalize()#### `runPhase3Tests()`

```Phase 3 전체 실행 (2개)



---**순서:**

1. attendance → 2초 대기 → 2. assignment → 2초 대기

## 📊 점수 계산 공식

**대기 시간:** 이벤트 처리를 위한 비동기 대기

### 출석 점수

---

```

출석 점수 = (출석율 × 출석만점) - (지각 횟수 × 지각페널티)## 4️⃣ `04-grade-test-runner.js`

```

### 역할

**예:**- 모든 테스트 통합 및 오케스트레이션

```- 사용자 친화적 인터페이스 제공

(77/80 × 80) - (3 × 0.5) = 77 - 1.5 = 75.5점

```### 의존성

- `02-grade-phase1-tests.js` 필수

### 과제 점수- `03-grade-phase3-tests.js` 필수



```### 글로벌 객체

과제 점수 = 과제1 점수 + 과제2 점수 + ... + 과제N 점수```javascript

```window.gradeTests

```

**자동 누적:**

- 과제 생성 시 개별 만점 설정### 통합 인터페이스

- 서버가 자동으로 합산

#### 개별 테스트 함수

### 총점 및 백분율

```javascript

```gradeTests.config()          // Phase 1-1

총점 = 출석 점수 + 과제 점수gradeTests.studentInfo()     // Phase 1-2

백분율 = (총점 / 총만점) × 100gradeTests.professorView()   // Phase 1-3

```gradeTests.gradeList()       // Phase 1-4

gradeTests.finalize()        // Phase 1-5

**예:**gradeTests.attendance()      // Phase 3-1

```gradeTests.assignment()      // Phase 3-2

총점 = 75.5 + 91 = 166.5점```

백분율 = (166.5 / 180) × 100 = 92.5%

```#### Phase 실행 함수



---```javascript

gradeTests.phase1()   // Phase 1 전체 (5개)

## 🔑 핵심 개념gradeTests.phase3()   // Phase 3 전체 (2개)

```

### 강의 설정 필수

#### 전체 실행 함수

모든 테스트 전에 반드시 호출:

```javascript```javascript

gradePhase1.setLecture('ETH201')  // 또는 강의코드 + 학생IDXgradeTests.runAll()   // Phase 1 + Phase 3 (7개)

``````



### 과제 총점 자동 관리**플로우:**

1. Phase 1 테스트 5개

- ❌ 사용자 설정 불가2. 3초 대기

- ✅ 서버에서 자동 누적3. Phase 3 테스트 2개

- 과제 추가/삭제 시 자동 갱신4. 통계 출력



### 등급 분포 제약---



- 합계 100% 필수#### 시나리오 함수

- A + B + C + D = 100%

- 하위 침범 방식 (남은 학생 → 다음 등급)```javascript

gradeTests.scenario()  // 6단계 워크플로우

---```



## 📚 다음 단계**워크플로우:**

1. 성적 구성 설정

- [빠른 시작](./05-QUICK-START.md)2. 출석 기록 (이벤트)

- [상세 사용법](./06-USAGE-GUIDE.md)3. 과제 채점 (이벤트)

- [문제 해결](./09-TROUBLESHOOTING.md)4. 학생 성적 확인

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
