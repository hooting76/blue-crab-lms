# 시설물 예약 API 빠른 참조

**최종 업데이트:** 2025-10-10

---

## 🎯 핵심 규칙

### ⚠️ 중요 제약사항
- **예약은 같은 날짜 내에서만 가능** (startTime과 endTime이 같은 날)
- 여러 날 예약 필요 시 → 각 날짜별로 따로 신청

### 🔒 승인 정책
- **즉시 승인 시설** (`requiresApproval: false`)
  - 예약 즉시 `APPROVED` 상태
  - 비관적 락으로 동시성 제어
  - 예: 소회의실, 세미나실

- **승인 필요 시설** (`requiresApproval: true`)
  - 예약 생성 시 `PENDING` 상태
  - 관리자 승인 후 `APPROVED` 상태
  - 예: 대강당, 체육관

---

## 📡 유저 API

### 시설 조회 (인증 불필요)

| API | 설명 | Path |
|-----|------|------|
| 전체 시설 목록 | 활성화된 모든 시설 | `POST /api/facilities` |
| 유형별 시설 목록 | 특정 유형 시설만 | `POST /api/facilities/type/{facilityType}` |
| 시설 상세 | 특정 시설 정보 | `POST /api/facilities/{facilityIdx}` |
| 시설 검색 | 키워드 검색 | `POST /api/facilities/search?keyword={keyword}` |
| 가용성 확인 | 예약 가능 여부 | `POST /api/facilities/{facilityIdx}/availability?startTime=...&endTime=...` |

### 예약 관리 (JWT 필요)

| API | 설명 | Method | Path |
|-----|------|--------|------|
| 예약 생성 | 새 예약 신청 | `POST` | `/api/reservations` |
| 내 예약 전체 | 모든 내 예약 | `POST` | `/api/reservations/my` |
| 내 예약 (상태별) | 특정 상태 예약만 | `POST` | `/api/reservations/my/status/{status}` |
| 예약 상세 | 특정 예약 정보 | `POST` | `/api/reservations/{reservationIdx}` |
| 예약 취소 | 예약 취소 | `DELETE` | `/api/reservations/{reservationIdx}` |

---

## 👨‍💼 관리자 API

**Base Path:** `/api/admin/reservations`  
**인증:** 관리자 JWT 필요

| API | 설명 | Method | Path |
|-----|------|--------|------|
| 승인 대기 목록 | PENDING 예약 목록 | `POST` | `/pending` |
| 대기 건수 | 승인 대기 건수 | `POST` | `/pending/count` |
| 예약 승인 | 예약 승인 처리 | `POST` | `/approve` |
| 예약 반려 | 예약 반려 처리 | `POST` | `/reject` |
| 통계 조회 | 예약 통계 | `POST` | `/stats` |

---

## 💾 Request/Response 예시

### 예약 생성 (유저)

**Request:**
```http
POST /api/reservations
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "facilityIdx": 1,
  "startTime": "2025-10-15T09:00:00",
  "endTime": "2025-10-15T12:00:00",
  "partySize": 50,
  "purpose": "신입생 오리엔테이션",
  "requestedEquipment": "빔프로젝터, 마이크"
}
```

**Response (즉시 승인):**
```json
{
  "success": true,
  "message": "예약이 자동으로 승인되었습니다.",
  "data": {
    "reservationIdx": 101,
    "status": "APPROVED",
    "approvedBy": "SYSTEM",
    ...
  }
}
```

**Response (승인 대기):**
```json
{
  "success": true,
  "message": "예약이 생성되었습니다. 관리자 승인 대기 중입니다.",
  "data": {
    "reservationIdx": 102,
    "status": "PENDING",
    ...
  }
}
```

### 예약 승인 (관리자)

**Request:**
```http
POST /api/admin/reservations/approve
Authorization: Bearer {ADMIN_JWT_TOKEN}
Content-Type: application/json

{
  "reservationIdx": 102,
  "adminNote": "장비는 행사 30분 전까지 준비됩니다."
}
```

**Response:**
```json
{
  "success": true,
  "message": "예약이 승인되었습니다.",
  "data": {
    "reservationIdx": 102,
    "status": "APPROVED",
    "approvedBy": "ADMIN001",
    "approvedAt": "2025-10-10T16:00:00",
    ...
  }
}
```

### 예약 반려 (관리자)

**Request:**
```http
POST /api/admin/reservations/reject
Authorization: Bearer {ADMIN_JWT_TOKEN}
Content-Type: application/json

{
  "reservationIdx": 103,
  "rejectionReason": "해당 날짜는 시설 정기 점검일입니다."
}
```

**Response:**
```json
{
  "success": true,
  "message": "예약이 반려되었습니다.",
  "data": {
    "reservationIdx": 103,
    "status": "REJECTED",
    "rejectionReason": "해당 날짜는 시설 정기 점검일입니다.",
    ...
  }
}
```

---

## 🔑 Enum 타입

### FacilityType (시설 유형)
```
AUDITORIUM      대강당
MEETING_ROOM    회의실
SEMINAR_ROOM    세미나실
LAB             실험실
GYM             체육관
OUTDOOR         야외 공간
OTHER           기타
```

### ReservationStatus (예약 상태)
```
PENDING         승인 대기
APPROVED        승인됨
REJECTED        반려됨
CANCELLED       취소됨
COMPLETED       완료됨
```

---

## ⚠️ 주요 에러 메시지

| 에러 | 원인 | HTTP Status |
|------|------|-------------|
| "예약은 같은 날짜 내에서만 가능합니다..." | startTime과 endTime 날짜 다름 | 400 |
| "해당 시간에는 이미 다른 예약이 존재합니다..." | 예약 시간 충돌 | 400 |
| "해당 시설은 ... 예약이 불가합니다. 사유: ..." | 시설 차단 기간 | 400 |
| "시작 시간은 현재 시간 이후여야 합니다." | 과거 시간 예약 시도 | 400 |
| "최소 예약 시간은 ... 분입니다." | 예약 시간 너무 짧음 | 400 |
| "최대 예약 시간은 ... 분입니다." | 예약 시간 너무 김 | 400 |
| "본인의 예약만 조회할 수 있습니다." | 다른 사용자 예약 접근 | 403 |
| "취소할 수 없는 상태입니다." | REJECTED/CANCELLED 상태 취소 시도 | 400 |

---

## 🔄 플로우 요약

### 즉시 승인 시설 예약
```
1. 시설 목록 조회 → requiresApproval: false 확인
2. 가용성 체크
3. 예약 생성 → status: APPROVED (자동)
4. 즉시 사용 가능 ✅
```

### 승인 필요 시설 예약
```
1. 시설 목록 조회 → requiresApproval: true 확인
2. 가용성 체크
3. 예약 생성 → status: PENDING
4. 관리자 승인 대기
5. 관리자 승인 → status: APPROVED
6. 사용 가능 ✅
```

### 여러 날 연속 예약
```
❌ 한 번에 여러 날 예약 불가
✅ 각 날짜별로 개별 신청 필요

예: 3일 연속 예약
→ POST /api/reservations (2025-10-15)
→ POST /api/reservations (2025-10-16)
→ POST /api/reservations (2025-10-17)
```

---

## 🎨 프론트엔드 체크리스트

### 필수 구현
- [ ] 시설 목록 표시 (`getAllFacilities`)
- [ ] 시설 상세 정보 (`getFacilityById`)
- [ ] **날짜 검증**: startTime과 endTime이 같은 날인지 확인
- [ ] 실시간 가용성 체크 (`checkAvailability`)
- [ ] 예약 생성 폼
- [ ] 내 예약 목록
- [ ] 예약 취소 기능
- [ ] `requiresApproval` 표시 (승인 필요 여부)

### 관리자 기능
- [ ] 승인 대기 예약 목록
- [ ] 승인/반려 처리
- [ ] 대기 건수 뱃지 표시
- [ ] 예약 통계 대시보드

### 사용자 안내
- [ ] "예약은 하루 단위로만 가능합니다" 안내 메시지
- [ ] "여러 날 예약이 필요한 경우 각 날짜별로 신청하세요" 안내
- [ ] 승인 필요 시설: "관리자 승인 후 사용 가능" 표시
- [ ] 즉시 승인 시설: "예약 즉시 사용 가능" 표시

---

## 📚 상세 문서

자세한 내용은 다음 문서를 참조하세요:
- [시설물 예약 API 완전 가이드](./facility-reservation-api-complete.md)
- [연속된 시간대만 예약 가능 정책](../feature-docs/facility-reservation/consecutive-time-only-policy.md)
- [시설 예약 최적화 보고서](../feature-docs/facility-reservation/facility-reservation-optimization-2025-10-10.md)

---

**⚡ 빠른 테스트:**
```bash
# 시설 목록 조회
curl -X POST http://localhost:8080/api/facilities

# 예약 생성 (토큰 필요)
curl -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "facilityIdx": 2,
    "startTime": "2025-10-15T09:00:00",
    "endTime": "2025-10-15T12:00:00",
    "purpose": "테스트"
  }'

# 내 예약 조회
curl -X POST http://localhost:8080/api/reservations/my \
  -H "Authorization: Bearer YOUR_TOKEN"
```
