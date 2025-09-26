// ========== 4단계: 게시글 조회 테스트 ==========

// ========== 설정 ==========
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// 인증 변수 (관리자 로그인 토큰 우선 사용)
if (typeof window.authToken === 'undefined') window.authToken = null;
if (typeof window.currentUser === 'undefined') window.currentUser = null;
if (typeof window.adminJwtToken === 'undefined') window.adminJwtToken = null;

// 관리자 토큰이 있으면 우선 사용, 없으면 일반 사용자 토큰 사용
let authToken = window.adminJwtToken || window.authToken;
let currentUser = window.currentUser;

// ========== HTTP 요청 함수 ==========
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
    
    if (requireAuth && currentToken) {
        options.headers['Authorization'] = `Bearer ${currentToken}`;
        console.log('🔑 사용 토큰:', tokenType);
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
    console.log('🔍 인증 상태:', isLoggedIn ? '✅ 로그인됨' : '❌ 로그인 필요');
    
    if (adminToken) {
        console.log('👤 관리자 로그인 상태');
    } else if (userToken && currentUser) {
        console.log('👤 일반 사용자:', currentUser.userName);
    }
    
    if (!isLoggedIn) {
        console.log('💡 관리자 로그인 또는 일반 사용자 로그인이 필요합니다.');
    }
    
    return isLoggedIn;
}

// ========== 게시글 조회 함수 ==========

// 1. 게시글 상세 조회
async function getBoardDetail() {
    if (!checkLoginStatus()) {
        console.log('❌ 로그인이 필요합니다.');
        return null;
    }

    console.log('\n🔍 게시글 상세 조회...');
    
    const boardIdx = parseInt(prompt('조회할 게시글 번호를 입력하세요:', '1'));

    const result = await apiRequest(`${API_BASE_URL}/api/boards/detail`, 'POST', { boardIdx }, true);
    
    if (result.success) {
        console.log('✅ 게시글 조회 성공!');
        console.log('게시글 번호:', result.data.boardIdx);
        console.log('제목:', result.data.boardTitle);
        console.log('내용:', result.data.boardContent);
        console.log('작성자:', result.data.boardWriter);
        console.log('작성일:', result.data.boardReg || result.data.boardDate);
        console.log('조회수:', result.data.boardView ?? result.data.boardViews ?? 0);
        console.log('코드:', result.data.boardCode);
        return result.data;
    } else {
        console.log('❌ 게시글 조회 실패:', result.data);
        return null;
    }
}

// 2. 게시글 목록 조회 (최신 5개)
async function getBoardList() {
    if (!checkLoginStatus()) {
        console.log('❌ 로그인이 필요합니다.');
        return null;
    }

    console.log('\n📋 게시글 목록 조회 (최신 5개)...');

    const result = await apiRequest(`${API_BASE_URL}/api/boards/list`, 'POST', { page: 0, size: 5 }, true);
    
    if (result.success) {
        console.log('✅ 게시글 목록 조회 성공!');
        console.log(`총 ${result.data.totalElements}개 중 ${result.data.content.length}개 표시`);
        
        result.data.content.forEach((board, index) => {
            const codeNames = { 0: '학교공지', 1: '학사공지', 2: '학과공지', 3: '교수공지' };
            const viewCount = board.boardView ?? board.boardViews ?? 0; // null/undefined일 때만 0 사용
            console.log(`\n${index + 1}. [${board.boardIdx}] ${board.boardTitle}`);
            console.log(`   작성자: ${board.boardWriter} | 날짜: ${board.boardReg || board.boardDate}`);
            console.log(`   유형: ${codeNames[board.boardCode]} | 조회수: ${viewCount}`);
        });
        
        return result.data;
    } else {
        console.log('❌ 게시글 목록 조회 실패:', result.data);
        return null;
    }
}

// 3. 특정 코드 게시글 조회
async function getBoardsByCode() {
    if (!checkLoginStatus()) {
        console.log('❌ 로그인이 필요합니다.');
        return null;
    }

    console.log('\n📂 코드별 게시글 조회...');
    
    const boardCode = parseInt(prompt('게시글 코드 (0:학교공지, 1:학사공지, 2:학과공지, 3:교수공지):', '3'));
    const codeNames = { 0: '학교공지', 1: '학사공지', 2: '학과공지', 3: '교수공지' };
    const codeName = codeNames[boardCode] || '알 수 없음';

    const result = await apiRequest(`${API_BASE_URL}/api/boards/bycode`, 'POST', { boardCode, page: 0, size: 5 }, true);
    
    if (result.success) {
        console.log(`✅ ${codeName} 조회 성공!`);
        console.log(`총 ${result.data.totalElements}개 중 ${result.data.content.length}개 표시`);
        
        result.data.content.forEach((board, index) => {
            const viewCount = board.boardView ?? board.boardViews ?? 0; // null/undefined일 때만 0 사용
            console.log(`\n${index + 1}. [${board.boardIdx}] ${board.boardTitle}`);
            console.log(`   작성자: ${board.boardWriter} | 날짜: ${board.boardReg || board.boardDate}`);
            console.log(`   조회수: ${viewCount}`);
        });
        
        return result.data;
    } else {
        console.log(`❌ ${codeName} 조회 실패:`, result.data);
        return null;
    }
}

// 4. 게시글 존재 확인
async function checkBoardExists() {
    if (!checkLoginStatus()) {
        console.log('❌ 로그인이 필요합니다.');
        return null;
    }

    console.log('\n❓ 게시글 존재 확인...');
    
    const boardIdx = parseInt(prompt('존재 여부를 확인할 게시글 번호:', '1'));

    const result = await apiRequest(`${API_BASE_URL}/api/boards/exists`, 'POST', { boardIdx }, true);
    
    if (result.success) {
        const exists = result.data;
        console.log(`✅ 게시글 ${boardIdx}번: ${exists ? '존재함' : '존재하지 않음'}`);
        return exists;
    } else {
        console.log('❌ 존재 확인 실패:', result.data);
        return null;
    }
}

// 5. 전체 게시글 개수 조회
async function getBoardCount() {
    if (!checkLoginStatus()) {
        console.log('❌ 로그인이 필요합니다.');
        return null;
    }

    console.log('\n📊 전체 게시글 개수 조회...');

    const result = await apiRequest(`${API_BASE_URL}/api/boards/count`, 'POST', {}, true);
    
    if (result.success) {
        console.log('✅ 전체 게시글 개수:', result.data + '개');
        return result.data;
    } else {
        console.log('❌ 게시글 개수 조회 실패:', result.data);
        return null;
    }
}

// ========== 사용법 안내 ==========
console.log(`
🔍 4단계: 게시글 조회 테스트
=========================

사용 방법:
1. await getBoardDetail()    - 특정 게시글 상세 조회
2. await getBoardList()      - 게시글 목록 조회 (최신 5개)
3. await getBoardsByCode()   - 코드별 게시글 조회
4. await checkBoardExists()  - 게시글 존재 여부 확인
5. await getBoardCount()     - 전체 게시글 개수 조회

예시:
await getBoardList();
await getBoardDetail();
await getBoardCount();
`);