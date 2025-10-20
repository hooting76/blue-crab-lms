#  성적 관리 시스템 테스트

Task G (성적 관리 시스템) Phase 4 브라우저 콘솔 테스트

---

##  파일 구조

```
04-grade/
├── 01-grade-test-utils.js       # 🧰 테스트 도구 모음 (실행X, 도구만 제공)
├── 02-grade-phase1-tests.js     # 🎯 Phase 1 (5개 테스트 실행)
├── 03-grade-phase3-tests.js     # ⚡ Phase 3 (2개 테스트 실행)
├── 04-grade-test-runner.js      # 🚀 통합 러너 (선택)
├── 00-README.md                 # 이 문서
├── 05-QUICK-START.md            # 빠른 시작
├── 06-USAGE-GUIDE.md            # 사용법
├── 07-TESTING-GUIDE.md          # Phase 4 테스트
├── 08-MODULE-REFERENCE.md       # 모듈 참조
├── 09-TROUBLESHOOTING.md        # 문제 해결
├── 10-LEGACY.md                 # 레거시
└── 11-TEST-FLOW.drawio          # 테스트 플로우 다이어그램
```

---

##  빠른 시작

```javascript
// 1. 로그인
await login()

// 2. 모듈 로드 (01  02  03  04)
// 각 파일 복사  붙여넣기

// 3. 테스트 실행
await gradeTests.runAll()
```

 [05-QUICK-START.md](./05-QUICK-START.md)

---

##  문서 가이드

### 처음 시작
 [05-QUICK-START.md](./05-QUICK-START.md)

### 모든 기능
 [06-USAGE-GUIDE.md](./06-USAGE-GUIDE.md)

### 정식 테스트
 [07-TESTING-GUIDE.md](./07-TESTING-GUIDE.md)

### 모듈 구조
 [08-MODULE-REFERENCE.md](./08-MODULE-REFERENCE.md)

### 문제 해결
 [09-TROUBLESHOOTING.md](./09-TROUBLESHOOTING.md)

### 레거시
 [10-LEGACY.md](./10-LEGACY.md)

---

##  테스트 목록

### Phase 1: 핵심 기능 (5개)

1. 성적 구성 설정 - `gradeTests.config()`
2. 학생 성적 조회 - `gradeTests.studentInfo()`
3. 교수용 성적 조회 - `gradeTests.professorView()`
4. 성적 목록 조회 - `gradeTests.gradeList()`
5. 최종 등급 배정 - `gradeTests.finalize()`

### Phase 3: 이벤트 (2개)

6. 출석 업데이트 - `gradeTests.attendance()`
7. 과제 채점 - `gradeTests.assignment()`

---

##  의존성

```
01-grade-test-utils.js
    
     02-grade-phase1-tests.js
     03-grade-phase3-tests.js
            
        04-grade-test-runner.js
```

**필수:** 순서대로 로드 (01  02  03  04)

---

##  성공 기준

- Phase 1: 5/5 성공
- Phase 3: 2/2 성공
- 전체: 100% (7/7)
- Eclipse: 이벤트 로그 2개 확인

---

##  관련 문서

- [강의관련기능 README](../README.md)
- [성적관리시스템 구현 가이드](../../성적관리/성적관리시스템_구현가이드.md)
