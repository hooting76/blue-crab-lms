// ========== 1단계: 게시글 생성 테스트 (BOARD_CODE 업데이트 반영) ==========

// ========== 설정 ==========
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// 인증 토큰 (window.authToken 사용)
// 로그인은 별도 파일에서 수행 (일반유저 로그인 폴더의 test-1-login.js 참조)
let lastCreatedBoard = null;

// ========== 유틸리티 함수 ==========

function getToken() {
    return window.authToken || window.adminJwtToken || localStorage.getItem('jwtAccessToken');
}

function checkAuth() {
    const token = getToken();
    if (!token) {
        console.error('❌ 로그인 필요! 먼저 로그인을 수행하세요.');
        console.log('💡 해결 방법:');
        console.log('   - 관리자: docs/관리자 로그인 폴더의 파일들 실행');
        console.log('   - 교수/사용자: docs/일반유저 로그인 폴더의 test-1-login.js 실행');
        return false;
    }
    return true;
}

async function apiRequest(url, method = 'POST', data = null) {
    if (!checkAuth()) return { success: false, error: '인증 필요' };
    
    const token = getToken();
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: data ? JSON.stringify(data) : null
    };

    try {
        console.log(`${method} ${url}`);
        if (data) console.log('📤 요청 데이터:', data);
        
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
    0: { name: '학사공지', auth: '관리자 전용', requiresLecSerial: false },
    1: { name: '행정공지', auth: '관리자 전용', requiresLecSerial: false },
    2: { name: '기타공지', auth: '관리자 전용', requiresLecSerial: false },
    3: { name: '강의공지', auth: '관리자 + 교수', requiresLecSerial: true }
};

function showBoardCodeInfo() {
    console.log('\n📋 BOARD_CODE 정보');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    Object.entries(BOARD_CODES).forEach(([code, info]) => {
        console.log(`${code}: ${info.name}`);
        console.log(`   - 권한: ${info.auth}`);
        if (info.requiresLecSerial) {
            console.log(`   - lecSerial 필수 ⚠️`);
        }
    });
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
}

// ========== 게시글 생성 함수 ==========

/**
 * 1. 기본 게시글 생성 (프롬프트로 입력)
 */
async function createBoard() {
    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📝 게시글 생성');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    showBoardCodeInfo();
    
    const boardCodeInput = prompt(
        '게시글 코드를 선택하세요\n' +
        '0: 학사공지 (관리자)\n' +
        '1: 행정공지 (관리자)\n' +
        '2: 기타공지 (관리자)\n' +
        '3: 강의공지 (관리자+교수, lecSerial 필수)',
        '2'
    );
    
    const boardCode = parseInt(boardCodeInput);
    if (isNaN(boardCode) || boardCode < 0 || boardCode > 3) {
        console.error('❌ 올바른 게시글 코드를 입력하세요 (0-3)');
        return null;
    }
    
    const codeInfo = BOARD_CODES[boardCode];
    console.log(`\n📌 선택: ${codeInfo.name} (권한: ${codeInfo.auth})`);
    
    // lecSerial 입력 (BOARD_CODE 3인 경우 필수)
    let lecSerial = null;
    if (codeInfo.requiresLecSerial) {
        lecSerial = prompt(
            '강의 코드(LEC_SERIAL)를 입력하세요 (필수):\n' +
            '예: ETH201, CS101',
            'ETH201'
        );
        
        if (!lecSerial || lecSerial.trim() === '') {
            console.error('❌ 강의 공지 작성 시 lecSerial은 필수입니다!');
            return null;
        }
        console.log(`📚 강의 코드: ${lecSerial}`);
    }
    
    const title = prompt(
        `${codeInfo.name} 제목을 입력하세요:`,
        `${codeInfo.name} - 테스트`
    );
    
    const content = prompt(
        `${codeInfo.name} 내용을 입력하세요:`,
        `${codeInfo.name} 내용입니다. 테스트 게시글입니다.`
    );
    
    const boardData = {
        boardTitle: title,
        boardContent: content,
        boardCode: boardCode
    };
    
    if (lecSerial) {
        boardData.lecSerial = lecSerial;
    }
    
    console.log('\n📤 게시글 생성 요청...');
    const result = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', boardData);
    
    if (result.success) {
        console.log('\n✅ 게시글 생성 성공!');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📌 게시글 정보:');
        console.log(`   - 번호: ${result.data.boardIdx}`);
        console.log(`   - 제목: ${result.data.boardTitle}`);
        console.log(`   - 작성자: ${result.data.boardWriter}`);
        console.log(`   - 유형: ${codeInfo.name}`);
        if (result.data.lecSerial) {
            console.log(`   - 강의 코드: ${result.data.lecSerial}`);
        }
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        lastCreatedBoard = result.data;
        window.lastCreatedBoard = result.data;
        return result.data;
    } else {
        console.error('\n❌ 게시글 생성 실패');
        console.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.error('에러:', result.data?.message || result.error);
        console.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return null;
    }
}

/**
 * 2. 학사 공지 생성 (관리자 전용)
 */
async function createAcademicNotice() {
    console.log('\n📚 학사 공지 생성 (BOARD_CODE = 0)');
    
    const boardData = {
        boardTitle: prompt('학사 공지 제목:', '2025학년도 1학기 학사일정 안내'),
        boardContent: prompt('학사 공지 내용:', '2025학년도 1학기 학사일정을 안내합니다.'),
        boardCode: 0
    };
    
    const result = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', boardData);
    
    if (result.success) {
        console.log('✅ 학사 공지 생성 성공!');
        console.log('게시글 번호:', result.data.boardIdx);
        lastCreatedBoard = result.data;
        window.lastCreatedBoard = result.data;
        return result.data;
    } else {
        console.error('❌ 학사 공지 생성 실패:', result.data?.message || result.error);
        return null;
    }
}

/**
 * 3. 행정 공지 생성 (관리자 전용)
 */
async function createAdminNotice() {
    console.log('\n🏢 행정 공지 생성 (BOARD_CODE = 1)');
    
    const boardData = {
        boardTitle: prompt('행정 공지 제목:', '학생증 발급 안내'),
        boardContent: prompt('행정 공지 내용:', '학생증 발급 관련 안내사항입니다.'),
        boardCode: 1
    };
    
    const result = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', boardData);
    
    if (result.success) {
        console.log('✅ 행정 공지 생성 성공!');
        console.log('게시글 번호:', result.data.boardIdx);
        lastCreatedBoard = result.data;
        window.lastCreatedBoard = result.data;
        return result.data;
    } else {
        console.error('❌ 행정 공지 생성 실패:', result.data?.message || result.error);
        return null;
    }
}

/**
 * 4. 기타 공지 생성 (관리자 전용)
 */
async function createOtherNotice() {
    console.log('\n📢 기타 공지 생성 (BOARD_CODE = 2)');
    
    const boardData = {
        boardTitle: prompt('기타 공지 제목:', '기타 안내사항'),
        boardContent: prompt('기타 공지 내용:', '기타 안내사항입니다.'),
        boardCode: 2
    };
    
    const result = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', boardData);
    
    if (result.success) {
        console.log('✅ 기타 공지 생성 성공!');
        console.log('게시글 번호:', result.data.boardIdx);
        lastCreatedBoard = result.data;
        window.lastCreatedBoard = result.data;
        return result.data;
    } else {
        console.error('❌ 기타 공지 생성 실패:', result.data?.message || result.error);
        return null;
    }
}

/**
 * 5. 강의 공지 생성 (관리자 + 교수, lecSerial 필수)
 */
async function createLectureNotice(lecSerial = 'ETH201') {
    console.log('\n🎓 강의 공지 생성 (BOARD_CODE = 3)');
    console.log(`📚 강의 코드: ${lecSerial}`);
    
    const boardData = {
        boardTitle: prompt('강의 공지 제목:', '중간고사 안내'),
        boardContent: prompt('강의 공지 내용:', '중간고사는 10월 25일에 실시됩니다.'),
        boardCode: 3,
        lecSerial: lecSerial
    };
    
    const result = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', boardData);
    
    if (result.success) {
        console.log('✅ 강의 공지 생성 성공!');
        console.log('게시글 번호:', result.data.boardIdx);
        console.log('강의 코드:', result.data.lecSerial);
        lastCreatedBoard = result.data;
        window.lastCreatedBoard = result.data;
        return result.data;
    } else {
        console.error('❌ 강의 공지 생성 실패:', result.data?.message || result.error);
        return null;
    }
}

/**
 * 6. 강의 공지 검증 테스트 (LEC_SERIAL 필수 확인)
 */
async function testLectureNoticeValidation() {
    console.log('\n🧪 강의 공지 검증 테스트');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    // 1. LEC_SERIAL 없이 강의 공지 시도 (실패해야 함)
    console.log('1️⃣ LEC_SERIAL 없이 강의 공지 생성 시도...');
    const failResult = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', {
        boardTitle: '테스트 강의 공지 (lecSerial 없음)',
        boardContent: 'LEC_SERIAL 없이 생성 시도',
        boardCode: 3
        // lecSerial 의도적으로 누락
    });
    
    if (!failResult.success) {
        console.log('✅ 예상대로 실패: LEC_SERIAL 필수 검증 작동');
        console.log('   에러 메시지:', failResult.data?.message || failResult.error);
    } else {
        console.log('❌ 예상과 다름: LEC_SERIAL 없이도 생성됨 (버그 가능성)');
    }
    
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 2. 빈 LEC_SERIAL로 강의 공지 시도 (실패해야 함)
    console.log('\n2️⃣ 빈 LEC_SERIAL로 강의 공지 생성 시도...');
    const emptyResult = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', {
        boardTitle: '테스트 강의 공지 (빈 lecSerial)',
        boardContent: '빈 LEC_SERIAL로 생성 시도',
        boardCode: 3,
        lecSerial: ''  // 빈 문자열
    });
    
    if (!emptyResult.success) {
        console.log('✅ 예상대로 실패: 빈 LEC_SERIAL 검증 작동');
        console.log('   에러 메시지:', emptyResult.data?.message || emptyResult.error);
    } else {
        console.log('❌ 예상과 다름: 빈 LEC_SERIAL로도 생성됨 (버그 가능성)');
    }
    
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 3. 공백만 있는 LEC_SERIAL로 시도 (실패해야 함)
    console.log('\n3️⃣ 공백 LEC_SERIAL로 강의 공지 생성 시도...');
    const spaceResult = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', {
        boardTitle: '테스트 강의 공지 (공백 lecSerial)',
        boardContent: '공백 LEC_SERIAL로 생성 시도',
        boardCode: 3,
        lecSerial: '   '  // 공백만
    });
    
    if (!spaceResult.success) {
        console.log('✅ 예상대로 실패: 공백 LEC_SERIAL 검증 작동');
        console.log('   에러 메시지:', spaceResult.data?.message || spaceResult.error);
    } else {
        console.log('❌ 예상과 다름: 공백 LEC_SERIAL로도 생성됨 (버그 가능성)');
    }
    
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 4. 올바른 LEC_SERIAL로 강의 공지 생성 (성공해야 함)
    console.log('\n4️⃣ 올바른 LEC_SERIAL로 강의 공지 생성...');
    const successResult = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', {
        boardTitle: '테스트 강의 공지 (올바른 lecSerial)',
        boardContent: '올바른 LEC_SERIAL로 생성',
        boardCode: 3,
        lecSerial: 'ETH201'
    });
    
    if (successResult.success) {
        console.log('✅ 예상대로 성공: 올바른 강의 공지 생성');
        console.log('   게시글 번호:', successResult.data?.boardIdx);
        window.lastCreatedBoard = successResult.data;
    } else {
        console.log('❌ 예상과 다름: 올바른 요청이 실패함');
        console.log('   에러 메시지:', successResult.data?.message || successResult.error);
    }
    
    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📋 LEC_SERIAL 검증 테스트 완료');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    return {
        noLecSerial: failResult,
        emptyLecSerial: emptyResult,
        spaceLecSerial: spaceResult,
        validLecSerial: successResult
    };
}

/**
 * 7. 빠른 테스트: 모든 타입 게시글 생성
 */
async function createAllTypes() {
    console.log('\n🚀 모든 타입 게시글 생성 테스트');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    const results = {
        academic: null,
        admin: null,
        other: null,
        lecture: null
    };
    
    // 학사 공지
    console.log('1️⃣ 학사 공지 생성...');
    results.academic = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', {
        boardTitle: '테스트 학사 공지',
        boardContent: '자동 테스트로 생성된 학사 공지입니다.',
        boardCode: 0
    });
    
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 행정 공지
    console.log('\n2️⃣ 행정 공지 생성...');
    results.admin = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', {
        boardTitle: '테스트 행정 공지',
        boardContent: '자동 테스트로 생성된 행정 공지입니다.',
        boardCode: 1
    });
    
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 기타 공지
    console.log('\n3️⃣ 기타 공지 생성...');
    results.other = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', {
        boardTitle: '테스트 기타 공지',
        boardContent: '자동 테스트로 생성된 기타 공지입니다.',
        boardCode: 2
    });
    
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 강의 공지
    console.log('\n4️⃣ 강의 공지 생성...');
    results.lecture = await apiRequest(`${API_BASE_URL}/api/boards/create`, 'POST', {
        boardTitle: '테스트 강의 공지',
        boardContent: '자동 테스트로 생성된 강의 공지입니다.',
        boardCode: 3,
        lecSerial: 'ETH201'
    });
    
    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📊 생성 결과 요약:');
    console.log(`   - 학사 공지: ${results.academic.success ? '✅ 성공' : '❌ 실패'}`);
    console.log(`   - 행정 공지: ${results.admin.success ? '✅ 성공' : '❌ 실패'}`);
    console.log(`   - 기타 공지: ${results.other.success ? '✅ 성공' : '❌ 실패'}`);
    console.log(`   - 강의 공지: ${results.lecture.success ? '✅ 성공' : '❌ 실패'}`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    return results;
}

// ========== 사용 안내 ==========

console.log(`
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📝 게시글 생성 테스트 (업데이트됨)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔹 BOARD_CODE 정의 (변경됨):
   0: 학사공지 (관리자 전용)
   1: 행정공지 (관리자 전용)
   2: 기타공지 (관리자 전용)
   3: 강의공지 (관리자 + 교수, lecSerial 필수)

📝 사용 방법:

   1. 프롬프트로 입력:
      await createBoard()

   2. 개별 타입 생성:
      await createAcademicNotice()    // 학사 공지
      await createAdminNotice()        // 행정 공지
      await createOtherNotice()        // 기타 공지
      await createLectureNotice('ETH201')  // 강의 공지

   3. LEC_SERIAL 검증 테스트:
      await testLectureNoticeValidation()  // 필수값 검증 확인

   4. 전체 타입 자동 생성:
      await createAllTypes()

   5. BOARD_CODE 정보 확인:
      showBoardCodeInfo()

   6. 마지막 생성 게시글 확인:
      window.lastCreatedBoard

⚠️  주의사항:
   - 사전에 로그인 필요 (window.authToken 설정)
   - BOARD_CODE 0~2는 관리자만 작성 가능
   - BOARD_CODE 3은 lecSerial 필수
   - 교수는 본인 담당 강의만 작성 가능

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
`);
