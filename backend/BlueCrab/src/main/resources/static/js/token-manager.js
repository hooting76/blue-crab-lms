// ==================== 토큰 관리 모듈 ====================

let accessToken = '';
let refreshToken = '';

// JWT 토큰 디코딩 함수
function decodeJWT(token) {
    try {
        const parts = token.split('.');
        if (parts.length !== 3) {
            throw new Error('유효하지 않은 JWT 형식입니다.');
        }

        const payload = parts[1];
        const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
        const paddedBase64 = base64 + '='.repeat((4 - base64.length % 4) % 4);
        const decodedPayload = JSON.parse(atob(paddedBase64));

        return decodedPayload;
    } catch (error) {
        throw new Error('JWT 디코딩 실패: ' + error.message);
    }
}

// 토큰 표시 업데이트
function updateTokenDisplay() {
    const tokenInfo = document.getElementById('tokenInfo');
    const accessTokenDisplay = document.getElementById('accessTokenDisplay');
    const refreshTokenDisplay = document.getElementById('refreshTokenDisplay');
    const tokenExpiry = document.getElementById('tokenExpiry');
    const accessTokenParsed = document.getElementById('accessTokenParsed');

    if (accessToken) {
        tokenInfo.style.display = 'block';
        tokenInfo.className = 'token-info';

        accessTokenDisplay.textContent = accessToken.substring(0, 50) + '...';
        refreshTokenDisplay.textContent = refreshToken ? refreshToken.substring(0, 50) + '...' : '없음';

        try {
            const decodedToken = decodeJWT(accessToken);
            const expiry = new Date(decodedToken.exp * 1000);
            tokenExpiry.textContent = expiry.toLocaleString();

            accessTokenParsed.textContent = JSON.stringify(decodedToken, null, 2);
        } catch (e) {
            tokenExpiry.textContent = '파싱 실패';
            accessTokenParsed.textContent = '토큰 디코딩 실패: ' + e.message;
        }
    } else {
        tokenInfo.style.display = 'none';
    }
}

// 토큰 내용 보기/숨기기 토글
function toggleTokenDecoded() {
    const tokenDecoded = document.getElementById('tokenDecoded');
    if (tokenDecoded.style.display === 'none') {
        tokenDecoded.style.display = 'block';
    } else {
        tokenDecoded.style.display = 'none';
    }
}

// 로컬스토리지에 토큰 저장
function saveTokensToStorage() {
    try {
        console.log('토큰 저장 시도:', { accessToken, refreshToken });

        if (accessToken && accessToken !== 'undefined') {
            localStorage.setItem('bluecrab_access_token', accessToken);
            console.log('Access Token 저장 완료');
        }

        if (refreshToken && refreshToken !== 'undefined') {
            localStorage.setItem('bluecrab_refresh_token', refreshToken);
            console.log('Refresh Token 저장 완료');
        }

        console.log('토큰이 로컬스토리지에 저장되었습니다.');
    } catch (error) {
        console.error('토큰 저장 실패:', error);
    }
}

// 로컬스토리지에서 토큰 로드
function loadTokensFromStorage() {
    try {
        const savedAccessToken = localStorage.getItem('bluecrab_access_token');
        const savedRefreshToken = localStorage.getItem('bluecrab_refresh_token');

        console.log('저장된 토큰 로드 시도:', { savedAccessToken, savedRefreshToken });

        if (savedAccessToken && savedAccessToken !== 'undefined' && savedAccessToken !== 'null' &&
            savedRefreshToken && savedRefreshToken !== 'undefined' && savedRefreshToken !== 'null') {
            try {
                const decodedToken = decodeJWT(savedAccessToken);
                const currentTime = Math.floor(Date.now() / 1000);

                console.log('토큰 만료 확인:', {
                    exp: decodedToken.exp,
                    current: currentTime,
                    isValid: decodedToken.exp > currentTime
                });

                if (decodedToken.exp > currentTime) {
                    accessToken = savedAccessToken;
                    refreshToken = savedRefreshToken;
                    updateTokenDisplay();
                    showResponse('저장된 토큰을 로드했습니다. (유효함)', 'success');
                } else {
                    clearTokensFromStorage();
                    showResponse('저장된 토큰이 만료되어 제거했습니다.', 'warning');
                }
            } catch (error) {
                console.error('토큰 파싱 실패:', error);
                clearTokensFromStorage();
                showResponse('저장된 토큰이 유효하지 않아 제거했습니다.', 'warning');
            }
        } else {
            console.log('저장된 토큰이 없거나 유효하지 않음');
        }
    } catch (error) {
        console.error('토큰 로드 실패:', error);
    }
}

// 로컬스토리지에서 토큰 제거
function clearTokensFromStorage() {
    try {
        localStorage.removeItem('bluecrab_access_token');
        localStorage.removeItem('bluecrab_refresh_token');
        console.log('토큰이 로컬스토리지에서 제거되었습니다.');
    } catch (error) {
        console.error('토큰 제거 실패:', error);
    }
}

// ==================== 토큰 세트 관리 ====================

// 토큰 세트 저장
function saveTokenSet() {
    const name = document.getElementById('tokenSetName').value.trim();
    if (!name) {
        alert('토큰 세트 이름을 입력해주세요.');
        return;
    }

    if (!accessToken || !refreshToken) {
        alert('저장할 토큰이 없습니다. 먼저 로그인해주세요.');
        return;
    }

    const tokenSets = JSON.parse(localStorage.getItem('bluecrab_token_sets') || '[]');

    const newSet = {
        id: Date.now().toString(),
        name: name,
        accessToken: accessToken,
        refreshToken: refreshToken,
        savedAt: new Date().toISOString()
    };

    tokenSets.push(newSet);
    localStorage.setItem('bluecrab_token_sets', JSON.stringify(tokenSets));

    document.getElementById('tokenSetName').value = '';
    loadTokenSets();

    alert(`토큰 세트 "${name}" 저장 완료!`);
}

// 저장된 토큰 세트 목록 로드
function loadTokenSets() {
    const tokenSets = JSON.parse(localStorage.getItem('bluecrab_token_sets') || '[]');
    const container = document.getElementById('tokenSetsList');

    if (tokenSets.length === 0) {
        container.innerHTML = '<div class="storage-empty">저장된 토큰 세트가 없습니다</div>';
        return;
    }

    container.innerHTML = tokenSets.map(set => `
        <div class="token-set-item">
            <div class="token-set-name">${set.name}</div>
            <div class="token-set-time">${new Date(set.savedAt).toLocaleString()}</div>
            <div class="storage-actions">
                <button onclick="loadTokenSet('${set.id}')" class="success">불러오기</button>
                <button onclick="deleteTokenSet('${set.id}')" class="danger">삭제</button>
            </div>
        </div>
    `).join('');
}

// 토큰 세트 불러오기
function loadTokenSet(id) {
    const tokenSets = JSON.parse(localStorage.getItem('bluecrab_token_sets') || '[]');
    const tokenSet = tokenSets.find(s => s.id === id);

    if (!tokenSet) {
        alert('토큰 세트를 찾을 수 없습니다.');
        return;
    }

    accessToken = tokenSet.accessToken;
    refreshToken = tokenSet.refreshToken;

    saveTokensToStorage();
    updateTokenDisplay();
    updateAuthStatus();

    showResponse(`토큰 세트 "${tokenSet.name}" 불러오기 완료!`, 'success');
}

// 토큰 세트 삭제
function deleteTokenSet(id) {
    if (!confirm('정말 이 토큰 세트를 삭제하시겠습니까?')) {
        return;
    }

    let tokenSets = JSON.parse(localStorage.getItem('bluecrab_token_sets') || '[]');
    tokenSets = tokenSets.filter(s => s.id !== id);
    localStorage.setItem('bluecrab_token_sets', JSON.stringify(tokenSets));

    loadTokenSets();
}
