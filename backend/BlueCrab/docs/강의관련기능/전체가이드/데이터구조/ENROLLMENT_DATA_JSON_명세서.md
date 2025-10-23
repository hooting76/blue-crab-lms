# ENROLLMENT_EXTENDED_TBL.ENROLLMENT_DATA JSON 구조 명세서

## 📋 개요

`ENROLLMENT_EXTENDED_TBL` 테이블의 `ENROLLMENT_DATA` 컬럼은 **LONGTEXT** 타입으로 수강신청, 출결, 성적 관련 모든 정보를 JSON 형식으로 저장합니다.

---

## 🏗️ 전체 JSON 구조

```json
{
  "version": "2.0",
  "enrollment": { ... },
  "attendance": { ... },
  "gradeConfig": { ... },
  "grade": { ... }
}
```

---

## 📊 1. enrollment (수강신청 정보)

수강신청 상태 및 이력을 관리합니다.

```json
{
  "enrollment": {
    "status": "ENROLLED",                    // 수강 상태
    "enrollmentDate": "2025-03-01T09:00:00", // 수강신청일
    "cancelDate": null,                      // 취소일 (nullable)
    "cancelReason": null                     // 취소 사유 (nullable)
  }
}
```

### 📝 필드 설명

| 필드명 | 타입 | 설명 | 가능한 값 |
|--------|------|------|-----------|
| `status` | String | 수강 상태 | `ENROLLED`, `CANCELLED`, `COMPLETED` |
| `enrollmentDate` | String (ISO DateTime) | 수강신청일시 | `2025-03-01T09:00:00` |
| `cancelDate` | String (ISO DateTime) | 수강 취소일시 | `null` 또는 날짜 |
| `cancelReason` | String | 수강 취소 사유 | `null` 또는 텍스트 |

---

## 📅 2. attendance (출결 정보)

출석 요청/승인 시스템의 핵심 데이터 구조입니다.

### 2.1 전체 구조

```json
{
  "attendance": {
    "summary": { ... },        // 출석 통계 요약
    "sessions": [ ... ],       // 확정된 출석 기록 (최대 80개)
    "pendingRequests": [ ... ] // 대기 중인 출석 요청 (최대 80개)
  }
}
```

### 2.2 summary (출석 통계)

```json
{
  "summary": {
    "attended": 30,                         // 출석 횟수
    "late": 5,                             // 지각 횟수
    "absent": 5,                           // 결석 횟수
    "totalSessions": 40,                   // 총 진행된 회차
    "attendanceRate": 75.0,                // 출석율 (%)
    "updatedAt": "2025-10-20 15:51:09"     // 마지막 업데이트
  }
}
```

**📝 필드 설명:**

| 필드명 | 타입 | 설명 | 계산 방식 |
|--------|------|------|-----------|
| `attended` | Integer | 출석 횟수 | sessions에서 `status: "출"` 개수 |
| `late` | Integer | 지각 횟수 | sessions에서 `status: "지"` 개수 |
| `absent` | Integer | 결석 횟수 | sessions에서 `status: "결"` 개수 |
| `totalSessions` | Integer | 총 진행 회차 | sessions 배열 길이 |
| `attendanceRate` | Float | 출석율 | `(attended / totalSessions) * 100` |
| `updatedAt` | String | 마지막 업데이트 | 자동 생성 |

### 2.3 sessions (확정된 출석 기록)

학생의 출석 요청이 교수에 의해 승인되거나 자동 승인된 기록들입니다.

```json
{
  "sessions": [
    {
      "sessionNumber": 1,                     // 강의 회차 (1~80)
      "status": "출",                         // 출석 상태
      "requestDate": "2025-10-22T10:00:00",   // 요청일시
      "approvedDate": "2025-10-22T10:30:00",  // 승인일시
      "approvedBy": 123,                      // 승인자 ID (교수)
      "tempApproved": false                   // 임시 승인 여부
    },
    {
      "sessionNumber": 2,
      "status": "지",
      "requestDate": "2025-10-22T14:00:00",
      "approvedDate": "2025-10-22T14:15:00",
      "approvedBy": 123,
      "tempApproved": false
    }
  ]
}
```

**📝 필드 설명:**

| 필드명 | 타입 | 설명 | 가능한 값 |
|--------|------|------|-----------|
| `sessionNumber` | Integer | 강의 회차 번호 | 1 ~ 80 |
| `status` | String | 출석 상태 | `"출"`, `"지"`, `"결"` |
| `requestDate` | String (ISO DateTime) | 학생 요청일시 | `2025-10-22T10:00:00` |
| `approvedDate` | String (ISO DateTime) | 교수 승인일시 | `2025-10-22T10:30:00` |
| `approvedBy` | Integer | 승인자 ID | 교수의 USER_IDX |
| `tempApproved` | Boolean | 임시 승인 여부 | `false` (확정 상태) |

### 2.4 pendingRequests (대기 중인 출석 요청)

학생이 출석 요청했지만 아직 교수 승인이 없는 임시 승인 상태의 요청들입니다.

```json
{
  "pendingRequests": [
    {
      "sessionNumber": 3,                     // 강의 회차 (1~80)
      "requestDate": "2025-10-22T16:00:00",   // 요청일시
      "expiresAt": "2025-10-29T16:00:00",     // 만료일시 (7일 후)
      "tempApproved": true                    // 임시 승인 상태
    }
  ]
}
```

**📝 필드 설명:**

| 필드명 | 타입 | 설명 | 계산 방식 |
|--------|------|------|-----------|
| `sessionNumber` | Integer | 강의 회차 번호 | 1 ~ 80 |
| `requestDate` | String (ISO DateTime) | 학생 요청일시 | 요청 시점 |
| `expiresAt` | String (ISO DateTime) | 자동 승인 만료일 | `requestDate + 7일` |
| `tempApproved` | Boolean | 임시 승인 여부 | `true` (대기 상태) |

---

## 🎯 3. gradeConfig (성적 설정)

교수가 설정한 강의별 성적 구성 및 정책입니다.

```json
{
  "gradeConfig": {
    "attendanceMaxScore": 20,                // 출석 만점
    "assignmentTotalScore": 50,              // 과제 총점
    "examTotalScore": 30,                    // 시험 총점
    "latePenaltyPerSession": 0.5,            // 지각 감점 (회당)
    "gradeDistribution": {                   // 등급 분포 (%)
      "A": 30,
      "B": 40,
      "C": 20,
      "D": 10
    },
    "totalMaxScore": 100,                    // 총 만점
    "configuredAt": "2025-10-22T10:00:00"   // 설정일시
  }
}
```

---

## 📊 4. grade (성적 정보)

계산된 성적 및 평가 결과입니다.

### 4.1 전체 구조

```json
{
  "grade": {
    "attendance": { ... },    // 출석 점수
    "assignments": [ ... ],   // 과제 점수들
    "total": { ... },         // 총점
    "letterGrade": "A",       // 최종 등급
    "finalizedAt": "..."      // 확정일시
  }
}
```

### 4.2 출석 점수 (grade.attendance)

```json
{
  "attendance": {
    "maxScore": 20,           // 만점
    "currentScore": 18.5,     // 현재 점수
    "rate": 92.5,            // 출석율 (%)
    "lateCount": 2,          // 지각 횟수
    "latePenalty": 1.0,      // 지각 감점
    "percentage": 92.5       // 백분율
  }
}
```

### 4.3 과제 점수 (grade.assignments)

```json
{
  "assignments": [
    {
      "name": "과제1",         // 과제명
      "score": 9,             // 획득 점수
      "maxScore": 10,         // 만점
      "submitted": true,      // 제출 여부
      "percentage": 90.0      // 백분율
    },
    {
      "name": "중간고사",
      "score": 85,
      "maxScore": 100,
      "submitted": true,
      "percentage": 85.0
    }
  ]
}
```

### 4.4 총점 (grade.total)

```json
{
  "total": {
    "score": 204.5,          // 총 획득 점수
    "maxScore": 230,         // 총 만점
    "percentage": 88.9       // 총 백분율
  }
}
```

---

## 📏 용량 분석

### 📊 예상 크기 (바이트)

| 섹션 | 일반적 크기 | 최대 크기 | 설명 |
|------|-------------|-----------|------|
| **enrollment** | 120 | 150 | 수강신청 기본 정보 |
| **attendance.summary** | 180 | 200 | 출석 통계 |
| **attendance.sessions** | 6,000 | 12,000 | 40개 → 80개 |
| **attendance.pendingRequests** | 600 | 9,600 | 5개 → 80개 |
| **gradeConfig** | 250 | 300 | 성적 설정 |
| **grade** | 800 | 1,200 | 성적 정보 |
| **JSON 오버헤드** | 500 | 750 | 구조, 쉼표 등 |

### 📈 총 용량

```
일반적인 경우: 약 8.5 KB
최악의 경우: 약 24 KB
LONGTEXT 최대: 4 GB

여유 공간: 약 170,000배
```

---

## 🔄 데이터 플로우

### 1️⃣ 학생 출석 요청
```
1. 학생 출석 요청
2. pendingRequests에 추가
3. tempApproved: true (임시 승인)
4. expiresAt: requestDate + 7일
```

### 2️⃣ 교수 승인/거부
```
1. 교수 승인 처리
2. pendingRequests에서 제거
3. sessions에 추가 (status: 출/지/결)
4. summary 자동 재계산
```

### 3️⃣ 자동 승인 (스케줄러)
```
1. 매일 오전 5시 실행
2. expiresAt 만료 확인
3. pendingRequests → sessions 이동
4. status: "출" (자동 출석)
```

### 4️⃣ 성적 연동
```
1. attendance.summary 업데이트
2. grade.attendance 자동 계산
3. grade.total 재계산
4. 이벤트 기반 업데이트
```

---

## ⚠️ 제약사항 및 규칙

### 📏 크기 제한
- `sessions`: 최대 80개 (강의 총 회차)
- `pendingRequests`: 최대 80개 (강의 총 회차)
- 한 학생당 각 회차별 1번씩만 요청 가능

### 🔄 업데이트 규칙
- **원자성**: JSON 전체를 교체 (부분 업데이트 불가)
- **트랜잭션**: 동시 수정 방지
- **이벤트**: 출석 변경 시 성적 자동 재계산

### 📅 시간 규칙
- `expiresAt`: 요청일로부터 정확히 7일 후
- `스케줄러`: 매일 오전 5시 (Asia/Seoul)
- `만료 처리`: 서버 시간 기준

---

## 🛠️ 구현 고려사항

### 성능 최적화
```sql
-- JSON 인덱스 생성 (선택사항)
ALTER TABLE ENROLLMENT_EXTENDED_TBL 
ADD INDEX idx_attendance_rate (
  (CAST(JSON_EXTRACT(ENROLLMENT_DATA, '$.attendance.summary.attendanceRate') AS DECIMAL(5,2)))
);
```

### 버전 관리
```json
{
  "version": "2.0",  // 스키마 버전 관리
  // ... 나머지 데이터
}
```

### 캐싱 전략
- Redis 캐싱 고려 (자주 조회되는 데이터)
- summary 정보 별도 캐싱
- 실시간 업데이트 vs 배치 업데이트

---

## 📖 관련 문서

- [출석 요청/승인 플로우](../../출결관리/출석요청승인_플로우.md)
- [성적 관리 시스템](../../성적관리/성적관리_시스템_문서.md)
- [브라우저 콘솔 테스트](../../브라우저콘솔테스트/06-attendance/README.md)
- [작업 순서 가이드](../../출결관리/작업%20수순/01_작업_순서_전체.md)

---

## 📞 개발팀 연락처

JSON 구조 변경이나 문의사항이 있으면 백엔드 개발팀에 연락하세요.