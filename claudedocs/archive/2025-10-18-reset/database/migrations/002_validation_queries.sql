-- =====================================================
-- 시설 예약 정책 테이블 - 데이터 검증 쿼리
-- =====================================================
-- 목적: 마이그레이션 전후 데이터 검증 및 상태 확인
--
-- 사용 시점:
--   1. 마이그레이션 실행 전 - 현재 상태 확인
--   2. 마이그레이션 실행 중 - 각 단계별 검증
--   3. 마이그레이션 실행 후 - 최종 검증
--
-- 실행일: 2025-10-10
-- 작성자: Development Team
-- =====================================================


-- =====================================================
-- 1. 마이그레이션 전 상태 확인
-- =====================================================

-- 1-1. 현재 시설 수 및 정책 분포
SELECT
    '=== CURRENT STATE ===' AS section,
    COUNT(*) AS total_facilities,
    SUM(CASE WHEN REQUIRES_APPROVAL = 1 THEN 1 ELSE 0 END) AS requires_approval_count,
    SUM(CASE WHEN REQUIRES_APPROVAL = 0 THEN 1 ELSE 0 END) AS instant_booking_count,
    SUM(CASE WHEN IS_ACTIVE = 1 THEN 1 ELSE 0 END) AS active_facilities,
    SUM(CASE WHEN IS_ACTIVE = 0 THEN 1 ELSE 0 END) AS inactive_facilities
FROM FACILITY_TBL;

-- 1-2. 시설 유형별 분포
SELECT
    '=== FACILITIES BY TYPE ===' AS section,
    FACILITY_TYPE,
    COUNT(*) AS count,
    SUM(CASE WHEN REQUIRES_APPROVAL = 1 THEN 1 ELSE 0 END) AS requires_approval,
    SUM(CASE WHEN REQUIRES_APPROVAL = 0 THEN 1 ELSE 0 END) AS instant_booking
FROM FACILITY_TBL
GROUP BY FACILITY_TYPE
ORDER BY FACILITY_TYPE;

-- 1-3. REQUIRES_APPROVAL 컬럼 존재 확인
SELECT
    '=== COLUMN CHECK ===' AS section,
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_DEFAULT,
    IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'blue_crab'
  AND TABLE_NAME = 'FACILITY_TBL'
  AND COLUMN_NAME = 'REQUIRES_APPROVAL';


-- =====================================================
-- 2. 마이그레이션 중 검증 (각 단계별)
-- =====================================================

-- 2-1. 백업 테이블 생성 확인
SELECT
    '=== BACKUP TABLE ===' AS section,
    COUNT(*) AS backup_row_count
FROM FACILITY_TBL_BACKUP;

-- 백업 데이터 샘플
SELECT
    '=== BACKUP SAMPLE ===' AS section,
    FACILITY_IDX,
    FACILITY_NAME,
    REQUIRES_APPROVAL
FROM FACILITY_TBL_BACKUP
LIMIT 5;

-- 2-2. 정책 테이블 생성 확인
SELECT
    '=== POLICY TABLE STRUCTURE ===' AS section,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'blue_crab'
  AND TABLE_NAME = 'FACILITY_POLICY_TBL'
ORDER BY ORDINAL_POSITION;

-- 2-3. 정책 데이터 복사 확인
SELECT
    '=== POLICY DATA COUNT ===' AS section,
    COUNT(*) AS total_policies,
    SUM(CASE WHEN REQUIRES_APPROVAL = 1 THEN 1 ELSE 0 END) AS approval_required,
    SUM(CASE WHEN REQUIRES_APPROVAL = 0 THEN 1 ELSE 0 END) AS instant_booking,
    SUM(CASE WHEN MIN_DURATION_MINUTES IS NOT NULL THEN 1 ELSE 0 END) AS custom_min_duration,
    SUM(CASE WHEN MAX_DURATION_MINUTES IS NOT NULL THEN 1 ELSE 0 END) AS custom_max_duration,
    SUM(CASE WHEN MAX_DAYS_IN_ADVANCE IS NOT NULL THEN 1 ELSE 0 END) AS custom_max_days
FROM FACILITY_POLICY_TBL;


-- =====================================================
-- 3. 데이터 일치성 검증
-- =====================================================

-- 3-1. 시설 수와 정책 수 일치 확인
SELECT
    '=== COUNT MATCH ===' AS section,
    (SELECT COUNT(*) FROM FACILITY_TBL) AS facility_count,
    (SELECT COUNT(*) FROM FACILITY_POLICY_TBL) AS policy_count,
    CASE
        WHEN (SELECT COUNT(*) FROM FACILITY_TBL) = (SELECT COUNT(*) FROM FACILITY_POLICY_TBL)
        THEN 'PASS ✓'
        ELSE 'FAIL ✗'
    END AS result;

-- 3-2. REQUIRES_APPROVAL 값 일치 확인
SELECT
    '=== VALUE MATCH ===' AS section,
    f.FACILITY_IDX,
    f.FACILITY_NAME,
    f.REQUIRES_APPROVAL AS original_value,
    p.REQUIRES_APPROVAL AS copied_value,
    CASE
        WHEN f.REQUIRES_APPROVAL = p.REQUIRES_APPROVAL THEN 'OK'
        ELSE 'MISMATCH'
    END AS status
FROM FACILITY_TBL f
LEFT JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX
WHERE f.REQUIRES_APPROVAL != p.REQUIRES_APPROVAL
   OR p.POLICY_IDX IS NULL;
-- 결과가 없어야 정상

-- 3-3. 누락된 시설 확인
SELECT
    '=== MISSING POLICIES ===' AS section,
    f.FACILITY_IDX,
    f.FACILITY_NAME,
    f.FACILITY_TYPE,
    'NO POLICY' AS issue
FROM FACILITY_TBL f
LEFT JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX
WHERE p.POLICY_IDX IS NULL;
-- 결과가 없어야 정상

-- 3-4. 중복 정책 확인
SELECT
    '=== DUPLICATE POLICIES ===' AS section,
    FACILITY_IDX,
    COUNT(*) AS policy_count
FROM FACILITY_POLICY_TBL
GROUP BY FACILITY_IDX
HAVING COUNT(*) > 1;
-- 결과가 없어야 정상


-- =====================================================
-- 4. 외래키 제약조건 확인
-- =====================================================

-- 4-1. 정책 테이블 외래키 확인
SELECT
    '=== FOREIGN KEYS ===' AS section,
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME,
    DELETE_RULE,
    UPDATE_RULE
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'blue_crab'
  AND (TABLE_NAME = 'FACILITY_POLICY_TBL' OR REFERENCED_TABLE_NAME = 'FACILITY_POLICY_TBL');

-- 4-2. 외래키 무결성 테스트
SELECT
    '=== FK INTEGRITY ===' AS section,
    p.POLICY_IDX,
    p.FACILITY_IDX,
    f.FACILITY_NAME,
    CASE
        WHEN f.FACILITY_IDX IS NOT NULL THEN 'VALID'
        ELSE 'INVALID - ORPHAN POLICY'
    END AS status
FROM FACILITY_POLICY_TBL p
LEFT JOIN FACILITY_TBL f ON p.FACILITY_IDX = f.FACILITY_IDX
WHERE f.FACILITY_IDX IS NULL;
-- 결과가 없어야 정상


-- =====================================================
-- 5. 인덱스 성능 확인
-- =====================================================

-- 5-1. 정책 테이블 인덱스 목록
SELECT
    '=== INDEXES ===' AS section,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    NON_UNIQUE,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'blue_crab'
  AND TABLE_NAME = 'FACILITY_POLICY_TBL'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 5-2. 인덱스 사용 테스트 쿼리
EXPLAIN SELECT p.*
FROM FACILITY_POLICY_TBL p
WHERE p.FACILITY_IDX = 1;

EXPLAIN SELECT p.*
FROM FACILITY_POLICY_TBL p
WHERE p.REQUIRES_APPROVAL = 1;


-- =====================================================
-- 6. 애플리케이션 호환성 확인
-- =====================================================

-- 6-1. 시설 정보 + 정책 조인 테스트 (API 응답 시뮬레이션)
SELECT
    '=== API RESPONSE SIMULATION ===' AS section,
    f.FACILITY_IDX,
    f.FACILITY_NAME,
    f.FACILITY_TYPE,
    f.CAPACITY,
    f.LOCATION,
    f.IS_ACTIVE,
    p.REQUIRES_APPROVAL,
    p.MIN_DURATION_MINUTES,
    p.MAX_DURATION_MINUTES,
    p.MAX_DAYS_IN_ADVANCE
FROM FACILITY_TBL f
LEFT JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX
WHERE f.IS_ACTIVE = 1
ORDER BY f.FACILITY_IDX
LIMIT 5;

-- 6-2. 예약 생성 시 정책 조회 시뮬레이션
SELECT
    '=== RESERVATION POLICY CHECK ===' AS section,
    f.FACILITY_IDX,
    f.FACILITY_NAME,
    p.REQUIRES_APPROVAL,
    COALESCE(p.MIN_DURATION_MINUTES, 30) AS effective_min_duration,
    COALESCE(p.MAX_DURATION_MINUTES, 480) AS effective_max_duration,
    COALESCE(p.MAX_DAYS_IN_ADVANCE, 30) AS effective_max_days
FROM FACILITY_TBL f
LEFT JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX
WHERE f.FACILITY_IDX = 1;


-- =====================================================
-- 7. 마이그레이션 후 최종 검증
-- =====================================================

-- 7-1. FACILITY_TBL에 REQUIRES_APPROVAL 컬럼이 삭제되었는지 확인
SELECT
    '=== COLUMN REMOVED ===' AS section,
    COUNT(*) AS requires_approval_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'blue_crab'
  AND TABLE_NAME = 'FACILITY_TBL'
  AND COLUMN_NAME = 'REQUIRES_APPROVAL';
-- 결과: 0이어야 정상

-- 7-2. FACILITY_TBL 현재 컬럼 목록
SELECT
    '=== FACILITY_TBL COLUMNS ===' AS section,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'blue_crab'
  AND TABLE_NAME = 'FACILITY_TBL'
ORDER BY ORDINAL_POSITION;

-- 7-3. 정책 테이블 통계
SELECT
    '=== POLICY STATISTICS ===' AS section,
    COUNT(*) AS total_policies,
    AVG(CASE WHEN REQUIRES_APPROVAL = 1 THEN 1 ELSE 0 END) * 100 AS approval_required_percentage,
    COUNT(CASE WHEN MIN_DURATION_MINUTES IS NOT NULL THEN 1 END) AS facilities_with_custom_min,
    COUNT(CASE WHEN MAX_DURATION_MINUTES IS NOT NULL THEN 1 END) AS facilities_with_custom_max,
    COUNT(CASE WHEN MAX_DAYS_IN_ADVANCE IS NOT NULL THEN 1 END) AS facilities_with_custom_days,
    MIN(MIN_DURATION_MINUTES) AS shortest_min_duration,
    MAX(MAX_DURATION_MINUTES) AS longest_max_duration,
    MAX(MAX_DAYS_IN_ADVANCE) AS furthest_advance_days
FROM FACILITY_POLICY_TBL;


-- =====================================================
-- 8. 성능 비교 (선택사항)
-- =====================================================

-- 8-1. 마이그레이션 전 쿼리 (시뮬레이션)
-- SELECT f.*, f.REQUIRES_APPROVAL
-- FROM FACILITY_TBL f
-- WHERE f.IS_ACTIVE = 1;

-- 8-2. 마이그레이션 후 쿼리
SELECT f.*, p.REQUIRES_APPROVAL
FROM FACILITY_TBL f
LEFT JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX
WHERE f.IS_ACTIVE = 1;

-- 실행 계획 비교
EXPLAIN SELECT f.*, p.REQUIRES_APPROVAL
FROM FACILITY_TBL f
LEFT JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX
WHERE f.IS_ACTIVE = 1;


-- =====================================================
-- 9. 롤백 준비 확인
-- =====================================================

-- 9-1. 백업 테이블 존재 및 데이터 무결성
SELECT
    '=== BACKUP INTEGRITY ===' AS section,
    (SELECT COUNT(*) FROM FACILITY_TBL_BACKUP) AS backup_count,
    (SELECT COUNT(*) FROM FACILITY_TBL) AS current_count,
    CASE
        WHEN (SELECT COUNT(*) FROM FACILITY_TBL_BACKUP) = (SELECT COUNT(*) FROM FACILITY_TBL)
        THEN 'BACKUP OK ✓'
        ELSE 'BACKUP INCOMPLETE ✗'
    END AS backup_status;


-- =====================================================
-- 10. 종합 검증 리포트
-- =====================================================

SELECT '========================================' AS report;
SELECT '       MIGRATION VALIDATION REPORT      ' AS report;
SELECT '========================================' AS report;

-- 테이블 상태
SELECT
    'Table Status' AS category,
    CASE WHEN EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'blue_crab' AND TABLE_NAME = 'FACILITY_POLICY_TBL')
        THEN '✓ FACILITY_POLICY_TBL exists'
        ELSE '✗ FACILITY_POLICY_TBL missing'
    END AS status
UNION ALL
SELECT
    'Table Status' AS category,
    CASE WHEN NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'blue_crab' AND TABLE_NAME = 'FACILITY_TBL' AND COLUMN_NAME = 'REQUIRES_APPROVAL')
        THEN '✓ REQUIRES_APPROVAL removed from FACILITY_TBL'
        ELSE '✗ REQUIRES_APPROVAL still in FACILITY_TBL'
    END AS status
UNION ALL

-- 데이터 무결성
SELECT
    'Data Integrity' AS category,
    CONCAT(
        CASE WHEN (SELECT COUNT(*) FROM FACILITY_TBL) = (SELECT COUNT(*) FROM FACILITY_POLICY_TBL)
            THEN '✓'
            ELSE '✗'
        END,
        ' Policy count matches facility count (',
        (SELECT COUNT(*) FROM FACILITY_POLICY_TBL),
        '/',
        (SELECT COUNT(*) FROM FACILITY_TBL),
        ')'
    ) AS status
UNION ALL

-- 외래키
SELECT
    'Foreign Keys' AS category,
    CONCAT(
        '✓ ',
        COUNT(*),
        ' foreign key constraint(s) created'
    ) AS status
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'blue_crab'
  AND TABLE_NAME = 'FACILITY_POLICY_TBL'
  AND REFERENCED_TABLE_NAME IS NOT NULL;

SELECT '========================================' AS report;


-- =====================================================
-- 검증 완료
-- =====================================================
