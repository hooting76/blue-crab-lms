-- ========================================
-- 상담 API 리팩토링 데이터베이스 마이그레이션
-- ========================================
-- 작성일: 2025-10-28
-- 목적: 이중 상태 구조를 단일 status 컬럼으로 통합
-- 영향: CONSULTATION_REQUEST_TBL 테이블
-- 하위 호환성: 기존 request_status, consultation_status 컬럼 유지
-- ========================================

USE blue_crab;

-- ========================================
-- 1단계: 새로운 컬럼 추가
-- ========================================

-- 통합 상태 컬럼 (NOT NULL, 기본값 'PENDING')
ALTER TABLE CONSULTATION_REQUEST_TBL
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '통합 상태 (PENDING/APPROVED/REJECTED/CANCELLED/IN_PROGRESS/COMPLETED)'
AFTER consultation_status;

-- 상태 변경 시간
ALTER TABLE CONSULTATION_REQUEST_TBL
ADD COLUMN status_changed_at DATETIME NULL COMMENT '상태 변경 시간'
AFTER status;

-- 상태 변경자
ALTER TABLE CONSULTATION_REQUEST_TBL
ADD COLUMN status_changed_by VARCHAR(20) NULL COMMENT '상태 변경자 USER_CODE'
AFTER status_changed_at;

-- 상태 변경 사유
ALTER TABLE CONSULTATION_REQUEST_TBL
ADD COLUMN status_reason VARCHAR(500) NULL COMMENT '상태 변경 사유 (반려/취소 시 필수)'
AFTER status_changed_by;

-- ========================================
-- 2단계: 기존 데이터 마이그레이션
-- ========================================

-- 마이그레이션 로직:
-- 1. consultation_status가 있으면 우선 사용
-- 2. 없으면 request_status 사용
-- 3. status_changed_at은 updated_at 또는 created_at 사용

UPDATE CONSULTATION_REQUEST_TBL
SET
    status = CASE
        -- consultation_status가 있으면 우선 (진행/완료 상태)
        WHEN consultation_status = 'SCHEDULED' THEN 'APPROVED'
        WHEN consultation_status = 'IN_PROGRESS' THEN 'IN_PROGRESS'
        WHEN consultation_status = 'COMPLETED' THEN 'COMPLETED'
        WHEN consultation_status = 'CANCELLED' THEN 'CANCELLED'
        -- consultation_status가 없으면 request_status 사용
        WHEN request_status = 'PENDING' THEN 'PENDING'
        WHEN request_status = 'APPROVED' THEN 'APPROVED'
        WHEN request_status = 'REJECTED' THEN 'REJECTED'
        WHEN request_status = 'CANCELLED' THEN 'CANCELLED'
        ELSE 'PENDING'
    END,
    status_changed_at = COALESCE(updated_at, created_at),
    status_changed_by = requester_user_code,
    status_reason = COALESCE(rejection_reason, cancel_reason, accept_message)
WHERE status = 'PENDING';  -- 기본값인 경우만 업데이트

-- ========================================
-- 3단계: 인덱스 생성
-- ========================================

-- status 컬럼 인덱스 (가장 많이 조회됨)
CREATE INDEX idx_status ON CONSULTATION_REQUEST_TBL(status);

-- status_changed_at 컬럼 인덱스 (정렬용)
CREATE INDEX idx_status_changed_at ON CONSULTATION_REQUEST_TBL(status_changed_at);

-- 복합 인덱스: status + requester (내가 보낸 요청)
CREATE INDEX idx_status_requester ON CONSULTATION_REQUEST_TBL(status, requester_user_code);

-- 복합 인덱스: status + recipient (받은 요청)
CREATE INDEX idx_status_recipient ON CONSULTATION_REQUEST_TBL(status, recipient_user_code);

-- ========================================
-- 4단계: 검증 쿼리
-- ========================================

-- 상태별 카운트 확인
SELECT
    status,
    COUNT(*) as count
FROM CONSULTATION_REQUEST_TBL
GROUP BY status
ORDER BY status;

-- 상태 전환 이력 확인
SELECT
    request_idx,
    request_status,
    consultation_status,
    status,
    status_changed_at,
    status_changed_by,
    status_reason
FROM CONSULTATION_REQUEST_TBL
ORDER BY request_idx DESC
LIMIT 10;

-- 마이그레이션 전후 비교
SELECT
    '기존 구조' as type,
    request_status,
    consultation_status,
    NULL as status,
    COUNT(*) as count
FROM CONSULTATION_REQUEST_TBL
GROUP BY request_status, consultation_status

UNION ALL

SELECT
    '새 구조' as type,
    NULL as request_status,
    NULL as consultation_status,
    status,
    COUNT(*) as count
FROM CONSULTATION_REQUEST_TBL
GROUP BY status
ORDER BY type, request_status, consultation_status, status;

-- ========================================
-- 5단계: 롤백 스크립트 (필요시 사용)
-- ========================================

-- 주의: 마이그레이션 전 반드시 백업하세요!

/*
-- 컬럼 삭제 (롤백)
ALTER TABLE CONSULTATION_REQUEST_TBL DROP COLUMN status_reason;
ALTER TABLE CONSULTATION_REQUEST_TBL DROP COLUMN status_changed_by;
ALTER TABLE CONSULTATION_REQUEST_TBL DROP COLUMN status_changed_at;
ALTER TABLE CONSULTATION_REQUEST_TBL DROP COLUMN status;

-- 인덱스 삭제 (롤백)
DROP INDEX idx_status ON CONSULTATION_REQUEST_TBL;
DROP INDEX idx_status_changed_at ON CONSULTATION_REQUEST_TBL;
DROP INDEX idx_status_requester ON CONSULTATION_REQUEST_TBL;
DROP INDEX idx_status_recipient ON CONSULTATION_REQUEST_TBL;
*/

-- ========================================
-- 6단계: 향후 계획 (하위 호환성 제거)
-- ========================================

-- 프론트엔드가 완전히 새 API로 전환된 후 실행
-- 주의: 최소 1개월 후 실행 권장

/*
-- 기존 인덱스 삭제
DROP INDEX idx_request_status ON CONSULTATION_REQUEST_TBL;
DROP INDEX idx_consultation_status ON CONSULTATION_REQUEST_TBL;

-- 기존 컬럼 삭제 (선택사항 - 하위 호환성 완전 제거)
-- ALTER TABLE CONSULTATION_REQUEST_TBL DROP COLUMN request_status;
-- ALTER TABLE CONSULTATION_REQUEST_TBL DROP COLUMN consultation_status;
-- ALTER TABLE CONSULTATION_REQUEST_TBL DROP COLUMN accept_message;
-- ALTER TABLE CONSULTATION_REQUEST_TBL DROP COLUMN rejection_reason;
-- ALTER TABLE CONSULTATION_REQUEST_TBL DROP COLUMN cancel_reason;
*/

-- ========================================
-- 마이그레이션 완료
-- ========================================
-- 실행 후 반드시 검증 쿼리로 확인하세요!
-- ========================================
