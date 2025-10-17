# 02. API 명세서

> **작성일**: 2025-10-10  
> **최종 수정**: 2025-10-17  
> **버전**: 6.0 (100% POST 방식 + lecSerial 기반 API)  
> **변경사항**:
> - ✅ **Phase 11: lecIdx → lecSerial 마이그레이션** (2025-10-17)
>   - 프론트엔드에서 lecIdx 완전 제거
>   - 모든 API가 lecSerial 기반으로 동작
>   - 백엔드 변환 레이어 구현
> - ✅ **Phase 10: 100% POST 방식 통일** (2025-10-16)
>   - 모든 GET, PUT, DELETE 엔드포인트를 POST로 변경
>   - Request Body를 통한 파라미터 전달로 통일
> - ✅ **Phase 9: 백엔드 필터링** (2025-10-16)
>   - 전공/부전공 필터링 로직 추가
>   - 0값 규칙 적용 (0 = 전체)

---

## 🔗 API 명세서 개요

강의 관리 시스템의 REST API 엔드포인트 명세를 정의합니다. JWT 기반 인증을 사용하는 Spring Boot REST API입니다.

---

## 📋 목차

1. [API 설계 원칙](#1-api-설계-원칙)
2. [인증 및 인가](#2-인증-및-인가)
3. [강의 관리 API](#3-강의-관리-api)
4. [수강신청 API](#4-수강신청-api)
5. [과제 관리 API](#5-과제-관리-api)
6. [출석 관리 API](#6-출석-관리-api)
7. [에러 코드](#7-에러-코드)
8. [미구현 기능](#8-미구현-기능)

---

## 1. API 설계 원칙

### **POST 방식 100% 통일 (Phase 10)**
- **HTTP Method**: 모든 엔드포인트는 POST 방식 사용
- **파라미터 전달**: Request Body를 통한 JSON 파라미터 전달
- **일관성**: 통일된 API 패턴으로 프론트엔드 개발 단순화
- **보안**: URL 파라미터 노출 제거

### **lecSerial 기반 API (Phase 11)**
- **프론트엔드**: lecIdx 완전 숨김, lecSerial만 사용
- **백엔드**: 변환 레이어에서 lecSerial ↔ lecIdx 자동 변환
- **응답**: @JsonIgnore로 lecIdx 숨김, lecSerial만 반환

  "lecOpen": 1,
  "lecReg": "2025-10-15 14:30:00"
}
```

#### **강의 수정**
```http
PUT /api/lectures/{lectureIdx}
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "lecTit": "심화 자바 프로그래밍",
  "lecMany": 25,
  "lecSummary": "자바 심화 과정을 다루는 강의입니다.",
  "lecOpen": 1
}
```

#### **강의 삭제 (폐강)**
```http
DELETE /api/lectures/{lectureIdx}
Authorization: Bearer {accessToken}
```

---

## 4. 교수 API

### **출석 관리**

#### **출석 인정 요청 목록 조회**
```http
GET /api/professor/attendance/requests?lectureIdx=1&page=0&size=10
Authorization: Bearer {accessToken}
```

#### **강의 상세 정보 조회**
```http
GET /api/lectures/{lectureIdx}
Authorization: Bearer {accessToken}
```

### **출결 관리**

#### **출결 요청 목록 조회**
```http
GET /api/professor/attendance/requests?lectureIdx=1&page=0&size=10
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
PUT /api/professor/attendance/{attendanceIdx}/approve
Authorization: Bearer {accessToken}
```

```http
PUT /api/professor/attendance/{attendanceIdx}/reject
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "reason": "사유 불충분"
}
```

### **성적 관리**

#### **강의별 학생 목록 및 성적 조회**
```http
GET /api/enrollments?lectureIdx=1
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
PUT /api/enrollments/{enrollmentIdx}/grade
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
PUT /api/enrollments/grade/finalize?lectureIdx=1
Authorization: Bearer {accessToken}
```

### **과제 관리**

#### **과제 목록 조회**
```http
GET /api/assignments?lectureIdx=1
Authorization: Bearer {accessToken}
```

#### **과제 등록**
```http
POST /api/assignments
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

**🔍 데이터 저장 구조 설명**

과제 제출 현황은 `ASSIGNMENT_EXTENDED_TBL.ASSIGNMENT_DATA` (LONGTEXT) 컬럼에 JSON 형식으로 저장됩니다.

**JSON 구조:**
```json
{
  "assignment": {
    "title": "중간고사 대체 레포트",
    "description": "5000자 이상 작성",
    "dueDate": "2025-10-30T23:59:59",
    "maxScore": 100,
    "createdAt": "2025-10-01T09:00:00"
  },
  "submissions": [
    {
      "studentIdx": 101,
      "submitted": true,
      "submissionMethod": "서면 제출 (2025-10-15)",
      "submittedAt": "2025-10-15T14:30:00",
      "score": 95,
      "feedback": "훌륭합니다",
      "gradedAt": "2025-10-16T10:00:00"
    },
    {
      "studentIdx": 102,
      "submitted": true,
      "submissionMethod": "이메일 제출 (prof@example.com)",
      "submittedAt": "2025-10-16T09:00:00",
      "score": 88,
      "feedback": "양호함",
      "gradedAt": "2025-10-17T11:00:00"
    }
  ]
}
```

**📊 데이터 크기 고려사항:**
- 각 제출 기록 (submission 객체): 약 200-300 bytes
- 학생 20명: 약 6KB
- 학생 100명: 약 30KB
- 학생 500명: 약 150KB
- LONGTEXT 최대 용량: 4GB → 대형 강의도 문제 없음

**💡 설계 철학:**
- **오프라인 제출 모델**: 실제 과제 파일은 서면/이메일/드라이브 등으로 제출
- **유연한 제출 방식**: submissionMethod 필드에 교수가 직접 입력
- **JSON 기반 확장성**: 스키마 변경 없이 필드 추가 가능

---

**API 엔드포인트:**

```http
GET /api/assignments/{assignmentIdx}/submissions
Authorization: Bearer {accessToken}
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "assignment": { ... },
    "submissions": [ ... ]
  }
}
```

---

**채점 API:**

```http
PUT /api/assignments/submissions/{submissionIdx}
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "studentIdx": 101,
  "submitted": true,
  "submissionMethod": "서면 제출 (2025-10-15)",
  "score": 85.0,
  "feedback": "좋은 구현입니다. 다만 예외 처리를 더 강화해보세요."
}
```

### **공지사항 관리**

#### **공지사항 등록**
```http
POST /api/boards
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
GET /api/lectures?year=2025&semester=1&department=CS
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
      "lectureTime": "월1월2수3수4",
      "isEnrolled": false,
      "isWaitlisted": false
    }
  ]
}
```

#### **수강신청**
```http
POST /api/enrollments
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
DELETE /api/enrollments/{enrollmentIdx}
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "reason": "시간표 중복"
}
```

### **내 강의 관리**

#### **수강중인 강의 목록 조회 (DTO 패턴 적용 ⭐)**

**엔드포인트:**
```http
GET /api/enrollments?studentIdx={studentIdx}&page=0&size=10
Authorization: Bearer {accessToken}
```

**특징:**
- ✅ **DTO 패턴 적용**: Entity 대신 EnrollmentDto 반환
- ✅ **PageImpl 최적화**: Entity 참조 완전 제거로 JSON 직렬화 안정성 확보
- ✅ **교수 이름 조회**: LEC_PROF (USER_CODE) → USER_NAME 자동 조회
- ✅ **Lazy Loading 안전**: Hibernate 프록시 객체 문제 해결
- ✅ **JWT 자동 인식**: 프론트엔드에서 토큰에서 studentIdx 자동 추출
- ✅ **쿼리 파라미터 통합**: studentIdx, lecIdx, page, size 조합 가능

**응답 (DTO 구조):**
```json
{
  "content": [
    {
      "enrollmentIdx": 1,
      "lecIdx": 101,
      "lecSerial": "CS101",
      "lecTit": "자바 프로그래밍",
      "lecProf": "PROF001",
      "lecProfName": "김교수",
      "lecPoint": 3,
      "lecTime": "월수 10:00-11:30",
      "studentIdx": 6,
      "studentCode": "2024001",
      "studentName": "홍길동",
      "enrollmentStatus": "ENROLLED",
      "enrollmentDate": "2024-09-01",
      "cancelDate": null,
      "cancelReason": null,
      "attendanceRecords": null,
      "grade": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 5,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "first": true,
  "last": true,
  "empty": false
}
```

**DTO 필드 설명:**
| 필드명 | 타입 | 설명 | 출처 |
|--------|------|------|------|
| enrollmentIdx | Long | 수강신청 ID (PK) | ENROLLMENT_EXTENDED_TBL |
| lecIdx | Long | 강의 ID | LEC_TBL (Lazy Loading) |
| lecSerial | String | 강의 코드 (예: CS101) | LEC_TBL |
| lecTit | String | 강의명 | LEC_TBL |
| lecProf | String | 교수 코드 (USER_CODE) ⭐ | LEC_TBL.LEC_PROF |
| lecProfName | String | 교수 이름 (USER_NAME) ⭐ | USER_TBL (Repository 조회) |
| lecPoint | Integer | 학점 | LEC_TBL |
| lecTime | String | 강의 시간 | LEC_TBL |
| studentIdx | Long | 학생 ID | USER_TBL (Lazy Loading) |
| studentCode | String | 학번 (USER_CODE) | USER_TBL |
| studentName | String | 학생 이름 (USER_NAME) | USER_TBL |
| enrollmentStatus | String | 수강 상태 (ENROLLED/CANCELLED) | ENROLLMENT_DATA JSON |
| enrollmentDate | String | 수강신청일 | ENROLLMENT_DATA JSON |
| cancelDate | String | 취소일 (nullable) | ENROLLMENT_DATA JSON |
| cancelReason | String | 취소 사유 (nullable) | ENROLLMENT_DATA JSON |
| attendanceRecords | Array | 출석 기록 (nullable) | ENROLLMENT_DATA JSON |
| grade | Object | 성적 정보 (nullable) | ENROLLMENT_DATA JSON |

**구현 세부사항:**
- **Controller**: EnrollmentController.convertToDto() 메서드
- **PageImpl 패턴**: 명시적으로 List<DTO> 생성 후 PageImpl로 래핑
- **교수 조회**: UserTblRepository.findByUserCode(lecProf) → getUserName()
- **안전 처리**: Lazy Loading 접근 시 try-catch로 예외 처리
- **JSON 파싱**: ENROLLMENT_DATA 필드를 Jackson ObjectMapper로 파싱
- **Performance**: 필요한 데이터만 전송하여 네트워크 최적화

**교수 이름 조회 로직:**
```java
// LEC_PROF에서 교수 코드 가져오기
dto.setLecProf(lecture.getLecProf());  // 예: "PROF001", "11"

// USER_TBL에서 교수 이름 조회
userTblRepository.findByUserCode(lecture.getLecProf())
    .ifPresent(professor -> {
        dto.setLecProfName(professor.getUserName());  // 예: "김교수", "굴림체"
    });
```

#### **강의 상세 정보 조회**
```http
GET /api/lectures/{lectureIdx}
Authorization: Bearer {accessToken}
```

### **출결 관리**

#### **출결 현황 조회**
```http
GET /api/student/attendance?lectureIdx=1
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
POST /api/student/attendance/request
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
GET /api/assignments?lectureIdx=1
Authorization: Bearer {accessToken}
```

#### **과제 제출**
```http
POST /api/assignments/{assignmentIdx}/submit
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}

{
  "content": "과제 내용 설명",
  "file": {file}
}
```

---

## 6. 공통 API

### **공지사항**

#### **공지사항 목록 조회**
```http
GET /api/boards?lectureIdx=1&page=0&size=10
```

#### **공지사항 상세 조회**
```http
GET /api/boards/{noticeIdx}
```

### **파일 업로드**

#### **파일 업로드**
```http
POST /api/board-attachments
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

## 📋 미구현 기능 목록

다음 기능들은 현재 백엔드에 구현되지 않았으므로 API 명세서에서 제외되었습니다.

### **관리자 통계 및 모니터링**
- `GET /api/admin/evaluation-items` - 평가 항목 관리
- `POST /api/admin/evaluation-items` - 평가 항목 등록
- `GET /api/admin/statistics/lectures` - 강의 통계 조회
- `GET /api/admin/statistics/students/{studentIdx}` - 학생별 통계 조회

**구현 시 필요 사항**: `EvaluationController`, `StatisticsController` 생성 필요

### **강의 평가 시스템**
- `GET /api/evaluations/items?lectureIdx={id}` - 평가 항목 조회
- `POST /api/evaluations` - 강의 평가 제출

**구현 시 필요 사항**: `LectureEvaluationController` 및 관련 엔티티 생성 필요

### **실시간 채팅 시스템**
- `GET /api/chat/rooms/{roomIdx}` - 채팅방 입장
- `POST /api/chat/messages` - 메시지 전송
- `GET /api/chat/messages` - 메시지 목록 조회

**구현 시 필요 사항**: `ChatController`, WebSocket 설정, 실시간 메시징 인프라 필요

### **교수 전용 성적 관리**
- `GET /api/professor/grades?lectureIdx={id}` - 성적 일괄 조회
- `PUT /api/professor/grades/batch` - 성적 일괄 입력
- `PUT /api/professor/grades/finalize` - 성적 확정

**현재 상태**: 성적 관리는 `EnrollmentController`를 통해 개별 수강신청별로만 가능

---

## 🎯 API 설계 완료

REST API 명세서가 완료되었습니다. 모든 엔드포인트의 요청/응답 포맷과 에러 코드가 정의되었습니다.