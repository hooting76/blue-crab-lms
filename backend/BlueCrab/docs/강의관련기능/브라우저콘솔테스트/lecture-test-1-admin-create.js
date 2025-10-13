// ===================================================================
// 📚 관리자 강의 등록 테스트
// Blue Crab LMS - 관리자 강의 생성 및 관리 테스트
// 
// ⚠️ 사전 준비: 먼저 관리자 로그인 테스트 파일을 실행하세요!
// 📁 위치: docs/관리자 로그인/admin-login-to-board-test.js
// 📝 실행: adminLogin() 또는 quickAdminLogin()
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/admin';

// ========== 로그인 상태 확인 ==========
function checkAuth() {
    console.log('\n🔍 로그인 상태 확인');
    console.log('═══════════════════════════════════════════════════════');
    
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    const user = window.currentUser;
    
    console.log(`🔑 JWT 토큰: ${token ? '보유 (' + token.substring(0, 20) + '...)' : '❌ 없음'}`);
    console.log(`👤 사용자 정보: ${user ? user.userName + ' (' + user.role + ')' : '❌ 없음'}`);
    
    if (!token) {
        console.log('\n⚠️ 로그인이 필요합니다!');
        console.log('🔧 해결 방법:');
        console.log('   1. docs/관리자 로그인/admin-login-to-board-test.js 파일 실행');
        console.log('   2. adminLogin() 또는 quickAdminLogin() 실행');
        console.log('   3. 로그인 완료 후 이 파일의 함수들 사용');
        return false;
    }
    
    console.log('\n✅ 로그인됨 - 테스트 실행 가능!');
    return true;
}

// ========== 강의 등록 테스트 ==========
async function createLecture() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const lectureName = prompt('📝 강의명을 입력하세요:', '자바 프로그래밍');
    const lectureCode = prompt('📝 강의 코드를 입력하세요:', 'CS101');
    const professorIdx = parseInt(prompt('👨‍🏫 교수 IDX를 입력하세요:', '1'));
    const maxStudents = parseInt(prompt('👥 최대 정원을 입력하세요:', '30'));
    const year = parseInt(prompt('📅 연도를 입력하세요:', new Date().getFullYear().toString()));
    const semester = parseInt(prompt('📅 학기를 입력하세요 (1/2):', '1'));

    if (!lectureName || !lectureCode) {
        console.log('❌ 필수 입력값이 없습니다.');
        return;
    }

    console.log('\n📚 강의 등록 테스트');
    console.log('═══════════════════════════════════════════════════════');
    
    const lectureData = {
        LECTURE_NAME: lectureName,
        LECTURE_CODE: lectureCode,
        LECTURE_DESCRIPTION: `${lectureName} 강의입니다.`,
        MAX_STUDENTS: maxStudents,
        CREDIT: 3,
        LECTURE_TIME: '월1,2 수3,4',
        LECTURE_ROOM: '공학관 101호',
        PROFESSOR_IDX: professorIdx,
        YEAR: year,
        SEMESTER: semester,
        START_DATE: `${year}-03-01`,
        END_DATE: `${year}-06-30`
    };

try {
        const response = await fetch(`${API_BASE_URL}/lectures`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(lectureData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        if (result.success) {
            console.log('\n✅ 강의 등록 성공!');
            console.log('📊 등록된 강의:', result.data);
            window.lastLectureIdx = result.data.LECTURE_IDX;
            console.log(`💾 저장된 LECTURE_IDX: ${window.lastLectureIdx}`);
        } else {
            console.log('❌ 강의 등록 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 목록 조회 ==========
async function getLectures() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));
    const year = prompt('📅 연도 (선택사항):', new Date().getFullYear().toString());
    const semester = prompt('📅 학기 (선택사항, 1/2):', '1');

    console.log('\n📚 강의 목록 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        let url = `${API_BASE_URL}/lectures?page=${page}&size=${size}`;
        if (year) url += `&year=${year}`;
        if (semester) url += `&semester=${semester}`;

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
            console.log(`📊 총 ${result.data.totalElements}개 강의 (${result.data.totalPages}페이지)`);
            console.log('📋 강의 목록:');
            result.data.content.forEach((lecture, idx) => {
                console.log(`\n${idx + 1}. ${lecture.LECTURE_NAME} (${lecture.LECTURE_CODE})`);
                console.log(`   교수: ${lecture.PROFESSOR_NAME || 'N/A'}`);
                console.log(`   정원: ${lecture.CURRENT_STUDENTS || 0}/${lecture.MAX_STUDENTS}`);
                console.log(`   상태: ${lecture.STATUS}`);
            });
        } else {
            console.log('❌ 조회 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 상세 조회 ==========
async function getLectureDetail() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const lectureIdx = parseInt(prompt('🔍 조회할 LECTURE_IDX:', window.lastLectureIdx || '1'));

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
        } else {
            console.log('❌ 조회 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 수정 ==========
async function updateLecture() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const lectureIdx = parseInt(prompt('✏️ 수정할 LECTURE_IDX:', window.lastLectureIdx || '1'));
    const lectureName = prompt('📝 새 강의명 (선택사항):');
    const maxStudents = prompt('👥 새 최대 정원 (선택사항):');

    console.log('\n📚 강의 수정');
    console.log('═══════════════════════════════════════════════════════');

    const updateData = {};
    if (lectureName) updateData.LECTURE_NAME = lectureName;
    if (maxStudents) updateData.MAX_STUDENTS = parseInt(maxStudents);

    if (Object.keys(updateData).length === 0) {
        console.log('❌ 수정할 내용이 없습니다.');
        return;
    }

try {
        const response = await fetch(`${API_BASE_URL}/lectures/${lectureIdx}`, {
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
            console.log('📊 수정된 강의:', result.data);
        } else {
            console.log('❌ 수정 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 삭제 (폐강) ==========
async function deleteLecture() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const lectureIdx = parseInt(prompt('🗑️ 삭제할 LECTURE_IDX:', window.lastLectureIdx || '1'));
    const confirm = prompt('⚠️ 정말 삭제하시겠습니까? (yes/no):', 'no');

    if (confirm.toLowerCase() !== 'yes') {
        console.log('❌ 삭제 취소됨');
        return;
    }

    console.log('\n📚 강의 삭제');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/lectures/${lectureIdx}`, {
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
            console.log('❌ 삭제 실패:', result.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 도움말 ==========
function help() {
    console.log('\n📚 관리자 강의 관리 테스트 함수 목록');
    console.log('═══════════════════════════════════════════════════════');
    console.log('� checkAuth()        - 로그인 상태 확인');
    console.log('📝 createLecture()    - 강의 등록');
    console.log('📋 getLectures()      - 강의 목록 조회');
    console.log('🔍 getLectureDetail() - 강의 상세 조회');
    console.log('✏️ updateLecture()    - 강의 수정');
    console.log('🗑️ deleteLecture()    - 강의 삭제');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 사전 준비:');
    console.log('   1. docs/관리자 로그인/admin-login-to-board-test.js 실행');
    console.log('   2. adminLogin() 또는 quickAdminLogin() 실행');
    console.log('   3. 로그인 완료 후 이 파일의 함수들 사용');
}

// 초기 메시지
console.log('✅ 관리자 강의 관리 테스트 스크립트 로드 완료!');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');
console.log('⚠️ 먼저 관리자 로그인을 완료하세요! (checkAuth()로 확인 가능)');
