# 🧪 Data-only 알림 테스트 가이드 (관리자 전용)

## 📋 개요
PC와 안드로이드에서 발생하는 **알림 중복 문제**를 테스트하기 위한 Data-only 방식 API입니다.

## 🔐 보안 정책
- ✅ **관리자 인증 필수**: `@PreAuthorize("hasRole('ADMIN')")`
- ✅ **프로덕션 안전**: testKey 없이 JWT 토큰으로 인증
- ✅ **감사 로그**: 모든 푸시 전송이 로그에 기록됨

## 🆕 추가된 API

### 엔드포인트
```
POST /api/push/send-data-only
```

### 인증
- **필수**: 관리자 JWT 토큰
- **방법**: Authorization 헤더에 `Bearer {token}` 포함

### 특징
- ✅ **Data-only 방식**: Notification 페이로드 없이 Data만 전송
- ✅ **중복 방지**: 시스템 자동 알림이 생성되지 않음
- ✅ **앱에서 처리**: 클라이언트가 Data를 받아 직접 알림 생성
- ⚠️ **제한사항**: 앱 종료 시 전달 보장 안됨

## 📱 테스트 페이지 사용법

### 1️⃣ 관리자 로그인
```
https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/status
```
- 상단 "로그인" 버튼 클릭
- 관리자 계정으로 로그인

### 2️⃣ API 선택
- 드롭다운에서 **"🧪 Data-only 알림 (관리자)"** 선택
- 자동으로 Authorization 헤더에 JWT 토큰 포함됨

### 3️⃣ FCM 토큰 입력
브라우저 콘솔(F12)에서 토큰 확인:
```javascript
sessionStorage.getItem('fcm')
```

### 4️⃣ 요청 Body 수정
```json
{
  "token": "여기에_실제_FCM_토큰_입력",
  "title": "Data-only 테스트",
  "body": "이 알림은 Data 방식으로만 전송됩니다",
  "data": {
    "type": "test",
    "action": "open_page"
  }
}
```

**⚠️ 주의**: `testKey` 필드는 제거됨 (JWT 인증 사용)

### 5️⃣ API 호출
"API 호출" 버튼 클릭

## 🔬 테스트 시나리오

### ✅ 시나리오 1: PC 웹 브라우저
```
1. 브라우저에서 LMS 사이트 열기
2. 로그인 후 알림 권한 허용
3. FCM 토큰 복사
4. 테스트 API 호출
5. 확인: 알림이 몇 번 표시되는지 확인
```

**예상 결과**:
- ⚠️ **현재**: Service Worker가 payload.notification을 찾지 못해 알림 안 뜰 수 있음
- ✅ **수정 후**: Service Worker가 payload.data를 처리하여 알림 1번 표시

### ✅ 시나리오 2: 안드로이드 모바일
```
1. 안드로이드 기기에서 앱 실행
2. 로그인 후 알림 권한 허용
3. FCM 토큰으로 테스트 API 호출
4. 확인: 알림이 1번만 표시되는지 확인
```

**예상 결과**:
- ✅ **앱 실행 중**: onMessageReceived()에서 Data 처리, 알림 1번 표시
- ❌ **앱 종료 시**: 알림 안 옴 (Notification 페이로드 없음)

## 📊 일반 API vs Data-only API 비교

| 구분 | 일반 API | Data-only API |
|------|---------|--------------|
| **엔드포인트** | `/api/push/send` | `/api/push/send-data-only` |
| **인증** | ✅ 관리자 JWT | ✅ 관리자 JWT |
| **Notification** | ✅ 포함 | ❌ 없음 |
| **Data** | ✅ 포함 | ✅ 포함 (title, body 포함) |
| **PC 웹** | 알림 2번 (중복) | 알림 1번 or 0번 |
| **안드로이드 (실행 중)** | 알림 2번 (중복) | 알림 1번 |
| **안드로이드 (종료 시)** | 알림 1번 | 알림 0번 ❌ |
| **재부팅 후** | 알림 1번 | 알림 0번 ❌ |
| **보안** | 🔐 안전 | 🔐 안전 |

## 🔧 프론트엔드 수정 필요

### Service Worker 수정
**파일**: `public/firebase-messaging-sw.js`

```javascript
messaging.onBackgroundMessage((payload) => {
    console.log('📨 메시지 수신:', payload);
    
    // ✅ Data-only 메시지 처리
    const title = payload.notification?.title || payload.data?.title || '새 알림';
    const body = payload.notification?.body || payload.data?.body || '새 메시지';
    
    console.log('  - 제목:', title);
    console.log('  - 본문:', body);

    const notificationOptions = {
        body: body,
        icon: '/firebase-logo.png',
        tag: 'notification-' + (title + body).substring(0, 30).replace(/\s/g, '-'),
        data: payload.data
    };

    return self.registration.showNotification(title, notificationOptions);
});
```

### React 컴포넌트 수정
**파일**: `firebase/PushNotification.jsx`

```javascript
setupForegroundListener() {
    onMessage(messaging, (payload) => {
        console.log('📨 포그라운드 메시지:', payload);
        
        // ✅ Data-only 메시지 처리
        const title = payload.notification?.title || payload.data?.title || '알림';
        const body = payload.notification?.body || payload.data?.body || '';
        
        // 포그라운드에서는 알림 표시 안함 (Service Worker가 처리)
        console.log('  - Service Worker가 처리함');

        // UI 업데이트 이벤트만 발생
        window.dispatchEvent(new CustomEvent('fcm-message', {
            detail: { title, body, data: payload.data }
        }));
    });
}
```

## 🎯 테스트 체크리스트

### 백엔드
- [x] `FirebasePushService.sendDataOnlyNotification()` 추가
- [x] `PushNotificationController.sendDataOnlyNotification()` 추가
- [x] 관리자 인증 적용 (`@PreAuthorize("hasRole('ADMIN')")`)
- [x] Security 설정 업데이트
- [x] API 템플릿 업데이트
- [ ] 서버 재시작 및 테스트

### 프론트엔드 (수정 필요)
- [ ] Service Worker에서 `payload.data` 처리 추가
- [ ] 포그라운드에서 중복 알림 방지
- [ ] 브라우저 강력 새로고침 (Ctrl+Shift+R)

### 테스트
- [ ] PC 웹: 알림 1번 표시 확인
- [ ] 안드로이드 (실행 중): 알림 1번 표시 확인
- [ ] 안드로이드 (종료 시): 알림 안 옴 확인 (예상 동작)

## 📝 cURL 예제

```bash
# 1. 먼저 관리자 로그인으로 JWT 토큰 발급
curl -X POST "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/admin/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin@example.com",
    "password": "your_password"
  }'

# 2. 응답에서 accessToken 복사 후 사용
curl -X POST "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/push/send-data-only" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE" \
  -d '{
    "token": "YOUR_FCM_TOKEN",
    "title": "Data-only 테스트",
    "body": "중복 방지 테스트 메시지",
    "data": {
      "type": "test",
      "timestamp": "2025-10-16"
    }
  }'
```

## 💡 결론

**Data-only 방식**은:
- ✅ **중복 방지**: PC와 안드로이드에서 중복 알림 해결
- ✅ **커스터마이징**: 완전한 제어 가능
- ❌ **전달 보장 없음**: 앱 종료/재부팅 시 알림 안 옴

**권장 사항**:
- 중요한 알림: Notification + Data 혼합 방식 (전달 보장)
- 실시간 알림: Data-only 방식 (중복 방지)

---

**작성일**: 2025-10-15  
**작성자**: GitHub Copilot
