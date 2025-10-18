# Frontend Checklist: Facility Reservation User Identity Normalization

## 배경
- 백엔드 예약/알림 흐름이 이메일 기반 `USER_CODE` 저장에서 학번 기반 정규화로 전환되었습니다.
- 예약 행에는 새 `USER_EMAIL` 스냅샷 컬럼이 추가되어, 학번(`userCode`)과 이메일(`userEmail`)이 동시에 내려옵니다.
- FCM 데이터 전송 API는 학번 배열(`userCodes`)을 기본 입력으로 사용하도록 업데이트되었습니다.

## 완료된 조정 사항
- `backend/BlueCrab/src/main/resources/static/js/api-test-standalone.js`
  - FCM 단건/배치 템플릿이 학번 예시(`20240001` 등)로 교체되었습니다.
- `backend/BlueCrab/src/main/resources/static/config/api-templates.json`
  - 동일한 예시 페이로드 업데이트로 관리자 테스트 페이지와 API 템플릿이 일관성을 유지합니다.

## 프론트엔드 후속 작업
1. **관리자 시설 예약 UI 동기화**
   - 파일: `component/common/Facilities/AdminFacilityReservations.jsx`, `component/common/Facilities/AdminReservationDetailModal.jsx`
   - 목록/상세 화면에서 `userCode`를 1순위로 노출하고, 필요 시 이메일은 보조 정보로 표기합니다.
   - 예약 승인/반려 액션에서 백엔드로 전달되는 추가 필드가 없다면 확인만 하면 되지만, 사용자 식별을 이메일로 의존하는 로직이 남아있지 않은지 점검합니다.

2. **FCM 발송/테스트 UI 검수**
   - 파일: `component/admin/api` 내부에 있는 FCM 관련 화면(예: 알림 발송 패널)에서 입력 필드 레이블과 도움말을 `userCodes` 기준으로 수정합니다.
   - 여러 학번을 쉼표/줄바꿈으로 받는 입력 UX가 필요한지 확인하고, 백엔드 API 스펙과 일치하는지 다시 테스트합니다.

3. **테스트/QA 도구 안내 갱신**
   - 파일: `docs/fcm-notification-admin.md`, `docs/facility-reservation-frontend-spec-ko.md` 등 FCM/시설 예약 관련 문서에서 이메일 기반 예시를 학번 예시로 교체합니다.
   - 개발자 콘솔 튜토리얼(예: `src/utils/consoleApiTester.js`의 help 텍스트)은 로그인 계정은 이메일을 유지하되, FCM 발송 예시가 있다면 학번 기반으로 수정합니다.

4. **폼 밸리데이션 재검토**
   - 회원 찾기/예약 신청 등에서 학번 입력 시 길이·패턴을 검증하는 로직이 없다면 추가를 고려합니다.
   - 학번이 비어 있을 때 이메일로 대체하던 로직이 남아 있다면 제거하고, 백엔드에서 내려오는 `userEmail`은 읽기 전용으로만 사용합니다.

5. **엔드투엔드 점검**
   - 최신 학번 샘플 데이터를 사용하여 관리자 승인 → 데이터 전용 FCM 발송까지 테스트합니다.
   - 문제 발생 시 백엔드 로그와 브라우저 네트워크 탭을 함께 캡처하여 공유합니다.

## 권장 테스트 시나리오
- 일반 사용자로 시설 예약 생성 → 관리자 화면에서 학번이 정상 표기되는지 확인.
- 동일 예약을 승인/반려하여 FCM 도착 여부 확인.
- 다중 학번 입력으로 배치 FCM 발송 → 성공/실패 응답 코드 확인.
- QA 콘솔(`api-test.html`)에서 학번 입력 후 API 호출이 성공하는지 재확인.

## 프론트엔드 영향 분석 (기존 코드 대비)
- `component/common/Facilities/AdminFacilityReservations.jsx`
   - 현재 `userEmail || userCode` 순으로 보조 정보를 렌더링하고 있어 이메일이 있을 때 학번이 가려집니다. 조건을 `userCode || userEmail`로 뒤집고, 스타일 가이드를 따라 학번을 기본, 이메일을 회색 보조 라벨로 표기합니다.
   - 예약 승인/반려 버튼 핸들러에서 `selectedRow.userEmail`에 의존하는 부분이 있는지 확인하고, 필요 시 `userCode` 중심으로 정리합니다.
- `component/common/Facilities/AdminReservationDetailModal.jsx`
   - 상단 사용자 정보 `userName (userCode)` 출력은 정상이나, `userEmail`을 메시지 템플릿 또는 관리자 메모에 곧바로 사용한다면 학번 기반 안내 문구를 추가합니다.
   - 승인/반려 요청 시 payload에 `userEmail`을 수동으로 넣던 코드가 있다면 제거합니다(백엔드가 학번으로 정규화).
- `component/common/Facilities/FacilityReservationRequestForm.jsx` (또는 사용자 예약 폼)
   - 사용자가 직접 학번을 입력하는 필드가 있다면 이메일 fallback 로직을 삭제하고, 학번 형식 검증을 강화합니다.
- `src/api/facilityReservationApi.js` 등 API 래퍼
   - 이메일을 기본값으로 두고 있는 파라미터가 남아있는지 검색 (`userEmail`, `email`, `targeta`) 후 학번 프로퍼티로 전환합니다.
- `component/admin/api/**`
   - FCM 발송 패널의 필드 라벨/placeholder를 “학번” 기준으로 교체하고, 이전 이메일 예시는 문서/툴킷과 함께 업데이트합니다.
- `src/utils/consoleApiTester.js`
   - Help 텍스트에 소개된 FCM 예제가 이메일을 사용 중이므로, 신규 학번 기반 샘플과 일치하도록 수정합니다.
