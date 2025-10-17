// ===================================================================
// 📊 관리자 통계 및 모니터링 테스트
// Blue Crab LMS - 관리자 강의 통계 조회 테스트
// 
// ⚠️ 사전 준비: 먼저 관리자 계정으로 로그인하세요!
// 📁 위치: docs/관리자 로그인/admin-login-to-board-test.js
// 📝 실행: await adminLogin() → await sendAuthCode() → await verifyAuthCode()
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

// 전역 변수 (admin-login-to-board-test.js에서 설정한 토큰 사용)
if (typeof window.authToken === 'undefined') window.authToken = null;

// ========== 로그인 상태 확인 ==========
function checkAuth() {
    const token = window.authToken;
    const user = window.currentUser;
    
    if (!token) {
        console.log('\n⚠️ 로그인이 필요합니다!');
        console.log('🔧 docs/관리자 로그인/admin-login-to-board-test.js 실행');
        console.log('   1. await adminLogin()');
        console.log('   2. await sendAuthCode()');
        console.log('   3. await verifyAuthCode()');
        return false;
    }
    return true;
}

// ========== 전체 강의 통계 조회 ==========
async function getLectureStatistics() {
    if (!checkAuth()) return;
    const token = window.authToken;
    const targetGrade = prompt('� 대상 학년 (1-4학년, 공란=전체):', ''); // LEC_YEAR = 강의 대상 학년
    const semester = parseInt(prompt('📅 학기 (1/2):', '1'));

    console.log('\n📊 전체 강의 통계 조회');
    console.log('═══════════════════════════════════════════════════════');
    console.log('💡 LEC_YEAR = 대상 학년 (1~4학년)');

    try {
        const url = `${API_BASE_URL}/admin/statistics/lectures?year=${targetGrade}&semester=${semester}`;
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
            const stats = result.data;
            console.log('📊 강의 통계:');
            console.log(`   총 강의 수: ${stats.TOTAL_LECTURES}개`);
            console.log(`   활성 강의: ${stats.ACTIVE_LECTURES}개`);
            console.log(`   폐강 강의: ${stats.CANCELLED_LECTURES}개`);
            console.log(`   총 수강생: ${stats.TOTAL_ENROLLMENTS}명`);
            console.log(`   평균 수강률: ${stats.AVERAGE_ENROLLMENT_RATE}%`);
            console.log(`   평균 정원: ${stats.AVERAGE_CAPACITY}명`);
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 학생별 통계 조회 ==========
async function getStudentStatistics() {
    if (!checkAuth()) return;
    const token = window.authToken;
    const studentIdx = parseInt(prompt('👨‍🎓 STUDENT_IDX:', '1'));

    console.log('\n📊 학생별 통계 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/statistics/students/${studentIdx}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 조회 성공!');
            const stats = result.data;
            console.log('📊 학생 통계:');
            console.log(`   학생명: ${stats.STUDENT_NAME}`);
            console.log(`   학번: ${stats.STUDENT_NO}`);
            console.log(`   총 수강 강의: ${stats.TOTAL_LECTURES}개`);
            console.log(`   활성 수강: ${stats.ACTIVE_ENROLLMENTS}개`);
            console.log(`   총 과제 수: ${stats.TOTAL_ASSIGNMENTS}개`);
            console.log(`   제출 과제: ${stats.SUBMITTED_ASSIGNMENTS}개`);
            console.log(`   평균 점수: ${stats.AVERAGE_SCORE}점`);
            console.log(`   평균 제출률: ${stats.SUBMISSION_RATE}%`);
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 교수별 통계 조회 ==========
async function getProfessorStatistics() {
    if (!checkAuth()) return;
    const token = window.authToken;
    const professorIdx = parseInt(prompt('👨‍🏫 PROFESSOR_IDX:', '1'));

    console.log('\n📊 교수별 통계 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/statistics/professors/${professorIdx}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 조회 성공!');
            const stats = result.data;
            console.log('📊 교수 통계:');
            console.log(`   교수명: ${stats.PROFESSOR_NAME}`);
            console.log(`   담당 강의 수: ${stats.TOTAL_LECTURES}개`);
            console.log(`   활성 강의: ${stats.ACTIVE_LECTURES}개`);
            console.log(`   총 수강생: ${stats.TOTAL_STUDENTS}명`);
            console.log(`   총 과제 수: ${stats.TOTAL_ASSIGNMENTS}개`);
            console.log(`   평균 과제 점수: ${stats.AVERAGE_ASSIGNMENT_SCORE}점`);
            console.log(`   평균 강의 평가: ${stats.AVERAGE_LECTURE_RATING}점`);
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 학기별 트렌드 조회 ==========
async function getSemesterTrends() {
    if (!checkAuth()) return;
    const token = window.authToken;
    const startYear = parseInt(prompt('📅 시작 연도:', (new Date().getFullYear() - 1).toString()));
    const endYear = parseInt(prompt('📅 종료 연도:', new Date().getFullYear().toString()));

    console.log('\n📊 학기별 트렌드 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const url = `${API_BASE_URL}/statistics/trends?startYear=${startYear}&endYear=${endYear}`;
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
            console.log('📊 학기별 트렌드:');
            if (result.data.TRENDS && result.data.TRENDS.length > 0) {
                result.data.TRENDS.forEach((trend, idx) => {
                    console.log(`\n${idx + 1}. ${trend.YEAR}-${trend.SEMESTER}학기`);
                    console.log(`   강의 수: ${trend.LECTURE_COUNT}개`);
                    console.log(`   수강생 수: ${trend.STUDENT_COUNT}명`);
                    console.log(`   평균 수강률: ${trend.ENROLLMENT_RATE}%`);
                });
            }
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 인기 강의 순위 조회 ==========
async function getPopularLectures() {
    if (!checkAuth()) return;
    const token = window.authToken;
    const year = parseInt(prompt('📅 연도:', new Date().getFullYear().toString()));
    const semester = parseInt(prompt('📅 학기 (1/2):', '1'));
    const limit = parseInt(prompt('🔢 조회 개수:', '10'));

    console.log('\n🏆 인기 강의 순위 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const url = `${API_BASE_URL}/statistics/popular-lectures?year=${year}&semester=${semester}&limit=${limit}`;
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
            console.log('🏆 인기 강의 TOP ' + limit);
            if (result.data.LECTURES && result.data.LECTURES.length > 0) {
                result.data.LECTURES.forEach((lecture, idx) => {
                    console.log(`\n${idx + 1}위. ${lecture.LECTURE_NAME}`);
                    console.log(`   교수: ${lecture.PROFESSOR_NAME}`);
                    console.log(`   수강생: ${lecture.STUDENT_COUNT}명 (${lecture.ENROLLMENT_RATE}%)`);
                    console.log(`   평가점수: ${lecture.RATING || 'N/A'}점`);
                });
            }
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 학과별 통계 조회 ==========
async function getDepartmentStatistics() {
    if (!checkAuth()) return;
    const token = window.authToken;
    const year = parseInt(prompt('📅 연도:', new Date().getFullYear().toString()));
    const semester = parseInt(prompt('📅 학기 (1/2):', '1'));

    console.log('\n📊 학과별 통계 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const url = `${API_BASE_URL}/statistics/departments?year=${year}&semester=${semester}`;
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
            console.log('📊 학과별 통계:');
            if (result.data.DEPARTMENTS && result.data.DEPARTMENTS.length > 0) {
                result.data.DEPARTMENTS.forEach((dept, idx) => {
                    console.log(`\n${idx + 1}. ${dept.DEPARTMENT_NAME}`);
                    console.log(`   개설 강의: ${dept.LECTURE_COUNT}개`);
                    console.log(`   수강생 수: ${dept.STUDENT_COUNT}명`);
                    console.log(`   평균 수강률: ${dept.ENROLLMENT_RATE}%`);
                });
            }
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 도움말 ==========
function help() {
    console.log('\n📊 관리자 통계 테스트 함수 목록');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 먼저 관리자 로그인하세요!');
    console.log('📁 docs/관리자 로그인/admin-login-to-board-test.js');
    console.log('   1. await adminLogin()');
    console.log('   2. await sendAuthCode()');
    console.log('   3. await verifyAuthCode()');
    console.log('📊 getLectureStatistics()     - 전체 강의 통계');
    console.log('👨‍🎓 getStudentStatistics()     - 학생별 통계');
    console.log('👨‍🏫 getProfessorStatistics()   - 교수별 통계');
    console.log('📈 getSemesterTrends()        - 학기별 트렌드');
    console.log('🏆 getPopularLectures()       - 인기 강의 순위');
    console.log('🏢 getDepartmentStatistics()  - 학과별 통계');
}

// 초기 메시지
console.log('✅ 관리자 통계 테스트 스크립트 로드 완료!');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');
