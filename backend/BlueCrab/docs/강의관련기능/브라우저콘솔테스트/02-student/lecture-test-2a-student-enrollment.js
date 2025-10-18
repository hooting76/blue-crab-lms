// ===================================================================
// 📝 학생 수강신청 테스트 (Part A: 수강 가능 강의 조회 및 신청)
// Blue Crab LMS - Phase 9: 백엔드 필터링 구현 완료
// 
// ⚠️ 사전 준비: 먼저 학생 계정으로 로그인하세요!
// 📁 위치: docs/일반유저 로그인+게시판/test-1-login.js
// 📝 실행: await login() (학생 계정 사용)
// 
// 🆕 Phase 9 기능:
//    - 백엔드 필터링: 학부/학과 코드 기반 수강 가능 강의 필터링
//    - 0값 규칙: LEC_MCODE/LEC_MCODE_DEP = "0" → 전체 학생 수강 가능
//    - 전공/부전공 매칭: 학생의 전공 OR 부전공이 강의 코드와 일치하면 수강 가능
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

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

// ========== 수강 가능한 강의 목록 조회 (Phase 9: 백엔드 필터링) ==========
async function getAvailableLectures() {
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
        studentIdx = parseInt(prompt('👨‍🎓 학생 ID (userIdx)를 입력하세요:', '1'));
        if (!studentIdx || isNaN(studentIdx)) {
            console.log('❌ 올바른 학생 ID를 입력해주세요.');
            return;
        }
    }
    
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n📚 수강 가능한 강의 목록 조회 (Phase 9: 백엔드 필터링) - POST');
    console.log('═══════════════════════════════════════════════════════');
    console.log('🆕 백엔드 필터링: 학부/학과 코드 + 0값 규칙 + 전공/부전공 매칭');

    try {
        const requestBody = {
            studentId: studentIdx,
            page: page,
            size: size
        };
        
        console.log('📤 요청 데이터:', JSON.stringify(requestBody, null, 2));
        
        const response = await fetch(`${API_BASE_URL}/lectures/eligible`, {
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
            
            // 통계 정보 출력
            console.log('\n📊 통계 정보:');
            console.log(`   전체 강의 수: ${result.totalCount || 'N/A'}`);
            console.log(`   수강 가능: ${result.eligibleCount || 'N/A'}`);
            console.log(`   수강 불가: ${result.ineligibleCount || 'N/A'}`);
            
            const lectures = result.eligibleLectures || result.content || result;
            if (Array.isArray(lectures) && lectures.length > 0) {
                console.log(`\n📋 수강 가능한 강의 목록 (${lectures.length}개):`);
                lectures.forEach((lecture, idx) => {
                    console.log(`\n${idx + 1}. ${lecture.lecTit} (${lecture.lecSerial})`);
                    console.log(`   📌 강의 ID: ${lecture.lecIdx}`);
                    console.log(`   👨‍🏫 교수코드: ${lecture.lecProf || 'N/A'}`);
                    console.log(`   👨‍🏫 교수명: ${lecture.lecProfName || 'N/A'}`);
                    console.log(`   📚 학점: ${lecture.lecPoint}점`);
                    console.log(`   👥 정원: ${lecture.lecCurrent || 0}/${lecture.lecMany}명`);
                    console.log(`   🕐 시간: ${lecture.lecTime || 'N/A'}`);
                    console.log(`   � 대상학년/학기: ${lecture.lecYear}학년 ${lecture.lecSemester}학기`);
                    
                    // Phase 9: 백엔드 필터링 정보
                    console.log(`   🎓 수강 자격 (Phase 9):`);
                    console.log(`      학부 코드: ${lecture.lecMcode || '0 (전체 가능)'}`);
                    console.log(`      학과 코드: ${lecture.lecMcodeDep || '0 (전체 가능)'}`);
                    console.log(`      자격 사유: ${lecture.eligibilityReason || '수강 가능'}`);
                    
                    // 강의 개요 (있을 경우)
                    if (lecture.lecSummary) {
                        console.log(`   📄 개요: ${lecture.lecSummary.substring(0, 50)}...`);
                    }
                    
                    // 개별 항목 JSON 확인 (디버깅용)
                    console.log(`   🔍 전체 JSON:`, lecture);
                });
                
                // 페이징 정보
                if (result.pagination) {
                    console.log(`\n📄 페이징: ${result.pagination.currentPage + 1}/${result.pagination.totalPages} 페이지`);
                }
            } else {
                console.log('📋 수강 가능한 강의가 없습니다.');
                console.log('💡 힌트: 학부/학과/학년 제한 또는 정원 초과로 수강 불가능할 수 있습니다.');
            }
            
            // 학생 정보 출력 (Phase 9)
            if (result.studentInfo) {
                console.log('\n👤 학생 정보 (Phase 9):');
                console.log(`   ID: ${result.studentInfo.userIdx}`);
                console.log(`   이름: ${result.studentInfo.userName || 'N/A'}`);
                console.log(`   이메일: ${result.studentInfo.userEmail || 'N/A'}`);
                console.log(`   전공 학부: ${result.studentInfo.majorFacultyCode || 'N/A'}`);
                console.log(`   전공 학과: ${result.studentInfo.majorDeptCode || 'N/A'}`);
                console.log(`   부전공 학부: ${result.studentInfo.minorFacultyCode || 'N/A'}`);
                console.log(`   부전공 학과: ${result.studentInfo.minorDeptCode || 'N/A'}`);
            }
        } else {
            const error = await response.text();
            console.log('❌ 조회 실패 [' + response.status + ']:', error);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 수강 신청 ==========
async function enrollLecture() {
    if (!checkAuth()) return;
    
    const token = window.authToken;
    let user = window.currentUser;
    
    // JWT 토큰에서 최신 사용자 정보 추출
    const userFromToken = getUserFromToken();
    if (userFromToken && userFromToken.userIdx) {
        user = userFromToken;
        console.log('✅ JWT에서 자동 추출된 학생 정보:', user);
    }
    
    // 사용자 정보 확인
    if (!user || !user.userIdx) {
        console.log('❌ 사용자 정보를 찾을 수 없습니다.');
        console.log('💡 수동으로 학생 ID를 입력해주세요.');
        
        const studentIdx = parseInt(prompt('📝 학생 ID (userIdx)를 입력하세요:', '1'));
        if (!studentIdx || isNaN(studentIdx)) {
            console.log('❌ 올바른 학생 ID를 입력해주세요.');
            return;
        }
        user = { userIdx: studentIdx };
    }
    
    const lecSerial = prompt('📝 수강신청할 강의 코드 (예: CS101):', 'CS101');
    
    if (!lecSerial || lecSerial.trim() === '') {
        console.log('❌ 강의 코드를 입력해주세요.');
        return;
    }

    console.log('\n📝 수강 신청');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const requestData = {
            studentIdx: user.userIdx,
            lecSerial: lecSerial  // ✅ lecIdx 대신 lecSerial 사용
        };
        
        console.log('📤 전송 데이터:', requestData);

        const response = await fetch(`${API_BASE_URL}/enrollments/enroll`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(requestData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);

        if (response.status === 201) {
            const result = await response.json();
            console.log('\n✅ 수강신청 성공!');
            
            // 전체 응답 JSON 구조 확인
            console.log('\n🔍 전체 응답 JSON:');
            console.log(result);
            
            console.log('\n📊 수강신청 정보:');
            console.log(`   ENROLLMENT_IDX: ${result.enrollmentIdx}`);
            console.log(`   강의 코드: ${result.lecSerial || 'N/A'}`);
            console.log(`   강의명: ${result.lecTit || 'N/A'}`);
            console.log(`   학생 ID: ${result.studentIdx}`);
            console.log(`   학생 이름: ${result.studentName || 'N/A'}`);
            console.log(`   등록일: ${result.enrollmentDate || 'N/A'}`);
            console.log(`   상태: ${result.enrollmentStatus || 'N/A'}`);
            
            window.lastEnrollmentIdx = result.enrollmentIdx;
            console.log('\n💡 마지막 수강신청 ID가 저장되었습니다.');
            console.log('💡 다음 단계: lecture-test-2b-student-my-courses.js로 내 수강 목록을 확인하세요!');
        } else {
            const error = await response.text();
            console.log('❌ 수강신청 실패 [' + response.status + ']:', error);
        }
    } catch (error) {
        console.log('❌ 에러:', error.message);
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
    console.log('\n📝 학생 수강신청 테스트 함수 목록 (Part A)');
    console.log('═══════════════════════════════════════════════════════');
    console.log('🆕 Phase 9: 백엔드 필터링 구현 완료');
    console.log('   - 학부/학과 코드 기반 자격 확인');
    console.log('   - 0값 규칙 지원 (0 = 전체 학생 가능)');
    console.log('   - 전공 OR 부전공 매칭');
    console.log('');
    console.log('📋 제공 함수:');
    console.log('🔐 checkAuth()             - 로그인 상태 확인');
    console.log('🔍 debugTokenInfo()        - JWT 토큰 정보 확인');
    console.log('📋 getAvailableLectures()  - 수강 가능 강의 목록 (백엔드 필터링)');
    console.log('📝 enrollLecture()         - 수강 신청');
    console.log('');
    console.log('📂 관련 파일:');
    console.log('   Part B: lecture-test-2b-student-my-courses.js');
    console.log('   - 내 수강 목록 조회');
    console.log('   - 수강 취소');
    console.log('   - 강의 상세 조회');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 사전 준비:');
    console.log('   1. docs/일반유저 로그인+게시판/test-1-login.js 실행');
    console.log('   2. await login() 실행 (학생 계정)');
    console.log('   3. 로그인 완료 후 이 파일의 함수들 사용');
    console.log('💡 JWT 토큰에서 자동으로 학생 정보를 추출합니다!');
}

// 초기 메시지
console.log('✅ 학생 수강신청 테스트 스크립트 로드 완료! (Part A: 수강 가능 강의 조회 및 신청)');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');
console.log('🔍 debugTokenInfo() 로 토큰 정보를 확인할 수 있습니다.');
console.log('⚠️ 먼저 학생 계정으로 로그인하세요! (checkAuth()로 확인 가능)');
console.log('🆕 Phase 9: 백엔드 필터링 - 학부/학과 코드 기반 수강 자격 확인');

// ═══════════════════════════════════════════════════════════════════
// 🚀 빠른 실행 명령어 (테스터용)
// ═══════════════════════════════════════════════════════════════════
console.log('\n' + '═'.repeat(63));
console.log('🚀 빠른 실행 명령어');
console.log('═'.repeat(63));
console.log('await getAvailableLectures()  // 수강 가능 강의 목록');
console.log('await enrollLecture()         // 수강 신청');
console.log('debugTokenInfo()              // JWT 토큰 정보');
console.log('checkAuth()                   // 로그인 확인');
console.log('help()                        // 전체 도움말');
console.log('═'.repeat(63) + '\n');
