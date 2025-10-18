# API 엔드포인트 종합 문서

## 🎯 API 설계 개요

### 기본 구조
- **Base URL**: `https://bluecrab.chickenkiller.com/BlueCrab-1.0.0`
- **응답 형식**: 모든 API는 `ApiResponse<T>` 형식으로 통일
- **인증 방식**: JWT Bearer Token (일반 사용자), 2단계 인증 (관리자)
- **Content-Type**: `application/json`

### 공통 응답 형식
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { ... },
  "timestamp": "2025-01-01T12:00:00Z"
}
```

## 🔐 인증 관련 API

### 일반 사용자 인증

#### POST /api/auth/login
사용자 로그인 처리

**요청**
```json
{
  "username": "user@example.com",
  "password": "password123"
}
```

**응답**
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
      "userIdx": 1,
      "userEmail": "user@example.com",
      "userName": "홍길동",
      "userStudent": 0
    }
  },
  "timestamp": "2025-01-01T12:00:00Z"
}
```

#### POST /api/auth/logout
사용자 로그아웃 처리

**요청 헤더**
```
Authorization: Bearer <access-token>
```

**요청 바디**
```json
{
  "refreshToken": "<refresh-token>"
}
```

**응답**
```json
{
  "success": true,
  "message": "로그아웃이 성공적으로 처리되었습니다.",
  "data": {
    "status": "SUCCESS",
    "message": "Logged out successfully",
    "instruction": "Please remove the tokens from client storage",
    "logoutTime": "2025-01-01T12:00:00Z",
    "username": "user@example.com",
    "tokensInvalidated": {
      "accessToken": true,
      "refreshToken": true
    }
  }
}
```

#### POST /api/auth/refresh
JWT 토큰 갱신

**요청**
```json
{
  "refreshToken": "<refresh-token>"
}
```

**응답**
```json
{
  "success": true,
  "message": "토큰이 성공적으로 갱신되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": { ... }
  }
}
```

#### GET /api/auth/validate
JWT 토큰 유효성 검증

**요청 헤더**
```
Authorization: Bearer <access-token>
```

**응답**
```json
{
  "success": true,
  "message": "토큰이 유효합니다.",
  "data": {
    "valid": true,
    "message": "Token is valid"
  }
}
```

### 관리자 인증

#### POST /api/admin/login
관리자 1차 로그인 (ID/PW 검증)

**요청**
```json
{
  "adminId": "admin@example.com",
  "password": "admin_password"
}
```

**응답**
```json
{
  "success": true,
  "message": "1차 인증이 완료되었습니다",
  "data": {
    "message": "1차 인증이 완료되었습니다. 인증 코드 발급 버튼을 눌러 2차 인증을 진행해주세요.",
    "sessionToken": "session_token_string",
    "expiresIn": 600,
    "maskedEmail": "a***n@example.com",
    "authUrl": null
  }
}
```

#### GET /api/admin/verify-email
관리자 2차 인증 (이메일 인증)

**요청 파라미터**
```
?token=<email-verification-token>
```

**응답**
```json
{
  "success": true,
  "message": "이메일 인증이 완료되었습니다. JWT 토큰이 발급되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "adminInfo": {
      "adminId": "admin@example.com",
      "name": "관리자",
      "email": "admin@example.com"
    }
  }
}
```

## 👤 사용자 관리 API

### 사용자 조회

#### GET /api/users
전체 사용자 목록 조회

**응답**
```json
{
  "success": true,
  "message": "사용자 목록을 성공적으로 조회했습니다.",
  "data": [
    {
      "userIdx": 1,
      "userEmail": "student@example.com",
      "userName": "홍길동",
      "userCode": "202500101",
      "userPhone": "01012345678",
      "userBirth": "1990-01-01",
      "userStudent": 0
    }
  ]
}
```

#### GET /api/users/students
학생 사용자만 조회

#### GET /api/users/professors
교수 사용자만 조회

#### GET /api/users/{id}
특정 사용자 조회

**응답**
```json
{
  "success": true,
  "message": "사용자를 성공적으로 조회했습니다.",
  "data": {
    "userIdx": 1,
    "userEmail": "student@example.com",
    "userName": "홍길동",
    "userCode": "202500101",
    "userPhone": "01012345678",
    "userBirth": "1990-01-01",
    "userStudent": 0,
    "userZip": 12345,
    "userFirstAdd": "서울특별시 강남구",
    "userLastAdd": "테헤란로 123"
  }
}
```

### 사용자 생성/수정/삭제

#### POST /api/users
새 사용자 생성

**요청**
```json
{
  "userEmail": "newuser@example.com",
  "userPw": "password123",
  "userName": "김철수",
  "userCode": "202500102",
  "userPhone": "01087654321",
  "userBirth": "1995-05-15",
  "userStudent": 0
}
```

**응답**
```json
{
  "success": true,
  "message": "사용자가 성공적으로 생성되었습니다.",
  "data": {
    "userIdx": 2,
    "userEmail": "newuser@example.com",
    "userName": "김철수",
    ...
  }
}
```

#### PUT /api/users/{id}
사용자 정보 수정

**요청**
```json
{
  "userName": "김철수 (수정)",
  "userPhone": "01011111111"
}
```

#### DELETE /api/users/{id}
사용자 삭제

**응답**
```json
{
  "success": true,
  "message": "사용자가 성공적으로 삭제되었습니다.",
  "data": null
}
```

#### PATCH /api/users/{id}/toggle-role
사용자 역할 전환 (학생 ↔ 교수)

**응답**
```json
{
  "success": true,
  "message": "사용자 역할이 '교수'로 변경되었습니다.",
  "data": {
    "userIdx": 1,
    "userName": "홍길동",
    "userStudent": 1
  }
}
```

### 사용자 검색

#### GET /api/users/search
이름으로 사용자 검색

**요청 파라미터**
```
?name=홍길동
```

**응답**
```json
{
  "success": true,
  "message": "이름 '홍길동'로 검색된 사용자 2명을 찾았습니다.",
  "data": [
    {
      "userIdx": 1,
      "userName": "홍길동",
      ...
    }
  ]
}
```

#### GET /api/users/search-all
키워드로 사용자 검색 (이름 + 이메일)

**요청 파라미터**
```
?keyword=student
```

#### GET /api/users/search-birth
생년월일 범위로 사용자 검색

**요청 파라미터**
```
?startDate=1990-01-01&endDate=1999-12-31
```

**응답**
```json
{
  "success": true,
  "message": "생년월일 1990-01-01~1999-12-31 범위의 사용자 5명을 찾았습니다.",
  "data": [...]
}
```

### 사용자 통계

#### GET /api/users/stats
사용자 통계 정보 조회

**응답**
```json
{
  "success": true,
  "message": "사용자 통계 정보를 성공적으로 조회했습니다.",
  "data": {
    "totalUsers": 100,
    "studentUsers": 80,
    "professorUsers": 20,
    "studentPercentage": 80.0,
    "professorPercentage": 20.0
  }
}
```

## 📝 게시판 API

### GET /api/board
게시글 목록 조회

### POST /api/board
게시글 생성

### GET /api/board/{id}
게시글 상세 조회

### PUT /api/board/{id}
게시글 수정

### DELETE /api/board/{id}
게시글 삭제

## 🔧 시스템 관리 API

### GET /api/metrics
시스템 메트릭 조회

### GET /api/database/status
데이터베이스 상태 확인

### GET /api/logs
로그 모니터링

## ⚠️ 에러 응답 형식

### 4xx 클라이언트 오류
```json
{
  "success": false,
  "message": "요청 데이터가 올바르지 않습니다.",
  "data": {
    "errorCode": "VALIDATION_ERROR",
    "details": {
      "userEmail": "이메일 형식이 올바르지 않습니다."
    }
  },
  "timestamp": "2025-01-01T12:00:00Z"
}
```

### 5xx 서버 오류
```json
{
  "success": false,
  "message": "서버 내부 오류가 발생했습니다.",
  "data": {
    "errorCode": "INTERNAL_SERVER_ERROR",
    "details": "잠시 후 다시 시도해주세요."
  },
  "timestamp": "2025-01-01T12:00:00Z"
}
```

## 🚀 API 개선 권장사항

### 1. 표준화 작업
- RESTful 원칙 철저히 적용
- HTTP 상태 코드 정확한 사용
- 에러 코드 체계화

### 2. 보안 강화
- Rate Limiting 적용
- CORS 정책 세밀화
- API 버전 관리

### 3. 성능 최적화
- 페이징 적용 (사용자 목록 등)
- 캐싱 헤더 추가
- 압축 응답 지원

### 4. 문서화 개선
- OpenAPI 3.0 스펙 작성
- 예제 코드 제공
- 테스트 케이스 문서화