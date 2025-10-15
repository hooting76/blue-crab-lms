// ===================================================================
// 📄 학생 과제 제출 테스트
// Blue Crab LMS - 학생 과제 조회 및 제출 테스트
// 
// ⚠️ 사전 준비: 먼저 학생 계정으로 로그인하세요!
// 📁 위치: docs/일반유저 로그인+게시판/test-1-login.js
// 📝 실행: await login() (학생 계정 사용)
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

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
        let url = `${API_BASE_URL}/api/assignments?page=${page}&size=${size}`;
        if (lectureIdx) url += `&lectureIdx=${lectureIdx}`;

        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        
        // 🔍 Phase 6.8.1: 서버 응답 원본 확인
        const responseText = await response.text();
        console.log('📦 서버 응답 원본:', responseText);
        
        const result = JSON.parse(responseText);

        // ⚠️ 학생용 API는 ApiResponse 래퍼 없이 직접 Page 객체 반환
        if (response.status === 200 && result.content) {
            console.log('\n✅ 조회 성공!');
            console.log(`📊 총 ${result.totalElements}개 과제`);
            console.log('📋 과제 목록:');
            result.content.forEach((assignment, idx) => {
                // assignmentData는 JSON 문자열이므로 파싱 필요
                const data = JSON.parse(assignment.assignmentData);
                const assignmentInfo = data.assignment;
                const submissions = data.submissions || [];
                const mySubmission = submissions.find(s => s.studentIdx === window.currentUser?.id);
                
                console.log(`\n${idx + 1}. ${assignmentInfo.title}`);
                console.log(`   IDX: ${assignment.assignmentIdx}`);
                console.log(`   LEC_IDX: ${assignment.lecIdx}`);
                console.log(`   설명: ${assignmentInfo.description || 'N/A'}`);
                console.log(`   마감일: ${assignmentInfo.dueDate}`);
                console.log(`   배점: ${assignmentInfo.maxScore}점`);
                console.log(`   제출상태: ${mySubmission ? mySubmission.status : '미제출'}`);
                console.log(`   점수: ${mySubmission?.score || 'N/A'}점`);
            });
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message || result);
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
        const response = await fetch(`${API_BASE_URL}/api/assignments/${assignmentIdx}`, {
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

// ========== 과제 제출 기록 ==========
// 📌 실제 제출물은 서면/메일 등 오프라인으로 제출
// 📌 DB에는 "제출 여부"만 기록 (파일 제출 없음)
async function markAsSubmitted() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = parseInt(prompt('📝 제출 완료 표시할 ASSIGNMENT_IDX:', '1'));
    const confirmMessage = '⚠️ 실제 과제는 서면/메일 등으로 교수님께 제출하셨나요?\n제출 완료 상태로 표시합니다. (yes/no):';
    const confirm = prompt(confirmMessage, 'yes');

    if (confirm.toLowerCase() !== 'yes') {
        console.log('❌ 제출 표시 취소됨');
        return;
    }

    console.log('\n📝 과제 제출 완료 표시');
    console.log('═══════════════════════════════════════════════════════');
    console.log('💡 실제 제출물: 서면/메일 등으로 오프라인 제출');
    console.log('💡 DB 기록: 제출 여부만 표시');

    const submissionData = {
        submitted: true,  // 제출 여부만 표시
        submittedAt: new Date().toISOString()
    };

    try {
        const response = await fetch(`${API_BASE_URL}/api/assignments/${assignmentIdx}/submit`, {
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
            console.log('\n✅ 제출 완료 표시 성공!');
            console.log('📊 학생의 과제 제출이 기록되었습니다.');
            console.log('📌 교수님께 실제 제출물을 전달하세요 (서면/메일 등)');
        } else {
            console.log('❌ 제출 표시 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 제출 취소 (미제출로 변경) ==========
async function cancelSubmission() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = parseInt(prompt('�️ 제출 취소할 ASSIGNMENT_IDX:', '1'));
    const confirm = prompt('⚠️ 제출 기록을 미제출 상태로 되돌립니다. (yes/no):', 'no');

    if (confirm.toLowerCase() !== 'yes') {
        console.log('❌ 취소 중단됨');
        return;
    }

    console.log('\n�️ 과제 제출 취소');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/api/assignments/${assignmentIdx}/cancel`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 제출 취소 성공!');
            console.log('📊 과제가 미제출 상태로 변경되었습니다.');
        } else {
            console.log('❌ 제출 취소 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 도움말 ==========
function help() {
    console.log('\n� 학생 과제 시스템 테스트 함수 목록');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 먼저 로그인하세요!');
    console.log('� docs/일반유저 로그인+게시판/test-1-login.js → await login()');
    console.log('');
    console.log('� getMyAssignments()     - 내 과제 목록 조회');
    console.log('🔍 getAssignmentDetail()  - 과제 상세 조회');
    console.log('📝 markAsSubmitted()      - 과제 제출 완료 표시 (제출 여부만 기록)');
    console.log('🗑️ cancelSubmission()     - 과제 제출 취소 (미제출로 변경)');
    console.log('');
    console.log('� 과제 제출 방식:');
    console.log('   1️⃣ 실제 제출물: 서면/메일 등으로 교수님께 직접 제출');
    console.log('   2️⃣ DB 기록: 제출 여부만 시스템에 표시 (markAsSubmitted)');
    console.log('   3️⃣ 채점: 교수님이 제출 여부와 점수 기록');
}

// 초기 메시지
console.log('✅ 학생 과제 제출 테스트 스크립트 로드 완료!');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');
