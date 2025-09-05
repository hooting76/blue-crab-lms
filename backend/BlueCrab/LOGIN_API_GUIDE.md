# BlueCrab 로그인 API 가이드

## 📋 개요
BlueCrab 시스템의 JWT 기반 로그인 API 사용 방법을 설명합니다.
프론트엔드 개발자가 로그인 기능을 구현할 때 참고하세요.

## 🔐 인증 시스템 구조
- **JWT 토큰 기반 인증**
- **액세스 토큰 + 리프레시 토큰** 방식
- **역할 기반 접근 제어 (RBAC)**

---

## 1. 로그인 API

### **엔드포인트**
```
POST /api/auth/login
```

### **요청 헤더**
```http
Content-Type: application/json
```

### **요청 본문**
```json
{
  "username": "사용자이메일@example.com",
  "password": "사용자비밀번호"
}
```

### **요청 예시**
```javascript
// JavaScript (Fetch API)
const loginRequest = {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    username: 'student@university.edu',
    password: 'myPassword123'
  })
};

fetch('https://your-domain.com/api/auth/login', loginRequest)
  .then(response => response.json())
  .then(data => {
    if (data.success) {
      // 로그인 성공 처리
      console.log('로그인 성공:', data);
    } else {
      // 로그인 실패 처리
      console.error('로그인 실패:', data.message);
    }
  });
```

### **성공 응답 (HTTP 200)**
```json
{
  "success": true,
  "message": "로그인에 성공했습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 123,
      "username": "student@university.edu",
      "name": "홍길동",
      "email": "student@university.edu"
    }
  },
  "timestamp": "2025-08-26T12:00:00Z"
}
```

### **실패 응답 (HTTP 400/401)**
```json
{
  "success": false,
  "message": "인증에 실패했습니다. 사용자명과 비밀번호를 확인해주세요.",
  "data": null,
  "timestamp": "2025-08-26T12:00:00Z"
}
```

---

## 2. 토큰 재발급 API

### **엔드포인트**
```
POST /api/auth/refresh
```

### **요청 헤더**
```http
Content-Type: application/json
Authorization: Bearer {refreshToken}
```

### **요청 본문**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### **요청 예시**
```javascript
// JavaScript (Fetch API)
const refreshRequest = {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('refreshToken')}`
  },
  body: JSON.stringify({
    refreshToken: localStorage.getItem('refreshToken')
  })
};

fetch('https://your-domain.com/api/auth/refresh', refreshRequest)
  .then(response => response.json())
  .then(data => {
    if (data.success) {
      // 토큰 갱신 성공
      localStorage.setItem('accessToken', data.data.accessToken);
      localStorage.setItem('refreshToken', data.data.refreshToken);
    }
  });
```

### **성공 응답 (HTTP 200)**
```json
{
  "success": true,
  "message": "토큰이 성공적으로 갱신되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 123,
      "username": "student@university.edu",
      "name": "홍길동",
      "email": "student@university.edu"
    }
  },
  "timestamp": "2025-08-26T12:00:00Z"
}
```

---

## 3. 인증이 필요한 API 호출

### **요청 헤더**
```http
Authorization: Bearer {accessToken}
Content-Type: application/json
```

### **요청 예시**
```javascript
// 인증이 필요한 API 호출
const authRequest = {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
    'Content-Type': 'application/json'
  }
};

fetch('https://your-domain.com/api/users/profile', authRequest)
  .then(response => {
    if (response.status === 401) {
      // 토큰 만료 - 리프레시 토큰으로 갱신 필요
      return refreshToken().then(() => {
        // 토큰 갱신 후 재요청
        return fetch('https://your-domain.com/api/users/profile', authRequest);
      });
    }
    return response.json();
  });
```

---

## 4. 권한 시스템

### **사용자 역할 종류**
- **학생**: userStudent = 1 (데이터베이스 USER_TBL.USER_STUDENT 필드)
- **교수**: userStudent = 0 (데이터베이스 USER_TBL.USER_STUDENT 필드)
- **관리자**: 특별한 이메일 패턴 또는 별도 권한 테이블로 관리

### **JWT 토큰에서 권한 정보 추출**
```javascript
// JWT 토큰 디코딩 (주의: 서명 검증은 서버에서만!)
function parseJWT(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('JWT 파싱 실패:', error);
    return null;
  }
}

// 권한 확인 예시
const token = localStorage.getItem('accessToken');
const payload = parseJWT(token);
const tokenType = payload?.type; // 'access' 또는 'refresh'
const username = payload?.sub; // 사용자 이메일
const userId = payload?.userId; // 사용자 ID
```

---

## 5. 프론트엔드 구현 권장사항

### **토큰 저장**
```javascript
// 로그인 성공 시 토큰 저장
function saveTokens(accessToken, refreshToken) {
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
  
  // 또는 보안이 중요한 경우 HttpOnly 쿠키 사용
  // document.cookie = `accessToken=${accessToken}; Secure; HttpOnly`;
}
```

### **자동 토큰 갱신**
```javascript
// Axios 인터셉터 예시
axios.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await axios.post('/api/auth/refresh', {
          refreshToken: refreshToken
        });
        
        const newToken = response.data.data.accessToken;
        localStorage.setItem('accessToken', newToken);
        
        // 원래 요청에 새 토큰 설정 후 재시도
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return axios(originalRequest);
        
      } catch (refreshError) {
        // 리프레시 실패 시 로그아웃 처리
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
      }
    }
    
    return Promise.reject(error);
  }
);
```

### **라우터 가드 예시 (React)**
```javascript
// PrivateRoute 컴포넌트
function PrivateRoute({ children, requiredRole }) {
  const token = localStorage.getItem('accessToken');
  
  if (!token) {
    return <Navigate to="/login" />;
  }
  
  const payload = parseJWT(token);
  const userRoles = payload?.roles || [];
  
  if (requiredRole && !userRoles.includes(requiredRole)) {
    return <div>접근 권한이 없습니다.</div>;
  }
  
  return children;
}

// 사용 예시 - userStudent 값으로 권한 확인
<PrivateRoute userType={1}> {/* 1: 학생, 0: 교수 */}
  <StudentDashboard />
</PrivateRoute>
```

---

## 6. 에러 처리

### **공통 에러 응답 형식**
```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null,
  "timestamp": "2024-08-27T10:30:00"
}
```

### **주요 에러 상황**
| 상태 코드 | 메시지 | 설명 |
|----------|--------|------|
| 400 | "Username is required" 또는 "Password is required" | 필수 필드 누락 (@Valid 검증 실패) |
| 401 | "Invalid username or password" | 잘못된 이메일/비밀번호 |
| 401 | "Invalid refresh token" | 리프레시 토큰 유효성 검증 실패 |
| 401 | "Token refresh failed: ..." | 토큰 갱신 중 오류 발생 |
| 500 | "Authentication failed: ..." | 인증 처리 중 서버 내부 오류 |

---

## 7. 보안 고려사항

### **클라이언트 측 보안**
- ✅ **HTTPS 사용**: 토큰 전송 시 반드시 HTTPS 사용
- ✅ **토큰 저장**: localStorage보다 HttpOnly 쿠키 권장
- ✅ **XSS 방지**: 토큰을 DOM에 노출하지 않기
- ✅ **자동 로그아웃**: 일정 시간 후 자동 로그아웃 구현

### **토큰 만료 시간**
- **액세스 토큰**: 15분 (900초)
- **리프레시 토큰**: 24시간 (86,400초)

### **브라우저 종료 시 처리**
```javascript
// 브라우저 종료 시 토큰 정리
window.addEventListener('beforeunload', () => {
  // 필요 시 토큰 정리
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
});
```

---

## 8. 테스트 계정

### **학생 계정**
```
이메일: student@university.edu
비밀번호: student123
역할: ROLE_USER, ROLE_STUDENT
```

### **교수 계정**
```
이메일: professor@university.edu
비밀번호: prof123
역할: ROLE_USER, ROLE_PROFESSOR
```

### **관리자 계정**
```
이메일: prof01@university.edu
비밀번호: admin123
역할: ROLE_USER, ROLE_PROFESSOR, ROLE_ADMIN
```

---

## 9. API 테스트 예시

### **cURL 테스트**
```bash
# 로그인 테스트
curl -X POST "https://your-domain.com/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student@university.edu",
    "password": "student123"
  }'

# 인증 API 테스트
curl -X GET "https://your-domain.com/api/test-auth" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 토큰 갱신 테스트
curl -X POST "https://your-domain.com/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_REFRESH_TOKEN" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

---

## 📞 문의사항
- **백엔드 개발팀**: BlueCrab Development Team
- **API 문서 버전**: 1.0.0
- **마지막 업데이트**: 2025-08-28

---

**⚠️ 주의사항**
- 운영 환경에서는 반드시 HTTPS를 사용하세요
- 토큰은 안전한 저장소에 보관하세요
- 디버깅 시에도 토큰 값을 로그에 출력하지 마세요
- 정기적으로 JWT 비밀 키를 교체하세요
