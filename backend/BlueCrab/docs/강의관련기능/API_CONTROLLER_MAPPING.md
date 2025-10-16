#  API 엔드포인트 컨트롤러 매핑 (v5.0 - POST 방식 완전 통일)

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
| `/lectures/eligible` | POST (Body: {studentIdx}) | 학생이 수강 가능한 강의 조회 (0값 규칙 + 전공 필터링) | LectureController |

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
/lectures/*              - 강의 관리 (LectureController) - 100% POST
  ├─ POST /lectures              - 강의 목록 조회
  ├─ POST /lectures/detail       - 강의 상세 조회
  ├─ POST /lectures/stats        - 강의 통계
  ├─ POST /lectures/eligible     - 수강 가능 강의 (전공 필터링)
  ├─ POST /lectures/create       - 강의 생성
  ├─ POST /lectures/update       - 강의 수정
  └─ POST /lectures/delete       - 강의 삭제
/enrollments/*           - 수강신청 (EnrollmentController) - 100% POST
  ├─ POST /enrollments/list      - 수강 목록
  ├─ POST /enrollments/detail    - 수강 상세
  ├─ POST /enrollments/data      - JSON 데이터
  ├─ POST /enrollments/enroll    - 수강신청
  ├─ POST /enrollments/drop      - 수강취소
  ├─ POST /enrollments/attendance - 출석 갱신
  └─ POST /enrollments/grade     - 성적 입력
/api/assignments/*       - 과제 관리 (AssignmentController) - 100% POST
  ├─ POST /list                  - 과제 목록
  ├─ POST /detail                - 과제 상세
  ├─ POST /data                  - JSON 데이터
  ├─ POST /submissions           - 제출 현황
  ├─ POST /                      - 과제 생성
  ├─ POST /{id}/submit           - 과제 제출
  ├─ POST /{id}                  - 과제 수정
  ├─ POST /{id}/grade            - 과제 채점
  └─ POST /{id}                  - 과제 삭제
/api/professor/attendance/* - 교수 출석 (ProfessorAttendanceController) - 100% POST
/api/student/attendance/*   - 학생 출석 (StudentAttendanceController) - 100% POST
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
- **API 통신 방식**: 100% POST (Request Body 기반)
- **엔드포인트 prefix 표준화**: 완료
- **백엔드 필터링**: 전공/부전공 기반 수강 가능 강의 자동 필터링 구현 

---

*최종 업데이트: 2025-10-16 - POST 방식 완전 통일 및 백엔드 필터링 구현*