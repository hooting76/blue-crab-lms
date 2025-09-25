-- ================================================================
-- 현재 업로드된 이미지 파일에 맞춘 DB 업데이트
-- ================================================================

-- 현재 MinIO에 업로드된 파일: 202500101000_20250925173216.jpg

-- 테스트용 사용자 생성 (기존 데이터가 없는 경우)
INSERT INTO USER_TBL (
    USER_EMAIL, 
    USER_PW, 
    USER_NAME, 
    USER_PHONE, 
    USER_BIRTH, 
    USER_STUDENT, 
    USER_CODE,
    USER_ZIP, 
    USER_FIRST_ADD, 
    USER_LAST_ADD,
    PROFILE_IMAGE_KEY,
    USER_REG
) VALUES (
    'test@bluecrab.ac.kr',
    '$2a$10$N3AR8Zn7wq1nh0RwCXu3COgR0D8F1qX9A2hJiKlM3nOeZh7YbRg0m', -- password: test123
    '테스트사용자',
    '01012345678',
    '20000101',
    1,  -- 학생
    10100,
    12345,
    '서울특별시 강남구 테헤란로 123',
    '테스트동 123호',
    '202500101000_20250925173216.jpg',  -- 현재 업로드된 파일명
    NOW()
)
ON DUPLICATE KEY UPDATE 
    PROFILE_IMAGE_KEY = '202500101000_20250925173216.jpg';

-- 기존 사용자가 있다면 이미지 키만 업데이트
UPDATE USER_TBL 
SET PROFILE_IMAGE_KEY = '202500101000_20250925173216.jpg'
WHERE USER_EMAIL = 'test@bluecrab.ac.kr';

-- 확인 쿼리
SELECT 
    USER_EMAIL,
    USER_NAME,
    PROFILE_IMAGE_KEY,
    CASE 
        WHEN PROFILE_IMAGE_KEY IS NOT NULL THEN '✅ 이미지 있음'
        ELSE '❌ 이미지 없음'
    END as IMAGE_STATUS
FROM USER_TBL 
WHERE USER_EMAIL = 'test@bluecrab.ac.kr';