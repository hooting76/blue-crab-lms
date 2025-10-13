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
    
    // 세션 토큰 확인
    const sessionToken = window.adminSessionToken || localStorage.getItem('adminSessionToken');
    const adminEmail = localStorage.getItem('adminEmail');
    
    console.log('🔍 요청 준비 상태:');
    console.log(`   - 관리자 이메일: ${adminEmail || '없음'}`);
    console.log(`   - 세션 토큰: ${sessionToken ? '보유 (' + sessionToken.substring(0, 20) + '...)' : '없음'}`);
    
    if (!sessionToken) {
        console.log('\n❌ 세션 토큰이 없습니다!');
        console.log('🔧 해결 방법:');
        console.log('   1. adminLogin()을 먼저 실행하세요');
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
                
                console.log('\n🎯 다음 단계: verifyAuthCode("123456") 실행');
                
                return result;
            } else {
                console.log('❌ 응답은 200이지만 처리 실패');
                console.log('   - 서버에서 success: false 반환');
            }
            
        } else if (response.status === 400) {
            console.log('❌ HTTP 400 - 잘못된 요청');
            console.log('   - 세션 토큰 형식 오류 가능성');
            console.log('🔄 해결: 1단계부터 다시 시작');
            
        } else if (response.status === 401) {
            console.log('❌ HTTP 401 - 인증 실패');
            console.log('   - 세션 토큰 만료 또는 무효');
            console.log('🔄 해결: 1단계부터 다시 시작');
            
        } else if (response.status === 409) {
            console.log('❌ HTTP 409 - 세션 충돌');
            console.log('🔄 해결: 1단계부터 다시 시작');
            
        } else if (response.status === 429) {
            console.log('❌ HTTP 429 - 요청 제한 초과');
            console.log('⏰ 해결: 잠시 후 재시도');
            
        } else if (response.status === 500) {
            console.log('❌ HTTP 500 - 서버 내부 오류');
            console.log('🔧 해결: 관리자 문의 또는 재시도');
        }

        return result;

    } catch (error) {
        console.error('\n💥 인증 코드 발송 네트워크 오류:');
        console.error('   - 오류 유형:', error.name);
        console.error('   - 오류 메시지:', error.message);
        
        return null;
    }
}

// ========== 3단계: 인증 코드 검증 + JWT 토큰 발급 ==========
async function verifyAuthCode(authCode) {
    console.log('\n✅ 3단계: 인증 코드 검증 및 JWT 토큰 생성');
    console.log('═══════════════════════════════════════════════════════════════');
    
    // 세션 토큰 확인
    const sessionToken = window.adminSessionToken || localStorage.getItem('adminSessionToken');
    const adminEmail = localStorage.getItem('adminEmail');
    
    console.log('� 현재 상태:');
    console.log(`   - 관리자 이메일: ${adminEmail || '없음'}`);
    console.log(`   - 세션 토큰: ${sessionToken ? '보유 (' + sessionToken.substring(0, 20) + '...)' : '없음'}`);
    
    if (!sessionToken) {
        console.log('\n❌ 세션 토큰이 없습니다!');
        console.log('🔧 해결 방법:');
        console.log('   1. adminLogin() 실행');
        console.log('   2. sendAuthCode() 실행');
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
    console.log(`� 입력된 인증 코드: ${authCode}`);
    
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
                
                if (jwtToken) {
                    console.log('🔑 JWT 액세스 토큰 획득!');
                    console.log(`   - 길이: ${jwtToken.length}자`);
                    console.log(`   - 앞부분: ${jwtToken.substring(0, 50)}...`);
                    
                    // 게시글 테스트용 전역 변수 설정
                    window.authToken = jwtToken;
                    localStorage.setItem('adminJwtToken', jwtToken);
                    localStorage.setItem('jwtAccessToken', jwtToken);
                    
                    // 관리자 정보 저장
                    if (result.admin || result.user || result.data) {
                        const adminInfo = result.admin || result.user || result.data;
                        console.log('👤 관리자 정보:', adminInfo);
                        
                        window.currentUser = {
                            adminId: adminInfo.adminId || adminInfo.ADMIN_ID,
                            userName: adminInfo.name || adminInfo.NAME,
                            email: adminInfo.email || adminInfo.EMAIL || adminEmail,
                            role: 'ADMIN'
                        };
                        
                        localStorage.setItem('adminInfo', JSON.stringify(adminInfo));
                        console.log('💾 사용자 정보 저장:', window.currentUser);
                    }
                    
                    console.log('\n💾 JWT 토큰 및 사용자 정보 저장 완료!');
                    console.log('🎯 로그인 완료! 이제 Board API 테스트 가능');
                    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
                    console.log('📝 Board 테스트 파일들:');
                    console.log('   - test-2-create.js (게시물 작성)');
                    console.log('   - test-3-update-delete.js (수정/삭제)'); 
                    console.log('   - test-4-read.js (조회/검색)');
                    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
                    
                    return result;
                    
                } else {
                    console.log('⚠️  JWT 토큰이 응답에 없습니다');
                    console.log('   - token, jwtToken, accessToken 필드 모두 없음');
                }
                
            } else {
                console.log('❌ 응답은 200이지만 검증 실패');
                console.log('   - 인증 코드가 틀렸을 가능성');
                console.log('   - 코드 만료 (5분 초과)');
            }
            
        } else if (response.status === 401) {
            console.log('❌ HTTP 401 - 인증 실패');
            
            // 서버 응답 메시지로 정확한 원인 파악
            if (result.message) {
                console.log(`📋 서버 메시지: "${result.message}"`);
                console.log('');
                
                if (result.message.includes('임시토큰') || result.message.includes('유효하지 않은')) {
                    console.log('🔍 원인: 세션 토큰 무효');
                    console.log('💡 해결: logout() 후 1단계부터 재시작');
                    
                } else if (result.message.includes('세션이 만료') || result.message.includes('만료되었습니다')) {
                    console.log('🔍 원인: 인증 코드 발송 후 5분 초과');
                    console.log('💡 해결: sendAuthCode() 재실행 후 5분 내 검증');
                    
                } else if (result.message.includes('인증코드가 올바르지 않')) {
                    console.log('🔍 원인: 인증 코드 불일치');
                    console.log(`   - 입력한 코드: ${authCode}`);
                    console.log('💡 해결: 이메일의 6자리 코드를 정확히 복사');
                }
            }
            
        } else if (response.status === 403) {
            console.log('❌ HTTP 403 - IP 불일치');
            console.log('   - VPN 또는 네트워크 변경 감지');
            console.log('💡 해결: 같은 네트워크에서 2-3단계 진행');
            
        } else if (response.status === 500) {
            console.log('❌ HTTP 500 - 서버 내부 오류');
        }

        return null;

    } catch (error) {
        console.error('\n💥 인증 코드 검증 네트워크 오류:');
        console.error('   - 오류 유형:', error.name);
        console.error('   - 오류 메시지:', error.message);
        
        return null;
    }
}

// ========== 통합 로그인 함수 (3단계 자동 실행) ==========
async function quickAdminLogin() {
    console.log('\n🚀 통합 관리자 로그인 시작...');
    console.log('════════════════════════════════════════════════════════════');
    
    // 1단계: 1차 로그인 (prompt로 ID/PW 입력)
    const loginResult = await adminLogin();
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
    
    // 3단계: 인증 코드 입력 (prompt로 코드 입력)
    console.log('\n📧 이메일에서 6자리 인증 코드를 확인한 후');
    console.log('⏳ 3초 후 인증 코드 입력을 요청합니다...');
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    const verifyResult = await verifyAuthCode();  // prompt로 코드 입력
    if (verifyResult) {
        console.log('\n🎉 통합 관리자 로그인 완료!');
        return verifyResult;
    } else {
        console.log('❌ 통합 로그인 실패: 인증 코드 검증 단계');
        return null;
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

// ========== 로그아웃 / 전체 초기화 ==========
function logout() {
    console.log('\n🚪 로그아웃 및 모든 인증 정보 초기화');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    // 전역 변수 초기화
    window.authToken = null;
    window.currentUser = null;
    window.adminSessionToken = null;
    window.adminJwtToken = null;
    
    // 로컬 스토리지 초기화
    localStorage.removeItem('jwtAccessToken');
    localStorage.removeItem('jwtRefreshToken');
    localStorage.removeItem('adminSessionToken');
    localStorage.removeItem('adminEmail');
    localStorage.removeItem('adminJwtToken');
    localStorage.removeItem('adminJwtTokenTime');
    localStorage.removeItem('adminInfo');
    
    console.log('✅ 로그아웃 완료');
    console.log('🔄 처음부터 로그인을 시작하세요');
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