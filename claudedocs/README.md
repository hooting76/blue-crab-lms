# Blue Crab LMS 문서 저장소

> **작성일**: 2025-10-10  
> **목적**: 프로젝트 기술 문서 및 가이드 통합 관리

---

> 📦 **2025-10-18 재정리 안내**
>
> - 모든 세부 문서는 `archive/2025-10-18-reset/` 하위로 이동했습니다.
> - 최신화가 필요한 자료는 해당 아카이브 폴더에서 꺼내어 `claudedocs/` 루트로 복원한 뒤 업데이트하세요.
> - 아래 폴더 구조 및 안내는 아카이브 내부 경로를 기준으로 유지됩니다.


## 📁 폴더 구조

```
claudedocs/
claudedocs/
├── archive/2025-10-18-reset/...
│   └── 기술스택_및_버전정보.md
│
├── backend-guide/                 # 백엔드 개발 가이드
│   ├── 프론트엔드_개발자를위한_백엔드_가이드_인덱스.md
│   ├── 백엔드_폴더구조_빠른참조.md
│   ├── 백엔드_최신화_확인_및_업데이트_필요사항.md
│   ├── 백엔드-분석문서-vs-실제코드-비교보고서.md
│   └── 프론트엔드를_위한_백엔드_동작방식_설명계획.md
│
├── feature-docs/                  # 기능별 문서
│   ├── fcm/                       # FCM 푸시 알림
│   │   ├── fcm-code-review.md
│   │   ├── fcm-frontend-flow.md
│   │   ├── fcm-frontend-integration.md
│   │   ├── fcm-redis-flow.md
│   │   ├── fcm-admin-test.js
│   │   └── fcm-browser-test.js
│   │
│   └── facility-reservation/      # 시설 예약
│       ├── facility-reservation-backend-blueprint.md
│       ├── facility-reservation-critical-fixes.md
│       ├── facility-reservation-final-plan.md
│       ├── facility-reservation-implementation-plan.md
│       ├── facility-reservation-implementation-ready.md
│       ├── facility-reservation-issues-analysis.md
│       ├── facility-reservation-jpql-fixes.md
│       ├── facility-reservation-plan.md
│       ├── facility-reservation-prerequisites.md
│       ├── facility-reservation-user-flow-analysis.md
│       ├── facility-reservation-final-plan-v2.0-deprecated.md
│       ├── admin_booking_system.tsx
│       ├── facility_booking_system.tsx
│       └── facility_reservation_schema.sql
│
├── api-endpoints/                 # API 문서
│   └── api-documentation.md
│
├── archive/                       # 완료 문서 & 버그 리포트 보관소
│   ├── phase-reports/             # Phase별 완료 보고서, 회고, 스크린샷 가이드
│   ├── bugfixes/                  # 버그 수정 및 보안 개선 기록
│   ├── api-tester/                # API 테스터 프로젝트 산출물 및 가이드
│   └── reference/                 # 참고용 분석/리포트 문서
│
├── database/                      # 데이터베이스 스키마
│   ├── database-schema.md         # 스키마 상세 문서 (Markdown)
│   └── blue_crab_schema.sql       # 스키마 생성 SQL
│
└── backend-analysis/             # 백엔드 분석 문서
    ├── 00-Phase1-통합분석보고서.md
    ├── 01-전체구조-분석.md
    ├── 02-API-엔드포인트-분석.md
    ├── 03-의존성-관계-분석.md
    ├── config-analysis/
    ├── controller-analysis/
    ├── entity-analysis/
    └── repository-analysis/
```

---

## 📚 문서 카테고리

### 1️⃣ 기술 스택 (`tech-stack/`)

**실제 운영 환경의 기술 스택 및 버전 정보**

- ✅ **기술스택_및_버전정보.md**: SSH 접속을 통해 확인한 실제 운영 환경 정보
  - Java 21.0.7 LTS (Oracle JDK)
  - Apache Tomcat 9.0.108
  - Spring Boot 2.7.18
  - Redis 7.0.15
  - Debian 12 (bookworm)
  - 모든 라이브러리 버전 정보

**대상**: 프론트엔드 개발자, 백엔드 개발자, DevOps

---

### 2️⃣ 백엔드 가이드 (`backend-guide/`)

**백엔드 구조 및 개발 가이드**

#### 📖 주요 문서

1. **프론트엔드_개발자를위한_백엔드_가이드_인덱스.md**
   - 백엔드 문서 통합 인덱스
   - 프론트엔드 개발자를 위한 백엔드 가이드

2. **백엔드_폴더구조_빠른참조.md**
   - 백엔드 폴더 구조 설명
   - 각 레이어별 역할 및 책임

3. **백엔드_최신화_확인_및_업데이트_필요사항.md**
   - 백엔드 코드 최신화 상태
   - 업데이트 필요 사항

4. **백엔드-분석문서-vs-실제코드-비교보고서.md**
   - 분석 문서와 실제 코드 비교
   - 차이점 및 불일치 사항

5. **프론트엔드를_위한_백엔드_동작방식_설명계획.md**
   - 백엔드 동작 방식 설명 계획
   - 프론트엔드 연동 가이드

**대상**: 프론트엔드 개발자, 신규 백엔드 개발자

---

### 3️⃣ 기능별 문서 (`feature-docs/`)

#### 🔔 FCM 푸시 알림 (`fcm/`)

**Firebase Cloud Messaging 구현 가이드**

- **fcm-code-review.md**: 코드 리뷰 및 분석
- **fcm-frontend-flow.md**: 프론트엔드 구현 흐름
- **fcm-frontend-integration.md**: 프론트엔드 연동 가이드
- **fcm-redis-flow.md**: Redis를 활용한 FCM 세션 관리
- **fcm-admin-test.js**: 관리자 테스트 스크립트
- **fcm-browser-test.js**: 브라우저 테스트 스크립트

**기술 스택**:
- Firebase Admin SDK 9.7.0
- Redis 7.0.15 (세션 관리)
- Service Worker (브라우저 푸시)

**대상**: 프론트엔드 개발자, 푸시 알림 담당자

---

#### 🏢 시설 예약 (`facility-reservation/`)

**시설 예약 시스템 구현 가이드**

##### 📄 계획 및 분석 문서

1. **facility-reservation-plan.md**: 초기 계획
2. **facility-reservation-prerequisites.md**: 사전 요구사항
3. **facility-reservation-user-flow-analysis.md**: 사용자 플로우 분석
4. **facility-reservation-issues-analysis.md**: 이슈 분석
```
claudedocs/
├── archive/
│   ├── 2025-10-18-reset/
│   │   ├── tech-stack/
│   │   │   └── 기술스택_및_버전정보.md
│   │   │
│   │   ├── backend-guide/
│   │   │   ├── 프론트엔드_개발자를위한_백엔드_가이드_인덱스.md
│   │   │   ├── 백엔드_폴더구조_빠른참조.md
│   │   │   ├── 백엔드_최신화_확인_및_업데이트_필요사항.md
│   │   │   ├── 백엔드-분석문서-vs-실제코드-비교보고서.md
│   │   │   └── 프론트엔드를_위한_백엔드_동작방식_설명계획.md
│   │   │
│   │   ├── feature-docs/
│   │   │   ├── fcm/
│   │   │   │   ├── fcm-code-review.md
│   │   │   │   ├── fcm-frontend-flow.md
│   │   │   │   ├── fcm-frontend-integration.md
│   │   │   │   ├── fcm-redis-flow.md
│   │   │   │   ├── fcm-admin-test.js
│   │   │   │   └── fcm-browser-test.js
│   │   │   │
│   │   │   └── facility-reservation/
│   │   │       ├── facility-reservation-backend-blueprint.md
│   │   │       ├── facility-reservation-critical-fixes.md
│   │   │       ├── facility-reservation-final-plan.md
│   │   │       ├── facility-reservation-implementation-plan.md
│   │   │       ├── facility-reservation-implementation-ready.md
│   │   │       ├── facility-reservation-issues-analysis.md
│   │   │       ├── facility-reservation-jpql-fixes.md
│   │   │       ├── facility-reservation-plan.md
│   │   │       ├── facility-reservation-prerequisites.md
│   │   │       ├── facility-reservation-user-flow-analysis.md
│   │   │       ├── facility-reservation-final-plan-v2.0-deprecated.md
│   │   │       ├── admin_booking_system.tsx
│   │   │       ├── facility_booking_system.tsx
│   │   │       └── facility_reservation_schema.sql
│   │   │
│   │   ├── api-endpoints/
│   │   │   └── api-documentation.md
│   │   │
│   │   ├── database/
│   │   │   ├── database-schema.md
│   │   │   └── blue_crab_schema.sql
│   │   │
│   │   └── backend-analysis/
│   │       ├── 00-Phase1-통합분석보고서.md
│   │       ├── 01-전체구조-분석.md
│   │       ├── 02-API-엔드포인트-분석.md
│   │       ├── 03-의존성-관계-분석.md
│   │       ├── config-analysis/
│   │       ├── controller-analysis/
│   │       ├── entity-analysis/
│   │       └── repository-analysis/
│   │
│   └── ... (기존 아카이브 폴더)
│
├── DOCS_INDEX.md
└── README.md
```
**데이터베이스 정보**:
- 호스트: 121.165.24.26:55511
- 데이터베이스: blue_crab
- 18개 테이블 (USER_TBL, ADMIN_TBL, BOARD_TBL, FACILITY_TBL 등)

**대상**: 백엔드 개발자, DBA, 프론트엔드 개발자

---

### 6️⃣ 아카이브 (`archive/`)

**완료된 Phase 보고서 & 히스토리 문서**

- `phase-reports/`: Phase 1~4 완료 보고서, 종합 점검 결과, 히스토리 문서
- `bugfixes/`: 치명 이슈 해결 과정, 보안 개선 작업 기록
- `api-tester/`: API 테스터 프로젝트 문서, 콘솔/테스트 가이드
- `reference/`: DTO 리팩토링, 엔드포인트 갭 분석 등 참고용 분석 자료

**활용 포인트**
- 현황 파악에는 최신 디렉터리를 우선 확인하고, 과거 결정 및 회고는 `archive/`에서 추적
- `phase-reports/`에는 각 마일스톤의 스크린샷, 완료 보고서가 정리되어 있어 향후 회고 시 유용
- `api-tester/`와 `reference/`는 특정 프로젝트/분석 회상 시에만 참조

---

### 7️⃣ 백엔드 분석 (`backend-analysis/`)

**백엔드 코드 상세 분석**

#### 📊 통합 분석 보고서

- **00-Phase1-통합분석보고서.md**: Phase 1 통합 분석
- **01-전체구조-분석.md**: 전체 구조 분석
- **02-API-엔드포인트-분석.md**: API 엔드포인트 분석
- **03-의존성-관계-분석.md**: 의존성 관계 분석

#### 📂 레이어별 분석

- **config-analysis/**: 설정 레이어 분석
- **controller-analysis/**: 컨트롤러 레이어 분석
- **entity-analysis/**: 엔티티 레이어 분석
- **repository-analysis/**: 리포지토리 레이어 분석

**대상**: 백엔드 개발자, 아키텍트

---

## 🚀 문서 활용 가이드

### 신규 프론트엔드 개발자

1. **기술 스택 파악**: `tech-stack/기술스택_및_버전정보.md`
2. **데이터베이스 이해**: `database/database-schema.md`
3. **백엔드 이해**: `backend-guide/프론트엔드_개발자를위한_백엔드_가이드_인덱스.md`
4. **API 확인**: `api-endpoints/api-documentation.md`
5. **기능 구현**: `feature-docs/` 해당 기능 문서 참조

### 신규 백엔드 개발자

1. **기술 스택 파악**: `tech-stack/기술스택_및_버전정보.md`
2. **데이터베이스 스키마**: `database/database-schema.md`
3. **폴더 구조**: `backend-guide/백엔드_폴더구조_빠른참조.md`
4. **코드 분석**: `backend-analysis/` 레이어별 분석 문서
5. **기능 구현**: `feature-docs/` 해당 기능 문서 참조

### DevOps / 인프라 담당자

1. **운영 환경**: `tech-stack/기술스택_및_버전정보.md`
2. **데이터베이스**: `database/blue_crab_schema.sql`
3. **배포 정보**: WAR 파일 배포 방법, 환경 변수 설정
4. **서버 구성**: Tomcat, Redis, MinIO 설정

---

## 📝 문서 작성 규칙

### 파일명 규칙

- **한글 문서**: `기술스택_및_버전정보.md` (언더스코어 사용)
- **영문 문서**: `fcm-frontend-flow.md` (하이픈 사용)
- **코드 파일**: `fcm-admin-test.js`, `facility_reservation_schema.sql`

### 문서 구조

```markdown
# 제목

> **작성일**: YYYY-MM-DD  
> **대상**: 대상 독자  
> **목적**: 문서 목적

---

## 섹션 1

내용...

---

## 관련 문서

- [링크 1](./...)
- [링크 2](./...)
```

### 버전 관리

- **최신 문서**: `{기능명}-{타입}.md`
- **폐기 문서**: `{기능명}-{타입}-deprecated.md`
- **버전 표기**: 문서 하단에 버전 정보 명시

---

## 🔄 문서 업데이트 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2025-10-18 | 전체 문서 `archive/2025-10-18-reset/` 이동 및 루트 최소화 | GitHub Copilot |
| 2025-10-10 | SSH 접속으로 실제 버전 확인 및 업데이트 | GitHub Copilot |
| 2025-10-10 | 문서 폴더 구조 정리 및 README 작성 | GitHub Copilot |

---

## 📞 문의

- 기술 문서 관련: GitHub Issues
- 백엔드 API 관련: 백엔드 팀
- 프론트엔드 연동 관련: 프론트엔드 팀

---

**작성자**: GitHub Copilot  
**최종 수정일**: 2025-10-10  
**버전**: 1.0
