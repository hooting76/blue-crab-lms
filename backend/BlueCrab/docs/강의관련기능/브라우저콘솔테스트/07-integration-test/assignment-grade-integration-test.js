// ===================================================================
// 🧪 과제-성적 통합 테스트 (심플 한방 테스트)
// Blue Crab LMS - 과제 채점 시 성적 자동 재계산 검증
// 
// 📍 위치: docs/강의관련기능/브라우저콘솔테스트/07-integration-test/
// 🎯 목적: 과제 채점 후 성적 자동 재계산 확인
// ⚡ 특징: 전체 플로우를 한 번에 자동으로 실행하는 심플한 테스트
// 
// 📋 시스템 플로우:
// 1. 교수가 과제 생성 (오프라인 제출용)
// 2. 학생이 오프라인으로 과제 제출 (시스템 외부)
// 3. 교수가 과제 채점 (평가 문구 + 점수)
// 4. 성적 자동 재계산 (@Transactional 검증)
// ===================================================================

/**
 * 🚀 심플 한방 사용법:
 * 
 * 1. 브라우저 F12 → Console 탭
 * 2. 이 스크립트 전체 복사/붙여넣기
 * 3. 로그인:
 *    await loginProfessor()   // 교수 계정
 * 4. 테스트 실행:
 *    await runAssignmentGradeTest()
 * 
 * ✅ 검증 항목:
 * - 과제 생성 → 채점 → 성적 재계산 전체 플로우
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
if (typeof window.professorToken === 'undefined') window.professorToken = null;
if (typeof window.currentProfessor === 'undefined') window.currentProfessor = null;

// 테스트 결과 저장
const assignmentTestResults = {
    steps: [],
    startTime: null,
    endTime: null,
    success: false,
    error: null,
    createdAssignmentIdx: null
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
    assignmentTestResults.steps.push({ emoji, title, timestamp, data });
}

// API 호출 헬퍼
async function apiCall(endpoint, method = 'POST', data = null, token = null) {
    const useToken = token || window.professorToken;
    
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
        window.currentProfessor = result.data.data.user;
        
        console.log('✅ 교수 로그인 성공!');
        console.log('👤 교수 정보:', window.currentProfessor);
        return true;
    }

    console.log('❌ 교수 로그인 실패');
    return false;
}

// ===================================================================
// 1단계: 학생의 초기 성적 확인
// ===================================================================

async function checkInitialGrade(lecSerial, studentIdx) {
    logStep('📊', '1단계: 학생 초기 성적 확인');
    
    // 성적 조회 API (교수가 학생 성적 조회)
    const gradeResult = await apiCall('/api/enrollments/grade-info', 'POST', {
        action: 'get-grade',
        lecSerial: lecSerial,
        studentIdx: studentIdx
    });

    if (!gradeResult.ok) {
        console.log('⚠️ 성적 조회 실패 (아직 생성 안되었을 수 있음)');
        return { grade: null };
    }

    const gradeData = gradeResult.data.data;
    console.log('📊 현재 성적 (전체):', gradeData);
    
    const gradeInfo = gradeData.grade || {};
    console.log('📊 현재 성적 (요약):', {
        attendance: gradeInfo.attendance,
        assignments: gradeInfo.assignments,
        total: gradeInfo.total
    });

    return { grade: gradeInfo, rawGradeData: gradeData };
}

// ===================================================================
// 2단계: 과제 생성
// ===================================================================

async function createAssignment(lecSerial) {
    logStep('📝', '2단계: 과제 생성 (오프라인 제출용)');

    const title = prompt('과제 제목:', '통합 테스트용 과제');
    const description = prompt('과제 설명:', '과제-성적 통합 테스트용 과제입니다.');
    const dueDate = prompt('마감일 (YYYYMMDD):', '20251231');
    const maxScore = parseInt(prompt('만점:', '10'));

    if (!title || !dueDate) {
        throw new Error('과제 정보 입력이 취소되었습니다.');
    }

    // ✅ 백엔드 API: POST /api/assignments
    // maxScore는 입력값 사용 (기본값 10점)
    const result = await apiCall('/api/assignments', 'POST', {
        lecSerial: lecSerial,
        title: title,
        body: description,
        dueDate: dueDate,
        maxScore: maxScore
    });

    if (!result.ok) {
        throw new Error('과제 생성 실패: ' + JSON.stringify(result.data));
    }

    console.log('✅ 과제 생성 성공!');
    console.log('📋 과제 데이터:', result.data);

    const assignmentIdx = result.data.assignmentIdx || result.data.ASSIGNMENT_IDX;
    assignmentTestResults.createdAssignmentIdx = assignmentIdx;
    
    return { assignmentIdx, assignmentData: result.data };
}

// ===================================================================
// 3단계: 과제 채점
// ===================================================================

async function gradeAssignment(assignmentIdx, studentIdx) {
    logStep('✅', '3단계: 과제 채점 (교수)', {
        assignmentIdx: assignmentIdx,
        studentIdx: studentIdx
    });

    const score = parseInt(prompt('부여할 점수:', '9'));
    const feedback = prompt('평가 코멘트:', '잘 작성되었습니다. 좋습니다!');

    if (score === null || score === undefined) {
        throw new Error('점수 입력이 취소되었습니다.');
    }

    console.log('⚠️ 오프라인 제출 방식: 백엔드에서 학생 submission 생성 확인 필요');
    console.log('💡 백엔드가 자동으로 submission을 생성하지 않으면 채점이 반영되지 않습니다.');

    // ✅ 백엔드 API: PUT /api/assignments/{assignmentIdx}/grade
    const result = await apiCall(`/api/assignments/${assignmentIdx}/grade`, 'PUT', {
        studentIdx: studentIdx,
        score: score,
        feedback: feedback || '평가 완료'
    });

    if (!result.ok) {
        throw new Error('과제 채점 실패: ' + JSON.stringify(result.data));
    }

    console.log('✅ 과제 채점 성공!');
    console.log('📋 채점 결과:', result.data);

    // assignmentData 확인
    if (result.data.assignmentData) {
        try {
            const data = JSON.parse(result.data.assignmentData);
            console.log('📊 채점 데이터 확인:', {
                submissions: data.submissions,
                submissionCount: data.submissions?.length || 0
            });
            
            const studentSubmission = data.submissions?.find(s => s.studentIdx === studentIdx);
            if (studentSubmission) {
                console.log('✅ 학생 submission 발견:', studentSubmission);
            } else {
                console.log('⚠️ 학생 submission 없음 - 백엔드에서 생성하지 못했을 수 있습니다.');
            }
        } catch (e) {
            console.log('⚠️ assignmentData 파싱 실패:', e.message);
        }
    }

    // ⚠️ 중요: 백엔드 로그 확인 요청
    console.log('\n🔍 백엔드 로그 확인하세요:');
    console.log('   - "과제 채점으로 인한 성적 재계산 이벤트 발행"');
    console.log('   - "성적 재계산 시작: lecIdx=X, studentIdx=Y"');
    console.log('   - "성적 재계산 완료"');
    console.log('');

    return result.data;
}

// ===================================================================
// 4단계: 성적 재확인 (변경 검증)
// ===================================================================

async function verifyGradeUpdate(lecSerial, studentIdx, initialState) {
    logStep('🔍', '4단계: 성적 업데이트 검증');

    // 잠시 대기 (비동기 처리 완료 대기)
    console.log('⏳ 3초 대기 중 (afterCommit 콜백 처리 대기)...');
    await sleep(3000);

    // 성적 재조회
    const gradeResult = await apiCall('/api/enrollments/grade-info', 'POST', {
        action: 'get-grade',
        lecSerial: lecSerial,
        studentIdx: studentIdx
    });

    if (!gradeResult.ok) {
        throw new Error('성적 재조회 실패');
    }

    const updatedGrade = gradeResult.data.data;
    console.log('📊 업데이트된 성적 (전체):', updatedGrade);
    
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
        const oldAssignments = initialState.grade.assignments || [];
        const newAssignments = gradeInfo.assignments || [];
        
        console.log(`과제 개수: ${oldAssignments.length} → ${newAssignments.length}`);
        
        // 새로 채점된 과제 찾기
        const newGradedAssignment = newAssignments.find(a => 
            !oldAssignments.find(old => 
                old.name === a.name && old.score === a.score
            )
        );

        if (newGradedAssignment) {
            console.log('✅ 새로 채점된 과제:', newGradedAssignment);
            console.log(`   - 과제명: ${newGradedAssignment.name}`);
            console.log(`   - 점수: ${newGradedAssignment.score} / ${newGradedAssignment.maxScore}`);
            console.log(`   - 비율: ${newGradedAssignment.percentage}%`);
        }

        // 총점 비교
        const oldTotal = initialState.grade.total?.score || 0;
        const newTotal = gradeInfo.total?.score || 0;
        const diff = newTotal - oldTotal;

        console.log(`\n총점: ${oldTotal} → ${newTotal} (${diff > 0 ? '+' : ''}${diff})`);
        
        if (diff !== 0) {
            console.log('✅ 총점이 변경되었습니다!');
            return { updated: gradeInfo, changed: true };
        } else {
            console.log('⚠️ 총점이 변경되지 않았습니다.');
            console.log('💡 DB 상태: totalScore =', oldTotal);
            return { updated: gradeInfo, changed: false };
        }
    } else {
        console.log('✅ 성적이 새로 생성되었습니다!');
        return { updated: gradeInfo, changed: true };
    }
}

// ===================================================================
// 🚀 전체 통합 테스트 실행 (심플 한방)
// ===================================================================

async function runAssignmentGradeTest() {
    console.clear();
    console.log('╔═══════════════════════════════════════════════════════════════════╗');
    console.log('║     🧪 과제-성적 통합 테스트 (@Transactional 수정 검증)         ║');
    console.log('║                      심플 한방 테스트                            ║');
    console.log('╚═══════════════════════════════════════════════════════════════════╝');
    
    assignmentTestResults.startTime = new Date();
    assignmentTestResults.steps = [];
    assignmentTestResults.success = false;
    assignmentTestResults.error = null;

    try {
        // 테스트 파라미터 입력
        const lecSerial = prompt('강의 코드 (LEC_SERIAL):', 'ETH201');
        const studentIdx = parseInt(prompt('학생 IDX:', '6'));
        
        if (!lecSerial || !studentIdx) {
            console.log('❌ 테스트 취소됨');
            return;
        }

        // 로그인 확인
        if (!window.professorToken || !window.currentProfessor) {
            console.log('⚠️ 교수 계정으로 로그인하세요: await loginProfessor()');
            return;
        }

        console.log('\n📋 테스트 설정:');
        console.log(`   - 강의: ${lecSerial}`);
        console.log(`   - 학생 IDX: ${studentIdx}`);
        console.log('');

        // 1단계: 초기 성적 확인
        const initialState = await checkInitialGrade(lecSerial, studentIdx);
        
        // 2단계: 과제 생성
        const { assignmentIdx } = await createAssignment(lecSerial);
        
        // 3단계: 과제 채점
        await gradeAssignment(assignmentIdx, studentIdx);
        
        // 4단계: 성적 업데이트 검증
        const verifyResult = await verifyGradeUpdate(lecSerial, studentIdx, initialState);
        
        // 최종 결과
        assignmentTestResults.endTime = new Date();
        assignmentTestResults.success = verifyResult.changed;
        
        console.log('\n╔═══════════════════════════════════════════════════════════════════╗');
        if (assignmentTestResults.success) {
            console.log('║                    ✅ 통합 테스트 성공!                          ║');
            console.log('║                                                                   ║');
            console.log('║  과제 채점 시 성적 자동 재계산이 정상 작동합니다.                ║');
            console.log('║  @Transactional 수정이 효과적으로 적용되었습니다.                ║');
        } else {
            console.log('║                    ⚠️  통합 테스트 실패                          ║');
            console.log('║                                                                   ║');
            console.log('║  과제 채점 후 성적이 업데이트되지 않았습니다.                    ║');
            console.log('║  백엔드 로그를 확인하여 "성적 재계산" 메시지를 찾으세요.         ║');
        }
        console.log('╚═══════════════════════════════════════════════════════════════════╝');

        const duration = (assignmentTestResults.endTime - assignmentTestResults.startTime) / 1000;
        console.log(`\n⏱️  총 소요 시간: ${duration.toFixed(2)}초`);
        console.log(`📊 실행 단계: ${assignmentTestResults.steps.length}개`);
        console.log(`📝 생성된 과제 IDX: ${assignmentTestResults.createdAssignmentIdx}`);

    } catch (error) {
        assignmentTestResults.error = error.message;
        assignmentTestResults.endTime = new Date();
        
        console.log('\n╔═══════════════════════════════════════════════════════════════════╗');
        console.log('║                    ❌ 통합 테스트 오류                            ║');
        console.log('╚═══════════════════════════════════════════════════════════════════╝');
        console.error('💥 에러:', error);
    }
}

// ===================================================================
// 🔍 개별 테스트 함수 (디버깅용)
// ===================================================================

// 과제 목록만 조회
async function quickCheckAssignments(lecSerial) {
    console.log('\n📊 과제 목록 조회');
    console.log('═'.repeat(70));
    
    const result = await apiCall('/api/assignments/list', 'POST', {
        lecSerial: lecSerial
    });

    if (result.ok) {
        console.log('✅ 조회 성공!');
        // API 응답: { content: [...], pageable: {...}, totalElements: N }
        const assignments = result.data.content || [];
        
        if (assignments.length === 0) {
            console.log('📭 과제가 없습니다.');
            return [];
        }
        
        console.table(assignments.map(a => ({
            IDX: a.assignmentIdx,
            과제명: a.assignmentName,
            만점: a.maxScore,
            마감일: a.dueDate,
            생성일: a.createdDate
        })));
        
        console.log(`\n📊 총 ${assignments.length}개의 과제`);
        return assignments;
    } else {
        console.log('❌ 조회 실패');
        return null;
    }
}

// 학생 성적만 조회
async function quickCheckStudentGrade(lecSerial, studentIdx) {
    console.log('\n📊 학생 성적 조회');
    console.log('═'.repeat(70));
    
    const result = await apiCall('/api/enrollments/grade-info', 'POST', {
        action: 'get-grade',
        lecSerial: lecSerial,
        studentIdx: studentIdx
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

// 과제 삭제
async function deleteAssignment(assignmentIdx) {
    console.log('\n🗑️  과제 삭제');
    console.log('═'.repeat(70));
    console.log(`삭제할 과제 IDX: ${assignmentIdx}`);
    
    const confirm = window.confirm(`과제 IDX ${assignmentIdx}를 삭제하시겠습니까?`);
    if (!confirm) {
        console.log('❌ 삭제 취소됨');
        return false;
    }
    
    const result = await apiCall(`/api/assignments/${assignmentIdx}`, 'DELETE', null);

    if (result.ok) {
        console.log('✅ 과제 삭제 성공!');
        return true;
    } else {
        console.log('❌ 과제 삭제 실패');
        console.log('에러:', result.data);
        return false;
    }
}

// 특정 강의의 모든 과제 삭제 (테스트 초기화용)
async function deleteAllAssignments(lecSerial) {
    console.log('\n🗑️  모든 과제 삭제 (테스트 초기화)');
    console.log('═'.repeat(70));
    
    // 먼저 과제 목록 조회
    const assignments = await quickCheckAssignments(lecSerial);
    
    if (!assignments || assignments.length === 0) {
        console.log('📭 삭제할 과제가 없습니다.');
        return;
    }
    
    const confirm = window.confirm(`${lecSerial} 강의의 모든 과제 ${assignments.length}개를 삭제하시겠습니까?`);
    if (!confirm) {
        console.log('❌ 삭제 취소됨');
        return;
    }
    
    console.log(`\n🔄 ${assignments.length}개의 과제 삭제 시작...`);
    
    let successCount = 0;
    let failCount = 0;
    
    for (const assignment of assignments) {
        const idx = assignment.assignmentIdx;
        console.log(`\n삭제 중: IDX ${idx} - ${assignment.assignmentName}`);
        
        const result = await apiCall(`/api/assignments/${idx}`, 'DELETE', null);
        
        if (result.ok) {
            console.log(`✅ IDX ${idx} 삭제 완료`);
            successCount++;
        } else {
            console.log(`❌ IDX ${idx} 삭제 실패`);
            failCount++;
        }
        
        // 서버 부하 방지를 위한 짧은 대기
        await sleep(300);
    }
    
    console.log('\n' + '═'.repeat(70));
    console.log('📊 삭제 결과:');
    console.log(`   ✅ 성공: ${successCount}개`);
    console.log(`   ❌ 실패: ${failCount}개`);
    console.log('═'.repeat(70));
    
    return { success: successCount, fail: failCount };
}

// ===================================================================
// 초기 안내 메시지
// ===================================================================

console.log('╔═══════════════════════════════════════════════════════════════════╗');
console.log('║        🧪 과제-성적 통합 테스트 스크립트 로드 완료!            ║');
console.log('║                  심플 한방 테스트 전용                           ║');
console.log('╚═══════════════════════════════════════════════════════════════════╝');
console.log('\n📍 위치: docs/강의관련기능/브라우저콘솔테스트/07-integration-test/');
console.log('🎯 목적: 과제 채점 후 성적 자동 재계산 검증\n');
console.log('═'.repeat(70));
console.log('\n⚡ 심플 사용법:\n');
console.log('1️⃣  await loginProfessor()          // 교수 로그인');
console.log('2️⃣  await runAssignmentGradeTest()  // 한방 테스트 실행');
console.log('');
console.log('═'.repeat(70));
console.log('\n🔍 개별 디버깅 함수:\n');
console.log('   await quickCheckAssignments("ETH201")         - 과제 목록 조회');
console.log('   await quickCheckStudentGrade("ETH201", 6)     - 학생 성적 조회');
console.log('   await deleteAssignment(assignmentIdx)         - 특정 과제 삭제');
console.log('   await deleteAllAssignments("ETH201")          - 모든 과제 삭제 (초기화)');
console.log('');
console.log('═'.repeat(70));
console.log('');
