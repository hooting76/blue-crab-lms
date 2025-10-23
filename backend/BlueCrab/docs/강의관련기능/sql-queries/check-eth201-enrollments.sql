-- ===================================================================
-- ETH201 강의 수강생 확인 SQL
-- ===================================================================

-- 1. ETH201 강의 정보 확인
SELECT 
    LEC_IDX,
    LEC_SERIAL,
    LEC_TIT,
    LEC_PROF,
    CREATED_AT
FROM LEC_TBL
WHERE LEC_SERIAL = 'ETH201';

-- 2. ETH201 강의 수강생 목록 확인
SELECT 
    e.ENROLLMENT_IDX,
    e.LEC_IDX,
    e.STUDENT_IDX,
    e.ENROLLMENT_DATA,
    l.LEC_SERIAL,
    l.LEC_TIT,
    u.USER_NAME as STUDENT_NAME,
    u.USER_CODE as STUDENT_CODE
FROM ENROLLMENT_EXTENDED_TBL e
INNER JOIN LEC_TBL l ON e.LEC_IDX = l.LEC_IDX
INNER JOIN USER_TBL u ON e.STUDENT_IDX = u.USER_IDX
WHERE l.LEC_SERIAL = 'ETH201'
ORDER BY e.ENROLLMENT_IDX;

-- 3. 전체 강의별 수강생 수 통계
SELECT 
    l.LEC_SERIAL,
    l.LEC_TIT,
    COUNT(e.ENROLLMENT_IDX) as ENROLLMENT_COUNT
FROM LEC_TBL l
LEFT JOIN ENROLLMENT_EXTENDED_TBL e ON l.LEC_IDX = e.LEC_IDX
GROUP BY l.LEC_IDX, l.LEC_SERIAL, l.LEC_TIT
ORDER BY l.LEC_SERIAL;

-- 4. 수강생이 없는 강의 찾기
SELECT 
    l.LEC_SERIAL,
    l.LEC_TIT,
    l.LEC_PROF
FROM LEC_TBL l
LEFT JOIN ENROLLMENT_EXTENDED_TBL e ON l.LEC_IDX = e.LEC_IDX
WHERE e.ENROLLMENT_IDX IS NULL;

-- 5. ETH201에 수강생 추가 (필요한 경우)
-- ⚠️ 실제 STUDENT_IDX, LEC_IDX 값을 확인한 후 실행하세요!
/*
INSERT INTO ENROLLMENT_EXTENDED_TBL (
    LEC_IDX,
    STUDENT_IDX,
    ENROLLMENT_DATA
) VALUES (
    (SELECT LEC_IDX FROM LEC_TBL WHERE LEC_SERIAL = 'ETH201'),
    25,  -- 교수 계정 IDX (실제 학생 IDX로 변경 필요)
    '{"enrollment": {"enrollmentDate": "2025-10-23", "status": "active"}}'
);
*/
