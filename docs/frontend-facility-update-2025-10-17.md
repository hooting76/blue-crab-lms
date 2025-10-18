# 시설물 예약 프론트엔드 업데이트 가이드 (2025-10-17)

## 📅 업데이트 일자
**2025년 10월 17일** - 커밋 `82045f4`, `4e21387`

## 🎯 변경 사항 요약

이번 백엔드 업데이트는 시설물 예약 시스템의 사용자 식별 방식을 개선하고, FCM 푸시 알림 기능을 추가했습니다. 프론트엔드에서는 **API 응답 필드 변경** 대응이 필요합니다.

> **참고**: FCM Data-only 방식의 알림 수신 처리는 프론트 팀에서 이미 구현 및 테스트 완료되었습니다.

---

## 🔄 주요 변경사항

### 1. 시설물 예약 API 응답 필드 추가

#### ✅ 추가된 필드: `userEmail`

백엔드에서 시설물 예약 테이블에 `USER_EMAIL` 컬럼이 추가되었으며, 모든 예약 관련 API 응답에 `userEmail` 필드가 포함됩니다.

**변경 전 (ReservationDto)**
```json
{
  "reservationIdx": 123,
  "facilityIdx": 1,
  "facilityName": "세미나실 A",
  "userCode": "20240001",
  "userName": "홍길동",
  "startTime": "2025-01-10 14:00:00",
  "endTime": "2025-01-10 16:00:00",
  "partySize": 4,
  "purpose": "스터디 모임",
  "status": "대기중",
  "adminNote": null,
  "rejectionReason": null,
  ...
}
```

**변경 후 (ReservationDto)**
```json
{
  "reservationIdx": 123,
  "facilityIdx": 1,
  "facilityName": "세미나실 A",
  "userCode": "20240001",
  "userName": "홍길동",
  "userEmail": "student@example.com",  // ⭐ 신규 필드
  "startTime": "2025-01-10 14:00:00",
  "endTime": "2025-01-10 16:00:00",
  "partySize": 4,
  "purpose": "스터디 모임",
  "status": "대기중",
  "adminNote": null,
  "rejectionReason": null,
  ...
}
```

---

## 🛠️ 프론트엔드 수정 필요 사항

### 1. TypeScript 타입 정의 업데이트

**파일 위치**: `src/types/facility.ts` (또는 해당 타입 정의 파일)

```typescript
// 기존
interface ReservationDto {
  reservationIdx: number;
  facilityIdx: number;
  facilityName: string;
  userCode: string;
  userName: string;
  startTime: string;
  endTime: string;
  partySize: number;
  purpose: string | null;
  requestedEquipment: string | null;
  status: string;
  adminNote: string | null;
  rejectionReason: string | null;
  approvedBy: string | null;
  approvedAt: string | null;
  createdAt: string;
}

// 변경 후
interface ReservationDto {
  reservationIdx: number;
  facilityIdx: number;
  facilityName: string;
  userCode: string;
  userName: string;
  userEmail: string;               // ⭐ 추가
  startTime: string;
  endTime: string;
  partySize: number;
  purpose: string | null;
  requestedEquipment: string | null;
  status: string;
  adminNote: string | null;
  rejectionReason: string | null;
  approvedBy: string | null;
  approvedAt: string | null;
  createdAt: string;
}
```

### 2. UI 컴포넌트 업데이트 (선택적)

예약 상세 정보를 표시하는 컴포넌트에서 `userEmail` 필드를 활용할 수 있습니다.

**예시: 관리자 페이지에서 예약 상세 정보 표시**

```jsx
// AdminReservationDetail.jsx
const ReservationDetailCard = ({ reservation }) => {
  return (
    <Card>
      <CardHeader>
        <h3>예약 상세 정보</h3>
      </CardHeader>
      <CardBody>
        <div className="reservation-info">
          <div className="info-row">
            <span className="label">예약자:</span>
            <span className="value">{reservation.userName}</span>
          </div>
          <div className="info-row">
            <span className="label">학번:</span>
            <span className="value">{reservation.userCode}</span>
          </div>
          {/* ⭐ 이메일 정보 표시 (선택적) */}
          {reservation.userEmail && (
            <div className="info-row">
              <span className="label">이메일:</span>
              <span className="value">{reservation.userEmail}</span>
            </div>
          )}
          <div className="info-row">
            <span className="label">시설:</span>
            <span className="value">{reservation.facilityName}</span>
          </div>
          <div className="info-row">
            <span className="label">예약 시간:</span>
            <span className="value">
              {reservation.startTime} ~ {reservation.endTime}
            </span>
          </div>
          <div className="info-row">
            <span className="label">인원:</span>
            <span className="value">{reservation.partySize}명</span>
          </div>

          {/* 반려 사유 표시 */}
          {reservation.status === '반려됨' && reservation.rejectionReason && (
            <Alert type="error">
              <strong>반려 사유:</strong> {reservation.rejectionReason}
            </Alert>
          )}

          {/* 관리자 노트 표시 */}
          {reservation.adminNote && (
            <Alert type="info">
              <strong>관리자 메모:</strong> {reservation.adminNote}
            </Alert>
          )}
        </div>
      </CardBody>
    </Card>
  );
};
```

### 3. Null-safe 처리

기존 예약 데이터는 `userEmail`이 `null`일 수 있으므로 안전하게 처리해야 합니다.

```javascript
// ✅ 안전한 접근 방법
const displayEmail = reservation.userEmail || '이메일 없음';

// ✅ 조건부 렌더링
{reservation.userEmail && (
  <div>이메일: {reservation.userEmail}</div>
)}

// ✅ Optional chaining
<div>이메일: {reservation.userEmail ?? '미등록'}</div>
```

---

## 📋 영향받는 API 엔드포인트

다음 API들의 응답에 `userEmail` 필드가 추가되었습니다:

| API | 엔드포인트 | 영향 |
|-----|-----------|------|
| 내 예약 목록 조회 | `POST /api/reservations/my` | 응답의 각 예약 객체에 `userEmail` 필드 추가 |
| 상태별 내 예약 조회 | `POST /api/reservations/my/status/{status}` | 응답의 각 예약 객체에 `userEmail` 필드 추가 |
| 예약 상세 조회 | `POST /api/reservations/{reservationIdx}` | 응답의 예약 객체에 `userEmail` 필드 추가 |
| 예약 생성 | `POST /api/reservations` | 응답의 예약 객체에 `userEmail` 필드 추가 |
| 관리자 예약 목록 조회 | `POST /api/admin/reservations` | 응답의 각 예약 객체에 `userEmail` 필드 추가 |
| 관리자 예약 상세 조회 | `POST /api/admin/reservations/{reservationIdx}` | 응답의 예약 객체에 `userEmail` 필드 추가 |

---

## 🔔 FCM 알림 관련 정보 (참고용)

### 알림 타입: `FACILITY_RESERVATION`

관리자가 시설 예약을 승인/반려하면 사용자에게 자동으로 FCM 푸시 알림이 전송됩니다.

#### 알림 데이터 구조

**승인 알림**
```json
{
  "data": {
    "type": "FACILITY_RESERVATION",
    "action": "APPROVED",
    "reservationIdx": "123",
    "facilityIdx": "1",
    "facilityName": "세미나실 A",
    "startTime": "2025-01-10T14:00:00",
    "endTime": "2025-01-10T16:00:00"
  }
}
```

**반려 알림**
```json
{
  "data": {
    "type": "FACILITY_RESERVATION",
    "action": "REJECTED",
    "reservationIdx": "123",
    "facilityIdx": "1",
    "facilityName": "세미나실 A",
    "rejectionReason": "시설 점검으로 인한 사용 불가"
  }
}
```

> **참고**: FCM Data-only 방식의 알림 수신 처리는 이미 프론트엔드에 구현되어 있으므로 추가 작업이 필요하지 않습니다.

---

## ✅ 구현 체크리스트

### 필수 구현 사항

- [ ] **TypeScript 인터페이스에 `userEmail` 필드 추가**
  - `ReservationDto` 타입 정의 업데이트
  - JSDoc 주석도 함께 업데이트 (TypeScript 미사용 프로젝트의 경우)

- [ ] **기존 코드에서 타입 에러 확인**
  - TypeScript 프로젝트의 경우 `tsc --noEmit` 실행
  - 예약 객체를 사용하는 모든 컴포넌트 확인

- [ ] **Null-safe 처리**
  - `userEmail`이 `null`일 수 있는 경우 대비
  - 조건부 렌더링 또는 기본값 처리

### 선택적 구현 사항

- [ ] **관리자 페이지 UI 개선**
  - 예약 상세 정보에 이메일 표시
  - 연락처 정보 섹션 추가

- [ ] **사용자 페이지 UI 개선**
  - 내 예약 목록에서 이메일 확인 기능 (필요 시)

---

## 🔍 백엔드 변경 이력

| 날짜 | 커밋 | 변경 내용 |
|------|------|----------|
| 2025-10-17 | `82045f4` | 시설물 예약 테이블에 `USER_EMAIL` 컬럼 추가, 사용자 식별 정규화 로직 추가, FCM 알림 전송 기능 구현 |
| 2025-10-17 | `4e21387` | FCM Data-only 전송 방식 개선, 토큰 조회 API 추가, 배치 전송 로직 최적화 |

---

## ❓ FAQ

### Q1. 기존 예약 데이터는 어떻게 되나요?
**A:** 기존 예약의 `userEmail`은 `null`일 수 있습니다. 프론트엔드에서 null-safe 처리를 해야 합니다.

```javascript
// ✅ 안전한 접근
const email = reservation.userEmail || '이메일 없음';

// ✅ 조건부 렌더링
{reservation.userEmail && <div>이메일: {reservation.userEmail}</div>}
```

### Q2. API 호출 방법이 변경되나요?
**A:** 아니요. API 요청 방식은 동일하며, 응답에 `userEmail` 필드가 추가되는 것뿐입니다. 기존 코드는 그대로 동작합니다.

### Q3. userEmail 필드를 반드시 UI에 표시해야 하나요?
**A:** 아니요. 선택 사항입니다. 관리자 페이지에서 연락처 정보가 필요한 경우 추가하면 유용하지만, 필수는 아닙니다.

### Q4. 이메일/학번 혼용 입력은 어떻게 처리되나요?
**A:** 백엔드에서 자동으로 처리됩니다. 프론트엔드는 변경사항이 없으며, JWT 토큰에서 사용자를 식별합니다.

```javascript
// 예약 생성 API - 변경 없음
const response = await fetch('/api/reservations', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,  // JWT에서 사용자 식별
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    facilityIdx: 1,
    startTime: '2025-01-10 14:00:00',
    endTime: '2025-01-10 16:00:00',
    partySize: 4,
    // userCode는 JWT에서 자동 추출
  })
});
```

### Q5. FCM 알림 수신 처리를 다시 구현해야 하나요?
**A:** 아니요. FCM Data-only 방식의 알림 수신 처리는 프론트 팀에서 이미 구현 및 테스트 완료되었으므로 추가 작업이 필요하지 않습니다.

---

## 📚 관련 문서

- [시설 예약 프론트엔드 스펙](./facility-reservation-frontend-spec-ko.md) - 전체 API 명세 및 구현 가이드
- [FCM Data-only 푸시 테스트 가이드](../claudedocs/data-only-push-test-guide.md) - FCM 알림 테스트 방법
- [푸시 알림 중복 발송 이슈](./push-notification-double-fire.md) - Data-only 방식 도입 배경

---

## 📞 문의

구현 중 문제가 발생하면 백엔드 팀에 문의하거나 다음 테스트 페이지를 활용하세요:
- **백엔드 API 테스트 페이지**: `http://localhost:8080/test.html`
- **시설물 예약 API 테스트**: 테스트 페이지 → "시설물 예약" 섹션
