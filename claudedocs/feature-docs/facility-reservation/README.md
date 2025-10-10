# 시설 예약 시스템 구현 가이드

> **폴더**: `feature-docs/facility-reservation/`  
> **목적**: 시설 예약 시스템 설계, 구현 및 버그 수정 가이드

---

## 🏢 개요

**시설 예약 시스템**

- **사용자 기능**: 시설 조회, 예약, 취소
- **관리자 기능**: 시설 관리, 예약 승인/거부
- **시설 유형**: 회의실, 강의실, 체육시설 등

---

## 📄 문서 목록

### 🎯 **시작하기 - 필독 문서**

#### ⭐ COMPLETE_SUMMARY.md
**시설물 예약 시스템 최종 정리** (2025-10-10)
- 전체 시스템 구조
- 15개 API 엔드포인트 요약
- 성능 최적화 결과
- 프론트엔드 작업 가이드
- 👉 **신규 개발자는 여기서 시작!**

#### ⚡ consecutive-time-only-policy.md 🆕
**연속된 시간대만 예약 가능 정책**
- 핵심 규칙: 같은 날짜 내에서만 예약 가능
- 도입 이유: 구현 복잡도 70% 감소
- 프론트엔드 구현 가이드
- 테스트 케이스

---

### 📚 **API 문서 (완전 가이드)**

> **위치:** `claudedocs/api-endpoints/`

#### 🌟 facility-reservation-api-complete.md
**시설 예약 API 완전 가이드**
- 유저 API 10개 상세 명세
- 관리자 API 5개 상세 명세
- Request/Response 예시
- 에러 응답 상세
- TypeScript 타입 정의
- 프론트엔드 통합 예시 코드
- 성능 최적화 설명

#### ⚡ facility-reservation-api-quick-reference.md
**시설 예약 API 빠른 참조**
- 핵심 내용만 요약
- 빠른 검색용
- Enum 타입 정리
- curl 예시

---

### 🏗️ **설계 및 구현**

#### facility-reservation-final-plan.md
**최종 설계 문서**
- 확정된 설계
- 구현 가이드
- 테스트 계획

#### facility-reservation-backend-blueprint.md
**백엔드 구조 설계**
- 엔티티 설계
- 서비스 레이어 설계
- API 설계

---

### ⚡ **성능 최적화 및 버그 수정**

#### facility-reservation-optimization-2025-10-10.md ⭐
**성능 최적화 보고서**
- 락 전략 최적화 (승인 정책별 차별화)
  - 즉시 승인 시설: 비관적 락 사용
  - 승인 필요 시설: 락 없이 처리 (80-90% 성능 향상)
- 중복 조회 제거 (50% 쿼리 감소)
- N+1 문제 해결 (98% 쿼리 감소)

#### facility-reservation-critical-fixes.md
**치명적 버그 수정**
- 예약 중복 문제
- 시간대 충돌
- 트랜잭션 이슈

#### facility-reservation-jpql-fixes.md
**JPQL 쿼리 수정**
- N+1 문제 해결
- 쿼리 최적화
- 인덱스 추가

---

### 📊 **사용자 플로우 및 UI 분석**

#### facility-reservation-user-flow-complete.md
**사용자 플로우 완전 가이드**
- 사용자 시나리오
- 관리자 시나리오
- 플로우 차트

#### UI_MISSING_FEATURES_ANALYSIS.md
**UI 누락 기능 분석**
- 현재 UI 상태 (하드코딩 데이터)
- 누락된 기능 목록
- 구현 우선순위

#### UI_QUICK_CHECKLIST.md
**UI 개발 빠른 체크리스트**
- 필수 구현 항목
- 관리자 기능
- 사용자 안내

---

### � **계획 및 분석 (Phase 1)**

#### facility-reservation-plan.md
**초기 계획 문서**
- 프로젝트 목표
- 기능 요구사항
- 기술 스택

#### facility-reservation-prerequisites.md
**사전 요구사항**
- 필요한 엔티티
- 데이터베이스 스키마
- API 엔드포인트

#### facility-reservation-user-flow-analysis.md
**사용자 플로우 분석**
- 사용자 시나리오
- 관리자 시나리오
- UI/UX 플로우

#### facility-reservation-issues-analysis.md
**이슈 분석**
- 발견된 문제점
- 원인 분석
- 해결 방안

---

### 🎨 **UI 컴포넌트 (참고용)**

#### facility-reservation-final-plan-v2.0-deprecated.md
**폐기된 v2.0 계획**
- 과거 버전 (참고용)
- 변경 이력

---

### 💻 코드 파일

#### admin_booking_system.tsx
**관리자 예약 관리 UI (React)**
```tsx
// 시설 관리
// 예약 승인/거부
// 통계 조회
```

#### facility_booking_system.tsx
**사용자 예약 UI (React)**
```tsx
// 시설 목록
// 예약 생성
// 예약 조회/취소
```

#### facility_reservation_schema.sql
**데이터베이스 스키마**
```sql
-- 시설 테이블
CREATE TABLE facilities ...

-- 예약 테이블
CREATE TABLE reservations ...
```

---

## 🗂️ 데이터베이스 스키마

### facilities (시설)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | 시설 ID |
| name | VARCHAR(100) | 시설명 |
| type | VARCHAR(50) | 시설 유형 |
| capacity | INT | 수용 인원 |
| location | VARCHAR(200) | 위치 |
| status | VARCHAR(20) | 상태 |

### reservations (예약)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | 예약 ID |
| facility_id | BIGINT | 시설 ID (FK) |
| user_id | BIGINT | 사용자 ID (FK) |
| start_time | DATETIME | 시작 시간 |
| end_time | DATETIME | 종료 시간 |
| status | VARCHAR(20) | 예약 상태 |
| purpose | TEXT | 사용 목적 |

---

## 🔌 API 엔드포인트

### 사용자 API

```
GET    /api/facilities              # 시설 목록
GET    /api/facilities/{id}         # 시설 상세
POST   /api/reservations            # 예약 생성
GET    /api/reservations/my         # 내 예약 목록
DELETE /api/reservations/{id}       # 예약 취소
```

### 관리자 API

```
POST   /api/admin/facilities        # 시설 생성
PUT    /api/admin/facilities/{id}   # 시설 수정
DELETE /api/admin/facilities/{id}   # 시설 삭제
GET    /api/admin/reservations      # 전체 예약 목록
PATCH  /api/admin/reservations/{id}/approve  # 예약 승인
PATCH  /api/admin/reservations/{id}/reject   # 예약 거부
```

---

## 🚀 현재 상태 및 다음 단계

### ✅ 완료된 작업 (2025-10-10)

#### 백엔드 구현 완료
- [x] 엔티티 및 리포지토리 (5개)
- [x] 서비스 로직 (3개)
  - `FacilityService.java`
  - `FacilityReservationService.java`
  - `AdminFacilityReservationService.java`
- [x] 컨트롤러 (3개)
  - `FacilityController.java` - 시설 조회 API (5개)
  - `FacilityReservationController.java` - 예약 관리 API (5개)
  - `AdminFacilityReservationController.java` - 관리자 API (5개)
- [x] 검증 로직
  - 연속된 시간대 검증 (같은 날짜만)
  - 예약 충돌 검증
  - 시설 차단 기간 검증

#### 성능 최적화 완료
- [x] 락 전략 최적화 (80-90% 성능 향상)
- [x] N+1 쿼리 제거 (98% 쿼리 감소)
- [x] 중복 조회 제거 (50% 쿼리 감소)

#### 문서화 완료
- [x] API 완전 가이드 (400+ 라인)
- [x] API 빠른 참조
- [x] 연속된 시간대 정책 문서
- [x] 성능 최적화 보고서
- [x] UI 누락 기능 분석
- [x] 최종 정리 문서

### 🚧 진행 중인 작업

#### 프론트엔드 통합 (예상 6일)

**1단계: 기본 통합 (2일)**
- [ ] API 클라이언트 작성
- [ ] TypeScript 타입 정의
- [ ] 시설 목록 연동
- [ ] 예약 생성 기능

**2단계: 핵심 기능 (2일)**
- [ ] 실시간 가용성 체크
- [ ] 날짜 검증 (같은 날짜만)
- [ ] 에러 핸들링
- [ ] 로딩 상태 관리

**3단계: 관리자 기능 (1일)**
- [ ] 승인 대기 목록
- [ ] 승인/반려 처리
- [ ] 통계 대시보드
- [ ] 대기 건수 뱃지

**4단계: UI/UX 개선 (1일)**
- [ ] 로딩 스피너
- [ ] 토스트 알림
- [ ] 상태별 색상
- [ ] 안내 메시지

### 📈 진행률

```
전체 진행률: ████████░░ 80%

백엔드:      ██████████ 100% ✅
문서화:      ██████████ 100% ✅
프론트엔드:  ░░░░░░░░░░   0% 🚧
```

---

## 🎓 신규 개발자 가이드

### 프론트엔드 개발자

**1단계: 정책 이해 (30분)**
```
→ consecutive-time-only-policy.md
```

**2단계: API 확인 (1시간)**
```
→ facility-reservation-api-quick-reference.md
```

**3단계: 상세 가이드 (2시간)**
```
→ facility-reservation-api-complete.md
```

**4단계: 개발 시작**
```
→ UI_QUICK_CHECKLIST.md
```

### 백엔드 개발자

**1단계: 최종 정리 확인 (30분)**
```
→ COMPLETE_SUMMARY.md
```

**2단계: 코드 분석**
```
→ FacilityReservationService.java
→ 성능 최적화 부분 확인
```

**3단계: 정책 이해**
```
→ consecutive-time-only-policy.md
→ facility-reservation-optimization-2025-10-10.md
```

---

## 🔍 빠른 검색

### 키워드로 찾기

| 찾고 싶은 내용 | 문서 |
|--------------|------|
| **전체 시스템 이해** | COMPLETE_SUMMARY.md |
| **API 스펙** | facility-reservation-api-complete.md |
| **빠른 참조** | facility-reservation-api-quick-reference.md |
| **날짜 제약 정책** | consecutive-time-only-policy.md |
| **성능 최적화** | facility-reservation-optimization-2025-10-10.md |
| **UI 구현 가이드** | UI_QUICK_CHECKLIST.md |
| **프론트엔드 예시** | facility-reservation-api-complete.md (섹션 9) |

---

## ⚠️ 주의사항

### 예약 중복 방지

```java
@Transactional
public Reservation createReservation(...) {
    // 1. 시간대 중복 체크
    List<Reservation> conflicts = reservationRepository
        .findConflictingReservations(facilityId, startTime, endTime);
    
    if (!conflicts.isEmpty()) {
        throw new ConflictException("예약 시간대가 중복됩니다.");
    }
    
    // 2. 예약 생성
    ...
}
```

### 트랜잭션 관리

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public Reservation updateReservation(...) {
    // 직렬화 격리 수준으로 동시성 문제 방지
}
```

---

## 🐛 알려진 이슈

### ✅ 해결됨

- [x] 예약 중복 문제
- [x] N+1 쿼리 문제
- [x] 시간대 충돌

### 🔄 진행 중

- [ ] 반복 예약 기능
- [ ] 알림 시스템 연동

---

## 📅 최종 업데이트

- **날짜**: 2025-10-10
- **버전**: 1.0
- **다음 버전 계획**: 반복 예약 기능 추가
