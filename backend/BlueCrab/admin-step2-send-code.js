// ===================================================================
// 📧 2단계: 이메일 인증 코드 발송
// Blue Crab LMS - 관리자 로그인 2단계 테스트
// ===================================================================

const AUTH_API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/admin/email-auth';

// ========== 이메일 인증 코드 발송 ==========
async function sendAuthCode() {
    console.log('\n📧 2단계: 이메일 인증 코드 발송');
    console.log('═══════════════════════════════════════════════════════');
    
    // 세션 토큰 확인
    const sessionToken = window.adminSessionToken || localStorage.getItem('adminSessionToken');
    const adminEmail = localStorage.getItem('adminEmail');
    
    console.log('🔍 요청 준비 상태:');
    console.log(`   - 관리자 이메일: ${adminEmail || '없음'}`);
    console.log(`   - 세션 토큰: ${sessionToken ? '보유 (' + sessionToken.substring(0, 20) + '...)' : '없음'}`);
    
    if (!sessionToken) {
        console.log('\n❌ 세션 토큰이 없습니다!');
        console.log('🔧 해결 방법:');
        console.log('   1. admin-step1-login.js를 먼저 실행하거나');
        console.log('   2. adminLogin()을 실행하세요');
        return null;
    }
    
    try {
        console.log('📡 인증 코드 발송 API 호출...');
        const response = await fetch(`${AUTH_API_BASE_URL}/request`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': `Bearer ${sessionToken}`
            }
        });

        console.log(`📡 HTTP 상태 코드: ${response.status}`);
        console.log(`📡 응답 헤더:`, Object.fromEntries(response.headers.entries()));
        
        // 응답 텍스트 먼저 받기 (JSON 파싱 오류 방지)
        const responseText = await response.text();
        console.log('📋 원본 응답 텍스트:', responseText);
        
        // JSON 파싱 시도
        let result;
        try {
            result = JSON.parse(responseText);
            console.log('📋 파싱된 JSON:', JSON.stringify(result, null, 2));
        } catch (e) {
            console.log('⚠️  JSON 파싱 실패:', e.message);
            result = { rawResponse: responseText, success: false };
        }

        // 상태 코드별 상세 분석
        console.log('\n📊 상태 코드 분석:');
        
        if (response.status === 200) {
            console.log('✅ HTTP 200 - 정상 응답');
            if (result.success || result.ok === true) {
                console.log('🎉 인증 코드 발송 성공!');
                console.log(`📮 이메일 주소: ${adminEmail || '저장된 이메일 없음'}`);
                console.log('🔢 6자리 인증 코드를 이메일에서 확인하세요');
                console.log('⏰ 인증 코드 유효시간: 5분');
                
                console.log('\n🎯 다음 단계: admin-step3-verify-code.js 실행');
                console.log('📝 사용법: verifyAuthCode("123456")');
                
                return result;
            } else {
                console.log('❌ 응답은 200이지만 처리 실패');
                console.log('   - 서버에서 success: false 반환');
            }
            
        } else if (response.status === 400) {
            console.log('❌ HTTP 400 - 잘못된 요청');
            console.log('   - 세션 토큰 형식 오류 가능성');
            console.log('   - 요청 데이터 검증 실패');
            console.log('🔄 해결: 1단계부터 다시 시작');
            
        } else if (response.status === 401) {
            console.log('❌ HTTP 401 - 인증 실패');
            console.log('   - 세션 토큰 만료 또는 무효');
            console.log('   - 세션 문제');
            console.log('🔄 해결: 1단계부터 다시 시작');
            
        } else if (response.status === 404) {
            console.log('❌ HTTP 404 - 엔드포인트 없음');
            console.log('   - URL 경로 확인 필요');
            console.log('   - 서버 배포 상태 확인');
            
        } else if (response.status === 409) {
            console.log('❌ HTTP 409 - 세션 충돌');
            console.log('   - 다른 곳에서 동시 진행');
            console.log('🔄 해결: 1단계부터 다시 시작');
            
        } else if (response.status === 429) {
            console.log('❌ HTTP 429 - 요청 제한 초과');
            console.log('   - IP별 레이트 리미팅 적용');
            console.log('⏰ 해결: 잠시 후 재시도');
            
        } else if (response.status === 500) {
            console.log('❌ HTTP 500 - 서버 내부 오류');
            console.log('   - 서버 측 처리 문제');
            console.log('🔧 해결: 관리자 문의 또는 재시도');
        }

        return result;

    } catch (error) {
        console.error('\n💥 인증 코드 발송 네트워크 오류:');
        console.error('   - 오류 유형:', error.name);
        console.error('   - 오류 메시지:', error.message);
        console.error('   - 전체 오류:', error);
        
        console.log('\n🔍 문제 진단:');
        console.log('   1. 네트워크 연결 상태');
        console.log('   2. 서버 응답 시간 초과');
        console.log('   3. CORS 정책 문제');
        console.log('   4. 세션 토큰 만료 가능성');
        
        return null;
    }
}

// ========== 세션 토큰 상태 확인 ==========
function checkStep2Status() {
    console.log('\n🔍 2단계 상태 확인');
    console.log('━━━━━━━━━━━━━━━━━━━━━━');
    
    const sessionToken = window.adminSessionToken || localStorage.getItem('adminSessionToken');
    const adminEmail = localStorage.getItem('adminEmail');
    
    console.log(`📧 관리자 이메일: ${adminEmail || '없음'}`);
    console.log(`🔑 세션 토큰: ${sessionToken ? '보유 (' + sessionToken.substring(0, 20) + '...)' : '없음'}`);
    
    if (sessionToken) {
        console.log('\n✅ 2단계 실행 준비됨');
        console.log('📝 사용법: sendAuthCode()');
        return true;
    } else {
        console.log('\n❌ 1단계 먼저 필요');
        console.log('🔧 admin-step1-login.js 실행 필요');
        return false;
    }
}

// ========== 이메일 재발송 (연속 호출 방지) ==========
async function resendAuthCode() {
    console.log('\n🔄 인증 코드 재발송');
    console.log('⏰ 5초 후 재발송...');
    
    await new Promise(resolve => setTimeout(resolve, 5000));
    return await sendAuthCode();
}

// ========== 전역 함수 등록 ==========
window.sendAuthCode = sendAuthCode;
window.checkStep2Status = checkStep2Status;
window.resendAuthCode = resendAuthCode;

console.log('📧 2단계: 인증 코드 발송 준비 완료!');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
console.log('📝 코드 발송: sendAuthCode()');
console.log('📝 상태 확인: checkStep2Status()');
console.log('📝 코드 재발송: resendAuthCode()');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');