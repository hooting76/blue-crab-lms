// 학생 출석 조회 API 테스트 스크립트
// 브라우저 콘솔에서 실행 (학생 권한 필요)

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// JWT 토큰 가져오기 (localStorage 또는 sessionStorage에서)
const getToken = () => {
    return localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
};

// 학생 출석 조회 테스트
async function testStudentAttendanceView() {
    const token = getToken();
    
    if (!token) {
        console.error('❌ 토큰이 없습니다. 먼저 로그인하세요.');
        return;
    }

    // 사용자 입력 받기
    const lecSerial = prompt('강의 코드 (LEC_SERIAL)를 입력하세요:', 'CS101');
    
    if (!lecSerial) {
        console.error('❌ 강의 코드를 입력해야 합니다.');
        return;
    }

    const requestData = {
        lecSerial: lecSerial
    };

    console.log('📤 학생 출석 조회 요청 전송 중...', requestData);
    console.log('ℹ️  studentIdx는 JWT 토큰에서 자동으로 추출됩니다.');
    console.log('ℹ️  lecSerial은 서버에서 LEC_TBL과 조인하여 LEC_IDX로 변환됩니다.');
    console.log('ℹ️  내부적으로: LEC_SERIAL → LEC_IDX → ENROLLMENT_EXTENDED_TBL 조회');
    console.log('---');

    try {
        const response = await fetch(`${API_BASE_URL}/api/attendance/student/view`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(requestData)
        });

        const data = await response.json();

        if (response.ok) {
            console.log('✅ 학생 출석 조회 성공!');
            console.log('응답 데이터:', data);
            console.log('---');
            
            if (data.data) {
                console.log('📊 출석 통계:');
                console.log('  - 출석:', data.data.summary.attended, '회');
                console.log('  - 지각:', data.data.summary.late, '회');
                console.log('  - 결석:', data.data.summary.absent, '회');
                console.log('  - 출석률:', data.data.summary.attendanceRate, '%');
                console.log('---');
                
                console.log('📅 회차별 출석 기록:');
                if (data.data.sessions && data.data.sessions.length > 0) {
                    data.data.sessions.forEach(session => {
                        const statusEmoji = session.status === '출' ? '✅' : 
                                          session.status === '지' ? '⏰' : '❌';
                        console.log(`  ${statusEmoji} ${session.sessionNumber}회차: ${session.status} (${session.recordedAt})`);
                    });
                } else {
                    console.log('  기록된 출석 내역이 없습니다.');
                }
                console.log('---');
                
                console.log('⏳ 대기 중인 요청:');
                if (data.data.pendingRequests && data.data.pendingRequests.length > 0) {
                    data.data.pendingRequests.forEach(req => {
                        console.log(`  - ${req.sessionNumber}회차: 요청일 ${req.requestedAt}, 만료일 ${req.expiresAt}`);
                        if (req.requestReason) {
                            console.log(`    사유: ${req.requestReason}`);
                        }
                    });
                } else {
                    console.log('  대기 중인 요청이 없습니다.');
                }
            }
        } else {
            console.error('❌ 학생 출석 조회 실패:', response.status);
            console.error('에러 메시지:', data);
        }
    } catch (error) {
        console.error('❌ 네트워크 오류:', error);
    }
}

// 실행
console.log('🚀 학생 출석 조회 테스트 시작');
console.log('ℹ️  이 API는 학생이 자신의 출석 현황을 조회하는 API입니다.');
console.log('ℹ️  studentIdx는 JWT 토큰에서 자동으로 추출됩니다.');
console.log('---');
testStudentAttendanceView();
