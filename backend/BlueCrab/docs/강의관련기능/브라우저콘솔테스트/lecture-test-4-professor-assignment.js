// ===================================================================
// 📄 교수 과제 관리 테스트
// Blue Crab LMS - 교수 과제 생성 및 관리 테스트
// 
// ⚠️ 사전 준비: 먼저 교수 계정으로 로그인하세요!
// 📁 위치: docs/일반유저 로그인+게시판/test-1-login.js
// 📝 실행: await login() (교수 계정 사용)
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

// ========== JWT에서 교수 정보 추출 ==========
function getProfessorFromToken() {
    if (!window.authToken) return null;
    
    const payload = decodeJWT(window.authToken);
    if (!payload) return null;
    
    // JWT에서 교수번호(USER_NAME) 추출 (필드명은 실제 JWT 구조에 맞게 조정)
    // USER_NAME = 학번/교수번호/사원번호 (예: "PROF001", "P2024001")
    return payload.userName || payload.username || payload.USER_NAME || payload.sub || null;
}

// ========== 교수 강의 목록 조회 ==========
if (typeof window.authToken === 'undefined') window.authToken = null;

// ========== 내 강의 목록 조회 ==========
async function getMyLectures() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    // JWT에서 교수번호(USER_NAME) 추출
    const professorCode = getProfessorFromToken();
    if (!professorCode) {
        console.log('⚠️ JWT에서 교수번호를 찾을 수 없습니다.');
        console.log('💡 수동으로 교수번호를 입력하시겠습니까?');
    }
    
    const professor = professorCode || prompt('👨‍🏫 교수번호 (예: PROF001):', 'PROF001');
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n📚 담당 강의 목록 조회');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`👨‍🏫 교수번호: ${professor}`);

    try {
        const url = `${API_BASE_URL}/lectures?professor=${encodeURIComponent(professor)}&page=${page}&size=${size}`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        console.log('\n🔍 전체 응답 JSON:');
        console.log(JSON.stringify(result, null, 2));

        // 응답 구조 확인
        if (Array.isArray(result)) {
            // 배열 직접 반환
            console.log(`\n✅ 조회 성공! 총 ${result.length}개 강의`);
            console.log('📋 강의 목록:');
            result.forEach((lecture, idx) => {
                console.log(`\n${idx + 1}. ${lecture.lecTit || lecture.LEC_TIT} (${lecture.lecSerial || lecture.LEC_SERIAL})`);
                console.log(`   강의 IDX: ${lecture.lecIdx || lecture.LEC_IDX}`);
                console.log(`   교수번호: ${lecture.lecProf || lecture.LEC_PROF}`);
                console.log(`   학기: ${lecture.lecYear || lecture.LEC_YEAR}-${lecture.lecSemester || lecture.LEC_SEMESTER}`);
                console.log(`   정원: ${lecture.lecCurrent || 0}/${lecture.lecMany || lecture.LEC_MANY}명`);
                
                // 첫 번째 강의 IDX 저장
                if (idx === 0) {
                    window.lastLectureIdx = lecture.lecIdx || lecture.LEC_IDX;
                    console.log(`   💾 저장됨: window.lastLectureIdx = ${window.lastLectureIdx}`);
                }
            });
        } else if (result.success) {
            // success 래핑된 응답
            console.log('\n✅ 조회 성공!');
            console.log(`📊 총 ${result.data.totalElements}개 강의`);
            console.log('📋 강의 목록:');
            result.data.content.forEach((lecture, idx) => {
                console.log(`\n${idx + 1}. ${lecture.lecTit || lecture.LEC_TIT} (${lecture.lecSerial || lecture.LEC_SERIAL})`);
                console.log(`   강의 IDX: ${lecture.lecIdx || lecture.LEC_IDX}`);
                console.log(`   교수번호: ${lecture.lecProf || lecture.LEC_PROF}`);
                console.log(`   학기: ${lecture.lecYear || lecture.LEC_YEAR}-${lecture.lecSemester || lecture.LEC_SEMESTER}`);
                console.log(`   정원: ${lecture.lecCurrent || 0}/${lecture.lecMany || lecture.LEC_MANY}명`);
                
                // 첫 번째 강의 IDX 저장
                if (idx === 0) {
                    window.lastLectureIdx = lecture.lecIdx || lecture.LEC_IDX;
                    console.log(`   💾 저장됨: window.lastLectureIdx = ${window.lastLectureIdx}`);
                }
            });
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 생성 ==========
async function createAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const lectureIdx = parseInt(prompt('📚 LECTURE_IDX:', '1'));
    const title = prompt('📝 과제 제목:', '1주차 과제');
    const description = prompt('📝 과제 설명:', '자바 프로그래밍 기초 과제입니다.');
    const maxScore = parseInt(prompt('💯 배점:', '100'));
    const dueDate = prompt('📅 마감일 (YYYY-MM-DD):', '2025-12-31');

    if (!title || !lectureIdx) {
        console.log('❌ 필수 입력값이 없습니다.');
        return;
    }

    console.log('\n📄 과제 생성');
    console.log('═══════════════════════════════════════════════════════');

    const assignmentData = {
        LECTURE_IDX: lectureIdx,
        ASSIGNMENT_TITLE: title,
        ASSIGNMENT_DESCRIPTION: description,
        MAX_SCORE: maxScore,
        DUE_DATE: dueDate
    };

try {
        const response = await fetch(`${API_BASE_URL}/assignments`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(assignmentData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 과제 생성 성공!');
            console.log('📊 생성된 과제:', result.data);
            window.lastAssignmentIdx = result.data.ASSIGNMENT_IDX;
            console.log(`💾 저장된 ASSIGNMENT_IDX: ${window.lastAssignmentIdx}`);
        } else {
            console.log('❌ 과제 생성 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 목록 조회 ==========
async function getAssignments() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const lectureIdx = parseInt(prompt('📚 LECTURE_IDX:', '1'));
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n📄 과제 목록 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const url = `${API_BASE_URL}/assignments?lectureIdx=${lectureIdx}&page=${page}&size=${size}`;
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
                console.log(`   마감일: ${assignment.DUE_DATE}`);
                console.log(`   배점: ${assignment.MAX_SCORE}점`);
                console.log(`   제출 수: ${assignment.SUBMISSION_COUNT || 0}개`);
            });
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 제출된 과제 목록 조회 ==========
async function getSubmissions() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = parseInt(prompt('📄 ASSIGNMENT_IDX:', window.lastAssignmentIdx || '1'));
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n📄 제출된 과제 목록 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const url = `${API_BASE_URL}/assignments/${assignmentIdx}/submissions?page=${page}&size=${size}`;
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
            console.log(`📊 총 ${result.data.totalElements}개 제출`);
            console.log('📋 제출 목록:');
            result.data.content.forEach((submission, idx) => {
                console.log(`\n${idx + 1}. ${submission.STUDENT_NAME} (${submission.STUDENT_NO})`);
                console.log(`   제출일: ${submission.SUBMITTED_AT}`);
                console.log(`   점수: ${submission.SCORE || '미채점'}점`);
                console.log(`   상태: ${submission.SUBMISSION_STATUS}`);
            });
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 채점 ==========
async function gradeAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = parseInt(prompt('📄 ASSIGNMENT_IDX:', window.lastAssignmentIdx || '1'));
    const studentIdx = parseInt(prompt('👨‍🎓 STUDENT_IDX:', '1'));
    const score = parseInt(prompt('💯 점수:', '85'));
    const feedback = prompt('📝 피드백:', '잘 작성했습니다.');

    console.log('\n💯 과제 채점');
    console.log('═══════════════════════════════════════════════════════');

    const gradingData = {
        STUDENT_IDX: studentIdx,
        SCORE: score,
        FEEDBACK: feedback
    };

try {
        const response = await fetch(`${API_BASE_URL}/assignments/${assignmentIdx}/grade`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(gradingData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 채점 성공!');
            console.log('📊 채점 정보:', result.data);
        } else {
            console.log('❌ 채점 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 수정 ==========
async function updateAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = parseInt(prompt('✏️ 수정할 ASSIGNMENT_IDX:', window.lastAssignmentIdx || '1'));
    const title = prompt('📝 새 제목 (선택사항):');
    const dueDate = prompt('📅 새 마감일 (선택사항, YYYY-MM-DD):');

    console.log('\n✏️ 과제 수정');
    console.log('═══════════════════════════════════════════════════════');

    const updateData = {};
    if (title) updateData.ASSIGNMENT_TITLE = title;
    if (dueDate) updateData.DUE_DATE = dueDate;

    if (Object.keys(updateData).length === 0) {
        console.log('❌ 수정할 내용이 없습니다.');
        return;
    }

try {
        const response = await fetch(`${API_BASE_URL}/assignments/${assignmentIdx}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(updateData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 수정 성공!');
            console.log('📊 수정된 과제:', result.data);
        } else {
            console.log('❌ 수정 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 삭제 ==========
async function deleteAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = parseInt(prompt('🗑️ 삭제할 ASSIGNMENT_IDX:', window.lastAssignmentIdx || '1'));
    const confirm = prompt('⚠️ 정말 삭제하시겠습니까? (yes/no):', 'no');

    if (confirm.toLowerCase() !== 'yes') {
        console.log('❌ 삭제 취소됨');
        return;
    }

    console.log('\n🗑️ 과제 삭제');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/assignments/${assignmentIdx}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 삭제 성공!');
            console.log('📊 결과:', result.message);
        } else {
            console.log('❌ 삭제 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}
// ========== 도움말 ==========
function help() {
    console.log('\n👨‍🏫 교수 과제 관리 테스트 함수 목록');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 먼저 로그인하세요!');
    console.log('📁 docs/일반유저 로그인+게시판/test-1-login.js → await login()');
    console.log('');
    console.log('📚 getMyLectures()      - 담당 강의 목록 (JWT 자동 교수번호 추출)');
    console.log('📝 createAssignment()   - 과제 생성');
    console.log('📋 getAssignments()     - 과제 목록 조회');
    console.log('📄 getSubmissions()     - 제출된 과제 목록');
    console.log('💯 gradeAssignment()    - 과제 채점');
    console.log('✏️ updateAssignment()   - 과제 수정');
    console.log('🗑️ deleteAssignment()   - 과제 삭제');
    console.log('');
    console.log('💡 JWT 토큰에서 자동으로 교수번호(USER_NAME)를 추출합니다.');
}

// 초기 메시지
console.log('✅ 교수 과제 관리 테스트 스크립트 로드 완료!');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');

// JWT 디코딩 테스트
if (window.authToken) {
    const professorCode = getProfessorFromToken();
    if (professorCode) {
        console.log(`👨‍🏫 인식된 교수번호: ${professorCode}`);
    } else {
        console.log('⚠️ JWT에서 교수번호(USER_NAME)를 자동 추출할 수 없습니다. 수동 입력이 필요합니다.');
    }
}
