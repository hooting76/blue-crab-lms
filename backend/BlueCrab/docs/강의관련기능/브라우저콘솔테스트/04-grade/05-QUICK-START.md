# 🚀 빠른 시작 가이드

성적 관리 시스템을 **3단계**로 빠르게 시작하세요!

---

## 📁 파일 구조

```
04-grade/
├── 01-grade-test-utils.js       # ① 🧰 테스트 도구 (실행X, 도구만 제공)
├── 02-grade-phase1-tests.js     # ② 🎯 Phase 1 (5개 테스트 실행)
├── 03-grade-phase3-tests.js     # ③ ⚡ Phase 3 (2개 테스트 실행)
└── 04-grade-test-runner.js      # ④ 🚀 통합 러너 (선택)
```

**💡 중요**: 01번은 도구 모음이므로 자체로는 테스트를 실행하지 않습니다!

---

## ⚡ 3단계 빠른 시작

### 1️⃣ 로그인

```javascript
await login()  // 교수 계정 필수
```

### 2️⃣ 모듈 로드 (순서대로)

각 파일을 **복사 → 붙여넣기 → 확인** 후 다음으로!

```javascript
// ① Utils
// 01-grade-test-utils.js 복사 → 콘솔 붙여넣기
window.gradeTestUtils  // ✅ 확인

// ② Phase 1
// 02-grade-phase1-tests.js 복사 → 콘솔 붙여넣기
window.gradePhase1Tests  // ✅ 확인

// ③ Phase 3
// 03-grade-phase3-tests.js 복사 → 콘솔 붙여넣기
window.gradePhase3Tests  // ✅ 확인

// ④ Runner
// 04-grade-test-runner.js 복사 → 콘솔 붙여넣기
window.gradeTests  // ✅ 확인
```

### 3️⃣ 테스트 실행

```javascript
await gradeTests.runAll()  // 전체 7개 테스트 실행
```

---

## 📋 개별 테스트

```javascript
// Phase 1 (핵심 기능 5개)
await gradeTests.config()          // 성적 구성 설정
await gradeTests.studentInfo()     // 학생 성적 조회
await gradeTests.professorView()   // 교수용 성적 조회
await gradeTests.gradeList()       // 성적 목록 조회
await gradeTests.finalize()        // 최종 등급 배정

// Phase 3 (이벤트 시스템 2개)
await gradeTests.attendance()      // 출석 업데이트 → 이벤트
await gradeTests.assignment()      // 과제 채점 → 이벤트
```

---

## 🔍 이벤트 확인

Phase 3 실행 후 **Eclipse Console**에서:

```
[GradeUpdateEventListener] 성적 업데이트 이벤트 수신
```

---

## ✅ 성공 기준

- ✅ 7/7 테스트 성공 (100%)
- ✅ 2/2 이벤트 발행 확인
- ✅ Eclipse 로그 확인

---

## 📚 더 알아보기

- [상세 사용법](./06-USAGE-GUIDE.md)
- [전체 테스트 가이드](./07-TESTING-GUIDE.md)
- [모듈 참조](./08-MODULE-REFERENCE.md)
- [문제 해결](./09-TROUBLESHOOTING.md)
