# 📢 안내문 API 테스트 ✅ (2025.01.05 완료)

수강신청 안내문 조회 및 저장 테스트

---

## 📋 테스트 파일

| 파일 | 설명 | 권한 | 상태 |
|------|------|------|------|
| `notice-test-1-view.js` | 안내문 조회 (공개) | 없음 | ✅ 테스트 완료 |
| `notice-test-2-admin-save.js` | 안내문 저장 (관리자/교수) | ADMIN/PROFESSOR | ✅ 테스트 완료 |

---

## 🔧 테스트 순서

### 1️⃣ 안내문 조회 (공개) - 인증 불필요
```javascript
// 브라우저 콘솔에 notice-test-1-view.js 내용 복사/붙여넣기

await testViewNotice();
// 또는
await viewNotice();
```

**결과 예시:**
```
✅ 조회 성공!

📄 안내문 내용:
2025학년도 1학기 수강신청 안내
...

⏰ 최종 수정: 2025-01-05T12:34:56
👤 수정자: prof.jellyfish
```

### 2️⃣ 안내문 저장 (관리자/교수) - 로그인 필요

```javascript
// Step 1: 교수 또는 관리자로 로그인 (별도 로그인 테스트 파일 사용)
await login();  // 프롬프트에서 이메일/비밀번호 입력

// Step 2: notice-test-2-admin-save.js 내용 복사/붙여넣기

// Step 3: 안내문 저장 (프롬프트로 입력)
await testSaveNotice();

// 또는 직접 메시지 전달
await testSaveNotice('새로운 수강신청 안내문 내용');

// 또는 샘플 메시지로 테스트
await testSampleNotice();
```

**저장 후 자동 조회:**
```javascript
await saveAndViewNotice();  // 프롬프트 입력 → 저장 → 2초 대기 → 조회
```

**안내문 삭제 (빈 메시지로 업데이트):**
```javascript
await deleteNotice();  // 확인 창이 뜸
```

---

## 🎯 간편 함수

더 짧은 함수명으로 빠르게 테스트:

```javascript
// 조회
await viewNotice();

// 저장
await saveNotice();           // 프롬프트 입력
await saveNotice("메시지");   // 직접 전달

// 저장 + 조회
await saveAndView();

// 샘플 테스트
await testSample();
```

---

## ⚠️ 주의사항

### 권한
- **조회**: 🔓 인증 불필요, 누구나 가능
- **저장**: 🔐 관리자(ADMIN) 또는 교수(PROFESSOR) 권한 필요
  - JWT 토큰 필요
  - 교수 계정: `userStudent = 1`

### 데이터 구조
- 안내문은 **단일 레코드**로 관리 (항상 최신 1개만)
- 저장 시마다 기존 안내문 업데이트 (Upsert 패턴)
- 삭제 기능 없음 (빈 메시지로 업데이트 방식)

### 토큰 공유
- 로그인 후 `window.authToken`에 JWT 저장
- 다른 테스트 파일에서 자동으로 사용 가능

---

## 📊 API 엔드포인트

| API | 메서드 | 엔드포인트 | 인증 | 상태 |
|-----|--------|------------|------|------|
| 조회 | POST | `/notice/course-apply/view` | ❌ 불필요 | ✅ 작동 |
| 저장 | POST | `/notice/course-apply/save` | ✅ ADMIN/PROFESSOR | ✅ 작동 |

**Base URL:** `https://bluecrab.chickenkiller.com/BlueCrab-1.0.0`

---

## 🐛 트러블슈팅

### 404 Not Found
- ❌ 잘못된 URL: `/api/notice/course-apply/...`
- ✅ 올바른 URL: `/notice/course-apply/...`
- `/api` 접두사 없이 사용

### 401 Unauthorized
```javascript
// 해결 방법
await login();  // 다시 로그인
checkStatus();  // 토큰 상태 확인
```

### 토큰이 없다고 나올 때
```javascript
// 전역 변수 확인
console.log(window.authToken);

// 수동으로 설정 (필요시)
window.authToken = 'your-jwt-token';
```

---

## 📝 테스트 결과

- ✅ **조회 API**: 공개 접근 정상 작동
- ✅ **저장 API**: 교수 권한으로 정상 저장
- ✅ **프롬프트 입력**: 사용자 입력 정상 작동
- ✅ **토큰 검증**: JWT 인증 정상 작동
- ✅ **단일 레코드**: Upsert 패턴 정상 작동

**최종 테스트 일시**: 2025-01-05 (완료)
