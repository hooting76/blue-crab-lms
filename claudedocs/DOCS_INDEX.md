# 📚 Blue Crab LMS 문서 빠른 참조

> **최종 업데이트**: 2025-10-10  
> **문서 위치**: `/claudedocs/`

---

## 🚀 빠른 링크

### ⚡ 자주 찾는 문서

| 문서 | 경로 | 설명 |
|------|------|------|
| **기술 스택** | [tech-stack/기술스택_및_버전정보.md](./tech-stack/기술스택_및_버전정보.md) | 실제 운영 환경 버전 정보 |
| **데이터베이스** | [database/database-schema.md](./database/database-schema.md) | 전체 DB 스키마 (18개 테이블) |
| **백엔드 가이드** | [backend-guide/프론트엔드_개발자를위한_백엔드_가이드_인덱스.md](./backend-guide/프론트엔드_개발자를위한_백엔드_가이드_인덱스.md) | 백엔드 통합 가이드 |
| **API 문서** | [api-endpoints/api-documentation.md](./api-endpoints/api-documentation.md) | REST API 엔드포인트 |
| **시설 예약 API** ⭐ | [api-endpoints/facility-reservation-api-complete.md](./api-endpoints/facility-reservation-api-complete.md) | 시설 예약 API 완전 가이드 |
| **시설 예약 빠른 참조** ⚡ | [api-endpoints/facility-reservation-api-quick-reference.md](./api-endpoints/facility-reservation-api-quick-reference.md) | 시설 예약 API 빠른 참조 |
| **FCM 가이드** | [feature-docs/fcm/README.md](./feature-docs/fcm/README.md) | 푸시 알림 구현 가이드 |
| **시설 예약 정책** 🆕 | [feature-docs/facility-reservation/consecutive-time-only-policy.md](./feature-docs/facility-reservation/consecutive-time-only-policy.md) | 연속된 시간대만 예약 가능 정책 |

---

## 📂 폴더별 문서

### 🔧 [tech-stack/](./tech-stack/)
**기술 스택 및 버전 정보**

- ✅ Java 21.0.7 LTS
- ✅ Tomcat 9.0.108
- ✅ Spring Boot 2.7.18
- ✅ Redis 7.0.15

---

### 📖 [backend-guide/](./backend-guide/)
**백엔드 개발 가이드**

1. 프론트엔드 개발자를 위한 백엔드 가이드 인덱스
2. 백엔드 폴더 구조 빠른 참조
3. 백엔드 최신화 확인 및 업데이트 필요사항
4. 백엔드-분석문서-vs-실제코드-비교보고서
5. 프론트엔드를 위한 백엔드 동작방식 설명계획

---

### 🎯 [feature-docs/](./feature-docs/)
**기능별 문서**

#### 📱 [fcm/](./feature-docs/fcm/)
**Firebase Cloud Messaging**
- fcm-code-review.md
- fcm-frontend-flow.md
- fcm-frontend-integration.md
- fcm-redis-flow.md

#### 🏢 [facility-reservation/](./feature-docs/facility-reservation/)
**시설 예약 시스템**
- **consecutive-time-only-policy.md** 🆕 (연속된 시간대만 예약 가능 정책)
- facility-reservation-optimization-2025-10-10.md (성능 최적화)
- facility-reservation-user-flow-complete.md (사용자 플로우)
- UI_MISSING_FEATURES_ANALYSIS.md (UI 누락 기능 분석)
- UI_QUICK_CHECKLIST.md (UI 체크리스트)
- facility-reservation-final-plan.md
- facility-reservation-critical-fixes.md
- facility-reservation-jpql-fixes.md
- admin_booking_system.tsx
- facility_booking_system.tsx

---

### 🔌 [api-endpoints/](./api-endpoints/)
**API 문서**

- api-documentation.md (전체 API 개요)
- **facility-reservation-api-complete.md** ⭐ (시설 예약 API 완전 가이드)
- **facility-reservation-api-quick-reference.md** ⚡ (시설 예약 API 빠른 참조)

---

### 🧪 API Testing Tool
**API 테스터 개발 문서**

- **api-tester-phase1.1-complete.md** ✅ (UI Redesign 완료)
- **api-tester-phase1.2-complete.md** ✅ (Badge Update 완료)
- **api-tester-phase1.3-complete.md** ✅ (Auth Tab Navigation 완료)
- **PHASE1.2-COMPLETION-REPORT.md** 📊 (Phase 1.2 완료 보고서)
- **PHASE1.2-SUMMARY.md** 📝 (빠른 참조용 요약)
- api-tester-refactoring-summary.md (리팩토링 이력)
- api-tester-testing-guide.md (테스트 가이드)
- bugfix-api-templates-path.md (버그 수정 이력)
- console-api-tester-guide.md (콘솔 가이드)
- feature-history-body-preview.md (히스토리 기능)

**개발 진행 상황**:
- ✅ Phase 1.1: 2-row 레이아웃 UI 재설계
- ✅ Phase 1.2: Body Template Badge 실시간 업데이트
- ✅ Phase 1.3: 일반/관리자 로그인 탭 네비게이션
- 🔜 Phase 2: 추가 기능 개선 (키보드 단축키, 다크 모드 등)

---

### �️ [database/](./database/)
**데이터베이스 스키마**

- ✅ database-schema.md (상세 문서)
- ✅ blue_crab_schema.sql (SQL 스크립트)

**18개 테이블**:
- USER_TBL, ADMIN_TBL (사용자 관리)
- BOARD_TBL, BOARD_ATTACHMENT_TBL (게시판)
- FACILITY_TBL, FACILITY_RESERVATION_TBL, FACILITY_BLOCK_TBL, FACILITY_RESERVATION_LOG (시설 예약)
- FCM_TOKEN_TABLE (푸시 알림)
- LAMP_TBL, LAMP_USAGE_LOG (열람실)
- LEC_TBL, REGIST_TABLE, SERIAL_CODE_TABLE, FACULTY, DEPARTMENT (학사)
- RENT_TABLE (레거시)

---

### �📊 [backend-analysis/](./backend-analysis/)
**백엔드 상세 분석**

- 00-Phase1-통합분석보고서.md
- 01-전체구조-분석.md
- 02-API-엔드포인트-분석.md
- 03-의존성-관계-분석.md
- config-analysis/
- controller-analysis/
- entity-analysis/
- repository-analysis/
- service-analysis/
- util-analysis/

---

## 🎓 학습 경로

### 신규 프론트엔드 개발자

```
1. 기술 스택 파악
   └─ tech-stack/기술스택_및_버전정보.md

2. 백엔드 이해
   └─ backend-guide/프론트엔드_개발자를위한_백엔드_가이드_인덱스.md

3. API 확인
   └─ api-endpoints/api-documentation.md

4. 기능 구현
   └─ feature-docs/{기능명}/
```

### 신규 백엔드 개발자

```
1. 기술 스택 파악
   └─ tech-stack/기술스택_및_버전정보.md

2. 폴더 구조 이해
   └─ backend-guide/백엔드_폴더구조_빠른참조.md

3. 코드 분석
   └─ backend-analysis/

4. 기능 구현
   └─ feature-docs/{기능명}/
```

### DevOps / 인프라

```
1. 운영 환경 확인
   └─ tech-stack/기술스택_및_버전정보.md
      - Java 21.0.7 LTS
      - Tomcat 9.0.108
      - Redis 7.0.15
      - Debian 12

2. 배포 정보
   └─ WAR 파일 배포 방법
   └─ 환경 변수 설정
```

---

## 🔍 문서 검색 팁

### 키워드로 찾기

- **버전 정보**: `tech-stack/`
- **API**: `api-endpoints/`
- **푸시 알림**: `feature-docs/fcm/`
- **시설 예약**: `feature-docs/facility-reservation/`
- **백엔드 구조**: `backend-guide/` 또는 `backend-analysis/`

### 파일명으로 찾기

```powershell
# PowerShell에서 검색
Get-ChildItem -Path .\claudedocs -Recurse -Filter "*키워드*.md"
```

---

## 📋 문서 작성 규칙

### 신규 문서 추가 시

1. **적절한 폴더 선택**
   - 기술 스택: `tech-stack/`
   - 백엔드 가이드: `backend-guide/`
   - 기능 문서: `feature-docs/{기능명}/`

2. **파일명 규칙**
   - 한글: `기술스택_및_버전정보.md` (언더스코어)
   - 영문: `fcm-frontend-flow.md` (하이픈)

3. **문서 헤더**
   ```markdown
   # 제목
   
   > **작성일**: YYYY-MM-DD  
   > **대상**: 대상 독자  
   > **목적**: 문서 목적
   ```

4. **README.md 업데이트**
   - 해당 폴더의 README.md에 추가
   - 이 문서(DOCS_INDEX.md)에 링크 추가

---

## 🔄 최근 업데이트

| 날짜 | 내용 | 문서 |
|------|------|------|
| 2025-10-11 | **API Tester Phase 1.3 완료** ✅ | api-tester-phase1.3-complete.md |
| 2025-10-11 | 일반/관리자 로그인 탭 네비게이션 | status.html, api-tester.css, ui-utils.js |
| 2025-10-11 | **API Tester Phase 1.2 완료** ✅ | api-tester-phase1.2-complete.md |
| 2025-10-11 | Body Template Badge 실시간 업데이트 | body-template-manager.js |
| 2025-10-10 | **시설 예약 API 완전 가이드 작성** ⭐ | facility-reservation-api-complete.md |
| 2025-10-10 | **시설 예약 API 빠른 참조 작성** ⚡ | facility-reservation-api-quick-reference.md |
| 2025-10-10 | **연속된 시간대만 예약 가능 정책** 🆕 | consecutive-time-only-policy.md |
| 2025-10-10 | SSH 접속으로 실제 버전 확인 | 기술스택_및_버전정보.md |
| 2025-10-10 | 문서 폴더 구조 정리 | 전체 |
| 2025-10-10 | README 파일 생성 | 각 폴더 |

---

## 📞 문의 및 피드백

- **문서 오류**: GitHub Issues
- **내용 추가 요청**: Pull Request
- **질문**: 팀 채널

---

**작성자**: GitHub Copilot  
**최종 수정일**: 2025-10-11  
**버전**: 1.1
