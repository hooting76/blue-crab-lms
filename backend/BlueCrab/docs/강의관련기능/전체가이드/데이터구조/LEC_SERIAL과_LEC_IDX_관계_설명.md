# LEC_SERIAL과 LEC_IDX 관계 설명

## 개요
출석 요청/승인 시스템에서 `lecSerial`을 파라미터로 사용하지만, 내부적으로는 `LEC_IDX`로 매핑되어 처리됩니다.

## DB 스키마 구조

### 1. LEC_TBL (강의 테이블)
```sql
CREATE TABLE LEC_TBL (
    LEC_IDX INT PRIMARY KEY AUTO_INCREMENT,      -- 기본키 (내부 식별자)
    LEC_SERIAL VARCHAR(50) UNIQUE NOT NULL,      -- 강의 코드 (사용자 친화적)
    LEC_TIT VARCHAR(255),                        -- 강의명
    LEC_PROF VARCHAR(50),                        -- 교수 USER_CODE
    ...
);
```

**예시 데이터:**
| LEC_IDX | LEC_SERIAL | LEC_TIT | LEC_PROF |
|---------|------------|---------|----------|
| 6 | CS101 | 컴퓨터과학개론 | 김철수 |
| 7 | CS201 | 자료구조 | 이영희 |

### 2. ENROLLMENT_EXTENDED_TBL (수강신청 테이블)
```sql
CREATE TABLE ENROLLMENT_EXTENDED_TBL (
    ENROLLMENT_IDX INT PRIMARY KEY AUTO_INCREMENT,
    LEC_IDX INT NOT NULL,                        -- 외래키 (LEC_TBL.LEC_IDX 참조)
    STUDENT_IDX INT NOT NULL,                    -- 외래키 (USER_TBL.USER_IDX 참조)
    ENROLLMENT_DATA JSON,                        -- 출석/성적 데이터
    FOREIGN KEY (LEC_IDX) REFERENCES LEC_TBL(LEC_IDX) ON DELETE CASCADE
);
```

**예시 데이터:**
| ENROLLMENT_IDX | LEC_IDX | STUDENT_IDX | ENROLLMENT_DATA |
|----------------|---------|-------------|-----------------|
| 1 | 6 | 6 | {"attendance": {...}} |
| 2 | 7 | 6 | {"attendance": {...}} |

## 데이터 흐름

### 프론트엔드 → 백엔드
```javascript
// 1. 프론트엔드에서 lecSerial 전송
const requestData = {
    lecSerial: "CS101",  // ✅ 사용자 친화적 코드
    sessionNumber: 1
};
```

### Repository 쿼리 처리
```java
// 2. Repository가 LEC_TBL과 JOIN하여 LEC_IDX로 변환
@Query("SELECT e FROM EnrollmentExtendedTbl e " +
       "JOIN FETCH e.lecture l " +
       "WHERE l.lecSerial = :lecSerial AND e.studentIdx = :studentIdx")
Optional<EnrollmentExtendedTbl> findByLecSerialAndStudentIdx(
    @Param("lecSerial") String lecSerial,    // "CS101" 입력
    @Param("studentIdx") Integer studentIdx
);

// 실제 실행되는 SQL (JPA가 자동 생성):
/*
SELECT e.*, l.*
FROM ENROLLMENT_EXTENDED_TBL e
INNER JOIN LEC_TBL l ON e.LEC_IDX = l.LEC_IDX
WHERE l.LEC_SERIAL = 'CS101'         -- lecSerial로 검색
  AND e.STUDENT_IDX = 6;             -- LEC_IDX = 6 자동 매핑
*/
```

### 처리 과정 상세
```
Step 1: lecSerial "CS101" 입력
         ↓
Step 2: LEC_TBL과 JOIN
         WHERE LEC_SERIAL = "CS101"
         ↓
Step 3: LEC_IDX = 6 추출 (자동 매핑)
         ↓
Step 4: ENROLLMENT_EXTENDED_TBL 조회
         WHERE LEC_IDX = 6 AND STUDENT_IDX = 6
         ↓
Step 5: ENROLLMENT_DATA JSON 반환
         {"attendance": {"summary": {...}, "sessions": [...], ...}}
```

## 왜 이렇게 설계했나?

### 장점
1. **사용자 친화성**: 프론트엔드에서 "CS101" 같은 의미 있는 코드 사용
2. **보안**: 내부 ID(LEC_IDX) 노출 방지
3. **유연성**: 강의 코드 변경 시 LEC_IDX는 그대로 유지
4. **일관성**: 기존 시스템과 호환 (다른 API도 lecSerial 사용)

### 단점
1. **JOIN 비용**: 매번 LEC_TBL과 JOIN 필요
   - 해결: `JOIN FETCH`로 N+1 문제 방지, 인덱스 최적화

## API 사용 예시

### 1. 학생 출석 요청
```javascript
POST /api/attendance/request
{
    "lecSerial": "CS101",      // ✅ 사용자 친화적
    "sessionNumber": 1,
    "requestReason": "교통체증"
}

// 내부 처리:
// lecSerial "CS101" → LEC_IDX 6 → ENROLLMENT_EXTENDED_TBL 업데이트
```

### 2. 학생 출석 조회
```javascript
POST /api/attendance/student/view
{
    "lecSerial": "CS101"       // ✅ 사용자 친화적
}

// 내부 처리:
// lecSerial "CS101" → LEC_IDX 6 → ENROLLMENT_DATA JSON 조회
```

### 3. 교수 출석 승인
```javascript
POST /api/attendance/approve
{
    "lecSerial": "CS101",      // ✅ 사용자 친화적
    "sessionNumber": 1,
    "attendanceRecords": [
        {"studentIdx": 6, "status": "출"}
    ]
}

// 내부 처리:
// 1. 교수 권한 검증: lecSerial "CS101" + professorIdx
// 2. LEC_IDX 6 확인
// 3. ENROLLMENT_EXTENDED_TBL 업데이트
```

## 성능 최적화

### 1. 인덱스 설정
```sql
-- LEC_TBL
CREATE INDEX idx_lec_serial ON LEC_TBL(LEC_SERIAL);

-- ENROLLMENT_EXTENDED_TBL
CREATE INDEX idx_lec_student ON ENROLLMENT_EXTENDED_TBL(LEC_IDX, STUDENT_IDX);
```

### 2. JOIN FETCH 사용
```java
// N+1 문제 방지
@Query("SELECT e FROM EnrollmentExtendedTbl e " +
       "JOIN FETCH e.lecture l " +  // ✅ 한 번에 조회
       "WHERE l.lecSerial = :lecSerial")
```

### 3. 캐싱 (향후 고려사항)
```java
// Redis 캐싱으로 lecSerial → LEC_IDX 매핑 캐시
@Cacheable("lecSerialToIdx")
public Integer getLecIdxBySerial(String lecSerial) {
    // ...
}
```

## 요약

| 항목 | 설명 |
|------|------|
| **프론트엔드** | `lecSerial` (예: "CS101") 사용 |
| **백엔드 API** | `lecSerial` 파라미터 받음 |
| **Repository** | `LEC_TBL`과 JOIN하여 `LEC_IDX` 매핑 |
| **DB 조회** | `ENROLLMENT_EXTENDED_TBL.LEC_IDX`로 조회 |
| **장점** | 사용자 친화적, 보안, 유연성 |
| **주의사항** | JOIN 비용 (인덱스 최적화 필요) |

## 참고 문서
- `EnrollmentExtendedTblRepository.java` - Repository 쿼리 구현
- `AttendanceRequestServiceImpl.java` - 비즈니스 로직
- `AttendanceController.java` - API 엔드포인트
- 브라우저 콘솔 테스트 스크립트 (01-04.js)
