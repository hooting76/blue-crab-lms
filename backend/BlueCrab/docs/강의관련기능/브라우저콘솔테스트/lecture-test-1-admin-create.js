// ===================================================================
// 📚 관리자 강의 관리 테스트 (POST 방식)
// Blue Crab LMS - Phase 9: 백엔드 필터링 구현 완료
// 
// ⚠️ 사전 준비: 먼저 관리자 계정으로 로그인하세요!
// 📁 위치: docs/관리자 로그인/admin-login-to-board-test.js
// 📝 실행: await adminLogin() → await sendAuthCode() → await verifyAuthCode()
// 
// 🆕 Phase 9 기능:
//    - 백엔드 필터링: 학부/학과 코드 기반 수강 가능 강의 필터링
//    - 0값 규칙: LEC_MCODE/LEC_MCODE_DEP = "0" → 전체 학생 수강 가능
//    - 전공/부전공 매칭: 학생의 전공 OR 부전공이 강의 코드와 일치하면 수강 가능
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// ========== 로그인 상태 확인 ==========
function checkAuth() {
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    if (!token) {
        console.log('\n⚠️ 로그인이 필요합니다!');
        console.log('🔧 docs/관리자 로그인/admin-login-to-board-test.js 실행');
        console.log('   1. await adminLogin()');
        console.log('   2. await sendAuthCode()');
        console.log('   3. await verifyAuthCode()');
        return false;
    }
    console.log('✅ 로그인 확인됨');
    return true;
}

// ========== 강의 목록 조회 ==========
async function getLectures() {
    if (!checkAuth()) return;
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));
    const professor = prompt('👨‍🏫 교수 코드 (선택, 공백=전체):', '');
    const year = prompt('📅 학년도 (선택, 공백=전체):', '');
    const semester = prompt('📅 학기 (선택, 1/2, 공백=전체):', '');

    console.log('\n📚 강의 목록 조회 - POST');
    console.log('═══════════════════════════════════════════════════════');
    
    try {
        const requestBody = { page, size };
        if (professor) requestBody.professor = professor;
        if (year) requestBody.year = parseInt(year);
        if (semester) requestBody.semester = parseInt(semester);

        const response = await fetch(`${API_BASE_URL}/lectures`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        const data = await response.json();
        if (data.success) {
            console.log(`\n✅ 총 ${data.data.totalElements}개 강의\n`);
            data.data.content.forEach((lec, i) => {
                console.log(`${i+1}. ${lec.lecName} (IDX:${lec.lecIdx})`);
                console.log(`   📋 코드: ${lec.lecCode || 'N/A'}`);
                console.log(`   👨‍🏫 교수: ${lec.lecProfName || 'N/A'}`);
                console.log(`   📅 ${lec.lecYear}년 ${lec.lecSemester}학기`);
                console.log(`   👥 수강인원: ${lec.lecCurrent}/${lec.lecMax}명`);
                console.log(`   🏢 강의실: ${lec.lecRoom || 'N/A'}`);
                console.log(`   ⏰ 시간: ${lec.lecTime || 'N/A'}`);
                
                // 🆕 Phase 9: 백엔드 필터링 정보
                if (lec.lecMcode || lec.lecMcodeDep) {
                    console.log(`   🎓 필터링 정보:`);
                    console.log(`      - 학부 코드: ${lec.lecMcode || '0 (전체 가능)'}`);
                    console.log(`      - 학과 코드: ${lec.lecMcodeDep || '0 (전체 가능)'}`);
                    
                    if (lec.lecMcode === '0' && lec.lecMcodeDep === '0') {
                        console.log(`      ✅ 모든 학생 수강 가능 (0값 규칙)`);
                    } else if (lec.lecMcode === '0') {
                        console.log(`      ✅ 학과 ${lec.lecMcodeDep} 학생만 수강 가능`);
                    } else if (lec.lecMcodeDep === '0') {
                        console.log(`      ✅ 학부 ${lec.lecMcode} 학생만 수강 가능`);
                    } else {
                        console.log(`      ✅ 학부 ${lec.lecMcode} + 학과 ${lec.lecMcodeDep} 학생만 수강 가능`);
                    }
                }
                console.log('');
            });
            if (data.data.content.length > 0) window.lastLectureIdx = data.data.content[0].lecIdx;
        } else {
            console.log('❌ 조회 실패:', data.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 상세 조회 ==========
async function getLectureDetail() {
    if (!checkAuth()) return;
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    const lecIdx = parseInt(prompt('🔍 조회할 LECTURE_IDX:', window.lastLectureIdx || '1'));

    console.log('\n📚 강의 상세 조회 - POST');
    console.log('═══════════════════════════════════════════════════════');
    
    try {
        const response = await fetch(`${API_BASE_URL}/lectures/detail`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ lecIdx })
        });

        const data = await response.json();
        if (data.success) {
            const lec = data.data;
            console.log(`\n✅ 강의 정보:`);
            console.log(`📋 강의명: ${lec.lecName} (${lec.lecCode || 'N/A'})`);
            console.log(`👨‍🏫 교수: ${lec.lecProfName || 'N/A'}`);
            console.log(`📅 학년도/학기: ${lec.lecYear}년 ${lec.lecSemester}학기`);
            console.log(`👥 수강인원: ${lec.lecCurrent}/${lec.lecMax}명`);
            console.log(`🏢 강의실: ${lec.lecRoom || 'N/A'}`);
            console.log(`⏰ 시간: ${lec.lecTime || 'N/A'}`);
            
            // 🆕 Phase 9: 백엔드 필터링 정보
            console.log(`\n🎓 수강 자격 정보 (Phase 9):`);
            console.log(`   학부 코드 (LEC_MCODE): ${lec.lecMcode || '0 (제한없음)'}`);
            console.log(`   학과 코드 (LEC_MCODE_DEP): ${lec.lecMcodeDep || '0 (제한없음)'}`);
            
            if (lec.lecMcode === '0' && lec.lecMcodeDep === '0') {
                console.log(`   ✅ 모든 학생 수강 가능 (0값 규칙)`);
            } else {
                console.log(`   ✅ 조건: 학생의 전공 OR 부전공이 일치해야 함`);
            }
            
            if (lec.lecMin) {
                console.log(`   최소 학년: ${lec.lecMin}학년 이상 (⏸️ 미구현)`);
            }
            
            window.lastLectureIdx = lec.lecIdx;
        } else {
            console.log('❌ 조회 실패:', data.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 통계 조회 ==========
async function getLectureStats() {
    if (!checkAuth()) return;
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    const lecIdx = parseInt(prompt('📊 통계 조회할 LECTURE_IDX:', window.lastLectureIdx || '1'));

    console.log('\n📊 강의 통계 조회 - POST');
    console.log('═══════════════════════════════════════════════════════');
    
    try {
        const response = await fetch(`${API_BASE_URL}/lectures/stats`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ lecIdx })
        });

        const data = await response.json();
        if (data.success) {
            const s = data.data;
            console.log('\n✅ 통계 정보:');
            console.log(`👥 수강생: ${s.totalStudents}명`);
            console.log(`📄 총 과제: ${s.totalAssignments}개`);
            console.log(`✅ 제출된 과제: ${s.submittedAssignments}개`);
            console.log(`📊 제출률: ${s.submissionRate}%`);
        } else {
            console.log('❌ 조회 실패:', data.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 도움말 ==========
function help() {
    console.log('\n📚 관리자 강의 관리 테스트 함수 목록 (Phase 9)');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 먼저 관리자 로그인하세요!');
    console.log('📁 docs/관리자 로그인/admin-login-to-board-test.js');
    console.log('   1. await adminLogin()');
    console.log('   2. await sendAuthCode()');
    console.log('   3. await verifyAuthCode()');
    console.log('');
    console.log('🔐 checkAuth()           - 로그인 상태 확인');
    console.log('📋 getLectures()         - 강의 목록 조회 (필터링 정보 포함)');
    console.log('🔍 getLectureDetail()    - 강의 상세 조회 (수강 자격 정보)');
    console.log('📊 getLectureStats()     - 강의 통계 조회');
    console.log('');
    console.log('🆕 Phase 9 백엔드 필터링 기능:');
    console.log('   ✅ 학부/학과 코드 기반 수강 자격 확인');
    console.log('   ✅ 0값 규칙: "0" = 모든 학생 수강 가능');
    console.log('   ✅ 전공 OR 부전공 매칭 지원');
    console.log('═══════════════════════════════════════════════════════');
    console.log('📡 API 엔드포인트:');
    console.log('   POST /lectures - 강의 목록');
    console.log('   POST /lectures/detail - 강의 상세');
    console.log('   POST /lectures/stats - 강의 통계');
}

// ========== 초기 메시지 ==========
console.log('✅ 관리자 강의 관리 테스트 스크립트 로드 완료! (Phase 9)');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');
console.log('🆕 Phase 9: 백엔드 필터링 구현 완료 - 학부/학과 코드 기반 수강 자격 확인');

