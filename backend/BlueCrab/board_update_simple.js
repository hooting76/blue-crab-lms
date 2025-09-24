/**
 * 게시글 조회/수정 테스트 (심플 모달)
 */

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

function getBoard() {
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border:2px solid #ccc;z-index:9999;width:400px">
            <h3>게시글 조회</h3>
            <input type="number" id="boardIdx" placeholder="게시글 번호" style="width:100%;padding:5px;margin-bottom:10px;box-sizing:border-box"><br>
            <button onclick="doGet()">조회</button>
            <button onclick="closeModal()">닫기</button>
            <div id="result" style="margin-top:10px;font-weight:bold;white-space:pre-line;max-height:200px;overflow-y:auto"></div>
        </div>
        <div style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9998" onclick="closeModal()"></div>
    `;
    
    document.body.appendChild(modal);
    window.currentModal = modal;
}

function updateBoard() {
    if (!window._accessToken) {
        alert('로그인이 필요합니다. loginTest()를 먼저 실행하세요.');
        return;
    }
    
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border:2px solid #ccc;z-index:9999;width:400px">
            <h3>게시글 수정</h3>
            <input type="number" id="boardIdx" placeholder="게시글 번호" style="width:100%;padding:5px;margin-bottom:10px;box-sizing:border-box"><br>
            <button onclick="loadBoard()" style="margin-bottom:10px">기존 내용 불러오기</button><br>
            <input type="text" id="newTitle" placeholder="새 제목 (비워두면 수정안함)" style="width:100%;padding:5px;margin-bottom:10px;box-sizing:border-box"><br>
            <textarea id="newContent" placeholder="새 내용 (비워두면 수정안함)" rows="3" style="width:100%;padding:5px;margin-bottom:10px;box-sizing:border-box"></textarea><br>
            <button onclick="doUpdate()">수정</button>
            <button onclick="closeModal()">닫기</button>
            <div id="result" style="margin-top:10px;font-weight:bold;white-space:pre-line"></div>
        </div>
        <div style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9998" onclick="closeModal()"></div>
    `;
    
    document.body.appendChild(modal);
    window.currentModal = modal;
}

async function doGet() {
    const boardIdx = document.getElementById('boardIdx').value;
    if (!boardIdx) {
        document.getElementById('result').innerHTML = '게시글 번호 입력하세요';
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/boards/${boardIdx}`);
        
        if (response.ok) {
            const result = await response.json();
            console.log('[게시글 조회] 성공:', `ID ${result.boardIdx} - ${result.boardTitle}`);
            document.getElementById('result').innerHTML = `제목: ${result.boardTitle}
작성자: ${result.boardWriter}
조회수: ${result.boardView}
작성일: ${result.boardReg}

내용: ${result.boardContent}`;
        } else if (response.status === 404) {
            console.log('[게시글 조회] 게시글을 찾을 수 없음');
            document.getElementById('result').innerHTML = '게시글을 찾을 수 없습니다';
        } else if (response.status === 403) {
            console.log('[게시글 조회] 권한 없음');
            document.getElementById('result').innerHTML = '접근 권한이 없습니다';
        } else {
            console.log('[게시글 조회] 실패:', response.status, response.statusText);
            document.getElementById('result').innerHTML = '조회 실패';
        }
    } catch (error) {
        console.log('[게시글 조회] 실패:', error.message);
        document.getElementById('result').innerHTML = '조회 실패';
    }
}

async function loadBoard() {
    const boardIdx = document.getElementById('boardIdx').value;
    if (!boardIdx) {
        document.getElementById('result').innerHTML = '게시글 번호를 먼저 입력하세요';
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/boards/${boardIdx}`);
        
        if (response.ok) {
            const result = await response.json();
            document.getElementById('newTitle').placeholder = `현재: ${result.boardTitle}`;
            document.getElementById('newContent').placeholder = `현재: ${result.boardContent}`;
            document.getElementById('result').innerHTML = '기존 내용을 불러왔습니다';
        } else if (response.status === 404) {
            console.log('[게시글 불러오기] 게시글을 찾을 수 없음');
            document.getElementById('result').innerHTML = '게시글을 찾을 수 없습니다';
        } else {
            console.log('[게시글 불러오기] 실패:', response.status, response.statusText);
            document.getElementById('result').innerHTML = '불러오기 실패';
        }
    } catch (error) {
        console.log('[게시글 불러오기] 네트워크 오류:', error.message);
        document.getElementById('result').innerHTML = '불러오기 실패';
    }
}

async function doUpdate() {
    const boardIdx = document.getElementById('boardIdx').value;
    const newTitle = document.getElementById('newTitle').value.trim();
    const newContent = document.getElementById('newContent').value.trim();
    
    if (!boardIdx) {
        document.getElementById('result').innerHTML = '게시글 번호 입력하세요';
        return;
    }
    
    if (!newTitle && !newContent) {
        document.getElementById('result').innerHTML = '제목 또는 내용 중 하나는 입력해야 합니다';
        return;
    }
    
    const data = {};
    if (newTitle) data.boardTitle = newTitle;
    if (newContent) data.boardContent = newContent;
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/boards/update/${boardIdx}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${window._accessToken}`
            },
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            console.log('[게시글 수정] 성공');
            document.getElementById('result').innerHTML = '수정 성공';
        } else if (response.status === 404) {
            console.log('[게시글 수정] 게시글을 찾을 수 없음');
            document.getElementById('result').innerHTML = '게시글을 찾을 수 없습니다';
        } else if (response.status === 403) {
            console.log('[게시글 수정] 권한 없음');
            document.getElementById('result').innerHTML = '수정 권한이 없습니다';
        } else {
            console.log('[게시글 수정] 실패:', response.status, response.statusText);
            document.getElementById('result').innerHTML = '수정 실패';
        }
    } catch (error) {
        console.log('[게시글 수정] 네트워크 오류:', error.message);
        document.getElementById('result').innerHTML = '수정 실패';
    }
}

function closeModal() {
    if (window.currentModal) {
        window.currentModal.remove();
        window.currentModal = null;
    }
}

window.getBoard = getBoard;
window.updateBoard = updateBoard;