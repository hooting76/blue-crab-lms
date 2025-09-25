-- ================================================================
-- Blue Crab LMS 프로필 테스트 데이터 생성 스크립트
-- ================================================================

-- 1. USER_TBL에 PROFILE_IMAGE_KEY 컬럼 추가 (없는 경우)
-- ALTER TABLE USER_TBL ADD COLUMN PROFILE_IMAGE_KEY VARCHAR(100);

-- 2. 테스트용 사용자 데이터 생성
-- ================================================================

-- 학생 사용자 1
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
    'student001@bluecrab.ac.kr',
    '$2a$10$N3AR8Zn7wq1nh0RwCXu3COgR0D8F1qX9A2hJiKlM3nOeZh7YbRg0m', -- password: test123
    '김학생',
    '01012345678',
    '20000315',
    1,  -- 학생
    12345,
    12345,
    '서울특별시 강남구 테헤란로 123',
    '101동 502호',
    'STU001_20250925173216.jpg',
    NOW()
);

-- 학생 사용자 2  
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
    'student002@bluecrab.ac.kr',
    '$2a$10$N3AR8Zn7wq1nh0RwCXu3COgR0D8F1qX9A2hJiKlM3nOeZh7YbRg0m', -- password: test123
    '이학생',
    '01023456789',
    '19990520',
    1,  -- 학생
    23456,
    23456,
    '서울특별시 서초구 강남대로 456', 
    '201동 1203호',
    'STU002_20250925173300.png',
    NOW()
);

-- 교수 사용자
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
    'professor001@bluecrab.ac.kr',
    '$2a$10$N3AR8Zn7wq1nh0RwCXu3COgR0D8F1qX9A2hJiKlM3nOeZh7YbRg0m', -- password: test123
    '박교수',
    '01087654321',
    '19750820',
    0,  -- 교수
    34567,
    54321,
    '서울특별시 마포구 월드컵로 789',
    '교수연구동 301호',
    'PRF001_20250925173400.jpg',
    NOW()
);

-- 이미지가 없는 학생 (완성도 테스트용)
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
    'incomplete@bluecrab.ac.kr',
    '$2a$10$N3AR8Zn7wq1nh0RwCXu3COgR0D8F1qX9A2hJiKlM3nOeZh7YbRg0m', -- password: test123
    '미완성',
    NULL,  -- 전화번호 없음 (완성도 테스트)
    '19980101',
    1,  -- 학생
    45678,
    NULL,  -- 주소 없음
    NULL,
    NULL,
    NULL,  -- 이미지 없음
    NOW()
);

-- 3. 학적 정보 추가 (REGIST_TABLE이 있는 경우)
-- ================================================================

-- 학생001 학적 정보
INSERT INTO REGIST_TABLE (USER_IDX, STD_STAT, JOIN_PATH)
SELECT USER_IDX, '재학', '정시전형'
FROM USER_TBL WHERE USER_EMAIL = 'student001@bluecrab.ac.kr';

-- 학생002 학적 정보
INSERT INTO REGIST_TABLE (USER_IDX, STD_STAT, JOIN_PATH)
SELECT USER_IDX, '재학', '수시전형'
FROM USER_TBL WHERE USER_EMAIL = 'student002@bluecrab.ac.kr';

-- 미완성 학생 학적 정보
INSERT INTO REGIST_TABLE (USER_IDX, STD_STAT, JOIN_PATH)
SELECT USER_IDX, '휴학', NULL
FROM USER_TBL WHERE USER_EMAIL = 'incomplete@bluecrab.ac.kr';

-- 4. 데이터 확인 쿼리
-- ================================================================

-- 생성된 사용자 확인
SELECT 
    USER_IDX,
    USER_EMAIL,
    USER_NAME,
    USER_PHONE,
    USER_STUDENT,
    PROFILE_IMAGE_KEY,
    CASE 
        WHEN USER_STUDENT = 1 THEN '학생'
        WHEN USER_STUDENT = 0 THEN '교수'
        ELSE '기타'
    END as USER_TYPE_TEXT
FROM USER_TBL 
WHERE USER_EMAIL LIKE '%@bluecrab.ac.kr'
ORDER BY USER_EMAIL;

-- PROFILE_VIEW 확인 (뷰가 생성된 경우)
-- SELECT * FROM PROFILE_VIEW WHERE user_email LIKE '%@bluecrab.ac.kr';

-- 5. JWT 토큰 생성용 정보
-- ================================================================
/*
테스트용 로그인 정보:
- 이메일: student001@bluecrab.ac.kr
- 비밀번호: test123
- 사용자 타입: 학생

- 이메일: professor001@bluecrab.ac.kr  
- 비밀번호: test123
- 사용자 타입: 교수

- 이메일: incomplete@bluecrab.ac.kr
- 비밀번호: test123
- 사용자 타입: 학생 (완성도 테스트용)
*/

-- 6. MinIO 업로드할 이미지 파일 목록
-- ================================================================
/*
다음 파일들을 MinIO의 profile-img 버킷에 업로드하세요:

1. STU001_20250925173216.jpg (학생001 프로필)
2. STU002_20250925173300.png (학생002 프로필)  
3. PRF001_20250925173400.jpg (교수001 프로필)

업로드 경로: /profile-img/{파일명}
*/