// 출석 승인 API 테스트 스크립트
// 브라우저 콘솔에서 실행 (교수 또는 관리자 권한 필요)

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// JWT 토큰 가져오기 (localStorage 또는 sessionStorage에서)
const getToken = () => {
    return localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
};

// 출석 승인 테스트
async function testAttendanceApprove() {
    const token = getToken();
    
    if (!token) {
        console.error('❌ 토큰이 없습니다. 먼저 로그인하세요.');
        return;
    }

    // 사용자 입력 받기
    const lecSerial = prompt('강의 코드 (LEC_SERIAL)를 입력하세요:', 'CS101');
    const sessionNumber = parseInt(prompt('회차 번호 (1~80)를 입력하세요:', '1'));
    
    if (!lecSerial || !sessionNumber) {
        console.error('❌ 강의 코드와 회차 번호를 입력해야 합니다.');
        return;
    }

    // 출석 기록 입력 받기
    const recordCount = parseInt(prompt('처리할 학생 수를 입력하세요:', '3'));
    const attendanceRecords = [];
    
    for (let i = 0; i < recordCount; i++) {
        const studentIdx = parseInt(prompt(`${i + 1}번째 학생 USER_IDX를 입력하세요:`, `${6 + i}`));
        const status = prompt(`${i + 1}번째 학생 출석 상태를 입력하세요:\n출: 출석\n지: 지각\n결: 결석`, '출');
        
        if (studentIdx && status) {
            attendanceRecords.push({
                studentIdx: studentIdx,
                status: status
            });
        }
    }

    if (attendanceRecords.length === 0) {
        console.error('❌ 최소 1명 이상의 출석 기록이 필요합니다.');
        return;
    }

    const requestData = {
        lecSerial: lecSerial,
        sessionNumber: sessionNumber,
        attendanceRecords: attendanceRecords
    };

    console.log('📤 출석 승인 요청 전송 중...', requestData);
    console.log('ℹ️  professorIdx는 JWT 토큰에서 자동으로 추출됩니다.');

    try {
        const response = await fetch(`${API_BASE_URL}/api/attendance/approve`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(requestData)
        });

        const data = await response.json();

        if (response.ok) {
            console.log('✅ 출석 승인 성공!');
            console.log('응답 데이터:', data);
            console.log('---');
            console.log('메시지:', data.message);
            console.log('성공 여부:', data.success);
        } else {
            console.error('❌ 출석 승인 실패:', response.status);
            console.error('에러 메시지:', data);
        }
    } catch (error) {
        console.error('❌ 네트워크 오류:', error);
    }
}

// 실행
console.log('🚀 출석 승인 테스트 시작');
console.log('ℹ️  이 API는 교수가 학생들의 출석 요청을 승인하는 API입니다.');
console.log('ℹ️  professorIdx는 JWT 토큰에서 자동으로 추출됩니다.');
console.log('ℹ️  lecSerial은 서버에서 LEC_TBL과 조인하여 LEC_IDX로 변환됩니다.');
console.log('📊 DB 처리 흐름: LEC_SERIAL → LEC_TBL.LEC_IDX → ENROLLMENT_EXTENDED_TBL 업데이트');
console.log('⚠️  출석 상태 입력 시 "출", "지", "결"을 정확히 입력하세요.');
console.log('---');
testAttendanceApprove();
