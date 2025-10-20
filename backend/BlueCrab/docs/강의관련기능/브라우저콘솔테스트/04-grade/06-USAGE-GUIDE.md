# 📚 사용법 상세 가이드

성적 관리 시스템의 모든 기능을 자세히 설명합니다.

---

## 📖 목차

- [테스트 목록](#-테스트-목록)
- [실행 방법](#-실행-방법)
- [테스트 데이터](#-테스트-데이터)
- [이벤트 시스템](#-이벤트-시스템)

---

## 📋 테스트 목록

### Phase 1: 핵심 기능 (5개)

| 번호 | 기능 | API 엔드포인트 | 함수 |
|------|------|----------------|------|
| 1 | 성적 구성 설정 | `POST /api/enrollments/grade-config` | `gradeTests.config()` |
| 2 | 학생 성적 조회 | `GET /api/enrollments/{lecIdx}/{studentIdx}/grade` | `gradeTests.studentInfo()` |
| 3 | 교수용 성적 조회 | `GET /api/enrollments/professor/grade` | `gradeTests.professorView()` |
| 4 | 성적 목록 조회 | `GET /api/enrollments/grade-list` | `gradeTests.gradeList()` |
| 5 | 최종 등급 배정 | `POST /api/enrollments/finalize-grades` | `gradeTests.finalize()` |

### Phase 3: 이벤트 시스템 (2개)

| 번호 | 기능 | API 엔드포인트 | 함수 | 이벤트 |
|------|------|----------------|------|--------|
| 6 | 출석 업데이트 | `PUT /api/enrollments/{enrollmentIdx}/attendance` | `gradeTests.attendance()` | ATTENDANCE |
| 7 | 과제 채점 | `PUT /api/assignments/{assignmentIdx}/grade` | `gradeTests.assignment()` | ASSIGNMENT |

---

## 🎮 실행 방법

### 1. 전체 테스트 (추천)

```javascript
await gradeTests.runAll()
```

**실행 순서:**
1. Phase 1 테스트 5개 (순차 실행)
2. 3초 대기
3. Phase 3 테스트 2개 (이벤트 포함)

**예상 소요 시간:** 15-20초

---

### 2. Phase별 실행

```javascript
// Phase 1만 (5개)
await gradeTests.phase1()

// Phase 3만 (2개)
await gradeTests.phase3()
```

---

### 3. 개별 테스트

#### Phase 1 테스트

```javascript
// 1. 성적 구성 설정
await gradeTests.config()
// 출석 만점, 과제 만점, 지각 페널티, 등급 분포 설정

// 2. 학생 성적 조회
await gradeTests.studentInfo()
// 특정 학생의 출석/과제/총점 조회

// 3. 교수용 성적 조회
await gradeTests.professorView()
// 전체 학생 성적 + 통계 (평균, 최고점, 최저점)

// 4. 성적 목록 조회
await gradeTests.gradeList()
// 페이징, 정렬 지원

// 5. 최종 등급 배정
await gradeTests.finalize()
// 60% 기준 + 상대평가 (A+~F)
```

#### Phase 3 테스트

```javascript
// 6. 출석 업데이트
await gradeTests.attendance()
// 출석/지각/결석 기록 → 이벤트 발행

// 7. 과제 채점
await gradeTests.assignment()
// 과제 점수 입력 → 이벤트 발행
```

---

### 4. 시나리오 테스트

```javascript
await gradeTests.scenario()
```

**6단계 워크플로우:**
1. 성적 구성 설정
2. 출석 기록 (with 이벤트)
3. 과제 채점 (with 이벤트)
4. 학생 성적 확인
5. 전체 성적 목록 조회
6. 최종 등급 배정

---

### 5. 커스텀 데이터 테스트

```javascript
// 현재 테스트 데이터 확인
gradeTests.getData()

// 데이터 수정
gradeTests.setData({
    lecIdx: 2,
    studentIdx: 101,
    attendanceMaxScore: 100
})

// 커스텀 데이터로 테스트
await gradeTests.customTest()
```

---

## ⚙️ 테스트 데이터

### 기본 데이터 구조

```javascript
{
    lecIdx: 1,                    // 강의 ID
    studentIdx: 100,              // 학생 ID
    professorIdx: 22,             // 교수 ID
    enrollmentIdx: 1,             // 수강 ID
    assignmentIdx: 1,             // 과제 ID
    passingThreshold: 60.0,       // 통과 기준 (60%)
    attendanceMaxScore: 80,       // 출석 만점
    assignmentTotalMaxScore: 100, // 과제 만점
    latePenaltyPerSession: 0.5,   // 지각 페널티 (회당)
    gradeDistribution: {          // 등급 분포
        "A+": 10,
        "A": 15,
        "B+": 15,
        "B": 25,
        "C": 25,
        "D": 10
    }
}
```

### 데이터 조회/수정

```javascript
// 조회
const data = gradeTests.getData()
console.log(data)

// 수정
gradeTests.setData({
    lecIdx: 2,              // 강의 변경
    studentIdx: 101,        // 학생 변경
    attendanceMaxScore: 100 // 출석 만점 변경
})
```

---

## 🔥 이벤트 시스템

### 이벤트 발행 메커니즘

#### 1. 출석 업데이트 이벤트

```javascript
await gradeTests.attendance()
```

**플로우:**
1. API 호출: `PUT /api/enrollments/{enrollmentIdx}/attendance`
2. DB 업데이트: 출석 데이터 저장
3. 이벤트 발행: `GradeUpdateEvent(ATTENDANCE)`
4. 리스너 실행: 성적 자동 재계산 (비동기)

#### 2. 과제 채점 이벤트

```javascript
await gradeTests.assignment()
```

**플로우:**
1. API 호출: `PUT /api/assignments/{assignmentIdx}/grade`
2. DB 업데이트: 과제 점수 저장
3. 이벤트 발행: `GradeUpdateEvent(ASSIGNMENT)`
4. 리스너 실행: 성적 자동 재계산 (비동기)

### Eclipse 로그 확인

**출석 이벤트 로그:**
```
[GradeUpdateEventListener] 성적 업데이트 이벤트 수신
- 타입: ATTENDANCE
- 수강 ID: 1
- 상세 정보: Attendance updated
```

**과제 이벤트 로그:**
```
[GradeUpdateEventListener] 성적 업데이트 이벤트 수신
- 타입: ASSIGNMENT
- 수강 ID: 1
- 상세 정보: Assignment graded
```

**로그 검색:**
1. Eclipse Console 클릭
2. `Ctrl + F` → 검색: `GradeUpdateEventListener`
3. 총 **2개** 로그 확인

---

## 📊 예상 출력

### 전체 테스트 실행 시

```
🎯 전체 테스트 실행 시작...
══════════════════════════════════════

📊 Phase 1: 핵심 기능 테스트 (5개)
──────────────────────────────────────

🧪 테스트 1/5: 성적 구성 설정
POST https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/enrollments/grade-config
✅ HTTP 200 - 성공
📝 메시지: 성적 구성이 성공적으로 저장되었습니다.

🧪 테스트 2/5: 학생 성적 조회
GET https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/enrollments/1/100/grade
✅ HTTP 200 - 성공
📊 성적 상세 정보:
  📅 출석: 18.5 / 80 (23.1%)
  📝 과제: 85.0 / 100 (85.0%)
  📈 총점: 103.5 (51.8%)

...

⏳ Phase 1 완료. 3초 후 Phase 3 시작...

📊 Phase 3: 이벤트 시스템 테스트 (2개)
──────────────────────────────────────

🧪 테스트 1/2: 출석 업데이트 (이벤트 발행)
PUT https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/enrollments/1/attendance
✅ HTTP 200 - 성공
🔔 이벤트 발행됨: ATTENDANCE

...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎉 전체 테스트 완료!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 최종 통계:
   총 테스트: 7개
   성공: 7개
   실패: 0개
   성공률: 100%
```

---

## 🎯 등급 배정 로직

### 60% 기준 + 상대평가

```
1단계: 합격/불합격 분류
  - 60% 이상 → 합격 (상대평가 대상)
  - 60% 미만 → F등급 확정

2단계: 등급 배정 (합격자만)
  - 기본 비율: A+ 10%, A 15%, B+ 15%, B 25%, C 25%, D 10%
  - 성적순 정렬 → 상위부터 배정
  - 동점자는 모두 상위 등급

3단계: 하위 침범 방식
  - 남은 학생 → 다음 등급으로 이동
  - 예: A+ 10% 넘으면 → A 등급으로
```

**예시:**
```
100명 중 75명 낙제 (F)
→ 합격 25명 전원 A등급
(A+ 10명, A 15명 배정 불가 → 모두 최상위)
```

---

## 📚 다음 단계

- [전체 테스트 가이드](./07-TESTING-GUIDE.md) - Phase 4 완전 가이드
- [모듈 참조](./08-MODULE-REFERENCE.md) - 각 모듈 상세 설명
- [문제 해결](./09-TROUBLESHOOTING.md) - 오류 해결 방법
