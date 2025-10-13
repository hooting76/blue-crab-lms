# 📋 시설 가용성 확인 API 설명

## 🎯 API 개요

**엔드포인트**: `POST /api/facilities/{facilityIdx}/availability`

**목적**: 특정 시설이 원하는 시간대에 예약 가능한지 확인

---

## 📝 무엇을 확인하나?

이 API는 다음 3가지를 체크합니다:

### 1️⃣ 시설 차단 여부 확인
```
관리자가 설정한 차단 기간인가?
└─ facility_block_tbl 테이블 확인
   └─ 예: 시설 공사, 점검 등으로 사용 불가 기간
```

### 2️⃣ 기존 예약과의 충돌 확인
```
해당 시간대에 이미 예약이 있나?
└─ facility_reservation_tbl 테이블 확인
   └─ 상태: PENDING(승인 대기) 또는 APPROVED(승인됨)
```

### 3️⃣ 가용성 판단
```
차단 없음 + 예약 충돌 없음 = 예약 가능! ✅
차단 있음 OR 예약 충돌 있음 = 예약 불가! ❌
```

---

## 🔍 동작 과정

```
사용자 요청
    ↓
┌─────────────────────────────────┐
│ 1. 시설 존재 여부 확인           │
│    - facilityIdx로 시설 조회     │
│    - 없으면 404 에러            │
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│ 2. 시설 차단 여부 확인           │
│    - facility_block_tbl 조회    │
│    - 차단 기간과 겹치는가?       │
│    - 있으면 에러 발생 ❌         │
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│ 3. 예약 충돌 확인                │
│    - facility_reservation_tbl   │
│    - PENDING/APPROVED 상태만     │
│    - 시간 겹침 확인              │
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│ 4. 결과 반환                     │
│    - isAvailable: true/false    │
│    - conflictingReservations    │
└─────────────────────────────────┘
```

---

## 📥 요청 예시

### 파라미터
- **facilityIdx** (Path): 시설 ID (예: 1)
- **startTime** (Query): 시작 시간 (ISO 8601 형식)
- **endTime** (Query): 종료 시간 (ISO 8601 형식)

### API 호출 예시
```http
POST /api/facilities/1/availability?startTime=2025-01-01T10:00:00&endTime=2025-01-01T12:00:00
Authorization: Bearer {token}
```

### 이미지에서 본 예시
```
facilityIdx: 1
startTime: 2025-01-01T10:00:00
endTime: 2025-01-01T12:00:00
```

---

## 📤 응답 예시

### ✅ 예약 가능한 경우
```json
{
  "status": "success",
  "message": "가용성을 확인했습니다.",
  "data": {
    "facilityIdx": 1,
    "facilityName": "세미나실 A",
    "isAvailable": true,
    "conflictingReservations": []
  }
}
```

### ❌ 예약 불가능한 경우 (기존 예약과 충돌)
```json
{
  "status": "success",
  "message": "가용성을 확인했습니다.",
  "data": {
    "facilityIdx": 1,
    "facilityName": "세미나실 A",
    "isAvailable": false,
    "conflictingReservations": [
      {
        "startTime": "2025-01-01T10:30:00",
        "endTime": "2025-01-01T11:30:00"
      }
    ]
  }
}
```

### ❌ 시설 차단된 경우 (에러)
```json
{
  "status": "error",
  "message": "해당 시설은 2025-01-01T00:00:00부터 2025-01-03T23:59:59까지 예약이 불가합니다. 사유: 시설 보수 공사"
}
```

---

## 🎯 사용 시나리오

### 시나리오 1: 예약 전 가용성 확인
```
학생이 세미나실 예약 페이지 접속
    ↓
원하는 날짜와 시간 선택
    ↓
"예약 가능 여부 확인" 버튼 클릭
    ↓
이 API 호출 → 결과 표시
    ↓
가능하면 "예약하기" 버튼 활성화
```

### 시나리오 2: 실시간 가용성 표시
```
시설 예약 달력 화면
    ↓
각 시간대마다 이 API 호출
    ↓
예약 가능: 초록색 표시 ✅
예약 불가: 빨간색 표시 ❌
```

### 시나리오 3: 예약 수정 시
```
기존 예약을 다른 시간으로 변경
    ↓
새 시간대의 가용성 확인
    ↓
excludeReservationIdx 사용 (자기 예약 제외)
    ↓
가능하면 수정 허용
```

---

## 🔑 핵심 로직

### 시간 충돌 체크
```sql
-- Repository에서 실행되는 쿼리
SELECT * FROM facility_reservation_tbl
WHERE facility_idx = :facilityIdx
  AND status IN ('PENDING', 'APPROVED')
  AND NOT (end_time <= :startTime OR start_time >= :endTime)
  -- 예약 시간이 겹치는 경우를 찾음
```

### 차단 체크
```sql
-- facility_block_tbl 확인
SELECT * FROM facility_block_tbl
WHERE facility_idx = :facilityIdx
  AND NOT (block_end <= :startTime OR block_start >= :endTime)
  -- 차단 기간과 겹치는지 확인
```

---

## 💡 주의사항

### ⚠️ 가용성 확인은 "순간"의 정보
- 다른 사용자가 동시에 예약할 수 있음
- 가용성 확인 후 즉시 예약 권장
- 예약 생성 API에서 재검증 필수

### ⚠️ 시간 형식
- ISO 8601 형식 사용: `yyyy-MM-dd'T'HH:mm:ss`
- 예: `2025-01-01T10:00:00`
- 타임존: 서버 기본 타임존 사용 (Asia/Seoul)

### ⚠️ 차단 vs 예약 충돌
- **차단**: 에러 발생 (예약 시도 불가)
- **예약 충돌**: 정상 응답 + isAvailable=false

---

## 📊 데이터베이스 테이블

### facility_block_tbl (시설 차단 정보)
```
facility_idx    시설 ID
block_start     차단 시작 시간
block_end       차단 종료 시간
block_reason    차단 사유
```

### facility_reservation_tbl (예약 정보)
```
reservation_idx 예약 ID
facility_idx    시설 ID
start_time      예약 시작 시간
end_time        예약 종료 시간
status          예약 상태 (PENDING/APPROVED/REJECTED/CANCELLED)
user_code       예약자
```

---

## 🔗 관련 API

- `POST /api/facilities/{facilityIdx}/reservations` - 예약 생성 (가용성 재확인 포함)
- `GET /api/facilities/{facilityIdx}` - 시설 정보 조회
- `GET /api/facilities/{facilityIdx}/reservations` - 시설의 예약 목록

---

## 📖 참고 문서

- [facility-reservation-api-complete.md](../claudedocs/api-endpoints/facility-reservation-api-complete.md)
- [consecutive-time-only-policy.md](../claudedocs/feature-docs/facility-reservation/consecutive-time-only-policy.md)

---

**작성자**: GitHub Copilot  
**작성일**: 2025-10-12  
**카테고리**: API 설명서
