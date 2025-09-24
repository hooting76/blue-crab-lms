/**
 * 표준화된 에러 처리 패턴
 */

// 네트워크 요청 표준 패턴
async function standardApiCall(apiName, url, options = {}) {
    console.log(`🔄 [${apiName}] 요청 시작`);
    console.log('├─ URL:', url);
    console.log('└─ 옵션:', options.method || 'GET');
    
    try {
        const response = await fetch(url, options);
        console.log(`📡 [${apiName}] 서버 응답:`, response.status, response.statusText);
        
        if (response.ok) {
            const result = await response.json();
            console.log(`✅ [${apiName}] 성공`);
            console.log('└─ 응답 데이터:', result);
            return { success: true, data: result };
        } else {
            const errorText = await response.text();
            console.error(`❌ [${apiName}] 실패`);
            console.error('├─ 상태 코드:', response.status);
            console.error('├─ 상태 메시지:', response.statusText);
            console.error('└─ 에러 내용:', errorText);
            return { success: false, error: errorText, status: response.status };
        }
    } catch (error) {
        console.error(`💥 [${apiName}] 네트워크 오류`);
        console.error('├─ 에러 타입:', error.name);
        console.error('├─ 에러 메시지:', error.message);
        console.error('└─ 전체 에러:', error);
        return { success: false, error: error.message, networkError: true };
    }
}

// 사용 예시
async function improvedDoGet() {
    const boardIdx = document.getElementById('boardIdx').value;
    if (!boardIdx) {
        console.warn('⚠️ [게시글 조회] 유효성 검사 실패: 게시글 번호 미입력');
        document.getElementById('result').innerHTML = '게시글 번호 입력하세요';
        return;
    }
    
    const result = await standardApiCall(
        '게시글 조회',
        `${API_BASE_URL}/api/boards/${boardIdx}`
    );
    
    if (result.success) {
        const board = result.data;
        console.log('📄 [게시글 조회] 게시글 정보');
        console.log('├─ ID:', board.boardIdx);
        console.log('├─ 제목:', board.boardTitle);
        console.log('├─ 작성자:', board.boardWriter);
        console.log('├─ 조회수:', board.boardView);
        console.log('└─ 작성일:', board.boardReg);
        
        document.getElementById('result').innerHTML = `제목: ${board.boardTitle}
작성자: ${board.boardWriter}
조회수: ${board.boardView}
작성일: ${board.boardReg}

내용: ${board.boardContent}`;
    } else if (result.status === 404) {
        console.warn('⚠️ [게시글 조회] 게시글을 찾을 수 없음');
        document.getElementById('result').innerHTML = '게시글을 찾을 수 없습니다';
    } else {
        document.getElementById('result').innerHTML = '조회 실패';
    }
}