# 프로필 기본정보 업데이트 API

## 개요
사용자의 **이름**과 **전화번호**를 업데이트하는 API입니다.

---

## 요청

### Endpoint
```
POST /api/profile/basic-info/update
```

### Headers
```
Authorization: Bearer {JWT_TOKEN} //인증 토큰
Content-Type: application/json
```

### Body
```json
{
  "userName": "홍길동",
  "userPhone": "01012345678"
}
```

### 파라미터

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| userName | String | O | 이름 | 한글/영문/공백만 허용, 최대 50자 |
| userPhone | String | O | 전화번호 | 010으로 시작하는 11자리 숫자 (하이픈 없이) |

---

## 응답

### 성공 (200 OK)
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

### 오류 응답

| 상태 코드 | 상황 | 메시지 예시 |
|-----------|------|-------------|
| 400 | 입력값 검증 실패 | "이름은 한글, 영문, 공백만 사용 가능합니다" |
| 401 | 인증 실패 | "인증 토큰이 필요합니다" |
| 404 | 사용자 없음 | "사용자를 찾을 수 없습니다" |
| 409 | 전화번호 중복 | "이미 사용 중인 전화번호입니다" |
| 500 | 서버 오류 | "기본 정보 업데이트 중 오류가 발생했습니다" |

---

## 검증 규칙

### 이름 (userName)
**허용**
- ✅ "홍길동"
- ✅ "John Smith"
- ✅ "김 철수" (띄어쓰기 포함)

**거부**
- ❌ "홍길동123" (숫자 포함)
- ❌ "김철수!@#" (특수문자 포함)
- ❌ "" (빈 문자열)

### 전화번호 (userPhone)
**허용**
- ✅ "01012345678"
- ✅ "01087654321"

**거부**
- ❌ "010-1234-5678" (하이픈 포함)
- ❌ "01112345678" (010으로 시작하지 않음)
- ❌ "0101234567" (11자리 미만)

---

## 구현 예시

### JavaScript (Fetch)
```javascript
async function updateBasicInfo(userName, userPhone) {
  const response = await fetch('/api/profile/basic-info/update', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      userName: userName.trim(),
      userPhone: userPhone.replace(/[^\d]/g, '')  // 하이픈 제거
    })
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }

  return await response.json();
}
```

### Axios
```javascript
import axios from 'axios';

const updateBasicInfo = async (userName, userPhone) => {
  try {
    const response = await axios.post('/api/profile/basic-info/update', {
      userName: userName.trim(),
      userPhone: userPhone.replace(/[^\d]/g, '')
    }, {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });

    return response.data;
  } catch (error) {
    console.error('업데이트 실패:', error.response.data);
    throw error;
  }
};
```

---

## 주요 특징

- ✅ **자동 공백 제거**: 이름과 전화번호의 앞뒤 공백 자동 제거 (중간 공백은 유지)
- ✅ **전화번호 중복 방지**: 변경 시에만 중복 검사 (본인 번호는 제외)
- ✅ **이름 중복 허용**: 동명이인 가능
- ✅ **트랜잭션 보장**: 오류 시 자동 롤백

---

## 테스트

### cURL
```bash
curl -X POST http://localhost:8080/api/profile/basic-info/update \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "홍길동",
    "userPhone": "01012345678"
  }'
```

---

## 관련 API
- 프로필 조회: `POST /api/profile/me`
- 주소 업데이트: `POST /api/profile/address/update`
- 프로필 이미지 업로드: `POST /api/profile/me/upload-image`

---

**작성일**: 2025-10-24
