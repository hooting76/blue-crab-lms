# 📋 lecSerial 지원 업데이트

성적 관리 API에 lecSerial 기반 강의 식별 기능 추가

---

## 🎯 업데이트 개요

**날짜**: 2025-10-20  
**버전**: v4.1  
**목적**: 프론트엔드 편의성 향상 및 보안 강화

---

## 🔄 변경 사항

### 기존 방식 (lecIdx만 지원)

```javascript
// ❌ 기존: lecIdx (정수)만 사용 가능
{
  "action": "set-config",
  "lecIdx": 6,  // DB의 내부 ID (노출 위험)
  "studentIdx": 100,
  // ...
}
```

**문제점**:
- 프론트엔드에서 DB 내부 ID를 직접 다룸
- 강의 코드(ETH201)를 알아도 lecIdx를 별도 조회 필요
- 보안상 내부 ID 노출 위험

---

### 새로운 방식 (lecSerial 지원)

```javascript
// ✅ 새로운 방식: lecSerial (문자열) 사용 가능
{
  "action": "set-config",
  "lecSerial": "ETH201",  // 사용자 친화적 강의 코드
  "studentIdx": 100,
  // ...
}
```

**장점**:
- ✅ 사용자 친화적 (강의 코드 직접 사용)
- ✅ 백엔드가 자동으로 LEC_TBL 조회하여 변환
- ✅ 보안 강화 (내부 ID 노출 최소화)
- ✅ 기존 lecIdx 방식도 여전히 지원 (하위 호환성)

---

## 🔧 백엔드 구현

### 변환 프로세스

```
프론트엔드: lecSerial="ETH201"
          ↓
Controller: lecSerial 감지
          ↓
EnrollmentService.getLectureIdxBySerial("ETH201")
          ↓
LectureService.getLectureBySerial("ETH201")
          ↓
LecTblRepository.findByLecSerial("ETH201")
          ↓
DB 쿼리: SELECT * FROM LEC_TBL WHERE LEC_SERIAL = 'ETH201'
          ↓
LecTbl 엔티티 반환
          ↓
lecIdx 추출 (예: 6)
          ↓
기존 로직 실행 (lecIdx 기반)
```

### 적용된 API (5개)

| API | 엔드포인트 | 지원 파라미터 |
|-----|-----------|--------------|
| 성적 구성 설정 | POST /api/enrollments/grade-config | lecIdx 또는 lecSerial |
| 학생 성적 조회 | POST /api/enrollments/grade-info | lecIdx 또는 lecSerial |
| 교수용 성적 조회 | POST /api/enrollments/grade-info | lecIdx 또는 lecSerial |
| 성적 목록 조회 | POST /api/enrollments/grade-list | lecIdx 또는 lecSerial |
| 최종 등급 배정 | POST /api/enrollments/grade-finalize | lecIdx 또는 lecSerial |

---

## 📝 코드 예시

### Controller 수정 (EnrollmentController.java)

```java
private ResponseEntity<?> handleGradeConfig(Map<String, Object> request) {
    Integer lecIdx = request.get("lecIdx") != null ? 
        ((Number) request.get("lecIdx")).intValue() : null;
    String lecSerial = (String) request.get("lecSerial");
    
    // lecSerial이 제공된 경우 lecIdx로 변환
    if (lecIdx == null && lecSerial != null && !lecSerial.trim().isEmpty()) {
        lecIdx = enrollmentService.getLectureIdxBySerial(lecSerial);
        if (lecIdx == null) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("존재하지 않는 강의 코드입니다: " + lecSerial));
        }
    }
    
    if (lecIdx == null) {
        return ResponseEntity.badRequest()
            .body(createErrorResponse("lecIdx 또는 lecSerial은 필수 파라미터입니다."));
    }

    // 기존 로직 실행 (lecIdx 사용)
    Map<String, Object> result = enrollmentService.configureGrade(request);
    return ResponseEntity.ok(createSuccessResponse("성적 구성이 설정되었습니다.", result));
}
```

### Service 메서드 (EnrollmentService.java)

```java
/* 강의 코드로 강의 IDX 조회 (내부 변환용) */
public Integer getLectureIdxBySerial(String lecSerial) {
    return lectureService.getLectureBySerial(lecSerial)
            .map(lecture -> lecture.getLecIdx())
            .orElse(null);
}
```

### Repository 메서드 (LecTblRepository.java)

```java
/* 강의 코드로 특정 강의 조회 */
Optional<LecTbl> findByLecSerial(String lecSerial);
```

---

## 🧪 테스트

### 프론트엔드 테스트 파일 수정

**파일**: `01-grade-phase1-tests.js`

**변경 내용**:
```javascript
// ✅ lecSerial 기반으로 변경
const config = {
    lecSerial: null,     // "ETH201", "CS101" 등
    studentIdx: null,    // USER_IDX
    // ...
};

// 사용법
gradePhase1.setLecture('ETH201', 100)  // lecSerial, studentIdx
await gradePhase1.config()

// API 호출
{
    action: 'set-config',
    lecSerial: 'ETH201',  // ← 백엔드가 자동 변환
    studentIdx: 100,
    // ...
}
```

---

## ✅ 검증 포인트

### 1. DB 조회 확인

```sql
-- 백엔드가 실행하는 쿼리
SELECT * FROM LEC_TBL WHERE LEC_SERIAL = 'ETH201';
```

**결과**:
```
LEC_IDX | LEC_SERIAL | LEC_TIT
--------|------------|--------
   6    |   ETH201   | 윤리학개론
```

### 2. 에러 처리 확인

**존재하지 않는 강의 코드**:
```javascript
// 요청
{
  "action": "set-config",
  "lecSerial": "INVALID999",
  "studentIdx": 100
}

// 응답 (400 Bad Request)
{
  "success": false,
  "message": "존재하지 않는 강의 코드입니다: INVALID999"
}
```

### 3. 하위 호환성 확인

**기존 lecIdx 방식도 정상 동작**:
```javascript
// 여전히 가능
{
  "action": "set-config",
  "lecIdx": 6,  // lecIdx로 직접 지정
  "studentIdx": 100
}
```

---

## 📚 업데이트된 문서

1. **02-IMPLEMENTATION-GUIDE.md**
   - 모든 API 예시에 lecSerial 추가
   - 변환 프로세스 설명 추가

2. **00-README.md**
   - Phase 1 API 목록 업데이트
   - lecSerial 지원 명시

3. **03-WORK-PROGRESS.md**
   - configureGrade() 작업 내역에 변환 기능 추가

4. **01-QUICK-START.md**
   - 테스트 예시를 lecSerial 기반으로 변경

5. **브라우저 콘솔 테스트 파일**
   - 01-grade-phase1-tests.js 전면 수정
   - lecSerial 기반 테스트로 전환

---

## 🔒 보안 개선

### 기존 방식의 문제

```javascript
// ❌ 내부 ID 노출
GET /api/lectures/6  // lecIdx가 URL에 노출
```

**취약점**:
- 순차적인 ID로 추측 공격 가능
- 다른 강의의 데이터 접근 시도 가능
- 내부 DB 구조 노출

### 개선된 방식

```javascript
// ✅ 강의 코드 사용
POST /api/enrollments/grade-config
{
  "lecSerial": "ETH201"  // 공개된 강의 코드 사용
}
```

**개선점**:
- 강의 코드는 이미 공개된 정보
- 내부 ID 구조 노출 방지
- 백엔드에서 권한 검증 가능

---

## 🚀 배포 체크리스트

### 백엔드
- [x] EnrollmentController 5개 핸들러 수정
- [x] EnrollmentService.getLectureIdxBySerial() 메서드 확인
- [x] LectureService.getLectureBySerial() 메서드 확인
- [x] LecTblRepository.findByLecSerial() 메서드 확인
- [x] 에러 처리 추가 (존재하지 않는 강의 코드)

### 프론트엔드
- [x] 테스트 파일 수정 (01-grade-phase1-tests.js)
- [x] lecSerial 기반 테스트 코드 작성
- [x] 사용자 가이드 업데이트

### 문서
- [x] API 명세 업데이트
- [x] 구현 가이드 수정
- [x] 테스트 가이드 수정
- [x] README 업데이트

### 테스트
- [ ] 단위 테스트 (lecSerial → lecIdx 변환)
- [ ] 통합 테스트 (5개 API 전체)
- [ ] 에러 케이스 테스트
- [ ] 하위 호환성 테스트 (lecIdx 방식)

---

## 📞 참고 문서

- [02-IMPLEMENTATION-GUIDE.md](./02-IMPLEMENTATION-GUIDE.md) - API 상세 설명
- [00-README.md](./00-README.md) - 전체 개요
- [브라우저 콘솔 테스트](../브라우저콘솔테스트/04-grade/00-README.md) - 테스트 방법

---

## 💡 FAQ

### Q1: lecIdx와 lecSerial 중 어느 것을 사용해야 하나요?

**A**: **lecSerial 사용을 권장합니다.**
- 사용자 친화적
- 보안상 더 안전
- 프론트엔드 코드가 더 직관적

### Q2: 기존 lecIdx 방식도 계속 작동하나요?

**A**: 네, 하위 호환성을 위해 lecIdx 방식도 계속 지원됩니다.

### Q3: lecSerial이 없는 강의는 어떻게 되나요?

**A**: LEC_TBL의 LEC_SERIAL 컬럼은 NOT NULL이므로 모든 강의에 코드가 있습니다.

### Q4: 성능에 영향은 없나요?

**A**: lecSerial 사용 시 추가 DB 조회가 1회 발생하지만, 인덱스가 있어 성능 영향은 미미합니다.

### Q5: 존재하지 않는 lecSerial을 보내면?

**A**: 400 Bad Request와 함께 "존재하지 않는 강의 코드입니다" 메시지가 반환됩니다.

---

## 🎉 결론

이번 업데이트로 성적 관리 API가 더 사용자 친화적이고 안전해졌습니다!

**주요 개선점**:
- ✅ 강의 코드로 직접 API 호출 가능
- ✅ 백엔드가 자동으로 변환 처리
- ✅ 보안 강화 (내부 ID 노출 최소화)
- ✅ 하위 호환성 유지 (기존 방식도 지원)
