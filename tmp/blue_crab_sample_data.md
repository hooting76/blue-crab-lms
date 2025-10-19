# Blue Crab LMS 데이터베이스 샘플 데이터

## 📊 개요

Blue Crab LMS 데이터베이스의 실제 데이터 샘플입니다. 각 테이블의 대표적인 데이터 예시를 확인할 수 있습니다.

**데이터 수집 일시:** 2025년 10월 18일
**총 레코드 수:** 약 200+ 개

---

## 👥 1. 사용자 관리 데이터

### FACULTY (학부 마스터)
```sql
INSERT INTO `FACULTY` (`faculty_id`, `faculty_code`, `faculty_name`, `established_at`, `capacity`) VALUES
	(1, '01', '해양학부', '2025', 410),
	(2, '02', '보건학부', '2025', 340),
	(3, '03', '자연과학부', '2025', 120),
	(4, '04', '인문학부', '2025', 320),
	(5, '05', '공학부', '2025', 320);
```

**학부별 정원:**
- 해양학부: 410명 (항해학과, 해양경찰, 해군사관 등)
- 보건학부: 340명 (간호학, 치위생, 약학 등)
- 자연과학부: 120명 (물리학, 수학, 화학)
- 인문학부: 320명 (철학, 국어, 역사, 경영 등)
- 공학부: 320명 (기계공학, 전자공학, ICT융합 등)

### DEPARTMENT (학과 마스터)
```sql
INSERT INTO `DEPARTMENT` (`dept_id`, `dept_code`, `dept_name`, `faculty_id`, `established_at`, `capacity`) VALUES
	(1, '01', '항해학과', 1, '2025', 50),
	(2, '02', '해양경찰', 1, '2025', 70),
	(3, '03', '해군사관', 1, '2025', 120),
	(4, '04', '도선학과', 1, '2025', 50),
	(5, '05', '해양수산학', 1, '2025', 40),
	(6, '06', '조선학과', 1, '2025', 80),
	(8, '01', '간호학', 2, '2025', 120),
	(9, '02', '치위생', 2, '2025', 80),
	(10, '03', '약학과', 2, '2025', 80),
	(11, '04', '보건정책학', 2, '2025', 60);
```

### ADMIN_TBL (관리자 계정)
```sql
INSERT INTO `ADMIN_TBL` (`ADMIN_IDX`, `ADMIN_SYS`, `ADMIN_ID`, `ADMIN_PW`, `ADMIN_NAME`, `ADMIN_PHONE`, `ADMIN_OFFICE`, `ADMIN_LATEST`, `ADMIN_LATEST_IP`, `ADMIN_REG`, `ADMIN_REG_IP`) VALUES
	(1, 1, 'sysadmin@test.com', 'a4286041084085ad1bfa6f8e8f1d5f7e4500ccd78e638e973c135db472cb4d96', '시스템 관리자', '01000000000', '604', NULL, NULL, '2025-09-08 15:38:38', '10.0.0.10'),
	(2, 0, 'admin01@test.com', '757dd9815ac8356fb7eb16775fd6c05bcdb1f80f1076f3829aebf827db1e6e0d', '집가고', '01011112222', '601', NULL, NULL, '2025-09-08 15:38:38', '10.0.0.11'),
	(3, 1, 'shs02140@kakao.com', '5fede982f82ea8b8093451429fea1e85cd4469788b732734c5d6d2ebed3de07c', '김운영', '01033334444', '602', NULL, NULL, '2025-09-08 15:38:38', '10.0.0.12');
```

**관리자 유형:**
- 시스템 관리자 (ADMIN_SYS=1): 시스템 전체 관리
- 학사 관리자 (ADMIN_SYS=0): 학사 업무 관리

---

## 📚 2. 강의 및 수강 데이터

### LEC_TBL (강의 정보 - 샘플)
현재 데이터베이스에는 66개의 강의가 등록되어 있으며, 주요 강의 정보는 다음과 같습니다:

**강의 상태 분포:**
- 개설됨 (LEC_OPEN=1): 수강신청 가능
- 미개설 (LEC_OPEN=0): 수강신청 불가

**학점 분포:**
- 2학점: 기초 과목
- 3학점: 전공 과목
- 1-4학점: 교양 과목

### ENROLLMENT_EXTENDED_TBL (수강 정보)
```sql
INSERT INTO `ENROLLMENT_EXTENDED_TBL` (`ENROLLMENT_IDX`, `LEC_IDX`, `STUDENT_IDX`, `ENROLLMENT_DATA`) VALUES
	(1, 6, 6, '{"grade":{},"attendance":[],"enrollment":{"status":"ENROLLED","enrollmentDate":"2025-10-14 11:50:07"}}'),
	(2, 7, 6, '{"grade":{},"attendance":[],"enrollment":{"status":"ENROLLED","enrollmentDate":"2025-10-14 12:58:36"}}'),
	(3, 48, 6, '{"grade":{},"attendance":[],"enrollment":{"status":"ENROLLED","enrollmentDate":"2025-10-17 22:55:20"}}');
```

**ENROLLMENT_DATA JSON 구조:**
```json
{
  "enrollment": {
    "status": "ENROLLED",
    "enrollmentDate": "2025-10-14 11:50:07"
  },
  "grade": {},
  "attendance": []
}
```

### ASSIGNMENT_EXTENDED_TBL (과제 정보)
```sql
INSERT INTO `ASSIGNMENT_EXTENDED_TBL` (`ASSIGNMENT_IDX`, `LEC_IDX`, `ASSIGNMENT_DATA`) VALUES
	(1, 6, '{"submissions":[],"assignment":{"description":"300자 이상","maxScore":25,"title":"식인대게의 생태조사","dueDate":"2025-12-31"}}');
```

**ASSIGNMENT_DATA JSON 구조:**
```json
{
  "assignment": {
    "title": "식인대게의 생태조사",
    "description": "300자 이상",
    "maxScore": 25,
    "dueDate": "2025-12-31"
  },
  "submissions": []
}
```

---

## 🏢 3. 시설 예약 데이터

### FACILITY_TBL (시설 정보)
```sql
INSERT INTO `FACILITY_TBL` (`FACILITY_IDX`, `FACILITY_NAME`, `FACILITY_TYPE`, `FACILITY_DESC`, `CAPACITY`, `LOCATION`, `DEFAULT_EQUIPMENT`, `IS_ACTIVE`, `CREATED_AT`, `UPDATED_AT`) VALUES
	(1, '다목적 회의실 1호', 'MEETING_ROOM', '소규모 회의 및 스터디. 정원 20명', 20, '본관 3층', '빔프로젝터, 화이트보드', 1, '2025-10-06 23:11:28', '2025-10-08 02:35:37'),
	(2, '다목적 회의실 2호', 'MEETING_ROOM', '소규모 회의 및 스터디. 정원 20명', 20, '본관 3층', '빔프로젝터, 화이트보드', 1, '2025-10-06 23:11:28', '2025-10-08 02:35:37'),
	(3, '다목적 회의실 3호', 'MEETING_ROOM', '중규모 회의실. 정원 30명', 30, '본관 4층', '빔프로젝터, 화이트보드, 마이크', 1, '2025-10-06 23:11:28', '2025-10-08 02:35:37'),
	(4, '다목적 회의실 4호', 'MEETING_ROOM', '중규모 회의실. 정원 30명', 30, '본관 4층', '빔프로젝터, 화이트보드, 마이크', 1, '2025-10-06 23:11:28', '2025-10-06 23:11:28'),
	(5, '강당', 'AUDITORIUM', '대규모 행사 및 강연. 정원 200명', 200, '본관 1층', '빔프로젝터, 스피커, 마이크', 1, '2025-10-06 23:11:28', '2025-10-06 23:11:28'),
	(22, '해양설비 공작실', 'WORKSHOP', '작업 정원 15명. 용접, 절단 등 실습 가능', 15, '공작실동', '용접기, 절단기, 드릴, 작업대', 1, '2025-10-06 23:11:28', '2025-10-06 23:11:28');
```

**시설 유형 분포:**
- MEETING_ROOM: 다목적 회의실 (4개)
- AUDITORIUM: 강당 (1개)
- WORKSHOP: 실습실/공작실 (1개)
- 기타: 세미나실, 강의실 등

### FACILITY_POLICY_TBL (시설 정책)
```sql
INSERT INTO `FACILITY_POLICY_TBL` (`POLICY_IDX`, `FACILITY_IDX`, `REQUIRES_APPROVAL`, `MIN_DURATION_MINUTES`, `MAX_DURATION_MINUTES`, `MIN_DAYS_IN_ADVANCE`, `MAX_DAYS_IN_ADVANCE`, `CANCELLATION_DEADLINE_HOURS`, `MAX_RESERVATIONS_PER_USER`, `ALLOW_WEEKEND_BOOKING`, `CREATED_AT`, `UPDATED_AT`) VALUES
	(1, 1, 0, 30, 240, 0, 14, NULL, NULL, NULL, '2025-10-06 23:11:28', '2025-10-13 10:09:48'),
	(2, 2, 0, 30, 240, 0, 14, NULL, NULL, NULL, '2025-10-06 23:11:28', '2025-10-13 10:09:48'),
	(3, 3, 0, 30, 240, 0, 14, NULL, NULL, NULL, '2025-10-06 23:11:28', '2025-10-13 10:09:48'),
	(4, 4, 1, 30, 240, 1, 30, NULL, NULL, NULL, '2025-10-06 23:11:28', '2025-10-13 10:09:48'),
	(5, 5, 1, 60, 480, 3, 60, NULL, NULL, NULL, '2025-10-06 23:11:28', '2025-10-13 10:09:48');
```

**승인 정책:**
- 즉시 예약 (REQUIRES_APPROVAL=0): 회의실 1-3호
- 승인 필요 (REQUIRES_APPROVAL=1): 회의실 4호, 강당, 실습실

### FACILITY_RESERVATION_TBL (시설 예약)
```sql
INSERT INTO `FACILITY_RESERVATION_TBL` (`RESERVATION_IDX`, `FACILITY_IDX`, `USER_CODE`, `USER_EMAIL`, `START_TIME`, `END_TIME`, `PARTY_SIZE`, `PURPOSE`, `REQUESTED_EQUIPMENT`, `STATUS`, `ADMIN_NOTE`, `REJECTION_REASON`, `APPROVED_BY`, `APPROVED_AT`, `CREATED_AT`, `UPDATED_AT`) VALUES
	(1, 1, '240105045', 'stu01@bluecrab.ac.kr', '2025-10-14 10:00:00', '2025-10-14 12:00:00', 4, '스터디', '빔프로젝터', 'COMPLETED', NULL, NULL, 'SYSTEM', '2025-10-13 10:07:17', '2025-10-13 10:07:17', '2025-10-17 16:53:48'),
	(16, 5, '240105045', 'stu01@bluecrab.ac.kr', '2025-10-22 15:00:00', '2025-10-22 18:00:00', 4, '화려한 조명이 나를 감싸는 무대 위에서 춤 연습', '화려한 조명 개쩌는 음향 시설과 마이크', 'REJECTED', NULL, '나는 너를 모른다.', NULL, NULL, '2025-10-18 15:32:15', '2025-10-18 15:35:35');
```

**예약 상태:**
- COMPLETED: 완료됨
- PENDING: 승인 대기
- APPROVED: 승인됨
- REJECTED: 거부됨

### FACILITY_RESERVATION_LOG (예약 로그)
```sql
INSERT INTO `FACILITY_RESERVATION_LOG` (`LOG_IDX`, `RESERVATION_IDX`, `EVENT_TYPE`, `ACTOR_TYPE`, `ACTOR_CODE`, `PAYLOAD`, `CREATED_AT`) VALUES
	(1, 1, 'AUTO_APPROVED', 'USER', 'stu01@bluecrab.ac.kr', '{"facilityIdx":1,"startTime":"2025-10-14T10:00:00","endTime":"2025-10-14T12:00:00","partySize":4,"purpose":"스터디","requestedEquipment":"빔프로젝터"}', '2025-10-13 10:07:17'),
	(29, 16, 'REJECTED', 'ADMIN', 'bluecrabtester9@gmail.com', '{"reservationIdx":16,"rejectionReason":"나는 너를 모른다."}', '2025-10-18 15:35:34');
```

---

## 📖 4. 열람실 데이터

### LAMP_TBL (좌석 현황)
현재 열람실에는 1번부터 80번까지 총 80개의 좌석이 있으며, 실시간으로 사용 현황이 관리됩니다.

### LAMP_USAGE_LOG (열람실 사용 로그)
```sql
INSERT INTO `LAMP_USAGE_LOG` (`log_id`, `lamp_idx`, `USER_CODE`, `start_time`, `end_time`, `pre_notice_sent_at`, `pre_notice_token_count`, `created_at`) VALUES
	(1, 20, '202500101000', '2025-09-29 11:05:20', '2025-09-29 11:08:54', NULL, NULL, '2025-09-29 11:05:20'),
	(2, 1, '202500101000', '2025-10-01 13:21:53', '2025-10-01 15:02:34', NULL, NULL, '2025-10-01 13:21:53'),
	(3, 1, '202500101000', '2025-10-01 15:02:37', '2025-10-01 16:50:44', NULL, NULL, '2025-10-01 15:02:37'),
	(4, 1, '202500101000', '2025-10-01 16:50:49', '2025-10-01 18:50:49', NULL, NULL, '2025-10-01 16:50:49'),
	(5, 1, '202500101000', '2025-10-02 09:56:21', '2025-10-02 10:33:56', NULL, NULL, '2025-10-02 09:56:21'),
	(18, 69, '240105045', '2025-10-18 15:38:48', '2025-10-18 17:38:48', '2025-10-18 17:23:00', 2, '2025-10-18 15:38:48');
```

**사용 패턴:**
- 최대 2시간 사용 제한
- 사전 퇴실 알림 기능 (pre_notice_sent_at)
- FCM 토큰 카운트 관리

---

## 📋 5. 게시판 데이터

### BOARD_TBL (게시글)
```sql
INSERT INTO `BOARD_TBL` (`BOARD_IDX`, `BOARD_CODE`, `BOARD_ON`, `BOARD_WRITER`, `BOARD_TIT`, `BOARD_CONT`, `BOARD_IMG`, `BOARD_FILE`, `BOARD_VIEW`, `BOARD_REG`, `BOARD_LATEST`, `BOARD_IP`, `BOARD_WRITER_IDX`, `BOARD_WRITER_TYPE`) VALUES
	(1, 3, 1, '실험용', '공지사항', '기본값아님', NULL, NULL, 9, '2025-09-24T16:49:26.532362153', '2025-09-24T16:51:36.884943821', NULL, 0, 0),
	(2, 3, 1, '실험용', '수동작성 제목임', '직접 작성한 내용임.', NULL, NULL, 2, '2025-09-24T16:57:39.859336959', '2025-09-24T16:57:39.859353383', NULL, 0, 0),
	(3, 2, 1, '실험용', '조선학과는 한국사 학과가 아닙니다.', '배 만드는 거 배우는 곳 이라고요.', NULL, NULL, 12, '2025-09-24T17:04:28.590152556', '2025-09-26T16:51:12.398790421', NULL, 0, 0);
```

**게시판 유형 (BOARD_CODE):**
- 0: 일반 게시판
- 1: 공지사항
- 2: 학과 공지
- 3: 학교 공지

**내용 인코딩:**
- BOARD_TIT: Base64 인코딩된 제목
- BOARD_CONT: Base64 인코딩된 내용

### BOARD_ATTACHMENT_TBL (첨부파일)
```sql
INSERT INTO `BOARD_ATTACHMENT_TBL` (`ATTACHMENT_IDX`, `BOARD_IDX`, `ORIGINAL_FILE_NAME`, `FILE_PATH`, `FILE_SIZE`, `MIME_TYPE`, `UPLOAD_DATE`, `IS_ACTIVE`, `EXPIRY_DATE`) VALUES
	(1, 26, 'Blizzard Of Ozz.jpg', '20251010/20251010-120009-8bd6aaa7.jpg', 20570, 'image/jpeg', '2025-10-10 12:00:09', 1, '2025-11-09 12:00:09'),
	(2, 26, 'Blizzard Of Ozz.jpg', '20251010/20251010-120028-9c107db1.jpg', 20570, 'image/jpeg', '2025-10-10 12:00:28', 1, '2025-11-09 12:00:28');
```

**파일 관리:**
- MinIO를 통한 파일 저장
- 30일 만료 정책
- MIME 타입별 분류

---

## 🔧 6. 시스템 데이터

### FCM_TOKEN_TABLE (FCM 토큰)
현재 7개의 FCM 토큰이 등록되어 있으며, Android/iOS/Web 플랫폼별로 토큰이 관리됩니다.

### RENT_TABLE (구버전 시설 이용)
현재 데이터 없음 (신규 FACILITY_RESERVATION_TBL로 대체)

### FACILITY_BLOCK_TBL (시설 차단)
현재 데이터 없음 (점검/행사 등으로 인한 시설 차단 이력 없음)

---

## 📈 7. 데이터 통계

### 테이블별 레코드 수 (샘플 데이터 기준)
- FACULTY: 5개 학부
- DEPARTMENT: 24개 학과
- FACILITY_TBL: 22개 시설
- FACILITY_RESERVATION_TBL: 16개 예약
- FACILITY_RESERVATION_LOG: 29개 로그
- BOARD_TBL: 46개 게시글
- BOARD_ATTACHMENT_TBL: 14개 첨부파일
- LAMP_USAGE_LOG: 18개 사용 로그
- ENROLLMENT_EXTENDED_TBL: 3개 수강
- ASSIGNMENT_EXTENDED_TBL: 1개 과제
- ADMIN_TBL: 5개 관리자 계정

### 주요 특징
- **JSON 데이터 활용**: 수강, 과제, 예약 정보에 JSON 구조 적극 활용
- **실시간 관리**: 열람실 좌석, 시설 예약 상태 실시간 업데이트
- **다중 플랫폼 지원**: FCM 토큰을 통한 Android/iOS/Web 푸시 알림
- **유연한 정책**: 시설별로 승인 정책, 시간 제한 등 개별 설정 가능
- **파일 관리**: MinIO 연동으로 첨부파일 및 프로필 이미지 관리

---

*본 문서는 Blue Crab LMS 데이터베이스의 실제 샘플 데이터를 정리한 문서입니다. 운영 데이터는 더 다양하고 방대할 수 있습니다.*