# Blue Crab LMS Frontend Docs

> **작성일**: 2025-10-18  
> **목적**: 프론트엔드/알림 관련 최신 문서 경로 안내 및 보관 정책 명시

---

## 📁 디렉터리 개요

- `backend-api-catalog.md` — 백엔드 전 API 목록 (자동 생성)
- `facility-reservation/` — 시설 예약 프론트/백엔드 명세 및 UX 문서
- `push-notification/` — FCM/웹푸시 플로우, 이슈 리포트, QA 문서
- `frontend-usercode-update.md` — 사용자 코드 정책 최신화 정리
- `reading-room-pre-expiry-alert-design.md` — 열람실 만료 알림 설계
- `test-page-review.md` — 테스트 페이지 QA 피드백 정리
- `archive/` — 날짜별 보관 문서 (히스토리, 회고)

---

## 🗂️ 아카이브 정책

- 날짜가 포함된 리포트 및 마일스톤 완료 문서는 `archive/<YYYY-MM>/`으로 이동
- 최신 작업에 필요한 문서만 상위 디렉터리에 유지
- 신규 리포트 작성 시, 작업 완료 후 `archive/`로 이동 여부 판단해 표기

### 현재 아카이브 분류

| 폴더 | 용도 |
|------|------|
| `archive/2025-10/` | 2025년 10월 작업 보고 및 변경 이력 (예: Git 히스토리, 시설 업데이트) |


## 📦 주제별 폴더 안내

### `facility-reservation/`
- README.md — 폴더 내 문서 가이드
- `facility-reservation.md` — 백엔드/서비스 개요
- `facility-reservation-frontend-spec.md` — 영문 프론트엔드 명세
- `facility-reservation-frontend-spec-ko.md` — 한글 프론트엔드 명세

### `push-notification/`
- README.md — 폴더 내 문서 가이드
- FCM 알림 플로우(관리자/사용자/시스템) 정리
- `fcm-push-flow.md`, `fcm-notification-*.md`, `push-notification-double-fire.md`
- 푸시 이슈 재현 및 대응 전략

## ✅ 유지보수 체크리스트

- `backend-api-catalog.md`는 스크립트 `python tmp/generate_api_catalog.py`로 갱신
- 새 기능 정의 시 `facility-reservation/` 또는 `push-notification/` 하위에 문서를 추가
- 사용 종료된 문서는 `archive/` 이동 후 상단 목록에서 제거
- 문서 명세 변경 시 작성일과 목적 업데이트
