# 📡 BlueCrab API 레퍼런스

## 🔗 **Base URL**
```
Production: https://bluecrab.chickenkiller.com/BlueCrab-1.0.0
Development: http://localhost:8080
```

## 🔐 **인증**
대부분의 API는 JWT 토큰 인증이 필요합니다.

```http
Authorization: Bearer <your-jwt-token>
```

## 📋 **응답 형식**
모든 API는 일관된 응답 형식을 사용합니다:

```json
{
    "success": true,
    "message": "성공 메시지",
    "data": { /* 실제 데이터 */ },
    "timestamp": "2025-08-27T10:30:00Z"
}
```

## 🔑 **인증 API**

### **로그인**
```http
POST /api/auth/login
Content-Type: application/json

{
    "username": "user@example.com",
    "password": "password123"
}
```

**응답:**
```json
{
    "success": true,
    "message": "로그인 성공",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "expiresIn": 900,
        "userInfo": {
            "id": 1,
            "email": "user@example.com",
            "name": "사용자명",
            "username": "user@example.com"
        }
    }
}
```

### **토큰 갱신**
```http
POST /api/auth/refresh
Content-Type: application/json

{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### **로그아웃**
```http
POST /api/auth/logout
Authorization: Bearer <token>
```

### **토큰 검증**
```http
GET /api/auth/validate
Authorization: Bearer <token>
```

## 👥 **사용자 API**

### **전체 사용자 조회**
```http
GET /api/users
Authorization: Bearer <token>
```

### **학생 사용자 조회**
```http
GET /api/users/students
Authorization: Bearer <token>
```

### **교수 사용자 조회**
```http
GET /api/users/professors
Authorization: Bearer <token>
```

### **특정 사용자 조회**
```http
GET /api/users/{id}
Authorization: Bearer <token>
```

### **사용자 생성**
```http
POST /api/users
Authorization: Bearer <token>
Content-Type: application/json

{
    "userEmail": "newuser@example.com",
    "userPw": "password123",
    "userName": "새 사용자",
    "userPhone": "01012345678",
    "userBirth": "19950315",
    "userStudent": 0,
    "userZip": 12345,
    "userFirstAdd": "서울시 강남구",
    "userLastAdd": "역삼동 123-45"
}
```

### **사용자 수정**
```http
PUT /api/users/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
    "userEmail": "updated@example.com",
    "userName": "수정된 이름",
    "userPhone": "01087654321",
    "userBirth": "19950315",
    "userStudent": 0
}
```

### **사용자 삭제**
```http
DELETE /api/users/{id}
Authorization: Bearer <token>
```

### **사용자 역할 전환**
```http
PATCH /api/users/{id}/toggle-role
Authorization: Bearer <token>
```

### **사용자 검색**

#### **이름으로 검색**
```http
GET /api/users/search?name=홍길동
Authorization: Bearer <token>
```

#### **키워드 검색 (이름, 이메일)**
```http
GET /api/users/search-all?keyword=hong
Authorization: Bearer <token>
```

#### **생년월일 범위 검색**
```http
GET /api/users/search-birth?startDate=19900101&endDate=19991231
Authorization: Bearer <token>
```

### **사용자 통계**
```http
GET /api/users/stats
Authorization: Bearer <token>
```

**응답:**
```json
{
    "success": true,
    "message": "사용자 통계 정보를 성공적으로 조회했습니다.",
    "data": {
        "totalUsers": 100,
        "studentUsers": 70,
        "professorUsers": 30,
        "studentPercentage": 70.0,
        "professorPercentage": 30.0
    }
}
```

## 👤 **프로필 API**

> 📘 **상세 문서**: [PROFILE_BASIC_INFO_UPDATE.md](./PROFILE_BASIC_INFO_UPDATE.md)

### **프로필 조회**
```http
POST /api/profile/me
Authorization: Bearer <token>
```

### **프로필 완성도 체크**
```http
POST /api/profile/me/completeness
Authorization: Bearer <token>
```

### **주소 업데이트**
```http
POST /api/profile/address/update
Authorization: Bearer <token>
Content-Type: application/json

{
    "postalCode": "05852",
    "roadAddress": "서울 송파구 위례광장로 120",
    "detailAddress": "장지동, 위례중앙푸르지오1단지"
}
```

### **기본정보 업데이트** ⭐ NEW
```http
POST /api/profile/basic-info/update
Authorization: Bearer <token>
Content-Type: application/json

{
    "userName": "홍길동",
    "userPhone": "01012345678"
}
```

**응답:**
```json
{
    "success": true,
    "message": "기본 정보가 성공적으로 업데이트되었습니다.",
    "data": {
        "userName": "홍길동",
        "userPhone": "01012345678"
    },
    "timestamp": "2025-10-24T15:30:00Z"
}
```

### **프로필 이미지 업로드**
```http
POST /api/profile/me/upload-image
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: (이미지 파일)
```

> 📘 **상세 문서**: [PROFILE_IMAGE_UPLOAD.md](./PROFILE_IMAGE_UPLOAD.md)

### **프로필 이미지 URL 조회**
```http
POST /api/profile/me/image
Authorization: Bearer <token>
```

### **프로필 이미지 파일 조회**
```http
POST /api/profile/me/image/file
Authorization: Bearer <token>
Content-Type: application/json

{
    "imageKey": "profile_123.jpg"
}
```

## 📊 **상태 코드**

| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 중복 리소스 |
| 500 | 서버 오류 |

## 🚨 **에러 응답**

```json
{
    "success": false,
    "message": "에러 메시지",
    "data": null,
    "timestamp": "2025-08-27T10:30:00Z"
}
```

### **일반적인 에러 예시**

#### **인증 실패**
```json
{
    "success": false,
    "message": "유효하지 않은 토큰입니다.",
    "data": null
}
```

#### **중복 리소스**
```json
{
    "success": false,
    "message": "이미 존재하는 이메일입니다: user@example.com",
    "data": null
}
```

#### **리소스 없음**
```json
{
    "success": false,
    "message": "사용자를 찾을 수 없습니다: ID 999",
    "data": null
}
```

## 🔧 **테스트 도구**

### **cURL 예시**
```bash
# 로그인
curl -X POST https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test@bluecrab.com","password":"test123!"}'

# 사용자 조회
curl -X GET https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/users \
  -H "Authorization: Bearer <your-token>"
```

### **Postman Collection**
프로젝트 루트의 `postman/` 폴더에서 Postman Collection을 확인할 수 있습니다.

---

**작성일**: 2025-08-27  
**버전**: 1.0.0
