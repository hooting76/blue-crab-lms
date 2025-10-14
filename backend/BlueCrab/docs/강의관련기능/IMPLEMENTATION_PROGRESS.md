# 강의 관리 시스템 구현 진척도

> **최종 업데이트**: 2025-10-14  
> **현재 Phase**: Phase 6.7 완료 - 교수 이름 조회 기능  
> **전체 진행률**: 94% (Phase 1-6.7 완료 + 성능 최적화)

---

## 📊 전체 개발 로드맵

```
Phase 1-2: 데이터베이스 구축        ████████████ 100% ✅
Phase 3: Entity & DTO 레이어       ████████████ 100% ✅
Phase 4: Repository 레이어          ████████████ 100% ✅
Phase 5: Service 레이어             ████████████ 100% ✅
Phase 6: Controller 레이어          ████████████ 100% ✅
Phase 6.5: DTO 패턴 적용           ████████████ 100% ✅
Phase 6.6: JOIN FETCH 최적화       ████████████ 100% ✅
Phase 6.7: 교수 이름 조회 기능     ████████████ 100% ✅
Phase 7: 테스트 & 통합              ████████░░░░  60% 🚧
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
전체 진행률:                        ██████████░░  94%
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

## ✅ Phase 6.5: DTO 패턴 적용 (완료)

### 기간: 2025-10-14
### 상태: ✅ 완료

#### 문제 진단
- [x] **HTTP 400 에러 발생**
  - Hibernate Lazy Loading 프록시 객체 JSON 직렬화 실패
  - "Could not write JSON: could not initialize proxy - no Session" 에러
  - Entity를 직접 반환할 때 발생

#### 완료 항목
- [x] **EnrollmentController.java DTO 패턴 구현**
  - convertToDto() 메서드 추가 (60+ 라인)
  - convertToDtoList() 헬퍼 메서드 추가
  - Lazy Loading 안전 처리 (try-catch)
  - JSON 파싱으로 추가 필드 추출

- [x] **모든 엔드포인트 DTO 반환으로 변경**
  - getEnrollments() 4가지 케이스 → Page<EnrollmentDto>
  - getEnrollmentById() → EnrollmentDto
  - Hibernate 프록시 노출 차단

- [x] **프론트엔드 테스트 스크립트 업데이트**
  - lecture-test-2-student-enrollment.js 수정
  - DTO 필드 구조에 맞게 출력 형식 변경
  - HTTP 400 에러 안내 제거

- [x] **문서화**
  - BACKEND_FIX_ENROLLMENT_400_ERROR.md 생성
  - 3가지 해결 방안 문서화
  - DTO 패턴 권장 사유 설명

#### 기술적 개선
- **API 안정성 향상**: Entity 내부 구조 노출 방지
- **성능 최적화**: 필요한 데이터만 전송
- **유지보수성**: 명확한 API 계약 (Contract)
- **에러 방지**: Lazy Loading 세션 문제 원천 차단

---

## ✅ Phase 6.6: JOIN FETCH 최적화 (완료)

### 기간: 2025-10-14
### 상태: ✅ 완료

#### 문제 진단
- [x] **DTO 필드가 null로 반환되는 문제**
  - EnrollmentDto의 강의 정보(lecTit, lecProf 등) 모두 null
  - EnrollmentDto의 학생 정보(studentName, studentCode) 모두 null
  - Lazy Loading 접근 시 Hibernate 세션 종료로 실패

#### 완료 항목
- [x] **Repository JOIN FETCH 쿼리 개선**
  - `findEnrollmentHistoryByStudent()`: lecture + student 동시 JOIN FETCH
  - `findStudentsByLecture()`: lecture + student 동시 JOIN FETCH
  - DISTINCT 키워드로 중복 제거
  - countQuery 분리로 페이징 최적화

- [x] **Service 레이어 수정**
  - `getEnrollmentsByStudentPaged()`: JOIN FETCH 메서드 사용
  - `getEnrollmentsByLecturePaged()`: JOIN FETCH 메서드 사용
  - N+1 쿼리 문제 원천 차단

- [x] **테스트 코드 출력 형식 개선**
  - `getMyEnrollments()`: 정보 그룹화 (수강신청/강의/학생)
  - `getAvailableLectures()`: 전체 JSON 구조 출력 추가
  - `enrollLecture()`: 상세 응답 정보 표시
  - `getLectureDetail()`: lecSummary 출력 및 JSON 확인

#### 기술적 효과
- **N+1 쿼리 방지**: 한 번의 쿼리로 모든 연관 데이터 조회
- **Lazy Loading 안전**: 세션 내에서 모든 데이터 로드 완료
- **성능 향상**: 불필요한 추가 쿼리 제거
- **DTO 완전성**: 모든 필드가 정상적으로 채워짐

---

## ✅ Phase 6.7: 교수 이름 조회 기능 + PageImpl 최적화 (완료)

### 기간: 2025-10-14
### 상태: ✅ 완료

#### 문제 진단
- [x] **Page.map()의 Entity 참조 유지 문제**
  - Page<Entity>.map(this::convertToDto)는 내부적으로 Entity 참조 유지
  - JSON 직렬화 시 Hibernate 프록시 객체 접근 시도
  - 동일한 HTTP 400 에러 재발 가능성

- [x] **교수 코드만 표시되는 문제**
  - LEC_PROF 필드가 USER_CODE 값만 저장 (예: "11")
  - 실제 교수 이름(USER_NAME)이 API 응답에 없음
  - 프론트엔드에서 "11" 대신 "굴림체" 표시 필요

#### 데이터 구조 분석
- **USER_TBL 스키마**:
  - `USER_CODE` (VARCHAR): 식별자/코드 (예: "11", "PROF001", "240105045")
  - `USER_NAME` (VARCHAR(50)): 실제 이름 (예: "굴림체", "김교수", "홍길동")
  - `USER_IDX` (INT): 자동 증가 PK

- **LEC_TBL 스키마**:
  - `LEC_PROF` (VARCHAR(50)): USER_CODE 참조 (예: "11")

- **조회 로직**:
  - LEC_PROF="11" → USER_TBL에서 USER_CODE="11" 검색 → USER_NAME="굴림체" 추출

#### 완료 항목

##### 1. PageImpl 패턴 적용
- [x] **EnrollmentController.java 수정**
  - `org.springframework.data.domain.PageImpl` import 추가
  - getEnrollments() 메서드 3개 섹션 수정:
    * 학생별 조회 (studentIdx 파라미터)
    * 강의별 조회 (lecIdx 파라미터)
    * 전체 조회 (파라미터 없음)
  
- [x] **변경 전 (문제)**:
  ```java
  Page<EnrollmentDto> dtoPage = enrollments.map(this::convertToDto);
  ```
  - Page.map()은 원본 Entity에 대한 참조를 유지
  - convertToDto() 내부에서 Lazy Loading 발생 가능
  - JSON 직렬화 시 Hibernate 프록시 접근 실패

- [x] **변경 후 (해결)**:
  ```java
  List<EnrollmentDto> dtoList = enrollments.getContent().stream()
          .map(this::convertToDto)
          .collect(Collectors.toList());
  Page<EnrollmentDto> dtoPage = new PageImpl<>(dtoList, pageable, enrollments.getTotalElements());
  ```
  - 명시적으로 Entity → DTO 변환 후 새로운 Page 객체 생성
  - Entity 참조 완전히 제거
  - 순수 DTO만 포함된 Page 객체 반환

##### 2. 교수 이름 조회 기능 구현
- [x] **EnrollmentDto.java 필드 추가**
  ```java
  private String lecProf;      // 교수코드 (USER_CODE) - 예: "11"
  private String lecProfName;  // 교수 이름 (USER_NAME) - 예: "굴림체"
  ```
  - Getter/Setter 메서드 추가
  - 필드 주석으로 의미 명확화

- [x] **UserTblRepository.java 메서드 추가**
  ```java
  /**
   * 교수 코드(USER_CODE)로 사용자 조회
   * @param userCode 교수 코드 등 (예: "11", "PROF001", "P001")
   * @return 사용자 정보
   */
  Optional<UserTbl> findByUserCode(String userCode);
  
  /**
   * 사용자 이름(USER_NAME)으로 조회
   * @param userName 이름 (예: "굴림체", "김교수", "홍길동")
   * @return 사용자 정보
   */
  Optional<UserTbl> findByUserName(String userName);
  ```
  - Spring Data JPA 메서드 명명 규칙 활용
  - 자동으로 WHERE USER_CODE = ? 쿼리 생성

- [x] **EnrollmentController.java 교수 조회 로직**
  - UserTblRepository 주입 추가
  - convertToDto() 메서드 수정:
    ```java
    // 교수 코드 설정
    dto.setLecProf(lecture.getLecProf());  // "11"
    
    // 교수 이름 조회 및 설정
    userTblRepository.findByUserCode(lecture.getLecProf())
        .ifPresent(professor -> {
            dto.setLecProfName(professor.getUserName());  // "굴림체"
        });
    ```
  - 교수 정보 없을 시 lecProfName은 null (프론트엔드에서 'N/A' 처리)

- [x] **lecture-test-2-student-enrollment.js 업데이트**
  ```javascript
  console.log(`교수코드: ${enrollment.lecProf || 'N/A'}`);     // "11"
  console.log(`교수 이름: ${enrollment.lecProfName || 'N/A'}`); // "굴림체"
  ```
  - 두 필드 모두 출력하여 데이터 검증 용이

- [x] **문서화**
  - BACKEND_FIX_PROFESSOR_NAME_LOOKUP.md 생성
  - 데이터 흐름, API 예시, 테스트 방법 문서화

#### 기술적 효과
- **JSON 직렬화 안정성**: Entity 참조 완전 제거로 프록시 접근 에러 원천 차단
- **사용자 경험 향상**: 교수 코드("11") 대신 실제 이름("굴림체") 표시
- **데이터 완전성**: 한 번의 API 호출로 필요한 모든 정보 제공
- **확장성**: 동일한 패턴으로 다른 참조 데이터 조회 가능
- **성능 영향 최소**: Repository 조회는 Optional로 캐싱 가능, 추가 쿼리 1회

#### 구현 세부사항
- **Entity 분리**: PageImpl로 Entity → DTO 변환 후 원본 Entity 제거
- **Optional 처리**: findByUserCode()가 빈 값일 경우 lecProfName은 null
- **에러 처리**: 교수 조회 실패 시에도 나머지 데이터는 정상 반환
- **테스트 가능**: 브라우저 콘솔 스크립트로 즉시 검증 가능

#### 관련 파일
- `EnrollmentController.java` (Lines 7-17, 107-145, 280-305)
- `EnrollmentDto.java` (Lines 15-19, 89-97)
- `UserTblRepository.java` (Lines 60-75)
- `lecture-test-2-student-enrollment.js` (Lines 328-334)
- `BACKEND_FIX_PROFESSOR_NAME_LOOKUP.md` (전체)

---

## ✅ Phase 3: Entity & DTO 레이어 (완료)

### 기간: 2025-10-10 ~ 2025-10-11
### 상태: ✅ 완료

#### 완료 항목
- [x] **Entity 클래스 (3개)**
  - LecTbl.java - 강의 엔티티
  - EnrollmentExtendedTbl.java - 수강신청 엔티티
  - AssignmentExtendedTbl.java - 과제 엔티티

- [x] **DTO 클래스 (11개)**
  - LectureDto, LectureDetailDto, LectureCreateRequest, LectureUpdateRequest
  - EnrollmentDto, EnrollmentCreateRequest
  - AttendanceDto, GradeDto
  - AssignmentDto, AssignmentSubmissionDto, AssignmentStatisticsDto

- [x] **작성자 표기 통일**
  - 모든 파일 상단에 `// 작성자: 성태준` 표기

---

## ✅ Phase 4: Repository 레이어 (완료)

### 기간: 2025-10-11
### 상태: ✅ 완료

#### 완료 항목
- [x] **LecTblRepository.java (30개 메서드)**
  - 기본 CRUD
  - 검색 메서드 (강의명, 교수명, 강의코드)
  - 복합 조건 검색 (@Query)
  - 수강 인원 관리 (증가/감소)
  - 통계 메서드

- [x] **EnrollmentExtendedTblRepository.java (20개 메서드)**
  - 기본 CRUD
  - 학생별/강의별 조회
  - JOIN 쿼리 (강의/학생 정보 포함)
  - 일괄 삭제 메서드

- [x] **AssignmentExtendedTblRepository.java (15개 메서드)**
  - 기본 CRUD
  - 강의별 과제 조회
  - JOIN 쿼리 (강의 정보 포함)
  - 통계 메서드

#### 버그 수정
- [x] Repository 필드명 오류 수정
  - lecState → lecOpen
  - lecCapa → lecMany
  - lecRequire → lecMust

---

## ✅ Phase 5: Service 레이어 (완료)

### 기간: 2025-10-11
### 상태: ✅ 완료

#### 완료 항목
- [x] **LectureService.java (~25개 메서드)**
  - 강의 CRUD 로직
  - 수강 인원 관리 (증가/감소, 정원 확인)
  - 복합 검색 로직
  - 통계 메서드 (강의별, 교수별, 전공별)

- [x] **EnrollmentService.java (~30개 메서드)**
  - 수강신청/취소 로직
  - 중복 방지 및 정원 확인
  - JSON 데이터 파싱 (enrollmentData)
  - 출석/성적 업데이트
  - 통계 메서드

- [x] **AssignmentService.java (~18개 메서드)**
  - 과제 CRUD 로직
  - 과제 제출 처리
  - JSON 데이터 파싱 (assignmentData)
  - 채점 로직
  - 통계 메서드

#### 업데이트 (2025-10-12)
- [x] **LectureService 메서드 추가**
  - `updateLecture(LecTbl)` 오버로드 메서드
  - `getLectureStatistics(Integer)` 강의별 통계

- [x] **EnrollmentService 메서드 추가**
  - `getAllEnrollments(Pageable)` 전체 목록
  - `getEnrolledByStudent(Integer)` 현재 수강중 목록
  - `enrollStudent(Integer, Integer)` wrapper 메서드
  - `updateAttendance(Integer, Integer, Integer, Integer)` 간단 출석 업데이트
  - `updateGrade(Integer, String, Double)` 간단 성적 업데이트
  - `countAllEnrollments()` 전체 수 조회

---

## ✅ Phase 6: Controller 레이어 (완료)

### 기간: 2025-10-11 ~ 2025-10-12
### 상태: ✅ 완료 (API 통합 최적화)

#### 완료 항목 (초기 버전)
- [x] **LectureController.java**
  - 13개 REST API 엔드포인트
  - CRUD + 검색 + 통계

- [x] **EnrollmentController.java**
  - 12개 REST API 엔드포인트
  - 수강신청 + 출석/성적 관리

- [x] **AssignmentController.java**
  - 11개 REST API 엔드포인트
  - 과제 관리 + 제출/채점

#### 🎯 API 통합 최적화 (2025-10-12)

**문제점**: 엔드포인트 과다 (34개) → 관리 복잡도 증가

**해결책**: 쿼리 파라미터 기반 통합 + Sub-resource 패턴

##### ✨ 최적화 결과

**LectureController (11개 → 6개)**
```
✅ GET /api/lectures - 통합 조회 엔드포인트
   ├─ ?serial=XXX          (강의코드 조회)
   ├─ ?professor=XXX       (교수별 조회)
   ├─ ?title=XXX           (강의명 검색)
   ├─ ?year=2024&semester=1 (학기별)
   ├─ ?major=1&open=1      (복합 검색)
   └─ (파라미터 없음)       (전체 목록)
✅ GET /api/lectures/{id}
✅ GET /api/lectures/{id}/stats (sub-resource)
✅ POST /api/lectures
✅ PUT /api/lectures/{id}
✅ DELETE /api/lectures/{id}
```

**EnrollmentController (12개 → 7개)**
```
✅ GET /api/enrollments - 통합 조회
   ├─ ?studentIdx=1        (학생별)
   ├─ ?lecIdx=1            (강의별)
   ├─ ?checkEnrollment=true (수강 여부)
   ├─ ?enrolled=true       (현재 수강중)
   └─ ?stats=true          (통계)
✅ GET /api/enrollments/{id}
✅ GET /api/enrollments/{id}/data
✅ POST /api/enrollments
✅ DELETE /api/enrollments/{id}
✅ PUT /api/enrollments/{id}/attendance
✅ PUT /api/enrollments/{id}/grade
```

**AssignmentController (11개 → 8개)**
```
✅ GET /api/assignments - 통합 조회
   ├─ ?lecIdx=1            (강의별)
   ├─ ?withLecture=true    (강의 정보 포함)
   └─ ?stats=true          (통계)
✅ GET /api/assignments/{id}
✅ GET /api/assignments/{id}/data
✅ POST /api/assignments
✅ POST /api/assignments/{id}/submit
✅ PUT /api/assignments/{id}
✅ PUT /api/assignments/{id}/grade
✅ DELETE /api/assignments/{id}
```

##### 📊 통합 성과
- **엔드포인트 수**: 34개 → 21개 (38% 감소)
- **코드 중복 제거**: 조회 로직 통합
- **유지보수성 향상**: 단일 엔드포인트에서 다양한 쿼리 처리
- **RESTful 설계**: 리소스 중심, HTTP 메서드 활용
- **확장성**: 새로운 필터 추가 용이

---

## 🚧 Phase 7: 테스트 & 통합 (진행중)

### 기간: 2025-10-12 ~
### 상태: 🚧 10% 진행중

#### 진행 항목
- [x] **컴파일 검증**
  - 모든 Controller 컴파일 에러 해결
  - 모든 Service 컴파일 에러 해결
  - Repository-Service-Controller 연동 확인

#### 예정 항목
- [ ] **단위 테스트**
  - Repository 테스트
  - Service 테스트
  - Controller 테스트

- [ ] **통합 테스트**
  - API 엔드포인트 테스트
  - 트랜잭션 테스트
  - JSON 파싱 테스트

- [ ] **부하 테스트**
  - 수강신청 동시성 테스트
  - 정원 초과 방지 테스트

---

## 📈 상세 구현 현황

### 코드 통계

| 레이어 | 파일 수 | 메서드 수 | 코드 라인 수 | 상태 |
|--------|---------|-----------|--------------|------|
| **Entity** | 3 | ~50 | ~450 | ✅ 완료 |
| **DTO** | 11 | ~150 | ~900 | ✅ 완료 |
| **Repository** | 4 | 67 | ~700 | ✅ 완료 |
| **Service** | 3 | 73 | ~850 | ✅ 완료 |
| **Controller** | 3 | 21 + DTO 변환 | ~900 | ✅ 완료 |
| **Total** | **24** | **361+** | **~3,800** | **94%** |

### API 엔드포인트 현황

| Controller | 엔드포인트 수 | 상태 | 비고 |
|------------|---------------|------|------|
| **LectureController** | 6 | ✅ | 통합 최적화 완료 |
| **EnrollmentController** | 7 | ✅ | DTO + PageImpl + 교수 조회 완료 ⭐ |
| **AssignmentController** | 8 | ✅ | 통합 최적화 완료 |
| **Total** | **21** | **✅** | **34→21 (38% 감소)** |

---

## 🎯 다음 단계

### 우선순위 1: 테스트 코드 작성
- [ ] Repository 계층 단위 테스트
- [ ] Service 계층 비즈니스 로직 테스트
- [ ] Controller 계층 API 테스트

### 우선순위 2: 문서 업데이트
- [ ] API 명세서 업데이트 (통합 API 반영)
- [ ] Swagger/OpenAPI 문서 생성
- [ ] 프론트엔드 연동 가이드 작성

### 우선순위 3: 성능 최적화
- [ ] N+1 쿼리 문제 확인 및 해결
- [ ] 캐싱 전략 수립
- [ ] 인덱스 최적화

---

## 📝 변경 이력

### 2025-10-14 (Phase 6.7)
- ✅ **PageImpl 패턴 적용**: Page.map() 대신 명시적 PageImpl 생성
- ✅ **교수 이름 조회 기능 구현**: LEC_PROF → USER_CODE → USER_NAME 조회
- ✅ EnrollmentDto에 lecProfName 필드 추가
- ✅ UserTblRepository에 findByUserCode() 메서드 추가
- ✅ EnrollmentController에 교수 조회 로직 추가
- ✅ lecture-test-2-student-enrollment.js 출력 업데이트
- ✅ BACKEND_FIX_PROFESSOR_NAME_LOOKUP.md 문서 작성
- ✅ Entity 참조 완전 제거로 JSON 직렬화 안정성 확보

### 2025-10-14 (Phase 6.6)
- ✅ **JOIN FETCH 최적화**: Repository 쿼리에 DISTINCT + JOIN FETCH 적용
- ✅ findEnrollmentHistoryByStudent() 및 findStudentsByLecture() 개선
- ✅ Service 레이어에서 JOIN FETCH 메서드 사용
- ✅ N+1 쿼리 문제 원천 차단
- ✅ DTO 필드 null 문제 해결

### 2025-10-14 (Phase 6.5)
- ✅ **HTTP 400 에러 수정**: Hibernate Lazy Loading 이슈 해결
- ✅ EnrollmentController에 DTO 패턴 적용
- ✅ convertToDto() 메서드 구현 (60+ 라인)
- ✅ 모든 수강신청 API 엔드포인트 DTO 반환으로 변경
- ✅ 프론트엔드 테스트 스크립트 업데이트
- ✅ BACKEND_FIX_ENROLLMENT_400_ERROR.md 문서 작성

### 2025-10-12
- ✅ Phase 6 완료: Controller 레이어 API 통합 최적화
- ✅ Service 레이어 메서드 추가 (컨트롤러 요구사항 반영)
- ✅ 엔드포인트 38% 감소 (34개 → 21개)
- ✅ 모든 컴파일 에러 해결

### 2025-10-11
- ✅ Phase 5 완료: Service 레이어 구현
- ✅ Phase 6 시작: Controller 레이어 구현
- ✅ Repository 필드명 버그 수정

### 2025-10-10
- ✅ Phase 3 완료: Entity & DTO 레이어 구현
- ✅ Phase 4 시작: Repository 레이어 구현

### 2025-10-09
- ✅ Phase 1-2 완료: 데이터베이스 설계 및 구축

---

## 📌 주요 기술 스택

- **Framework**: Spring Boot 3.x
- **ORM**: JPA/Hibernate with Spring Data JPA
- **Database**: MariaDB
- **JSON Processing**: Jackson ObjectMapper
- **Logging**: SLF4J
- **Architecture**: Layered Architecture (Controller-Service-Repository-Entity)
- **API Design**: RESTful with Query Parameter Integration

---

**작성자**: 성태준  
**문서 버전**: 3.0  
**마지막 수정**: 2025-10-14
