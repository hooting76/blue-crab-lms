-- =====================================================
-- 시설 예약 정책 테이블 분리 마이그레이션
-- =====================================================
-- 목적: FACILITY_TBL의 REQUIRES_APPROVAL과 신규 정책 필드를
--       별도의 FACILITY_POLICY_TBL로 분리하여 확장 가능한 구조 구축
--
-- 작업 순서:
--   1. FACILITY_POLICY_TBL 생성
--   2. 기존 데이터 복사 (REQUIRES_APPROVAL)
--   3. 데이터 검증
--   4. FACILITY_TBL에서 REQUIRES_APPROVAL 컬럼 삭제
--
-- 실행일: 2025-10-10
-- 작성자: Development Team
-- =====================================================

-- =====================================================
-- STEP 1: 백업 테이블 생성 (안전장치)
-- =====================================================
CREATE TABLE IF NOT EXISTS FACILITY_TBL_BACKUP AS
SELECT * FROM FACILITY_TBL;

-- 백업 확인
SELECT
    'BACKUP_CREATED' AS status,
    COUNT(*) AS facility_count,
    NOW() AS backup_time
FROM FACILITY_TBL_BACKUP;


-- =====================================================
-- STEP 2: FACILITY_POLICY_TBL 생성
-- =====================================================
CREATE TABLE FACILITY_POLICY_TBL (
    -- 기본키
    POLICY_IDX INT AUTO_INCREMENT PRIMARY KEY
        COMMENT '정책 ID',

    -- 외래키 (1:1 관계)
    FACILITY_IDX INT NOT NULL UNIQUE
        COMMENT '시설 ID (FK)',

    -- 승인 정책 (기존 FACILITY_TBL에서 이동)
    REQUIRES_APPROVAL TINYINT(4) NOT NULL DEFAULT 1
        COMMENT '승인 필요(1)/즉시예약(0)',

    -- 예약 시간 정책 (신규, NULL = 전역 정책 사용)
    MIN_DURATION_MINUTES INT NULL DEFAULT NULL
        COMMENT '최소 예약 시간(분), NULL=전역 정책 사용',

    MAX_DURATION_MINUTES INT NULL DEFAULT NULL
        COMMENT '최대 예약 시간(분), NULL=전역 정책 사용',

    MAX_DAYS_IN_ADVANCE INT NULL DEFAULT NULL
        COMMENT '최대 사전 예약 기간(일), NULL=전역 정책 사용',

    -- 추가 정책 (향후 확장)
    CANCELLATION_DEADLINE_HOURS INT NULL DEFAULT NULL
        COMMENT '취소 가능 기한(시간 전), NULL=전역 정책',

    MAX_RESERVATIONS_PER_USER INT NULL DEFAULT NULL
        COMMENT '사용자당 최대 동시 예약 수, NULL=제한없음',

    ALLOW_WEEKEND_BOOKING TINYINT(1) NULL DEFAULT NULL
        COMMENT '주말 예약 가능(1)/불가(0), NULL=가능',

    -- 타임스탬프
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '생성일시',

    UPDATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT '수정일시',

    -- 인덱스
    INDEX idx_policy_facility (FACILITY_IDX),
    INDEX idx_policy_approval (REQUIRES_APPROVAL),
    INDEX idx_policy_updated (UPDATED_AT),

    -- 외래키 제약조건
    CONSTRAINT fk_policy_facility
        FOREIGN KEY (FACILITY_IDX)
        REFERENCES FACILITY_TBL(FACILITY_IDX)
        ON DELETE CASCADE
        ON UPDATE CASCADE

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='시설별 예약 정책 설정 (승인, 시간제한, 예약제한 등)';

-- 테이블 생성 확인
SHOW CREATE TABLE FACILITY_POLICY_TBL;


-- =====================================================
-- STEP 3: 기존 FACILITY_TBL 데이터를 새 테이블로 복사
-- =====================================================
INSERT INTO FACILITY_POLICY_TBL (
    FACILITY_IDX,
    REQUIRES_APPROVAL,
    CREATED_AT,
    UPDATED_AT
)
SELECT
    FACILITY_IDX,
    REQUIRES_APPROVAL,
    CREATED_AT,
    UPDATED_AT
FROM FACILITY_TBL
WHERE FACILITY_IDX IS NOT NULL;

-- 복사 결과 확인
SELECT
    'DATA_COPIED' AS status,
    COUNT(*) AS policy_count,
    SUM(CASE WHEN REQUIRES_APPROVAL = 1 THEN 1 ELSE 0 END) AS approval_required_count,
    SUM(CASE WHEN REQUIRES_APPROVAL = 0 THEN 1 ELSE 0 END) AS instant_booking_count
FROM FACILITY_POLICY_TBL;


-- =====================================================
-- STEP 4: 데이터 무결성 검증
-- =====================================================

-- 검증 1: 모든 시설에 정책이 생성되었는지 확인
SELECT
    'VALIDATION_1' AS test_name,
    'All facilities have policies' AS description,
    COUNT(*) AS facility_count,
    (SELECT COUNT(*) FROM FACILITY_POLICY_TBL) AS policy_count,
    CASE
        WHEN COUNT(*) = (SELECT COUNT(*) FROM FACILITY_POLICY_TBL)
        THEN 'PASS ✓'
        ELSE 'FAIL ✗'
    END AS result
FROM FACILITY_TBL;

-- 검증 2: REQUIRES_APPROVAL 값이 정확히 복사되었는지 확인
SELECT
    'VALIDATION_2' AS test_name,
    'REQUIRES_APPROVAL values match' AS description,
    COUNT(*) AS total_count,
    SUM(CASE
        WHEN f.REQUIRES_APPROVAL = p.REQUIRES_APPROVAL THEN 1
        ELSE 0
    END) AS matching_count,
    CASE
        WHEN COUNT(*) = SUM(CASE WHEN f.REQUIRES_APPROVAL = p.REQUIRES_APPROVAL THEN 1 ELSE 0 END)
        THEN 'PASS ✓'
        ELSE 'FAIL ✗'
    END AS result
FROM FACILITY_TBL f
INNER JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX;

-- 검증 3: 누락된 시설이 없는지 확인
SELECT
    'VALIDATION_3' AS test_name,
    'No missing facilities' AS description,
    f.FACILITY_IDX,
    f.FACILITY_NAME,
    'MISSING POLICY' AS issue
FROM FACILITY_TBL f
LEFT JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX
WHERE p.POLICY_IDX IS NULL;
-- 결과가 없어야 정상

-- 검증 4: 중복된 정책이 없는지 확인
SELECT
    'VALIDATION_4' AS test_name,
    'No duplicate policies' AS description,
    FACILITY_IDX,
    COUNT(*) AS duplicate_count,
    'DUPLICATE FOUND' AS issue
FROM FACILITY_POLICY_TBL
GROUP BY FACILITY_IDX
HAVING COUNT(*) > 1;
-- 결과가 없어야 정상


-- =====================================================
-- STEP 5: FACILITY_TBL에서 REQUIRES_APPROVAL 컬럼 삭제
-- =====================================================
-- ⚠️ 주의: 위의 모든 검증이 PASS된 후에만 실행!
-- ⚠️ 검증 실패 시 롤백 스크립트(001_rollback.sql) 실행 필요

ALTER TABLE FACILITY_TBL DROP COLUMN REQUIRES_APPROVAL;

-- 컬럼 삭제 확인
SHOW COLUMNS FROM FACILITY_TBL;

-- 최종 확인: REQUIRES_APPROVAL 컬럼이 없어야 함
SELECT
    'FINAL_CHECK' AS status,
    COUNT(*) AS requires_approval_column_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'blue_crab'
  AND TABLE_NAME = 'FACILITY_TBL'
  AND COLUMN_NAME = 'REQUIRES_APPROVAL';
-- 결과: 0이어야 정상


-- =====================================================
-- STEP 6: 마이그레이션 완료 확인
-- =====================================================
SELECT
    '====== MIGRATION COMPLETED ======' AS status,
    NOW() AS completed_at;

-- 최종 데이터 확인
SELECT
    f.FACILITY_IDX,
    f.FACILITY_NAME,
    f.FACILITY_TYPE,
    p.REQUIRES_APPROVAL,
    p.MIN_DURATION_MINUTES,
    p.MAX_DURATION_MINUTES,
    p.MAX_DAYS_IN_ADVANCE
FROM FACILITY_TBL f
LEFT JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX
ORDER BY f.FACILITY_IDX
LIMIT 10;


-- =====================================================
-- STEP 7: 백업 테이블 정리 (선택사항)
-- =====================================================
-- ⚠️ 마이그레이션이 완전히 성공하고 충분히 검증된 후에만 실행
-- ⚠️ 최소 1주일 후 삭제 권장

-- DROP TABLE IF EXISTS FACILITY_TBL_BACKUP;


-- =====================================================
-- 참고: 샘플 정책 설정 예시
-- =====================================================
-- 특정 시설에 커스텀 정책 설정하기

-- 예시 1: 대강당 - 최소 2시간, 최대 8시간, 60일 사전 예약
-- UPDATE FACILITY_POLICY_TBL
-- SET
--     MIN_DURATION_MINUTES = 120,
--     MAX_DURATION_MINUTES = 480,
--     MAX_DAYS_IN_ADVANCE = 60
-- WHERE FACILITY_IDX = 1;

-- 예시 2: 스터디룸 - 최소 30분, 최대 2시간, 7일 사전 예약
-- UPDATE FACILITY_POLICY_TBL
-- SET
--     MIN_DURATION_MINUTES = 30,
--     MAX_DURATION_MINUTES = 120,
--     MAX_DAYS_IN_ADVANCE = 7,
--     MAX_RESERVATIONS_PER_USER = 2
-- WHERE FACILITY_IDX IN (SELECT FACILITY_IDX FROM FACILITY_TBL WHERE FACILITY_TYPE = 'STUDY_ROOM');

-- 예시 3: 즉시 예약 시설 (승인 불필요)
-- UPDATE FACILITY_POLICY_TBL
-- SET REQUIRES_APPROVAL = 0
-- WHERE FACILITY_IDX = 5;


-- =====================================================
-- 마이그레이션 완료
-- =====================================================
