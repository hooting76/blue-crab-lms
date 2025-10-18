# UI 빠진 부분 - 빠른 체크리스트

## 🔴 가장 심각한 문제 (즉시 수정 필요!)

### 1. API 연동이 전혀 없음 ❌
```tsx
// 현재: 모든 데이터가 하드코딩
const facilities = [{ id: 1, name: '...' }];
const myBookings = [{ id: 1, facility: '...' }];

// 필요: 실제 API 호출
fetch('/api/facilities')
fetch('/api/reservations/my')
fetch('/api/reservations', { method: 'POST', body: ... })
```

### 2. 실시간 가용성 체크 없음 ❌
```tsx
// 필요: 시간 선택 시마다 실시간 체크
fetch(`/api/facilities/${id}/availability?startTime=...&endTime=...`)
```

### 3. 정책 정보 표시 없음 ❌
```tsx
// 필요: 승인 필요 여부 표시
{facility.requiresApproval ? (
  <span className="text-yellow-600">⏳ 관리자 승인 필요</span>
) : (
  <span className="text-green-600">✅ 즉시 승인</span>
)}
```

---

## 📋 빠진 API 목록

### 사용자 UI
- [ ] `POST /api/facilities` - 시설 목록 조회
- [ ] `POST /api/facilities/{id}` - 시설 상세 조회
- [ ] `POST /api/facilities/{id}/availability` - 가용성 체크
- [ ] `POST /api/facilities/type/{type}` - 타입별 조회
- [ ] `POST /api/facilities/search?keyword=` - 검색
- [ ] `POST /api/reservations` - 예약 생성
- [ ] `POST /api/reservations/my` - 내 예약 목록
- [ ] `POST /api/reservations/my/status/{status}` - 상태별 조회
- [ ] `POST /api/reservations/{id}` - 예약 상세 조회
- [ ] `DELETE /api/reservations/{id}` - 예약 취소

### 관리자 UI
- [ ] `POST /api/admin/reservations/pending` - 승인 대기 목록
- [ ] `POST /api/admin/reservations/pending/count` - 대기 건수
- [ ] `POST /api/admin/reservations/approve` - 승인 처리
- [ ] `POST /api/admin/reservations/reject` - 반려 처리
- [ ] `POST /api/admin/reservations/stats` - 통계 조회
- [ ] ⚠️ `POST /api/admin/reservations/all` - **백엔드에 없음! 추가 필요**

---

## 🎯 빠른 수정 가이드

### Step 1: API 연동 (1일)
```tsx
// 1. useState 추가
const [facilities, setFacilities] = useState([]);
const [loading, setLoading] = useState(false);
const [error, setError] = useState(null);

// 2. useEffect로 데이터 로드
useEffect(() => {
  const fetchData = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/facilities', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await response.json();
      setFacilities(result.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };
  fetchData();
}, []);

// 3. 로딩/에러 UI 추가
{loading && <LoadingSpinner />}
{error && <ErrorMessage message={error} />}
```

### Step 2: 가용성 체크 (반나절)
```tsx
const checkAvailability = async (facilityId, date, time) => {
  const startTime = `${date}T${time}:00:00`;
  const endTime = `${date}T${parseInt(time) + 1}:00:00`;
  
  const params = new URLSearchParams({ startTime, endTime });
  const response = await fetch(
    `/api/facilities/${facilityId}/availability?${params}`,
    { method: 'POST', headers: { 'Authorization': `Bearer ${token}` } }
  );
  const result = await response.json();
  return result.data.isAvailable;
};
```

### Step 3: 예약 생성 (반나절)
```tsx
const handleSubmit = async () => {
  try {
    const response = await fetch('/api/reservations', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        facilityIdx: selectedFacility,
        startTime: `${selectedDate}T${selectedTimes[0]}:00:00`,
        endTime: `${selectedDate}T${calculateEndTime()}:00:00`,
        partySize: participantCount,
        purpose: bookingPurpose,
        requestedEquipment: requestedEquipment
      })
    });
    
    const result = await response.json();
    
    if (result.success) {
      alert(result.message);
      navigate('/myBookings');
    } else {
      alert('예약 실패: ' + result.message);
    }
  } catch (error) {
    alert('오류 발생: ' + error.message);
  }
};
```

---

## 📊 현재 상태

| 기능 | 백엔드 | 프론트엔드 | 상태 |
|-----|--------|-----------|------|
| 시설 목록 조회 | ✅ | ❌ | **미연동** |
| 시설 상세 조회 | ✅ | ❌ | **미연동** |
| 가용성 체크 | ✅ | ❌ | **미연동** |
| 예약 생성 | ✅ | ❌ | **미연동** |
| 내 예약 목록 | ✅ | ❌ | **미연동** |
| 예약 취소 | ✅ | ❌ | **미연동** |
| 승인/반려 | ✅ | ❌ | **미연동** |
| 정책 표시 | ✅ | ❌ | **미표시** |
| 에러 처리 | - | ❌ | **없음** |
| 로딩 상태 | - | ❌ | **없음** |

---

## ⚠️ 추가로 필요한 백엔드 API

```java
// AdminFacilityReservationController.java에 추가 필요

@PostMapping("/all")
public ResponseEntity<ApiResponse<List<ReservationDto>>> getAllReservations(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String facility,
        @RequestParam(required = false) String search,
        HttpServletRequest request) {
    String adminId = getAdminIdFromToken(request);
    
    // 필터링 로직
    List<ReservationDto> reservations = adminReservationService
        .getAllReservations(adminId, status, facility, search);
    
    return ResponseEntity.ok(
        ApiResponse.success("전체 예약 목록을 조회했습니다.", reservations)
    );
}
```

---

## 🚀 작업 순서

1. **Day 1 오전**: 시설 목록 + 내 예약 목록 API 연동
2. **Day 1 오후**: 예약 생성 + 예약 취소 API 연동
3. **Day 2 오전**: 가용성 체크 API 연동
4. **Day 2 오후**: 관리자 승인/반려 API 연동
5. **Day 3**: 에러 처리, 로딩 상태, 정책 표시, 테스트

**총 예상 시간: 2-3일**

---

## 💡 Tip

- 먼저 하나의 API부터 연동해서 패턴 익히기
- 공통 fetch 함수 만들어서 재사용
- TypeScript 사용 시 타입 정의 먼저
- 에러 처리는 처음부터 넣기 (나중에 추가하기 어려움)

---

**결론: UI는 있지만 백엔드와 완전히 분리된 상태입니다. API 연동이 최우선 과제입니다!** 🎯
