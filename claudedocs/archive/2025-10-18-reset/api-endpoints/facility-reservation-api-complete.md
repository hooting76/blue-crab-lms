# 시설물 예약 API 완전 가이드

**작성일:** 2025-10-10  
**버전:** 1.0  
**Base URL:** `http://your-domain.com/api`

---

## 📋 목차

1. [유저 API - 시설 조회](#1-유저-api---시설-조회)
2. [유저 API - 예약 관리](#2-유저-api---예약-관리)
3. [관리자 API - 예약 승인/반려](#3-관리자-api---예약-승인반려)
4. [공통 데이터 모델](#4-공통-데이터-모델)
5. [에러 응답](#5-에러-응답)

---

## 1. 유저 API - 시설 조회

**Base Path:** `/api/facilities`  
**인증:** 필요 없음 (공개 API)

### 1.1 모든 활성 시설 조회

```http
POST /api/facilities
```

**Request:**
```json
{} 
```
- Body 없음 (POST 방식이지만 페이로드 불필요)

**Response:**
```json
{
  "success": true,
  "message": "시설 목록을 조회했습니다.",
  "data": [
    {
      "facilityIdx": 1,
      "facilityName": "대강당",
      "facilityType": "AUDITORIUM",
      "location": "본관 3층",
      "capacity": 200,
      "description": "대규모 행사 및 강연에 적합한 공간",
      "imageUrl": "https://example.com/images/auditorium.jpg",
      "operatingHours": "09:00-22:00",
      "isActive": true,
      "requiresApproval": true,
      "createdAt": "2025-01-01T00:00:00"
    },
    {
      "facilityIdx": 2,
      "facilityName": "소회의실 A",
      "facilityType": "MEETING_ROOM",
      "location": "본관 2층",
      "capacity": 10,
      "description": "소규모 회의에 적합",
      "imageUrl": "https://example.com/images/meeting-room-a.jpg",
      "operatingHours": "09:00-18:00",
      "isActive": true,
      "requiresApproval": false,
      "createdAt": "2025-01-01T00:00:00"
    }
  ]
}
```

---

### 1.2 시설 유형별 조회

```http
POST /api/facilities/type/{facilityType}
```

**Path Parameters:**
- `facilityType` (string, required): 시설 유형
  - 가능한 값: `AUDITORIUM`, `MEETING_ROOM`, `SEMINAR_ROOM`, `LAB`, `GYM`, `OUTDOOR`, `OTHER`

**Example:**
```http
POST /api/facilities/type/MEETING_ROOM
```

**Response:**
```json
{
  "success": true,
  "message": "시설 목록을 조회했습니다.",
  "data": [
    {
      "facilityIdx": 2,
      "facilityName": "소회의실 A",
      "facilityType": "MEETING_ROOM",
      "capacity": 10,
      "requiresApproval": false,
      ...
    },
    {
      "facilityIdx": 3,
      "facilityName": "소회의실 B",
      "facilityType": "MEETING_ROOM",
      "capacity": 8,
      "requiresApproval": false,
      ...
    }
  ]
}
```

---

### 1.3 특정 시설 상세 조회

```http
POST /api/facilities/{facilityIdx}
```

**Path Parameters:**
- `facilityIdx` (integer, required): 시설 ID

**Example:**
```http
POST /api/facilities/1
```

**Response:**
```json
{
  "success": true,
  "message": "시설 정보를 조회했습니다.",
  "data": {
    "facilityIdx": 1,
    "facilityName": "대강당",
    "facilityType": "AUDITORIUM",
    "location": "본관 3층",
    "capacity": 200,
    "description": "대규모 행사 및 강연에 적합한 공간",
    "imageUrl": "https://example.com/images/auditorium.jpg",
    "operatingHours": "09:00-22:00",
    "isActive": true,
    "requiresApproval": true,
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

---

### 1.4 시설 검색 (키워드)

```http
POST /api/facilities/search?keyword={keyword}
```

**Query Parameters:**
- `keyword` (string, required): 검색 키워드 (시설명, 위치 등)

**Example:**
```http
POST /api/facilities/search?keyword=회의실
```

**Response:**
```json
{
  "success": true,
  "message": "검색 결과를 조회했습니다.",
  "data": [
    {
      "facilityIdx": 2,
      "facilityName": "소회의실 A",
      ...
    },
    {
      "facilityIdx": 3,
      "facilityName": "소회의실 B",
      ...
    }
  ]
}
```

---

### 1.5 시설 가용성 확인 (실시간)

```http
POST /api/facilities/{facilityIdx}/availability?startTime={startTime}&endTime={endTime}
```

**Path Parameters:**
- `facilityIdx` (integer, required): 시설 ID

**Query Parameters:**
- `startTime` (ISO 8601 DateTime, required): 시작 시간
  - Format: `2025-10-15T09:00:00`
- `endTime` (ISO 8601 DateTime, required): 종료 시간
  - Format: `2025-10-15T12:00:00`

**⚠️ 중요 제약 사항:**
- `startTime`과 `endTime`은 **같은 날짜**여야 함
- 여러 날에 걸친 예약 불가

**Example:**
```http
POST /api/facilities/1/availability?startTime=2025-10-15T09:00:00&endTime=2025-10-15T12:00:00
```

**Response (예약 가능):**
```json
{
  "success": true,
  "message": "가용성을 확인했습니다.",
  "data": {
    "facilityIdx": 1,
    "facilityName": "대강당",
    "isAvailable": true,
    "conflictingReservations": []
  }
}
```

**Response (예약 불가 - 충돌 있음):**
```json
{
  "success": true,
  "message": "가용성을 확인했습니다.",
  "data": {
    "facilityIdx": 1,
    "facilityName": "대강당",
    "isAvailable": false,
    "conflictingReservations": [
      {
        "startTime": "2025-10-15T10:00:00",
        "endTime": "2025-10-15T11:00:00"
      }
    ]
  }
}
```

---

## 2. 유저 API - 예약 관리

**Base Path:** `/api/reservations`  
**인증:** 필요 (JWT Token)

### 2.1 예약 생성

```http
POST /api/reservations
```

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Body:**
```json
{
  "facilityIdx": 1,
  "startTime": "2025-10-15T09:00:00",
  "endTime": "2025-10-15T12:00:00",
  "partySize": 50,
  "purpose": "신입생 오리엔테이션",
  "requestedEquipment": "빔프로젝터, 마이크, 음향장비"
}
```

**필드 설명:**
- `facilityIdx` (integer, required): 시설 ID
- `startTime` (ISO 8601 DateTime, required): 시작 시간
- `endTime` (ISO 8601 DateTime, required): 종료 시간
- `partySize` (integer, optional): 사용 인원
- `purpose` (string, required): 사용 목적
- `requestedEquipment` (string, optional): 요청 장비

**검증 규칙:**
- ✅ 시작 시간 > 현재 시간
- ✅ 종료 시간 > 시작 시간
- ✅ **startTime과 endTime이 같은 날짜** (연속된 시간대만 가능)
- ✅ 최소 예약 시간: 30분 (설정 가능)
- ✅ 최대 예약 시간: 480분 (8시간, 설정 가능)
- ✅ 최대 예약 가능 기간: 현재부터 30일 이내 (설정 가능)

**Response (즉시 승인 시설):**
```json
{
  "success": true,
  "message": "예약이 자동으로 승인되었습니다.",
  "data": {
    "reservationIdx": 101,
    "facilityIdx": 2,
    "facilityName": "소회의실 A",
    "userCode": "USER001",
    "userName": "홍길동",
    "startTime": "2025-10-15T09:00:00",
    "endTime": "2025-10-15T12:00:00",
    "partySize": 8,
    "purpose": "프로젝트 회의",
    "requestedEquipment": "빔프로젝터",
    "status": "APPROVED",
    "adminNote": null,
    "rejectionReason": null,
    "approvedBy": "SYSTEM",
    "approvedAt": "2025-10-10T14:30:00",
    "createdAt": "2025-10-10T14:30:00"
  }
}
```

**Response (승인 필요 시설):**
```json
{
  "success": true,
  "message": "예약이 생성되었습니다. 관리자 승인 대기 중입니다.",
  "data": {
    "reservationIdx": 102,
    "facilityIdx": 1,
    "facilityName": "대강당",
    "userCode": "USER001",
    "userName": "홍길동",
    "startTime": "2025-10-15T09:00:00",
    "endTime": "2025-10-15T12:00:00",
    "partySize": 50,
    "purpose": "신입생 오리엔테이션",
    "requestedEquipment": "빔프로젝터, 마이크, 음향장비",
    "status": "PENDING",
    "adminNote": null,
    "rejectionReason": null,
    "approvedBy": null,
    "approvedAt": null,
    "createdAt": "2025-10-10T14:30:00"
  }
}
```

**Error Response (날짜 제약 위반):**
```json
{
  "success": false,
  "message": "예약은 같은 날짜 내에서만 가능합니다. 여러 날에 걸친 예약이 필요한 경우 각 날짜별로 따로 신청해주세요.",
  "data": null
}
```

---

### 2.2 내 예약 목록 조회 (전체)

```http
POST /api/reservations/my
```

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Request Body:**
```json
{}
```

**Response:**
```json
{
  "success": true,
  "message": "내 예약 목록을 조회했습니다.",
  "data": [
    {
      "reservationIdx": 102,
      "facilityIdx": 1,
      "facilityName": "대강당",
      "userCode": "USER001",
      "userName": "홍길동",
      "startTime": "2025-10-15T09:00:00",
      "endTime": "2025-10-15T12:00:00",
      "status": "PENDING",
      "createdAt": "2025-10-10T14:30:00"
    },
    {
      "reservationIdx": 101,
      "facilityIdx": 2,
      "facilityName": "소회의실 A",
      "userCode": "USER001",
      "userName": "홍길동",
      "startTime": "2025-10-14T14:00:00",
      "endTime": "2025-10-14T16:00:00",
      "status": "APPROVED",
      "approvedBy": "SYSTEM",
      "approvedAt": "2025-10-10T10:00:00",
      "createdAt": "2025-10-10T10:00:00"
    }
  ]
}
```

---

### 2.3 내 예약 목록 조회 (상태별)

```http
POST /api/reservations/my/status/{status}
```

**Path Parameters:**
- `status` (string, required): 예약 상태
  - 가능한 값: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED`

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Example:**
```http
POST /api/reservations/my/status/PENDING
```

**Response:**
```json
{
  "success": true,
  "message": "예약 목록을 조회했습니다.",
  "data": [
    {
      "reservationIdx": 102,
      "facilityIdx": 1,
      "facilityName": "대강당",
      "status": "PENDING",
      ...
    }
  ]
}
```

---

### 2.4 특정 예약 상세 조회

```http
POST /api/reservations/{reservationIdx}
```

**Path Parameters:**
- `reservationIdx` (integer, required): 예약 ID

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Example:**
```http
POST /api/reservations/102
```

**Response:**
```json
{
  "success": true,
  "message": "예약 정보를 조회했습니다.",
  "data": {
    "reservationIdx": 102,
    "facilityIdx": 1,
    "facilityName": "대강당",
    "userCode": "USER001",
    "userName": "홍길동",
    "startTime": "2025-10-15T09:00:00",
    "endTime": "2025-10-15T12:00:00",
    "partySize": 50,
    "purpose": "신입생 오리엔테이션",
    "requestedEquipment": "빔프로젝터, 마이크, 음향장비",
    "status": "PENDING",
    "adminNote": null,
    "rejectionReason": null,
    "approvedBy": null,
    "approvedAt": null,
    "createdAt": "2025-10-10T14:30:00"
  }
}
```

**Error Response (권한 없음):**
```json
{
  "success": false,
  "message": "본인의 예약만 조회할 수 있습니다.",
  "data": null
}
```

---

### 2.5 예약 취소

```http
DELETE /api/reservations/{reservationIdx}
```

**Path Parameters:**
- `reservationIdx` (integer, required): 예약 ID

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Example:**
```http
DELETE /api/reservations/102
```

**Response:**
```json
{
  "success": true,
  "message": "예약이 취소되었습니다.",
  "data": null
}
```

**취소 가능 조건:**
- 예약 상태가 `PENDING` 또는 `APPROVED`
- 본인의 예약만 취소 가능

**Error Response:**
```json
{
  "success": false,
  "message": "취소할 수 없는 상태입니다.",
  "data": null
}
```

---

## 3. 관리자 API - 예약 승인/반려

**Base Path:** `/api/admin/reservations`  
**인증:** 필요 (관리자 JWT Token)

### 3.1 승인 대기 예약 목록 조회

```http
POST /api/admin/reservations/pending
```

**Headers:**
```
Authorization: Bearer {ADMIN_JWT_TOKEN}
```

**Request Body:**
```json
{}
```

**Response:**
```json
{
  "success": true,
  "message": "승인 대기 중인 예약 목록을 조회했습니다.",
  "data": [
    {
      "reservationIdx": 102,
      "facilityIdx": 1,
      "facilityName": "대강당",
      "userCode": "USER001",
      "userName": "홍길동",
      "startTime": "2025-10-15T09:00:00",
      "endTime": "2025-10-15T12:00:00",
      "partySize": 50,
      "purpose": "신입생 오리엔테이션",
      "requestedEquipment": "빔프로젝터, 마이크, 음향장비",
      "status": "PENDING",
      "createdAt": "2025-10-10T14:30:00"
    },
    {
      "reservationIdx": 103,
      "facilityIdx": 1,
      "facilityName": "대강당",
      "userCode": "USER002",
      "userName": "김철수",
      "startTime": "2025-10-16T14:00:00",
      "endTime": "2025-10-16T18:00:00",
      "partySize": 100,
      "purpose": "학술 세미나",
      "requestedEquipment": "마이크, 음향장비",
      "status": "PENDING",
      "createdAt": "2025-10-10T15:00:00"
    }
  ]
}
```

---

### 3.2 승인 대기 건수 조회

```http
POST /api/admin/reservations/pending/count
```

**Headers:**
```
Authorization: Bearer {ADMIN_JWT_TOKEN}
```

**Request Body:**
```json
{}
```

**Response:**
```json
{
  "success": true,
  "message": "승인 대기 건수를 조회했습니다.",
  "data": 5
}
```

---

### 3.3 예약 승인

```http
POST /api/admin/reservations/approve
```

**Headers:**
```
Authorization: Bearer {ADMIN_JWT_TOKEN}
Content-Type: application/json
```

**Request Body:**
```json
{
  "reservationIdx": 102,
  "adminNote": "장비는 행사 시작 30분 전까지 준비 완료 예정입니다."
}
```

**필드 설명:**
- `reservationIdx` (integer, required): 승인할 예약 ID
- `adminNote` (string, optional): 관리자 메모 (사용자에게 전달됨)

**Response:**
```json
{
  "success": true,
  "message": "예약이 승인되었습니다.",
  "data": {
    "reservationIdx": 102,
    "facilityIdx": 1,
    "facilityName": "대강당",
    "userCode": "USER001",
    "userName": "홍길동",
    "startTime": "2025-10-15T09:00:00",
    "endTime": "2025-10-15T12:00:00",
    "partySize": 50,
    "purpose": "신입생 오리엔테이션",
    "requestedEquipment": "빔프로젝터, 마이크, 음향장비",
    "status": "APPROVED",
    "adminNote": "장비는 행사 시작 30분 전까지 준비 완료 예정입니다.",
    "rejectionReason": null,
    "approvedBy": "ADMIN001",
    "approvedAt": "2025-10-10T16:00:00",
    "createdAt": "2025-10-10T14:30:00"
  }
}
```

**승인 시 자동 검증:**
- ✅ 해당 시간대에 다른 승인된 예약과 충돌하는지 확인
- ✅ 시설 차단 기간과 겹치는지 확인
- ❌ 충돌 발견 시 승인 불가 에러 반환

---

### 3.4 예약 반려

```http
POST /api/admin/reservations/reject
```

**Headers:**
```
Authorization: Bearer {ADMIN_JWT_TOKEN}
Content-Type: application/json
```

**Request Body:**
```json
{
  "reservationIdx": 103,
  "rejectionReason": "해당 날짜에 시설 정기 점검이 예정되어 있습니다."
}
```

**필드 설명:**
- `reservationIdx` (integer, required): 반려할 예약 ID
- `rejectionReason` (string, required): 반려 사유 (필수)

**Response:**
```json
{
  "success": true,
  "message": "예약이 반려되었습니다.",
  "data": {
    "reservationIdx": 103,
    "facilityIdx": 1,
    "facilityName": "대강당",
    "userCode": "USER002",
    "userName": "김철수",
    "startTime": "2025-10-16T14:00:00",
    "endTime": "2025-10-16T18:00:00",
    "partySize": 100,
    "purpose": "학술 세미나",
    "requestedEquipment": "마이크, 음향장비",
    "status": "REJECTED",
    "adminNote": null,
    "rejectionReason": "해당 날짜에 시설 정기 점검이 예정되어 있습니다.",
    "approvedBy": null,
    "approvedAt": null,
    "createdAt": "2025-10-10T15:00:00"
  }
}
```

---

### 3.5 예약 통계 조회

```http
POST /api/admin/reservations/stats?startDate={startDate}&endDate={endDate}
```

**Query Parameters:**
- `startDate` (ISO 8601 DateTime, optional): 통계 시작 일자
- `endDate` (ISO 8601 DateTime, optional): 통계 종료 일자
- 둘 다 생략 시 전체 기간 통계

**Headers:**
```
Authorization: Bearer {ADMIN_JWT_TOKEN}
```

**Example:**
```http
POST /api/admin/reservations/stats?startDate=2025-10-01T00:00:00&endDate=2025-10-31T23:59:59
```

**Response:**
```json
{
  "success": true,
  "message": "통계 정보를 조회했습니다.",
  "data": {
    "totalReservations": 150,
    "pendingCount": 5,
    "approvedCount": 100,
    "rejectedCount": 20,
    "cancelledCount": 15,
    "completedCount": 10
  }
}
```

---

## 4. 공통 데이터 모델

### 4.1 FacilityDto (시설 정보)

```typescript
interface FacilityDto {
  facilityIdx: number;          // 시설 ID
  facilityName: string;          // 시설명
  facilityType: FacilityType;    // 시설 유형
  location: string;              // 위치
  capacity: number;              // 수용 인원
  description: string;           // 설명
  imageUrl: string;              // 이미지 URL
  operatingHours: string;        // 운영 시간
  isActive: boolean;             // 활성 상태
  requiresApproval: boolean;     // 승인 필요 여부
  createdAt: string;             // 생성 일시 (ISO 8601)
}
```

### 4.2 ReservationDto (예약 정보)

```typescript
interface ReservationDto {
  reservationIdx: number;        // 예약 ID
  facilityIdx: number;           // 시설 ID
  facilityName: string;          // 시설명
  userCode: string;              // 사용자 코드
  userName: string;              // 사용자 이름
  startTime: string;             // 시작 시간 (ISO 8601)
  endTime: string;               // 종료 시간 (ISO 8601)
  partySize?: number;            // 사용 인원
  purpose: string;               // 사용 목적
  requestedEquipment?: string;   // 요청 장비
  status: ReservationStatus;     // 예약 상태
  adminNote?: string;            // 관리자 메모
  rejectionReason?: string;      // 반려 사유
  approvedBy?: string;           // 승인자 ID
  approvedAt?: string;           // 승인 일시 (ISO 8601)
  createdAt: string;             // 생성 일시 (ISO 8601)
}
```

### 4.3 FacilityAvailabilityDto (가용성 정보)

```typescript
interface FacilityAvailabilityDto {
  facilityIdx: number;           // 시설 ID
  facilityName: string;          // 시설명
  isAvailable: boolean;          // 예약 가능 여부
  conflictingReservations: {     // 충돌하는 예약 목록
    startTime: string;           // 충돌 시작 시간
    endTime: string;             // 충돌 종료 시간
  }[];
}
```

### 4.4 Enum 타입

#### FacilityType (시설 유형)
```typescript
enum FacilityType {
  AUDITORIUM = "AUDITORIUM",           // 대강당
  MEETING_ROOM = "MEETING_ROOM",       // 회의실
  SEMINAR_ROOM = "SEMINAR_ROOM",       // 세미나실
  LAB = "LAB",                         // 실험실
  GYM = "GYM",                         // 체육관
  OUTDOOR = "OUTDOOR",                 // 야외 공간
  OTHER = "OTHER"                      // 기타
}
```

#### ReservationStatus (예약 상태)
```typescript
enum ReservationStatus {
  PENDING = "PENDING",         // 승인 대기
  APPROVED = "APPROVED",       // 승인됨
  REJECTED = "REJECTED",       // 반려됨
  CANCELLED = "CANCELLED",     // 취소됨
  COMPLETED = "COMPLETED"      // 완료됨 (사용 완료)
}
```

---

## 5. 에러 응답

### 5.1 공통 에러 형식

```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null
}
```

### 5.2 주요 에러 케이스

#### 인증 실패 (401)
```json
{
  "success": false,
  "message": "인증이 필요합니다.",
  "data": null
}
```

#### 권한 없음 (403)
```json
{
  "success": false,
  "message": "접근 권한이 없습니다.",
  "data": null
}
```

#### 리소스 없음 (404)
```json
{
  "success": false,
  "message": "시설을 찾을 수 없습니다.",
  "data": null
}
```

#### 검증 실패 (400)
```json
{
  "success": false,
  "message": "시작 시간은 현재 시간 이후여야 합니다.",
  "data": null
}
```

#### 날짜 제약 위반 (400)
```json
{
  "success": false,
  "message": "예약은 같은 날짜 내에서만 가능합니다. 여러 날에 걸친 예약이 필요한 경우 각 날짜별로 따로 신청해주세요.",
  "data": null
}
```

#### 예약 충돌 (400)
```json
{
  "success": false,
  "message": "해당 시간에는 이미 다른 예약이 존재합니다. 충돌 시간: 2025-10-15T10:00:00 ~ 2025-10-15T11:00:00",
  "data": null
}
```

#### 시설 차단 기간 (400)
```json
{
  "success": false,
  "message": "해당 시설은 2025-10-16T00:00:00부터 2025-10-17T23:59:59까지 예약이 불가합니다. 사유: 시설 정기 점검",
  "data": null
}
```

---

## 6. 예약 플로우 시나리오

### 6.1 즉시 승인 시설 예약 플로우

```
1. 사용자: GET /api/facilities
   → 시설 목록 확인

2. 사용자: POST /api/facilities/2
   → 소회의실 A 상세 정보 확인
   → requiresApproval: false 확인

3. 사용자: POST /api/facilities/2/availability?startTime=...&endTime=...
   → 가용성 실시간 체크
   → isAvailable: true 확인

4. 사용자: POST /api/reservations
   → 예약 생성 요청

5. 백엔드: 
   ① 기본 검증 (날짜, 시간, 정책)
   ② 정책 조회 → requiresApproval: false
   ③ 비관적 락 획득 (동시성 제어)
   ④ 가용성 재확인 (race condition 방지)
   ⑤ 예약 생성 (status: APPROVED)
   ⑥ 락 해제

6. 응답: "예약이 자동으로 승인되었습니다."
   → status: APPROVED
   → approvedBy: SYSTEM

✅ 완료: 즉시 사용 가능
```

### 6.2 승인 필요 시설 예약 플로우

```
1. 사용자: GET /api/facilities
   → 시설 목록 확인

2. 사용자: POST /api/facilities/1
   → 대강당 상세 정보 확인
   → requiresApproval: true 확인

3. 사용자: POST /api/facilities/1/availability?startTime=...&endTime=...
   → 가용성 실시간 체크
   → isAvailable: true 확인

4. 사용자: POST /api/reservations
   → 예약 생성 요청

5. 백엔드:
   ① 기본 검증 (날짜, 시간, 정책)
   ② 정책 조회 → requiresApproval: true
   ③ 락 없이 빠르게 처리 (승인 시 충돌 체크)
   ④ 차단 기간만 확인
   ⑤ 예약 생성 (status: PENDING)

6. 응답: "예약이 생성되었습니다. 관리자 승인 대기 중입니다."
   → status: PENDING

7. 관리자: POST /api/admin/reservations/pending
   → 승인 대기 목록 확인

8. 관리자: POST /api/admin/reservations/approve
   → 예약 승인 (이 시점에 충돌 재확인)

9. 백엔드:
   ① 예약 충돌 검증
   ② 차단 기간 검증
   ③ status: APPROVED 업데이트
   ④ approvedBy, approvedAt 기록

10. 사용자: POST /api/reservations/my
    → 내 예약 확인
    → status: APPROVED 확인

✅ 완료: 승인 후 사용 가능
```

### 6.3 여러 날 연속 예약 플로우

```
사용자가 3일 연속 예약을 원하는 경우:

옵션 1: 수동으로 3번 신청
1. POST /api/reservations (2025-10-15)
2. POST /api/reservations (2025-10-16)
3. POST /api/reservations (2025-10-17)

옵션 2: 프론트엔드 자동화
1. 사용자가 날짜 배열 선택: [2025-10-15, 2025-10-16, 2025-10-17]
2. 프론트엔드가 루프로 각 날짜에 대해 POST 요청
3. 각 요청의 성공/실패 결과를 사용자에게 표시

예시 코드는 consecutive-time-only-policy.md 참조
```

---

## 7. 성능 최적화 포인트

### 7.1 동시성 제어 최적화

**즉시 승인 시설:**
- 비관적 락 사용 (`SELECT ... FOR UPDATE`)
- 동시 예약 요청 시 순차 처리로 데이터 무결성 보장

**승인 필요 시설:**
- 락 없이 빠르게 PENDING 상태로 저장
- 승인 시점에만 충돌 검증
- 80-90% 성능 향상

### 7.2 N+1 쿼리 방지

예약 목록 조회 시 배치 페치 사용:
```java
// ❌ N+1 문제 (100개 예약 → 201개 쿼리)
for (reservation : reservations) {
  facility = facilityRepository.findById(...)
  user = userRepository.findByUserCode(...)
}

// ✅ 배치 페치 (100개 예약 → 3개 쿼리)
facilities = facilityRepository.findAllById(facilityIds)
users = userRepository.findAllByUserCodeIn(userCodes)
```

### 7.3 중복 쿼리 제거

예약 생성 시 시설 정보 재사용:
```java
// ❌ 중복 조회
facility = findById(facilityIdx)           // 1번 조회
availability = checkAvailability(...)
  facility = findById(facilityIdx)         // 2번 조회 (중복!)

// ✅ 한 번만 조회
facility = findById(facilityIdx)           // 1번 조회
availability = checkAvailabilityWithFacility(facility)  // 재사용
```

---

## 8. 테스트 가이드

### 8.1 Postman/Insomnia Collection

**환경 변수 설정:**
```json
{
  "baseUrl": "http://localhost:8080/api",
  "userToken": "{{USER_JWT_TOKEN}}",
  "adminToken": "{{ADMIN_JWT_TOKEN}}"
}
```

### 8.2 주요 테스트 케이스

#### TC-001: 즉시 승인 시설 예약 (정상)
```
POST {{baseUrl}}/reservations
Authorization: Bearer {{userToken}}

{
  "facilityIdx": 2,
  "startTime": "2025-10-15T09:00:00",
  "endTime": "2025-10-15T12:00:00",
  "purpose": "테스트"
}

Expected: 201, status: APPROVED
```

#### TC-002: 승인 필요 시설 예약 (정상)
```
POST {{baseUrl}}/reservations
Authorization: Bearer {{userToken}}

{
  "facilityIdx": 1,
  "startTime": "2025-10-15T09:00:00",
  "endTime": "2025-10-15T12:00:00",
  "purpose": "테스트"
}

Expected: 201, status: PENDING
```

#### TC-003: 날짜 제약 위반 (실패)
```
POST {{baseUrl}}/reservations
Authorization: Bearer {{userToken}}

{
  "facilityIdx": 1,
  "startTime": "2025-10-15T14:00:00",
  "endTime": "2025-10-16T10:00:00",  // 다음 날
  "purpose": "테스트"
}

Expected: 400, message: "예약은 같은 날짜 내에서만 가능합니다..."
```

#### TC-004: 예약 충돌 (실패)
```
1. POST {{baseUrl}}/reservations (09:00-12:00) → 성공
2. POST {{baseUrl}}/reservations (10:00-13:00) → 실패 (충돌)

Expected: 400, message: "해당 시간에는 이미 다른 예약이 존재합니다..."
```

#### TC-005: 관리자 승인
```
POST {{baseUrl}}/admin/reservations/approve
Authorization: Bearer {{adminToken}}

{
  "reservationIdx": 102,
  "adminNote": "승인합니다."
}

Expected: 200, status: APPROVED
```

---

## 9. 프론트엔드 통합 가이드

### 9.1 TypeScript 타입 정의

```typescript
// src/types/facility.ts

export interface Facility {
  facilityIdx: number;
  facilityName: string;
  facilityType: FacilityType;
  location: string;
  capacity: number;
  description: string;
  imageUrl: string;
  operatingHours: string;
  isActive: boolean;
  requiresApproval: boolean;
  createdAt: string;
}

export interface Reservation {
  reservationIdx: number;
  facilityIdx: number;
  facilityName: string;
  userCode: string;
  userName: string;
  startTime: string;
  endTime: string;
  partySize?: number;
  purpose: string;
  requestedEquipment?: string;
  status: ReservationStatus;
  adminNote?: string;
  rejectionReason?: string;
  approvedBy?: string;
  approvedAt?: string;
  createdAt: string;
}

export enum FacilityType {
  AUDITORIUM = "AUDITORIUM",
  MEETING_ROOM = "MEETING_ROOM",
  SEMINAR_ROOM = "SEMINAR_ROOM",
  LAB = "LAB",
  GYM = "GYM",
  OUTDOOR = "OUTDOOR",
  OTHER = "OTHER"
}

export enum ReservationStatus {
  PENDING = "PENDING",
  APPROVED = "APPROVED",
  REJECTED = "REJECTED",
  CANCELLED = "CANCELLED",
  COMPLETED = "COMPLETED"
}
```

### 9.2 API 클라이언트 예시

```typescript
// src/api/facilityAPI.ts

import axios from 'axios';
import type { Facility, Reservation } from '@/types/facility';

const BASE_URL = '/api';

// 시설 목록 조회
export const getAllFacilities = async (): Promise<Facility[]> => {
  const response = await axios.post(`${BASE_URL}/facilities`, {});
  return response.data.data;
};

// 예약 생성
export const createReservation = async (data: {
  facilityIdx: number;
  startTime: string;
  endTime: string;
  partySize?: number;
  purpose: string;
  requestedEquipment?: string;
}): Promise<Reservation> => {
  const response = await axios.post(`${BASE_URL}/reservations`, data, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  });
  return response.data.data;
};

// 내 예약 목록 조회
export const getMyReservations = async (): Promise<Reservation[]> => {
  const response = await axios.post(`${BASE_URL}/reservations/my`, {}, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  });
  return response.data.data;
};

// 예약 취소
export const cancelReservation = async (reservationIdx: number): Promise<void> => {
  await axios.delete(`${BASE_URL}/reservations/${reservationIdx}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  });
};

// 가용성 체크
export const checkAvailability = async (
  facilityIdx: number,
  startTime: string,
  endTime: string
) => {
  const response = await axios.post(
    `${BASE_URL}/facilities/${facilityIdx}/availability`,
    {},
    {
      params: { startTime, endTime }
    }
  );
  return response.data.data;
};

function getToken(): string {
  return localStorage.getItem('accessToken') || '';
}
```

### 9.3 날짜 검증 헬퍼

```typescript
// src/utils/dateValidator.ts

export const validateSameDay = (
  startTime: Date | string,
  endTime: Date | string
): boolean => {
  const start = new Date(startTime);
  const end = new Date(endTime);
  
  return (
    start.getFullYear() === end.getFullYear() &&
    start.getMonth() === end.getMonth() &&
    start.getDate() === end.getDate()
  );
};

export const formatDateTime = (date: Date): string => {
  return date.toISOString().slice(0, 19);
};

// 사용 예시
const startTime = new Date('2025-10-15T09:00:00');
const endTime = new Date('2025-10-15T12:00:00');

if (!validateSameDay(startTime, endTime)) {
  alert('예약은 같은 날짜 내에서만 가능합니다.');
  return;
}

const formattedStart = formatDateTime(startTime); // "2025-10-15T09:00:00"
```

---

## 10. 보안 고려사항

### 10.1 인증/인가

- **유저 API:** JWT 토큰 필수
- **관리자 API:** 관리자 JWT 토큰 필수
- 토큰 만료 시간: 1시간 (갱신 토큰 사용 권장)

### 10.2 입력 검증

- SQL Injection 방지: Prepared Statement 사용
- XSS 방지: 사용자 입력 이스케이프
- CSRF 방지: CSRF 토큰 사용 (필요 시)

### 10.3 Rate Limiting

권장 설정:
- 예약 생성: 1분에 5회
- 조회 API: 1분에 60회

---

## 11. 문서 히스토리

| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2025-10-10 | 초안 작성 | System |

---

## 12. 참고 문서

- [연속된 시간대만 예약 가능 정책](./consecutive-time-only-policy.md)
- [시설 예약 최적화 보고서](./facility-reservation-optimization-2025-10-10.md)
- [시설 예약 사용자 플로우](./facility-reservation-user-flow-complete.md)
- [UI 누락 기능 분석](./UI_MISSING_FEATURES_ANALYSIS.md)
