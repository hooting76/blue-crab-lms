# 강의 관리 시스템 구현 진척도

> **최종 업데이트**: 2025-10-11  
> **현재 Phase**: Phase 3 완료, Phase 4 진입 예정  
> **전체 진행률**: 35% (Phase 1-3 완료)

---

## 📊 전체 개발 로드맵

```
Phase 1-2: 데이터베이스 구축        ████████████ 100% ✅
Phase 3: Entity & DTO 레이어       ████████████ 100% ✅
Phase 4: Repository 레이어          ░░░░░░░░░░░░   0% 🚧
Phase 5: Service 레이어             ░░░░░░░░░░░░   0% 📅
Phase 6: Controller 레이어          ░░░░░░░░░░░░   0% 📅
Phase 7: 테스트 & 통합              ░░░░░░░░░░░░   0% 📅
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
전체 진행률:                        █████░░░░░░░  35%
```

---

## ✅ Phase 1-2: 데이터베이스 구축 (완료)

### 기간: 2025-10-09 ~ 2025-10-10
### 상태: ✅ 완료

#### 완료 항목
- [x] **USER_TBL 확장**
  - LECTURE_EVALUATIONS (LONGTEXT) 추가
  - 강의 평가 JSON 데이터 저장

- [x] **LEC_TBL 확장**
  - LEC_CURRENT (INT) 추가 - 현재 수강 인원
  - LEC_YEAR (INT) 추가 - 대상 학년
  - LEC_SEMESTER (INT) 추가 - 학기

- [x] **ENROLLMENT_EXTENDED_TBL 생성**
  - 수강신청 + 출결 + 성적 통합 관리
  - JSON 데이터 필드 (ENROLLMENT_DATA)
  - 외래키: LEC_IDX, STUDENT_IDX

- [x] **ASSIGNMENT_EXTENDED_TBL 생성**
  - 과제 + 제출 통합 관리
  - JSON 데이터 필드 (ASSIGNMENT_DATA)
  - 외래키: LEC_IDX

---

## ✅ Phase 3: Entity & DTO 레이어 (완료)

### 기간: 2025-10-11
### 상태: ✅ 완료

### Entity 클래스 (3개)

#### 1. LecTbl.java ✅
- **위치**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/entity/Lecture/LecTbl.java`
- **필드**: 18개 (LEC_IDX, LEC_SERIAL, LEC_TIT, LEC_PROF 등)
- **관계**: None (참조 대상 엔티티)
- **비즈니스 메서드**:
  - `isOpenForEnrollment()` - 수강신청 가능 여부
  - `isFull()` - 정원 초과 여부
  - `getAvailableSeats()` - 남은 수강 가능 인원
- **JavaDoc**: 완전 문서화

#### 2. EnrollmentExtendedTbl.java ✅
- **위치**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/entity/Lecture/EnrollmentExtendedTbl.java`
- **필드**: 4개 (ENROLLMENT_IDX, LEC_IDX, STUDENT_IDX, ENROLLMENT_DATA)
- **관계**: 
  - `@ManyToOne` → LecTbl
  - `@ManyToOne` → UserTbl
- **JSON 구조**: 수강상태, 출결배열, 성적객체
- **JavaDoc**: 완전 문서화

#### 3. AssignmentExtendedTbl.java ✅
- **위치**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/entity/Lecture/AssignmentExtendedTbl.java`
- **필드**: 3개 (ASSIGNMENT_IDX, LEC_IDX, ASSIGNMENT_DATA)
- **관계**: `@ManyToOne` → LecTbl
- **JSON 구조**: 과제정보, 제출배열, 통계객체
- **JavaDoc**: 완전 문서화

---

### DTO 클래스 (11개)

#### 강의 관련 DTO (4개) ✅
1. **LectureDto.java**
   - 용도: 강의 목록 조회, 검색 결과
   - 특징: availableSeats, isFull 자동 계산

2. **LectureDetailDto.java**
   - 용도: 강의 상세 조회
   - 특징: LectureDto 상속 + lecSummary 추가

3. **LectureCreateRequest.java**
   - 용도: 관리자 강의 생성 요청

4. **LectureUpdateRequest.java**
   - 용도: 관리자 강의 수정 요청

#### 수강신청 관련 DTO (2개) ✅
5. **EnrollmentDto.java**
   - 용도: 수강신청 정보 조회
   - 포함: 강의정보, 학생정보, 출결/성적 객체

6. **EnrollmentCreateRequest.java**
   - 용도: 학생 수강신청 요청

#### 출결/성적 DTO (2개) ✅
7. **AttendanceDto.java**
   - 용도: 출결 정보 전송
   - 필드: 날짜, 상태, 신청사유, 승인정보

8. **GradeDto.java**
   - 용도: 성적 정보 전송
   - 필드: 중간/기말/과제/참여도/총점/학점

#### 과제 관련 DTO (3개) ✅
9. **AssignmentDto.java**
   - 용도: 과제 정보 조회
   - 포함: 과제기본정보, 제출목록, 통계

10. **AssignmentSubmissionDto.java**
    - 용도: 과제 제출 정보
    - 필드: 학생정보, 제출내용, 점수, 피드백

11. **AssignmentStatisticsDto.java**
    - 용도: 과제 통계 정보
    - 필드: 제출률, 평균점수, 채점현황

---

### 폴더 구조화 ✅

```
backend/BlueCrab/src/main/java/BlueCrab/com/example/
│
├── entity/
│   └── Lecture/                    ← 강의 관련 엔티티
│       ├── LecTbl.java
│       ├── EnrollmentExtendedTbl.java
│       └── AssignmentExtendedTbl.java
│
└── dto/
    └── Lecture/                    ← 강의 관련 DTO
        ├── LectureDto.java
        ├── LectureDetailDto.java
        ├── LectureCreateRequest.java
        ├── LectureUpdateRequest.java
        ├── EnrollmentDto.java
        ├── EnrollmentCreateRequest.java
        ├── AttendanceDto.java
        ├── GradeDto.java
        ├── AssignmentDto.java
        ├── AssignmentSubmissionDto.java
        └── AssignmentStatisticsDto.java
```

---

## 🚧 Phase 4: Repository 레이어 (다음 단계)

### 예상 기간: 2일
### 상태: 준비 중

### 구현 예정 Repository (3개)

#### 1. LecTblRepository.java
- **상속**: `JpaRepository<LecTbl, Integer>`
- **위치**: `repository/Lecture/LecTblRepository.java`
- **쿼리 메서드**:
  - `findByLecOpen(Integer lecOpen)` - 수강신청 가능 강의
  - `findByLecMcodeAndLecMcodeDep(String mcode, String mcodeDep)` - 학부/학과별 강의
  - `findByLecYearAndLecSemester(Integer year, Integer semester)` - 학년/학기별 강의
  - `findByLecProfContaining(String profName)` - 교수명 검색
  - `findByLecTitContaining(String title)` - 강의명 검색

#### 2. EnrollmentExtendedTblRepository.java
- **상속**: `JpaRepository<EnrollmentExtendedTbl, Integer>`
- **위치**: `repository/Lecture/EnrollmentExtendedTblRepository.java`
- **쿼리 메서드**:
  - `findByLecIdx(Integer lecIdx)` - 강의별 수강생 목록
  - `findByStudentIdx(Integer studentIdx)` - 학생별 수강 내역
  - `findByLecIdxAndStudentIdx(Integer lecIdx, Integer studentIdx)` - 특정 수강신청
  - `countByLecIdx(Integer lecIdx)` - 수강 인원 카운트

#### 3. AssignmentExtendedTblRepository.java
- **상속**: `JpaRepository<AssignmentExtendedTbl, Integer>`
- **위치**: `repository/Lecture/AssignmentExtendedTblRepository.java`
- **쿼리 메서드**:
  - `findByLecIdx(Integer lecIdx)` - 강의별 과제 목록
  - `findByLecIdxOrderByAssignmentIdxDesc(Integer lecIdx)` - 최신순 과제

---

## 📅 Phase 5: Service 레이어 (예정)

### 예상 기간: 5일
### 상태: 대기 중

### 구현 예정 Service (3개)

#### 1. LectureService.java
- 강의 CRUD 비즈니스 로직
- 수강 인원 관리
- 강의 상태 변경

#### 2. EnrollmentService.java
- 수강신청 처리
- 출결 관리
- 성적 계산 및 관리

#### 3. AssignmentService.java
- 과제 생성 및 관리
- 과제 제출 처리
- 과제 채점 및 통계

---

## 📅 Phase 6: Controller 레이어 (예정)

### 예상 기간: 5일
### 상태: 대기 중

### 구현 예정 Controller (3개)

#### 1. LectureController.java
- REST API 엔드포인트
- 권한 검증
- 요청/응답 처리

#### 2. EnrollmentController.java
- 수강신청 API
- 출결 API
- 성적 API

#### 3. AssignmentController.java
- 과제 API
- 제출 API
- 채점 API

---

## 📈 다음 작업 계획

### 즉시 시작 가능 (Phase 4)
1. **Repository 레이어 생성**
   - LecTblRepository 인터페이스
   - EnrollmentExtendedTblRepository 인터페이스
   - AssignmentExtendedTblRepository 인터페이스
   - 기본 CRUD 메서드 정의
   - 커스텀 쿼리 메서드 추가

### 이후 작업 (Phase 5-6)
2. **Service 레이어 구현**
   - 비즈니스 로직 구현
   - JSON 데이터 처리
   - 트랜잭션 관리

3. **Controller 레이어 구현**
   - REST API 구현
   - 요청 검증
   - 예외 처리

4. **테스트 코드 작성**
   - 단위 테스트
   - 통합 테스트
   - API 테스트

---

## 📝 참고 문서

- [DB_IMPLEMENTATION_STATUS.md](DB_IMPLEMENTATION_STATUS.md) - 데이터베이스 구현 현황
- [07-구현순서.md](07-구현순서.md) - 상세 구현 순서
- [README.md](README.md) - 전체 문서 개요

---

**작성자**: BlueCrab Development Team  
**문서 버전**: 1.0  
**마지막 업데이트**: 2025-10-11
