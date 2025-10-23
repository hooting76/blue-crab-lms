# 👨‍🏫 교수 테스트# 👨‍🏫 교수 테스트



교수 전용 강의 관리, 수강생 관리, 과제 관리 기능 테스트 스크립트입니다.교수 전용 강의 관리, 수강생 관리, 과제 관리 기능 테스트 스크립트입니다.



## 📁 파일 목록## 📁 파일 목록



### `lecture-test-4a-professor-assignment-create.js`### ⭐ `professor-all-in-one.js` (통합 버전)

**과제 생성 및 목록 조회 + 수강생 목록****모든 교수 기능을 하나의 파일로 통합**



**포함 기능**:**포함 기능**:

- 📚 담당 강의 목록 조회 (JWT에서 USER_IDX 자동 추출)- 📚 **강의 관리**: 내 강의 목록 조회

- 👨‍🎓 수강생 목록 조회 (lecSerial 기반)- 👨‍🎓 **수강생 관리**: 수강생 목록, 강의 통계

- 📄 과제 생성- 📄 **과제 관리**: 과제 생성, 목록, 제출 현황, 채점

- 📄 과제 목록 조회

**주요 함수**:

**주요 함수**:```javascript

```javascript// 강의 관리

await getMyLectures()       // 내 강의 목록 (자동으로 lecSerial 저장)await getMyLectures()       // 내 강의 목록

await getStudents()         // 수강생 목록 조회

await createAssignment()    // 과제 생성 (자동으로 assignmentIdx 저장)// 수강생 관리

await getAssignments()      // 과제 목록 조회await getStudents()         // 수강생 목록

```await getLectureStats()     // 강의 통계



---// 과제 관리

await createAssignment()    // 과제 생성

### `lecture-test-4b-professor-assignment-grade.js`await getAssignments()      // 과제 목록

**과제 채점 및 관리 + 수강생 목록**await getSubmissions()      // 제출 현황

await gradeAssignment()     // 과제 채점

**포함 기능**:```

- 👨‍🎓 수강생 목록 조회 (lecSerial 기반)

- 📊 과제 제출 현황 조회##  사용 방법

- 💯 과제 채점 (제출 방법 + 점수)

- ✏️ 과제 수정### 1. 교수로 로그인

- 🗑️ 과제 삭제```javascript

// 먼저 로그인 스크립트 실행

**주요 함수**:await login()  // 교수 계정 (userStudent: 1)

```javascript```

await getStudents()         // 수강생 목록 조회

await getSubmissions()      // 과제 제출 현황 (저장된 assignmentIdx 사용)### 2. 통합 스크립트 로드

await gradeAssignment()     // 과제 채점```javascript

await updateAssignment()    // 과제 수정// professor-all-in-one.js 파일을 브라우저 콘솔에 붙여넣기

await deleteAssignment()    // 과제 삭제// 스크립트 로드 확인 메시지 출력됨

``````



## 🚀 사용 방법### 3. 기본 워크플로우



### 1. 교수로 로그인**강의 조회 → 수강생 확인**:

```javascript```javascript

// 먼저 로그인 스크립트 실행await getMyLectures()    // 내 강의 목록 (자동으로 lecSerial 저장)

await login()  // 교수 계정 (userStudent: 1)await getStudents()      // 수강생 목록 (저장된 lecSerial 사용)

```await getLectureStats()  // 강의 통계

```

### 2. Part A: 과제 생성 워크플로우

```javascript**과제 관리**:

// lecture-test-4a-professor-assignment-create.js 실행```javascript

await getMyLectures()       // 내 강의 목록 (자동으로 lecSerial 저장)await createAssignment()  // 과제 생성 (자동으로 assignmentIdx 저장)

await getStudents()         // 수강생 목록 확인await getAssignments()    // 과제 목록 확인

await createAssignment()    // 과제 생성 (자동으로 assignmentIdx 저장)await getSubmissions()    // 제출 현황 조회 (저장된 assignmentIdx 사용)

await getAssignments()      // 과제 목록 확인await gradeAssignment()   // 과제 채점

``````



### 3. Part B: 과제 채점 워크플로우## ⚠️ 주의사항

```javascript

// lecture-test-4b-professor-assignment-grade.js 실행- **교수 권한 필수**: `userStudent = 1` (교수 계정)

await getStudents()         // 수강생 목록 확인- **자동 저장**: 강의 코드와 과제 IDX가 자동으로 저장되어 다음 함수에서 재사용됨

await getSubmissions()      // 제출 현황 조회 (Part A에서 저장된 assignmentIdx 사용)- **간편한 사용**: 저장된 값이 있으면 프롬프트에서 Enter만 누르면 됨

await gradeAssignment()     // 과제 채점

```## 💡 자동 저장 값



## ⚠️ 주의사항- `window.lastLectureSerial` - 마지막 조회한 강의 코드

- `window.lastAssignmentIdx` - 마지막 생성/조회한 과제 IDX

- **교수 권한 필수**: `userStudent = 1` (교수 계정)

- **자동 저장**: ## 📊 예상 결과

  - `window.lastLectureSerial` - Part A에서 강의 코드 자동 저장

  - `window.lastAssignmentIdx` - Part A에서 과제 IDX 자동 저장### 수강생 목록 조회 (ETH201 테스트 완료 ✅)

  - Part B에서 저장된 값 자동 사용```

- **파일 순서**: Part A → Part B 순서로 사용 권장✅ 조회 성공!

📊 총 1명 수강생

## 💡 제출 방법 옵션 (gradeAssignment)📋 수강생 목록:



- `email` - 이메일 제출1. 테스터 (0)

- `print` - 출력물 제출   IDX: 6

- `hands` - 직접 제출   강의: 현대 윤리학 (ETH201)

- `absent` - 미제출   담당교수: null (lecProf: 25)

   등록일: 2025-10-17 22:55:20

## 📊 예상 결과   상태: ENROLLED

```

### 수강생 목록 조회 (ETH201 테스트 완료 ✅)

```**알려진 이슈**:

✅ 조회 성공!- ⚠️ `lecProfName` returns `null` (LEC_PROF와 USER_CODE 매칭 필요)

📊 총 1명 수강생- ⚠️ `studentCode` shows `"0"` (USER_TBL.USER_CODE에 실제 학번 없음)

📋 수강생 목록:

### 과제 관리

1. 테스터 (0)```

   IDX: 6✅ 내 강의 조회

   강의: 현대 윤리학 (ETH201)✅ 과제 생성 성공

   담당교수: null (lecProf: 25)✅ 제출 현황 조회

   등록일: 2025-10-17 22:55:20✅ 과제 채점 완료

   상태: ENROLLED```

```

## 🎯 성적 관리 연동

**알려진 이슈**:

- ⚠️ `lecProfName` returns `null` (LEC_PROF와 USER_CODE 매칭 필요)과제 채점 시 자동으로 성적 관리 시스템과 연동됩니다:

- ⚠️ `studentCode` shows `"0"` (USER_TBL.USER_CODE에 실제 학번 없음)

```javascript

### 과제 관리// 과제 채점

```await gradeAssignment()

✅ 내 강의 조회

✅ 수강생 목록 조회// 🔥 자동 이벤트 발생

✅ 과제 생성 성공// → GradeUpdateEvent(lecIdx, studentIdx, "ASSIGNMENT")

✅ 제출 현황 조회// → 학생 성적 자동 재계산

✅ 과제 채점 완료// → ENROLLMENT_DATA JSON 업데이트

``````



## 🎯 성적 관리 연동자세한 내용은 [성적 관리 테스트](../04-grade/) 참조



과제 채점 시 자동으로 성적 관리 시스템과 연동됩니다:## 🔗 관련 문서



```javascript- [상위 README](../README.md)

// 과제 채점- [학생 테스트](../02-student/)

await gradeAssignment()- [성적 관리](../04-grade/)


// 🔥 자동 이벤트 발생
// → GradeUpdateEvent(lecIdx, studentIdx, "ASSIGNMENT")
// → 학생 성적 자동 재계산
// → ENROLLMENT_DATA JSON 업데이트
```

자세한 내용은 [성적 관리 테스트](../04-grade/) 참조

## 🔗 관련 문서

- [상위 README](../README.md)
- [학생 테스트](../02-student/)
- [성적 관리](../04-grade/)
- [TEST-GUIDE.md](./TEST-GUIDE.md) - 상세 테스트 가이드
- [TEST-RESULT.md](./TEST-RESULT.md) - 실제 테스트 결과
