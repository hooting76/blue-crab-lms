# 🧪 Blue Crab LMS 브라우저 콘솔 테스트 가이드 v6.0

## 📋 개요

이 문서는 Blue Crab LMS 강의 관련 기능을 브라우저 콘솔에서 테스트하기 위한 완전한 가이드입니다.
모든 테스트 스크립트는 실제 API 엔드포인트와 연동되며, 날짜 형식이 **YYYYMMDD**로 통일되었습니다.

## 🗂️ 테스트 파일 구조

### 🔐 로그인 관련
- **일반유저**: `docs/일반유저 로그인+게시판/test-1-login.js`
- **관리자**: `docs/관리자 로그인/admin-login-to-board-test.js`

### 👨‍💼 관리자 테스트
- **`lecture-test-1-admin-create.js`**: 강의 등록/수정/삭제 및 통계

### 👨‍🎓 학생 테스트
- **`lecture-test-2a-student-enrollment.js`**: 수강 가능 강의 조회 및 신청
- **`lecture-test-2b-student-my-courses.js`**: 내 수강 목록 조회 및 취소
- **`lecture-test-3-student-assignment.js`**: 과제 조회 및 제출 기록

### 👨‍🏫 교수 테스트
- **`lecture-test-4a-professor-assignment-create.js`**: 과제 생성 및 목록 조회
- **`lecture-test-4b-professor-assignment-grade.js`**: 과제 채점 및 관리
- **`lecture-test-5-professor-students.js`**: 수강생 조회 및 관리

### 📊 관리자 통계
- **`lecture-test-6-admin-statistics.js`**: 전체 통계 및 모니터링

## 🚀 시작하기

### 1. 기본 환경 설정
```javascript
// API 기본 URL
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

// 전역 토큰 변수 (로그인 후 자동 설정)
window.authToken = null;
window.currentUser = null;
```

**⚠️ API URL 주의사항:**
- 프로젝트 베이스 URL: `https://bluecrab.chickenkiller.com/BlueCrab-1.0.0`  
- API 전용 URL: `https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api`
- 모든 강의 관련 API 엔드포인트는 `/api/` 경로를 사용하므로 편의상 `API_BASE_URL`로 통일
- 실제 호출 시: `${API_BASE_URL}/lectures` → `https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/lectures`

**🔍 프론트엔드 URL 패턴 비교:**
```javascript
// 1. AuthFunc.jsx (권장) - 동적 URL 생성
const BASE = 'https://bluecrab.chickenkiller.com';
const CONTEXT = '/BlueCrab-1.0.0';
apiUrl('/api/lectures') // 추천: 유연하고 재사용 가능

// 2. CourseRegister.jsx (테스트 코드와 동일) - API 베이스 고정
const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
fetch(`${BASE_URL}/lectures`) // 현재 테스트 코드 방식

// 3. noticeAPI.jsx (비추천) - 특정 리소스까지 하드코딩
const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/boards';
fetch(`${BASE_URL}/list`) // 유연성 부족
```

**💡 테스트 코드는 방식 2번을 사용** - 프론트엔드 `CourseRegister.jsx`와 동일한 패턴

### 2. 사용자 역할별 로그인

#### 👨‍🎓 학생/교수 로그인
```javascript
// 1. 로그인 스크립트 로드
// docs/일반유저 로그인+게시판/test-1-login.js 실행

// 2. 로그인 수행
await login()
// ID/PW 입력 후 JWT 토큰 자동 저장
```

#### 👨‍💼 관리자 로그인
```javascript
// 1. 관리자 로그인 스크립트 로드
// docs/관리자 로그인/admin-login-to-board-test.js 실행

// 2. 단계별 로그인
await adminLogin()      // ID/PW 입력
await sendAuthCode()    // 이메일 인증코드 발송
await verifyAuthCode()  // 인증코드 확인
```

## 📝 테스트 시나리오별 가이드

### 🎯 학생 시나리오

#### 1단계: 수강신청
```javascript
// lecture-test-2a-student-enrollment.js 로드 후

// 수강 가능한 강의 조회 (백엔드 필터링)
await getAvailableLectures()

// 수강신청
await enrollInLecture()
```

#### 2단계: 내 수강 관리
```javascript
// lecture-test-2b-student-my-courses.js 로드 후

// 내 수강 목록 조회
await getMyEnrollments()

// 수강 취소 (필요시)
await cancelEnrollment()

// 강의 상세 정보
await getLectureDetail()
```

#### 3단계: 과제 관리
```javascript
// lecture-test-3-student-assignment.js 로드 후

// 내 과제 목록
await getMyAssignments()

// 과제 상세 조회
await getAssignmentDetail()

// 과제 제출 완료 표시 (오프라인 제출 후)
await markAsSubmitted()
```

### 🎯 교수 시나리오

#### 1단계: 과제 생성
```javascript
// lecture-test-4a-professor-assignment-create.js 로드 후

// 내 담당 강의 조회
await getMyLectures()

// 과제 생성 (날짜: YYYYMMDD 형식)
await createAssignment()
// 예시: dueDate = "20251231"

// 과제 목록 확인
await getAssignments()
```

#### 2단계: 과제 채점
```javascript
// lecture-test-4b-professor-assignment-grade.js 로드 후

// 학생별 제출 현황
await getSubmissions()

// 과제 채점
await gradeAssignment()

// 과제 수정/삭제
await updateAssignment()
await deleteAssignment()
```

#### 3단계: 수강생 관리
```javascript
// lecture-test-5-professor-students.js 로드 후

// 수강생 목록
await getStudents()

// 수강생 상세 정보
await getStudentDetail()

// 수강생 성적 조회
await getStudentGrades()

// 강의 통계
await getLectureStatistics()
```

### 🎯 관리자 시나리오

#### 강의 관리
```javascript
// lecture-test-1-admin-create.js 로드 후

// 강의 등록
await createLecture()

// 전체 강의 목록
await getAllLectures()

// 강의 수정/삭제
await updateLecture()
await deleteLecture()

// 강의 통계
await getLectureStats()
```

#### 시스템 통계
```javascript
// lecture-test-6-admin-statistics.js 로드 후

// 전체 강의 통계
await getLectureStatistics()

// 학생별 통계
await getStudentStatistics()

// 교수별 통계
await getProfessorStatistics()

// 인기 강의 순위
await getPopularLectures()
```

## 🔧 주요 기능 및 특징

### 🎯 JWT 토큰 자동 관리
- 로그인 후 `window.authToken`에 자동 저장
- 모든 API 요청에 `Authorization: Bearer ${token}` 헤더 포함
- JWT에서 USER_IDX 자동 추출 (`getUserIdxFromToken()`)

### 📅 날짜 형식 통일
```javascript
// 모든 날짜 입력은 YYYYMMDD 형식
dueDate: "20251231"        // 과제 마감일
enrollmentDate: "20250301" // 수강신청일
```

### 🔍 디버깅 도구
```javascript
// JWT 토큰 정보 확인
debugTokenInfo()

// 로그인 상태 확인
checkAuth()

// 전역 변수 확인
console.log('Token:', window.authToken)
console.log('User:', window.currentUser)
```

### 📊 응답 형식 처리
- **Page 객체**: 페이징된 목록 (content, totalElements 등)
- **ApiResponse 래퍼**: 성공/실패 메시지 포함
- **직접 객체**: 단순 엔티티 반환

## ⚠️ 주의사항

### 🔐 인증 관련
- 모든 API 호출 전 `checkAuth()` 확인
- 토큰 만료 시 재로그인 필요
- 관리자 기능은 관리자 계정으로만 접근 가능

### 📝 데이터 입력
- **lecSerial**: 강의 코드 (예: CS101, MATH201)
- **professor**: USER_IDX 숫자 (예: 22, 23, 24)
- **dates**: YYYYMMDD 형식 (예: 20251231)
- **scores**: 과제 점수는 항상 10점 만점

### 🌐 네트워크
- **실제 서버**: `https://bluecrab.chickenkiller.com`
- **프로젝트 베이스**: `/BlueCrab-1.0.0`
- **API 엔드포인트**: `/api/lectures`, `/api/assignments` 등
- **완전한 URL 예시**: `https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/lectures`
- CORS 정책으로 브라우저 콘솔에서만 실행 가능
- 네트워크 에러 시 서버 상태 확인 필요

## 🆘 트러블슈팅

### 1. 로그인 실패
```javascript
// 토큰 상태 확인
console.log('Token exists:', !!window.authToken)

// 재로그인
await login() // 또는 await adminLogin()
```

### 2. API 호출 실패
```javascript
// 로그인 상태 확인
checkAuth()

// 네트워크 상태 확인 (실제 API 엔드포인트 테스트)
fetch('https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/lectures/list', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${window.authToken}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ page: 0, size: 1 })
}).then(r => console.log('API status:', r.status))
```

### 3. 권한 에러 (403)
```javascript
// 사용자 역할 확인
debugTokenInfo()

// 올바른 계정으로 재로그인
// 학생: 학생 계정, 교수: 교수 계정, 관리자: 관리자 계정
```

## 📚 참고 자료

### API 문서
- **Phase1**: 강의 등록 및 준비
- **Phase2**: 수강신청 프로세스  
- **Phase3**: 학기 진행 및 과제
- **Phase4**: 성적 및 완료 처리

### 테스트 데이터
- **professor_accounts.csv**: 교수 계정 정보
- 강의 코드: CS101, MATH201, ENG301 등
- 학생 계정: 테스트용 학생 ID/PW

---

## 🎉 마무리

이 가이드를 통해 Blue Crab LMS의 모든 강의 관련 기능을 체계적으로 테스트할 수 있습니다.
각 테스트 파일의 `help()` 함수를 호출하면 더 자세한 사용법을 확인할 수 있습니다.

**Happy Testing! 🚀**