-- ===================================================================
-- 🔥 CS101 중복 해결 - LEC_IDX=1을 CS2310으로 변경
-- ===================================================================

-- 1단계: 변경 전 확인
SELECT 
    LEC_IDX,
    LEC_SERIAL,
    LEC_TIT,
    LEC_PROF
FROM LEC_TBL
WHERE LEC_IDX = 1 OR LEC_SERIAL = 'CS101';

-- 2단계: LEC_IDX=1의 LEC_SERIAL을 CS2310으로 변경
UPDATE LEC_TBL 
SET LEC_SERIAL = 'CS2310'
WHERE LEC_IDX = 1;

-- 3단계: 변경 후 확인
SELECT 
    LEC_IDX,
    LEC_SERIAL,
    LEC_TIT,
    LEC_PROF
FROM LEC_TBL
WHERE LEC_IDX = 1 OR LEC_SERIAL = 'CS101' OR LEC_SERIAL = 'CS2310';

-- 4단계: CS101이 1개만 남았는지 확인
SELECT COUNT(*) as 'CS101개수' FROM LEC_TBL WHERE LEC_SERIAL = 'CS101';
-- 결과: 1 이어야 함

-- 5단계: CS2310 확인
SELECT * FROM LEC_TBL WHERE LEC_SERIAL = 'CS2310';
