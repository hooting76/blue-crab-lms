# 📊 성적 관리 시스템

성적 계산, 등급 배정, 이벤트 기반 자동 재계산

---

## 📁 문서 구조

```
성적관리/
├── 00-README.md                 # 이 문서
├── 01-QUICK-START.md            # 빠른 시작
├── 02-IMPLEMENTATION-GUIDE.md   # 구현 가이드
└── 03-WORK-PROGRESS.md          # 작업 진행
```

---

## 🎯 시스템 구성

### Phase 1: 핵심 API (5개) ✅
1. **성적 구성 설정** - `POST /enrollments/grade-config`
2. **학생 성적 조회** - `POST /enrollments/grade-info`
3. **교수 성적 조회** - `POST /enrollments/grade-info`
4. **성적 목록 조회** - `POST /enrollments/grade-list`
5. **최종 등급 배정** - `POST /enrollments/grade-finalize`

### Phase 3: 이벤트 시스템 (2개) ✅
6. **출석 업데이트** - `PUT /enrollments/{enrollmentIdx}/attendance`
7. **과제 채점** - `PUT /assignments/{assignmentIdx}/grade`

### 강의 식별 방식
- **lecSerial + studentIdx** 통일
- **enrollmentIdx** 자동 조회

---

## 🚀 빠른 시작

### 1. 서버 실행
```powershell
cd f:\main_project\team_work\blue-crab-lms\backend\BlueCrab
mvn spring-boot:run
```

### 2. Phase 1 테스트 (5개 API)
```javascript
// 01-grade-phase1-tests.js 로드
gradePhase1.setLecture('ETH201', 6)
await gradePhase1.runAll()  // 10-15초
```

### 3. Phase 3 테스트 (2개 API)
```javascript
// 02-grade-phase3-tests.js 로드
await gradePhase3.listStudents()
gradePhase3.setLecture('ETH201', 6, 1)
await gradePhase3.runAll()  // 5-10초
```

---

## 🗃️ 데이터 구조

### ENROLLMENT_DATA (JSON)
```json
{
  "gradeConfig": {
    "attendanceMaxScore": 80,
    "assignmentTotalMaxScore": 100,
    "latePenaltyPerSession": 0.5,
    "gradeDistribution": {"A+": 10, "A": 15, "B+": 20}
  },
  "grade": {
    "attendance": {
      "maxScore": 20.0,
      "currentScore": 18.0,
      "percentage": 90.00,
      "latePenalty": 1.5
    },
    "assignments": [
      {"name": "과제1", "score": 9.0, "maxScore": 10.0}
    ],
    "total": {
      "totalScore": 91.0,
      "maxScore": 100.0,
      "percentage": 91.00
    },
    "finalGrade": "A+"
  }
}
```

---

## 🔄 이벤트 시스템

```
출석/과제 변경
    ↓
GradeUpdateEvent 발행
    ↓
EventListener 비동기 처리
    ↓
성적 자동 재계산
    ↓
ENROLLMENT_DATA 업데이트
```

---

## 📋 핵심 알고리즘

### 출석 점수 계산
```
출석율 = (출석 + 지각) / 총 회차
출석율 기반 점수 = 출석율 × 만점
지각 감점 = 지각 횟수 × latePenaltyPerSession
최종 점수 = 출석율 기반 점수 - 지각 감점
```

### 등급 배정 (하위 침범 방식)
```
1. 60% 미만 → F등급 확정
2. 기본 비율 계산
3. 낙제자가 하위 등급 자리 차지
4. 합격자를 남은 상위 등급에 배정
5. 동점자는 모두 상위 등급
```

---

## 📚 문서 목록

| 문서 | 설명 |
|------|------|
| [00-README.md](./00-README.md) | 📌 시스템 개요 |
| [01-QUICK-START.md](./01-QUICK-START.md) | 🚀 빠른 시작 |
| [02-IMPLEMENTATION-GUIDE.md](./02-IMPLEMENTATION-GUIDE.md) | 🔧 구현 가이드 |
| [03-WORK-PROGRESS.md](./03-WORK-PROGRESS.md) | 📊 작업 진행 |

---

## 🎓 주요 특징

- ✅ 자동 성적 재계산 (이벤트 기반)
- ✅ enrollmentIdx 자동 조회
- ✅ 등급 배정 알고리즘 (하위 침범 방식)
- ✅ JWT 토큰 기반 인증
- ✅ 출석율 정밀 계산 (소수점 둘째자리)
- ✅ 지각 페널티 시스템

---

## 📊 진행 상황

| Phase | 완료 | 진행률 |
|-------|------|--------|
| Phase 1 (5개 API) | ✅ | 100% |
| Phase 2 (DB 연동) | ✅ | 100% |
| Phase 3 (이벤트) | ✅ | 100% |
| Phase 4 (테스트) | ⏳ | 0% |
| **전체** | **11/14** | **79%** |

---

## 💡 관련 문서

- [브라우저 콘솔 테스트](../브라우저콘솔테스트/04-grade/)
- [테스트 가이드](../브라우저콘솔테스트/04-grade/05-QUICK-START.md)
- [API 레퍼런스](../브라우저콘솔테스트/04-grade/08-MODULE-REFERENCE.md)

---

> **현재 상태**: Phase 1~3 완료, Phase 4 테스트 대기 중
