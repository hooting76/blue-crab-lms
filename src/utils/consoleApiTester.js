// ============================================================================
// BlueCrab LMS - Browser Console API Tester
// ============================================================================
// 브라우저 개발자 콘솔에서 API를 테스트하기 위한 유틸리티
//
// 사용법:
// 1. 이 파일의 전체 내용을 복사
// 2. 브라우저 개발자 도구 콘솔에 붙여넣기
// 3. testAPI 객체의 메서드 사용
//
// 예시:
//   await testAPI.login('user@example.com', 'password123')
//   testAPI.getTokens()
//   await testAPI.call('/api/some-endpoint')
// ============================================================================

(function() {
  'use strict';

  // ========== Configuration ==========
  const CONFIG = {
    API_BASE: 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api',
    STORAGE_KEYS: {
      ACCESS_TOKEN: 'accessToken',
      REFRESH_TOKEN: 'refreshToken',
      USER_DATA: 'user'
    }
  };

  // ========== Utility Functions ==========

  /**
   * 토큰에서 Bearer 접두사와 따옴표 제거
   */
  function normalizeToken(token) {
    if (!token) return '';
    let normalized = String(token).trim();

    // Bearer 제거
    if (/^Bearer\s+/i.test(normalized)) {
      normalized = normalized.replace(/^Bearer\s+/i, '');
    }

    // 따옴표 제거
    if ((normalized.startsWith('"') && normalized.endsWith('"')) ||
        (normalized.startsWith("'") && normalized.endsWith("'"))) {
      normalized = normalized.slice(1, -1);
    }

    return normalized.trim();
  }

  /**
   * API 응답 로깅 (컬러풀한 콘솔 출력)
   */
  function logResponse(title, data, isError = false) {
    const color = isError ? '#ff4444' : '#44ff44';
    const icon = isError ? '❌' : '✅';

    console.group(`%c${icon} ${title}`, `color: ${color}; font-weight: bold; font-size: 14px;`);
    console.log('%cResponse:', 'color: #888; font-weight: bold;', data);
    console.groupEnd();
  }

  /**
   * API 요청 로깅
   */
  function logRequest(method, url, body = null) {
    console.group(`%c🔄 ${method} ${url}`, 'color: #4444ff; font-weight: bold;');
    if (body) {
      console.log('%cRequest Body:', 'color: #888; font-weight: bold;', body);
    }
    console.groupEnd();
  }

  // ========== Token Management ==========

  const TokenManager = {
    /**
     * 토큰을 localStorage에 저장
     */
    save(accessToken, refreshToken, userData = null) {
      const normalizedAccess = normalizeToken(accessToken);
      const normalizedRefresh = normalizeToken(refreshToken);

      if (normalizedAccess) {
        localStorage.setItem(CONFIG.STORAGE_KEYS.ACCESS_TOKEN, normalizedAccess);
      }

      if (normalizedRefresh) {
        localStorage.setItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN, normalizedRefresh);
      }

      // user 객체에도 저장 (기존 코드 호환성)
      if (userData) {
        try {
          const userObject = {
            data: {
              accessToken: normalizedAccess,
              refreshToken: normalizedRefresh,
              ...userData
            }
          };
          localStorage.setItem(CONFIG.STORAGE_KEYS.USER_DATA, JSON.stringify(userObject));
        } catch (e) {
          console.warn('Failed to save user data:', e);
        }
      }

      console.log('✅ Tokens saved to localStorage');
    },

    /**
     * 저장된 토큰 조회
     */
    get() {
      const accessToken = localStorage.getItem(CONFIG.STORAGE_KEYS.ACCESS_TOKEN);
      const refreshToken = localStorage.getItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN);

      return {
        accessToken,
        refreshToken,
        hasTokens: !!(accessToken && refreshToken)
      };
    },

    /**
     * Access Token 조회
     */
    getAccessToken() {
      return localStorage.getItem(CONFIG.STORAGE_KEYS.ACCESS_TOKEN);
    },

    /**
     * Refresh Token 조회
     */
    getRefreshToken() {
      let token = localStorage.getItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN);
      if (token) return token;

      // user 객체에서도 확인
      try {
        const raw = localStorage.getItem(CONFIG.STORAGE_KEYS.USER_DATA);
        if (raw) {
          const obj = JSON.parse(raw);
          return obj?.data?.refreshToken || obj?.refreshToken || '';
        }
      } catch (e) {
        return '';
      }
      return '';
    },

    /**
     * 모든 토큰 및 사용자 데이터 삭제
     */
    clear() {
      localStorage.removeItem(CONFIG.STORAGE_KEYS.ACCESS_TOKEN);
      localStorage.removeItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN);
      localStorage.removeItem(CONFIG.STORAGE_KEYS.USER_DATA);
      localStorage.removeItem('authToken'); // 추가 정리
      console.log('✅ All tokens cleared from localStorage');
    },

    /**
     * 사용자 정보 조회
     */
    getUserData() {
      try {
        const raw = localStorage.getItem(CONFIG.STORAGE_KEYS.USER_DATA);
        if (raw) {
          return JSON.parse(raw);
        }
      } catch (e) {
        console.error('Failed to parse user data:', e);
      }
      return null;
    }
  };

  // ========== API Client ==========

  const ApiClient = {
    /**
     * 범용 API 호출 메서드
     */
    async request(endpoint, options = {}) {
      const url = endpoint.startsWith('http')
        ? endpoint
        : `${CONFIG.API_BASE}${endpoint}`;

      const headers = {
        'Content-Type': 'application/json',
        ...options.headers
      };

      // Authorization 헤더 자동 추가 (skipAuth가 false일 때)
      if (!options.skipAuth) {
        const accessToken = TokenManager.getAccessToken();
        if (accessToken) {
          headers['Authorization'] = `Bearer ${accessToken}`;
        }
      }

      const fetchOptions = {
        method: options.method || 'GET',
        headers,
        ...options
      };

      // body가 있으면 추가
      if (options.body && typeof options.body === 'object') {
        fetchOptions.body = JSON.stringify(options.body);
      }

      logRequest(fetchOptions.method, url, options.body);

      try {
        const response = await fetch(url, fetchOptions);
        const contentType = response.headers.get('content-type') || '';

        let data;
        if (contentType.includes('application/json')) {
          data = await response.json();
        } else {
          data = await response.text();
        }

        if (!response.ok) {
          logResponse(`Error ${response.status}`, data, true);
          return { success: false, status: response.status, data };
        }

        logResponse(`Success ${response.status}`, data, false);
        return { success: true, status: response.status, data };

      } catch (error) {
        logResponse('Network Error', error, true);
        throw error;
      }
    },

    /**
     * 일반 사용자 로그인
     */
    async login(username, password) {
      console.log(`\n${'='.repeat(60)}\n🔐 User Login\n${'='.repeat(60)}`);

      const result = await this.request('/auth/login', {
        method: 'POST',
        body: { username, password },
        skipAuth: true
      });

      if (result.success && result.data.success) {
        const loginData = result.data.data;
        TokenManager.save(
          loginData.accessToken,
          loginData.refreshToken,
          loginData.user
        );

        console.log(`\n✅ Login successful!`);
        console.log(`👤 User:`, loginData.user);
        console.log(`🔑 Access Token:`, loginData.accessToken.substring(0, 20) + '...');
        console.log(`🔄 Refresh Token:`, loginData.refreshToken.substring(0, 20) + '...');
        console.log(`⏰ Expires In:`, loginData.expiresIn, 'seconds');
      }

      return result;
    },

    /**
     * 관리자 인증코드 요청
     */
    async adminRequestCode(tempToken) {
      console.log(`\n${'='.repeat(60)}\n🔐 Admin - Request Auth Code\n${'='.repeat(60)}`);

      const result = await this.request('/admin/email-auth/request', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${normalizeToken(tempToken)}`
        },
        skipAuth: true
      });

      if (result.success) {
        console.log(`\n✅ Auth code sent to admin email!`);
        console.log(`📧 Check your email for the 6-digit code`);
      }

      return result;
    },

    /**
     * 관리자 인증코드 검증
     */
    async adminVerify(tempToken, authCode) {
      console.log(`\n${'='.repeat(60)}\n🔐 Admin - Verify Auth Code\n${'='.repeat(60)}`);

      const result = await this.request('/admin/email-auth/verify', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${normalizeToken(tempToken)}`
        },
        body: { authCode: authCode.toUpperCase().trim() },
        skipAuth: true
      });

      if (result.success && result.data.success) {
        const authData = result.data.data;
        TokenManager.save(
          authData.accessToken,
          authData.refreshToken,
          authData.admin
        );

        console.log(`\n✅ Admin login successful!`);
        console.log(`👤 Admin:`, authData.admin);
        console.log(`🔑 Access Token:`, authData.accessToken.substring(0, 20) + '...');
        console.log(`🔄 Refresh Token:`, authData.refreshToken.substring(0, 20) + '...');
      }

      return result;
    },

    /**
     * 로그아웃
     */
    async logout() {
      console.log(`\n${'='.repeat(60)}\n🚪 Logout\n${'='.repeat(60)}`);

      const refreshToken = TokenManager.getRefreshToken();

      const result = await this.request('/auth/logout', {
        method: 'POST',
        body: { refreshToken }
      });

      if (result.success) {
        TokenManager.clear();
        console.log(`\n✅ Logout successful!`);
      }

      return result;
    },

    /**
     * 토큰 갱신
     */
    async refresh() {
      console.log(`\n${'='.repeat(60)}\n🔄 Refresh Token\n${'='.repeat(60)}`);

      const refreshToken = TokenManager.getRefreshToken();

      if (!refreshToken) {
        console.error('❌ No refresh token found');
        return { success: false, message: 'No refresh token' };
      }

      const result = await this.request('/auth/refresh', {
        method: 'POST',
        body: { refreshToken },
        skipAuth: true
      });

      if (result.success && result.data.success) {
        const tokenData = result.data.data;
        TokenManager.save(
          tokenData.accessToken,
          tokenData.refreshToken,
          tokenData.user
        );

        console.log(`\n✅ Token refreshed!`);
        console.log(`🔑 New Access Token:`, tokenData.accessToken.substring(0, 20) + '...');
      }

      return result;
    },

    /**
     * 토큰 검증
     */
    async validate() {
      console.log(`\n${'='.repeat(60)}\n✔️  Validate Token\n${'='.repeat(60)}`);

      const result = await this.request('/auth/validate', {
        method: 'GET'
      });

      if (result.success) {
        console.log(`\n✅ Token is valid!`);
      }

      return result;
    }
  };

  // ========== Public API ==========

  window.testAPI = {
    // ===== 로그인 =====
    /**
     * 일반 사용자 로그인
     * @param {string} username - 사용자 이메일
     * @param {string} password - 비밀번호
     */
    login: (username, password) => ApiClient.login(username, password),

    /**
     * 관리자 인증코드 요청 (1단계)
     * @param {string} tempToken - 임시 토큰
     */
    adminRequestCode: (tempToken) => ApiClient.adminRequestCode(tempToken),

    /**
     * 관리자 인증코드 검증 (2단계)
     * @param {string} tempToken - 임시 토큰
     * @param {string} authCode - 6자리 인증코드
     */
    adminVerify: (tempToken, authCode) => ApiClient.adminVerify(tempToken, authCode),

    // ===== 토큰 관리 =====
    /**
     * 저장된 토큰 조회
     */
    getTokens: () => {
      const tokens = TokenManager.get();
      console.log(`\n${'='.repeat(60)}\n🔑 Stored Tokens\n${'='.repeat(60)}`);
      console.log('Access Token:', tokens.accessToken ? tokens.accessToken.substring(0, 30) + '...' : 'None');
      console.log('Refresh Token:', tokens.refreshToken ? tokens.refreshToken.substring(0, 30) + '...' : 'None');
      console.log('Has Valid Tokens:', tokens.hasTokens);
      return tokens;
    },

    /**
     * 토큰 삭제
     */
    clearTokens: () => TokenManager.clear(),

    /**
     * 현재 사용자 정보 조회
     */
    getCurrentUser: () => {
      const userData = TokenManager.getUserData();
      console.log(`\n${'='.repeat(60)}\n👤 Current User\n${'='.repeat(60)}`);
      console.log(userData);
      return userData;
    },

    // ===== API 호출 =====
    /**
     * 범용 API 호출 (Authorization 헤더 자동 추가)
     * @param {string} endpoint - API 엔드포인트
     * @param {object} options - fetch 옵션
     */
    call: (endpoint, options = {}) => ApiClient.request(endpoint, options),

    /**
     * 로그아웃
     */
    logout: () => ApiClient.logout(),

    /**
     * 토큰 갱신
     */
    refresh: () => ApiClient.refresh(),

    /**
     * 토큰 검증
     */
    validate: () => ApiClient.validate(),

    // ===== 유틸리티 =====
    /**
     * 도움말 출력
     */
    help: () => {
      console.log(`
╔════════════════════════════════════════════════════════════════╗
║           BlueCrab LMS - Console API Tester v1.0              ║
╚════════════════════════════════════════════════════════════════╝

📚 사용 가능한 명령어:

┌─ 로그인 ──────────────────────────────────────────────────────┐
│ await testAPI.login(username, password)                        │
│   - 일반 사용자 로그인                                           │
│   - 예: await testAPI.login('user@example.com', 'pass123')     │
│                                                                 │
│ await testAPI.adminRequestCode(tempToken)                      │
│   - 관리자 인증코드 요청 (1단계)                                 │
│                                                                 │
│ await testAPI.adminVerify(tempToken, authCode)                 │
│   - 관리자 인증코드 검증 (2단계)                                 │
│   - 예: await testAPI.adminVerify(token, 'ABC123')             │
└────────────────────────────────────────────────────────────────┘

┌─ 토큰 관리 ───────────────────────────────────────────────────┐
│ testAPI.getTokens()                                            │
│   - 저장된 토큰 조회                                            │
│                                                                 │
│ testAPI.clearTokens()                                          │
│   - 모든 토큰 삭제                                              │
│                                                                 │
│ testAPI.getCurrentUser()                                       │
│   - 현재 로그인한 사용자 정보 조회                               │
└────────────────────────────────────────────────────────────────┘

┌─ API 호출 ────────────────────────────────────────────────────┐
│ await testAPI.call(endpoint, options)                          │
│   - 범용 API 호출 (Authorization 자동 추가)                     │
│   - 예: await testAPI.call('/api/profile', {method: 'GET'})   │
│                                                                 │
│ await testAPI.logout()                                         │
│   - 로그아웃                                                    │
│                                                                 │
│ await testAPI.refresh()                                        │
│   - 액세스 토큰 갱신                                            │
│                                                                 │
│ await testAPI.validate()                                       │
│   - 현재 토큰 유효성 검증                                       │
└────────────────────────────────────────────────────────────────┘

┌─ 유틸리티 ────────────────────────────────────────────────────┐
│ testAPI.help()                                                 │
│   - 이 도움말 출력                                              │
└────────────────────────────────────────────────────────────────┘

💡 사용 예시:

  // 1. 일반 사용자 로그인
  await testAPI.login('student@example.com', 'password123')

  // 2. 토큰 확인
  testAPI.getTokens()

  // 3. API 호출
  await testAPI.call('/api/profile')

  // 4. 로그아웃
  await testAPI.logout()

📝 참고:
  - 모든 API 호출은 자동으로 Authorization 헤더를 추가합니다
  - 토큰은 localStorage에 자동으로 저장/관리됩니다
  - 컬러풀한 로그로 요청/응답을 쉽게 확인할 수 있습니다

🔗 API Base URL: ${CONFIG.API_BASE}
      `);
    },

    // ===== 설정 =====
    config: CONFIG
  };

  // ===== 초기화 메시지 =====
  console.log(`
╔════════════════════════════════════════════════════════════════╗
║     ✅ BlueCrab Console API Tester Loaded Successfully!       ║
╚════════════════════════════════════════════════════════════════╝

Type 'testAPI.help()' to see available commands.
  `);

})();
