// ===================================================================
// 📝 학생 수강신청 테스트
// Blue Crab LMS - 학생 수강신청 및 조회 테스트
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
        console.log('� docs/일반유저 로그인+게시판/test-1-login.js → await login()');
        return false;
    }
    return true;
}

// ========== 수강 가능한 강의 목록 조회 ==========
async function getAvailableLectures() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken;
    
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));
    const year = prompt('📅 연도:', new Date().getFullYear().toString());
    const semester = prompt('📅 학기 (1/2):', '1');

    try {
        const url = `${API_BASE_URL}/lectures/available?page=${page}&size=${size}&year=${year}&semester=${semester}`;
        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();

        if (result.success) {
            console.log(`\n✅ 수강 가능 강의 ${result.data.totalElements}개`);
            result.data.content.forEach((lecture, idx) => {
                console.log(`${idx + 1}. ${lecture.LECTURE_NAME} (${lecture.LECTURE_CODE}) - ${lecture.PROFESSOR_NAME || 'N/A'} - ${lecture.CURRENT_STUDENTS || 0}/${lecture.MAX_STUDENTS}명`);
            });
        } else {
            console.log('❌ 조회 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 수강 신청 ==========
async function enrollLecture() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken;
    
    const lectureIdx = parseInt(prompt('📝 수강신청할 LECTURE_IDX:', '1'));

    try {
        const response = await fetch(`${API_BASE_URL}/enrollments`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ LECTURE_IDX: lectureIdx })
        });

        const result = await response.json();

        if (result.success) {
            console.log(`✅ 수강신청 성공! (ID: ${result.data.ENROLLMENT_IDX})`);
            window.lastEnrollmentIdx = result.data.ENROLLMENT_IDX;
        } else {
            console.log('❌ 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러:', error.message);
    }
}

// ========== 내 수강 목록 조회 ==========
async function getMyEnrollments() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken;
    
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n📚 내 수강 목록 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const url = `${API_BASE_URL}/enrollments?page=${page}&size=${size}`;
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
            console.log(`📊 총 ${result.data.totalElements}개 수강`);
            console.log('📋 수강 목록:');
            result.data.content.forEach((enrollment, idx) => {
                console.log(`\n${idx + 1}. ${enrollment.LECTURE_NAME}`);
                console.log(`   ENROLLMENT_IDX: ${enrollment.ENROLLMENT_IDX}`);
                console.log(`   교수: ${enrollment.PROFESSOR_NAME}`);
                console.log(`   상태: ${enrollment.STATUS}`);
                console.log(`   신청일: ${enrollment.ENROLLED_AT}`);
            });
        } else {
            console.log('❌ 조회 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 수강 취소 ==========
async function cancelEnrollment() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken;
    
    const enrollmentIdx = parseInt(prompt('🗑️ 취소할 ENROLLMENT_IDX:', window.lastEnrollmentIdx || '1'));
    const confirm = prompt('⚠️ 정말 수강취소 하시겠습니까? (yes/no):', 'no');

    if (confirm.toLowerCase() !== 'yes') {
        console.log('❌ 취소 중단됨');
        return;
    }

    console.log('\n🗑️ 수강 취소');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/enrollments/${enrollmentIdx}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 수강취소 성공!');
            console.log('📊 결과:', result.message);
        } else {
            console.log('❌ 수강취소 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 상세 조회 ==========
async function getLectureDetail() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken;
    
    const lectureIdx = parseInt(prompt('🔍 조회할 LECTURE_IDX:', '1'));

    console.log('\n📚 강의 상세 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/lectures/${lectureIdx}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 조회 성공!');
            console.log('📊 강의 정보:');
            const lecture = result.data;
            console.log(`   강의명: ${lecture.LECTURE_NAME}`);
            console.log(`   강의코드: ${lecture.LECTURE_CODE}`);
            console.log(`   교수: ${lecture.PROFESSOR_NAME || 'N/A'}`);
            console.log(`   학점: ${lecture.CREDIT}`);
            console.log(`   정원: ${lecture.CURRENT_STUDENTS || 0}/${lecture.MAX_STUDENTS}`);
            console.log(`   시간: ${lecture.LECTURE_TIME}`);
            console.log(`   강의실: ${lecture.LECTURE_ROOM}`);
            console.log(`   설명: ${lecture.LECTURE_DESCRIPTION || 'N/A'}`);
        } else {
            console.log('❌ 조회 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 도움말 ==========
function help() {
    console.log('\n📝 학생 수강신청 테스트 함수 목록');
    console.log('═══════════════════════════════════════════════════════');
    console.log('� checkAuth()             - 로그인 상태 확인');
    console.log('📋 getAvailableLectures()  - 수강 가능 강의 목록');
    console.log('📝 enrollLecture()         - 수강 신청');
    console.log('📚 getMyEnrollments()      - 내 수강 목록');
    console.log('🗑️ cancelEnrollment()      - 수강 취소');
    console.log('🔍 getLectureDetail()      - 강의 상세 조회');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 사전 준비:');
    console.log('   1. docs/일반유저 로그인+게시판/test-1-login.js 실행');
    console.log('   2. await login() 실행 (학생 계정)');
    console.log('   3. 로그인 완료 후 이 파일의 함수들 사용');
}

// 초기 메시지
console.log('✅ 학생 수강신청 테스트 스크립트 로드 완료!');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');
console.log('⚠️ 먼저 학생 계정으로 로그인하세요! (checkAuth()로 확인 가능)');
