# 👨‍🏫 교수 테스트

교수 전용 과제 관리 및 수강생 관리 기능 테스트 스크립트입니다.

## 📁 파일 목록

### `lecture-test-4a-professor-assignment-create.js`
**과제 생성 및 관리**

- ✅ 내 강의 목록 조회
- ✅ 과제 생성 (POST /api/assignments)
- ✅ 과제 목록 조회 (POST /api/assignments/list)
- ✅ 과제 수정 (PUT /api/assignments/{idx})
- ✅ 과제 삭제 (DELETE /api/assignments/{idx})

### `lecture-test-4b-professor-assignment-grade.js`
**과제 채점**

- ✅ 제출된 과제 목록 조회 (POST /api/assignments/submissions)
- ✅ 과제 채점 (PUT /api/assignments/{idx}/grade)
- ✅ 채점 결과 확인

### `lecture-test-5-professor-students.js`
**수강생 관리**

- ✅ 수강생 목록 조회 (POST /api/enrollments/list)
- ✅ 수강생 상세 정보
- ✅ 수강생 성적 관리 연동

## 🚀 사용 방법

### 1. 교수로 로그인
```javascript
// docs/일반유저 로그인+게시판/test-1-login.js
await login()
// 교수 계정 사용 (userStudent: 1)
```

### 2. 과제 생성 흐름
```javascript
// 1단계: 내 강의 조회
await getMyLectures()

// 2단계: 과제 생성
await createAssignment(lecIdx, {
    title: "중간고사",
    description: "중간고사 과제입니다",
    dueDate: "2025-11-30"
})

// 3단계: 확인
await getAssignmentList(lecIdx)
```

### 3. 과제 채점 흐름
```javascript
// 1단계: 제출된 과제 조회
await getSubmissions(assignmentIdx)

// 2단계: 채점
await gradeAssignment(assignmentIdx, studentIdx, {
    score: 95,
    feedback: "잘 했습니다"
})

// 3단계: 확인
await getGradedSubmissions(assignmentIdx)
```

### 4. 수강생 관리
```javascript
// 수강생 목록 조회
await getStudentList(lecIdx)

// 수강생 상세 정보
await getStudentDetail(lecIdx, studentIdx)
```

## ⚠️ 주의사항

- **교수 권한 필수**: userStudent = 1 (교수)
- **강의 권한**: 본인이 담당하는 강의만 접근 가능
- **과제 채점**: 제출된 과제만 채점 가능
- **성적 연동**: 과제 채점 시 자동으로 성적 업데이트 (이벤트 기반)

## 📊 예상 결과

```javascript
✅ 내 강의 3개 조회
✅ 과제 생성 성공
✅ 제출된 과제 10개 조회
✅ 과제 채점 완료
✅ 수강생 30명 조회
```

## 🎯 성적 관리 연동

과제 채점 시 자동으로 성적 관리 시스템과 연동됩니다:

```javascript
// 과제 채점
await gradeAssignment(assignmentIdx, studentIdx, { score: 95 })

// 🔥 자동 이벤트 발생
// → GradeUpdateEvent(lecIdx, studentIdx, "ASSIGNMENT")
// → 학생 성적 자동 재계산
// → ENROLLMENT_DATA JSON 업데이트
```

자세한 내용은 [성적 관리 테스트](../04-grade/) 참조

## 🔗 관련 문서

- [상위 README](../README.md)
- [테스트 가이드](../TEST_GUIDE.md)
- [학생 테스트](../02-student/)
- [성적 관리](../04-grade/)
