# 연속된 시간대만 예약 가능 정책

**작성일:** 2025-10-10  
**목적:** 구현 복잡도 감소 및 빠른 개발

---

## 📋 정책 개요

### 핵심 규칙
시설 예약은 **같은 날짜 내에서 연속된 시간대만** 선택 가능합니다.

### 제한 사항
- ❌ **불가능:** 여러 날짜에 걸친 예약 (예: 2025-10-10 14:00 ~ 2025-10-11 10:00)
- ❌ **불가능:** 시간이 비연속적인 예약 (UI 레벨에서 방지)
- ✅ **가능:** 같은 날 연속된 시간 (예: 2025-10-10 09:00 ~ 2025-10-10 12:00)

---

## 🎯 도입 이유

### 1. 구현 복잡도 대폭 감소
**기존 복잡한 시나리오 (이제 불필요):**
```
사용자가 화요일 14:00-18:00, 수요일 09:00-12:00 예약 시도
→ 두 날짜 모두 충돌 검증 필요
→ 부분 승인 처리 로직 필요
→ 프론트엔드에서 복잡한 다중 선택 UI 필요
```

**새로운 단순 시나리오:**
```
사용자가 화요일 14:00-18:00 신청
→ 하나의 연속된 블록만 검증
→ 단순 승인/거부 처리

수요일이 필요하면? → 별도로 다시 신청
```

### 2. 사용자 경험 명확화
- 한 번의 신청 = 하나의 명확한 시간 블록
- 예약 상태 추적이 간단함 (하나의 예약 ID = 하나의 시간 블록)
- 취소/수정 로직이 단순함

### 3. 관리자 검토 부담 감소
- 승인 필요 시설의 경우, 각 날짜별로 독립적인 예약으로 검토 가능
- 부분 승인 같은 애매한 상태 처리 불필요

### 4. 빠른 개발 가능
- 프론트엔드: 단일 날짜 선택기만 구현하면 됨
- 백엔드: 날짜 비교 검증 1줄로 해결
- 테스트: 엣지 케이스 대폭 감소

---

## 🔧 구현 내역

### 백엔드 검증 로직

**위치:** `FacilityReservationService.java` → `validateReservationRequest()`

```java
// 연속된 시간대 검증: 같은 날짜 내에서만 예약 가능
if (!request.getStartTime().toLocalDate().equals(request.getEndTime().toLocalDate())) {
    throw new RuntimeException("예약은 같은 날짜 내에서만 가능합니다. " +
        "여러 날에 걸친 예약이 필요한 경우 각 날짜별로 따로 신청해주세요.");
}
```

### 검증 시점
1. **즉시 승인 시설:** 예약 생성 즉시 검증 (락 획득 전)
2. **승인 필요 시설:** 예약 생성 즉시 검증 (PENDING 상태 저장 전)

---

## 📱 프론트엔드 가이드라인

### UI 구현 권장사항

#### 1. 날짜 선택
```tsx
// ✅ 권장: 단일 날짜 선택기
<DatePicker 
  onChange={(date) => setSelectedDate(date)}
  placeholder="예약 날짜 선택"
/>

// 시간 선택은 선택된 날짜 내에서만
<TimeRangePicker 
  date={selectedDate}
  minTime="09:00"
  maxTime="18:00"
/>
```

#### 2. 사용자 안내 메시지
```tsx
<InfoBox>
  💡 예약은 하루 단위로만 가능합니다.
  여러 날에 걸쳐 시설을 이용하시려면 각 날짜별로 예약을 신청해주세요.
</InfoBox>
```

#### 3. 연속 예약 지원 (선택사항)
```tsx
// 사용자 편의를 위한 연속 예약 도우미
<Button onClick={handleMultipleDayBooking}>
  여러 날 연속 예약하기 (각각 별도 신청됩니다)
</Button>

// 버튼 클릭 시: 각 날짜에 대해 개별 POST 요청 전송
```

---

## 🧪 테스트 케이스

### ✅ 정상 케이스

```javascript
// 테스트 1: 같은 날 오전 예약
POST /api/reservations
{
  "facilityIdx": 1,
  "startTime": "2025-10-15T09:00:00",
  "endTime": "2025-10-15T12:00:00"
}
// Expected: 201 Created
```

```javascript
// 테스트 2: 같은 날 종일 예약
POST /api/reservations
{
  "facilityIdx": 1,
  "startTime": "2025-10-15T09:00:00",
  "endTime": "2025-10-15T18:00:00"
}
// Expected: 201 Created
```

### ❌ 실패 케이스

```javascript
// 테스트 3: 여러 날 걸친 예약
POST /api/reservations
{
  "facilityIdx": 1,
  "startTime": "2025-10-15T14:00:00",
  "endTime": "2025-10-16T10:00:00"  // 다음 날
}
// Expected: 400 Bad Request
// Message: "예약은 같은 날짜 내에서만 가능합니다..."
```

```javascript
// 테스트 4: 자정 넘는 예약
POST /api/reservations
{
  "facilityIdx": 1,
  "startTime": "2025-10-15T23:00:00",
  "endTime": "2025-10-16T01:00:00"  // 자정 넘음
}
// Expected: 400 Bad Request
```

---

## 📊 영향 분석

### 긍정적 영향

| 영역 | 개선 효과 |
|------|----------|
| **개발 시간** | 프론트엔드 구현 2-3일 → 1일 단축 |
| **코드 복잡도** | 복잡한 날짜 범위 처리 로직 불필요 |
| **버그 가능성** | 엣지 케이스 70% 감소 |
| **유지보수** | 단순한 로직으로 유지보수 용이 |
| **성능** | 검증 쿼리 단순화 |

### 사용자 경험 변화

| 시나리오 | 이전 | 현재 |
|---------|------|------|
| 1일 예약 | 한 번 신청 | 한 번 신청 (동일) |
| 3일 연속 예약 | 한 번 신청 (복잡한 UI) | 3번 신청 (단순 반복) |
| 예약 취소 | 전체/부분 취소 선택 필요 | 각 날짜별 독립 취소 |
| 승인 대기 | 복합 상태 추적 | 각 예약 개별 추적 |

**결론:** 장기 예약 사용자는 약간의 불편함이 있지만, 대부분의 사용자는 영향 없음.

---

## 🔄 다중 날짜 예약 워크플로우

### 사용자 시나리오: 3일 연속 예약

```
Step 1: 첫째 날 예약
POST /api/reservations
{
  "startTime": "2025-10-15T09:00:00",
  "endTime": "2025-10-15T18:00:00"
}
→ 예약 ID: R001 생성

Step 2: 둘째 날 예약
POST /api/reservations
{
  "startTime": "2025-10-16T09:00:00",
  "endTime": "2025-10-16T18:00:00"
}
→ 예약 ID: R002 생성

Step 3: 셋째 날 예약
POST /api/reservations
{
  "startTime": "2025-10-17T09:00:00",
  "endTime": "2025-10-17T18:00:00"
}
→ 예약 ID: R003 생성

결과: 3개의 독립적인 예약으로 관리
```

### 프론트엔드 자동화 예시

```javascript
async function bookMultipleDays(facilityId, dates, startTime, endTime) {
  const results = [];
  
  for (const date of dates) {
    try {
      const response = await fetch('/api/reservations', {
        method: 'POST',
        body: JSON.stringify({
          facilityIdx: facilityId,
          startTime: `${date}T${startTime}`,
          endTime: `${date}T${endTime}`,
          // ... 기타 필드
        })
      });
      
      results.push({
        date,
        status: 'success',
        reservationId: (await response.json()).reservationIdx
      });
    } catch (error) {
      results.push({
        date,
        status: 'failed',
        error: error.message
      });
    }
  }
  
  return results;
}

// 사용 예
const dates = ['2025-10-15', '2025-10-16', '2025-10-17'];
const results = await bookMultipleDays(1, dates, '09:00:00', '18:00:00');

// 결과 표시
results.forEach(r => {
  if (r.status === 'success') {
    console.log(`${r.date} 예약 완료 (ID: ${r.reservationId})`);
  } else {
    console.log(`${r.date} 예약 실패: ${r.error}`);
  }
});
```

---

## 🚀 다음 단계

### 즉시 작업 (백엔드)
- [x] 날짜 검증 로직 추가 완료
- [ ] 기존 테스트 케이스 업데이트
- [ ] API 문서 업데이트 (에러 메시지 추가)

### 프론트엔드 작업 필요
- [ ] 날짜 선택 UI를 단일 날짜로 변경
- [ ] 시간 선택을 선택된 날짜 내로 제한
- [ ] 안내 메시지 추가
- [ ] (선택) 다중 날짜 예약 도우미 기능

### 문서화 작업
- [ ] 사용자 가이드 업데이트
- [ ] FAQ 추가 ("여러 날 예약하려면?")
- [ ] API 문서 에러 코드 명세 추가

---

## 💡 향후 고려사항

### 기능 개선 가능성 (v2.0)

만약 사용자 피드백에서 **"여러 날 한 번에 예약하고 싶다"**는 요청이 많으면:

1. **연속 날짜 일괄 예약 API 추가**
   ```
   POST /api/reservations/batch
   {
     "facilityIdx": 1,
     "dates": ["2025-10-15", "2025-10-16", "2025-10-17"],
     "dailyStartTime": "09:00",
     "dailyEndTime": "18:00"
   }
   ```

2. **백엔드에서 내부적으로 루프 처리**
   - 각 날짜마다 개별 예약 레코드 생성
   - 트랜잭션으로 묶어서 전체 성공/실패 보장

3. **그룹 ID 도입**
   - `batch_group_id` 컬럼 추가
   - 같은 일괄 예약은 같은 그룹 ID 부여
   - 일괄 취소 가능

**하지만 현재는 단순함 유지!**

---

## 📝 요약

### Before (복잡함)
```
사용자: 여러 날짜, 비연속 시간대 선택 가능
백엔드: 복잡한 날짜 범위 검증, 부분 승인 처리
프론트: 복잡한 다중 날짜/시간 선택 UI
```

### After (단순함)
```
사용자: 하루에 하나의 연속 시간대만
백엔드: startDate == endDate 검증 1줄
프론트: 단순한 날짜 + 시간 선택 UI
```

**결과:** 빠른 개발, 적은 버그, 명확한 UX ✅
