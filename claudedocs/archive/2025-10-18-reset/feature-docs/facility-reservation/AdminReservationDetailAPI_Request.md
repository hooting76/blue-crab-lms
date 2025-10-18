# 🧩 관리자용 “예약 상세 조회” API 제안서

## 📌 목적  
관리자 화면(시설물 예약 관리)에서 “처리하기 / 상세보기” 클릭 시,  
**예약의 전체 상세 정보를 단건 조회**하기 위한 엔드포인트 추가 제안입니다.  

현재 사용자 전용 상세 API는 **본인 예약만 조회 가능**하도록 되어 있어  
관리자 계정으로 접근 시 **400 / 403 오류가 발생**합니다.  
👉 따라서 **관리자 권한으로 모든 예약을 조회할 수 있는 별도 엔드포인트**가 필요합니다.

---

## 🔗 엔드포인트 제안  

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **Path** | `/api/admin/reservations/{reservationIdx}` |
| **Auth** | `Authorization: Bearer <JWT>` (관리자 권한 필요) |
| **Request Body** | 없음 (PathVariable만 사용) |

> 📍 참고: 기존 시스템이 POST 위주이므로 GET 대신 POST 유지 권장  
> (`POST /api/admin/reservations/{reservationIdx}`)

---

## ✅ 응답 예시 (성공 시)

```json
{
  "success": true,
  "message": "예약 상세 조회 성공",
  "data": {
    "reservationIdx": 123,
    "facilityIdx": 1,
    "facilityName": "세미나실 301호",

    "userCode": "20240123",
    "userName": "홍길동",
    "email": "gildong@univ.ac.kr",

    "startTime": "2025-10-05 14:00:00",
    "endTime": "2025-10-05 16:00:00",
    "partySize": 8,
    "createdAt": "2025-09-28 10:00:00",

    "purpose": "캡스톤 팀 프로젝트 회의",
    "requestedEquipment": "빔프로젝터, 음향시설",

    "status": "승인됨", 
    "adminNote": "장비 세팅 완료했습니다.",
    "rejectionReason": null,

    "approvedBy": "ADMIN",
    "approvedAt": "2025-09-29 13:20:00"
  }
}
```

---

## 📋 필드 규격  

| 필드명 | 설명 |
|--------|------|
| `facilityName` | 시설명 |
| `startTime` / `endTime` | 예약 시작 / 종료 시간 (`yyyy-MM-dd HH:mm:ss`) |
| `userName`, `userCode`, `email` | 신청자 정보 |
| `partySize` | 인원 수 |
| `createdAt` | 신청 일시 |
| `purpose`, `requestedEquipment` | 사용 목적 / 요청 장비 |
| `status` | 상태 (승인 대기 / 승인됨 / 반려됨 / 취소됨 / 완료됨) |
| `adminNote` | 관리자 비고 (선택) |
| `rejectionReason` | 반려 사유 (반려 시 필수) |
| `approvedBy`, `approvedAt` | 승인자 / 승인일시 |

---

## ⚠️ 오류 응답 예시

| 상황 | HTTP 코드 | 예시 응답 |
|------|------------|-----------|
| 인증 누락 | 401 | `{ "success": false, "message": "인증이 필요합니다." }` |
| 관리자 권한 아님 | 403 | `{ "success": false, "message": "접근 권한이 없습니다." }` |
| 예약 없음 | 404 | `{ "success": false, "message": "예약을 찾을 수 없습니다." }` |
| 서버 오류 | 500 | `{ "success": false, "message": "서버 오류가 발생했습니다." }` |

---

## 🔒 권한 및 검증  

- 관리자 롤 (`ROLE_ADMIN`)만 접근 가능  
- PathVariable `reservationIdx`는 정수 검증 필요  
- 응답 시간 포맷 및 한글 상태명(`승인됨`, `반려됨` 등) 일관성 유지  

---

## 🧠 프런트 연동 참고  

- 프런트는 먼저 **목록 API 데이터로 모달을 즉시 렌더링**하고,  
  이 API를 통해 **이메일·관리자 비고 등 추가정보를 보강**합니다.  
- 실패 시(400/403 등) 기본 정보로만 표시해도 무방.  
- 승인/반려 로직은 기존 API 그대로 유지:  
  - 승인 → `POST /api/admin/reservations/approve`  
  - 반려 → `POST /api/admin/reservations/reject`

---

## 💬 샘플 cURL  

```bash
curl -X POST   -H "Authorization: Bearer <ADMIN_JWT>"   -H "Content-Type: application/json"   https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/admin/reservations/123
```

---

## ✅ 수락 기준 (Acceptance Criteria)

- 관리자 토큰으로 호출 시 `success: true` + 상세 데이터 반환  
- 비관리자 토큰 → 403  
- 없는 예약 ID → 404  
- 응답 필드 포맷: 한글 상태명 + 날짜포맷 통일  
- 이메일, 관리자 비고, 반려 사유, 승인자/승인일시는 null 허용  

---

> ✉️ **요약:**  
> 관리자 전용 예약 상세조회 API (`POST /api/admin/reservations/{reservationIdx}`) 추가 요청.  
> 기존 사용자 API는 소유자 제한이 있어, 관리자 계정에서 전체 예약 확인용 별도 엔드포인트가 필요합니다.  
> 프런트에서는 승인/반려 기능과 함께 모달 내 상세정보 표시용으로 사용됩니다.
