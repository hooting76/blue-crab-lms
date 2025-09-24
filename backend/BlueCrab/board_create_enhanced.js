/**
 * 개선된 콘솔 로그 버전 - 게시글 작성 테스트
 */

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

function createBoard() {
    if (!window._accessToken) {
        console.warn('⚠️ [인증] 로그인이 필요합니다');
        alert('로그인이 필요합니다. loginTest()를 먼저 실행하세요.');
        return;
    }
    
    console.log('🎯 [게시글 작성] 모달 열기');
    
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border:2px solid #ccc;z-index:9999;width:400px">
            <h3>게시글 작성</h3>
            <select id="boardCode" style="width:100%;padding:5px;margin-bottom:10px">
                <option value="0">0 - 학교공지</option>
                <option value="1">1 - 학사공지</option>
                <option value="2">2 - 학과공지</option>
                <option value="3" selected>3 - 교수공지</option>
            </select><br>
            <input type="text" id="boardTitle" placeholder="제목 (비워두면 기본값)" style="width:100%;padding:5px;margin-bottom:10px;box-sizing:border-box"><br>
            <textarea id="boardContent" placeholder="내용" rows="4" style="width:100%;padding:5px;margin-bottom:10px;box-sizing:border-box"></textarea><br>
            <button onclick="doCreate()">작성</button>
            <button onclick="closeModal()">닫기</button>
            <div id="result" style="margin-top:10px;font-weight:bold"></div>
        </div>
        <div style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9998" onclick="closeModal()"></div>
    `;
    
    document.body.appendChild(modal);
    window.currentModal = modal;
}

async function doCreate() {
    const boardCode = parseInt(document.getElementById('boardCode').value);
    const title = document.getElementById('boardTitle').value.trim();
    const content = document.getElementById('boardContent').value.trim();
    
    const codeNames = ['학교공지', '학사공지', '학과공지', '교수공지'];
    
    console.log('📝 [게시글 작성] 요청 시작');
    console.log('├─ 코드:', boardCode, `(${codeNames[boardCode]})`);
    console.log('├─ 제목:', title || '(기본값 사용)');
    console.log('├─ 내용 길이:', content.length, '문자');
    console.log('└─ API URL:', `${API_BASE_URL}/api/boards/create`);
    
    if (!content) {
        console.error('❌ [게시글 작성] 유효성 검사 실패: 내용이 비어있음');
        document.getElementById('result').innerHTML = '<span style="color:red">내용은 필수입니다</span>';
        return;
    }
    
    const data = { boardCode, boardContent: content };
    if (title) data.boardTitle = title;
    
    console.log('🔄 [게시글 작성] 서버 전송 중...');
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/boards/create`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${window._accessToken}`
            },
            body: JSON.stringify(data)
        });
        
        console.log('📡 [게시글 작성] 서버 응답:', response.status, response.statusText);
        
        if (response.ok) {
            const result = await response.json();
            console.log('✅ [게시글 작성] 성공!');
            console.log('├─ 생성된 ID:', result.boardIdx);
            console.log('├─ 제목:', result.boardTitle);
            console.log('├─ 작성자:', result.boardWriter);
            console.log('├─ 작성일:', result.boardReg);
            console.log('└─ 조회수:', result.boardView, '(초기값)');
            
            document.getElementById('result').innerHTML = `<span style="color:green">성공 (ID: ${result.boardIdx})</span>`;
        } else {
            const errorText = await response.text();
            console.error('❌ [게시글 작성] 실패');
            console.error('├─ 상태 코드:', response.status);
            console.error('├─ 상태 메시지:', response.statusText);
            console.error('└─ 에러 내용:', errorText);
            
            document.getElementById('result').innerHTML = '<span style="color:red">실패</span>';
        }
    } catch (error) {
        console.error('💥 [게시글 작성] 네트워크 오류');
        console.error('├─ 에러 타입:', error.name);
        console.error('├─ 에러 메시지:', error.message);
        console.error('└─ 전체 에러:', error);
        
        document.getElementById('result').innerHTML = '<span style="color:red">실패</span>';
    }
}

function closeModal() {
    if (window.currentModal) {
        console.log('🔒 [UI] 모달 닫기');
        window.currentModal.remove();
        window.currentModal = null;
    }
}

window.createBoard = createBoard;