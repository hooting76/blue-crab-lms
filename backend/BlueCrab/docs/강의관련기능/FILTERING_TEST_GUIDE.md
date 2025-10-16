# 수강신청 필터링 테스트 가이드

## 🧪 테스트 준비

### 1. 데이터베이스 테스트 데이터 준비

#### USER_TBL 샘플 데이터
```sql
-- 학생 사용자 생성
INSERT INTO USER_TBL (USER_EMAIL, USER_PW, USER_NAME, USER_CODE, USER_PHONE, USER_BIRTH, USER_STUDENT)
VALUES 
('student1@test.com', 'password', '김철수', '2025-001-05-001', '01012345678', '20000101', 0),
('student2@test.com', 'password', '이영희', '2025-002-03-002', '01087654321', '20000202', 0),
('student3@test.com', 'password', '박민수', '2025-001-05-003', '01011112222', '20000303', 0);
```

#### SERIAL_CODE_TABLE 샘플 데이터
```sql
-- 학생1: 주전공 01-05 (공대-컴공), 부전공 없음
INSERT INTO SERIAL_CODE_TABLE (USER_IDX, SERIAL_CODE, SERIAL_SUB, SERIAL_REG)
VALUES (1, '01', '05', NOW());

-- 학생2: 주전공 02-03 (경영-경영), 부전공 01-05 (공대-컴공)
INSERT INTO SERIAL_CODE_TABLE (USER_IDX, SERIAL_CODE, SERIAL_SUB, SERIAL_CODE_ND, SERIAL_SUB_ND, SERIAL_REG, SERIAL_REG_ND)
VALUES (2, '02', '03', '01', '05', NOW(), NOW());

-- 학생3: 전공 정보 없음 (테스트용)
-- (데이터 없음)
```

#### LEC_TBL 샘플 강의
```sql
-- 강의1: 0값 강의 (전체 공개)
INSERT INTO LEC_TBL (LEC_SERIAL, LEC_TIT, LEC_PROF, LEC_OPEN, LEC_MANY, LEC_CURRENT, LEC_MCODE, LEC_MCODE_DEP, LEC_YEAR, LEC_SEMESTER)
VALUES ('GE101', '대학글쓰기', 'P001', 1, 100, 0, '0', '0', 2025, 1);

-- 강의2: 컴퓨터공학과 전용 (01-05)
INSERT INTO LEC_TBL (LEC_SERIAL, LEC_TIT, LEC_PROF, LEC_OPEN, LEC_MANY, LEC_CURRENT, LEC_MCODE, LEC_MCODE_DEP, LEC_YEAR, LEC_SEMESTER)
VALUES ('CS101', '자료구조', 'P002', 1, 50, 0, '01', '05', 2025, 1);

-- 강의3: 경영학과 전용 (02-03)
INSERT INTO LEC_TBL (LEC_SERIAL, LEC_TIT, LEC_PROF, LEC_OPEN, LEC_MANY, LEC_CURRENT, LEC_MCODE, LEC_MCODE_DEP, LEC_YEAR, LEC_SEMESTER)
VALUES ('BUS201', '마케팅원론', 'P003', 1, 40, 0, '02', '03', 2025, 1);

-- 강의4: 공대 전체 (학과 무관)
INSERT INTO LEC_TBL (LEC_SERIAL, LEC_TIT, LEC_PROF, LEC_OPEN, LEC_MANY, LEC_CURRENT, LEC_MCODE, LEC_MCODE_DEP, LEC_YEAR, LEC_SEMESTER)
VALUES ('ENG301', '공학윤리', 'P004', 1, 60, 0, '01', '0', 2025, 1);

-- 강의5: 정원 초과 강의
INSERT INTO LEC_TBL (LEC_SERIAL, LEC_TIT, LEC_PROF, LEC_OPEN, LEC_MANY, LEC_CURRENT, LEC_MCODE, LEC_MCODE_DEP, LEC_YEAR, LEC_SEMESTER)
VALUES ('CS102', '알고리즘', 'P002', 1, 30, 30, '01', '05', 2025, 1);

-- 강의6: 미개설 강의
INSERT INTO LEC_TBL (LEC_SERIAL, LEC_TIT, LEC_PROF, LEC_OPEN, LEC_MANY, LEC_CURRENT, LEC_MCODE, LEC_MCODE_DEP, LEC_YEAR, LEC_SEMESTER)
VALUES ('CS103', '운영체제', 'P002', 0, 50, 0, '01', '05', 2025, 1);
```

---

## 🧪 테스트 케이스

### 테스트 1: 학생1 (주전공 01-05)

**학생 정보**:
- 이메일: `student1@test.com`
- 주전공: 01-05 (공대-컴공)
- 부전공: 없음

**API 호출**:
```bash
curl -X POST http://localhost:8080/api/lectures/eligible \
-H "Content-Type: application/json" \
-d '{
  "studentId": 1,
  "page": 0,
  "size": 20
}'
```

**예상 결과**:
| 강의코드 | 강의명 | 수강가능 | 사유 |
|---------|-------|----------|------|
| GE101 | 대학글쓰기 | ✅ | 제한 없음 - 전체 학생 대상 |
| CS101 | 자료구조 | ✅ | 전공 일치: 01-05 |
| BUS201 | 마케팅원론 | ❌ | 학부 코드 불일치 (요구: 02, 보유: 01) |
| ENG301 | 공학윤리 | ✅ | 전공 일치 (학과 제한 없음) |
| CS102 | 알고리즘 | ❌ | 정원이 초과되었습니다 (30/30) |
| CS103 | 운영체제 | ❌ | 개설되지 않은 강의입니다 |

**eligibleCount**: 3개 (GE101, CS101, ENG301)

---

### 테스트 2: 학생2 (주전공 02-03, 부전공 01-05)

**학생 정보**:
- 이메일: `student2@test.com`
- 주전공: 02-03 (경영-경영)
- 부전공: 01-05 (공대-컴공)

**API 호출**:
```bash
curl -X POST http://localhost:8080/api/lectures/eligible \
-H "Content-Type: application/json" \
-d '{
  "studentId": 2,
  "page": 0,
  "size": 20
}'
```

**예상 결과**:
| 강의코드 | 강의명 | 수강가능 | 사유 |
|---------|-------|----------|------|
| GE101 | 대학글쓰기 | ✅ | 제한 없음 - 전체 학생 대상 |
| CS101 | 자료구조 | ✅ | 부전공 일치: 01-05 |
| BUS201 | 마케팅원론 | ✅ | 전공 일치: 02-03 |
| ENG301 | 공학윤리 | ✅ | 부전공 일치 (학과 제한 없음) |
| CS102 | 알고리즘 | ❌ | 정원이 초과되었습니다 (30/30) |
| CS103 | 운영체제 | ❌ | 개설되지 않은 강의입니다 |

**eligibleCount**: 4개 (GE101, CS101, BUS201, ENG301)  
**특이사항**: 부전공 덕분에 컴공 강의도 수강 가능! 🎓

---

### 테스트 3: 학생3 (전공 정보 없음)

**학생 정보**:
- 이메일: `student3@test.com`
- 주전공: 없음
- 부전공: 없음

**API 호출**:
```bash
curl -X POST http://localhost:8080/api/lectures/eligible \
-H "Content-Type: application/json" \
-d '{
  "studentId": 3,
  "page": 0,
  "size": 20
}'
```

**예상 결과**:
| 강의코드 | 강의명 | 수강가능 | 사유 |
|---------|-------|----------|------|
| GE101 | 대학글쓰기 | ✅ | 제한 없음 - 전체 학생 대상 |
| CS101 | 자료구조 | ❌ | 전공 정보가 등록되지 않았습니다 (0값 강의만 수강 가능) |
| BUS201 | 마케팅원론 | ❌ | 전공 정보가 등록되지 않았습니다 (0값 강의만 수강 가능) |
| ENG301 | 공학윤리 | ❌ | 전공 정보가 등록되지 않았습니다 (0값 강의만 수강 가능) |
| CS102 | 알고리즘 | ❌ | 정원이 초과되었습니다 (30/30) |
| CS103 | 운영체제 | ❌ | 개설되지 않은 강의입니다 |

**eligibleCount**: 1개 (GE101만!)  
**특이사항**: 전공 정보가 없으면 0값 강의만 수강 가능 ⚠️

---

## 🔍 응답 구조 검증

### 성공 응답 (200 OK)

```json
{
  "eligibleLectures": [
    {
      "lecIdx": 101,
      "lecSerial": "CS101",
      "lecTit": "자료구조",
      "lecProf": "P002",
      "lecPoint": 3,
      "lecTime": "월3,4 수3,4",
      "lecCurrent": 0,
      "lecMany": 50,
      "lecMcode": "01",
      "lecMcodeDep": "05",
      "lecMin": 2,
      "isEligible": true,
      "eligibilityReason": "수강 가능 (전공 일치: 01-05)"
    }
  ],
  "totalCount": 150,
  "eligibleCount": 45,
  "ineligibleCount": 105,
  "pagination": {
    "currentPage": 0,
    "pageSize": 20,
    "totalElements": 45,
    "totalPages": 3
  },
  "studentInfo": {
    "userIdx": 1,
    "userName": "김철수",
    "userEmail": "student1@test.com",
    "userStudent": 0,
    "majorFacultyCode": "01",
    "majorDeptCode": "05",
    "minorFacultyCode": null,
    "minorDeptCode": null,
    "hasMajorInfo": true,
    "hasMinorInfo": false
  }
}
```

### 오류 응답 (400 Bad Request)

**studentId 누락**:
```json
{
  "success": false,
  "message": "studentId는 필수입니다."
}
```

**존재하지 않는 학생**:
```json
{
  "success": false,
  "message": "존재하지 않는 학생입니다."
}
```

**교수 권한 오류**:
```json
{
  "success": false,
  "message": "학생 권한이 필요합니다."
}
```

---

## 🧩 브라우저 콘솔 테스트

### JavaScript Fetch API

```javascript
// 학생1 수강가능 강의 조회
fetch('http://localhost:8080/api/lectures/eligible', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    // 'Authorization': 'Bearer YOUR_JWT_TOKEN' // 인증 필요 시
  },
  body: JSON.stringify({
    studentId: 1,
    page: 0,
    size: 20
  })
})
.then(response => response.json())
.then(data => {
  console.log('총 강의 수:', data.totalCount);
  console.log('수강 가능:', data.eligibleCount);
  console.log('수강 불가:', data.ineligibleCount);
  console.log('학생 정보:', data.studentInfo);
  
  // 수강 가능 강의 목록
  data.eligibleLectures.forEach(lecture => {
    console.log(`[${lecture.lecSerial}] ${lecture.lecTit} - ${lecture.eligibilityReason}`);
  });
})
.catch(error => console.error('Error:', error));
```

---

## 📊 필터링 로직 검증 체크리스트

### ✅ 개설 여부 체크
- [ ] `LEC_OPEN = 1`인 강의만 수강 가능
- [ ] `LEC_OPEN = 0` 또는 NULL이면 불가

### ✅ 정원 체크
- [ ] `LEC_CURRENT < LEC_MANY`일 때만 수강 가능
- [ ] 정원 초과 시 "정원이 초과되었습니다 (X/Y)" 메시지

### ✅ 0값 규칙 (학부)
- [ ] `LEC_MCODE = "0"` → 모든 학부 가능
- [ ] `LEC_MCODE ≠ "0"` → 주전공 또는 부전공 학부 일치 필요

### ✅ 0값 규칙 (학과)
- [ ] `LEC_MCODE_DEP = "0"` → 모든 학과 가능
- [ ] `LEC_MCODE_DEP ≠ "0"` → 주전공 또는 부전공 학과 일치 필요

### ✅ 전공 정보 없는 학생
- [ ] 0값 강의(`0-0`)만 수강 가능
- [ ] "전공 정보가 등록되지 않았습니다" 메시지

### ✅ 부전공 매칭
- [ ] 부전공 학부/학과로도 수강 가능
- [ ] "부전공 일치" 메시지 출력

### ✅ 상세 사유 메시지
- [ ] 수강 가능: "전공 일치", "부전공 일치", "제한 없음" 등
- [ ] 수강 불가: "학부 불일치", "학과 불일치", "정원 초과" 등

---

## 🔧 디버깅 가이드

### 1. 로그 확인

**application.properties**:
```properties
# SQL 로그 활성화
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# 컨트롤러 로그 활성화
logging.level.BlueCrab.com.example.controller.Lecture.LectureController=DEBUG
```

### 2. 학생 프로필 확인 쿼리

```sql
-- 학생의 전공 정보 확인
SELECT 
    u.USER_IDX,
    u.USER_NAME,
    u.USER_EMAIL,
    s.SERIAL_CODE AS major_faculty,
    s.SERIAL_SUB AS major_dept,
    s.SERIAL_CODE_ND AS minor_faculty,
    s.SERIAL_SUB_ND AS minor_dept
FROM USER_TBL u
LEFT JOIN SERIAL_CODE_TABLE s ON u.USER_IDX = s.USER_IDX
WHERE u.USER_EMAIL = 'student1@test.com';
```

### 3. 강의 필터링 쿼리 (수동 검증)

```sql
-- 학생1이 수강 가능한 강의 (수동 쿼리)
SELECT 
    l.LEC_SERIAL,
    l.LEC_TIT,
    l.LEC_MCODE,
    l.LEC_MCODE_DEP,
    l.LEC_OPEN,
    l.LEC_CURRENT,
    l.LEC_MANY,
    CASE 
        WHEN l.LEC_OPEN != 1 THEN '개설 안됨'
        WHEN l.LEC_CURRENT >= l.LEC_MANY THEN '정원 초과'
        WHEN l.LEC_MCODE = '0' AND l.LEC_MCODE_DEP = '0' THEN '0값 강의'
        WHEN l.LEC_MCODE = '01' AND l.LEC_MCODE_DEP = '05' THEN '전공 일치'
        ELSE '수강 불가'
    END AS eligibility
FROM LEC_TBL l
WHERE l.LEC_YEAR = 2025 AND l.LEC_SEMESTER = 1;
```

---

## 🎯 통합 테스트 시나리오

### 시나리오 A: 신입생 수강신청
1. 전공 정보 등록 전 → 교양(0값) 강의만 조회
2. 전공 등록 후 → 전공 필수 과목 추가 조회
3. 정원 초과 과목 → 대기 목록 표시

### 시나리오 B: 복수전공 학생
1. 주전공 강의 조회 → 전공 과목 표시
2. 부전공 강의 조회 → 부전공 과목도 표시
3. 중복 방지 → 이미 수강한 과목 제외

### 시나리오 C: 졸업예정자
1. 고학년 전공선택 → LEC_MIN 체크 (향후 구현)
2. 졸업요건 확인 → 필수/선택 이수 체크
3. 수강신청 우선순위 → 졸업예정자 우선

---

## 📈 성능 테스트

### 1. 응답 시간 측정

```javascript
const startTime = performance.now();

fetch('http://localhost:8080/api/lectures/eligible', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({studentId: 1})
})
.then(response => response.json())
.then(data => {
  const endTime = performance.now();
  console.log(`응답 시간: ${(endTime - startTime).toFixed(2)}ms`);
  console.log(`처리된 강의 수: ${data.totalCount}개`);
  console.log(`필터링된 강의: ${data.eligibleCount}개`);
});
```

**목표 성능**:
- 100개 강의: < 200ms
- 1000개 강의: < 500ms
- 5000개 강의: < 1000ms

### 2. 부하 테스트 (JMeter/Artillery)

```yaml
# artillery.yml
config:
  target: 'http://localhost:8080'
  phases:
    - duration: 60
      arrivalRate: 10
scenarios:
  - name: 'Get Eligible Lectures'
    flow:
      - post:
          url: '/api/lectures/eligible'
          json:
            studentId: 1
            page: 0
            size: 20
```

---

## ✅ 테스트 완료 체크리스트

- [ ] 테스트 데이터 삽입 완료
- [ ] 학생1 테스트: 주전공 필터링 확인
- [ ] 학생2 테스트: 부전공 매칭 확인
- [ ] 학생3 테스트: 전공 없는 학생 처리 확인
- [ ] 0값 강의 전체 공개 확인
- [ ] 정원 초과 필터링 확인
- [ ] 미개설 강의 필터링 확인
- [ ] 상세 사유 메시지 출력 확인
- [ ] studentInfo 응답 필드 확인
- [ ] 페이징 동작 확인
- [ ] 오류 처리 (studentId 누락) 확인
- [ ] 성능 테스트 (응답 시간) 확인
- [ ] 프론트엔드 연동 테스트 완료

---

**작성일**: 2025-10-16  
**버전**: v1.0.0  
**상태**: 테스트 준비 완료 ✅
