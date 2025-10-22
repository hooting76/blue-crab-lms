-- 수강신청 안내문 테이블 생성 스크립트
-- 작성일: 2025-10-22
-- 설명: 수강신청 안내문을 관리하는 테이블

-- ===============================================
-- 테이블 생성
-- ===============================================

CREATE TABLE IF NOT EXISTS COURSE_APPLY_NOTICE (
    notice_idx INT AUTO_INCREMENT PRIMARY KEY COMMENT '안내문 고유 ID (자동증가)',
    message TEXT NOT NULL COMMENT '안내 메시지 내용',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 수정 시간',
    updated_by VARCHAR(50) NOT NULL COMMENT '최종 수정자 (username)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '최초 생성 시간'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='수강신청 안내문 테이블';

-- ===============================================
-- 인덱스 생성
-- ===============================================

-- 최신 안내문 조회를 위한 인덱스 (updated_at 기준 정렬)
CREATE INDEX idx_updated_at ON COURSE_APPLY_NOTICE (updated_at DESC);

-- ===============================================
-- 초기 데이터 삽입
-- ===============================================

-- 테스트용 초기 안내문
INSERT INTO COURSE_APPLY_NOTICE (message, updated_by) 
VALUES (
    '2025학년도 1학기 수강신청 안내\n\n수강신청 기간: 2025년 2월 1일 ~ 2월 7일\n최대 신청학점: 21학점\n\n문의사항은 학사지원팀으로 연락주시기 바랍니다.',
    'system'
);

-- ===============================================
-- 권한 설정 (필요시)
-- ===============================================

-- 예시: 특정 사용자에게 테이블 권한 부여
-- GRANT SELECT, INSERT, UPDATE ON COURSE_APPLY_NOTICE TO 'bluecrab_admin'@'localhost';

-- ===============================================
-- 테이블 구조 확인
-- ===============================================

-- 테이블 구조 출력
-- DESC COURSE_APPLY_NOTICE;

-- 데이터 확인
-- SELECT * FROM COURSE_APPLY_NOTICE ORDER BY updated_at DESC LIMIT 1;
