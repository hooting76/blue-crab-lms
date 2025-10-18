# 프론트엔드-백엔드 API 매핑 분석 보고서

## 📋 개요
- **분석 일자**: 2025-10-13
- **대상**: 시설 예약 시스템
- **프론트엔드**: React (facility_booking_system.tsx, admin_booking_system.tsx)
- **백엔드**: Spring Boot REST API

---

## ✅ 사용자용 기능 매핑

### 1. 시설 목록 조회
**프론트엔드 요구사항:**
```tsx
const facilities = [ /* 시설 목록 데이터 */ ];
const categories = [ /* 카테고리 필터 */ ];
```

**백엔드 API:** ✅ 완벽 대응
```java
POST /api/facilities                    // 전체 시설 목록
POST /api/facilities/type/{facilityType} // 카테고리별 필터
POST /api/facilities/{facilityIdx}       // 개별 시설 상세
POST /api/facilities/search?keyword=xxx  // 검색 기능
```

**추가 정보:**
- 시설별 정책 (승인 필요 여부, 예약 제한 등) 포함
- 차단 정보 (현재 사용 불가 여부) 포함

---

### 2. 예약 가능 시간 조회
**프론트엔드 요구사항:**
```tsx
const timeSlots = [
  { time: '09:00', status: 'available' },
  { time: '10:00', status: 'reserved' },
  { time: '11:00', status: 'pending' },
  // ...
];
```

**백엔드 API:** ✅ 있음
```java
POST /api/facilities/{facilityIdx}/availability
  ?startTime=2025-10-15 09:00:00
  &endTime=2025-10-15 18:00:00
```

**응답 예시:**
```json
{
  "success": true,
  "message": "가용성을 확인했습니다.",
  "data": {
    "facilityIdx": 1,
    "facilityName": "다목적 회의실 1호",
    "isAvailable": true,
    "conflictingReservations": [
      {
        "startTime": "2025-10-15 13:00:00",
        "endTime": "2025-10-15 14:00:00"
      }
    ]
  }
}
```

**⚠️ 주의:** 
- 백엔드는 특정 시간대의 가용성만 체크
- **프론트엔드가 필요한 "하루 전체 시간대별 상태"를 한 번에 조회하는 API 없음**
- 프론트엔드에서 여러 번 API 호출하거나, 백엔드에 새로운 API 필요

**💡 개선 제안:**
```java
// 새로운 API 추가 필요
POST /api/facilities/{facilityIdx}/daily-availability
  ?date=2025-10-15
  
// 응답: 하루 전체 시간대별 상태
{
  "date": "2025-10-15",
  "slots": [
    { "time": "09:00", "status": "available" },
    { "time": "10:00", "status": "available" },
    { "time": "13:00", "status": "reserved" },
    { "time": "14:00", "status": "pending" }
  ]
}
```

---

### 3. 예약 신청
**프론트엔드 요구사항:**
```tsx
// 예약 신청 폼
- selectedTimes: ['09:00', '10:00']  // 여러 시간대 선택
- bookingPurpose: '사용 목적'
- participantCount: 4
- requestedEquipment: '빔프로젝터'
```

**백엔드 API:** ✅ 완벽 대응
```java
POST /api/reservations
{
  "facilityIdx": 1,
  "startTime": "2025-10-15 09:00:00",
  "endTime": "2025-10-15 11:00:00",  // 연속 시간 처리
  "purpose": "중간고사 스터디",
  "participantCount": 4,
  "requestedEquipment": "빔프로젝터, 화이트보드"
}
```

**검증 로직:** ✅ 완벽
- 과거 날짜 체크
- 시설별 최소/최대 사전 예약 일수
- 시설별 최소/최대 예약 시간
- 예약 충돌 체크
- 차단 기간 체크

---

### 4. 내 예약 목록 조회
**프론트엔드 요구사항:**
```tsx
const myBookings = [
  { id, facility, date, time, status, purpose, participants, equipment, adminNote, reason }
];

// 탭 필터
- activeTab: 'ongoing' (진행중) or 'completed' (완료)
// 상태 필터
- bookingStatusFilter: 'approved', 'pending', 'completed', 'rejected'
```

**백엔드 API:** ✅ 완벽 대응
```java
POST /api/reservations/my                      // 전체 내 예약
POST /api/reservations/my/status/{status}      // 상태별 필터
POST /api/reservations/{reservationIdx}        // 개별 예약 상세
```

**응답 데이터:** ✅ 완벽
```json
{
  "reservationIdx": 1,
  "facilityIdx": 1,
  "facilityName": "세미나실 301호",
  "startTime": "2025-10-05 14:00:00",
  "endTime": "2025-10-05 16:00:00",
  "status": "APPROVED",
  "purpose": "캡스톤 프로젝트 회의",
  "participantCount": 8,
  "requestedEquipment": "빔프로젝터",
  "approvedAt": "2025-10-01 10:30:00",
  "adminNote": "장비 세팅 완료",
  "rejectionReason": null,
  "createdAt": "2025-10-01 10:00:00"
}
```

---

### 5. 예약 취소
**프론트엔드 요구사항:**
```tsx
const handleCancelBooking = (bookingId) => {
  if (window.confirm('예약을 취소하시겠습니까?')) {
    // API 호출
  }
};
```

**백엔드 API:** ✅ 완벽 대응
```java
DELETE /api/reservations/{reservationIdx}
```

**검증 로직:** ✅ 있음
- 본인 예약만 취소 가능
- 취소 가능 기한 체크 (정책별)

---

## ✅ 관리자용 기능 매핑

### 1. 승인 대기 목록
**프론트엔드 요구사항:**
```tsx
const pendingBookings = [
  { id, facility, date, time, status: 'pending', userName, userCode, userEmail, requestedAt }
];
```

**백엔드 API:** ✅ 완벽 대응
```java
POST /api/admin/reservations/pending         // 승인 대기 목록
POST /api/admin/reservations/pending/count   // 승인 대기 건수
```

---

### 2. 예약 승인/반려
**프론트엔드 요구사항:**
```tsx
// 승인
- approvalNote: '승인 비고 (선택)'

// 반려
- rejectionReason: '반려 사유 (필수)'
```

**백엔드 API:** ✅ 완벽 대응
```java
POST /api/admin/reservations/approve
{
  "reservationIdx": 1,
  "adminNote": "장비 세팅 완료"
}

POST /api/admin/reservations/reject
{
  "reservationIdx": 2,
  "rejectionReason": "해당 시간대 정기 행사"
}
```

---

### 3. 전체 예약 목록 (필터링)
**프론트엔드 요구사항:**
```tsx
// 필터
- filterStatus: '', 'pending', 'approved', 'rejected'
- filterFacility: '', '스터디룸', '세미나실', ...
- searchTerm: '이름, 학번, 시설명'
```

**백엔드 API:** ✅ 완벽 대응
```java
POST /api/admin/reservations/all
  ?status=pending
  &facilityIdx=1
  &startDate=2025-10-01T00:00:00
  &endDate=2025-10-31T23:59:59
```

**⚠️ 주의:**
- 사용자명/학번 검색 기능은 백엔드에서 구현 필요
- 현재는 날짜와 상태, 시설만 필터 가능

---

### 4. 통계
**프론트엔드 요구사항:**
```tsx
const stats = {
  today: { total: 12, inUse: 3, upcoming: 9 },
  pending: 3,
  thisWeek: 45,
  thisMonth: 180
};
```

**백엔드 API:** ✅ 있음
```java
POST /api/admin/reservations/stats
  ?startDate=2025-10-01T00:00:00
  &endDate=2025-10-31T23:59:59
```

**응답:**
```json
{
  "totalReservations": 180,
  "pendingCount": 3,
  "approvedCount": 150,
  "rejectedCount": 27,
  "completedCount": 145
}
```

**⚠️ 주의:**
- "오늘 예약", "사용 중", "예정" 같은 세부 통계는 추가 필요

---

## 🔍 기능 비교 매트릭스

| 기능 | 프론트엔드 | 백엔드 API | 상태 |
|------|-----------|-----------|------|
| **사용자 기능** |
| 시설 목록 조회 | ✅ | ✅ | 완벽 |
| 카테고리 필터 | ✅ | ✅ | 완벽 |
| 시설 검색 | ✅ | ✅ | 완벽 |
| 시설 상세 정보 | ✅ | ✅ | 완벽 |
| **예약 가능 여부 확인** | ✅ | ⚠️ | **개선 필요** |
| └ 특정 시간대 체크 | ✅ | ✅ | 완벽 |
| └ 하루 전체 시간대 | ✅ | ❌ | **API 없음** |
| 예약 신청 | ✅ | ✅ | 완벽 |
| └ 정책 검증 | ✅ | ✅ | 완벽 |
| └ 충돌 체크 | ✅ | ✅ | 완벽 |
| 내 예약 목록 | ✅ | ✅ | 완벽 |
| └ 상태별 필터 | ✅ | ✅ | 완벽 |
| └ 진행중/완료 탭 | ✅ | ✅ | 완벽 |
| 예약 상세 정보 | ✅ | ✅ | 완벽 |
| 예약 취소 | ✅ | ✅ | 완벽 |
| **관리자 기능** |
| 승인 대기 목록 | ✅ | ✅ | 완벽 |
| 승인 대기 건수 | ✅ | ✅ | 완벽 |
| 예약 승인 | ✅ | ✅ | 완벽 |
| └ 관리자 비고 | ✅ | ✅ | 완벽 |
| 예약 반려 | ✅ | ✅ | 완벽 |
| └ 반려 사유 | ✅ | ✅ | 완벽 |
| 전체 예약 목록 | ✅ | ✅ | 완벽 |
| └ 상태 필터 | ✅ | ✅ | 완벽 |
| └ 시설 필터 | ✅ | ✅ | 완벽 |
| └ **사용자 검색** | ✅ | ❌ | **API 개선 필요** |
| 통계 정보 | ✅ | ⚠️ | 부분 구현 |
| └ 기본 통계 | ✅ | ✅ | 완벽 |
| └ **세부 통계** | ✅ | ❌ | **추가 필요** |

---

## ⚠️ 주요 발견 사항

### 1. 🔴 하루 전체 시간대 조회 API 없음

**문제:**
```tsx
// 프론트엔드는 이런 UI를 원함
09:00 [예약 가능]
10:00 [예약 가능]
11:00 [예약됨]      ← 누군가 예약함
12:00 [예약 가능]
13:00 [승인 대기]    ← 다른 사람이 신청했으나 대기중
14:00 [예약됨]
```

**현재 백엔드:**
- 특정 시간대(09:00-11:00)만 체크 가능
- 하루 전체를 보려면 12번 API 호출 필요 (비효율)

**해결책:**
```java
// 새로운 API 추가
@PostMapping("/{facilityIdx}/daily-schedule")
public ResponseEntity<ApiResponse<DailyScheduleDto>> getDailySchedule(
    @PathVariable Integer facilityIdx,
    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
    // 해당 날짜의 전체 예약 현황 반환
}
```

---

### 2. 🟡 사용자 검색 기능 부족

**문제:**
```tsx
// 관리자가 "김철수" 또는 "20241234"로 검색하려고 함
<input placeholder="이름, 학번, 시설명" />
```

**현재 백엔드:**
- 시설, 상태, 날짜만 필터 가능
- 사용자명, 학번 검색 불가

**해결책:**
```java
@PostMapping("/all")
public ResponseEntity<ApiResponse<List<ReservationDto>>> getAllReservations(
    @RequestParam(required = false) String status,
    @RequestParam(required = false) Integer facilityIdx,
    @RequestParam(required = false) String searchKeyword,  // 추가
    // ...
) {
    // searchKeyword로 userName, userCode, facilityName 검색
}
```

---

### 3. 🟡 세부 통계 부족

**프론트엔드가 원하는 통계:**
```tsx
today: {
  total: 12,        // 오늘 총 예약
  inUse: 3,         // 현재 사용 중
  upcoming: 9       // 예정된 예약
}
```

**현재 백엔드:**
- 총 예약 수, 승인/반려/완료 개수만 제공
- "현재 사용 중", "오늘 예약" 같은 세부 정보 없음

---

## ✅ 잘 구현된 부분

### 1. 시설별 정책 시스템 완벽
- ✅ 최소/최대 사전 예약 일수
- ✅ 최소/최대 예약 시간
- ✅ 승인 필요 여부
- ✅ 시설별 차단 기간

### 2. 예약 생성 검증 완벽
- ✅ 과거 날짜 체크
- ✅ 시설별 정책 검증
- ✅ 예약 충돌 체크
- ✅ 차단 기간 체크

### 3. 상태 관리 완벽
- ✅ PENDING → APPROVED/REJECTED 흐름
- ✅ 자동 승인 vs 수동 승인 구분
- ✅ 관리자 비고/반려 사유 저장

### 4. 권한 관리 완벽
- ✅ JWT 토큰 기반 인증
- ✅ 본인 예약만 조회/취소
- ✅ 관리자 전용 API 분리

---

## 📝 권장 사항

### 우선순위 1 (필수)
1. **하루 전체 시간대 조회 API 추가**
   - 프론트엔드 UX에 필수적
   - 현재 구현으로는 비효율적

### 우선순위 2 (권장)
2. **사용자 검색 기능 추가**
   - 관리자가 특정 사용자 예약 찾기 필요
   
3. **세부 통계 API 개선**
   - "오늘", "현재 사용 중" 통계 추가

### 우선순위 3 (개선)
4. **일괄 예약 API**
   - 현재: 여러 시간대 → 연속 시간으로만 예약
   - 개선: 불연속 시간대(09:00, 11:00, 14:00) 한 번에 예약

---

## 📊 전체 평가

| 항목 | 점수 | 비고 |
|------|------|------|
| 핵심 기능 | ⭐⭐⭐⭐⭐ 5/5 | 예약 생성/조회/취소/승인 완벽 |
| 정책 시스템 | ⭐⭐⭐⭐⭐ 5/5 | 시설별 상세 정책 완벽 |
| 권한 관리 | ⭐⭐⭐⭐⭐ 5/5 | JWT 인증 완벽 |
| UI 지원 | ⭐⭐⭐ 3/5 | 하루 시간대 조회 부족 |
| 검색/필터 | ⭐⭐⭐⭐ 4/5 | 사용자 검색 추가 필요 |
| 통계 | ⭐⭐⭐ 3/5 | 세부 통계 추가 필요 |

**종합 평가: ⭐⭐⭐⭐ 4.2/5**

핵심 기능은 완벽하게 구현되었으나, 프론트엔드 UX를 최적화하려면 **하루 전체 시간대 조회 API**가 필수적으로 필요합니다.

---

**작성자**: BlueCrab Development Team  
**검토 일자**: 2025-10-13
