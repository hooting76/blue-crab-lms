# 데이터베이스 구현 완료 현황 (v2.1)

> **작성일**: 2025-10-10  
> **상태**: ✅ Phase 1-2 완료 (4개 Task 완료)

---

## ✅ 완료된 작업

### **Phase 1: 기존 테이블 확장**

#### Task 1: USER_TBL 확장 ✅
```sql
-- 실행 완료
ALTER TABLE USER_TBL 
ADD COLUMN LECTURE_EVALUATIONS LONGTEXT COMMENT '강의 평가 데이터 (JSON 배열)';
```

#### Task 2: LEC_TBL 확장 ✅
```sql
-- 실행 완료
ALTER TABLE LEC_TBL 
ADD COLUMN LEC_CURRENT INT(11) NULL DEFAULT '0' COMMENT '현재 수강 인원',
ADD COLUMN LEC_YEAR INT(11) NULL DEFAULT NULL COMMENT '대상 학년 (1~4학년)',
ADD COLUMN LEC_SEMESTER INT(1) NULL DEFAULT NULL COMMENT '학기 (1학기:1, 2학기:2)';
```

---

### **Phase 2: 신규 테이블 생성**

#### Task 3: ENROLLMENT_EXTENDED_TBL 생성 ✅
```sql
-- 실행 완료
CREATE TABLE `ENROLLMENT_EXTENDED_TBL` (
  `ENROLLMENT_IDX` INT(200) NOT NULL AUTO_INCREMENT COMMENT '수강 고유번호',
  `LEC_IDX` INT(200) NOT NULL COMMENT '강의 IDX (FK)',
  `STUDENT_IDX` INT(200) NOT NULL COMMENT '학생 IDX (FK)',
  `ENROLLMENT_DATA` LONGTEXT NULL DEFAULT NULL COMMENT 'JSON 데이터 (수강/출결/성적)',
  PRIMARY KEY (`ENROLLMENT_IDX`) USING BTREE,
  INDEX `FK_LEC` (`LEC_IDX`) USING BTREE,
  INDEX `FK_STUDENT` (`STUDENT_IDX`) USING BTREE,
  CONSTRAINT `FK_ENROLLMENT_LEC` FOREIGN KEY (`LEC_IDX`) REFERENCES `LEC_TBL` (`LEC_IDX`) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `FK_ENROLLMENT_STUDENT` FOREIGN KEY (`STUDENT_IDX`) REFERENCES `USER_TBL` (`USER_IDX`) ON UPDATE CASCADE ON DELETE CASCADE
)
COLLATE='utf8mb3_general_ci'
ENGINE=InnoDB;
```

#### Task 4: ASSIGNMENT_EXTENDED_TBL 생성 ✅
```sql
-- 실행 완료
CREATE TABLE `ASSIGNMENT_EXTENDED_TBL` (
  `ASSIGNMENT_IDX` INT(200) NOT NULL AUTO_INCREMENT COMMENT '과제 고유번호',
  `LEC_IDX` INT(200) NOT NULL COMMENT '강의 IDX (FK)',
  `ASSIGNMENT_DATA` LONGTEXT NULL DEFAULT NULL COMMENT 'JSON 데이터 (과제정보/제출목록)',
  PRIMARY KEY (`ASSIGNMENT_IDX`) USING BTREE,
  INDEX `FK_ASSIGNMENT_LEC` (`LEC_IDX`) USING BTREE,
  CONSTRAINT `FK_ASSIGNMENT_LEC` FOREIGN KEY (`LEC_IDX`) REFERENCES `LEC_TBL` (`LEC_IDX`) ON UPDATE CASCADE ON DELETE CASCADE
)
COLLATE='utf8mb3_general_ci'
ENGINE=InnoDB;
```

---

## 📊 최종 데이터베이스 구조

### 1. USER_TBL (확장됨)
- 기존 컬럼 유지
- **신규**: `LECTURE_EVALUATIONS` (LONGTEXT)

### 2. LEC_TBL (확장됨)
- 기존 컬럼 유지
- **신규**: `LEC_CURRENT` (INT)
- **신규**: `LEC_YEAR` (INT) - 1~4학년
- **신규**: `LEC_SEMESTER` (INT) - 1 또는 2

### 3. ENROLLMENT_EXTENDED_TBL (신규)
- `ENROLLMENT_IDX` (PK)
- `LEC_IDX` (FK → LEC_TBL)
- `STUDENT_IDX` (FK → USER_TBL)
- `ENROLLMENT_DATA` (LONGTEXT) - JSON 통합 데이터

### 4. ASSIGNMENT_EXTENDED_TBL (신규)
- `ASSIGNMENT_IDX` (PK)
- `LEC_IDX` (FK → LEC_TBL)
- `ASSIGNMENT_DATA` (LONGTEXT) - JSON 통합 데이터

---

## 🎯 JSON 데이터 구조

### ENROLLMENT_DATA 예시
```json
{
  "enrollmentStatus": "ENROLLED",
  "enrolledAt": "2025-03-01T10:00:00",
  "attendance": [
    {
      "date": "2025-03-05",
      "status": "PRESENT"
    }
  ],
  "grade": {
    "midtermScore": 85,
    "finalScore": 90,
    "totalScore": 90,
    "letterGrade": "A"
  }
}
```

### ASSIGNMENT_DATA 예시
```json
{
  "title": "1주차 과제",
  "description": "Spring Boot 기초",
  "dueDate": "2025-03-20T23:59:59",
  "maxScore": 100,
  "submissions": [
    {
      "studentIdx": 1,
      "submittedAt": "2025-03-15T14:30:00",
      "score": 95
    }
  ]
}
```

---

## 📋 체크리스트

- ✅ USER_TBL.LECTURE_EVALUATIONS 추가
- ✅ LEC_TBL.LEC_CURRENT 추가
- ✅ LEC_TBL.LEC_YEAR 추가 (1~4학년)
- ✅ LEC_TBL.LEC_SEMESTER 추가 (1 또는 2)
- ✅ ENROLLMENT_EXTENDED_TBL 생성
- ✅ ASSIGNMENT_EXTENDED_TBL 생성
- ✅ 외래키 제약조건 설정
- ✅ 인덱스 설정

---

## 🚀 다음 단계

**Phase 3: JPA 엔티티 생성**
- [ ] LectureTbl.java 업데이트
- [ ] EnrollmentExtendedTbl.java 생성
- [ ] AssignmentExtendedTbl.java 생성
- [ ] Repository 인터페이스 생성
- [ ] Service 계층 구현

---

**총 변경사항**: 기존 테이블 2개 확장 + 신규 테이블 2개 생성
