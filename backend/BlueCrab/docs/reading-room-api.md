# 열람실 API 문서

## 📋 개요

Blue-Crab LMS 열람실 좌석 신청 및 관리 시스템의 REST API 문서입니다.

### 주요 기능
- 실시간 좌석 현황 조회
- 좌석 신청 및 예약
- 퇴실 처리
- 사용자별 예약 현황 확인

### 시스템 특징
- **80석 규모** 단일 열람실 관리
- **2시간 기본** 사용시간 제공
- **1인 1좌석** 제한 정책
- **자동 퇴실** 처리 (시간 만료 시)

## 🔐 인증

모든 API는 JWT 토큰 기반 인증이 필요합니다.

### 헤더 설정
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

### JWT 토큰 획득
기존 로그인 API (`/api/auth/login`)를 통해 JWT 토큰을 획득합니다.

## ⏱️ 요청 빈도 제한 (Rate Limiting)

서버 부하 방지와 안정적인 서비스 제공을 위해 API별로 요청 빈도 제한이 적용됩니다.

### 제한 정책
| API 엔드포인트 | 시간 윈도우 | 최대 요청 횟수 | 제한 사유 |
|----------------|------------|--------------|-----------|
| `/status` | 10초 | 5회 | 빈번한 현황 조회 방지 |
| `/reserve` | 60초 | 3회 | 좌석 예약 스팸 방지 |
| `/checkout` | 30초 | 2회 | 실수 퇴실 방지 |
| `/my-reservation` | 5초 | 10회 | 가벼운 조회이므로 관대 |

### 제한 초과 시 응답
```json
HTTP 429 Too Many Requests
{
  "success": false,
  "message": "현황 조회가 너무 빈번합니다. 10초 후 다시 시도해주세요.",
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "timestamp": "2025-09-29T14:30:00"
}
```

### 제한 범위
- **사용자 기반**: JWT 토큰으로 식별된 사용자별로 독립적인 제한
- **엔드포인트별**: 각 API 엔드포인트마다 별도의 카운터 관리
- **자동 초기화**: 시간 윈도우 경과 시 자동으로 카운터 리셋

## 🚀 API 엔드포인트

### Base URL
```
http://localhost:8080/api/reading-room
```

---

## 1. 열람실 현황 조회

실시간 좌석 현황과 통계 정보를 조회합니다.

### 요청
```http
POST /api/reading-room/status
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{}
```

### 응답

#### 성공 (200 OK)
```json
{
  "success": true,
  "message": "열람실 현황 조회가 완료되었습니다.",
  "data": {
    "seats": [
      {
        "seatNumber": 1,
        "isOccupied": false,
        "endTime": null,
        "remainingMinutes": null
      },
      {
        "seatNumber": 2,
        "isOccupied": true,
        "endTime": "2025-09-29T16:30:00",
        "remainingMinutes": 87
      }
    ],
    "totalSeats": 80,
    "availableSeats": 45,
    "occupiedSeats": 35,
    "occupancyRate": 43.75
  },
  "timestamp": "2025-09-29T14:30:00"
}
```

#### 실패 (401 Unauthorized)
```json
{
  "success": false,
  "message": "인증이 필요합니다.",
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2025-09-29T14:30:00"
}
```

---

## 2. 좌석 신청

원하는 좌석을 신청합니다.

### 요청
```http
POST /api/reading-room/reserve
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{
  "seatNumber": 15
}
```

### 요청 파라미터
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| seatNumber | Integer | ✅ | 신청할 좌석 번호 (1~80) |

### 응답

#### 성공 (200 OK)
```json
{
  "success": true,
  "message": "좌석 예약이 완료되었습니다.",
  "data": {
    "seatNumber": 15,
    "startTime": "2025-09-29T14:30:00",
    "endTime": "2025-09-29T16:30:00",
    "usageTimeMinutes": 120
  },
  "timestamp": "2025-09-29T14:30:00"
}
```

#### 실패 - 이미 예약된 좌석 (400 Bad Request)
```json
{
  "success": false,
  "message": "해당 좌석은 이미 다른 사용자가 예약했습니다.",
  "errorCode": "SEAT_ALREADY_OCCUPIED",
  "timestamp": "2025-09-29T14:30:00"
}
```

#### 실패 - 중복 예약 (400 Bad Request)
```json
{
  "success": false,
  "message": "이미 다른 좌석을 사용 중입니다.",
  "errorCode": "USER_ALREADY_RESERVED",
  "timestamp": "2025-09-29T14:30:00"
}
```

#### 실패 - 잘못된 좌석 번호 (400 Bad Request)
```json
{
  "success": false,
  "message": "올바른 좌석 번호를 입력해주세요. (1~80)",
  "errorCode": "INVALID_SEAT_NUMBER",
  "timestamp": "2025-09-29T14:30:00"
}
```

---

## 3. 퇴실 처리

현재 사용 중인 좌석에서 퇴실합니다.

### 요청
```http
POST /api/reading-room/checkout
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{
  "seatNumber": 15
}
```

### 요청 파라미터
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| seatNumber | Integer | ✅ | 퇴실할 좌석 번호 (1~80) |

### 응답

#### 성공 (200 OK)
```json
{
  "success": true,
  "message": "퇴실 처리가 완료되었습니다.",
  "data": {
    "seatNumber": 15,
    "usageTime": 87
  },
  "timestamp": "2025-09-29T15:57:00"
}
```

#### 실패 - 권한 없음 (400 Bad Request)
```json
{
  "success": false,
  "message": "본인이 예약한 좌석이 아닙니다.",
  "errorCode": "UNAUTHORIZED_SEAT",
  "timestamp": "2025-09-29T14:30:00"
}
```

---

## 4. 내 예약 조회

현재 사용자의 예약 좌석 정보를 조회합니다.

### 요청
```http
POST /api/reading-room/my-reservation
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{}
```

### 응답

#### 성공 - 예약 있음 (200 OK)
```json
{
  "success": true,
  "message": "예약 좌석 정보를 조회했습니다.",
  "data": {
    "seatNumber": 15,
    "isOccupied": true,
    "endTime": "2025-09-29T16:30:00",
    "remainingMinutes": 87
  },
  "timestamp": "2025-09-29T15:03:00"
}
```

#### 성공 - 예약 없음 (200 OK)
```json
{
  "success": true,
  "message": "현재 예약된 좌석이 없습니다.",
  "data": null,
  "timestamp": "2025-09-29T14:30:00"
}
```

---

## 📊 데이터 모델

### ReadingSeatDto
```json
{
  "seatNumber": 1,           // 좌석 번호 (1~80)
  "isOccupied": false,       // 사용 여부 (true: 사용중, false: 사용가능)
  "endTime": "2025-09-29T16:30:00",  // 종료 예정 시간 (ISO-8601)
  "remainingMinutes": 87     // 남은 시간 (분)
}
```

### ReadingRoomStatusDto
```json
{
  "seats": [...],            // 좌석별 상태 정보 배열
  "totalSeats": 80,          // 전체 좌석 수
  "availableSeats": 45,      // 사용 가능한 좌석 수
  "occupiedSeats": 35,       // 사용 중인 좌석 수
  "occupancyRate": 43.75     // 사용률 (%)
}
```

### SeatReserveResponseDto
```json
{
  "seatNumber": 15,          // 예약된 좌석 번호
  "startTime": "2025-09-29T14:30:00",    // 시작 시간 (ISO-8601)
  "endTime": "2025-09-29T16:30:00",      // 종료 예정 시간 (ISO-8601)
  "usageTimeMinutes": 120    // 사용 가능 시간 (분)
}
```

### SeatCheckoutResponseDto
```json
{
  "seatNumber": 15,          // 퇴실한 좌석 번호
  "usageTime": 87            // 실제 사용 시간 (분)
}
```

---

## ⚠️ 에러 코드

| 에러 코드 | HTTP 상태 | 설명 |
|-----------|-----------|------|
| `UNAUTHORIZED` | 401 | 인증 토큰이 유효하지 않음 |
| `SEAT_ALREADY_OCCUPIED` | 400 | 요청한 좌석이 이미 사용 중 |
| `USER_ALREADY_RESERVED` | 400 | 사용자가 이미 다른 좌석을 예약 중 |
| `UNAUTHORIZED_SEAT` | 400 | 본인이 예약한 좌석이 아님 |
| `INVALID_SEAT_NUMBER` | 400 | 유효하지 않은 좌석 번호 (1~80 범위 외) |
| `INVALID_REQUEST` | 400 | 잘못된 요청 데이터 |
| `RATE_LIMIT_EXCEEDED` | 429 | 요청 빈도 제한 초과 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

---

## 🔄 비즈니스 룰

### 좌석 예약 규칙
1. **1인 1좌석 제한**: 한 사용자는 동시에 하나의 좌석만 예약 가능
2. **2시간 기본 사용**: 예약 시 자동으로 2시간 후 종료 시간 설정
3. **실시간 확인**: 예약 요청 시 실시간으로 좌석 사용 가능 여부 확인

### 자동 정리 시스템
1. **자동 퇴실**: 종료 시간이 지나면 자동으로 좌석 해제
2. **주기적 정리**: 5분마다 만료된 좌석들을 자동 정리
3. **사용 기록**: 모든 입실/퇴실 이력을 데이터베이스에 기록

### 권한 관리
1. **본인 좌석만**: 퇴실은 본인이 예약한 좌석에서만 가능
2. **JWT 기반**: 모든 요청에서 JWT 토큰으로 사용자 식별
3. **실시간 검증**: 요청 시마다 토큰 유효성 검증

### 요청 빈도 제한
1. **사용자별 제한**: JWT 토큰으로 식별된 사용자별로 독립적인 카운터 관리
2. **API별 제한**: 각 엔드포인트마다 다른 제한 정책 적용
3. **슬라이딩 윈도우**: 시간 윈도우 내에서 지속적인 요청 모니터링
4. **자동 복구**: 시간 윈도우 경과 시 자동으로 제한 해제

---

## 🧪 테스트

### API 테스트 도구
프로젝트에 포함된 `reading-room-api-test.html` 파일을 브라우저에서 열어 GUI 환경에서 API를 테스트할 수 있습니다.

### 테스트 시나리오

#### 1. 기본 시나리오
1. JWT 토큰 획득 (로그인)
2. 현황 조회로 사용 가능한 좌석 확인
3. 원하는 좌석 신청
4. 내 예약 확인
5. 퇴실 처리

#### 2. 예외 상황 테스트
- 이미 예약된 좌석 신청 시도
- 중복 예약 시도
- 잘못된 좌석 번호 입력
- 다른 사용자 좌석 퇴실 시도
- 요청 빈도 제한 테스트 (연속 API 호출)

### 3. Rate Limiting 테스트
#### 현황 조회 제한 테스트
```javascript
// 10초 내 6회 요청 시도 → 6번째부터 HTTP 429 응답
for (let i = 0; i < 6; i++) {
    await fetch('/api/reading-room/status', { method: 'POST', ... });
}
```

#### 좌석 예약 제한 테스트  
```javascript
// 60초 내 4회 예약 시도 → 4번째부터 HTTP 429 응답
for (let i = 0; i < 4; i++) {
    await fetch('/api/reading-room/reserve', { 
        method: 'POST', 
        body: JSON.stringify({ seatNumber: i + 1 }) 
    });
}
```

---

## 📈 모니터링

### 로그 확인
시스템은 다음과 같은 로그를 제공합니다:

- **API 요청 로그**: 모든 API 호출 기록
- **예약/퇴실 로그**: 좌석 사용 이벤트 기록
- **자동 정리 로그**: 만료 좌석 정리 작업 기록
- **시간별 현황 로그**: 매 시간 정각의 이용 현황

### 통계 정보
- 실시간 사용률 계산
- 좌석별 이용 빈도
- 사용자별 이용 이력
- 시간대별 이용 패턴

---

## 🔧 설정

### 애플리케이션 설정
- **자동 정리 주기**: 5분 (300,000ms)
- **상태 로깅 주기**: 매시간 정각
- **기본 사용시간**: 2시간 (120분)

### Rate Limiting 설정
- **현황 조회**: 10초 윈도우, 최대 5회 요청
- **좌석 예약**: 60초 윈도우, 최대 3회 요청  
- **퇴실 처리**: 30초 윈도우, 최대 2회 요청
- **예약 조회**: 5초 윈도우, 최대 10회 요청
- **저장 방식**: 메모리 기반 (ConcurrentHashMap)
- **키 형식**: `rate_limit:user:{userCode}:{endpoint}`

### 데이터베이스 설정
- **좌석 수**: 80석 고정
- **사용 기록 보관**: 영구 보관
- **인덱스 최적화**: 자주 사용되는 쿼리 최적화

---

## 📝 추가 개발 사항

### 향후 확장 가능 기능
1. **다중 열람실 지원**: 여러 열람실 관리
2. **예약 연장**: 사용 시간 연장 기능
3. **대기열 시스템**: 좌석 대기 예약
4. **통계 대시보드**: 관리자용 통계 화면
5. **알림 시스템**: 종료 시간 임박 알림
6. **분산 Rate Limiting**: Redis 기반 다중 서버 환경 지원

### 성능 최적화
1. **캐싱 시스템**: Redis를 활용한 현황 캐싱
2. **배치 처리**: 대량 데이터 처리 최적화
3. **인덱스 튜닝**: 쿼리 성능 향상
4. **Rate Limit 최적화**: 
   - Redis를 통한 분산 환경 지원
   - 메모리 사용량 최적화를 위한 주기적 정리
   - IP 기반 제한으로 추가 보안 강화

---

## 🛡️ 클라이언트 개발 가이드

### Rate Limiting 대응 방법

#### 1. 응답 상태 코드 확인
```javascript
const response = await fetch('/api/reading-room/status', {
    method: 'POST',
    headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({})
});

if (response.status === 429) {
    const errorData = await response.json();
    showUserMessage(errorData.message); // "현황 조회가 너무 빈번합니다. 10초 후 다시 시도해주세요."
    return;
}
```

#### 2. 프론트엔드 쿨타임 구현
```javascript
class ApiCooldownManager {
    constructor() {
        this.lastRequestTimes = new Map();
    }
    
    canRequest(apiKey, cooldownSeconds) {
        const now = Date.now();
        const lastRequest = this.lastRequestTimes.get(apiKey);
        
        if (lastRequest && (now - lastRequest) < (cooldownSeconds * 1000)) {
            return false;
        }
        
        this.lastRequestTimes.set(apiKey, now);
        return true;
    }
}

// 사용 예시
const cooldownManager = new ApiCooldownManager();

async function getReadingRoomStatus() {
    if (!cooldownManager.canRequest('status', 10)) {
        showUserMessage('너무 빨리 요청하고 있습니다. 잠시 후 다시 시도해주세요.');
        return;
    }
    
    // API 호출 진행
    const result = await callAPI('/status');
    return result;
}
```

#### 3. 버튼 비활성화
```javascript
function disableButtonWithCooldown(buttonId, seconds) {
    const button = document.getElementById(buttonId);
    button.disabled = true;
    button.textContent = `${seconds}초 후 다시 시도`;
    
    const countdown = setInterval(() => {
        seconds--;
        if (seconds <= 0) {
            button.disabled = false;
            button.textContent = '현황 새로고침';
            clearInterval(countdown);
        } else {
            button.textContent = `${seconds}초 후 다시 시도`;
        }
    }, 1000);
}
```

#### 4. 자동 재시도 (권장하지 않음)
```javascript
// ⚠️ 주의: 자동 재시도는 Rate Limiting을 우회하려는 시도로 간주될 수 있습니다.
// 사용자가 직접 재시도하도록 유도하는 것이 바람직합니다.

async function apiCallWithRetry(endpoint, data, maxRetries = 1) {
    for (let i = 0; i < maxRetries; i++) {
        try {
            const response = await callAPI(endpoint, data);
            return response;
        } catch (error) {
            if (error.status === 429 && i < maxRetries - 1) {
                // 서버에서 지정한 시간만큼 대기 (권장하지 않음)
                await new Promise(resolve => setTimeout(resolve, 10000));
            } else {
                throw error;
            }
        }
    }
}
```

### 권장 사항
1. **사용자 피드백**: Rate Limit 오류 시 명확한 안내 메시지 표시
2. **버튼 제어**: 요청 후 일정 시간 동안 버튼 비활성화
3. **로딩 상태**: API 호출 중임을 시각적으로 표시
4. **적절한 간격**: 불필요한 빈번한 요청 방지
5. **에러 처리**: 429 응답에 대한 적절한 사용자 안내

---

이상으로 Blue-Crab LMS 열람실 API 문서를 마칩니다. 
추가 질문이나 개선 사항이 있으면 언제든 문의해 주세요! 🚀