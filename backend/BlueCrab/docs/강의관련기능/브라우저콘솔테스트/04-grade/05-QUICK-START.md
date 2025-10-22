# 🚀 빠른 시작 가이드# 🚀 빠른 시작 가이드



성적 관리 시스템을 5분 안에 시작하는 방법입니다.성적 관리 시스템을 **3단계**로 빠르게 시작하세요!



------



## 📖 목차## 📁 파일 구조



- [Phase 1: 핵심 기능](#phase-1-핵심-기능)```

- [Phase 3: 이벤트 시스템](#phase-3-이벤트-시스템)04-grade/

├── 01-grade-phase1-tests.js     # ① 🎯 Phase 1 (5개 테스트 실행)

---├── 02-grade-phase3-tests.js     # ② ⚡ Phase 3 (2개 테스트 실행) - 선택

└── 03-grade-test-runner.js      # ③ 🚀 통합 러너 - 선택

## Phase 1: 핵심 기능```



### ⚡ 3분 퀵 스타트**💡 중요**: Phase 1만으로도 완전한 성적 관리 테스트 가능!



```javascript---

// 1. 로그인

await login()  // 교수 계정## ⚡ 3단계 빠른 시작



// 2. 파일 로드### 1️⃣ 로그인

// 01-grade-phase1-tests.js 복사 → 브라우저 콘솔 붙여넣기

```javascript

// 3. 강의 설정await login()  // 교수 계정 필수

gradePhase1.setLecture('ETH201')```



// 4. 전체 테스트### 2️⃣ Phase 1 파일 로드

await gradePhase1.runAll()

``````javascript

// 01-grade-phase1-tests.js 복사 → 콘솔 붙여넣기 → Enter

### 📋 테스트 항목 (5개)window.gradePhase1  // ✅ 확인

```

1. ⚙️ 성적 구성 설정

2. 👤 학생 성적 조회### 3️⃣ 강의 설정 & 테스트 실행

3. 👨‍🏫 교수용 성적 조회

4. 📊 성적 목록 조회```javascript

5. 🎓 최종 등급 배정// 강의 설정

gradePhase1.setLecture('ETH201')

---

// 전체 테스트 실행 (5개)

## Phase 3: 이벤트 시스템await gradePhase1.runAll()

```

### ⚡ 3분 퀵 스타트

---

```javascript

// 1. 로그인## 📋 개별 테스트

await login()  // 교수 계정

```javascript

// 2. 파일 로드// 1. 성적 구성 설정 (강의 단위 - 출석/지각 페널티/등급 분포)

// 02-grade-phase3-tests.js 복사 → 브라우저 콘솔 붙여넣기gradePhase1.setLecture('ETH201')

await gradePhase1.config()

// 3. 수강생 확인 (선택)

gradePhase3.setLecture('ETH201')// 2. 학생 성적 조회 (출석+과제 점수, 총점, 백분율)

await gradePhase3.listStudents()gradePhase1.setLecture('ETH201', 6)  // 강의코드 + 학생IDX

// → studentIdx 확인await gradePhase1.studentInfo()



// 4. 강의+학생 설정// 3. 교수용 성적 조회 (학생 성적 + 반 통계)

gradePhase3.setLecture('ETH201', 6)await gradePhase1.professorView()



// 5. 전체 테스트// 4. 성적 목록 조회 (전체 학생 목록)

await gradePhase3.runAll()gradePhase1.setLecture('ETH201')

```await gradePhase1.gradeList()



### 📋 테스트 항목 (2개)// 5. 최종 등급 배정 (60% 합격 + 상대평가)

await gradePhase1.finalize()

1. 📅 출석 업데이트 → 성적 자동 재계산```

2. 📝 과제 점수 → 성적 자동 재계산

---

### 🎯 핵심 기능

## ⚙️ 성적 구성 설정 (3가지 방법)

- **enrollmentIdx 자동 조회**: lecSerial + studentIdx만 입력

- **수강생 목록 조회**: `listStudents()` 함수로 간편 확인### 방법 1: 간편 수정 (출석 관련만) ⭐ 추천!

- **이벤트 기반 재계산**: 출석/과제 변경 시 성적 자동 갱신

```javascript

---gradePhase1.quickAttendanceConfig(80, 0.5)

//                                 ^^  ^^^

## 📚 관련 가이드//                          출석 만점  지각 감점/회

await gradePhase1.config()  // 서버 반영

- [사용법 가이드](./06-USAGE-GUIDE.md) - 전체 기능 설명```

- [테스트 가이드](./07-TESTING-GUIDE.md) - 단계별 테스트

- [문제 해결](./09-TROUBLESHOOTING.md) - 오류 해결### 방법 2: 대화형 입력


```javascript
gradePhase1.promptConfig()   // 프롬프트로 입력
await gradePhase1.config()   // 서버 반영
```

### 방법 3: 객체로 직접 수정

```javascript
gradePhase1.updateConfig({
    attendanceMaxScore: 90,
    latePenaltyPerSession: 1.0,
    gradeDistribution: { A: 25, B: 45, C: 20, D: 10 }
})
await gradePhase1.config()
```

**💡 참고**: 과제 총점은 과제 생성 시 자동 누적되므로 설정 불필요

---

## ✅ 성공 기준

- ✅ 5/5 테스트 성공 (100%)
- ✅ HTTP 200 응답 확인
- ✅ 성적 데이터 정상 조회

---

## 📚 더 알아보기

- [상세 사용법](./06-USAGE-GUIDE.md)
- [전체 테스트 가이드](./07-TESTING-GUIDE.md)
- [문제 해결](./09-TROUBLESHOOTING.md)
