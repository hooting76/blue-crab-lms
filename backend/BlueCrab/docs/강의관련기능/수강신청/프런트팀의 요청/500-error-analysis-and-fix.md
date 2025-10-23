# 🔴 수강신청 500 에러 원인 분석 및 해결 방법

**작성일**: 2025-10-23  
**상태**: 🔥 긴급 - DB 중복 데이터 문제  
**영향**: POST /api/enrollments/enroll 전체 차단

---

## 📋 문제 요약

### 증상
```
POST /api/enrollments/enroll
Request: {"studentIdx":15,"lecSerial":"CS101"}
Response: 500 Internal Server Error
{"success":false,"message":"수강신청 중 오류가 발생했습니다."}
```

### 발생 시각
- 2025-10-23T07:59:53.917Z (UTC)

---

## 🔍 원인 분석

### 1. 에러 발생 위치
```java
// EnrollmentService.java:141
public EnrollmentExtendedTbl enrollStudentBySerial(Integer studentIdx, String lecSerial) {
    Integer lecIdx = lectureService.getLectureBySerial(lecSerial)  // ⚠️ 여기서 에러!
            .map(lecture -> lecture.getLecIdx())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의 코드입니다: " + lecSerial));
    
    return enrollLecture(studentIdx, lecIdx);
}
```

### 2. 실제 에러
```
javax.persistence.NonUniqueResultException: 
  query did not return a unique result: 2
```

**원인**: `LEC_TBL` 테이블에 `LEC_SERIAL='CS101'`인 레코드가 **2개** 존재

### 3. 에러 체인
```
1. POST /api/enrollments/enroll (studentIdx=15, lecSerial=CS101)
   ↓
2. EnrollmentController.enrollInLecture()
   ↓
3. EnrollmentService.enrollStudentBySerial()
   ↓
4. LectureService.getLectureBySerial("CS101")  ← 여기서 Exception!
   ↓
5. LecTblRepository.findByLecSerial("CS101")
   ↓
6. JPA Query: SELECT ... WHERE LEC_SERIAL='CS101'
   → 결과: 2개 레코드 반환
   → JPA getSingleResult() 내부 호출
   → NonUniqueResultException 발생!
```

### 4. DB 현황
```sql
-- 현재 LEC_TBL 상태
SELECT LEC_IDX, LEC_SERIAL, LEC_TIT 
FROM LEC_TBL 
WHERE LEC_SERIAL = 'CS101';

-- 예상 결과:
-- LEC_IDX=X, LEC_SERIAL='CS101', LEC_TIT='컴퓨터과학개론'
-- LEC_IDX=Y, LEC_SERIAL='CS101', LEC_TIT='컴퓨터과학개론'  ← 중복!
```

---

## 🔧 해결 방법

### 🚨 긴급 조치 (5분 소요)

**SSH 접속**:
```bash
ssh project01@10.125.121.213
```

**MariaDB 접속**:
```bash
mysql -u [사용자명] -p blue_crab
```

**중복 확인 및 제거**:
```sql
-- 1. 중복 확인
SELECT 
    l.LEC_IDX,
    l.LEC_SERIAL,
    l.LEC_TIT,
    COUNT(e.ENROLLMENT_IDX) as ENROLLMENT_COUNT
FROM LEC_TBL l
LEFT JOIN ENROLLMENT_EXTENDED_TBL e ON l.LEC_IDX = e.LEC_IDX
WHERE l.LEC_SERIAL = 'CS101'
GROUP BY l.LEC_IDX
ORDER BY ENROLLMENT_COUNT DESC;

-- 2. 수강생이 없는 레코드 삭제 (LEC_IDX 확인 후 입력)
-- DELETE FROM LEC_TBL WHERE LEC_IDX = [수강생 0명인 LEC_IDX];

-- 3. 삭제 확인 (1개만 남아야 함)
SELECT COUNT(*) FROM LEC_TBL WHERE LEC_SERIAL = 'CS101';
-- 결과: 1
```

**재시작 불필요**: DB 수정 즉시 반영됨

---

## ✅ 검증 방법

### 1. DB 확인
```sql
-- CS101이 정확히 1개만 있는지 확인
SELECT COUNT(*) as CNT FROM LEC_TBL WHERE LEC_SERIAL = 'CS101';
-- 결과: CNT = 1 이어야 함
```

### 2. API 재테스트
```javascript
// 브라우저 콘솔에서
const result = await fetch('https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/enrollments/enroll', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer <accessToken>',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    studentIdx: 15,
    lecSerial: 'CS101'
  })
});

console.log(result.status);  // 201 또는 400 (정상 응답)
console.log(await result.json());
```

**예상 결과**:
- ✅ 201 Created (수강신청 성공)
- ✅ 400 Bad Request "이미 수강신청한 강의입니다" (중복 신청)
- ❌ 500 Internal Server Error (여전히 문제 있음)

---

## 🛡️ 재발 방지

### UNIQUE 제약조건 추가

**다른 중복 확인**:
```sql
SELECT LEC_SERIAL, COUNT(*) as CNT
FROM LEC_TBL
GROUP BY LEC_SERIAL
HAVING COUNT(*) > 1;
```

**중복이 없다면 제약조건 추가**:
```sql
ALTER TABLE LEC_TBL 
ADD UNIQUE INDEX idx_lec_serial_unique (LEC_SERIAL);
```

**효과**:
- 향후 동일한 `LEC_SERIAL`을 가진 레코드 INSERT 시 자동 차단
- DB 레벨에서 데이터 무결성 보장

---

## 📊 프런트엔드 요청사항 응답

### 1. enroll 컨트롤러가 lecSerial을 처리하는지?
✅ **YES** - `EnrollmentController.enrollInLecture()` 메서드가 `lecSerial`을 받아 처리합니다:

```java
@PostMapping("/enroll")
public ResponseEntity<?> enrollInLecture(@RequestBody Map<String, Object> request) {
    Integer studentIdx = (Integer) request.get("studentIdx");
    String lecSerial = (String) request.get("lecSerial");  // ← lecSerial 사용
    
    // lecSerial로 강의 ID 조회는 EnrollmentService에서 처리
    EnrollmentExtendedTbl enrollment = enrollmentService.enrollStudentBySerial(studentIdx, lecSerial);
    // ...
}
```

### 2. 서버 로그/스택트레이스
**발견된 에러**:
```
org.springframework.dao.IncorrectResultSizeDataAccessException: 
  query did not return a unique result: 2

Caused by: javax.persistence.NonUniqueResultException: 
  query did not return a unique result: 2
  at org.hibernate.query.internal.AbstractProducedQuery.getSingleResult(...)
  at BlueCrab.com.example.service.Lecture.LectureService.getLectureBySerial(LectureService.java:171)
  at BlueCrab.com.example.service.Lecture.EnrollmentService.enrollStudentBySerial(EnrollmentService.java:141)
```

**근본 원인**: DB에 CS101이 2개 존재

### 3. 구체적 에러코드/메시지 개선
현재는 모든 Exception이 500으로 처리되지만, 개선안:

```java
// EnrollmentController.java 개선안
@PostMapping("/enroll")
public ResponseEntity<?> enrollInLecture(@RequestBody Map<String, Object> request) {
    try {
        // ... 기존 로직
    } catch (javax.persistence.NonUniqueResultException e) {
        logger.error("중복 강의 데이터 감지: lecSerial={}", lecSerial, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("강의 데이터 오류: 관리자에게 문의하세요 (ERR_DUPLICATE_LECTURE)"));
    } catch (IllegalStateException | IllegalArgumentException e) {
        // 비즈니스 로직 에러 (400)
        return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
    } catch (Exception e) {
        // 기타 서버 에러 (500)
        logger.error("수강신청 실패", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("수강신청 중 오류가 발생했습니다."));
    }
}
```

---

## 🎯 조치 우선순위

1. **🔥 최우선** (지금 즉시): CS101 중복 레코드 삭제
2. **⚠️ 중요** (오늘 중): 다른 강의도 중복 확인 및 UNIQUE 제약조건 추가
3. **📝 개선** (이번 주): 에러 처리 로직 강화 (구체적 에러 메시지)

---

## 📞 문의

**백엔드 담당**: 중복 레코드 삭제 진행 예정  
**프런트팀**: 삭제 완료 후 재테스트 요청 예정

**예상 작업 시간**: 10분 이내  
**서비스 중단 여부**: 없음 (DB 수정만)
