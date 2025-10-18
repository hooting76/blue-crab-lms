# 👨‍💼 관리자 테스트

관리자 전용 강의 관리 및 통계 기능 테스트 스크립트입니다.

## 📁 파일 목록

### `lecture-test-1-admin-create.js`
**강의 CRUD 및 통계**

- ✅ 강의 생성 (POST /api/lectures/create)
- ✅ 강의 목록 조회 (POST /api/lectures)
- ✅ 강의 상세 조회 (POST /api/lectures/detail)
- ✅ 강의 수정 (POST /api/lectures/update)
- ✅ 강의 삭제 (POST /api/lectures/delete)
- ✅ 강의 통계 조회 (POST /api/lectures/stats)

### `lecture-test-6-admin-statistics.js`
**전체 시스템 통계**

- ✅ 전체 강의 통계
- ✅ 수강 신청 통계
- ✅ 과제 제출 통계
- ✅ 시스템 사용 현황

## 🚀 사용 방법

### 1. 관리자로 로그인
```javascript
// docs/일반유저 로그인+게시판/test-1-login.js
await login()
// 관리자 계정 사용
```

### 2. 스크립트 로드
```javascript
// 강의 CRUD 테스트
// (F12 콘솔에서 lecture-test-1-admin-create.js 복사 & 붙여넣기)
await createTestLecture()
await getLectureList()
```

### 3. 전체 통계 확인
```javascript
// 시스템 통계 테스트
// (lecture-test-6-admin-statistics.js 실행)
await getSystemStatistics()
```

## ⚠️ 주의사항

- **관리자 권한 필수**: 일반 사용자 계정으로는 접근 불가
- **테스트 데이터**: 테스트 후 생성된 강의는 수동 삭제 필요
- **API 순서**: 강의 생성 → 조회 → 수정 → 삭제 순서 권장

## 📊 예상 결과

```javascript
✅ 강의 생성 성공
✅ 강의 목록 조회 성공 (10개 강의)
✅ 강의 상세 조회 성공
✅ 강의 수정 성공
✅ 강의 삭제 성공
✅ 시스템 통계 조회 성공
```

## 🔗 관련 문서

- [상위 README](../README.md)
- [테스트 가이드](../TEST_GUIDE.md)
- [성적 관리 테스트](../04-grade/)
