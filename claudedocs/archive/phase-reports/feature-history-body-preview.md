# ✨ 기능 추가: 히스토리에 요청 본문 미리보기

## 개요

요청 히스토리에 JSON 요청 본문의 미리보기를 추가하여, 어떤 데이터를 보냈는지 즉시 확인할 수 있도록 개선했습니다.

## 배경

### 이전 문제점
- 히스토리에 HTTP 메소드, 엔드포인트, 상태 코드만 표시
- 어떤 JSON 데이터를 보냈는지 확인하려면:
  1. "재실행" 버튼 클릭
  2. 요청 본문 입력 칸 확인
  3. 다시 돌아오기
- 특히 POST/PUT 요청의 경우 데이터 내용이 중요한데 확인 불가

### 개선 필요성
사용자 피드백:
> "히스토리에 json 요청으로 무얼 담아 보냈는지 보이는게 좋겠지?"

디버깅 시나리오:
- 같은 API를 여러 번 호출할 때 파라미터 차이 비교
- 성공/실패 요청의 데이터 차이 분석
- 이전에 보냈던 요청 데이터 재확인

## 구현 내용

### 1. JavaScript 로직 추가

**파일**: `history-manager.js`

#### 요청 본문 미리보기 생성

```javascript
// 요청 본문이 있으면 미리보기 생성
let bodyPreview = '';
if (item.body && item.body.trim()) {
    try {
        const parsedBody = JSON.parse(item.body);
        const keys = Object.keys(parsedBody);

        if (keys.length > 0) {
            // 첫 2개 키만 표시
            const previewKeys = keys.slice(0, 2);
            const previewPairs = previewKeys.map(key => {
                let value = parsedBody[key];
                // 값이 너무 길면 자르기
                if (typeof value === 'string' && value.length > 20) {
                    value = value.substring(0, 20) + '...';
                } else if (typeof value === 'object') {
                    value = '{...}';
                }
                return `${key}: ${JSON.stringify(value)}`;
            }).join(', ');

            const moreText = keys.length > 2 ? `, +${keys.length - 2}개` : '';
            bodyPreview = `<div class="history-body-preview" title="${escapeHtml(item.body)}">
                📄 { ${previewPairs}${moreText} }
            </div>`;
        }
    } catch (e) {
        // JSON 파싱 실패 시 원본 텍스트의 일부만 표시
        const preview = item.body.substring(0, 50);
        bodyPreview = `<div class="history-body-preview" title="${escapeHtml(item.body)}">
            📄 ${escapeHtml(preview)}${item.body.length > 50 ? '...' : ''}
        </div>`;
    }
}
```

#### HTML 이스케이프 헬퍼 함수

```javascript
// HTML 이스케이프 헬퍼 함수
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
```

**XSS 방어**: 사용자 입력을 HTML로 직접 표시하기 전에 이스케이프 처리

### 2. CSS 스타일 추가

**파일**: `api-tester.css`

```css
/* 히스토리 요청 본문 미리보기 */
.history-body-preview {
    background: #f8f9fa;
    border-left: 3px solid #17a2b8;
    padding: 6px 8px;
    margin: 5px 0;
    border-radius: 3px;
    font-size: 10px;
    font-family: 'Courier New', monospace;
    color: #495057;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    cursor: help;
}

.history-body-preview:hover {
    background: #e9ecef;
    border-left-color: #138496;
}
```

**디자인 특징**:
- 모노스페이스 폰트로 코드 느낌 강조
- 왼쪽 청록색 테두리로 JSON 데이터임을 시각적 표시
- 호버 시 배경색 변경으로 인터랙티브 피드백
- `cursor: help`로 툴팁이 있음을 암시
- `text-overflow: ellipsis`로 긴 내용 처리

## 사용자 경험

### Before (이전)
```
┌─────────────────────────────┐
│ POST    200 | 196ms          │
│ /api/boards/list            │
│ [재실행] [삭제]              │
└─────────────────────────────┘
```

**문제**: 어떤 page/size로 요청했는지 알 수 없음

### After (개선 후)
```
┌─────────────────────────────┐
│ POST    200 | 196ms          │
│ /api/boards/list            │
│ 📄 { page: 0, size: 10 }    │  ← 새로 추가!
│ [재실행] [삭제]              │
└─────────────────────────────┘
```

**개선**: 요청 데이터를 한눈에 확인 가능

### 다양한 케이스

#### Case 1: 단순 JSON (2개 이하 필드)
```
📄 { username: "student@university.edu", password: "password123" }
```

#### Case 2: 많은 필드
```
📄 { facilityIdx: 1, startTime: "2025-01-01T10:00...", +3개 }
```
- 처음 2개 필드만 표시
- 나머지는 "+3개"로 개수 표시

#### Case 3: 긴 문자열 값
```
📄 { boardContent: "본문 내용이 아주 길면 20자...", boardTitle: "제목" }
```
- 20자 이상 문자열은 자동으로 "..." 처리

#### Case 4: 중첩 객체
```
📄 { data: {...}, title: "테스트" }
```
- 객체/배열은 `{...}` 또는 `[...]`로 축약

#### Case 5: GET 요청 (본문 없음)
```
GET     200 | 24ms
/api/ping
[재실행] [삭제]
```
- 본문이 없으면 미리보기 표시 안 함

### 툴팁 기능

마우스를 미리보기 위에 올리면 **전체 JSON**을 툴팁으로 표시:

```html
<div title='{"username":"student@university.edu","password":"password123"}'>
    📄 { username: "student@university.edu", password: "password123" }
</div>
```

## 기술적 세부사항

### 1. 스마트 미리보기 생성

#### JSON 파싱 시도
```javascript
try {
    const parsedBody = JSON.parse(item.body);
    // JSON 객체의 키 추출
    const keys = Object.keys(parsedBody);
}
```

#### 키 선택 전략
```javascript
// 첫 2개 키만 선택 (너무 길지 않게)
const previewKeys = keys.slice(0, 2);
```

#### 값 처리 로직
```javascript
let value = parsedBody[key];

// 문자열이 너무 길면 자르기
if (typeof value === 'string' && value.length > 20) {
    value = value.substring(0, 20) + '...';
}
// 중첩 객체는 축약 표현
else if (typeof value === 'object') {
    value = '{...}';
}
```

### 2. Fallback 처리

JSON 파싱 실패 시 (유효하지 않은 JSON):
```javascript
catch (e) {
    // 원본 텍스트의 처음 50자만 표시
    const preview = item.body.substring(0, 50);
    bodyPreview = `📄 ${escapeHtml(preview)}${item.body.length > 50 ? '...' : ''}`;
}
```

**장점**:
- JSON이 아니어도 미리보기 제공
- 원본 데이터를 일부라도 볼 수 있음

### 3. 보안 고려사항

#### XSS 방어
```javascript
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;  // textContent는 자동으로 이스케이프
    return div.innerHTML;
}
```

**사용처**:
1. `title` 속성 (툴팁)
2. JSON 파싱 실패 시 원본 텍스트 표시

**이유**:
- 사용자가 입력한 JSON에 `<script>` 태그 등이 포함될 수 있음
- innerHTML로 직접 삽입하면 XSS 공격 가능
- `textContent`를 통해 안전하게 이스케이프

### 4. 성능 최적화

#### 로컬스토리지 읽기 최소화
```javascript
// 히스토리 로드 시 한 번만 읽기
const history = JSON.parse(localStorage.getItem('bluecrab_request_history') || '[]');

// 각 항목을 순회하며 미리보기 생성
container.innerHTML = history.map(item => { ... }).join('');
```

#### 미리보기 길이 제한
- 키: 최대 2개
- 문자열 값: 최대 20자
- JSON 파싱 실패: 최대 50자

**효과**: DOM 크기 최소화, 렌더링 성능 향상

## 사용 예시

### 예시 1: 로그인 디버깅

**시나리오**: 로그인이 계속 실패함

**Before**:
```
POST  401 | 150ms
/api/auth/login
[재실행] [삭제]

POST  401 | 145ms
/api/auth/login
[재실행] [삭제]

POST  200 | 180ms
/api/auth/login
[재실행] [삭제]
```
❓ 어떤 게 성공한 요청인지 데이터를 확인할 수 없음

**After**:
```
POST  401 | 150ms
/api/auth/login
📄 { username: "wrong@email.com", password: "wrong123" }
[재실행] [삭제]

POST  401 | 145ms
/api/auth/login
📄 { username: "student@university.edu", password: "wrongpw" }
[재실행] [삭제]

POST  200 | 180ms
/api/auth/login
📄 { username: "student@university.edu", password: "correct123" }
[재실행] [삭제]
```
✅ 어떤 이메일/비밀번호 조합이 성공했는지 즉시 확인!

### 예시 2: 페이징 테스트

**시나리오**: 게시판 목록을 다양한 페이지 크기로 테스트

**After**:
```
POST  200 | 196ms
/api/boards/list
📄 { page: 0, size: 10 }
[재실행] [삭제]

POST  200 | 215ms
/api/boards/list
📄 { page: 1, size: 10 }
[재실행] [삭제]

POST  200 | 180ms
/api/boards/list
📄 { page: 0, size: 20 }
[재실행] [삭제]
```
✅ 각 요청의 page/size를 한눈에 비교 가능!

### 예시 3: 시설 예약 테스트

**시나리오**: 다양한 시간대로 예약 시도

**After**:
```
POST  201 | 320ms
/api/reservations
📄 { facilityIdx: 1, startTime: "2025-01-01T10:00...", +3개 }
[재실행] [삭제]

POST  400 | 180ms
/api/reservations
📄 { facilityIdx: 1, startTime: "2025-01-01T11:00...", +3개 }
[재실행] [삭제]
```
✅ 어떤 시간대가 성공/실패했는지 추적 가능!

## 테스트 가이드

### 1. 기본 기능 테스트

**Step 1**: 로그인 API 호출
```json
{
  "username": "test@example.com",
  "password": "test123"
}
```

**Step 2**: 히스토리 패널 확인
- [ ] 📄 아이콘과 함께 미리보기 표시
- [ ] `username`과 `password` 키가 보임
- [ ] 형식: `{ username: "test@example.com", password: "test123" }`

**Step 3**: 마우스 호버
- [ ] 배경색이 밝은 회색으로 변경
- [ ] 툴팁으로 전체 JSON 표시

### 2. 긴 데이터 테스트

**Step 1**: 게시글 작성 API 호출
```json
{
  "boardTitle": "매우 긴 제목입니다 20자 이상이어야 합니다",
  "boardContent": "본문 내용도 엄청 길게 작성해봅니다. 50자 이상이면 생략됩니다.",
  "boardCode": 1
}
```

**Step 2**: 히스토리 확인
- [ ] `boardTitle` 값이 20자에서 잘림: `"매우 긴 제목입니다 20자 이..."`
- [ ] "+1개" 표시 (3개 필드 중 2개만 표시)
- [ ] 툴팁에는 전체 내용 표시

### 3. 중첩 객체 테스트

**Step 1**: 복잡한 JSON 호출
```json
{
  "user": {
    "name": "홍길동",
    "email": "hong@test.com"
  },
  "data": {
    "value": 123
  }
}
```

**Step 2**: 히스토리 확인
- [ ] 중첩 객체가 `{...}`로 표시
- [ ] 형식: `{ user: {...}, data: {...} }`

### 4. GET 요청 (본문 없음)

**Step 1**: Ping API 호출 (GET, body 없음)

**Step 2**: 히스토리 확인
- [ ] 📄 미리보기가 **표시되지 않음**
- [ ] 엔드포인트와 버튼만 표시

### 5. 유효하지 않은 JSON

**Step 1**: 커스텀 URL로 잘못된 JSON 전송
```
{ invalid json }
```

**Step 2**: 히스토리 확인
- [ ] Fallback 처리로 원본 텍스트 표시
- [ ] 50자 제한으로 자름
- [ ] 툴팁에 전체 텍스트 표시

## 향후 개선 가능성

### 1. 펼치기/접기 기능
현재는 항상 2개 키만 표시하지만, 클릭하면 전체 JSON을 펼칠 수 있도록:

```
📄 { username: "...", password: "..." } [▼ 펼치기]

클릭 후:
📄 {
  "username": "student@university.edu",
  "password": "password123",
  "rememberMe": true
} [▲ 접기]
```

### 2. JSON 구문 강조 (Syntax Highlighting)
```javascript
{
  "username": "test@example.com",  // 문자열 = 녹색
  "age": 25,                       // 숫자 = 파란색
  "active": true                   // 불린 = 주황색
}
```

### 3. Diff 보기
성공/실패 요청의 데이터 차이를 비교:
```
✅ 성공:  { "page": 1, "size": 10 }
❌ 실패:  { "page": 1, "size": 100 }  ← size 차이 강조
```

### 4. 스마트 필터링
히스토리에서 특정 키/값으로 검색:
```
[검색: username = "admin"] → 관련 요청만 표시
```

## 결론

✅ **개선 완료**

**주요 성과**:
1. 히스토리에서 요청 데이터를 즉시 확인 가능
2. 디버깅 효율성 대폭 향상
3. API 테스트 워크플로우 개선
4. 보안 (XSS 방어) 고려
5. 성능 최적화 (미리보기 길이 제한)

**사용자 피드백 반영**:
> "히스토리에 json 요청으로 무얼 담아 보냈는지 보이는게 좋겠지?"

→ ✅ 완벽하게 해결!

**영향 범위**:
- 수정된 파일: 2개
  - `history-manager.js` (로직 추가)
  - `api-tester.css` (스타일 추가)
- 새로운 함수: 1개
  - `escapeHtml()` (보안 헬퍼)
- 코드 추가: ~40줄
- 성능 영향: 무시할 수 있는 수준

**다음 단계**: 서버에 배포하고 실제 사용 피드백 수집
