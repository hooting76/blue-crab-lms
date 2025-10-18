# API Endpoint Gap Analysis Report

**Generated**: 2025-10-14
**Source**: Comparison of Controller Documentation vs API Test Page Templates

---

## Executive Summary

### Coverage Statistics

| Metric | Count | Percentage |
|--------|-------|------------|
| **Total Controller Endpoints** | 106 | 100% |
| **Endpoints in Test Page** | 44 | 41.5% |
| **Missing Endpoints** | 62 | 58.5% |
| **Coverage Gap** | - | **58.5%** |

### Key Findings

- **Critical Gap**: 62 endpoints (58.5%) are not covered in the API test page
- **Highest Missing Category**: Academic/Lecture System (24 endpoints)
- **Second Priority**: User Management (10 endpoints)
- **Third Priority**: Board Management (8 endpoints)

---

## Missing Endpoints by Category

### 1. Academic/Lecture System (24 endpoints)

#### Lecture Management

| Endpoint | Method | Controller | Auth | Description |
|----------|--------|------------|------|-------------|
| `/api/lectures` | GET | LectureController | Yes | Get lectures list with filters (serial, professor, year, semester, title, major, open, pagination) |
| `/api/lectures/{lecIdx}` | GET | LectureController | Yes | Get lecture detail by ID |
| `/api/lectures/{lecIdx}/stats` | GET | LectureController | Yes | Get lecture statistics |
| `/api/lectures` | POST | LectureController | Yes | Create new lecture |
| `/api/lectures/{lecIdx}` | PUT | LectureController | Yes | Update lecture information |
| `/api/lectures/{lecIdx}` | DELETE | LectureController | Yes | Delete lecture |

**Priority**: HIGH - Core academic functionality

---

#### Enrollment Management

| Endpoint | Method | Controller | Auth | Description |
|----------|--------|------------|------|-------------|
| `/api/enrollments` | GET | EnrollmentController | Yes | Get enrollments with filters (studentIdx, lecIdx, checkEnrollment, enrolled, stats, pagination) |
| `/api/enrollments/{enrollmentIdx}` | GET | EnrollmentController | Yes | Get enrollment detail by ID |
| `/api/enrollments/{enrollmentIdx}/data` | GET | EnrollmentController | Yes | Get parsed enrollment JSON data |
| `/api/enrollments` | POST | EnrollmentController | Yes | Enroll in lecture |
| `/api/enrollments/{enrollmentIdx}` | DELETE | EnrollmentController | Yes | Cancel enrollment |
| `/api/enrollments/{enrollmentIdx}/attendance` | PUT | EnrollmentController | Yes | Update attendance information |
| `/api/enrollments/{enrollmentIdx}/grade` | PUT | EnrollmentController | Yes | Update grade information |

**Priority**: HIGH - Core academic functionality

---

#### Assignment Management

| Endpoint | Method | Controller | Auth | Description |
|----------|--------|------------|------|-------------|
| `/api/assignments` | GET | AssignmentController | Yes | Get assignments with filters (lecIdx, withLecture, stats, pagination) |
| `/api/assignments/{assignmentIdx}` | GET | AssignmentController | Yes | Get assignment detail by ID |
| `/api/assignments/{assignmentIdx}/data` | GET | AssignmentController | Yes | Get parsed assignment JSON data |
| `/api/assignments` | POST | AssignmentController | Yes | Create new assignment |
| `/api/assignments/{assignmentIdx}/submit` | POST | AssignmentController | Yes | Submit assignment |
| `/api/assignments/{assignmentIdx}` | PUT | AssignmentController | Yes | Update assignment information |
| `/api/assignments/{assignmentIdx}/grade` | PUT | AssignmentController | Yes | Grade assignment |
| `/api/assignments/{assignmentIdx}` | DELETE | AssignmentController | Yes | Delete assignment |

**Priority**: HIGH - Core academic functionality

---

#### Registry/Certificate Management

| Endpoint | Method | Controller | Auth | Description |
|----------|--------|------------|------|-------------|
| `/api/registry/me` | POST | RegistryController | Yes | Get my academic registry information |
| `/api/registry/cert/issue` | POST | RegistryController | Yes | Issue certificate and save history |
| `/api/registry/me/exists` | GET | RegistryController | Yes | Check if registry exists for user |

**Priority**: MEDIUM - Certificate issuance functionality

---

### 2. User Management (10 endpoints)

| Endpoint | Method | Controller | Auth | Description |
|----------|--------|------------|------|-------------|
| `/api/users/students` | GET | UserController | Yes | Get student users only |
| `/api/users/professors` | GET | UserController | Yes | Get professor users only |
| `/api/users/{id}` | PUT | UserController | Yes | Update user information |
| `/api/users/{id}` | DELETE | UserController | Yes | Delete user |
| `/api/users/{id}/toggle-role` | PATCH | UserController | Yes | Toggle user role between student and professor |
| `/api/users/search` | GET | UserController | Yes | Search users by name |
| `/api/users/search-all` | GET | UserController | Yes | Search users by keyword (name or email) |
| `/api/users/search-birth` | GET | UserController | Yes | Search users by birth date range |
| `/api/users/stats` | GET | UserController | Yes | Get user statistics |
| `/api/profile/me/completeness` | POST | ProfileController | Yes | Check profile completeness |
| `/api/profile/me/image/file` | POST | ProfileController | Yes | Get profile image file directly (proxy method) |

**Priority**: MEDIUM - User management and search capabilities

---

### 3. Board Management (8 endpoints)

| Endpoint | Method | Controller | Auth | Description |
|----------|--------|------------|------|-------------|
| `/api/boards/bycode` | POST | BoardController | Yes | Get boards by code (performance optimized) |
| `/api/boards/link-attachments/{boardIdx}` | POST | BoardController | Yes | Link attachments to board |
| `/api/boards/update/{boardIdx}` | POST | BoardUpdateController | Yes | Update board post |
| `/api/boards/delete/{boardIdx}` | POST | BoardUpdateController | Yes | Delete board post (soft delete) |
| `/api/boards/count` | POST | BoardStatisticsController | Yes | Get active board count |
| `/api/boards/exists` | POST | BoardStatisticsController | Yes | Check if board exists |
| `/api/boards/count/bycode` | POST | BoardStatisticsController | Yes | Get board count by code |

**Priority**: MEDIUM - Board management features

---

#### Board Attachments (Missing 6 attachment endpoints)

| Endpoint | Method | Controller | Auth | Description |
|----------|--------|------------|------|-------------|
| `/api/board-attachments/upload/{boardIdx}` | POST | BoardAttachmentUploadController | Yes | Upload board attachments |
| `/api/board-attachments/delete` | POST | BoardAttachmentDeleteController | Yes | Delete multiple attachments |
| `/api/board-attachments/delete-all/{boardIdx}` | POST | BoardAttachmentDeleteController | Yes | Delete all attachments of a board |
| `/api/board-attachments/download` | POST | BoardAttachmentDownloadController | Yes | Download attachment file |
| `/api/board-attachments/info` | POST | BoardAttachmentDownloadController | Yes | Get attachment metadata |
| `/api/board-attachments/download/health` | POST | BoardAttachmentDownloadController | No | Download service health check |

**Priority**: MEDIUM-HIGH - Essential for complete board functionality

---

### 4. Facility & Reservations (6 endpoints)

| Endpoint | Method | Controller | Auth | Description |
|----------|--------|------------|------|-------------|
| `/api/facilities/type/{facilityType}` | POST | FacilityController | Yes | Get facilities by type |
| `/api/facilities/{facilityIdx}` | POST | FacilityController | Yes | Get facility by ID |
| `/api/facilities/search` | POST | FacilityController | Yes | Search facilities by keyword |
| `/api/facilities/{facilityIdx}/daily-schedule` | POST | FacilityController | Yes | Get daily schedule for facility |
| `/api/reading-room/my-reservation` | POST | ReadingRoomController | Yes | Get my current reading room reservation |

**Priority**: MEDIUM - Enhanced facility browsing

---

### 5. Admin Functions (4 endpoints)

| Endpoint | Method | Controller | Auth | Description |
|----------|--------|------------|------|-------------|
| `/api/admin/auth/refresh` | POST | AdminAuthTokenController | No | Refresh admin JWT token |
| `/api/admin/reservations/pending/count` | POST | AdminFacilityReservationController | Yes | Get pending reservations count |
| `/api/admin/reservations/stats` | POST | AdminFacilityReservationController | Yes | Get reservation statistics (with date filters) |
| `/api/admin/reservations/all` | POST | AdminFacilityReservationController | Yes | Get all reservations with filters (status, facilityIdx, date range) |

**Priority**: MEDIUM - Admin reporting and management

---

### 6. Notifications (4 endpoints)

| Endpoint | Method | Controller | Auth | Description |
|----------|--------|------------|------|-------------|
| `/api/fcm/register/force` | POST | FcmTokenController | Yes | Force register FCM token |
| `/api/fcm/unregister` | DELETE | FcmTokenController | Yes | Unregister FCM token (logout) |
| `/api/fcm/send/batch` | POST | FcmTokenController | Yes | Send batch notifications (admin only) |
| `/api/fcm/send/broadcast` | POST | FcmTokenController | Yes | Send broadcast notification to all users (admin only) |
| `/api/fcm/stats` | GET | FcmTokenController | Yes | Get FCM token statistics (admin only) |
| `/api/push/send` | POST | PushNotificationController | Yes | Send push notification to token (admin only) |
| `/api/push/send-to-topic` | POST | PushNotificationController | Yes | Send push notification to topic (admin only) |

**Priority**: LOW-MEDIUM - Advanced notification features

---

### 7. System/Database Utilities (5 endpoints)

| Endpoint | Method | Controller | Auth | Description |
|----------|--------|------------|------|-------------|
| `/api/test` | GET | ApiController | No | API connection test |
| `/api/test-auth` | GET | ApiController | Yes | Test JWT authentication |
| `/api/auth/config` | GET | MailAuthCheckController | No | Get authentication configuration |
| `/api/database/tables` | GET | DatabaseController | No | Get database tables list |
| `/api/database/tables/{tableName}/columns` | GET | DatabaseController | No | Get table column information |
| `/api/database/tables/{tableName}/sample` | GET | DatabaseController | No | Get table sample data (max 10 rows) |
| `/api/database/query` | GET | DatabaseController | No | Execute SQL query (SELECT only) |

**Priority**: LOW - Development/debugging utilities

---

### 8. Password Reset (1 endpoint)

| Endpoint | Method | Controller | Auth | Description |
|----------|--------|------------|------|-------------|
| `/api/auth/password-reset/rate-limit-status` | GET | PasswordResetController | No | Get rate limit status for debugging |

**Priority**: LOW - Debugging utility

---

## Discrepancies in Existing Endpoints

### Endpoints with Different Information

1. **Board List Endpoint**
   - **Documentation**: `requiresAuth: true`
   - **Test Page**: `auth: false`
   - **Recommendation**: Verify actual authentication requirement

2. **Board Detail Endpoint**
   - **Documentation**: `requiresAuth: true`
   - **Test Page**: `auth: false`
   - **Recommendation**: Verify actual authentication requirement

3. **Database Info Endpoint**
   - **Documentation**: `requiresAuth: false`
   - **Test Page**: `auth: true`
   - **Recommendation**: Security review recommended (sensitive database metadata)

---

## Priority Recommendations

### Phase 1: Critical Business Functions (HIGH Priority)

**Add these 24 endpoints immediately** - Core academic system

1. **Lecture Management** (6 endpoints)
   - CRUD operations for lectures
   - Lecture statistics

2. **Enrollment Management** (7 endpoints)
   - Student enrollment/withdrawal
   - Attendance and grade management

3. **Assignment Management** (8 endpoints)
   - Assignment CRUD
   - Submission and grading

4. **Board Attachments** (6 endpoints)
   - Essential for complete board functionality

**Suggested Category**: "학사 관리" (Academic Management)

---

### Phase 2: Enhanced Operations (MEDIUM Priority)

**Add these 23 endpoints next** - Operational improvements

5. **User Management Enhancements** (10 endpoints)
   - Search and filtering
   - Role management
   - User statistics

6. **Board Management** (8 endpoints)
   - Board statistics
   - Update/delete operations

7. **Facility Enhancements** (6 endpoints)
   - Type filtering
   - Search functionality
   - Daily schedules

8. **Admin Reporting** (4 endpoints)
   - Statistics and reporting
   - All reservations view

9. **Registry/Certificates** (3 endpoints)
   - Academic records
   - Certificate issuance

**Suggested Categories**:
- "사용자 관리 확장" (Extended User Management)
- "게시판 관리" (Board Management)
- "관리자 리포팅" (Admin Reporting)

---

### Phase 3: Advanced Features (LOW Priority)

**Add these 15 endpoints later** - Nice-to-have features

10. **Advanced Notifications** (7 endpoints)
    - Batch and broadcast notifications
    - FCM statistics

11. **System Utilities** (7 endpoints)
    - Database inspection
    - Development tools

12. **Debugging Endpoints** (1 endpoint)
    - Rate limit status

**Suggested Category**: "시스템 유틸리티" (System Utilities)

---

## Suggested api-templates.json Structure for Missing Endpoints

### Example: Lecture Management Section

```json
{
  "lectureList": {
    "name": "강의 목록",
    "category": "학사 관리",
    "description": "강의 목록 조회 (필터: 교수, 연도, 학기, 전공 등)",
    "auth": true,
    "method": "GET",
    "endpoint": "/api/lectures",
    "params": [
      {
        "name": "serial",
        "type": "text",
        "location": "query",
        "required": false,
        "example": "CS101",
        "description": "강의 코드"
      },
      {
        "name": "professor",
        "type": "text",
        "location": "query",
        "required": false,
        "example": "홍길동",
        "description": "교수 이름"
      },
      {
        "name": "year",
        "type": "number",
        "location": "query",
        "required": false,
        "example": "2025",
        "description": "학년도"
      },
      {
        "name": "semester",
        "type": "number",
        "location": "query",
        "required": false,
        "example": "1",
        "description": "학기"
      },
      {
        "name": "page",
        "type": "number",
        "location": "query",
        "required": false,
        "example": "0",
        "description": "페이지 번호"
      },
      {
        "name": "size",
        "type": "number",
        "location": "query",
        "required": false,
        "example": "20",
        "description": "페이지 크기"
      }
    ],
    "headers": [],
    "bodyTemplate": null
  },
  "lectureDetail": {
    "name": "강의 상세",
    "category": "학사 관리",
    "description": "강의 상세 정보 조회",
    "auth": true,
    "method": "GET",
    "endpoint": "/api/lectures/{lecIdx}",
    "params": [
      {
        "name": "lecIdx",
        "type": "number",
        "location": "path",
        "required": true,
        "example": "1",
        "description": "강의 ID"
      }
    ],
    "headers": [],
    "bodyTemplate": null
  },
  "lectureCreate": {
    "name": "강의 생성",
    "category": "학사 관리",
    "description": "새 강의 등록 (교수 권한 필요)",
    "auth": true,
    "method": "POST",
    "endpoint": "/api/lectures",
    "params": [],
    "headers": [],
    "bodyTemplate": {
      "lecSerial": "CS101",
      "lecTitle": "컴퓨터과학 입문",
      "lecProfessor": "홍길동",
      "lecYear": 2025,
      "lecSemester": 1,
      "lecMajor": "컴퓨터공학",
      "lecCredit": 3,
      "lecMaxStudent": 40,
      "lecOpen": true
    }
  },
  "enrollmentCreate": {
    "name": "수강 신청",
    "category": "학사 관리",
    "description": "강의 수강 신청",
    "auth": true,
    "method": "POST",
    "endpoint": "/api/enrollments",
    "params": [],
    "headers": [],
    "bodyTemplate": {
      "lecIdx": 1,
      "studentIdx": 5
    }
  },
  "enrollmentList": {
    "name": "수강 목록",
    "category": "학사 관리",
    "description": "수강 신청 목록 조회 (학생별/강의별 필터)",
    "auth": true,
    "method": "GET",
    "endpoint": "/api/enrollments",
    "params": [
      {
        "name": "studentIdx",
        "type": "number",
        "location": "query",
        "required": false,
        "example": "5",
        "description": "학생 ID"
      },
      {
        "name": "lecIdx",
        "type": "number",
        "location": "query",
        "required": false,
        "example": "1",
        "description": "강의 ID"
      },
      {
        "name": "page",
        "type": "number",
        "location": "query",
        "required": false,
        "example": "0",
        "description": "페이지 번호"
      }
    ],
    "headers": [],
    "bodyTemplate": null
  },
  "assignmentList": {
    "name": "과제 목록",
    "category": "학사 관리",
    "description": "과제 목록 조회",
    "auth": true,
    "method": "GET",
    "endpoint": "/api/assignments",
    "params": [
      {
        "name": "lecIdx",
        "type": "number",
        "location": "query",
        "required": false,
        "example": "1",
        "description": "강의 ID"
      },
      {
        "name": "withLecture",
        "type": "checkbox",
        "location": "query",
        "required": false,
        "example": "true",
        "description": "강의 정보 포함 여부"
      }
    ],
    "headers": [],
    "bodyTemplate": null
  },
  "assignmentSubmit": {
    "name": "과제 제출",
    "category": "학사 관리",
    "description": "과제 제출",
    "auth": true,
    "method": "POST",
    "endpoint": "/api/assignments/{assignmentIdx}/submit",
    "params": [
      {
        "name": "assignmentIdx",
        "type": "number",
        "location": "path",
        "required": true,
        "example": "1",
        "description": "과제 ID"
      }
    ],
    "headers": [],
    "bodyTemplate": {
      "submissionContent": "과제 제출 내용입니다.",
      "submissionData": {
        "fileUrls": ["https://example.com/file1.pdf"],
        "comments": "제출 코멘트"
      }
    }
  }
}
```

### Example: User Search Section

```json
{
  "userSearchByName": {
    "name": "사용자 이름 검색",
    "category": "사용자 · 프로필",
    "description": "이름으로 사용자 검색",
    "auth": true,
    "method": "GET",
    "endpoint": "/api/users/search",
    "params": [
      {
        "name": "name",
        "type": "text",
        "location": "query",
        "required": true,
        "example": "홍길동",
        "description": "검색할 이름"
      }
    ],
    "headers": [],
    "bodyTemplate": null
  },
  "userSearchAll": {
    "name": "사용자 통합 검색",
    "category": "사용자 · 프로필",
    "description": "이름 또는 이메일로 검색",
    "auth": true,
    "method": "GET",
    "endpoint": "/api/users/search-all",
    "params": [
      {
        "name": "keyword",
        "type": "text",
        "location": "query",
        "required": true,
        "example": "홍길동",
        "description": "검색 키워드"
      }
    ],
    "headers": [],
    "bodyTemplate": null
  },
  "userStats": {
    "name": "사용자 통계",
    "category": "사용자 · 프로필",
    "description": "전체 사용자 통계 조회",
    "auth": true,
    "method": "GET",
    "endpoint": "/api/users/stats",
    "params": [],
    "headers": [],
    "bodyTemplate": null
  }
}
```

### Example: Board Attachments Section

```json
{
  "boardAttachmentUpload": {
    "name": "게시글 첨부파일 업로드",
    "category": "게시판",
    "description": "게시글에 파일 첨부",
    "auth": true,
    "method": "POST",
    "endpoint": "/api/board-attachments/upload/{boardIdx}",
    "params": [
      {
        "name": "boardIdx",
        "type": "number",
        "location": "path",
        "required": true,
        "example": "1",
        "description": "게시글 ID"
      }
    ],
    "headers": [
      {
        "name": "Content-Type",
        "required": true,
        "example": "multipart/form-data",
        "description": "파일 업로드용 컨텐츠 타입"
      }
    ],
    "bodyTemplate": {
      "note": "multipart/form-data로 파일 전송"
    }
  },
  "boardAttachmentDownload": {
    "name": "게시글 첨부파일 다운로드",
    "category": "게시판",
    "description": "첨부파일 다운로드",
    "auth": true,
    "method": "POST",
    "endpoint": "/api/board-attachments/download",
    "params": [],
    "headers": [],
    "bodyTemplate": {
      "attachmentIdx": 1
    }
  },
  "boardAttachmentInfo": {
    "name": "첨부파일 정보",
    "category": "게시판",
    "description": "첨부파일 메타데이터 조회",
    "auth": true,
    "method": "POST",
    "endpoint": "/api/board-attachments/info",
    "params": [],
    "headers": [],
    "bodyTemplate": {
      "attachmentIdx": 1
    }
  },
  "boardUpdate": {
    "name": "게시글 수정",
    "category": "게시판",
    "description": "게시글 수정 (작성자만 가능)",
    "auth": true,
    "method": "POST",
    "endpoint": "/api/boards/update/{boardIdx}",
    "params": [
      {
        "name": "boardIdx",
        "type": "number",
        "location": "path",
        "required": true,
        "example": "1",
        "description": "게시글 ID"
      }
    ],
    "headers": [],
    "bodyTemplate": {
      "boardTitle": "수정된 제목",
      "boardContent": "수정된 본문 내용"
    }
  },
  "boardDelete": {
    "name": "게시글 삭제",
    "category": "게시판",
    "description": "게시글 삭제 (소프트 삭제)",
    "auth": true,
    "method": "POST",
    "endpoint": "/api/boards/delete/{boardIdx}",
    "params": [
      {
        "name": "boardIdx",
        "type": "number",
        "location": "path",
        "required": true,
        "example": "1",
        "description": "게시글 ID"
      }
    ],
    "headers": [],
    "bodyTemplate": null
  }
}
```

---

## Action Items

### Immediate Actions (Week 1)

1. Review authentication requirements discrepancies for board endpoints
2. Add Phase 1 endpoints (24 academic system endpoints) to api-templates.json
3. Test all newly added endpoints in the API test page

### Short-term Actions (Weeks 2-4)

4. Add Phase 2 endpoints (23 operational enhancement endpoints)
5. Organize endpoints into logical categories in the test page UI
6. Add documentation/tooltips for complex endpoints

### Long-term Actions (Month 2+)

7. Add Phase 3 endpoints (15 advanced/utility endpoints)
8. Create automated sync process between Controllers and api-templates.json
9. Consider adding endpoint usage analytics to prioritize future additions

---

## Recommendations

### 1. Automated Sync Process

Consider creating a script or CI/CD pipeline step that:
- Extracts endpoints from Controllers automatically
- Compares with api-templates.json
- Generates diff reports
- Flags newly added/removed endpoints

### 2. Endpoint Categorization

Reorganize test page categories for better UX:

**Current Categories**:
- 인증 · 계정 (Auth & Account)
- 관리자 (Admin)
- 사용자 · 프로필 (User & Profile)
- 게시판 (Board)
- 시설 · 열람실 (Facility & Reading Room)
- 알림 · 푸시 (Notifications & Push)
- 시스템 (System)

**Suggested Additional Categories**:
- 학사 관리 (Academic Management) - NEW
- 강의 (Lectures) - NEW
- 수강신청 (Enrollment) - NEW
- 과제 (Assignments) - NEW
- 성적관리 (Grade Management) - NEW
- 학적부 (Registry) - NEW

### 3. Documentation Enhancements

For each endpoint in api-templates.json, consider adding:
- **Response examples**: Sample successful responses
- **Error codes**: Common error scenarios
- **Permission requirements**: Student vs Professor vs Admin
- **Rate limits**: If applicable
- **Deprecation notices**: For endpoints being phased out

### 4. Testing Priority

Focus testing efforts on:
1. High-traffic endpoints (auth, profile, board list)
2. Complex business logic (reservations, enrollment)
3. Admin-only endpoints (security verification)
4. File upload/download endpoints (attachment handling)

---

## Conclusion

The current API test page covers **41.5%** of available endpoints. The most significant gap is in the **Academic/Lecture System** (24 missing endpoints), which represents core LMS functionality.

**Recommended approach**:
1. **Phase 1**: Add critical academic system endpoints immediately
2. **Phase 2**: Add operational enhancements within 2-4 weeks
3. **Phase 3**: Add utility/debugging endpoints as needed

This phased approach will provide the most value to testers and developers while maintaining manageable implementation scope.

---

**Report End**
