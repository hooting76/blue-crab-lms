# 📢 안내문 API 테스트

수강신청 안내문 조회 및 저장 테스트

---

## 📋 테스트 파일

| 파일 | 설명 | 권한 |
|------|------|------|
| `notice-test-1-view.js` | 안내문 조회 (공개) | 없음 |
| `notice-test-2-admin-save.js` | 안내문 저장 (관리자) | ADMIN |

---

## 🔧 테스트 순서

### 1. 안내문 조회 (공개)
```javascript
// 브라우저 콘솔에 복사/붙여넣기
// notice-test-1-view.js 내용 실행

await testViewNotice();
```

### 2. 안내문 저장 (관리자)
```javascript
// 1) 관리자 로그인 필요
await adminLogin();  // admin-login.js 사용

// 2) 안내문 저장
await testSaveNotice('새로운 수강신청 안내문');
```

---

## ⚠️ 주의사항

- **조회**: 인증 불필요, 누구나 가능
- **저장**: 관리자 또는 교수 권한 필요 (JWT 토큰)
- 안내문은 **단일 레코드**로 관리 (항상 최신 1개만)

---

## 📊 API 엔드포인트

| API | 메서드 | 엔드포인트 | 인증 |
|-----|--------|------------|------|
| 조회 | POST | `/notice/course-apply/view` | ❌ 불필요 |
| 저장 | POST | `/notice/course-apply/save` | ✅ ADMIN/PROFESSOR |
