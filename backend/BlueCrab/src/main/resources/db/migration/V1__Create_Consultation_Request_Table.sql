-- =====================================================
-- Blue Crab LMS - 상담 시스템 데이터베이스 스키마
-- Version: 2.0 (단일 테이블 최적화)
-- Created: 2025-10-24
-- =====================================================

-- CONSULTATION_REQUEST_TBL (요청 + 진행 통합)
CREATE TABLE CONSULTATION_REQUEST_TBL (
    request_idx BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '요청 고유 ID',

    -- [요청 정보]
    requester_user_code VARCHAR(20) NOT NULL COMMENT '요청자 학번',
    recipient_user_code VARCHAR(20) NOT NULL COMMENT '수신자 교번',
    consultation_type VARCHAR(50) NOT NULL COMMENT '상담 유형 (ACADEMIC/CAREER/CAMPUS_LIFE/ETC)',
    title VARCHAR(100) NOT NULL COMMENT '상담 제목',
    content VARCHAR(1000) COMMENT '상담 내용',
    desired_date DATETIME COMMENT '희망 날짜',

    -- [요청 처리]
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '요청 상태 (PENDING/APPROVED/REJECTED/CANCELLED)',
    accept_message VARCHAR(500) COMMENT '수락 메시지',
    rejection_reason VARCHAR(500) COMMENT '거절 사유 (가능 시간 포함)',
    cancel_reason VARCHAR(500) COMMENT '취소 사유',

    -- [상담 진행] (수락 후)
    consultation_status VARCHAR(20) COMMENT '상담 진행 상태 (NULL/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED)',
    started_at DATETIME COMMENT '상담 시작 시간',
    ended_at DATETIME COMMENT '상담 종료 시간',
    duration_minutes INT COMMENT '상담 시간(분)',
    last_activity_at DATETIME COMMENT '마지막 활동 시간 (자동 종료용)',

    -- [읽음 처리] (방 단위)
    last_read_time_student DATETIME COMMENT '학생 마지막 읽음 시간',
    last_read_time_professor DATETIME COMMENT '교수 마지막 읽음 시간',

    -- [메모 및 메타데이터]
    memo TEXT COMMENT '상담 메모 (교수 작성)',

    -- [타임스탬프]
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

    -- [인덱스]
    INDEX idx_requester (requester_user_code),
    INDEX idx_recipient (recipient_user_code),
    INDEX idx_request_status (request_status),
    INDEX idx_consultation_status (consultation_status),
    INDEX idx_created_at (created_at),
    INDEX idx_last_activity (last_activity_at),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상담 요청 및 진행 (통합)';
