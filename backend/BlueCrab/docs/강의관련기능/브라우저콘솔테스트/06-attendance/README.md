# 출석 요청/승인 API 브라우저 콘솔 테스트

이 폴더는 출석 요청/승인 기능을 브라우저 콘솔에서 테스트하기 위한 JavaScript 스크립트를 포함합니다.

## 📁 파일 구성

| 파일명 | 설명 | 사용자 |
|--------|------|--------|
| `01-attendance-request.js` | 학생 출석 요청 API 테스트 | 학생 |
| `02-attendance-approve.js` | 교수 출석 승인 API 테스트 | 교수 |
| `03-student-view.js` | 학생 출석 조회 API 테스트 | 학생 |
| `04-professor-view.js` | 교수 출석 조회 API 테스트 | 교수 |

---

## 🚀 사용 방법

### 1. 사전 준비

1. 브라우저에서 프론트엔드 애플리케이션 열기
   ```
   https://bluecrab-front-test.chickenkiller.com
   ```

2. F12 키를 눌러 개발자 도구 열기

3. **Console** 탭으로 이동

4. 해당 권한의 계정으로 로그인 (학생 또는 교수)

### 2. 테스트 실행

각 스크립트 파일을 열어서 내용을 복사한 후, 브라우저 콘솔에 붙여넣기 후 Enter

```javascript
// 예시: 콘솔에서 실행
// 1. 스크립트 전체를 복사
// 2. 콘솔에 붙여넣기
// 3. Enter 누르면 입력 프롬프트가 자동으로 나타남
// 4. 각 값을 입력하여 테스트 진행
```

**입력 방식:**
- 모든 입력은 `prompt()` 창을 통해 대화형으로 입력
- 기본값이 제공되므로 Enter만 눌러도 테스트 가능
- 실제 데이터로 테스트할 때는 값을 수정하여 입력
- **studentIdx는 JWT에서 자동 추출** (수동 입력 불필요)
- **professorIdx는 JWT에서 자동 추출** (수동 입력 불필요)

**중요:**
- 모든 API는 **POST** 방식을 사용합니다
- JWT 토큰은 **window.authToken** (우선) > localStorage > sessionStorage 순서로 확인됩니다
- lecSerial은 내부적으로 LEC_IDX로 자동 매핑됨
- **교수 검증**: LEC_PROF 필드가 USER_IDX(문자열)로 저장되어 있으므로 CAST 비교 사용
- **응답 구조**: 교수용 API는 `attendanceData` 중첩, 학생용 API는 최상위
- **필드명**: `approvedDate`, `requestDate` 사용 (recordedAt, requestedAt 아님)

---

## 📝 테스트 시나리오

### 시나리오 1: 학생 출석 요청
**파일:** `01-attendance-request.js`

**순서:**

1. 학생 계정으로 로그인
2. 스크립트 실행
3. 프롬프트 창에서 값 입력:
   - 강의 코드 (예: `CS101`)
   - 회차 번호 (예: `1`)
   - 요청 사유 (선택사항, 취소 가능)
   - ~~학생 USER_IDX~~ (JWT에서 자동 추출)
4. 출석 요청 결과 확인

**예상 응답:**
```json
{
  "success": true,
  "message": "출석 요청이 완료되었습니다.",
  "data": {
    "summary": {
      "attended": 30,
      "late": 5,
      "absent": 5,
      "attendanceRate": 75.0
    },
    "sessions": [...],
    "pendingRequests": [
      {
        "sessionNumber": 2,
        "requestDate": "2025-10-23 10:00:00",
        "expiresAt": "2025-10-30 00:00:00",
        "requestReason": "교통체증",
        "tempApproved": true
      }
    ]
  }
}
```

---

### 시나리오 2: 교수 출석 승인

**파일:** `02-attendance-approve.js`

**순서:**

1. 교수 계정으로 로그인
2. 스크립트 실행
3. 프롬프트 창에서 값 입력:
   - 강의 코드 (예: `CS101`)
   - 회차 번호 (예: `1`)
   - 처리할 학생 수 (예: `3`)
   - 각 학생의 USER_IDX와 출석 상태 ("출", "지", "결") 입력
   - ~~교수 USER_IDX~~ (JWT에서 자동 추출)
4. 승인 결과 확인

**예상 응답:**

```json
{
  "success": true,
  "message": "출석 승인이 완료되었습니다. (3/3)",
  "data": null
}
```

---

## 🔍 디버깅 팁

### 토큰 확인
```javascript
// localStorage에서 토큰 확인
console.log('Access Token:', localStorage.getItem('accessToken'));

// sessionStorage에서 토큰 확인
console.log('Access Token:', sessionStorage.getItem('accessToken'));
```

### CORS 오류 발생 시
- 백엔드 `web.xml`에서 CORS 설정 확인
- `/notice/*` 경로가 CORS 필터에 포함되어 있는지 확인

### 403 Forbidden 오류
- 로그인된 계정의 권한 확인
- JWT 토큰 유효성 확인
- SecurityConfig에서 엔드포인트 권한 설정 확인

### 404 Not Found 오류
- API 엔드포인트 경로 확인
- 백엔드 서버가 실행 중인지 확인
- Controller가 올바르게 매핑되어 있는지 확인

---

## 📌 주의사항

- 테스트 전에 반드시 로그인 상태 확인
- JWT 토큰이 localStorage 또는 sessionStorage에 저장되어 있어야 함
- 실제 데이터를 사용하므로 테스트 후 데이터 정리 필요
- 테스트용 강의 ID와 학생 ID를 미리 준비

---

## 🔗 관련 문서

- [출석 요청/승인 플로우 문서](../../출결관리/출석요청승인_플로우.md)
- [출석 요청/승인 다이어그램](../../출결관리/출석요청승인_플로우.drawio)
- [작업 순서 문서](../../출결관리/작업%20수순/01_작업_순서_전체.md)

---

## 📞 문의

테스트 중 문제가 발생하면 백엔드 개발팀에 문의하세요.
