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

// ========== 백엔드에서 받은 사용자 정보 추출 ==========
// 📌 JWT 디코딩은 백엔드에서 처리됩니다!
// 📌 로그인 시 response.data.user에 사용자 정보가 포함되어 있습니다.
function getUserInfo() {
    const user = window.currentUser;
    if (!user) {
        console.log('⚠️ 사용자 정보가 없습니다. 로그인이 필요합니다.');
        return null;
    }
    
    return {
        userIdx: user.id || user.userIdx || user.userId,
        email: user.email,
        name: user.name,
        role: user.role,
        profCode: user.profCode || user.professorCode,  // 교수 코드
        userProfessor: user.userProfessor  // 0=교수, 1=학생 구분
    };
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
    
    if (!user || !user.id) {
        console.log('\n⚠️ 사용자 정보가 없습니다!');
        console.log('🔧 다시 로그인하세요: await login()');
        return false;
    }
    
    console.log('✅ 로그인 확인됨:', {
        userIdx: user.id,
        email: user.email,
        name: user.name
    });
    
    return true;
}

// ========== 사용자 정보 디버깅 ==========
// 📌 백엔드에서 받은 사용자 정보를 표시합니다
function debugUserInfo() {
    if (!window.authToken) {
        console.log('❌ 로그인 토큰이 없습니다.');
        console.log('💡 먼저 로그인하세요: await login()');
        return;
    }
    
    const user = window.currentUser;
    if (!user) {
        console.log('❌ 사용자 정보가 없습니다.');
        console.log('💡 다시 로그인하세요: await login()');
        return;
    }
    
    console.log('\n🔍 사용자 정보 (백엔드에서 받음):');
    console.log('═══════════════════════════════════════════════════════');
    console.log('   - ID:', user.id);
    console.log('   - 이메일:', user.email);
    console.log('   - 이름:', user.name);
    console.log('   - 역할:', user.role);
    console.log('   - 교수 코드:', user.profCode || user.professorCode || 'N/A');
    console.log('   - 구분:', user.userProfessor === 0 ? '교수' : '학생');
    
    console.log('\n� 전체 사용자 객체:');
    console.log('═══════════════════════════════════════════════════════');
    console.log(JSON.stringify(user, null, 2));
    
    console.log('\n💡 getUserInfo() 함수로 추출되는 정보:');
    console.log('═══════════════════════════════════════════════════════');
    const userInfo = getUserInfo();
    console.log(JSON.stringify(userInfo, null, 2));
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
        const url = `${API_BASE_URL}/assignments/submissions`;
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

// ========== 과제 채점 (오프라인 제출 방식) ==========
async function gradeAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = window.lastAssignmentIdx || parseInt(prompt('📄 과제 IDX:', '1'));
    const studentIdx = parseInt(prompt('👨‍🎓 학생 IDX:', '6'));
    const score = parseInt(prompt('� 점수 (0~10):', '8'));
    const feedback = prompt('� 평가 코멘트:', '잘 작성되었습니다.');

    if (!assignmentIdx || !studentIdx || score === null) {
        console.log('❌ 필수 입력값이 없습니다.');
        return;
    }

    console.log('\n💯 과제 채점 (오프라인 제출)');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📄 과제 IDX: ${assignmentIdx}`);
    console.log(`👨‍🎓 학생 IDX: ${studentIdx}`);
    console.log(`� 점수: ${score}점`);
    console.log(`� 코멘트: ${feedback || '(없음)'}`);

    // ✅ 백엔드 API: PUT /api/assignments/{assignmentIdx}/grade
    const gradeData = {
        studentIdx: studentIdx,
        score: score,
        feedback: feedback || ''
    };

    console.log('\n📤 요청 데이터:');
    console.log(JSON.stringify(gradeData, null, 2));

    try {
        const url = `${API_BASE_URL}/assignments/${assignmentIdx}/grade`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            method: 'PUT',
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

        if (response.ok) {
            console.log('\n✅ 채점 성공!');
            console.log('📋 채점 완료 정보:', result);
            
            console.log('\n🔍 백엔드 로그 확인:');
            console.log('   - "과제 채점으로 인한 성적 재계산 이벤트 발행"');
            console.log('   - "성적 재계산 시작: lecIdx=X, studentIdx=Y"');
            console.log('   - "성적 재계산 완료"');
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
    const dueDate = prompt('📅 새 마감일 (수정할 경우, YYYYMMDD):', '');

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
        const response = await fetch(`${API_BASE_URL}/assignments/update`, {
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
        const response = await fetch(`${API_BASE_URL}/assignments/delete`, {
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
    console.log('getUserInfo()                 - 사용자 정보 추출');
    console.log('debugUserInfo()               - 사용자 정보 디버깅');
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
console.log('');
console.log('📌 중요: JWT 디코딩은 백엔드에서 처리됩니다!');
console.log('   - 로그인 시 백엔드가 response.data.user에 사용자 정보 포함');
console.log('   - 프론트엔드는 받은 정보를 그대로 사용');

// 사용자 정보 자동 확인
if (window.authToken && window.currentUser) {
    console.log('🔄 사용자 정보 확인 중...');
    const userInfo = getUserInfo();
    if (userInfo) {
        console.log(`✅ 인식된 USER_IDX: ${userInfo.userIdx}`);
        console.log(`   - 이름: ${userInfo.name}`);
        console.log(`   - 이메일: ${userInfo.email}`);
        console.log('💡 이제 자동으로 사용됩니다.');
    }
} else if (window.authToken && !window.currentUser) {
    console.log('⚠️ 사용자 정보가 없습니다. 다시 로그인하세요.');
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
console.log('debugUserInfo()            // 사용자 정보 디버깅');
console.log('help()                     // 전체 도움말');
console.log('═'.repeat(63) + '\n');