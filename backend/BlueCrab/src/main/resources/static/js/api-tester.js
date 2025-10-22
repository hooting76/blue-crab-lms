// ==================== API 테스터 메인 모듈 ====================

// 현재 서버의 베이스 URL을 자동으로 설정
// pathname에서 /status, /log-monitor 등의 페이지 경로를 제거하고 컨텍스트 경로만 추출
const baseURL = (() => {
    const pathname = window.location.pathname;
    // /BlueCrab-1.0.0/status → /BlueCrab-1.0.0
    // /status → '' (빈 문자열, 컨텍스트 경로 없음)
    const knownPages = ['status', 'log-monitor'];
    const parts = pathname.split('/').filter(p => p);

    // 마지막 부분이 알려진 페이지 이름이면 제거
    if (parts.length > 0 && knownPages.includes(parts[parts.length - 1])) {
        parts.pop();
    }

    const contextPath = parts.length > 0 ? '/' + parts.join('/') : '';
    return window.location.origin + contextPath;
})();

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
    const requestBodyLabel = document.getElementById('requestBodyLabel');
    const formDataContainer = document.getElementById('formDataContainer');
    const formDataFields = document.getElementById('formDataFields');
    const formDataNote = document.getElementById('formDataNote');

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

        const hasFormData = template.bodyTemplate
            && typeof template.bodyTemplate === 'object'
            && template.bodyTemplate._formData;

        if (hasFormData && formDataContainer && formDataFields) {
            const formDataTemplate = template.bodyTemplate._formData;
            const noteText = template.bodyTemplate._note || 'multipart/form-data 요청을 사용합니다.';

            formDataContainer.style.display = 'block';
            formDataFields.innerHTML = '';
            if (formDataNote) {
                formDataNote.textContent = noteText;
                formDataNote.style.display = noteText ? 'block' : 'none';
            }

            Object.entries(formDataTemplate).forEach(([field, config]) => {
                const fieldWrapper = document.createElement('div');
                fieldWrapper.className = 'form-data-field';

                const label = document.createElement('label');
                const fieldId = `formData_${field}`;
                let fieldType = 'text';
                let placeholder = '';
                let required = true;
                let defaultValue = '';

                if (typeof config === 'string') {
                    placeholder = config;
                    const lowered = config.toLowerCase();
                    if (lowered.includes('파일')) {
                        fieldType = 'file';
                    } else if (lowered.includes('숫자')) {
                        fieldType = 'number';
                    }
                } else if (config && typeof config === 'object') {
                    fieldType = config.type || 'text';
                    placeholder = config.placeholder || '';
                    required = config.required !== false;
                    if (Object.prototype.hasOwnProperty.call(config, 'value')) {
                        defaultValue = config.value;
                    }
                }

                if (field.toLowerCase().includes('file')) {
                    fieldType = 'file';
                }

                label.setAttribute('for', fieldId);
                label.textContent = required ? `${field} *` : `${field} (선택)`;

                const input = document.createElement('input');
                input.id = fieldId;
                input.dataset.required = required ? 'true' : 'false';

                if (fieldType === 'file') {
                    input.type = 'file';
                } else if (fieldType === 'number') {
                    input.type = 'number';
                    if (placeholder) {
                        input.placeholder = placeholder;
                    }
                    if (defaultValue !== '') {
                        input.value = defaultValue;
                    }
                } else {
                    input.type = 'text';
                    if (placeholder) {
                        input.placeholder = placeholder;
                    }
                    if (defaultValue !== '') {
                        input.value = defaultValue;
                    }
                }

                if (required) {
                    input.required = true;
                }

                fieldWrapper.appendChild(label);
                fieldWrapper.appendChild(input);
                formDataFields.appendChild(fieldWrapper);
            });

            if (requestBodyLabel) {
                requestBodyLabel.style.display = 'none';
            }
            requestBody.style.display = 'none';
            requestBody.value = '';
        } else {
            if (formDataContainer && formDataFields) {
                formDataContainer.style.display = 'none';
                formDataFields.innerHTML = '';
                if (formDataNote) {
                    formDataNote.textContent = '';
                    formDataNote.style.display = 'none';
                }
            }
            if (requestBodyLabel) {
                requestBodyLabel.style.display = 'block';
            }
            requestBody.style.display = 'block';

            if (Object.prototype.hasOwnProperty.call(template, 'bodyTemplate')) {
                if (template.bodyTemplate) {
                    if (typeof template.bodyTemplate === 'object' && !Array.isArray(template.bodyTemplate)) {
                        const { _note, _formData, ...rest } = template.bodyTemplate;
                        const keys = Object.keys(rest);
                        requestBody.value = keys.length > 0
                            ? JSON.stringify(rest, null, 2)
                            : '';
                    } else {
                        requestBody.value = JSON.stringify(template.bodyTemplate, null, 2);
                    }
                } else {
                    requestBody.value = '';
                }
            } else {
                requestBody.value = '';
            }

            if (typeof autoResizeRequestBody === 'function') {
                autoResizeRequestBody();
            }
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
            const payloadLabel = hasFormData ? ' · multipart/form-data' : '';
            endpointSummary.textContent = `${requiresAuth} · ${methodLabel} ${template.endpoint}${payloadLabel}${description ? ' · ' + description : ''}`;
        }
    } else {
        if (dynamicHeaders) {
            dynamicHeaders.innerHTML = '';
            dynamicHeaders.style.display = 'none';
        }
        if (dynamicHeadersLabel) {
            dynamicHeadersLabel.style.display = 'none';
        }
        if (formDataContainer && formDataFields) {
            formDataContainer.style.display = 'none';
            formDataFields.innerHTML = '';
            if (formDataNote) {
                formDataNote.textContent = '';
                formDataNote.style.display = 'none';
            }
        }
        if (requestBodyLabel) {
            requestBodyLabel.style.display = 'block';
        }
        requestBody.style.display = 'block';
        requestBody.value = '';
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

// 임시 세션 토큰 저장 변수 (메모리에만 저장, localStorage 사용 안 함)
let adminSessionToken = '';

// 관리자 자동 로그인 (ID/PW → 세션토큰 → 인증코드 발송)
async function adminAutoLogin() {
    const adminId = document.getElementById('adminId').value.trim();
    const adminPassword = document.getElementById('adminPassword').value.trim();
    const step2Section = document.getElementById('adminStep2Section');

    if (!adminId || !adminPassword) {
        showAdminStatus('관리자 ID와 비밀번호를 입력해주세요.', 'error');
        return;
    }

    try {
        showAdminStatus('🔄 Step 1/2: 관리자 로그인 중...', 'info');

        // Step 1: /api/admin/login (ID/PW → 임시 세션토큰)
        const loginResponse = await fetch(`${baseURL}/api/admin/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                adminId: adminId,
                password: adminPassword
            })
        });

        const loginParsed = await parseResponseBody(loginResponse);
        const loginData = loginParsed.isJson ? loginParsed.body : null;

        if (!loginResponse.ok || !loginData || !loginData.success) {
            const message = loginData && loginData.message ? loginData.message : '로그인 실패';
            showAdminStatus('❌ 로그인 실패: ' + message, 'error');
            showResponse(formatResponseDisplay(loginResponse, loginParsed), 'error');
            return;
        }

        // 임시 세션 토큰 추출 및 저장 (메모리에만 저장)
        const responseData = loginData.data || {};
        adminSessionToken = responseData.sessionToken || responseData.tempToken || '';

        if (!adminSessionToken) {
            showAdminStatus('⚠️ 세션 토큰을 받지 못했습니다.', 'error');
            showResponse(formatResponseDisplay(loginResponse, loginParsed), 'error');
            return;
        }

        console.log('관리자 1차 인증 성공, 세션 토큰 획득');
        showAdminStatus('✅ Step 1/2 완료: 로그인 성공', 'success');

        // Step 2: /api/admin/email-auth/request (세션토큰 → 인증코드 발송)
        showAdminStatus('🔄 Step 2/2: 인증코드 요청 중...', 'info');

        const authCodeResponse = await fetch(`${baseURL}/api/admin/email-auth/request`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${adminSessionToken}`
            }
        });

        const authCodeParsed = await parseResponseBody(authCodeResponse);
        const authCodeData = authCodeParsed.isJson ? authCodeParsed.body : null;

        if (authCodeResponse.ok && authCodeData && authCodeData.success) {
            showAdminStatus('✅ 인증코드가 이메일로 전송되었습니다! 이메일을 확인하고 인증코드를 입력하세요.', 'success');
            step2Section.style.display = 'block';
            showResponse(formatResponseDisplay(authCodeResponse, authCodeParsed), 'success');
        } else {
            const message = authCodeData && authCodeData.message ? authCodeData.message : '알 수 없는 오류';
            showAdminStatus('❌ 인증코드 요청 실패: ' + message, 'error');
            step2Section.style.display = 'none';
            adminSessionToken = ''; // 실패 시 세션 토큰 제거
            showResponse(formatResponseDisplay(authCodeResponse, authCodeParsed), 'error');
        }

    } catch (error) {
        showAdminStatus('❌ 네트워크 오류: ' + error.message, 'error');
        adminSessionToken = ''; // 오류 시 세션 토큰 제거
        showResponse(`네트워크 오류: ${error.message}`, 'error');
    }
}

// 관리자 인증코드 요청 (Step 1) - 수동 모드용
async function adminRequestAuthCode() {
    const tempToken = document.getElementById('adminTempToken').value.trim();
    const step2Section = document.getElementById('adminStep2Section');

    if (!tempToken) {
        showAdminStatus('임시 토큰을 입력해주세요.', 'error');
        return;
    }

    // 수동 모드에서는 직접 입력한 임시 토큰 사용
    adminSessionToken = tempToken;

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
    const authCode = document.getElementById('adminAuthCode').value.trim().toUpperCase();

    // 자동 로그인 모드에서는 메모리에 저장된 세션 토큰 사용
    // 수동 모드에서는 adminRequestAuthCode()가 세션 토큰을 설정함
    if (!adminSessionToken) {
        showAdminStatus('세션 토큰이 없습니다. 먼저 로그인하거나 임시 토큰을 입력하세요.', 'error');
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
                'Authorization': `Bearer ${adminSessionToken}`
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

                // 세션 토큰 제거 (보안)
                adminSessionToken = '';

                // UI 초기화
                const adminIdField = document.getElementById('adminId');
                const adminPasswordField = document.getElementById('adminPassword');
                const adminTempTokenField = document.getElementById('adminTempToken');

                if (adminIdField) adminIdField.value = '';
                if (adminPasswordField) adminPasswordField.value = '';
                if (adminTempTokenField) adminTempTokenField.value = '';

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

// 관리자 토큰 갱신 함수
async function adminRefreshToken() {
    if (!refreshToken) {
        showAdminStatus('Refresh token이 없습니다. 먼저 관리자 로그인을 완료해주세요.', 'error');
        return;
    }

    try {
        showAdminStatus('관리자 토큰 갱신 중...', 'info');

        const response = await fetch(`${baseURL}/api/admin/auth/refresh`, {
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

                showAdminStatus('✅ 관리자 토큰이 성공적으로 갱신되었습니다.', 'success');
                showResponse(formatResponseDisplay(response, parsed), 'success');
            } else {
                showAdminStatus('⚠️ 토큰 갱신 응답에서 토큰을 찾을 수 없습니다.', 'error');
                showResponse(formatResponseDisplay(response, parsed), 'error');
            }
        } else {
            const message = data && data.message ? data.message : '알 수 없는 오류';
            showAdminStatus('❌ 관리자 토큰 갱신 실패: ' + message, 'error');
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
    const useFormData = template
        && template.bodyTemplate
        && typeof template.bodyTemplate === 'object'
        && template.bodyTemplate._formData;

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

    if (!useFormData && method !== 'GET') {
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

    if (useFormData) {
        const formData = new FormData();
        const formDataTemplate = template.bodyTemplate._formData;
        let formDataError = null;

        Object.entries(formDataTemplate).forEach(([field]) => {
            if (formDataError) {
                return;
            }

            const input = document.getElementById(`formData_${field}`);
            if (!input) {
                return;
            }

            const isRequired = input.dataset.required !== 'false';

            if (input.type === 'file') {
                if (input.files.length === 0) {
                    if (isRequired) {
                        formDataError = `필수 파일 "${field}"을(를) 선택해주세요.`;
                    }
                    return;
                }
                formData.append(field, input.files[0]);
            } else {
                const value = input.value.trim();
                if (!value) {
                    if (isRequired) {
                        formDataError = `필수 필드 "${field}"를 입력해주세요.`;
                    }
                    return;
                }
                formData.append(field, value);
            }
        });

        if (formDataError) {
            showResponse(formDataError, 'error');
            return;
        }

        requestOptions.body = formData;
    } else if (method !== 'GET' && requestBody.trim()) {
        try {
            JSON.parse(requestBody);
            requestOptions.body = requestBody;
        } catch (e) {
            showResponse('요청 본문이 유효한 JSON이 아닙니다.', 'error');
            return;
        }
    }

    try {
        const bodyPreview = useFormData
            ? '(form-data)'
            : (requestBody.trim() ? requestBody : '(empty)');
        showResponse(`요청 전송 중...\nURL: ${url}\nMethod: ${method}\nHeaders: ${JSON.stringify(headers, null, 2)}\nBody: ${bodyPreview}`, 'info');

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
            body: useFormData ? '(form-data)' : (requestBody || null),
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
