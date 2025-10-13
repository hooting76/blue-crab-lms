// ==================== API 테스터 메인 모듈 ====================

// 현재 서버의 베이스 URL을 자동으로 설정
const baseURL = window.location.origin + window.location.pathname.replace(/\/$/, '');

// API 템플릿 (JSON에서 로드됨)
let apiTemplates = {};

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', async function() {
    document.getElementById('serverUrl').textContent = baseURL;
    console.log('Base URL:', baseURL);

    // API 템플릿 로드
    await loadApiTemplates();

    // 로컬스토리지에서 토큰 로드
    loadTokensFromStorage();

    // 저장된 토큰 세트 및 히스토리 로드
    loadTokenSets();
    loadRequestHistory();
    if (typeof loadBodyTemplateList === 'function') {
        loadBodyTemplateList();
    }

    // 초기 인증 상태 및 엔드포인트 정보 업데이트
    updateAuthStatus();
    updateEndpointInfo();
});

// JSON에서 API 템플릿 로드
async function loadApiTemplates() {
    try {
        // baseURL을 사용하여 상대 경로 문제 해결
        // 예: http://server/BlueCrab-1.0.0 + /config/api-templates.json
        const configUrl = baseURL.replace(/\/$/, '') + '/config/api-templates.json';
        console.log('Loading API templates from:', configUrl);

        const response = await fetch(configUrl);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        // Content-Type 검증
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            const text = await response.text();
            console.error('Non-JSON response received:', text.substring(0, 200));
            throw new Error(`서버가 JSON이 아닌 응답을 반환했습니다. Content-Type: ${contentType || 'unknown'}`);
        }

        apiTemplates = await response.json();
        console.log('✅ API templates loaded successfully:', Object.keys(apiTemplates).length, 'endpoints');
        populateEndpointSelect();
    } catch (error) {
        console.error('❌ Failed to load API templates:', error);
        showResponse('API 템플릿 로드 실패: ' + error.message, 'error');

        // Fallback: 빈 선택 박스라도 표시
        populateEndpointSelect();
    }
}

// 엔드포인트 셀렉트 박스 동적 구성
function populateEndpointSelect() {
    const select = document.getElementById('testEndpoint');
    if (!select) {
        return;
    }

    const previousSelection = select.value;
    select.innerHTML = '';

    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = '-- 엔드포인트 선택 --';
    select.appendChild(placeholder);

    const categories = {};
    Object.entries(apiTemplates).forEach(([key, template]) => {
        const category = template.category || '기타';
        if (!categories[category]) {
            categories[category] = [];
        }
        categories[category].push({ key, template });
    });

    const sortedCategories = Object.keys(categories).sort((a, b) => a.localeCompare(b, 'ko'));
    sortedCategories.forEach(category => {
        const group = document.createElement('optgroup');
        group.label = category;

        categories[category]
            .sort((a, b) => a.template.name.localeCompare(b.template.name, 'ko'))
            .forEach(({ key, template }) => {
                const option = document.createElement('option');
                option.value = key;
                option.textContent = template.name;
                option.dataset.method = template.method;
                option.dataset.description = template.description || '';
                group.appendChild(option);
            });

        select.appendChild(group);
    });

    const customOption = document.createElement('option');
    customOption.value = 'custom';
    customOption.textContent = '커스텀 URL';
    select.appendChild(customOption);

    if (previousSelection && select.querySelector(`option[value="${previousSelection}"]`)) {
        select.value = previousSelection;
    } else {
        select.value = '';
    }
}

// 엔드포인트 정보 업데이트
function updateEndpointInfo() {
    const endpoint = document.getElementById('testEndpoint').value;
    const customUrlLabel = document.getElementById('customUrlLabel');
    const customUrl = document.getElementById('customUrl');
    const requestBody = document.getElementById('requestBody');
    const dynamicParams = document.getElementById('dynamicParams');
    const dynamicHeaders = document.getElementById('dynamicHeaders');
    const dynamicHeadersLabel = document.getElementById('dynamicHeadersLabel');
    const endpointSummary = document.getElementById('endpointSummary');

    // 커스텀 URL 표시/숨김
    if (endpoint === 'custom') {
        customUrlLabel.style.display = 'block';
        customUrl.style.display = 'block';
        dynamicParams.innerHTML = '';
        if (dynamicHeaders) {
            dynamicHeaders.innerHTML = '';
            dynamicHeaders.style.display = 'none';
        }
        if (endpointSummary) {
            endpointSummary.textContent = '커스텀 요청을 직접 구성하세요.';
        }
        return;
    } else {
        customUrlLabel.style.display = 'none';
        customUrl.style.display = 'none';
    }

    // API 템플릿이 있으면 적용
    const template = apiTemplates[endpoint];
    if (template) {
        document.getElementById('httpMethod').value = template.method;

        if (Object.prototype.hasOwnProperty.call(template, 'bodyTemplate')) {
            if (template.bodyTemplate) {
                requestBody.value = JSON.stringify(template.bodyTemplate, null, 2);
            } else {
                requestBody.value = '';
            }
        } else {
            requestBody.value = '';
        }

        if (typeof autoResizeRequestBody === 'function') {
            autoResizeRequestBody();
        }

        // 동적 파라미터 필드 생성
        dynamicParams.innerHTML = '';
        if (template.params && template.params.length > 0) {
            template.params.forEach(param => {
                const paramDiv = document.createElement('div');
                paramDiv.className = 'param-field';
                const locationLabel = param.location === 'path' ? '[PATH]' :
                    param.location === 'query' ? '[QUERY]' :
                    param.location === 'header' ? '[HEADER]' : '';
                const hint = param.description ? `<div class="param-hint">${param.description}</div>` : '';
                paramDiv.innerHTML = `
                    <label>
                        ${locationLabel} ${param.name}
                        ${param.required ? '<span style="color: red;">*</span>' : '(선택)'}
                    </label>
                    <input
                        type="${param.type === 'number' ? 'number' : 'text'}"
                        id="param_${param.name}"
                        placeholder="${param.example || ''}"
                        value="${param.example || ''}"
                    >
                    ${hint}
                `;
                dynamicParams.appendChild(paramDiv);
            });
        }

        // 동적 헤더 필드 생성
        if (dynamicHeaders) {
            dynamicHeaders.innerHTML = '';
            if (template.headers && template.headers.length > 0) {
                template.headers.forEach(header => {
                    const headerDiv = document.createElement('div');
                    headerDiv.className = 'param-field';
                    const hint = header.description ? `<div class="param-hint">${header.description}</div>` : '';
                    headerDiv.innerHTML = `
                        <label>
                            ${header.name}
                            ${header.required ? '<span style="color: red;">*</span>' : '(선택)'}
                        </label>
                        <input
                            type="text"
                            id="header_${header.name}"
                            placeholder="${header.example || ''}"
                            value="${header.example || ''}"
                        >
                        ${hint}
                    `;
                    dynamicHeaders.appendChild(headerDiv);
                });
                dynamicHeaders.style.display = 'block';
                if (dynamicHeadersLabel) {
                    dynamicHeadersLabel.style.display = 'block';
                }
            } else {
                dynamicHeaders.style.display = 'none';
                if (dynamicHeadersLabel) {
                    dynamicHeadersLabel.style.display = 'none';
                }
            }
        }

        if (endpointSummary) {
            const requiresAuth = template.auth ? '🔐 인증 필요' : '🔓 인증 불필요';
            const methodLabel = template.method || 'GET';
            const description = template.description || '';
            endpointSummary.textContent = `${requiresAuth} · ${methodLabel} ${template.endpoint} ${description ? '· ' + description : ''}`;
        }
    } else {
        if (dynamicHeaders) {
            dynamicHeaders.innerHTML = '';
            dynamicHeaders.style.display = 'none';
        }
        if (dynamicHeadersLabel) {
            dynamicHeadersLabel.style.display = 'none';
        }
        if (endpointSummary) {
            endpointSummary.textContent = '';
        }
    }
}

// ==================== 로그인 및 인증 ====================

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
        const data = parsed.isJson ? parsed.body : null;

        if (response.ok && data && data.success) {
            const tokenPayload = extractTokenPayload(data);
            if (tokenPayload && tokenPayload.accessToken) {
                accessToken = tokenPayload.accessToken;
                refreshToken = tokenPayload.refreshToken || '';

                console.log('로그인 응답 데이터:', data);
                console.log('Access Token:', accessToken);
                console.log('Refresh Token:', refreshToken);

                saveTokensToStorage();
                updateTokenDisplay();
                updateAuthStatus();
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
        const data = parsed.isJson ? parsed.body : null;

        if (response.ok && data && data.success) {
            const tokenPayload = extractTokenPayload(data);
            if (tokenPayload && tokenPayload.accessToken) {
                accessToken = tokenPayload.accessToken;
                if (tokenPayload.refreshToken) {
                    refreshToken = tokenPayload.refreshToken;
                }

                saveTokensToStorage();
                updateTokenDisplay();
                updateAuthStatus();
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
    updateAuthStatus();
    showResponse('로그아웃되었습니다.', 'success');
}

// ==================== 관리자 인증 ====================

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
            const authData = data.data || {};
            accessToken = authData.accessToken || authData.token || '';
            refreshToken = authData.refreshToken || '';

            console.log('관리자 로그인 성공:', { accessToken, refreshToken, admin: authData.admin });

            if (accessToken) {
                saveTokensToStorage();
                updateTokenDisplay();
                updateAuthStatus();

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

// ==================== API 요청 ====================

// API 요청 함수
async function sendRequest() {
    const endpoint = document.getElementById('testEndpoint').value;
    const method = document.getElementById('httpMethod').value;
    const requestBody = document.getElementById('requestBody').value;
    const startTime = Date.now();
    const template = apiTemplates[endpoint];

    if (!endpoint) {
        showResponse('먼저 테스트할 엔드포인트를 선택해주세요.', 'error');
        return;
    }

    let url;
    let urlPath;
    if (endpoint === 'custom') {
        const customUrl = document.getElementById('customUrl').value;
        if (!customUrl) {
            showResponse('커스텀 URL을 입력해주세요.', 'error');
            return;
        }
        url = `${baseURL}${customUrl}`;
        urlPath = customUrl;
    } else {
        if (template) {
            urlPath = template.endpoint;

            // 파라미터 처리 (path, query)
            if (template.params && template.params.length > 0) {
                const queryParams = [];

                for (const param of template.params) {
                    const input = document.getElementById(`param_${param.name}`);
                    const rawValue = input ? input.value.trim() : '';

                    if (!rawValue) {
                        if (param.required) {
                            showResponse(`필수 파라미터 "${param.name}"를 입력해주세요.`, 'error');
                            return;
                        }
                        continue;
                    }

                    if ((param.location || 'query') === 'path') {
                        if (!urlPath.includes(`{${param.name}}`)) {
                            showResponse(`경로에 {${param.name}} 플레이스홀더가 없습니다.`, 'error');
                            return;
                        }
                        urlPath = urlPath.replace(`{${param.name}}`, encodeURIComponent(rawValue));
                    } else {
                        queryParams.push(`${param.name}=${encodeURIComponent(rawValue)}`);
                    }
                }

                if (queryParams.length > 0) {
                    urlPath += (urlPath.includes('?') ? '&' : '?') + queryParams.join('&');
                }
            }
            url = `${baseURL}${urlPath}`;
        }
    }

    const headers = {};

    if (method !== 'GET') {
        headers['Content-Type'] = 'application/json';
    }

    // 인증이 필요한 엔드포인트에 토큰 추가
    if (template && template.auth) {
        if (!accessToken) {
            showResponse('이 엔드포인트는 인증이 필요합니다. 먼저 로그인해주세요.', 'error');
            return;
        }
        headers['Authorization'] = `Bearer ${accessToken}`;
    }

    if (template && template.headers && template.headers.length > 0) {
        for (const header of template.headers) {
            const input = document.getElementById(`header_${header.name}`);
            const value = input ? input.value.trim() : '';
            if (!value && header.required) {
                showResponse(`필수 헤더 "${header.name}"를 입력해주세요.`, 'error');
                return;
            }
            if (value) {
                headers[header.name] = value;
            }
        }
    }

    const requestOptions = {
        method: method,
        headers: headers,
    };

    if (method !== 'GET' && requestBody.trim()) {
        try {
            JSON.parse(requestBody);
            requestOptions.body = requestBody;
        } catch (e) {
            showResponse('요청 본문이 유효한 JSON이 아닙니다.', 'error');
            return;
        }
    }

    try {
        showResponse(`요청 전송 중...\nURL: ${url}\nMethod: ${method}\nHeaders: ${JSON.stringify(headers, null, 2)}`, 'info');

        const response = await fetch(url, requestOptions);
        const parsed = await parseResponseBody(response);
        const duration = Date.now() - startTime;

        const statusClass = response.ok ? 'success' : 'error';
        const responseText = `${formatResponseDisplay(response, parsed)}\n응답시간: ${duration}ms`;

        showResponse(responseText, statusClass);

        // 히스토리에 저장
        saveToHistory({
            method: method,
            endpoint: urlPath || url,
            body: requestBody || null,
            status: response.status,
            duration: duration,
            success: response.ok
        });

    } catch (error) {
        const duration = Date.now() - startTime;
        showResponse(`네트워크 오류: ${error.message}`, 'error');

        // 에러도 히스토리에 저장
        saveToHistory({
            method: method,
            endpoint: urlPath || url,
            body: requestBody || null,
            status: 0,
            duration: duration,
            success: false
        });
    }
}

// ==================== 인증 상태 업데이트 ====================

// 인증 상태 배너 업데이트
function updateAuthStatus() {
    const banner = document.getElementById('authStatusBanner');
    const icon = document.getElementById('authStatusIcon');
    const text = document.getElementById('authStatusText');

    const accessToken = localStorage.getItem('bluecrab_access_token');

    // 토큰 제거 시 기본 클래스만 남김
    banner.classList.remove('authenticated', 'expired');

    if (!accessToken) {
        icon.textContent = '🔓';
        text.textContent = '로그인 필요 (토큰 없음)';
        return;
    }

    try {
        // JWT 토큰 파싱 (만료 시간 확인)
        const payload = JSON.parse(atob(accessToken.split('.')[1]));
        const expiryTime = payload.exp * 1000; // 초를 밀리초로 변환
        const now = Date.now();

        if (expiryTime < now) {
            // 만료됨
            banner.classList.add('expired');
            icon.textContent = '⏰';
            text.textContent = '토큰 만료됨 (재로그인 필요)';
        } else {
            // 유효함
            banner.classList.add('authenticated');
            icon.textContent = '🔐';
            const username = payload.sub || '사용자';
            text.textContent = `로그인됨 (${username})`;
        }
    } catch (e) {
        // 파싱 실패 시
        banner.classList.add('expired');
        icon.textContent = '❌';
        text.textContent = '잘못된 토큰 형식';
    }
}
