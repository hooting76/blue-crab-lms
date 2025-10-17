// ===================================================================
// 📄 교수 과제 관리 테스트 (Part B: 과제 채점 및 관리)
// Blue Crab LMS - 교수 과제 채점 및 관리 테스트
// 
// ⚠️ 사전 준비: 먼저 교수 계정으로 로그인하세요!
// 📁 위치: docs/일반유저 로그인+게시판/test-1-login.js
// 📝 실행: await login() (교수 계정 사용)
// 
// 📂 파일 구조:
//    Part A: lecture-test-4a-professor-assignment-create.js (과제 생성 및 목록)
//    Part B: 이 파일 (과제 채점 및 관리)
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

// 전역 변수 (test-1-login.js에서 설정한 토큰 사용)
if (typeof window.authToken === 'undefined') window.authToken = null;

// ========== JWT 디코딩 ==========
function decodeJWT(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => 
            '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
        ).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error('JWT 디코딩 실패:', e);
        return null;
    }
}

// ========== 로그인 상태 확인 ==========
function checkAuth() {
    const token = window.authToken;
    const user = window.currentUser;
    
    if (!token) {
        console.log('\n⚠️ 로그인이 필요합니다!');
        console.log('🔧 docs/일반유저 로그인+게시판/test-1-login.js 실행 → await login()');
        return false;
    }
    return true;
}

// ========== JWT에서 USER_IDX 추출 ==========
function getUserIdxFromToken() {
    if (!window.authToken) {
        console.log('⚠️ 로그인 토큰이 없습니다.');
        return null;
    }
    
    const payload = decodeJWT(window.authToken);
    if (!payload) {
        console.log('❌ JWT 디코딩 실패');
        return null;
    }
    
    // JWT에서 USER_IDX 추출 (가능한 필드명들 시도)
    const userIdx = payload.userIdx || payload.USER_IDX || payload.userId || payload.USER_ID || payload.user_id || payload.id;
    
    if (userIdx) {
        console.log(`✅ JWT에서 USER_IDX 추출 성공: ${userIdx}`);
        return String(userIdx); // 문자열로 변환
    } else {
        console.log('❌ JWT에서 USER_IDX를 찾을 수 없습니다.');
        console.log('📋 JWT Payload:', payload);
        return null;
    }
}

// ========== JWT 토큰 디버깅 ==========
async function debugTokenInfo() {
    if (!window.authToken) {
        console.log('❌ 로그인 토큰이 없습니다.');
        console.log('💡 먼저 로그인하세요: await login()');
        return;
    }
    
    const payload = decodeJWT(window.authToken);
    if (!payload) {
        console.log('❌ JWT 디코딩 실패');
        return;
    }
    
    console.log('\n🔍 JWT Payload 전체 내용:');
    console.log('═══════════════════════════════════════════════════════');
    console.log(JSON.stringify(payload, null, 2));
    
    console.log('\n📋 모든 필드 나열:');
    console.log('═══════════════════════════════════════════════════════');
    Object.keys(payload).forEach(key => {
        console.log(`   ${key}: ${JSON.stringify(payload[key])}`);
    });
    
    console.log('\n🔎 USER_IDX 확인:');
    console.log('═══════════════════════════════════════════════════════');
    const userIdx = getUserIdxFromToken();
    if (userIdx) {
        console.log(`   ✅ 최종 USER_IDX: "${userIdx}"`);
    } else {
        console.log('   ❌ USER_IDX 조회 실패 - 수동 입력이 필요합니다.');
    }
    
    console.log('\n💡 currentUser 정보:');
    console.log('═══════════════════════════════════════════════════════');
    if (window.currentUser) {
        console.log(JSON.stringify(window.currentUser, null, 2));
    } else {
        console.log('   (없음)');
    }
}

// ========== 제출 현황 조회 (학생별) ==========
async function getSubmissions() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = window.lastAssignmentIdx || parseInt(prompt('📄 과제 IDX:', '1'));
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n📊 과제 제출 현황');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📄 과제 IDX: ${assignmentIdx}`);

    try {
        // ✅ DTO 패턴: POST 방식으로 변경
        const url = `${API_BASE_URL}/api/assignments/submissions`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                assignmentIdx: assignmentIdx,
                page: page,
                size: size,
                action: 'get_submissions'
            })
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        console.log('\n🔍 전체 응답:');
        console.log(JSON.stringify(result, null, 2));

        if (result.content) {
            console.log(`\n✅ 조회 성공! 총 ${result.totalElements}명 수강생`);
            console.log('📋 제출 현황:');
            result.content.forEach((submission, idx) => {
                console.log(`\n${idx + 1}. 학생 이름: ${submission.studentName}`);
                console.log(`   학번: ${submission.studentId}`);
                console.log(`   제출 상태: ${submission.submitted ? '✅ 제출' : '❌ 미제출'}`);
                console.log(`   채점 상태: ${submission.graded ? '✅ 채점완료' : '⏳ 채점대기'}`);
                if (submission.graded) {
                    console.log(`   획득 점수: ${submission.score}/${submission.maxScore}점`);
                    console.log(`   제출 방법: ${submission.submissionMethod || 'N/A'}`);
                }
            });
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 채점 ==========
async function gradeAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = window.lastAssignmentIdx || parseInt(prompt('📄 과제 IDX:', '1'));
    const studentCode = prompt('👨‍🎓 학생번호 (예: 1, STU001):', '1');
    const submissionMethod = prompt('📤 제출 방법 (예: email, print, hands, absent):', 'email');
    const score = parseInt(prompt('💯 점수 (0~10):', '10'));

    if (!assignmentIdx || !studentCode || !submissionMethod || score === null) {
        console.log('❌ 필수 입력값이 없습니다.');
        return;
    }

    console.log('\n💯 과제 채점');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📄 과제 IDX: ${assignmentIdx}`);
    console.log(`👨‍🎓 학생번호: ${studentCode}`);
    console.log(`📤 제출 방법: ${submissionMethod}`);
    console.log(`💯 점수: ${score}점`);

    // ✅ DTO 패턴
    const gradeData = {
        assignmentIdx: assignmentIdx,
        studentCode: studentCode,
        submissionMethod: submissionMethod,
        score: score,
        action: 'grade'
    };

    console.log('\n📤 요청 데이터:');
    console.log(JSON.stringify(gradeData, null, 2));

    try {
        const response = await fetch(`${API_BASE_URL}/api/assignments/grade`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(gradeData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        console.log('\n🔍 전체 응답:');
        console.log(JSON.stringify(result, null, 2));

        if (response.ok || result.success) {
            console.log('\n✅ 채점 성공!');
            const grade = result.data || result;
            console.log('📊 채점 정보:');
            console.log(`   학생: ${grade.studentName || 'N/A'}`);
            console.log(`   제출 방법: ${grade.submissionMethod || submissionMethod}`);
            console.log(`   점수: ${grade.score || score}/${grade.maxScore || 10}점`);
            console.log(`   채점 일시: ${grade.gradedDate || new Date().toISOString()}`);
        } else {
            console.log('❌ 채점 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 수정 ==========
async function updateAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = window.lastAssignmentIdx || parseInt(prompt('📄 과제 IDX:', '1'));
    const title = prompt('📝 새 제목 (수정할 경우):', '');
    const description = prompt('📝 새 설명 (수정할 경우):', '');
    const maxScore = 10;  // ✅ 항상 10점으로 고정
    const dueDate = prompt('📅 새 마감일 (수정할 경우, YYYY-MM-DD):', '');

    console.log('\n✏️ 과제 수정');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📄 과제 IDX: ${assignmentIdx}`);

    // ✅ DTO 패턴 - 수정할 필드만 포함
    const updateData = {
        assignmentIdx: assignmentIdx,
        action: 'update'
    };

    if (title) {
        updateData.title = title;
        console.log(`📝 제목: ${title}`);
    }
    if (description) {
        updateData.body = description;
        console.log(`📝 설명: ${description}`);
    }
    if (dueDate) {
        updateData.dueDate = dueDate;
        console.log(`📅 마감일: ${dueDate}`);
    }

    // 배점은 항상 10점 (수정 불가)
    updateData.maxScore = maxScore;
    console.log(`💯 배점: 10점 (고정)`);

    console.log('\n📤 요청 데이터:');
    console.log(JSON.stringify(updateData, null, 2));

    try {
        const response = await fetch(`${API_BASE_URL}/api/assignments/update`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(updateData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        console.log('\n🔍 전체 응답:');
        console.log(JSON.stringify(result, null, 2));

        if (response.ok || result.success) {
            console.log('\n✅ 과제 수정 성공!');
            const assignment = result.data || result;
            console.log('📊 수정된 과제:');
            console.log(`   IDX: ${assignment.assignmentIdx || assignment.ASSIGNMENT_IDX}`);
            if (title) console.log(`   제목: ${assignment.title || title}`);
            if (dueDate) console.log(`   마감일: ${assignment.dueDate || dueDate}`);
            console.log(`   배점: ${assignment.maxScore || maxScore}점`);
        } else {
            console.log('❌ 수정 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 삭제 ==========
async function deleteAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = window.lastAssignmentIdx || parseInt(prompt('📄 과제 IDX:', '1'));
    
    const confirm = window.confirm(`정말로 과제 ${assignmentIdx}를 삭제하시겠습니까?`);
    if (!confirm) {
        console.log('❌ 삭제 취소됨');
        return;
    }

    console.log('\n🗑️ 과제 삭제');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📄 과제 IDX: ${assignmentIdx}`);

    try {
        // ✅ DTO 패턴: POST 방식으로 변경
        const response = await fetch(`${API_BASE_URL}/api/assignments/delete`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                assignmentIdx: assignmentIdx,
                action: 'delete'
            })
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        console.log('\n🔍 전체 응답:');
        console.log(JSON.stringify(result, null, 2));

        if (response.ok || result.success) {
            console.log('\n✅ 과제 삭제 성공!');
            console.log(`📄 삭제된 과제 IDX: ${assignmentIdx}`);
        } else {
            console.log('❌ 삭제 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 도움말 ==========
function help() {
    console.log('\n👨‍🏫 교수 과제 관리 테스트 (Part B: 과제 채점 및 관리)');
    console.log('═══════════════════════════════════════════════════════\n');
    
    console.log('📋 제공 함수:\n');
    console.log('await checkAuth()             - 로그인 상태 확인');
    console.log('getUserIdxFromToken()         - JWT에서 USER_IDX 추출');
    console.log('await debugTokenInfo()        - JWT 토큰 디버깅');
    console.log('await getSubmissions()        - 제출 현황 조회 (학생별)');
    console.log('await gradeAssignment()       - 과제 채점 (제출방식 + 점수)');
    console.log('await updateAssignment()      - 과제 수정');
    console.log('await deleteAssignment()      - 과제 삭제\n');
    
    console.log('📂 관련 파일:');
    console.log('   Part A: lecture-test-4a-professor-assignment-create.js');
    console.log('   - 담당 강의 목록 조회');
    console.log('   - 과제 생성 (10점 고정)');
    console.log('   - 과제 목록 조회\n');
    
    console.log('═══════════════════════════════════════════════════════\n');
    console.log('💡 제출 방법 옵션:');
    console.log('   email  - 이메일 제출');
    console.log('   print  - 출력물 제출');
    console.log('   hands  - 직접 제출');
    console.log('   absent - 미제출\n');
    console.log('💡 채점 규칙: 제출 방법 기록 + 0~10점 점수 입력');
    console.log('💡 window.lastAssignmentIdx 자동 사용 (Part A에서 저장된 값)');
}

// 초기 메시지
console.log('✅ 교수 과제 관리 테스트 스크립트 로드 완료! (Part B: 과제 채점 및 관리)');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');

// JWT에서 USER_IDX 자동 추출 테스트
if (window.authToken) {
    console.log('🔄 JWT에서 USER_IDX 추출 중...');
    const userIdx = getUserIdxFromToken();
    if (userIdx) {
        console.log(`✅ 인식된 USER_IDX: ${userIdx}`);
        console.log('💡 이제 자동으로 사용됩니다.');
    } else {
        console.log('⚠️ JWT에서 USER_IDX를 자동 추출할 수 없습니다. 수동 입력이 필요합니다.');
        console.log('💡 debugTokenInfo()를 실행하여 JWT 구조를 확인하세요.');
    }
}

// ═══════════════════════════════════════════════════════════════════
// 🚀 빠른 실행 명령어 (테스터용)
// ═══════════════════════════════════════════════════════════════════
console.log('\n' + '═'.repeat(63));
console.log('🚀 빠른 실행 명령어');
console.log('═'.repeat(63));
console.log('await getSubmissions()     // 과제 제출 현황');
console.log('await gradeAssignment()    // 과제 채점');
console.log('await updateAssignment()   // 과제 수정');
console.log('await deleteAssignment()   // 과제 삭제');
console.log('await debugTokenInfo()     // JWT 디버깅');
console.log('help()                     // 전체 도움말');
console.log('═'.repeat(63) + '\n');