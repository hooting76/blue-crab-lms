# Profile Image Upload API

**경로**: `POST /api/profile/me/upload-image`  
**인증**: JWT Bearer 토큰 (본인 계정만 업로드 가능)  
**요청 형식**: `multipart/form-data` (`file` 필드에 이미지 첨부)  
**응답 형식**: `ApiResponse<Map<String, Object>>`

---

## 작동 흐름

1. **인증 확인**  
   `Authorization: Bearer <token>` 헤더에서 사용자 이메일을 추출하고 토큰 유효성을 검사합니다.

2. **입력 검증**
   - 파일 존재 여부
   - 파일 크기 (`15MB` 이하)
   - 확장자 (`jpg`, `jpeg`, `png`, `gif`)
   - MIME 타입 (`image/jpeg`, `image/png`, `image/gif`)

3. **파일명 생성**
   `profile_{userIdx}_{timestamp}.{extension}` 규칙으로 고유한 키를 생성합니다.  
   (예: `profile_123_1739856123456.png`)

4. **MinIO 업로드**
   - `MinIOService.uploadProfileImage()`가 `app.minio.bucket-name`과 `app.minio.profile-folder` 설정을 반영해 객체 경로를 생성합니다.
   - 업로드가 실패하면 즉시 예외를 반환합니다.

5. **DB 업데이트**
   - `USER_TBL.PROFILE_IMAGE_KEY` 값을 새 키로 교체합니다.
   - 저장 실패 시 업로드한 MinIO 객체를 롤백 삭제합니다.

6. **캐시 및 기존 파일 정리**
   - Redis 이미지 캐시 무효화 (`ImageCacheService.evictImageCache`, `evictUserImageCache`).
   - 이전 이미지가 있으면 `MinIOService.deleteProfileImage()`로 삭제합니다. (실패 시 경고 로그만 남김)

7. **응답**
   ```json
   {
     "success": true,
     "message": "프로필 이미지가 성공적으로 업로드되었습니다.",
     "data": {
       "imageKey": "profile_123_1739856123456.png",
       "hasImage": true
     },
     "timestamp": "..."
   }
   ```

---

## 주요 구성 요소

| 파일 | 역할 |
|------|------|
| `ProfileController.uploadProfileImage()` | 요청 수신, JWT 검증, Service 호출, 예외 매핑 |
| `UserTblService.updateProfileImage()` | 검증/업로드/DB 저장/캐시 및 파일 정리 |
| `MinIOService.uploadProfileImage()` | 프로필 버킷 및 폴더 경로를 적용한 MinIO 업로드 |
| `MinIOService.deleteProfileImage()` | 기존 이미지 객체 삭제 |
| `ImageCacheService` | 프로필 이미지 Redis 캐시 무효화 |

---

## 예외 처리

| 상황 | 상태 코드 | 메시지 |
|------|-----------|--------|
| 인증 헤더 누락/무효 | `401` | `인증 토큰이 필요합니다.` |
| 파일 검증 실패 | `400` | 검증 메시지 (예: `허용되지 않는 파일 형식입니다.`) |
| 사용자 미존재 | `404` | `사용자를 찾을 수 없습니다.` |
| MinIO/DB 오류 | `500` | `프로필 이미지 업로드 중 오류가 발생했습니다.` |

---

## 운영 체크리스트

- `MINIO_BUCKET_NAME`, `MINIO_PROFILE_FOLDER` 환경 변수가 올바르게 설정되어 있는지 확인합니다.
- Redis 캐시가 활성화된 환경에서는 이미지 캐시 TTL이 적절한지 검토합니다.
- `/status` 페이지의 API Tester에서 `profileUploadImage` 템플릿으로 수동 검증이 가능합니다.
- 서버에 `mvn` 설치 후 `mvn -pl backend/BlueCrab compile -DskipTests`로 빌드 검증을 권장합니다.

