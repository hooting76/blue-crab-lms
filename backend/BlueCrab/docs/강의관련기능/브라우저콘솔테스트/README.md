# 📚 강의 관리 시스템 브라우저 콘솔 테스트 가이드

> **작성일**: 2025-10-12  
> **최종 업데이트**: 2025-10-13
> **버전**: 2.0
> **목적**: 브라우저 콘솔에서 API 테스트하기

---

## ⚠️ 중요: 사전 로그인 필수!

**모든 테스트 파일 실행 전에 반드시 로그인을 완료**해야 합니다!

### � 역할별 로그인 방법

#### 1️⃣ 관리자 (Admin)
**로그인 파일**: `docs/관리자 로그인/admin-login-to-board-test.js`

```javascript
// 1. admin-login-to-board-test.js 파일 내용을 콘솔에 복사
// 2. 로그인 실행
adminLogin()  // 단계별 로그인
// 또는
quickAdminLogin()  // 빠른 통합 로그인

// 3. 로그인 확인
checkLoginStatus()
```

**로그인 후 실행 가능:**
- ✅ `lecture-test-1-admin-create.js` - 관리자 강의 등록/관리
- ✅ `lecture-test-6-admin-statistics.js` - 관리자 통계 조회

#### 2️⃣ 학생 (Student)
**로그인 파일**: `docs/일반유저 로그인+게시판/test-1-login.js`

```javascript
// 1. test-1-login.js 파일 내용을 콘솔에 복사
// 2. 학생 로그인 실행
loginUser()

// 3. 로그인 확인
checkAuth()
```

**로그인 후 실행 가능:**
- ✅ `lecture-test-2-student-enrollment.js` - 학생 수강신청
- ✅ `lecture-test-3-student-assignment.js` - 학생 과제 제출

#### 3️⃣ 교수 (Professor)
**로그인 파일**: 교수용 로그인 파일 필요 (현재 미구현)

**로그인 후 실행 가능:**
- ⏳ `lecture-test-4-professor-assignment.js` - 교수 과제 관리
- ⏳ `lecture-test-5-professor-students.js` - 교수 수강생 관리

---

## �📋 목차

1. [개요](#개요)
2. [사용 방법](#사용-방법)
3. [테스트 파일 목록](#테스트-파일-목록)
4. [테스트 시나리오](#테스트-시나리오)
5. [문제 해결](#문제-해결)

---

## 개요

강의 관리 시스템의 모든 API를 **브라우저 콘솔에서 직접 테스트**할 수 있는 JavaScript 파일 모음입니다.

### ✨ 특징

- 🎯 **prompt 함수**로 입력받기
- 📊 **심플한 콘솔 로그**로 결과 확인
- 🔄 **복사-붙여넣기** 방식으로 간편 실행
- 🔑 **JWT 토큰** 자동 저장 및 재사용
- 🔐 **로그인 연동** - 사전 로그인 후 토큰 자동 사용

---

## 사용 방법

### 1️⃣ 브라우저 개발자 도구 열기

- **Chrome/Edge**: `F12` 또는 `Ctrl + Shift + I`
- **Firefox**: `F12` 또는 `Ctrl + Shift + K`
- **Safari**: `Cmd + Option + I` (개발자 메뉴 활성화 필요)

### 2️⃣ Console 탭 선택

### 3️⃣ 로그인 먼저 수행

**중요**: 테스트 파일을 실행하기 전에 역할에 맞는 로그인을 먼저 완료하세요!

### 4️⃣ 테스트 파일 로드

1. 테스트 파일 열기 (예: `lecture-test-1-admin-create.js`)
2. 전체 내용 복사 (`Ctrl + A` → `Ctrl + C`)
3. 브라우저 콘솔에 붙여넣기 (`Ctrl + V`)
4. `Enter` 키로 실행

```javascript
✅ 관리자 강의 관리 테스트 스크립트 로드 완료!
💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.
```

### 4️⃣ 도움말 확인

```javascript
help()
```

### 5️⃣ JWT 토큰 설정

```javascript
setToken()
// 프롬프트에서 JWT 토큰 입력
```

### 6️⃣ 테스트 함수 실행

```javascript
createLecture()
// 프롬프트에서 강의 정보 입력
```

---

## 테스트 파일 목록

### 1. 관리자 - 강의 생성 및 관리
**파일**: `lecture-test-1-admin-create.js`

#### 제공 함수
- `setToken()` - JWT 토큰 설정
- `createLecture()` - 강의 등록
- `getLectures()` - 강의 목록 조회
- `getLectureDetail()` - 강의 상세 조회
- `updateLecture()` - 강의 수정
- `deleteLecture()` - 강의 삭제

#### 테스트 순서
```javascript
// 1. 토큰 설정
setToken()

// 2. 강의 등록
createLecture()

// 3. 강의 목록 조회
getLectures()

// 4. 강의 상세 조회
getLectureDetail()

// 5. 강의 수정
updateLecture()

// 6. 강의 삭제 (선택)
deleteLecture()
```

---

### 2. 학생 - 수강 신청
**파일**: `lecture-test-2-student-enrollment.js`

#### 제공 함수
- `setToken()` - JWT 토큰 설정
- `getAvailableLectures()` - 수강 가능 강의 목록
- `enrollLecture()` - 수강 신청
- `getMyEnrollments()` - 내 수강 목록
- `cancelEnrollment()` - 수강 취소
- `getLectureDetail()` - 강의 상세 조회

#### 테스트 순서
```javascript
// 1. 토큰 설정 (학생 토큰)
setToken()

// 2. 수강 가능 강의 조회
getAvailableLectures()

// 3. 수강 신청
enrollLecture()

// 4. 내 수강 목록 확인
getMyEnrollments()

// 5. 수강 취소 (선택)
cancelEnrollment()
```

---

### 3. 학생 - 과제 제출
**파일**: `lecture-test-3-student-assignment.js`

#### 제공 함수
- `setToken()` - JWT 토큰 설정
- `getMyAssignments()` - 내 과제 목록
- `getAssignmentDetail()` - 과제 상세 조회
- `submitAssignment()` - 과제 제출
- `resubmitAssignment()` - 과제 재제출
- `cancelSubmission()` - 과제 제출 취소

#### 테스트 순서
```javascript
// 1. 토큰 설정 (학생 토큰)
setToken()

// 2. 내 과제 목록 조회
getMyAssignments()

// 3. 과제 상세 조회
getAssignmentDetail()

// 4. 과제 제출
submitAssignment()

// 5. 과제 재제출 (선택)
resubmitAssignment()
```

---

### 4. 교수 - 과제 관리
**파일**: `lecture-test-4-professor-assignment.js`

#### 제공 함수
- `setToken()` - JWT 토큰 설정
- `getMyLectures()` - 담당 강의 목록
- `createAssignment()` - 과제 생성
- `getAssignments()` - 과제 목록 조회
- `getSubmissions()` - 제출된 과제 목록
- `gradeAssignment()` - 과제 채점
- `updateAssignment()` - 과제 수정
- `deleteAssignment()` - 과제 삭제

#### 테스트 순서
```javascript
// 1. 토큰 설정 (교수 토큰)
setToken()

// 2. 담당 강의 확인
getMyLectures()

// 3. 과제 생성
createAssignment()

// 4. 제출된 과제 확인
getSubmissions()

// 5. 과제 채점
gradeAssignment()
```

---

### 5. 교수 - 수강생 관리
**파일**: `lecture-test-5-professor-students.js`

#### 제공 함수
- `setToken()` - JWT 토큰 설정
- `getStudents()` - 수강생 목록 조회
- `getStudentDetail()` - 수강생 상세 조회
- `getStudentGrades()` - 수강생 성적 조회
- `getLectureStatistics()` - 강의 통계 조회
- `searchStudents()` - 수강생 검색

#### 테스트 순서
```javascript
// 1. 토큰 설정 (교수 토큰)
setToken()

// 2. 수강생 목록 조회
getStudents()

// 3. 수강생 상세 정보
getStudentDetail()

// 4. 수강생 성적 조회
getStudentGrades()

// 5. 강의 통계 확인
getLectureStatistics()
```

---

### 6. 관리자 - 통계 및 모니터링
**파일**: `lecture-test-6-admin-statistics.js`

#### 제공 함수
- `setToken()` - JWT 토큰 설정
- `getLectureStatistics()` - 전체 강의 통계
- `getStudentStatistics()` - 학생별 통계
- `getProfessorStatistics()` - 교수별 통계
- `getSemesterTrends()` - 학기별 트렌드
- `getPopularLectures()` - 인기 강의 순위
- `getDepartmentStatistics()` - 학과별 통계

#### 테스트 순서
```javascript
// 1. 토큰 설정 (관리자 토큰)
setToken()

// 2. 전체 강의 통계
getLectureStatistics()

// 3. 인기 강의 순위
getPopularLectures()

// 4. 학과별 통계
getDepartmentStatistics()

// 5. 학기별 트렌드
getSemesterTrends()
```

---

## 테스트 시나리오

### 🎯 시나리오 1: 전체 플로우 테스트

#### 1단계: 관리자 - 강의 개설
```javascript
// lecture-test-1-admin-create.js 로드
setToken() // 관리자 토큰 입력
createLecture() // 강의명: 자바 프로그래밍, 코드: CS101
getLectures() // 생성 확인
```

#### 2단계: 교수 - 과제 생성
```javascript
// lecture-test-4-professor-assignment.js 로드
setToken() // 교수 토큰 입력
getMyLectures() // 담당 강의 확인
createAssignment() // LECTURE_IDX 입력, 과제 생성
```

#### 3단계: 학생 - 수강신청 및 과제 제출
```javascript
// lecture-test-2-student-enrollment.js 로드
setToken() // 학생 토큰 입력
getAvailableLectures() // 수강 가능 강의 확인
enrollLecture() // LECTURE_IDX 입력, 수강신청

// lecture-test-3-student-assignment.js 로드
getMyAssignments() // 내 과제 확인
submitAssignment() // ASSIGNMENT_IDX 입력, 과제 제출
```

#### 4단계: 교수 - 과제 채점
```javascript
// lecture-test-4-professor-assignment.js 로드
getSubmissions() // 제출된 과제 확인
gradeAssignment() // STUDENT_IDX 입력, 채점
```

#### 5단계: 관리자 - 통계 확인
```javascript
// lecture-test-6-admin-statistics.js 로드
getLectureStatistics() // 전체 통계 확인
getPopularLectures() // 인기 강의 순위
```

---

## 문제 해결

### ❓ 토큰이 만료되었다고 나올 때
```javascript
// 새로운 토큰으로 다시 설정
setToken()
```

### ❓ CORS 에러가 발생할 때
- 백엔드 서버에서 CORS 설정 확인
- 브라우저가 API 서버와 같은 도메인에 있는지 확인

### ❓ 함수가 정의되지 않았다고 나올 때
- 테스트 파일을 다시 로드
- 콘솔 창을 새로고침 (`Ctrl + L`)

### ❓ 입력 프롬프트가 뜨지 않을 때
- 브라우저 팝업 차단 설정 확인
- 다른 브라우저에서 시도

### ❓ 응답이 없을 때
```javascript
// 네트워크 탭에서 요청 확인
// F12 → Network 탭 → XHR 필터
```

---

## 🎓 추가 정보

### API 베이스 URL
```javascript
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
```

### 토큰 저장 위치
- `window.authToken` - 메모리
- `localStorage.authToken` - 브라우저 저장소

### 전역 변수
- `window.lastLectureIdx` - 마지막 생성된 강의 IDX
- `window.lastEnrollmentIdx` - 마지막 수강신청 IDX
- `window.lastAssignmentIdx` - 마지막 과제 IDX

### 강의 시간 포맷 (v2.0 업데이트)

**새로운 포맷**: `요일명+교시` 형식

#### 입력 가능한 형식
1. **쉼표/공백 형식**: `월1,2 수3,4` 
2. **표준 형식**: `월1월2수3수4`

두 형식 모두 자동으로 표준 형식(`월1월2수3수4`)으로 변환됩니다.

#### 요일 코드
- `월`: 월요일
- `화`: 화요일  
- `수`: 수요일
- `목`: 목요일
- `금`: 금요일

#### 교시 코드 (1~8교시)
- 1교시: 09:00-09:50
- 2교시: 10:00-10:50
- 3교시: 11:00-11:50
- 4교시: 12:00-12:50
- **점심시간**: 12:50-13:50
- 5교시: 13:50-14:40
- 6교시: 14:50-15:40
- 7교시: 15:50-16:40
- 8교시: 16:50-17:40

#### 예시
| 입력 | 변환 결과 | 의미 |
|------|-----------|------|
| `월1,2 수3,4` | `월1월2수3수4` | 월요일 1,2교시 + 수요일 3,4교시 |
| `화2,3 목2` | `화2화3목2` | 화요일 2,3교시 + 목요일 2교시 |
| `월1월2월3월4` | `월1월2월3월4` | 월요일 1~4교시 (변환 불필요) |

---

## 📞 문의

테스트 중 문제가 발생하면 백엔드 로그를 확인하세요:
```bash
# 백엔드 로그 확인
tail -f logs/application.log
```

---

**Happy Testing! 🚀**
