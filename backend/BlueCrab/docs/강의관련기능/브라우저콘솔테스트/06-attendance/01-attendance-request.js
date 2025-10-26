// 출석 요청 API 테스트 스크립트
// 브라우저 콘솔에서 실행 (학생 권한 필요)

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// JWT 토큰 가져오기 (우선순위: window.authToken > localStorage > sessionStorage)
const getToken = () => {
    return window.authToken || 
           localStorage.getItem('accessToken') || 
           sessionStorage.getItem('accessToken');
};

// 출석 요청 테스트
async function testAttendanceRequest() {
    const token = getToken();
    
    if (!token) {
        console.error('❌ 토큰이 없습니다. 먼저 로그인하세요.');
        return;
    }

    // 사용자 입력 받기 (studentIdx는 JWT에서 자동 추출됨)
    const lecSerial = prompt('강의 코드 (LEC_SERIAL)를 입력하세요:', 'ETH201');
    const sessionNumber = parseInt(prompt('회차 번호 (1~80)를 입력하세요:', '1'));
    const requestReason = prompt('요청 사유를 입력하세요 (선택사항, 취소 가능):', '교통체증으로 지각');

    if (!lecSerial || !sessionNumber) {
        console.error('❌ 강의 코드와 회차 번호는 필수입니다.');
        return;
    }

    const requestData = {
        lecSerial: lecSerial,
        sessionNumber: sessionNumber
    };
    
    // 요청 사유가 있으면 추가 (선택사항)
    if (requestReason && requestReason.trim() !== '') {
        requestData.requestReason = requestReason;
    }

    console.log('📤 출석 요청 전송 중...', requestData);
    console.log('ℹ️  studentIdx는 JWT 토큰에서 자동으로 추출됩니다.');

    try {
        const response = await fetch(`${API_BASE_URL}/api/attendance/request`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(requestData)
        });

        const data = await response.json();

        if (response.ok) {
            console.log('✅ 출석 요청 성공!');
            console.log('응답 데이터:', data);
            console.log('---');
            if (data.data) {
                console.log('📊 출석 통계:');
                console.log('  - 출석:', data.data.summary.attended);
                console.log('  - 지각:', data.data.summary.late);
                console.log('  - 결석:', data.data.summary.absent);
                console.log('  - 출석률:', data.data.summary.attendanceRate + '%');
                console.log('---');
                console.log('⏳ 대기 중인 요청:', data.data.pendingRequests.length + '건');
            }
        } else {
            console.error('❌ 출석 요청 실패:', response.status);
            console.error('에러 메시지:', data);
        }
    } catch (error) {
        console.error('❌ 네트워크 오류:', error);
    }
}

// 실행
console.log('🚀 출석 요청 테스트 시작');
console.log('ℹ️  이 API는 학생이 자신의 출석을 요청하는 API입니다.');
console.log('ℹ️  studentIdx는 JWT 토큰에서 자동으로 추출됩니다.');
console.log('ℹ️  lecSerial은 서버에서 LEC_TBL과 조인하여 LEC_IDX로 변환됩니다.');
console.log('📊 DB 처리 흐름: LEC_SERIAL → LEC_TBL.LEC_IDX → ENROLLMENT_EXTENDED_TBL 조회');
console.log('---');
testAttendanceRequest();
