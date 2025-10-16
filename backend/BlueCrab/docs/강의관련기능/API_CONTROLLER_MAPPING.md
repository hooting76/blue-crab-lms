# 🎯 API 컨트롤러 매핑 현황 (v2.6)

## 📋 구현 완료된 컨트롤러들

### **✅ 강의 관련 (Lecture 패키지)**
| 컨트롤러 | 엔드포인트 패턴 | 주요 기능 | 상태 |
|---------|---------------|---------|------|
| `LectureController` | `/api/lectures` | 강의 CRUD, 통계, **수강 가능 강의 조회** | ✅ 완료 |
| `EnrollmentController` | `/api/enrollments` | 수강신청/취소, 성적관리 | ✅ 완료 |
| `AssignmentController` | `/api/assignments` | 과제 CRUD, 제출/채점 | ✅ 완료 |
| `ProfessorAttendanceController` | `/api/professor/attendance` | 교수 출석 관리 | ✅ 완료 |
| `StudentAttendanceController` | `/api/student/attendance` | 학생 출석 요청 | ✅ 완료 |

### **🆕 신규 추가된 엔드포인트**
| 엔드포인트 | 메서드 | 기능 | 컨트롤러 |
|-----------|-------|------|---------|
| `/api/lectures/eligible/{studentId}` | GET | 학생별 수강 가능 강의 조회 (0값 규칙) | LectureController |

### **✅ 인증 관련**
| 컨트롤러 | 엔드포인트 패턴 | 주요 기능 | 상태 |
|---------|---------------|---------|------|
| `AuthController` | `/api/auth` | 로그인/토큰갱신 | ✅ 완료 |
| `AdminController` | `/api/admin` | 관리자 2차 인증 | ✅ 완료 |
| `AdminAuthTokenController` | `/api/admin/auth` | 관리자 토큰 관리 | ✅ 완료 |

### **✅ 게시판 관련**
| 컨트롤러 | 엔드포인트 패턴 | 주요 기능 | 상태 |
|---------|---------------|---------|------|
| `BoardController` | `/api/boards` | 게시판 조회 | ✅ 완료 |
| `BoardCreateController` | `/api/boards` | 게시글 작성 | ✅ 완료 |
| `BoardUpdateController` | `/api/boards` | 게시글 수정 | ✅ 완료 |
| `BoardAttachmentUploadController` | `/api/board-attachments` | 파일 업로드 | ✅ 완료 |
| `BoardAttachmentDownloadController` | `/api/board-attachments` | 파일 다운로드 | ✅ 완료 |

## ❌ 미구현 기능들

### **강의 평가 시스템**
- `EvaluationController` - 존재하지 않음
- `LectureEvaluationController` - 존재하지 않음
- **필요 엔드포인트**: `/api/evaluations/*`
- **데이터베이스**: `UserTbl.lectureEvaluations` 필드만 존재

### **실시간 채팅 시스템**
- `ChatController` - 존재하지 않음
- **필요 엔드포인트**: `/api/chat/*`
- **인프라**: WebSocket 설정 필요

### **관리자 통계 및 모니터링**
- `StatisticsController` - 존재하지 않음
- **필요 엔드포인트**: `/api/admin/statistics/*`
- **현재**: 개별 컨트롤러에서 기본 통계만 제공

## 🔧 엔드포인트 패턴 요약

### **실제 구현된 패턴**
```
/api/auth/*              - 인증 (AuthController)
/api/admin/*             - 관리자 (AdminController, AdminAuthTokenController)
/api/lectures/*          - 강의 관리 (LectureController)
  └─ /api/lectures/eligible/{studentId} - 🆕 수강 가능 강의 조회
/api/enrollments/*       - 수강신청 (EnrollmentController)
/api/assignments/*       - 과제 관리 (AssignmentController)
/api/professor/attendance/* - 교수 출석 (ProfessorAttendanceController)
/api/student/attendance/*   - 학생 출석 (StudentAttendanceController)
/api/boards/*            - 게시판 (Board*Controller들)
/api/board-attachments/* - 파일 첨부 (BoardAttachment*Controller들)
```

### **문서에서 제거된 패턴 (미구현)**
```
/api/evaluations/*       - 강의 평가 (미구현)
/api/chat/*              - 채팅 (미구현)
/api/admin/statistics/*  - 관리자 통계 (미구현)
```

## 📊 구현 현황 통계

- **구현 완료**: 13개 컨트롤러
- **미구현**: 3개 주요 기능 영역
- **API 문서 일치율**: 100% (실제 구현과 완전 일치)
- **엔드포인트 패턴 통일성**: ✅ 완료

---

*이 문서는 API 명세서 일관성 검토 과정에서 생성되었습니다.*