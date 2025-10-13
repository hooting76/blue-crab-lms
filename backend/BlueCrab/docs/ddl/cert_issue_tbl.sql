-- =========================================================
-- BlueCrab LMS - 증명서 발급 이력 테이블 DDL
-- 작성일: 2025-10-13
-- 설명: 증명서 발급 이력을 저장하여 감사 로그를 관리
-- =========================================================

-- 1. 증명서 발급 이력 테이블 생성
CREATE TABLE IF NOT EXISTS CERT_ISSUE_TBL (
  CERT_IDX      INT AUTO_INCREMENT PRIMARY KEY COMMENT '발급 이력 ID',
  USER_IDX      INT NOT NULL COMMENT '발급 대상 사용자 (FK)',
  CERT_TYPE     VARCHAR(50) NOT NULL COMMENT '증명서 유형 (enrollment, graduation_expected 등)',
  AS_OF_DATE    DATE NULL COMMENT '스냅샷 기준일 (특정 시점 기준 발급 시)',
  FORMAT        VARCHAR(20) NOT NULL DEFAULT 'html' COMMENT '발급 형식 (html, pdf, image)',
  SNAPSHOT_JSON JSON NOT NULL COMMENT '발급 당시 학적/프로필 데이터 스냅샷',
  ISSUED_AT     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급 일시',
  ISSUED_IP     VARCHAR(45) NULL COMMENT '발급 발생 IP 주소 (IPv6 지원)',
  
  -- 외래키 제약조건
  CONSTRAINT FK_CERT_USER FOREIGN KEY (USER_IDX) 
    REFERENCES USER_TBL (USER_IDX)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '증명서 발급 이력 테이블 - 발급 감사 로그 및 스냅샷 저장';

-- =========================================================
-- 2. 인덱스 생성
-- =========================================================

-- 사용자별 발급 이력 조회 최적화
CREATE INDEX IX_CERT_USER_TIME ON CERT_ISSUE_TBL (USER_IDX, ISSUED_AT DESC)
  COMMENT '사용자별 발급 이력 조회 (최신순)';

-- 증명서 유형별 통계 조회 최적화
CREATE INDEX IX_CERT_TYPE ON CERT_ISSUE_TBL (CERT_TYPE)
  COMMENT '증명서 유형별 통계';

-- 발급 시간 범위 조회 최적화 (통계 및 리포트용)
CREATE INDEX IX_CERT_ISSUED_AT ON CERT_ISSUE_TBL (ISSUED_AT DESC)
  COMMENT '발급 시간 범위 조회';

-- 사용자별 + 증명서 유형별 조회 최적화 (남발 방지 체크용)
CREATE INDEX IX_CERT_USER_TYPE_TIME ON CERT_ISSUE_TBL (USER_IDX, CERT_TYPE, ISSUED_AT DESC)
  COMMENT '사용자별 증명서 유형별 발급 이력 (남발 방지 체크)';

-- IP 주소별 발급 이력 조회 (보안 감사용)
CREATE INDEX IX_CERT_IP ON CERT_ISSUE_TBL (ISSUED_IP)
  COMMENT 'IP 주소별 발급 이력 (보안 감사)';

-- =========================================================
-- 3. 샘플 데이터 삽입 (테스트용 - 선택)
-- =========================================================

-- 사용자 ID 1번이 재학증명서 발급
INSERT INTO CERT_ISSUE_TBL (USER_IDX, CERT_TYPE, AS_OF_DATE, FORMAT, SNAPSHOT_JSON, ISSUED_AT, ISSUED_IP)
VALUES (
  1,
  'enrollment',
  '2025-03-01',
  'html',
  JSON_OBJECT(
    'userName', '테스트사용자',
    'userEmail', 'test@univ.edu',
    'studentCode', '202500101000',
    'academicStatus', '재학',
    'admissionRoute', '정시',
    'enrolledTerms', 2
  ),
  NOW(),
  '127.0.0.1'
);

-- 사용자 ID 1번이 졸업예정증명서 발급
INSERT INTO CERT_ISSUE_TBL (USER_IDX, CERT_TYPE, AS_OF_DATE, FORMAT, SNAPSHOT_JSON, ISSUED_AT, ISSUED_IP)
VALUES (
  1,
  'graduation_expected',
  NULL,
  'html',
  JSON_OBJECT(
    'userName', '테스트사용자',
    'userEmail', 'test@univ.edu',
    'studentCode', '202500101000',
    'academicStatus', '재학',
    'admissionRoute', '정시',
    'enrolledTerms', 7
  ),
  NOW(),
  '127.0.0.1'
);

-- =========================================================
-- 4. 통계 쿼리 예시
-- =========================================================

-- 증명서 유형별 발급 건수 조회
-- SELECT CERT_TYPE, COUNT(*) as issue_count
-- FROM CERT_ISSUE_TBL
-- GROUP BY CERT_TYPE
-- ORDER BY issue_count DESC;

-- 사용자별 총 발급 건수 조회
-- SELECT u.USER_NAME, u.USER_EMAIL, COUNT(c.CERT_IDX) as issue_count
-- FROM USER_TBL u
-- LEFT JOIN CERT_ISSUE_TBL c ON u.USER_IDX = c.USER_IDX
-- GROUP BY u.USER_IDX
-- ORDER BY issue_count DESC
-- LIMIT 10;

-- 오늘 발급된 증명서 조회
-- SELECT c.*, u.USER_NAME, u.USER_EMAIL
-- FROM CERT_ISSUE_TBL c
-- JOIN USER_TBL u ON c.USER_IDX = u.USER_IDX
-- WHERE DATE(c.ISSUED_AT) = CURDATE()
-- ORDER BY c.ISSUED_AT DESC;

-- 최근 5분 이내 발급 이력 조회 (남발 방지 체크)
-- SELECT *
-- FROM CERT_ISSUE_TBL
-- WHERE USER_IDX = 1
--   AND CERT_TYPE = 'enrollment'
--   AND ISSUED_AT > DATE_SUB(NOW(), INTERVAL 5 MINUTE)
-- ORDER BY ISSUED_AT DESC;

-- =========================================================
-- 5. 테이블 정보 확인
-- =========================================================

-- DESCRIBE CERT_ISSUE_TBL;
-- SHOW INDEX FROM CERT_ISSUE_TBL;
-- SELECT COUNT(*) FROM CERT_ISSUE_TBL;

-- =========================================================
-- 6. 롤백 스크립트 (필요 시)
-- =========================================================

-- DROP TABLE IF EXISTS CERT_ISSUE_TBL;
