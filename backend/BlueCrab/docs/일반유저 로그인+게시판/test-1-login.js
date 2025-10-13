// ========== 1단계: 로그인 테스트 ==========

// ========== 설정 ==========
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// 인증 토큰 저장 (전역 변수로 다른 파일들과 공유)
if (typeof window.authToken === 'undefined') window.authToken = null;
if (typeof window.refreshToken === 'undefined') window.refreshToken = null;
if (typeof window.currentUser === 'undefined') window.currentUser = null;

let authToken = window.authToken;
let refreshToken = window.refreshToken;
let currentUser = window.currentUser;

// ========== 기본 HTTP 요청 함수 ==========
async function apiRequest(url, method = 'GET', data = null, requireAuth = false) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
        mode: 'cors',
        credentials: 'include'
    };

    if (requireAuth && authToken) {
        options.headers['Authorization'] = `Bearer ${authToken}`;
    }

    if (data) {
        options.body = JSON.stringify(data);
    }

    try {
        console.log(`${method} ${url}`);
        if (data) console.log('Data:', data);
        
        const response = await fetch(url, options);
        const result = await response.json();
        
        console.log('Status:', response.status);
        console.log('Response:', result);
        
        return { success: response.ok, status: response.status, data: result };
    } catch (error) {
        console.error('Request failed:', error);
        return { success: false, error: error.message };
    }
}

// ========== 로그인 함수 ==========
async function login() {
    console.log('\n🔐 로그인 시작...');
    
    const username = prompt('이메일을 입력하세요:', 'professor@example.com');
    const password = prompt('비밀번호를 입력하세요:', '');
    
    if (!username || !password) {
        console.log('❌ 로그인 정보가 입력되지 않았습니다.');
        return false;
    }

    const loginData = {
        username: username,  // userEmail을 username으로 사용
        password: password   // 평문 비밀번호
    };

    const result = await apiRequest(`${API_BASE_URL}/api/auth/login`, 'POST', loginData);
    
    if (result.success && result.data && result.data.data) {
        const loginResponse = result.data.data;
        
        // JWT 토큰 추출 및 전역 변수에 저장
        if (loginResponse.accessToken) {
            authToken = loginResponse.accessToken;
            refreshToken = loginResponse.refreshToken;
            currentUser = loginResponse.user;
            
            // 전역 변수에 저장하여 다른 파일들과 공유
            window.authToken = authToken;
            window.refreshToken = refreshToken;
            window.currentUser = currentUser;
            
            console.log('✅ 로그인 성공!');
            console.log('메시지:', result.data.message);
            console.log('사용자 정보:', currentUser);
            console.log('토큰 타입:', loginResponse.tokenType);
            console.log('만료 시간:', loginResponse.expiresIn + '초');
            console.log('🔗 전역 변수에 토큰 저장됨 (다른 테스트 파일에서 사용 가능)');
            
            // 교수 계정인지 확인 (userStudent=1)
            if (currentUser && currentUser.userStudent === 1) {
                console.log('🎓 교수 계정으로 로그인되었습니다.');
                return true;
            } else {
                console.log('⚠️ 학생 계정입니다. userStudent:', currentUser?.userStudent);
                console.log('게시글 작성 권한이 없을 수 있습니다.');
                return true; // 로그인은 성공했으므로 true 반환
            }
        }
    }
    
    console.log('❌ 로그인 실패');
    console.log('응답:', result.data);
    return false;
}

// ========== 로그아웃 함수 ==========
async function logout() {
    console.log('\n🚪 로그아웃...');
    
    if (!authToken) {
        console.log('❌ 로그인 상태가 아닙니다.');
        return false;
    }

    const logoutData = {
        refreshToken: refreshToken
    };

    const result = await apiRequest(`${API_BASE_URL}/api/auth/logout`, 'POST', logoutData, true);
    
    if (result.success) {
        authToken = null;
        refreshToken = null;
        currentUser = null;
        
        // 전역 변수도 초기화
        window.authToken = null;
        window.refreshToken = null;
        window.currentUser = null;
        
        console.log('✅ 로그아웃 성공!');
        console.log('🔗 전역 변수도 초기화됨');
        return true;
    }
    
    console.log('❌ 로그아웃 실패');
    console.log('응답:', result.data);
    return false;
}



// ========== 현재 상태 확인 함수 ==========
function checkStatus() {
    // 최신 전역 변수 값으로 업데이트
    authToken = window.authToken;
    currentUser = window.currentUser;
    
    console.log('\n📋 현재 상태:');
    console.log('로그인됨:', !!authToken);
    console.log('사용자:', currentUser?.userName || 'None');
    console.log('유형:', currentUser?.userStudent === 1 ? '교수' : '학생');
    console.log('전역 토큰:', window.authToken ? '있음' : '없음');
    return !!authToken;
}

// ========== 사용법 안내 ==========
console.log(`
🔐 1단계: 로그인 테스트
===================

사용 방법:
1. await login()    - 로그인 (전역 변수에 토큰 저장)
2. checkStatus()    - 현재 상태 확인  
3. await logout()   - 로그아웃

🔗 토큰 공유:
- window.authToken    - JWT 액세스 토큰 (다른 파일들과 공유)
- window.currentUser  - 사용자 정보 (다른 파일들과 공유)

예시:
await login();
checkStatus();

💡 로그인 후 다른 테스트 파일들을 실행하면 토큰이 자동으로 공유됩니다.
`);