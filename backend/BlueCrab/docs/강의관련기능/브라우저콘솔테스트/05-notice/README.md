# 📢 안내문 관리 브라우저 콘솔 테스트

이 폴더에는 안내문 관리 기능의 브라우저 콘솔 테스트 스크립트들이 포함되어 있습니다.

## 📁 파일 목록

### 1. `notice-test-1-view.js`
**안내문 조회 테스트 (공개)**

- **권한**: 인증 불필요 (누구나 조회 가능)
- **기능**: 현재 설정된 안내문 내용 조회
- **사용법**:
  1. 브라우저 콘솔에 코드 복사/붙여넣기
  2. `await testViewNotice()` 또는 `await viewNotice()` 실행

### 2. `notice-test-2-admin-save.js`
**안내문 저장 테스트 (관리자/교수)**

- **권한**: `ROLE_ADMIN` 또는 `ROLE_PROFESSOR` 필요
- **기능**: 안내문 내용 저장, 조회, 클리어
- **사용법**:
  1. 관리자 로그인 실행: `await adminLogin()`
  2. 브라우저 콘솔에 코드 복사/붙여넣기
  3. 원하는 함수 실행

## 🚀 사용법 상세

### 공통 준비사항
1. Blue Crab LMS 웹사이트 접속
2. F12 키로 개발자 도구 열기
3. Console 탭 선택
4. 아래 각 파일의 코드를 복사하여 붙여넣기

### 안내문 조회 테스트
```javascript
// 간단한 조회
await testViewNotice()

// 또는 짧은 이름으로
await viewNotice()
```

### 안내문 저장 테스트
```javascript
// 1. 기본 저장 (프롬프트로 메시지 입력)
await testSaveNotice()

// 2. 직접 메시지 전달
await testSaveNotice("안내문 내용입니다")

// 3. 저장 후 즉시 조회
await saveAndViewNotice()

// 4. 샘플 안내문으로 테스트
await testSampleNotice()

// 5. 안내문 클리어 (빈 메시지로 업데이트)
await clearNotice()

// 간편 함수들
await save("메시지")           // 저장
await saveAndView("메시지")    // 저장+조회
await testSample()             // 샘플 저장
await clear()                  // 클리어
```

## 📋 API 엔드포인트

| 엔드포인트 | 메서드 | 권한 | 설명 |
|-----------|--------|------|------|
| `/notice/course-apply/view` | POST | 없음 | 안내문 조회 |
| `/notice/course-apply/save` | POST | 관리자/교수 | 안내문 저장 |

## ⚠️ 주의사항

- **인증 필요**: 저장 기능은 반드시 관리자 로그인 후 사용
- **토큰 만료**: JWT 토큰이 만료되면 재로그인 필요
- **실제 데이터**: 테스트 시 실제 안내문이 변경되므로 주의
- **클리어 기능**: 실제 삭제가 아닌 빈 메시지로 업데이트됨

## 🔍 테스트 검증 포인트

### 조회 테스트 성공 기준
- ✅ HTTP 200 응답
- ✅ `success: true`
- ✅ 안내문 내용, 수정일시, 수정자 정보 반환

### 저장 테스트 성공 기준
- ✅ HTTP 200 응답
- ✅ `success: true`
- ✅ `noticeIdx`, 메시지, 수정 정보 반환
- ✅ 저장 후 조회 시 동일한 내용 확인

## 🐛 문제 해결

### "JWT 토큰이 없습니다" 오류
```
해결: 먼저 관리자 로그인 실행
await adminLogin()
```

### "401 Unauthorized" 오류
```
해결: 토큰 만료 확인 및 재로그인
await adminLogin()
```

### 네트워크 오류
```
해결: 인터넷 연결 및 서버 상태 확인
API_BASE_URL이 올바른지 확인
```

## 📝 관련 문서

- [API 컬렉션 문서](../../../../README.md)
- [관리자 로그인 테스트](../00-login/admin-login.js)
- [기능 통합 테스트](../07-integration-test/)