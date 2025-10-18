# 🎯 Blue Crab LMS 핵심 중요 API 목록

## 📌 **1. 인증 & 권한 관리** (최우선)
```
POST /api/auth/login                    # 사용자 로그인
POST /api/auth/refresh                  # 토큰 갱신
GET  /api/auth/validate                 # 토큰 검증
POST /api/auth/logout                   # 로그아웃

POST /api/admin/login                   # 관리자 로그인
POST /api/admin/email-auth/request      # 관리자 이메일 인증 요청
POST /api/admin/email-auth/verify       # 관리자 이메일 인증 확인
```

## 📚 **2. 강의 관리** (핵심 비즈니스)
```
POST /api/lectures                      # 강의 목록 조회
POST /api/lectures/detail               # 강의 상세 조회
POST /api/lectures/stats                # 강의 통계
POST /api/lectures/eligible             # 수강 가능 강의
POST /api/lectures/create               # 강의 생성 (관리자/교수)
POST /api/lectures/update               # 강의 수정
POST /api/lectures/delete               # 강의 삭제
```

## 🎓 **3. 수강신청 & 성적 관리** (핵심 비즈니스)
```
POST /api/enrollments/list              # 수강신청 목록
POST /api/enrollments/detail            # 수강신청 상세
POST /api/enrollments/enroll            # 수강신청
DELETE /api/enrollments/{enrollmentIdx} # 수강 취소

# 🔥 성적 관리 (우리가 추가한 부분)
POST /api/enrollments/grade-config      # 성적 구성 설정
POST /api/enrollments/grade-info        # 개별 성적 조회
POST /api/enrollments/grade-list        # 전체 성적 목록
POST /api/enrollments/grade-finalize    # 최종 등급 배정

# ⚡ 성적 업데이트 트리거
PUT  /api/enrollments/{enrollmentIdx}/grade      # 성적 직접 수정
PUT  /api/enrollments/{enrollmentIdx}/attendance # 출석 정보 수정
```

## 📝 **4. 과제 관리** (핵심 비즈니스)
```
POST /api/assignments/list              # 과제 목록 조회
POST /api/assignments/detail            # 과제 상세 조회
POST /api/assignments                   # 과제 생성
POST /api/assignments/{assignmentIdx}/submit    # 과제 제출
PUT  /api/assignments/{assignmentIdx}/grade     # ⚡ 과제 채점 (성적 이벤트 발생)
PUT  /api/assignments/{assignmentIdx}           # 과제 수정
DELETE /api/assignments/{assignmentIdx}         # 과제 삭제
POST /api/assignments/submissions       # 과제 제출 목록 조회
```

## 📅 **5. 출석 관리** (핵심 비즈니스)
```
# 교수용 출석 관리
POST /api/professor/attendance/mark     # ⚡ 출석 체크 (성적 이벤트 발생)
POST /api/professor/attendance/requests # 출석 수정 요청 목록
PUT  /api/professor/attendance/requests/{requestIdx}/approve # 출석 요청 승인
PUT  /api/professor/attendance/requests/{requestIdx}/reject  # 출석 요청 거부

# 학생용 출석 관리
POST /api/student/attendance/detail     # 출석 상세 조회
POST /api/student/attendance/request    # 출석 수정 요청
POST /api/student/attendance/requests   # 출석 요청 목록
```

## 📋 **6. 게시판 시스템** (일반 기능)
```
POST /api/boards/list                   # 게시글 목록
POST /api/boards/detail                 # 게시글 상세
POST /api/boards/create                 # 게시글 작성
POST /api/boards/update/{boardIdx}      # 게시글 수정
POST /api/boards/delete/{boardIdx}      # 게시글 삭제

# 첨부파일 관리
POST /api/board-attachments/upload/{boardIdx}  # 파일 업로드
POST /api/board-attachments/download            # 파일 다운로드
POST /api/board-attachments/delete              # 파일 삭제
```

## 👤 **7. 사용자 프로필** (일반 기능)
```
POST /api/profile/me                    # 내 프로필 조회
POST /api/profile/me/completeness       # 프로필 완성도
POST /api/profile/me/image              # 프로필 이미지 업로드
```

## 🏢 **8. 시설 예약** (부가 기능)
```
POST /api/facilities                    # 시설 목록
POST /api/facilities/{facilityIdx}      # 시설 상세
POST /api/facilities/{facilityIdx}/availability # 시설 예약 가능 시간

POST /api/reservations                  # 예약 생성
POST /api/reservations/my               # 내 예약 목록
DELETE /api/reservations/{reservationIdx} # 예약 취소

# 관리자용 예약 관리
POST /api/admin/reservations/pending    # 대기중인 예약
POST /api/admin/reservations/approve    # 예약 승인
POST /api/admin/reservations/reject     # 예약 거부
```

## 📚 **9. 열람실 관리** (부가 기능)
```
POST /api/reading-room/status           # 열람실 현황
POST /api/reading-room/reserve          # 좌석 예약
POST /api/reading-room/checkout         # 퇴실 처리
POST /api/reading-room/my-reservation   # 내 예약 조회
```

## 🔔 **10. 푸시 알림** (부가 기능)
```
POST /api/fcm/register                  # FCM 토큰 등록
POST /api/fcm/send                      # 푸시 알림 발송
DELETE /api/fcm/unregister              # FCM 토큰 해제
POST /api/fcm/send/batch                # 일괄 알림 발송
```

## 🔍 **11. 시스템 모니터링** (운영 지원)
```
GET  /api/health                        # 헬스 체크
GET  /api/system-info                   # 시스템 정보
GET  /api/ping                          # 연결 확인
GET  /api/test                          # 테스트 엔드포인트
```

---

## 🎯 **우선순위별 분류**

### **🔥 최우선 (Core Business)**
1. **인증 시스템** - 모든 기능의 기반
2. **강의 관리** - 핵심 비즈니스 로직
3. **수강신청 & 성적 관리** - 핵심 비즈니스 로직
4. **과제 관리** - 핵심 비즈니스 로직
5. **출석 관리** - 핵심 비즈니스 로직

### **⚡ 성적 관리 이벤트 연동이 필요한 API**
- `PUT /api/assignments/{assignmentIdx}/grade` (과제 채점)
- `POST /api/professor/attendance/mark` (출석 체크)
- `PUT /api/enrollments/{enrollmentIdx}/attendance` (출석 수정)

### **📝 중요 (Supporting Features)**
- 게시판 시스템
- 사용자 프로필 관리

### **🔧 보조 (Additional Features)**
- 시설 예약
- 열람실 관리
- 푸시 알림

### **🛠️ 운영 지원 (Operations)**
- 시스템 모니터링
- 관리자 도구