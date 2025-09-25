# 🖼️ MinIO 프로필 이미지 업로드 가이드

## 📁 파일 준비

### 1. 필요한 테스트 이미지 파일들

다음 파일명으로 이미지를 준비하세요:

```
STU001_20250925173216.jpg    (학생001 - 김학생)
STU002_20250925173300.png    (학생002 - 이학생)  
PRF001_20250925173400.jpg    (교수001 - 박교수)
```

### 2. 파일 규격 권장사항
- **크기**: 500x500px ~ 1000x1000px
- **용량**: 500KB 이하
- **형식**: JPG, PNG, WebP
- **비율**: 1:1 (정사각형) 권장

## 🚀 MinIO 업로드 방법

### 방법 1: MinIO 웹 콘솔 사용

1. **MinIO 웹 콘솔 접속**
   ```
   URL: http://localhost:9001 (또는 설정된 주소)
   ```

2. **로그인**
   - Access Key: `minioadmin` (또는 설정값)
   - Secret Key: `minioadmin` (또는 설정값)

3. **버킷 선택**
   - 좌측 메뉴에서 `profile-img` 버킷 클릭

4. **파일 업로드**
   - `Upload` 버튼 클릭
   - 준비한 이미지 파일들을 선택하여 업로드
   - 업로드 완료 후 파일 목록에서 확인

### 방법 2: MinIO CLI 사용 (mc 명령어)

```bash
# MinIO 클라이언트 설정
mc alias set local http://localhost:9000 minioadmin minioadmin

# 파일 업로드
mc cp STU001_20250925173216.jpg local/profile-img/
mc cp STU002_20250925173300.png local/profile-img/
mc cp PRF001_20250925173400.jpg local/profile-img/

# 업로드 확인
mc ls local/profile-img/
```

### 방법 3: curl 명령어 사용

```bash
# Presigned URL 생성 후 PUT 요청
curl -X PUT "http://localhost:9000/profile-img/STU001_20250925173216.jpg" \
     -H "Authorization: AWS4-HMAC-SHA256 ..." \
     --data-binary @STU001_20250925173216.jpg
```

## 🔍 업로드 확인

### MinIO 웹 콘솔에서 확인
1. `profile-img` 버킷 내부 확인
2. 파일 목록에 다음 파일들이 있는지 확인:
   - `STU001_20250925173216.jpg`
   - `STU002_20250925173300.png`
   - `PRF001_20250925173400.jpg`

### API 테스트로 확인
```javascript
// 브라우저 개발자도구에서 실행
// 1. 토큰 설정 (로그인 후)
setAuthToken("your_jwt_token_here");

// 2. 프로필 조회
testGetMyProfile();

// 3. 이미지 URL 확인
testGetProfileImage();
```

## 📂 파일 구조 예시

업로드 완료 후 MinIO 구조:
```
profile-img/
├── STU001_20250925173216.jpg (500KB, 800x800)
├── STU002_20250925173300.png (300KB, 600x600)  
└── PRF001_20250925173400.jpg (400KB, 1000x1000)
```

## 🧪 테스트 시나리오

### 1. 정상적인 이미지가 있는 사용자
- **이메일**: student001@bluecrab.ac.kr
- **이미지**: STU001_20250925173216.jpg
- **예상 결과**: 이미지 URL 정상 반환

### 2. 다른 확장자 이미지
- **이메일**: student002@bluecrab.ac.kr  
- **이미지**: STU002_20250925173300.png
- **예상 결과**: PNG 이미지 정상 표시

### 3. 교수 계정
- **이메일**: professor001@bluecrab.ac.kr
- **이미지**: PRF001_20250925173400.jpg
- **예상 결과**: 교수 프로필 이미지 정상 표시

### 4. 이미지가 없는 사용자
- **이메일**: incomplete@bluecrab.ac.kr
- **이미지**: NULL
- **예상 결과**: hasImage: false

## 🛠️ 문제 해결

### 업로드 실패 시
1. **권한 확인**: MinIO 접근 권한 확인
2. **네트워크**: MinIO 서버 연결 상태 확인
3. **파일명**: 정확한 파일명 규칙 준수 확인
4. **용량**: 파일 크기 제한 확인

### API 테스트 실패 시
1. **DB 확인**: PROFILE_IMAGE_KEY 값이 정확히 설정되었는지 확인
2. **토큰 확인**: 유효한 JWT 토큰인지 확인
3. **사용자 매칭**: 로그인한 사용자와 이미지 소유자 일치 확인

## 📊 검증 쿼리

```sql
-- 이미지 키 설정 확인
SELECT 
    USER_EMAIL,
    USER_NAME, 
    PROFILE_IMAGE_KEY,
    CASE 
        WHEN PROFILE_IMAGE_KEY IS NOT NULL THEN '이미지 있음'
        ELSE '이미지 없음'
    END as IMAGE_STATUS
FROM USER_TBL 
WHERE USER_EMAIL LIKE '%@bluecrab.ac.kr'
ORDER BY USER_EMAIL;

-- PROFILE_VIEW 통합 확인
SELECT 
    user_email,
    user_name,
    user_type,
    profile_image_key,
    CONCAT('(', zip_code, ') ', main_address, ' ', detail_address) as full_address
FROM PROFILE_VIEW 
WHERE user_email LIKE '%@bluecrab.ac.kr';
```

이제 다음 순서로 진행하세요:

1. 📊 **SQL 스크립트 실행** (`test_profile_data.sql`)
2. 🖼️ **이미지 파일 준비 및 업로드** (위 가이드 참조)
3. 🧪 **API 테스트** (브라우저 개발자도구 콘솔)
4. 🔍 **결과 확인** (프로필 정보 및 이미지 표시)