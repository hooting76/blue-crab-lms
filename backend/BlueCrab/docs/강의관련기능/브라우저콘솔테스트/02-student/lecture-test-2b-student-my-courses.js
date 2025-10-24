// ===================================================================
// 📝 학생 수강 관리 테스트 (Part B: 내 수강 목록 조회 및 취소)
// Blue Crab LMS - Phase 9: 백엔드 필터링 구현 완료
// 
// ⚠️ 사전 준비: 먼저 학생 계정으로 로그인하세요!
// 📁 위치: docs/일반유저 로그인+게시판/test-1-login.js
// 📝 실행: await login() (학생 계정 사용)
// 
// 📂 파일 구조:
//    Part A: lecture-test-2a-student-enrollment.js (수강 가능 강의 조회 및 신청)
//    Part B: 이 파일 (내 수강 목록 및 취소)
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

// 전역 변수 (test-1-login.js에서 설정한 토큰 및 사용자 정보 사용)
if (typeof window.authToken === 'undefined') window.authToken = null;
if (typeof window.currentUser === 'undefined') window.currentUser = null;

// ========== 백엔드에서 받은 사용자 정보 사용 ==========
// 로그인 시 백엔드가 response.data.user에 사용자 정보를 포함하여 반환
// JWT 디코딩은 백엔드에서 처리되므로 프론트엔드에서는 필요 없음
function getUserInfo() {
    // window.currentUser는 로그인 시 설정됨 (test-1-login.js 참고)
    const user = window.currentUser;
    if (!user) return null;
    
    console.log('✅ 저장된 사용자 정보:', user);
    return {
        userIdx: user.id || user.userIdx || user.userId,
        email: user.email || user.userEmail,
        name: user.name || user.userName,
        role: user.role || user.authority
    };
}

// ========== 로그인 상태 확인 ==========
function checkAuth() {
    const token = window.authToken;
    const user = window.currentUser;
    
    if (!token) {
        console.log('\n⚠️ 로그인이 필요합니다!');
        console.log('📁 docs/일반유저 로그인+게시판/test-1-login.js → await login()');
        console.log('💡 로그인 시 백엔드가 사용자 정보를 자동으로 반환합니다.');
        return false;
    }
    
    if (!user || !user.id) {
        console.log('\n⚠️ 사용자 정보가 없습니다!');
        console.log('📁 다시 로그인해주세요: await login()');
        console.log('💡 로그인 응답의 user 객체가 window.currentUser에 저장되어야 합니다.');
        return false;
    }
    
    console.log('✅ 로그인 확인됨:', {
        userIdx: user.id,
        email: user.email,
        name: user.name
    });
    
    return true;
}

// ========== 내 수강 목록 조회 ==========
async function getMyEnrollments() {
    if (!checkAuth()) return;
    
    const token = window.authToken;
    const user = window.currentUser;
    
    // 백엔드에서 받은 사용자 정보 사용
    const userInfo = getUserInfo();
    if (!userInfo || !userInfo.userIdx) {
        console.log('❌ 사용자 정보를 불러올 수 없습니다. 다시 로그인해주세요.');
        return;
    }
    
    const studentIdx = userInfo.userIdx;
    console.log('✅ 학생 ID 확인:', studentIdx, `(${userInfo.name || userInfo.email})`)
    
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n📚 내 수강 목록 조회 - POST');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const requestBody = {
            studentIdx: studentIdx,
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

        if (response.ok) {
            const result = await response.json();
            console.log('\n✅ 조회 성공!');
            
            // 전체 응답 JSON 구조 확인
            console.log('\n🔍 전체 응답 JSON:');
            console.log(result);
            
            const enrollments = result.content || result;
            if (Array.isArray(enrollments) && enrollments.length > 0) {
                console.log(`\n📊 총 ${result.totalElements || enrollments.length}개 수강`);
                console.log('📋 수강 목록:');
                enrollments.forEach((enrollment, idx) => {
                    console.log(`\n${idx + 1}. ${enrollment.lecTit || '강의명 없음'} (${enrollment.lecSerial || 'N/A'})`);
                    console.log(`   📌 수강신청 정보:`);
                    console.log(`      ENROLLMENT_IDX: ${enrollment.enrollmentIdx}`);
                    console.log(`      등록일: ${enrollment.enrollmentDate || 'N/A'}`);
                    console.log(`      상태: ${enrollment.enrollmentStatus || 'N/A'}`);
                    
                    console.log(`   📚 강의 정보:`);
                    console.log(`      강의 ID (lecIdx): ${enrollment.lecIdx}`);
                    console.log(`      강의코드: ${enrollment.lecSerial || 'N/A'}`);
                    console.log(`      교수코드: ${enrollment.lecProf || 'N/A'}`);
                    console.log(`      교수 이름: ${enrollment.lecProfName || 'N/A'}`);
                    console.log(`      학점: ${enrollment.lecPoint !== null && enrollment.lecPoint !== undefined ? enrollment.lecPoint + '점' : 'N/A'}`);
                    console.log(`      시간: ${enrollment.lecTime || 'N/A'}`);
                    
                    // 강의 설명 (있을 경우)
                    if (enrollment.lecSummary) {
                        const summaryPreview = enrollment.lecSummary.length > 50 
                            ? enrollment.lecSummary.substring(0, 50) + '...' 
                            : enrollment.lecSummary;
                        console.log(`      📄 강의 설명: ${summaryPreview}`);
                    }
                    
                    console.log(`   👨‍🎓 학생 정보:`);
                    console.log(`      학생 ID (studentIdx): ${enrollment.studentIdx}`);
                    console.log(`      학번: ${enrollment.studentCode || 'N/A'}`);
                    console.log(`      이름: ${enrollment.studentName || 'N/A'}`);
                    
                    // 추가 정보 (있을 경우)
                    if (enrollment.cancelDate) {
                        console.log(`   ⚠️ 취소일: ${enrollment.cancelDate}`);
                    }
                    if (enrollment.cancelReason) {
                        console.log(`   ⚠️ 취소 사유: ${enrollment.cancelReason}`);
                    }
                    
                    // 개별 항목 JSON 확인 (디버깅용)
                    console.log(`   🔍 항목 전체 JSON:`, enrollment);
                    
                    // 첫 번째 수강 저장
                    if (idx === 0) {
                        window.lastEnrollmentIdx = enrollment.enrollmentIdx;
                    }
                });
            } else {
                console.log('📋 수강중인 강의가 없습니다.');
                console.log('💡 lecture-test-2a-student-enrollment.js로 수강신청을 먼저 해보세요!');
            }
        } else {
            const error = await response.text();
            console.log('❌ 조회 실패 [' + response.status + ']:', error);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 수강 취소 ==========
async function cancelEnrollment() {
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

        if (response.ok) {
            const result = await response.json();
            console.log('\n✅ 수강취소 성공!');
            console.log('📊 결과:', result.message || '수강이 취소되었습니다.');
        } else {
            const error = await response.text();
            console.log('❌ 수강취소 실패 [' + response.status + ']:', error);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 상세 조회 ==========
async function getLectureDetail() {
    if (!checkAuth()) return;
    
    const token = window.authToken;
    
    const lecSerial = prompt('🔍 조회할 강의 코드 (예: CS101):', 'CS101');
    
    if (!lecSerial || lecSerial.trim() === '') {
        console.log('❌ 강의 코드를 입력해주세요.');
        return;
    }

    console.log('\n📚 강의 상세 조회 - POST');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const requestBody = { lecSerial: lecSerial };  // ✅ lecIdx 대신 lecSerial 사용
        console.log('📤 요청 데이터:', JSON.stringify(requestBody, null, 2));

        const response = await fetch(`${API_BASE_URL}/lectures/detail`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);

        if (response.ok) {
            const lecture = await response.json();
            console.log('\n✅ 조회 성공!');
            console.log('📊 강의 정보:');
            console.log(`   강의 코드: ${lecture.lecSerial}`);
            console.log(`   강의명: ${lecture.lecTit}`);
            console.log(`   교수코드: ${lecture.lecProf || 'N/A'}`);
            console.log(`   교수명: ${lecture.lecProfName || 'N/A'}`);
            console.log(`   학점: ${lecture.lecPoint}점`);
            console.log(`   정원: ${lecture.lecCurrent || 0}/${lecture.lecMany}명`);
            console.log(`   시간: ${lecture.lecTime}`);
            console.log(`   학년: ${lecture.lecYear}학년`);
            console.log(`   학기: ${lecture.lecSemester}학기`);
            
            // Phase 9: 백엔드 필터링 정보
            console.log(`\n🎓 수강 자격 정보 (Phase 9):`);
            console.log(`   학부 코드: ${lecture.lecMcode || '0 (전체 가능)'}`);
            console.log(`   학과 코드: ${lecture.lecMcodeDep || '0 (전체 가능)'}`);
            
            // 강의 개요 출력
            if (lecture.lecSummary) {
                console.log(`   📝 강의 개요: ${lecture.lecSummary}`);
            } else {
                console.log(`   📝 강의 개요: (없음)`);
            }
            
            // 전체 JSON 구조 확인 (디버깅용)
            console.log('\n🔍 전체 응답 JSON:');
            console.log(lecture);
        } else {
            const error = await response.text();
            console.log('❌ 조회 실패 [' + response.status + ']:', error);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 사용자 정보 확인 (디버깅용) ==========
function debugUserInfo() {
    console.log('\n🔍 사용자 정보 디버깅');
    console.log('═══════════════════════════════════════════════════════');
    
    const token = window.authToken;
    console.log('📋 토큰 존재:', !!token);
    
    if (token) {
        console.log('📋 토큰 길이:', token.length);
        console.log('📋 토큰 앞부분:', token.substring(0, 50) + '...');
    }
    
    const user = window.currentUser;
    console.log('\n📋 사용자 정보 (백엔드에서 받음):');
    if (user) {
        console.log('   - ID:', user.id);
        console.log('   - 이메일:', user.email);
        console.log('   - 이름:', user.name);
        console.log('   - 학번:', user.code);
        console.log('   - 학과:', user.major);
        console.log('   - 역할:', user.role);
        console.log('   - 전체 객체:', user);
    } else {
        console.log('   ⚠️ 사용자 정보 없음');
        console.log('   💡 await login() 으로 로그인하세요');
    }
    
    const userInfo = getUserInfo();
    console.log('\n📋 추출된 사용자 정보:');
    console.log(userInfo);
}

// ========== 도움말 ==========
function help() {
    console.log('\n📝 학생 수강 관리 테스트 함수 목록 (Part B)');
    console.log('═══════════════════════════════════════════════════════');
    console.log('📋 제공 함수:');
    console.log('🔐 checkAuth()             - 로그인 상태 확인');
    console.log('🔍 debugUserInfo()         - 사용자 정보 확인 (백엔드에서 받은 정보)');
    console.log('📚 getMyEnrollments()      - 내 수강 목록 조회');
    console.log('🗑️ cancelEnrollment()      - 수강 취소');
    console.log('🔍 getLectureDetail()      - 강의 상세 조회');
    console.log('');
    console.log('📂 관련 파일:');
    console.log('   Part A: lecture-test-2a-student-enrollment.js');
    console.log('   - 수강 가능 강의 목록 조회 (백엔드 필터링)');
    console.log('   - 수강 신청');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 사전 준비:');
    console.log('   1. docs/일반유저 로그인+게시판/test-1-login.js 실행');
    console.log('   2. await login() 실행 (학생 계정)');
    console.log('   3. 로그인 완료 후 이 파일의 함수들 사용');
    console.log('💡 백엔드가 로그인 시 사용자 정보를 자동으로 반환합니다!');
    console.log('💡 프론트엔드에서 JWT 디코딩이 필요 없습니다!');
}

// 초기 메시지
console.log('✅ 학생 수강 관리 테스트 스크립트 로드 완료! (Part B: 내 수강 목록 및 취소)');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');
console.log('🔍 debugUserInfo() 로 사용자 정보를 확인할 수 있습니다.');
console.log('⚠️ 먼저 학생 계정으로 로그인하세요! (checkAuth()로 확인 가능)');
console.log('');
console.log('📌 중요: JWT 디코딩은 백엔드에서 처리됩니다!');
console.log('   - 로그인 시 백엔드가 response.data.user에 사용자 정보 포함');
console.log('   - 프론트엔드는 받은 정보를 그대로 사용');

// ═══════════════════════════════════════════════════════════════════
// 🚀 빠른 실행 명령어 (테스터용)
// ═══════════════════════════════════════════════════════════════════
console.log('\n' + '═'.repeat(63));
console.log('🚀 빠른 실행 명령어');
console.log('═'.repeat(63));
console.log('await getMyEnrollments()   // 내 수강 목록');
console.log('await cancelEnrollment()   // 수강 취소');
console.log('await getLectureDetail()   // 강의 상세 조회');
console.log('debugUserInfo()            // 사용자 정보 확인');
console.log('help()                     // 전체 도움말');
console.log('═'.repeat(63) + '\n');
