# Facility Reservation Front-End Specification

## 1. Scope
Build end-user UI flows for browsing facilities, checking availability, creating reservations, viewing personal reservations, and cancelling approved/pending reservations. Admin facing pages are out of scope.

## 2. Architecture Overview
- **Runtime**: React (Vite), router state already routes `Community` → `FacilityRequest`.
- **State Management**: Local component state + fetch hooks (existing pattern in `component/common`).
- **Auth Context**: Use current JWT handling (token stored client side). All facility APIs require authenticated user.
- **API Base URL**: `/api/...` (same origin).

## 3. UI Flows

### 3.1 Facility List & Availability
1. On load, fetch active facilities via `POST /api/facilities`.
2. Display cards with name, capacity, location, approval requirement, and active block info (`isBlocked`, `blockReason`).
3. Allow filtering by facility type via `POST /api/facilities/type/{facilityType}` (optional future enhancement).
4. For selected facility, collect:
   - Date (single day, enforced by backend).
   - Start / end time (format `yyyy-MM-dd HH:mm:ss`).
   - Party size (integer ≥ 1).
   - Purpose / requested equipment (optional text inputs).
5. Prior to submission, offer “Check availability” button calling `POST /api/facilities/{facilityIdx}/availability?startTime=...&endTime=...`.
   - Show conflicts list if `isAvailable` is false.

### 3.2 Reservation Creation
1. Submit form to `POST /api/reservations` with payload:
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
2. Response: `ApiResponse<ReservationDto>`.
   - `status` field returns enum description (`대기중`, `승인됨`, etc.).
   - Message indicates immediate approval vs pending.
3. On success:
   - Toast success.
   - Append to local “My reservations” list or refetch.
4. Handle validation errors (HTTP 400 with message string). Display inline or global error message.

### 3.3 My Reservations
1. Fetch via `POST /api/reservations/my`.
   - For filters, call `POST /api/reservations/my/status/{status}` (`status` options: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED`).
2. Show cards/table with key fields:
   - Facility name.
   - Time range.
   - Party size.
   - Status (use color badges).
   - Admin note / rejection reason when present.
   - Approved by / at if available.
3. Provide “View detail” action calling `POST /api/reservations/{reservationIdx}` (optional).

### 3.4 Cancellation
1. `PENDING` or `APPROVED` statuses should show “Cancel” button.
2. On click:
   - Confirm modal summarizing cancellation deadline.
   - Call `DELETE /api/reservations/{reservationIdx}`.
3. Success → toast + update status to `취소됨` locally.
4. Failure scenarios:
   - Past cancellation deadline (HTTP 400, message: “예약 시작 XX시간 전까지만 취소 가능합니다...”). Display message to user.
   - Not owner / invalid state (HTTP 400 / 403). Show generic error.

## 4. API Summary

| Use Case | Method & Path | Notes |
| --- | --- | --- |
| Facility list | `POST /api/facilities` | Auth required. |
| Facility availability | `POST /api/facilities/{facilityIdx}/availability` | Params via query string (`startTime`, `endTime`), format `yyyy-MM-dd HH:mm:ss`. |
| Create reservation | `POST /api/reservations` | Body as described; returns `ReservationDto`. |
| My reservations | `POST /api/reservations/my` | List newest first. |
| My reservations by status | `POST /api/reservations/my/status/{status}` | `status` must match enum name. |
| Reservation detail | `POST /api/reservations/{reservationIdx}` | Optional detail view. |
| Cancel reservation | `DELETE /api/reservations/{reservationIdx}` | Allowed for `PENDING`/`APPROVED`. |

### ReservationDto Fields (subset)
```
{
  reservationIdx: number,
  facilityIdx: number,
  facilityName: string,
  userCode: string,
  userName: string,
  startTime: "yyyy-MM-dd HH:mm:ss",
  endTime: "yyyy-MM-dd HH:mm:ss",
  partySize: number,
  purpose: string,
  requestedEquipment: string,
  status: "대기중" | "승인됨" | "반려됨" | "취소됨" | "완료됨",
  adminNote: string | null,
  rejectionReason: string | null,
  approvedBy: string | null,
  approvedAt: "yyyy-MM-dd HH:mm:ss" | null,
  createdAt: "yyyy-MM-dd HH:mm:ss"
}
```

## 5. Validation & UX Requirements
- **Date/Time**: Enforce same-day selection. Use picker constrained to `minDate = today`, `maxDate = today + policy.maxDaysInAdvance` if available from backend (fetch via facility detail?). For initial implementation, validate on submit and rely on backend error message.
- **Time Format**: Backend expects `yyyy-MM-dd HH:mm:ss`. Convert from picker output before submit.
- **Party Size**: Must be positive integer. Compare with facility `capacity` (from facility list) when present.
- **Weekend Restriction**: If `requiresApproval` is false or `isBlocked` true, show info text. Weekend disallow is enforced server-side; display backend error message.
- **Cancellation Deadline**: After fetch of `ReservationDto`, compute deadline for UI display using backend response (deadline = `startTime - policyHours`). Policy hours are not returned; UI should rely on error message. Optional improvement: augment API to provide `cancellationDeadlineHours`.
- **Blocked Facilities**: If `isBlocked`, disable booking button and show reason.
- **Loading & Error States**: Provide spinner while fetch pending. Show inline error area for backend message.
- **Token Expiry**: Reuse existing global handler (auto redirect on 401).

## 6. Components & Responsibilities

| Component | Responsibility |
| --- | --- |
| `FacilityRequest.jsx` (existing) | Extend to fetch facility list, manage form state, call availability + create endpoints, display errors. |
| `FacilityReservationList` (new) | Render reservation table/list with cancel buttons. Shared between tabs. |
| `FacilityReservationCard` (new) | Individual card layout for mobile/responsive view. |
| `hooks/useFacilityReservations.ts` (new) | Encapsulate API calls (list, by status, cancel). |
| `hooks/useFacilities.ts` (new) | Fetch active facilities + optional caching. |
| `components/modals/CancelReservationModal.jsx` (new) | Confirmation modal that triggers cancel API and surfaces deadline info text. |

## 7. Error & Success Messages
- Surface `ApiResponse.message` for success.
- For errors, parse backend message string; fallback to generic “요청 처리 중 오류가 발생했습니다.”
- Provide localized client copies:
  - Availability conflict: “해당 시간에는 이미 다른 예약이 존재합니다.”
  - Weekend disallowed: “이 시설은 주말 예약이 허용되지 않습니다.”
  - Capacity exceeded: “요청 인원이 시설 정원을 초과합니다.”

## 8. Testing Checklist
- Facility list loads correctly and block/approval icons match backend data.
- Availability check shows conflicts when overlapping reservation exists.
- Create reservation:
  - Success path for approval-required and auto-approved facilities.
  - Validation errors for past date, end before start, party size 0, weekend restriction.
- My reservations:
  - Displays newly created reservation at top.
  - Filters by status update list correctly.
- Cancel reservation:
  - Succeeds before deadline, updates UI to `취소됨`.
  - Fails after deadline with backend error message shown.
- Token expiry: 401 redirects to login (existing behavior).

## 9. Future Enhancements (Backlog)
- Fetch facility-specific policy data (min/max duration, cancellation hours) via new backend endpoint to provide client-side hints.
- Add calendar view for availability.
- Add pagination for reservation list if volume grows.
- Support file attachments for reservation requests if needed.
