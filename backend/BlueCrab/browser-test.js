// 브라우저 콘솔 테스트용 JavaScript 코드
// 교수 계정으로 로그인 후 게시글 CRUD 테스트

// ========== 설정 및 전역 변수 ==========
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';
let authToken = null; // JWT 토큰 저장용
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

// 1. 교수 로그인 함수
async function loginAsProfessor() {
    // 사용자에게 로그인 정보 입력 받기
    const email = prompt('교수 이메일을 입력하세요:', 'professor@example.com');
    const password = prompt('비밀번호를 입력하세요:', '');
    
    if (!email || !password) {
        console.log('❌ 로그인 정보가 입력되지 않았습니다.');
        return false;
    }

    const loginData = {
        email: email,  // 또는 userEmail (실제 API 스펙에 맞게)
        password: password  // 또는 userPw
    };

    // 여러 가능한 로그인 엔드포인트 시도
    const possibleEndpoints = [
        `${API_BASE_URL}/api/auth/login`,
        `${API_BASE_URL}/api/user/login`,
        `${API_BASE_URL}/api/login`
    ];

    for (const endpoint of possibleEndpoints) {
        const result = await apiRequest(endpoint, 'POST', loginData);
        
        if (result.success) {
            // JWT 토큰 추출 (여러 가능한 응답 구조 고려)
            let token = null;
            if (result.data.token) {
                token = result.data.token;
            } else if (result.data.accessToken) {
                token = result.data.accessToken;
            } else if (result.data.jwt) {
                token = result.data.jwt;
            } else if (typeof result.data === 'string' && result.data.includes('.')) {
                // 응답 자체가 토큰인 경우
                token = result.data;
            }
            
            if (token) {
                authToken = token;
                currentUser = result.data.user || { email: email };
                console.log('✅ 로그인 성공!');
                console.log('토큰:', authToken.substring(0, 50) + '...');
                console.log('사용자 정보:', currentUser);
                return true;
            }
        }
    }
    
    console.log('❌ 로그인 실패');
    return false;
}

// 2. 로그인 상태 확인
function checkLoginStatus() {
    console.log('\n📋 현재 로그인 상태:');
    console.log('토큰 있음:', !!authToken);
    console.log('현재 사용자:', currentUser);
    return !!authToken;
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
        boardData.boardTitle = title;
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
        updateData.boardTitle = title;
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
1. loginAsProfessor()       - 교수 계정으로 로그인
2. createTestBoard()        - 게시글 작성 테스트
3. getBoardList()           - 게시글 목록 조회
4. getBoardDetail()         - 특정 게시글 조회
5. updateTestBoard()        - 게시글 수정 테스트
6. deleteTestBoard()        - 게시글 삭제 테스트
7. getBoardsByCode()        - 코드별 게시글 조회
8. runFullTest()            - 전체 테스트 자동 실행

🔧 유틸리티:
- checkLoginStatus()        - 현재 로그인 상태 확인
- authToken                 - 현재 JWT 토큰 확인

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
`);