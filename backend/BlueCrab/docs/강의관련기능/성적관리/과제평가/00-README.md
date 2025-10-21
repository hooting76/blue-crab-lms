# 📝 과제 평가 시스템

> **소속**: 성적 관리 시스템의 하위 기능  
> **목적**: 오프라인 제출 과제에 대한 평가 결과 기록  
> **방식**: 교수 재량 평가 → 결과 입력

---

## 📋 개요

### 시스템 특징

본 시스템은 **온라인 과제 제출 기능이 없는** 평가 기록 시스템입니다.

- **제출 방식**: 서면, 이메일, 손수 전달 등 오프라인 제출
- **평가 주체**: 교수 재량
- **시스템 역할**: 평가 결과 기록 및 성적 반영만 수행

### 과제 제출 API (더미)

```javascript
⚠️ POST /api/assignments/{assignmentIdx}/submit
→ 더미 API (실제 사용 안 함)
→ 학생이 직접 제출하지 않음
```

### 실제 사용 API

```javascript
✅ POST /api/assignments/grade
→ 교수가 평가 결과 입력
→ 제출 방법, 점수 기록
```

---

## 🎯 과제 평가 흐름

```
학생
  ↓ (오프라인 제출)
  📧 이메일 전송
  📄 서면 제출
  🤝 손수 전달
  ↓
교수
  ↓ (평가)
  📊 채점
  💬 피드백 작성
  ↓
시스템
  ↓ (기록)
  POST /api/assignments/grade
  ↓
성적 관리 시스템
  ↓ (자동 반영)
  과제 점수 → 총점 계산
```

---

## 📚 문서 구조

- **[00-README.md](./00-README.md)**: 이 문서 (개요)
- **[01-API-SPECIFICATION.md](./01-API-SPECIFICATION.md)**: API 상세 명세
- **[02-SUBMISSION-METHODS.md](./02-SUBMISSION-METHODS.md)**: 제출 방법 코드표

---

## 🔧 주요 API

### 1. 과제 평가 입력

**엔드포인트**: `POST /api/assignments/grade`

**요청 예시**:
```json
{
  "assignmentIdx": 1,
  "studentCode": "STU001",
  "submissionMethod": "email",
  "score": 9,
  "action": "grade"
}
```

**제출 방법 코드**:
- `email`: 이메일 제출
- `print`: 서면(출력물) 제출
- `hands`: 손수 전달
- `absent`: 미제출

---

## 📊 성적 반영

### 자동 계산

과제 점수는 **성적 관리 시스템**으로 자동 전달되어 총점에 반영됩니다.

```
과제 평가 입력
  ↓
EnrollmentExtendedTbl 업데이트
  ↓
이벤트 발생 (AssignmentGradedEvent)
  ↓
GradeCalculationService
  ↓
자동 재계산 (출석 + 과제 + 시험)
```

### 점수 구조

```json
{
  "attendance": {
    "total": 20,
    "current": 18
  },
  "assignments": {
    "total": 30,
    "assignments": [
      {
        "assignmentIdx": 1,
        "title": "과제 1",
        "maxScore": 10,
        "score": 9,
        "submissionMethod": "email"
      }
    ]
  },
  "exams": {
    "midterm": 25,
    "final": 25
  }
}
```

---

## ⚠️ 주의사항

### 1. 더미 API

**학생 과제 제출 API**는 더미입니다:
```javascript
❌ POST /api/assignments/{assignmentIdx}/submit
→ 사용하지 않음
→ 테스트 코드에만 존재
```

### 2. 제출 방법

모든 제출은 **오프라인**으로 이루어집니다:
- 이메일 제출 → 교수가 확인 후 `email` 코드로 입력
- 서면 제출 → 교수가 확인 후 `print` 코드로 입력
- 손수 전달 → 교수가 확인 후 `hands` 코드로 입력

### 3. 점수 범위

- **기본**: 0~10점 (10점 만점)
- **변경 불가**: maxScore는 항상 10점으로 고정

---

## 🧪 테스트 코드

### 위치
```
브라우저콘솔테스트/03-professor/
└── lecture-test-4b-professor-assignment-grade.js
```

### 주요 함수

```javascript
// 과제 채점
await gradeAssignment();

// 제출 현황 조회
await getAssignmentSubmissions();
```

### 실행 방법

1. 교수 계정 로그인
2. 콘솔에서 스크립트 로드
3. `await gradeAssignment()` 실행

---

## 🔗 연관 문서

### 성적 관리 시스템
- **상위 폴더**: `성적관리/`
- **자동 재계산**: 과제 점수 반영 시 전체 성적 자동 업데이트

### 전체 가이드
- **Phase3**: `전체가이드/Phase3_Semester_Progress.md`
- **과제 관리**: 섹션 4~7 참조

---

> **📌 핵심**: 이 시스템은 **평가 결과 기록 시스템**이며, 온라인 제출 기능은 제공하지 않습니다.
