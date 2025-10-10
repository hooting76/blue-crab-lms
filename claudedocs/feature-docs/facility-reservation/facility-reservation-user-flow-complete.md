# 시설물 예약 시스템 - 사용자 플로우 분석 (개선 후)

> **작성일**: 2025-10-10  
> **버전**: v1.1 (최적화 적용 후)

---

## 🔍 현재 구현 상태 vs 예상 플로우 비교

### ❌ 현재 프론트엔드 상태 (미완성)

**FacilityRequest.jsx 분석:**
```jsx
// ❌ 문제점:
1. 하드코딩된 시설물 목록 (실제 API 호출 없음)
2. 가용성 체크 API 미연동
3. 예약 생성 API 미연동
4. 정책 정보 표시 없음
5. 실시간 예약 현황 미표시

// 현재 코드:
<select id="Facility">
  <option value="Fac01">시설물01</option>  // 하드코딩
  <option value="Fac02">시설물02</option>
  <option value="Fac03">시설물03</option>
</select>

// 예약 생성도 alert만 표시
const handleSubmit = () => {
  alert("신청 완료");  // ❌ 실제 API 호출 없음
};
```

---

## 📊 정확한 사용자 플로우 (백엔드 기준)

### ✅ 백엔드가 제공하는 완전한 플로우

```
┌─────────────────────────────────────────────────────────────────┐
│                    1. 페이지 진입                                │
│                 /Facility/FacilityRequest                        │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│            2. 시설물 목록 조회 (페이지 로드 시)                    │
│                                                                   │
│  [Frontend]                                                       │
│  useEffect(() => {                                                │
│    fetch('/api/facilities', { method: 'POST' })                  │
│  }, [])                                                           │
│                                                                   │
│  [Backend] FacilityController.getAllFacilities()                 │
│  ├─ FacilityService.getAllActiveFacilities()                     │
│  └─ DB: SELECT * FROM FACILITY_TBL WHERE IS_ACTIVE = true        │
│                                                                   │
│  [Response]                                                       │
│  {                                                                │
│    "facilities": [                                                │
│      {                                                            │
│        "facilityIdx": 1,                                          │
│        "facilityName": "대강의실 A",                               │
│        "facilityType": "CLASSROOM",                               │
│        "capacity": 100,                                           │
│        "location": "본관 3층",                                    │
│        "equipmentList": ["빔프로젝터", "음향시설"],                │
│        "requiresApproval": true  // ⭐ 승인 필요 여부              │
│      },                                                           │
│      ...                                                          │
│    ]                                                              │
│  }                                                                │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│            3. 사용자가 시설물 선택 + 날짜/시간 선택                 │
│                                                                   │
│  - 시설물 드롭다운에서 선택: "대강의실 A"                           │
│  - 날짜 선택: 2025-10-15                                          │
│  - 시작 시간: 14:00                                               │
│  - 종료 시간: 16:00                                               │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│        4. 실시간 가용성 체크 (날짜/시간 선택 시마다) ⭐              │
│                                                                   │
│  [Frontend]                                                       │
│  const checkAvailability = async () => {                         │
│    const params = new URLSearchParams({                          │
│      startTime: '2025-10-15T14:00:00',                           │
│      endTime: '2025-10-15T16:00:00'                              │
│    });                                                            │
│    const response = await fetch(                                 │
│      `/api/facilities/${facilityIdx}/availability?${params}`,    │
│      { method: 'POST' }                                           │
│    );                                                             │
│  }                                                                │
│                                                                   │
│  [Backend] FacilityController.checkAvailability()                │
│  ├─ FacilityReservationService.checkAvailability()               │
│  ├─ DB: 시설 차단 기간 체크 (FACILITY_BLOCK_TBL)                  │
│  └─ DB: 예약 충돌 체크 (FACILITY_RESERVATION_TBL)                 │
│                                                                   │
│  [Response]                                                       │
│  {                                                                │
│    "isAvailable": false,  // ❌ 예약 불가                         │
│    "conflictingReservations": [                                  │
│      {                                                            │
│        "startTime": "2025-10-15T14:00:00",                       │
│        "endTime": "2025-10-15T15:00:00"                          │
│      }                                                            │
│    ],                                                             │
│    "message": "해당 시간대에 다른 예약이 있습니다"                  │
│  }                                                                │
│                                                                   │
│  → Frontend: 빨간색으로 "예약 불가" 표시                           │
│  → 사용자: 다른 시간대 선택                                        │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│            5. 가용한 시간대 찾음 + 사유/장비 입력                   │
│                                                                   │
│  ✅ 가용성 체크 결과: "예약 가능"                                  │
│  - 날짜/시간: 2025-10-15 16:00 ~ 18:00                           │
│  - 사유: "학술 세미나"                                            │
│  - 참석 인원: 50명                                                │
│  - 요청 장비: "빔프로젝터, 마이크"                                 │
│                                                                   │
│  [신청하기] 버튼 클릭                                              │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                6. 예약 생성 API 호출 ⭐                           │
│                                                                   │
│  [Frontend]                                                       │
│  const createReservation = async () => {                         │
│    const response = await fetch('/api/reservations', {           │
│      method: 'POST',                                              │
│      headers: {                                                   │
│        'Authorization': `Bearer ${token}`,                        │
│        'Content-Type': 'application/json'                         │
│      },                                                           │
│      body: JSON.stringify({                                       │
│        facilityIdx: 1,                                            │
│        startTime: "2025-10-15T16:00:00",                         │
│        endTime: "2025-10-15T18:00:00",                           │
│        partySize: 50,                                             │
│        purpose: "학술 세미나",                                    │
│        requestedEquipment: "빔프로젝터, 마이크"                    │
│      })                                                           │
│    });                                                            │
│  };                                                               │
│                                                                   │
│  [Backend] FacilityReservationController.createReservation()    │
│  └─ JWT에서 userCode 추출                                        │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│        7. Service Layer - 예약 생성 로직 (개선된 버전) ⭐⭐        │
│                                                                   │
│  FacilityReservationService.createReservation()                  │
│                                                                   │
│  ┌──────────────────────────────────────────────────────┐        │
│  │ ① 요청 데이터 검증 (validateReservationRequest)       │        │
│  │   - 시작 시간 > 현재 시간                             │        │
│  │   - 종료 시간 > 시작 시간                             │        │
│  │   - 최대 예약 기간 내 (30일)                          │        │
│  │   - 최소/최대 예약 시간 준수 (30분~8시간)              │        │
│  └──────────────────────────────────────────────────────┘        │
│                      ↓                                            │
│  ┌──────────────────────────────────────────────────────┐        │
│  │ ② 정책 조회 (정책 먼저 확인 - 개선 사항!)             │        │
│  │   DB: SELECT * FROM FACILITY_POLICY_TBL               │        │
│  │       WHERE FACILITY_IDX = 1                          │        │
│  │                                                        │        │
│  │   Result: { requiresApproval: true }                  │        │
│  └──────────────────────────────────────────────────────┘        │
│                      ↓                                            │
│  ┌──────────────────────────────────────────────────────┐        │
│  │ ③-A [승인 필요 시설] 락 없이 처리 🚀                   │        │
│  │                                                        │        │
│  │   • 시설 조회 (락 없음)                                │        │
│  │     DB: SELECT * FROM FACILITY_TBL WHERE ID = 1       │        │
│  │                                                        │        │
│  │   • 차단 기간만 체크                                   │        │
│  │     DB: SELECT * FROM FACILITY_BLOCK_TBL              │        │
│  │         WHERE FACILITY_IDX = 1                        │        │
│  │         AND BLOCK_START < '2025-10-15T18:00'          │        │
│  │         AND BLOCK_END > '2025-10-15T16:00'            │        │
│  │                                                        │        │
│  │   • 예약 충돌 체크 생략! (승인 시 체크)                 │        │
│  │     → 여러 사용자 동시 요청 가능 ⚡                     │        │
│  │     → 관리자가 승인할 때 충돌 검증                      │        │
│  │                                                        │        │
│  │   • 예약 상태: PENDING                                 │        │
│  └──────────────────────────────────────────────────────┘        │
│                      OR                                           │
│  ┌──────────────────────────────────────────────────────┐        │
│  │ ③-B [즉시 승인 시설] 비관적 락 적용 🔒                │        │
│  │                                                        │        │
│  │   • 시설 조회 (락 획득)                                │        │
│  │     DB: SELECT * FROM FACILITY_TBL                    │        │
│  │         WHERE ID = 1 FOR UPDATE                       │        │
│  │                                                        │        │
│  │   • 차단 기간 체크                                     │        │
│  │   • 예약 충돌 체크 (락 보유 상태에서)                   │        │
│  │     DB: SELECT * FROM FACILITY_RESERVATION_TBL        │        │
│  │         WHERE FACILITY_IDX = 1                        │        │
│  │         AND STATUS IN ('PENDING', 'APPROVED')         │        │
│  │         AND START_TIME < '2025-10-15T18:00'           │        │
│  │         AND END_TIME > '2025-10-15T16:00'             │        │
│  │                                                        │        │
│  │   • 충돌 있으면 즉시 예외 발생                          │        │
│  │   • 예약 상태: APPROVED (자동 승인)                    │        │
│  └──────────────────────────────────────────────────────┘        │
│                      ↓                                            │
│  ┌──────────────────────────────────────────────────────┐        │
│  │ ④ 예약 레코드 DB 저장                                  │        │
│  │   DB: INSERT INTO FACILITY_RESERVATION_TBL            │        │
│  │       (FACILITY_IDX, USER_CODE, START_TIME,           │        │
│  │        END_TIME, PARTY_SIZE, PURPOSE,                 │        │
│  │        REQUESTED_EQUIPMENT, STATUS, ...)              │        │
│  │       VALUES (1, 'USER001', '2025-10-15 16:00',      │        │
│  │                '2025-10-15 18:00', 50,                │        │
│  │                '학술 세미나', '빔프로젝터, 마이크',      │        │
│  │                'PENDING', ...)                        │        │
│  └──────────────────────────────────────────────────────┘        │
│                      ↓                                            │
│  ┌──────────────────────────────────────────────────────┐        │
│  │ ⑤ 예약 로그 기록                                       │        │
│  │   DB: INSERT INTO FACILITY_RESERVATION_LOG            │        │
│  │       (RESERVATION_IDX, EVENT_TYPE, ACTOR_TYPE,       │        │
│  │        ACTOR_CODE, PAYLOAD, CREATED_AT)               │        │
│  │       VALUES (123, 'CREATED', 'USER',                 │        │
│  │                'USER001', '{"purpose":"..."}', NOW()) │        │
│  └──────────────────────────────────────────────────────┘        │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    8. 응답 반환                                   │
│                                                                   │
│  [Case A: 승인 필요 시설]                                         │
│  {                                                                │
│    "success": true,                                               │
│    "message": "예약이 생성되었습니다. 관리자 승인 대기 중입니다.",  │
│    "data": {                                                      │
│      "reservationIdx": 123,                                       │
│      "status": "PENDING",  // 대기 중                             │
│      "facilityName": "대강의실 A",                                │
│      "startTime": "2025-10-15T16:00:00",                         │
│      "endTime": "2025-10-15T18:00:00",                           │
│      ...                                                          │
│    }                                                              │
│  }                                                                │
│                                                                   │
│  [Case B: 즉시 승인 시설]                                         │
│  {                                                                │
│    "success": true,                                               │
│    "message": "예약이 자동으로 승인되었습니다.",                    │
│    "data": {                                                      │
│      "reservationIdx": 124,                                       │
│      "status": "APPROVED",  // 승인 완료                          │
│      "approvedBy": "SYSTEM",                                      │
│      "approvedAt": "2025-10-15T10:30:00",                        │
│      ...                                                          │
│    }                                                              │
│  }                                                                │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                9. Frontend 처리                                   │
│                                                                   │
│  if (response.data.status === 'PENDING') {                       │
│    // 승인 대기 알림                                              │
│    showNotification("예약 신청 완료! 관리자 승인 대기 중");         │
│    navigate('/Facility/MyFacilityRequests');                     │
│  } else if (response.data.status === 'APPROVED') {               │
│    // 즉시 승인 알림                                              │
│    showNotification("예약 완료! 바로 이용 가능합니다.");            │
│    navigate('/Facility/MyFacilityRequests');                     │
│  }                                                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 당신의 예상 플로우 vs 실제 구현

### ✅ 맞는 부분:
1. ✅ 시설물 신청 컴포넌트 이동
2. ✅ 시설물 조회 API 호출
3. ✅ DB에서 사용가능 시설 확인 후 전송
4. ✅ 예약할 시설물 클릭 (+ 날짜/시간 선택)
5. ✅ 사유와 장비 대여 담아서 예약 생성 호출
6. ✅ 정책 확인
7. ✅ DB에서 예약 생성

### ⭐ 추가/개선된 부분:
1. **실시간 가용성 체크** (추가)
   - 날짜/시간 선택 시마다 실시간으로 예약 가능 여부 확인
   - 충돌하는 예약 정보 표시

2. **정책 기반 락 전략** (개선)
   - 승인 필요 시설: 락 없이 PENDING 저장 (병렬 처리)
   - 즉시 승인 시설: 락으로 충돌 방지

3. **차단 기간 체크** (추가)
   - 관리자가 설정한 예약 불가 기간 확인

4. **예약 로그 기록** (추가)
   - 모든 예약 활동 추적

---

## 📋 현재 프론트엔드에 필요한 개선사항

### ❌ 미구현 기능들:

```jsx
// 1. 시설물 목록 API 연동
useEffect(() => {
  const fetchFacilities = async () => {
    const response = await fetch('/api/facilities', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const data = await response.json();
    setFacilities(data.data);
  };
  fetchFacilities();
}, []);

// 2. 실시간 가용성 체크
const checkAvailability = async (facilityIdx, start, end) => {
  const params = new URLSearchParams({
    startTime: start.toISOString(),
    endTime: end.toISOString()
  });
  const response = await fetch(
    `/api/facilities/${facilityIdx}/availability?${params}`,
    { method: 'POST' }
  );
  const result = await response.json();
  setIsAvailable(result.data.isAvailable);
  setConflicts(result.data.conflictingReservations);
};

// 3. 예약 생성 API 호출
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
        startTime: startDate.toISOString(),
        endTime: endDate.toISOString(),
        partySize: parseInt(partySize),
        purpose: selectedReason === 'EtcReason' ? customReason : selectedReason,
        requestedEquipment: selectedEquipment
      })
    });
    
    const result = await response.json();
    
    if (result.success) {
      if (result.data.status === 'PENDING') {
        alert('예약 신청 완료! 관리자 승인 대기 중입니다.');
      } else {
        alert('예약이 자동으로 승인되었습니다!');
      }
      navigate('/Facility/MyFacilityRequests');
    }
  } catch (error) {
    alert('예약 생성 실패: ' + error.message);
  }
};

// 4. 시설물 정보 표시
{selectedFacility && (
  <div className="facility-info">
    <h3>{facilities[selectedFacility].facilityName}</h3>
    <p>위치: {facilities[selectedFacility].location}</p>
    <p>수용 인원: {facilities[selectedFacility].capacity}명</p>
    <p>이용 가능 장비: {facilities[selectedFacility].equipmentList.join(', ')}</p>
    <p className={facilities[selectedFacility].requiresApproval ? 'needs-approval' : 'auto-approved'}>
      {facilities[selectedFacility].requiresApproval 
        ? '⏳ 관리자 승인 필요' 
        : '✅ 즉시 승인'}
    </p>
  </div>
)}

// 5. 예약 충돌 표시
{conflicts && conflicts.length > 0 && (
  <div className="conflict-warning">
    ⚠️ 해당 시간대에 다른 예약이 있습니다:
    {conflicts.map(c => (
      <div key={c.startTime}>
        {c.startTime} ~ {c.endTime}
      </div>
    ))}
  </div>
)}
```

---

## 📊 완전한 플로우 체크리스트

| 단계 | 설명 | 백엔드 | 프론트엔드 | 상태 |
|-----|------|--------|-----------|------|
| 1 | 페이지 진입 | ✅ | ✅ | 완료 |
| 2 | 시설물 목록 조회 API | ✅ | ❌ | **미연동** |
| 3 | 시설물 정보 표시 | ✅ | ❌ | **미연동** |
| 4 | 날짜/시간 선택 UI | - | ✅ | 완료 |
| 5 | 실시간 가용성 체크 API | ✅ | ❌ | **미연동** |
| 6 | 충돌 예약 표시 | ✅ | ❌ | **미연동** |
| 7 | 사유/장비 입력 UI | - | ✅ | 완료 |
| 8 | 예약 생성 API 호출 | ✅ | ❌ | **미연동** |
| 9 | 정책 확인 (승인 필요 여부) | ✅ | ❌ | **미연동** |
| 10 | 차단 기간 체크 | ✅ | - | 완료 |
| 11 | 중복 예약 체크 (즉시 승인 시) | ✅ | - | 완료 |
| 12 | DB 예약 생성 | ✅ | - | 완료 |
| 13 | 로그 기록 | ✅ | - | 완료 |
| 14 | 응답 처리 및 페이지 이동 | ✅ | ❌ | **미연동** |

---

## 🎯 결론

### ✅ 당신의 예상 플로우는 **기본적으로 맞습니다!**

하지만 다음 사항들이 추가로 필요합니다:

1. **실시간 가용성 체크** - 사용자가 시간 선택 시 즉시 예약 가능 여부 확인
2. **정책 기반 처리** - 승인 필요/즉시 승인 구분하여 다른 메시지 표시
3. **차단 기간 확인** - 예약 불가 기간 체크
4. **프론트엔드 API 연동** - 현재 하드코딩된 부분을 실제 API와 연결

### 📝 우선순위 작업:

1. **High**: FacilityRequest.jsx에 API 연동
2. **High**: 실시간 가용성 체크 UI 구현
3. **Medium**: 시설물 정보 상세 표시
4. **Medium**: 예약 충돌 경고 UI
5. **Low**: 로딩 상태, 에러 처리

---

프론트엔드 개선 작업을 진행하시겠습니까? 🚀
