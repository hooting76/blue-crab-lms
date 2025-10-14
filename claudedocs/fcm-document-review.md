# 📋 FCM 문서 검토 보고서

## 검토 일시
- **날짜**: 2025-10-14
- **검토 대상**: `docs/fcm-notification-flow.md`
- **검토자**: GitHub Copilot

---

## ✅ 정확한 부분

### 1. 토큰 등록 시나리오 (섹션 2)
- ✅ `/api/fcm/register` 엔드포인트 정확
- ✅ `status` 값들 (`registered`, `renewed`, `conflict`, `temporary`, `replaced`) 정확
- ✅ `keepSignedIn` 로직 정확
- ✅ `temporaryOnly` 플래그 정확
- ✅ `/api/fcm/register/force` 정확

### 2. 로그아웃 시나리오 (섹션 3)
- ✅ `/api/fcm/unregister` (DELETE) 정확
- ✅ `forceDelete` 로직 정확
- ✅ Redis 임시 토큰 제거 우선순위 정확

### 3. 브로드캐스트 (섹션 4.3)
- ✅ `/api/fcm/send/broadcast` 정확
- ✅ 500개씩 분할 전송 정확
- ✅ 무효 토큰 자동 제거 정확

### 4. 통계 조회 (섹션 4.4)
- ✅ `/api/fcm/stats` 정확

---

## ⚠️ 업데이트 필요한 부분

### 1. **프론트엔드 코드 변경 사항 미반영** (섹션 5.3)

#### ❌ 문서의 내용:
```markdown
3. **알림 수신 UI**  
   - `setupForegroundListener()` 가 발생시키는 `fcm-message` 이벤트를 구독해 토스트/배지 업데이트.
```

#### ⚠️ 실제 코드:
```javascript
// PushNotification.jsx - 현재 코드
setupForegroundListener() {
    onMessage(messaging, (payload) => {
        // ⚠️ 여전히 브라우저 알림을 표시하고 있음 (중복 방지 수정 전)
        if (Notification.permission === 'granted') {
            new Notification(title, { ... });
        }
        
        window.dispatchEvent(new CustomEvent('fcm-message', {
            detail: payload
        }));
    });
}
```

**문제점**: 중복 알림 수정사항이 반영되지 않았습니다.

---

### 2. **Service Worker 중복 방지 로직 미반영** (섹션 5.3)

#### ❌ 문서에 누락된 내용:
중복 알림 방지를 위한 태그 전략이 설명되어 있지 않음

#### ⚠️ 실제 코드:
```javascript
// firebase-messaging-sw.js - 현재 코드
const notificationOptions = {
    tag: 'notification-' + Date.now(),  // ⚠️ 매번 새로운 태그
};
```

**문제점**: `Date.now()`를 사용하면 중복 방지가 안됩니다.

---

### 3. **중요 누락: 중복 알림 이슈** (신규 섹션 필요)

문서에 다음 내용이 **완전히 누락**되어 있습니다:
- 브로드캐스트에서 DB 토큰 + Redis 임시 토큰 중복 전송 이슈
- 포그라운드/백그라운드 리스너 중복 이슈
- 해결 방법

**추천**: 섹션 7에 "알려진 이슈 및 해결책" 추가 필요

---

### 4. **응답 형식 불일치** (섹션 4.1)

#### ❌ 문서의 단일 사용자 응답:
```json
{
  "sent": { "android": false, "web": true },
  "failedReasons": { "android": "토큰이 등록되지 않았습니다" }
}
```

#### ⚠️ 실제 응답 (추정):
백엔드 코드를 확인해야 하지만, 표준 `ApiResponse` 래퍼가 있을 가능성 높음:
```json
{
  "success": true,
  "data": {
    "sent": { ... },
    "failedReasons": { ... }
  },
  "message": "...",
  "timestamp": "..."
}
```

---

### 5. **플랫폼 감지 로직 단순화** (섹션 1)

#### ❌ 문서의 설명:
```markdown
**플랫폼 구분**: 현재 구현은 `navigator.userAgent` 를 기반으로 
`ANDROID` / `IOS` / `WEB` 값을 추정합니다.
```

#### ⚠️ 실제 코드:
```javascript
// PushNotification.jsx
if(agentSwitch.includes('Windows')){
    platform = "WEB";
}else if(agentSwitch.includes('Android')){
    platform = "ANDROID";
}else if(agentSwitch.includes('iPhone')){
    platform = "IOS";
};
```

**문제점**: 
- Mac/Linux도 "WEB"으로 처리해야 함
- iPad가 누락됨
- 더 정확한 감지 로직 필요

---

## 📝 권장 수정 사항

### 1. 섹션 5.3 알림 수신 UI 업데이트

**현재**:
```markdown
3. **알림 수신 UI**  
   - `setupForegroundListener()` 가 발생시키는 `fcm-message` 이벤트를 구독해 토스트/배지 업데이트.  
   - `firebase-messaging-sw.js` 의 `notificationclick`에서 `payload.data`를 활용해 특정 화면으로 라우팅.
```

**수정안**:
```markdown
3. **알림 수신 UI**  
   - **포그라운드**: `setupForegroundListener()`가 발생시키는 `fcm-message` 이벤트를 구독해 UI 업데이트.
     - ⚠️ **중요**: 포그라운드에서는 브라우저 알림을 표시하지 않습니다 (중복 방지).
     - Service Worker가 모든 알림 표시를 담당합니다.
   - **백그라운드**: `firebase-messaging-sw.js`의 `onBackgroundMessage`가 자동으로 알림 표시.
     - 중복 방지를 위해 고유한 `tag` 사용 권장 (예: 제목+본문 해시).
   - **클릭 이벤트**: `notificationclick`에서 `payload.data`를 활용해 특정 화면으로 라우팅.
```

---

### 2. 섹션 7 신규 추가: "알려진 이슈 및 해결책"

```markdown
## 7. 알려진 이슈 및 해결책

### 7.1 중복 알림 이슈

#### 문제 1: 브로드캐스트 중복 전송
**증상**: 같은 사용자에게 알림이 2번 도착
**원인**: DB 토큰과 Redis 임시 토큰이 모두 전송 대상에 포함
**해결**: 브로드캐스트(`/send/broadcast`)에서는 DB 영구 토큰만 사용하도록 수정

```java
// ✅ 수정된 코드
for (FcmToken fcmToken : allTokens) {
    for (String platform : targetPlatforms) {
        String token = fcmToken.getTokenByPlatform(platform);
        if (token != null) {
            allValidTokens.add(token);
        }
        // ⚠️ 임시 토큰 제외 (개별 전송에서만 사용)
    }
}
```

#### 문제 2: 포그라운드/백그라운드 중복
**증상**: 브라우저 탭이 열려있을 때 알림이 2번 표시
**원인**: `onMessage` + Service Worker 둘 다 알림 표시
**해결**: 포그라운드에서는 브라우저 알림 표시 제거, UI 이벤트만 발생

```javascript
// ✅ 수정된 코드
setupForegroundListener() {
    onMessage(messaging, (payload) => {
        // 브라우저 알림 표시 안함 (Service Worker가 처리)
        // new Notification(...); ← 주석 처리
        
        // UI 이벤트만 발생
        window.dispatchEvent(new CustomEvent('fcm-message', {
            detail: payload
        }));
    });
}
```

#### 문제 3: Service Worker 태그 중복
**증상**: 같은 알림이 연속으로 여러 번 표시
**원인**: `tag: 'notification-' + Date.now()` 사용으로 매번 새로운 알림 생성
**해결**: 내용 기반 고유 태그 사용

```javascript
// ✅ 수정된 코드
const uniqueTag = 'notification-' + 
    (payload.notification?.title || '') + 
    (payload.notification?.body || '').substring(0, 20);

const notificationOptions = {
    tag: uniqueTag,  // 같은 태그는 덮어쓰기
    renotify: false  // 재알림 시 진동 안함
};
```

### 7.2 토큰 만료 이슈

**증상**: 모바일 기기에서 알림이 오지 않음
**원인**: FCM 토큰 만료 (앱 재설치, 데이터 삭제, 장기 미사용)
**해결**: 
- 백엔드가 자동으로 무효 토큰 감지 및 DB에서 제거
- 사용자는 앱 재실행 또는 재로그인으로 새 토큰 등록
- 권장: 24시간마다 토큰 유효성 체크 로직 추가

### 7.3 플랫폼 감지 정확도

**문제**: User Agent 기반 플랫폼 감지가 불완전
**개선안**:
```javascript
function detectPlatform() {
    const ua = navigator.userAgent.toLowerCase();
    
    if (/android/.test(ua)) {
        return 'ANDROID';
    } else if (/iphone|ipad|ipod/.test(ua)) {
        return 'IOS';
    } else {
        return 'WEB';  // Windows, Mac, Linux 모두
    }
}
```
```

---

### 3. 섹션 1 플랫폼 구분 개선

**현재**:
```markdown
- **플랫폼 구분**: 현재 구현은 `navigator.userAgent` 를 기반으로 
  `ANDROID` / `IOS` / `WEB` 값을 추정합니다. 필요 시 UI 또는 UA Detection을 보완해 전달합니다.
```

**수정안**:
```markdown
- **플랫폼 구분**: `navigator.userAgent`를 기반으로 플랫폼 자동 감지
  - `ANDROID`: Android 기기
  - `IOS`: iPhone, iPad, iPod
  - `WEB`: Windows, Mac, Linux 등 기타 모든 플랫폼
  - ⚠️ **주의**: User Agent는 쉽게 변조될 수 있으므로, 보안이 중요한 경우 추가 검증 필요
```

---

## 🎯 종합 평가

### ✅ 장점
1. **전체 흐름이 명확**: 로그인 → 등록 → 전송 → 로그아웃 시나리오가 잘 정리됨
2. **API 명세 정확**: 엔드포인트, 요청/응답 형식이 대부분 정확
3. **구조화 잘됨**: 시나리오별로 분리되어 읽기 쉬움

### ⚠️ 개선 필요
1. **최신 코드 미반영**: 중복 알림 수정사항 누락
2. **이슈 섹션 부재**: 알려진 문제와 해결책 설명 필요
3. **일부 세부사항 불일치**: 응답 형식, 플랫폼 감지 로직

### 📊 점수
- **정확성**: 7/10 (주요 로직은 맞지만 최신 변경사항 미반영)
- **완전성**: 6/10 (이슈 섹션, 중복 방지 설명 누락)
- **유용성**: 8/10 (전체 흐름 이해에는 매우 유용)

**총평**: **프론트엔드/백엔드 개발자가 참고하기에 좋은 문서**이지만, **최신 코드 변경사항과 알려진 이슈를 추가하면 더욱 완벽**해질 것입니다.

---

## 📋 체크리스트

### 즉시 수정 필요
- [ ] 섹션 5.3: 중복 알림 방지 로직 추가
- [ ] 섹션 7: 알려진 이슈 섹션 추가
- [ ] 섹션 1: 플랫폼 감지 로직 정확히 기술

### 확인 필요
- [ ] 섹션 4.1: 실제 API 응답 형식 확인 (ApiResponse 래퍼 여부)
- [ ] 섹션 5.5: VAPID 키와 백엔드 URL 환경 변수 분리 여부 확인

### 선택 사항
- [ ] 예제 코드 스니펫 추가 (React Hook 예제 등)
- [ ] 트러블슈팅 FAQ 섹션 추가
- [ ] 다이어그램 추가 (시퀀스 다이어그램 등)

---

## 🚀 최종 권장 사항

이 문서를 **프로덕션 레벨**로 만들려면:

1. **지금 바로**: 중복 알림 관련 내용 추가 (섹션 7)
2. **이번 주 내**: 최신 코드와 동기화 (포그라운드 리스너 로직)
3. **향후 개선**: 실제 API 호출 예제, 에러 처리 가이드 추가

**전체적으로 매우 잘 작성된 문서**이며, 위의 개선사항만 반영하면 **팀 내 표준 문서**로 사용하기에 충분합니다! 👍
