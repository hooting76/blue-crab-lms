# 프로필 이미지 업로드 API 구현 계획서

**작성일**: 2025-10-21
**작성자**: Claude AI
**버전**: 1.0
**상태**: 계획 단계

---

## 📋 목차

1. [개요](#개요)
2. [현재 상태 분석](#현재-상태-분석)
3. [구현 방식 결정](#구현-방식-결정)
4. [상세 구현 계획](#상세-구현-계획)
5. [API 명세](#api-명세)
6. [구현 체크리스트](#구현-체크리스트)
7. [테스트 계획](#테스트-계획)

---

## 개요

### 목적
사용자가 자신의 프로필 사진을 업로드하고 관리할 수 있는 API를 구현합니다.

### 요구사항
- JWT 인증을 통한 본인 인증
- 이미지 파일 업로드 (jpg, jpeg, png, gif)
- 기존 이미지가 있을 경우 삭제 후 새 이미지로 교체
- MinIO 스토리지에 안전하게 저장
- 트랜잭션 안전성 보장

### 기술 스택
- **백엔드**: Spring Boot, JPA
- **인증**: JWT (Bearer Token)
- **스토리지**: MinIO
- **응답 형식**: ApiResponse<T> 표준 래퍼

---

## 현재 상태 분석

### ✅ 이미 구축된 인프라

#### 1. 데이터베이스 구조
**파일**: `UserTbl.java` (line 88-89)
```java
@Column(name = "PROFILE_IMAGE_KEY", length = 255)
private String profileImageKey;
```
- `USER_TBL` 테이블에 `PROFILE_IMAGE_KEY` 컬럼 존재
- 이미지 파일의 MinIO 키를 저장하는 필드

#### 2. MinIO 설정
**파일**: `application.properties` (line 56-57)
```properties
app.minio.bucket-name=${MINIO_BUCKET_NAME:profile-img}
app.minio.profile-folder=${MINIO_PROFILE_FOLDER:}
```
- Bucket: `profile-img`
- Folder prefix: 설정 가능 (현재 빈 값)

**파일 업로드 제한** (line 66-67):
```properties
spring.servlet.multipart.max-file-size=${BOARD_FILE_MAX_SIZE:15MB}
spring.servlet.multipart.max-request-size=${BOARD_FILE_MAX_REQUEST_SIZE:20MB}
```

#### 3. 유틸리티 클래스
**파일**: `MinIOFileUtil.java`
- `uploadFile(MultipartFile file, String filePath)`: 파일 업로드
- `deleteFile(String filePath)`: 파일 삭제
- 이미 구현 완료된 메서드들

#### 4. 기존 컨트롤러
**파일**: `ProfileController.java`
- ✅ 프로필 이미지 조회 API (`/api/profile/me/image`)
- ✅ 프로필 이미지 파일 다운로드 API (`/api/profile/me/image/file`)
- ❌ 프로필 이미지 **업로드** API (미구현)

#### 5. 보안 설정
**파일**: `SecurityConfig.java` (line 146-150)
```java
.requestMatchers(HttpMethod.POST, "/api/profile/me/image").authenticated()
.requestMatchers(HttpMethod.POST, "/api/profile/me/image/file").authenticated()
```
- 프로필 관련 POST 엔드포인트는 이미 인증 필수로 설정됨

#### 6. 참고할 기존 구현
**파일**: `BoardAttachmentUploadController.java`
- MultipartFile 업로드 패턴
- JWT 토큰 검증 방식
- MinIO 업로드 에러 처리

### ❌ 구현 필요 사항

1. **프로필 이미지 업로드 엔드포인트** (ProfileController)
2. **이미지 업데이트 서비스 로직** (UserTblService)
3. **파일 유효성 검증** (이미지 타입, 크기)
4. **기존 이미지 삭제 로직**
5. **API 테스트 템플릿** (api-templates.json)

---

## 구현 방식 결정

### 선택된 방식: **방식 1 - 삭제 후 새 파일명 업로드** ⭐

#### 동작 흐름
```
[기존 상태]
DB: profileImageKey = "profile_123_1234567890.jpg"
MinIO: profile-img/profile_123_1234567890.jpg

[업로드 요청]
1. 새 파일 생성: profile_123_1735789012.png
2. MinIO 업로드: profile-img/profile_123_1735789012.png
3. DB 업데이트: profileImageKey = "profile_123_1735789012.png"
4. 이전 파일 삭제: profile-img/profile_123_1234567890.jpg

[결과]
DB: profileImageKey = "profile_123_1735789012.png"
MinIO: profile-img/profile_123_1735789012.png (이전 파일 삭제됨)
```

#### 장점
- ✅ **스토리지 효율성**: 불필요한 파일 즉시 삭제
- ✅ **확장자 유연성**: jpg→png 변경 시에도 자연스럽게 처리
- ✅ **캐시 무효화**: 파일명 변경으로 브라우저 캐시 자동 갱신
- ✅ **기존 패턴 일치**: BoardAttachmentUploadController와 동일한 방식

#### 트랜잭션 안전성 확보 전략
```
순서 보장:
1. 새 이미지 업로드 (실패 시 전체 중단)
2. DB 업데이트 (@Transactional 보호)
3. 기존 이미지 삭제 (실패해도 진행 - 로그만 기록)
```

**안전성 보장 포인트:**
- 새 이미지 업로드 실패 → DB 변경 없음 (롤백 불필요)
- DB 업데이트 실패 → @Transactional로 자동 롤백, MinIO에 불필요한 파일 남음 (허용 가능)
- 기존 이미지 삭제 실패 → 서비스 정상 진행 (MinIO에 오래된 파일 남음, 정리 필요)

#### 파일명 생성 규칙
```
패턴: profile_{userIdx}_{timestamp}.{extension}

예시:
- profile_123_1735789012345.jpg
- profile_456_1735789023456.png
```

**규칙 상세:**
- `userIdx`: 사용자 고유 ID (충돌 방지)
- `timestamp`: `System.currentTimeMillis()` (고유성 보장)
- `extension`: 원본 파일 확장자 (소문자 변환)

---

## 상세 구현 계획

### Phase 1: Service Layer 구현

#### 파일: `UserTblService.java`

##### 1-1. 프로필 이미지 업데이트 메서드

```java
/**
 * 사용자 프로필 이미지 업데이트
 * 기존 이미지가 있으면 삭제하고 새 이미지로 교체
 *
 * @param userEmail JWT에서 추출한 사용자 이메일
 * @param file 업로드할 이미지 파일
 * @return 업로드된 이미지 키
 * @throws RuntimeException 파일 검증 실패, 업로드 실패, 사용자 없음
 */
@Transactional
public String updateProfileImage(String userEmail, MultipartFile file) {
    // 1. 파일 유효성 검증
    validateImageFile(file);

    // 2. 사용자 조회
    UserTbl user = userTblRepository.findByUserEmail(userEmail)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    String oldImageKey = user.getProfileImageKey();

    // 3. 새 파일명 생성
    String newImageKey = generateProfileImageKey(user.getUserIdx(), file);

    // 4. MinIO에 새 이미지 업로드
    try {
        minIOFileUtil.uploadFile(file, newImageKey);
        logger.info("프로필 이미지 업로드 완료 - 사용자: {}, 이미지: {}",
                   userEmail, newImageKey);
    } catch (Exception e) {
        logger.error("프로필 이미지 업로드 실패 - 사용자: {}", userEmail, e);
        throw new RuntimeException("이미지 업로드에 실패했습니다: " + e.getMessage());
    }

    // 5. DB 업데이트
    user.setProfileImageKey(newImageKey);
    userTblRepository.save(user);

    // 6. 기존 이미지 삭제 (업로드 성공 후)
    if (oldImageKey != null && !oldImageKey.trim().isEmpty()) {
        deleteOldProfileImage(oldImageKey, userEmail);
    }

    return newImageKey;
}
```

##### 1-2. 파일 유효성 검증 메서드

```java
/**
 * 이미지 파일 유효성 검증
 *
 * @param file 검증할 파일
 * @throws RuntimeException 검증 실패 시
 */
private void validateImageFile(MultipartFile file) {
    // 1. 파일 존재 여부
    if (file == null || file.isEmpty()) {
        throw new RuntimeException("업로드할 파일이 없습니다.");
    }

    // 2. 파일 크기 검증 (15MB)
    long maxSize = 15 * 1024 * 1024; // 15MB
    if (file.getSize() > maxSize) {
        throw new RuntimeException("파일 크기는 15MB를 초과할 수 없습니다.");
    }

    // 3. 파일 확장자 검증
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null) {
        throw new RuntimeException("파일명이 유효하지 않습니다.");
    }

    String extension = getFileExtension(originalFilename).toLowerCase();
    List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif");

    if (!allowedExtensions.contains(extension)) {
        throw new RuntimeException(
            "허용되지 않는 파일 형식입니다. (jpg, jpeg, png, gif만 가능)"
        );
    }

    // 4. MIME 타입 검증 (추가 보안)
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
        throw new RuntimeException("이미지 파일만 업로드 가능합니다.");
    }

    List<String> allowedMimeTypes = Arrays.asList(
        "image/jpeg", "image/png", "image/gif"
    );

    if (!allowedMimeTypes.contains(contentType)) {
        throw new RuntimeException(
            "허용되지 않는 이미지 형식입니다. (JPEG, PNG, GIF만 가능)"
        );
    }
}
```

##### 1-3. 파일명 생성 메서드

```java
/**
 * 프로필 이미지 파일명 생성
 * 패턴: profile_{userIdx}_{timestamp}.{extension}
 *
 * @param userIdx 사용자 ID
 * @param file 업로드 파일
 * @return 생성된 파일명
 */
private String generateProfileImageKey(Integer userIdx, MultipartFile file) {
    String originalFilename = file.getOriginalFilename();
    String extension = getFileExtension(originalFilename).toLowerCase();
    long timestamp = System.currentTimeMillis();

    return String.format("profile_%d_%d.%s", userIdx, timestamp, extension);
}

/**
 * 파일 확장자 추출
 *
 * @param filename 파일명
 * @return 확장자 (점 제외)
 */
private String getFileExtension(String filename) {
    int lastDotIndex = filename.lastIndexOf('.');
    if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
        return filename.substring(lastDotIndex + 1);
    }
    return "";
}
```

##### 1-4. 기존 이미지 삭제 메서드

```java
/**
 * 기존 프로필 이미지 삭제
 * 삭제 실패해도 서비스는 정상 진행 (로그만 기록)
 *
 * @param imageKey 삭제할 이미지 키
 * @param userEmail 사용자 이메일 (로깅용)
 */
private void deleteOldProfileImage(String imageKey, String userEmail) {
    try {
        minIOFileUtil.deleteFile(imageKey);
        logger.info("기존 프로필 이미지 삭제 완료 - 사용자: {}, 이미지: {}",
                   userEmail, imageKey);
    } catch (Exception e) {
        // 삭제 실패해도 서비스는 정상 진행
        // MinIO에 오래된 파일이 남지만, 새 이미지는 정상 업로드됨
        logger.warn("기존 프로필 이미지 삭제 실패 (무시) - 사용자: {}, 이미지: {}, 오류: {}",
                   userEmail, imageKey, e.getMessage());
    }
}
```

---

### Phase 2: Controller Layer 구현

#### 파일: `ProfileController.java`

```java
/**
 * 프로필 이미지 업로드
 * JWT 토큰으로 인증된 사용자의 프로필 이미지를 업로드
 *
 * @param file 업로드할 이미지 파일
 * @param request HTTP 요청 (Authorization 헤더에서 토큰 추출)
 * @return 업로드된 이미지 정보
 *
 * 요청 예시:
 * POST /api/profile/me/upload-image
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * Content-Type: multipart/form-data
 *
 * Form Data:
 * - file: (이미지 파일)
 *
 * 응답 예시:
 * {
 *   "success": true,
 *   "message": "프로필 이미지가 성공적으로 업로드되었습니다.",
 *   "data": {
 *     "imageKey": "profile_123_1735789012345.jpg",
 *     "hasImage": true
 *   },
 *   "timestamp": "2025-10-21T10:30:00Z"
 * }
 */
@PostMapping("/me/upload-image")
public ResponseEntity<ApiResponse<Map<String, Object>>> uploadProfileImage(
        @RequestParam("file") MultipartFile file,
        HttpServletRequest request) {

    try {
        // JWT 토큰에서 사용자 이메일 추출
        String userEmail = extractUserEmailFromToken(request);

        logger.info("프로필 이미지 업로드 요청 - 사용자: {}, 파일: {}, 크기: {}",
                   userEmail, file.getOriginalFilename(), file.getSize());

        // 서비스 계층 호출
        String imageKey = userTblService.updateProfileImage(userEmail, file);

        // 응답 데이터 생성
        Map<String, Object> responseData = Map.of(
            "imageKey", imageKey,
            "hasImage", true
        );

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
            "프로필 이미지가 성공적으로 업로드되었습니다.", responseData);

        logger.info("프로필 이미지 업로드 성공 - 사용자: {}, 이미지: {}",
                   userEmail, imageKey);

        return ResponseEntity.ok(response);

    } catch (RuntimeException e) {
        logger.warn("프로필 이미지 업로드 실패: {}", e.getMessage());
        ApiResponse<Map<String, Object>> response = ApiResponse.failure(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

    } catch (Exception e) {
        logger.error("프로필 이미지 업로드 중 시스템 오류 발생: {}", e.getMessage(), e);
        ApiResponse<Map<String, Object>> response = ApiResponse.failure(
            "프로필 이미지 업로드 중 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

---

### Phase 3: API 테스트 템플릿 추가

#### 파일: `src/main/resources/static/config/api-templates.json`

```json
"profileUploadImage": {
  "name": "프로필 이미지 업로드",
  "category": "사용자 · 프로필",
  "description": "프로필 사진을 업로드하여 변경 (기존 이미지 자동 삭제)",
  "auth": true,
  "method": "POST",
  "endpoint": "/api/profile/me/upload-image",
  "params": [],
  "headers": [],
  "bodyTemplate": {
    "_note": "파일 업로드는 multipart/form-data 사용",
    "_formData": {
      "file": "(이미지 파일 선택)"
    }
  }
}
```

---

## API 명세

### 엔드포인트

```
POST /api/profile/me/upload-image
```

### 인증
- **필수**: JWT Bearer Token
- **헤더**: `Authorization: Bearer {token}`

### 요청

#### Content-Type
```
multipart/form-data
```

#### Form Data Parameters

| 필드명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| file | File | O | 업로드할 이미지 파일 (jpg, jpeg, png, gif) |

#### 파일 제약사항
- **최대 크기**: 15MB
- **허용 확장자**: jpg, jpeg, png, gif
- **MIME 타입**: image/jpeg, image/png, image/gif

### 응답

#### 성공 응답 (200 OK)

```json
{
  "success": true,
  "message": "프로필 이미지가 성공적으로 업로드되었습니다.",
  "data": {
    "imageKey": "profile_123_1735789012345.jpg",
    "hasImage": true
  },
  "timestamp": "2025-10-21T10:30:00.000+00:00"
}
```

#### 실패 응답

##### 1. 파일 검증 실패 (400 Bad Request)
```json
{
  "success": false,
  "message": "허용되지 않는 파일 형식입니다. (jpg, jpeg, png, gif만 가능)",
  "data": null,
  "timestamp": "2025-10-21T10:30:00.000+00:00"
}
```

##### 2. 파일 크기 초과 (400 Bad Request)
```json
{
  "success": false,
  "message": "파일 크기는 15MB를 초과할 수 없습니다.",
  "data": null,
  "timestamp": "2025-10-21T10:30:00.000+00:00"
}
```

##### 3. 인증 실패 (401 Unauthorized)
```json
{
  "success": false,
  "message": "인증 토큰이 필요합니다.",
  "data": null,
  "timestamp": "2025-10-21T10:30:00.000+00:00"
}
```

##### 4. 사용자 없음 (404 Not Found)
```json
{
  "success": false,
  "message": "사용자를 찾을 수 없습니다.",
  "data": null,
  "timestamp": "2025-10-21T10:30:00.000+00:00"
}
```

##### 5. 서버 오류 (500 Internal Server Error)
```json
{
  "success": false,
  "message": "프로필 이미지 업로드 중 오류가 발생했습니다.",
  "data": null,
  "timestamp": "2025-10-21T10:30:00.000+00:00"
}
```

---

## 구현 체크리스트

### Phase 1: Service Layer ✅

- [ ] `UserTblService.java`에 `updateProfileImage()` 메서드 추가
- [ ] `validateImageFile()` 메서드 구현 (파일 검증 로직)
- [ ] `generateProfileImageKey()` 메서드 구현 (파일명 생성)
- [ ] `getFileExtension()` 메서드 구현 (확장자 추출)
- [ ] `deleteOldProfileImage()` 메서드 구현 (기존 이미지 삭제)
- [ ] `@Transactional` 어노테이션 추가
- [ ] 예외 처리 및 로깅 추가

### Phase 2: Controller Layer ✅

- [ ] `ProfileController.java`에 `uploadProfileImage()` 엔드포인트 추가
- [ ] `@PostMapping("/me/upload-image")` 매핑
- [ ] `@RequestParam("file") MultipartFile` 파라미터 처리
- [ ] JWT 토큰 추출 및 검증 (`extractUserEmailFromToken()` 재사용)
- [ ] 서비스 계층 호출
- [ ] `ApiResponse<Map<String, Object>>` 응답 생성
- [ ] 예외 처리 (RuntimeException, Exception)
- [ ] 로깅 추가 (info, warn, error)

### Phase 3: API 템플릿 ✅

- [ ] `api-templates.json`에 `profileUploadImage` 엔트리 추가
- [ ] category, description, endpoint 정확히 기재
- [ ] multipart/form-data 사용 안내 추가

### Phase 4: 빌드 및 배포 ✅

- [ ] 로컬 빌드: `mvn clean install -DskipTests`
- [ ] 컴파일 에러 없음 확인
- [ ] WAR 파일 생성 확인
- [ ] 서버 배포
- [ ] Tomcat 재시작

### Phase 5: 테스트 ✅

- [ ] `/status` 페이지에서 API 템플릿 확인
- [ ] JWT 토큰 발급 (로그인 API 사용)
- [ ] 이미지 파일 업로드 테스트 (jpg)
- [ ] 이미지 파일 업로드 테스트 (png)
- [ ] 파일 크기 초과 테스트 (15MB 이상)
- [ ] 잘못된 파일 형식 테스트 (txt, pdf 등)
- [ ] MinIO에 파일 업로드 확인
- [ ] DB에 `profileImageKey` 업데이트 확인
- [ ] 기존 이미지 삭제 확인 (2회 업로드 시)
- [ ] 프로필 조회 API로 새 이미지 확인

---

## 테스트 계획

### 1. 정상 시나리오 테스트

#### 테스트 케이스 1: 첫 프로필 이미지 업로드
**전제조건:**
- 사용자 로그인 완료 (JWT 토큰 보유)
- 기존 프로필 이미지 없음 (`profileImageKey = null`)

**테스트 순서:**
1. POST `/api/auth/login` - JWT 토큰 발급
2. POST `/api/profile/me/upload-image` - 이미지 업로드 (test.jpg, 2MB)
3. POST `/api/profile/me` - 프로필 조회로 `imageKey` 확인

**예상 결과:**
- 업로드 성공 (200 OK)
- `imageKey`: `profile_{userIdx}_{timestamp}.jpg` 형식
- MinIO에 파일 존재 확인
- DB에 `profileImageKey` 저장 확인

---

#### 테스트 케이스 2: 기존 이미지 교체
**전제조건:**
- 기존 프로필 이미지 존재 (`profile_123_1234567890.jpg`)

**테스트 순서:**
1. POST `/api/profile/me/upload-image` - 새 이미지 업로드 (new.png, 3MB)
2. POST `/api/profile/me` - 프로필 조회로 새 `imageKey` 확인
3. MinIO에서 기존 이미지 삭제 확인

**예상 결과:**
- 업로드 성공 (200 OK)
- 새 `imageKey`: `profile_123_{new_timestamp}.png`
- MinIO에서 `profile_123_1234567890.jpg` 삭제됨
- MinIO에 `profile_123_{new_timestamp}.png` 존재

---

### 2. 예외 시나리오 테스트

#### 테스트 케이스 3: 파일 크기 초과
**테스트 순서:**
1. POST `/api/profile/me/upload-image` - 대용량 파일 업로드 (large.jpg, 20MB)

**예상 결과:**
- 실패 (400 Bad Request)
- 메시지: "파일 크기는 15MB를 초과할 수 없습니다."

---

#### 테스트 케이스 4: 잘못된 파일 형식
**테스트 순서:**
1. POST `/api/profile/me/upload-image` - 비이미지 파일 업로드 (document.pdf)

**예상 결과:**
- 실패 (400 Bad Request)
- 메시지: "허용되지 않는 파일 형식입니다. (jpg, jpeg, png, gif만 가능)"

---

#### 테스트 케이스 5: 인증 토큰 없음
**테스트 순서:**
1. POST `/api/profile/me/upload-image` - Authorization 헤더 없이 요청

**예상 결과:**
- 실패 (401 Unauthorized)
- 메시지: "인증 토큰이 필요합니다."

---

#### 테스트 케이스 6: 빈 파일 업로드
**테스트 순서:**
1. POST `/api/profile/me/upload-image` - 빈 파일 (0 bytes) 업로드

**예상 결과:**
- 실패 (400 Bad Request)
- 메시지: "업로드할 파일이 없습니다."

---

### 3. 통합 테스트 시나리오

#### 시나리오: 프로필 이미지 전체 플로우
```
1. 회원가입 (POST /api/auth/register)
2. 로그인 (POST /api/auth/login) → JWT 토큰 획득
3. 프로필 조회 (POST /api/profile/me) → imageKey 없음 확인
4. 프로필 이미지 업로드 (POST /api/profile/me/upload-image) → 첫 이미지 업로드
5. 프로필 조회 (POST /api/profile/me) → imageKey 존재 확인
6. 프로필 이미지 다운로드 (POST /api/profile/me/image/file) → 이미지 표시
7. 프로필 이미지 재업로드 (POST /api/profile/me/upload-image) → 이미지 교체
8. 프로필 조회 (POST /api/profile/me) → 새 imageKey 확인
9. MinIO 확인 → 이전 이미지 삭제, 새 이미지만 존재
```

---

## 구현 시 주의사항

### 1. 보안
- ✅ JWT 토큰 검증 필수
- ✅ 본인의 프로필만 수정 가능 (userEmail 기반)
- ✅ 파일 타입 검증 (확장자 + MIME 타입)
- ⚠️ 파일 내용 검증 (악성 파일 업로드 방지) - 필요 시 추가

### 2. 성능
- ✅ 트랜잭션 범위 최소화 (파일 업로드는 트랜잭션 외부)
- ✅ 기존 이미지 삭제는 비동기 처리 가능 (옵션)
- ⚠️ 대용량 파일 처리 시 타임아웃 설정 확인

### 3. 에러 처리
- ✅ MinIO 업로드 실패 시 롤백
- ✅ DB 업데이트 실패 시 @Transactional 자동 롤백
- ✅ 기존 이미지 삭제 실패는 로그만 기록 (서비스 진행)

### 4. 로깅
- ✅ 모든 주요 단계 로깅 (업로드 시작, 성공, 실패)
- ✅ 사용자 정보 로깅 (감사 추적)
- ⚠️ 민감 정보 로깅 금지 (토큰, 비밀번호 등)

### 5. 테스트
- ⚠️ 로컬 환경에서는 MinIO 연결 불가 (서버 환경에서만 테스트)
- ✅ `/status` 페이지의 API Tester 활용
- ✅ 브라우저 개발자 도구로 요청/응답 확인

---

## 참고 자료

### 관련 파일 경로
- **Entity**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/entity/UserTbl.java`
- **Service**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/service/UserTblService.java`
- **Controller**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/controller/ProfileController.java`
- **Utility**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/util/MinIOFileUtil.java`
- **Security**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/config/SecurityConfig.java`
- **Properties**: `backend/BlueCrab/src/main/resources/application.properties`
- **API Template**: `backend/BlueCrab/src/main/resources/static/config/api-templates.json`

### 참고 코드
- **MultipartFile 업로드 패턴**: `BoardAttachmentUploadController.java` (line 58-98)
- **JWT 토큰 추출**: `ProfileController.java` (line 209-230)
- **MinIO 파일 업로드**: `MinIOFileUtil.java` (line 53-78)
- **MinIO 파일 삭제**: `MinIOFileUtil.java` (line 86-103)

---

## 버전 히스토리

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2025-10-21 | Claude AI | 초안 작성 - 프로필 이미지 업로드 API 구현 계획 |

---

**문서 끝**
