// 출석 승인 API 테스트 스크립트
// 브라우저 콘솔에서 실행 (교수 또는 관리자 권한 필요)

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// JWT 토큰 가져오기 (우선순위: window.authToken > localStorage > sessionStorage)
const getToken = () => {
    return window.authToken || 
           localStorage.getItem('accessToken') || 
           sessionStorage.getItem('accessToken');
};

// 학생 목록 조회 함수
async function viewStudentList(lecSerial) {
    const token = getToken();
    
    if (!token) {
        console.error('❌ 토큰이 없습니다. 먼저 로그인하세요.');
        return null;
    }

    console.log('\n📋 학생 목록 조회 중...');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/attendance/professor/view`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ lecSerial: lecSerial })
        });

        const result = await response.json();

        if (response.ok && result.data) {
            console.log(`✅ 총 ${result.data.length}명의 학생이 등록되어 있습니다.\n`);
            
            // 🔍 디버깅: 전체 응답 데이터 출력
            console.log('📊 응답 데이터 구조:');
            console.log(JSON.stringify(result.data, null, 2));
            
            if (result.data.length > 0) {
                console.log('\n📊 첫 번째 학생의 필드 구조:');
                console.log('사용 가능한 필드:', Object.keys(result.data[0]));
            }
            
            // 학생 목록 테이블 형태로 출력
            console.table(
                result.data.map(student => ({
                    'USER_IDX': student.studentIdx,
                    '학번': student.studentCode,
                    '이름': student.studentName,
                    '출석': student.attendanceData?.summary?.attended || 0,
                    '지각': student.attendanceData?.summary?.late || 0,
                    '결석': student.attendanceData?.summary?.absent || 0,
                    '출석률': (student.attendanceData?.summary?.attendanceRate || 0) + '%',
                    '대기요청': student.attendanceData?.pendingRequests?.length || 0
                }))
            );
            
            // 대기 중인 요청이 있는 학생 표시
            const studentsWithPending = result.data.filter(s => s.attendanceData?.pendingRequests?.length > 0);
            if (studentsWithPending.length > 0) {
                console.log('\n⏳ 대기 중인 출석 요청이 있는 학생:');
                studentsWithPending.forEach(student => {
                    console.log(`\n👤 ${student.studentName} (${student.studentCode}) - USER_IDX: ${student.studentIdx}`);
                    student.attendanceData.pendingRequests.forEach(req => {
                        console.log(`  - ${req.sessionNumber}회차: ${req.requestDate} (만료: ${req.expiresAt})`);
                        if (req.requestReason) {
                            console.log(`    사유: ${req.requestReason}`);
                        }
                    });
                });
            }
            
            return result.data;
        } else {
            console.error('❌ 학생 목록 조회 실패:', response.status);
            console.error('에러 메시지:', result);
            return null;
        }
    } catch (error) {
        console.error('❌ 네트워크 오류:', error);
        return null;
    }
}

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

    // 학생 목록 먼저 조회
    const viewList = confirm('학생 목록을 먼저 확인하시겠습니까?');
    if (viewList) {
        const students = await viewStudentList(lecSerial);
        if (!students) {
            console.error('❌ 학생 목록 조회에 실패했습니다. 계속 진행할 수 없습니다.');
            return;
        }
        
        const continueApprove = confirm('\n출석 승인을 계속 진행하시겠습니까?');
        if (!continueApprove) {
            console.log('ℹ️  출석 승인을 취소했습니다.');
            return;
        }
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
console.log('💡 사용 가능한 함수:');
console.log('  - testAttendanceApprove()     : 출석 승인 (학생 목록 확인 옵션 포함)');
console.log('  - viewStudentList(lecSerial)  : 학생 목록만 조회');
console.log('---');
testAttendanceApprove();
