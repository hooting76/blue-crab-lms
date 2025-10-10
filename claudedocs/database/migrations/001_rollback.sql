-- =====================================================
-- 시설 예약 정책 테이블 분리 마이그레이션 - 롤백
-- =====================================================
-- 목적: 001_create_facility_policy_table.sql 실행 후 문제 발생 시
--       원래 상태로 되돌리기
--
-- 사용 시나리오:
--   - 마이그레이션 중 오류 발생
--   - 데이터 검증 실패
--   - 애플리케이션 호환성 문제 발견
--
-- 실행일: 2025-10-10
-- 작성자: Development Team
-- =====================================================

-- ⚠️ 주의사항
-- 1. 이 스크립트는 신중하게 실행해야 합니다
-- 2. 롤백 전 현재 상태를 백업하세요
-- 3. 프로덕션 환경에서는 DBA 검토 필수


-- =====================================================
-- STEP 1: 현재 상태 확인
-- =====================================================
SELECT
    '====== ROLLBACK START ======' AS status,
    NOW() AS rollback_start_time;

-- 현재 테이블 존재 확인
SELECT
    'TABLE_CHECK' AS step,
    SUM(CASE WHEN TABLE_NAME = 'FACILITY_TBL' THEN 1 ELSE 0 END) AS facility_tbl_exists,
    SUM(CASE WHEN TABLE_NAME = 'FACILITY_POLICY_TBL' THEN 1 ELSE 0 END) AS policy_tbl_exists,
    SUM(CASE WHEN TABLE_NAME = 'FACILITY_TBL_BACKUP' THEN 1 ELSE 0 END) AS backup_exists
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'blue_crab'
  AND TABLE_NAME IN ('FACILITY_TBL', 'FACILITY_POLICY_TBL', 'FACILITY_TBL_BACKUP');


-- =====================================================
-- STEP 2: FACILITY_TBL에 REQUIRES_APPROVAL 컬럼 복원
-- =====================================================
-- REQUIRES_APPROVAL 컬럼이 없는지 확인
SELECT
    'COLUMN_CHECK' AS step,
    COUNT(*) AS requires_approval_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'blue_crab'
  AND TABLE_NAME = 'FACILITY_TBL'
  AND COLUMN_NAME = 'REQUIRES_APPROVAL';
-- 결과가 0이면 컬럼 추가 필요

-- REQUIRES_APPROVAL 컬럼 추가 (삭제되었다면)
ALTER TABLE FACILITY_TBL
ADD COLUMN REQUIRES_APPROVAL TINYINT(4) NOT NULL DEFAULT 1
    COMMENT '승인 필요(1)/즉시예약(0)'
AFTER IS_ACTIVE;

-- 컬럼 추가 확인
SHOW COLUMNS FROM FACILITY_TBL LIKE 'REQUIRES_APPROVAL';


-- =====================================================
-- STEP 3: FACILITY_POLICY_TBL에서 데이터 복원
-- =====================================================
-- FACILITY_POLICY_TBL이 존재하는 경우에만 실행
UPDATE FACILITY_TBL f
INNER JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX
SET f.REQUIRES_APPROVAL = p.REQUIRES_APPROVAL;

-- 데이터 복원 확인
SELECT
    'DATA_RESTORED' AS step,
    COUNT(*) AS total_facilities,
    SUM(CASE WHEN REQUIRES_APPROVAL = 1 THEN 1 ELSE 0 END) AS approval_required,
    SUM(CASE WHEN REQUIRES_APPROVAL = 0 THEN 1 ELSE 0 END) AS instant_booking
FROM FACILITY_TBL;


-- =====================================================
-- STEP 4: 데이터 무결성 검증
-- =====================================================
-- 검증 1: 모든 시설의 REQUIRES_APPROVAL이 복원되었는지 확인
SELECT
    'VALIDATION_1' AS test_name,
    'All REQUIRES_APPROVAL restored' AS description,
    COUNT(*) AS total_facilities,
    SUM(CASE WHEN f.REQUIRES_APPROVAL = p.REQUIRES_APPROVAL THEN 1 ELSE 0 END) AS matching_count,
    CASE
        WHEN COUNT(*) = SUM(CASE WHEN f.REQUIRES_APPROVAL = p.REQUIRES_APPROVAL THEN 1 ELSE 0 END)
        THEN 'PASS ✓'
        ELSE 'FAIL ✗'
    END AS result
FROM FACILITY_TBL f
INNER JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX;

-- 검증 2: 백업 데이터와 비교
SELECT
    'VALIDATION_2' AS test_name,
    'Backup data matches' AS description,
    COUNT(*) AS total_facilities,
    SUM(CASE WHEN f.REQUIRES_APPROVAL = b.REQUIRES_APPROVAL THEN 1 ELSE 0 END) AS matching_count,
    CASE
        WHEN COUNT(*) = SUM(CASE WHEN f.REQUIRES_APPROVAL = b.REQUIRES_APPROVAL THEN 1 ELSE 0 END)
        THEN 'PASS ✓'
        ELSE 'FAIL ✗'
    END AS result
FROM FACILITY_TBL f
INNER JOIN FACILITY_TBL_BACKUP b ON f.FACILITY_IDX = b.FACILITY_IDX
WHERE EXISTS (SELECT 1 FROM information_schema.TABLES
              WHERE TABLE_SCHEMA = 'blue_crab'
                AND TABLE_NAME = 'FACILITY_TBL_BACKUP');

-- 검증 3: NULL 값 확인
SELECT
    'VALIDATION_3' AS test_name,
    'No NULL values in REQUIRES_APPROVAL' AS description,
    COUNT(*) AS null_count,
    CASE
        WHEN COUNT(*) = 0 THEN 'PASS ✓'
        ELSE 'FAIL ✗'
    END AS result
FROM FACILITY_TBL
WHERE REQUIRES_APPROVAL IS NULL;


-- =====================================================
-- STEP 5: FACILITY_POLICY_TBL 삭제
-- =====================================================
-- ⚠️ 주의: 위의 모든 검증이 PASS된 후에만 실행!

-- 외래키 제약조건 때문에 먼저 확인
SELECT
    'FK_CHECK' AS step,
    CONSTRAINT_NAME,
    TABLE_NAME,
    REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME = 'FACILITY_POLICY_TBL'
  AND TABLE_SCHEMA = 'blue_crab';

-- FACILITY_POLICY_TBL 삭제
DROP TABLE IF EXISTS FACILITY_POLICY_TBL;

-- 삭제 확인
SELECT
    'TABLE_DROPPED' AS step,
    COUNT(*) AS policy_table_exists
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'blue_crab'
  AND TABLE_NAME = 'FACILITY_POLICY_TBL';
-- 결과: 0이어야 정상


-- =====================================================
-- STEP 6: 백업 테이블 정리 (선택사항)
-- =====================================================
-- ⚠️ 롤백이 완전히 성공하고 충분히 검증된 후에만 실행
-- ⚠️ 최소 1주일 후 삭제 권장

-- DROP TABLE IF EXISTS FACILITY_TBL_BACKUP;


-- =====================================================
-- STEP 7: 최종 확인
-- =====================================================
SELECT
    '====== ROLLBACK COMPLETED ======' AS status,
    NOW() AS rollback_completed_time;

-- 최종 데이터 확인
SELECT
    FACILITY_IDX,
    FACILITY_NAME,
    FACILITY_TYPE,
    REQUIRES_APPROVAL,
    IS_ACTIVE
FROM FACILITY_TBL
ORDER BY FACILITY_IDX
LIMIT 10;

-- 테이블 구조 확인
SHOW CREATE TABLE FACILITY_TBL;


-- =====================================================
-- 체크리스트
-- =====================================================
/*
롤백 완료 후 확인사항:

□ FACILITY_TBL에 REQUIRES_APPROVAL 컬럼 존재
□ 모든 시설의 REQUIRES_APPROVAL 값 복원됨
□ 백업 데이터와 일치함
□ FACILITY_POLICY_TBL 삭제됨
□ 애플리케이션 정상 동작 확인
□ 예약 생성 API 테스트 통과
□ 시설 목록 조회 API 테스트 통과

롤백 사유 기록:
- 날짜:
- 담당자:
- 사유:
- 조치 사항:
*/


-- =====================================================
-- 롤백 완료
-- =====================================================
