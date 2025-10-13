// ==================== Standalone API 테스트 페이지 스크립트 ====================

// 현재 서버의 베이스 URL을 자동으로 설정
const baseURL = window.location.origin + window.location.pathname.replace('/api-test.html', '');
let accessToken = '';
let refreshToken = '';

// 페이지 로드 시 서버 URL 표시 및 저장된 토큰 로드
document.addEventListener('DOMContentLoaded', function() {
    const serverUrlEl = document.getElementById('serverUrl');
    if (serverUrlEl) {
        serverUrlEl.textContent = baseURL;
    }
    console.log('Base URL:', baseURL);

    loadTokensFromStorage();
});

// 엔드포인트 정보 업데이트
function updateEndpointInfo() {
    const endpoint = document.getElementById('testEndpoint').value;
    const customUrlLabel = document.getElementById('customUrlLabel');
    const customUrl = document.getElementById('customUrl');
    const requestBody = document.getElementById('requestBody');

    if (endpoint === 'custom') {
        customUrlLabel.style.display = 'block';
        customUrl.style.display = 'block';
    } else {
        customUrlLabel.style.display = 'none';
        customUrl.style.display = 'none';
    }

    // 엔드포인트별 예시 설정
    switch(endpoint) {
        case 'profile':
        case 'users':
            requestBody.value = '';
            document.getElementById('httpMethod').value = 'GET';
            break;
        case 'facilityList':
        case 'facilityAvailability':
        case 'reservationMy':
        case 'reservationMyStatus':
            requestBody.value = '';
            document.getElementById('httpMethod').value = 'POST';
            break;
        case 'reservationCreate':
            document.getElementById('httpMethod').value = 'POST';
            requestBody.value = JSON.stringify({
                facilityIdx: 1,
                startTime: '2025-01-08 10:00:00',
                endTime: '2025-01-08 12:00:00',
                partySize: 4,
                purpose: '스터디 모임',
                requestedEquipment: '빔프로젝터'
            }, null, 2);
            break;
        case 'reservationCancel':
            requestBody.value = '';
            document.getElementById('httpMethod').value = 'DELETE';
            break;
        default:
            requestBody.value = '';
    }
}

// 로그인 함수
async function login() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    if (!username || !password) {
        showResponse('사용자명과 비밀번호를 입력해주세요.', 'error');
        return;
    }

    try {
        const response = await fetch(`${baseURL}/api/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                username: username,
                password: password
            })
        });

        const parsed = await parseResponseBody(response);

        if (response.ok && parsed.isJson) {
            const tokenPayload = extractTokenPayload(parsed.body);

            if (tokenPayload && tokenPayload.accessToken) {
                accessToken = tokenPayload.accessToken;
                refreshToken = tokenPayload.refreshToken || '';

                saveTokensToStorage();
                updateTokenDisplay();
                showResponse(formatResponseDisplay(response, parsed), 'success');
            } else {
                showResponse('로그인 응답에서 토큰을 찾을 수 없습니다.', 'error');
            }
        } else {
            showResponse(formatResponseDisplay(response, parsed), 'error');
        }
    } catch (error) {
        showResponse(`네트워크 오류: ${error.message}`, 'error');
    }
}

// 토큰 갱신 함수
async function refreshAccessToken() {
    if (!refreshToken) {
        showResponse('Refresh token이 없습니다. 먼저 로그인해주세요.', 'error');
        return;
    }

    try {
        const response = await fetch(`${baseURL}/api/auth/refresh`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                refreshToken: refreshToken
            })
        });

        const parsed = await parseResponseBody(response);

        if (response.ok && parsed.isJson) {
            const tokenPayload = extractTokenPayload(parsed.body);
            if (tokenPayload && tokenPayload.accessToken) {
                accessToken = tokenPayload.accessToken;
                if (tokenPayload.refreshToken) {
                    refreshToken = tokenPayload.refreshToken;
                }

                saveTokensToStorage();
                updateTokenDisplay();
                showResponse(formatResponseDisplay(response, parsed), 'success');
            } else {
                showResponse('토큰 갱신 응답에서 토큰을 찾을 수 없습니다.', 'error');
            }
        } else {
            showResponse(formatResponseDisplay(response, parsed), 'error');
        }
    } catch (error) {
        showResponse(`네트워크 오류: ${error.message}`, 'error');
    }
}

// 로그아웃 함수
function logout() {
    accessToken = '';
    refreshToken = '';

    clearTokensFromStorage();
    updateTokenDisplay();
    showResponse('로그아웃되었습니다.', 'success');
}

// API 요청 함수
async function sendRequest() {
    const endpoint = document.getElementById('testEndpoint').value;
    const method = document.getElementById('httpMethod').value;
    const requestBody = document.getElementById('requestBody').value;

    let url;
    if (endpoint === 'custom') {
        const customUrl = document.getElementById('customUrl').value;
        if (!customUrl) {
            showResponse('커스텀 URL을 입력해주세요.', 'error');
            return;
        }
        url = `${baseURL}${customUrl}`;
    } else {
        const endpoints = {
            'ping': '/api/ping',
            'health': '/api/health',
            'profile': '/api/user/profile',
            'users': '/api/user/list',
            'facilityList': '/api/facilities',
            'reservationCreate': '/api/reservations',
            'reservationMy': '/api/reservations/my'
        };

        switch (endpoint) {
            case 'facilityAvailability': {
                const facilityIdx = prompt('시설 ID를 입력하세요.', '1');
                if (!facilityIdx) {
                    showResponse('시설 ID가 필요합니다.', 'error');
                    return;
                }
                const startTime = prompt('시작 시간을 입력하세요. (yyyy-MM-dd HH:mm:ss)', '2025-01-08 10:00:00');
                if (!startTime) {
                    showResponse('시작 시간이 필요합니다.', 'error');
                    return;
                }
                const endTime = prompt('종료 시간을 입력하세요. (yyyy-MM-dd HH:mm:ss)', '2025-01-08 12:00:00');
                if (!endTime) {
                    showResponse('종료 시간이 필요합니다.', 'error');
                    return;
                }
                url = `${baseURL}/api/facilities/${encodeURIComponent(facilityIdx)}/availability?startTime=${encodeURIComponent(startTime)}&endTime=${encodeURIComponent(endTime)}`;
                break;
            }
            case 'reservationMyStatus': {
                const status = prompt('조회할 예약 상태를 입력하세요. (예: PENDING, APPROVED)', 'PENDING');
                if (!status) {
                    showResponse('예약 상태가 필요합니다.', 'error');
                    return;
                }
                url = `${baseURL}/api/reservations/my/status/${encodeURIComponent(status)}`;
                break;
            }
            case 'reservationCancel': {
                const reservationIdx = prompt('취소할 예약 ID를 입력하세요.');
                if (!reservationIdx) {
                    showResponse('예약 ID가 필요합니다.', 'error');
                    return;
                }
                url = `${baseURL}/api/reservations/${encodeURIComponent(reservationIdx)}`;
                break;
            }
            default: {
                if (!endpoints[endpoint]) {
                    showResponse('지원하지 않는 엔드포인트입니다.', 'error');
                    return;
                }
                url = `${baseURL}${endpoints[endpoint]}`;
                break;
            }
        }
    }

    const headers = {
        'Content-Type': 'application/json',
    };

    // 인증이 필요한 엔드포인트에 토큰 추가
    if (endpoint !== 'ping' && endpoint !== 'health' && accessToken) {
        headers['Authorization'] = `Bearer ${accessToken}`;
    }

    const requestOptions = {
        method: method,
        headers: headers,
    };

    if (method !== 'GET' && requestBody.trim()) {
        try {
            JSON.parse(requestBody); // JSON 유효성 검사
            requestOptions.body = requestBody;
        } catch (e) {
            showResponse('요청 본문이 유효한 JSON이 아닙니다.', 'error');
            return;
        }
    }

    try {
        showResponse(`요청 전송 중...\nURL: ${url}\nMethod: ${method}`, 'info');

        const response = await fetch(url, requestOptions);
        const parsed = await parseResponseBody(response);

        const statusClass = response.ok ? 'success' : 'error';
        showResponse(formatResponseDisplay(response, parsed), statusClass);
    } catch (error) {
        showResponse(`네트워크 오류: ${error.message}`, 'error');
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

        // 토큰 일부만 표시
        accessTokenDisplay.textContent = accessToken.substring(0, 50) + '...';
        refreshTokenDisplay.textContent = refreshToken ? refreshToken.substring(0, 50) + '...' : '없음';

        // JWT 토큰 디코딩해서 만료 시간 표시
        try {
            const decodedToken = decodeJWT(accessToken);
            const expiry = new Date(decodedToken.exp * 1000);
            tokenExpiry.textContent = expiry.toLocaleString();

            // 디코딩된 토큰 내용 표시
            accessTokenParsed.textContent = JSON.stringify(decodedToken, null, 2);
        } catch (e) {
            tokenExpiry.textContent = '파싱 실패';
            accessTokenParsed.textContent = '토큰 디코딩 실패: ' + e.message;
        }
    } else {
        tokenInfo.style.display = 'none';
    }
}

// JWT 토큰 디코딩 함수
function decodeJWT(token) {
    const parts = token.split('.');
    if (parts.length !== 3) {
        throw new Error('유효하지 않은 JWT 형식입니다.');
    }

    const payload = parts[1];
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const paddedBase64 = base64 + '='.repeat((4 - base64.length % 4) % 4);

    const decodedPayload = JSON.parse(atob(paddedBase64));
    return decodedPayload;
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
        if (accessToken && accessToken !== 'undefined') {
            localStorage.setItem('bluecrab_access_token', accessToken);
        }

        if (refreshToken && refreshToken !== 'undefined') {
            localStorage.setItem('bluecrab_refresh_token', refreshToken);
        }
    } catch (error) {
        console.error('토큰 저장 실패:', error);
    }
}

// 로컬스토리지에서 토큰 로드
function loadTokensFromStorage() {
    try {
        const savedAccessToken = localStorage.getItem('bluecrab_access_token');
        const savedRefreshToken = localStorage.getItem('bluecrab_refresh_token');

        if (savedAccessToken && savedAccessToken !== 'undefined' && savedAccessToken !== 'null' &&
            savedRefreshToken && savedRefreshToken !== 'undefined' && savedRefreshToken !== 'null') {
            try {
                const decodedToken = decodeJWT(savedAccessToken);
                const currentTime = Math.floor(Date.now() / 1000);

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

// 관리자 인증코드 요청 (Step 1)
async function adminRequestAuthCode() {
    const tempToken = document.getElementById('adminTempToken').value.trim();
    const step2Section = document.getElementById('adminStep2Section');

    if (!tempToken) {
        showAdminStatus('임시 토큰을 입력해주세요.', 'error');
        return;
    }

    try {
        showAdminStatus('인증코드 요청 중...', 'info');

        const response = await fetch(`${baseURL}/api/admin/email-auth/request`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${tempToken}`
            }
        });

        const parsed = await parseResponseBody(response);
        const data = parsed.isJson ? parsed.body : null;

        if (response.ok && data && data.success) {
            showAdminStatus('✅ 인증코드가 이메일로 전송되었습니다. Step 2로 진행하세요.', 'success');
            step2Section.style.display = 'block';
            showResponse(formatResponseDisplay(response, parsed), 'success');
        } else {
            const message = data && data.message ? data.message : '알 수 없는 오류';
            showAdminStatus('❌ 인증코드 요청 실패: ' + message, 'error');
            step2Section.style.display = 'none';
            showResponse(formatResponseDisplay(response, parsed), 'error');
        }
    } catch (error) {
        showAdminStatus('❌ 네트워크 오류: ' + error.message, 'error');
        step2Section.style.display = 'none';
        showResponse(`네트워크 오류: ${error.message}`, 'error');
    }
}

// 관리자 인증코드 검증 (Step 2)
async function adminVerifyAuthCode() {
    const tempToken = document.getElementById('adminTempToken').value.trim();
    const authCode = document.getElementById('adminAuthCode').value.trim().toUpperCase();

    if (!tempToken) {
        showAdminStatus('임시 토큰을 입력해주세요.', 'error');
        return;
    }

    if (!authCode || authCode.length !== 6) {
        showAdminStatus('6자리 인증코드를 입력해주세요.', 'error');
        return;
    }

    try {
        showAdminStatus('인증코드 검증 중...', 'info');

        const response = await fetch(`${baseURL}/api/admin/email-auth/verify`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${tempToken}`
            },
            body: JSON.stringify({
                authCode: authCode
            })
        });

        const parsed = await parseResponseBody(response);
        const data = parsed.isJson ? parsed.body : null;

        if (response.ok && data && data.success) {
            const authData = data.data;

            if (authData && (authData.accessToken || authData.token)) {
                accessToken = authData.accessToken || authData.token || '';
                refreshToken = authData.refreshToken || '';
            } else {
                accessToken = '';
                refreshToken = '';
            }

            if (accessToken) {
                saveTokensToStorage();
                updateTokenDisplay();

                showAdminStatus('✅ 관리자 인증 성공! 토큰이 저장되었습니다.', 'success');
                showResponse(formatResponseDisplay(response, parsed), 'success');

                document.getElementById('adminTempToken').value = '';
                document.getElementById('adminAuthCode').value = '';
                document.getElementById('adminStep2Section').style.display = 'none';
            } else {
                showAdminStatus('⚠️ 토큰을 찾을 수 없습니다.', 'error');
                showResponse(formatResponseDisplay(response, parsed), 'error');
            }
        } else {
            const message = data && data.message ? data.message : '알 수 없는 오류';
            showAdminStatus('❌ 인증코드 검증 실패: ' + message, 'error');
            showResponse(formatResponseDisplay(response, parsed), 'error');
        }
    } catch (error) {
        showAdminStatus('❌ 네트워크 오류: ' + error.message, 'error');
        showResponse(`네트워크 오류: ${error.message}`, 'error');
    }
}

// 초기 엔드포인트 정보 업데이트
updateEndpointInfo();
