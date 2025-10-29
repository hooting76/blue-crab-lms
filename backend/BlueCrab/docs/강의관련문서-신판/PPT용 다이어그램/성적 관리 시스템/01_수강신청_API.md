# 01. 수강신청 API

> 학생이 강의를 수강 신청하는 API

---

## 📌 기본 정보

- **엔드포인트**: `POST /api/enrollments/register`
- **권한**: 학생
- **설명**: 학생이 특정 강의에 수강 신청

---

## 📥 Request

```json
{
  "lecSerial": "ETH201",
  "studentIdx": 33
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| lecSerial | String | ✅ | 강의 코드 |
| studentIdx | Integer | ✅ | 학생 ID (JWT에서 자동 추출 가능) |

---

## 📤 Response

### 성공 (200)

```json
{
  "success": true,
  "message": "수강신청이 완료되었습니다.",
  "data": {
    "enrollmentIdx": 1,
    "lecIdx": 42,
    "studentIdx": 33,
    "enrollmentData": "{}"
  }
}
```

### 실패 (400)

```json
{
  "success": false,
  "message": "이미 수강신청한 강의입니다."
}
```

---

## 🗄️ DB 변화

### ENROLLMENT_EXTENDED_TBL

**새 레코드 생성**:
- `ENROLLMENT_IDX`: 자동 생성
- `LEC_IDX`: 강의 ID
- `STUDENT_IDX`: 학생 ID
- `ENROLLMENT_DATA`: `{}` (초기값)

---

## 🔄 시퀀스 다이어그램

```
학생 → API: 수강신청 요청
API → DB: ENROLLMENT_EXTENDED_TBL INSERT
DB → API: enrollmentIdx 반환
API → 학생: 수강신청 완료
```

---

## 💡 주요 로직

1. 중복 수강 체크 (같은 학생, 같은 강의)
2. 정원 초과 체크 (`LEC_TBL.LEC_CURRENT < LEC_MANY`)
3. `ENROLLMENT_DATA` 초기화 (`{}`)

---

## 📋 다음 단계

수강신청 후 교수가 **성적 구성 설정**을 해야 출석/과제 관리가 가능합니다.

→ [02. 성적 구성 설정 API](./02_성적구성설정_API.md)
