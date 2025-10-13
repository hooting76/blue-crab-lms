-- ================================================================
-- Migration: 최소 사전 예약 일수 정책 추가
-- Date: 2025-10-13
-- Description: 
--   - FACILITY_POLICY_TBL에 MIN_DAYS_IN_ADVANCE 컬럼 추가
--   - "N일 이전까지만 예약 가능" 정책 구현
--   - 예: 3일 이전까지만 예약 가능 (당일/1일전/2일전 예약 불가)
-- ================================================================

-- 1. MIN_DAYS_IN_ADVANCE 컬럼 추가
ALTER TABLE FACILITY_POLICY_TBL 
ADD COLUMN MIN_DAYS_IN_ADVANCE INT NULL 
COMMENT '최소 사전 예약 일수 (NULL이면 제한없음, 0이면 당일 예약 가능, 1이면 최소 1일 전, 3이면 최소 3일 전)';

-- 2. 컬럼 설명 추가
ALTER TABLE FACILITY_POLICY_TBL 
MODIFY COLUMN MIN_DAYS_IN_ADVANCE INT NULL 
COMMENT '최소 사전 예약 일수: NULL=제한없음(즉시예약), 0=당일예약가능, 1=최소하루전, 3=최소3일전까지만예약가능';

-- 3. 기존 데이터 업데이트 예시 (필요시 사용)
-- 특정 시설에 3일 전 예약 정책 적용 예시
-- UPDATE FACILITY_POLICY_TBL 
-- SET MIN_DAYS_IN_ADVANCE = 3 
-- WHERE FACILITY_IDX = 1;

-- 4. 롤백 쿼리 (문제 발생 시 사용)
-- ALTER TABLE FACILITY_POLICY_TBL DROP COLUMN MIN_DAYS_IN_ADVANCE;

-- ================================================================
-- 테스트 데이터 예시
-- ================================================================
-- 시설 1: 3일 전까지만 예약 가능
-- UPDATE FACILITY_POLICY_TBL SET MIN_DAYS_IN_ADVANCE = 3 WHERE FACILITY_IDX = 1;

-- 시설 2: 1일 전까지만 예약 가능
-- UPDATE FACILITY_POLICY_TBL SET MIN_DAYS_IN_ADVANCE = 1 WHERE FACILITY_IDX = 2;

-- 시설 3: 당일 예약 가능
-- UPDATE FACILITY_POLICY_TBL SET MIN_DAYS_IN_ADVANCE = 0 WHERE FACILITY_IDX = 3;

-- 시설 4: 즉시 예약 가능 (제한 없음)
-- UPDATE FACILITY_POLICY_TBL SET MIN_DAYS_IN_ADVANCE = NULL WHERE FACILITY_IDX = 4;

-- ================================================================
-- 검증 쿼리
-- ================================================================
-- 컬럼 추가 확인
-- DESC FACILITY_POLICY_TBL;

-- 모든 시설의 예약 정책 확인
-- SELECT 
--     f.FACILITY_IDX,
--     f.FACILITY_NAME,
--     p.MIN_DAYS_IN_ADVANCE,
--     p.MAX_DAYS_IN_ADVANCE,
--     p.REQUIRES_APPROVAL
-- FROM FACILITY_TBL f
-- LEFT JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX
-- ORDER BY f.FACILITY_IDX;
