# Phase 2-1: Repository 계층 구현

## 📋 작업 개요

**목표**: `EnrollmentExtendedRepository`에 출석 관련 메서드 추가  
**소요 시간**: 2시간  
**상태**: ✅ 완료

---

## 🎯 작업 내용

### 1. Repository 메서드 추가

**파일 위치**: `src/main/java/BlueCrab/com/example/repository/Lecture/EnrollmentExtendedTblRepository.java`

#### 추가할 메서드 목록

##### ① `findByLecSerialWithDetails()`
**용도**: 강의 코드로 수강생 목록 조회 (강의 정보 + 학생 정보 포함)

```java
/**
 * 강의 코드(LEC_SERIAL)로 수강생 목록 조회
 * 강의 정보와 학생 정보를 함께 조회 (JOIN FETCH)
 * 
 * @param lecSerial 강의 코드 (예: "CS101")
 * @return 수강생 목록
 */
@Query("SELECT e FROM EnrollmentExtendedTbl e " +
       "JOIN FETCH e.lecture l " +
       "JOIN FETCH e.student s " +
       "WHERE l.lecSerial = :lecSerial")
List<EnrollmentExtendedTbl> findByLecSerialWithDetails(@Param("lecSerial") String lecSerial);
```

**사용 시나리오**:
- 교수가 강의의 전체 학생 출석 현황을 조회할 때
- 스케줄러가 특정 강의의 대기 요청을 처리할 때

---

##### ② `findByLecSerialAndStudentIdx()`
**용도**: 강의 코드 + 학생 IDX로 특정 수강 정보 조회

```java
/**
 * 강의 코드와 학생 IDX로 수강 정보 조회
 * 학생의 특정 강의 수강 여부 확인 및 출석 데이터 조회에 사용
 * 
 * @param lecSerial 강의 코드
 * @param studentIdx 학생 USER_IDX
 * @return 수강 정보 (Optional)
 */
@Query("SELECT e FROM EnrollmentExtendedTbl e " +
       "JOIN FETCH e.lecture l " +
       "WHERE l.lecSerial = :lecSerial AND e.studentIdx = :studentIdx")
Optional<EnrollmentExtendedTbl> findByLecSerialAndStudentIdx(
    @Param("lecSerial") String lecSerial,
    @Param("studentIdx") Integer studentIdx
);
```

**사용 시나리오**:
- 학생이 출석 요청을 할 때 (수강 여부 확인)
- 학생이 자신의 출석 현황을 조회할 때
- 교수가 특정 학생의 출석을 승인할 때

---

##### ③ `findByLecSerialAndProfessorIdx()` (권한 검증용)
**용도**: 교수가 해당 강의의 담당 교수인지 검증

```java
/**
 * 강의 코드와 교수 USER_IDX로 강의 정보 조회
 * 교수 권한 검증에 사용 (담당 강의인지 확인)
 * 
 * @param lecSerial 강의 코드
 * @param professorIdx 교수 USER_IDX
 * @return 수강 정보 목록 (담당 강의가 맞으면 1개 이상 반환)
 */
@Query("SELECT e FROM EnrollmentExtendedTbl e " +
       "JOIN FETCH e.lecture l " +
       "JOIN FETCH e.student s " +
       "WHERE l.lecSerial = :lecSerial AND " +
       "EXISTS (SELECT 1 FROM UserTbl u WHERE u.userIdx = :professorIdx AND u.userCode = l.lecProf)")
List<EnrollmentExtendedTbl> findByLecSerialAndProfessorIdx(
    @Param("lecSerial") String lecSerial,
    @Param("professorIdx") Integer professorIdx
);
```

**사용 시나리오**:
- 교수가 출석 승인 API를 호출할 때 권한 검증
- 교수가 강의의 출석 현황을 조회할 때 권한 검증

**권한 검증 로직**:
```java
// Service에서 사용 예시
List<EnrollmentExtendedTbl> enrollments = 
    repository.findByLecSerialAndProfessorIdx(lecSerial, professorIdx);

if (enrollments.isEmpty()) {
    throw new AccessDeniedException("해당 강의의 담당 교수가 아닙니다.");
}
```

---

### 2. 기존 메서드 활용

기존 Repository에 이미 존재하는 메서드도 활용:

```java
// 학생 IDX로 수강 강의 목록 조회
List<EnrollmentExtendedTbl> findByStudentIdx(Integer studentIdx);

// 강의 IDX로 수강생 목록 조회
List<EnrollmentExtendedTbl> findByLecIdx(Integer lecIdx);

// 학생 + 강의 IDX로 조회
Optional<EnrollmentExtendedTbl> findByStudentIdxAndLecIdx(Integer studentIdx, Integer lecIdx);
```

---

## 🔍 쿼리 최적화

### JOIN FETCH 전략
- **목적**: N+1 문제 방지
- **적용**: 모든 조회 메서드에 `JOIN FETCH` 사용
- **효과**: 한 번의 쿼리로 연관 엔티티 함께 조회

### 예시
```sql
-- JOIN FETCH 없이
SELECT * FROM ENROLLMENT_EXTENDED_TBL WHERE LEC_IDX = 1;  -- 1회 쿼리
SELECT * FROM LEC_TBL WHERE LEC_IDX = 1;                  -- N회 쿼리 (학생 수만큼)
SELECT * FROM USER_TBL WHERE USER_IDX = ...;              -- N회 쿼리 (학생 수만큼)

-- JOIN FETCH 사용
SELECT e.*, l.*, s.* 
FROM ENROLLMENT_EXTENDED_TBL e
JOIN LEC_TBL l ON e.LEC_IDX = l.LEC_IDX
JOIN USER_TBL s ON e.STUDENT_IDX = s.USER_IDX
WHERE l.LEC_SERIAL = 'CS101';                             -- 1회 쿼리로 모두 조회
```

---

## 📋 체크리스트

### 메서드 추가
- [x] `findByLecSerialWithDetails()` 메서드 추가
- [x] `findByLecSerialAndStudentIdx()` 메서드 추가
- [x] `findByLecSerialAndProfessorIdx()` 메서드 추가
- [x] `@Query` JPQL 작성
- [x] `@Param` 어노테이션 적용
- [x] JavaDoc 주석 작성

### 쿼리 검증
- [x] JOIN FETCH 적용 확인
- [x] 파라미터 바인딩 확인
- [x] 반환 타입 확인 (List vs Optional)

### 테스트 (Optional)
- [ ] Repository 단위 테스트 작성
- [ ] 쿼리 실행 결과 확인
- [ ] N+1 문제 발생 여부 확인

---

## 🎯 다음 단계

**Phase 2-2: Service 계층 구현**
- `AttendanceService` 인터페이스 생성
- `AttendanceServiceImpl` 구현
- JSON 파싱/업데이트 로직
- 출석률 계산 로직

---

## 📝 예상 산출물

- `EnrollmentExtendedTblRepository.java` (메서드 3개 추가)
- Repository 단위 테스트 (Optional)

---

## ⚠️ 주의사항

### 1. LEC_SERIAL vs LEC_IDX
- **프론트엔드**: `lecSerial` (LEC_SERIAL) 사용
- **백엔드**: `lecIdx` (LEC_IDX)는 내부 로직용
- **쿼리**: `JOIN`을 통해 `lecSerial`로 조회

### 2. 권한 검증
- 교수 권한: `LecTbl.lecProf` == `UserTbl.userCode` 확인
- 학생 권한: `EnrollmentExtendedTbl.studentIdx` == JWT의 USER_IDX

### 3. Optional 사용
- 단일 결과: `Optional<EnrollmentExtendedTbl>` 반환
- 다중 결과: `List<EnrollmentExtendedTbl>` 반환
- Empty 처리: Service 계층에서 적절한 예외 발생

---

## 📚 참고 문서

- [출석요청승인_플로우.md](../../출석요청승인_플로우.md)
- 기존 Repository: `EnrollmentExtendedTblRepository.java`
- Entity: `EnrollmentExtendedTbl.java`, `LecTbl.java`, `UserTbl.java`
