# 하루 전체 시간대 조회 API 가이드

## 📋 개요
- **API 명**: 하루 전체 시간대별 예약 현황 조회
- **엔드포인트**: `POST /api/facilities/{facilityIdx}/daily-schedule`
- **작성일**: 2025-10-13
- **목적**: 프론트엔드가 한 번의 API 호출로 하루 전체의 예약 상태를 확인

---

## 🎯 기능 설명

### 기존 방식의 문제점
프론트엔드에서 하루 전체 시간대를 표시하려면:
```
09:00 [예약 가능]
10:00 [예약됨]
11:00 [승인 대기]
12:00 [예약 가능]
...
20:00 [예약 가능]
```

**기존 방식**: `/availability` API를 12번 호출 (09:00~20:00)
- 비효율적
- 네트워크 트래픽 증가
- 로딩 시간 증가

**새로운 방식**: `/daily-schedule` API를 1번 호출
- ✅ 효율적
- ✅ 빠른 응답
- ✅ 일관된 데이터

---

## 📡 API 명세

### Request

**Method**: `POST`  
**URL**: `/api/facilities/{facilityIdx}/daily-schedule`  
**Headers**:
```
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
```

**Path Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| facilityIdx | Integer | ✅ | 시설 ID |

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 형식 | 설명 |
|---------|------|------|------|------|
| date | String | ✅ | yyyy-MM-dd | 조회할 날짜 |

**예시**:
```
POST /api/facilities/1/daily-schedule?date=2025-10-15
```

---

### Response

**Success (200 OK)**:
```json
{
  "success": true,
  "message": "하루 일정을 조회했습니다.",
  "data": {
    "facilityIdx": 1,
    "facilityName": "다목적 회의실 1호",
    "date": "2025-10-15",
    "slots": [
      {
        "time": "09:00",
        "startTime": "2025-10-15 09:00:00",
        "endTime": "2025-10-15 10:00:00",
        "status": "available",
        "statusMessage": "예약 가능"
      },
      {
        "time": "10:00",
        "startTime": "2025-10-15 10:00:00",
        "endTime": "2025-10-15 11:00:00",
        "status": "available",
        "statusMessage": "예약 가능"
      },
      {
        "time": "11:00",
        "startTime": "2025-10-15 11:00:00",
        "endTime": "2025-10-15 12:00:00",
        "status": "reserved",
        "statusMessage": "예약됨"
      },
      {
        "time": "12:00",
        "startTime": "2025-10-15 12:00:00",
        "endTime": "2025-10-15 13:00:00",
        "status": "available",
        "statusMessage": "예약 가능"
      },
      {
        "time": "13:00",
        "startTime": "2025-10-15 13:00:00",
        "endTime": "2025-10-15 14:00:00",
        "status": "pending",
        "statusMessage": "승인 대기"
      },
      {
        "time": "14:00",
        "startTime": "2025-10-15 14:00:00",
        "endTime": "2025-10-15 15:00:00",
        "status": "reserved",
        "statusMessage": "예약됨"
      },
      {
        "time": "15:00",
        "startTime": "2025-10-15 15:00:00",
        "endTime": "2025-10-15 16:00:00",
        "status": "blocked",
        "statusMessage": "정기 점검으로 사용 불가"
      },
      {
        "time": "16:00",
        "startTime": "2025-10-15 16:00:00",
        "endTime": "2025-10-15 17:00:00",
        "status": "available",
        "statusMessage": "예약 가능"
      },
      {
        "time": "17:00",
        "startTime": "2025-10-15 17:00:00",
        "endTime": "2025-10-15 18:00:00",
        "status": "available",
        "statusMessage": "예약 가능"
      },
      {
        "time": "18:00",
        "startTime": "2025-10-15 18:00:00",
        "endTime": "2025-10-15 19:00:00",
        "status": "available",
        "statusMessage": "예약 가능"
      },
      {
        "time": "19:00",
        "startTime": "2025-10-15 19:00:00",
        "endTime": "2025-10-15 20:00:00",
        "status": "available",
        "statusMessage": "예약 가능"
      },
      {
        "time": "20:00",
        "startTime": "2025-10-15 20:00:00",
        "endTime": "2025-10-15 21:00:00",
        "status": "available",
        "statusMessage": "예약 가능"
      }
    ]
  }
}
```

---

## 🎨 Status 종류

| Status | 설명 | UI 색상 추천 |
|--------|------|--------------|
| `available` | 예약 가능 | 🟢 Green |
| `reserved` | 승인 완료된 예약 존재 | 🔴 Red |
| `pending` | 승인 대기 중인 예약 존재 | 🟡 Yellow |
| `blocked` | 관리자가 차단한 시간 | ⚫ Gray |

---

## 💻 프론트엔드 사용 예시

### React Example
```tsx
const [dailySchedule, setDailySchedule] = useState(null);
const [selectedDate, setSelectedDate] = useState('2025-10-15');

const fetchDailySchedule = async (facilityIdx, date) => {
  try {
    const response = await fetch(
      `/api/facilities/${facilityIdx}/daily-schedule?date=${date}`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      }
    );
    
    const result = await response.json();
    if (result.success) {
      setDailySchedule(result.data);
    }
  } catch (error) {
    console.error('Failed to fetch schedule:', error);
  }
};

// UI 렌더링
return (
  <div>
    <h2>{dailySchedule?.facilityName}</h2>
    <h3>{dailySchedule?.date}</h3>
    
    <div className="time-slots">
      {dailySchedule?.slots.map(slot => (
        <div 
          key={slot.time}
          className={`slot ${slot.status}`}
        >
          <div className="time">{slot.time}</div>
          <div className="status">{slot.statusMessage}</div>
        </div>
      ))}
    </div>
  </div>
);
```

### CSS Example
```css
.slot {
  padding: 12px;
  margin: 8px 0;
  border-radius: 8px;
  border: 2px solid;
}

.slot.available {
  background: #f0fdf4;
  border-color: #86efac;
  color: #15803d;
}

.slot.reserved {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #991b1b;
}

.slot.pending {
  background: #fefce8;
  border-color: #fde047;
  color: #854d0e;
}

.slot.blocked {
  background: #f3f4f6;
  border-color: #d1d5db;
  color: #6b7280;
  opacity: 0.6;
}
```

---

## 🔍 로직 설명

### 1. 시간대 생성
```java
// 09:00부터 20:00까지 1시간 단위
for (int hour = 9; hour <= 20; hour++) {
    LocalTime slotTime = LocalTime.of(hour, 0);
    LocalDateTime slotStart = date.atTime(slotTime);
    LocalDateTime slotEnd = slotStart.plusHours(1);
    // ...
}
```

### 2. 상태 결정 우선순위
1. **차단 기간 체크** (최우선)
   - 관리자가 설정한 차단 기간에 포함되면 `blocked`

2. **예약 충돌 체크**
   - 승인된 예약(APPROVED)과 겹치면 `reserved`
   - 승인 대기(PENDING)와 겹치면 `pending`

3. **기본값**
   - 위 조건에 해당하지 않으면 `available`

### 3. 시간 겹침 판정
```java
boolean overlaps = slotStart.isBefore(reservation.getEndTime()) && 
                  slotEnd.isAfter(reservation.getStartTime());
```

**예시**:
```
슬롯:    [09:00 -------- 10:00)
예약:      [09:30 ---------- 11:00)
→ 겹침! (09:30~10:00 구간)

슬롯:    [09:00 -------- 10:00)
예약:                      [10:00 -------- 11:00)
→ 안 겹침! (경계는 제외)
```

---

## 🚀 성능 최적화

### 데이터베이스 쿼리 최적화
```java
// 하루 전체 예약을 한 번에 조회
List<FacilityReservationTbl> dayReservations = 
    reservationRepository.findConflictingReservations(
        facilityIdx, dayStart, dayEnd, activeStatuses, null);

// 하루 전체 차단 정보를 한 번에 조회
List<FacilityBlockTbl> dayBlocks = 
    blockRepository.findConflictingBlocks(
        facilityIdx, dayStart, dayEnd);
```

**효과**:
- 기존: 시간대당 2개 쿼리 × 12 = 24개 쿼리
- 개선: 총 2개 쿼리 (12배 감소!)

---

## 📊 테스트 시나리오

### 시나리오 1: 정상 조회
```bash
curl -X POST "http://localhost:8080/api/facilities/1/daily-schedule?date=2025-10-15" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**예상 결과**: 09:00~20:00 시간대별 상태 반환

---

### 시나리오 2: 차단된 시간대 포함
```sql
-- 테스트 데이터 삽입
INSERT INTO FACILITY_BLOCK_TBL 
(FACILITY_IDX, BLOCK_START, BLOCK_END, BLOCK_REASON, CREATED_AT, UPDATED_AT)
VALUES 
(1, '2025-10-15 15:00:00', '2025-10-15 17:00:00', '정기 점검', NOW(), NOW());
```

**예상 결과**: 15:00, 16:00 시간대가 `blocked` 상태

---

### 시나리오 3: 예약과 승인 대기 혼재
```sql
-- 승인된 예약
INSERT INTO FACILITY_RESERVATION_TBL
(FACILITY_IDX, USER_CODE, START_TIME, END_TIME, STATUS, ...)
VALUES (1, 'user1', '2025-10-15 11:00:00', '2025-10-15 12:00:00', 'APPROVED', ...);

-- 승인 대기 예약
INSERT INTO FACILITY_RESERVATION_TBL
(FACILITY_IDX, USER_CODE, START_TIME, END_TIME, STATUS, ...)
VALUES (1, 'user2', '2025-10-15 13:00:00', '2025-10-15 14:00:00', 'PENDING', ...);
```

**예상 결과**: 
- 11:00 → `reserved`
- 13:00 → `pending`

---

## ⚠️ 주의사항

### 1. 시간대 범위
현재는 **09:00~20:00** 고정
- 필요시 시설별로 운영 시간 설정 기능 추가 가능

### 2. 개인정보 보호
- 예약자 정보는 포함되지 않음
- 단순히 "예약됨" 상태만 표시
- 관리자 API에서만 예약자 정보 확인 가능

### 3. 연속 예약 처리
```
예약: 09:00~12:00 (3시간)

결과:
09:00 → reserved
10:00 → reserved  
11:00 → reserved
```

모든 겹치는 시간대가 동일한 상태로 표시됨

---

## 🔗 관련 API

| API | 용도 | 비교 |
|-----|------|------|
| `POST /api/facilities/{id}/availability` | 특정 시간대 예약 가능 여부 | 예약 신청 전 검증 |
| `POST /api/facilities/{id}/daily-schedule` | 하루 전체 스케줄 조회 | UI 표시용 |

**사용 시나리오**:
1. 프론트엔드에서 `/daily-schedule`로 하루 전체 표시
2. 사용자가 특정 시간 선택
3. 예약 신청 전 `/availability`로 재확인
4. 예약 API 호출

---

## 📝 향후 개선 사항

### 1. 시설별 운영 시간 설정
```java
// FacilityPolicyTbl에 추가
private LocalTime operatingStart;  // 09:00
private LocalTime operatingEnd;    // 22:00
```

### 2. 30분 단위 지원
```java
// 현재: 1시간 단위 (09:00, 10:00, 11:00)
// 개선: 30분 단위 (09:00, 09:30, 10:00, 10:30)
```

### 3. 예약자 수 표시 (관리자용)
```json
{
  "time": "09:00",
  "status": "reserved",
  "reservationCount": 3  // 이 시간대 예약 수
}
```

---

**작성일**: 2025-10-13  
**작성자**: BlueCrab Development Team  
**버전**: 1.0.0
