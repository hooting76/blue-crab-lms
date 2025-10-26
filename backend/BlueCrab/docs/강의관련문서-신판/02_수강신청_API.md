# 수강신청 API 문서

## 📋 개요

수강신청 조회, 등록, 취소, 통계 기능을 제공하는 API 문서입니다.

**컨트롤러**: `EnrollmentController.java`  
**기본 경로**: `/api/enrollments`  
**관련 DB 테이블**: `ENROLLMENT_EXTENDED_TBL`, `LEC_TBL`, `USER_TBL`

---

## 🔍 API 목록

### 1. 수강신청 목록 조회 (통합)

**엔드포인트**: `POST /api/enrollments/list`

**설명**: 다양한 조건으로 수강신청을 조회합니다.

#### Request Body 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| studentIdx | Integer | X | 학생 ID |
| lecSerial | String | X | 강의 코드 |
| checkEnrollment | Boolean | X | 수강 여부 확인 (studentIdx + lecSerial 필요) |
| enrolled | Boolean | X | 현재 수강중인 목록만 (studentIdx 필요) |
| stats | Boolean | X | 통계 조회 |
| page | Integer | X | 페이지 번호 (기본값: 0) |
| size | Integer | X | 페이지 크기 (기본값: 20) |

#### 사용 패턴

##### 1) 수강 여부 확인
```json
POST /api/enrollments/list
{
  "studentIdx": 6,
  "lecSerial": "CS284",
  "checkEnrollment": true
}
```

**응답 예시**:
```json
{
  "enrolled": true,
  "studentIdx": 6,
  "lecSerial": "CS284"
}
```

##### 2) 통계 조회 (특정 강의)
```json
POST /api/enrollments/list
{
  "lecSerial": "CS284",
  "stats": true
}
```

**응답 예시**:
```json
{
  "enrollmentCount": 23,
  "lecSerial": "CS284"
}
```

##### 3) 통계 조회 (전체)
```json
POST /api/enrollments/list
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

##### 4) 학생별 수강 목록
```json
POST /api/enrollments/list
{
  "studentIdx": 6,
  "page": 0,
  "size": 20
}
```

**응답**: Page 객체
```json
{
  "content": [
    {
      "enrollmentIdx": 1,
      "lecIdx": 6,
      "studentIdx": 6,
      "enrollmentData": "{\"attendance\":\"1출2출3결\",\"attendanceRate\":\"2/3\"}"
    }
  ],
  "totalElements": 5,
  "totalPages": 1
}
```

##### 5) 강의별 수강생 목록
```json
POST /api/enrollments/list
{
  "lecSerial": "CS284",
  "page": 0,
  "size": 20
}
```

---

### 2. 수강신청 상세 조회

**엔드포인트**: `POST /api/enrollments/detail`

#### Request Body
```json
{
  "enrollmentIdx": 1
}
```

#### 응답 예시
```json
{
  "enrollmentIdx": 1,
  "lecIdx": 6,
  "studentIdx": 6,
  "studentName": "집갈래",
  "lecTit": "운영체제",
  "enrollmentData": "{\"attendance\":\"1출2출3결\",\"attendanceRate\":\"2/3\",\"midterm\":85,\"final\":90}"
}
```

---

### 3. 수강신청 등록

**엔드포인트**: `POST /api/enrollments/enroll`

**권한**: 학생

#### Request Body
```json
{
  "studentIdx": 6,
  "lecSerial": "CS284"
}
```

#### 응답 예시 (성공)
```json
{
  "success": true,
  "message": "수강신청이 완료되었습니다.",
  "data": {
    "enrollmentIdx": 42,
    "lecIdx": 1,
    "studentIdx": 6,
    "lecSerial": "CS284"
  }
}
```

#### 응답 예시 (실패 - 정원 초과)
```json
{
  "success": false,
  "message": "수강 정원이 초과되었습니다."
}
```

#### 응답 예시 (실패 - 중복 신청)
```json
{
  "success": false,
  "message": "이미 수강신청한 강의입니다."
}
```

---

### 4. 수강신청 취소

**엔드포인트**: `POST /api/enrollments/cancel`

**권한**: 학생 (본인만)

#### Request Body
```json
{
  "enrollmentIdx": 42
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "수강신청이 취소되었습니다."
}
```

---

### 5. 성적 입력

**엔드포인트**: `POST /api/enrollments/grade`

**권한**: 교수 (담당 강의만)

#### Request Body
```json
{
  "enrollmentIdx": 1,
  "midterm": 85,
  "final": 90,
  "assignment": 95,
  "attendance": 100
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "성적이 입력되었습니다.",
  "data": {
    "enrollmentIdx": 1,
    "totalGrade": 92.5,
    "letterGrade": "A+"
  }
}
```

---

### 6. 수강생 목록 (교수용)

**엔드포인트**: `POST /api/enrollments/students`

**권한**: 교수

#### Request Body
```json
{
  "lecSerial": "CS284",
  "page": 0,
  "size": 50
}
```

#### 응답 예시
```json
{
  "content": [
    {
      "enrollmentIdx": 1,
      "studentIdx": 6,
      "studentCode": "240105045",
      "studentName": "집갈래",
      "studentEmail": "stu01@bluecrab.ac.kr",
      "enrollmentData": "{}",
      "lecTit": "컴퓨터과학개론"
    }
  ],
  "totalElements": 23,
  "totalPages": 1
}
```

---

## 📊 DTO 구조

### EnrollmentDto
```java
{
  "enrollmentIdx": Integer,
  "lecIdx": Integer,
  "studentIdx": Integer,
  "studentName": String,      // JOIN 결과
  "studentCode": String,      // JOIN 결과
  "lecTit": String,          // JOIN 결과
  "enrollmentData": String   // JSON 문자열
}
```

### EnrollmentData 구조 (JSON)
```json
{
  "attendance": "1출2출3결4지",
  "attendanceRate": "2/4",
  "midterm": 85,
  "final": 90,
  "assignment": 95,
  "totalGrade": 92.5,
  "letterGrade": "A+"
}
```

---

## 🔗 관련 테이블

### ENROLLMENT_EXTENDED_TBL
- **기본 키**: `ENROLLMENT_IDX`
- **주요 컬럼**:
  - `LEC_IDX`: 강의 ID (FK → LEC_TBL)
  - `STUDENT_IDX`: 학생 ID (FK → USER_TBL)
  - `ENROLLMENT_DATA`: JSON 형식 데이터 (성적, 출석 등)

### 샘플 데이터
```sql
INSERT INTO ENROLLMENT_EXTENDED_TBL VALUES
(1, 6, 6, '{}'),
(2, 7, 6, '{}');
```

---

## 📈 비즈니스 로직

### 수강신청 프로세스
1. 강의 존재 여부 확인 (`lecSerial` → `lecIdx` 변환)
2. 중복 신청 확인
3. 정원 확인 (`lecCurrent` < `lecMany`)
4. 수강신청 등록
5. `lecCurrent` 증가
6. 이벤트 발행 (필요시)

### 성적 계산 로직
- 중간고사: 30%
- 기말고사: 30%
- 과제: 20%
- 출석: 20%

**총점 계산**:
```
totalGrade = (midterm * 0.3) + (final * 0.3) + (assignment * 0.2) + (attendance * 0.2)
```

**학점 변환**:
- A+: 95 이상
- A0: 90~94
- B+: 85~89
- B0: 80~84
- C+: 75~79
- C0: 70~74
- D+: 65~69
- D0: 60~64
- F: 60 미만

---

## ⚠️ 주의사항

1. **lecSerial 사용**: `lecIdx` 대신 `lecSerial` 권장
2. **트랜잭션**: 수강신청/취소 시 `lecCurrent` 업데이트 동기화 필요
3. **권한 검증**: 
   - 학생: 본인 수강신청만 조회/등록/취소 가능
   - 교수: 담당 강의의 수강생만 조회/성적입력 가능
4. **데이터 형식**: `enrollmentData`는 JSON 문자열로 저장

---

## 🔄 이벤트

### GradeUpdateEvent
성적 입력/수정 시 발행되는 이벤트

```java
eventPublisher.publishEvent(
    new GradeUpdateEvent(this, enrollmentIdx, lecIdx, studentIdx)
);
```

---

© 2025 BlueCrab LMS Development Team
