# 과제 API 문서

## 📋 개요

과제 조회, 생성, 제출, 채점, 통계 기능을 제공하는 API 문서입니다.

**컨트롤러**: `AssignmentController.java`  
**기본 경로**: `/api/assignments`  
**관련 DB 테이블**: `ASSIGNMENT_EXTENDED_TBL`, `LEC_TBL`

---

## 🔍 API 목록

### 1. 과제 목록 조회 (통합)

**엔드포인트**: `POST /api/assignments/list`

**설명**: 과제 목록을 조회하거나 통계를 확인합니다.

#### Request Body 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| lecSerial | String | X | 강의 코드 |
| withLecture | Boolean | X | 강의 정보 포함 여부 (lecSerial 필요) |
| stats | Boolean | X | 통계 조회 |
| page | Integer | X | 페이지 번호 (기본값: 0) |
| size | Integer | X | 페이지 크기 (기본값: 20) |

#### 사용 패턴

##### 1) 통계 조회 (특정 강의)
```json
POST /api/assignments/list
{
  "lecSerial": "CS284",
  "stats": true
}
```

**응답 예시**:
```json
{
  "assignmentCount": 5,
  "lecIdx": 1
}
```

##### 2) 통계 조회 (전체)
```json
POST /api/assignments/list
{
  "stats": true
}
```

**응답 예시**:
```json
{
  "totalCount": 150
}
```

##### 3) 강의별 과제 목록 (강의 정보 포함)
```json
POST /api/assignments/list
{
  "lecSerial": "CS284",
  "withLecture": true
}
```

**응답 예시**:
```json
[
  {
    "assignIdx": 1,
    "lecIdx": 1,
    "assignTitle": "자료구조 과제 1",
    "assignContent": "스택과 큐 구현하기",
    "assignDueDate": "2025-11-15T23:59:59",
    "assignMaxScore": 100,
    "assignCreatedAt": "2025-10-20T10:00:00",
    "lecture": {
      "lecIdx": 1,
      "lecSerial": "CS284",
      "lecTit": "컴퓨터과학개론"
    }
  }
]
```

##### 4) 강의별 과제 목록 (페이징)
```json
POST /api/assignments/list
{
  "lecSerial": "CS284",
  "page": 0,
  "size": 10
}
```

**응답**: Page 객체
```json
{
  "content": [
    {
      "assignIdx": 1,
      "lecIdx": 1,
      "assignTitle": "자료구조 과제 1",
      ...
    }
  ],
  "totalElements": 5,
  "totalPages": 1
}
```

##### 5) 전체 과제 목록
```json
POST /api/assignments/list
{
  "page": 0,
  "size": 20
}
```

---

### 2. 과제 상세 조회

**엔드포인트**: `POST /api/assignments/detail`

#### Request Body
```json
{
  "assignIdx": 1
}
```

#### 응답 예시
```json
{
  "assignIdx": 1,
  "lecIdx": 1,
  "lecSerial": "CS284",
  "lecTit": "컴퓨터과학개론",
  "assignTitle": "자료구조 과제 1",
  "assignContent": "스택과 큐를 구현하고 테스트 케이스를 작성하세요.",
  "assignDueDate": "2025-11-15T23:59:59",
  "assignMaxScore": 100,
  "assignCreatedAt": "2025-10-20T10:00:00",
  "assignFiles": "[{\"name\":\"template.zip\",\"url\":\"/files/assignments/1/template.zip\"}]",
  "submissionCount": 23,
  "totalStudents": 50
}
```

---

### 3. 과제 생성

**엔드포인트**: `POST /api/assignments/create`

**권한**: 교수 (담당 강의만)

#### Request Body
```json
{
  "lecSerial": "CS284",
  "assignTitle": "자료구조 과제 2",
  "assignContent": "이진 트리를 구현하세요.",
  "assignDueDate": "2025-11-30T23:59:59",
  "assignMaxScore": 100,
  "assignFiles": "[{\"name\":\"tree_template.zip\",\"url\":\"/files/assignments/template.zip\"}]"
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "과제가 생성되었습니다.",
  "data": {
    "assignIdx": 10,
    "lecIdx": 1,
    "assignTitle": "자료구조 과제 2",
    ...
  }
}
```

---

### 4. 과제 수정

**엔드포인트**: `PUT /api/assignments/{assignIdx}`

**권한**: 교수 (담당 강의만)

#### Request Body
```json
{
  "assignTitle": "자료구조 과제 2 (수정)",
  "assignContent": "이진 트리와 힙을 구현하세요.",
  "assignDueDate": "2025-12-05T23:59:59"
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "과제가 수정되었습니다.",
  "data": {
    "assignIdx": 10,
    "assignTitle": "자료구조 과제 2 (수정)",
    ...
  }
}
```

---

### 5. 과제 삭제

**엔드포인트**: `DELETE /api/assignments/{assignIdx}`

**권한**: 교수 (담당 강의만)

#### 응답 예시
```json
{
  "success": true,
  "message": "과제가 삭제되었습니다."
}
```

---

### 6. 과제 제출

**엔드포인트**: `POST /api/assignments/submit`

**권한**: 학생 (수강 중인 강의만)

#### Request Body
```json
{
  "assignIdx": 1,
  "studentIdx": 6,
  "submissionContent": "과제를 완료했습니다.",
  "submissionFiles": "[{\"name\":\"homework.zip\",\"url\":\"/uploads/students/6/homework.zip\"}]"
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "과제가 제출되었습니다.",
  "data": {
    "submissionIdx": 50,
    "assignIdx": 1,
    "studentIdx": 6,
    "submittedAt": "2025-11-10T14:30:00",
    "isLate": false
  }
}
```

---

### 7. 과제 채점

**엔드포인트**: `POST /api/assignments/grade`

**권한**: 교수 (담당 강의만)

#### Request Body
```json
{
  "submissionIdx": 50,
  "score": 95,
  "feedback": "잘했습니다. 코드 스타일이 우수합니다."
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "채점이 완료되었습니다.",
  "data": {
    "submissionIdx": 50,
    "score": 95,
    "feedback": "잘했습니다. 코드 스타일이 우수합니다.",
    "gradedAt": "2025-11-12T10:00:00"
  }
}
```

---

### 8. 내 제출 내역 조회 (학생용)

**엔드포인트**: `POST /api/assignments/my-submissions`

**권한**: 학생

#### Request Body
```json
{
  "studentIdx": 6,
  "lecSerial": "CS284"
}
```

#### 응답 예시
```json
[
  {
    "submissionIdx": 50,
    "assignIdx": 1,
    "assignTitle": "자료구조 과제 1",
    "submittedAt": "2025-11-10T14:30:00",
    "score": 95,
    "maxScore": 100,
    "isLate": false,
    "feedback": "잘했습니다."
  }
]
```

---

### 9. 제출 현황 조회 (교수용)

**엔드포인트**: `POST /api/assignments/submissions`

**권한**: 교수

#### Request Body
```json
{
  "assignIdx": 1,
  "page": 0,
  "size": 50
}
```

#### 응답 예시
```json
{
  "content": [
    {
      "submissionIdx": 50,
      "studentIdx": 6,
      "studentCode": "240105045",
      "studentName": "집갈래",
      "submittedAt": "2025-11-10T14:30:00",
      "score": 95,
      "isGraded": true,
      "isLate": false
    }
  ],
  "totalElements": 23,
  "totalPages": 1,
  "submissionRate": "46%"
}
```

---

## 📊 DTO 구조

### AssignmentDto
```java
{
  "assignIdx": Integer,
  "lecIdx": Integer,
  "lecSerial": String,
  "lecTit": String,
  "assignTitle": String,
  "assignContent": String,
  "assignDueDate": String (ISO-8601),
  "assignMaxScore": Integer,
  "assignCreatedAt": String (ISO-8601),
  "assignFiles": String (JSON array)
}
```

### AssignmentSubmissionDto
```java
{
  "submissionIdx": Integer,
  "assignIdx": Integer,
  "studentIdx": Integer,
  "submissionContent": String,
  "submissionFiles": String (JSON array),
  "submittedAt": String (ISO-8601),
  "score": Integer,
  "feedback": String,
  "isLate": Boolean,
  "isGraded": Boolean
}
```

---

## 🔗 관련 테이블

### ASSIGNMENT_EXTENDED_TBL
- **기본 키**: `ASSIGN_IDX`
- **주요 컬럼**:
  - `LEC_IDX`: 강의 ID (FK → LEC_TBL)
  - `ASSIGN_TITLE`: 과제 제목
  - `ASSIGN_CONTENT`: 과제 내용
  - `ASSIGN_DUE_DATE`: 마감일
  - `ASSIGN_MAX_SCORE`: 배점
  - `ASSIGN_FILES`: 첨부파일 (JSON)

---

## ⚠️ 주의사항

1. **lecSerial 사용**: `lecIdx` 대신 `lecSerial` 권장
2. **마감일 체크**: 제출 시 `isLate` 자동 계산
3. **파일 처리**: `assignFiles`와 `submissionFiles`는 JSON 배열 문자열
4. **권한 검증**:
   - 교수: 담당 강의 과제만 생성/수정/삭제/채점 가능
   - 학생: 수강 중인 강의 과제만 제출 가능

---

## 📈 비즈니스 로직

### 과제 제출 프로세스
1. 수강 여부 확인
2. 마감일 체크 (`isLate` 계산)
3. 중복 제출 확인 (기존 제출 덮어쓰기 또는 새 버전)
4. 파일 업로드 처리
5. 제출 기록 저장
6. 이벤트 발행 (알림 등)

### 지각 제출 판정
```java
isLate = submittedAt.isAfter(assignDueDate)
```

---

© 2025 BlueCrab LMS Development Team
