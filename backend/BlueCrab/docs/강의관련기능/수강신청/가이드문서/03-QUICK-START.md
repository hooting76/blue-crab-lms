# 🚀 수강신청 빠른 시작 가이드

5분 내 수강신청 기능 구현하기

---

## 📋 사전 준비

### 1. 필수 정보
- **학생 ID** (`studentIdx` 또는 `userIdx`): 로그인한 학생의 ID
- **JWT Token**: 인증 토큰 (`window.authToken` 또는 localStorage)

### 2. API 엔드포인트
```javascript
const API_BASE_URL = '/api';
const ENDPOINTS = {
  eligible: `${API_BASE_URL}/lectures/eligible`,
  enroll: `${API_BASE_URL}/enrollments/enroll`,
  list: `${API_BASE_URL}/enrollments/list`
};
```

---

## ⚡ 1분 퀵스타트

### HTML 구조

```html
<!-- 수강 가능 강의 목록 -->
<div id="lecture-list">
  <h2>수강 가능 강의</h2>
  <button id="load-lectures">강의 목록 불러오기</button>
  <div id="lectures"></div>
</div>

<!-- 내 수강목록 -->
<div id="my-courses">
  <h2>내 수강목록</h2>
  <div id="enrollments"></div>
</div>
```

### JavaScript (바로 사용 가능)

```javascript
// ========================================
// 설정
// ========================================
const API_BASE = '/api';
const studentIdx = 100; // 로그인한 학생 ID
const token = window.authToken; // JWT 토큰

// ========================================
// 1. 수강 가능 강의 불러오기
// ========================================
async function loadEligibleLectures() {
  const response = await fetch(`${API_BASE}/lectures/eligible`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      studentId: studentIdx,
      page: 0,
      size: 20
    })
  });

  const data = await response.json();
  displayLectures(data.eligibleLectures);
}

// ========================================
// 2. 강의 목록 표시
// ========================================
function displayLectures(lectures) {
  const container = document.getElementById('lectures');
  container.innerHTML = lectures.map(lecture => `
    <div class="lecture-card">
      <h3>${lecture.lecTit} (${lecture.lecSerial})</h3>
      <p>교수: ${lecture.lecProfName}</p>
      <p>학점: ${lecture.lecPoint} | 시간: ${lecture.lecTime}</p>
      <p>정원: ${lecture.lecCurrent}/${lecture.lecMany}</p>
      <button onclick="enrollLecture('${lecture.lecSerial}')">
        수강신청
      </button>
    </div>
  `).join('');
}

// ========================================
// 3. 수강신청
// ========================================
async function enrollLecture(lecSerial) {
  try {
    const response = await fetch(`${API_BASE}/enrollments/enroll`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        studentIdx: studentIdx,
        lecSerial: lecSerial
      })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }

    alert('수강신청 완료!');
    loadMyEnrollments(); // 목록 새로고침
  } catch (error) {
    alert(error.message);
  }
}

// ========================================
// 4. 내 수강목록 불러오기
// ========================================
async function loadMyEnrollments() {
  const response = await fetch(`${API_BASE}/enrollments/list`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      studentIdx: studentIdx,
      enrolled: true,
      page: 0,
      size: 20
    })
  });

  const data = await response.json();
  displayEnrollments(data.content);
}

// ========================================
// 5. 수강목록 표시
// ========================================
function displayEnrollments(enrollments) {
  const container = document.getElementById('enrollments');
  container.innerHTML = enrollments.map(course => `
    <div class="course-card">
      <h3>${course.lecTit} (${course.lecSerial})</h3>
      <p>교수: ${course.lecProfName}</p>
      <p>학점: ${course.lecPoint} | 시간: ${course.lecTime}</p>
    </div>
  `).join('');
}

// ========================================
// 이벤트 리스너
// ========================================
document.getElementById('load-lectures').addEventListener('click', loadEligibleLectures);

// 페이지 로드 시 목록 불러오기
window.addEventListener('DOMContentLoaded', () => {
  loadEligibleLectures();
  loadMyEnrollments();
});
```

### CSS (선택사항)

```css
.lecture-card, .course-card {
  border: 1px solid #ddd;
  padding: 15px;
  margin: 10px 0;
  border-radius: 8px;
}

.lecture-card button {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 5px;
  cursor: pointer;
}

.lecture-card button:hover {
  background-color: #0056b3;
}

.lecture-card button:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}
```

---

## 🎓 React 버전

### EnrollmentPage.jsx

```jsx
import React, { useState, useEffect } from 'react';

const EnrollmentPage = () => {
  const [eligibleLectures, setEligibleLectures] = useState([]);
  const [myEnrollments, setMyEnrollments] = useState([]);
  const [loading, setLoading] = useState(false);

  const studentIdx = 100; // 로그인한 학생 ID
  const token = window.authToken;

  // 수강 가능 강의 불러오기
  const loadEligibleLectures = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/lectures/eligible', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          studentId: studentIdx,
          page: 0,
          size: 20
        })
      });
      const data = await response.json();
      setEligibleLectures(data.eligibleLectures);
    } catch (error) {
      console.error('강의 목록 불러오기 실패:', error);
      alert('강의 목록을 불러올 수 없습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 수강신청
  const enrollLecture = async (lecSerial) => {
    try {
      const response = await fetch('/api/enrollments/enroll', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          studentIdx: studentIdx,
          lecSerial: lecSerial
        })
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message);
      }

      alert('수강신청 완료!');
      loadMyEnrollments();
    } catch (error) {
      alert(error.message);
    }
  };

  // 내 수강목록 불러오기
  const loadMyEnrollments = async () => {
    try {
      const response = await fetch('/api/enrollments/list', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          studentIdx: studentIdx,
          enrolled: true,
          page: 0,
          size: 20
        })
      });
      const data = await response.json();
      setMyEnrollments(data.content);
    } catch (error) {
      console.error('수강목록 불러오기 실패:', error);
    }
  };

  // 페이지 로드 시 목록 불러오기
  useEffect(() => {
    loadEligibleLectures();
    loadMyEnrollments();
  }, []);

  return (
    <div className="enrollment-page">
      {/* 수강 가능 강의 목록 */}
      <section>
        <h2>수강 가능 강의</h2>
        {loading ? (
          <p>로딩 중...</p>
        ) : (
          <div className="lecture-grid">
            {eligibleLectures.map((lecture) => (
              <div key={lecture.lecSerial} className="lecture-card">
                <h3>{lecture.lecTit} ({lecture.lecSerial})</h3>
                <p>교수: {lecture.lecProfName}</p>
                <p>학점: {lecture.lecPoint} | 시간: {lecture.lecTime}</p>
                <p>정원: {lecture.lecCurrent}/{lecture.lecMany}</p>
                <button onClick={() => enrollLecture(lecture.lecSerial)}>
                  수강신청
                </button>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* 내 수강목록 */}
      <section>
        <h2>내 수강목록</h2>
        <div className="course-grid">
          {myEnrollments.map((course) => (
            <div key={course.enrollmentIdx} className="course-card">
              <h3>{course.lecTit} ({course.lecSerial})</h3>
              <p>교수: {course.lecProfName}</p>
              <p>학점: {course.lecPoint} | 시간: {course.lecTime}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};

export default EnrollmentPage;
```

---

## 🔧 고급 기능

### 1. 중복 신청 방지

```javascript
// 수강신청 전 중복 확인
async function enrollLecture(lecSerial) {
  // 1. 중복 확인
  const isAlreadyEnrolled = await checkEnrollment(lecSerial);
  if (isAlreadyEnrolled) {
    alert('이미 신청한 강의입니다.');
    return;
  }

  // 2. 수강신청
  try {
    const response = await fetch('/api/enrollments/enroll', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        studentIdx: studentIdx,
        lecSerial: lecSerial
      })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }

    alert('수강신청 완료!');
    loadMyEnrollments();
  } catch (error) {
    alert(error.message);
  }
}

// 수강 여부 확인
async function checkEnrollment(lecSerial) {
  const response = await fetch('/api/enrollments/list', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      studentIdx: studentIdx,
      lecSerial: lecSerial,
      checkEnrollment: true
    })
  });

  if (!response.ok) return false;

  const data = await response.json();
  return data.enrolled;
}
```

### 2. 정원 체크

```javascript
function displayLectures(lectures) {
  const container = document.getElementById('lectures');
  container.innerHTML = lectures.map(lecture => {
    const isFull = lecture.lecCurrent >= lecture.lecMany;
    const buttonText = isFull ? '정원 마감' : '수강신청';
    const buttonDisabled = isFull ? 'disabled' : '';

    return `
      <div class="lecture-card">
        <h3>${lecture.lecTit} (${lecture.lecSerial})</h3>
        <p>교수: ${lecture.lecProfName}</p>
        <p>학점: ${lecture.lecPoint} | 시간: ${lecture.lecTime}</p>
        <p class="${isFull ? 'full' : ''}">
          정원: ${lecture.lecCurrent}/${lecture.lecMany}
        </p>
        <button 
          onclick="enrollLecture('${lecture.lecSerial}')"
          ${buttonDisabled}
        >
          ${buttonText}
        </button>
      </div>
    `;
  }).join('');
}
```

### 3. 페이지네이션

```javascript
let currentPage = 0;
const pageSize = 10;

async function loadEligibleLectures(page = 0) {
  currentPage = page;
  
  const response = await fetch('/api/lectures/eligible', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      studentId: studentIdx,
      page: page,
      size: pageSize
    })
  });

  const data = await response.json();
  displayLectures(data.eligibleLectures);
  displayPagination(data.pagination);
}

function displayPagination(pagination) {
  const container = document.getElementById('pagination');
  const buttons = [];

  for (let i = 0; i < pagination.totalPages; i++) {
    buttons.push(`
      <button 
        onclick="loadEligibleLectures(${i})"
        ${i === currentPage ? 'class="active"' : ''}
      >
        ${i + 1}
      </button>
    `);
  }

  container.innerHTML = buttons.join('');
}
```

---

## 🧪 테스트 방법

### 브라우저 콘솔 테스트

```javascript
// 1. 토큰 설정
window.authToken = 'your-jwt-token-here';

// 2. 수강 가능 강의 조회
await loadEligibleLectures();

// 3. 수강신청
await enrollLecture('CS101');

// 4. 내 수강목록 확인
await loadMyEnrollments();
```

### 전체 플로우 테스트

```javascript
// 테스트 시나리오
async function testEnrollmentFlow() {
  console.log('=== 수강신청 플로우 테스트 ===');

  // 1. 강의 목록 불러오기
  console.log('1. 강의 목록 불러오기...');
  await loadEligibleLectures();

  // 2. 첫 번째 강의 수강신청
  console.log('2. 수강신청...');
  const firstLecture = eligibleLectures[0];
  await enrollLecture(firstLecture.lecSerial);

  // 3. 수강목록 확인
  console.log('3. 수강목록 확인...');
  await loadMyEnrollments();

  console.log('=== 테스트 완료 ===');
}

// 실행
testEnrollmentFlow();
```

---

## ⚠️ 문제 해결

### 1. 인증 에러 (401 Unauthorized)
```javascript
// 토큰 확인
console.log('Token:', window.authToken);

// 토큰 만료 확인
const tokenPayload = JSON.parse(atob(window.authToken.split('.')[1]));
console.log('Token expires at:', new Date(tokenPayload.exp * 1000));
```

### 2. CORS 에러
```javascript
// 개발 환경에서 프록시 설정 (vite.config.js)
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
```

### 3. 응답 형식 에러
```javascript
// 응답 디버깅
const response = await fetch('/api/lectures/eligible', { ... });
console.log('Status:', response.status);
console.log('Headers:', response.headers);
const text = await response.text();
console.log('Raw response:', text);
```

---

## 📚 다음 단계

1. **상세 API 문서**: [01-API-SPECIFICATION.md](./01-API-SPECIFICATION.md)
2. **수강 자격 로직**: [02-ELIGIBILITY-LOGIC.md](./02-ELIGIBILITY-LOGIC.md)
3. **플로우 다이어그램**: [04-ENROLLMENT-FLOW.drawio](./04-ENROLLMENT-FLOW.drawio)

---

> **테스트 코드**: `브라우저콘솔테스트/02-student/lecture-test-2a-student-enrollment.js`
> **백엔드 문서**: `Phase2_Enrollment_Process.md`
