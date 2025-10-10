# 시설물 예약 시스템 UI - 빠진 부분 분석

> **작성일**: 2025-10-10  
> **분석 대상**: facility_booking_system.tsx, admin_booking_system.tsx  
> **비교 기준**: 백엔드 API 명세

---

## 📊 전체 비교 요약

### ✅ 잘 구현된 부분
- 시설 목록 표시 (UI만)
- 시설 카테고리 필터
- 날짜/시간 선택 UI
- 내 예약 현황 페이지
- 관리자 승인/반려 UI
- 상세 정보 모달

### ❌ 빠진 중요 부분
1. **모든 API 연동** (가장 심각!)
2. 실시간 가용성 체크
3. 정책 정보 표시
4. 시설 상세 정보
5. 에러 처리 및 로딩 상태
6. 실시간 데이터 갱신

---

## 🔴 사용자 UI (facility_booking_system.tsx) - 빠진 부분

### 1. ❌ API 연동 전혀 없음 (Critical!)

#### 현재 상태:
```tsx
// ❌ 하드코딩된 시설물 데이터
const facilities = [
  { id: 1, name: '스터디룸 201호', capacity: 4, ... },
  { id: 2, name: '스터디룸 202호', capacity: 4, ... },
  // ...
];

// ❌ 하드코딩된 타임슬롯
const timeSlots = [
  { time: '09:00', status: 'available' },
  { time: '10:00', status: 'available' },
  // ...
];

// ❌ 하드코딩된 내 예약 목록
const myBookings = [
  { id: 1, facility: '세미나실 301호', ... },
  // ...
];
```

#### 필요한 API:
```tsx
// 1. 시설 목록 조회
useEffect(() => {
  const fetchFacilities = async () => {
    try {
      const response = await fetch('/api/facilities', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      const data = await response.json();
      setFacilities(data.data);
    } catch (error) {
      console.error('시설 목록 조회 실패:', error);
    }
  };
  fetchFacilities();
}, []);

// 2. 내 예약 목록 조회
useEffect(() => {
  const fetchMyReservations = async () => {
    try {
      const response = await fetch('/api/reservations/my', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      const data = await response.json();
      setMyBookings(data.data);
    } catch (error) {
      console.error('예약 목록 조회 실패:', error);
    }
  };
  fetchMyReservations();
}, []);

// 3. 예약 생성
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
        startTime: `${selectedDate}T${selectedTimes[0]}:00`,
        endTime: `${selectedDate}T${calculateEndTime()}:00`,
        partySize: participantCount,
        purpose: bookingPurpose,
        requestedEquipment: requestedEquipment
      })
    });
    const result = await response.json();
    // 처리...
  } catch (error) {
    console.error('예약 생성 실패:', error);
  }
};

// 4. 예약 취소
const handleCancelBooking = async (bookingId) => {
  try {
    const response = await fetch(`/api/reservations/${bookingId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    const result = await response.json();
    // 처리...
  } catch (error) {
    console.error('예약 취소 실패:', error);
  }
};
```

---

### 2. ❌ 실시간 가용성 체크 없음 (Critical!)

#### 현재 문제:
```tsx
// ❌ 하드코딩된 상태로 가용성 체크 불가능
const timeSlots = [
  { time: '09:00', status: 'available' },  // 실제 DB와 무관
  { time: '13:00', status: 'reserved' },   // 실제 DB와 무관
];
```

#### 필요한 구현:
```tsx
const [timeSlots, setTimeSlots] = useState([]);
const [loading, setLoading] = useState(false);

// 날짜나 시설 변경 시 실시간 가용성 체크
useEffect(() => {
  if (!selectedFacility || !selectedDate) return;
  
  const checkAvailability = async () => {
    setLoading(true);
    try {
      // 하루 전체 시간대에 대한 가용성 체크
      const slots = [];
      for (let hour = 9; hour <= 20; hour++) {
        const startTime = `${selectedDate}T${hour.toString().padStart(2, '0')}:00:00`;
        const endTime = `${selectedDate}T${(hour + 1).toString().padStart(2, '0')}:00:00`;
        
        const params = new URLSearchParams({
          startTime,
          endTime
        });
        
        const response = await fetch(
          `/api/facilities/${selectedFacility}/availability?${params}`,
          {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
          }
        );
        
        const result = await response.json();
        slots.push({
          time: `${hour.toString().padStart(2, '0')}:00`,
          status: result.data.isAvailable ? 'available' : 'reserved',
          conflicts: result.data.conflictingReservations
        });
      }
      
      setTimeSlots(slots);
    } catch (error) {
      console.error('가용성 체크 실패:', error);
    } finally {
      setLoading(false);
    }
  };
  
  checkAvailability();
}, [selectedFacility, selectedDate]);
```

---

### 3. ❌ 정책 정보 표시 없음 (High Priority)

#### 현재 문제:
```tsx
// 시설 선택 시 정책 정보가 표시되지 않음
// 사용자는 승인이 필요한지, 즉시 승인인지 알 수 없음
```

#### 필요한 구현:
```tsx
const [facilityPolicy, setFacilityPolicy] = useState(null);

// 시설 선택 시 정책 정보 표시
useEffect(() => {
  if (!selectedFacility) return;
  
  const facility = facilities.find(f => f.id === selectedFacility);
  if (facility) {
    setFacilityPolicy(facility.policy);
  }
}, [selectedFacility]);

// UI에 표시
{facilityPolicy && (
  <div className={`p-4 rounded-lg border ${
    facilityPolicy.requiresApproval 
      ? 'bg-yellow-50 border-yellow-200' 
      : 'bg-green-50 border-green-200'
  }`}>
    <div className="flex items-center gap-2 mb-2">
      {facilityPolicy.requiresApproval ? (
        <>
          <Clock className="text-yellow-600" size={20} />
          <span className="font-semibold text-yellow-800">
            관리자 승인 필요
          </span>
        </>
      ) : (
        <>
          <CheckCircle className="text-green-600" size={20} />
          <span className="font-semibold text-green-800">
            즉시 승인
          </span>
        </>
      )}
    </div>
    <div className="text-sm text-gray-700 space-y-1">
      {facilityPolicy.minDurationMinutes && (
        <div>• 최소 예약 시간: {facilityPolicy.minDurationMinutes}분</div>
      )}
      {facilityPolicy.maxDurationMinutes && (
        <div>• 최대 예약 시간: {facilityPolicy.maxDurationMinutes}분</div>
      )}
      {facilityPolicy.maxDaysInAdvance && (
        <div>• 최대 예약 가능일: {facilityPolicy.maxDaysInAdvance}일 전</div>
      )}
      {facilityPolicy.cancellationDeadlineHours && (
        <div>• 취소 가능 시간: {facilityPolicy.cancellationDeadlineHours}시간 전</div>
      )}
    </div>
  </div>
)}
```

---

### 4. ❌ 시설 상세 정보 API 없음

#### 현재 문제:
```tsx
// 시설 클릭 시 하드코딩된 데이터만 표시
const facility = facilities.find(f => f.id === selectedFacility);
```

#### 필요한 구현:
```tsx
const [facilityDetail, setFacilityDetail] = useState(null);

useEffect(() => {
  if (!selectedFacility) return;
  
  const fetchFacilityDetail = async () => {
    try {
      const response = await fetch(
        `/api/facilities/${selectedFacility}`,
        {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${token}` }
        }
      );
      const result = await response.json();
      setFacilityDetail(result.data);
    } catch (error) {
      console.error('시설 상세 정보 조회 실패:', error);
    }
  };
  
  fetchFacilityDetail();
}, [selectedFacility]);

// 상세 정보 표시
{facilityDetail && (
  <div className="space-y-3">
    <div>
      <h4 className="font-medium mb-1">시설 설명</h4>
      <p className="text-gray-600">{facilityDetail.description}</p>
    </div>
    <div>
      <h4 className="font-medium mb-1">이용 안내</h4>
      <p className="text-gray-600">{facilityDetail.usageGuidelines}</p>
    </div>
    <div>
      <h4 className="font-medium mb-1">주의사항</h4>
      <p className="text-gray-600">{facilityDetail.notes}</p>
    </div>
  </div>
)}
```

---

### 5. ❌ 로딩 상태 및 에러 처리 없음

#### 현재 문제:
```tsx
// API 호출 중 로딩 표시 없음
// 에러 발생 시 사용자에게 알림 없음
```

#### 필요한 구현:
```tsx
const [loading, setLoading] = useState(false);
const [error, setError] = useState(null);

// 로딩 UI
{loading && (
  <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div className="bg-white rounded-lg p-6">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      <p className="mt-4 text-gray-600">처리 중...</p>
    </div>
  </div>
)}

// 에러 UI
{error && (
  <div className="fixed top-4 right-4 bg-red-50 border border-red-200 rounded-lg p-4 shadow-lg">
    <div className="flex items-center gap-2">
      <AlertCircle className="text-red-600" size={20} />
      <p className="text-red-800">{error}</p>
    </div>
    <button 
      onClick={() => setError(null)}
      className="mt-2 text-sm text-red-600 hover:text-red-700"
    >
      닫기
    </button>
  </div>
)}
```

---

### 6. ❌ 예약 상태별 필터링 API 연동 없음

#### 현재 문제:
```tsx
// 하드코딩된 데이터로만 필터링
const filteredBookings = bookingStatusFilter 
  ? myBookings.filter(b => b.status === bookingStatusFilter)
  : myBookings;
```

#### 필요한 구현:
```tsx
useEffect(() => {
  if (!bookingStatusFilter) {
    // 전체 조회
    fetchMyReservations();
  } else {
    // 상태별 조회
    const fetchByStatus = async () => {
      try {
        const response = await fetch(
          `/api/reservations/my/status/${bookingStatusFilter}`,
          {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
          }
        );
        const result = await response.json();
        setMyBookings(result.data);
      } catch (error) {
        console.error('예약 목록 조회 실패:', error);
      }
    };
    fetchByStatus();
  }
}, [bookingStatusFilter]);
```

---

### 7. ❌ 시설 타입별 조회 API 없음

#### 현재 문제:
```tsx
// 클라이언트 사이드에서만 필터링
const filteredFacilities = selectedCategory 
  ? facilities.filter(f => f.category === selectedCategory)
  : facilities;
```

#### 필요한 구현:
```tsx
useEffect(() => {
  const fetchFacilities = async () => {
    try {
      const url = selectedCategory 
        ? `/api/facilities/type/${selectedCategory}`
        : '/api/facilities';
      
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await response.json();
      setFacilities(result.data);
    } catch (error) {
      console.error('시설 목록 조회 실패:', error);
    }
  };
  
  fetchFacilities();
}, [selectedCategory]);
```

---

### 8. ❌ 시설 검색 기능 없음

#### 백엔드 API 존재:
```java
@PostMapping("/search")
public ResponseEntity<ApiResponse<List<FacilityDto>>> searchFacilities(
    @RequestParam String keyword) {
    // ...
}
```

#### 필요한 UI 구현:
```tsx
const [searchKeyword, setSearchKeyword] = useState('');

const handleSearch = async () => {
  if (!searchKeyword.trim()) return;
  
  try {
    const response = await fetch(
      `/api/facilities/search?keyword=${encodeURIComponent(searchKeyword)}`,
      {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      }
    );
    const result = await response.json();
    setFacilities(result.data);
  } catch (error) {
    console.error('검색 실패:', error);
  }
};

// UI
<div className="mb-6">
  <div className="relative">
    <input
      type="text"
      value={searchKeyword}
      onChange={(e) => setSearchKeyword(e.target.value)}
      onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
      placeholder="시설명 또는 위치로 검색..."
      className="w-full border rounded-lg p-3 pl-10"
    />
    <Search className="absolute left-3 top-3 text-gray-400" size={20} />
    <button
      onClick={handleSearch}
      className="absolute right-2 top-2 px-4 py-1 bg-blue-600 text-white rounded"
    >
      검색
    </button>
  </div>
</div>
```

---

### 9. ❌ 예약 상세 조회 API 없음

#### 백엔드 API 존재:
```java
@PostMapping("/{reservationIdx}")
public ResponseEntity<ApiResponse<ReservationDto>> getReservationById(
    @PathVariable Integer reservationIdx,
    HttpServletRequest httpRequest) {
    // ...
}
```

#### 현재 문제:
```tsx
// 하드코딩된 배열에서만 찾음
const booking = myBookings.find(b => b.id === bookingId);
```

#### 필요한 구현:
```tsx
const fetchBookingDetail = async (bookingId) => {
  try {
    const response = await fetch(
      `/api/reservations/${bookingId}`,
      {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      }
    );
    const result = await response.json();
    setSelectedBooking(result.data);
  } catch (error) {
    console.error('예약 상세 조회 실패:', error);
  }
};
```

---

### 10. ❌ 충돌 예약 정보 표시 없음

#### 필요한 구현:
```tsx
{conflicts && conflicts.length > 0 && (
  <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg">
    <div className="flex items-start gap-2">
      <AlertCircle className="text-red-600 mt-0.5" size={20} />
      <div className="flex-1">
        <div className="font-semibold text-red-800 mb-2">
          ⚠️ 이 시간대에 다른 예약이 있습니다
        </div>
        <div className="space-y-1">
          {conflicts.map((conflict, idx) => (
            <div key={idx} className="text-sm text-red-700">
              • {conflict.startTime} ~ {conflict.endTime}
            </div>
          ))}
        </div>
        <p className="text-sm text-red-600 mt-2">
          다른 시간대를 선택해주세요.
        </p>
      </div>
    </div>
  </div>
)}
```

---

## 🔴 관리자 UI (admin_booking_system.tsx) - 빠진 부분

### 1. ❌ 모든 API 연동 없음

#### 필요한 API:
```tsx
// 1. 승인 대기 목록
const fetchPendingBookings = async () => {
  const response = await fetch('/api/admin/reservations/pending', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const result = await response.json();
  setPendingBookings(result.data);
};

// 2. 전체 예약 목록 (필터링 포함)
const fetchAllBookings = async () => {
  // 전체 목록 조회 API가 없음!
  // 백엔드에 추가 필요: GET /api/admin/reservations/all
};

// 3. 승인 처리
const handleApprove = async (bookingId) => {
  const response = await fetch('/api/admin/reservations/approve', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      reservationIdx: bookingId,
      adminNote: approvalNote
    })
  });
  const result = await response.json();
  // 처리...
};

// 4. 반려 처리
const handleReject = async (bookingId) => {
  const response = await fetch('/api/admin/reservations/reject', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      reservationIdx: bookingId,
      rejectionReason: rejectionReason
    })
  });
  const result = await response.json();
  // 처리...
};

// 5. 통계 정보
const fetchStats = async () => {
  const response = await fetch('/api/admin/reservations/stats', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const result = await response.json();
  setStats(result.data);
};

// 6. 승인 대기 건수
const fetchPendingCount = async () => {
  const response = await fetch('/api/admin/reservations/pending/count', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const result = await response.json();
  setPendingCount(result.data);
};
```

---

### 2. ❌ 백엔드에 없는 API 사용 시도

#### 문제:
```tsx
// ❌ 전체 예약 목록 조회 API가 백엔드에 없음
// 현재 백엔드에는 승인 대기만 있음:
// - /api/admin/reservations/pending

// 필요하지만 없는 API:
// - GET /api/admin/reservations/all (전체 목록)
// - GET /api/admin/reservations/all?status=approved (상태별)
// - GET /api/admin/reservations/all?facility=스터디룸 (시설별)
```

#### 백엔드에 추가 필요:
```java
@PostMapping("/all")
public ResponseEntity<ApiResponse<List<ReservationDto>>> getAllReservations(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String facility,
        @RequestParam(required = false) String search,
        HttpServletRequest request) {
    String adminId = getAdminIdFromToken(request);
    // 필터링 로직 구현
    // ...
}
```

---

### 3. ❌ 실시간 통계 갱신 없음

#### 현재 문제:
```tsx
// 하드코딩된 통계
const stats = {
  today: { total: 12, inUse: 3, upcoming: 9 },
  pending: pendingBookings.length,
  thisWeek: 45,
  thisMonth: 180
};
```

#### 필요한 구현:
```tsx
useEffect(() => {
  const fetchStats = async () => {
    try {
      const response = await fetch('/api/admin/reservations/stats', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await response.json();
      setStats(result.data);
    } catch (error) {
      console.error('통계 조회 실패:', error);
    }
  };
  
  fetchStats();
  
  // 1분마다 통계 갱신
  const interval = setInterval(fetchStats, 60000);
  return () => clearInterval(interval);
}, []);
```

---

### 4. ❌ 승인 시 가용성 재확인 없음

#### 현재 문제:
```tsx
// 승인 버튼 클릭 시 바로 승인
// 중간에 다른 관리자가 동일 시간대 승인했을 수 있음
```

#### 필요한 개선:
```tsx
const handleApprove = async (booking) => {
  // 1. 먼저 가용성 재확인
  const checkResponse = await fetch(
    `/api/facilities/${booking.facilityIdx}/availability?` +
    `startTime=${booking.startTime}&endTime=${booking.endTime}`,
    {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    }
  );
  const availability = await checkResponse.json();
  
  if (!availability.data.isAvailable) {
    alert('다른 예약과 충돌합니다. 승인할 수 없습니다.');
    return;
  }
  
  // 2. 승인 처리
  const response = await fetch('/api/admin/reservations/approve', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      reservationIdx: booking.id,
      adminNote: approvalNote
    })
  });
  
  const result = await response.json();
  
  if (!result.success) {
    alert(result.message || '승인 처리 실패');
    return;
  }
  
  alert('승인되었습니다.');
  // 목록 갱신
  fetchPendingBookings();
};
```

---

## 📋 우선순위별 개선 항목

### 🔥 Critical (즉시 수정 필요)
1. **모든 API 연동** - 현재 완전히 하드코딩 상태
2. **실시간 가용성 체크** - 예약 충돌 방지 필수
3. **예약 생성/취소 API** - 핵심 기능

### 🟠 High (빠른 시일 내 수정)
4. **정책 정보 표시** - 사용자 혼란 방지
5. **에러 처리 및 로딩 상태** - UX 개선
6. **시설 상세 정보** - 정보 제공

### 🟡 Medium (점진적 개선)
7. **시설 검색 기능** - 편의성 향상
8. **상태별 필터링 API** - 성능 최적화
9. **충돌 예약 상세 표시** - 정보 가시화

### 🟢 Low (선택적 개선)
10. **실시간 통계 갱신** - 관리자 편의
11. **페이지네이션** - 대량 데이터 처리
12. **알림 기능** - 추가 기능

---

## 🎯 체크리스트

### 사용자 UI
- [ ] 시설 목록 API 연동
- [ ] 시설 상세 정보 API 연동
- [ ] 실시간 가용성 체크 API 연동
- [ ] 예약 생성 API 연동
- [ ] 내 예약 목록 API 연동
- [ ] 예약 취소 API 연동
- [ ] 정책 정보 표시
- [ ] 시설 검색 기능
- [ ] 로딩 상태 표시
- [ ] 에러 처리
- [ ] 충돌 예약 표시

### 관리자 UI
- [ ] 승인 대기 목록 API 연동
- [ ] 전체 예약 목록 API 연동 (백엔드 추가 필요)
- [ ] 승인 처리 API 연동
- [ ] 반려 처리 API 연동
- [ ] 통계 API 연동
- [ ] 승인 대기 건수 API 연동
- [ ] 승인 전 가용성 재확인
- [ ] 필터링 API 연동
- [ ] 로딩 상태 표시
- [ ] 에러 처리

---

## 📝 결론

**현재 UI는 프로토타입 수준입니다.**

### 주요 문제:
1. **API 연동 0%** - 모든 데이터가 하드코딩
2. **실시간 기능 없음** - 가용성 체크, 충돌 방지 불가능
3. **정책 정보 부재** - 사용자가 승인 필요 여부를 모름
4. **에러 처리 없음** - 실패 시 사용자에게 알림 없음

### 필요한 작업:
1. **1단계**: 모든 API 연동 (Critical)
2. **2단계**: 실시간 가용성 체크 구현 (Critical)
3. **3단계**: 에러 처리 및 로딩 상태 추가 (High)
4. **4단계**: 정책 정보 및 상세 정보 표시 (High)
5. **5단계**: 추가 기능 구현 (Medium/Low)

**예상 작업 시간: 2-3일** (API 연동 + 핵심 기능)

---

프론트엔드 개선 작업을 시작하시겠습니까? 🚀
