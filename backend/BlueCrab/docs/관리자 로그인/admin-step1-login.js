// ===================================================================
// 🔐 1단계: 관리자 1차 로그인 (ID/PW 검증)
// Blue Crab LMS - 관리자 로그인 1단계 테스트
// ===================================================================

const ADMIN_API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/admin';

// 전역 변수 초기화
if (typeof window.adminSessionToken === 'undefined') window.adminSessionToken = null;

// ========== 관리자 1차 로그인 (ID/PW 검증) ==========
async function adminLogin() {
    console.log('\n🔐 1단계: 관리자 1차 로그인 (ID/PW 검증)');
    console.log('═══════════════════════════════════════════════════════');
    
    // 관리자 ID 입력 받기
    const adminId = prompt('🔑 관리자 ID (이메일)를 입력하세요:');
    if (!adminId) {
        console.log('❌ 관리자 ID 입력이 취소되었습니다.');
        return null;
    }
    
    // 비밀번호 입력 받기
    const password = prompt('🔒 관리자 비밀번호를 입력하세요:');
    if (!password) {
        console.log('❌ 비밀번호 입력이 취소되었습니다.');
        return null;
    }
    
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
            console.log('🎯 다음 단계: admin-step2-send-code.js 실행');
            console.log('📝 사용법: sendAuthCode()');
            
            return result.data;
        } else {
            console.log('\n❌ 1차 로그인 실패');
            console.log(`   - 응답: ${result.message || '로그인 정보 확인 필요'}`);
            
            // 상태 코드별 상세 분석
            if (response.status === 400) {
                console.log('💡 해결책: 관리자 ID와 비밀번호를 확인하세요');
            } else if (response.status === 401) {
                console.log('💡 해결책: 관리자 계정 정보가 올바르지 않습니다');
            } else if (response.status === 423) {
                console.log('💡 해결책: 계정이 정지되었습니다. 관리자에게 문의하세요');
            } else if (response.status === 500) {
                console.log('💡 해결책: 서버 오류입니다. 잠시 후 다시 시도하세요');
            }
            
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

// ========== 세션 토큰 상태 확인 ==========
function checkSessionToken() {
    console.log('\n🔍 세션 토큰 상태 확인');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const sessionToken = window.adminSessionToken || localStorage.getItem('adminSessionToken');
    const adminEmail = localStorage.getItem('adminEmail');
    
    console.log(`📧 관리자 이메일: ${adminEmail || '없음'}`);
    console.log(`🔑 세션 토큰: ${sessionToken ? '보유 (' + sessionToken.substring(0, 20) + '...)' : '없음'}`);
    
    if (sessionToken) {
        console.log('\n✅ 1차 로그인 완료됨');
        console.log('🎯 다음 단계: sendAuthCode() 실행 가능');
        return true;
    } else {
        console.log('\n❌ 1차 로그인 필요');
        console.log('🔧 adminLogin()을 먼저 실행하세요');
        return false;
    }
}

// ========== 세션 토큰 초기화 ==========
function clearSession() {
    console.log('\n🧹 세션 초기화...');
    window.adminSessionToken = null;
    localStorage.removeItem('adminSessionToken');
    localStorage.removeItem('adminEmail');
    console.log('✅ 세션 초기화 완료');
}

// ========== 전역 함수 등록 ==========
window.adminLogin = adminLogin;
window.checkSessionToken = checkSessionToken;
window.clearSession = clearSession;

console.log('🔐 1단계: 관리자 1차 로그인 준비 완료!');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
console.log('📝 관리자 로그인: adminLogin()');
console.log('📝 상태 확인: checkSessionToken()');
console.log('📝 세션 초기화: clearSession()');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');