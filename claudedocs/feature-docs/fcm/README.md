# FCM 푸시 알림 구현 가이드

> **폴더**: `feature-docs/fcm/`  
> **목적**: Firebase Cloud Messaging 구현 및 연동 가이드

---

## 🔔 개요

**Firebase Cloud Messaging을 활용한 웹 푸시 알림 시스템**

- **백엔드**: Firebase Admin SDK 9.7.0
- **프론트엔드**: Firebase Messaging SDK + Service Worker
- **세션 관리**: Redis 7.0.15

---

## 📄 문서 목록

### 1. fcm-code-review.md

**FCM 코드 리뷰 및 분석**

- 백엔드 구현 분석
- 프론트엔드 구현 분석
- 개선 사항

---

### 2. fcm-frontend-flow.md

**프론트엔드 구현 흐름**

```
사용자 접속
  ↓
알림 권한 요청
  ↓
FCM 토큰 발급
  ↓
백엔드에 토큰 전송
  ↓
Redis에 세션 저장
  ↓
푸시 알림 수신
```

---

### 3. fcm-frontend-integration.md

**프론트엔드 연동 가이드**

- Service Worker 등록
- Firebase 초기화
- 토큰 발급 및 전송
- 메시지 수신 처리

---

### 4. fcm-redis-flow.md

**Redis를 활용한 FCM 세션 관리**

- 다중 기기 로그인 지원
- 세션 관리 전략
- TTL 설정

---

### 5. fcm-admin-test.js

**관리자 테스트 스크립트**

```bash
node fcm-admin-test.js
```

---

### 6. fcm-browser-test.js

**브라우저 테스트 스크립트**

```bash
node fcm-browser-test.js
```

---

## 🚀 빠른 시작

### 1. Service Worker 등록

```javascript
// public/firebase-messaging-sw.js
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "YOUR_API_KEY",
  projectId: "lms-project-b8489",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID"
});

const messaging = firebase.messaging();
```

---

### 2. 프론트엔드 초기화

```javascript
import { getToken, onMessage } from 'firebase/messaging';

// 토큰 발급
const token = await getToken(messaging, {
  vapidKey: 'BFH1FPAvU0xBq1_SuGJs_4TYFN6a9D7qktiEzOcsE2OHhjfLRqlyvelzI8ZiLQVwd2FJN-4gXjQ4Yc0Xpo-bQ2E'
});

// 백엔드에 토큰 전송
await axios.post('/api/fcm/register', { token });

// 메시지 수신
onMessage(messaging, (payload) => {
  console.log('Message received:', payload);
});
```

---

### 3. 백엔드 API

**토큰 등록**
```
POST /api/fcm/register
Content-Type: application/json
Authorization: Bearer {token}

{
  "token": "FCM_TOKEN"
}
```

**푸시 알림 발송**
```
POST /api/fcm/send
Content-Type: application/json
Authorization: Bearer {token}

{
  "userId": "user123",
  "title": "알림 제목",
  "body": "알림 내용"
}
```

---

## ⚠️ 주의사항

### 브라우저 호환성

- ✅ Chrome 90+
- ✅ Firefox 88+
- ✅ Edge 90+
- ⚠️ Safari 16.4+ (제한적)
- ❌ Internet Explorer (지원 안 함)

### VAPID Keys

```properties
# 백엔드 설정
firebase.vapid.public-key=BFH1FPAvU0xBq1_SuGJs_4TYFN6a9D7qktiEzOcsE2OHhjfLRqlyvelzI8ZiLQVwd2FJN-4gXjQ4Yc0Xpo-bQ2E
firebase.vapid.private-key=4RZmK2xocSsh_2UOli83xZEeZaO4q2j5QiG1q4XXdGI
```

⚠️ **프론트엔드에서는 Public Key만 사용**

---

## 🔍 문제 해결

### "알림 권한이 거부되었습니다"

```javascript
// 권한 재요청
const permission = await Notification.requestPermission();
if (permission === 'granted') {
  // 토큰 발급
}
```

### "Service Worker 등록 실패"

```javascript
// HTTPS 환경인지 확인
if ('serviceWorker' in navigator && 'PushManager' in window) {
  // 등록 진행
}
```

---

## 📅 최종 업데이트

- **날짜**: 2025-10-10
- **버전**: 1.0
