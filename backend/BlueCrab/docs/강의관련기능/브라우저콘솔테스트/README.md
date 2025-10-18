# 📋 브라우저 콘솔 테스트 디렉토리

Blue Crab LMS 강의 관련 기능을 브라우저 콘솔에서 테스트하기 위한 파일들입니다.

## 📁 파일 구조

### 📖 가이드 문서
- **`TEST_GUIDE.md`** - 🆕 완전한 테스트 가이드 (v6.0)
- **`usage-diagram.drawio`** - 테스트 흐름도 (Draw.io 형식)
- **`usage-diagram.drawio.png`** - 테스트 흐름도 (이미지)

### 🧪 테스트 스크립트

#### 👨‍💼 관리자
- **`lecture-test-1-admin-create.js`** - 강의 CRUD 및 통계
- **`lecture-test-6-admin-statistics.js`** - 전체 시스템 통계

#### 👨‍🎓 학생  
- **`lecture-test-2a-student-enrollment.js`** - 수강 가능 강의 조회 및 신청
- **`lecture-test-2b-student-my-courses.js`** - 내 수강 목록 및 취소
- **`lecture-test-3-student-assignment.js`** - 과제 조회 및 제출

#### 👨‍🏫 교수
- **`lecture-test-4a-professor-assignment-create.js`** - 과제 생성 및 목록
- **`lecture-test-4b-professor-assignment-grade.js`** - 과제 채점 및 관리  
- **`lecture-test-5-professor-students.js`** - 수강생 조회 및 관리

### 📊 참고 데이터
- **`professor_accounts.csv`** - 교수 계정 정보

## 🚀 빠른 시작

### 1. 테스트 가이드 확인
```markdown
👉 TEST_GUIDE.md를 먼저 읽어보세요!
- 전체 시나리오별 가이드
- 역할별 테스트 순서
- 트러블슈팅 방법
```

### 2. 로그인 수행
```javascript
// 학생/교수: docs/일반유저 로그인+게시판/test-1-login.js
await login()

// 관리자: docs/관리자 로그인/admin-login-to-board-test.js  
await adminLogin()
await sendAuthCode()
await verifyAuthCode()
```

### 3. 역할별 테스트 실행
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