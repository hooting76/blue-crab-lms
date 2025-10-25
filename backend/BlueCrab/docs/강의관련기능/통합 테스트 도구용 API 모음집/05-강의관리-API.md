# 📝 강의 관리 API 명세서

> **Base URL**: `/api/lectures`
> 
> **작성일**: 2025-10-25
> 
> **컨트롤러**: `LectureController.java`

---

## 📋 목차
1. [강의 목록 조회](#1-강의-목록-조회)
2. [강의 상세 조회](#2-강의-상세-조회)
3. [강의 통계 조회](#3-강의-통계-조회)
4. [수강 가능 강의 조회](#4-수강-가능-강의-조회)
5. [강의 생성](#5-강의-생성)
6. [강의 수정](#6-강의-수정)
7. [강의 삭제](#7-강의-삭제)

---

## 1. 강의 목록 조회

### `POST /api/lectures`

강의 목록을 조회합니다. 다양한 필터링과 검색 조건을 지원하며, 페이징 처리가 가능합니다.

#### Request Body (모든 필드 선택사항)
```json
{
  "serial": "ETH201",           // [선택] 강의코드로 단일 조회
  "professor": "김교수",         // [선택] 교수명 필터
  "year": 2025,                 // [선택] 학년 필터
  "semester": 1,                // [선택] 학기 필터 (1: 1학기, 2: 2학기)
  "title": "데이터베이스",       // [선택] 강의명 검색 (부분 일치)
  "major": 1,                   // [선택] 전공 코드 필터
  "open": 1,                    // [선택] 개설 여부 (1: 개설, 0: 미개설)
  "page": 0,                    // [선택] 페이지 번호 (기본값: 0)
  "size": 20                    // [선택] 페이지 크기 (기본값: 20)
}
```

#### Response (단일 조회 - serial 사용)
```json
{
  "lecIdx": 48,
  "lecSerial": "ETH201",
  "lecTit": "데이터베이스 설계",
  "lecProf": "3",
  "lecProfName": "김교수",
  "lecSummary": "데이터베이스의 기본 개념과 설계 방법론을 학습합니다.",
  "lecPoint": 3,
  "lecMajor": 1,
  "lecMust": 1,
  "lecTime": "월3,수3",
  "lecAssign": 5,
  "lecOpen": 1,
  "lecMany": 30,
  "lecCurrent": 25,
  "lecMcode": "1",
  "lecMcodeDep": "101",
  "lecMin": 2,
  "lecYear": 2025,
  "lecSemester": 1
}
```

#### Response (목록 조회 - 복합 검색)
```json
{
  "content": [
    {
      "lecIdx": 48,
      "lecSerial": "ETH201",
      "lecTit": "데이터베이스 설계",
      "lecProf": "3",
      "lecProfName": "김교수",
      "lecSummary": "데이터베이스의 기본 개념과 설계 방법론을 학습합니다.",
      "lecPoint": 3,
      "lecMajor": 1,
      "lecMust": 1,
      "lecTime": "월3,수3",
      "lecAssign": 5,
      "lecOpen": 1,
      "lecMany": 30,
      "lecCurrent": 25,
      "lecMcode": "1",
      "lecMcodeDep": "101",
      "lecMin": 2,
      "lecYear": 2025,
      "lecSemester": 1
    }
  ],
  "pageable": {
    "sort": {
      "sorted": false,
      "empty": true
    },
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

#### HTTP Status Codes
- `200 OK`: 조회 성공
- `404 Not Found`: 강의를 찾을 수 없음 (serial 조회 시)
- `500 Internal Server Error`: 서버 오류

#### 조회 방식별 동작
1. **강의코드 단일 조회**: `serial` 파라미터 사용 시
2. **교수별 조회**: `professor`만 사용 시 (배열 형태 반환)
3. **강의명 검색**: `title`만 사용 시 (배열 형태 반환)
4. **복합 검색**: 여러 필터 조합 시 (Page 객체 반환)
5. **전체 목록**: 필터 없음 시 (Page 객체 반환)

#### 테스트 예제 (JavaScript)
```javascript
// 1. 강의코드로 단일 조회
function getLectureBySerial(serial) {
    return fetch(`${API_BASE_URL}/lectures`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            serial: serial
        })
    });
}

// 2. 교수별 강의 목록 조회
function getLecturesByProfessor(professorName) {
    return fetch(`${API_BASE_URL}/lectures`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            professor: professorName
        })
    });
}

// 3. 복합 검색 (개설된 1학기 강의만)
function getOpenLectures(page = 0, size = 20) {
    return fetch(`${API_BASE_URL}/lectures`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            year: 2025,
            semester: 1,
            open: 1,
            page: page,
            size: size
        })
    });
}

// 사용 예시
getLectureBySerial("ETH201")
    .then(response => response.json())
    .then(data => console.log('강의 정보:', data));
```

---

## 2. 강의 상세 조회

### `POST /api/lectures/detail`

강의 코드(lecSerial)를 기반으로 특정 강의의 상세 정보를 조회합니다.

#### Request Body
```json
{
  "lecSerial": "ETH201"         // [필수] 강의 코드
}
```

#### Response (성공)
```json
{
  "lecIdx": 48,
  "lecSerial": "ETH201",
  "lecTit": "데이터베이스 설계",
  "lecProf": "3",
  "lecProfName": "김교수",
  "lecSummary": "데이터베이스의 기본 개념과 설계 방법론을 학습합니다. 관계형 모델, 정규화, SQL 등을 다룹니다.",
  "lecPoint": 3,
  "lecMajor": 1,
  "lecMust": 1,
  "lecTime": "월3,수3",
  "lecAssign": 5,
  "lecOpen": 1,
  "lecMany": 30,
  "lecCurrent": 25,
  "lecMcode": "1",
  "lecMcodeDep": "101",
  "lecMin": 2,
  "lecYear": 2025,
  "lecSemester": 1
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "lecSerial(강의 코드)은 필수입니다."
}
```

#### HTTP Status Codes
- `200 OK`: 조회 성공
- `400 Bad Request`: 파라미터 오류
- `404 Not Found`: 강의를 찾을 수 없음
- `500 Internal Server Error`: 서버 오류

#### 테스트 예제 (JavaScript)
```javascript
// 강의 상세 조회
function getLectureDetail(lecSerial) {
    return fetch(`${API_BASE_URL}/lectures/detail`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            lecSerial: lecSerial
        })
    });
}

// 사용 예시
getLectureDetail("ETH201")
    .then(response => response.json())
    .then(data => {
        console.log(`강의명: ${data.lecTit}`);
        console.log(`교수: ${data.lecProfName}`);
        console.log(`수강인원: ${data.lecCurrent}/${data.lecMany}`);
    });
```

---

## 3. 강의 통계 조회

### `POST /api/lectures/stats`

특정 강의의 통계 정보를 조회합니다. 수강 인원, 과제 현황, 성적 분포 등을 포함합니다.

#### Request Body
```json
{
  "lecSerial": "ETH201"         // [필수] 강의 코드
}
```

#### Response (성공)
```json
{
  "lectureInfo": {
    "lecSerial": "ETH201",
    "lecTit": "데이터베이스 설계",
    "lecProfName": "김교수"
  },
  "enrollmentStats": {
    "totalEnrollments": 25,
    "capacity": 30,
    "enrollmentRate": 83.33
  },
  "assignmentStats": {
    "totalAssignments": 5,
    "avgSubmissionRate": 92.0,
    "avgScore": 85.4
  },
  "attendanceStats": {
    "avgAttendanceRate": 88.5,
    "totalSessions": 16
  },
  "gradeDistribution": {
    "A": 8,
    "B": 12,
    "C": 4,
    "D": 1,
    "F": 0
  }
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "존재하지 않는 강의입니다."
}
```

#### HTTP Status Codes
- `200 OK`: 조회 성공
- `400 Bad Request`: 파라미터 오류 또는 존재하지 않는 강의
- `500 Internal Server Error`: 서버 오류

#### 테스트 예제 (JavaScript)
```javascript
// 강의 통계 조회
function getLectureStats(lecSerial) {
    return fetch(`${API_BASE_URL}/lectures/stats`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            lecSerial: lecSerial
        })
    });
}

// 사용 예시
getLectureStats("ETH201")
    .then(response => response.json())
    .then(data => {
        console.log(`수강률: ${data.enrollmentStats.enrollmentRate}%`);
        console.log(`출석률: ${data.attendanceStats.avgAttendanceRate}%`);
        console.log(`과제 평균 점수: ${data.assignmentStats.avgScore}점`);
    });
```

---

## 4. 수강 가능 강의 조회

### `POST /api/lectures/eligible`

학생의 전공/부전공 정보를 기반으로 수강 가능한 강의 목록을 조회합니다. 0값 규칙(제한 없음)을 적용합니다.

#### Request Body
```json
{
  "studentId": 1,               // [필수] 학생 ID (userIdx)
  "page": 0,                    // [선택] 페이지 번호 (기본값: 0)
  "size": 20                    // [선택] 페이지 크기 (기본값: 20)
}
```

#### Response (성공)
```json
{
  "eligibleLectures": [
    {
      "lecSerial": "ETH201",
      "lecTit": "데이터베이스 설계",
      "lecProf": "3",
      "lecProfName": "김교수",
      "lecPoint": 3,
      "lecTime": "월3,수3",
      "lecCurrent": 25,
      "lecMany": 30,
      "lecMcode": "1",
      "lecMcodeDep": "101",
      "lecMin": 2,
      "eligibilityReason": "전공 일치",
      "isEligible": true
    }
  ],
  "totalCount": 50,
  "eligibleCount": 23,
  "ineligibleCount": 27,
  "pagination": {
    "currentPage": 0,
    "pageSize": 20,
    "totalElements": 23,
    "totalPages": 2
  },
  "studentInfo": {
    "userIdx": 1,
    "userName": "김학생",
    "userEmail": "student@example.com",
    "majorFacultyCode": "1",
    "majorDeptCode": "101",
    "minorFacultyCode": null,
    "minorDeptCode": null,
    "hasMajorInfo": true,
    "hasMinorInfo": false
  }
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "학생 권한이 필요합니다."
}
```

#### HTTP Status Codes
- `200 OK`: 조회 성공
- `400 Bad Request`: 파라미터 오류 또는 권한 오류
- `500 Internal Server Error`: 서버 오류

#### 수강 자격 검증 규칙
1. **개설 여부**: lecOpen = 1인 강의만
2. **정원**: lecCurrent < lecMany인 강의만
3. **학부 코드**: lecMcode = "0" 또는 전공/부전공과 일치
4. **학과 코드**: lecMcodeDep = "0" 또는 전공/부전공과 일치
5. **최소 학년**: lecMin = 0 또는 학생 학년 이상

#### 테스트 예제 (JavaScript)
```javascript
// 수강 가능 강의 조회
function getEligibleLectures(studentId, page = 0, size = 20) {
    return fetch(`${API_BASE_URL}/lectures/eligible`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            studentId: studentId,
            page: page,
            size: size
        })
    });
}

// 사용 예시
getEligibleLectures(1)
    .then(response => response.json())
    .then(data => {
        console.log(`수강 가능: ${data.eligibleCount}/${data.totalCount}강의`);
        data.eligibleLectures.forEach(lecture => {
            console.log(`${lecture.lecSerial}: ${lecture.lecTit} (${lecture.eligibilityReason})`);
        });
    });
```

---

## 5. 강의 생성

### `POST /api/lectures/create`

새로운 강의를 생성합니다. 강의 코드는 고유해야 합니다.

#### Request Body
```json
{
  "lecSerial": "ETH301",
  "lecTit": "고급 데이터베이스",
  "lecProf": "3",
  "lecSummary": "고급 데이터베이스 기법과 최신 동향을 다룹니다.",
  "lecPoint": 3,
  "lecMajor": 1,
  "lecMust": 0,
  "lecTime": "화2,목2",
  "lecAssign": 0,
  "lecOpen": 1,
  "lecMany": 25,
  "lecMcode": "1",
  "lecMcodeDep": "101",
  "lecMin": 3,
  "lecYear": 2025,
  "lecSemester": 2
}
```

#### Response (성공)
```json
{
  "lecIdx": 49,
  "lecSerial": "ETH301",
  "lecTit": "고급 데이터베이스",
  "lecProf": "3",
  "lecSummary": "고급 데이터베이스 기법과 최신 동향을 다룹니다.",
  "lecPoint": 3,
  "lecMajor": 1,
  "lecMust": 0,
  "lecTime": "화2,목2",
  "lecAssign": 0,
  "lecOpen": 1,
  "lecMany": 25,
  "lecCurrent": 0,
  "lecMcode": "1",
  "lecMcodeDep": "101",
  "lecMin": 3,
  "lecYear": 2025,
  "lecSemester": 2
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "이미 존재하는 강의 코드입니다."
}
```

#### HTTP Status Codes
- `201 Created`: 생성 성공
- `400 Bad Request`: 입력 데이터 오류
- `500 Internal Server Error`: 서버 오류

#### 필수 필드
- `lecSerial`: 강의 코드 (고유값)
- `lecTit`: 강의명
- `lecProf`: 교수 ID (userIdx)

#### 테스트 예제 (JavaScript)
```javascript
// 강의 생성
function createLecture(lectureData) {
    return fetch(`${API_BASE_URL}/lectures/create`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(lectureData)
    });
}

// 사용 예시
const newLecture = {
    lecSerial: "ETH301",
    lecTit: "고급 데이터베이스",
    lecProf: "3",
    lecSummary: "고급 데이터베이스 기법과 최신 동향을 다룹니다.",
    lecPoint: 3,
    lecMajor: 1,
    lecMust: 0,
    lecTime: "화2,목2",
    lecOpen: 1,
    lecMany: 25,
    lecMcode: "1",
    lecMcodeDep: "101",
    lecMin: 3,
    lecYear: 2025,
    lecSemester: 2
};

createLecture(newLecture)
    .then(response => response.json())
    .then(data => {
        if (response.status === 201) {
            console.log('강의 생성 성공:', data.lecSerial);
        }
    });
```

---

## 6. 강의 수정

### `POST /api/lectures/update`

기존 강의 정보를 수정합니다. 강의 코드(lecSerial)는 수정할 수 없습니다.

#### Request Body
```json
{
  "lecSerial": "ETH201",        // [필수] 수정할 강의 코드 (식별자)
  "lecTit": "데이터베이스 시스템",  // [선택] 수정할 필드들
  "lecSummary": "데이터베이스 시스템의 이론과 실습을 다룹니다.",
  "lecPoint": 3,
  "lecTime": "월3,수3,금2",
  "lecMany": 35,
  "lecOpen": 1
}
```

#### Response (성공)
```json
{
  "lecIdx": 48,
  "lecSerial": "ETH201",
  "lecTit": "데이터베이스 시스템",
  "lecProf": "3",
  "lecSummary": "데이터베이스 시스템의 이론과 실습을 다룹니다.",
  "lecPoint": 3,
  "lecMajor": 1,
  "lecMust": 1,
  "lecTime": "월3,수3,금2",
  "lecAssign": 5,
  "lecOpen": 1,
  "lecMany": 35,
  "lecCurrent": 25,
  "lecMcode": "1",
  "lecMcodeDep": "101",
  "lecMin": 2,
  "lecYear": 2025,
  "lecSemester": 1
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "존재하지 않는 강의입니다."
}
```

#### HTTP Status Codes
- `200 OK`: 수정 성공
- `400 Bad Request`: 파라미터 오류
- `500 Internal Server Error`: 서버 오류

#### 수정 가능한 필드
- `lecTit`: 강의명
- `lecProf`: 교수 ID
- `lecSummary`: 강의 설명
- `lecPoint`: 학점
- `lecMajor`: 전공 구분
- `lecMust`: 필수/선택 구분
- `lecTime`: 강의 시간
- `lecAssign`: 과제 수
- `lecOpen`: 개설 여부
- `lecMany`: 수강 정원
- `lecMcode`: 학부 코드
- `lecMcodeDep`: 학과 코드
- `lecMin`: 최소 학년
- `lecYear`: 학년도
- `lecSemester`: 학기

#### 테스트 예제 (JavaScript)
```javascript
// 강의 정보 수정
function updateLecture(lecSerial, updateData) {
    const requestData = {
        lecSerial: lecSerial,
        ...updateData
    };
    
    return fetch(`${API_BASE_URL}/lectures/update`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(requestData)
    });
}

// 사용 예시 - 강의명과 정원 수정
updateLecture("ETH201", {
    lecTit: "데이터베이스 시스템",
    lecMany: 35,
    lecTime: "월3,수3,금2"
})
.then(response => response.json())
.then(data => {
    console.log('수정 완료:', data.lecTit);
});
```

---

## 7. 강의 삭제

### `POST /api/lectures/delete`

기존 강의를 삭제합니다. 수강생이 있는 강의는 삭제할 수 없습니다.

#### Request Body
```json
{
  "lecSerial": "ETH301"         // [필수] 삭제할 강의 코드
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "강의가 삭제되었습니다."
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "수강생이 있는 강의는 삭제할 수 없습니다."
}
```

#### HTTP Status Codes
- `200 OK`: 삭제 성공
- `400 Bad Request`: 파라미터 오류 또는 삭제 불가 조건
- `500 Internal Server Error`: 서버 오류

#### 삭제 제약 조건
1. **수강생 존재**: lecCurrent > 0인 강의는 삭제 불가
2. **과제 존재**: 등록된 과제가 있는 강의는 삭제 불가
3. **성적 데이터**: 성적이 입력된 강의는 삭제 불가

#### 테스트 예제 (JavaScript)
```javascript
// 강의 삭제
function deleteLecture(lecSerial) {
    return fetch(`${API_BASE_URL}/lectures/delete`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            lecSerial: lecSerial
        })
    });
}

// 사용 예시
deleteLecture("ETH301")
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log('삭제 완료:', data.message);
        } else {
            console.error('삭제 실패:', data.message);
        }
    });
```

---

## 💡 중요 참고사항

### 강의 코드 (lecSerial)
- **고유 식별자**: 각 강의를 구분하는 고유값
- **변경 불가**: 생성 후 수정 불가능
- **형식 예시**: "ETH201", "MAT101", "ENG301"

### 교수 정보 (lecProf)
- **저장 형태**: UserTbl의 userIdx (문자열)
- **응답 형태**: lecProf(ID) + lecProfName(이름)
- **권한 확인**: userStudent = 1인 사용자만 교수

### 0값 규칙 (제한 없음)
- **lecMcode = "0"**: 모든 학부 수강 가능
- **lecMcodeDep = "0"**: 모든 학과 수강 가능
- **lecMin = 0**: 모든 학년 수강 가능

### 페이징 처리
- **Page 객체**: Spring Data의 표준 페이징 응답
- **기본값**: page=0, size=20
- **정렬**: 기본적으로 lecIdx 순서

### 비즈니스 로직
1. **수강 정원 관리**: lecCurrent ≤ lecMany
2. **개설 상태**: lecOpen = 1인 강의만 수강 신청 가능
3. **전공 제한**: 학생의 전공/부전공과 강의의 학부/학과 코드 매칭
4. **데이터 무결성**: 외래키 제약 조건으로 데이터 일관성 보장

### 연관 시스템
- **수강 관리**: EnrollmentController와 연동
- **과제 관리**: AssignmentController와 연동
- **출석 관리**: AttendanceController와 연동
- **성적 관리**: 자동 성적 계산 시스템과 연동