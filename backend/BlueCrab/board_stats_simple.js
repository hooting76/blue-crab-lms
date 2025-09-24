/**
 * 게시글 통계/조회 테스트 (심플 모달)
 */

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

function boardStats() {
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border:2px solid #ccc;z-index:9999;width:350px">
            <h3>게시판 통계 & 조회</h3>
            <button onclick="totalCount()" style="width:100%;margin:5px 0;padding:8px;background:#007bff;color:white">전체 게시글 개수</button>
            <button onclick="countByCode()" style="width:100%;margin:5px 0;padding:8px;background:#28a745;color:white">코드별 게시글 개수</button>
            <button onclick="boardList()" style="width:100%;margin:5px 0;padding:8px;background:#17a2b8;color:white">게시글 목록</button>
            <button onclick="listByCode()" style="width:100%;margin:5px 0;padding:8px;background:#ffc107;color:#212529">코드별 게시글 목록</button>
            <button onclick="boardDetail()" style="width:100%;margin:5px 0;padding:8px;background:#6f42c1;color:white">특정 게시글 상세</button>
            <button onclick="closeModal()" style="width:100%;margin:10px 0;padding:8px;background:#6c757d;color:white">닫기</button>
        </div>
        <div style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9998" onclick="closeModal()"></div>
    `;
    
    document.body.appendChild(modal);
    window.currentModal = modal;
}

function totalCount() {
    closeModal();
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border:2px solid #ccc;z-index:9999">
            <h3>전체 게시글 개수</h3>
            <button onclick="getTotalCount()">조회</button>
            <button onclick="backToStats()">메뉴로</button>
            <div id="result" style="margin-top:10px;font-weight:bold;text-align:center;font-size:18px"></div>
        </div>
        <div style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9998" onclick="closeModal()"></div>
    `;
    document.body.appendChild(modal);
    window.currentModal = modal;
}

function countByCode() {
    closeModal();
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border:2px solid #ccc;z-index:9999;width:300px">
            <h3>코드별 게시글 개수</h3>
            <select id="boardCode" style="width:100%;padding:5px;margin-bottom:10px">
                <option value="0">0 - 학교공지</option>
                <option value="1">1 - 학사공지</option>
                <option value="2">2 - 학과공지</option>
                <option value="3">3 - 교수공지</option>
            </select><br>
            <button onclick="getCountByCode()">조회</button>
            <button onclick="backToStats()">메뉴로</button>
            <div id="result" style="margin-top:10px;font-weight:bold;text-align:center;font-size:18px"></div>
        </div>
        <div style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9998" onclick="closeModal()"></div>
    `;
    document.body.appendChild(modal);
    window.currentModal = modal;
}

function boardList() {
    closeModal();
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border:2px solid #ccc;z-index:9999;width:500px">
            <h3>게시글 목록</h3>
            <input type="number" id="pageNum" value="0" min="0" placeholder="페이지" style="width:48%;padding:5px;margin-right:4%">
            <input type="number" id="pageSize" value="10" min="1" max="50" placeholder="크기" style="width:48%;padding:5px"><br><br>
            <button onclick="getBoardList()">조회</button>
            <button onclick="backToStats()">메뉴로</button>
            <div id="result" style="margin-top:10px;font-family:monospace;font-size:12px;max-height:300px;overflow-y:auto;white-space:pre-line"></div>
        </div>
        <div style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9998" onclick="closeModal()"></div>
    `;
    document.body.appendChild(modal);
    window.currentModal = modal;
}

function listByCode() {
    closeModal();
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border:2px solid #ccc;z-index:9999;width:500px">
            <h3>코드별 게시글 목록</h3>
            <select id="boardCode" style="width:100%;padding:5px;margin-bottom:10px">
                <option value="0">0 - 학교공지</option>
                <option value="1">1 - 학사공지</option>
                <option value="2">2 - 학과공지</option>
                <option value="3">3 - 교수공지</option>
            </select><br>
            <input type="number" id="pageNum" value="0" min="0" placeholder="페이지" style="width:48%;padding:5px;margin-right:4%">
            <input type="number" id="pageSize" value="10" min="1" max="50" placeholder="크기" style="width:48%;padding:5px"><br><br>
            <button onclick="getListByCode()">조회</button>
            <button onclick="backToStats()">메뉴로</button>
            <div id="result" style="margin-top:10px;font-family:monospace;font-size:12px;max-height:300px;overflow-y:auto;white-space:pre-line"></div>
        </div>
        <div style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9998" onclick="closeModal()"></div>
    `;
    document.body.appendChild(modal);
    window.currentModal = modal;
}

function boardDetail() {
    closeModal();
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border:2px solid #ccc;z-index:9999;width:450px">
            <h3>게시글 상세 조회</h3>
            <input type="number" id="boardIdx" placeholder="게시글 번호" style="width:100%;padding:5px;margin-bottom:10px;box-sizing:border-box"><br>
            <button onclick="getBoardDetail()">조회</button>
            <button onclick="backToStats()">메뉴로</button>
            <div id="result" style="margin-top:10px;font-size:13px;white-space:pre-line"></div>
        </div>
        <div style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9998" onclick="closeModal()"></div>
    `;
    document.body.appendChild(modal);
    window.currentModal = modal;
}

async function getTotalCount() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/boards/count`);
        console.log('[전체 게시글 개수] 서버 응답:', response.status, response.statusText);
        
        if (response.ok) {
            const result = await response.text();
            console.log('[전체 게시글 개수] 성공 (' + response.status + '):', result + '개');
            document.getElementById('result').innerHTML = `전체 게시글: ${result}개`;
        } else {
            console.log('[전체 게시글 개수] 실패 (' + response.status + '):', response.statusText);
            document.getElementById('result').innerHTML = '조회 실패 (' + response.status + ')';
        }
    } catch (error) {
        console.log('[전체 게시글 개수] 실패:', error.message);
        document.getElementById('result').innerHTML = '조회 실패';
    }
}

async function getCountByCode() {
    const boardCode = document.getElementById('boardCode').value;
    const codeNames = ['학교공지', '학사공지', '학과공지', '교수공지'];
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/boards/count/bycode/${boardCode}`);
        console.log('[코드별 게시글 개수] 서버 응답:', response.status, response.statusText);
        
        if (response.ok) {
            const result = await response.text();
            console.log(`[코드별 게시글 개수] 성공 (${response.status}):`, `${codeNames[boardCode]} ${result}개`);
            document.getElementById('result').innerHTML = `${codeNames[boardCode]}: ${result}개`;
        } else {
            console.log('[코드별 게시글 개수] 실패 (' + response.status + '):', response.statusText);
            document.getElementById('result').innerHTML = '조회 실패 (' + response.status + ')';
        }
    } catch (error) {
        console.log('[코드별 게시글 개수] 실패:', error.message);
        document.getElementById('result').innerHTML = '조회 실패';
    }
}

async function getBoardList() {
    const page = document.getElementById('pageNum').value || 0;
    const size = document.getElementById('pageSize').value || 10;
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/boards/list?page=${page}&size=${size}`);
        if (response.ok) {
            const result = await response.json();
            console.log('[게시글 목록] 성공:', `${result.totalElements}개 게시글 (${parseInt(page) + 1}/${result.totalPages} 페이지)`);
            let listText = `게시글 목록 (${parseInt(page) + 1}/${result.totalPages} 페이지, 총 ${result.totalElements}개)\n\n`;
            
            result.content.forEach((board, index) => {
                listText += `${index + 1}. [${board.boardIdx}] ${board.boardTitle}\n`;
                listText += `   작성자: ${board.boardWriter} | 조회수: ${board.boardView}\n\n`;
            });
            
            document.getElementById('result').innerHTML = listText;
        } else {
            console.log('[게시글 목록] 실패:', response.status, response.statusText);
            document.getElementById('result').innerHTML = '조회 실패';
        }
    } catch (error) {
        console.log('[게시글 목록] 실패:', error.message);
        document.getElementById('result').innerHTML = '조회 실패';
    }
}

async function getListByCode() {
    const boardCode = document.getElementById('boardCode').value;
    const page = document.getElementById('pageNum').value || 0;
    const size = document.getElementById('pageSize').value || 10;
    const codeNames = ['학교공지', '학사공지', '학과공지', '교수공지'];
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/boards/bycode/${boardCode}?page=${page}&size=${size}`);
        if (response.ok) {
            const result = await response.json();
            console.log(`[코드별 목록] 성공:`, `${codeNames[boardCode]} ${result.totalElements}개 (${parseInt(page) + 1}/${result.totalPages} 페이지)`);
            let listText = `${codeNames[boardCode]} 목록 (${parseInt(page) + 1}/${result.totalPages} 페이지, 총 ${result.totalElements}개)\n\n`;
            
            result.content.forEach((board, index) => {
                listText += `${index + 1}. [${board.boardIdx}] ${board.boardTitle}\n`;
                listText += `   작성자: ${board.boardWriter} | 조회수: ${board.boardView}\n\n`;
            });
            
            if (result.content.length === 0) {
                listText += '해당 코드의 게시글이 없습니다.';
            }
            
            document.getElementById('result').innerHTML = listText;
        } else {
            console.log('[코드별 목록] 실패:', response.status, response.statusText);
            document.getElementById('result').innerHTML = '조회 실패';
        }
    } catch (error) {
        console.log('[코드별 목록] 실패:', error.message);
        document.getElementById('result').innerHTML = '조회 실패';
    }
}

async function getBoardDetail() {
    const boardIdx = document.getElementById('boardIdx').value;
    if (!boardIdx) {
        document.getElementById('result').innerHTML = '게시글 번호를 입력하세요';
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/boards/${boardIdx}`);
        if (response.ok) {
            const result = await response.json();
            console.log('[게시글 상세] 성공:', `ID ${result.boardIdx} - ${result.boardTitle}`);
            const detailText = `게시글 상세 정보

ID: ${result.boardIdx}
제목: ${result.boardTitle}
작성자: ${result.boardWriter}
조회수: ${result.boardView}
작성일: ${result.boardReg}
수정일: ${result.boardLast}
코드: ${result.boardCode} (${['학교공지','학사공지','학과공지','교수공지'][result.boardCode]})

내용:
${result.boardContent}`;
            
            document.getElementById('result').innerHTML = detailText;
        } else if (response.status === 404) {
            console.log('[게시글 상세] 게시글을 찾을 수 없음');
            document.getElementById('result').innerHTML = '게시글을 찾을 수 없습니다';
        } else {
            console.log('[게시글 상세] 실패:', response.status, response.statusText);
            document.getElementById('result').innerHTML = '조회 실패';
        }
    } catch (error) {
        console.log('[게시글 상세] 실패:', error.message);
        document.getElementById('result').innerHTML = '조회 실패';
    }
}

function backToStats() {
    closeModal();
    boardStats();
}

function closeModal() {
    if (window.currentModal) {
        window.currentModal.remove();
        window.currentModal = null;
    }
}

window.boardStats = boardStats;