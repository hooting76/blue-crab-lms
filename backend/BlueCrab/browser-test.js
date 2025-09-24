// 브라우저 콘솔 테스트용 JavaScript 코드
// 교수 계정(userStudent=1)으로 로그인 후 게시글 CRUD 테스트

// ========== 설정 및 전역 변수 ==========
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';
let authToken = null; // JWT 액세스 토큰 저장용
let refreshToken = null; // JWT 리프레시 토큰 저장용
let currentUser = null; // 현재 사용자 정보

// ========== 유틸리티 함수 ==========
// HTTP 요청 헬퍼 함수
async function apiRequest(url, method = 'GET', data = null, requireAuth = false) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
        mode: 'cors', // CORS 모드 명시적 설정
        credentials: 'include' // 쿠키/세션 포함
    };

    // 인증이 필요한 경우 토큰 추가
    if (requireAuth && authToken) {
        options.headers['Authorization'] = `Bearer ${authToken}`;
        console.log('🔑 Authorization 헤더 추가됨');
        console.log('토큰 앞부분:', authToken.substring(0, 50) + '...');
    } else if (requireAuth && !authToken) {
        console.log('❌ 인증이 필요하지만 토큰이 없습니다!');
    }

    // POST, PUT 요청시 데이터 추가
    if (data) {
        options.body = JSON.stringify(data);
    }

    try {
        console.log(`\n=== ${method} ${url} ===`);
        if (data) console.log('Request Data:', data);
        
        const response = await fetch(url, options);
        const contentType = response.headers.get('content-type');
        
        console.log('Status:', response.status, response.statusText);
        console.log('Content-Type:', contentType);
        
        let result;
        
        // Content-Type에 따른 응답 처리
        if (contentType && contentType.includes('application/json')) {
            result = await response.json();
            console.log('Response (JSON):', result);
        } else {
            result = await response.text();
            console.log('Response (Text):', result);
            
            // JSON 파싱 시도
            try {
                const jsonResult = JSON.parse(result);
                result = jsonResult;
                console.log('Parsed as JSON:', result);
            } catch (e) {
                // JSON이 아닌 경우 그대로 사용
                console.log('Response is not JSON');
            }
        }
        
        return { success: response.ok, status: response.status, data: result };
    } catch (error) {
        console.error('Request failed:', error);
        console.error('Error details:', {
            message: error.message,
            name: error.name,
            stack: error.stack
        });
        return { success: false, error: error.message };
    }
}

// ========== 인증 관련 함수 ==========

// 0. 서버 연결 테스트
async function checkServerConnection() {
    console.log('\n🌐 서버 연결 상태 확인...');
    
    // 서버 기본 상태 확인
    const healthEndpoints = [
        `${API_BASE_URL}/api/health`,
        `${API_BASE_URL}/api/actuator/health`,
        `${API_BASE_URL}/`,
        `${API_BASE_URL}/api/boards/count` // 게시글 수 조회로 API 동작 확인
    ];
    
    for (const endpoint of healthEndpoints) {
        try {
            console.log(`체크 중: ${endpoint}`);
            const result = await apiRequest(endpoint, 'GET');
            if (result.success) {
                console.log('✅ 서버 연결 성공!');
                console.log('응답:', result.data);
                return true;
            }
        } catch (error) {
            console.log(`❌ ${endpoint} 연결 실패:`, error.message);
        }
    }
    
    console.log('❌ 서버 연결 실패. 다음을 확인해주세요:');
    console.log('  1. 도메인이 올바른가? (https://bluecrab.chickenkiller.com)');
    console.log('  2. SSL 인증서가 유효한가?');
    console.log('  3. Context Path가 올바른가? (/BlueCrab-1.0.0)');
    console.log('  4. CORS 설정이 올바른가?');
    console.log('  5. 서버가 실행 중인가?');
    return false;
}

// 1. 교수 로그인 함수 (userStudent=1인 계정으로 로그인)
async function loginAsProfessor() {
    // 사용자에게 로그인 정보 입력 받기
    const username = prompt('교수 이메일을 입력하세요:', 'professor@example.com');
    const password = prompt('비밀번호를 입력하세요:', '');
    
    if (!username || !password) {
        console.log('❌ 로그인 정보가 입력되지 않았습니다.');
        return false;
    }

    // 실제 API 스펙에 맞는 LoginRequest 형식
    const loginData = {
        username: username,  // userEmail을 username으로 사용
        password: password   // 평문 비밀번호
    };

    const result = await apiRequest(`${API_BASE_URL}/api/auth/login`, 'POST', loginData);
    
    if (result.success && result.data && result.data.data) {
        const loginResponse = result.data.data;
        
        // JWT 토큰 추출
        if (loginResponse.accessToken) {
            authToken = loginResponse.accessToken;
            refreshToken = loginResponse.refreshToken;
            currentUser = loginResponse.user;
            
            console.log('✅ 로그인 성공!');
            console.log('메시지:', result.data.message);
            console.log('사용자 정보:', currentUser);
            console.log('토큰 타입:', loginResponse.tokenType);
            console.log('만료 시간:', loginResponse.expiresIn + '초');
            
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

// 2. 로그인 상태 확인
function checkLoginStatus() {
    console.log('\n📋 현재 로그인 상태:');
    console.log('액세스 토큰 있음:', !!authToken);
    console.log('리프레시 토큰 있음:', !!refreshToken);
    console.log('현재 사용자:', currentUser);
    
    if (currentUser) {
        console.log('사용자 정보:');
        console.log('  - 이름:', currentUser.userName);
        console.log('  - 이메일:', currentUser.userEmail);
        console.log('  - 유형:', currentUser.userStudent === 1 ? '교수' : '학생');
        console.log('  - 사용자 ID:', currentUser.userIdx);
    }
    
    return !!authToken;
}

// 2-1. JWT 토큰 갱신 함수
async function refreshAccessToken() {
    if (!refreshToken) {
        console.log('❌ 리프레시 토큰이 없습니다.');
        return false;
    }

    const refreshData = {
        refreshToken: refreshToken
    };

    const result = await apiRequest(`${API_BASE_URL}/api/auth/refresh`, 'POST', refreshData);
    
    if (result.success && result.data && result.data.data) {
        const refreshResponse = result.data.data;
        
        if (refreshResponse.accessToken) {
            authToken = refreshResponse.accessToken;
            console.log('✅ 토큰 갱신 성공!');
            console.log('새로운 액세스 토큰 발급됨');
            return true;
        }
    }
    
    console.log('❌ 토큰 갱신 실패');
    console.log('응답:', result.data);
    return false;
}

// 2-2. 로그아웃 함수
async function logout() {
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
        console.log('✅ 로그아웃 성공!');
        return true;
    }
    
    console.log('❌ 로그아웃 실패');
    console.log('응답:', result.data);
    return false;
}

// 2-3. 사용자 권한 상세 확인
async function checkUserPermissions() {
    if (!authToken || !currentUser) {
        console.log('❌ 로그인 상태가 아닙니다.');
        return false;
    }
    
    console.log('\n🔍 사용자 권한 상세 분석:');
    console.log('사용자 ID:', currentUser.id);
    console.log('사용자 이름:', currentUser.name);
    console.log('사용자 이메일:', currentUser.email);
    console.log('userStudent 값:', currentUser.userStudent);
    console.log('현재 설정: 0=학생, 1=교수');
    console.log('이 사용자는:', currentUser.userStudent === 1 ? '교수' : '학생');
    console.log('게시글 작성 권한:', currentUser.userStudent === 1 ? '있음 ✅' : '없음 ❌');
    console.log('JWT 토큰 보유:', !!authToken);
    console.log('토큰 길이:', authToken?.length);
    
    return currentUser.userStudent === 1;
}

// 2-4. JWT 토큰 디코딩 (base64 디코딩으로 페이로드 확인)
function decodeJWTPayload(token) {
    try {
        // JWT는 header.payload.signature 구조
        const parts = token.split('.');
        if (parts.length !== 3) {
            console.log('❌ JWT 형식이 잘못되었습니다.');
            return null;
        }
        
        // payload 부분 디코딩
        const payload = parts[1];
        // URL-safe base64 디코딩
        const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
        const jsonPayload = JSON.parse(decoded);
        
        console.log('🔓 JWT Payload 내용:');
        console.log('  - 발급자(iss):', jsonPayload.iss);
        console.log('  - 주제(sub):', jsonPayload.sub);
        console.log('  - 발급시간(iat):', new Date(jsonPayload.iat * 1000));
        console.log('  - 만료시간(exp):', new Date(jsonPayload.exp * 1000));
        console.log('  - 현재시간:', new Date());
        console.log('  - 토큰 유효:', jsonPayload.exp * 1000 > Date.now() ? '✅' : '❌');
        console.log('  - 전체 페이로드:', jsonPayload);
        
        return jsonPayload;
    } catch (error) {
        console.log('❌ JWT 디코딩 실패:', error.message);
        return null;
    }
}

// ========== 게시글 관련 함수 ==========

// 3. 게시글 작성 테스트
async function createTestBoard() {
    if (!checkLoginStatus()) {
        console.log('❌ 로그인이 필요합니다. loginAsProfessor()를 먼저 실행하세요.');
        return;
    }

    console.log('\n📝 게시글 작성 테스트...');
    
    // 사용자에게 게시글 정보 입력 받기
    const boardCode = parseInt(prompt('게시글 코드를 입력하세요 (0:학교공지, 1:학사공지, 2:학과공지, 3:교수공지):', '3'));
    const title = prompt('게시글 제목을 입력하세요 (비워두면 기본값 사용):', '');
    const content = prompt('게시글 내용을 입력하세요:', '안녕하세요. 교수 공지사항입니다.');

    const boardData = {
        boardCode: boardCode,
        boardContent: content
    };

    // 제목이 입력된 경우에만 추가
    if (title && title.trim() !== '') {
        boardData.boardTit = title; // boardTitle → boardTit로 수정
    }

    console.log('전송할 데이터:', boardData);

    const result = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', boardData, true);
    
    if (result.success) {
        console.log('✅ 게시글 작성 성공!');
        console.log('생성된 게시글:', result.data);
        return result.data;
    } else {
        console.log('❌ 게시글 작성 실패:', result.data);
        return null;
    }
}

// 4. 게시글 목록 조회
async function getBoardList() {
    console.log('\n📋 게시글 목록 조회...');
    
    const page = parseInt(prompt('페이지 번호를 입력하세요 (0부터 시작):', '0'));
    const size = parseInt(prompt('페이지 크기를 입력하세요:', '10'));

    const result = await apiRequest(`${API_BASE_URL}/api/boards/list?page=${page}&size=${size}`);
    
    if (result.success) {
        console.log('✅ 게시글 목록 조회 성공!');
        console.log('총 게시글 수:', result.data.totalElements);
        console.log('현재 페이지:', result.data.number);
        console.log('게시글 목록:', result.data.content);
        return result.data;
    } else {
        console.log('❌ 게시글 목록 조회 실패:', result.data);
        return null;
    }
}

// 5. 특정 게시글 조회
async function getBoardDetail() {
    console.log('\n🔍 특정 게시글 조회...');
    
    const boardIdx = parseInt(prompt('조회할 게시글 번호를 입력하세요:', '1'));

    const result = await apiRequest(`${API_BASE_URL}/api/boards/${boardIdx}`);
    
    if (result.success) {
        console.log('✅ 게시글 조회 성공!');
        console.log('게시글 상세:', result.data);
        return result.data;
    } else {
        console.log('❌ 게시글 조회 실패:', result.data);
        return null;
    }
}

// 6. 게시글 수정 테스트
async function updateTestBoard() {
    if (!checkLoginStatus()) {
        console.log('❌ 로그인이 필요합니다. loginAsProfessor()를 먼저 실행하세요.');
        return;
    }

    console.log('\n✏️ 게시글 수정 테스트...');
    
    const boardIdx = parseInt(prompt('수정할 게시글 번호를 입력하세요:', '1'));
    const title = prompt('새 제목을 입력하세요 (비워두면 수정안함):', '');
    const content = prompt('새 내용을 입력하세요 (비워두면 수정안함):', '');

    const updateData = {};
    
    if (title && title.trim() !== '') {
        updateData.boardTit = title; // boardTitle → boardTit로 수정
    }
    
    if (content && content.trim() !== '') {
        updateData.boardContent = content;
    }

    if (Object.keys(updateData).length === 0) {
        console.log('❌ 수정할 내용이 없습니다.');
        return;
    }

    console.log('수정할 데이터:', updateData);

    const result = await apiRequest(`${API_BASE_URL}/api/boards/update/${boardIdx}`, 'PUT', updateData, true);
    
    if (result.success) {
        console.log('✅ 게시글 수정 성공!');
        console.log('수정된 게시글:', result.data);
        return result.data;
    } else {
        console.log('❌ 게시글 수정 실패:', result.data);
        return null;
    }
}

// 7. 게시글 삭제 테스트
async function deleteTestBoard() {
    if (!checkLoginStatus()) {
        console.log('❌ 로그인이 필요합니다. loginAsProfessor()를 먼저 실행하세요.');
        return;
    }

    console.log('\n🗑️ 게시글 삭제 테스트...');
    
    const boardIdx = parseInt(prompt('삭제할 게시글 번호를 입력하세요:', '1'));
    const confirm = window.confirm(`정말로 게시글 ${boardIdx}번을 삭제하시겠습니까?`);
    
    if (!confirm) {
        console.log('❌ 삭제가 취소되었습니다.');
        return;
    }

    const result = await apiRequest(`${API_BASE_URL}/api/boards/delete/${boardIdx}`, 'DELETE', null, true);
    
    if (result.success) {
        console.log('✅ 게시글 삭제 성공!');
        console.log('삭제 결과:', result.data);
        return result.data;
    } else {
        console.log('❌ 게시글 삭제 실패:', result.data);
        return null;
    }
}

// 8. 코드별 게시글 조회
async function getBoardsByCode() {
    console.log('\n📂 코드별 게시글 조회...');
    
    const boardCode = parseInt(prompt('게시글 코드를 입력하세요 (0:학교공지, 1:학사공지, 2:학과공지, 3:교수공지):', '3'));
    const page = parseInt(prompt('페이지 번호를 입력하세요 (0부터 시작):', '0'));
    const size = parseInt(prompt('페이지 크기를 입력하세요:', '10'));

    const result = await apiRequest(`${API_BASE_URL}/api/boards/bycode/${boardCode}?page=${page}&size=${size}`);
    
    if (result.success) {
        console.log('✅ 코드별 게시글 조회 성공!');
        console.log('총 게시글 수:', result.data.totalElements);
        console.log('게시글 목록:', result.data.content);
        return result.data;
    } else {
        console.log('❌ 코드별 게시글 조회 실패:', result.data);
        return null;
    }
}

// ========== 전체 테스트 시나리오 ==========
async function runFullTest() {
    console.log('🚀 전체 테스트 시나리오 시작...\n');
    
    // 0. 서버 연결 확인
    const serverOk = await checkServerConnection();
    if (!serverOk) {
        console.log('❌ 서버 연결 실패로 테스트 중단');
        return;
    }
    
    // 1. 로그인
    console.log('\n⏳ 3초 후 로그인 테스트...');
    await new Promise(resolve => setTimeout(resolve, 3000));
    const loginSuccess = await loginAsProfessor();
    if (!loginSuccess) {
        console.log('❌ 로그인 실패로 테스트 중단');
        return;
    }
    
    // 2. 게시글 작성
    console.log('\n⏳ 3초 후 게시글 작성 테스트...');
    await new Promise(resolve => setTimeout(resolve, 3000));
    const createdBoard = await createTestBoard();
    
    if (createdBoard) {
        // 3. 게시글 목록 조회
        console.log('\n⏳ 3초 후 게시글 목록 조회...');
        await new Promise(resolve => setTimeout(resolve, 3000));
        await getBoardList();
        
        // 4. 생성된 게시글 상세 조회
        if (createdBoard.boardIdx) {
            console.log('\n⏳ 3초 후 생성된 게시글 상세 조회...');
            await new Promise(resolve => setTimeout(resolve, 3000));
            await apiRequest(`${API_BASE_URL}/api/boards/${createdBoard.boardIdx}`);
        }
    }
    
    console.log('\n✅ 전체 테스트 완료!');
    console.log('\n📊 테스트 요약:');
    console.log(`- 서버 연결: ${serverOk ? '성공' : '실패'}`);
    console.log(`- 로그인: ${loginSuccess ? '성공' : '실패'}`);
    console.log(`- 게시글 작성: ${createdBoard ? '성공' : '실패'}`);
    console.log(`- 토큰 상태: ${authToken ? '보유' : '없음'}`);
}

// ========== 사용법 안내 ==========
console.log(`
🎯 브라우저 콘솔 게시판 테스트 도구 (Blue Crab LMS)
=====================================================

🌐 서버 정보:
- Base URL: ${API_BASE_URL}
- Domain: https://bluecrab.chickenkiller.com
- Context Path: /BlueCrab-1.0.0
- Database: MariaDB (원격 서버: 121.165.24.26:55511)
- 캐시: Redis
- 보안: JWT + Spring Security
- 프로토콜: HTTPS (SSL)

📝 사용 방법:
1. loginAsProfessor()       - 교수 계정(userStudent=1)으로 로그인
2. createTestBoard()        - 게시글 작성 테스트
3. getBoardList()           - 게시글 목록 조회
4. getBoardDetail()         - 특정 게시글 조회
5. updateTestBoard()        - 게시글 수정 테스트
6. deleteTestBoard()        - 게시글 삭제 테스트
7. getBoardsByCode()        - 코드별 게시글 조회
8. runFullTest()            - 전체 테스트 자동 실행

🔧 인증 관련:
- checkLoginStatus()        - 현재 로그인 상태 및 사용자 정보 확인
- refreshAccessToken()      - JWT 액세스 토큰 갱신
- logout()                  - 로그아웃 및 토큰 무효화

🔍 디버깅 변수:
- authToken                 - 현재 JWT 액세스 토큰
- refreshToken              - 현재 JWT 리프레시 토큰  
- currentUser               - 현재 사용자 정보 (UserTbl 객체)

💡 사용 예시:
await loginAsProfessor();
await createTestBoard();
await getBoardList();

⚡ 한번에 모든 테스트: await runFullTest();

🔍 디버깅:
- 모든 HTTP 요청/응답이 콘솔에 출력됩니다
- CORS 에러나 인증 문제시 상세 로그 확인 가능
- JWT 토큰은 보안상 일부만 표시됩니다

📋 게시글 코드:
- 0: 학교공지
- 1: 학사공지  
- 2: 학과공지
- 3: 교수공지

👥 사용자 유형 (userStudent 필드):
- 0: 학생 (일반적으로 게시글 작성 권한 없음)
- 1: 교수 (게시글 작성 권한 있음)

🔐 게시글 작성 권한:
- 관리자 (AdminTbl) 또는 교수 (userStudent=1)만 게시글 작성 가능
- 학생 계정으로는 게시글 작성이 제한됨

💡 테스트 팁:
- 먼저 checkLoginStatus()로 로그인된 사용자가 교수인지 확인
- 학생 계정으로 테스트하려면 조회 기능만 사용
- 교수 계정이 필요한 경우 관리자에게 계정 요청
`);