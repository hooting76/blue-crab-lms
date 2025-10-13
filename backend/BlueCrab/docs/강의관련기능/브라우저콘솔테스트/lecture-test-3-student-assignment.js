// ===================================================================
// 📄 학생 과제 제출 테스트
// Blue Crab LMS - 학생 과제 조회 및 제출 테스트
// 
// ⚠️ 사전 준비: 먼저 학생 계정으로 로그인하세요!
// 📁 위치: docs/일반유저 로그인+게시판/test-1-login.js
// 📝 실행: await login() (학생 계정 사용)
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/student';

// 전역 변수 (test-1-login.js에서 설정한 토큰 사용)
if (typeof window.authToken === 'undefined') window.authToken = null;

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

// ========== 내 과제 목록 조회 ==========
async function getMyAssignments() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const lectureIdx = prompt('📚 LECTURE_IDX (전체 조회는 비워두세요):');
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n📄 내 과제 목록 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        let url = `${API_BASE_URL}/assignments?page=${page}&size=${size}`;
        if (lectureIdx) url += `&lectureIdx=${lectureIdx}`;

        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 조회 성공!');
            console.log(`📊 총 ${result.data.totalElements}개 과제`);
            console.log('📋 과제 목록:');
            result.data.content.forEach((assignment, idx) => {
                console.log(`\n${idx + 1}. ${assignment.ASSIGNMENT_TITLE}`);
                console.log(`   IDX: ${assignment.ASSIGNMENT_IDX}`);
                console.log(`   강의: ${assignment.LECTURE_NAME}`);
                console.log(`   마감일: ${assignment.DUE_DATE}`);
                console.log(`   배점: ${assignment.MAX_SCORE}점`);
                console.log(`   제출상태: ${assignment.SUBMISSION_STATUS || '미제출'}`);
                console.log(`   점수: ${assignment.SCORE || 'N/A'}점`);
            });
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 상세 조회 ==========
async function getAssignmentDetail() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = parseInt(prompt('🔍 조회할 ASSIGNMENT_IDX:', '1'));

    console.log('\n📄 과제 상세 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/assignments/${assignmentIdx}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 조회 성공!');
            const assignment = result.data;
            console.log('📊 과제 정보:');
            console.log(`   제목: ${assignment.ASSIGNMENT_TITLE}`);
            console.log(`   강의: ${assignment.LECTURE_NAME}`);
            console.log(`   설명: ${assignment.ASSIGNMENT_DESCRIPTION || 'N/A'}`);
            console.log(`   마감일: ${assignment.DUE_DATE}`);
            console.log(`   배점: ${assignment.MAX_SCORE}점`);
            console.log(`   제출상태: ${assignment.SUBMISSION_STATUS || '미제출'}`);
            console.log(`   제출일: ${assignment.SUBMITTED_AT || 'N/A'}`);
            console.log(`   점수: ${assignment.SCORE || 'N/A'}점`);
            console.log(`   피드백: ${assignment.FEEDBACK || 'N/A'}`);
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 제출 ==========
async function submitAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = parseInt(prompt('📝 제출할 ASSIGNMENT_IDX:', '1'));
    const content = prompt('📝 제출 내용을 입력하세요:', '과제 제출 내용입니다.');

    if (!content) {
        console.log('❌ 제출 내용이 없습니다.');
        return;
    }

    console.log('\n📝 과제 제출');
    console.log('═══════════════════════════════════════════════════════');

    const submissionData = {
        SUBMISSION_CONTENT: content,
        SUBMISSION_FILE_PATH: null // 파일 업로드는 별도 구현 필요
    };

try {
        const response = await fetch(`${API_BASE_URL}/assignments/${assignmentIdx}/submit`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(submissionData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 제출 성공!');
            console.log('📊 제출 정보:', result.data);
        } else {
            console.log('❌ 제출 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 재제출 ==========
async function resubmitAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = parseInt(prompt('📝 재제출할 ASSIGNMENT_IDX:', '1'));
    const content = prompt('📝 재제출 내용을 입력하세요:', '과제 재제출 내용입니다.');

    if (!content) {
        console.log('❌ 재제출 내용이 없습니다.');
        return;
    }

    console.log('\n📝 과제 재제출');
    console.log('═══════════════════════════════════════════════════════');

    const submissionData = {
        SUBMISSION_CONTENT: content,
        SUBMISSION_FILE_PATH: null
    };

try {
        const response = await fetch(`${API_BASE_URL}/assignments/${assignmentIdx}/resubmit`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(submissionData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 재제출 성공!');
            console.log('📊 재제출 정보:', result.data);
        } else {
            console.log('❌ 재제출 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 제출 취소 ==========
async function cancelSubmission() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = parseInt(prompt('🗑️ 제출취소할 ASSIGNMENT_IDX:', '1'));
    const confirm = prompt('⚠️ 정말 제출을 취소하시겠습니까? (yes/no):', 'no');

    if (confirm.toLowerCase() !== 'yes') {
        console.log('❌ 취소 중단됨');
        return;
    }

    console.log('\n🗑️ 과제 제출 취소');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/assignments/${assignmentIdx}/cancel`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 제출취소 성공!');
            console.log('📊 결과:', result.message);
        } else {
            console.log('❌ 제출취소 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 도움말 ==========
function help() {
    console.log('\n📄 학생 과제 제출 테스트 함수 목록');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 먼저 로그인하세요!');
    console.log('📁 docs/일반유저 로그인+게시판/test-1-login.js → await login()');
📋 getMyAssignments()     - 내 과제 목록');
    console.log('🔍 getAssignmentDetail()  - 과제 상세 조회');
    console.log('📝 submitAssignment()     - 과제 제출');
    console.log('📝 resubmitAssignment()   - 과제 재제출');
    console.log('🗑️ cancelSubmission()     - 과제 제출 취소');
}

// 초기 메시지
console.log('✅ 학생 과제 제출 테스트 스크립트 로드 완료!');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');
