# 👤 사용자 관리 API

## 📋 개요

BlueCrab 프로젝트의 사용자 관리를 위한 REST API 문서입니다. 사용자 CRUD 작업, 검색, 통계 조회 등의 기능을 제공합니다.

> ⚠️ **참고**: 현재 일부 기능은 개발 중이며, 요구사항에 따라 변경될 수 있습니다.

## 🔐 인증 요구사항

모든 사용자 API는 JWT 인증이 필요합니다.

```http
Authorization: Bearer <JWT_TOKEN>
```

## 📊 공통 응답 형식

모든 API 응답은 다음 형식을 따릅니다:

```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    // 실제 응답 데이터
  },
  "timestamp": "2025-08-27T12:00:00Z"
}
```

## 👥 사용자 조회 API

### 1. 전체 사용자 조회

모든 사용자를 조회합니다.

```http
GET /api/users
```

#### 응답 예시
```json
{
  "success": true,
  "message": "사용자 목록을 성공적으로 조회했습니다.",
  "data": [
    {
      "userIdx": 1,
      "userEmail": "student@bluecrab.com",
      "userName": "홍길동",
      "userPhone": "01012345678",
      "userBirth": "19900101",
      "userStudent": 0,
      "userLatest": "컴퓨터공학과 4학년",
      "userZip": 12345,
      "userFirstAdd": "서울시 강남구",
      "userLastAdd": "테헤란로 123",
      "userReg": "2025-01-01 00:00:00",
      "userRegIp": "192.168.1.100"
    }
  ],
  "timestamp": "2025-08-27T12:00:00Z"
}
```

### 2. 학생 사용자 조회

학생 사용자만 조회합니다.

```http
GET /api/users/students
```

#### 필터링 조건
- `userStudent = 0` (학생)

### 3. 교수 사용자 조회

교수 사용자만 조회합니다.

```http
GET /api/users/professors
```

#### 필터링 조건
- `userStudent = 1` (교수)

### 4. 특정 사용자 조회

사용자 ID로 개별 사용자를 조회합니다.

```http
GET /api/users/{id}
```

#### Path Parameters
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| id | Integer | 사용자 고유 식별자 |

#### 응답
- **200 OK**: 사용자 정보 반환
- **404 NOT FOUND**: 사용자를 찾을 수 없음

## ✏️ 사용자 생성/수정/삭제 API

### 1. 사용자 생성

새로운 사용자를 생성합니다.

```http
POST /api/users
```

#### Request Body
```json
{
  "userEmail": "newuser@bluecrab.com",
  "userPw": "hashedPassword123",
  "userName": "김철수",
  "userPhone": "01098765432",
  "userBirth": "19851215",
  "userStudent": 1,
  "userLatest": "경영학과 3학년",
  "userZip": 54321,
  "userFirstAdd": "부산시 해운대구",
  "userLastAdd": "마린시티 456"
}
```

#### 필수 필드
- `userEmail`: 이메일 (로그인 ID)
- `userPw`: 암호화된 비밀번호
- `userName`: 사용자 실명
- `userPhone`: 휴대폰번호 (11자리)
- `userBirth`: 생년월일 (YYYYMMDD)
- `userStudent`: 사용자 유형 (0: 학생, 1: 교수)

#### 응답
- **201 CREATED**: 사용자 생성 성공
- **400 BAD REQUEST**: 필수 필드 누락 또는 형식 오류

### 2. 사용자 정보 수정

기존 사용자의 정보를 수정합니다.

```http
PUT /api/users/{id}
```

#### Path Parameters
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| id | Integer | 수정할 사용자 ID |

#### Request Body
```json
{
  "userEmail": "updated@bluecrab.com",
  "userName": "김철수(수정됨)",
  "userPhone": "01011111111",
  "userLatest": "경영학과 4학년"
}
```

#### 응답
- **200 OK**: 수정 성공
- **404 NOT FOUND**: 사용자를 찾을 수 없음

### 3. 사용자 삭제

사용자를 삭제합니다.

```http
DELETE /api/users/{id}
```

#### Path Parameters
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| id | Integer | 삭제할 사용자 ID |

#### 응답
- **200 OK**: 삭제 성공
- **404 NOT FOUND**: 사용자를 찾을 수 없음

## 🔄 사용자 역할 관리 API

### 사용자 역할 전환

사용자의 역할을 학생 ↔ 교수 간 전환합니다.

```http
PATCH /api/users/{id}/toggle-role
```

#### Path Parameters
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| id | Integer | 역할을 변경할 사용자 ID |

#### 동작 방식
- `userStudent = 0` → `userStudent = 1` (학생 → 교수)
- `userStudent = 1` → `userStudent = 0` (교수 → 학생)

#### 응답 예시
```json
{
  "success": true,
  "message": "사용자 역할이 '교수'로 변경되었습니다.",
  "data": {
    "userIdx": 1,
    "userEmail": "user@bluecrab.com",
    "userName": "홍길동",
    "userStudent": 1
  },
  "timestamp": "2025-08-27T12:00:00Z"
}
```

## 🔍 사용자 검색 API

### 1. 이름으로 검색

사용자 이름으로 검색합니다 (부분 매치).

```http
GET /api/users/search?name={searchName}
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| name | String | ✅ | 검색할 이름 (부분 매치) |

#### 예시
```http
GET /api/users/search?name=홍길
```

### 2. 키워드로 검색

이름과 이메일에서 키워드를 검색합니다.

```http
GET /api/users/search-all?keyword={searchKeyword}
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| keyword | String | ✅ | 검색 키워드 (이름, 이메일) |

#### 예시
```http
GET /api/users/search-all?keyword=bluecrab
```

### 3. 생년월일 범위 검색

생년월일 범위로 사용자를 검색합니다.

```http
GET /api/users/search-birth?startDate={start}&endDate={end}
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| startDate | String | ✅ | 시작 날짜 (YYYYMMDD) |
| endDate | String | ✅ | 종료 날짜 (YYYYMMDD) |

#### 예시
```http
GET /api/users/search-birth?startDate=19900101&endDate=19991231
```

#### 응답 예시
```json
{
  "success": true,
  "message": "생년월일 19900101~19991231 범위의 사용자 15명을 찾았습니다.",
  "data": [
    {
      "userIdx": 1,
      "userEmail": "user1990@bluecrab.com",
      "userName": "김영희",
      "userBirth": "19950315"
    }
  ],
  "timestamp": "2025-08-27T12:00:00Z"
}
```

## 📊 사용자 통계 API

### 사용자 통계 조회

전체 사용자 통계 정보를 조회합니다.

```http
GET /api/users/stats
```

#### 응답 예시
```json
{
  "success": true,
  "message": "사용자 통계 정보를 성공적으로 조회했습니다.",
  "data": {
    "totalUsers": 150,
    "studentUsers": 120,
    "professorUsers": 30,
    "studentPercentage": 80.0,
    "professorPercentage": 20.0,
    "recentRegistrations": 15,
    "activeUsers": 140
  },
  "timestamp": "2025-08-27T12:00:00Z"
}
```

#### 통계 필드 설명
| 필드 | 타입 | 설명 |
|------|------|------|
| totalUsers | Integer | 전체 사용자 수 |
| studentUsers | Integer | 학생 사용자 수 |
| professorUsers | Integer | 교수 사용자 수 |
| studentPercentage | Double | 학생 비율 (%) |
| professorPercentage | Double | 교수 비율 (%) |
| recentRegistrations | Integer | 최근 가입자 수 (30일) |
| activeUsers | Integer | 활성 사용자 수 |

## 🔒 보안 고려사항

### 개인정보 보호
- 비밀번호(`userPw`)는 항상 암호화된 상태로 저장
- 민감한 개인정보 필드는 권한에 따라 필터링
- 로그에는 개인정보 출력 금지

### 권한 제어
- 일반 사용자: 본인 정보만 조회/수정 가능
- 관리자: 모든 사용자 정보 관리 가능
- 교수: 학생 정보 조회 가능 (제한적)

### API 사용 제한
- 검색 API: 너무 짧은 키워드 제한 (2자 이상)
- 대량 조회: 페이징 적용 예정
- Rate Limiting: 과도한 요청 방지

## 🚨 에러 응답

### 공통 에러 형식
```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null,
  "timestamp": "2025-08-27T12:00:00Z"
}
```

### 주요 에러 코드
| HTTP 상태 | 메시지 | 설명 |
|-----------|--------|------|
| 400 | 잘못된 요청입니다 | 필수 파라미터 누락, 형식 오류 |
| 401 | 인증이 필요합니다 | JWT 토큰 없음 또는 무효 |
| 403 | 접근 권한이 없습니다 | 권한 부족 |
| 404 | 사용자를 찾을 수 없습니다 | 존재하지 않는 사용자 ID |
| 409 | 이미 존재하는 사용자입니다 | 이메일 중복 |
| 500 | 서버 내부 오류입니다 | 예상치 못한 서버 오류 |

## 📝 사용 예제

### Postman 컬렉션 예제

#### 1. 로그인 후 토큰 획득
```javascript
// Pre-request Script
pm.test("Login and get token", function () {
    pm.sendRequest({
        url: pm.environment.get("base_url") + "/api/auth/login",
        method: 'POST',
        header: {
            'Content-Type': 'application/json'
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                username: "test@bluecrab.com",
                password: "password123"
            })
        }
    }, function (err, response) {
        var jsonData = response.json();
        pm.environment.set("jwt_token", jsonData.data.accessToken);
    });
});
```

#### 2. 사용자 조회 테스트
```javascript
// Test Script
pm.test("Get all users", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.be.true;
    pm.expect(jsonData.data).to.be.an('array');
});
```

### cURL 예제

#### 사용자 생성
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -d '{
    "userEmail": "newuser@bluecrab.com",
    "userPw": "$2a$10$hashedPassword",
    "userName": "신규사용자",
    "userPhone": "01012345678",
    "userBirth": "19900101",
    "userStudent": 1
  }'
```

#### 사용자 검색
```bash
curl -X GET "http://localhost:8080/api/users/search?name=김" \
  -H "Authorization: Bearer ${JWT_TOKEN}"
```

## 🔮 향후 개발 계획

### 단기 계획 (v1.1)
- [ ] 페이징 및 정렬 기능 추가
- [ ] 사용자 프로필 이미지 업로드
- [ ] 이메일 중복 검증 API
- [ ] 비밀번호 변경 API

### 장기 계획 (v2.0)
- [ ] 사용자 그룹 관리
- [ ] 권한 기반 접근 제어 (RBAC)
- [ ] 소셜 로그인 연동
- [ ] 사용자 활동 로그

---

> 💡 **참고**: API 명세는 개발 진행에 따라 변경될 수 있습니다. 
> 최신 정보는 [API 레퍼런스](API_REFERENCE.md)를 참고해주세요.