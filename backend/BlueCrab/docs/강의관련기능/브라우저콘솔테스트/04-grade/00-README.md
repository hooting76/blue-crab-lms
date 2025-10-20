# 성적 관리 시스템 테스트 (독립 실행 버전)

## 🎯 개요

**완전히 새로 설계된 독립 실행 버전**

- ✅ **각 파일이 완전 독립적으로 실행 가능**
- ✅ **lecSerial + JWT 토큰 기반 보안 인증**
- ✅ **파일 간 의존성 없음**
- ✅ **서버 측 권한 검증 (교수가 해당 강의 접근 권한 있는지 확인)**

---

## 📁 파일 구성

### ✅ 테스트 파일

| 파일 | 용도 | 독립 실행 |
|------|------|----------|
| `01-grade-phase1-tests.js` | 5개 핵심 성적 관리 테스트 | ✅ 가능 |
| `02-grade-phase3-tests.js` | 2개 이벤트 업데이트 테스트 | ✅ 가능 |

### 🔧 선택 파일

| 파일 | 용도 | 비고 |
|------|------|------|
| `03-grade-test-runner.js` | 통합 실행 편의 기능 | Phase 1+3 통합 |

---

## 🚀 빠른 시작

### 1️⃣ 로그인 (필수)

```javascript
// 교수 계정으로 로그인
await login()
```

### 2️⃣ Phase 1 테스트 (핵심 기능)

```javascript
// 01-grade-phase1-tests.js 파일 전체 복사 → 콘솔 붙여넣기

// 강의 설정
gradePhase1.setLecture('CS101-2024-1', 'student@univ.edu')

// 전체 실행
await gradePhase1.runAll()
```

### 3️⃣ Phase 3 테스트 (이벤트 업데이트)

```javascript
// 02-grade-phase3-tests.js 파일 전체 복사 → 콘솔 붙여넣기

// 강의 설정
gradePhase3.setLecture('CS101-2024-1', 'student@univ.edu', 42)
//                                                          ^^
//                                                       과제 IDX

// 전체 실행
await gradePhase3.runAll()
```

### 4️⃣ 통합 실행 (선택)

```javascript
// Phase 1, 3 파일 로드 후
// 03-grade-test-runner.js 파일 복사 → 콘솔 붙여넣기

// 통합 설정
gradeRunner.setup('CS101-2024-1', 'student@univ.edu', 42)

// Phase 1 + Phase 3 전체 실행
await gradeRunner.runAll()
```

---

## 📊 Phase 1 테스트 (5개)

### API 엔드포인트 (lecSerial 기반)

| # | 테스트 | API | 메서드 |
|---|--------|-----|--------|
| 1 | 성적 구성 설정 | `/lectures/{lecSerial}/grade-config` | POST |
| 2 | 학생 성적 조회 | `/lectures/{lecSerial}/students/{email}/grade` | GET |
| 3 | 교수용 조회 | `/lectures/{lecSerial}/professor/grade?studentEmail={email}` | GET |
| 4 | 성적 목록 | `/lectures/{lecSerial}/grade-list?page=0&size=20` | GET |
| 5 | 최종 등급 배정 | `/lectures/{lecSerial}/finalize-grades` | POST |

### 개별 실행

```javascript
await gradePhase1.config()        // 1. 성적 구성
await gradePhase1.studentInfo()   // 2. 학생 조회
await gradePhase1.professorView() // 3. 교수 조회
await gradePhase1.gradeList()     // 4. 목록
await gradePhase1.finalize()      // 5. 등급 배정
```

---

## 📊 Phase 3 테스트 (2개)

### API 엔드포인트

| # | 테스트 | API | 메서드 |
|---|--------|-----|--------|
| 1 | 출석 업데이트 | `/lectures/{lecSerial}/attendance` | POST |
| 2 | 과제 점수 업데이트 | `/lectures/{lecSerial}/assignments/{assignmentIdx}/grade` | PUT |

### 개별 실행

```javascript
await gradePhase3.attendance()  // 1. 출석 → 성적 반영
await gradePhase3.assignment()  // 2. 과제 → 성적 반영
```

---

## 🔐 보안 개선사항

### AS-IS (구버전 - 보안 취약)

```javascript
// lecIdx로 직접 접근 - 권한 검증 없음
POST /enrollments/1/students/100/grade

// 문제점:
// - 교수가 다른 강의(lecIdx=2,3,4...)도 접근 가능
// - JWT 토큰이 있어도 서버가 권한 검증 안 함
```

### TO-BE (v2 - 보안 강화)

```javascript
// lecSerial로 식별 + JWT 토큰 검증
POST /lectures/CS101-2024-1/students/student@univ.edu/grade
Authorization: Bearer {JWT_TOKEN}

// 개선사항:
// ✅ 서버가 JWT에서 교수 ID 추출
// ✅ CS101-2024-1 강의의 담당 교수인지 DB 검증
// ✅ 권한 없으면 403 Forbidden 반환
// ✅ lecSerial 방식으로 자연스러운 접근 제어
```

---

## 💡 사용 예시

### 시나리오 1: Phase 1만 빠르게 테스트

```javascript
// 1. 로그인
await login()

// 2. 01 파일 로드 (콘솔에 붙여넣기)

// 3. 프롬프트로 설정
gradePhase1.promptLecture()
// → 팝업에서 강의 코드, 학생 이메일 입력

// 4. 실행
await gradePhase1.runAll()
```

### 시나리오 2: 특정 테스트만 실행

```javascript
// 성적 구성만 테스트
gradePhase1.setLecture('CS101-2024-1')
await gradePhase1.config()

// 학생 성적 조회만 테스트
await gradePhase1.studentInfo()
```

### 시나리오 3: 통합 실행

```javascript
// 1. 로그인
await login()

// 2. Phase 1, 3, Runner 파일 모두 로드

// 3. 한 번에 설정
gradeRunner.setup('CS101-2024-1', 'student@univ.edu', 42)

// 4. Phase 1만
await gradeRunner.phase1()

// 5. Phase 3만
await gradeRunner.phase3()

// 6. 전체
await gradeRunner.runAll()
```

### 시나리오 4: 바로가기 사용

```javascript
// Runner 로드 후 바로 개별 테스트 실행
await gradeRunner.config()      // Phase 1의 config()
await gradeRunner.attendance()  // Phase 3의 attendance()
```

---

## 📝 설정 방법

### 방법 1: setLecture() 직접 호출

```javascript
// Phase 1
gradePhase1.setLecture('CS101-2024-1', 'student@univ.edu')

// Phase 3
gradePhase3.setLecture('CS101-2024-1', 'student@univ.edu', 42)
//                                                          ^^
//                                                       과제 IDX
```

### 방법 2: promptLecture() 대화형 입력

```javascript
gradePhase1.promptLecture()
// → 팝업창에서 입력

gradePhase3.promptLecture()
// → 팝업창에서 입력 (과제 IDX 포함)
```

### 방법 3: 설정 확인

```javascript
gradePhase1.getConfig()
// → { lecSerial: 'CS101-2024-1', studentEmail: '...' }

gradePhase3.getConfig()
// → { lecSerial: '...', studentEmail: '...', assignmentIdx: 42 }
```

---

## ⚙️ API 데이터 구조

### 성적 구성 설정 (POST)

```javascript
{
  "attendanceMaxScore": 80,
  "assignmentTotalMaxScore": 100,
  "latePenaltyPerSession": 0.5,
  "gradeDistribution": {
    "A+": 10, "A": 15, "B+": 20,
    "B": 25, "C": 20, "D": 10
  }
}
```

### 출석 기록 (POST)

```javascript
{
  "studentEmail": "student@univ.edu",
  "attendanceDate": "2024-10-20",
  "status": "PRESENT"  // PRESENT, LATE, ABSENT
}
```

### 과제 채점 (PUT)

```javascript
{
  "studentEmail": "student@univ.edu",
  "score": 85
}
```

### 최종 등급 배정 (POST)

```javascript
{
  "passingThreshold": 60.0,
  "gradeDistribution": {
    "A+": 10, "A": 15, "B+": 20,
    "B": 25, "C": 20, "D": 10
  }
}
```

---

## 🔄 v1 → v2 마이그레이션

### 주요 변경사항

| 항목 | v1 (구버전) | v2 (신버전) |
|------|------------|-----------|
| **의존성** | 01-utils.js 필수 | 완전 독립 |
| **식별자** | `lecIdx`, `studentIdx` | `lecSerial`, `studentEmail` |
| **API** | `/enrollments/{lecIdx}/...` | `/lectures/{lecSerial}/...` |
| **인증** | JWT 토큰만 | JWT + 서버 권한 검증 |
| **실행** | 3파일 순차 로드 필요 | 각 파일 단독 실행 가능 |

### 코드 비교

```javascript
// v1 (구버전)
gradeTestUtils.inputTestData()  // 01 파일 필요
gradeTestUtils.setTestData(6, 100)
await runPhase1Tests()  // 전역 함수

// v2 (신버전)
gradePhase1.setLecture('CS101-2024-1', 'student@univ.edu')
await gradePhase1.runAll()  // 네임스페이스화
```

---

## 🛠️ 트러블슈팅

### Q: "gradePhase1 is not defined"

```javascript
// A: 파일을 콘솔에 붙여넣지 않았습니다
// 02-grade-phase1-tests-v2.js 전체를 콘솔에 복사 → 붙여넣기
```

### Q: "❌ 로그인 필요!"

```javascript
// A: JWT 토큰이 없습니다
await login()  // 교수 계정으로 로그인
```

### Q: "lecSerial 미설정!"

```javascript
// A: 강의 코드를 설정하지 않았습니다
gradePhase1.setLecture('CS101-2024-1')
// 또는
gradePhase1.promptLecture()  // 팝업으로 입력
```

### Q: "Phase 파일 미로드"

```javascript
// A: Runner 사용 시 Phase 파일 먼저 로드 필요
// 1. 01-grade-phase1-tests.js 로드
// 2. 02-grade-phase3-tests.js 로드
// 3. 03-grade-test-runner.js 로드
```

---

## 📋 체크리스트

### 테스트 전 확인사항

- [ ] 교수 계정으로 로그인 완료
- [ ] JWT 토큰 존재 확인 (`window.authToken` 또는 `localStorage.getItem('jwtAccessToken')`)
- [ ] 테스트할 강의 코드 준비 (예: `CS101-2024-1`)
- [ ] 테스트할 학생 이메일 준비 (예: `student@univ.edu`)
- [ ] (Phase 3) 테스트할 과제 IDX 준비

### 실행 순서

1. [ ] `await login()` 실행
2. [ ] 테스트 파일 콘솔에 붙여넣기 (01 또는 02)
3. [ ] `setLecture()` 또는 `promptLecture()` 실행
4. [ ] `runAll()` 또는 개별 테스트 실행
5. [ ] 콘솔 출력 확인

---

## 📞 지원

문제 발생 시:

1. 콘솔 에러 메시지 확인
2. `gradePhase1.getConfig()` 로 설정 확인
3. `window.authToken` 확인
4. 네트워크 탭에서 API 응답 확인

---

## 📌 변경 이력

### v2.1 (2024-10-20)

- ✅ 파일 번호 정리 (01, 02, 03)
- ✅ 불필요한 utils 파일 완전 제거
- ✅ 문서 업데이트

### v2.0 (2024-10-20)

- ✅ 완전 독립 실행 가능 구조
- ✅ lecSerial + JWT 보안 인증
- ✅ 파일 간 의존성 제거
- ✅ 네임스페이스 분리 (gradePhase1, gradePhase3, gradeRunner)
- ✅ 각 파일별 API 함수 내장

### v1 (이전)

- ❌ utils.js 필수 의존성
- ❌ lecIdx 직접 접근 (보안 취약)
- ❌ 전역 함수 충돌 위험
