# 🧪 통합 테스트

성적 자동 재계산 기능의 E2E(End-to-End) 통합 테스트입니다.

## 📁 파일 목록

### `attendance-grade-integration-test.js`

**출석-성적 통합 테스트**

테스트 시나리오:

1. 학생 초기 성적 확인
2. 학생이 출석 요청 생성
3. 교수가 출석 승인
4. **성적 자동 재계산 확인** ← @Transactional 검증

검증 항목:

- ✅ 출석 승인 시 `afterCommit` 콜백 실행
- ✅ 성적 데이터 자동 업데이트
- ✅ `attendance.currentScore` 증가 확인

### `assignment-grade-integration-test.js`

**과제-성적 통합 테스트**

테스트 시나리오:

1. 학생 초기 성적 확인
2. 교수가 과제 생성 (오프라인 제출용)
3. (학생이 오프라인으로 과제 제출 - 시스템 외부)
4. 교수가 과제 채점 (점수 + 피드백)
5. **성적 자동 재계산 확인** ← @Transactional 검증

검증 항목:

- ✅ 과제 채점 시 이벤트 발행
- ✅ 성적 데이터 자동 업데이트
- ✅ `assignments` 배열 업데이트 확인
- ✅ `total.score` 증가 확인

## 🚀 사용 방법

### 출석-성적 통합 테스트

```javascript
// 1. 스크립트 로드
// attendance-grade-integration-test.js 전체 복사/붙여넣기

// 2. 로그인
await loginStudent()     // 학생 계정
await loginProfessor()   // 교수 계정

// 3. 통합 테스트 실행
await runFullIntegrationTest()
```

### 과제-성적 통합 테스트

```javascript
// 1. 스크립트 로드
// assignment-grade-integration-test.js 전체 복사/붙여넣기

// 2. 로그인
await loginProfessor()   // 교수 계정만 필요

// 3. 통합 테스트 실행
await runAssignmentGradeTest()
```

## 🎯 테스트 목적

### @Transactional 수정 검증

출석 승인 및 과제 채점 시 성적 자동 재계산이 정상 작동하는지 확인:

1. **출석 승인 → 성적 재계산**
   - `AttendanceRequestServiceImpl.approveAttendance()`
   - `TransactionSynchronizationManager.afterCommit()` 콜백
   - `GradeCalculationService.calculateStudentGrade()` 호출

2. **과제 채점 → 성적 재계산**
   - `AssignmentController.gradeAssignment()`
   - `GradeUpdateEvent` 발행
   - `GradeCalculationService.calculateStudentGrade()` 호출

### 백엔드 로그 확인

테스트 실행 시 백엔드 로그에서 다음 메시지 확인:

```
성적 재계산 시작: lecIdx=48, studentIdx=6
출석 점수 계산: currentScore=9.63, percentage=12.51%
성적 재계산 완료: lecIdx=48, studentIdx=6
```

또는

```
과제 채점으로 인한 성적 재계산 이벤트 발행: lecIdx=48, studentIdx=6
성적 재계산 시작: lecIdx=48, studentIdx=6
과제 점수 계산: 1주차 과제 - 8.0/10.0점
성적 재계산 완료: lecIdx=48, studentIdx=6
```

## ⚠️ 주의사항

### 오프라인 과제 제출 방식

- **과제 제출**: 오프라인으로 진행 (시스템 외부)
- **시스템 역할**:
  - 교수가 과제 생성
  - 교수가 과제 채점 (점수 + 피드백)
  - 자동으로 성적에 반영

### 테스트 데이터 준비

- 테스트용 강의 존재 (예: ETH201)
- 테스트용 학생 존재 (예: studentIdx=6)
- 테스트용 교수 존재 (예: prof.octopus@univ.edu)

### 성적 구조

```json
{
  "grade": {
    "attendance": {
      "currentScore": 9.63,
      "percentage": 12.51,
      "presentCount": 10,
      "maxScore": 77
    },
    "assignments": [
      {
        "name": "1주차 과제",
        "score": 8,
        "maxScore": 10,
        "percentage": 16.0,
        "submitted": true
      }
    ],
    "total": {
      "score": 17.63,
      "maxScore": 87,
      "percentage": 20.26
    }
  }
}
```

## 📊 예상 결과

### 출석 테스트 성공

```
✅ 통합 테스트 성공!

출석 승인 시 성적 자동 재계산이 정상 작동합니다.
@Transactional 수정이 효과적으로 적용되었습니다.

출석 점수: 8.66 → 9.63 (+0.97)
총점: 8.66 → 9.63 (+0.97)
```

### 과제 테스트 성공

```
✅ 통합 테스트 성공!

과제 채점 시 성적 자동 재계산이 정상 작동합니다.
@Transactional 수정이 효과적으로 적용되었습니다.

과제 개수: 0 → 1
새로 채점된 과제: 통합 테스트용 과제 (8/10점)
총점: 9.63 → 17.63 (+8.0)
```

## 🔗 관련 문서

- [출석 테스트](../06-attendance/)
- [교수 과제 테스트](../03-professor/)
- [성적 테스트](../04-grade/)
- [상위 README](../README.md)

## 💡 개별 테스트와의 차이

**개별 테스트**: 각 기능을 단계별로 수동 테스트

- `06-attendance/` - 출석 요청, 승인, 조회
- `03-professor/` - 과제 생성, 채점
- `04-grade/` - 성적 조회, 계산

**통합 테스트** (이 폴더): 전체 플로우 자동 실행

- 시간 절약 (한 번에 전체 테스트)
- E2E 검증 (API → DB → 성적 반영)
- 회귀 테스트 (기능 수정 후 빠른 검증)
