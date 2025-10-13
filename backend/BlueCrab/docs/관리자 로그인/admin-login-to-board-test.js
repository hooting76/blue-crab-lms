// ===================================================================
// 🔐 관리자 로그인 → 게시글 테스트 토큰 발급
// Blue Crab LMS - 관리자 로그인 후 게시글 테스트용 토큰 설정
// ===================================================================

const ADMIN_API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/admin';
const AUTH_API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/admin/email-auth';

// 전역 변수 초기화
if (typeof window.authToken === 'undefined') window.authToken = null;
if (typeof window.currentUser === 'undefined') window.currentUser = null;
if (typeof window.adminSessionToken === 'undefined') window.adminSessionToken = null;

// ========== 1단계: 관리자 1차 로그인 (ID/PW 검증) ==========
async function adminLogin(adminId = 'admin@test.com', password = 'admin123') {
    console.log('\n🔐 1단계: 관리자 1차 로그인 (ID/PW 검증)');
    console.log('═══════════════════════════════════════════════════════');
    console.log('📤 로그인 정보:', { adminId, password: '***' });
    
    try {
        console.log('📡 관리자 로그인 API 호출...');
        const response = await fetch(`${ADMIN_API_BASE_URL}/login`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ adminId, password })
        });

        console.log(`📡 HTTP 상태 코드: ${response.status}`);
        console.log(`📡 응답 헤더:`, Object.fromEntries(response.headers.entries()));
        
        const result = await response.json();
        console.log('📋 전체 응답 데이터:', JSON.stringify(result, null, 2));

        // 1차 로그인 성공 케이스 분석
        if (result.success && result.data) {
            console.log('\n✅ 1차 로그인 성공!');
            console.log(`📊 응답 분석:`);
            console.log(`   - message: ${result.message}`);
            console.log(`   - sessionToken: ${result.data.sessionToken ? result.data.sessionToken.substring(0, 20) + '...' : 'N/A'}`);
            console.log(`   - expiresIn: ${result.data.expiresIn}초`);
            console.log(`   - maskedEmail: ${result.data.maskedEmail}`);
            
            // 세션 토큰 저장
            window.adminSessionToken = result.data.sessionToken;
            localStorage.setItem('adminSessionToken', result.data.sessionToken);
            localStorage.setItem('adminEmail', adminId);
            
            console.log('\n💾 세션 토큰 저장 완료!');
            console.log('🎯 다음 단계: sendAuthCode() 실행 가능');
            
            return result.data;
        } else {
            console.log('\n❌ 1차 로그인 실패');
            console.log(`   - 응답: ${result.message || '로그인 정보 확인 필요'}`);
            return null;
        }

    } catch (error) {
        console.error('\n💥 1차 로그인 네트워크 오류:');
        console.error('   - 오류 유형:', error.name);
        console.error('   - 오류 메시지:', error.message);
        console.error('   - 전체 오류:', error);
        
        console.log('\n🔍 문제 진단:');
        console.log('   1. 인터넷 연결 상태 확인');
        console.log('   2. 관리자 계정 정보 확인 (adminId, password)');
        console.log('   3. 서버 상태 확인');
        console.log('   4. CORS 정책 문제 가능성');
        
        return null;
    }
}

// ========== 2단계: 이메일 인증 코드 발송 ==========
async function sendAuthCode() {
    console.log('\n📧 2단계: 이메일 인증 코드 발송');
    console.log('═══════════════════════════════════════════════════════');
    
    const sessionToken = window.adminSessionToken || localStorage.getItem('adminSessionToken');
    
    if (!sessionToken) {
        console.log('❌ 세션 토큰이 없습니다!');
        console.log('🔧 adminLogin()을 먼저 실행하세요.');
        return null;
    }
    
    console.log(`📤 세션 토큰: ${sessionToken.substring(0, 20)}...`);
    
    try {
        console.log('📡 인증 코드 발송 API 호출...');
        const response = await fetch(`${AUTH_API_BASE_URL}/send-code`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': `Bearer ${sessionToken}`
            },
            body: JSON.stringify({ sessionToken })
        });

        console.log(`📡 HTTP 상태 코드: ${response.status}`);
        
        const result = await response.json();
        console.log('📋 전체 응답 데이터:', JSON.stringify(result, null, 2));

        if (result.success) {
            console.log('\n✅ 인증 코드 발송 성공!');
            console.log(`📮 이메일을 확인하여 6자리 인증 코드를 입력하세요.`);
            console.log('🎯 다음 단계: verifyAuthCode("123456") 실행');
            
            return result;
        } else {
            console.log('\n❌ 인증 코드 발송 실패');
            console.log(`   - 응답: ${result.message}`);
            return null;
        }

    } catch (error) {
        console.error('\n💥 인증 코드 발송 오류:', error);
        return null;
    }
}

// ========== 3단계: 인증 코드 검증 + JWT 토큰 발급 ==========
async function verifyAuthCode(authCode) {
    console.log('\n🔢 3단계: 인증 코드 검증 + JWT 토큰 발급');
    console.log('═══════════════════════════════════════════════════════');
    
    if (!authCode || authCode.length !== 6) {
        console.log('❌ 6자리 인증 코드를 입력해주세요!');
        console.log('📝 사용법: verifyAuthCode("123456")');
        return null;
    }
    
    const sessionToken = window.adminSessionToken || localStorage.getItem('adminSessionToken');
    
    if (!sessionToken) {
        console.log('❌ 세션 토큰이 없습니다!');
        console.log('🔧 adminLogin() → sendAuthCode()를 먼저 실행하세요.');
        return null;
    }
    
    console.log(`📤 인증 코드: ${authCode}`);
    console.log(`📤 세션 토큰: ${sessionToken.substring(0, 20)}...`);
    
    try {
        console.log('📡 인증 코드 검증 API 호출...');
        const response = await fetch(`${AUTH_API_BASE_URL}/verify-code`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ 
                sessionToken: sessionToken,
                authCode: authCode
            })
        });

        console.log(`📡 HTTP 상태 코드: ${response.status}`);
        
        const result = await response.json();
        console.log('📋 전체 응답 데이터:', JSON.stringify(result, null, 2));

        if (result.success && result.data) {
            console.log('\n🎉 인증 코드 검증 성공! JWT 토큰 발급됨!');
            console.log(`📊 JWT 토큰 정보:`);
            console.log(`   - accessToken: ${result.data.accessToken ? result.data.accessToken.substring(0, 30) + '...' : 'N/A'}`);
            console.log(`   - refreshToken: ${result.data.refreshToken ? result.data.refreshToken.substring(0, 30) + '...' : 'N/A'}`);
            
            if (result.data.adminInfo) {
                console.log(`   - adminId: ${result.data.adminInfo.adminId}`);
                console.log(`   - name: ${result.data.adminInfo.name}`);
                console.log(`   - email: ${result.data.adminInfo.email}`);
            }
            
            // 게시글 테스트용 전역 변수 설정
            window.authToken = result.data.accessToken;
            window.currentUser = {
                adminId: result.data.adminInfo?.adminId,
                userName: result.data.adminInfo?.name,
                email: result.data.adminInfo?.email,
                role: 'ADMIN'
            };
            
            // 로컬 스토리지에도 저장
            localStorage.setItem('jwtAccessToken', result.data.accessToken);
            localStorage.setItem('jwtRefreshToken', result.data.refreshToken);
            
            console.log('\n💾 게시글 테스트용 토큰 설정 완료!');
            console.log('🎯 이제 게시글 테스트 파일들을 사용할 수 있습니다:');
            console.log('   - test-2-create.js (게시글 작성)');
            console.log('   - test-3-update-delete.js (게시글 수정/삭제)');
            console.log('   - test-4-read.js (게시글 조회)');
            
            return result.data;
        } else {
            console.log('\n❌ 인증 코드 검증 실패');
            console.log(`   - 응답: ${result.message}`);
            return null;
        }

    } catch (error) {
        console.error('\n💥 인증 코드 검증 오류:', error);
        return null;
    }
}

// ========== 통합 로그인 함수 (원클릭 로그인 - 개발용) ==========
async function quickAdminLogin(adminId = 'admin@test.com', password = 'admin123', authCode = null) {
    console.log('\n🚀 통합 관리자 로그인 시작...');
    console.log('════════════════════════════════════════════════════════════');
    
    // 1단계: 1차 로그인
    const loginResult = await adminLogin(adminId, password);
    if (!loginResult) {
        console.log('❌ 통합 로그인 실패: 1차 로그인 단계');
        return null;
    }
    
    // 2단계: 인증 코드 발송
    const sendResult = await sendAuthCode();
    if (!sendResult) {
        console.log('❌ 통합 로그인 실패: 인증 코드 발송 단계');
        return null;
    }
    
    // 3단계: 인증 코드 입력 대기 (수동)
    if (authCode) {
        console.log('\n⏳ 3초 후 인증 코드 검증 진행...');
        await new Promise(resolve => setTimeout(resolve, 3000));
        
        const verifyResult = await verifyAuthCode(authCode);
        if (verifyResult) {
            console.log('\n🎉 통합 관리자 로그인 완료!');
            return verifyResult;
        } else {
            console.log('❌ 통합 로그인 실패: 인증 코드 검증 단계');
            return null;
        }
    } else {
        console.log('\n📧 이메일에서 6자리 인증 코드를 확인하고');
        console.log('🔢 verifyAuthCode("123456")를 실행하세요!');
        return 'WAITING_FOR_AUTH_CODE';
    }
}

// ========== 현재 로그인 상태 확인 ==========
function checkLoginStatus() {
    console.log('\n🔍 현재 로그인 상태 확인');
    console.log('═══════════════════════════════');
    
    const token = window.authToken;
    const user = window.currentUser;
    const sessionToken = window.adminSessionToken;
    
    console.log(`🔑 JWT 토큰: ${token ? '보유 (' + token.substring(0, 20) + '...)' : '없음'}`);
    console.log(`👤 사용자 정보: ${user ? user.userName + ' (' + user.role + ')' : '없음'}`);
    console.log(`📱 세션 토큰: ${sessionToken ? '보유 (' + sessionToken.substring(0, 20) + '...)' : '없음'}`);
    
    if (token && user) {
        console.log('\n✅ 로그인됨 - 게시글 테스트 실행 가능!');
        return true;
    } else if (sessionToken) {
        console.log('\n⏳ 1차 로그인됨 - 인증 코드 검증 필요');
        console.log('🔢 verifyAuthCode("123456")를 실행하세요.');
        return false;
    } else {
        console.log('\n❌ 로그인되지 않음');
        console.log('🔧 adminLogin()을 먼저 실행하세요.');
        return false;
    }
}

// ========== 로그아웃 ==========
function logout() {
    console.log('\n🚪 로그아웃...');
    window.authToken = null;
    window.currentUser = null;
    window.adminSessionToken = null;
    
    localStorage.removeItem('jwtAccessToken');
    localStorage.removeItem('jwtRefreshToken');
    localStorage.removeItem('adminSessionToken');
    localStorage.removeItem('adminEmail');
    
    console.log('✅ 로그아웃 완료');
}

// ========== 전역 함수 등록 ==========
window.adminLogin = adminLogin;
window.sendAuthCode = sendAuthCode;
window.verifyAuthCode = verifyAuthCode;
window.quickAdminLogin = quickAdminLogin;
window.checkLoginStatus = checkLoginStatus;
window.logout = logout;

console.log('🔐 관리자 로그인 → 게시글 테스트 준비 완료!');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
console.log('📝 기본 로그인: adminLogin()');
console.log('📝 인증코드 발송: sendAuthCode()');
console.log('📝 인증코드 검증: verifyAuthCode("123456")');
console.log('📝 통합 로그인: quickAdminLogin()');
console.log('📝 상태 확인: checkLoginStatus()');
console.log('📝 로그아웃: logout()');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');