# 🦀 BlueCrab LMS - 프로필 API 명세서

## 📖 개요

BlueCrab Learning Management System의 사용자 프로필 관리를 위한 REST API 명세서입니다.  
JWT 토큰 기반 인증을 통해 본인의 프로필 정보만 조회할 수 있도록 보안이 구현되어 있습니다.

## 🔐 인증 방식

모든 API 요청은 Authorization 헤더에 Bearer 토큰을 포함해야 합니다.

```
Authorization: Bearer {JWT_TOKEN}
```

## 📊 공통 응답 형식

모든 API 응답은 다음과 같은 통일된 형식을 따릅니다:

```json
{
  "success": true,              // 요청 성공/실패 여부
  "message": "응답 메시지",      // 사용자에게 표시할 메시지 (한국어)
  "data": { ... },              // 실제 응답 데이터
  "errorCode": null,            // 에러 코드 (에러 시에만 값 존재)
  "timestamp": "2025-09-25T12:34:56.789Z"  // 응답 생성 시간 (ISO-8601)
}
```

## 🎯 API 엔드포인트

### 1. 프로필 조회 API

사용자의 종합적인 프로필 정보를 조회합니다.

**요청**
```
POST /api/profile/me
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
```

**응답 예시**
```json
{
  "success": true,
  "message": "프로필 정보를 성공적으로 조회했습니다.",  
  "data": {
    "userEmail": "student001@univ.edu",
    "userName": "서혜진",
    "userPhone": "01012345678",
    "userType": 0,
    "userTypeText": "학생",
    "majorCode": "202500101000",
    "zipCode": "12345",
    "mainAddress": "서울특별시 강남구 테헤란로 124",
    "detailAddress": "VALUE 아파트101호 501호",
    "birthDate": "20050315",
    "academicStatus": "재학",
    "admissionRoute": "정시",
    "majorFacultyCode": "01",
    "majorDeptCode": "01",
    "minorFacultyCode": "03", 
    "minorDeptCode": "03",
    "hasMajorInfo": true,
    "hasMinorInfo": true,
    "image": {
      "hasImage": true,
      "imageKey": "202500101000_20250925173216.jpg"
    }
  },
  "errorCode": null,
  "timestamp": "2025-09-25T03:26:31.526Z"
}
```

**응답 데이터 필드 설명**

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `userEmail` | String | 사용자 이메일 |
| `userName` | String | 사용자 이름 |
| `userPhone` | String | 전화번호 |
| `userType` | Integer | 사용자 타입 (0: 학생, 1: 교수) |
| `userTypeText` | String | 사용자 타입 텍스트 |
| `majorCode` | String | 학번 또는 교번 |
| `zipCode` | String | 우편번호 |
| `mainAddress` | String | 기본 주소 |
| `detailAddress` | String | 상세 주소 |
| `birthDate` | String | 생년월일 (YYYYMMDD) |
| `academicStatus` | String | 학적 상태 (재학, 휴학, 졸업 등) |
| `admissionRoute` | String | 입학 경로 (정시, 수시 등) |
| `majorFacultyCode` | String | 주전공 학부 코드 |
| `majorDeptCode` | String | 주전공 학과 코드 |
| `minorFacultyCode` | String | 부전공 학부 코드 |
| `minorDeptCode` | String | 부전공 학과 코드 |
| `hasMajorInfo` | Boolean | 주전공 정보 존재 여부 |
| `hasMinorInfo` | Boolean | 부전공 정보 존재 여부 |
| `image` | Object | 프로필 이미지 정보 |

**프로필 이미지 객체 (`image`) 필드**

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `hasImage` | Boolean | 프로필 이미지 존재 여부 |
| `imageKey` | String | 이미지 파일 키 (파일명) |

> **💡 이미지 조회**: 프론트엔드에서 `imageKey`를 사용하여 POST 요청으로 이미지를 조회할 수 있습니다.  
> 예시: `POST /api/profile/me/image/file` with body `{"imageKey": "${imageKey}"}`

**에러 응답**

```json
{
  "success": false,
  "message": "사용자를 찾을 수 없습니다.",
  "data": null,
  "errorCode": "USER_NOT_FOUND",
  "timestamp": "2025-09-25T17:32:16.789Z"
}
```

**HTTP 상태 코드**
- `200 OK`: 성공
- `401 Unauthorized`: 인증 실패 (토큰 없음 또는 유효하지 않음)
- `404 Not Found`: 사용자를 찾을 수 없음
- `500 Internal Server Error`: 서버 오류

### 2. 프로필 이미지 조회 API

사용자의 프로필 이미지 파일을 직접 조회합니다. 본인의 이미지만 접근 가능합니다.

**요청**
```
POST /api/profile/me/image/file
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
```

**요청 본문**
```json
{
  "imageKey": "202500101000_20250925173216.jpg"
}
```

**Parameters**
- `imageKey` (Request Body): 이미지 파일 키 (예: `202500101000_20250925173216.jpg`)

**응답**
- Content-Type: `image/jpeg`, `image/png`, `image/gif` 등
- 이미지 바이너리 데이터가 직접 반환됩니다.
- 브라우저에서 직접 이미지로 표시되거나 다운로드됩니다.

**응답 헤더**
```
Content-Type: image/jpeg
Content-Length: 123456
Cache-Control: public, max-age=86400
Content-Disposition: inline; filename="202500101000_20250925173216.jpg"
```

**에러 응답**

이미지 API는 일반적인 JSON 응답 대신 HTTP 상태 코드로 오류를 표현합니다:

- `400 Bad Request`: 잘못된 이미지 요청
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 본인의 이미지가 아님
- `404 Not Found`: 이미지를 찾을 수 없음
- `500 Internal Server Error`: 서버 오류

## 📋 사용 예시

### 1. 프로필 조회

```javascript
// 기본 프로필 조회
async function getMyProfile(token) {
    try {
        const response = await fetch('https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/profile/me', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();
        
        if (result.success) {
            return result.data;
        } else {
            throw new Error(result.message);
        }
    } catch (error) {
        console.error('프로필 조회 실패:', error);
        throw error;
    }
}

// 사용 예시
const token = localStorage.getItem('authToken');
const profile = await getMyProfile(token);

// UI에 표시
document.getElementById('userName').textContent = profile.userName;
document.getElementById('userEmail').textContent = profile.userEmail;
document.getElementById('userPhone').textContent = profile.userPhone;
```

### 2. 프로필 이미지 처리

```javascript
// 이미지 조회 함수
async function getProfileImage(token, imageKey) {
    try {
        const response = await fetch('https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/profile/me/image/file', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                imageKey: imageKey
            })
        });

        if (response.ok) {
            const blob = await response.blob();
            return URL.createObjectURL(blob);
        } else {
            throw new Error('이미지 로드 실패');
        }
    } catch (error) {
        console.error('프로필 이미지 조회 실패:', error);
        throw error;
    }
}

// 이미지 표시
async function displayProfileImage(profile, token) {
    const imgElement = document.getElementById('profileImage');
    
    if (profile.image.hasImage) {
        try {
            const imageUrl = await getProfileImage(token, profile.image.imageKey);
            imgElement.src = imageUrl;
        } catch (error) {
            // 기본 이미지 표시
            imgElement.src = '/assets/default-profile.png';
        }
    } else {
        // 기본 이미지 표시
        imgElement.src = '/assets/default-profile.png';
    }
}

// 사용 예시
const token = localStorage.getItem('authToken');
const profile = await getMyProfile(token);
await displayProfileImage(profile, token);
```

### 3. 주소 정보 처리

```javascript
// 전체 주소 조합
const fullAddress = profile.zipCode ? 
    `(${profile.zipCode}) ${profile.mainAddress} ${profile.detailAddress}`.trim() :
    `${profile.mainAddress} ${profile.detailAddress}`.trim();

document.getElementById('address').textContent = fullAddress;
```

### 4. 에러 처리

```javascript
try {
    const profile = await getMyProfile(token);
    // 성공 처리
} catch (error) {
    if (error.message.includes('토큰')) {
        // 토큰 만료 - 재로그인 필요
        window.location.href = '/login';
    } else {
        // 기타 오류
        alert('프로필 정보를 불러올 수 없습니다: ' + error.message);
    }
}
```

## 🔧 기술적 세부사항

### 보안 구현
- JWT 토큰 기반 인증
- 본인의 프로필 정보만 접근 가능
- CORS 설정으로 허용된 도메인에서만 접근 가능
- 프로필 이미지는 본인 소유 여부 검증 후 제공

### 캐싱 전략
- 프로필 이미지는 24시간 브라우저 캐시 설정
- MinIO 스토리지와 서버 메모리 캐시 이중 구조
- 이미지 변경 시 캐시 자동 무효화

### 에러 처리
- 모든 에러는 일관된 JSON 형식으로 응답
- 클라이언트 친화적인 한국어 에러 메시지
- 서버 로그와 클라이언트 에러 메시지 분리로 보안 강화

## 📞 지원 및 문의

- **개발팀**: BlueCrab Development Team
- **API 버전**: 1.0.0
- **문서 작성일**: 2025년 9월 25일
- **마지막 업데이트**: 2025년 9월 25일

---

> 이 문서는 BlueCrab LMS의 프로필 관리 API에 대한 완전한 명세를 제공합니다. 추가 문의사항이 있으시면 개발팀에 연락해 주세요.