# 02. API 명세서

> **작성일**: 2025-10-09
> **버전**: 1.0

---

## 🔗 API 명세서 개요

강의 관리 시스템의 REST API 엔드포인트 명세를 정의합니다. JWT 기반 인증을 사용하는 Spring Boot REST API입니다.

---

## 📋 목차

1. [API 설계 원칙](#1-api-설계-원칙)
2. [인증 및 인가](#2-인증-및-인가)
3. [관리자 API](#3-관리자-api)
4. [교수 API](#4-교수-api)
5. [학생 API](#5-학생-api)
6. [공통 API](#6-공통-api)
7. [에러 코드](#7-에러-코드)

---

## 1. API 설계 원칙

### **RESTful 설계**
- **HTTP Methods**: GET, POST, PUT, DELETE 적절히 사용
- **Resource Naming**: 복수형 명사 사용 (`/lectures`, `/enrollments`)
- **Status Codes**: 표준 HTTP 상태 코드 사용
- **Versioning**: URL 경로에 버전 포함 (`/api/v1/`)

### **응답 포맷**
```json
// 성공 응답
{
  "success": true,
  "data": { ... },
  "message": "요청이 성공적으로 처리되었습니다."
}

// 에러 응답
{
  "success": false,
  "error": {
    "code": "ENROLLMENT_FULL",
    "message": "강의 정원이 초과되었습니다."
  }
}

// 페이지네이션 응답
{
  "success": true,
  "data": {
    "content": [ ... ],
    "pageable": {
      "page": 0,
      "size": 10,
      "totalElements": 25,
      "totalPages": 3
    }
  }
}
```

---

## 2. 인증 및 인가

### **JWT 토큰 구조**
```javascript
// 헤더
{
  "alg": "HS256",
  "typ": "JWT"
}

// 페이로드
{
  "userIdx": 123,
  "username": "student001",
  "role": "ROLE_STUDENT",
  "department": "CS",
  "iat": 1638360000,
  "exp": 1638363600
}
```

### **인증 API**

#### **로그인**
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "student001",
  "password": "password123"
}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "userIdx": 123,
      "username": "student001",
      "name": "홍길동",
      "role": "ROLE_STUDENT",
      "department": "CS"
    }
  }
}
```

#### **토큰 갱신**
```http
POST /api/v1/auth/refresh
Authorization: Bearer {refreshToken}
```

---

## 3. 관리자 API

### **강의 관리**

#### **강의 목록 조회**
```http
GET /api/v1/admin/lectures?page=0&size=10&year=2025&semester=1
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "lectureIdx": 1,
        "lectureName": "자바 프로그래밍",
        "lectureCode": "CS101",
        "professorName": "김교수",
        "currentStudents": 25,
        "maxStudents": 30,
        "status": "ACTIVE"
      }
    ],
    "pageable": {
      "page": 0,
      "size": 10,
      "totalElements": 25
    }
  }
}
```

#### **강의 등록**
```http
POST /api/v1/admin/lectures
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "lectureName": "자바 프로그래밍",
  "lectureCode": "CS101",
  "lectureDescription": "자바 기초 프로그래밍 강의",
  "maxStudents": 30,
  "credit": 3,
  "lectureTime": "월1,2 수3,4",
  "lectureRoom": "공학관 101호",
  "professorIdx": 1,
  "year": 2025,
  "semester": 1,
  "startDate": "2025-03-01",
  "endDate": "2025-06-30"
}
```

#### **강의 수정**
```http
PUT /api/v1/admin/lectures/{lectureIdx}
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "lectureName": "심화 자바 프로그래밍",
  "maxStudents": 25
}
```

#### **강의 삭제 (폐강)**
```http
DELETE /api/v1/admin/lectures/{lectureIdx}
Authorization: Bearer {accessToken}
```

### **평가 항목 관리**

#### **평가 항목 목록 조회**
```http
GET /api/v1/admin/evaluation-items
Authorization: Bearer {accessToken}
```

#### **평가 항목 등록**
```http
POST /api/v1/admin/evaluation-items
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "itemName": "강의 내용의 적절성",
  "itemType": "CONTENT"
}
```

### **통계 및 모니터링**

#### **강의 통계 조회**
```http
GET /api/v1/admin/statistics/lectures?year=2025&semester=1
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "totalLectures": 25,
    "activeLectures": 23,
    "cancelledLectures": 2,
    "totalEnrollments": 450,
    "averageEnrollmentRate": 85.2
  }
}
```

#### **학생별 통계 조회**
```http
GET /api/v1/admin/statistics/students/{studentIdx}
Authorization: Bearer {accessToken}
```

---

## 4. 교수 API

### **강의 운영**

#### **담당 강의 목록 조회**
```http
GET /api/v1/professor/lectures
Authorization: Bearer {accessToken}
```

#### **강의 상세 정보 조회**
```http
GET /api/v1/professor/lectures/{lectureIdx}
Authorization: Bearer {accessToken}
```

### **출결 관리**

#### **출결 요청 목록 조회**
```http
GET /api/v1/professor/attendance/requests?lectureIdx=1&page=0&size=10
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "attendanceIdx": 1,
        "studentName": "홍길동",
        "studentNo": "2025001",
        "attendanceDate": "2025-03-15",
        "requestReason": "병원 진료",
        "requestedAt": "2025-03-15T09:00:00",
        "status": "PENDING"
      }
    ]
  }
}
```

#### **출결 승인/거부**
```http
PUT /api/v1/professor/attendance/{attendanceIdx}/approve
Authorization: Bearer {accessToken}
```

```http
PUT /api/v1/professor/attendance/{attendanceIdx}/reject
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "reason": "사유 불충분"
}
```

### **성적 관리**

#### **강의별 학생 목록 및 성적 조회**
```http
GET /api/v1/professor/grades?lectureIdx=1
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "data": [
    {
      "enrollmentIdx": 1,
      "studentName": "홍길동",
      "studentNo": "2025001",
      "midtermScore": 85.5,
      "finalScore": 92.0,
      "assignmentScore": 88.0,
      "participationScore": 90.0,
      "totalScore": 88.9,
      "grade": "A"
    }
  ]
}
```

#### **성적 일괄 입력**
```http
PUT /api/v1/professor/grades/batch
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "grades": [
    {
      "enrollmentIdx": 1,
      "midtermScore": 85.5,
      "finalScore": 92.0,
      "assignmentScore": 88.0,
      "participationScore": 90.0
    }
  ]
}
```

#### **성적 확정**
```http
PUT /api/v1/professor/grades/finalize?lectureIdx=1
Authorization: Bearer {accessToken}
```

### **과제 관리**

#### **과제 목록 조회**
```http
GET /api/v1/professor/assignments?lectureIdx=1
Authorization: Bearer {accessToken}
```

#### **과제 등록**
```http
POST /api/v1/professor/assignments
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "lectureIdx": 1,
  "title": "자바 프로그래밍 과제 1",
  "description": "클래스와 객체 구현",
  "dueDate": "2025-03-25"
}
```

#### **과제 제출물 조회 및 채점**
```http
GET /api/v1/professor/assignments/{assignmentIdx}/submissions
Authorization: Bearer {accessToken}
```

```http
PUT /api/v1/professor/assignments/submissions/{submissionIdx}
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "score": 85.0,
  "feedback": "좋은 구현입니다. 다만 예외 처리를 더 강화해보세요."
}
```

### **공지사항 관리**

#### **공지사항 등록**
```http
POST /api/v1/professor/notices
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "lectureIdx": 1,
  "title": "중간고사 일정 변경 안내",
  "content": "중간고사가 3월 20일로 변경되었습니다."
}
```

---

## 5. 학생 API

### **수강신청**

#### **수강 가능 강의 목록 조회**
```http
GET /api/v1/student/lectures/available?year=2025&semester=1&department=CS
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "data": [
    {
      "lectureIdx": 1,
      "lectureName": "자바 프로그래밍",
      "lectureCode": "CS101",
      "professorName": "김교수",
      "credit": 3,
      "currentStudents": 25,
      "maxStudents": 30,
      "lectureTime": "월1,2 수3,4",
      "isEnrolled": false,
      "isWaitlisted": false
    }
  ]
}
```

#### **수강신청**
```http
POST /api/v1/student/enrollments
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "lectureIdxs": [1, 2, 3]
}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "successCount": 2,
    "waitlistCount": 1,
    "results": [
      {
        "lectureIdx": 1,
        "status": "SUCCESS",
        "message": "수강신청이 완료되었습니다."
      },
      {
        "lectureIdx": 2,
        "status": "SUCCESS",
        "message": "수강신청이 완료되었습니다."
      },
      {
        "lectureIdx": 3,
        "status": "WAITLIST",
        "message": "대기열에 등록되었습니다. (순번: 3)"
      }
    ]
  }
}
```

#### **수강신청 취소**
```http
DELETE /api/v1/student/enrollments/{enrollmentIdx}
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "reason": "시간표 중복"
}
```

### **내 강의 관리**

#### **수강중인 강의 목록 조회**
```http
GET /api/v1/student/enrollments
Authorization: Bearer {accessToken}
```

#### **강의 상세 정보 조회**
```http
GET /api/v1/student/lectures/{lectureIdx}
Authorization: Bearer {accessToken}
```

### **출결 관리**

#### **출결 현황 조회**
```http
GET /api/v1/student/attendance?lectureIdx=1
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "lectureName": "자바 프로그래밍",
    "totalClasses": 15,
    "presentCount": 13,
    "lateCount": 1,
    "absentCount": 1,
    "attendanceRate": 86.7,
    "details": [
      {
        "date": "2025-03-10",
        "status": "PRESENT"
      },
      {
        "date": "2025-03-12",
        "status": "LATE",
        "requestReason": "교통 체증",
        "approvalStatus": "APPROVED"
      }
    ]
  }
}
```

#### **출결 사유 신청**
```http
POST /api/v1/student/attendance/request
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "lectureIdx": 1,
  "attendanceDate": "2025-03-15",
  "reason": "병원 진료로 인한 결석"
}
```

### **과제 관리**

#### **과제 목록 조회**
```http
GET /api/v1/student/assignments?lectureIdx=1
Authorization: Bearer {accessToken}
```

#### **과제 제출**
```http
POST /api/v1/student/assignments/{assignmentIdx}/submit
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}

{
  "content": "과제 내용 설명",
  "file": {file}
}
```

### **강의 평가**

#### **평가 항목 조회**
```http
GET /api/v1/student/evaluations/items?lectureIdx=1
Authorization: Bearer {accessToken}
```

#### **강의 평가 제출**
```http
POST /api/v1/student/evaluations
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "lectureIdx": 1,
  "evaluationData": {
    "content_quality": 4,
    "material_quality": 5,
    "pace": 3,
    "attitude": 4,
    "overall": 4
  },
  "comments": "좋은 강의였습니다."
}
```

---

## 6. 공통 API

### **공지사항**

#### **공지사항 목록 조회**
```http
GET /api/v1/common/notices?lectureIdx=1&page=0&size=10
```

#### **공지사항 상세 조회**
```http
GET /api/v1/common/notices/{noticeIdx}
```

### **채팅**

#### **채팅방 입장**
```http
GET /api/v1/common/chat/rooms/{roomIdx}
Authorization: Bearer {accessToken}
```

#### **메시지 전송**
```http
POST /api/v1/common/chat/messages
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "roomIdx": 1,
  "message": "질문 있습니다.",
  "messageType": "TEXT"
}
```

#### **메시지 목록 조회**
```http
GET /api/v1/common/chat/messages?roomIdx=1&before=2025-03-15T10:00:00&page=0&size=50
```

### **파일 업로드**

#### **파일 업로드**
```http
POST /api/v1/common/upload
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}

{
  "file": {file},
  "type": "ASSIGNMENT"
}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "fileUrl": "/uploads/assignments/assignment_123.pdf",
    "fileName": "assignment_123.pdf",
    "fileSize": 2048576
  }
}
```

---

## 7. 에러 코드

### **공통 에러 코드**

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `VALIDATION_ERROR` | 400 | 입력값 검증 실패 |
| `UNAUTHORIZED` | 401 | 인증 실패 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `CONFLICT` | 409 | 리소스 충돌 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

### **도메인별 에러 코드**

#### **수강신청 관련**
| 코드 | 설명 |
|------|------|
| `ENROLLMENT_FULL` | 강의 정원 초과 |
| `ENROLLMENT_DUPLICATE` | 중복 수강신청 |
| `ENROLLMENT_CLOSED` | 수강신청 기간 종료 |
| `ENROLLMENT_PREREQUISITE` | 선수과목 미이수 |

#### **출결 관련**
| 코드 | 설명 |
|------|------|
| `ATTENDANCE_ALREADY_REQUESTED` | 이미 사유 신청됨 |
| `ATTENDANCE_REQUEST_EXPIRED` | 사유 신청 기간 만료 |
| `ATTENDANCE_CANNOT_MODIFY` | 출결 수정 불가 |

#### **성적 관련**
| 코드 | 설명 |
|------|------|
| `GRADE_ALREADY_FINALIZED` | 성적 이미 확정됨 |
| `GRADE_INVALID_SCORE` | 잘못된 점수 범위 |
| `GRADE_MISSING_DATA` | 필수 성적 데이터 누락 |

---

## 🎯 API 설계 완료

REST API 명세서가 완료되었습니다. 모든 엔드포인트의 요청/응답 포맷과 에러 코드가 정의되었습니다.