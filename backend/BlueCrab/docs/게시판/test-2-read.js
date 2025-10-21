// ========== 2단계: 게시글 조회 테스트 (BOARD_CODE 업데이트 반영) ==========

// ========== 설정 ==========
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// ========== 유틸리티 함수 ==========

function getToken() {
    return window.authToken || window.adminJwtToken || localStorage.getItem('jwtAccessToken');
}

async function apiRequest(url, method = 'POST', data = null, requireAuth = false) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        },
        body: data ? JSON.stringify(data) : null
    };

    if (requireAuth) {
        const token = getToken();
        if (token) {
            options.headers['Authorization'] = `Bearer ${token}`;
        }
    }

    try {
        console.log(`${method} ${url}`);
        if (data) console.log('📤 요청:', data);
        
        const response = await fetch(url, options);
        const result = await response.json();
        
        console.log(`📥 응답 (${response.status}):`, result);
        return { success: response.ok, status: response.status, data: result };
    } catch (error) {
        console.error('🔥 요청 실패:', error);
        return { success: false, error: error.message };
    }
}

// ========== BOARD_CODE 정보 ==========

const BOARD_CODES = {
    0: '학사공지',
    1: '행정공지',
    2: '기타공지',
    3: '강의공지'
};

function getBoardCodeName(code) {
    return BOARD_CODES[code] || `알 수 없음(${code})`;
}

// ========== 게시글 조회 함수 ==========

/**
 * 1. 게시글 상세 조회
 */
async function getBoardDetail(boardIdx) {
    if (!boardIdx) {
        boardIdx = parseInt(prompt('조회할 게시글 번호를 입력하세요:', window.lastCreatedBoard?.boardIdx || '1'));
    }
    
    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`🔍 게시글 상세 조회 (번호: ${boardIdx})`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

    const result = await apiRequest(`${API_BASE_URL}/api/boards/detail`, 'POST', { boardIdx });
    
    if (result.success) {
        const board = result.data;
        console.log('✅ 조회 성공!');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`📌 게시글 정보:`);
        console.log(`   - 번호: ${board.boardIdx}`);
        console.log(`   - 유형: ${getBoardCodeName(board.boardCode)} (${board.boardCode})`);
        if (board.lecSerial) {
            console.log(`   - 강의 코드: ${board.lecSerial}`);
        }
        console.log(`   - 제목: ${board.boardTitle}`);
        console.log(`   - 작성자: ${board.boardWriter}`);
        console.log(`   - 작성일: ${board.boardReg}`);
        console.log(`   - 조회수: ${board.boardView || 0}`);
        console.log(`\n📄 내용:\n${board.boardContent}`);
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return board;
    } else {
        console.error('❌ 조회 실패:', result.data?.message || result.error);
        return null;
    }
}

/**
 * 2. 게시글 목록 조회 (전체)
 */
async function getBoardList(page = 0, size = 10) {
    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`📋 게시글 목록 조회 (페이지: ${page}, 크기: ${size})`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

    const result = await apiRequest(`${API_BASE_URL}/api/boards/list`, 'POST', { page, size });
    
    if (result.success) {
        const data = result.data;
        console.log('✅ 조회 성공!');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`📊 통계:`);
        console.log(`   - 전체: ${data.totalElements}개`);
        console.log(`   - 페이지: ${data.number + 1}/${data.totalPages}`);
        console.log(`   - 현재 페이지 게시글: ${data.content.length}개`);
        console.log('\n📋 게시글 목록:');
        
        data.content.forEach((board, idx) => {
            console.log(`\n${idx + 1}. [${getBoardCodeName(board.boardCode)}] ${board.boardTitle}`);
            console.log(`   - 번호: ${board.boardIdx}`);
            console.log(`   - 작성자: ${board.boardWriter}`);
            console.log(`   - 작성일: ${board.boardReg}`);
            console.log(`   - 조회수: ${board.boardView || 0}`);
            if (board.lecSerial) {
                console.log(`   - 강의: ${board.lecSerial}`);
            }
        });
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return data;
    } else {
        console.error('❌ 조회 실패:', result.data?.message || result.error);
        return null;
    }
}

/**
 * 3. 특정 유형 게시글 조회
 */
async function getBoardListByCode(boardCode, page = 0, size = 10) {
    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`📋 ${getBoardCodeName(boardCode)} 목록 조회`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

    const result = await apiRequest(`${API_BASE_URL}/api/boards/list`, 'POST', { boardCode, page, size });
    
    if (result.success) {
        const data = result.data;
        console.log(`✅ ${getBoardCodeName(boardCode)} 조회 성공!`);
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`📊 통계:`);
        console.log(`   - 전체: ${data.totalElements}개`);
        console.log(`   - 페이지: ${data.number + 1}/${data.totalPages}`);
        console.log('\n📋 게시글 목록:');
        
        if (data.content.length === 0) {
            console.log('   (게시글이 없습니다)');
        } else {
            data.content.forEach((board, idx) => {
                console.log(`\n${idx + 1}. ${board.boardTitle}`);
                console.log(`   - 번호: ${board.boardIdx}`);
                console.log(`   - 작성자: ${board.boardWriter}`);
                console.log(`   - 작성일: ${board.boardReg}`);
                if (board.lecSerial) {
                    console.log(`   - 강의: ${board.lecSerial}`);
                }
            });
        }
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return data;
    } else {
        console.error('❌ 조회 실패:', result.data?.message || result.error);
        return null;
    }
}

/**
 * 4. 특정 강의 공지 조회
 */
async function getLectureNotices(lecSerial, page = 0, size = 10) {
    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`🎓 강의 공지 조회 (강의: ${lecSerial})`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

    const result = await apiRequest(`${API_BASE_URL}/api/boards/list`, 'POST', { 
        boardCode: 3,
        lecSerial,
        page,
        size 
    });
    
    if (result.success) {
        const data = result.data;
        console.log(`✅ ${lecSerial} 강의 공지 조회 성공!`);
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`📊 통계:`);
        console.log(`   - 전체: ${data.totalElements}개`);
        console.log(`   - 페이지: ${data.number + 1}/${data.totalPages}`);
        console.log('\n📋 강의 공지 목록:');
        
        if (data.content.length === 0) {
            console.log('   (강의 공지가 없습니다)');
        } else {
            data.content.forEach((board, idx) => {
                console.log(`\n${idx + 1}. ${board.boardTitle}`);
                console.log(`   - 번호: ${board.boardIdx}`);
                console.log(`   - 작성자: ${board.boardWriter}`);
                console.log(`   - 작성일: ${board.boardReg}`);
                console.log(`   - 조회수: ${board.boardView || 0}`);
            });
        }
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return data;
    } else {
        console.error('❌ 조회 실패:', result.data?.message || result.error);
        return null;
    }
}

/**
 * 5. 빠른 조회: 각 유형별 최신 5개
 */
async function quickViewAll() {
    console.log('\n🚀 전체 게시판 빠른 조회');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    const results = {};
    
    // 학사공지
    console.log('1️⃣ 학사공지 최신 5개');
    results.academic = await apiRequest(`${API_BASE_URL}/api/boards/list`, 'POST', { boardCode: 0, page: 0, size: 5 });
    if (results.academic.success) {
        console.log(`   ✅ ${results.academic.data.totalElements}개 중 ${results.academic.data.content.length}개 표시`);
    }
    
    await new Promise(resolve => setTimeout(resolve, 300));
    
    // 행정공지
    console.log('\n2️⃣ 행정공지 최신 5개');
    results.admin = await apiRequest(`${API_BASE_URL}/api/boards/list`, 'POST', { boardCode: 1, page: 0, size: 5 });
    if (results.admin.success) {
        console.log(`   ✅ ${results.admin.data.totalElements}개 중 ${results.admin.data.content.length}개 표시`);
    }
    
    await new Promise(resolve => setTimeout(resolve, 300));
    
    // 기타공지
    console.log('\n3️⃣ 기타공지 최신 5개');
    results.other = await apiRequest(`${API_BASE_URL}/api/boards/list`, 'POST', { boardCode: 2, page: 0, size: 5 });
    if (results.other.success) {
        console.log(`   ✅ ${results.other.data.totalElements}개 중 ${results.other.data.content.length}개 표시`);
    }
    
    await new Promise(resolve => setTimeout(resolve, 300));
    
    // 강의공지
    console.log('\n4️⃣ 강의공지 최신 5개');
    results.lecture = await apiRequest(`${API_BASE_URL}/api/boards/list`, 'POST', { boardCode: 3, page: 0, size: 5 });
    if (results.lecture.success) {
        console.log(`   ✅ ${results.lecture.data.totalElements}개 중 ${results.lecture.data.content.length}개 표시`);
    }
    
    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📊 전체 통계:');
    console.log(`   - 학사공지: ${results.academic.success ? results.academic.data.totalElements : 0}개`);
    console.log(`   - 행정공지: ${results.admin.success ? results.admin.data.totalElements : 0}개`);
    console.log(`   - 기타공지: ${results.other.success ? results.other.data.totalElements : 0}개`);
    console.log(`   - 강의공지: ${results.lecture.success ? results.lecture.data.totalElements : 0}개`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    return results;
}

/**
 * 6. 마지막 생성 게시글 조회
 */
async function viewLastCreated() {
    if (!window.lastCreatedBoard) {
        console.log('❌ 마지막 생성된 게시글 정보가 없습니다.');
        console.log('💡 먼저 test-1-create.js로 게시글을 생성하세요.');
        return null;
    }
    
    return await getBoardDetail(window.lastCreatedBoard.boardIdx);
}

// ========== 사용 안내 ==========

console.log(`
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 게시글 조회 테스트 (업데이트됨)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📝 사용 방법:

   1. 게시글 상세 조회:
      await getBoardDetail(1)          // 특정 번호
      await getBoardDetail()            // 프롬프트로 입력
      await viewLastCreated()           // 마지막 생성 게시글

   2. 게시글 목록 조회:
      await getBoardList()              // 전체 (첫 페이지)
      await getBoardList(1, 20)         // 2페이지, 20개씩

   3. 유형별 조회:
      await getBoardListByCode(0)       // 학사공지
      await getBoardListByCode(1)       // 행정공지
      await getBoardListByCode(2)       // 기타공지
      await getBoardListByCode(3)       // 강의공지

   4. 특정 강의 공지:
      await getLectureNotices('ETH201')
      await getLectureNotices('CS101', 0, 5)

   5. 전체 빠른 조회:
      await quickViewAll()              // 각 유형별 최신 5개

🔹 BOARD_CODE:
   0: 학사공지
   1: 행정공지
   2: 기타공지
   3: 강의공지 (lecSerial로 필터 가능)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
`);
