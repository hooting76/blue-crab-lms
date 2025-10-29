# 02. 성적 구성 설정 API

> 교수가 출석/과제 배점을 설정하는 API

---

## 📌 기본 정보

- **엔드포인트**: `POST /api/enrollments/grade-config`
- **권한**: 교수
- **설명**: 출석 만점, 지각 감점, 등급 분포를 설정하여 전체 수강생의 성적 구조 초기화
- **참고**: 
  - 과제 총점은 백엔드가 ASSIGNMENT_EXTENDED_TBL에서 자동 계산
  - 등급 분포는 최종 등급 배정 시 사용됨

---

## 📥 Request

```json
{
  "action": "set-config",
  "lecSerial": "ETH201",
  "attendanceMaxScore": 20,
  "latePenaltyPerSession": 0.3,
  "gradeDistribution": {
    "A": 30,
    "B": 40,
    "C": 20,
    "D": 10
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| action | String | ✅ | 반드시 `"set-config"` |
| lecSerial | String | ✅ | 강의 코드 |
| attendanceMaxScore | Integer | ✅ | 출석 만점 (예: 20점) |
| latePenaltyPerSession | Double | ✅ | 지각 감점/회 (예: 0.3점) |
| gradeDistribution | Object | ✅ | 등급 분포 비율 (A+B+C+D=100%) |
| gradeDistribution.A | Integer | ✅ | A 등급 비율 (%) |
| gradeDistribution.B | Integer | ✅ | B 등급 비율 (%) |
| gradeDistribution.C | Integer | ✅ | C 등급 비율 (%) |
| gradeDistribution.D | Integer | ✅ | D 등급 비율 (%) |

**⚠️ 중요**: 
- `assignmentTotalScore`는 **전송하지 않습니다** (백엔드가 ASSIGNMENT_EXTENDED_TBL에서 자동 계산)
- `gradeDistribution` 합계는 반드시 100% (F는 60% 미만 절대평가로 자동 배정)

---

## 📤 Response

```json
{
  "success": true,
  "message": "성적 구성이 설정되었습니다.",
  "data": {
    "lecIdx": 42,
    "updatedCount": 42,
    "gradeConfig": {
      "attendanceMaxScore": 20,
      "assignmentTotalScore": 80,
      "totalMaxScore": 100,
      "latePenaltyPerSession": 0.3,
      "gradeDistribution": {
        "A": 30,
        "B": 40,
        "C": 20,
        "D": 10
      }
    }
  }
}
```

**주요 필드 설명**:
- `updatedCount`: 설정이 적용된 수강생 수
- `assignmentTotalScore`: 백엔드가 자동 계산한 과제 총점 (ASSIGNMENT_EXTENDED_TBL 합산)
- `totalMaxScore`: 출석 만점 + 과제 총점
- `latePenaltyPerSession`: 지각 1회당 감점
- `gradeDistribution`: 상대평가 등급 분포 (60% 이상 학생 대상)

---

## 🗄️ DB 변화

### ENROLLMENT_EXTENDED_TBL

**모든 수강생의 `ENROLLMENT_DATA` 업데이트**:

```json
{
  "gradeConfig": {
    "attendanceMaxScore": 20,
    "assignmentTotalScore": 80,
    "totalMaxScore": 100,
    "latePenaltyPerSession": 0.3,
    "gradeDistribution": {
      "A": 30,
      "B": 40,
      "C": 20,
      "D": 10
    }
  },
  "attendance": {
    "summary": { 
      "presentCount": 0, 
      "lateCount": 0, 
      "absentCount": 0, 
      "pendingCount": 0 
    },
    "sessions": []
  },
  "grade": {
    "attendanceScore": {
      "currentScore": 0,
      "latePenalty": 0,
      "presentCount": 0,
      "lateCount": 0,
      "absentCount": 0
    },
    "assignments": [],
    "total": { 
      "currentScore": 0, 
      "percentage": 0 
    },
    "letterGrade": null
  }
}
```

**초기화 내용**:
- `gradeConfig`: 성적 구성 정보 저장 (등급 분포 포함)
- `attendance`: 출석 데이터 초기화
- `grade`: 성적 계산 구조 초기화

---

## 🔄 시퀀스 다이어그램

```plaintext
교수 → API: 출석/과제 배점 설정
API → DB: LEC_TBL에서 해당 강의의 모든 수강생 조회
API → DB: 각 수강생의 ENROLLMENT_DATA 업데이트
DB → API: 업데이트 완료 (42건)
API → 교수: 성공 메시지
```

---

## 💡 주요 로직

### 1. 과제 총점 자동 계산

백엔드가 ASSIGNMENT_EXTENDED_TBL에서 해당 강의의 모든 과제를 조회하여 자동 합산:

```sql
-- ASSIGNMENT_EXTENDED_TBL의 ASSIGNMENT_DATA JSON에서 maxScore 추출 후 합산
SELECT ASSIGNMENT_DATA FROM ASSIGNMENT_EXTENDED_TBL WHERE LEC_IDX = 42
-- JSON에서 assignment.maxScore를 추출하여 SUM 계산
```

**⚠️ 중요**: 
- 클라이언트는 `assignmentTotalScore`를 보내지 않습니다
- 백엔드가 실제 과제 테이블에서 자동으로 계산합니다
- 과제 생성/삭제 시 자동으로 재계산됩니다

### 2. 총 만점 계산

```javascript
totalMaxScore = attendanceMaxScore + assignmentTotalScore(자동 계산)
// 예: 20 + 80 = 100점
```

### 3. 등급 분포 저장

```javascript
gradeDistribution = { A: 30, B: 40, C: 20, D: 10 }
// 합계 = 100% (F는 60% 미만 절대평가로 자동 배정)
```

### 4. 일괄 업데이트

해당 강의의 **모든 수강생** ENROLLMENT_DATA를 동일하게 초기화:
- 성적 구성 정보 저장
- 출석/성적 구조 초기화
- 등급 분포 비율 저장

---

## ⚠️ 주의사항

### 과제 총점 자동 계산
- **assignmentTotalScore는 Request에 포함하지 않습니다**
- 백엔드가 ASSIGNMENT_EXTENDED_TBL의 실제 과제 데이터(JSON)에서 maxScore를 자동 합산
- 과제가 없는 경우 0으로 계산됨

### 등급 분포 설정
- **A + B + C + D = 100%** (합계 검증 필수)
- **F는 포함하지 않습니다** (60% 미만 학생에게 자동 배정)
- 이 비율은 **60% 이상 학생들에게만** 적용됩니다

### 지각 감점
- `latePenaltyPerSession`: 지각 1회당 감점 (기본: 0.3점)
- 출석 점수 계산 시 자동 적용: `출석점수 - (지각횟수 × 감점)`

### 재설정 시 주의
- 이미 설정된 경우 **기존 데이터 덮어쓰기**
- 출석/과제 데이터는 유지되지만 gradeConfig는 새 값으로 대체
- 성적 재계산이 자동으로 실행됨

### 실행 타이밍
- **학기 초 1회만 실행** 권장
- 과제 생성 **이전**에 실행해도 무방 (과제 총점은 나중에 자동 업데이트)
- 수강신청 완료 후 전체 수강생 확정 시점에 실행

---

## 📋 다음 단계

성적 구성 설정 완료 후:

1. 교수가 **출석 관리** 시작 가능
2. 교수가 **과제 생성** 시작 가능

→ [03. 출석 관리 API](./03_출석관리_API.md)  
→ [04. 과제 관리 API](./04_과제관리_API.md)
