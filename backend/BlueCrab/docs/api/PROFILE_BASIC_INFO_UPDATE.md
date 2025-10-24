# Profile Basic Info Update API

**경로**: `POST /api/profile/basic-info/update`
**인증**: JWT Bearer 토큰 (본인 계정만 수정 가능)
**요청 형식**: `application/json`
**응답 형식**: `ApiResponse<Map<String, Object>>`

---

## 개요

현재 로그인한 사용자의 **이름(userName)**과 **전화번호(userPhone)**를 업데이트하는 API입니다.
주소 업데이트 API(`/api/profile/address/update`)와 동일한 패턴으로 구현되어 있습니다.

**주요 기능:**
- 이름과 전화번호 동시 업데이트
- 자동 공백 정제 (앞뒤 trim)
- 전화번호 중복 방지 (변경 시만 검사)
- 입력값 유효성 검증

---

## 요청 예시

### HTTP 요청

```http
POST /api/profile/basic-info/update HTTP/1.1
Host: your-server.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "userName": "홍길동",
  "userPhone": "01012345678"
}
```

### JavaScript (Fetch API)

```javascript
const response = await fetch('/api/profile/basic-info/update', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    userName: '홍길동',
    userPhone: '01012345678'
  })
});

const result = await response.json();
console.log(result);
```

### Axios

```javascript
import axios from 'axios';

try {
  const response = await axios.post('/api/profile/basic-info/update', {
    userName: '홍길동',
    userPhone: '01012345678'
  }, {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  console.log(response.data);
} catch (error) {
  console.error('업데이트 실패:', error.response.data);
}
```

---

## 요청 파라미터

### Body Parameters (JSON)

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| `userName` | String | ✅ | 사용자 이름 | - 최대 50자<br>- 한글, 영문, 공백만 허용<br>- 숫자, 특수문자 불가 |
| `userPhone` | String | ✅ | 전화번호 | - 정확히 11자리<br>- 010으로 시작<br>- 하이픈 없이 숫자만 |

### 검증 규칙 (Bean Validation)

#### userName 검증
```java
@NotBlank(message = "이름은 필수입니다")
@Size(max = 50, message = "이름은 50자를 초과할 수 없습니다")
@Pattern(regexp = "^[가-힣a-zA-Z\\s]+$",
         message = "이름은 한글, 영문, 공백만 사용 가능합니다")
```

**허용되는 이름:**
- ✅ "홍길동"
- ✅ "John Smith"
- ✅ "김 철수" (띄어쓰기 포함)
- ✅ "이영희"

**거부되는 이름:**
- ❌ "홍길동123" (숫자 포함)
- ❌ "김철수!@#" (특수문자 포함)
- ❌ "Mary-Jane" (하이픈 불가)
- ❌ "" (빈 문자열)

#### userPhone 검증
```java
@NotBlank(message = "전화번호는 필수입니다")
@Pattern(regexp = "^010[0-9]{8}$",
         message = "전화번호는 010으로 시작하는 11자리 숫자여야 합니다")
```

**허용되는 전화번호:**
- ✅ "01012345678"
- ✅ "01087654321"
- ✅ "01099998888"

**거부되는 전화번호:**
- ❌ "010-1234-5678" (하이픈 포함)
- ❌ "01112345678" (010으로 시작하지 않음)
- ❌ "0101234567" (10자리)
- ❌ "010123456789" (12자리)

---

## 작동 흐름

### 1. 인증 확인
`Authorization: Bearer <token>` 헤더에서 JWT 토큰을 추출하고 유효성을 검사합니다.
- 토큰이 없으면 `401 Unauthorized` 반환
- 토큰이 유효하지 않으면 `401 Unauthorized` 반환

### 2. 입력 검증 (Bean Validation)
요청 Body의 JSON 데이터를 `@Valid`로 자동 검증합니다.
- 검증 실패 시 `400 Bad Request` 반환
- 에러 메시지에 구체적인 검증 실패 이유 포함

### 3. 사용자 조회
JWT 토큰에서 추출한 이메일로 `USER_TBL`에서 사용자를 조회합니다.
- 사용자가 없으면 `404 Not Found` 반환

### 4. 입력값 정제 (Sanitization)
앞뒤 공백을 자동으로 제거합니다. (중간 공백은 유지)

```java
String cleanUserName = userName.trim();    // "  홍길동  " → "홍길동"
String cleanUserPhone = userPhone.trim();  // "01012345678  " → "01012345678"
```

**처리 예시:**
- 입력: `"홍길동   "` → 저장: `"홍길동"`
- 입력: `"  김철수"` → 저장: `"김철수"`
- 입력: `"이 영 희  "` → 저장: `"이 영 희"` (중간 공백 유지)

### 5. 전화번호 중복 검사
**변경된 경우에만** 중복을 검사합니다. (본인 번호는 중복 검사 제외)

```java
if (!user.getUserPhone().equals(cleanUserPhone) &&
    userTblRepository.existsByUserPhone(cleanUserPhone)) {
    throw new DuplicateResourceException("이미 사용 중인 전화번호입니다");
}
```

- 전화번호가 변경되지 않았으면 중복 검사 생략
- 다른 사용자가 이미 사용 중인 번호면 `409 Conflict` 반환

> **참고:** 이름은 중복 검사를 하지 않습니다. (동명이인 허용)

### 6. 데이터베이스 업데이트
`@Transactional` 내에서 `USER_TBL` 테이블을 업데이트합니다.

```java
user.setUserName(cleanUserName);    // USER_NAME VARCHAR(50)
user.setUserPhone(cleanUserPhone);  // USER_PHONE CHAR(11)
userTblRepository.save(user);       // dirty checking으로 자동 UPDATE
```

- 트랜잭션 내에서 실행되므로 오류 시 자동 롤백
- JPA dirty checking으로 변경된 필드만 UPDATE

### 7. 응답 반환
업데이트된 정보를 `ApiResponse` 형식으로 반환합니다.

---

## 응답 형식

### 성공 응답 (200 OK)

```json
{
  "success": true,
  "message": "기본 정보가 성공적으로 업데이트되었습니다.",
  "data": {
    "userName": "홍길동",
    "userPhone": "01012345678"
  },
  "timestamp": "2025-10-24T15:30:00.123Z"
}
```

#### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | Boolean | 성공 여부 (항상 `true`) |
| `message` | String | 성공 메시지 |
| `data` | Object | 업데이트된 기본 정보 |
| `data.userName` | String | 업데이트된 이름 (trim 처리됨) |
| `data.userPhone` | String | 업데이트된 전화번호 (trim 처리됨) |
| `timestamp` | String | 응답 생성 시각 (ISO 8601) |

---

## 오류 응답

### 1. 인증 오류 (401 Unauthorized)

**상황:**
- Authorization 헤더가 없는 경우
- JWT 토큰이 유효하지 않은 경우
- 토큰이 만료된 경우

```json
{
  "success": false,
  "message": "인증 토큰이 필요합니다.",
  "data": null,
  "timestamp": "2025-10-24T15:30:00.123Z"
}
```

**프론트엔드 처리:**
```javascript
if (response.status === 401) {
  // 토큰 갱신 또는 로그인 페이지로 리다이렉트
  redirectToLogin();
}
```

---

### 2. 입력값 검증 오류 (400 Bad Request)

**상황:**
- 이름이 빈 문자열인 경우
- 이름에 숫자/특수문자가 포함된 경우
- 전화번호 형식이 잘못된 경우

```json
{
  "success": false,
  "message": "이름은 한글, 영문, 공백만 사용 가능합니다",
  "data": null,
  "timestamp": "2025-10-24T15:30:00.123Z"
}
```

**다른 검증 오류 메시지 예시:**
- `"이름은 필수입니다"`
- `"이름은 50자를 초과할 수 없습니다"`
- `"전화번호는 필수입니다"`
- `"전화번호는 010으로 시작하는 11자리 숫자여야 합니다"`

**프론트엔드 처리:**
```javascript
if (response.status === 400) {
  const error = await response.json();
  // 에러 메시지를 입력 필드 아래에 표시
  showFieldError('userName', error.message);
}
```

---

### 3. 사용자 없음 (404 Not Found)

**상황:**
- JWT 토큰은 유효하지만 해당 이메일의 사용자가 DB에 없는 경우 (드물게 발생)

```json
{
  "success": false,
  "message": "사용자를 찾을 수 없습니다: user@example.com",
  "data": null,
  "timestamp": "2025-10-24T15:30:00.123Z"
}
```

**프론트엔드 처리:**
```javascript
if (response.status === 404) {
  // 사용자 세션 만료 처리
  alert('사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요.');
  redirectToLogin();
}
```

---

### 4. 전화번호 중복 (409 Conflict)

**상황:**
- 입력한 전화번호가 이미 다른 사용자가 사용 중인 경우

```json
{
  "success": false,
  "message": "이미 사용 중인 전화번호입니다: 01012345678",
  "data": null,
  "timestamp": "2025-10-24T15:30:00.123Z"
}
```

**프론트엔드 처리:**
```javascript
if (response.status === 409) {
  const error = await response.json();
  // 전화번호 필드에 중복 오류 표시
  showFieldError('userPhone', '이미 사용 중인 전화번호입니다.');
}
```

---

### 5. 서버 오류 (500 Internal Server Error)

**상황:**
- 데이터베이스 연결 오류
- 예상치 못한 시스템 오류

```json
{
  "success": false,
  "message": "기본 정보 업데이트 중 오류가 발생했습니다.",
  "data": null,
  "timestamp": "2025-10-24T15:30:00.123Z"
}
```

**프론트엔드 처리:**
```javascript
if (response.status === 500) {
  alert('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
}
```

---

## 프론트엔드 구현 가이드

### 1. 기본 사용 예시

```javascript
// API 호출 함수
async function updateBasicInfo(userName, userPhone) {
  try {
    const response = await fetch('/api/profile/basic-info/update', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${getAccessToken()}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userName: userName.trim(),  // 앞뒤 공백 제거 (선택적)
        userPhone: userPhone.replace(/-/g, '')  // 하이픈 제거
      })
    });

    const result = await response.json();

    if (!response.ok) {
      throw new Error(result.message);
    }

    return result;
  } catch (error) {
    console.error('기본정보 업데이트 실패:', error);
    throw error;
  }
}

// 사용
try {
  const result = await updateBasicInfo('홍길동', '010-1234-5678');
  console.log('업데이트 성공:', result.data);
  alert('기본 정보가 업데이트되었습니다.');
} catch (error) {
  alert(`업데이트 실패: ${error.message}`);
}
```

---

### 2. React 구현 예시

```jsx
import { useState } from 'react';
import axios from 'axios';

function ProfileBasicInfoForm() {
  const [formData, setFormData] = useState({
    userName: '',
    userPhone: ''
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  // 전화번호 자동 하이픈 추가 (입력용)
  const formatPhoneNumber = (value) => {
    const numbers = value.replace(/[^\d]/g, '');
    if (numbers.length <= 3) return numbers;
    if (numbers.length <= 7) return `${numbers.slice(0, 3)}-${numbers.slice(3)}`;
    return `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7, 11)}`;
  };

  // 입력 핸들러
  const handleChange = (e) => {
    const { name, value } = e.target;

    if (name === 'userPhone') {
      setFormData(prev => ({
        ...prev,
        [name]: formatPhoneNumber(value)
      }));
    } else {
      setFormData(prev => ({
        ...prev,
        [name]: value
      }));
    }

    // 에러 초기화
    setErrors(prev => ({ ...prev, [name]: '' }));
  };

  // 클라이언트 측 검증
  const validate = () => {
    const newErrors = {};

    // 이름 검증
    if (!formData.userName.trim()) {
      newErrors.userName = '이름은 필수입니다';
    } else if (formData.userName.length > 50) {
      newErrors.userName = '이름은 50자를 초과할 수 없습니다';
    } else if (!/^[가-힣a-zA-Z\s]+$/.test(formData.userName)) {
      newErrors.userName = '이름은 한글, 영문, 공백만 사용 가능합니다';
    }

    // 전화번호 검증
    const phoneNumbers = formData.userPhone.replace(/[^\d]/g, '');
    if (!phoneNumbers) {
      newErrors.userPhone = '전화번호는 필수입니다';
    } else if (!/^010[0-9]{8}$/.test(phoneNumbers)) {
      newErrors.userPhone = '전화번호는 010으로 시작하는 11자리 숫자여야 합니다';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // 제출 핸들러
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validate()) {
      return;
    }

    setLoading(true);

    try {
      const response = await axios.post('/api/profile/basic-info/update', {
        userName: formData.userName.trim(),
        userPhone: formData.userPhone.replace(/[^\d]/g, '')  // 하이픈 제거
      }, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        }
      });

      alert('기본 정보가 성공적으로 업데이트되었습니다.');
      console.log('업데이트된 정보:', response.data.data);

    } catch (error) {
      if (error.response) {
        const { status, data } = error.response;

        switch (status) {
          case 400:
            setErrors({ general: data.message });
            break;
          case 401:
            alert('인증이 만료되었습니다. 다시 로그인해주세요.');
            // 로그인 페이지로 리다이렉트
            break;
          case 409:
            setErrors({ userPhone: '이미 사용 중인 전화번호입니다.' });
            break;
          case 500:
            alert('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
            break;
          default:
            alert(`오류가 발생했습니다: ${data.message}`);
        }
      } else {
        alert('네트워크 오류가 발생했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <div>
        <label htmlFor="userName">이름</label>
        <input
          type="text"
          id="userName"
          name="userName"
          value={formData.userName}
          onChange={handleChange}
          placeholder="홍길동"
          maxLength={50}
        />
        {errors.userName && <span className="error">{errors.userName}</span>}
      </div>

      <div>
        <label htmlFor="userPhone">전화번호</label>
        <input
          type="text"
          id="userPhone"
          name="userPhone"
          value={formData.userPhone}
          onChange={handleChange}
          placeholder="010-1234-5678"
          maxLength={13}  // 하이픈 포함 최대 길이
        />
        {errors.userPhone && <span className="error">{errors.userPhone}</span>}
      </div>

      {errors.general && <div className="error">{errors.general}</div>}

      <button type="submit" disabled={loading}>
        {loading ? '업데이트 중...' : '저장'}
      </button>
    </form>
  );
}

export default ProfileBasicInfoForm;
```

---

### 3. Vue.js 구현 예시

```vue
<template>
  <form @submit.prevent="handleSubmit">
    <div>
      <label for="userName">이름</label>
      <input
        type="text"
        id="userName"
        v-model="formData.userName"
        placeholder="홍길동"
        maxlength="50"
      />
      <span v-if="errors.userName" class="error">{{ errors.userName }}</span>
    </div>

    <div>
      <label for="userPhone">전화번호</label>
      <input
        type="text"
        id="userPhone"
        v-model="formData.userPhone"
        @input="formatPhone"
        placeholder="010-1234-5678"
        maxlength="13"
      />
      <span v-if="errors.userPhone" class="error">{{ errors.userPhone }}</span>
    </div>

    <div v-if="errors.general" class="error">{{ errors.general }}</div>

    <button type="submit" :disabled="loading">
      {{ loading ? '업데이트 중...' : '저장' }}
    </button>
  </form>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      formData: {
        userName: '',
        userPhone: ''
      },
      errors: {},
      loading: false
    };
  },
  methods: {
    formatPhone() {
      const numbers = this.formData.userPhone.replace(/[^\d]/g, '');
      if (numbers.length <= 3) {
        this.formData.userPhone = numbers;
      } else if (numbers.length <= 7) {
        this.formData.userPhone = `${numbers.slice(0, 3)}-${numbers.slice(3)}`;
      } else {
        this.formData.userPhone = `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7, 11)}`;
      }
    },
    validate() {
      this.errors = {};

      // 이름 검증
      if (!this.formData.userName.trim()) {
        this.errors.userName = '이름은 필수입니다';
      } else if (this.formData.userName.length > 50) {
        this.errors.userName = '이름은 50자를 초과할 수 없습니다';
      } else if (!/^[가-힣a-zA-Z\s]+$/.test(this.formData.userName)) {
        this.errors.userName = '이름은 한글, 영문, 공백만 사용 가능합니다';
      }

      // 전화번호 검증
      const phoneNumbers = this.formData.userPhone.replace(/[^\d]/g, '');
      if (!phoneNumbers) {
        this.errors.userPhone = '전화번호는 필수입니다';
      } else if (!/^010[0-9]{8}$/.test(phoneNumbers)) {
        this.errors.userPhone = '전화번호는 010으로 시작하는 11자리 숫자여야 합니다';
      }

      return Object.keys(this.errors).length === 0;
    },
    async handleSubmit() {
      if (!this.validate()) {
        return;
      }

      this.loading = true;

      try {
        const response = await axios.post('/api/profile/basic-info/update', {
          userName: this.formData.userName.trim(),
          userPhone: this.formData.userPhone.replace(/[^\d]/g, '')
        }, {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
          }
        });

        alert('기본 정보가 성공적으로 업데이트되었습니다.');
        console.log('업데이트된 정보:', response.data.data);

      } catch (error) {
        if (error.response) {
          const { status, data } = error.response;

          switch (status) {
            case 400:
              this.errors.general = data.message;
              break;
            case 401:
              alert('인증이 만료되었습니다. 다시 로그인해주세요.');
              break;
            case 409:
              this.errors.userPhone = '이미 사용 중인 전화번호입니다.';
              break;
            case 500:
              alert('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
              break;
            default:
              alert(`오류가 발생했습니다: ${data.message}`);
          }
        } else {
          alert('네트워크 오류가 발생했습니다.');
        }
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
```

---

## 주요 특징

### ✅ 자동 데이터 정제
- 앞뒤 공백 자동 제거 (서버 측에서 처리)
- 중간 공백은 유지 ("이 영 희" 같은 케이스 허용)

### ✅ 전화번호 중복 방지
- 변경된 경우에만 중복 검사
- 본인의 기존 번호는 중복 검사 제외
- 409 Conflict 응답으로 명확한 오류 전달

### ✅ 이름 중복 허용
- 동명이인 가능
- 중복 검사 없음

### ✅ 트랜잭션 보장
- `@Transactional`로 원자성 보장
- 오류 시 자동 롤백

---

## 데이터베이스 매핑

| DTO 필드 | DB 컬럼 | 타입 | 제약사항 |
|----------|---------|------|----------|
| `userName` | `USER_NAME` | VARCHAR(50) | NOT NULL |
| `userPhone` | `USER_PHONE` | CHAR(11) | NOT NULL, UNIQUE |

---

## 보안 고려사항

### 1. JWT 인증
- 모든 요청에 유효한 JWT 토큰 필요
- 토큰에서 추출한 이메일로만 본인 정보 수정 가능
- 다른 사용자의 정보는 절대 수정 불가

### 2. 입력값 검증
- 서버 측 Bean Validation으로 2중 검증
- SQL Injection 방지 (JPA 사용)
- XSS 방지 (프론트엔드에서 출력 시 이스케이프 필요)

### 3. 민감 정보 보호
- 전화번호는 UNIQUE 제약으로 중복 방지
- 로그에 민감 정보 최소화 (이름만 기록, 전화번호는 마스킹 권장)

---

## 테스트 케이스

### Postman / cURL 테스트

```bash
# 성공 케이스
curl -X POST http://localhost:8080/api/profile/basic-info/update \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "홍길동",
    "userPhone": "01012345678"
  }'

# 검증 오류 케이스 - 잘못된 이름
curl -X POST http://localhost:8080/api/profile/basic-info/update \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "홍길동123",
    "userPhone": "01012345678"
  }'

# 검증 오류 케이스 - 잘못된 전화번호
curl -X POST http://localhost:8080/api/profile/basic-info/update \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "홍길동",
    "userPhone": "010-1234-5678"
  }'

# 전화번호 중복 케이스
curl -X POST http://localhost:8080/api/profile/basic-info/update \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "홍길동",
    "userPhone": "01087654321"
  }'
```

---

## FAQ

### Q1. 이름만 수정하고 싶은데 전화번호도 필수인가요?
**A:** 네, 현재 API는 이름과 전화번호를 모두 받도록 설계되어 있습니다. 둘 다 필수 파라미터입니다. 이름만 수정하려면 기존 전화번호를 그대로 전송하면 됩니다.

### Q2. 전화번호에 하이픈을 포함해서 보내도 되나요?
**A:** 아니요, 하이픈 없이 숫자만 11자리로 보내야 합니다. 프론트엔드에서 하이픈을 제거한 후 전송해주세요.
```javascript
userPhone: formData.userPhone.replace(/[^\d]/g, '')
```

### Q3. 공백이 포함된 이름은 어떻게 처리되나요?
**A:** 앞뒤 공백은 자동으로 제거되지만, 중간 공백은 유지됩니다.
- 입력: `"  홍 길 동  "` → 저장: `"홍 길 동"`

### Q4. 전화번호를 변경하지 않으면 중복 검사를 안 하나요?
**A:** 네, 기존 전화번호와 동일하면 중복 검사를 생략합니다. 이는 불필요한 DB 조회를 방지하기 위함입니다.

### Q5. 이름 길이 제한이 50자인 이유는?
**A:** 데이터베이스 컬럼이 `VARCHAR(50)`으로 정의되어 있기 때문입니다. 일반적인 한국/영문 이름은 충분히 수용 가능합니다.

### Q6. 특수문자가 포함된 외국인 이름은 어떻게 하나요?
**A:** 현재는 한글, 영문, 공백만 허용됩니다. 하이픈(`-`)이나 아포스트로피(`'`)가 필요한 경우 관리자에게 문의해주세요.

---

## 관련 API

- **프로필 조회**: `POST /api/profile/me`
- **주소 업데이트**: `POST /api/profile/address/update`
- **프로필 이미지 업로드**: `POST /api/profile/me/upload-image`
- **프로필 완성도 체크**: `POST /api/profile/me/completeness`

---

## 버전 이력

| 버전 | 날짜 | 변경사항 |
|------|------|----------|
| 1.0.0 | 2025-10-24 | 초기 릴리스 |

---

## 문의

기술적 문제나 버그 발견 시:
- 백엔드 담당자에게 문의
- GitHub Issue 등록
- 개발팀 Slack 채널

---

**작성자**: BlueCrab Development Team
**최종 수정일**: 2025-10-24
