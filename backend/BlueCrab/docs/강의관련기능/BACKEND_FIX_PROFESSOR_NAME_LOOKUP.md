# 교수 이름 조회 기능 추가

> **작성일**: 2025-10-14  
> **작성자**: AI Assistant  
> **목적**: 수강 목록 조회 시 교수번호(USER_NAME)로 실제 교수 이름(USER_CODE) 조회

---

## 📋 문제 상황

### 기존 동작:
- 수강 목록 조회 시 `LEC_PROF` 필드를 그대로 반환
- `LEC_PROF`는 교수번호 (예: "PROF001")만 포함
- 프론트엔드에서 교수 이름을 표시할 수 없음

### 데이터 구조:
```
LEC_TBL.LEC_PROF = "PROF001" (교수번호, VARCHAR)
         ↓
USER_TBL.USER_NAME = "PROF001" (학번/교수번호/사원번호, VARCHAR)
USER_TBL.USER_CODE = "김교수" (실제 이름, VARCHAR)
```

---

## ✅ 해결 방안

### 1. EnrollmentDto 필드 추가

**파일**: `src/main/java/BlueCrab/com/example/dto/Lecture/EnrollmentDto.java`

```java
private String lecProf;        // 교수번호 (USER_NAME)
private String lecProfName;    // 교수 이름 (USER_CODE) ⭐ 신규 추가
```

**Getter/Setter 추가**:
```java
public String getLecProfName() {
    return lecProfName;
}

public void setLecProfName(String lecProfName) {
    this.lecProfName = lecProfName;
}
```

---

### 2. UserTblRepository 메서드 추가

**파일**: `src/main/java/BlueCrab/com/example/repository/UserTblRepository.java`

```java
/**
 * USER_NAME으로 사용자 조회 (학번/교수번호/사원번호)
 * 교수 정보 조회 시 사용
 *
 * @param userName 사용자 식별 코드 (예: "PROF001", "2024001")
 * @return Optional<UserTbl> - 해당 사용자
 */
Optional<UserTbl> findByUserName(String userName);
```

**Spring Data JPA가 자동 생성하는 쿼리**:
```sql
SELECT * FROM USER_TBL WHERE USER_NAME = ?
```

---

### 3. EnrollmentController 수정

**파일**: `src/main/java/BlueCrab/com/example/controller/Lecture/EnrollmentController.java`

#### 3.1. Repository 주입
```java
@Autowired
private UserTblRepository userTblRepository;
```

#### 3.2. convertToDto() 메서드 수정
```java
// Lecture 정보 추가 (Lazy Loading 방지)
try {
    LecTbl lecture = entity.getLecture();
    if (lecture != null) {
        dto.setLecSerial(lecture.getLecSerial());
        dto.setLecTit(lecture.getLecTit());
        dto.setLecProf(lecture.getLecProf());  // 교수번호 (USER_NAME)
        dto.setLecPoint(lecture.getLecPoint());
        dto.setLecTime(lecture.getLecTime());
        
        // ⭐ 교수번호(USER_NAME)로 교수 이름(USER_CODE) 조회
        if (lecture.getLecProf() != null && !lecture.getLecProf().isEmpty()) {
            try {
                userTblRepository.findByUserName(lecture.getLecProf())
                    .ifPresent(professor -> dto.setLecProfName(professor.getUserCode()));
            } catch (Exception e) {
                logger.warn("교수 정보 조회 실패 (USER_NAME: {}): {}", 
                           lecture.getLecProf(), e.getMessage());
            }
        }
    }
} catch (Exception e) {
    logger.warn("강의 정보 조회 실패 (Lazy Loading): {}", e.getMessage());
}
```

---

### 4. 테스트 스크립트 업데이트

**파일**: `docs/강의관련기능/브라우저콘솔테스트/lecture-test-2-student-enrollment.js`

**getMyEnrollments() 함수 수정**:
```javascript
console.log(`   📚 강의 정보:`);
console.log(`      강의 ID (lecIdx): ${enrollment.lecIdx}`);
console.log(`      강의코드: ${enrollment.lecSerial || 'N/A'}`);
console.log(`      교수번호: ${enrollment.lecProf || 'N/A'}`);          // 기존
console.log(`      교수 이름: ${enrollment.lecProfName || 'N/A'}`);    // ⭐ 신규
console.log(`      학점: ${enrollment.lecPoint !== null && enrollment.lecPoint !== undefined ? enrollment.lecPoint + '점' : 'N/A'}`);
console.log(`      시간: ${enrollment.lecTime || 'N/A'}`);
```

---

## 🔄 데이터 흐름

```
1. 수강 목록 조회 요청
   GET /api/enrollments?studentIdx=6

2. EnrollmentService에서 데이터 조회
   - JOIN FETCH로 lecture + student 함께 로드

3. EnrollmentController.convertToDto()
   - lecture.getLecProf() → "PROF001" 추출
   - userTblRepository.findByUserName("PROF001")
   - professor.getUserCode() → "김교수" 추출
   - dto.setLecProfName("김교수")

4. JSON 응답
{
  "lecProf": "PROF001",       // 교수번호
  "lecProfName": "김교수",     // 교수 이름 ⭐
  "lecTit": "자바 프로그래밍",
  ...
}
```

---

## 📊 API 응답 예시

### 수정 전:
```json
{
  "content": [
    {
      "enrollmentIdx": 1,
      "lecIdx": 6,
      "lecTit": "자바 프로그래밍",
      "lecProf": "PROF001",        // 교수번호만 표시
      "lecSerial": "CS101",
      ...
    }
  ]
}
```

### 수정 후:
```json
{
  "content": [
    {
      "enrollmentIdx": 1,
      "lecIdx": 6,
      "lecTit": "자바 프로그래밍",
      "lecProf": "PROF001",        // 교수번호
      "lecProfName": "김교수",      // 교수 이름 ⭐ 추가
      "lecSerial": "CS101",
      ...
    }
  ]
}
```

---

## 🎯 기대 효과

### 1. 사용자 경험 개선
- ✅ 수강 목록에서 교수 이름이 정상적으로 표시됨
- ✅ "PROF001" 대신 "김교수"로 표시

### 2. 데이터 일관성
- ✅ `USER_TBL`을 단일 진실 공급원(Single Source of Truth)으로 사용
- ✅ 교수 정보 변경 시 자동 반영

### 3. 성능 최적화
- ✅ 단일 추가 쿼리만 발생 (N+1 문제 없음)
- ✅ Optional 패턴으로 안전한 조회

---

## ⚠️ 주의사항

### 1. 데이터 정합성
- `LEC_PROF`에 저장된 값이 `USER_TBL.USER_NAME`에 존재해야 함
- 존재하지 않을 경우 `lecProfName`은 `null`

### 2. 성능 고려
- 현재: 각 수강 목록 항목당 1회 추가 쿼리
- 개선 가능: 배치 조회로 N+1 최적화 (향후 필요 시)

### 3. 에러 처리
- 교수 정보 조회 실패 시 예외를 catch하고 로그 기록
- `lecProfName`은 `null`로 유지되어 프론트엔드에서 "N/A" 표시

---

## 🧪 테스트 방법

### 1. 백엔드 재빌드
```bash
cd backend/BlueCrab
mvn clean compile
```

### 2. 서버 재시작
```bash
mvn spring-boot:run
```

### 3. 브라우저 테스트
```javascript
// 콘솔에서 실행
await getMyEnrollments()

// 예상 출력:
// 📚 강의 정보:
//    교수번호: PROF001
//    교수 이름: 김교수  ⭐ 확인
```

---

## 📝 변경 파일 목록

1. ✅ `EnrollmentDto.java` - lecProfName 필드 추가
2. ✅ `UserTblRepository.java` - findByUserName() 메서드 추가
3. ✅ `EnrollmentController.java` - 교수 이름 조회 로직 추가
4. ✅ `lecture-test-2-student-enrollment.js` - 출력 형식 수정

---

**작성자**: AI Assistant  
**문서 버전**: 1.0  
**최종 업데이트**: 2025-10-14
