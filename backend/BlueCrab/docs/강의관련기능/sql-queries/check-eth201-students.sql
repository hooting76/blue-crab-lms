-- ===================================================================
-- ETH201 강의 수강생 확인
-- ===================================================================

-- 1. ETH201 강의 정보 확인
SELECT 
    LEC_IDX,
    LEC_SERIAL,
    LEC_TIT,
    LEC_PROF,
    LEC_CURRENT as '현재수강생수'
FROM LEC_TBL
WHERE LEC_SERIAL = 'ETH201';

-- 2. ETH201에 실제 수강생이 있는지 확인
SELECT 
    e.ENROLLMENT_IDX,
    e.LEC_IDX,
    e.STUDENT_IDX,
    u.USER_NAME as '학생이름',
    u.USER_CODE as '학번',
    l.LEC_SERIAL,
    l.LEC_TIT as '강의명'
FROM ENROLLMENT_EXTENDED_TBL e
INNER JOIN LEC_TBL l ON e.LEC_IDX = l.LEC_IDX
INNER JOIN USER_TBL u ON e.STUDENT_IDX = u.USER_IDX
WHERE l.LEC_SERIAL = 'ETH201';

-- 3. 전체 강의별 수강생 현황
SELECT 
    l.LEC_SERIAL as '강의코드',
    l.LEC_TIT as '강의명',
    COUNT(e.ENROLLMENT_IDX) as '수강생수'
FROM LEC_TBL l
LEFT JOIN ENROLLMENT_EXTENDED_TBL e ON l.LEC_IDX = e.LEC_IDX
GROUP BY l.LEC_IDX, l.LEC_SERIAL, l.LEC_TIT
HAVING COUNT(e.ENROLLMENT_IDX) > 0
ORDER BY COUNT(e.ENROLLMENT_IDX) DESC;

-- 4. ETH201에 테스트 수강생 추가 (필요한 경우)
-- ⚠️ 먼저 위의 쿼리로 LEC_IDX와 STUDENT_IDX를 확인하세요!
/*
INSERT INTO ENROLLMENT_EXTENDED_TBL (LEC_IDX, STUDENT_IDX, ENROLLMENT_DATA)
VALUES (
    (SELECT LEC_IDX FROM LEC_TBL WHERE LEC_SERIAL = 'ETH201'),
    6,  -- STUDENT_IDX (실제 학생 IDX로 변경)
    '{"enrollment": {"enrollmentDate": "2025-10-23", "status": "ENROLLED"}}'
);
*/
