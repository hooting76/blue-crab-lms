# 출결 관리 시스템 가이드

> **작성일**: 2025-10-17  
> **버전**: 1.0  
> **기능**: 학생 출석 요청 및 교수 승인 시스템

---

## 📋 목차

1. [시스템 개요](#1-시스템-개요)
2. [데이터베이스 설계](#2-데이터베이스-설계)
3. [API 명세](#3-api-명세)
4. [비즈니스 로직](#4-비즈니스-로직)
5. [사용자 플로우](#5-사용자-플로우)
6. [구현 예시](#6-구현-예시)

---

## 1. 시스템 개요

### **핵심 기능**
- **학생**: 수강 강의의 특정 회차 출석 요청
- **교수**: 출석 요청 승인/거부 (출석/지각/결석)
- **자동 처리**: 1주일 후 미처리 요청은 자동 출석 승인

### **출석 상태 코드**
| 코드 | 의미 | DB 저장값 |
|------|------|-----------|
| PRESENT | 출석 | `출` |
| LATE | 지각 | `지` |
| ABSENT | 결석 | `결` |

### **주요 특징**
- ✅ 강의당 최대 80회차 지원
- ✅ 출석 요청 시 임시 승인 (기본값: 출석)
- ✅ 1주일 유효기간 (7일 후 자동 확정)
- ✅ 교수 승인 전까지 수정 가능

---

## 2. 데이터베이스 설계

### **2.1 ENROLLMENT_EXTENDED_TBL 활용**

기존 JSON 구조를 확장하여 출석 데이터 저장:

```json
{
  "enrollment": {
    "status": "ENROLLED",
    "enrollmentDate": "2025-03-01T09:00:00"
  },
  "attendance": [
    {
      "sessionNumber": 1,
      "status": "출",
      "requestDate": "2025-03-10T10:30:00",
      "approvalDate": null,
      "approvalBy": null,
      "autoApproved": false,
      "expiryDate": "2025-03-17T10:30:00"
    },
    {
      "sessionNumber": 2,
      "status": "출",
      "requestDate": "2025-03-12T10:30:00",
      "approvalDate": "2025-03-13T15:20:00",
      "approvalBy": "PROF001",
      "autoApproved": false,
      "expiryDate": null
    },
    {
      "sessionNumber": 3,
      "status": "지",
      "requestDate": "2025-03-15T10:35:00",
      "approvalDate": "2025-03-15T16:00:00",
      "approvalBy": "PROF001",
      "autoApproved": false,
      "expiryDate": null
    }
  ],
  "grade": {
    "total": 0,
    "letterGrade": null,
    "status": "PENDING"
  }
}
```

### **2.2 Attendance Record 필드 설명**

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `sessionNumber` | Integer | 회차 번호 (1~80) |
| `status` | String | 출석 상태 ("출", "지", "결") |
| `requestDate` | DateTime | 출석 요청 일시 |
| `approvalDate` | DateTime | 교수 승인 일시 (null = 미승인) |
| `approvalBy` | String | 승인 교수 코드 |
| `autoApproved` | Boolean | 자동 승인 여부 |
| `expiryDate` | DateTime | 유효기간 만료일 (요청일 + 7일) |

---

## 3. API 명세

### **3.1 출석 요청 (학생)**

**엔드포인트**
```http
POST /api/student/attendance/request
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**요청 Body**
```json
{
  "lecSerial": "CS101",
  "sessionNumber": 5
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| lecSerial | String | YES | 강의 코드 |
| sessionNumber | Integer | YES | 회차 번호 (1~80) |

**응답 (성공)**
```json
{
  "success": true,
  "message": "출석 요청이 완료되었습니다",
  "data": {
    "enrollmentIdx": 123,
    "sessionNumber": 5,
    "status": "출",
    "requestDate": "2025-03-20T10:30:00",
    "expiryDate": "2025-03-27T10:30:00",
    "autoApproveDate": "2025-03-27T10:30:00"
  }
}
```

**응답 (실패)**
```json
{
  "success": false,
  "message": "이미 출석 요청한 회차입니다",
  "errorCode": "ATTENDANCE_DUPLICATE"
}
```

**에러 코드**

| 코드 | 설명 |
|------|------|
| `ATTENDANCE_DUPLICATE` | 이미 요청한 회차 |
| `ATTENDANCE_INVALID_SESSION` | 잘못된 회차 번호 (1~80 범위 외) |
| `ENROLLMENT_NOT_FOUND` | 수강 정보 없음 |
| `LECTURE_NOT_FOUND` | 강의 정보 없음 |

---

### **3.2 출석 승인 (교수)**

**엔드포인트**
```http
POST /api/professor/attendance/approve
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**요청 Body**
```json
{
  "lecSerial": "CS101",
  "approvalList": [
    {
      "enrollmentIdx": 123,
      "sessionNumber": 5,
      "status": "출"
    },
    {
      "enrollmentIdx": 124,
      "sessionNumber": 5,
      "status": "지"
    },
    {
      "enrollmentIdx": 125,
      "sessionNumber": 5,
      "status": "결"
    }
  ]
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| lecSerial | String | YES | 강의 코드 |
| approvalList | Array | YES | 승인 목록 |
| ↳ enrollmentIdx | Integer | YES | 수강신청 IDX |
| ↳ sessionNumber | Integer | YES | 회차 번호 |
| ↳ status | String | YES | 출석 상태 ("출", "지", "결") |

**응답 (성공)**
```json
{
  "success": true,
  "message": "출석 승인이 완료되었습니다",
  "data": {
    "totalCount": 3,
    "successCount": 3,
    "failCount": 0,
    "results": [
      {
        "enrollmentIdx": 123,
        "sessionNumber": 5,
        "status": "SUCCESS",
        "finalStatus": "출"
      },
      {
        "enrollmentIdx": 124,
        "sessionNumber": 5,
        "status": "SUCCESS",
        "finalStatus": "지"
      },
      {
        "enrollmentIdx": 125,
        "sessionNumber": 5,
        "status": "SUCCESS",
        "finalStatus": "결"
      }
    ]
  }
}
```

---

### **3.3 출석 요청 목록 조회 (교수)**

**엔드포인트**
```http
POST /api/professor/attendance/requests
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**요청 Body**
```json
{
  "lecSerial": "CS101",
  "sessionNumber": 5,
  "status": "PENDING"
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| lecSerial | String | YES | 강의 코드 |
| sessionNumber | Integer | NO | 회차 번호 (필터용) |
| status | String | NO | 상태 필터 (PENDING/APPROVED/AUTO) |

**응답**
```json
{
  "success": true,
  "data": {
    "requests": [
      {
        "enrollmentIdx": 123,
        "studentIdx": 101,
        "studentCode": "2024001",
        "studentName": "홍길동",
        "sessionNumber": 5,
        "status": "출",
        "requestDate": "2025-03-20T10:30:00",
        "approvalDate": null,
        "expiryDate": "2025-03-27T10:30:00",
        "daysRemaining": 5,
        "isPending": true
      }
    ],
    "summary": {
      "totalRequests": 25,
      "pendingRequests": 5,
      "approvedRequests": 20
    }
  }
}
```

---

### **3.4 내 출석 현황 조회 (학생)**

**엔드포인트**
```http
POST /api/student/attendance/status
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**요청 Body**
```json
{
  "lecSerial": "CS101"
}
```

**응답**
```json
{
  "success": true,
  "data": {
    "lecSerial": "CS101",
    "lecTit": "자바 프로그래밍",
    "totalSessions": 80,
    "attendanceRecords": [
      {
        "sessionNumber": 1,
        "status": "출",
        "requestDate": "2025-03-10T10:30:00",
        "approvalDate": "2025-03-11T15:00:00",
        "approvedBy": "김교수",
        "autoApproved": false
      },
      {
        "sessionNumber": 2,
        "status": "출",
        "requestDate": "2025-03-12T10:30:00",
        "approvalDate": null,
        "expiryDate": "2025-03-19T10:30:00",
        "isPending": true,
        "autoApproved": false
      },
      {
        "sessionNumber": 3,
        "status": "지",
        "requestDate": "2025-03-15T10:35:00",
        "approvalDate": "2025-03-15T16:00:00",
        "approvedBy": "김교수",
        "autoApproved": false
      }
    ],
    "summary": {
      "totalAttendance": 15,
      "present": 12,
      "late": 2,
      "absent": 1,
      "pending": 3,
      "attendanceRate": 93.3
    }
  }
}
```

---

## 4. 비즈니스 로직

### **4.1 출석 요청 처리 (학생)**

```java
@Transactional
public AttendanceRequestResult requestAttendance(String lecSerial, Integer studentIdx, Integer sessionNumber) {
    // 1. 회차 번호 검증 (1~80)
    if (sessionNumber < 1 || sessionNumber > 80) {
        throw new ValidationException("회차 번호는 1~80 사이여야 합니다");
    }
    
    // 2. 수강 정보 조회
    EnrollmentExtendedTbl enrollment = enrollmentRepository
        .findByLecSerialAndStudentIdx(lecSerial, studentIdx)
        .orElseThrow(() -> new NotFoundException("수강 정보를 찾을 수 없습니다"));
    
    // 3. JSON 데이터 파싱
    EnrollmentData data = fromJson(enrollment.getEnrollmentData(), EnrollmentData.class);
    
    // 4. 중복 요청 확인
    boolean alreadyExists = data.getAttendance().stream()
        .anyMatch(a -> a.getSessionNumber().equals(sessionNumber));
    
    if (alreadyExists) {
        throw new DuplicateException("이미 출석 요청한 회차입니다");
    }
    
    // 5. 출석 기록 생성 (기본값: 출석)
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime expiryDate = now.plusDays(7); // 7일 후
    
    AttendanceRecord record = AttendanceRecord.builder()
        .sessionNumber(sessionNumber)
        .status("출")  // 기본값: 출석
        .requestDate(now)
        .approvalDate(null)  // 미승인
        .approvalBy(null)
        .autoApproved(false)
        .expiryDate(expiryDate)
        .build();
    
    // 6. 출석 기록 추가
    data.getAttendance().add(record);
    
    // 7. JSON 저장
    enrollment.setEnrollmentData(toJson(data));
    enrollmentRepository.save(enrollment);
    
    return AttendanceRequestResult.success(record);
}
```

---

### **4.2 출석 승인 처리 (교수)**

```java
@Transactional
public AttendanceApprovalResult approveAttendance(String lecSerial, String professorCode, 
                                                  List<AttendanceApproval> approvalList) {
    List<ApprovalResult> results = new ArrayList<>();
    
    for (AttendanceApproval approval : approvalList) {
        try {
            // 1. 수강 정보 조회
            EnrollmentExtendedTbl enrollment = enrollmentRepository
                .findById(approval.getEnrollmentIdx())
                .orElseThrow(() -> new NotFoundException("수강 정보를 찾을 수 없습니다"));
            
            // 2. 권한 검증 (담당 교수인지)
            validateProfessorPermission(enrollment.getLecIdx(), professorCode);
            
            // 3. JSON 데이터 파싱
            EnrollmentData data = fromJson(enrollment.getEnrollmentData(), EnrollmentData.class);
            
            // 4. 해당 회차 출석 기록 찾기
            AttendanceRecord record = data.getAttendance().stream()
                .filter(a -> a.getSessionNumber().equals(approval.getSessionNumber()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("출석 기록을 찾을 수 없습니다"));
            
            // 5. 출석 상태 업데이트
            record.setStatus(approval.getStatus());  // "출", "지", "결"
            record.setApprovalDate(LocalDateTime.now());
            record.setApprovalBy(professorCode);
            record.setExpiryDate(null);  // 승인되면 만료일 제거
            
            // 6. JSON 저장
            enrollment.setEnrollmentData(toJson(data));
            enrollmentRepository.save(enrollment);
            
            results.add(ApprovalResult.success(approval.getEnrollmentIdx(), 
                                               approval.getSessionNumber(), 
                                               approval.getStatus()));
            
        } catch (Exception e) {
            results.add(ApprovalResult.fail(approval.getEnrollmentIdx(), 
                                           approval.getSessionNumber(), 
                                           e.getMessage()));
        }
    }
    
    return AttendanceApprovalResult.of(results);
}
```

---

### **4.3 자동 승인 처리 (스케줄러)**

```java
@Scheduled(cron = "0 0 3 * * *")  // 매일 오전 3시 실행
@Transactional
public void autoApproveExpiredRequests() {
    LocalDateTime now = LocalDateTime.now();
    
    // 1. 모든 수강 정보 조회
    List<EnrollmentExtendedTbl> enrollments = enrollmentRepository.findAll();
    
    for (EnrollmentExtendedTbl enrollment : enrollments) {
        // 2. JSON 데이터 파싱
        EnrollmentData data = fromJson(enrollment.getEnrollmentData(), EnrollmentData.class);
        
        boolean updated = false;
        
        // 3. 만료된 출석 요청 찾기
        for (AttendanceRecord record : data.getAttendance()) {
            if (record.getApprovalDate() == null && 
                record.getExpiryDate() != null && 
                record.getExpiryDate().isBefore(now)) {
                
                // 4. 자동 승인 (출석 유지)
                record.setApprovalDate(now);
                record.setAutoApproved(true);
                record.setExpiryDate(null);
                // status는 "출"로 유지
                
                updated = true;
            }
        }
        
        // 5. 변경사항 저장
        if (updated) {
            enrollment.setEnrollmentData(toJson(data));
            enrollmentRepository.save(enrollment);
        }
    }
}
```

---

## 5. 사용자 플로우

### **5.1 학생 플로우**

**시나리오**: 학생이 5회차 강의 출석을 요청합니다.

```
1. 수강 중인 강의 목록 조회
   ↓
2. "자바 프로그래밍" 강의 선택
   ↓
3. 출석 요청 가능한 회차 확인 (1~80회 중)
   ↓
4. 5회차 출석 요청 클릭
   ↓
5. API 호출: POST /api/student/attendance/request
   Body: { lecSerial: "CS101", sessionNumber: 5 }
   ↓
6. 출석 상태: "출" (임시 승인)
   유효기간: 7일 후까지
   ↓
7. 교수 승인 대기 또는 7일 후 자동 확정
```

---

### **5.2 교수 플로우**

**시나리오**: 교수가 5회차 출석 요청을 승인합니다.

```
1. 담당 강의 목록 조회
   ↓
2. "자바 프로그래밍" 강의 선택
   ↓
3. 출석 요청 목록 조회 (5회차)
   POST /api/professor/attendance/requests
   ↓
4. 학생별 출석 상태 결정
   - 홍길동: 출석
   - 김철수: 지각 (5분 늦음)
   - 이영희: 결석 (무단 결석)
   ↓
5. 일괄 승인 API 호출
   POST /api/professor/attendance/approve
   Body: {
     lecSerial: "CS101",
     approvalList: [
       { enrollmentIdx: 123, sessionNumber: 5, status: "출" },
       { enrollmentIdx: 124, sessionNumber: 5, status: "지" },
       { enrollmentIdx: 125, sessionNumber: 5, status: "결" }
     ]
   }
   ↓
6. 승인 완료 확인
```

---

## 6. 구현 예시

### **6.1 프론트엔드 (학생)**

```javascript
// 출석 요청
async function requestAttendance(lecSerial, sessionNumber) {
  try {
    const response = await api.post('/student/attendance/request', {
      lecSerial: lecSerial,
      sessionNumber: sessionNumber
    });
    
    const result = response.data;
    alert(`${sessionNumber}회차 출석 요청이 완료되었습니다\n유효기간: ${result.data.expiryDate}`);
    
    // 출석 현황 새로고침
    fetchAttendanceStatus(lecSerial);
    
  } catch (error) {
    if (error.response?.data?.errorCode === 'ATTENDANCE_DUPLICATE') {
      alert('이미 출석 요청한 회차입니다');
    } else {
      alert('출석 요청에 실패했습니다');
    }
  }
}

// 출석 현황 조회
async function fetchAttendanceStatus(lecSerial) {
  const response = await api.post('/student/attendance/status', {
    lecSerial: lecSerial
  });
  
  const data = response.data.data;
  
  // 출석률 표시
  console.log(`출석률: ${data.summary.attendanceRate}%`);
  console.log(`출석: ${data.summary.present}회`);
  console.log(`지각: ${data.summary.late}회`);
  console.log(`결석: ${data.summary.absent}회`);
  console.log(`대기중: ${data.summary.pending}회`);
  
  // 회차별 출석 상태 표시
  data.attendanceRecords.forEach(record => {
    console.log(`${record.sessionNumber}회차: ${record.status} ${record.isPending ? '(승인대기)' : ''}`);
  });
}
```

---

### **6.2 프론트엔드 (교수)**

```javascript
// 출석 요청 목록 조회
async function fetchAttendanceRequests(lecSerial, sessionNumber) {
  const response = await api.post('/professor/attendance/requests', {
    lecSerial: lecSerial,
    sessionNumber: sessionNumber,
    status: 'PENDING'
  });
  
  const requests = response.data.data.requests;
  
  // UI에 표시
  requests.forEach(req => {
    console.log(`${req.studentName} (${req.studentCode})`);
    console.log(`  - ${req.sessionNumber}회차`);
    console.log(`  - 현재 상태: ${req.status}`);
    console.log(`  - 남은 기간: ${req.daysRemaining}일`);
  });
}

// 출석 승인
async function approveAttendance(lecSerial, approvalList) {
  try {
    const response = await api.post('/professor/attendance/approve', {
      lecSerial: lecSerial,
      approvalList: approvalList
    });
    
    const result = response.data.data;
    alert(`승인 완료: ${result.successCount}건 / 실패: ${result.failCount}건`);
    
    // 결과 상세 표시
    result.results.forEach(r => {
      if (r.status === 'SUCCESS') {
        console.log(`승인 완료: ${r.enrollmentIdx} - ${r.finalStatus}`);
      } else {
        console.log(`승인 실패: ${r.enrollmentIdx}`);
      }
    });
    
  } catch (error) {
    alert('출석 승인에 실패했습니다');
  }
}

// 사용 예시
const approvalList = [
  { enrollmentIdx: 123, sessionNumber: 5, status: "출" },
  { enrollmentIdx: 124, sessionNumber: 5, status: "지" },
  { enrollmentIdx: 125, sessionNumber: 5, status: "결" }
];

approveAttendance('CS101', approvalList);
```

---

## 🎯 구현 체크리스트

### **백엔드**
- [ ] AttendanceRecord DTO 생성
- [ ] 출석 요청 API 구현
- [ ] 출석 승인 API 구현
- [ ] 출석 조회 API 구현 (학생/교수)
- [ ] 자동 승인 스케줄러 구현
- [ ] 회차 번호 검증 (1~80)
- [ ] 중복 요청 방지
- [ ] 교수 권한 검증

### **프론트엔드**
- [ ] 출석 요청 UI (학생)
- [ ] 출석 승인 UI (교수)
- [ ] 출석 현황 대시보드 (학생)
- [ ] 출석 관리 대시보드 (교수)
- [ ] 회차 선택 필터
- [ ] 실시간 유효기간 표시
- [ ] 자동 승인 카운트다운

### **테스트**
- [ ] 출석 요청 테스트
- [ ] 출석 승인 테스트
- [ ] 중복 요청 테스트
- [ ] 자동 승인 테스트
- [ ] 권한 검증 테스트
- [ ] 회차 범위 테스트 (1~80)

---

## 📚 참고 문서

- [`01-데이터베이스설계.md`](./01-데이터베이스설계.md): ENROLLMENT_EXTENDED_TBL 구조
- [`02-API명세서.md`](./02-API명세서.md): 기존 API 명세
- [`03-비즈니스로직.md`](./03-비즈니스로직.md): 출결 관리 로직

---

**최종 수정**: 2025-10-17  
**문서 상태**: 초안 완성 ✅