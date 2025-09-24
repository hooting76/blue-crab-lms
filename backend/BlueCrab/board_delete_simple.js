/**
 * 게시글 삭제 테스트 (심플 모달)
 */

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

function deleteBoard() {
    if (!window._accessToken) {
        alert('로그인이 필요합니다. loginTest()를 먼저 실행하세요.');
        return;
    }
    
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border:2px solid #ccc;z-index:9999;width:400px">
            <h3>게시글 삭제</h3>
            <input type="number" id="boardIdx" placeholder="삭제할 게시글 번호" style="width:100%;padding:5px;margin-bottom:10px;box-sizing:border-box"><br>
            <button onclick="previewBoard()" style="margin-bottom:10px">삭제 전 미리보기</button><br>
            <div id="preview" style="margin-bottom:10px;padding:10px;background:#f8f9fa;border-radius:4px;display:none;white-space:pre-line;font-size:13px;max-height:150px;overflow-y:auto"></div>
            <div style="background:#fff3cd;padding:8px;border-radius:4px;font-size:12px;margin-bottom:10px">
                ⚠️ 주의: 게시글은 실제로 삭제되지 않고 비활성화됩니다
            </div>
            <button onclick="doDelete()" style="background:#dc3545;color:white">삭제</button>
            <button onclick="closeModal()">닫기</button>
            <div id="result" style="margin-top:10px;font-weight:bold"></div>
        </div>
        <div style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9998" onclick="closeModal()"></div>
    `;
    
    document.body.appendChild(modal);
    window.currentModal = modal;
}

function boardExists() {
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border:2px solid #ccc;z-index:9999;width:350px">
            <h3>게시글 존재 확인</h3>
            <input type="number" id="boardIdx" placeholder="확인할 게시글 번호" style="width:100%;padding:5px;margin-bottom:10px;box-sizing:border-box"><br>
            <button onclick="checkExists()">확인</button>
            <button onclick="closeModal()">닫기</button>
            <div id="result" style="margin-top:10px;font-weight:bold"></div>
        </div>
        <div style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9998" onclick="closeModal()"></div>
    `;
    
    document.body.appendChild(modal);
    window.currentModal = modal;
}

async function previewBoard() {
    const boardIdx = document.getElementById('boardIdx').value;
    if (!boardIdx) {
        document.getElementById('result').innerHTML = '게시글 번호를 입력하세요';
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/boards/${boardIdx}`);
        
        if (response.ok) {
            const result = await response.json();
            const previewDiv = document.getElementById('preview');
            previewDiv.style.display = 'block';
            previewDiv.innerHTML = `삭제 대상 게시글:

ID: ${result.boardIdx}
제목: ${result.boardTitle}
작성자: ${result.boardWriter}
작성일: ${result.boardReg}

내용: ${result.boardContent}`;
            document.getElementById('result').innerHTML = '미리보기 로드됨';
        } else if (response.status === 404) {
            console.log('[게시글 미리보기] 게시글을 찾을 수 없음');
            document.getElementById('result').innerHTML = '게시글을 찾을 수 없습니다';
        } else {
            console.log('[게시글 미리보기] 실패:', response.status, response.statusText);
            document.getElementById('result').innerHTML = '게시글 조회 실패';
        }
    } catch (error) {
        console.log('[게시글 미리보기] 네트워크 오류:', error.message);
        document.getElementById('result').innerHTML = '게시글 조회 실패';
    }
}

async function doDelete() {
    const boardIdx = document.getElementById('boardIdx').value;
    if (!boardIdx) {
        document.getElementById('result').innerHTML = '게시글 번호를 입력하세요';
        return;
    }
    
    if (!confirm(`정말로 게시글 ${boardIdx}번을 삭제하시겠습니까?`)) {
        document.getElementById('result').innerHTML = '삭제가 취소되었습니다';
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/boards/delete/${boardIdx}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${window._accessToken}`
            }
        });
        
        if (response.ok) {
            console.log('[게시글 삭제] 성공');
            document.getElementById('result').innerHTML = `<span style="color:green">삭제 성공</span>`;
        } else if (response.status === 404) {
            console.log('[게시글 삭제] 게시글을 찾을 수 없음');
            document.getElementById('result').innerHTML = `<span style="color:orange">게시글을 찾을 수 없습니다</span>`;
        } else if (response.status === 403) {
            console.log('[게시글 삭제] 권한 없음');
            document.getElementById('result').innerHTML = `<span style="color:red">삭제 권한이 없습니다</span>`;
        } else {
            console.log('[게시글 삭제] 실패:', response.status, response.statusText);
            document.getElementById('result').innerHTML = `<span style="color:red">삭제 실패</span>`;
        }
    } catch (error) {
        console.log('[게시글 삭제] 실패:', error.message);
        document.getElementById('result').innerHTML = `<span style="color:red">삭제 실패</span>`;
    }
}

async function checkExists() {
    const boardIdx = document.getElementById('boardIdx').value;
    if (!boardIdx) {
        document.getElementById('result').innerHTML = '게시글 번호를 입력하세요';
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/boards/exists/${boardIdx}`);
        
        if (response.ok) {
            const result = await response.json();
            if (result === true) {
                console.log('[게시글 존재 확인] 게시글 존재함');
                document.getElementById('result').innerHTML = `<span style="color:green">존재함</span>`;
            } else {
                console.log('[게시글 존재 확인] 게시글 존재하지 않음');
                document.getElementById('result').innerHTML = `<span style="color:orange">존재하지 않음</span>`;
            }
        } else {
            console.log('[게시글 존재 확인] 실패:', response.status, response.statusText);
            document.getElementById('result').innerHTML = `<span style="color:red">확인 실패</span>`;
        }
    } catch (error) {
        console.log('[게시글 존재 확인] 네트워크 오류:', error.message);
        document.getElementById('result').innerHTML = `<span style="color:red">확인 실패</span>`;
    }
}

function closeModal() {
    if (window.currentModal) {
        window.currentModal.remove();
        window.currentModal = null;
    }
}

window.deleteBoard = deleteBoard;
window.boardExists = boardExists;