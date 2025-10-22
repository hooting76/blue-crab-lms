# 📋 수강신청 API 명세서

상세 API 명세 및 요청/응답 예시

---

## 🔧 API 명세

### 1. 수강 가능 강의 조회

**엔드포인트**: `POST /api/lectures/eligible`

**목적**: 학생이 수강할 수 있는 강의 목록을 조회합니다. 학부/학과 기반 필터링 적용.

**인증**: Bearer Token 필수

**Request Body**:
```json
{
  "studentId": 100,  // 학생 ID (필수, Integer)
  "page": 0,         // 페이지 번호 (선택, 기본: 0)
  "size": 20         // 페이지 크기 (선택, 기본: 20)
}
```

**Response (성공 - 200 OK)**:
```json
{
  "eligibleLectures": [
    {
      "lecSerial": "CS101",
      "lecTit": "자료구조",
      "lecProf": "22",
      "lecProfName": "김교수",
      "lecPoint": 3,
      "lecTime": "월1수1",
      "lecCurrent": 25,
      "lecMany": 30,
      "lecMcode": "01",
      "lecMcodeDep": "001",
      "lecMin": 0,
      "isEligible": true,
      "eligibilityReason": "수강 가능 (전공 일치: 01-001)"
    },
    {
      "lecSerial": "MATH101",
      "lecTit": "미적분학",
      "lecProf": "23",
      "lecProfName": "이교수",
      "lecPoint": 3,
      "lecTime": "화2목2",
      "lecCurrent": 30,
      "lecMany": 30,
      "lecMcode": "0",
      "lecMcodeDep": "0",
      "lecMin": 0,
      "isEligible": true,
      "eligibilityReason": "수강 가능 (전체 학생 허용: 0-0)"
    }
  ],
  "totalCount": 45,
  "eligibleCount": 30,
  "ineligibleCount": 15,
  "pagination": {
    "currentPage": 0,
    "pageSize": 20,
    "totalElements": 30,
    "totalPages": 2
  },
  "studentInfo": {
    "userIdx": 100,
    "userName": "홍길동",
    "majorFacultyCode": "01",
    "majorDeptCode": "001"
  }
}
```

**Response (에러 - 400 Bad Request)**:
```json
{
  "success": false,
  "message": "studentId는 필수입니다.",
  "timestamp": "2025-10-21T10:00:00Z"
}
```

**프론트엔드 호출 예시**:
```javascript
async function getEligibleLectures(studentId, page = 0, size = 20) {
  const response = await fetch('/api/lectures/eligible', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${window.authToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      studentId: studentId,
      page: page,
      size: size
    })
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || '강의 조회 실패');
  }

  return await response.json();
}

// 사용 예시
try {
  const data = await getEligibleLectures(100, 0, 10);
  console.log('수강 가능 강의:', data.eligibleLectures);
  console.log('총 개수:', data.totalCount);
  console.log('수강 가능:', data.eligibleCount);
} catch (error) {
  console.error('에러:', error.message);
  alert('강의 목록을 불러올 수 없습니다.');
}
```

---

### 2. 수강신청

**엔드포인트**: `POST /api/enrollments/enroll`

**목적**: 선택한 강의에 수강신청을 합니다.

**인증**: Bearer Token 필수

**Request Body**:
```json
{
  "studentIdx": 100,        // 학생 ID (필수, Integer)
  "lecSerial": "CS101"      // 강의 코드 (필수, String)
}
```

**Response (성공 - 200 OK)**:
```json
{
  "enrollmentIdx": 1,
  "lecIdx": 1,
  "studentIdx": 100,
  "enrollmentData": "{\"enrollment\":{\"status\":\"ENROLLED\",\"enrollmentDate\":\"2025-03-01T09:00:00\"}}"
}
```

**⚠️ 주의**: 실제 응답은 `EnrollmentExtendedTbl` 엔티티를 직접 반환하며, `success`/`message` 래퍼가 없습니다.

**Response (에러 - 400 Bad Request)**:
```json
// 정원 초과
{
  "success": false,
  "message": "강의 정원이 초과되었습니다.",
  "timestamp": "2025-10-21T10:00:00Z"
}

// 중복 신청
{
  "success": false,
  "message": "이미 수강신청한 강의입니다.",
  "timestamp": "2025-10-21T10:00:00Z"
}

// 자격 미달
{
  "success": false,
  "message": "수강 자격이 없습니다.",
  "timestamp": "2025-10-21T10:00:00Z"
}
```

**프론트엔드 호출 예시**:
```javascript
async function enrollLecture(studentIdx, lecSerial) {
  const response = await fetch('/api/enrollments/enroll', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${window.authToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      studentIdx: studentIdx,
      lecSerial: lecSerial
    })
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || '수강신청 실패');
  }

  return await response.json();
}

// 사용 예시
try {
  const enrollment = await enrollLecture(100, 'CS101');
  console.log('수강신청 완료:', enrollment);
  alert('수강신청이 완료되었습니다!');
  
  // 수강목록 새로고침
  await refreshMyCourses();
} catch (error) {
  console.error('에러:', error.message);
  alert(error.message);
}
```

---

### 3. 수강목록 조회

**엔드포인트**: `POST /api/enrollments/list`

**목적**: 학생의 수강중인 강의 목록을 조회합니다.

**인증**: Bearer Token 필수

**Request Body**:
```json
{
  "studentIdx": 100,  // 학생 ID (필수, Integer)
  "enrolled": true,   // 현재 수강중인 것만 (필수, Boolean)
  "page": 0,          // 페이지 번호 (선택, 기본: 0)
  "size": 20          // 페이지 크기 (선택, 기본: 20)
}
```

**Response (성공 - 200 OK)**:
```json
{
  "content": [
    {
      "enrollmentIdx": 123,
      "studentIdx": 100,
      "lecIdx": 1,
      "lecSerial": "CS101",
      "lecTit": "자료구조",
      "lecProfName": "김교수",
      "lecYear": 1,
      "lecSemester": 1,
      "lecPoint": 3,
      "lecTime": "월1,수1",
      "enrollmentDate": "2025-03-01T00:00:00Z",
      "status": "ACTIVE"
    },
    {
      "enrollmentIdx": 124,
      "studentIdx": 100,
      "lecIdx": 2,
      "lecSerial": "MATH101",
      "lecTit": "미적분학",
      "lecProfName": "이교수",
      "lecYear": 1,
      "lecSemester": 1,
      "lecPoint": 3,
      "lecTime": "화2,목2",
      "enrollmentDate": "2025-03-01T00:00:00Z",
      "status": "ACTIVE"
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**프론트엔드 호출 예시**:
```javascript
async function getMyEnrollments(studentIdx, page = 0, size = 20) {
  const response = await fetch('/api/enrollments/list', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${window.authToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      studentIdx: studentIdx,
      enrolled: true,
      page: page,
      size: size
    })
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || '수강목록 조회 실패');
  }

  return await response.json();
}

// 사용 예시
try {
  const data = await getMyEnrollments(100);
  console.log('내 수강목록:', data.content);
  console.log('총 개수:', data.totalElements);
  
  // 목록 표시
  data.content.forEach(course => {
    console.log(`${course.lecTit} (${course.lecSerial}) - ${course.lecProfName}`);
  });
} catch (error) {
  console.error('에러:', error.message);
  alert('수강목록을 불러올 수 없습니다.');
}
```

---

### 4. 수강신청 여부 확인

**엔드포인트**: `POST /api/enrollments/list`

**목적**: 특정 강의에 대한 수강신청 여부를 확인합니다.

**인증**: Bearer Token 필수

**Request Body**:
```json
{
  "studentIdx": 100,                    // 학생 ID (필수, Integer)
  "lecSerial": "CS101",      // 강의 코드 (필수, String)
  "checkEnrollment": true               // 수강 여부 확인 플래그 (필수, Boolean)
}
```

**Response (성공 - 200 OK)**:
```json
{
  "enrolled": true,
  "studentIdx": 100,
  "lecSerial": "CS101"
}
```

**프론트엔드 호출 예시**:
```javascript
async function checkEnrollment(studentIdx, lecSerial) {
  const response = await fetch('/api/enrollments/list', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${window.authToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      studentIdx: studentIdx,
      lecSerial: lecSerial,
      checkEnrollment: true
    })
  });

  if (!response.ok) {
    return { enrolled: false };
  }

  return await response.json();
}

// 사용 예시
async function updateEnrollButton(lectureElement, studentIdx, lecSerial) {
  const result = await checkEnrollment(studentIdx, lecSerial);
  const button = lectureElement.querySelector('.enroll-btn');
  
  if (result.enrolled) {
    button.textContent = '신청 완료';
    button.disabled = true;
    button.classList.add('enrolled');
  } else {
    button.textContent = '수강신청';
    button.disabled = false;
    button.classList.remove('enrolled');
  }
}
```

---

## 🔄 전체 통합 예시

```javascript
// 수강신청 전체 플로우
class EnrollmentManager {
  constructor(studentIdx) {
    this.studentIdx = studentIdx;
    this.token = window.authToken;
  }

  // 1. 수강 가능 강의 목록 불러오기
  async loadEligibleLectures() {
    const data = await this.getEligibleLectures();
    this.renderLectureList(data.eligibleLectures);
    return data;
  }

  // 2. 강의 선택 및 수강신청
  async enrollLecture(lecSerial) {
    // 2-1. 중복 확인
    const check = await this.checkEnrollment(lecSerial);
    if (check.enrolled) {
      alert('이미 신청한 강의입니다.');
      return;
    }

    // 2-2. 수강신청
    try {
      const result = await this.enroll(lecSerial);
      alert('수강신청 완료!');
      
      // 2-3. 목록 새로고침
      await this.loadMyEnrollments();
    } catch (error) {
      alert(error.message);
    }
  }

  // 3. 내 수강목록 불러오기
  async loadMyEnrollments() {
    const data = await this.getMyEnrollments();
    this.renderMyEnrollments(data.content);
    return data;
  }

  // API 호출 메서드들
  async getEligibleLectures(page = 0, size = 20) {
    const response = await fetch('/api/lectures/eligible', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        studentId: this.studentIdx,
        page, size
      })
    });
    return await response.json();
  }

  async enroll(lecSerial) {
    const response = await fetch('/api/enrollments/enroll', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        studentIdx: this.studentIdx,
        lecSerial: lecSerial
      })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }

    return await response.json();
  }

  async checkEnrollment(lecSerial) {
    const response = await fetch('/api/enrollments/list', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        studentIdx: this.studentIdx,
        lecSerial: lecSerial,
        checkEnrollment: true
      })
    });
    return await response.json();
  }

  async getMyEnrollments(page = 0, size = 20) {
    const response = await fetch('/api/enrollments/list', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        studentIdx: this.studentIdx,
        enrolled: true,
        page, size
      })
    });
    return await response.json();
  }

  // UI 렌더링 메서드들
  renderLectureList(lectures) {
    // 강의 목록 표시 로직
    console.log('수강 가능 강의:', lectures.length);
  }

  renderMyEnrollments(enrollments) {
    // 수강목록 표시 로직
    console.log('내 수강목록:', enrollments.length);
  }
}

// 사용 예시
const manager = new EnrollmentManager(100);
await manager.loadEligibleLectures();
await manager.enrollLecture('CS101');
await manager.loadMyEnrollments();
```

---

## 📝 주의사항

### 응답 형식 불일치
- `POST /api/lectures/eligible`: 커스텀 객체 반환
- `POST /api/enrollments/enroll`: 엔티티 직접 반환
- `POST /api/enrollments/list`: Page 객체 반환

### 에러 처리
- 모든 API에서 `response.ok` 체크 필수
- 에러 응답은 `{ success: false, message: "..." }` 형식

### 필수 필드
- `studentId` 또는 `studentIdx`: 일관성 없음 (API별 상이)
- `lecSerial`: 강의 코드 (문자열)
- `enrolled`: Boolean 값 (수강목록 조회 시)

---

> **테스트**: `브라우저콘솔테스트/02-student/` 디렉토리 참조
> **상세 로직**: [02-ELIGIBILITY-LOGIC.md](./02-ELIGIBILITY-LOGIC.md) 참조
