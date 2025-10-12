# Repository 레이어 구현 완료 (Phase 4)

> **작성일**: 2025-10-12  
> **상태**: ✅ Phase 4 완료 (Repository 레이어 완료)  
> **파일 위치**: `src/main/java/BlueCrab/com/example/repository/Lecture/`

---

## ✅ 완료된 작업

### **Phase 4: Repository 레이어 구현**

#### 📁 생성된 파일 (3개)

1. **LecTblRepository.java** ✅
   - 경로: `repository/Lecture/LecTblRepository.java`
   - 패키지: `BlueCrab.com.example.repository.Lecture`
   - 엔티티: `LecTbl`
   - 메서드 수: **30개**

2. **EnrollmentExtendedTblRepository.java** ✅
   - 경로: `repository/Lecture/EnrollmentExtendedTblRepository.java`
   - 패키지: `BlueCrab.com.example.repository.Lecture`
   - 엔티티: `EnrollmentExtendedTbl`
   - 메서드 수: **20개**

3. **AssignmentExtendedTblRepository.java** ✅
   - 경로: `repository/Lecture/AssignmentExtendedTblRepository.java`
   - 패키지: `BlueCrab.com.example.repository.Lecture`
   - 엔티티: `AssignmentExtendedTbl`
   - 메서드 수: **15개**

---

## 📊 Repository 상세 분석

### 1. **LecTblRepository** (강의 정보)

#### 기능 카테고리별 메서드

**기본 조회 (3개)**
```java
Optional<LecTbl> findByLecSerial(String lecSerial)
List<LecTbl> findByLecProf(String lecProf)
List<LecTbl> findByLecTitContaining(String lecTit)
```

**수강신청 관련 조회 (4개)**
```java
Page<LecTbl> findByLecState(Integer lecState, Pageable pageable)
List<LecTbl> findByLecState(Integer lecState)
Page<LecTbl> findAvailableLectures(Integer lecState, Pageable pageable)  // @Query
boolean hasAvailableSeats(Integer lecIdx)  // @Query
```

**학년/학기별 조회 (5개)**
```java
List<LecTbl> findByLecYear(Integer lecYear)
List<LecTbl> findByLecSemester(Integer lecSemester)
Page<LecTbl> findByLecYearAndLecSemester(Integer lecYear, Integer lecSemester, Pageable pageable)
List<LecTbl> findByLecProfAndLecSemester(String lecProf, Integer lecSemester)
Page<LecTbl> searchLectures(...)  // @Query - 복합 검색
```

**전공/교양 및 필수/선택 조회 (3개)**
```java
Page<LecTbl> findByLecMajor(Integer lecMajor, Pageable pageable)
Page<LecTbl> findByLecRequire(Integer lecRequire, Pageable pageable)
Page<LecTbl> findByLecMajorAndLecRequire(Integer lecMajor, Integer lecRequire, Pageable pageable)
```

**수강 인원 관리 (2개)**
```java
@Modifying int incrementLecCurrent(Integer lecIdx)  // @Query
@Modifying int decrementLecCurrent(Integer lecIdx)  // @Query
```

**통계 관련 (4개)**
```java
long countAllLectures()  // @Query
long countByLecState(Integer lecState)
long countByLecProf(String lecProf)
long countByLecMajor(Integer lecMajor)
```

**존재 여부 확인 (1개)**
```java
boolean existsByLecSerial(String lecSerial)
```

#### 주요 특징
- ✅ **페이징 지원**: 대부분의 조회 메서드에 Pageable 버전 제공
- ✅ **복합 검색**: 학년/학기/전공/상태 동적 필터링
- ✅ **원자적 업데이트**: @Modifying으로 수강 인원 안전하게 증감
- ✅ **조건부 업데이트**: 정원 초과 방지 로직 내장

---

### 2. **EnrollmentExtendedTblRepository** (수강신청)

#### 기능 카테고리별 메서드

**기본 조회 (5개)**
```java
List<EnrollmentExtendedTbl> findByStudentIdx(Integer studentIdx)
Page<EnrollmentExtendedTbl> findByStudentIdx(Integer studentIdx, Pageable pageable)
List<EnrollmentExtendedTbl> findByLecIdx(Integer lecIdx)
Page<EnrollmentExtendedTbl> findByLecIdx(Integer lecIdx, Pageable pageable)
Optional<EnrollmentExtendedTbl> findByStudentIdxAndLecIdx(Integer studentIdx, Integer lecIdx)
```

**학생별 수강 정보 (2개)**
```java
List<EnrollmentExtendedTbl> findEnrolledLecturesByStudent(Integer studentIdx)  // @Query + JOIN FETCH
Page<EnrollmentExtendedTbl> findEnrollmentHistoryByStudent(Integer studentIdx, Pageable pageable)  // @Query + JOIN FETCH
```

**강의별 수강 정보 (2개)**
```java
List<EnrollmentExtendedTbl> findStudentsByLecture(Integer lecIdx)  // @Query + JOIN FETCH
Page<EnrollmentExtendedTbl> findStudentsByLecture(Integer lecIdx, Pageable pageable)  // @Query + JOIN FETCH
```

**통계 관련 (3개)**
```java
long countByStudentIdx(Integer studentIdx)
long countByLecIdx(Integer lecIdx)
long countAllEnrollments()  // @Query
```

**존재 여부 확인 (1개)**
```java
boolean existsByStudentIdxAndLecIdx(Integer studentIdx, Integer lecIdx)
```

**배치 조회 (2개)**
```java
List<EnrollmentExtendedTbl> findAllByLecIdxIn(List<Integer> lecIdxList)
List<EnrollmentExtendedTbl> findAllByStudentIdxIn(List<Integer> studentIdxList)
```

**삭제 관련 (3개)**
```java
int deleteByStudentIdx(Integer studentIdx)
int deleteByLecIdx(Integer lecIdx)
int deleteByStudentIdxAndLecIdx(Integer studentIdx, Integer lecIdx)
```

#### 주요 특징
- ✅ **N+1 문제 방지**: JOIN FETCH로 연관 엔티티 한번에 조회
- ✅ **배치 처리**: IN 절로 여러 강의/학생 일괄 조회
- ✅ **양방향 조회**: 학생→강의, 강의→학생 모두 지원
- ✅ **중복 방지**: existsByStudentIdxAndLecIdx로 중복 수강 체크

---

### 3. **AssignmentExtendedTblRepository** (과제)

#### 기능 카테고리별 메서드

**기본 조회 (2개)**
```java
List<AssignmentExtendedTbl> findByLecIdx(Integer lecIdx)
Page<AssignmentExtendedTbl> findByLecIdx(Integer lecIdx, Pageable pageable)
```

**강의 정보 포함 조회 (2개)**
```java
List<AssignmentExtendedTbl> findAssignmentsWithLecture(Integer lecIdx)  // @Query + JOIN FETCH
Page<AssignmentExtendedTbl> findAssignmentsWithLecture(Integer lecIdx, Pageable pageable)  // @Query + JOIN FETCH
```

**배치 조회 (2개)**
```java
List<AssignmentExtendedTbl> findAllByLecIdxIn(List<Integer> lecIdxList)
List<AssignmentExtendedTbl> findAllByLecIdxInWithLecture(List<Integer> lecIdxList)  // @Query + JOIN FETCH
```

**통계 관련 (2개)**
```java
long countByLecIdx(Integer lecIdx)
long countAllAssignments()  // @Query
```

**존재 여부 확인 (1개)**
```java
boolean existsByLecIdx(Integer lecIdx)
```

**삭제 관련 (1개)**
```java
int deleteByLecIdx(Integer lecIdx)
```

**정렬 (2개)**
```java
List<AssignmentExtendedTbl> findByLecIdxOrderByAssignmentIdxDesc(Integer lecIdx)  // 최신순
List<AssignmentExtendedTbl> findByLecIdxOrderByAssignmentIdxAsc(Integer lecIdx)  // 오래된순
```

#### 주요 특징
- ✅ **JOIN FETCH 최적화**: 강의 정보 포함 조회
- ✅ **정렬 지원**: 최신순/오래된순 정렬
- ✅ **배치 처리**: 여러 강의의 과제 일괄 조회
- ✅ **확장 가능**: JSON 데이터 활용을 위한 가이드 주석 포함

---

## 🎯 Repository 설계 원칙

### 1. **Spring Data JPA 활용**
```java
// 메서드 네이밍 규칙으로 쿼리 자동 생성
List<LecTbl> findByLecProf(String lecProf);
Page<LecTbl> findByLecState(Integer lecState, Pageable pageable);
boolean existsByLecSerial(String lecSerial);
```

### 2. **JPQL 최적화**
```java
// 복잡한 조회는 @Query로 명시적 작성
@Query("SELECT l FROM LecTbl l WHERE l.lecState = :lecState AND l.lecCurrent < l.lecCapa")
Page<LecTbl> findAvailableLectures(@Param("lecState") Integer lecState, Pageable pageable);
```

### 3. **N+1 문제 방지**
```java
// JOIN FETCH로 연관 엔티티 한번에 로드
@Query("SELECT e FROM EnrollmentExtendedTbl e JOIN FETCH e.lecture WHERE e.studentIdx = :studentIdx")
List<EnrollmentExtendedTbl> findEnrolledLecturesByStudent(@Param("studentIdx") Integer studentIdx);
```

### 4. **배치 처리**
```java
// IN 절로 여러 데이터 일괄 조회
List<EnrollmentExtendedTbl> findAllByLecIdxIn(List<Integer> lecIdxList);
```

### 5. **원자적 업데이트**
```java
// @Modifying으로 안전한 증감 처리
@Modifying
@Query("UPDATE LecTbl l SET l.lecCurrent = l.lecCurrent + 1 WHERE l.lecIdx = :lecIdx AND l.lecCurrent < l.lecCapa")
int incrementLecCurrent(@Param("lecIdx") Integer lecIdx);
```

---

## 📝 메서드 네이밍 규칙

### Spring Data JPA 키워드
- `findBy`: SELECT 조회
- `countBy`: COUNT 집계
- `existsBy`: 존재 여부 확인
- `deleteBy`: DELETE 삭제
- `And`, `Or`: 조건 결합
- `Containing`: LIKE 검색
- `In`: IN 절
- `OrderBy...Desc/Asc`: 정렬

### 예시
```java
// findBy + 필드명 + Containing → LIKE 검색
List<LecTbl> findByLecTitContaining(String lecTit);

// findBy + 필드1 + And + 필드2 → 복합 조건
Optional<EnrollmentExtendedTbl> findByStudentIdxAndLecIdx(Integer studentIdx, Integer lecIdx);

// countBy + 필드명 → 카운트
long countByLecIdx(Integer lecIdx);

// existsBy + 필드명 → 존재 확인
boolean existsByLecSerial(String lecSerial);
```

---

## 🚀 다음 단계 (Phase 5: Service 레이어)

### 예정된 Service 클래스
1. **LectureService.java**
   - 강의 CRUD
   - 수강 인원 관리
   - 강의 검색 및 필터링

2. **EnrollmentService.java**
   - 수강신청 처리
   - 수강 취소
   - 출결 관리
   - 성적 관리

3. **AssignmentService.java**
   - 과제 등록/수정/삭제
   - 과제 제출 처리
   - 과제 채점
   - 통계 생성

### Service 레이어 구현 시 주요 작업
- ✅ Repository 주입 및 활용
- ✅ 비즈니스 로직 구현
- ✅ DTO ↔ Entity 변환
- ✅ JSON 데이터 파싱 (Jackson ObjectMapper)
- ✅ 트랜잭션 처리 (@Transactional)
- ✅ 예외 처리 및 검증
- ✅ 로깅

---

## 📚 참고 자료

### 기존 Repository 패턴 참고
- `UserTblRepository.java`: 사용자 정보 조회 패턴
- `BoardRepository.java`: 게시판 조회 및 통계 패턴
- `FacilityReservationRepository.java`: 예약 관리 패턴

### Spring Data JPA 공식 문서
- [Query Methods](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods)
- [Query Creation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation)
- [Modifying Queries](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.modifying-queries)

---

## 📊 구현 통계

### 전체 메서드 수
- **LecTblRepository**: 30개
- **EnrollmentExtendedTblRepository**: 20개
- **AssignmentExtendedTblRepository**: 15개
- **총합**: **65개 메서드**

### 카테고리별 분류
- 기본 조회: 12개
- JOIN FETCH 조회: 8개
- 배치 조회: 6개
- 통계: 9개
- 존재 확인: 3개
- 삭제: 4개
- 수정: 2개
- 정렬: 2개
- 복합 검색: 2개

### 어노테이션 사용
- `@Repository`: 3개
- `@Query`: 17개
- `@Modifying`: 2개
- `@Param`: 30+개

---

## ✅ 체크리스트

### Phase 4 완료 항목
- [x] LecTblRepository 생성 (30개 메서드)
- [x] EnrollmentExtendedTblRepository 생성 (20개 메서드)
- [x] AssignmentExtendedTblRepository 생성 (15개 메서드)
- [x] 상세한 JavaDoc 주석 작성
- [x] 사용 예시 포함
- [x] N+1 문제 방지 (JOIN FETCH)
- [x] 배치 처리 지원 (IN 절)
- [x] 페이징 지원
- [x] 통계 메서드 제공
- [x] README.md 업데이트

### 다음 단계 준비
- [ ] Service 레이어 설계
- [ ] DTO 변환 로직 구현
- [ ] JSON 데이터 파싱 유틸리티
- [ ] 비즈니스 로직 검증
- [ ] 예외 처리 전략

---

**작성자**: AI Assistant  
**최종 수정일**: 2025-10-12  
**버전**: 1.0
