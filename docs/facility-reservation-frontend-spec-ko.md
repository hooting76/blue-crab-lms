# 시설 예약 프론트엔드 스펙 문서

## 1. 범위

시설 조회, 가용성 확인, 예약 생성, 내 예약 조회, 승인/대기 중 예약 취소를 위한 사용자용 UI 플로우 구축. 관리자 페이지는 범위 외.

## 2. 아키텍처 개요

- **런타임**: React (Vite). 메인 진입점 `src/App.jsx`는 `currentPage` 상태를 통해 시설 관련 화면을 렌더링하며, 현재 `component/common/Facilities/FacilityRequest.jsx`가 목업 형태로 연결돼 있음
- **상태 관리**: 개별 컴포넌트의 로컬 상태와 커스텀 훅(`component`/`src/api` 하위에 존재하는 패턴)에 맞춰 확장 예정
- **인증 컨텍스트**: 현재 JWT 처리 방식 사용 (토큰 클라이언트 측 저장). 모든 시설 API는 인증된 사용자 필요
- **API Base URL**: `/api/...` (same origin)

## 3. UI 플로우

현재 `component/common/Facilities/FacilityRequest.jsx`는 시설 선택/사유 선택을 위한 기본 목업만 제공한다. 아래 요구사항에 맞춰 컴포넌트를 확장하고 필요한 서브 컴포넌트·훅을 추가 구현한다.

### 3.1 시설 목록 & 가용성 확인

1. 로드 시 `POST /api/facilities`로 활성 시설 목록 조회
2. 카드 형태로 표시: 시설명, 정원, 위치, 승인 필요 여부, 차단 정보 (`isBlocked`, `blockReason`)
3. 시설 유형별 필터링: `POST /api/facilities/type/{facilityType}` (선택적 기능)
4. 선택한 시설에 대해 다음 정보 수집:
   - 날짜 (단일 날짜, 백엔드에서 강제)
   - 시작/종료 시간 (형식: `yyyy-MM-dd HH:mm:ss`)
   - 인원수 (1명 이상 정수)
   - 목적 / 요청 장비 (선택적 텍스트 입력)
5. 제출 전 "가용성 확인" 버튼으로 `POST /api/facilities/{facilityIdx}/availability?startTime=...&endTime=...` 호출
   - `isAvailable`이 false면 충돌 목록 표시

### 3.2 예약 생성

1. 폼을 `POST /api/reservations`로 제출, 페이로드:
   ```json
   {
     "facilityIdx": 1,
     "startTime": "2025-01-08 10:00:00",
     "endTime": "2025-01-08 12:00:00",
     "partySize": 4,
     "purpose": "스터디 모임",
     "requestedEquipment": "빔프로젝터"
   }
   ```
2. 응답: `ApiResponse<ReservationDto>`
   - `status` 필드는 한글 설명 반환 (`대기중`, `승인됨` 등)
   - 메시지는 즉시 승인 vs 대기 중 표시
3. 성공 시:
   - 메시지를 화면에 표시 (예: 인라인 완료 안내 또는 `alert`)
   - 로컬 "내 예약" 목록에 추가하거나 재조회
4. 유효성 검증 오류 처리 (HTTP 400, 메시지 문자열). 인라인 또는 전역 에러 메시지 표시

### 3.3 내 예약 목록

1. `POST /api/reservations/my`로 조회
   - 필터링: `POST /api/reservations/my/status/{status}` (`status` 옵션: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED`)
2. 카드/테이블로 주요 필드 표시:
   - 시설명
   - 시간 범위
   - 인원수
   - 상태 (색상 뱃지 사용)
   - 관리자 노트 / 반려 사유 (있을 경우)
   - 승인자 / 승인 시간 (있을 경우)
3. "상세 보기" 액션: `POST /api/reservations/{reservationIdx}` (선택적)

### 3.4 예약 취소

1. `PENDING` 또는 `APPROVED` 상태는 "취소" 버튼 표시
2. 클릭 시:
   - 취소 마감시간을 요약한 확인 모달 표시
   - `DELETE /api/reservations/{reservationIdx}` 호출
3. 성공 → 성공 메시지 표기 + 상태를 `취소됨`으로 로컬 업데이트
4. 실패 시나리오:
   - 취소 마감시간 지남 (HTTP 400, 메시지: "예약 시작 XX시간 전까지만 취소 가능합니다..."). 사용자에게 메시지 표시
   - 소유자 아님 / 잘못된 상태 (HTTP 400 / 403). 일반 에러 표시

## 4. API 요약

| 사용 사례 | 메서드 & 경로 | 비고 |
| --- | --- | --- |
| 시설 목록 | `POST /api/facilities` | 인증 필요 |
| 시설 가용성 | `POST /api/facilities/{facilityIdx}/availability` | 쿼리 파라미터 (`startTime`, `endTime`), 형식 `yyyy-MM-dd HH:mm:ss` |
| 일일 일정 조회 | `POST /api/facilities/{facilityIdx}/daily-schedule` | 날짜 파라미터 (`date`), 형식 `yyyy-MM-dd`. 09:00~20:00 1시간 단위 시간대별 예약 상태 반환 |
| 예약 생성 | `POST /api/reservations` | 본문에 명시된 대로; `ReservationDto` 반환 |
| 내 예약 목록 | `POST /api/reservations/my` | 최신순 정렬 |
| 상태별 내 예약 | `POST /api/reservations/my/status/{status}` | `status`는 enum 이름과 일치해야 함 (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED`) |
| 예약 상세 | `POST /api/reservations/{reservationIdx}` | 선택적 상세 뷰 |
| 예약 취소 | `DELETE /api/reservations/{reservationIdx}` | `PENDING`/`APPROVED`만 허용 |

### API 상세 스펙 (엔드포인트별)

#### 1. 시설 목록 조회
```
POST /api/facilities
Headers: Authorization: Bearer {JWT_TOKEN}
Body: (없음)

성공 응답 (200 OK):
{
  "success": true,
  "message": "시설 목록 조회 성공",
  "data": [
    {
      "facilityIdx": 1,
      "facilityName": "세미나실 A",
      "facilityType": "SEMINAR_ROOM",
      "facilityDesc": "20인 수용 가능한 세미나실",
      "capacity": 20,
      "location": "본관 3층",
      "defaultEquipment": "빔프로젝터, 화이트보드",
      "isActive": true,
      "requiresApproval": true,
      "isBlocked": false,
      "blockReason": null
    }
  ]
}
```

#### 2. 시설 가용성 확인
```
POST /api/facilities/{facilityIdx}/availability
      ?startTime=2025-01-10 14:00:00
      &endTime=2025-01-10 16:00:00
Headers: Authorization: Bearer {JWT_TOKEN}

성공 응답 - 가용 (200 OK):
{
  "success": true,
  "message": "해당 시간에 예약 가능합니다.",
  "data": {
    "facilityIdx": 1,
    "facilityName": "세미나실 A",
    "isAvailable": true,
    "conflictingReservations": []
  }
}

성공 응답 - 충돌 있음 (200 OK):
{
  "success": true,
  "message": "해당 시간에 이미 다른 예약이 존재합니다.",
  "data": {
    "facilityIdx": 1,
    "facilityName": "세미나실 A",
    "isAvailable": false,
    "conflictingReservations": [
      {
        "startTime": "2025-01-10 14:00:00",
        "endTime": "2025-01-10 16:00:00"
      }
    ]
  }
}
```

#### 3. 일일 일정 조회
```
POST /api/facilities/{facilityIdx}/daily-schedule?date=2025-01-10
Headers: Authorization: Bearer {JWT_TOKEN}

성공 응답 (200 OK):
{
  "success": true,
  "message": "일일 일정 조회 성공",
  "data": {
    "facilityIdx": 1,
    "facilityName": "세미나실 A",
    "date": "2025-01-10",
    "timeSlots": [
      { "hour": "09:00", "isAvailable": true },
      { "hour": "10:00", "isAvailable": false },
      { "hour": "11:00", "isAvailable": false },
      { "hour": "12:00", "isAvailable": true },
      { "hour": "13:00", "isAvailable": true },
      { "hour": "14:00", "isAvailable": true },
      { "hour": "15:00", "isAvailable": true },
      { "hour": "16:00", "isAvailable": false },
      { "hour": "17:00", "isAvailable": true },
      { "hour": "18:00", "isAvailable": true },
      { "hour": "19:00", "isAvailable": true },
      { "hour": "20:00", "isAvailable": true }
    ]
  }
}
```

#### 4. 예약 생성
```
POST /api/reservations
Headers: Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

요청 Body:
{
  "facilityIdx": 1,
  "startTime": "2025-01-10 14:00:00",
  "endTime": "2025-01-10 16:00:00",
  "partySize": 4,
  "purpose": "스터디 모임",
  "requestedEquipment": "빔프로젝터"
}

성공 응답 - 자동 승인 (200 OK):
{
  "success": true,
  "message": "예약이 생성되었습니다. (자동 승인)",
  "data": {
    "reservationIdx": 123,
    "facilityIdx": 1,
    "facilityName": "세미나실 A",
    "userCode": "USER001",
    "userName": "홍길동",
    "startTime": "2025-01-10 14:00:00",
    "endTime": "2025-01-10 16:00:00",
    "partySize": 4,
    "purpose": "스터디 모임",
    "requestedEquipment": "빔프로젝터",
    "status": "승인됨",
    "adminNote": null,
    "rejectionReason": null,
    "approvedBy": "SYSTEM",
    "approvedAt": "2025-01-08 10:00:00",
    "createdAt": "2025-01-08 10:00:00"
  }
}

성공 응답 - 승인 대기 (200 OK):
{
  "success": true,
  "message": "예약이 생성되었습니다. 관리자 승인을 기다리고 있습니다.",
  "data": {
    "reservationIdx": 124,
    "facilityIdx": 2,
    "facilityName": "대회의실",
    "userCode": "USER001",
    "userName": "홍길동",
    "startTime": "2025-01-10 14:00:00",
    "endTime": "2025-01-10 16:00:00",
    "partySize": 15,
    "purpose": "팀 회의",
    "requestedEquipment": null,
    "status": "대기중",
    "adminNote": null,
    "rejectionReason": null,
    "approvedBy": null,
    "approvedAt": null,
    "createdAt": "2025-01-08 10:00:00"
  }
}

실패 응답 - 정원 초과 (400 Bad Request):
{
  "success": false,
  "message": "요청 인원(25명)이 시설 정원(20명)을 초과합니다.",
  "data": null
}
```

#### 5. 내 예약 목록 조회
```
POST /api/reservations/my
Headers: Authorization: Bearer {JWT_TOKEN}

성공 응답 (200 OK):
{
  "success": true,
  "message": "예약 목록 조회 성공",
  "data": [
    {
      "reservationIdx": 124,
      "facilityIdx": 2,
      "facilityName": "대회의실",
      "userCode": "USER001",
      "userName": "홍길동",
      "startTime": "2025-01-10 14:00:00",
      "endTime": "2025-01-10 16:00:00",
      "partySize": 15,
      "purpose": "팀 회의",
      "requestedEquipment": null,
      "status": "대기중",
      "adminNote": null,
      "rejectionReason": null,
      "approvedBy": null,
      "approvedAt": null,
      "createdAt": "2025-01-08 10:00:00"
    },
    {
      "reservationIdx": 123,
      "facilityIdx": 1,
      "facilityName": "세미나실 A",
      "userCode": "USER001",
      "userName": "홍길동",
      "startTime": "2025-01-08 10:00:00",
      "endTime": "2025-01-08 12:00:00",
      "partySize": 4,
      "purpose": "스터디 모임",
      "requestedEquipment": "빔프로젝터",
      "status": "승인됨",
      "adminNote": "승인되었습니다.",
      "rejectionReason": null,
      "approvedBy": "ADMIN",
      "approvedAt": "2025-01-07 15:00:00",
      "createdAt": "2025-01-07 14:00:00"
    }
  ]
}
```

#### 6. 상태별 내 예약 조회
```
POST /api/reservations/my/status/{status}
Headers: Authorization: Bearer {JWT_TOKEN}

예시: POST /api/reservations/my/status/PENDING

성공 응답 (200 OK):
{
  "success": true,
  "message": "예약 목록 조회 성공",
  "data": [
    {
      "reservationIdx": 124,
      "facilityName": "대회의실",
      "startTime": "2025-01-10 14:00:00",
      "endTime": "2025-01-10 16:00:00",
      "status": "대기중",
      ...
    }
  ]
}
```

#### 7. 예약 상세 조회
```
POST /api/reservations/{reservationIdx}
Headers: Authorization: Bearer {JWT_TOKEN}

예시: POST /api/reservations/123

성공 응답 (200 OK):
{
  "success": true,
  "message": "예약 상세 조회 성공",
  "data": {
    "reservationIdx": 123,
    "facilityIdx": 1,
    "facilityName": "세미나실 A",
    "userCode": "USER001",
    "userName": "홍길동",
    "startTime": "2025-01-08 10:00:00",
    "endTime": "2025-01-08 12:00:00",
    "partySize": 4,
    "purpose": "스터디 모임",
    "requestedEquipment": "빔프로젝터",
    "status": "승인됨",
    "adminNote": "승인되었습니다.",
    "rejectionReason": null,
    "approvedBy": "ADMIN",
    "approvedAt": "2025-01-07 15:00:00",
    "createdAt": "2025-01-07 14:00:00"
  }
}

실패 응답 - 예약 없음 (404 Not Found):
{
  "success": false,
  "message": "예약을 찾을 수 없습니다.",
  "data": null
}
```

#### 8. 예약 취소
```
DELETE /api/reservations/{reservationIdx}
Headers: Authorization: Bearer {JWT_TOKEN}

예시: DELETE /api/reservations/123

성공 응답 (200 OK):
{
  "success": true,
  "message": "예약이 취소되었습니다.",
  "data": null
}

실패 응답 - 취소 마감시간 지남 (400 Bad Request):
{
  "success": false,
  "message": "예약 시작 24시간 전까지만 취소 가능합니다. 취소 마감 시간: 2025-01-10 10:00",
  "data": null
}

실패 응답 - 권한 없음 (403 Forbidden):
{
  "success": false,
  "message": "본인의 예약만 취소할 수 있습니다.",
  "data": null
}

실패 응답 - 이미 취소됨 (400 Bad Request):
{
  "success": false,
  "message": "이미 취소된 예약입니다.",
  "data": null
}
```

## 4.1 API 응답 형식 및 HTTP 상태 코드

### 응답 구조

모든 API 응답은 다음 공통 구조를 따릅니다:

```typescript
interface ApiResponse<T> {
  success: boolean;      // 성공 여부
  message: string;       // 사용자 표시용 메시지
  data: T | null;        // 응답 데이터 (타입은 API에 따라 다름)
}
```

### HTTP 상태 코드

| 상태 코드 | 의미 | 발생 시나리오 |
| --- | --- | --- |
| **200 OK** | 요청 성공 | 정상적인 모든 성공 응답 |
| **400 Bad Request** | 유효성 검증 실패 | 정원 초과, 과거 날짜, 취소 마감시간 지남, 주말 제한 등 |
| **401 Unauthorized** | 인증 실패 | JWT 토큰 없음/만료/유효하지 않음 |
| **403 Forbidden** | 권한 없음 | 본인의 예약이 아닌 경우 (취소 시) |
| **404 Not Found** | 리소스 없음 | 존재하지 않는 시설/예약 |
| **500 Internal Server Error** | 서버 오류 | 예상치 못한 서버 에러 |

### 성공 응답 예시

#### 시설 목록 조회 성공
```json
{
  "success": true,
  "message": "시설 목록 조회 성공",
  "data": [
    {
      "facilityIdx": 1,
      "facilityName": "세미나실 A",
      "facilityType": "SEMINAR_ROOM",
      "capacity": 20,
      "location": "본관 3층",
      "requiresApproval": true,
      "isBlocked": false
    }
  ]
}
```

#### 가용성 확인 - 가용한 경우
```json
{
  "success": true,
  "message": "해당 시간에 예약 가능합니다.",
  "data": {
    "facilityIdx": 1,
    "facilityName": "세미나실 A",
    "isAvailable": true,
    "conflictingReservations": []
  }
}
```

#### 가용성 확인 - 충돌이 있는 경우
```json
{
  "success": true,
  "message": "해당 시간에 이미 다른 예약이 존재합니다.",
  "data": {
    "facilityIdx": 1,
    "facilityName": "세미나실 A",
    "isAvailable": false,
    "conflictingReservations": [
      {
        "startTime": "2025-01-08 10:00:00",
        "endTime": "2025-01-08 12:00:00"
      },
      {
        "startTime": "2025-01-08 13:00:00",
        "endTime": "2025-01-08 15:00:00"
      }
    ]
  }
}
```

#### 예약 생성 성공 - 즉시 승인
```json
{
  "success": true,
  "message": "예약이 생성되었습니다. (자동 승인)",
  "data": {
    "reservationIdx": 123,
    "facilityIdx": 1,
    "facilityName": "세미나실 A",
    "userCode": "USER001",
    "userName": "홍길동",
    "startTime": "2025-01-08 10:00:00",
    "endTime": "2025-01-08 12:00:00",
    "partySize": 4,
    "purpose": "스터디 모임",
    "requestedEquipment": "빔프로젝터",
    "status": "승인됨",
    "adminNote": null,
    "rejectionReason": null,
    "approvedBy": "SYSTEM",
    "approvedAt": "2025-01-08 09:30:00",
    "createdAt": "2025-01-08 09:30:00"
  }
}
```

#### 예약 생성 성공 - 승인 대기
```json
{
  "success": true,
  "message": "예약이 생성되었습니다. 관리자 승인을 기다리고 있습니다.",
  "data": {
    "reservationIdx": 124,
    "facilityIdx": 2,
    "facilityName": "대회의실",
    "userCode": "USER001",
    "userName": "홍길동",
    "startTime": "2025-01-10 14:00:00",
    "endTime": "2025-01-10 16:00:00",
    "partySize": 15,
    "purpose": "팀 회의",
    "requestedEquipment": null,
    "status": "대기중",
    "adminNote": null,
    "rejectionReason": null,
    "approvedBy": null,
    "approvedAt": null,
    "createdAt": "2025-01-08 09:35:00"
  }
}
```

#### 내 예약 목록 조회 성공
```json
{
  "success": true,
  "message": "예약 목록 조회 성공",
  "data": [
    {
      "reservationIdx": 124,
      "facilityName": "대회의실",
      "startTime": "2025-01-10 14:00:00",
      "endTime": "2025-01-10 16:00:00",
      "status": "대기중"
    },
    {
      "reservationIdx": 123,
      "facilityName": "세미나실 A",
      "startTime": "2025-01-08 10:00:00",
      "endTime": "2025-01-08 12:00:00",
      "status": "승인됨"
    }
  ]
}
```

#### 예약 취소 성공
```json
{
  "success": true,
  "message": "예약이 취소되었습니다.",
  "data": null
}
```

### 에러 응답 예시

#### 유효성 검증 실패 (HTTP 400)

**정원 초과**
```json
{
  "success": false,
  "message": "요청 인원(25명)이 시설 정원(20명)을 초과합니다.",
  "data": null
}
```

**과거 날짜 선택**
```json
{
  "success": false,
  "message": "시작 시간은 현재 시간 이후여야 합니다.",
  "data": null
}
```

**취소 마감시간 지남**
```json
{
  "success": false,
  "message": "예약 시작 24시간 전까지만 취소 가능합니다. 취소 마감 시간: 2025-01-10 10:00",
  "data": null
}
```

**주말 예약 불가**
```json
{
  "success": false,
  "message": "이 시설은 주말 예약이 허용되지 않습니다.",
  "data": null
}
```

**예약 횟수 초과**
```json
{
  "success": false,
  "message": "이 시설에 대한 예약 가능 횟수(3회)를 초과했습니다. 현재 활성 예약: 3건",
  "data": null
}
```

**시간대 충돌**
```json
{
  "success": false,
  "message": "해당 시간에는 이미 다른 예약이 존재합니다.",
  "data": null
}
```

#### 인증 실패 (HTTP 401)
```json
{
  "success": false,
  "message": "인증이 필요합니다. 다시 로그인해주세요.",
  "data": null
}
```

#### 권한 없음 (HTTP 403)
```json
{
  "success": false,
  "message": "본인의 예약만 취소할 수 있습니다.",
  "data": null
}
```

#### 리소스 없음 (HTTP 404)
```json
{
  "success": false,
  "message": "예약을 찾을 수 없습니다.",
  "data": null
}
```

### 프론트엔드 에러 처리 가이드

```javascript
async function handleApiCall(apiFunction, { onSuccess, onError } = {}) {
  try {
    const response = await apiFunction();

    // HTTP 상태 코드 확인
    if (!response.ok) {
      if (response.status === 401) {
        // 토큰 만료 - 로그인 페이지로 리다이렉트
        redirectToLogin();
        return;
      }

      // 에러 응답 파싱
      const errorData = await response.json();
      const message = errorData?.message || '요청 처리 중 오류가 발생했습니다.';
      onError?.(message);
      return null;
    }

    // 성공 응답 파싱
    const result = await response.json();

    if (result.success) {
      onSuccess?.(result.message, result.data);
      return result.data;
    } else {
      onError?.(result.message);
      return null;
    }

  } catch (error) {
    // 네트워크 오류 등
    const message = error?.message || '서버와 통신할 수 없습니다.';
    onError?.(message);
    return null;
  }
}
```

### ReservationDto 필드

```json
{
  "reservationIdx": 123,
  "facilityIdx": 1,
  "facilityName": "세미나실 A",
  "userCode": "USER001",
  "userName": "홍길동",
  "startTime": "2025-01-08 10:00:00",
  "endTime": "2025-01-08 12:00:00",
  "partySize": 4,
  "purpose": "스터디 모임",
  "requestedEquipment": "빔프로젝터",
  "status": "대기중",
  "adminNote": null,
  "rejectionReason": null,
  "approvedBy": null,
  "approvedAt": null,
  "createdAt": "2025-01-08 09:30:00"
}
```

**status 필드 값**: `"대기중"` | `"승인됨"` | `"반려됨"` | `"취소됨"` | `"완료됨"`

**중요**:
- JSON 응답에서 `status`는 한글 문자열로 반환됨 (`"대기중"`, `"승인됨"` 등)
- 상태별 필터링 시 경로에는 영어 enum 이름 사용 (`PENDING`, `APPROVED` 등)

### FacilityDto 필드

```json
{
  "facilityIdx": 1,
  "facilityName": "세미나실 A",
  "facilityType": "SEMINAR_ROOM",
  "facilityDesc": "20인 수용 가능한 세미나실",
  "capacity": 20,
  "location": "본관 3층",
  "defaultEquipment": "빔프로젝터, 화이트보드",
  "isActive": true,
  "requiresApproval": true,
  "isBlocked": false,
  "blockReason": null
}
```

### FacilityAvailabilityDto 필드

```json
{
  "facilityIdx": 1,
  "facilityName": "세미나실 A",
  "isAvailable": false,
  "conflictingReservations": [
    {
      "startTime": "2025-01-08 10:00:00",
      "endTime": "2025-01-08 12:00:00"
    }
  ]
}
```

## 5. 유효성 검증 & UX 요구사항

### 날짜/시간 검증
- **같은 날 선택 강제**: 시작/종료 시간이 같은 날짜여야 함
- **날짜 피커 제약**: `minDate = today`, `maxDate = today + policy.maxDaysInAdvance` (백엔드에서 시설 상세 조회 시 제공 가능)
- **초기 구현**: 제출 시 검증하고 백엔드 에러 메시지에 의존

### 시간 형식
- 백엔드 기대 형식: `yyyy-MM-dd HH:mm:ss`
- 피커 출력값을 제출 전 변환

### 인원수 검증
- 양의 정수여야 함
- 시설 `capacity`와 비교 (시설 목록에서 제공)
- **에러 메시지**: `"요청 인원(5명)이 시설 정원(4명)을 초과합니다."`

### 주말 제한
- `requiresApproval`이 false이거나 `isBlocked`가 true면 안내 텍스트 표시
- 주말 불가는 서버 측에서 강제; 백엔드 에러 메시지 표시
- **에러 메시지**: `"이 시설은 주말 예약이 허용되지 않습니다."`

### 취소 마감시간
- `ReservationDto` 조회 후 UI 표시용 마감시간 계산 (deadline = `startTime - policyHours`)
- 정책 시간은 응답에 포함되지 않음; UI는 에러 메시지에 의존
- **선택적 개선**: API 확장하여 `cancellationDeadlineHours` 제공
- **에러 메시지**: `"예약 시작 24시간 전까지만 취소 가능합니다. 취소 마감 시간: 2025-01-10 10:00"`

### 사용자당 예약 횟수 제한 (Phase 2)
- 시설별로 사용자당 최대 예약 수 제한 가능
- **에러 메시지**: `"이 시설에 대한 예약 가능 횟수(3회)를 초과했습니다. 현재 활성 예약: 3건"`

### 차단된 시설
- `isBlocked`가 true면 예약 버튼 비활성화하고 이유(`blockReason`) 표시

### 로딩 & 에러 상태
- fetch 대기 중 스피너 제공
- 백엔드 메시지용 인라인 에러 영역 표시

### 토큰 만료
- 기존 전역 핸들러 재사용 (401 시 자동 리다이렉트)

## 6. 컴포넌트 & 역할

| 컴포넌트 | 역할 |
| --- | --- |
| `component/common/Facilities/FacilityRequest.jsx` (기존) | 하드코딩된 셀렉터/폼을 교체하고, 시설 목록 조회·가용성 확인·예약 생성 호출 및 에러 표시에 사용 |
| `component/common/Facilities/MyFacilityRequests.jsx` (기존) | 현재 플레이스홀더만 존재. 내 예약 목록/필터/취소 UI를 실제로 렌더링하도록 확장 |
| `component/common/Facilities/FacilityReservationList.jsx` (신규) | 취소 버튼이 포함된 예약 목록 테이블/카드를 담당 (데스크톱 뷰) |
| `component/common/Facilities/FacilityReservationCard.jsx` (신규) | 모바일/반응형 개별 카드 레이아웃 |
| `component/common/Facilities/CancelReservationModal.jsx` (신규) | 취소 요청 전 확인 모달. 마감시간 안내 및 API 트리거 |
| `hook/UseFacilities.jsx` 또는 `hook/UseFacilities.js` (신규) | 시설 목록/유형별 목록을 fetch하고 로컬 캐싱 |
| `hook/UseFacilityReservations.jsx` (신규) | 내 예약 목록, 상태별 필터, 취소 API 호출 래핑 |

## 7. 에러 & 성공 메시지

### 기본 원칙
- 성공 시 `ApiResponse.message`를 화면에 노출 (예: 인라인 알림, 배너, 필요 시 `alert`)
- 에러 시 백엔드 메시지를 그대로 보여주고, 메시지가 없을 때는 `"요청 처리 중 오류가 발생했습니다."` 같은 공통 문구 사용

### 주요 에러 메시지 (백엔드에서 제공)

#### 기본 검증
- `"시작 시간은 현재 시간 이후여야 합니다."`
- `"종료 시간은 시작 시간 이후여야 합니다."`
- `"시설을 찾을 수 없습니다."`

#### Phase 1 검증 (정원 & 취소 마감)
- `"인원수는 최소 1명 이상이어야 합니다."`
- `"요청 인원(5명)이 시설 정원(4명)을 초과합니다."`
- `"예약 시작 24시간 전까지만 취소 가능합니다. 취소 마감 시간: 2025-01-10 10:00"`

#### Phase 2 검증 (예약 횟수 & 주말)
- `"이 시설에 대한 예약 가능 횟수(3회)를 초과했습니다. 현재 활성 예약: 3건"`
- `"이 시설은 주말 예약이 허용되지 않습니다."`

#### 가용성 & 충돌
- `"해당 시간에는 이미 다른 예약이 존재합니다."` (가용성 체크 시)

#### 권한 & 상태
- `"예약을 찾을 수 없습니다."` (잘못된 reservationIdx)
- `"본인의 예약만 취소할 수 있습니다."` (권한 없음)
- `"이미 취소된 예약입니다."` (중복 취소)

### 클라이언트 측 대체 메시지
프론트엔드에서 백엔드 메시지를 그대로 사용하되, 필요 시 다음 대체 메시지 준비:
- 가용성 충돌: `"해당 시간에는 이미 다른 예약이 존재합니다."`
- 주말 불가: `"이 시설은 주말 예약이 허용되지 않습니다."`
- 정원 초과: `"요청 인원이 시설 정원을 초과합니다."`

## 8. 데이터 구조 참조 (JavaScript/JSDoc)

### 예약 DTO 예시

```javascript
/**
 * @typedef {Object} ReservationDto
 * @property {number} reservationIdx - 예약 ID
 * @property {number} facilityIdx - 시설 ID
 * @property {string} facilityName - 시설명
 * @property {string} userCode - 사용자 코드
 * @property {string} userName - 사용자명
 * @property {string} startTime - 시작 시간 (형식: "yyyy-MM-dd HH:mm:ss")
 * @property {string} endTime - 종료 시간 (형식: "yyyy-MM-dd HH:mm:ss")
 * @property {number} partySize - 인원수
 * @property {string|null} purpose - 목적
 * @property {string|null} requestedEquipment - 요청 장비
 * @property {("대기중"|"승인됨"|"반려됨"|"취소됨"|"완료됨")} status - 예약 상태
 * @property {string|null} adminNote - 관리자 노트
 * @property {string|null} rejectionReason - 반려 사유
 * @property {string|null} approvedBy - 승인자
 * @property {string|null} approvedAt - 승인 시간
 * @property {string} createdAt - 생성 시간
 */

// 예시 객체
const reservationExample = {
  reservationIdx: 123,
  facilityIdx: 1,
  facilityName: "세미나실 A",
  userCode: "USER001",
  userName: "홍길동",
  startTime: "2025-01-08 10:00:00",
  endTime: "2025-01-08 12:00:00",
  partySize: 4,
  purpose: "스터디 모임",
  requestedEquipment: "빔프로젝터",
  status: "대기중",
  adminNote: null,
  rejectionReason: null,
  approvedBy: null,
  approvedAt: null,
  createdAt: "2025-01-08 09:30:00"
};
```

### 예약 생성 요청 예시

```javascript
/**
 * @typedef {Object} ReservationCreateRequest
 * @property {number} facilityIdx - 시설 ID
 * @property {string} startTime - 시작 시간 (형식: "yyyy-MM-dd HH:mm:ss")
 * @property {string} endTime - 종료 시간 (형식: "yyyy-MM-dd HH:mm:ss")
 * @property {number} partySize - 인원수
 * @property {string} [purpose] - 목적 (선택)
 * @property {string} [requestedEquipment] - 요청 장비 (선택)
 */

// 예시 요청 객체
const createRequest = {
  facilityIdx: 1,
  startTime: "2025-01-08 10:00:00",
  endTime: "2025-01-08 12:00:00",
  partySize: 4,
  purpose: "스터디 모임",
  requestedEquipment: "빔프로젝터"
};
```

### 시설 DTO 예시

```javascript
/**
 * @typedef {Object} FacilityDto
 * @property {number} facilityIdx - 시설 ID
 * @property {string} facilityName - 시설명
 * @property {string} facilityType - 시설 유형 (예: "SEMINAR_ROOM", "READING_ROOM")
 * @property {string} facilityDesc - 시설 설명
 * @property {number|null} capacity - 정원
 * @property {string} location - 위치
 * @property {string|null} defaultEquipment - 기본 장비
 * @property {boolean} isActive - 활성 여부
 * @property {boolean} requiresApproval - 승인 필요 여부
 * @property {boolean} isBlocked - 차단 여부
 * @property {string|null} blockReason - 차단 사유
 */

// 예시 객체
const facilityExample = {
  facilityIdx: 1,
  facilityName: "세미나실 A",
  facilityType: "SEMINAR_ROOM",
  facilityDesc: "20인 수용 가능한 세미나실",
  capacity: 20,
  location: "본관 3층",
  defaultEquipment: "빔프로젝터, 화이트보드",
  isActive: true,
  requiresApproval: true,
  isBlocked: false,
  blockReason: null
};
```

### 가용성 체크 응답 예시

```javascript
/**
 * @typedef {Object} TimeSlot
 * @property {string} startTime - 시작 시간 (형식: "yyyy-MM-dd HH:mm:ss")
 * @property {string} endTime - 종료 시간 (형식: "yyyy-MM-dd HH:mm:ss")
 */

/**
 * @typedef {Object} FacilityAvailabilityDto
 * @property {number} facilityIdx - 시설 ID
 * @property {string} facilityName - 시설명
 * @property {boolean} isAvailable - 가용 여부
 * @property {TimeSlot[]} conflictingReservations - 충돌하는 예약 목록
 */

// 예시 객체
const availabilityExample = {
  facilityIdx: 1,
  facilityName: "세미나실 A",
  isAvailable: false,
  conflictingReservations: [
    {
      startTime: "2025-01-08 10:00:00",
      endTime: "2025-01-08 12:00:00"
    }
  ]
};
```

### API 응답 래퍼

```javascript
/**
 * @typedef {Object} ApiResponse
 * @property {boolean} success - 성공 여부
 * @property {string} message - 응답 메시지
 * @property {*} data - 응답 데이터 (타입은 API에 따라 다름)
 */

// 예시 성공 응답
const successResponse = {
  success: true,
  message: "예약이 생성되었습니다.",
  data: reservationExample
};

// 예시 에러 응답
const errorResponse = {
  success: false,
  message: "요청 인원(5명)이 시설 정원(4명)을 초과합니다.",
  data: null
};
```

### 상태 필터 상수

```javascript
/**
 * 예약 상태 필터 (API 경로 파라미터용)
 * 주의: 응답의 status는 한글("대기중")이지만, 필터링 시에는 영어 enum 사용
 */
const RESERVATION_STATUS = {
  PENDING: "PENDING",      // 대기중
  APPROVED: "APPROVED",    // 승인됨
  REJECTED: "REJECTED",    // 반려됨
  CANCELLED: "CANCELLED",  // 취소됨
  COMPLETED: "COMPLETED"   // 완료됨
};

// 사용 예시
const url = `/api/reservations/my/status/${RESERVATION_STATUS.PENDING}`;
```

## 9. 테스트 체크리스트

### 시설 목록
- [ ] 시설 목록이 올바르게 로드되고 차단/승인 아이콘이 백엔드 데이터와 일치

### 가용성 체크
- [ ] 중복 예약 존재 시 가용성 체크가 충돌 표시

### 예약 생성
- [ ] 승인 필요 시설과 자동 승인 시설 모두 성공 경로 동작
- [ ] 유효성 검증 에러 표시:
  - [ ] 과거 날짜
  - [ ] 종료 시간이 시작 시간보다 빠름
  - [ ] 인원수 0
  - [ ] 정원 초과
  - [ ] 주말 제한
  - [ ] 사용자당 예약 횟수 초과

### 내 예약 목록
- [ ] 새로 생성된 예약이 목록 상단에 표시
- [ ] 상태별 필터링이 목록을 올바르게 업데이트

### 예약 취소
- [ ] 마감시간 전 취소 성공, UI가 `취소됨`으로 업데이트
- [ ] 마감시간 후 취소 실패, 백엔드 에러 메시지 표시
- [ ] 토큰 만료: 401 시 로그인으로 리다이렉트 (기존 동작)

### 에러 처리
- [ ] 모든 백엔드 에러 메시지가 사용자에게 명확히 표시
- [ ] 로딩 상태 시 스피너 표시

## 10. 향후 개선 사항 (백로그)

### API 확장
- [ ] 시설별 정책 데이터 조회 엔드포인트 추가 (최소/최대 시간, 취소 마감 시간) → 클라이언트 측 힌트 제공
- [ ] 예약 목록 페이지네이션 (볼륨 증가 시)

### UI/UX 개선
- [ ] 가용성 표시용 캘린더 뷰 추가 (`/daily-schedule` API 활용)
- [ ] 파일 첨부 지원 (필요 시)
- [ ] 실시간 가용성 업데이트 (WebSocket)

### 성능 최적화
- [ ] 시설 목록 캐싱
- [ ] Optimistic UI 업데이트

## 11. 백엔드 구현 상태

### ✅ 완전 구현된 기능
- 모든 API 엔드포인트 (섹션 4 참조)
- DTO 필드 (ReservationDto, FacilityDto, FacilityAvailabilityDto)
- 날짜/시간 형식 (`@JsonFormat`)
- Bean Validation (`@Valid`, `@NotNull`, `@Min`)
- Phase 1 검증: 정원 & 취소 마감시간
- Phase 2 검증: 사용자당 예약 횟수 & 주말 제한
- 에러 메시지 (RuntimeException)

### 📝 백엔드 코드 참조
- **Controller**: `FacilityController.java`, `FacilityReservationController.java`
- **Service**: `FacilityReservationService.java` (validateCapacity, validateUserReservationLimit 포함)
- **Repository**: `FacilityReservationRepository.java`
- **Entity**: `FacilityPolicyTbl.java` (getEffective* 메서드)
- **Enum**: `ReservationStatus.java` (@JsonValue로 한글 직렬화)

### 검증된 사항
- API 경로 100% 일치
- DTO 필드 100% 일치
- 날짜 형식 통일 (`yyyy-MM-dd HH:mm:ss`)
- Status enum 한글 직렬화 정상 동작
- 에러 메시지 실용적이고 명확함

**결론**: 백엔드는 프론트엔드 개발을 위해 완전히 준비됨. 이 문서를 기반으로 즉시 구현 가능.

## 12. API 호출 흐름 (User Journey)

### 12.1 예약 생성 플로우 (완전한 흐름)

사용자가 시설을 예약하는 전체 과정입니다.

```
[사용자]
   ↓
┌──────────────────────────────────────────────────┐
│ 1. 시설 목록 조회                                │
│    POST /api/facilities                          │
│    → 응답: List<FacilityDto>                     │
└──────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────┐
│ 2. 시설 선택 & 예약 정보 입력                    │
│    - 시설: "세미나실 A" 선택                     │
│    - 날짜: 2025-01-10                           │
│    - 시간: 14:00 ~ 16:00                        │
│    - 인원: 4명                                   │
│    - 목적: "스터디 모임"                         │
└──────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────┐
│ 3. (선택) 가용성 확인                            │
│    POST /api/facilities/1/availability           │
│    ?startTime=2025-01-10 14:00:00               │
│    &endTime=2025-01-10 16:00:00                 │
│                                                  │
│    Case A: 가용                                  │
│    ← { "isAvailable": true, ... }               │
│                                                  │
│    Case B: 충돌 있음                             │
│    ← { "isAvailable": false,                    │
│        "conflictingReservations": [...] }       │
│    → 사용자에게 충돌 시간 표시, 시간 재선택      │
└──────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────┐
│ 4. 예약 생성 요청                                │
│    POST /api/reservations                        │
│    Body: {                                       │
│      "facilityIdx": 1,                          │
│      "startTime": "2025-01-10 14:00:00",        │
│      "endTime": "2025-01-10 16:00:00",          │
│      "partySize": 4,                            │
│      "purpose": "스터디 모임"                    │
│    }                                             │
│                                                  │
│    Case A: 자동 승인 시설                        │
│    ← HTTP 200 OK                                │
│       { "success": true,                        │
│         "message": "예약이 생성되었습니다.",     │
│         "data": {                               │
│           "status": "승인됨",                   │
│           "approvedBy": "SYSTEM" } }            │
│                                                  │
│    Case B: 승인 필요 시설                        │
│    ← HTTP 200 OK                                │
│       { "success": true,                        │
│         "message": "관리자 승인을 기다립니다.",  │
│         "data": { "status": "대기중" } }        │
│                                                  │
│    Case C: 유효성 검증 실패                      │
│    ← HTTP 400 Bad Request                       │
│       { "success": false,                       │
│         "message": "정원 초과..." }             │
└──────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────┐
│ 5. 성공 시 UI 업데이트                           │
│    - 성공 메시지를 화면에 표시                   │
│    - "내 예약" 목록으로 이동 또는               │
│    - 목록 새로고침                               │
└──────────────────────────────────────────────────┘
```

### 12.2 내 예약 조회 및 취소 플로우

```
[사용자: 내 예약 확인 버튼 클릭]
   ↓
┌──────────────────────────────────────────────────┐
│ 1. 전체 예약 목록 조회                           │
│    POST /api/reservations/my                     │
│    ← HTTP 200 OK                                │
│       { "data": [                               │
│         { "reservationIdx": 124,                │
│           "status": "대기중", ... },            │
│         { "reservationIdx": 123,                │
│           "status": "승인됨", ... }             │
│       ]}                                         │
└──────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────┐
│ 2. (선택) 상태별 필터링                          │
│    [대기중] 탭 클릭                              │
│    POST /api/reservations/my/status/PENDING      │
│    ← HTTP 200 OK                                │
│       { "data": [                               │
│         { "reservationIdx": 124,                │
│           "status": "대기중", ... }             │
│       ]}                                         │
└──────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────┐
│ 3. 예약 취소 시도                                │
│    [취소] 버튼 클릭 (예약 124)                   │
│    → 확인 모달 표시                              │
│       "예약 시작 24시간 전까지만 취소 가능..."   │
│                                                  │
│    [확인] 클릭                                   │
│    DELETE /api/reservations/124                  │
│                                                  │
│    Case A: 취소 성공                             │
│    ← HTTP 200 OK                                │
│       { "success": true,                        │
│         "message": "예약이 취소되었습니다." }    │
│    → UI에서 해당 예약 상태를 "취소됨"으로 변경   │
│                                                  │
│    Case B: 취소 마감시간 지남                    │
│    ← HTTP 400 Bad Request                       │
│       { "success": false,                       │
│         "message": "예약 시작 24시간 전까지..." }│
│    → 에러 메시지 표시, 취소 불가                 │
│                                                  │
│    Case C: 권한 없음 (타인의 예약)               │
│    ← HTTP 403 Forbidden                         │
│       { "message": "본인의 예약만 취소 가능..." }│
└──────────────────────────────────────────────────┘
```

### 12.3 시각적 가용성 확인 플로우 (일일 일정 조회)

```
[사용자: 시설 선택 후 날짜 선택]
   ↓
┌──────────────────────────────────────────────────┐
│ 1. 특정 날짜의 시간대별 가용성 조회              │
│    POST /api/facilities/1/daily-schedule         │
│    ?date=2025-01-10                             │
│                                                  │
│    ← HTTP 200 OK                                │
│       { "data": {                               │
│         "facilityIdx": 1,                       │
│         "facilityName": "세미나실 A",           │
│         "date": "2025-01-10",                   │
│         "timeSlots": [                          │
│           { "hour": "09:00", "isAvailable": true },│
│           { "hour": "10:00", "isAvailable": false },│
│           { "hour": "11:00", "isAvailable": false },│
│           { "hour": "12:00", "isAvailable": true },│
│           ...                                   │
│         ]                                       │
│       }}                                         │
└──────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────┐
│ 2. UI에 시간대별 시각화                          │
│    09:00 [✓ 가능]                               │
│    10:00 [✗ 예약됨]                             │
│    11:00 [✗ 예약됨]                             │
│    12:00 [✓ 가능]                               │
│    ...                                           │
│                                                  │
│    사용자는 가능한 시간대를 보고 선택            │
└──────────────────────────────────────────────────┘
```

### 12.4 에러 처리 플로우

```
[모든 API 요청]
   ↓
┌──────────────────────────────────────────────────┐
│ Response 상태 코드 확인                          │
└──────────────────────────────────────────────────┘
   ↓
   ├─→ [401 Unauthorized]
   │   → JWT 토큰 만료 또는 없음
   │   → 자동으로 로그인 페이지로 리다이렉트
   │
   ├─→ [400 Bad Request]
   │   → 유효성 검증 실패
   │   → response.message를 사용자에게 표시
   │      예: "정원 초과", "취소 마감시간 지남" 등
   │
   ├─→ [403 Forbidden]
   │   → 권한 없음
   │   → "권한이 없습니다" 메시지 표시
   │
   ├─→ [404 Not Found]
   │   → 리소스 없음
   │   → "요청한 데이터를 찾을 수 없습니다" 표시
   │
   ├─→ [500 Internal Server Error]
   │   → 서버 오류
   │   → "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요"
   │
   └─→ [200 OK]
       → response.success 확인
          ├─ true: response.message를 성공 메시지로 표시
          └─ false: response.message를 에러로 표시
```

### 12.5 상태 다이어그램 (예약 상태 전환)

```
[예약 생성]
     ↓
┌─────────────────────┐
│  자동 승인 시설?    │
└─────────────────────┘
     ↓         ↓
   YES        NO
     ↓         ↓
┌─────────┐ ┌─────────┐
│ 승인됨  │ │ 대기중  │
└─────────┘ └─────────┘
     │           │
     │           ├─→ [관리자 승인] → 승인됨
     │           │
     │           └─→ [관리자 반려] → 반려됨
     │
     ├─→ [사용자 취소] → 취소됨
     │   (취소 마감시간 전까지만)
     │
     └─→ [예약 시간 종료] → 완료됨
```

### 12.6 주요 시나리오별 API 호출 순서

#### 시나리오 1: 첫 방문 사용자가 예약하는 경우
```
1. POST /api/facilities (시설 목록)
2. POST /api/facilities/{id}/availability (가용성 확인)
3. POST /api/reservations (예약 생성)
4. POST /api/reservations/my (내 예약 확인)
```

#### 시나리오 2: 기존 예약을 취소하는 경우
```
1. POST /api/reservations/my (내 예약 조회)
2. DELETE /api/reservations/{id} (취소)
3. POST /api/reservations/my (업데이트된 목록 재조회)
```

#### 시나리오 3: 특정 상태의 예약만 보는 경우
```
1. POST /api/reservations/my/status/PENDING (대기중 예약)
2. POST /api/reservations/my/status/APPROVED (승인된 예약)
```

#### 시나리오 4: 시각적으로 가용 시간 확인 후 예약
```
1. POST /api/facilities (시설 목록)
2. POST /api/facilities/{id}/daily-schedule?date=2025-01-10 (일일 일정)
3. POST /api/reservations (가능한 시간대로 예약)
```

### 12.7 프론트엔드 구현 시 권장 순서

```
Phase 1: 기본 조회 기능
├─ 1. 시설 목록 조회 UI
├─ 2. 내 예약 목록 UI
└─ 3. API 연동 및 로딩/에러 처리

Phase 2: 예약 생성 기능
├─ 1. 예약 폼 구현
├─ 2. 가용성 확인 연동
├─ 3. 예약 생성 API 연동
└─ 4. 유효성 검증 에러 처리

Phase 3: 예약 관리 기능
├─ 1. 예약 취소 기능
├─ 2. 상태별 필터링
└─ 3. 예약 상세 보기 (선택)

Phase 4: UX 개선
├─ 1. 일일 일정 시각화
├─ 2. 실시간 가용성 업데이트
└─ 3. Optimistic UI 업데이트
```
