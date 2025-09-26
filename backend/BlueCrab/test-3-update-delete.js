// ========== 3단계: 게시글 수정/삭제 테스트 ==========

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

// ========== 게시글 수정 함수 ==========

// 1. 게시글 수정
async function updateBoard() {
    if (!checkLoginStatus()) {
        console.log('❌ 로그인이 필요합니다.');
        return null;
    }

    console.log('\n✏️ 게시글 수정...');
    
    const boardIdx = parseInt(prompt('수정할 게시글 번호를 입력하세요:', '1'));
    
    // 먼저 현재 게시글 정보를 조회
    console.log('현재 게시글 정보를 조회 중...');
    const currentBoardResult = await apiRequest(`${API_BASE_URL}/api/boards/detail`, 'POST', { boardIdx }, true);
    
    if (!currentBoardResult.success) {
        console.log('❌ 게시글 조회 실패:', currentBoardResult.data);
        return null;
    }
    
    const currentBoard = currentBoardResult.data;
    console.log('현재 제목:', currentBoard.boardTitle);
    console.log('현재 내용:', currentBoard.boardContent);
    
    const titleInput = prompt('새 제목을 입력하세요 (비워두면 수정하지 않음):', '');
    const contentInput = prompt('새 내용을 입력하세요 (비워두면 수정하지 않음):', '');
    
    // 비워둘 경우 원래 값 유지
    const newTitle = titleInput?.trim() || currentBoard.boardTitle;
    const newContent = contentInput?.trim() || currentBoard.boardContent;

    const updateData = {
        boardTitle: newTitle,
        boardContent: newContent,
        boardCode: currentBoard.boardCode, // 기존 코드 유지
        boardFile: null
    };

    const result = await apiRequest(`${API_BASE_URL}/api/boards/update/${boardIdx}`, 'PUT', updateData, true);
    
    if (result.success) {
        console.log('✅ 게시글 수정 성공!');
        console.log('게시글 번호:', result.data.boardIdx);
        console.log('새 제목:', result.data.boardTitle);
        console.log('수정자:', result.data.boardWriter);
        return result.data;
    } else {
        console.log('❌ 게시글 수정 실패:', result.data);
        return null;
    }
}

// 2. 게시글 삭제
async function deleteBoard() {
    if (!checkLoginStatus()) {
        console.log('❌ 로그인이 필요합니다.');
        return false;
    }

    console.log('\n🗑️ 게시글 삭제...');
    
    const boardIdx = parseInt(prompt('삭제할 게시글 번호를 입력하세요:', '1'));
    
    const confirm = window.confirm(`정말로 게시글 ${boardIdx}번을 삭제하시겠습니까?`);
    if (!confirm) {
        console.log('❌ 삭제가 취소되었습니다.');
        return false;
    }

    const result = await apiRequest(`${API_BASE_URL}/api/boards/delete/${boardIdx}`, 'DELETE', null, true);
    
    if (result.success) {
        console.log('✅ 게시글 삭제 성공!');
        console.log('삭제된 게시글 번호:', boardIdx);
        return true;
    } else {
        console.log('❌ 게시글 삭제 실패:', result.data);
        return false;
    }
}

// 3. 게시글 상세 조회 (수정/삭제 전 확인용)
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
        console.log('작성일:', result.data.boardDate);
        console.log('조회수:', result.data.boardViews);
        return result.data;
    } else {
        console.log('❌ 게시글 조회 실패:', result.data);
        return null;
    }
}

// ========== 사용법 안내 ==========
console.log(`
✏️ 3단계: 게시글 수정/삭제 테스트
===========================

사용 방법:
1. await getBoardDetail()   - 게시글 상세 조회 (수정/삭제 전 확인)
2. await updateBoard()      - 게시글 수정 (비워두면 기본값 적용)
3. await deleteBoard()      - 게시글 삭제

수정 시 입력값:
- 제목: 비워두면 기존 제목 유지 (수정하지 않음)
- 내용: 비워두면 기존 내용 유지 (수정하지 않음)

💡 팁: 제목이나 내용을 비워두면 해당 부분은 수정되지 않고 원래 내용이 유지됩니다!

예시:
await getBoardDetail();  // 수정 전 내용 확인
await updateBoard();     // 수정 (엔터만 치면 해당 부분은 수정되지 않음)
await deleteBoard();     // 삭제
`);