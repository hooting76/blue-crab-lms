# FCM 푸시알림 프론트엔드 연동 가이드

## 📌 개요

백엔드 FCM API가 완성되었습니다. 프론트엔드에서 다음 시나리오를 처리해주세요.

## 🔄 API 엔드포인트

### 1. FCM 토큰 등록 (충돌 감지)
```
POST /api/fcm/register
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "fcmToken": "브라우저에서_생성한_FCM_토큰",
  "platform": "WEB",  // "ANDROID", "IOS", "WEB"
  "keepSignedIn": false,  // 로그인 유지 여부 (선택)
  "temporaryOnly": false  // 임시 등록 여부 (선택)
}
```

### 2. FCM 토큰 강제 변경
```
POST /api/fcm/register/force
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "fcmToken": "브라우저에서_생성한_FCM_토큰",
  "platform": "WEB",
  "keepSignedIn": false
}
```

### 3. FCM 토큰 삭제 (로그아웃)
```
DELETE /api/fcm/unregister
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "fcmToken": "브라우저에서_생성한_FCM_토큰",
  "platform": "WEB",
  "forceDelete": false  // keepSignedIn 무시하고 강제 삭제 (선택)
}
```

## 📊 응답 타입별 처리

### 1. `status: "registered"` - 최초 등록 성공
```json
{
  "success": true,
  "message": "알림이 활성화되었습니다",
  "data": {
    "status": "registered",
    "message": "알림이 활성화되었습니다",
    "keepSignedIn": false
  }
}
```
**처리:** 성공 메시지 표시

---

### 2. `status: "renewed"` - 같은 기기 재등록
```json
{
  "success": true,
  "message": "토큰이 갱신되었습니다",
  "data": {
    "status": "renewed",
    "message": "토큰이 갱신되었습니다",
    "keepSignedIn": false
  }
}
```
**처리:** 조용히 처리 (사용자에게 알릴 필요 없음)

---

### 3. `status: "conflict"` - 기기 충돌 발생 ⚠️
```json
{
  "success": true,
  "message": "이미 다른 기기에서 알림을 받고 있습니다",
  "data": {
    "status": "conflict",
    "message": "이미 다른 기기에서 알림을 받고 있습니다",
    "platform": "WEB",
    "lastUsed": "2025-10-06T21:48:00"
  }
}
```

**처리:** 사용자에게 선택 모달 표시

#### 모달 UI 구성:
```
┌─────────────────────────────────────────┐
│  ⚠️  기기 충돌 감지                      │
├─────────────────────────────────────────┤
│                                         │
│  이미 다른 기기(WEB)에서                │
│  알림을 받고 있습니다.                  │
│                                         │
│  마지막 사용: 2025-10-06 21:48         │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │  🔄 이 기기로 변경하기            │ │
│  │  (기존 기기는 알림 받지 않음)     │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │  📱 이 세션만 알림받기            │ │
│  │  (로그인 중에만 임시 알림)        │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │  ❌ 취소                          │ │
│  └───────────────────────────────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

#### 선택지별 처리:

**A. "이 기기로 변경하기" 선택 시:**
```javascript
// POST /api/fcm/register/force 호출
{
  "fcmToken": "현재_기기_토큰",
  "platform": "WEB",
  "keepSignedIn": false
}

// 응답: { status: "replaced" } → "기기가 변경되었습니다" 메시지
```

**B. "이 세션만 알림받기" 선택 시:**
```javascript
// POST /api/fcm/register 호출 (temporaryOnly=true)
{
  "fcmToken": "현재_기기_토큰",
  "platform": "WEB",
  "temporaryOnly": true
}

// 응답: { status: "temporary" } → "로그인 중에만 알림을 받습니다" 메시지
```

**C. "취소" 선택 시:**
```javascript
// 아무것도 하지 않음
// 기존 기기가 계속 알림을 받음
```

---

### 4. `status: "replaced"` - 강제 변경 완료
```json
{
  "success": true,
  "message": "기기가 변경되었습니다",
  "data": {
    "status": "replaced",
    "message": "기기가 변경되었습니다",
    "keepSignedIn": false
  }
}
```
**처리:** "기기가 변경되었습니다. 이제 이 기기로 알림을 받습니다." 메시지 표시

---

### 5. `status: "temporary"` - 임시 등록 완료
```json
{
  "success": true,
  "message": "임시 알림이 활성화되었습니다 (로그인 중에만 알림 받음)",
  "data": {
    "status": "temporary",
    "message": "임시 알림이 활성화되었습니다 (로그인 중에만 알림 받음)",
    "keepSignedIn": false
  }
}
```
**처리:** "이 세션 동안만 알림을 받습니다" 메시지 표시

---

## 💻 프론트엔드 구현 예제

### React 예제

```javascript
import { useState } from 'react';

function useFCMRegistration() {
  const [showConflictModal, setShowConflictModal] = useState(false);
  const [conflictData, setConflictData] = useState(null);

  // FCM 토큰 등록
  const registerFCMToken = async (fcmToken, keepSignedIn = false) => {
    try {
      const response = await fetch('/api/fcm/register', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          fcmToken,
          platform: 'WEB',
          keepSignedIn
        })
      });

      const result = await response.json();

      switch (result.data.status) {
        case 'registered':
          alert('알림이 활성화되었습니다');
          break;

        case 'renewed':
          // 조용히 처리
          console.log('토큰 갱신됨');
          break;

        case 'conflict':
          // 충돌 모달 표시
          setConflictData(result.data);
          setShowConflictModal(true);
          break;

        case 'replaced':
          alert('기기가 변경되었습니다');
          break;

        case 'temporary':
          alert('이 세션 동안만 알림을 받습니다');
          break;
      }
    } catch (error) {
      console.error('FCM 등록 실패:', error);
    }
  };

  // 강제 변경
  const forceRegister = async (fcmToken, keepSignedIn = false) => {
    try {
      const response = await fetch('/api/fcm/register/force', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          fcmToken,
          platform: 'WEB',
          keepSignedIn
        })
      });

      const result = await response.json();
      if (result.data.status === 'replaced') {
        alert('기기가 변경되었습니다');
        setShowConflictModal(false);
      }
    } catch (error) {
      console.error('강제 등록 실패:', error);
    }
  };

  // 임시 등록
  const registerTemporary = async (fcmToken) => {
    try {
      const response = await fetch('/api/fcm/register', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          fcmToken,
          platform: 'WEB',
          temporaryOnly: true
        })
      });

      const result = await response.json();
      if (result.data.status === 'temporary') {
        alert('이 세션 동안만 알림을 받습니다');
        setShowConflictModal(false);
      }
    } catch (error) {
      console.error('임시 등록 실패:', error);
    }
  };

  // 로그아웃 시 토큰 삭제
  const unregisterFCMToken = async (fcmToken, forceDelete = false) => {
    try {
      const response = await fetch('/api/fcm/unregister', {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          fcmToken,
          platform: 'WEB',
          forceDelete
        })
      });

      const result = await response.json();
      console.log('로그아웃 완료:', result);
    } catch (error) {
      console.error('FCM 토큰 삭제 실패:', error);
    }
  };

  return {
    registerFCMToken,
    forceRegister,
    registerTemporary,
    unregisterFCMToken,
    showConflictModal,
    conflictData,
    setShowConflictModal
  };
}

// 충돌 모달 컴포넌트
function DeviceConflictModal({ data, onChangeDevice, onTemporary, onCancel }) {
  return (
    <div className="modal">
      <div className="modal-content">
        <h2>⚠️ 기기 충돌 감지</h2>
        <p>
          이미 다른 기기({data.platform})에서<br />
          알림을 받고 있습니다.
        </p>
        <p className="last-used">
          마지막 사용: {new Date(data.lastUsed).toLocaleString()}
        </p>

        <button onClick={onChangeDevice} className="btn-primary">
          🔄 이 기기로 변경하기
          <small>(기존 기기는 알림 받지 않음)</small>
        </button>

        <button onClick={onTemporary} className="btn-secondary">
          📱 이 세션만 알림받기
          <small>(로그인 중에만 임시 알림)</small>
        </button>

        <button onClick={onCancel} className="btn-cancel">
          ❌ 취소
        </button>
      </div>
    </div>
  );
}
```

### Vanilla JavaScript 예제

```javascript
// FCM 토큰 등록
async function registerFCM(fcmToken, keepSignedIn = false) {
  const response = await fetch('/api/fcm/register', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      fcmToken: fcmToken,
      platform: 'WEB',
      keepSignedIn: keepSignedIn
    })
  });

  const result = await response.json();

  if (result.data.status === 'conflict') {
    showConflictModal(result.data, fcmToken);
  } else {
    handleNormalResponse(result.data);
  }
}

// 충돌 모달 표시
function showConflictModal(data, fcmToken) {
  const modal = document.createElement('div');
  modal.className = 'fcm-conflict-modal';
  modal.innerHTML = `
    <div class="modal-overlay"></div>
    <div class="modal-content">
      <h2>⚠️ 기기 충돌 감지</h2>
      <p>이미 다른 기기(${data.platform})에서 알림을 받고 있습니다.</p>
      <p>마지막 사용: ${new Date(data.lastUsed).toLocaleString()}</p>

      <button id="changeDevice">
        🔄 이 기기로 변경하기
        <small>(기존 기기는 알림 받지 않음)</small>
      </button>

      <button id="temporaryOnly">
        📱 이 세션만 알림받기
        <small>(로그인 중에만 임시 알림)</small>
      </button>

      <button id="cancel">❌ 취소</button>
    </div>
  `;

  document.body.appendChild(modal);

  // 이벤트 리스너
  document.getElementById('changeDevice').onclick = async () => {
    await registerForce(fcmToken);
    modal.remove();
  };

  document.getElementById('temporaryOnly').onclick = async () => {
    await registerTemporary(fcmToken);
    modal.remove();
  };

  document.getElementById('cancel').onclick = () => {
    modal.remove();
  };
}

// 강제 변경
async function registerForce(fcmToken) {
  const response = await fetch('/api/fcm/register/force', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      fcmToken: fcmToken,
      platform: 'WEB',
      keepSignedIn: false
    })
  });

  const result = await response.json();
  if (result.data.status === 'replaced') {
    alert('기기가 변경되었습니다');
  }
}

// 임시 등록
async function registerTemporary(fcmToken) {
  const response = await fetch('/api/fcm/register', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      fcmToken: fcmToken,
      platform: 'WEB',
      temporaryOnly: true
    })
  });

  const result = await response.json();
  if (result.data.status === 'temporary') {
    alert('이 세션 동안만 알림을 받습니다');
  }
}
```

## 🔔 로그인 플로우 통합

```javascript
// 1. 로그인 성공 후
async function onLoginSuccess(jwtToken) {
  localStorage.setItem('token', jwtToken);

  // 2. FCM 토큰 요청 (브라우저 권한)
  const permission = await Notification.requestPermission();

  if (permission === 'granted') {
    // 3. Firebase에서 FCM 토큰 생성
    const fcmToken = await getFirebaseToken();

    // 4. 백엔드에 등록
    await registerFCM(fcmToken, false);
  }
}

// 로그아웃 시
async function onLogout() {
  const fcmToken = await getFirebaseToken();

  // FCM 토큰 삭제 요청
  await fetch('/api/fcm/unregister', {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('token')}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      fcmToken: fcmToken,
      platform: 'WEB',
      forceDelete: false  // keepSignedIn 설정 존중
    })
  });

  localStorage.removeItem('token');
  window.location.href = '/login';
}
```

## 📱 "로그인 상태 유지" 체크박스 처리

```javascript
// 로그인 폼에 체크박스 추가
<input type="checkbox" id="keepSignedIn" />
<label for="keepSignedIn">로그인 상태 유지</label>

// 등록 시 체크박스 값 전달
const keepSignedIn = document.getElementById('keepSignedIn').checked;
await registerFCM(fcmToken, keepSignedIn);
```

**동작:**
- `keepSignedIn: true` → 로그아웃해도 토큰 유지 (재로그인 시 자동 알림)
- `keepSignedIn: false` → 로그아웃 시 토큰 삭제 (재로그인 시 다시 등록 필요)

## ⚠️ 주의사항

1. **FCM 토큰은 브라우저에서 생성**
   - Firebase SDK 사용: `firebase.messaging().getToken()`

2. **로그아웃 시 반드시 unregister 호출**
   - keepSignedIn=false인 경우 토큰이 DB에서 삭제됨

3. **충돌 모달은 무조건 표시**
   - 사용자가 선택하지 않으면 알림 등록 안됨

4. **플랫폼 값은 대소문자 구분 안함**
   - "WEB", "web", "Web" 모두 허용됨

## 🧪 테스트 방법

### 1. 정상 등록 테스트
```javascript
// 최초 로그인 → status: "registered"
await registerFCM(fcmToken, false);
```

### 2. 충돌 테스트
```javascript
// 다른 브라우저/시크릿 모드에서 같은 계정 로그인
// → status: "conflict" → 모달 표시
```

### 3. 임시 등록 테스트
```javascript
// 충돌 모달에서 "이 세션만 알림받기" 선택
// → status: "temporary"
// → 로그아웃 후 재로그인 시 기존 기기로 알림 감
```

### 4. 강제 변경 테스트
```javascript
// 충돌 모달에서 "이 기기로 변경하기" 선택
// → status: "replaced"
// → 기존 기기는 알림 못받음
```

## 📞 백엔드 담당자 연락처

문제 발생 시 백엔드 팀에 문의해주세요.

---

**작성일:** 2025-10-06
**버전:** 1.0
