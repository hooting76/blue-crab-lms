// ===================================================================
// � 교수 학생 관리 테스트
// Blue Crab LMS - 교수 수강생 조회 및 관리 테스트
// 
// ⚠️ 사전 준비: 먼저 교수 계정으로 로그인하세요!
// 📁 위치: docs/일반유저 로그인+게시판/test-1-login.js
// 📝 실행: await login() (교수 계정 사용)
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

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

// ========== 수강생 목록 조회 ==========
async function getStudents() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const lecSerial = prompt('📚 강의 코드 (예: CS101):', 'CS101');
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n👨‍🎓 수강생 목록 조회 - POST');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const requestBody = {
            lecSerial: lecSerial,
            page: page,
            size: size
        };
        console.log('📤 요청 데이터:', JSON.stringify(requestBody, null, 2));

        const response = await fetch(`${API_BASE_URL}/enrollments/list`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 조회 성공!');
            console.log(`📊 총 ${result.data.totalElements}명 수강생`);
            console.log('📋 수강생 목록:');
            result.data.content.forEach((student, idx) => {
                console.log(`\n${idx + 1}. ${student.STUDENT_NAME} (${student.STUDENT_NO})`);
                console.log(`   IDX: ${student.STUDENT_IDX}`);
                console.log(`   학과: ${student.DEPARTMENT || 'N/A'}`);
                console.log(`   상태: ${student.STATUS}`);
                console.log(`   신청일: ${student.ENROLLED_AT}`);
            });
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 수강생 상세 조회 ==========
async function getStudentDetail() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const enrollmentIdx = parseInt(prompt('� ENROLLMENT_IDX:', '1'));

    console.log('\n👨‍🎓 수강생 상세 조회 - POST');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const requestBody = { enrollmentIdx: enrollmentIdx };
        console.log('📤 요청 데이터:', JSON.stringify(requestBody, null, 2));

        const response = await fetch(`${API_BASE_URL}/enrollments/detail`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 조회 성공!');
            const student = result.data;
            console.log('📊 수강생 정보:');
            console.log(`   이름: ${student.STUDENT_NAME}`);
            console.log(`   학번: ${student.STUDENT_NO}`);
            console.log(`   학과: ${student.DEPARTMENT || 'N/A'}`);
            console.log(`   상태: ${student.STATUS}`);
            console.log(`   신청일: ${student.ENROLLED_AT}`);
            console.log(`   과제 제출 수: ${student.ASSIGNMENT_COUNT || 0}개`);
            console.log(`   평균 점수: ${student.AVERAGE_SCORE || 'N/A'}점`);
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 수강생 성적 조회 ==========
async function getStudentGrades() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const lectureIdx = parseInt(prompt('📚 LECTURE_IDX:', '1'));
    const studentIdx = parseInt(prompt('👨‍🎓 STUDENT_IDX:', '1'));

    console.log('\n📊 수강생 성적 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/grades/my-grades`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                studentIdx: studentIdx,
                lecSerial: lectureSerial || 'CS101',
                action: "professor-view"
            })
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 조회 성공!');
            console.log('📊 성적 정보:');
            console.log(`   학생: ${result.data.STUDENT_NAME}`);
            console.log(`   강의: ${result.data.LECTURE_NAME}`);
            console.log(`   총 과제 수: ${result.data.TOTAL_ASSIGNMENTS}개`);
            console.log(`   제출 수: ${result.data.SUBMITTED_ASSIGNMENTS}개`);
            console.log(`   평균 점수: ${result.data.AVERAGE_SCORE}점`);
            console.log(`   총점: ${result.data.TOTAL_SCORE}/${result.data.MAX_TOTAL_SCORE}점`);
            
            if (result.data.ASSIGNMENTS && result.data.ASSIGNMENTS.length > 0) {
                console.log('\n📋 과제별 점수:');
                result.data.ASSIGNMENTS.forEach((assignment, idx) => {
                    console.log(`${idx + 1}. ${assignment.TITLE}: ${assignment.SCORE || '미채점'}/${assignment.MAX_SCORE}점`);
                });
            }
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 통계 조회 ==========
async function getLectureStatistics() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const lecSerial = prompt('📚 강의 코드 (예: CS101):', 'CS101');

    console.log('\n📊 강의 통계 조회 - POST');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const requestBody = { lecSerial: lecSerial };
        console.log('📤 요청 데이터:', JSON.stringify(requestBody, null, 2));

        const response = await fetch(`${API_BASE_URL}/lectures/stats`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 조회 성공!');
            const stats = result.data;
            console.log('📊 강의 통계:');
            console.log(`   강의명: ${stats.LECTURE_NAME}`);
            console.log(`   총 수강생: ${stats.TOTAL_STUDENTS}명`);
            console.log(`   활성 수강생: ${stats.ACTIVE_STUDENTS}명`);
            console.log(`   총 과제 수: ${stats.TOTAL_ASSIGNMENTS}개`);
            console.log(`   평균 제출률: ${stats.AVERAGE_SUBMISSION_RATE}%`);
            console.log(`   전체 평균 점수: ${stats.AVERAGE_SCORE}점`);
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 수강생 검색 ==========
async function searchStudents() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const lectureIdx = parseInt(prompt('📚 LECTURE_IDX:', '1'));
    const keyword = prompt('🔍 검색어 (이름 또는 학번):', '');
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    if (!keyword) {
        console.log('❌ 검색어를 입력하세요.');
        return;
    }

    console.log('\n🔍 수강생 검색');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const url = `${API_BASE_URL}/lectures/${lectureIdx}/students/search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 검색 성공!');
            console.log(`📊 총 ${result.data.totalElements}명 검색됨`);
            console.log('📋 검색 결과:');
            result.data.content.forEach((student, idx) => {
                console.log(`\n${idx + 1}. ${student.STUDENT_NAME} (${student.STUDENT_NO})`);
                console.log(`   IDX: ${student.STUDENT_IDX}`);
                console.log(`   학과: ${student.DEPARTMENT || 'N/A'}`);
            });
        } else {
            console.log('❌ 검색 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 도움말 ==========
function help() {
    console.log('\n👨‍🏫 교수 수강생 관리 테스트 함수 목록');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 먼저 로그인하세요!');
    console.log('📁 docs/일반유저 로그인+게시판/test-1-login.js → await login()');
    console.log('📋 getStudents()           - 수강생 목록 조회');
    console.log('🔍 getStudentDetail()      - 수강생 상세 조회');
    console.log('📊 getStudentGrades()      - 수강생 성적 조회');
    console.log('📊 getLectureStatistics()  - 강의 통계 조회');
    console.log('🔍 searchStudents()        - 수강생 검색');
}

// 초기 메시지
console.log('✅ 교수 수강생 관리 테스트 스크립트 로드 완료!');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');

// ═══════════════════════════════════════════════════════════════════
// 🚀 빠른 실행 명령어 (테스터용)
// ═══════════════════════════════════════════════════════════════════
console.log('\n' + '═'.repeat(63));
console.log('🚀 빠른 실행 명령어');
console.log('═'.repeat(63));
console.log('await getStudents()            // 수강생 목록');
console.log('await getStudentDetail()       // 수강생 상세');
console.log('await getStudentGrades()       // 수강생 성적');
console.log('await getLectureStatistics()   // 강의 통계');
console.log('await searchStudents()         // 수강생 검색');
console.log('help()                         // 전체 도움말');
console.log('═'.repeat(63) + '\n');
