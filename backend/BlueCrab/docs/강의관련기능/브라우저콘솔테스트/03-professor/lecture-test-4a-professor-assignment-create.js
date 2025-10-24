// ===================================================================
// 📄 교수 과제 관리 테스트 (Part A: 과제 생성 및 목록 조회)
// Blue Crab LMS - 교수 과제 생성 및 관리 테스트
// 
// ⚠️ 사전 준비: 먼저 교수 계정으로 로그인하세요!
// 📁 위치: docs/일반유저 로그인+게시판/test-1-login.js
// 📝 실행: await login() (교수 계정 사용)
// 
// 📂 파일 구조:
//    Part A: 이 파일 (과제 생성 및 목록 조회)
//    Part B: lecture-test-4b-professor-assignment-grade.js (과제 채점 및 관리)
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

// ========== 내 강의 목록 조회 ==========
async function getMyLectures() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    // 백엔드에서 받은 사용자 정보 사용
    console.log('🔄 사용자 정보 확인 중...');
    const userInfo = getUserInfo();
    
    if (!userInfo) {
        console.log('⚠️ 사용자 정보를 가져올 수 없습니다.');
        console.log('💡 다시 로그인하세요: await login()');
        return;
    }
    
    const professor = userInfo.userIdx;
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n📚 담당 강의 목록 조회');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`👨‍🏫 교수번호: ${professor}`);

    try {
        const url = `${API_BASE_URL}/lectures`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                professor: professor,
                page: page,
                size: size
            })
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
                console.log(`   강의 코드: ${lecture.lecSerial || lecture.LEC_SERIAL}`);
                console.log(`   교수코드: ${lecture.lecProf || lecture.LEC_PROF}`);
                console.log(`   교수명: ${lecture.lecProfName || lecture.LEC_PROF_NAME || 'N/A'}`);
                console.log(`   학기: ${lecture.lecYear || lecture.LEC_YEAR}-${lecture.lecSemester || lecture.LEC_SEMESTER}`);
                console.log(`   정원: ${lecture.lecCurrent || 0}/${lecture.lecMany || lecture.LEC_MANY}명`);
                
                // 첫 번째 강의 코드 저장
                if (idx === 0) {
                    window.lastLectureSerial = lecture.lecSerial || lecture.LEC_SERIAL;
                    console.log(`   💾 저장됨: window.lastLectureSerial = ${window.lastLectureSerial}`);
                }
            });
        } else if (result.success) {
            // success 래핑된 응답
            console.log('\n✅ 조회 성공!');
            console.log(`📊 총 ${result.data.totalElements}개 강의`);
            console.log('📋 강의 목록:');
            result.data.content.forEach((lecture, idx) => {
                console.log(`\n${idx + 1}. ${lecture.lecTit || lecture.LEC_TIT} (${lecture.lecSerial || lecture.LEC_SERIAL})`);
                console.log(`   강의 코드: ${lecture.lecSerial || lecture.LEC_SERIAL}`);
                console.log(`   교수코드: ${lecture.lecProf || lecture.LEC_PROF}`);
                console.log(`   교수명: ${lecture.lecProfName || lecture.LEC_PROF_NAME || 'N/A'}`);
                console.log(`   학기: ${lecture.lecYear || lecture.LEC_YEAR}-${lecture.lecSemester || lecture.LEC_SEMESTER}`);
                console.log(`   정원: ${lecture.lecCurrent || 0}/${lecture.lecMany || lecture.LEC_MANY}명`);
                
                // 첫 번째 강의 코드 저장
                if (idx === 0) {
                    window.lastLectureSerial = lecture.lecSerial || lecture.LEC_SERIAL;
                    console.log(`   💾 저장됨: window.lastLectureSerial = ${window.lastLectureSerial}`);
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
    
    const lecSerial = window.lastLectureSerial || prompt('📚 강의 코드 (예: CS101):', 'CS101');
    const title = prompt('📝 과제 제목:', '1주차 과제');
    const description = prompt('📝 과제 설명:', '자바 프로그래밍 기초 과제입니다.');
    const maxScore = parseInt(prompt('💯 과제 배점 (점수):', '10'));
    const dueDate = prompt('📅 마감일 (YYYYMMDD):', '20251231');

    if (!title || !lecSerial) {
        console.log('❌ 필수 입력값이 없습니다.');
        return;
    }

    console.log('\n📄 과제 생성');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📚 강의 코드: ${lecSerial}`);
    console.log(`📝 제목: ${title}`);
    console.log(`📅 마감일: ${dueDate}`);
    console.log(`💯 배점: ${maxScore}점`);

    // ✅ DTO 패턴 - camelCase 필드명 사용
    const assignmentData = {
        lecSerial: lecSerial,
        title: title,
        body: description,
        maxScore: maxScore,
        dueDate: dueDate
    };

    console.log('\n📤 요청 데이터:');
    console.log(JSON.stringify(assignmentData, null, 2));

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

        console.log('\n🔍 전체 응답:');
        console.log(JSON.stringify(result, null, 2));

        if (response.ok || result.success) {
            console.log('\n✅ 과제 생성 성공!');
            const assignment = result.data || result;
            console.log('📊 생성된 과제:');
            console.log(`   IDX: ${assignment.assignmentIdx || assignment.ASSIGNMENT_IDX}`);
            console.log(`   제목: ${assignment.title || assignment.ASSIGNMENT_TITLE}`);
            console.log(`   마감일: ${assignment.dueDate || assignment.DUE_DATE}`);
            console.log(`   배점: ${assignment.maxScore || assignment.MAX_SCORE}점`);
            
            window.lastAssignmentIdx = assignment.assignmentIdx || assignment.ASSIGNMENT_IDX;
            console.log(`💾 저장됨: window.lastAssignmentIdx = ${window.lastAssignmentIdx}`);
            console.log('💡 다음 단계: lecture-test-4b-professor-assignment-grade.js로 채점하세요!');
        } else {
            console.log('❌ 과제 생성 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 목록 조회 ==========
async function getAssignments() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const lecSerial = window.lastLectureSerial || prompt('📚 강의 코드 (예: CS101):', 'CS101');
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n📄 과제 목록 조회');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📚 강의 코드: ${lecSerial}`);

    try {
        // ✅ DTO 패턴: POST 방식으로 변경
        const url = `${API_BASE_URL}/assignments/list`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                lecSerial: lecSerial,
                page: page,
                size: size,
                action: 'list'  // 목록 조회 액션
            })
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        
        // 응답 텍스트 먼저 확인
        const responseText = await response.text();
        console.log('\n📄 응답 텍스트 (원본):');
        console.log(responseText);
        
        // JSON 파싱 시도
        let result;
        try {
            result = JSON.parse(responseText);
            console.log('\n🔍 파싱된 JSON:');
            console.log(JSON.stringify(result, null, 2));
        } catch (parseError) {
            console.log('❌ JSON 파싱 실패:', parseError.message);
            console.log('서버가 JSON이 아닌 응답을 반환했습니다.');
            return;
        }

        // Page 객체 응답 처리
        if (result.content) {
            console.log(`\n✅ 조회 성공! 총 ${result.totalElements}개 과제`);
            console.log('📋 과제 목록:');
            result.content.forEach((assignment, idx) => {
                console.log(`\n${idx + 1}. 과제 IDX: ${assignment.assignmentIdx}`);
                console.log(`   강의 IDX: ${assignment.lecIdx}`);
                console.log(`   생성일: ${assignment.createdDate}`);
                
                // assignmentData JSON 파싱
                try {
                    const data = JSON.parse(assignment.assignmentData || '{}');
                    const assignmentInfo = data.assignment || {};
                    console.log(`   제목: ${assignmentInfo.title || 'N/A'}`);
                    console.log(`   설명: ${assignmentInfo.description || 'N/A'}`);
                    console.log(`   마감일: ${assignmentInfo.dueDate || 'N/A'}`);
                    console.log(`   배점: ${assignmentInfo.maxScore || 'N/A'}점`);
                } catch (e) {
                    console.log(`   ⚠️ JSON 파싱 실패: ${e.message}`);
                }
                
                // 첫 번째 과제 IDX 저장
                if (idx === 0) {
                    window.lastAssignmentIdx = assignment.assignmentIdx;
                    console.log(`   💾 저장됨: window.lastAssignmentIdx = ${window.lastAssignmentIdx}`);
                }
            });
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 도움말 ==========
function help() {
    console.log('\n👨‍🏫 교수 과제 관리 테스트 (Part A: 과제 생성 및 목록)');
    console.log('═══════════════════════════════════════════════════════\n');
    
    console.log('📋 제공 함수:\n');
    console.log('await checkAuth()             - 로그인 상태 확인');
    console.log('getUserInfo()                 - 사용자 정보 추출');
    console.log('debugUserInfo()               - 사용자 정보 디버깅');
    console.log('await getMyLectures()         - 담당 강의 목록 (자동 USER_IDX)');
    console.log('await createAssignment()      - 과제 생성 (10점 고정)');
    console.log('await getAssignments()        - 과제 목록 조회\n');
    
    console.log('📂 관련 파일:');
    console.log('   Part B: lecture-test-4b-professor-assignment-grade.js');
    console.log('   - 학생별 제출 현황 조회');
    console.log('   - 과제 채점 (제출 방식 + 점수)');
    console.log('   - 과제 수정');
    console.log('   - 과제 삭제\n');
    
    console.log('═══════════════════════════════════════════════════════\n');
    console.log('💡 백엔드에서 사용자 정보를 받아 자동으로 사용');
    console.log('💡 JWT 디코딩은 백엔드에서 처리됩니다');
    console.log('💡 과제 시스템: 오프라인 제출 + DB는 점수만 기록');
    console.log('💡 배점: 모든 과제는 10점 만점으로 고정');
    console.log('💡 window.lastLectureIdx, window.lastAssignmentIdx 자동 저장');
}

// 초기 메시지
console.log('✅ 교수 과제 관리 테스트 스크립트 로드 완료! (Part A: 과제 생성 및 목록 조회)');
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
        console.log('💡 이제 getMyLectures() 실행 시 자동으로 사용됩니다.');
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
console.log('await getMyLectures()      // 내 강의 목록');
console.log('await createAssignment()   // 과제 생성');
console.log('await getAssignments()     // 과제 목록');
console.log('debugUserInfo()            // 사용자 정보 디버깅');
console.log('help()                     // 전체 도움말');
console.log('═'.repeat(63) + '\n');
