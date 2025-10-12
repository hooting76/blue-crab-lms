// ===================================================================
// 👨‍🏫 교수 수강생 관리 테스트
// Blue Crab LMS - 교수 수강생 조회 및 관리 테스트
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/professor';

// 전역 변수 초기화
if (typeof window.authToken === 'undefined') window.authToken = null;

// ========== 수강생 목록 조회 ==========
async function getStudents() {
    const lectureIdx = parseInt(prompt('📚 LECTURE_IDX:', '1'));
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n👨‍🎓 수강생 목록 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const url = `${API_BASE_URL}/lectures/${lectureIdx}/students?page=${page}&size=${size}`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${window.authToken}`
            }
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
            console.log('❌ 조회 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 수강생 상세 조회 ==========
async function getStudentDetail() {
    const lectureIdx = parseInt(prompt('📚 LECTURE_IDX:', '1'));
    const studentIdx = parseInt(prompt('👨‍🎓 STUDENT_IDX:', '1'));

    console.log('\n👨‍🎓 수강생 상세 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/lectures/${lectureIdx}/students/${studentIdx}`, {
            headers: {
                'Authorization': `Bearer ${window.authToken}`
            }
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
            console.log('❌ 조회 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 수강생 성적 조회 ==========
async function getStudentGrades() {
    const lectureIdx = parseInt(prompt('📚 LECTURE_IDX:', '1'));
    const studentIdx = parseInt(prompt('👨‍🎓 STUDENT_IDX:', '1'));

    console.log('\n📊 수강생 성적 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/lectures/${lectureIdx}/students/${studentIdx}/grades`, {
            headers: {
                'Authorization': `Bearer ${window.authToken}`
            }
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
            console.log('❌ 조회 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 통계 조회 ==========
async function getLectureStatistics() {
    const lectureIdx = parseInt(prompt('📚 LECTURE_IDX:', '1'));

    console.log('\n📊 강의 통계 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/lectures/${lectureIdx}/statistics`, {
            headers: {
                'Authorization': `Bearer ${window.authToken}`
            }
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
            console.log('❌ 조회 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 수강생 검색 ==========
async function searchStudents() {
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
                'Authorization': `Bearer ${window.authToken}`
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
            console.log('❌ 검색 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 토큰 설정 ==========
function setToken() {
    const token = prompt('🔑 JWT 토큰을 입력하세요:');
    if (token) {
        window.authToken = token;
        localStorage.setItem('authToken', token);
        console.log('✅ 토큰 저장 완료!');
    }
}

// ========== 도움말 ==========
function help() {
    console.log('\n👨‍🏫 교수 수강생 관리 테스트 함수 목록');
    console.log('═══════════════════════════════════════════════════════');
    console.log('🔑 setToken()              - JWT 토큰 설정');
    console.log('📋 getStudents()           - 수강생 목록 조회');
    console.log('🔍 getStudentDetail()      - 수강생 상세 조회');
    console.log('📊 getStudentGrades()      - 수강생 성적 조회');
    console.log('📊 getLectureStatistics()  - 강의 통계 조회');
    console.log('🔍 searchStudents()        - 수강생 검색');
    console.log('═══════════════════════════════════════════════════════');
    console.log('💡 먼저 setToken()으로 토큰을 설정하세요!');
}

// 초기 메시지
console.log('✅ 교수 수강생 관리 테스트 스크립트 로드 완료!');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');
