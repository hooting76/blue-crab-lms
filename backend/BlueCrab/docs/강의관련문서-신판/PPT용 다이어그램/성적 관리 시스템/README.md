# 성적 관리 시스템 API 명세서

> PPT 다이어그램 제작을 위한 핵심 API 문서

## 📋 목차

1. [**수강신청 API**](./01_수강신청_API.md) - 강의 등록
2. [**성적 구성 설정 API**](./02_성적구성설정_API.md) - 출석/과제 배점 설정
3. [**출석 관리 API**](./03_출석관리_API.md) - 출석 요청/승인
4. [**과제 관리 API**](./04_과제관리_API.md) - 과제 생성/채점
5. [**성적 조회 API**](./05_성적조회_API.md) - 학생/교수 성적 확인
6. [**최종 등급 배정 API**](./06_최종등급배정_API.md) - 상대평가 등급 부여

---

## 🔄 전체 프로세스 흐름

```
학기 시작
    ↓
① 수강신청 (학생)
    ↓
② 성적 구성 설정 (교수)
    ↓
③ 출석 관리 (학생 요청 → 교수 승인)
    ↓
④ 과제 관리 (교수 출제 → 학생 제출 → 교수 채점)
    ↓
⑤ 성적 조회 (학생/교수)
    ↓
⑥ 최종 등급 배정 (교수)
    ↓
학기 종료
```

---

## 📊 핵심 데이터베이스

### ENROLLMENT_EXTENDED_TBL

수강신청 정보 및 성적 데이터를 저장하는 핵심 테이블

**주요 컬럼**:
- `ENROLLMENT_IDX`: 수강신청 ID (PK)
- `LEC_IDX`: 강의 ID (FK)
- `STUDENT_IDX`: 학생 ID (FK)
- `ENROLLMENT_DATA`: 성적 데이터 (JSON)

**ENROLLMENT_DATA 구조**:
```json
{
  "gradeConfig": { /* 성적 구성 설정 */ },
  "attendance": { /* 출석 데이터 */ },
  "grade": { /* 계산된 성적 */ }
}
```

---

## 🎯 주요 특징

### 1. JSON 기반 데이터 관리
- 출석, 과제, 성적 데이터를 `ENROLLMENT_DATA` JSON 컬럼에 저장
- 유연한 데이터 구조 확장 가능

### 2. 자동 성적 계산
- 출석/과제 데이터 입력 시 자동 재계산
- `GradeUpdateEvent` 이벤트 기반 처리

### 3. 과제 총점 자동 합산
- `ASSIGNMENT_EXTENDED_TBL`에서 실제 과제 배점 조회
- `assignmentTotalScore` 자동 계산

### 4. 상대평가 지원
- 백분율 기준 정렬
- 등급 분포(A/B/C/D %) 적용

---

## 🔗 관련 테이블

| 테이블 | 설명 |
|--------|------|
| `ENROLLMENT_EXTENDED_TBL` | 수강신청 및 성적 데이터 |
| `LEC_TBL` | 강의 정보 |
| `USER_TBL` | 사용자 정보 (학생/교수) |
| `ASSIGNMENT_EXTENDED_TBL` | 과제 정보 |
| `ATTENDANCE_REQUEST_TBL` | 출석 요청 이력 |

---

## 📌 API 엔드포인트 요약

| 기능 | 엔드포인트 | 메서드 | 권한 |
|------|------------|--------|------|
| 수강신청 | `/api/enrollments/enroll` | POST | 학생 |
| 성적 구성 설정 | `/api/enrollments/grade-config` | POST | 교수 |
| 출석 요청 | `/api/attendance/request` | POST | 학생 |
| 출석 승인 | `/api/attendance/approve` | POST | 교수 |
| 과제 생성 | `/api/assignments` | POST | 교수 |
| 과제 채점 | `/api/assignments/{id}/grade` | PUT | 교수 |
| 성적 조회 (학생) | `/api/enrollments/grade-info` | POST | 학생 |
| 성적 조회 (교수) | `/api/enrollments/grade-list` | POST | 교수 |
| 최종 등급 배정 | `/api/enrollments/grade-finalize` | POST | 교수 |

---

## 💡 사용 예시

### 학생 관점

```javascript
// 1. 수강신청
POST /api/enrollments/enroll

// 2. 출석 요청
POST /api/attendance/request

// 3. 내 성적 조회
POST /api/enrollments/grade-info
```

### 교수 관점

```javascript
// 1. 성적 구성 설정
POST /api/enrollments/grade-config

// 2. 출석 승인
POST /api/attendance/approve

// 3. 과제 생성
POST /api/assignments

// 4. 과제 채점
PUT /api/assignments/{id}/grade

// 5. 전체 성적 조회
POST /api/enrollments/grade-list

// 6. 최종 등급 배정
POST /api/enrollments/grade-finalize
```

---

© 2025 BlueCrab LMS - Grade Management System
