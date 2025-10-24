# 👨‍🎓 학생 테스트

학생 전용 수강 신청 기능 테스트 스크립트입니다.

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

## ⚠️ 과제 제출 안내

**과제는 오프라인으로 제출됩니다.**
- 학생이 오프라인으로 과제 제출
- 교수가 시스템에서 과제 생성 및 채점
- 자동으로 성적에 반영

과제 관련 테스트는 `03-professor/` 폴더를 참고하세요.

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

## ⚠️ 주의사항

- **학생 권한 필수**: userStudent = 2 (학생)
- **수강 신청 제한**: 이미 신청한 강의는 재신청 불가
- **수강 취소**: 강의 시작 전에만 가능
- **과제 제출**: 오프라인으로 진행 (시스템 외부)

## 📊 예상 결과

```javascript
✅ 수강 가능 강의 5개 조회
✅ 수강 신청 성공
✅ 내 수강 목록 1개
```

## 🔗 관련 문서

- [상위 README](../README.md)
- [테스트 가이드](../TEST_GUIDE.md)
- [교수 테스트](../03-professor/)
