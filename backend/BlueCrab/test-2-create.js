// ========== 2단계: 게시글 생성 테스트 ==========

// ========== 설정 ==========
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// 인증 변수 (관리자 로그인 토큰 우선 사용)
// 1. 관리자 JWT 토큰 확인 (admin-step3-verify-code.js에서 설정)
// 2. 일반 사용자 토큰 확인 (test-1-login.js에서 설정)
if (typeof window.authToken === 'undefined') window.authToken = null;
if (typeof window.refreshToken === 'undefined') window.refreshToken = null;
if (typeof window.currentUser === 'undefined') window.currentUser = null;
if (typeof window.adminJwtToken === 'undefined') window.adminJwtToken = null;

// 관리자 토큰이 있으면 우선 사용, 없으면 일반 사용자 토큰 사용
let authToken = window.adminJwtToken || window.authToken;
let refreshToken = window.refreshToken;
let currentUser = window.currentUser;
let lastCreatedBoard = null;

// ========== HTTP 요청 함수 ==========
async function apiRequest(url, method = 'GET', data = null, requireAuth = false) {
    // 매번 최신 토큰 확인 (최신 로그인 토큰 우선)
    const currentAdminToken = window.adminJwtToken || localStorage.getItem('adminJwtToken');
    const currentUserToken = window.authToken;
    
    // 최신 토큰 결정 로직: 더 최근에 생성된 토큰을 우선 사용
    let currentToken;
    let tokenType;
    
    if (currentAdminToken && currentUserToken) {
        // 두 토큰이 모두 있으면 더 최근 것을 사용
        try {
            const adminDecoded = JSON.parse(atob(currentAdminToken.split('.')[1]));
            const userDecoded = JSON.parse(atob(currentUserToken.split('.')[1]));
            
            if (adminDecoded.iat > userDecoded.iat) {
                currentToken = currentAdminToken;
                tokenType = '관리자 JWT (최신)';
            } else {
                currentToken = currentUserToken;
                tokenType = '일반 사용자 (최신)';
            }
        } catch (e) {
            // 토큰 파싱 실패 시 기본 우선순위
            currentToken = currentUserToken || currentAdminToken;
            tokenType = currentUserToken ? '일반 사용자 (기본)' : '관리자 JWT (기본)';
        }
    } else {
        currentToken = currentUserToken || currentAdminToken;
        tokenType = currentUserToken ? '일반 사용자' : '관리자 JWT';
    }
    
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
        mode: 'cors',
        credentials: 'include'
    };

    if (requireAuth && currentToken) {
        options.headers['Authorization'] = `Bearer ${currentToken}`;
        console.log('🔑 사용 토큰:', tokenType, 
                   '(' + currentToken.substring(0, 20) + '...)');
    } else if (requireAuth && !currentToken) {
        console.log('❌ 토큰 없음 - 인증 필요!');
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

// ========== 상태 확인 ==========
function checkLoginStatus() {
    // 최신 전역 변수 값으로 업데이트 (최신 로그인 토큰 우선)
    const adminToken = window.adminJwtToken || localStorage.getItem('adminJwtToken');
    const userToken = window.authToken;
    
    // 최신 토큰 결정 로직
    if (adminToken && userToken) {
        try {
            const adminDecoded = JSON.parse(atob(adminToken.split('.')[1]));
            const userDecoded = JSON.parse(atob(userToken.split('.')[1]));
            
            authToken = (adminDecoded.iat > userDecoded.iat) ? adminToken : userToken;
        } catch (e) {
            authToken = userToken || adminToken;
        }
    } else {
        authToken = userToken || adminToken;
    }
    
    currentUser = window.currentUser;
    
    const isLoggedIn = !!authToken;
    console.log('🔍 인증 상태 확인:');
    console.log('   - 관리자 JWT 토큰:', adminToken ? '✅ 있음' : '❌ 없음');
    console.log('   - 일반 사용자 토큰:', userToken ? '✅ 있음' : '❌ 없음');
    console.log('   - 사용될 토큰:', authToken ? '✅ 있음 (' + authToken.substring(0, 20) + '...)' : '❌ 없음');
    
    if (adminToken) {
        console.log('👤 관리자 로그인 상태로 게시글 작성');
        const adminInfo = localStorage.getItem('adminInfo');
        if (adminInfo) {
            try {
                const info = JSON.parse(adminInfo);
                console.log('   - 관리자명:', info.name || '정보 없음');
                console.log('   - 관리자 ID:', info.adminId || '정보 없음');
            } catch (e) {
                console.log('   - 관리자 정보: 파싱 오류');
            }
        }
    } else if (userToken && currentUser) {
        console.log('👤 일반 사용자 로그인 상태로 게시글 작성');
        console.log('   - 사용자:', currentUser.userName);
        console.log('   - 유형:', currentUser.userStudent === 1 ? '교수' : '학생');
    }
    
    if (!isLoggedIn) {
        console.log('❌ 로그인이 필요합니다!');
        console.log('💡 해결 방법:');
        console.log('   1. 관리자 로그인: admin-step1-login.js → admin-step2-send-code.js → admin-step3-verify-code.js');
        console.log('   2. 일반 사용자 로그인: test-1-login.js');
    }
    
    return isLoggedIn;
}

// ========== 게시글 생성 함수 ==========

// 1. 기본 게시글 생성 (prompt로 입력받기)
async function createBoard() {
    if (!checkLoginStatus()) {
        console.log('❌ 로그인이 필요합니다. 먼저 test-1-login.js로 로그인하세요.');
        return null;
    }

    console.log('\n📝 게시글 생성...');
    
    // 1. 먼저 게시글 코드 선택
    const boardCodeInput = prompt('게시글 코드를 선택하세요 (0:학교공지, 1:학사공지, 2:학과공지, 3:교수공지):', '3');
    
    const boardCode = parseInt(boardCodeInput);
    if (isNaN(boardCode) || boardCode < 0 || boardCode > 3) {
        console.log('❌ 올바른 게시글 코드를 입력하세요 (0-3).');
        return null;
    }
    
    const codeNames = { 0: '학교공지', 1: '학사공지', 2: '학과공지', 3: '교수공지' };
    const selectedType = codeNames[boardCode];
    console.log(`📋 ${selectedType} 선택됨`);
    
    // 2. 선택된 유형에 맞는 기본값으로 제목과 내용 입력받기
    const defaultTitle = selectedType;
    const defaultContent = `${selectedType} 내용입니다.`;
    
    const titleInput = prompt(`${selectedType} 제목을 입력하세요 (비워두면 기본값 사용):`, defaultTitle);
    const contentInput = prompt(`${selectedType} 내용을 입력하세요 (비워두면 기본값 사용):`, defaultContent);
    
    // 비워둘 경우 기본값 사용
    const title = titleInput?.trim() || defaultTitle;
    const content = contentInput?.trim() || defaultContent;
    
    console.log(`📋 ${selectedType} 게시글 생성 중...`);
    
    const boardData = {
        boardTitle: title,
        boardContent: content,
        boardCode: boardCode
    };

    const result = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', boardData, true);
    
    if (result.success) {
        console.log('✅ 게시글 생성 성공!');
        console.log('게시글 번호:', result.data.boardIdx);
        console.log('제목:', result.data.boardTitle);
        console.log('작성자:', result.data.boardWriter);
        console.log('유형:', selectedType);
        
        lastCreatedBoard = result.data;
        return result.data;
    } else {
        console.log('❌ 게시글 생성 실패:', result.data);
        return null;
    }
}

// 2. 빠른 게시글 생성 (기본값 사용)
async function createQuickBoard() {
    if (!checkLoginStatus()) {
        console.log('❌ 로그인이 필요합니다.');
        return null;
    }

    console.log('\n� 빠른 게시글 생성 (기본값 사용)...');
    
    const boardData = {
        boardTitle: `빠른 테스트 게시글 ${new Date().toLocaleTimeString()}`,
        boardContent: `빠른 테스트용 게시글입니다.\n생성 시간: ${new Date().toISOString()}`,
        boardCode: 3 // 교수공지
    };

    console.log('📋 교수공지 게시글 자동 생성 중...');

    const result = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', boardData, true);
    
    if (result.success) {
        console.log('✅ 빠른 게시글 생성 성공!');
        console.log('게시글 번호:', result.data.boardIdx);
        console.log('제목:', result.data.boardTitle);
        console.log('작성자:', result.data.boardWriter);
        
        lastCreatedBoard = result.data;
        return result.data;
    } else {
        console.log('❌ 빠른 게시글 생성 실패:', result.data);
        return null;
    }
}

// 3. 최근 생성된 게시글 정보 확인
function getLastCreatedBoard() {
    if (lastCreatedBoard) {
        console.log('\n📋 최근 생성된 게시글:');
        console.log('번호:', lastCreatedBoard.boardIdx);
        console.log('제목:', lastCreatedBoard.boardTitle);
        console.log('작성자:', lastCreatedBoard.boardWriter);
        return lastCreatedBoard;
    } else {
        console.log('❌ 생성된 게시글이 없습니다.');
        return null;
    }
}

// ========== 사용법 안내 ==========
console.log(`
📝 2단계: 게시글 생성 테스트
========================

사용 방법:
1. await createBoard()      - 게시글 생성 (순서: 코드 → 제목 → 내용)
2. await createQuickBoard() - 빠른 게시글 생성 (기본값 사용)
3. getLastCreatedBoard()    - 최근 생성된 게시글 정보

게시글 생성 순서:
1️⃣ 게시글 코드 선택 (0:학교공지, 1:학사공지, 2:학과공지, 3:교수공지)
2️⃣ 제목 입력 (비워두면 선택한 유형명이 기본값)
3️⃣ 내용 입력 (비워두면 "유형 + 내용입니다" 기본값)

💡 팁: 제목이나 내용을 비워두면 선택한 유형에 맞는 기본값이 자동 적용됩니다!

예시:
await createBoard();      // 단계별 입력으로 게시글 생성
await createQuickBoard(); // 기본값으로 빠른 생성
getLastCreatedBoard();    // 최근 생성된 게시글 확인
`);