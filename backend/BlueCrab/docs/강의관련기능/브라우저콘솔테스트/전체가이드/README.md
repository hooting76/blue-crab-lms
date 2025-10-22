# 📋 브라우저 콘솔 테스트 디렉토리

Blue Crab LMS 강의 관련 기능을 브라우저 콘솔에서 테스트하기 위한 파일들입니다.

## 📁 디렉토리 구조

```
브라우저콘솔테스트/
├── 전체가이드/             # 📖 종합 가이드 문서
│   ├── README.md              # 이 파일
│   ├── TEST_GUIDE.md          # 완전한 테스트 가이드 (v6.0)
│   ├── usage-diagram.drawio   # 테스트 흐름도
│   └── usage-diagram.drawio.png
│
├── 00-login/              # 🔐 로그인
│   ├── admin-login.js                     # 관리자 로그인
│   └── user-login.js                      # 일반 사용자 로그인
│
├── 01-admin/              # 👨‍💼 관리자 테스트
│   ├── lecture-test-1-admin-create.js      # 강의 CRUD
│   └── lecture-test-6-admin-statistics.js  # 시스템 통계
│
├── 02-student/            # 👨‍🎓 학생 테스트
│   ├── lecture-test-2a-student-enrollment.js  # 수강 신청
│   ├── lecture-test-2b-student-my-courses.js  # 내 수강 목록
│   └── lecture-test-3-student-assignment.js   # 과제 조회/제출
│
├── 03-professor/          # 👨‍🏫 교수 테스트
│   ├── lecture-test-4a-professor-assignment-create.js  # 과제 생성
│   ├── lecture-test-4b-professor-assignment-grade.js   # 과제 채점
│   └── lecture-test-5-professor-students.js            # 수강생 관리
│
├── 04-grade/              # 📊 성적 관리 (Phase4)
│   └── grade-management-test.js            # 성적 관리 시스템 v2.0
│
├── 05-notice/             # 📢 안내문 (신규)
│   ├── notice-test-1-view.js               # 안내문 조회 (공개)
│   └── notice-test-2-admin-save.js         # 안내문 저장 (관리자)
│
└── data/                  # 📁 참고 데이터
    └── professor_accounts.csv              # 교수 계정 정보
```

## 🚀 빠른 시작

### 1. 테스트 가이드 확인

```markdown
👉 전체가이드/TEST_GUIDE.md를 먼저 읽어보세요!
- 전체 시나리오별 가이드
- 역할별 테스트 순서
- 트러블슈팅 방법
```

### 2. 역할별 테스트 파일 선택

#### 👨‍💼 관리자 테스트
```bash
01-admin/
├── lecture-test-1-admin-create.js      # 강의 생성, 수정, 삭제, 통계
└── lecture-test-6-admin-statistics.js  # 전체 시스템 통계 조회
```

**주요 기능**:
- 강의 CRUD
- 강의 통계 조회
- 시스템 전체 통계

#### 👨‍🎓 학생 테스트
```bash
02-student/
├── lecture-test-2a-student-enrollment.js  # 수강 가능 강의 조회 및 신청
├── lecture-test-2b-student-my-courses.js  # 내 수강 목록 및 취소
└── lecture-test-3-student-assignment.js   # 과제 조회 및 제출
```

**주요 기능**:
- 수강 가능 강의 조회
- 수강 신청/취소
- 과제 조회/제출

#### 👨‍🏫 교수 테스트
```bash
03-professor/
├── lecture-test-4a-professor-assignment-create.js  # 과제 생성
├── lecture-test-4b-professor-assignment-grade.js   # 과제 채점
└── lecture-test-5-professor-students.js            # 수강생 조회
```

**주요 기능**:
- 과제 생성/관리
- 과제 채점
- 수강생 목록 조회

#### 📊 성적 관리 (신규 Phase4)
```bash
04-grade/
└── grade-management-test.js  # 성적 관리 시스템 v2.0
```

**주요 기능**:
- ✅ 성적 구성 설정 (출석, 과제, 시험 배점)
- ✅ 학생 성적 조회 (출석율, 과제 점수, 총점)
- ✅ 교수용 성적 조회
- ✅ 전체 성적 목록 조회
- ✅ 최종 등급 배정 (60% 기준 + 상대평가)

**v2.0 개선사항**:
- ✅ HTTP 상태 코드 검증
- ✅ 성공/실패 명확한 표시
- ✅ 응답 시간 측정
- ✅ 응답 데이터 구조화 출력
- ✅ 동적 테스트 데이터 변경
- ✅ 테스트 결과 요약

#### 📢 안내문 (신규)

```bash
05-notice/
├── notice-test-1-view.js        # 안내문 조회 (공개, 인증 불필요)
└── notice-test-2-admin-save.js  # 안내문 저장 (관리자/교수)
```

**주요 기능**:
- 안내문 조회 (공개)
- 안내문 저장/수정 (관리자/교수)
- 단일 레코드 관리 (최신 1개만)

**권한**:
- 조회: 인증 불필요
- 저장: ROLE_ADMIN 또는 ROLE_PROFESSOR

### 3. API URL 이해
```javascript
// 프로젝트 베이스: https://bluecrab.chickenkiller.com/BlueCrab-1.0.0
// API 베이스: https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
```

### 4. 로그인 수행
```javascript
// 학생/교수: docs/일반유저 로그인+게시판/test-1-login.js
await login()

// 관리자: docs/관리자 로그인/admin-login-to-board-test.js  
await adminLogin()
await sendAuthCode()
await verifyAuthCode()
```

### 4. 역할별 테스트 실행
```javascript
// 각 테스트 파일을 브라우저 콘솔에 복사 후 실행
// 예: 학생 수강신청
await getAvailableLectures()  // 수강 가능 강의 조회
await enrollInLecture()       // 수강신청
```

## ✨ v6.0 주요 특징

### 🎯 완전한 기능 커버리지
- ✅ 관리자: 강의 관리 + 시스템 통계
- ✅ 학생: 수강신청 + 과제 제출
- ✅ 교수: 과제 관리 + 수강생 관리

### 📅 날짜 형식 통일
```javascript
// 모든 날짜는 YYYYMMDD 형식
dueDate: "20251231"        // 과제 마감일
enrollmentDate: "20250301" // 수강신청일
```

### 🔐 JWT 자동 처리
```javascript
// 로그인 후 자동 설정
window.authToken = "jwt_token_here"
window.currentUser = {userIdx: 123, ...}

// 모든 API에서 자동 USER_IDX 추출
getUserIdxFromToken()  // JWT에서 자동 추출
```

### 📋 체계적인 디버깅
```javascript
// 모든 테스트 파일에서 제공
checkAuth()        // 로그인 상태 확인
debugTokenInfo()   // JWT 정보 확인  
help()            // 사용 가능한 함수 목록
```

## 📞 도움말

### 🆘 문제 해결
1. **로그인 실패** → `checkAuth()` 확인 후 재로그인
2. **API 에러** → 네트워크 상태 및 서버 상태 확인
3. **권한 에러** → 올바른 역할의 계정으로 로그인

### 📚 추가 문서
- **API 명세**: `docs/강의관련기능/Phase1~4_*.md`
- **백엔드 구조**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/`
- **데이터베이스**: ERD 및 테이블 구조 문서

---

**🎉 Happy Testing!** 모든 기능이 실제 API와 연동되어 완전한 테스트 환경을 제공합니다.