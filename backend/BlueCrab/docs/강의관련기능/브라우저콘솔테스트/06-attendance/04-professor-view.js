// 교수 출석 조회 API 테스트 스크립트
// 브라우저 콘솔에서 실행 (교수 또는 관리자 권한 필요)

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// JWT 토큰 가져오기 (우선순위: window.authToken > localStorage > sessionStorage)
const getToken = () => {
    return window.authToken || 
           localStorage.getItem('accessToken') || 
           sessionStorage.getItem('accessToken');
};

// 교수 출석 조회 테스트
async function testProfessorAttendanceView() {
    const token = getToken();
    
    if (!token) {
        console.error('❌ 토큰이 없습니다. 먼저 로그인하세요.');
        return;
    }

    // 사용자 입력 받기
    const lecSerial = prompt('강의 코드 (LEC_SERIAL)를 입력하세요:', 'ETH201');
    
    if (!lecSerial) {
        console.error('❌ 강의 코드를 입력해야 합니다.');
        return;
    }

    const requestData = {
        lecSerial: lecSerial
    };

    console.log('📤 교수 출석 조회 요청 전송 중...', requestData);
    console.log('ℹ️  professorIdx는 JWT 토큰에서 자동으로 추출됩니다.');
    console.log('ℹ️  lecSerial은 서버에서 LEC_TBL과 조인하여 LEC_IDX로 변환됩니다.');
    console.log('ℹ️  내부적으로: LEC_SERIAL → LEC_IDX → ENROLLMENT_EXTENDED_TBL 조회');
    console.log('---');

    try {
        const response = await fetch(`${API_BASE_URL}/api/attendance/professor/view`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(requestData)
        });

        const data = await response.json();

        if (response.ok) {
            console.log('✅ 교수 출석 조회 성공!');
            console.log('응답 데이터:', data);
            console.log('---');
            
            if (data.data && data.data.length > 0) {
                console.log(`📚 총 ${data.data.length}명의 학생 출석 현황:`);
                console.log('---');
                
                data.data.forEach((student, index) => {
                    console.log(`👤 ${index + 1}. ${student.studentName} (${student.studentCode}) - USER_IDX: ${student.studentIdx}`);
                    console.log('  📊 출석 통계:');
                    console.log(`    - 출석: ${student.attendanceData?.summary?.attended || 0}회`);
                    console.log(`    - 지각: ${student.attendanceData?.summary?.late || 0}회`);
                    console.log(`    - 결석: ${student.attendanceData?.summary?.absent || 0}회`);
                    console.log(`    - 출석률: ${student.attendanceData?.summary?.attendanceRate || 0}%`);
                    
                    // 최근 5개 출석 기록만 표시
                    if (student.attendanceData?.sessions && student.attendanceData.sessions.length > 0) {
                        console.log('  📅 최근 출석 기록 (최대 5개):');
                        student.attendanceData.sessions.slice(-5).forEach(session => {
                            const statusEmoji = session.status === '출' ? '✅' : 
                                              session.status === '지' ? '⏰' : '❌';
                            console.log(`    ${statusEmoji} ${session.sessionNumber}회차: ${session.status} (${session.approvedDate || session.recordedAt})`);
                        });
                    }
                    
                    // 대기 중인 요청 표시
                    if (student.attendanceData?.pendingRequests && student.attendanceData.pendingRequests.length > 0) {
                        console.log(`  ⏳ 대기 중인 요청: ${student.attendanceData.pendingRequests.length}건`);
                        student.attendanceData.pendingRequests.forEach(req => {
                            console.log(`    - ${req.sessionNumber}회차: 요청일 ${req.requestDate}, 만료일 ${req.expiresAt}`);
                            if (req.requestReason) {
                                console.log(`      사유: ${req.requestReason}`);
                            }
                        });
                    }
                    
                    console.log('---');
                });
                
                // 전체 통계 계산
                const totalAttended = data.data.reduce((sum, s) => sum + (s.attendanceData?.summary?.attended || 0), 0);
                const totalLate = data.data.reduce((sum, s) => sum + (s.attendanceData?.summary?.late || 0), 0);
                const totalAbsent = data.data.reduce((sum, s) => sum + (s.attendanceData?.summary?.absent || 0), 0);
                const totalPending = data.data.reduce((sum, s) => sum + (s.attendanceData?.pendingRequests?.length || 0), 0);
                
                console.log('📈 전체 통계:');
                console.log(`  - 전체 출석: ${totalAttended}회`);
                console.log(`  - 전체 지각: ${totalLate}회`);
                console.log(`  - 전체 결석: ${totalAbsent}회`);
                console.log(`  - 대기 중인 요청: ${totalPending}건`);
            } else {
                console.log('ℹ️  등록된 학생이 없거나 출석 기록이 없습니다.');
            }
        } else {
            console.error('❌ 교수 출석 조회 실패:', response.status);
            console.error('에러 메시지:', data);
        }
    } catch (error) {
        console.error('❌ 네트워크 오류:', error);
    }
}

// 실행
console.log('🚀 교수 출석 조회 테스트 시작');
console.log('ℹ️  이 API는 교수가 강의에 등록된 모든 학생의 출석 현황을 조회하는 API입니다.');
console.log('ℹ️  professorIdx는 JWT 토큰에서 자동으로 추출되며, 해당 교수의 강의인지 검증합니다.');
console.log('---');
testProfessorAttendanceView();
