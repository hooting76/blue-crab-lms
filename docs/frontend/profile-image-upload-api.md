# 프로필 이미지 업로드 API (Frontend 연동 가이드)

**최종 수정**: 2025-10-22  
**사용 대상**: 프론트엔드 (React SPA) 개발자  
**관련 백엔드 문서**: `backend/BlueCrab/docs/api/PROFILE_IMAGE_UPLOAD.md`

---

## 1. 개요

로그인한 사용자가 자신의 프로필 사진을 업로드/교체하기 위한 API입니다.  
React 애플리케이션에서 `multipart/form-data` 요청을 직접 구성해야 하며, 업로드 직후 `profileImageKey`를 갱신해 사용자 상태를 최신화합니다.

---

## 2. API 요약

| 항목 | 값 |
|------|-----|
| HTTP Method | `POST` |
| Endpoint | `/api/profile/me/upload-image` |
| 인증 | 필수 (JWT Bearer 토큰) |
| Content-Type | `multipart/form-data` (자동 전송) |
| 요청 필드 | `file` (단일 이미지) |
| 허용 확장자 | `jpg`, `jpeg`, `png`, `gif` |
| 최대 크기 | 15MB |

---

## 3. 요청 규격

### Header

```
Authorization: Bearer <ACCESS_TOKEN>
```

> `Content-Type`은 `FormData` 사용 시 브라우저가 자동으로 설정합니다. 수동으로 지정하지 마세요.

### Body (`multipart/form-data`)

| 필드 | 타입 | 필수 | 비고 |
|------|------|------|------|
| `file` | File | O | `<input type="file">` 또는 드래그 드롭으로 선택한 단일 이미지 파일 |

---

## 4. 응답 규격

성공 시 (`HTTP 200`)

```json
{
  "success": true,
  "message": "프로필 이미지가 성공적으로 업로드되었습니다.",
  "data": {
    "imageKey": "profile_123_1739856123456.png",
    "hasImage": true
  },
  "timestamp": "2025-10-22T10:35:41.123+09:00"
}
```

- `imageKey`는 MinIO에 저장된 객체 키이며, 이후 `/api/profile/me` 응답에도 동일하게 반영됩니다.
- 실 이미지는 `/api/profile/me/image/file` 으로 조회합니다 (이미지 캐시 무효화도 백엔드에서 처리).

오류 예시

| 코드 | 상황 | 메시지 |
|------|------|--------|
| 400 | 파일 누락/유효성 실패 | `업로드할 파일이 없습니다.` / `허용되지 않는 파일 형식입니다.` 등 |
| 401 | 토큰 없음/만료 | `인증 토큰이 필요합니다.` |
| 404 | 사용자 정보 누락 | `사용자를 찾을 수 없습니다.` |
| 500 | MinIO 연결 등 서버 오류 | `프로필 이미지 업로드 중 오류가 발생했습니다.` |

---

## 5. React 연동 예시

### 5.1. Ant Design Form + axios 사용 예

```tsx
import axios from 'axios';
import { message } from 'antd';

async function uploadProfileImage(file: File, accessToken: string) {
  const formData = new FormData();
  formData.append('file', file);

  try {
    const { data } = await axios.post(
      '/api/profile/me/upload-image',
      formData,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        // axios는 FormData 사용 시 Content-Type을 자동 처리
      }
    );

    if (data.success) {
      message.success('프로필 이미지가 업데이트되었습니다.');
      return data.data.imageKey as string;
    }

    throw new Error(data.message || '업로드에 실패했습니다.');
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.data?.message) {
      message.error(error.response.data.message);
    } else {
      message.error('프로필 이미지 업로드 중 오류가 발생했습니다.');
    }
    throw error;
  }
}
```

### 5.2. Fetch API 사용 예

```ts
async function uploadProfileImage(file: File, token: string) {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch('/api/profile/me/upload-image', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      // Content-Type 설정 금지 (자동 처리)
    },
    body: formData,
  });

  const result = await response.json();

  if (!response.ok || !result.success) {
    throw new Error(result.message || '업로드 실패');
  }

  return result.data.imageKey as string;
}
```

> **TIP**  
> - 업로드 성공 후 `profileImageKey`가 변경되므로, 프로필 조회 API (`POST /api/profile/me`)를 다시 호출하거나, 프론트 스토어 상태를 직접 갱신해 UI에 반영하세요.  
> - 이미지 표시 시에는 서버에서 제공하는 `/api/profile/me/image/file`을 사용하거나 Presigned URL (`/api/profile/me/image`)을 활용할 수 있습니다.

---

## 6. 개발 및 테스트 참고

- 백엔드 API 테스트 페이지(`/status`)에서 “**프로필 이미지 업로드**” 템플릿을 선택하면 로컬에서도 쉽게 검증 가능합니다.
- MinIO 서버가 동작 중인지 반드시 확인 (`MINIO_ENDPOINT` 설정 필수). 정지 상태에서 업로드하면 500 오류가 발생합니다.
- 브라우저 캐시가 작동 중이면 새 이미지를 즉시 반영하기 위해 `img` 태그에 `?t=${Date.now()}` 같은 캐시 버스터를 붙이는 것이 좋습니다.

---

## 7. TODO / 향후 계획

- 이미지 업로드 진행률 UI(ProgressBar) 제공  
- 15MB 제한을 UI에서도 안내 (선제적 validate)  
- 필요 시 썸네일 생성 및 다중 사이즈 지원 논의

---

문의: 프론트엔드/백엔드 연동 담당자 또는 #bluecrab-backend 슬랙 채널

