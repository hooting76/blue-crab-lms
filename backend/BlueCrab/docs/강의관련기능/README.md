# 강의 관리 시스템 문서

> **작성일**: 2025-10-10  
> **업데이트**: 2025-10-14  
> **버전**: 5.0 (DTO 패턴 적용 완료)  
> **변경사항**: 
> - Phase 1-2: 데이터베이스 구축 완료 ✅
> - Phase 3: Entity 3개, DTO 11개 생성 완료 ✅
> - Phase 4: Repository 3개 생성 완료 ✅
> - Phase 5: Service 레이어 완료 ✅
> - Phase 6: Controller 레이어 완료 ✅
> - **Phase 6.5: EnrollmentController DTO 패턴 적용 완료 ⭐**
> - **HTTP 400 Hibernate Lazy Loading 이슈 해결 완료 ✅**
> - 폴더 구조화 완료 (entity/Lecture/, dto/Lecture/, repository/Lecture/) ✅

---

## 📚 문서 개요

강의 관리 시스템의 **최소화 구현**을 위한 기술 문서입니다. **신규 테이블 2개**로 모든 기능을 구현하는 효율적인 설계를 제공합니다.

**명명 규칙**: 모든 테이블/컬럼명은 **대문자 + 언더스코어** (예: `USER_TBL`, `LEC_IDX`, `ENROLLMENT_DATA`)

### 🎯 **핵심 특징**
- ✅ **신규 테이블 최소화**: 기존 테이블 확장 + 2개 신규 테이블
- ✅ **JSON 데이터 통합**: 복잡한 데이터를 유연하게 저장
- ✅ **단계적 구현**: Phase별로 체계적인 개발 진행
- ✅ **확장성 보장**: 필요시 기능별 테이블 분리 가능
- ✅ **폴더 구조화**: 기능별 하위 폴더로 체계적 관리

---

## 📊 **구현 현황**

### ✅ Phase 1-2: 데이터베이스 구축 (완료)
- [x] USER_TBL 확장 (LECTURE_EVALUATIONS 추가)
- [x] LEC_TBL 확장 (LEC_CURRENT, LEC_YEAR, LEC_SEMESTER 추가)
- [x] ENROLLMENT_EXTENDED_TBL 생성
- [x] ASSIGNMENT_EXTENDED_TBL 생성
- [x] 외래키 및 인덱스 설정

### ✅ Phase 3: Entity & DTO 레이어 (완료)

#### Entity 클래스 (3개)
- [x] **LecTbl.java** (`entity/Lecture/LecTbl.java`)
  - 18개 필드 매핑
  - 비즈니스 메서드 포함
  
- [x] **EnrollmentExtendedTbl.java** (`entity/Lecture/EnrollmentExtendedTbl.java`)
  - @ManyToOne 관계 (LecTbl, UserTbl)
  - JSON 데이터 구조 문서화
  
- [x] **AssignmentExtendedTbl.java** (`entity/Lecture/AssignmentExtendedTbl.java`)
  - @ManyToOne 관계 (LecTbl)
  - JSON 데이터 구조 문서화

#### DTO 클래스 (11개)
- [x] LectureDto, LectureDetailDto
- [x] LectureCreateRequest, LectureUpdateRequest
- [x] EnrollmentDto, EnrollmentCreateRequest
- [x] AttendanceDto, GradeDto
- [x] AssignmentDto, AssignmentSubmissionDto, AssignmentStatisticsDto

### ✅ Phase 4: Repository 레이어 (완료)

#### Repository 인터페이스 (3개)
- [x] **LecTblRepository.java** (`repository/Lecture/LecTblRepository.java`)
  - 강의 조회: 강의코드, 교수명, 학년/학기별 조회
  - 수강신청 관련: 상태별, 정원 확인 조회
  - 복합 검색: 학년/학기/전공/상태 복합 필터링
  - 수강 인원 관리: 증가/감소 메서드
  - 통계: 전체/상태별/교수별/전공별 통계
  - 총 **30개 메서드** 제공
  
- [x] **EnrollmentExtendedTblRepository.java** (`repository/Lecture/EnrollmentExtendedTblRepository.java`)
  - 수강신청 조회: 학생별, 강의별 조회
  - JOIN FETCH: N+1 문제 방지를 위한 최적화 쿼리
  - 배치 조회: 여러 강의/학생의 수강신청 일괄 조회
  - 통계: 수강생 수, 수강신청 건수 조회
  - 삭제: 수강 취소 관련 메서드
  - 총 **20개 메서드** 제공
  
- [x] **AssignmentExtendedTblRepository.java** (`repository/Lecture/AssignmentExtendedTblRepository.java`)
  - 과제 조회: 강의별 과제 목록 조회
  - JOIN FETCH: 강의 정보 포함 최적화 조회
  - 배치 조회: 여러 강의의 과제 일괄 조회
  - 정렬: 최신순/오래된순 조회
  - 통계: 과제 수 조회
  - JSON 기반 확장 가능 설계
  - 총 **15개 메서드** 제공

#### Repository 주요 특징
- ✅ **Spring Data JPA 활용**: 메서드 네이밍 규칙으로 쿼리 자동 생성
- ✅ **JPQL 최적화**: @Query로 복잡한 조회 쿼리 작성
- ✅ **N+1 문제 방지**: JOIN FETCH 활용
- ✅ **배치 처리**: IN 절을 활용한 일괄 조회
- ✅ **상세한 주석**: 각 메서드의 용도와 사용 예시 포함
- ✅ **확장 가능 설계**: JSON 데이터 활용을 위한 가이드 포함

### ✅ Phase 5: Service 레이어 (완료)
- [x] **LectureService.java**
  - 강의 CRUD 및 검색 기능
  - 수강 인원 관리
  - 강의 통계 조회

- [x] **EnrollmentService.java**
  - 수강신청 처리
  - 수강 목록 조회
  - 수강신청 취소

- [x] **AssignmentService.java**
  - 과제 CRUD
  - 과제 제출 관리
  - 과제 통계 조회

### ✅ Phase 6: Controller 레이어 (완료)
- [x] **LectureController.java** (6개 엔드포인트)
  - 통합 API 설계 완료
  - 쿼리 파라미터 기반 필터링

- [x] **EnrollmentController.java** (7개 엔드포인트) ⭐
  - 통합 API 설계 완료
  - **DTO 패턴 적용 완료** (HTTP 400 해결)
  - convertToDto() 메서드 구현
  - Lazy Loading 안전 처리

- [x] **AssignmentController.java** (8개 엔드포인트)
  - 통합 API 설계 완료
  - 과제 제출 관리 API

### ⭐ Phase 6.5: DTO 패턴 적용 (신규 완료)

#### 문제 상황
- **HTTP 400 에러**: "Could not write JSON: could not initialize proxy - no Session"
- **원인**: Hibernate Lazy Loading 프록시 객체를 직접 JSON 직렬화
- **발생 위치**: EnrollmentController의 모든 GET 엔드포인트

#### 해결 방법
- [x] **convertToDto() 메서드 구현**
  - EnrollmentExtendedTbl → EnrollmentDto 변환
  - Lazy Loading 안전 처리 (try-catch)
  - JSON 파싱으로 추가 필드 추출
  - 60+ 라인 상세 구현

- [x] **모든 엔드포인트 DTO 반환**
  - getEnrollments() 4가지 케이스 → Page<EnrollmentDto>
  - getEnrollmentById() → EnrollmentDto
  - Entity 내부 구조 노출 차단

- [x] **프론트엔드 테스트 스크립트 업데이트**
  - lecture-test-2-student-enrollment.js 수정
  - DTO 필드 구조에 맞게 출력 형식 변경

- [x] **문서화**
  - BACKEND_FIX_ENROLLMENT_400_ERROR.md 작성
  - 3가지 해결 방안 비교
  - DTO 패턴 권장 사유 설명

#### 기술적 효과
- ✅ **API 안정성 향상**: Hibernate 세션 문제 원천 차단
- ✅ **성능 최적화**: 필요한 데이터만 전송
- ✅ **유지보수성 향상**: 명확한 API 계약 (Contract)
- ✅ **확장성 보장**: Entity 변경 시 API 영향 최소화

### 📅 Phase 7: 테스트 & 통합 (진행 중)

---

## 🏗️ **시스템 아키텍처**

```
┌─────────────────────────────────────────────────────────────┐
│                    강의 관리 시스템                           │
│  (Blue Crab LMS Lecture Management System)                  │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐    │
│  │                기존 테이블 확장                       │    │
│  │  • LEC_TBL: 강의 정보 확장 (+컬럼)                  │    │
│  │  • USER_TBL: 평가 기능 추가 (JSON 필드)             │    │
│  │  • BOARD_TBL: 강의별 게시판 (코드 확장)             │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              신규 테이블 (2개)                       │    │
│  │  • ENROLLMENT_EXTENDED_TBL: 수강+출결+성적 통합     │    │
│  │  • ASSIGNMENT_EXTENDED_TBL: 과제+제출 통합          │    │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│  Spring Boot + JPA + MariaDB + JWT 인증                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 📋 문서 구조

### 1. [요구사항분석.md](요구사항분석.md)
- 현재 요구사항 요약
- 코멘트 반영된 개선사항
- 데이터베이스 설계 가이드라인
- API 설계 원칙

### 2. [01-데이터베이스설계.md](01-데이터베이스설계.md)
- ERD (Entity Relationship Diagram)
- 테이블 스키마 및 제약조건
- 인덱스 설계
- 데이터 마이그레이션 전략

### 3. [02-API명세서.md](02-API명세서.md)
- REST API 엔드포인트 명세
- 요청/응답 포맷
- 에러 코드 정의
- 인증/인가 방식

### 4. [03-비즈니스로직.md](03-비즈니스로직.md)
- 수강신청 비즈니스 로직
- 출결 관리 로직
- 성적 계산 로직
- 시퀀스 다이어그램

### 5. [04-관리자플로우.md](04-관리자플로우.md)
- 강의 등록/관리 워크플로우
- 평가 항목 관리
- 통계 및 모니터링

### 6. [05-교수플로우.md](05-교수플로우.md)
- 강의 운영 워크플로우
- 출결 승인 프로세스
- 성적 관리
- 과제 및 공지사항 관리

### 7. [06-학생플로우.md](06-학생플로우.md)
- 수강신청 프로세스
- 출결 확인 및 요청
- 과제 제출
- 강의 평가

### 8. [07-구현순서.md](07-구현순서.md)
- 단계별 개발 로드맵
- 우선순위 및 의존성
- 테스트 전략

### 9. [08-프론트엔드연동.md](08-프론트엔드연동.md)
- UI/UX 설계 가이드
- React 컴포넌트 구조
- API 연동 패턴
- 반응형 디자인

---

## 🏗️ 시스템 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   Database      │
│   (React)       │◄──►│   (Spring Boot) │◄──►│   (MariaDB)     │
│                 │    │                 │    │                 │
│ - 관리자 UI     │    │ - REST API      │    │ - 강의 정보     │
│ - 교수 UI       │    │ - 비즈니스 로직 │    │ - 수강 정보     │
│ - 학생 UI       │    │ - 인증/인가     │    │ - 출결 정보     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

---

## 📊 주요 기능

### 관리자 기능
- ✅ 강의 등록 및 관리
- ✅ 강의 평가 항목 설정
- ✅ 학생/강의별 통계 조회
- ✅ 시스템 모니터링

### 교수 기능
- ✅ 강의 운영 관리
- ✅ 출결 승인/거부
- ✅ 성적 입력 및 관리
- ✅ 과제 및 공지사항 관리
- ✅ 실시간 채팅

### 학생 기능
- ✅ 수강신청 (조건 필터링)
- ✅ 출결 확인 및 요청
- ✅ 과제 제출
- ✅ 강의 평가
- ✅ 공지사항 및 알림 확인

---

## 🔧 기술 스택

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **Database**: MariaDB
- **ORM**: JPA/Hibernate
- **Authentication**: JWT
- **Documentation**: Swagger/OpenAPI

### Frontend
- **Framework**: React 18
- **Language**: JavaScript/TypeScript
- **Styling**: Material-UI
- **State Management**: Context API
- **Build Tool**: Vite

### DevOps
- **Version Control**: Git
- **CI/CD**: GitHub Actions
- **Container**: Docker
- **Deployment**: Azure/AWS

---

## 📈 개발 진행 상황

- [x] 요구사항 분석 완료
- [x] 데이터베이스 설계 완료
- [x] API 명세서 작성 완료
- [x] 비즈니스 로직 설계 완료
- [x] 관리자 플로우 설계 완료
- [x] 교수 플로우 설계 완료
- [x] 학생 플로우 설계 완료
- [x] 구현 순서 및 로드맵 작성 완료
- [x] 프론트엔드 연동 가이드 작성 완료
- [ ] 백엔드 구현 시작
- [ ] 프론트엔드 구현 시작
- [ ] 통합 테스트
- [ ] 배포 및 운영

---

## 🎯 다음 단계

1. **백엔드 개발 시작**
   - 데이터베이스 스키마 생성
   - JPA 엔티티 및 레포지토리 구현
   - 서비스 계층 비즈니스 로직 구현
   - REST 컨트롤러 구현

2. **프론트엔드 개발 시작**
   - React 프로젝트 설정
   - 공통 컴포넌트 개발
   - 각 역할별 페이지 구현
   - API 연동

3. **통합 및 테스트**
   - 단위 테스트 작성
   - 통합 테스트 수행
   - 사용자验收 테스트

---

## 📞 문의 및 지원

문서와 관련된 질문이나 개선사항이 있으시면 언제든지 연락주세요.

**작성자**: AI Assistant
**최종 수정일**: 2025-10-09