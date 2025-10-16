# 📚 강의 관리 시스템 브라우저 콘솔 테스트 가이드

> **작성일**: 2025-10-12  
> **최종 업데이트**: 2025-10-16  
> **버전**: 4.0 (Phase 9 + 파일 분할 완료)  
> **목적**: 브라우저 콘솔에서 API 테스트하기  
> **변경사항 (v4.0)**:
> - ✅ Phase 9 백엔드 필터링 구현 반영 (학부/학과 코드 + 0값 규칙)
> - ✅ 테스트 파일 분할: 과도하게 긴 파일들을 기능별로 분리
>   * lecture-test-2 → 2a (수강신청), 2b (내 수강목록)
>   * lecture-test-4 → 4a (과제생성), 4b (과제채점)
> - ✅ 모든 테스트 파일 최신화 완료

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
- ✅ `lecture-test-1-admin-create.js` - 관리자 강의 등록/관리 (Phase 9 반영)
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
- ✅ `lecture-test-2a-student-enrollment.js` - 학생 수강 가능 강의 조회 및 신청 (Phase 9 백엔드 필터링)
- ✅ `lecture-test-2b-student-my-courses.js` - 학생 내 수강 목록 조회 및 취소
- ✅ `lecture-test-3-student-assignment.js` - 학생 과제 제출

#### 3️⃣ 교수 (Professor)
**로그인 파일**: `docs/일반유저 로그인+게시판/test-1-login.js` (교수 계정 사용)
**로그인 후 실행 가능:**
- ✅ `lecture-test-4a-professor-assignment-create.js` - 교수 과제 생성 및 목록 조회
- ✅ `lecture-test-4b-professor-assignment-grade.js` - 교수 과제 채점 및 관리
- ✅ `lecture-test-5-professor-students.js` - 교수 수강생 관리

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

### 1. 관리자 - 강의 생성 및 관리 (Phase 9 반영)
**파일**: `lecture-test-1-admin-create.js`

#### 제공 함수
- `checkAuth()` - 로그인 상태 확인
- `getLectures()` - 강의 목록 조회 (필터링 정보 포함)
- `getLectureDetail()` - 강의 상세 조회 (수강 자격 정보)
- `getLectureStats()` - 강의 통계 조회

#### Phase 9 새 기능
- ✅ 학부/학과 코드 정보 표시
- ✅ 0값 규칙 설명
- ✅ 수강 자격 조건 안내

---

### 2. 학생 - 수강 신청 (🆕 Phase 9 백엔드 필터링 + 파일 분할)

#### 2A. 수강 가능 강의 조회 및 신청
**파일**: `lecture-test-2a-student-enrollment.js`

##### 제공 함수
- `checkAuth()` - JWT 토큰 및 사용자 정보 자동 확인 ⭐
- `getUserFromToken()` - JWT에서 사용자 IDX 추출
- `debugTokenInfo()` - JWT 디버깅
- `getAvailableLectures()` - 수강 가능 강의 목록 (Phase 9 백엔드 필터링) ⭐
- `enrollLecture()` - 수강 신청 (JWT 자동 studentIdx)

##### Phase 9 백엔드 필터링 기능
- ✅ 학부/학과 코드 기반 자격 확인
- ✅ 0값 규칙: "0" = 모든 학생 수강 가능
- ✅ 전공 OR 부전공 매칭 지원
- ✅ 상세한 eligibilityReason 메시지
- ✅ 학생 전공/부전공 정보 표시

#### 2B. 내 수강 목록 조회 및 취소
**파일**: `lecture-test-2b-student-my-courses.js`

##### 제공 함수
- `checkAuth()` - 로그인 상태 확인
- `getMyEnrollments()` - 내 수강 목록 (DTO 응답)
- `cancelEnrollment()` - 수강 취소
- `getLectureDetail()` - 강의 상세 조회
- `debugTokenInfo()` - JWT 디버깅

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

### 4. 교수 - 과제 관리 (🆕 파일 분할)

#### 4A. 과제 생성 및 목록 조회
**파일**: `lecture-test-4a-professor-assignment-create.js`

##### 제공 함수
- `checkAuth()` - 로그인 상태 확인
- `getProfessorFromToken()` - JWT에서 교수 정보 추출
- `debugTokenInfo()` - JWT 디버깅
- `getMyLectures()` - 담당 강의 목록
- `createAssignment()` - 과제 생성 (10점 고정)
- `getAssignments()` - 과제 목록 조회

##### 특징
- ✅ JWT 토큰 자동 인식
- ✅ 교수번호 자동 추출
- ✅ 10점 만점 고정

#### 4B. 과제 채점 및 관리
**파일**: `lecture-test-4b-professor-assignment-grade.js`

##### 제공 함수
- `checkAuth()` - 로그인 상태 확인
- `getSubmissions()` - 학생별 제출 현황 조회
- `gradeAssignment()` - 과제 채점 (제출 방식 + 점수)
- `updateAssignment()` - 과제 수정
- `deleteAssignment()` - 과제 삭제

##### 특징
- ✅ 오프라인 제출 방식 기록
- ✅ 10점 초과 시 자동 변환
- ✅ 제출 방식과 피드백 분리

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
// lecture-test-2a-student-enrollment.js 로드 (수강신청)
checkAuth() // JWT 토큰 확인
getAvailableLectures() // 수강 가능 강의 확인 (Phase 9 백엔드 필터링)
enrollLecture() // LECTURE_IDX 입력, 수강신청

// lecture-test-2b-student-my-courses.js 로드 (수강목록)
getMyEnrollments() // 내 수강 목록 확인

// lecture-test-3-student-assignment.js 로드 (과제)
getMyAssignments() // 내 과제 확인
submitAssignment() // ASSIGNMENT_IDX 입력, 과제 제출
```

#### 4단계: 교수 - 과제 채점
```javascript
// lecture-test-4b-professor-assignment-grade.js 로드
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

## 📝 파일 분할 정보 (v4.0)

과도하게 긴 테스트 파일들을 기능별로 분할하여 가독성과 유지보수성을 향상했습니다.

### 분할된 파일

| 원본 파일 | 라인 수 | 분할 후 | 상태 |
|-----------|---------|---------|------|
| `lecture-test-2-student-enrollment.js` | 538줄 | → `2a-student-enrollment.js` (328줄)<br>→ `2b-student-my-courses.js` (335줄) | ✅ 완료 |
| `lecture-test-4-professor-assignment.js` | 643줄 | → `4a-professor-assignment-create.js` (404줄)<br>→ `4b-professor-assignment-grade.js` (396줄) | ✅ 완료 |

### 분할 기준

- **Part A**: 생성 및 조회 기능
- **Part B**: 관리 및 수정 기능

각 파일은 독립적으로 실행 가능하며, Part A에서 저장된 `window.lastXxxIdx` 값을 Part B에서 활용할 수 있습니다.

---

**Happy Testing! 🚀**
