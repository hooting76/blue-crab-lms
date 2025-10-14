// ===================================================================
// ✅ 3단계: 인증 코드 검증 및 JWT 토큰 생성
// Blue Crab LMS - 관리자 로그인 3단계 테스트
// ===================================================================

const AUTH_API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/admin/email-auth';

// ========== 인증 코드 검증 및 JWT 토큰 획득 ==========
async function verifyAuthCode(authCode = null) {
    console.log('\n✅ 3단계: 인증 코드 검증 및 JWT 토큰 생성');
    console.log('═══════════════════════════════════════════════════════════════');
    
    // 세션 토큰 확인
    const sessionToken = window.adminSessionToken || localStorage.getItem('adminSessionToken');
    const adminEmail = localStorage.getItem('adminEmail');
    
    console.log('🔍 현재 상태:');
    console.log(`   - 관리자 이메일: ${adminEmail || '없음'}`);
    console.log(`   - 세션 토큰: ${sessionToken ? '보유 (' + sessionToken.substring(0, 20) + '...)' : '없음'}`);
    
    if (!sessionToken) {
        console.log('\n❌ 세션 토큰이 없습니다!');
        console.log('🔧 해결 방법:');
        console.log('   1. admin-step1-login.js 실행');
        console.log('   2. admin-step2-send-code.js 실행');
        console.log('   3. 그 후 이 함수 실행');
        return null;
    }
    
    // 인증 코드 입력 받기
    if (!authCode) {
        authCode = prompt('📧 이메일로 받은 6자리 인증 코드를 입력하세요:');
        if (!authCode) {
            console.log('❌ 인증 코드 입력이 취소되었습니다.');
            return null;
        }
    }
    
    // 인증 코드 검증
    authCode = authCode.toString().trim();
    console.log(`🔢 입력된 인증 코드: ${authCode}`);
    
    if (!/^[A-Z0-9]{6}$/.test(authCode.toUpperCase())) {
        console.log('❌ 잘못된 인증 코드 형식!');
        console.log('   - 6자리 영문 대문자 + 숫자여야 합니다');
        console.log('   - 예: A1B2C3, 123456, ABC123');
        return null;
    }
    
    // 대문자로 변환하여 서버 요구사항 맞춤
    authCode = authCode.toUpperCase();
    
    try {
        console.log('📡 인증 코드 검증 API 호출...');
        const response = await fetch(`${AUTH_API_BASE_URL}/verify`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': `Bearer ${sessionToken}`
            },
            body: JSON.stringify({ 
                authCode: authCode
            })
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
                console.log('🎉 인증 코드 검증 성공!');
                
                // JWT 토큰 추출 및 저장 (여러 위치에서 토큰 찾기)
                const jwtToken = result.token || result.jwtToken || result.accessToken || 
                               (result.data && result.data.accessToken) ||
                               (result.data && result.data.token) ||
                               (result.data && result.data.jwtToken);
                               
                console.log('🔍 토큰 검색 결과:');
                console.log('   - result.token:', result.token ? '있음' : '없음');
                console.log('   - result.jwtToken:', result.jwtToken ? '있음' : '없음');
                console.log('   - result.accessToken:', result.accessToken ? '있음' : '없음');
                console.log('   - result.data.accessToken:', (result.data && result.data.accessToken) ? '있음' : '없음');
                console.log('   - result.data.token:', (result.data && result.data.token) ? '있음' : '없음');
                
                if (jwtToken) {
                    console.log('🔑 JWT 액세스 토큰 획득!');
                    console.log(`   - 길이: ${jwtToken.length}자`);
                    console.log(`   - 앞부분: ${jwtToken.substring(0, 50)}...`);
                    
                    // 전역 및 로컬 저장소에 저장
                    window.adminJwtToken = jwtToken;
                    localStorage.setItem('adminJwtToken', jwtToken);
                    localStorage.setItem('adminJwtTokenTime', new Date().toISOString());
                    
                    console.log('💾 JWT 토큰 저장 완료!');
                    console.log('   - window.adminJwtToken');
                    console.log('   - localStorage.adminJwtToken');
                    
                    // 관리자 정보 저장
                    if (result.admin || result.user) {
                        const adminInfo = result.admin || result.user;
                        console.log('👤 관리자 정보:', adminInfo);
                        localStorage.setItem('adminInfo', JSON.stringify(adminInfo));
                    }
                    
                    console.log('\n🎯 로그인 완료! 이제 Board API 테스트 가능');
                    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
                    console.log('📝 Board 테스트 파일들:');
                    console.log('   - test-2-create.js (게시물 작성)');
                    console.log('   - test-3-update-delete.js (수정/삭제)'); 
                    console.log('   - test-4-read.js (조회/검색)');
                    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
                    
                } else {
                    console.log('⚠️  JWT 토큰이 응답에 없습니다');
                    console.log('   - token, jwtToken, accessToken 필드 모두 없음');
                }
                
                return result;
                
            } else {
                console.log('❌ 응답은 200이지만 검증 실패');
                console.log('   - 인증 코드가 틀렸을 가능성');
                console.log('   - 코드 만료 (5분 초과)');
                console.log('🔄 해결: 올바른 코드 재입력 또는 재발송');
            }
            
        } else if (response.status === 400) {
            console.log('❌ HTTP 400 - 잘못된 요청');
            console.log('   - 인증 코드 형식 오류');
            console.log('   - 요청 데이터 누락');
            console.log('🔧 해결: 6자리 숫자 코드 확인');
            
        } else if (response.status === 401) {
            console.log('❌ HTTP 401 - 인증 실패');
            
            // 서버 응답 메시지로 정확한 원인 파악
            if (result.message) {
                console.log(`📋 서버 메시지: "${result.message}"`);
                console.log('');
                
                if (result.message.includes('임시토큰') || result.message.includes('유효하지 않은')) {
                    console.log('🔍 원인: 세션 토큰에서 이메일 추출 실패');
                    console.log('   - 세션 토큰이 유효하지 않음');
                    console.log('   - 세션 토큰 형식 오류');
                    console.log('💡 해결: clearAllTokens() 후 1단계부터 재시작');
                    
                } else if (result.message.includes('세션이 만료') || result.message.includes('만료되었습니다')) {
                    console.log('🔍 원인: 인증 코드 발송 후 5분 초과');
                    console.log('   - Redis에서 인증 데이터 삭제됨');
                    console.log('💡 해결: sendAuthCode() 재실행 후 5분 내 검증');
                    
                } else if (result.message.includes('인증코드가 올바르지 않')) {
                    console.log('🔍 원인: 인증 코드 불일치');
                    console.log(`   - 입력한 코드: ${authCode}`);
                    console.log('   - 이메일에서 최신 코드 확인 필요');
                    console.log('💡 해결: 이메일의 6자리 코드를 정확히 복사');
                    
                } else if (result.message.includes('계정을 찾을 수 없')) {
                    console.log('� 원인: 관리자 계정 정보 없음');
                    console.log('   - DB에 해당 이메일의 관리자 계정 없음');
                    console.log('💡 해결: 관리자 계정 등록 확인');
                    
                } else {
                    console.log('🔍 일반 인증 실패');
                    console.log('   - 세션 토큰 만료 가능성');
                    console.log('   - 인증 코드 불일치 가능성');
                }
            } else {
                console.log('   - 응답 메시지 없음 (401 Unauthorized)');
            }
            
            console.log('\n🔄 권장 조치:');
            console.log('   1. clearAllTokens() 실행하여 초기화');
            console.log('   2. 1단계부터 다시 시작 (adminLogin)');
            console.log('   3. 코드 발송 후 5분 이내 검증 완료');
            
        } else if (response.status === 403) {
            console.log('❌ HTTP 403 - IP 불일치');
            console.log('   - 코드 발송 시 IP와 검증 시 IP가 다름');
            console.log('   - VPN 또는 네트워크 변경 감지');
            console.log('💡 해결: 같은 네트워크에서 2-3단계 진행');
            
        } else if (response.status === 404) {
            console.log('❌ HTTP 404 - 엔드포인트 없음');
            console.log('   - URL 경로 확인 필요');
            
        } else if (response.status === 409) {
            console.log('❌ HTTP 409 - 세션 충돌');
            console.log('   - 인증 코드 이미 사용됨');
            console.log('   - 세션 상태 불일치');
            console.log('🔄 해결: 새 코드 발송 후 재시도');
            
        } else if (response.status === 410) {
            console.log('❌ HTTP 410 - 코드 만료');
            console.log('   - 인증 코드 5분 만료');
            console.log('🔄 해결: 새 코드 발송');
            
        } else if (response.status === 429) {
            console.log('❌ HTTP 429 - 요청 제한 초과');
            console.log('   - 코드 검증 시도 횟수 초과');
            console.log('⏰ 해결: 잠시 후 재시도');
            
        } else if (response.status === 500) {
            console.log('❌ HTTP 500 - 서버 내부 오류');
            console.log('   - 서버 측 처리 문제');
        }

        return result;

    } catch (error) {
        console.error('\n💥 인증 코드 검증 네트워크 오류:');
        console.error('   - 오류 유형:', error.name);
        console.error('   - 오류 메시지:', error.message);
        console.error('   - 전체 오류:', error);
        
        console.log('\n🔍 문제 진단:');
        console.log('   1. 네트워크 연결 상태');
        console.log('   2. 서버 응답 시간 초과');
        console.log('   3. CORS 정책 문제');
        
        return null;
    }
}

// ========== JWT 토큰 상태 확인 ==========
function checkJwtToken() {
    console.log('\n🔍 JWT 토큰 상태 확인');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const jwtToken = window.adminJwtToken || localStorage.getItem('adminJwtToken');
    const tokenTime = localStorage.getItem('adminJwtTokenTime');
    const adminInfo = localStorage.getItem('adminInfo');
    
    if (jwtToken) {
        console.log('✅ JWT 토큰 보유');
        console.log(`   - 길이: ${jwtToken.length}자`);
        console.log(`   - 앞부분: ${jwtToken.substring(0, 30)}...`);
        console.log(`   - 발급 시간: ${tokenTime || '알 수 없음'}`);
        
        if (adminInfo) {
            try {
                const info = JSON.parse(adminInfo);
                console.log('👤 관리자 정보:', info);
            } catch (e) {
                console.log('👤 관리자 정보: 파싱 오류');
            }
        }
        
        console.log('\n🎯 Board API 테스트 준비 완료!');
        return true;
    } else {
        console.log('❌ JWT 토큰 없음');
        console.log('🔧 1-3단계 로그인 과정 필요');
        return false;
    }
}

// ========== 로그인 상태 완전 초기화 ==========
function clearAllTokens() {
    console.log('\n🧹 모든 토큰 및 세션 초기화');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    // 전역 변수 초기화
    delete window.adminSessionToken;
    delete window.adminJwtToken;
    
    // 로컬 스토리지 초기화
    localStorage.removeItem('adminSessionToken');
    localStorage.removeItem('adminJwtToken');
    localStorage.removeItem('adminJwtTokenTime');
    localStorage.removeItem('adminEmail');
    localStorage.removeItem('adminInfo');
    
    console.log('✅ 모든 인증 정보 초기화 완료');
    console.log('🔄 처음부터 로그인을 시작하세요');
}

// ========== 빠른 전체 과정 테스트 ==========
async function quickFullLogin() {
    console.log('\n🚀 빠른 전체 로그인 테스트');
    console.log('══════════════════════════════════════');
    
    const adminId = prompt('관리자 ID:');
    const adminPw = prompt('관리자 비밀번호:');
    
    if (!adminId || !adminPw) {
        console.log('❌ ID/PW 입력 취소');
        return null;
    }
    
    console.log('1️⃣ 1단계: 로그인 시도...');
    // 1단계는 admin-step1-login.js의 adminLogin 호출 필요
    console.log('⚠️  1단계 함수가 로드되지 않았습니다');
    console.log('🔧 admin-step1-login.js를 먼저 로드하세요');
    
    return null;
}

// ========== 전역 함수 등록 ==========
window.verifyAuthCode = verifyAuthCode;
window.checkJwtToken = checkJwtToken;
window.clearAllTokens = clearAllTokens;
window.quickFullLogin = quickFullLogin;

console.log('✅ 3단계: 인증 코드 검증 준비 완료!');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
console.log('📝 코드 검증: verifyAuthCode("123456")');
console.log('📝 토큰 확인: checkJwtToken()');
console.log('📝 전체 초기화: clearAllTokens()');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');