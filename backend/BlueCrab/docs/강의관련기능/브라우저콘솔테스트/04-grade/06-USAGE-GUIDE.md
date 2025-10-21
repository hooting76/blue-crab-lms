# 📚 사용법 가이드# 📚 사용법 상세 가이드



성적 관리 시스템의 전체 기능을 설명합니다.성적 관리 시스템의 모든 기능을 자세히 설명합니다.



---------



## 📖 목차## 📖 목차



- [Phase 1: 핵심 기능](#phase-1-핵심-기능)- [테스트 목록](#-테스트-목록)

- [Phase 3: 이벤트 시스템](#phase-3-이벤트-시스템)- [실행 방법](#-실행-방법)

- [공통 기능](#공통-기능)- [성적 구성 설정](#-성적-구성-설정)

- [점수 계산 구조](#-점수-계산-구조)

------

---

## Phase 1: 핵심 기능

## 📋 테스트 목록

### 📋 테스트 목록 (5개)

### Phase 1: 핵심 기능 (5개)

| 번호 | 기능 | 함수 | API |

|------|------|------|-----|| 번호 | 기능 | 함수 |

| 1 | 성적 구성 설정 | `config()` | POST /enrollments/grade-config ||------|------|------|

| 2 | 학생 성적 조회 | `studentInfo()` | POST /enrollments/grade-info || 1 | 성적 구성 설정 (출석/지각/등급 분포) | `gradePhase1.config()` |

| 3 | 교수용 성적 조회 | `professorView()` | POST /enrollments/grade-info || 2 | 학생 성적 조회 | `gradePhase1.studentInfo()` |

| 4 | 성적 목록 조회 | `gradeList()` | POST /enrollments/grade-list || 3 | 교수용 성적 조회 | `gradePhase1.professorView()` |

| 5 | 최종 등급 배정 | `finalize()` | POST /enrollments/grade-finalize || 4 | 성적 목록 조회 | `gradePhase1.gradeList()` |

| 5 | 최종 등급 배정 | `gradePhase1.finalize()` |

### ⚙️ 성적 구성 설정

---

**설정 항목:**

## 🎮 실행 방법

| 항목 | 설명 | 기본값 |

|------|------|--------|### 1. 전체 테스트 (추천)

| `attendanceMaxScore` | 출석 만점 | 80점 |

| `latePenaltyPerSession` | 지각 감점/회 | 0.5점 |```javascript

| `gradeDistribution` | 등급 분포 (A/B/C/D) | 30/40/20/10% |gradePhase1.setLecture('ETH201')

| `passingThreshold` | 합격 기준 | 60% |await gradePhase1.runAll()

```

**💡 참고**: 과제 총점은 과제 생성 시 자동 누적 (설정 불필요)

**예상 소요 시간:** 10-15초

**방법 1: 간편 수정 (추천)**

---

```javascript

gradePhase1.quickAttendanceConfig(80, 0.5)### 2. 개별 테스트

await gradePhase1.config()

```#### 1️⃣ 성적 구성 설정



**방법 2: 대화형 입력**```javascript

gradePhase1.setLecture('ETH201')

```javascriptawait gradePhase1.config()

gradePhase1.promptConfig()// → 출석 만점, 지각 페널티, 등급 분포 설정

await gradePhase1.config()// → 과제 총점은 서버에서 자동 계산

``````



**방법 3: 직접 수정**#### 2️⃣ 학생 성적 조회



```javascript```javascript

gradePhase1.updateConfig({gradePhase1.setLecture('ETH201', 6)  // 강의코드 + 학생IDX

    attendanceMaxScore: 90,await gradePhase1.studentInfo()

    latePenaltyPerSession: 1.0,// → 출석/과제 점수, 총점, 백분율 조회

    gradeDistribution: { A: 25, B: 45, C: 20, D: 10 }```

})

await gradePhase1.config()#### 3️⃣ 교수용 성적 조회

```

```javascript

### 🎯 점수 계산 구조gradePhase1.setLecture('ETH201', 6)

await gradePhase1.professorView()

```// → 학생 성적 + 반 평균/최고점/순위 통계

1. 출석 점수 = (출석율 × 출석만점) - (지각 횟수 × 지각페널티)```

2. 과제 점수 = 과제1 + 과제2 + ... + 과제N (자동 누적)

3. 총점 = 출석 점수 + 과제 점수#### 4️⃣ 성적 목록 조회

4. 백분율 = (총점 / 총만점) × 100

``````javascript

gradePhase1.setLecture('ETH201')

------await gradePhase1.gradeList()

// → 전체 학생 목록 (페이징/정렬)

## Phase 3: 이벤트 시스템```



### 📋 테스트 목록 (2개)#### 5️⃣ 최종 등급 배정



| 번호 | 기능 | 함수 | API |```javascript

|------|------|------|-----|gradePhase1.setLecture('ETH201')

| 1 | 출석 업데이트 | `attendance()` | PUT /enrollments/{idx}/attendance |await gradePhase1.finalize()

| 2 | 과제 채점 | `assignment()` | PUT /assignments/{idx}/grade |// → 60% 합격 기준 + 상대평가 등급 배정 (A/B/C/D/F)

```

### 🎯 핵심 기능

---

**enrollmentIdx 자동 조회:**

## ⚙️ 성적 구성 설정

```javascript

// lecSerial + studentIdx만 입력### 설정 가능 항목

gradePhase3.setLecture('ETH201', 6)

| 항목 | 설명 | 기본값 |

// enrollmentIdx는 내부에서 자동 조회|------|------|--------|

await gradePhase3.attendance()| `attendanceMaxScore` | 출석 만점 | 80점 |

```| `latePenaltyPerSession` | 지각 감점/회 | 0.5점 |

| `gradeDistribution` | 등급 분포 (A/B/C/D) | 30/40/20/10% |

**수강생 목록 조회:**| `passingThreshold` | 합격 기준 | 60% |



```javascript**💡 참고**: 과제 총점은 과제 생성 시 자동 누적 (설정 불필요)

gradePhase3.setLecture('ETH201')

await gradePhase3.listStudents()### 방법 1: 간편 수정 (추천)



// 출력 예시:```javascript

// 1. [IDX: 6] 홍길동 (2024001)gradePhase1.quickAttendanceConfig(80, 0.5)

//    학과: 컴퓨터공학과 | 상태: ENROLLED//                                 ^^  ^^^

```//                          출석 만점  지각 감점/회

await gradePhase1.config()

**이벤트 기반 재계산:**```



```### 방법 2: 대화형 입력

출석/과제 업데이트 → GradeUpdateEvent 발행 → 성적 자동 재계산

``````javascript

gradePhase1.promptConfig()   // 프롬프트로 입력

------await gradePhase1.config()

```

## 공통 기능

### 방법 3: 객체로 직접 수정

### 설정 함수

```javascript

**`setLecture(lecSerial, studentIdx)`**gradePhase1.updateConfig({

    attendanceMaxScore: 90,

```javascript    latePenaltyPerSession: 1.0,

// 강의만 설정    gradeDistribution: { A: 25, B: 45, C: 20, D: 10 }

gradePhase1.setLecture('ETH201')})

await gradePhase1.config()

// 강의 + 학생 설정```

gradePhase3.setLecture('ETH201', 6)

```### 현재 설정 조회



**`getConfig()`**```javascript

gradePhase1.getConfig()

```javascript// → { attendanceMaxScore: 80, latePenaltyPerSession: 0.5, ... }

const config = gradePhase1.getConfig()```

console.log(config)

```---



### 전체 실행## 🎯 점수 계산 구조



**Phase 1:**### 자동 계산 흐름



```javascript```

gradePhase1.setLecture('ETH201')1. 출석 점수

await gradePhase1.runAll()   = (출석율 × 출석만점) - (지각 횟수 × 지각페널티)

// → 5개 테스트 순차 실행   = (77/80 × 80) - (3 × 0.5) = 77점 - 1.5점 = 75.5점

```

2. 과제 점수

**Phase 3:**   = 과제1 점수 + 과제2 점수 + ... + 과제N 점수

   = 45 + 28 + 18 = 91점

```javascript

gradePhase3.setLecture('ETH201', 6)3. 총점

await gradePhase3.runAll()   = 출석 점수 + 과제 점수

// → 2개 테스트 순차 실행   = 75.5 + 91 = 166.5점

```

4. 백분율

------   = (총점 / 총만점) × 100

   = (166.5 / (80 + 100)) × 100 = 92.5%

## 📚 관련 문서```



- [빠른 시작](./05-QUICK-START.md) - 5분 퀵 스타트### 과제 자동 누적 예시

- [테스트 가이드](./07-TESTING-GUIDE.md) - 브라우저 테스트

- [문제 해결](./09-TROUBLESHOOTING.md) - 오류 해결```javascript

// 과제 생성 시 개별 만점 설정
과제1: 50점 만점
과제2: 30점 만점
과제3: 20점 만점

// 서버에서 자동 계산
과제 총만점 = 50 + 30 + 20 = 100점

// 새 과제 추가 시
과제4: 25점 만점 추가
→ 과제 총만점 = 100 + 25 = 125점 (자동 갱신)
```

---

## 🎯 등급 배정 로직

### 60% 기준 + 상대평가

```
1단계: 합격/불합격 분류
  - 60% 이상 → 합격 (상대평가 대상)
  - 60% 미만 → F등급 확정

2단계: 등급 배정 (합격자만)
  - 기본 비율: A 30%, B 40%, C 20%, D 10%
  - 성적순 정렬 → 상위부터 배정
  - 동점자는 모두 상위 등급

3단계: 하위 침범 방식
  - 남은 학생 → 다음 등급으로 이동
```

**예시:**
```
100명 중 75명 낙제 (F)
→ 합격 25명 전원 A등급
(A 30명, B 40명 배정 불가 → 모두 최상위)
```

---

## 📊 실전 사용 예시

### 시나리오: 지각 많아서 페널티 강화

```javascript
// 1. 강의 설정
gradePhase1.setLecture('ETH201')

// 2. 지각 페널티 강화 (0.5 → 1.0점)
gradePhase1.quickAttendanceConfig(80, 1.0)
await gradePhase1.config()

// 3. 등급 재배정
await gradePhase1.finalize()

// 4. 결과 확인
await gradePhase1.gradeList()
```

---

## 📚 다음 단계

- [전체 테스트 가이드](./07-TESTING-GUIDE.md)
- [문제 해결](./09-TROUBLESHOOTING.md)

- [전체 테스트 가이드](./07-TESTING-GUIDE.md) - Phase 4 완전 가이드
- [모듈 참조](./08-MODULE-REFERENCE.md) - 각 모듈 상세 설명
- [문제 해결](./09-TROUBLESHOOTING.md) - 오류 해결 방법
