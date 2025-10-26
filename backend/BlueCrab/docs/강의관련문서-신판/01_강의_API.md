# 강의 API 문서

## 📋 개요

강의 조회, 검색, 생성, 수정, 삭제 기능을 제공하는 API 문서입니다.

**컨트롤러**: `LectureController.java`  
**기본 경로**: `/api/lectures`  
**관련 DB 테이블**: `LEC_TBL`, `FACULTY`, `DEPARTMENT`, `USER_TBL`, `PROFILE_VIEW`

---

## 🔍 API 목록

### 1. 강의 목록 조회 및 검색 (통합)

**엔드포인트**: `POST /api/lectures`

**설명**: 다양한 조건으로 강의를 조회하거나 검색합니다. Request Body의 파라미터 조합에 따라 다른 동작을 수행합니다.

#### Request Body 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| serial | String | X | 강의코드 (단일 조회) |
| professor | String | X | 교수명 필터 |
| year | Integer | X | 학년 필터 |
| semester | Integer | X | 학기 필터 (1: 1학기, 2: 2학기) |
| title | String | X | 강의명 검색 (부분 일치) |
| major | Integer | X | 전공 코드 필터 |
| open | Integer | X | 개설 여부 (1: 개설, 0: 미개설) |
| page | Integer | X | 페이지 번호 (기본값: 0) |
| size | Integer | X | 페이지 크기 (기본값: 20) |

#### 사용 패턴

##### 1) 강의코드로 단일 조회
```json
POST /api/lectures
{
  "serial": "CS284"
}
```

**응답 예시**:
```json
{
  "lecIdx": 1,
  "lecSerial": "CS284",
  "lecTit": "컴퓨터과학개론",
  "lecProf": 6,
  "professorName": "김교수",
  "lecPoint": 3,
  "lecMajor": 1,
  "lecMust": 1,
  "lecSummary": "컴퓨터 과학의 기초적인 개념을 학습합니다.",
  "lecTime": "월1월2수1수2",
  "lecAssign": 1,
  "lecOpen": 1,
  "lecMany": 50,
  "lecCurrent": 0,
  "lecYear": null,
  "lecSemester": null
}
```

##### 2) 교수별 강의 조회
```json
POST /api/lectures
{
  "professor": "김교수"
}
```

**응답**: 배열 형태로 반환
```json
[
  {
    "lecIdx": 1,
    "lecSerial": "CS284",
    "lecTit": "컴퓨터과학개론",
    "professorName": "김교수",
    ...
  },
  ...
]
```

##### 3) 강의명 검색
```json
POST /api/lectures
{
  "title": "컴퓨터"
}
```

**응답**: 강의명에 "컴퓨터"가 포함된 강의 목록 (배열)

##### 4) 복합 검색 (페이징)
```json
POST /api/lectures
{
  "year": 2,
  "semester": 1,
  "major": 1,
  "open": 1,
  "page": 0,
  "size": 20
}
```

**응답**: Page 객체
```json
{
  "content": [ /* 강의 목록 */ ],
  "pageable": { /* 페이징 정보 */ },
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

##### 5) 전체 목록 조회
```json
POST /api/lectures
{
  "page": 0,
  "size": 20
}
```

---

### 2. 강의 상세 조회

**엔드포인트**: `POST /api/lectures/detail`

**설명**: 강의 상세 정보와 수강 인원을 조회합니다.

#### Request Body
```json
{
  "lecIdx": 1
}
```

#### 응답 예시
```json
{
  "lecIdx": 1,
  "lecSerial": "CS284",
  "lecTit": "컴퓨터과학개론",
  "professorName": "김교수",
  "lecPoint": 3,
  "lecSummary": "컴퓨터 과학의 기초적인 개념을 학습합니다.",
  "lecTime": "월1월2수1수2",
  "lecMany": 50,
  "lecCurrent": 23,
  "lecOpen": 1
}
```

---

### 3. 강의 생성

**엔드포인트**: `POST /api/lectures/create`

**권한**: 관리자/교수

#### Request Body
```json
{
  "lecSerial": "CS101",
  "lecTit": "프로그래밍 기초",
  "lecProf": 6,
  "lecPoint": 3,
  "lecMajor": 1,
  "lecMust": 1,
  "lecSummary": "프로그래밍의 기초를 배웁니다.",
  "lecTime": "월1월2수1수2",
  "lecAssign": 1,
  "lecOpen": 1,
  "lecMany": 50,
  "lecMcode": "05",
  "lecMcodeDep": "01",
  "lecMin": 1,
  "lecYear": 2,
  "lecSemester": 1
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "강의가 생성되었습니다.",
  "data": {
    "lecIdx": 67,
    "lecSerial": "CS101",
    ...
  }
}
```

---

### 4. 강의 수정

**엔드포인트**: `PUT /api/lectures/{lecIdx}`

**권한**: 관리자/교수

#### Request Body
```json
{
  "lecTit": "프로그래밍 기초 (수정)",
  "lecSummary": "프로그래밍의 기초를 배웁니다. (업데이트)",
  "lecMany": 60,
  "lecOpen": 1
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "강의가 수정되었습니다.",
  "data": {
    "lecIdx": 67,
    "lecTit": "프로그래밍 기초 (수정)",
    ...
  }
}
```

---

### 5. 강의 삭제

**엔드포인트**: `DELETE /api/lectures/{lecIdx}`

**권한**: 관리자/교수

#### 응답 예시
```json
{
  "success": true,
  "message": "강의가 삭제되었습니다."
}
```

---

### 6. 강의 통계

**엔드포인트**: `POST /api/lectures/stats`

#### Request Body (전체 통계)
```json
{}
```

#### Request Body (특정 강의)
```json
{
  "lecSerial": "CS284"
}
```

#### 응답 예시
```json
{
  "totalLectures": 66,
  "openLectures": 45,
  "closedLectures": 21
}
```

또는

```json
{
  "lecSerial": "CS284",
  "lecTit": "컴퓨터과학개론",
  "totalEnrollments": 23,
  "capacity": 50,
  "availableSeats": 27
}
```

---

## 📊 DTO 구조

### LectureDto
```java
{
  "lecIdx": Integer,
  "lecSerial": String,
  "lecTit": String,
  "lecProf": Integer,
  "professorName": String,  // JOIN 결과
  "lecPoint": Integer,
  "lecMajor": Integer,
  "lecMust": Integer,
  "lecSummary": String,
  "lecTime": String,
  "lecAssign": Integer,
  "lecOpen": Integer,
  "lecMany": Integer,
  "lecMcode": String,
  "lecMcodeDep": String,
  "lecMin": Integer,
  "lecCurrent": Integer,
  "lecYear": Integer,
  "lecSemester": Integer
}
```

---

## 🔗 관련 테이블

### LEC_TBL
- **기본 키**: `LEC_IDX`
- **주요 컬럼**:
  - `LEC_SERIAL`: 강의 코드 (예: CS284)
  - `LEC_TIT`: 강의명
  - `LEC_PROF`: 교수 USER_IDX (FK)
  - `LEC_POINT`: 학점
  - `LEC_MANY`: 정원
  - `LEC_CURRENT`: 현재 수강 인원
  - `LEC_OPEN`: 개설 여부 (1/0)

### FACULTY
- **기본 키**: `faculty_id`
- **주요 컬럼**: `faculty_code`, `faculty_name`

### DEPARTMENT
- **기본 키**: `dept_id`
- **주요 컬럼**: `dept_code`, `dept_name`, `faculty_id`

---

## ⚠️ 주의사항

1. **lecSerial 우선**: 가능하면 `lecIdx` 대신 `lecSerial` 사용
2. **권한 검증**: 생성/수정/삭제는 교수/관리자만 가능
3. **정원 체크**: 수강신청 시 `lecMany`와 `lecCurrent` 비교 필요
4. **페이징**: 대량 조회 시 `page`, `size` 파라미터 사용 권장

---

© 2025 BlueCrab LMS Development Team
