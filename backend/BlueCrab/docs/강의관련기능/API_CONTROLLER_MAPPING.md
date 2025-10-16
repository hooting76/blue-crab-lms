#  API 엔드포인트컨트롤러 매핑 (v4.0 - POST 방식 통일)

## 강의 관련 핵심 API

### **강의 관리 컨트롤러 (Lecture 도메인)**
| 컨트롤러 | 엔드포인트 prefix | 주요 기능 | 상태 |
|---------|---------------|---------|------|
| `LectureController` | `/lectures` | 강의 CRUD, 통계, **수강 가능 강의 조회** | 완료  |
| `EnrollmentController` | `/enrollments` | 수강신청/취소 관리 | 완료  |
| `AssignmentController` | `/api/assignments` | 과제 CRUD, 제출 관리 | 완료  |
| `ProfessorAttendanceController` | `/api/professor/attendance` | 교수 출석 관리 | 완료  |
| `StudentAttendanceController` | `/api/student/attendance` | 학생 출석 요청 | 완료  |

### **특별 엔드포인트**
| 엔드포인트 | 메서드 | 설명 | 컨트롤러 |
|-----------|-------|------|---------|
| `/lectures/eligible` | POST (Body: {studentIdx}) | 학생이 수강 가능한 강의 조회 (0값 규칙) | LectureController |

### **공통 시스템 API**
| 컨트롤러 | 엔드포인트 prefix | 주요 기능 | 상태 |
|---------|---------------|---------|------|
| `AuthController` | `/api/auth` | 로그인/로그아웃 | 완료  |
| `AdminController` | `/api/admin` | 관리자 2단계 인증 | 완료  |
| `AdminAuthTokenController` | `/api/admin/auth` | 관리자 토큰 관리 | 완료  |

### **공통 게시판 API**
| 컨트롤러 | 엔드포인트 prefix | 주요 기능 | 상태 |
|---------|---------------|---------|------|
| `BoardController` | `/api/boards` | 게시판 조회 | 완료  |
| `BoardCreateController` | `/api/boards` | 게시글 작성 | 완료  |
| `BoardUpdateController` | `/api/boards` | 게시글 수정 | 완료  |
| `BoardAttachmentUploadController` | `/api/board-attachments` | 첨부파일 업로드 | 완료  |
| `BoardAttachmentDownloadController` | `/api/board-attachments` | 첨부파일 다운로드 | 완료  |

## 향후 구현 계획

### **강의 평가 (미구현)**
- `EvaluationController` - 미구현 상태
- `LectureEvaluationController` - 미구현 상태
- **예정 엔드포인트**: `/api/evaluations/*`
- **데이터 저장**: `UserTbl.lectureEvaluations` 필드 사용

### **채팅 시스템 (미구현)**
- `ChatController` - 미구현 상태
- **예정 엔드포인트**: `/api/chat/*`
- **기술**: WebSocket 기반 예정

### **관리자 통계 (미구현)**
- `StatisticsController` - 미구현 상태
- **예정 엔드포인트**: `/api/admin/statistics/*`
- **비고**: 엔드포인트로 통계 데이터 제공 예정

## 전체 엔드포인트 prefix 요약

### **구현 완료 prefix**
```
/api/auth/*              - 인증 (AuthController)
/api/admin/*             - 관리자 (AdminController, AdminAuthTokenController)
/lectures/*              - 강의 관리 (LectureController)
  특히: /lectures/eligible - 수강 가능 강의 조회
/enrollments/*           - 수강신청 (EnrollmentController)
/api/assignments/*       - 과제 관리 (AssignmentController)
/api/professor/attendance/* - 교수 출석 (ProfessorAttendanceController)
/api/student/attendance/*   - 학생 출석 (StudentAttendanceController)
/api/boards/*            - 게시판 (Board*Controller들)
/api/board-attachments/* - 첨부파일 (BoardAttachment*Controller들)
```

### **미구현 예정 prefix**
```
/api/evaluations/*       - 강의 평가 (미구현)
/api/chat/*              - 채팅 (미구현)
/api/admin/statistics/*  - 관리자 통계 (미구현)
```

## 통계 요약

- **구현 완료**: 13개 컨트롤러
- **미구현**: 3개 기능 예정
- **API 엔드포인트**: 100% (구현 완료 기능)
- **엔드포인트 prefix 표준화**: 완료 

---

*이 문서는 API 매핑 현황을 추적하기 위한 문서입니다.*