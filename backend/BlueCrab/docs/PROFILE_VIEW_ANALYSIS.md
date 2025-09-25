# 📊 PROFILE_VIEW 분석 및 데이터 입력 가이드

## 🔍 뷰 테이블 구조 분석

### PROFILE_VIEW 컬럼 매핑

| 뷰 컬럼명 | 원본 테이블.컬럼 | 데이터 타입 | 설명 |
|----------|----------------|------------|------|
| user_email | USER_TBL.USER_EMAIL | VARCHAR(200) | 사용자 이메일 (PK) |
| user_name | USER_TBL.USER_NAME | VARCHAR(50) | 사용자 이름 |
| user_phone | USER_TBL.USER_PHONE | CHAR(11) | 전화번호 |
| user_type | USER_TBL.USER_STUDENT | INTEGER | 사용자 타입 (0:교수, 1:학생) |
| major_code | USER_TBL.USER_CODE | INTEGER | 전공 코드 |
| zip_code | USER_TBL.USER_ZIP | VARCHAR(5) | 우편번호 (5자리 패딩) |
| main_address | USER_TBL.USER_FIRST_ADD | VARCHAR(200) | 기본 주소 |
| detail_address | USER_TBL.USER_LAST_ADD | VARCHAR(100) | 상세 주소 |
| **profile_image_key** | **USER_TBL.PROFILE_IMAGE_KEY** | **VARCHAR** | **프로필 이미지 키** |
| birth_date | USER_TBL.USER_BIRTH | VARCHAR(100) | 생년월일 |
| academic_status | REGIST_TABLE.STD_STAT | VARCHAR | 학적 상태 |
| admission_route | REGIST_TABLE.JOIN_PATH | VARCHAR | 입학 경로 |
| major_faculty_code | FACULTY.faculty_code | VARCHAR | 주전공 대학 코드 |
| major_dept_code | DEPARTMENT.dept_code | VARCHAR | 주전공 학과 코드 |
| minor_faculty_code | FACULTY.faculty_code | VARCHAR | 부전공 대학 코드 |
| minor_dept_code | DEPARTMENT.dept_code | VARCHAR | 부전공 학과 코드 |

## 🏠 프로필 주소 처리 방식

### 주소 구성 요소
```
완전한 주소 = (우편번호) + 기본주소 + 상세주소
```

### 1. DB 저장 형태
- **zip_code**: `LPAD(COALESCE(u.USER_ZIP, 0), 5, '0')` → 5자리 패딩
- **main_address**: 기본 주소 (도로명/지번 주소)
- **detail_address**: 상세 주소 (동/호수 등)

### 2. Java 처리 (ProfileView.getFullAddress())
```java
public String getFullAddress() {
    StringBuilder address = new StringBuilder();
    
    // 우편번호 (00000이 아닌 경우에만 표시)
    if (zipCode != null && !zipCode.trim().isEmpty() && !"00000".equals(zipCode)) {
        address.append("(").append(zipCode).append(") ");
    }
    
    // 기본 주소
    if (mainAddress != null && !mainAddress.trim().isEmpty()) {
        address.append(mainAddress);
    }
    
    // 상세 주소
    if (detailAddress != null && !detailAddress.trim().isEmpty()) {
        address.append(" ").append(detailAddress);
    }
    
    return address.toString().trim();
}
```

### 3. 주소 입력 예시
```sql
-- 완전한 주소가 있는 경우
USER_ZIP = 12345
USER_FIRST_ADD = '서울특별시 강남구 테헤란로 123'
USER_LAST_ADD = '101동 502호'
-- 결과: "(12345) 서울특별시 강남구 테헤란로 123 101동 502호"

-- 우편번호가 없는 경우
USER_ZIP = NULL 또는 0
USER_FIRST_ADD = '서울특별시 강남구 테헤란로 123'
USER_LAST_ADD = '101동 502호'
-- 결과: "서울특별시 강남구 테헤란로 123 101동 502호"
```

## 🖼️ 프로필 이미지 수동 추가 방법

### 1. 파일명 규칙
```
형식: {userCode}_{timestamp}.{extension}
예시: STU001_20250925173216.jpg
```

### 2. userCode 생성 규칙
- **학생**: `STU` + 3자리 숫자 (STU001, STU002, ...)
- **교수**: `PRF` + 3자리 숫자 (PRF001, PRF002, ...)
- **관리자**: `ADM` + 3자리 숫자 (ADM001, ADM002, ...)

### 3. timestamp 형식
- **형식**: `yyyyMMddHHmmss` (14자리)
- **예시**: `20250925173216` (2025년 9월 25일 17시 32분 16초)

### 4. 단계별 수동 추가 과정

#### Step 1: 이미지 파일명 생성
```javascript
// JavaScript로 현재 시간 기준 파일명 생성
function generateImageKey(userType, userNumber) {
    const now = new Date();
    const timestamp = now.toISOString()
        .replace(/[-:T]/g, '')
        .substring(0, 14);
    
    let prefix;
    switch(userType) {
        case 'student': prefix = 'STU'; break;
        case 'professor': prefix = 'PRF'; break;
        case 'admin': prefix = 'ADM'; break;
        default: prefix = 'STU';
    }
    
    const userCode = prefix + String(userNumber).padStart(3, '0');
    return `${userCode}_${timestamp}.jpg`;
}

// 사용 예시
const imageKey = generateImageKey('student', 1); 
// 결과: STU001_20250925173216.jpg
```

#### Step 2: MinIO 버킷에 이미지 업로드
1. MinIO 웹 콘솔 접속: `http://localhost:9001`
2. `profile-img` 버킷 선택
3. 파일 업로드: 생성한 파일명으로 이미지 업로드
4. 경로: `/profile-img/{imageKey}`

#### Step 3: DB 업데이트
```sql
-- USER_TBL에 PROFILE_IMAGE_KEY 컬럼이 없다면 추가
ALTER TABLE USER_TBL ADD COLUMN PROFILE_IMAGE_KEY VARCHAR(100);

-- 특정 사용자에게 이미지 키 설정
UPDATE USER_TBL 
SET PROFILE_IMAGE_KEY = 'STU001_20250925173216.jpg'
WHERE USER_EMAIL = 'test@student.com';

-- 여러 사용자에게 한 번에 설정
UPDATE USER_TBL 
SET PROFILE_IMAGE_KEY = CASE 
    WHEN USER_EMAIL = 'student1@test.com' THEN 'STU001_20250925173216.jpg'
    WHEN USER_EMAIL = 'student2@test.com' THEN 'STU002_20250925173217.jpg'
    WHEN USER_EMAIL = 'professor@test.com' THEN 'PRF001_20250925173218.jpg'
    ELSE PROFILE_IMAGE_KEY
END
WHERE USER_EMAIL IN ('student1@test.com', 'student2@test.com', 'professor@test.com');
```

## 🧪 테스트 데이터 샘플

### 학생 데이터 예시
```sql
-- 학생 사용자 생성 (기본 정보)
INSERT INTO USER_TBL (
    USER_EMAIL, USER_PW, USER_NAME, USER_PHONE, USER_BIRTH, 
    USER_STUDENT, USER_ZIP, USER_FIRST_ADD, USER_LAST_ADD,
    PROFILE_IMAGE_KEY
) VALUES (
    'student001@bluecrab.ac.kr',
    '$2a$10$encrypted_password_hash',
    '김학생',
    '01012345678',
    '20000315',
    1,  -- 학생
    12345,
    '서울특별시 강남구 테헤란로 123',
    '101동 502호',
    'STU001_20250925173216.jpg'
);

-- 학적 정보 추가
INSERT INTO REGIST_TABLE (USER_IDX, STD_STAT, JOIN_PATH)
SELECT USER_IDX, '재학', '정시전형'
FROM USER_TBL WHERE USER_EMAIL = 'student001@bluecrab.ac.kr';
```

### 교수 데이터 예시
```sql
-- 교수 사용자 생성
INSERT INTO USER_TBL (
    USER_EMAIL, USER_PW, USER_NAME, USER_PHONE, USER_BIRTH, 
    USER_STUDENT, USER_ZIP, USER_FIRST_ADD, USER_LAST_ADD,
    PROFILE_IMAGE_KEY
) VALUES (
    'professor001@bluecrab.ac.kr',
    '$2a$10$encrypted_password_hash',
    '박교수',
    '01087654321',
    '19750820',
    0,  -- 교수
    54321,
    '서울특별시 서초구 강남대로 456',
    '202호',
    'PRF001_20250925173300.jpg'
);
```

## 🔧 검증 쿼리

### 프로필 뷰 데이터 확인
```sql
-- 특정 사용자 프로필 조회
SELECT * FROM PROFILE_VIEW 
WHERE user_email = 'student001@bluecrab.ac.kr';

-- 이미지가 있는 사용자들 조회
SELECT user_email, user_name, profile_image_key 
FROM PROFILE_VIEW 
WHERE profile_image_key IS NOT NULL;

-- 주소 정보 확인
SELECT 
    user_email, 
    user_name,
    CONCAT('(', zip_code, ') ', main_address, ' ', detail_address) as full_address
FROM PROFILE_VIEW 
WHERE main_address IS NOT NULL;
```

## 🚀 API 테스트 준비

위 데이터가 준비되면 다음 API들을 테스트할 수 있습니다:

1. **GET /api/profile/me** - 프로필 정보 조회
2. **GET /api/profile/me/completeness** - 완성도 체크
3. **GET /api/profile/me/image** - 이미지 URL 조회
4. **GET /api/profile/me/image/{imageKey}** - 이미지 파일 직접 조회

## 📝 주의사항

1. **PROFILE_IMAGE_KEY 컬럼**: USER_TBL에 해당 컬럼이 없다면 추가 필요
2. **MinIO 경로**: `/profile-img/` 버킷 하위에 파일 업로드
3. **파일명 규칙**: 반드시 `{userCode}_{timestamp}.{extension}` 형식 준수
4. **charset 이슈**: COLLATE utf8mb4_general_ci 설정 확인
5. **이미지 확장자**: jpg, png, webp, gif 지원