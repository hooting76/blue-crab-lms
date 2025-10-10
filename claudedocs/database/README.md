# 데이터베이스 스키마 문서

> **폴더**: `claudedocs/database/`  
> **목적**: Blue Crab LMS 데이터베이스 스키마 관리

---

## 📄 파일 목록

### 1. database-schema.md

**데이터베이스 스키마 상세 문서 (Markdown)**

✅ **포함 내용**:
- 전체 테이블 목록 (18개)
- ERD 다이어그램
- 테이블별 상세 스키마
- 인덱스 및 외래키 정보
- 데이터 타입 및 제약조건
- VIEW 정의
- Trigger 정의

📖 **대상**: 개발자, DBA, 아키텍트

---

### 2. blue_crab_schema.sql

**데이터베이스 생성 SQL 스크립트**

✅ **포함 내용**:
- CREATE TABLE 문 (전체 18개 테이블)
- 인덱스 정의
- 외래키 제약조건
- TRIGGER 정의
- VIEW 정의
- 주석 및 설명

🔧 **사용 방법**:
```bash
# SSH 접속
ssh project01@121.165.24.26

# 데이터베이스 생성
mysql -h 121.165.24.26 -P 55511 -u KDT_project -p'Kdtkdt!1120' < blue_crab_schema.sql
```

---

## 🗂️ 테이블 분류

### 사용자 관리 (2개)
- **USER_TBL**: 사용자 (학생/교수)
- **ADMIN_TBL**: 관리자

### 게시판 시스템 (2개)
- **BOARD_TBL**: 게시판
- **BOARD_ATTACHMENT_TBL**: 게시글 첨부파일

### 시설 예약 시스템 (4개)
- **FACILITY_TBL**: 시설
- **FACILITY_RESERVATION_TBL**: 시설 예약
- **FACILITY_BLOCK_TBL**: 시설 마감
- **FACILITY_RESERVATION_LOG**: 시설 예약 로그

### FCM 푸시 알림 (1개)
- **FCM_TOKEN_TABLE**: FCM 토큰 관리

### 열람실 시스템 (2개)
- **LAMP_TBL**: 열람실 좌석 현황
- **LAMP_USAGE_LOG**: 열람실 사용 기록

### 학사 시스템 (5개)
- **FACULTY**: 학부 마스터
- **DEPARTMENT**: 학과 마스터
- **LEC_TBL**: 강의
- **REGIST_TABLE**: 학생 학적 이력
- **SERIAL_CODE_TABLE**: 학생 전공/부전공 정보

### 레거시 (1개)
- **RENT_TABLE**: 시설물 대여 (레거시)

### VIEW (1개)
- **PROFILE_VIEW**: 사용자 통합 프로필 조회

---

## 📊 ERD 다이어그램

### 전체 구조

```
사용자 관리
┌─────────────┐
│  USER_TBL   │─┬──► FCM_TOKEN_TABLE
│  ADMIN_TBL  │ │
└─────────────┘ ├──► REGIST_TABLE
                ├──► SERIAL_CODE_TABLE
                └──► LAMP_TBL ──► LAMP_USAGE_LOG

게시판 시스템
┌─────────────┐
│  BOARD_TBL  │──► BOARD_ATTACHMENT_TBL
└─────────────┘

시설 예약 시스템
┌──────────────┐
│ FACILITY_TBL │─┬──► FACILITY_RESERVATION_TBL ──► FACILITY_RESERVATION_LOG
└──────────────┘ │
                 └──► FACILITY_BLOCK_TBL

학사 시스템
┌─────────────┐
│   FACULTY   │──► DEPARTMENT
│   LEC_TBL   │
└─────────────┘
```

---

## 🔐 데이터베이스 연결 정보

### 운영 환경

```properties
# JDBC URL
jdbc:mariadb://121.165.24.26:55511/blue_crab?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul

# 데이터베이스 정보
호스트: 121.165.24.26
포트: 55511
데이터베이스: blue_crab
문자셋: utf8mb3, utf8mb4
엔진: InnoDB
```

### HikariCP 설정

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000
```

---

## 🔧 유지보수 가이드

### 스키마 백업

```bash
# 구조만 백업 (no-data)
mysqldump -h 121.165.24.26 -P 55511 -u KDT_project -p'Kdtkdt!1120' \
  --no-data --skip-comments blue_crab > blue_crab_schema_backup.sql

# 구조 + 데이터 백업
mysqldump -h 121.165.24.26 -P 55511 -u KDT_project -p'Kdtkdt!1120' \
  blue_crab > blue_crab_full_backup.sql
```

### 스키마 복원

```bash
# 스키마 복원
mysql -h 121.165.24.26 -P 55511 -u KDT_project -p'Kdtkdt!1120' \
  blue_crab < blue_crab_schema.sql
```

### 테이블 통계 조회

```sql
SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    DATA_LENGTH,
    INDEX_LENGTH,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS 'SIZE_MB'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'blue_crab'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;
```

---

## ⚠️ 주의사항

### Base64 인코딩

**BOARD_TBL**의 `BOARD_TIT`, `BOARD_CONT`는 Base64 인코딩되어 저장됩니다.

```java
// 저장 시
String encodedTitle = Base64.getEncoder().encodeToString(title.getBytes());

// 조회 시
String decodedTitle = new String(Base64.getDecoder().decode(encodedTitle));
```

### 문자 인코딩

- **utf8mb3**: 레거시 테이블 (USER_TBL, ADMIN_TBL, BOARD_TBL, LEC_TBL, LAMP_TBL, RENT_TABLE)
- **utf8mb4**: 신규 테이블 (이모지 지원)

### Trigger

**auto_student_regist**: 학생 (USER_STUDENT = 0) 가입 시 자동으로 REGIST_TABLE에 기본 학적 생성

---

## 📅 스키마 변경 이력

| 날짜 | 테이블 | 변경 내용 | 작성자 |
|------|--------|----------|--------|
| 2025-10-10 | 전체 | SSH 접속으로 실제 스키마 확인 및 문서화 | GitHub Copilot |

---

## 🔗 관련 문서

- [기술 스택 및 버전 정보](../tech-stack/기술스택_및_버전정보.md)
- [백엔드 폴더 구조](../backend-guide/백엔드_폴더구조_빠른참조.md)
- [API 문서](../api-endpoints/api-documentation.md)
- [시설 예약 시스템](../feature-docs/facility-reservation/README.md)
- [FCM 푸시 알림](../feature-docs/fcm/README.md)

---

## 📚 추가 자료

### 데이터베이스 모델링 도구

- **dbdiagram.io**: ERD 온라인 생성
- **MySQL Workbench**: 스키마 시각화
- **DBeaver**: 범용 데이터베이스 클라이언트

### SQL 쿼리 최적화

```sql
-- 인덱스 사용 확인
EXPLAIN SELECT * FROM FACILITY_RESERVATION_TBL 
WHERE FACILITY_IDX = 1 AND START_TIME >= '2025-10-10';

-- 느린 쿼리 로그 확인
SHOW VARIABLES LIKE 'slow_query_log';
```

---

**작성자**: GitHub Copilot  
**최종 수정일**: 2025-10-10  
**버전**: 1.0  
**다음 업데이트**: 스키마 변경 시
