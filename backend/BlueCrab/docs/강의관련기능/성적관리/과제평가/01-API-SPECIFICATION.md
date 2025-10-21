# 📋 과제 평가 API 명세서

상세 API 명세 및 요청/응답 예시

---

## 🔧 API 명세

### 1. 과제 평가 입력 (교수)

**엔드포인트**: `POST /api/assignments/grade`

**목적**: 교수가 오프라인으로 제출받은 과제의 평가 결과를 시스템에 입력합니다.

**인증**: Bearer Token 필수

**Request Body**:
```json
{
  "assignmentIdx": 1,              // 과제 ID (필수, Integer)
  "studentCode": "STU001",         // 학생 코드 (필수, String)
  "submissionMethod": "email",     // 제출 방법 (필수, String)
  "score": 9,                      // 점수 (필수, 0~10)
  "action": "grade"                // 액션 타입 (필수)
}
```

**제출 방법 (submissionMethod)**:
- `email`: 이메일 제출
- `print`: 서면(출력물) 제출
- `hands`: 손수 전달
- `absent`: 미제출

**Response (성공 - 200 OK)**:
```json
{
  "success": true,
  "message": "과제 채점이 완료되었습니다.",
  "data": {
    "assignmentIdx": 1,
    "studentCode": "STU001",
    "studentName": "홍길동",
    "submissionMethod": "email",
    "score": 9,
    "maxScore": 10,
    "gradedDate": "2025-10-21T14:30:00Z",
    "gradedBy": 22
  }
}
```

**Response (에러 - 400 Bad Request)**:
```json
{
  "success": false,
  "message": "점수는 0~10 사이여야 합니다.",
  "timestamp": "2025-10-21T10:00:00Z"
}
```

**프론트엔드 호출 예시**:
```javascript
async function gradeAssignment(assignmentIdx, studentCode, method, score) {
  const response = await fetch('/api/assignments/grade', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${window.authToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      assignmentIdx: assignmentIdx,
      studentCode: studentCode,
      submissionMethod: method,
      score: score,
      action: 'grade'
    })
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || '과제 채점 실패');
  }

  return await response.json();
}

// 사용 예시
try {
  const result = await gradeAssignment(1, 'STU001', 'email', 9);
  console.log('채점 완료:', result.data);
  alert(`${result.data.studentName} 학생 과제 채점 완료! (${result.data.score}/${result.data.maxScore}점)`);
} catch (error) {
  console.error('에러:', error.message);
  alert(error.message);
}
```

---

### 2. 과제 제출 현황 조회 (교수)

**엔드포인트**: `POST /api/assignments/submissions`

**목적**: 특정 과제의 제출/채점 현황을 조회합니다.

**인증**: Bearer Token 필수

**Request Body**:
```json
{
  "assignmentIdx": 1,     // 과제 ID (필수, Integer)
  "action": "list",       // 액션 타입 (필수)
  "page": 0,              // 페이지 번호 (선택, 기본: 0)
  "size": 20              // 페이지 크기 (선택, 기본: 20)
}
```

**Response (성공 - 200 OK)**:
```json
{
  "content": [
    {
      "studentCode": "STU001",
      "studentName": "홍길동",
      "studentId": "2021001",
      "submitted": true,
      "graded": true,
      "score": 9,
      "maxScore": 10,
      "submissionMethod": "email",
      "gradedDate": "2025-10-21T14:30:00Z"
    },
    {
      "studentCode": "STU002",
      "studentName": "김철수",
      "studentId": "2021002",
      "submitted": false,
      "graded": false,
      "score": 0,
      "maxScore": 10,
      "submissionMethod": null,
      "gradedDate": null
    }
  ],
  "totalElements": 25,
  "totalPages": 2,
  "size": 20,
  "number": 0
}
```

**프론트엔드 호출 예시**:
```javascript
async function getAssignmentSubmissions(assignmentIdx, page = 0, size = 20) {
  const response = await fetch('/api/assignments/submissions', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${window.authToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      assignmentIdx: assignmentIdx,
      action: 'list',
      page: page,
      size: size
    })
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || '제출 현황 조회 실패');
  }

  return await response.json();
}

// 사용 예시
try {
  const data = await getAssignmentSubmissions(1);
  console.log(`총 ${data.totalElements}명 수강생`);
  
  data.content.forEach((student, idx) => {
    console.log(`${idx + 1}. ${student.studentName} (${student.studentId})`);
    console.log(`   제출: ${student.submitted ? '✅' : '❌'}`);
    console.log(`   채점: ${student.graded ? '✅' : '⏳'}`);
    if (student.graded) {
      console.log(`   점수: ${student.score}/${student.maxScore}점`);
      console.log(`   방법: ${student.submissionMethod}`);
    }
  });
} catch (error) {
  console.error('에러:', error.message);
}
```

---

### 3. 과제 평가 수정 (교수)

**엔드포인트**: `PUT /api/assignments/grade/{assignmentIdx}/student/{studentCode}`

**목적**: 이미 입력된 과제 평가를 수정합니다.

**인증**: Bearer Token 필수

**Path Parameters**:
- `assignmentIdx`: 과제 ID
- `studentCode`: 학생 코드

**Request Body**:
```json
{
  "submissionMethod": "print",     // 제출 방법 (선택)
  "score": 8                       // 점수 (선택, 0~10)
}
```

**Response (성공 - 200 OK)**:
```json
{
  "success": true,
  "message": "과제 평가가 수정되었습니다.",
  "data": {
    "assignmentIdx": 1,
    "studentCode": "STU001",
    "studentName": "홍길동",
    "submissionMethod": "print",
    "score": 8,
    "maxScore": 10,
    "gradedDate": "2025-10-21T14:30:00Z",
    "modifiedDate": "2025-10-21T15:00:00Z"
  }
}
```

**프론트엔드 호출 예시**:
```javascript
async function updateAssignmentGrade(assignmentIdx, studentCode, updates) {
  const response = await fetch(
    `/api/assignments/grade/${assignmentIdx}/student/${studentCode}`,
    {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${window.authToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(updates)
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || '평가 수정 실패');
  }

  return await response.json();
}

// 사용 예시
try {
  const result = await updateAssignmentGrade(1, 'STU001', {
    submissionMethod: 'print',
    score: 8
  });
  console.log('평가 수정 완료:', result.data);
  alert('평가가 수정되었습니다.');
} catch (error) {
  console.error('에러:', error.message);
  alert(error.message);
}
```

---

## 🔄 전체 통합 예시

```javascript
// 과제 평가 관리 클래스
class AssignmentGradeManager {
  constructor(token) {
    this.token = token;
    this.apiBase = '/api/assignments';
  }

  // 1. 제출 현황 조회
  async getSubmissions(assignmentIdx, page = 0, size = 20) {
    const response = await fetch(`${this.apiBase}/submissions`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        assignmentIdx: assignmentIdx,
        action: 'list',
        page: page,
        size: size
      })
    });

    if (!response.ok) {
      throw new Error('제출 현황 조회 실패');
    }

    return await response.json();
  }

  // 2. 과제 채점
  async grade(assignmentIdx, studentCode, method, score) {
    const response = await fetch(`${this.apiBase}/grade`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        assignmentIdx: assignmentIdx,
        studentCode: studentCode,
        submissionMethod: method,
        score: score,
        action: 'grade'
      })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || '채점 실패');
    }

    return await response.json();
  }

  // 3. 평가 수정
  async updateGrade(assignmentIdx, studentCode, updates) {
    const response = await fetch(
      `${this.apiBase}/grade/${assignmentIdx}/student/${studentCode}`,
      {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${this.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(updates)
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || '평가 수정 실패');
    }

    return await response.json();
  }

  // 4. 일괄 채점
  async gradeMultiple(assignmentIdx, gradings) {
    const results = [];
    
    for (const grading of gradings) {
      try {
        const result = await this.grade(
          assignmentIdx,
          grading.studentCode,
          grading.method,
          grading.score
        );
        results.push({
          success: true,
          studentCode: grading.studentCode,
          data: result.data
        });
      } catch (error) {
        results.push({
          success: false,
          studentCode: grading.studentCode,
          error: error.message
        });
      }
    }

    return results;
  }

  // 5. 제출 통계
  async getStatistics(assignmentIdx) {
    const data = await this.getSubmissions(assignmentIdx, 0, 1000);
    
    const total = data.totalElements;
    const submitted = data.content.filter(s => s.submitted).length;
    const graded = data.content.filter(s => s.graded).length;
    const avgScore = data.content
      .filter(s => s.graded)
      .reduce((sum, s) => sum + s.score, 0) / graded;

    return {
      total: total,
      submitted: submitted,
      submissionRate: (submitted / total * 100).toFixed(1),
      graded: graded,
      gradingRate: (graded / total * 100).toFixed(1),
      avgScore: avgScore.toFixed(2),
      methodBreakdown: this.getMethodBreakdown(data.content)
    };
  }

  // 제출 방법별 통계
  getMethodBreakdown(submissions) {
    const methods = {};
    submissions
      .filter(s => s.submitted && s.submissionMethod)
      .forEach(s => {
        methods[s.submissionMethod] = (methods[s.submissionMethod] || 0) + 1;
      });
    return methods;
  }
}

// 사용 예시
const manager = new AssignmentGradeManager(window.authToken);

// 제출 현황 조회
const submissions = await manager.getSubmissions(1);
console.log('제출 현황:', submissions);

// 과제 채점
const result = await manager.grade(1, 'STU001', 'email', 9);
console.log('채점 완료:', result);

// 일괄 채점
const gradings = [
  { studentCode: 'STU001', method: 'email', score: 9 },
  { studentCode: 'STU002', method: 'print', score: 8 },
  { studentCode: 'STU003', method: 'hands', score: 10 }
];
const results = await manager.gradeMultiple(1, gradings);
console.log('일괄 채점 결과:', results);

// 통계 조회
const stats = await manager.getStatistics(1);
console.log('과제 통계:', stats);
```

---

## 📝 주의사항

### 1. 제출 방법 코드

올바른 코드만 사용하세요:
```javascript
✅ 허용: 'email', 'print', 'hands', 'absent'
❌ 금지: 'online', 'upload', '이메일', '서면'
```

### 2. 점수 범위

```javascript
✅ 올바른 점수: 0, 1, 2, ..., 9, 10
❌ 잘못된 점수: -1, 11, 3.5, "10"
```

### 3. 자동 성적 반영

과제 채점 후 **자동으로** 전체 성적이 재계산됩니다:
```
과제 채점 → 이벤트 발생 → 성적 재계산 → EnrollmentExtendedTbl 업데이트
```

별도로 성적 업데이트 API를 호출할 필요 없습니다.

---

> **테스트**: `브라우저콘솔테스트/03-professor/lecture-test-4b-professor-assignment-grade.js` 참조  
> **제출 방법**: [02-SUBMISSION-METHODS.md](./02-SUBMISSION-METHODS.md) 참조
