# 🎯 수강 자격 로직 상세 설명

"0-value rule"과 학부/학과 매칭 알고리즘

---

## 📌 개요

수강신청 시스템의 핵심은 **"0-value rule"**입니다.
- `lecMcode = "0"` AND `lecMcodeDep = "0"` → **모든 학생 수강 가능**
- 그 외 → **학부/학과 코드 매칭 필터링**

---

## 🔢 0-value Rule

### 기본 원리

```java
// Lecture 테이블
lecMcode     = "0"     // 학부 코드
lecMcodeDep  = "0"     // 학과 코드

// 위 조건이면 → 전체 학생 허용
```

### 예시

| lecSerial | lecTit     | lecMcode | lecMcodeDep | 의미                    |
|-----------|------------|----------|-------------|-------------------------|
| GEN101    | 글쓰기     | 0        | 0           | 전체 학생 (교양)         |
| CS101     | 자료구조   | 01       | 001         | 공과대학 컴퓨터공학과    |
| MATH101   | 미적분학   | 01       | 000         | 공과대학 전체           |

---

## 🏫 학부/학과 매칭 알고리즘

### 1. SERIAL_CODE_TABLE 구조

```sql
CREATE TABLE SERIAL_CODE_TABLE (
  CODE VARCHAR(10) PRIMARY KEY,        -- 코드 (예: "01", "001")
  DIVISION VARCHAR(20),                -- 구분 (major_dept, major_fac)
  NAME VARCHAR(100)                    -- 이름 (예: "컴퓨터공학과")
);
```

**데이터 예시**:
```
CODE | DIVISION    | NAME
-----|-------------|----------------
01   | major_fac   | 공과대학
001  | major_dept  | 컴퓨터공학과
002  | major_dept  | 전기공학과
02   | major_fac   | 자연과학대학
003  | major_dept  | 물리학과
```

### 2. User 테이블 매칭 필드

```java
// User 테이블
userMajorFacultyCode = "01"   // 학부 코드
userMajorDeptCode    = "001"  // 학과 코드
```

### 3. 매칭 로직 (LectureService.java)

```java
public boolean isEligible(Lecture lecture, User student) {
    String lecMcode = lecture.getLecMcode();
    String lecMcodeDep = lecture.getLecMcodeDep();
    
    // Rule 1: 0-value rule
    if ("0".equals(lecMcode) && "0".equals(lecMcodeDep)) {
        return true; // 전체 학생 허용
    }
    
    String studentFac = student.getUserMajorFacultyCode();
    String studentDep = student.getUserMajorDeptCode();
    
    // Rule 2: 학부만 지정 (학과는 0)
    if ("0".equals(lecMcodeDep)) {
        return lecMcode.equals(studentFac);
    }
    
    // Rule 3: 학부 + 학과 모두 지정
    return lecMcode.equals(studentFac) 
        && lecMcodeDep.equals(studentDep);
}
```

### 4. 매칭 시나리오

#### 시나리오 1: 전체 학생 허용
```
강의: lecMcode = "0", lecMcodeDep = "0"
학생: 누구든지
결과: ✅ 수강 가능
```

#### 시나리오 2: 학부 제한 (공과대학 전체)
```
강의: lecMcode = "01", lecMcodeDep = "0"
학생1: userMajorFacultyCode = "01", userMajorDeptCode = "001" (컴퓨터공학과)
학생2: userMajorFacultyCode = "01", userMajorDeptCode = "002" (전기공학과)
학생3: userMajorFacultyCode = "02", userMajorDeptCode = "003" (물리학과)

결과:
- 학생1: ✅ (공과대학)
- 학생2: ✅ (공과대학)
- 학생3: ❌ (자연과학대학)
```

#### 시나리오 3: 학과 제한 (컴퓨터공학과만)
```
강의: lecMcode = "01", lecMcodeDep = "001"
학생1: userMajorFacultyCode = "01", userMajorDeptCode = "001" (컴퓨터공학과)
학생2: userMajorFacultyCode = "01", userMajorDeptCode = "002" (전기공학과)

결과:
- 학생1: ✅ (학부 + 학과 일치)
- 학생2: ❌ (학부는 일치하지만 학과 불일치)
```

---

## 🧪 코드 예시

### 백엔드 (LectureService.java)

```java
@Service
public class LectureService {
    
    public List<LectureDTO> getEligibleLectures(Long studentId, int page, int size) {
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("학생을 찾을 수 없습니다."));
        
        String studentFac = student.getUserMajorFacultyCode();
        String studentDep = student.getUserMajorDeptCode();
        
        // 1. 모든 강의 가져오기
        Page<Lecture> allLectures = lectureRepository.findAll(
            PageRequest.of(page, size)
        );
        
        // 2. 필터링
        List<LectureDTO> eligibleLectures = allLectures.stream()
            .filter(lecture -> {
                String lecMcode = lecture.getLecMcode();
                String lecMcodeDep = lecture.getLecMcodeDep();
                
                // 0-value rule
                if ("0".equals(lecMcode) && "0".equals(lecMcodeDep)) {
                    return true;
                }
                
                // 학부만 체크
                if ("0".equals(lecMcodeDep)) {
                    return lecMcode.equals(studentFac);
                }
                
                // 학부 + 학과 체크
                return lecMcode.equals(studentFac) 
                    && lecMcodeDep.equals(studentDep);
            })
            .map(lecture -> convertToDTO(lecture))
            .collect(Collectors.toList());
        
        return eligibleLectures;
    }
    
    private LectureDTO convertToDTO(Lecture lecture) {
        LectureDTO dto = new LectureDTO();
        dto.setLecSerial(lecture.getLecSerial());
        dto.setLecTit(lecture.getLecTit());
        dto.setLecMcode(lecture.getLecMcode());
        dto.setLecMcodeDep(lecture.getLecMcodeDep());
        // ... 기타 필드
        return dto;
    }
}
```

### 프론트엔드 (React 예시)

```javascript
// 수강 가능 여부 판단 (클라이언트 사이드)
function isEligibleForLecture(lecture, student) {
  const { lecMcode, lecMcodeDep } = lecture;
  const { userMajorFacultyCode, userMajorDeptCode } = student;
  
  // Rule 1: 0-value rule
  if (lecMcode === '0' && lecMcodeDep === '0') {
    return {
      eligible: true,
      reason: '전체 학생 허용 (교양 과목)'
    };
  }
  
  // Rule 2: 학부만 체크
  if (lecMcodeDep === '0') {
    if (lecMcode === userMajorFacultyCode) {
      return {
        eligible: true,
        reason: `학부 일치 (${lecMcode})`
      };
    } else {
      return {
        eligible: false,
        reason: `학부 불일치 (요구: ${lecMcode}, 본인: ${userMajorFacultyCode})`
      };
    }
  }
  
  // Rule 3: 학부 + 학과 체크
  if (lecMcode === userMajorFacultyCode && lecMcodeDep === userMajorDeptCode) {
    return {
      eligible: true,
      reason: `학부/학과 일치 (${lecMcode}-${lecMcodeDep})`
    };
  } else {
    return {
      eligible: false,
      reason: `학부/학과 불일치 (요구: ${lecMcode}-${lecMcodeDep}, 본인: ${userMajorFacultyCode}-${userMajorDeptCode})`
    };
  }
}

// 사용 예시
const lecture = {
  lecSerial: 'CS101',
  lecTit: '자료구조',
  lecMcode: '01',
  lecMcodeDep: '001'
};

const student = {
  userIdx: 100,
  userName: '홍길동',
  userMajorFacultyCode: '01',
  userMajorDeptCode: '001'
};

const result = isEligibleForLecture(lecture, student);
console.log(result);
// { eligible: true, reason: '학부/학과 일치 (01-001)' }
```

---

## 🔍 복수전공/부전공 처리

### 현재 시스템 (단일 전공)

```java
// User 테이블
userMajorFacultyCode = "01"   // 주전공 학부
userMajorDeptCode    = "001"  // 주전공 학과
```

### 향후 확장 (복수전공 지원)

```java
// User 테이블
userMajorFacultyCode = "01"    // 주전공 학부
userMajorDeptCode    = "001"   // 주전공 학과

userSecondMajorFacultyCode = "02"   // 복수전공 학부
userSecondMajorDeptCode    = "003"  // 복수전공 학과

// 수강 자격 판단 시 OR 조건
boolean isPrimaryMatch = checkMajorMatch(lecture, user.getPrimaryMajor());
boolean isSecondMatch = checkMajorMatch(lecture, user.getSecondMajor());

return isPrimaryMatch || isSecondMatch;
```

---

## 📊 SERIAL_CODE_TABLE 조회

### API: `POST /api/settings/query`

**Request**:
```json
{
  "table": "SERIAL_CODE_TABLE",
  "condition": "division='major_fac' OR division='major_dept'"
}
```

**Response**:
```json
{
  "success": true,
  "data": [
    { "code": "01", "division": "major_fac", "name": "공과대학" },
    { "code": "001", "division": "major_dept", "name": "컴퓨터공학과" },
    { "code": "002", "division": "major_dept", "name": "전기공학과" },
    { "code": "02", "division": "major_fac", "name": "자연과학대학" },
    { "code": "003", "division": "major_dept", "name": "물리학과" }
  ]
}
```

### 프론트엔드 활용

```javascript
// 학부/학과 코드 → 이름 변환
async function getMajorName(code, division) {
  const response = await fetch('/api/settings/query', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      table: 'SERIAL_CODE_TABLE',
      condition: `code='${code}' AND division='${division}'`
    })
  });
  
  const data = await response.json();
  return data.data[0]?.name || '알 수 없음';
}

// 사용 예시
const facultyName = await getMajorName('01', 'major_fac');
console.log(facultyName); // "공과대학"

const deptName = await getMajorName('001', 'major_dept');
console.log(deptName); // "컴퓨터공학과"
```

---

## ⚠️ 주의사항

### 1. 코드 형식
- 코드는 **문자열**로 처리 (`"0"`, `"01"`, `"001"`)
- 숫자 비교가 아닌 **문자열 일치** 비교

### 2. 0-value 판단
```javascript
// ✅ 올바른 방법
if (lecMcode === '0' && lecMcodeDep === '0') { ... }

// ❌ 잘못된 방법
if (lecMcode == 0 && lecMcodeDep == 0) { ... }  // 숫자 비교
if (!lecMcode && !lecMcodeDep) { ... }          // null/undefined 체크
```

### 3. 빈 문자열 처리
- `""` (빈 문자열)과 `"0"`은 **다름**
- null 값은 허용되지 않음

---

## 🧪 테스트 케이스

### 테스트 1: 전체 학생 허용
```javascript
const lecture = { lecMcode: '0', lecMcodeDep: '0' };
const student = { userMajorFacultyCode: '01', userMajorDeptCode: '001' };

const result = isEligible(lecture, student);
console.assert(result === true, '전체 학생 허용 실패');
```

### 테스트 2: 학부 제한
```javascript
const lecture = { lecMcode: '01', lecMcodeDep: '0' };
const student1 = { userMajorFacultyCode: '01', userMajorDeptCode: '001' };
const student2 = { userMajorFacultyCode: '02', userMajorDeptCode: '003' };

console.assert(isEligible(lecture, student1) === true, '공과대학 학생 허용 실패');
console.assert(isEligible(lecture, student2) === false, '타 학부 학생 차단 실패');
```

### 테스트 3: 학과 제한
```javascript
const lecture = { lecMcode: '01', lecMcodeDep: '001' };
const student1 = { userMajorFacultyCode: '01', userMajorDeptCode: '001' };
const student2 = { userMajorFacultyCode: '01', userMajorDeptCode: '002' };

console.assert(isEligible(lecture, student1) === true, '컴퓨터공학과 학생 허용 실패');
console.assert(isEligible(lecture, student2) === false, '타 학과 학생 차단 실패');
```

---

> **참조**: Phase2_Enrollment_Process.md, LectureService.java
> **테스트**: `브라우저콘솔테스트/02-student/lecture-test-2a-student-enrollment.js`
