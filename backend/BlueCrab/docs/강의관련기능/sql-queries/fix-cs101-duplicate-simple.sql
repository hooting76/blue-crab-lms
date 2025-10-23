-- ===================================================================
-- 🔥 CS101 중복 레코드 제거 (간단 버전)
-- HeidiSQL에서 바로 실행 가능
-- ===================================================================

-- 📊 1단계: CS101 중복 확인
SELECT 
    LEC_IDX,
    LEC_SERIAL,
    LEC_TIT,
    LEC_PROF,
    LEC_CURRENT as '현재수강생',
    LEC_MANY as '정원',
    LEC_REG as '등록일'
FROM LEC_TBL
WHERE LEC_SERIAL = 'CS101'
ORDER BY LEC_REG;

-- 📊 2단계: 각 레코드의 실제 수강생 수 확인
SELECT 
    l.LEC_IDX,
    l.LEC_SERIAL,
    l.LEC_TIT,
    COUNT(e.ENROLLMENT_IDX) as '실제수강생수'
FROM LEC_TBL l
LEFT JOIN ENROLLMENT_EXTENDED_TBL e ON l.LEC_IDX = e.LEC_IDX
WHERE l.LEC_SERIAL = 'CS101'
GROUP BY l.LEC_IDX, l.LEC_SERIAL, l.LEC_TIT;

-- ⚠️ 위 결과를 보고 삭제할 LEC_IDX를 확인하세요!
-- 보통 '실제수강생수'가 0인 레코드를 삭제합니다.

-- 🗑️ 3단계: 중복 레코드 삭제 (LEC_IDX를 확인 후 실행)
-- DELETE FROM LEC_TBL WHERE LEC_IDX = [수강생이 0명인 LEC_IDX] AND LEC_SERIAL = 'CS101';

-- 예시:
-- DELETE FROM LEC_TBL WHERE LEC_IDX = 5 AND LEC_SERIAL = 'CS101';

-- ✅ 4단계: 삭제 확인 (1개만 남아야 함)
SELECT COUNT(*) as '남은개수' FROM LEC_TBL WHERE LEC_SERIAL = 'CS101';

-- ✅ 5단계: 최종 확인
SELECT 
    LEC_IDX,
    LEC_SERIAL,
    LEC_TIT,
    LEC_PROF
FROM LEC_TBL
WHERE LEC_SERIAL = 'CS101';
