// ===================================================================
// 🧪 출석-성적 통합 테스트 (심플 한방 테스트)
// Blue Crab LMS - 출석 승인 시 성적 자동 재계산 검증
// 
// 📍 위치: docs/강의관련기능/브라우저콘솔테스트/07-integration-test/
// 🎯 목적: @Transactional 수정 후 afterCommit 콜백 실행 확인
// ⚡ 특징: 전체 플로우를 한 번에 자동으로 실행하는 심플한 테스트
// ===================================================================

/**
 * 🚀 심플 한방 사용법:
 * 
 * 1. 브라우저 F12 → Console 탭
 * 2. 이 스크립트 전체 복사/붙여넣기
 * 3. 로그인 2번:
 *    await loginStudent()     // 학생 계정
 *    await loginProfessor()   // 교수 계정
 * 4. 테스트 실행:
 *    await runFullIntegrationTest()
 * 
 * ✅ 검증 항목:
 * - 출석 요청 → 승인 → 성적 재계산 전체 플로우
 * - 백엔드 "성적 재계산" 로그 출력 확인
 * - DB 성적 데이터 실제 변경 확인
 * 
 * ⚠️ 사전 준비:
 * - 백엔드 실행 중
 * - 테스트용 강의/학생/교수 데이터 존재
 */

// ===================================================================
// 기본 설정
// ===================================================================

const API_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// 전역 변수
if (typeof window.authToken === 'undefined') window.authToken = null;
if (typeof window.currentUser === 'undefined') window.currentUser = null;
if (typeof window.professorToken === 'undefined') window.professorToken = null;

// 테스트 결과 저장
const testResults = {
    steps: [],
    startTime: null,
    endTime: null,
    success: false,
    error: null
};

// ===================================================================
// 유틸리티 함수
// ===================================================================

// 로그 출력 헬퍼
function logStep(emoji, title, data = null) {
    const timestamp = new Date().toLocaleTimeString();
    console.log(`\n${emoji} [${timestamp}] ${title}`);
    console.log('═'.repeat(70));
    if (data) {
        console.log(data);
    }
    testResults.steps.push({ emoji, title, timestamp, data });
}

// API 호출 헬퍼
async function apiCall(endpoint, method = 'POST', data = null, token = null) {
    const useToken = token || window.authToken;
    
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    };

    if (useToken) {
        options.headers['Authorization'] = `Bearer ${useToken}`;
    }

    if (data && (method === 'POST' || method === 'PUT')) {
        options.body = JSON.stringify(data);
    }

    const url = `${API_BASE}${endpoint}`;
    console.log(`📡 ${method} ${url}`);
    if (data) console.log('📤 Request:', data);

    const response = await fetch(url, options);
    const result = await response.json();
    
    console.log(`📊 Status: ${response.status}`);
    console.log('📥 Response:', result);

    return { status: response.status, data: result, ok: response.ok };
}

// 대기 함수
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// ===================================================================
// 로그인 함수
// ===================================================================

// 학생 로그인
async function loginStudent() {
    logStep('🔐', '학생 계정 로그인');
    
    const email = prompt('학생 이메일:', '123joon@naver.com');
    const password = prompt('비밀번호:', 'Bluecrab256@');
    
    if (!email || !password) {
        console.log('❌ 로그인 취소됨');
        return false;
    }

    const result = await apiCall('/api/auth/login', 'POST', {
        username: email,
        password: password
    });

    if (result.ok && result.data.data) {
        window.authToken = result.data.data.accessToken;
        window.currentUser = result.data.data.user;
        
        console.log('✅ 학생 로그인 성공!');
        console.log('👤 사용자:', window.currentUser);
        return true;
    }

    console.log('❌ 학생 로그인 실패');
    return false;
}

// 교수 로그인
async function loginProfessor() {
    logStep('🔐', '교수 계정 로그인');
    
    const email = prompt('교수 이메일:', 'prof.octopus@univ.edu');
    const password = prompt('비밀번호:', 'Bluecrab256@');
    
    if (!email || !password) {
        console.log('❌ 로그인 취소됨');
        return false;
    }

    const result = await apiCall('/api/auth/login', 'POST', {
        username: email,
        password: password
    });

    if (result.ok && result.data.data) {
        window.professorToken = result.data.data.accessToken;
        
        console.log('✅ 교수 로그인 성공!');
        console.log('👤 교수 정보:', result.data.data.user);
        return true;
    }

    console.log('❌ 교수 로그인 실패');
    return false;
}

// ===================================================================
// 1단계: 초기 상태 확인
// ===================================================================

async function checkInitialState(lecSerial, studentIdx) {
    logStep('📊', '1단계: 초기 성적 상태 확인');
    
    // 학생 출석 현황 조회
    const attendanceResult = await apiCall('/api/attendance/student/view', 'POST', {
        lecSerial: lecSerial
    });

    if (!attendanceResult.ok) {
        throw new Error('출석 현황 조회 실패');
    }

    const attendanceData = attendanceResult.data.data;
    console.log('📋 출석 현황:', attendanceData.summary);

    // 성적 조회 (학생용 API)
    const gradeResult = await apiCall('/api/enrollments/grade-info', 'POST', {
        action: 'get-grade',
        lecSerial: lecSerial,
        studentIdx: studentIdx
    });

    if (!gradeResult.ok) {
        console.log('⚠️ 성적 조회 실패 (아직 생성 안되었을 수 있음)');
        return {
            attendance: attendanceData.summary,
            grade: null
        };
    }

    const gradeData = gradeResult.data.data;
    console.log('📊 현재 성적 (전체):', gradeData);
    
    // API 응답 구조: { grade: { attendance: {...}, assignments: [...], total: {...} } }
    const gradeInfo = gradeData.grade || {};
    console.log('📊 현재 성적 (요약):', {
        attendance: gradeInfo.attendance,
        assignments: gradeInfo.assignments,
        total: gradeInfo.total
    });

    return {
        attendance: attendanceData.summary,
        grade: gradeInfo,  // grade 객체만 저장
        rawGradeData: gradeData  // 디버깅용
    };
}

// ===================================================================
// 2단계: 출석 요청 생성
// ===================================================================

async function createAttendanceRequest(lecSerial, sessionNumber, reason = '교통체증으로 지각') {
    logStep('📝', '2단계: 출석 요청 생성', {
        lecSerial: lecSerial,
        sessionNumber: sessionNumber,
        reason: reason
    });

    const result = await apiCall('/api/attendance/request', 'POST', {
        lecSerial: lecSerial,
        sessionNumber: sessionNumber,
        requestReason: reason
    });

    if (!result.ok) {
        throw new Error('출석 요청 생성 실패: ' + JSON.stringify(result.data));
    }

    console.log('✅ 출석 요청 생성 성공!');
    console.log('📋 요청 데이터:', result.data.data);

    return result.data.data;
}

// ===================================================================
// 3단계: 출석 승인 (교수)
// ===================================================================

async function approveAttendanceRequest(lecSerial, studentIdx, sessionNumber) {
    logStep('✅', '3단계: 출석 승인 (교수)', {
        lecSerial: lecSerial,
        studentIdx: studentIdx,
        sessionNumber: sessionNumber
    });

    // 백엔드 API 스펙: attendanceRecords 배열 형태
    const result = await apiCall('/api/attendance/approve', 'POST', {
        lecSerial: lecSerial,
        sessionNumber: sessionNumber,
        attendanceRecords: [
            {
                studentIdx: studentIdx,
                status: '출'  // '출' (출석), '지' (지각), '결' (결석)
            }
        ]
    }, window.professorToken);

    if (!result.ok) {
        throw new Error('출석 승인 실패: ' + JSON.stringify(result.data));
    }

    console.log('✅ 출석 승인 성공!');
    console.log('📋 승인 결과:', result.data);

    // ⚠️ 중요: 백엔드 로그 확인 요청
    console.log('\n🔍 백엔드 로그 확인하세요:');
    console.log('   - "성적 재계산 시작: lecIdx=X, studentIdx=Y"');
    console.log('   - "성적 재계산 완료: 출석 점수 업데이트됨"');
    console.log('');

    return result.data.data;
}

// ===================================================================
// 4단계: 성적 재확인 (변경 검증)
// ===================================================================

async function verifyGradeUpdate(lecSerial, initialState) {
    logStep('🔍', '4단계: 성적 업데이트 검증');

    // 잠시 대기 (비동기 처리 완료 대기)
    console.log('⏳ 3초 대기 중 (afterCommit 콜백 처리 대기)...');
    await sleep(3000);

    // 성적 재조회
    const gradeResult = await apiCall('/api/enrollments/grade-info', 'POST', {
        action: 'get-grade',
        lecSerial: lecSerial,
        studentIdx: window.currentUser.id
    });

    if (!gradeResult.ok) {
        throw new Error('성적 재조회 실패');
    }

    const updatedGrade = gradeResult.data.data;
    console.log('📊 업데이트된 성적 (전체):', updatedGrade);
    
    // API 응답 구조: { grade: { attendance: {...}, assignments: [...], total: {...} } }
    const gradeInfo = updatedGrade.grade || {};
    console.log('📊 업데이트된 성적 (요약):', {
        attendance: gradeInfo.attendance,
        assignments: gradeInfo.assignments,
        total: gradeInfo.total
    });

    // 변경 사항 비교
    console.log('\n📈 변경 사항 비교:');
    console.log('═'.repeat(70));

    if (initialState.grade) {
        const oldScore = initialState.grade.attendance?.currentScore || 0;
        const newScore = gradeInfo.attendance?.currentScore || 0;
        const diff = newScore - oldScore;

        console.log(`출석 점수: ${oldScore} → ${newScore} (${diff > 0 ? '+' : ''}${diff})`);
        
        if (diff !== 0) {
            console.log('✅ 출석 점수가 변경되었습니다!');
        } else {
            console.log('⚠️ 출석 점수가 변경되지 않았습니다.');
            console.log('💡 DB 상태: currentScore =', oldScore);
        }
    } else {
        console.log(`출석 점수: (없음) → ${gradeInfo.attendance?.currentScore || 0}`);
        console.log('✅ 성적이 새로 생성되었습니다!');
    }

    return {
        updated: gradeInfo,  // grade 객체만 반환
        changed: initialState.grade ? 
            (gradeInfo.attendance?.currentScore || 0) !== (initialState.grade.attendance?.currentScore || 0) : 
            true
    };
}

// ===================================================================
// 🚀 전체 통합 테스트 실행 (심플 한방)
// ===================================================================

async function runFullIntegrationTest() {
    console.clear();
    console.log('╔═══════════════════════════════════════════════════════════════════╗');
    console.log('║     🧪 출석-성적 통합 테스트 (@Transactional 수정 검증)         ║');
    console.log('║                      심플 한방 테스트                            ║');
    console.log('╚═══════════════════════════════════════════════════════════════════╝');
    
    testResults.startTime = new Date();
    testResults.steps = [];
    testResults.success = false;
    testResults.error = null;

    try {
        // 테스트 파라미터 입력
        const lecSerial = prompt('강의 코드 (LEC_SERIAL):', 'ETH201');
        const sessionNumber = parseInt(prompt('회차 번호:', '5'));
        
        if (!lecSerial || !sessionNumber) {
            console.log('❌ 테스트 취소됨');
            return;
        }

        // 로그인 확인
        if (!window.authToken || !window.currentUser) {
            console.log('⚠️ 학생 계정으로 로그인하세요: await loginStudent()');
            return;
        }

        if (!window.professorToken) {
            console.log('⚠️ 교수 계정으로 로그인하세요: await loginProfessor()');
            return;
        }

        const studentIdx = window.currentUser.id;

        console.log('\n📋 테스트 설정:');
        console.log(`   - 강의: ${lecSerial}`);
        console.log(`   - 학생 IDX: ${studentIdx}`);
        console.log(`   - 회차: ${sessionNumber}`);
        console.log('');

        // 1단계: 초기 상태 확인
        const initialState = await checkInitialState(lecSerial, studentIdx);
        
        // 2단계: 출석 요청 생성
        await createAttendanceRequest(lecSerial, sessionNumber, '통합 테스트용 출석 요청');
        
        // 3단계: 출석 승인
        await approveAttendanceRequest(lecSerial, studentIdx, sessionNumber);
        
        // 4단계: 성적 업데이트 검증
        const verifyResult = await verifyGradeUpdate(lecSerial, initialState);
        
        // 최종 결과
        testResults.endTime = new Date();
        testResults.success = verifyResult.changed;
        
        console.log('\n╔═══════════════════════════════════════════════════════════════════╗');
        if (testResults.success) {
            console.log('║                    ✅ 통합 테스트 성공!                          ║');
            console.log('║                                                                   ║');
            console.log('║  출석 승인 시 성적 자동 재계산이 정상 작동합니다.                ║');
            console.log('║  @Transactional 수정이 효과적으로 적용되었습니다.                ║');
        } else {
            console.log('║                    ⚠️  통합 테스트 실패                          ║');
            console.log('║                                                                   ║');
            console.log('║  출석 승인 후 성적이 업데이트되지 않았습니다.                    ║');
            console.log('║  백엔드 로그를 확인하여 "성적 재계산" 메시지를 찾으세요.         ║');
        }
        console.log('╚═══════════════════════════════════════════════════════════════════╝');

        const duration = (testResults.endTime - testResults.startTime) / 1000;
        console.log(`\n⏱️  총 소요 시간: ${duration.toFixed(2)}초`);
        console.log(`📊 실행 단계: ${testResults.steps.length}개`);

    } catch (error) {
        testResults.error = error.message;
        testResults.endTime = new Date();
        
        console.log('\n╔═══════════════════════════════════════════════════════════════════╗');
        console.log('║                    ❌ 통합 테스트 오류                            ║');
        console.log('╚═══════════════════════════════════════════════════════════════════╝');
        console.error('💥 에러:', error);
    }
}

// ===================================================================
// 🔍 개별 테스트 함수 (디버깅용)
// ===================================================================

// 출석 현황만 조회
async function quickCheckAttendance(lecSerial) {
    console.log('\n📊 출석 현황 조회');
    console.log('═'.repeat(70));
    
    const result = await apiCall('/api/attendance/student/view', 'POST', {
        lecSerial: lecSerial
    });

    if (result.ok) {
        console.log('✅ 조회 성공!');
        console.table(result.data.data.summary);
        return result.data.data;
    } else {
        console.log('❌ 조회 실패');
        return null;
    }
}

// 성적만 조회
async function quickCheckGrade(lecSerial) {
    console.log('\n📊 성적 조회');
    console.log('═'.repeat(70));
    
    const result = await apiCall('/api/enrollments/grade-info', 'POST', {
        action: 'get-grade',
        lecSerial: lecSerial,
        studentIdx: window.currentUser.id
    });

    if (result.ok) {
        console.log('✅ 조회 성공!');
        const gradeInfo = result.data.data.grade || {};
        console.log('출석:', gradeInfo.attendance);
        console.log('과제:', gradeInfo.assignments);
        console.log('총점:', gradeInfo.total);
        return result.data.data;
    } else {
        console.log('❌ 조회 실패');
        return null;
    }
}

// 교수용 출석 현황 조회
async function professorCheckAttendance(lecSerial) {
    console.log('\n👨‍🏫 교수용 출석 현황 조회');
    console.log('═'.repeat(70));
    
    const result = await apiCall('/api/attendance/professor/view', 'POST', {
        lecSerial: lecSerial
    }, window.professorToken);

    if (result.ok) {
        console.log('✅ 조회 성공!');
        console.log(`총 ${result.data.data.length}명의 학생`);
        console.table(result.data.data.map(s => ({
            이름: s.studentName,
            학번: s.studentCode,
            출석: s.attendanceData?.summary?.attended || 0,
            지각: s.attendanceData?.summary?.late || 0,
            결석: s.attendanceData?.summary?.absent || 0,
            출석률: (s.attendanceData?.summary?.attendanceRate || 0) + '%'
        })));
        return result.data.data;
    } else {
        console.log('❌ 조회 실패');
        return null;
    }
}

// ===================================================================
// 초기 안내 메시지
// ===================================================================

console.log('╔═══════════════════════════════════════════════════════════════════╗');
console.log('║        🧪 출석-성적 통합 테스트 스크립트 로드 완료!            ║');
console.log('║                  심플 한방 테스트 전용                           ║');
console.log('╚═══════════════════════════════════════════════════════════════════╝');
console.log('\n📍 위치: docs/강의관련기능/브라우저콘솔테스트/07-integration-test/');
console.log('🎯 목적: @Transactional 수정 후 성적 자동 재계산 검증\n');
console.log('═'.repeat(70));
console.log('\n⚡ 심플 사용법:\n');
console.log('1️⃣  await loginStudent()           // 학생 로그인');
console.log('2️⃣  await loginProfessor()         // 교수 로그인');
console.log('3️⃣  await runFullIntegrationTest() // 한방 테스트 실행');
console.log('');
console.log('═'.repeat(70));
console.log('\n🔍 개별 디버깅 함수:\n');
console.log('   await quickCheckAttendance("ETH201")     - 출석 현황만 조회');
console.log('   await quickCheckGrade("ETH201")          - 성적만 조회');
console.log('   await professorCheckAttendance("ETH201") - 교수용 출석 조회');
console.log('');
console.log('═'.repeat(70));
console.log('');
