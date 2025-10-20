# 📊 성적 관리 시스템

Task G (성적 관리 시스템) 구현 문서 허브

---

## 📁 문서 구조

```
성적관리/
├── 00-README.md                    # 이 문서
├── 01-QUICK-START.md               # 빌드 & 테스트
├── 02-IMPLEMENTATION-GUIDE.md      # 구현 가이드
├── 03-WORK-PROGRESS.md             # 작업 진행상황
├── 04-TEST-REPORT.md               # 테스트 완료보고
├── 05-LATE-PENALTY.md              # 지각 처리
├── 06-SYSTEM-DESIGN.drawio         # 시스템 설계도
└── 06-SYSTEM-DESIGN.drawio.png     # 설계도 이미지
```

---

## 🚀 빠른 시작

```powershell
# 1. 빌드
cd f:\main_project\team_work\blue-crab-lms\backend\BlueCrab
mvn clean package -DskipTests

# 2. 실행
mvn spring-boot:run

# 3. 테스트 (브라우저 콘솔)
await gradeTests.runAll()
```

→ [01-QUICK-START.md](./01-QUICK-START.md)

---

## 📚 문서 가이드

### 처음 시작
→ [01-QUICK-START.md](./01-QUICK-START.md)
빌드, 실행, 테스트 방법

### 구현 상세
→ [02-IMPLEMENTATION-GUIDE.md](./02-IMPLEMENTATION-GUIDE.md)
API 설계, 데이터 구조, 백엔드 구현

### 작업 진행
→ [03-WORK-PROGRESS.md](./03-WORK-PROGRESS.md)
Phase 1~4 진행상황 (85% 완료)

### 테스트 현황
→ [04-TEST-REPORT.md](./04-TEST-REPORT.md)
v3.0 테스트 코드 업데이트 내역

### 지각 처리
→ [05-LATE-PENALTY.md](./05-LATE-PENALTY.md)
지각 시스템 설명 (출석율 vs 감점)

---

## 📊 시스템 개요

### Phase 1: 핵심 메서드 (5개)
1. 성적 구성 설정 - `POST /api/enrollments/grade-config`
2. 학생 성적 조회 - `POST /api/enrollments/grade-info` (action: get-grade)
3. 교수용 성적 조회 - `POST /api/enrollments/grade-info` (action: professor-view)
4. 성적 목록 조회 - `POST /api/enrollments/grade-list` (action: list-all)
5. 최종 등급 배정 - `POST /api/enrollments/grade-finalize` (action: finalize)

**강의 식별**: 모든 API에서 `lecIdx` (정수) 또는 `lecSerial` (문자열) 사용 가능  
→ lecSerial 사용 시 백엔드가 LEC_TBL 조회하여 자동 변환

### Phase 3: 이벤트 시스템 (2개)
6. 출석 업데이트 - `PUT /api/enrollments/{enrollmentIdx}/attendance`
7. 과제 채점 - `PUT /api/assignments/{assignmentIdx}/grade`

---

## 🗃️ 데이터 구조

### ENROLLMENT_DATA (JSON)

```json
{
  "grade": {
    "attendance": {
      "maxScore": 20.0,
      "currentScore": 18.0,
      "percentage": 90.0,
      "latePenalty": 1.5
    },
    "assignments": [...],
    "total": {
      "totalScore": 91.0,
      "maxScore": 100.0,
      "percentage": 91.0
    },
    "letterGrade": "A"
  }
}
```

---

## 🔗 의존성

```
AttendanceService ─┐
                   ├─→ GradeCalculationService
AssignmentService ─┘         ↓
                    GradeManagementService
                             ↓
                    EnrollmentController
```

---

## ✅ 진행 상황

| Phase | 작업 수 | 완료 | 진행률 |
|-------|---------|------|--------|
| Phase 1 | 4 | 4 | 100% ✅ |
| 리팩토링 | 1 | 1 | 100% ✅ |
| Phase 2 | 4 | 4 | 100% ✅ |
| Phase 3 | 3 | 3 | 100% ✅ |
| Phase 4 | 3 | 0 | 0% ⏳ |
| **전체** | **20** | **17** | **85%** |

---

## 📞 관련 문서

- [브라우저 콘솔 테스트](../브라우저콘솔테스트/04-grade/00-README.md)
- [강의관련기능 README](../README.md)
