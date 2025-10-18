# 👨‍🎓 학생 테스트

학생 전용 수강 신청 및 과제 제출 기능 테스트 스크립트입니다.

## 📁 파일 목록

### `lecture-test-2a-student-enrollment.js`
**수강 신청**

- ✅ 수강 가능 강의 조회 (POST /api/lectures/eligible)
- ✅ 수강 신청 (POST /api/enrollments/enroll)
- ✅ 수강 신청 확인

### `lecture-test-2b-student-my-courses.js`
**내 수강 목록**

- ✅ 내 수강 목록 조회 (POST /api/enrollments/list)
- ✅ 수강 상세 조회 (POST /api/enrollments/detail)
- ✅ 수강 취소 (DELETE /api/enrollments/{idx})

### `lecture-test-3-student-assignment.js`
**과제 관리**

- ✅ 과제 목록 조회 (POST /api/assignments/list)
- ✅ 과제 상세 조회 (POST /api/assignments/detail)
- ✅ 과제 제출 (POST /api/assignments/{idx}/submit)
- ✅ 내 제출 내역 조회

## 🚀 사용 방법

### 1. 학생으로 로그인
```javascript
// docs/일반유저 로그인+게시판/test-1-login.js
await login()
// 학생 계정 사용 (userStudent: 2)
```

### 2. 수강 신청 흐름
```javascript
// 1단계: 수강 가능 강의 조회
await getEligibleLectures()

// 2단계: 수강 신청
await enrollLecture(lecIdx)

// 3단계: 확인
await getMyEnrollments()
```

### 3. 과제 제출 흐름
```javascript
// 1단계: 과제 목록 조회
await getAssignmentList(lecIdx)

// 2단계: 과제 제출
await submitAssignment(assignmentIdx, "제출 내용")

// 3단계: 제출 확인
await getMySubmissions(lecIdx)
```

## ⚠️ 주의사항

- **학생 권한 필수**: userStudent = 2 (학생)
- **수강 신청 제한**: 이미 신청한 강의는 재신청 불가
- **과제 제출**: 마감일 이전에만 제출 가능
- **수강 취소**: 강의 시작 전에만 가능

## 📊 예상 결과

```javascript
✅ 수강 가능 강의 5개 조회
✅ 수강 신청 성공
✅ 내 수강 목록 1개
✅ 과제 목록 3개 조회
✅ 과제 제출 성공
```

## 🔗 관련 문서

- [상위 README](../README.md)
- [테스트 가이드](../TEST_GUIDE.md)
- [교수 테스트](../03-professor/)
