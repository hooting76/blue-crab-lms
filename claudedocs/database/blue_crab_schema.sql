-- ============================================================================
-- Blue Crab LMS Database Schema
-- ============================================================================
-- Database: blue_crab
-- Charset: utf8mb3, utf8mb4
-- Engine: InnoDB
-- Generated: 2025-10-10
-- Source: SSH connection to production server (121.165.24.26:55511)
-- ============================================================================

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- ============================================================================
-- 1. USER MANAGEMENT TABLES
-- ============================================================================

-- 1.1 USER_TBL: 사용자 (학생/교수)
DROP TABLE IF EXISTS `USER_TBL`;
CREATE TABLE `USER_TBL` (
  `USER_IDX` int(200) NOT NULL AUTO_INCREMENT,
  `USER_EMAIL` varchar(200) NOT NULL COMMENT '이메일이 계정임',
  `USER_PW` varchar(200) NOT NULL,
  `USER_NAME` varchar(50) NOT NULL,
  `USER_CODE` varchar(255) NOT NULL DEFAULT '' COMMENT '학번/교수코드',
  `USER_PHONE` char(11) NOT NULL,
  `USER_BIRTH` varchar(100) NOT NULL,
  `USER_STUDENT` int(1) NOT NULL COMMENT '학생/교수 구분값',
  `USER_LATEST` varchar(100) DEFAULT NULL COMMENT '유저 마지막 접속일',
  `USER_ZIP` int(5) DEFAULT NULL COMMENT '우편번호',
  `USER_FIRST_ADD` varchar(200) DEFAULT NULL COMMENT '회원 기본주소',
  `USER_LAST_ADD` varchar(100) DEFAULT NULL COMMENT '상세주소',
  `USER_REG` varchar(100) DEFAULT NULL,
  `USER_REG_IP` varchar(100) DEFAULT NULL,
  `PROFILE_IMAGE_KEY` varchar(255) DEFAULT NULL COMMENT '프로필 이미지 MinIO 키',
  PRIMARY KEY (`USER_IDX`),
  KEY `idx_user_code` (`USER_CODE`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

-- 1.2 ADMIN_TBL: 관리자
DROP TABLE IF EXISTS `ADMIN_TBL`;
CREATE TABLE `ADMIN_TBL` (
  `ADMIN_IDX` int(200) NOT NULL AUTO_INCREMENT,
  `ADMIN_SYS` int(1) NOT NULL DEFAULT 0 COMMENT '1: 시스템 어드민 / 0: 학사 어드민 구분값',
  `ADMIN_ID` varchar(50) NOT NULL COMMENT '학교에서 발급해주는 계정 ID',
  `ADMIN_PW` varchar(200) NOT NULL,
  `ADMIN_NAME` varchar(100) NOT NULL,
  `ADMIN_PHONE` varchar(11) NOT NULL COMMENT '''-'' 빼고 받기',
  `ADMIN_OFFICE` varchar(11) NOT NULL COMMENT '관리자 사무실 번호 / ''-'' 빼고 받기',
  `ADMIN_LATEST` varchar(100) DEFAULT NULL COMMENT '마지막 접속 시간 / 연월일 시분 까지 표기',
  `ADMIN_LATEST_IP` varchar(50) DEFAULT NULL COMMENT '마지막 접속 IP',
  `ADMIN_REG` varchar(100) DEFAULT NULL COMMENT '어드민계정 등록일자',
  `ADMIN_REG_IP` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`ADMIN_IDX`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

-- ============================================================================
-- 2. BOARD SYSTEM TABLES
-- ============================================================================

-- 2.1 BOARD_TBL: 게시판
DROP TABLE IF EXISTS `BOARD_TBL`;
CREATE TABLE `BOARD_TBL` (
  `BOARD_IDX` int(200) NOT NULL AUTO_INCREMENT,
  `BOARD_CODE` int(10) NOT NULL DEFAULT 0,
  `BOARD_ON` int(10) NOT NULL DEFAULT 1 COMMENT '게시판 공개: 1 / 비공개: 0',
  `BOARD_WRITER` varchar(250) NOT NULL COMMENT '게시판 작성자',
  `BOARD_TIT` text DEFAULT NULL COMMENT '게시글 제목 (base64 인코딩)',
  `BOARD_CONT` text DEFAULT NULL COMMENT '게시글 본문 (base64 인코딩)',
  `BOARD_IMG` varchar(250) DEFAULT NULL COMMENT '게시판 내용을 대채하여 이미지로 올라갈 것을 대비/이미지 저장경로',
  `BOARD_FILE` varchar(500) DEFAULT NULL COMMENT '첨부파일 IDX 목록 (JSON 또는 콤마구분)',
  `BOARD_VIEW` int(250) NOT NULL DEFAULT 0 COMMENT '조회수',
  `BOARD_REG` varchar(250) DEFAULT NULL COMMENT '게시판 최초게시일',
  `BOARD_LATEST` varchar(250) DEFAULT NULL COMMENT '게시판 최근 수정일',
  `BOARD_IP` varchar(250) DEFAULT NULL,
  `BOARD_WRITER_IDX` int(11) NOT NULL COMMENT '작성자 IDX (USER_TBL.USER_IDX 또는 ADMIN_TBL.ADMIN_IDX)',
  `BOARD_WRITER_TYPE` int(11) NOT NULL DEFAULT 0 COMMENT '작성자 유형 (0=일반 사용자, 1=관리자)',
  PRIMARY KEY (`BOARD_IDX`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

-- 2.2 BOARD_ATTACHMENT_TBL: 게시글 첨부파일
DROP TABLE IF EXISTS `BOARD_ATTACHMENT_TBL`;
CREATE TABLE `BOARD_ATTACHMENT_TBL` (
  `ATTACHMENT_IDX` int(11) NOT NULL AUTO_INCREMENT COMMENT '첨부파일 IDX',
  `BOARD_IDX` int(11) NOT NULL COMMENT '게시글 IDX',
  `ORIGINAL_FILE_NAME` varchar(255) NOT NULL COMMENT '원본 파일명',
  `FILE_PATH` varchar(500) NOT NULL COMMENT '파일 경로 (변환된 파일명 포함)',
  `FILE_SIZE` bigint(20) NOT NULL COMMENT '파일 크기 (bytes)',
  `MIME_TYPE` varchar(100) NOT NULL COMMENT '파일 MIME 타입',
  `UPLOAD_DATE` varchar(50) NOT NULL COMMENT '업로드 일자',
  `IS_ACTIVE` tinyint(1) NOT NULL DEFAULT 1 COMMENT '활성화 여부',
  `EXPIRY_DATE` varchar(50) DEFAULT NULL COMMENT '파일 만료일',
  PRIMARY KEY (`ATTACHMENT_IDX`),
  KEY `idx_board_attachment_board_idx` (`BOARD_IDX`),
  KEY `idx_board_attachment_active` (`IS_ACTIVE`),
  KEY `idx_board_attachment_upload_date` (`UPLOAD_DATE`),
  KEY `idx_board_attachment_board_active` (`BOARD_IDX`,`IS_ACTIVE`),
  CONSTRAINT `BOARD_ATTACHMENT_TBL_ibfk_1` FOREIGN KEY (`BOARD_IDX`) REFERENCES `BOARD_TBL` (`BOARD_IDX`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='게시글 첨부파일 테이블';

-- ============================================================================
-- 3. FACILITY RESERVATION SYSTEM TABLES
-- ============================================================================

-- 3.1 FACILITY_TBL: 시설
DROP TABLE IF EXISTS `FACILITY_TBL`;
CREATE TABLE `FACILITY_TBL` (
  `FACILITY_IDX` int(11) NOT NULL AUTO_INCREMENT,
  `FACILITY_NAME` varchar(100) NOT NULL,
  `FACILITY_TYPE` varchar(20) NOT NULL,
  `FACILITY_DESC` text DEFAULT NULL,
  `CAPACITY` int(11) DEFAULT NULL,
  `LOCATION` varchar(200) DEFAULT NULL,
  `DEFAULT_EQUIPMENT` text DEFAULT NULL,
  `IS_ACTIVE` tinyint(4) NOT NULL DEFAULT 1,
  `CREATED_AT` datetime NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `REQUIRES_APPROVAL` tinyint(4) NOT NULL DEFAULT 1 COMMENT '승인 필요 여부 (1: 승인 필요, 0: 즉시 예약)',
  PRIMARY KEY (`FACILITY_IDX`),
  KEY `idx_facility_type_active` (`FACILITY_TYPE`,`IS_ACTIVE`),
  KEY `idx_facility_name` (`FACILITY_NAME`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 3.2 FACILITY_RESERVATION_TBL: 시설 예약
DROP TABLE IF EXISTS `FACILITY_RESERVATION_TBL`;
CREATE TABLE `FACILITY_RESERVATION_TBL` (
  `RESERVATION_IDX` int(11) NOT NULL AUTO_INCREMENT,
  `FACILITY_IDX` int(11) NOT NULL,
  `USER_CODE` varchar(50) NOT NULL,
  `START_TIME` datetime NOT NULL,
  `END_TIME` datetime NOT NULL,
  `PARTY_SIZE` int(11) DEFAULT NULL,
  `PURPOSE` text DEFAULT NULL,
  `REQUESTED_EQUIPMENT` text DEFAULT NULL,
  `STATUS` varchar(20) NOT NULL DEFAULT 'PENDING',
  `ADMIN_NOTE` text DEFAULT NULL,
  `REJECTION_REASON` text DEFAULT NULL,
  `APPROVED_BY` varchar(50) DEFAULT NULL,
  `APPROVED_AT` datetime DEFAULT NULL,
  `CREATED_AT` datetime NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`RESERVATION_IDX`),
  KEY `idx_reservation_facility_time` (`FACILITY_IDX`,`START_TIME`,`END_TIME`),
  KEY `idx_reservation_user_status` (`USER_CODE`,`STATUS`),
  KEY `idx_reservation_status_time` (`STATUS`,`START_TIME`),
  KEY `idx_reservation_created` (`CREATED_AT`),
  CONSTRAINT `fk_reservation_facility` FOREIGN KEY (`FACILITY_IDX`) REFERENCES `FACILITY_TBL` (`FACILITY_IDX`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 3.3 FACILITY_BLOCK_TBL: 시설 마감
DROP TABLE IF EXISTS `FACILITY_BLOCK_TBL`;
CREATE TABLE `FACILITY_BLOCK_TBL` (
  `BLOCK_IDX` int(11) NOT NULL AUTO_INCREMENT,
  `FACILITY_IDX` int(11) NOT NULL,
  `BLOCK_START` datetime NOT NULL,
  `BLOCK_END` datetime NOT NULL,
  `BLOCK_REASON` varchar(200) NOT NULL COMMENT '마감 사유',
  `BLOCK_TYPE` varchar(20) DEFAULT 'MAINTENANCE' COMMENT 'MAINTENANCE(점검), EMERGENCY(긴급), EVENT(행사)',
  `CREATED_BY` varchar(50) NOT NULL COMMENT '마감 설정한 관리자 코드',
  `CREATED_AT` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`BLOCK_IDX`),
  KEY `idx_block_facility_time` (`FACILITY_IDX`,`BLOCK_START`,`BLOCK_END`),
  KEY `idx_block_time_range` (`BLOCK_START`,`BLOCK_END`),
  CONSTRAINT `fk_block_facility` FOREIGN KEY (`FACILITY_IDX`) REFERENCES `FACILITY_TBL` (`FACILITY_IDX`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 3.4 FACILITY_RESERVATION_LOG: 시설 예약 로그
DROP TABLE IF EXISTS `FACILITY_RESERVATION_LOG`;
CREATE TABLE `FACILITY_RESERVATION_LOG` (
  `LOG_IDX` int(11) NOT NULL AUTO_INCREMENT,
  `RESERVATION_IDX` int(11) NOT NULL,
  `EVENT_TYPE` varchar(50) NOT NULL,
  `ACTOR_TYPE` varchar(20) DEFAULT NULL,
  `ACTOR_CODE` varchar(50) DEFAULT NULL,
  `PAYLOAD` text DEFAULT NULL,
  `CREATED_AT` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`LOG_IDX`),
  KEY `idx_reservationlog_reservation` (`RESERVATION_IDX`),
  KEY `idx_reservationlog_created` (`CREATED_AT`),
  CONSTRAINT `fk_log_reservation` FOREIGN KEY (`RESERVATION_IDX`) REFERENCES `FACILITY_RESERVATION_TBL` (`RESERVATION_IDX`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================================
-- 4. FCM PUSH NOTIFICATION TABLES
-- ============================================================================

-- 4.1 FCM_TOKEN_TABLE: FCM 토큰 관리
DROP TABLE IF EXISTS `FCM_TOKEN_TABLE`;
CREATE TABLE `FCM_TOKEN_TABLE` (
  `FCM_IDX` int(11) NOT NULL AUTO_INCREMENT COMMENT 'FCM 토큰 ID',
  `USER_IDX` int(11) NOT NULL COMMENT '사용자 ID (FK)',
  `USER_CODE` varchar(255) NOT NULL COMMENT '학번/교번',
  `FCM_TOKEN_ANDROID` varchar(255) DEFAULT NULL COMMENT '안드로이드 토큰',
  `FCM_TOKEN_ANDROID_LAST_USED` datetime DEFAULT NULL COMMENT '안드로이드 토큰 마지막 사용일시',
  `FCM_TOKEN_IOS` varchar(255) DEFAULT NULL COMMENT 'iOS 토큰',
  `FCM_TOKEN_IOS_LAST_USED` datetime DEFAULT NULL COMMENT 'iOS 토큰 마지막 사용일시',
  `FCM_TOKEN_WEB` varchar(255) DEFAULT NULL COMMENT '웹 토큰',
  `FCM_TOKEN_WEB_LAST_USED` datetime DEFAULT NULL COMMENT '웹 토큰 마지막 사용일시',
  `UPDATED_AT` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '최종 수정일시',
  `FCM_TOKEN_ANDROID_KEEP_SIGNED_IN` tinyint(1) DEFAULT NULL,
  `FCM_TOKEN_IOS_KEEP_SIGNED_IN` tinyint(1) DEFAULT NULL,
  `FCM_TOKEN_WEB_KEEP_SIGNED_IN` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`FCM_IDX`) USING BTREE,
  UNIQUE KEY `uq_user` (`USER_IDX`) USING BTREE,
  KEY `idx_user_code` (`USER_CODE`) USING BTREE,
  CONSTRAINT `fk_fcm_user` FOREIGN KEY (`USER_IDX`) REFERENCES `USER_TBL` (`USER_IDX`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='FCM 토큰 관리';

-- ============================================================================
-- 5. READING ROOM SYSTEM TABLES
-- ============================================================================

-- 5.1 LAMP_TBL: 열람실 좌석 현황
DROP TABLE IF EXISTS `LAMP_TBL`;
CREATE TABLE `LAMP_TBL` (
  `LAMP_IDX` int(11) NOT NULL COMMENT '좌석 번호 (1~80)',
  `LAMP_ON` int(11) NOT NULL DEFAULT 0 COMMENT '사용 여부 (0:빈자리, 1:사용중)',
  `USER_CODE` varchar(255) DEFAULT NULL COMMENT '현재 사용자 학번/교번',
  `start_time` datetime DEFAULT NULL COMMENT '사용 시작 시간',
  `end_time` datetime DEFAULT NULL COMMENT '예정 종료 시간 (2시간 후)',
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '최종 업데이트 시간',
  PRIMARY KEY (`LAMP_IDX`),
  KEY `idx_user_code` (`USER_CODE`),
  KEY `idx_lamp_on` (`LAMP_ON`),
  CONSTRAINT `fk_lamp_user` FOREIGN KEY (`USER_CODE`) REFERENCES `USER_TBL` (`USER_CODE`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='열람실 좌석 현황';

-- 5.2 LAMP_USAGE_LOG: 열람실 사용 기록
DROP TABLE IF EXISTS `LAMP_USAGE_LOG`;
CREATE TABLE `LAMP_USAGE_LOG` (
  `log_id` int(11) NOT NULL AUTO_INCREMENT,
  `lamp_idx` int(11) NOT NULL COMMENT '좌석 번호',
  `USER_CODE` varchar(255) NOT NULL COMMENT '사용자 학번/교번',
  `start_time` datetime NOT NULL COMMENT '입실 시간',
  `end_time` datetime DEFAULT NULL COMMENT '퇴실 시간 (NULL이면 사용중)',
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`log_id`),
  KEY `idx_user_date` (`USER_CODE`,`start_time`),
  KEY `idx_seat_date` (`lamp_idx`,`start_time`),
  KEY `idx_end_time` (`end_time`),
  CONSTRAINT `fk_log_lamp` FOREIGN KEY (`lamp_idx`) REFERENCES `LAMP_TBL` (`LAMP_IDX`),
  CONSTRAINT `fk_log_user` FOREIGN KEY (`USER_CODE`) REFERENCES `USER_TBL` (`USER_CODE`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='열람실 사용 기록 (백엔드에서 직접 관리)';

-- ============================================================================
-- 6. ACADEMIC SYSTEM TABLES
-- ============================================================================

-- 6.1 FACULTY: 학부 마스터
DROP TABLE IF EXISTS `FACULTY`;
CREATE TABLE `FACULTY` (
  `faculty_id` int(11) NOT NULL AUTO_INCREMENT,
  `faculty_code` char(2) NOT NULL,
  `faculty_name` varchar(50) NOT NULL,
  `established_at` year(4) NOT NULL,
  `capacity` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`faculty_id`),
  UNIQUE KEY `uq_faculty_code` (`faculty_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 6.2 DEPARTMENT: 학과 마스터
DROP TABLE IF EXISTS `DEPARTMENT`;
CREATE TABLE `DEPARTMENT` (
  `dept_id` int(11) NOT NULL AUTO_INCREMENT,
  `dept_code` char(2) NOT NULL,
  `dept_name` varchar(100) NOT NULL,
  `faculty_id` int(11) NOT NULL,
  `established_at` year(4) NOT NULL,
  `capacity` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`dept_id`),
  UNIQUE KEY `uq_faculty_dept` (`faculty_id`,`dept_code`),
  KEY `idx_dept_code` (`dept_code`),
  CONSTRAINT `fk_dept_faculty` FOREIGN KEY (`faculty_id`) REFERENCES `FACULTY` (`faculty_id`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='학과 마스터(학부 하위)';

-- 6.3 LEC_TBL: 강의
DROP TABLE IF EXISTS `LEC_TBL`;
CREATE TABLE `LEC_TBL` (
  `LEC_IDX` int(200) NOT NULL AUTO_INCREMENT,
  `LEC_SERIAL` varchar(50) NOT NULL COMMENT '강의 코드',
  `LEC_TIT` varchar(50) NOT NULL COMMENT '강의명칭',
  `LEC_PROF` varchar(50) NOT NULL COMMENT '강의 담당교수',
  `LEC_POINT` int(10) NOT NULL DEFAULT 0 COMMENT '이수학점',
  `LEC_MAJOR` int(1) NOT NULL DEFAULT 1 COMMENT '전공 강의: 1/ 그외(교양): 0',
  `LEC_MUST` int(1) NOT NULL DEFAULT 1 COMMENT '필수과목: 1 / 선택과목: 0',
  `LEC_SUMMARY` text DEFAULT NULL COMMENT '강의 개요 내용',
  `LEC_TIME` varchar(50) NOT NULL COMMENT '강의 시간',
  `LEC_ASSIGN` int(1) NOT NULL DEFAULT 0 COMMENT '과제있음: 1 / 과제없음: 0',
  `LEC_OPEN` int(1) NOT NULL DEFAULT 0 COMMENT '강의 열림: 1 / 강의 닫힘: 0 <= 수강신청에 대한 상태값',
  `LEC_MANY` int(10) NOT NULL DEFAULT 0 COMMENT '수강가능 인원수',
  `LEC_MCODE` varchar(50) NOT NULL COMMENT '학부 코드',
  `LEC_MCODE_DEP` varchar(50) NOT NULL COMMENT '학과 코드',
  `LEC_MIN` int(10) NOT NULL DEFAULT 0 COMMENT '수강 가능한 최저 학년 제한(학기수로 판별) / ',
  `LEC_REG` varchar(100) DEFAULT NULL COMMENT '강의 등록일',
  `LEC_IP` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`LEC_IDX`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

-- 6.4 REGIST_TABLE: 학생 학적 이력
DROP TABLE IF EXISTS `REGIST_TABLE`;
CREATE TABLE `REGIST_TABLE` (
  `REG_IDX` int(11) NOT NULL AUTO_INCREMENT COMMENT '학적 이력 행 ID(생성순)',
  `USER_IDX` int(11) NOT NULL COMMENT '학생 ID (USER_TBL.FK)',
  `USER_CODE` varchar(50) NOT NULL DEFAULT '' COMMENT '학번/교번(조회 보조)',
  `JOIN_PATH` varchar(100) NOT NULL DEFAULT '신규' COMMENT '입학경로',
  `STD_STAT` varchar(100) NOT NULL DEFAULT '재학' COMMENT '학적상태',
  `STD_REST_DATE` varchar(200) DEFAULT NULL COMMENT '휴학기간(문자열)',
  `CNT_TERM` int(11) NOT NULL DEFAULT 0 COMMENT '이수완료 학기 수',
  `ADMIN_NAME` varchar(200) DEFAULT NULL COMMENT '처리 관리자명',
  `ADMIN_REG` datetime DEFAULT NULL COMMENT '처리일시',
  `ADMIN_IP` varchar(45) DEFAULT NULL COMMENT '처리발생 IP',
  PRIMARY KEY (`REG_IDX`),
  KEY `FK_REG_USER` (`USER_IDX`),
  CONSTRAINT `FK_REG_USER` FOREIGN KEY (`USER_IDX`) REFERENCES `USER_TBL` (`USER_IDX`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='학생 학적 이력: 생성순 REG_IDX + USER_IDX 별도 관리';

-- 6.5 SERIAL_CODE_TABLE: 학생 전공/부전공 정보
DROP TABLE IF EXISTS `SERIAL_CODE_TABLE`;
CREATE TABLE `SERIAL_CODE_TABLE` (
  `SERIAL_IDX` int(11) NOT NULL AUTO_INCREMENT COMMENT '학부 관리 코드 idx',
  `USER_IDX` int(11) NOT NULL COMMENT '회원 테이블 인덱스',
  `SERIAL_CODE` char(2) NOT NULL COMMENT '전공 학부 코드',
  `SERIAL_SUB` char(2) NOT NULL COMMENT '전공 학과 코드',
  `SERIAL_CODE_ND` char(2) DEFAULT NULL COMMENT '부전공 학부 코드',
  `SERIAL_SUB_ND` char(2) DEFAULT NULL COMMENT '부전공 학과 코드',
  `SERIAL_REG` varchar(50) NOT NULL COMMENT '전공 등록일',
  `SERIAL_REG_ND` varchar(50) DEFAULT NULL COMMENT '부전공 등록일',
  PRIMARY KEY (`SERIAL_IDX`),
  UNIQUE KEY `uq_user_major` (`USER_IDX`,`SERIAL_CODE`,`SERIAL_SUB`),
  KEY `idx_user` (`USER_IDX`),
  CONSTRAINT `fk_sct_user` FOREIGN KEY (`USER_IDX`) REFERENCES `USER_TBL` (`USER_IDX`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='학생 전공/부전공 정보 테이블';

-- ============================================================================
-- 7. LEGACY TABLES
-- ============================================================================

-- 7.1 RENT_TABLE: 시설물 대여 (레거시)
DROP TABLE IF EXISTS `RENT_TABLE`;
CREATE TABLE `RENT_TABLE` (
  `RENT_IDX` int(200) NOT NULL AUTO_INCREMENT,
  `RENT_USER` int(200) NOT NULL COMMENT '회원 테이블 참조',
  `RENT_CODE` varchar(200) NOT NULL DEFAULT '' COMMENT '시설물 코드',
  `RENT_TITLE` varchar(200) NOT NULL DEFAULT '' COMMENT '시설물 대여 이유',
  `RENT_DETAIL` varchar(200) DEFAULT NULL COMMENT '이유에 대한 디테일',
  `RENT_DATE` varchar(200) NOT NULL DEFAULT '' COMMENT '시설물 대여시간(09 - 18시)',
  `RENT_REG` varchar(50) DEFAULT NULL COMMENT '글 등록시간',
  `RENT_IP` varchar(50) DEFAULT NULL,
  `RENT_OK` int(5) DEFAULT NULL COMMENT '처리중(NULL) / 사용허가(1) / 사용불가(0)',
  `RENT_RES` varchar(100) DEFAULT NULL COMMENT '승인여부에 따른 사유 리턴',
  `RENT_ADMIN` varchar(100) NOT NULL COMMENT '응답 관리자',
  `RENT_CONFIRM` varchar(100) DEFAULT NULL COMMENT '응답일시',
  `RENT_CONF_IP` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`RENT_IDX`),
  KEY `USER_IDX` (`RENT_USER`),
  CONSTRAINT `USER_IDX` FOREIGN KEY (`RENT_USER`) REFERENCES `USER_TBL` (`USER_IDX`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='시설이용테이블';

-- ============================================================================
-- 8. TRIGGERS
-- ============================================================================

-- 8.1 auto_student_regist: 학생 가입 시 자동 학적 생성
DELIMITER ;;
CREATE TRIGGER auto_student_regist
    AFTER INSERT ON USER_TBL
    FOR EACH ROW
    INSERT INTO REGIST_TABLE (
        USER_IDX, USER_CODE,
        JOIN_PATH,
        STD_STAT,
        CNT_TERM
    )
    SELECT NEW.USER_IDX,
        NEW.USER_CODE,
        '신규',
        '재학',
        0
    WHERE NEW.USER_STUDENT = 0;;
DELIMITER ;

-- ============================================================================
-- 9. VIEWS
-- ============================================================================

-- 9.1 PROFILE_VIEW: 사용자 통합 프로필 조회
DROP VIEW IF EXISTS `PROFILE_VIEW`;
CREATE ALGORITHM=UNDEFINED DEFINER=`KDT_project`@`%` SQL SECURITY DEFINER 
VIEW `PROFILE_VIEW` AS 
SELECT 
    `u`.`USER_EMAIL` AS `user_email`,
    `u`.`USER_NAME` AS `user_name`,
    `u`.`USER_PHONE` AS `user_phone`,
    `u`.`USER_STUDENT` AS `user_type`,
    `u`.`USER_CODE` AS `major_code`,
    LPAD(COALESCE(`u`.`USER_ZIP`,0),5,'0') AS `zip_code`,
    `u`.`USER_FIRST_ADD` AS `main_address`,
    `u`.`USER_LAST_ADD` AS `detail_address`,
    `u`.`PROFILE_IMAGE_KEY` AS `profile_image_key`,
    `u`.`USER_BIRTH` AS `birth_date`,
    `r`.`STD_STAT` AS `academic_status`,
    `r`.`JOIN_PATH` AS `admission_route`,
    `mf`.`faculty_code` AS `major_faculty_code`,
    `md`.`dept_code` AS `major_dept_code`,
    `minf`.`faculty_code` AS `minor_faculty_code`,
    `mind`.`dept_code` AS `minor_dept_code` 
FROM `USER_TBL` `u` 
LEFT JOIN `REGIST_TABLE` `r` ON `u`.`USER_IDX` = `r`.`USER_IDX`
LEFT JOIN `SERIAL_CODE_TABLE` `sc` ON `u`.`USER_IDX` = `sc`.`USER_IDX`
LEFT JOIN `FACULTY` `mf` ON `sc`.`SERIAL_CODE` COLLATE utf8mb4_general_ci = `mf`.`faculty_code` COLLATE utf8mb4_general_ci
LEFT JOIN `DEPARTMENT` `md` ON `mf`.`faculty_id` = `md`.`faculty_id` AND `sc`.`SERIAL_SUB` COLLATE utf8mb4_general_ci = `md`.`dept_code` COLLATE utf8mb4_general_ci
LEFT JOIN `FACULTY` `minf` ON `sc`.`SERIAL_CODE_ND` COLLATE utf8mb4_general_ci = `minf`.`faculty_code` COLLATE utf8mb4_general_ci
LEFT JOIN `DEPARTMENT` `mind` ON `minf`.`faculty_id` = `mind`.`faculty_id` AND `sc`.`SERIAL_SUB_ND` COLLATE utf8mb4_general_ci = `mind`.`dept_code` COLLATE utf8mb4_general_ci;

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
