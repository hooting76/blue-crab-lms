# 출석 요청/승인 기능 구현 가이드

## 📋 개요

출석 요청/승인 기능을 구현하기 위한 단계별 작업 가이드입니다.

**전체 예상 소요 시간:** 약 25시간

---

## 🗂️ 문서 구조

### 📚 기본 문서
- **[출석요청승인_플로우.md](../출석요청승인_플로우.md)** - 전체 비즈니스 로직 및 플로우
- **[출석요청승인_플로우.drawio](../출석요청승인_플로우.drawio)** - 시퀀스 다이어그램

### 📊 데이터 구조
- **[ENROLLMENT_DATA_JSON_명세서.md](../../../전체가이드/데이터구조/ENROLLMENT_DATA_JSON_명세서.md)** - JSON 구조 상세 명세

### 🧪 테스트
- **[브라우저콘솔테스트/06-attendance/](../../../브라우저콘솔테스트/06-attendance/)** - API 테스트 스크립트

---

## 📝 작업 단계별 가이드

### ✅ Phase 1: 기반 작업 (완료)

| 단계 | 문서 | 상태 | 소요 시간 |
|------|------|------|-----------|
| 1-1. DB 스키마 확인 및 Entity 검증 | [Phase1-1_Entity검증.md](./Phase1-1_Entity검증.md) | ✅ 완료 | 1시간 |
| 1-2. DTO 클래스 생성 | [Phase1-2_DTO생성.md](./Phase1-2_DTO생성.md) | ✅ 완료 | 1시간 |

**Phase 1 산출물:**
- ✅ Entity 검증 완료 (`EnrollmentExtendedTbl.java`)
- ✅ DTO 클래스 9개 (`dto/Lecture/attendance/` 패키지)
- ✅ JSON 구조 명세서 작성
- ✅ 브라우저 콘솔 테스트 스크립트

---

### 🔄 Phase 2: Repository 및 Service Layer (완료)

| 단계 | 문서 | 상태 | 소요 시간 |
|------|------|------|-----------|
| 2-1. Repository 계층 구현 | **[Phase2-1_Repository구현.md](./Phase2-1_Repository구현.md)** | ✅ 완료 | 2시간 |
| 2-2. Service 계층 구현 | **[Phase2-2_Service구현.md](./Phase2-2_Service구현.md)** | ✅ 완료 | 4시간 |

**Phase 2-1 산출물:**
- ✅ Repository 메서드 3개 추가

**Phase 2-2 산출물:**
- ✅ AttendanceRequestService 인터페이스
- ✅ AttendanceRequestServiceImpl 구현 클래스 (500줄)
- ✅ JSON 파싱/직렬화 유틸리티 메서드
- ✅ 출석 통계 자동 계산 로직
- ✅ 패키지 구조 통일 (`dto/Lecture/Attendance`, `service/Lecture/Attendance`)

---

### 🌐 Phase 3: Controller 및 API (완료)

| 단계 | 문서 | 상태 | 소요 시간 |
|------|------|------|-----------|
| 3-1. Controller 구현 | **[Phase3-1_Controller구현.md](./Phase3-1_Controller구현.md)** | ✅ 완료 | 2시간 |
| 3-2. Security 설정 | **[Phase3-2_Security설정.md](./Phase3-2_Security설정.md)** | ✅ 완료 | 0.5시간 |

**Phase 3-1 산출물:**
- ✅ AttendanceController 클래스 (296줄)
- ✅ 4개 REST API 엔드포인트 (모두 POST 방식)
  - POST /api/attendance/request
  - POST /api/attendance/approve
  - POST /api/attendance/student/view
  - POST /api/attendance/professor/view
- ✅ JWT 인증 처리 및 권한 검증
- ✅ 예외 처리 및 응답 포맷팅
- ✅ 패키지 구조: `controller/Lecture/Attendance/`

**Phase 3-2 산출물:**
- ✅ SecurityConfig에 출석 API 권한 규칙 추가
- ✅ 학생용 API: authenticated
- ✅ 교수용 API: PROFESSOR, ADMIN 권한 필요
- ✅ CORS 설정 확인 (기존 /api/* 패턴 적용)

**Phase 3 주요 작업:**
- `POST /api/attendance/request` 엔드포인트
- `PUT /api/attendance/approve` 엔드포인트
- Security 권한 설정
- CORS 설정

---

### ⏰ Phase 4: 스케줄러 구현 (예정)

| 단계 | 문서 | 상태 | 소요 시간 |
|------|------|------|-----------|
| 4-1. 자동 승인 스케줄러 | **[Phase4-1_Scheduler구현.md](./Phase4-1_Scheduler구현.md)** | 🟡 대기 | 2시간 |

**Phase 4 주요 작업:**
- 7일 경과 자동 승인 로직
- `@Scheduled` cron 설정
- 배치 처리 최적화

---

### 🧪 Phase 5: 테스트 및 검증 (예정)

| 단계 | 문서 | 상태 | 소요 시간 |
|------|------|------|-----------|
| 5. 테스트 및 검증 | **[Phase5_테스트.md](./Phase5_테스트.md)** | 🟡 대기 | 7시간 |

**Phase 5 주요 작업:**
- Repository/Service 단위 테스트
- Controller 통합 테스트
- 브라우저 콘솔 API 테스트
- 스케줄러 테스트

---

### 📚 Phase 6: 문서화 및 배포 (예정)

| 단계 | 문서 | 상태 | 소요 시간 |
|------|------|------|-----------|
| 6. 문서화 및 배포 | **[Phase6_문서화배포.md](./Phase6_문서화배포.md)** | 🟡 대기 | 3.5시간 |

**Phase 6 주요 작업:**
- Swagger/OpenAPI 문서 작성
- 프론트엔드 연동 가이드
- 배포 체크리스트
- 롤백 계획

---

## 📊 진행 상황 요약

```
Phase 1: ████████████████████ 100% (완료)
Phase 2: ████████████████████ 100% (완료)
Phase 3: ████████████████████ 100% (완료)
Phase 4: ░░░░░░░░░░░░░░░░░░░░   0% (대기)
Phase 5: ░░░░░░░░░░░░░░░░░░░░   0% (대기)
Phase 6: ░░░░░░░░░░░░░░░░░░░░   0% (대기)
```

**전체 진행률:** 42% (10.5/25 시간)

---

## 🎯 다음 작업

### Phase 4-1: 자동 승인 스케줄러 구현
- **문서:** [Phase4-1_Scheduler구현.md](./Phase4-1_Scheduler구현.md)
- **예상 시간:** 2시간
- **주요 작업:**
  - @Scheduled 어노테이션으로 스케줄러 구현
  - 7일 경과된 출석 요청 자동 승인
  - 배치 처리 로직 및 로깅

---

## 📌 참고 사항

### 핵심 필드명
- **강의 식별**: `lecSerial` (LEC_SERIAL)
- **학생 식별**: `studentIdx` (USER_IDX)
- **회차**: `sessionNumber` (1~80)
- **출석 상태**: "출", "지", "결"

### JSON 구조
- `ENROLLMENT_DATA.attendance.summary` - 출석 통계
- `ENROLLMENT_DATA.attendance.sessions` - 확정 출석 (최대 80개)
- `ENROLLMENT_DATA.attendance.pendingRequests` - 대기 요청 (최대 80개)

### 자동 승인 로직
- **대기 기간**: 7일
- **스케줄러**: 매일 오전 5시 (Asia/Seoul)
- **자동 처리**: `tempApproved: true` → `status: "출"`

---

## 📞 문의

- **작성자**: BlueCrab Development Team
- **작성일**: 2025-10-23
- **버전**: 1.0.0
