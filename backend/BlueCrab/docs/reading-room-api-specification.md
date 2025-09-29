# 🏢 Blue-Crab LMS 열람실 API 명세서

> **버전**: v1.0.0  
> **최종 업데이트**: 2025-09-29  
> **Base URL**: `https://your-domain.com/BlueCrab-1.0.0/api/reading-room`

## 📋 개요

Blue-Crab LMS의 80석 규모 열람실 좌석 관리 시스템을 위한 REST API입니다.
모든 API는 JWT 토큰 기반 인증이 필요하며, POST 방식으로 통일되어 있습니다.

### 🔐 인증 방식
- **Type**: Bearer Token (JWT)
- **Header**: `Authorization: Bearer {JWT_TOKEN}`
- **토큰 획득**: 기존 로그인 API 사용

### 📤 공통 응답 형식
```json
{
  "success": boolean,           // 요청 성공/실패 여부
  "message": "string",          // 사용자 표시용 메시지 (한국어)
  "data": object | null,        // 실제 응답 데이터
  "errorCode": "string",        // 에러 코드 (실패시에만)
  "timestamp": "datetime"       // 응답 생성 시간
}
```

---

## 🎯 API 엔드포인트

### 1. 열람실 현황 조회

**실시간 좌석 상태와 통계 정보를 조회합니다.**

```http
POST /api/reading-room/status
```

#### 요청

**Headers:**
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Body:**
```json
{}
```

#### 응답

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "message": "열람실 현황 조회가 완료되었습니다.",
  "data": {
    "seats": [
      {
        "seatNumber": 1,              // 좌석 번호 (1~80)
        "isOccupied": false,          // 사용 여부 (false: 사용가능, true: 사용중)
        "endTime": null,              // 종료 예정 시간 (사용중일 때만)
        "remainingMinutes": null      // 남은 시간(분) (사용중일 때만)
      },
      {
        "seatNumber": 2,
        "isOccupied": true,
        "endTime": "2025-09-29T16:30:00",
        "remainingMinutes": 87
      }
      // ... 80개 좌석 정보
    ],
    "totalSeats": 80,               // 전체 좌석 수
    "availableSeats": 45,           // 사용 가능한 좌석 수
    "occupiedSeats": 35,            // 사용 중인 좌석 수
    "occupancyRate": 43.75          // 점유율 (%)
  },
  "timestamp": "2025-09-29T14:30:00"
}
```

**실패 응답:**
```json
// 인증 실패 (401 Unauthorized)
{
  "success": false,
  "message": "인증이 필요합니다.",
  "data": null,
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2025-09-29T14:30:00"
}

// 서버 오류 (500 Internal Server Error)
{
  "success": false,
  "message": "현황 조회 중 오류가 발생했습니다.",
  "data": null,
  "errorCode": "INTERNAL_ERROR",
  "timestamp": "2025-09-29T14:30:00"
}
```

---

### 2. 좌석 예약

**지정한 좌석을 예약합니다.**

```http
POST /api/reading-room/reserve
```

#### 요청

**Headers:**
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Body:**
```json
{
  "seatNumber": 15              // 예약할 좌석 번호 (1~80)
}
```

#### 응답

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "message": "좌석 예약이 완료되었습니다.",
  "data": {
    "seatNumber": 15,
    "startTime": "2025-09-29T14:30:00",      // 사용 시작 시간
    "endTime": "2025-09-29T16:30:00"         // 종료 예정 시간 (2시간 후)
  },
  "timestamp": "2025-09-29T14:30:00"
}
```

**실패 응답:**
```json
// 이미 예약된 좌석 (400 Bad Request)
{
  "success": false,
  "message": "해당 좌석은 이미 다른 사용자가 예약했습니다.",
  "data": null,
  "errorCode": "SEAT_ALREADY_OCCUPIED",
  "timestamp": "2025-09-29T14:30:00"
}

// 중복 예약 시도 (400 Bad Request)
{
  "success": false,
  "message": "이미 다른 좌석을 사용 중입니다.",
  "data": null,
  "errorCode": "USER_ALREADY_RESERVED",
  "timestamp": "2025-09-29T14:30:00"
}

// 존재하지 않는 좌석 (400 Bad Request)
{
  "success": false,
  "message": "존재하지 않는 좌석 번호입니다.",
  "data": null,
  "errorCode": "INVALID_SEAT_NUMBER",
  "timestamp": "2025-09-29T14:30:00"
}

// 필수 파라미터 누락 (400 Bad Request)
{
  "success": false,
  "message": "좌석 번호는 필수입니다.",
  "data": null,
  "errorCode": "MISSING_REQUIRED_PARAMETER",
  "timestamp": "2025-09-29T14:30:00"
}
```

---

### 3. 퇴실 처리

**현재 사용 중인 좌석에서 퇴실합니다.**

```http
POST /api/reading-room/checkout
```

#### 요청

**Headers:**
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Body:**
```json
{
  "seatNumber": 15              // 퇴실할 좌석 번호
}
```

#### 응답

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "message": "퇴실 처리가 완료되었습니다.",
  "data": {
    "seatNumber": 15,
    "usageTime": 87             // 실제 사용 시간 (분)
  },
  "timestamp": "2025-09-29T15:57:00"
}
```

**실패 응답:**
```json
// 권한 없음 - 본인 좌석이 아님 (403 Forbidden)
{
  "success": false,
  "message": "본인이 예약한 좌석이 아닙니다.",
  "data": null,
  "errorCode": "UNAUTHORIZED_SEAT",
  "timestamp": "2025-09-29T15:30:00"
}

// 존재하지 않는 좌석 (400 Bad Request)
{
  "success": false,
  "message": "존재하지 않는 좌석 번호입니다.",
  "data": null,
  "errorCode": "INVALID_SEAT_NUMBER",
  "timestamp": "2025-09-29T15:30:00"
}
```

---

### 4. 내 예약 조회

**현재 사용자의 예약 좌석 정보를 조회합니다.**

```http
POST /api/reading-room/my-reservation
```

#### 요청

**Headers:**
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Body:**
```json
{}
```

#### 응답

**성공 응답 - 예약 있음 (200 OK):**
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

**성공 응답 - 예약 없음 (200 OK):**
```json
{
  "success": true,
  "message": "현재 예약된 좌석이 없습니다.",
  "data": null,
  "timestamp": "2025-09-29T15:03:00"
}
```

---

## 🔄 프론트엔드 구현 가이드

### 1. JavaScript 예제 코드

#### API 호출 공통 함수
```javascript
// API 호출 공통 함수
async function callReadingRoomAPI(endpoint, data = {}) {
    const jwtToken = localStorage.getItem('jwtToken'); // 또는 적절한 토큰 저장소
    
    try {
        const response = await fetch(`/BlueCrab-1.0.0/api/reading-room${endpoint}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify(data)
        });
        
        const result = await response.json();
        
        if (!response.ok) {
            throw new Error(result.message || '요청 처리 중 오류가 발생했습니다.');
        }
        
        return result;
    } catch (error) {
        console.error('API 호출 오류:', error);
        throw error;
    }
}

// 열람실 현황 조회
async function getReadingRoomStatus() {
    return await callReadingRoomAPI('/status');
}

// 좌석 예약
async function reserveSeat(seatNumber) {
    return await callReadingRoomAPI('/reserve', { seatNumber });
}

// 퇴실 처리
async function checkoutSeat(seatNumber) {
    return await callReadingRoomAPI('/checkout', { seatNumber });
}

// 내 예약 조회
async function getMyReservation() {
    return await callReadingRoomAPI('/my-reservation');
}
```

#### 사용 예시
```javascript
// 현황 조회 및 화면 업데이트
async function updateReadingRoomStatus() {
    try {
        const result = await getReadingRoomStatus();
        
        if (result.success) {
            const { seats, totalSeats, availableSeats, occupiedSeats } = result.data;
            
            // 통계 정보 업데이트
            document.getElementById('total-seats').textContent = totalSeats;
            document.getElementById('available-seats').textContent = availableSeats;
            document.getElementById('occupied-seats').textContent = occupiedSeats;
            
            // 좌석 상태 업데이트
            seats.forEach(seat => {
                const seatElement = document.getElementById(`seat-${seat.seatNumber}`);
                if (seat.isOccupied) {
                    seatElement.classList.add('occupied');
                    seatElement.classList.remove('available');
                } else {
                    seatElement.classList.add('available');
                    seatElement.classList.remove('occupied');
                }
            });
        } else {
            alert(result.message);
        }
    } catch (error) {
        console.error('현황 조회 실패:', error);
        alert('현황 조회 중 오류가 발생했습니다.');
    }
}

// 좌석 예약
async function handleSeatReservation(seatNumber) {
    try {
        const result = await reserveSeat(seatNumber);
        
        if (result.success) {
            alert(result.message);
            await updateReadingRoomStatus(); // 현황 새로고침
            await checkMyReservation(); // 내 예약 정보 업데이트
        } else {
            alert(result.message);
        }
    } catch (error) {
        console.error('예약 실패:', error);
        alert('예약 처리 중 오류가 발생했습니다.');
    }
}

// 퇴실 처리
async function handleCheckout(seatNumber) {
    if (confirm('정말 퇴실하시겠습니까?')) {
        try {
            const result = await checkoutSeat(seatNumber);
            
            if (result.success) {
                alert(`${result.message} (사용시간: ${result.data.usageTime}분)`);
                await updateReadingRoomStatus(); // 현황 새로고침
                await checkMyReservation(); // 내 예약 정보 업데이트
            } else {
                alert(result.message);
            }
        } catch (error) {
            console.error('퇴실 실패:', error);
            alert('퇴실 처리 중 오류가 발생했습니다.');
        }
    }
}
```

### 2. React 컴포넌트 예제

```jsx
import { useState, useEffect } from 'react';

const ReadingRoom = () => {
    const [seats, setSeats] = useState([]);
    const [stats, setStats] = useState({});
    const [myReservation, setMyReservation] = useState(null);
    const [loading, setLoading] = useState(false);

    // 현황 조회
    const fetchStatus = async () => {
        setLoading(true);
        try {
            const result = await callReadingRoomAPI('/status');
            if (result.success) {
                setSeats(result.data.seats);
                setStats({
                    total: result.data.totalSeats,
                    available: result.data.availableSeats,
                    occupied: result.data.occupiedSeats
                });
            }
        } catch (error) {
            console.error('현황 조회 실패:', error);
        } finally {
            setLoading(false);
        }
    };

    // 내 예약 확인
    const fetchMyReservation = async () => {
        try {
            const result = await callReadingRoomAPI('/my-reservation');
            if (result.success) {
                setMyReservation(result.data);
            }
        } catch (error) {
            console.error('예약 정보 조회 실패:', error);
        }
    };

    useEffect(() => {
        fetchStatus();
        fetchMyReservation();
        
        // 30초마다 자동 새로고침
        const interval = setInterval(() => {
            fetchStatus();
            fetchMyReservation();
        }, 30000);

        return () => clearInterval(interval);
    }, []);

    return (
        <div className="reading-room">
            <div className="stats">
                <span>전체: {stats.total}</span>
                <span>사용가능: {stats.available}</span>
                <span>사용중: {stats.occupied}</span>
            </div>
            
            {myReservation && (
                <div className="my-reservation">
                    <h3>내 예약 정보</h3>
                    <p>좌석: {myReservation.seatNumber}번</p>
                    <p>남은시간: {myReservation.remainingMinutes}분</p>
                    <button onClick={() => handleCheckout(myReservation.seatNumber)}>
                        퇴실하기
                    </button>
                </div>
            )}
            
            <div className="seat-grid">
                {seats.map(seat => (
                    <div
                        key={seat.seatNumber}
                        className={`seat ${seat.isOccupied ? 'occupied' : 'available'}`}
                        onClick={() => !seat.isOccupied && handleSeatReservation(seat.seatNumber)}
                    >
                        {seat.seatNumber}
                    </div>
                ))}
            </div>
        </div>
    );
};
```

---

## 🎨 UI/UX 권장사항

### 1. 좌석 상태 표시
- **사용가능**: 초록색 (`#10B981`)
- **사용중**: 분홍색 (`#EC4899`)
- **내 좌석**: 파란색 (`#3B82F6`)

### 2. 자동 새로고침
- **권장 주기**: 30초~1분
- **사용자 액션 후**: 즉시 새로고침

### 3. 에러 처리
- **네트워크 오류**: "인터넷 연결을 확인해주세요."
- **인증 오류**: "다시 로그인해주세요."
- **비즈니스 오류**: API 응답의 `message` 필드 그대로 표시

---

## 🐛 문제해결 가이드

### 자주 발생하는 문제들

1. **401 Unauthorized 오류**
   - JWT 토큰 만료 또는 잘못된 토큰
   - 해결: 재로그인 또는 토큰 갱신

2. **CORS 오류**
   - 개발 환경에서 발생 가능
   - 해결: 프록시 설정 또는 서버 CORS 설정 확인

3. **좌석 예약 실패**
   - 다른 사용자가 먼저 예약한 경우
   - 해결: 현황 새로고침 후 다른 좌석 선택

---