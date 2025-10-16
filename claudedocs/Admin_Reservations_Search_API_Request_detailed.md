
# 관리자용 **전체 예약 조회(검색)** API 제안서 
  
> 본 문서는 **신규 요청 API**인 `/api/admin/reservations/search`에 대한 상세 제안이며,
  이미 구현된 항목은 간략 확인용으로 하단에 분리 표기합니다.

---

## 1) 엔드포인트 개요

- **URL**: `/api/admin/reservations/search`
- **Method**: `POST`
- **Auth**: `Bearer {JWT}` (관리자 전용)
- **목적**: 관리자 화면의 **전체 예약 목록**을 페이지네이션으로 조회 (필터는 **선택적** 적용)

---

## 2) 요청 바디 스키마 (필수/선택 + 이유/기본값)

> 아래 표는 **각 필드가 왜 필수인지/선택인지**, **누락 시 서버가 어떻게 동작해야 하는지**를 함께 설명합니다.

| 필드 | 타입 | 필수 여부 | 누락 시 서버 동작(기본값) | 필수/선택 사유 |
|---|---|---|---|---|
| `status` | `string` (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED`) | **선택** | 미지정 시 **모든 상태 포함**(= 전체 조회) | 기본 “전체 조회” UX를 지원하기 위해 **필터가 없어도 동작**해야 함. 특정 상태만 보고 싶을 때만 전달. |
| `facilityIdx` | `number` | **선택** | 미지정 시 **모든 시설 포함** | 관리자가 특정 시설만 필터링할 필요가 있을 때만 사용. 전체 조회가 기본이므로 선택으로 둠. |
| `dateFrom` | `string (YYYY-MM-DD)` | **선택** | 미지정 시 **기간 제한 없음(또는 시스템 기본 기간)** | 기본 목록 확인이 목적이므로 기간 필터는 선택. 단, 운영 정책상 기본 기간(예: 최근 3개월)로 제한하고 싶다면 서버 기본값으로 처리 가능. |
| `dateTo` | `string (YYYY-MM-DD)` | **선택** | 미지정 시 **기간 제한 없음(또는 시스템 기본 기간)** | 위와 동일. `dateFrom`만 있는 경우에는 `dateTo`가 **같은 날**로 간주하거나 **오늘**로 간주하는 등 서버 정책 필요. |
| `query` | `string` | **선택** | 미지정 시 **키워드 필터 미적용** | 검색창을 사용하지 않는 기본 전체 조회를 지원해야 하므로 선택. 이름/학번/시설명 등에 매칭. |
| `page` | `number` (0-base) | **필수** | - | 페이지네이션 계산을 위해 필수. **프론트는 항상 숫자 제공**(기본 0). |
| `size` | `number` | **필수** | - | 페이지네이션 크기를 서버가 알아야 함. **프론트 기본값은 5**로 사용 중. |

### 추가 유효성 규칙 제안
- `dateFrom`과 `dateTo`가 **둘 다 존재**하면: `dateFrom <= dateTo`이어야 함 (위반 시 400 + 메시지).
- `page >= 0`, `1 <= size <= 50` 범위 권장 (지나치게 큰 size 제한).
- `status`는 대문자 enum 값만 허용(대소문자 혼용 금지).

---

## 3) 요청/응답 예시

### 3-1. 기본 전체 조회 (필터 없이)
```http
POST /api/admin/reservations/search
Authorization: Bearer {JWT}
Content-Type: application/json

{
  "page": 0,
  "size": 5
}
```
**의도**: 어떤 조건도 없이 **전체 예약**을 첫 페이지부터 5개씩 가져온다.

### 3-2. 상태 + 기간 필터 조회
```http
POST /api/admin/reservations/search
Authorization: Bearer {JWT}
Content-Type: application/json

{
  "status": "PENDING",
  "dateFrom": "2025-10-01",
  "dateTo": "2025-10-31",
  "page": 0,
  "size": 5
}
```
**의도**: 10월 한 달 동안의 **승인 대기** 예약만 첫 페이지로 조회.

### 3-3. 시설 + 키워드 필터 조회
```http
POST /api/admin/reservations/search
Authorization: Bearer {JWT}
Content-Type: application/json

{
  "facilityIdx": 2,
  "query": "홍길동",
  "page": 1,
  "size": 5
}
```
**의도**: 시설 2번에서, 이름/학번/시설명 중 **홍길동**에 매칭되는 건들을 **2페이지째**로 조회.

### 3-4. 성공 응답 (예시)
```json
{
  "success": true,
  "message": "예약 목록 조회 성공",
  "data": {
    "content": [
      {
        "reservationIdx": 124,
        "facilityIdx": 2,
        "facilityName": "대회의실",
        "userCode": "USER001",
        "userName": "홍길동",
        "startTime": "2025-10-10 14:00:00",
        "endTime": "2025-10-10 16:00:00",
        "partySize": 15,
        "purpose": "팀 회의",
        "requestedEquipment": null,
        "status": "대기중",
        "adminNote": null,
        "rejectionReason": null,
        "approvedBy": null,
        "approvedAt": null,
        "createdAt": "2025-10-08 10:00:00"
      }
    ],
    "page": 0,
    "size": 5,
    "totalElements": 37,
    "totalPages": 8
  }
}
```
- 프론트는 `content.length === size` 이면 **다음 페이지 있음**으로 처리.
- 가능하다면 `totalElements/totalPages`를 함께 주시면 더 정확한 페이지네이션이 가능합니다.

### 3-5. 오류 응답 (예시)
```json
{
  "success": false,
  "message": "dateFrom은 dateTo보다 늦을 수 없습니다.",
  "data": null
}
```
또는 표준 HTTP 상태 기반:
```json
{
  "timestamp": "2025-10-15T09:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "dateFrom은 dateTo보다 늦을 수 없습니다.",
  "path": "/api/admin/reservations/search"
}
```

---

## 4) 정렬/기본 정책 제안

- **정렬 기본값**: `createdAt DESC`(최신 신청 먼저) 또는 `startTime ASC`(가까운 일정 먼저).  
  둘 중 하나로 서버 표준을 정해주시면 프론트가 동일하게 표시하겠습니다.
- **권한/인증 실패**: 401/403 시 기존 전역 처리 유지(로그인 리다이렉트/권한 없음 메시지).

---

## 5) 프론트 연동 포인트 (구현 관점)

- 프론트 구성:
  - 탭: **승인대기**(기 구현) / **전체예약**(본 API 필요)
  - 페이지네이션: **5개/페이지**, 다음/이전
  - 상세보기: `POST /api/reservations/{id}` **(이미 요청/합의 완료)** 재사용
- 호출 규약:
  - `page`와 `size`는 **항상 전송** (필수)
  - 나머지 필터는 **선택** (전송하지 않으면 전체 조건으로 조회)

---

## 6) 이미 구현/운영 중인 관련 API (확인용)

> 아래 3개는 **이미 백엔드 구현되어 있고 프론트 연동 완료** 상태입니다.

1. **승인 대기 목록**  
   - `POST /api/admin/reservations/pending`  
   - 관리자 화면 “승인 대기” 탭에 사용 중

2. **예약 승인 처리**  
   - `POST /api/admin/reservations/approve`  
   - 바디: `{ reservationIdx, adminNote? }`

3. **예약 반려 처리**  
   - `POST /api/admin/reservations/reject`  
   - 바디: `{ reservationIdx, rejectionReason }` (반려 사유 필수)

> 또한, **관리자용 “예약 상세 조회” API 제안**은 별도 문서로 이미 전달/요청 완료되었고,  
> 현재까지는 사용자 상세조회(`POST /api/reservations/{id}`)를 임시 재사용 중입니다.

---

## 7) 결론

- `/api/admin/reservations/search` 는 **필터가 없어도 전체 조회가 가능한 구조**를 목표로 합니다.  
- `page`/`size`는 **반드시 필요**하며, 나머지 필터는 **선택**입니다.  
- 응답은 페이지네이션 정보를 포함하여 프론트가 **안정적으로 다음/이전**을 표시할 수 있게 해주세요.  
- 상세보기는 기존 사용자 상세조회 API를 **재사용**(또는 전용 관리자 상세 API 적용)합니다.
