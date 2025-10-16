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

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// 전역 변수 (test-1-login.js에서 설정한 토큰 사용)
if (typeof window.authToken === 'undefined') window.authToken = null;

// ========== JWT 토큰 디코딩 함수 ==========
function decodeJWT(token) {
    try {
        if (!token) return null;
        const parts = token.split('.');
        if (parts.length !== 3) return null;
        const payload = parts[1];
        const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
        const padded = base64 + '=='.substring(0, (4 - base64.length % 4) % 4);
        return JSON.parse(atob(padded));
    } catch (error) {
        console.log('🔍 JWT 디코딩 실패:', error.message);
        return null;
    }
}

// ========== 토큰에서 사용자 정보 추출 ==========
function getUserFromToken() {
    const token = window.authToken;
    if (!token) return null;
    const decoded = decodeJWT(token);
    if (!decoded) return null;
    console.log('🔍 JWT 토큰 내용:', decoded);
    if (decoded.userIdx || decoded.userId || decoded.id || decoded.sub) {
        return {
            userIdx: decoded.userIdx || decoded.userId || decoded.id || decoded.sub,
            email: decoded.email || decoded.userEmail || null,
            name: decoded.name || decoded.userName || null,
            role: decoded.role || decoded.authority || null
        };
    }
    return null;
}

// ========== 로그인 상태 확인 ==========
function checkAuth() {
    const token = window.authToken;
    
    if (!token) {
        console.log('\n⚠️ 로그인이 필요합니다!');
        console.log('📁 docs/일반유저 로그인+게시판/test-1-login.js → await login()');
        return false;
    }
    
    // JWT 토큰에서 사용자 정보 추출 시도
    const userFromToken = getUserFromToken();
    if (userFromToken) {
        console.log('✅ JWT에서 사용자 정보 추출:', userFromToken);
        if (!window.currentUser || !window.currentUser.userIdx) {
            window.currentUser = userFromToken;
            console.log('🔄 사용자 정보 업데이트됨');
        }
    }
    
    return true;
}

// ========== 내 수강 목록 조회 ==========
async function getMyEnrollments() {
    if (!checkAuth()) return;
    
    const token = window.authToken;
    let user = window.currentUser;
    
    // JWT 토큰에서 최신 사용자 정보 추출
    const userFromToken = getUserFromToken();
    if (userFromToken && userFromToken.userIdx) {
        user = userFromToken;
        console.log('✅ JWT에서 자동 추출된 학생 정보:', user);
    }
    
    let studentIdx;
    if (user && user.userIdx) {
        studentIdx = user.userIdx;
        console.log('✅ 사용자 ID 자동 감지:', studentIdx);
    } else {
        console.log('⚠️ 사용자 정보 자동 감지 실패. 수동 입력 필요.');
        studentIdx = parseInt(prompt('📝 학생 ID (userIdx)를 입력하세요:', '1'));
        if (!studentIdx || isNaN(studentIdx)) {
            console.log('❌ 올바른 학생 ID를 입력해주세요.');
            return;
        }
    }
    
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
    
    const lectureIdx = parseInt(prompt('🔍 조회할 LECTURE_IDX:', '1'));

    console.log('\n📚 강의 상세 조회 - POST');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const requestBody = { lecIdx: lectureIdx };
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
            console.log(`   LECTURE_IDX: ${lecture.lecIdx}`);
            console.log(`   강의명: ${lecture.lecTit}`);
            console.log(`   강의코드: ${lecture.lecSerial}`);
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

// ========== 토큰 정보 확인 (디버깅용) ==========
function debugTokenInfo() {
    console.log('\n🔍 토큰 디버깅 정보');
    console.log('═══════════════════════════════════════════════════════');
    
    const token = window.authToken;
    console.log('📋 토큰 존재:', !!token);
    
    if (token) {
        console.log('📋 토큰 길이:', token.length);
        console.log('📋 토큰 앞부분:', token.substring(0, 50) + '...');
        
        const decoded = decodeJWT(token);
        if (decoded) {
            console.log('📋 디코딩된 토큰:', decoded);
            console.log('📋 사용 가능한 필드:', Object.keys(decoded));
            
            if (decoded.iat) {
                console.log('📋 발급 시간:', new Date(decoded.iat * 1000));
            }
            if (decoded.exp) {
                console.log('📋 만료 시간:', new Date(decoded.exp * 1000));
                const now = Math.floor(Date.now() / 1000);
                console.log('📋 토큰 유효:', decoded.exp > now ? '✅ 유효' : '❌ 만료');
            }
        }
    }
    
    console.log('📋 window.currentUser:', window.currentUser);
}

// ========== 도움말 ==========
function help() {
    console.log('\n📝 학생 수강 관리 테스트 함수 목록 (Part B)');
    console.log('═══════════════════════════════════════════════════════');
    console.log('📋 제공 함수:');
    console.log('🔐 checkAuth()             - 로그인 상태 확인');
    console.log('🔍 debugTokenInfo()        - JWT 토큰 정보 확인');
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
    console.log('💡 JWT 토큰에서 자동으로 학생 정보를 추출합니다!');
}

// 초기 메시지
console.log('✅ 학생 수강 관리 테스트 스크립트 로드 완료! (Part B: 내 수강 목록 및 취소)');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');
console.log('🔍 debugTokenInfo() 로 토큰 정보를 확인할 수 있습니다.');
console.log('⚠️ 먼저 학생 계정으로 로그인하세요! (checkAuth()로 확인 가능)');
