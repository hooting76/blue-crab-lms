// ===================================================================
// Blue-Crab LMS 열람실 API 테스트 스크립트 (개발자 콘솔용)
// ===================================================================

/**
 * 사용법:
 * 1. 브라우저에서 F12를 눌러 개발자 도구 열기
 * 2. Console 탭으로 이동
 * 3. 이 스크립트를 전체 복사하여 붙여넣기
 * 4. Enter로 실행
 * 5. setToken('your_jwt_token') 으로 토큰 설정
 * 6. 각 테스트 함수 실행
 */

// ===================================================================
// 설정 및 초기화
// ===================================================================

let API_BASE_URL = 'http://localhost:8080/api/reading-room';
let JWT_TOKEN = '';

// 토큰 설정 함수
function setToken(token) {
    JWT_TOKEN = token;
    console.log('✅ JWT 토큰이 설정되었습니다.');
    console.log('🔑 토큰 길이:', token.length);
    console.log('📝 토큰 시작:', token.substring(0, 50) + '...');
}

// API URL 설정 함수 (포트나 호스트가 다른 경우)
function setApiUrl(url) {
    API_BASE_URL = url;
    console.log('🌐 API URL이 설정되었습니다:', url);
}

// 공통 API 호출 함수
async function callAPI(endpoint, data = {}) {
    if (!JWT_TOKEN) {
        console.error('❌ JWT 토큰이 설정되지 않았습니다. setToken("your_token") 먼저 실행하세요.');
        return null;
    }

    const url = API_BASE_URL + endpoint;
    const startTime = performance.now();

    try {
        console.log(`🚀 API 호출: ${endpoint}`);
        console.log('📤 요청 데이터:', data);

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${JWT_TOKEN}`
            },
            body: JSON.stringify(data)
        });

        const responseData = await response.json();
        const endTime = performance.now();
        const duration = Math.round(endTime - startTime);

        console.log(`⏱️  응답 시간: ${duration}ms`);
        console.log(`📊 HTTP 상태: ${response.status} ${response.statusText}`);

        if (response.ok) {
            console.log('✅ 요청 성공!');
            console.log('📥 응답 데이터:', responseData);
        } else {
            console.log('❌ 요청 실패!');
            console.log('📥 에러 응답:', responseData);
        }

        return responseData;

    } catch (error) {
        const endTime = performance.now();
        const duration = Math.round(endTime - startTime);
        
        console.error(`💥 네트워크 오류 (${duration}ms):`, error);
        return null;
    }
}

// ===================================================================
// 테스트 함수들
// ===================================================================

// 1. 열람실 현황 조회 테스트
async function testGetStatus() {
    console.log('\n📊 === 열람실 현황 조회 테스트 ===');
    const result = await callAPI('/status', {});
    
    if (result && result.success) {
        const data = result.data;
        console.log(`🏛️ 전체 좌석: ${data.totalSeats}석`);
        console.log(`✅ 사용가능: ${data.availableSeats}석`);
        console.log(`🔴 사용중: ${data.occupiedSeats}석`);
        console.log(`📈 사용률: ${data.occupancyRate}%`);
        
        // 처음 10개 좌석 상태 출력
        console.log('\n🪑 좌석 상태 (1-10번):');
        data.seats.slice(0, 10).forEach(seat => {
            const status = seat.isOccupied ? '🔴 사용중' : '✅ 사용가능';
            const endTime = seat.endTime ? ` (종료: ${seat.endTime})` : '';
            console.log(`  좌석 ${seat.seatNumber}: ${status}${endTime}`);
        });
    }
    
    return result;
}

// 2. 좌석 신청 테스트
async function testReserveSeat(seatNumber) {
    if (!seatNumber) {
        console.log('❌ 좌석 번호를 입력하세요. 예: testReserveSeat(15)');
        return null;
    }

    console.log(`\n🪑 === 좌석 ${seatNumber}번 신청 테스트 ===`);
    const result = await callAPI('/reserve', { seatNumber: seatNumber });
    
    if (result && result.success) {
        const data = result.data;
        console.log(`✅ 좌석 ${data.seatNumber}번 예약 완료!`);
        console.log(`⏰ 시작시간: ${data.startTime}`);
        console.log(`⏰ 종료시간: ${data.endTime}`);
        console.log(`⏳ 사용시간: ${data.usageTimeMinutes}분`);
    }
    
    return result;
}

// 3. 퇴실 처리 테스트
async function testCheckout(seatNumber) {
    if (!seatNumber) {
        console.log('❌ 좌석 번호를 입력하세요. 예: testCheckout(15)');
        return null;
    }

    console.log(`\n🚪 === 좌석 ${seatNumber}번 퇴실 테스트 ===`);
    const result = await callAPI('/checkout', { seatNumber: seatNumber });
    
    if (result && result.success) {
        const data = result.data;
        console.log(`✅ 좌석 ${data.seatNumber}번 퇴실 완료!`);
        console.log(`⏱️ 실제 사용시간: ${data.usageTime}분`);
    }
    
    return result;
}

// 4. 내 예약 조회 테스트
async function testMyReservation() {
    console.log('\n👤 === 내 예약 조회 테스트 ===');
    const result = await callAPI('/my-reservation', {});
    
    if (result && result.success) {
        if (result.data) {
            const data = result.data;
            console.log(`🪑 현재 예약 좌석: ${data.seatNumber}번`);
            console.log(`⏰ 종료예정: ${data.endTime}`);
            console.log(`⏳ 남은시간: ${data.remainingMinutes}분`);
        } else {
            console.log('📝 현재 예약된 좌석이 없습니다.');
        }
    }
    
    return result;
}

// ===================================================================
// 통합 테스트 시나리오
// ===================================================================

// 전체 시나리오 테스트 (좌석번호 필요)
async function testFullScenario(seatNumber = 1) {
    console.log('\n🎯 === 전체 시나리오 테스트 시작 ===');
    console.log(`테스트 좌석: ${seatNumber}번`);
    
    try {
        // 1. 현황 조회
        console.log('\n1️⃣ 현황 조회 중...');
        await testGetStatus();
        await sleep(1000);

        // 2. 내 예약 확인
        console.log('\n2️⃣ 내 예약 확인 중...');
        await testMyReservation();
        await sleep(1000);

        // 3. 좌석 신청
        console.log('\n3️⃣ 좌석 신청 중...');
        await testReserveSeat(seatNumber);
        await sleep(1000);

        // 4. 신청 후 현황 확인
        console.log('\n4️⃣ 신청 후 현황 확인 중...');
        await testGetStatus();
        await sleep(1000);

        // 5. 내 예약 재확인
        console.log('\n5️⃣ 내 예약 재확인 중...');
        await testMyReservation();
        await sleep(1000);

        // 6. 퇴실 처리
        console.log('\n6️⃣ 퇴실 처리 중...');
        await testCheckout(seatNumber);
        await sleep(1000);

        // 7. 퇴실 후 현황 확인
        console.log('\n7️⃣ 퇴실 후 현황 확인 중...');
        await testGetStatus();

        console.log('\n🎉 === 전체 시나리오 테스트 완료 ===');

    } catch (error) {
        console.error('💥 시나리오 테스트 중 오류:', error);
    }
}

// 빠른 상태 확인
async function quickCheck() {
    console.log('\n⚡ === 빠른 상태 확인 ===');
    
    const [status, myReservation] = await Promise.all([
        callAPI('/status', {}),
        callAPI('/my-reservation', {})
    ]);

    if (status && status.success) {
        const data = status.data;
        console.log(`📊 현재 상황: ${data.occupiedSeats}/${data.totalSeats} 사용중 (${data.occupancyRate}%)`);
    }

    if (myReservation && myReservation.success) {
        if (myReservation.data) {
            console.log(`🪑 내 좌석: ${myReservation.data.seatNumber}번 (${myReservation.data.remainingMinutes}분 남음)`);
        } else {
            console.log('📝 예약 없음');
        }
    }
}

// 유틸리티 함수
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// ===================================================================
// 사용 가이드
// ===================================================================

console.log(`
🚀 Blue-Crab LMS 열람실 API 테스트 스크립트 로드 완료!

📝 사용법:
1. setToken('your_jwt_token_here')  # JWT 토큰 설정
2. testGetStatus()                  # 현황 조회
3. testReserveSeat(15)             # 15번 좌석 신청
4. testCheckout(15)                # 15번 좌석 퇴실
5. testMyReservation()             # 내 예약 조회
6. testFullScenario(15)            # 전체 시나리오 (15번 좌석)
7. quickCheck()                    # 빠른 상태 확인

⚙️ 설정:
- setApiUrl('http://localhost:8080/api/reading-room')  # API URL 변경

🎯 추천 순서:
1. setToken('토큰')
2. quickCheck()
3. testReserveSeat(원하는좌석번호)
4. testCheckout(같은좌석번호)

⚠️ 주의: 실제 데이터에 영향을 주므로 테스트용 계정으로만 사용하세요!
`);