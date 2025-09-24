/**
 * 로그인 및 토큰 획득 (초심플 모달)
 */

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

function loginTest() {
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:white;padding:20px;border:2px solid #ccc;z-index:9999">
            <h3>로그인</h3>
            <input type="text" id="username" placeholder="이메일" style="width:250px;padding:5px"><br><br>
            <input type="password" id="password" placeholder="비밀번호" style="width:250px;padding:5px"><br><br>
            <button onclick="doLogin()">로그인</button>
            <button onclick="closeModal()">닫기</button>
            <div id="result" style="margin-top:10px;font-weight:bold"></div>
        </div>
        <div style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9998" onclick="closeModal()"></div>
    `;
    
    document.body.appendChild(modal);
    window.currentModal = modal;
}

async function doLogin() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();
    
    if (!username || !password) {
        document.getElementById('result').innerHTML = '<span style="color:red">이메일과 비밀번호 입력하세요</span>';
        return;
    }
    
    const loginData = {
        username: username,
        password: password
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(loginData)
        });
        
        console.log('[로그인] 서버 응답:', response.status, response.statusText);
        const result = await response.json();
        
        if (response.ok && result.data && result.data.accessToken) {
            window._accessToken = result.data.accessToken;
            window._refreshToken = result.data.refreshToken;
            window._currentUser = result.data.user;
            
            console.log('[로그인] 성공 (200 OK)');
            console.log('사용자:', result.data.user.userName || username);
            console.log('토큰 길이:', result.data.accessToken.length, '문자');
            closeModal();
        } else if (response.status === 401) {
            console.log('[로그인] 인증 실패 (401):', '잘못된 이메일 또는 비밀번호');
            document.getElementById('result').innerHTML = '<span style="color:red">인증 실패 - 이메일/비밀번호 확인</span>';
        } else if (response.status === 400) {
            console.log('[로그인] 요청 오류 (400):', result.message || '잘못된 요청');
            document.getElementById('result').innerHTML = '<span style="color:red">요청 오류</span>';
        } else {
            console.log('[로그인] 실패 (' + response.status + '):', result.message || '알 수 없는 오류');
            document.getElementById('result').innerHTML = '<span style="color:red">실패 (' + response.status + ')</span>';
        }
    } catch (error) {
        console.log('[로그인] 네트워크 오류:', error.message);
        document.getElementById('result').innerHTML = '<span style="color:red">네트워크 오류</span>';
    }
}

function closeModal() {
    if (window.currentModal) {
        window.currentModal.remove();
        window.currentModal = null;
    }
}

window.loginTest = loginTest;