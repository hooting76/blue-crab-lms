-- ===================================================================
-- 🔥 긴급: CS101 중복 레코드 제거
-- 문제: POST /api/enrollments/enroll 500 에러
-- 원인: LEC_TBL에 LEC_SERIAL='CS101'이 2개 존재
-- ===================================================================

-- 1단계: 현재 상황 확인
SELECT 
    LEC_IDX,
    LEC_SERIAL,
    LEC_TIT,
    LEC_PROF,
    LEC_MANY as MAX_STUDENTS,
    LEC_CURRENT as CURRENT_STUDENTS,
    LEC_REG as REGISTERED_DATE
FROM LEC_TBL
WHERE LEC_SERIAL = 'CS101'
ORDER BY LEC_REG;

-- 2단계: 각 CS101 레코드의 수강생 수 확인
SELECT 
    l.LEC_IDX,
    l.LEC_SERIAL,
    l.LEC_TIT,
    COUNT(e.ENROLLMENT_IDX) as ENROLLMENT_COUNT
FROM LEC_TBL l
LEFT JOIN ENROLLMENT_EXTENDED_TBL e ON l.LEC_IDX = e.LEC_IDX
WHERE l.LEC_SERIAL = 'CS101'
GROUP BY l.LEC_IDX, l.LEC_SERIAL, l.LEC_TIT
ORDER BY ENROLLMENT_COUNT DESC;

-- 3단계: 중복 제거 (수강생이 없는 레코드 삭제)
-- ⚠️ 주의: 먼저 위의 2단계 결과를 확인하고,
--         ENROLLMENT_COUNT가 0인 LEC_IDX를 아래에 입력하세요!

-- 예시: LEC_IDX=XX가 수강생 0명이라면
-- DELETE FROM LEC_TBL WHERE LEC_IDX = XX AND LEC_SERIAL = 'CS101';

-- 또는 수강생이 적은 쪽을 삭제하려면:
/*
DELETE FROM LEC_TBL 
WHERE LEC_IDX IN (
    SELECT LEC_IDX FROM (
        SELECT l.LEC_IDX, COUNT(e.ENROLLMENT_IDX) as cnt
        FROM LEC_TBL l
        LEFT JOIN ENROLLMENT_EXTENDED_TBL e ON l.LEC_IDX = e.LEC_IDX
        WHERE l.LEC_SERIAL = 'CS101'
        GROUP BY l.LEC_IDX
        ORDER BY cnt ASC
        LIMIT 1
    ) AS subquery
);
*/

-- 4단계: 삭제 후 확인 (정확히 1개만 남아야 함)
SELECT 
    LEC_IDX,
    LEC_SERIAL,
    LEC_TIT,
    LEC_PROF
FROM LEC_TBL
WHERE LEC_SERIAL = 'CS101';

-- 5단계: UNIQUE 제약조건 추가 (재발 방지)
-- ⚠️ 다른 LEC_SERIAL도 중복이 없는지 확인 후 실행
SELECT LEC_SERIAL, COUNT(*) as DUPLICATE_COUNT
FROM LEC_TBL
GROUP BY LEC_SERIAL
HAVING COUNT(*) > 1;

-- 중복이 CS101만 있었다면:
-- ALTER TABLE LEC_TBL ADD UNIQUE INDEX idx_lec_serial_unique (LEC_SERIAL);
