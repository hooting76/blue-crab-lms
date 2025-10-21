// ========== 3단계: 게시글 수정/삭제 테스트 (BOARD_CODE 업데이트 반영) ==========

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
    0: { name: '학사공지', requiresAuth: 'admin', requiresLecSerial: false },
    1: { name: '행정공지', requiresAuth: 'admin', requiresLecSerial: false },
    2: { name: '기타공지', requiresAuth: 'admin', requiresLecSerial: false },
    3: { name: '강의공지', requiresAuth: 'admin-or-professor', requiresLecSerial: true }
};

function getBoardCodeName(code) {
    return BOARD_CODES[code]?.name || `알 수 없음(${code})`;
}

function showBoardCodeInfo() {
    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📋 BOARD_CODE 정보');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    Object.entries(BOARD_CODES).forEach(([code, info]) => {
        console.log(`${code}: ${info.name}`);
        console.log(`   권한: ${info.requiresAuth === 'admin' ? '관리자만' : '관리자 + 교수'}`);
        console.log(`   lecSerial: ${info.requiresLecSerial ? '필수' : '불필요'}`);
    });
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
}

// ========== 게시글 수정 함수 ==========

/**
 * 1. 게시글 상세 조회 (수정/삭제 전 확인용)
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
        console.log(`\n📄 내용:\n${board.boardContent}`);
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        // 수정/삭제용으로 저장
        window.currentBoard = board;
        return board;
    } else {
        console.error('❌ 조회 실패:', result.data?.message || result.error);
        return null;
    }
}

/**
 * 2. 게시글 수정 (대화형)
 */
async function updateBoard(boardIdx) {
    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('✏️ 게시글 수정');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

    // 게시글 번호 확인
    if (!boardIdx) {
        boardIdx = window.currentBoard?.boardIdx || 
                   window.lastCreatedBoard?.boardIdx ||
                   parseInt(prompt('수정할 게시글 번호를 입력하세요:', '1'));
    }
    
    // 현재 게시글 정보 조회
    console.log('📥 현재 게시글 정보 조회 중...\n');
    const currentBoard = await getBoardDetail(boardIdx);
    
    if (!currentBoard) {
        console.error('❌ 게시글을 찾을 수 없습니다.');
        return null;
    }
    
    // 수정할 내용 입력
    const newTitle = prompt('새 제목 (비워두면 유지):', currentBoard.boardTitle);
    const newContent = prompt('새 내용 (비워두면 유지):', currentBoard.boardContent);
    
    // 강의공지인 경우 lecSerial도 수정 가능
    let newLecSerial = currentBoard.lecSerial;
    if (currentBoard.boardCode === 3) {
        const lecSerialInput = prompt('새 강의 코드 (비워두면 유지):', currentBoard.lecSerial);
        if (lecSerialInput?.trim()) {
            newLecSerial = lecSerialInput.trim();
        }
    }
    
    // 실제 변경사항 확인
    const hasChanges = (newTitle && newTitle !== currentBoard.boardTitle) ||
                       (newContent && newContent !== currentBoard.boardContent) ||
                       (newLecSerial !== currentBoard.lecSerial);
    
    if (!hasChanges) {
        console.log('ℹ️ 변경사항이 없습니다.');
        return currentBoard;
    }
    
    // 수정 요청 데이터
    const updateData = {
        boardTitle: newTitle?.trim() || currentBoard.boardTitle,
        boardContent: newContent?.trim() || currentBoard.boardContent,
        boardCode: currentBoard.boardCode
    };
    
    if (currentBoard.boardCode === 3) {
        updateData.lecSerial = newLecSerial;
    }
    
    console.log('\n📤 수정 요청 전송 중...');
    const result = await apiRequest(
        `${API_BASE_URL}/api/boards/update/${boardIdx}`, 
        'PUT', 
        updateData, 
        true
    );
    
    if (result.success) {
        console.log('\n✅ 게시글 수정 성공!');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`📌 수정된 게시글:`);
        console.log(`   - 번호: ${result.data.boardIdx}`);
        console.log(`   - 제목: ${result.data.boardTitle}`);
        console.log(`   - 유형: ${getBoardCodeName(result.data.boardCode)}`);
        if (result.data.lecSerial) {
            console.log(`   - 강의: ${result.data.lecSerial}`);
        }
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        window.lastUpdatedBoard = result.data;
        return result.data;
    } else {
        console.error('\n❌ 게시글 수정 실패!');
        console.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.error(`에러: ${result.data?.message || result.error}`);
        console.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return null;
    }
}

/**
 * 3. 게시글 삭제
 */
async function deleteBoard(boardIdx) {
    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('🗑️ 게시글 삭제');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

    // 게시글 번호 확인
    if (!boardIdx) {
        boardIdx = window.currentBoard?.boardIdx || 
                   window.lastCreatedBoard?.boardIdx ||
                   parseInt(prompt('삭제할 게시글 번호를 입력하세요:', '1'));
    }
    
    // 삭제 전 정보 조회
    console.log('📥 게시글 정보 조회 중...\n');
    const board = await getBoardDetail(boardIdx);
    
    if (!board) {
        console.error('❌ 게시글을 찾을 수 없습니다.');
        return false;
    }
    
    // 삭제 확인
    const confirmMsg = `정말로 "${board.boardTitle}" (번호: ${boardIdx})를 삭제하시겠습니까?`;
    const confirmed = confirm(confirmMsg);
    
    if (!confirmed) {
        console.log('ℹ️ 삭제가 취소되었습니다.');
        return false;
    }
    
    console.log('\n📤 삭제 요청 전송 중...');
    const result = await apiRequest(
        `${API_BASE_URL}/api/boards/delete/${boardIdx}`, 
        'DELETE', 
        null, 
        true
    );
    
    if (result.success) {
        console.log('\n✅ 게시글 삭제 성공!');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`🗑️ 삭제된 게시글: ${boardIdx}번`);
        console.log(`   제목: ${board.boardTitle}`);
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        window.currentBoard = null;
        return true;
    } else {
        console.error('\n❌ 게시글 삭제 실패!');
        console.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.error(`에러: ${result.data?.message || result.error}`);
        console.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return false;
    }
}

/**
 * 4. 빠른 수정 (제목만)
 */
async function quickUpdateTitle(boardIdx, newTitle) {
    if (!boardIdx || !newTitle) {
        console.error('❌ boardIdx와 newTitle이 필요합니다.');
        console.log('사용법: await quickUpdateTitle(1, "새로운 제목")');
        return null;
    }
    
    console.log('\n⚡ 빠른 제목 수정...');
    
    // 현재 게시글 조회
    const currentBoard = await getBoardDetail(boardIdx);
    if (!currentBoard) return null;
    
    const updateData = {
        boardTitle: newTitle,
        boardContent: currentBoard.boardContent,
        boardCode: currentBoard.boardCode
    };
    
    if (currentBoard.boardCode === 3) {
        updateData.lecSerial = currentBoard.lecSerial;
    }
    
    const result = await apiRequest(
        `${API_BASE_URL}/api/boards/update/${boardIdx}`, 
        'PUT', 
        updateData, 
        true
    );
    
    if (result.success) {
        console.log(`✅ 제목 수정 완료: "${newTitle}"`);
        return result.data;
    } else {
        console.error('❌ 수정 실패:', result.data?.message || result.error);
        return null;
    }
}

/**
 * 5. 빠른 수정 (내용만)
 */
async function quickUpdateContent(boardIdx, newContent) {
    if (!boardIdx || !newContent) {
        console.error('❌ boardIdx와 newContent가 필요합니다.');
        console.log('사용법: await quickUpdateContent(1, "새로운 내용")');
        return null;
    }
    
    console.log('\n⚡ 빠른 내용 수정...');
    
    // 현재 게시글 조회
    const currentBoard = await getBoardDetail(boardIdx);
    if (!currentBoard) return null;
    
    const updateData = {
        boardTitle: currentBoard.boardTitle,
        boardContent: newContent,
        boardCode: currentBoard.boardCode
    };
    
    if (currentBoard.boardCode === 3) {
        updateData.lecSerial = currentBoard.lecSerial;
    }
    
    const result = await apiRequest(
        `${API_BASE_URL}/api/boards/update/${boardIdx}`, 
        'PUT', 
        updateData, 
        true
    );
    
    if (result.success) {
        console.log('✅ 내용 수정 완료!');
        return result.data;
    } else {
        console.error('❌ 수정 실패:', result.data?.message || result.error);
        return null;
    }
}

/**
 * 6. 강의 공지 강의 코드 변경
 */
async function updateLecSerial(boardIdx, newLecSerial) {
    if (!boardIdx || !newLecSerial) {
        console.error('❌ boardIdx와 newLecSerial이 필요합니다.');
        console.log('사용법: await updateLecSerial(1, "ETH202")');
        return null;
    }
    
    console.log('\n🎓 강의 코드 변경...');
    
    // 현재 게시글 조회
    const currentBoard = await getBoardDetail(boardIdx);
    if (!currentBoard) return null;
    
    // 강의공지인지 확인
    if (currentBoard.boardCode !== 3) {
        console.error('❌ 강의공지(BOARD_CODE=3)만 lecSerial을 가질 수 있습니다.');
        console.log(`   현재 게시글 유형: ${getBoardCodeName(currentBoard.boardCode)}`);
        return null;
    }
    
    const updateData = {
        boardTitle: currentBoard.boardTitle,
        boardContent: currentBoard.boardContent,
        boardCode: 3,
        lecSerial: newLecSerial
    };
    
    const result = await apiRequest(
        `${API_BASE_URL}/api/boards/update/${boardIdx}`, 
        'PUT', 
        updateData, 
        true
    );
    
    if (result.success) {
        console.log(`✅ 강의 코드 변경 완료: ${currentBoard.lecSerial} → ${newLecSerial}`);
        return result.data;
    } else {
        console.error('❌ 변경 실패:', result.data?.message || result.error);
        return null;
    }
}

/**
 * 7. 마지막 생성 게시글 수정
 */
async function updateLastCreated() {
    if (!window.lastCreatedBoard) {
        console.log('❌ 마지막 생성된 게시글 정보가 없습니다.');
        console.log('💡 먼저 test-1-create.js로 게시글을 생성하세요.');
        return null;
    }
    
    return await updateBoard(window.lastCreatedBoard.boardIdx);
}

/**
 * 8. 마지막 생성 게시글 삭제
 */
async function deleteLastCreated() {
    if (!window.lastCreatedBoard) {
        console.log('❌ 마지막 생성된 게시글 정보가 없습니다.');
        console.log('💡 먼저 test-1-create.js로 게시글을 생성하세요.');
        return false;
    }
    
    return await deleteBoard(window.lastCreatedBoard.boardIdx);
}

// ========== 사용 안내 ==========

console.log(`
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✏️ 게시글 수정/삭제 테스트 (업데이트됨)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📝 사용 방법:

   1. 게시글 상세 조회:
      await getBoardDetail(1)          // 특정 번호
      await getBoardDetail()            // 프롬프트로 입력

   2. 게시글 수정 (대화형):
      await updateBoard(1)              // 특정 번호
      await updateBoard()               // 프롬프트로 입력
      await updateLastCreated()         // 마지막 생성 게시글

   3. 게시글 삭제:
      await deleteBoard(1)              // 특정 번호
      await deleteBoard()               // 프롬프트로 입력
      await deleteLastCreated()         // 마지막 생성 게시글

   4. 빠른 수정 (개별 필드):
      await quickUpdateTitle(1, "새 제목")
      await quickUpdateContent(1, "새 내용")
      await updateLecSerial(1, "ETH202")  // 강의공지만

   5. BOARD_CODE 정보:
      showBoardCodeInfo()               // 코드 정보 표시

🔹 BOARD_CODE:
   0: 학사공지 (관리자만, lecSerial 불필요)
   1: 행정공지 (관리자만, lecSerial 불필요)
   2: 기타공지 (관리자만, lecSerial 불필요)
   3: 강의공지 (관리자+교수, lecSerial 필수)

⚠️ 권한:
   - BOARD_CODE 0-2: 관리자만 수정/삭제 가능
   - BOARD_CODE 3: 관리자 또는 담당 교수만 수정/삭제 가능

💡 수정 시 팁:
   - 비워두면 기존 값 유지
   - 강의공지는 lecSerial도 변경 가능
   - 권한 없으면 실패 (403 Forbidden)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
`);
