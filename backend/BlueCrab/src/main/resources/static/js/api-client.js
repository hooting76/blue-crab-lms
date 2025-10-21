// ==================== API 클라이언트 (자동 토큰 리프레시 기능 포함) ====================

/**
 * 자동 토큰 리프레시 기능을 가진 API 클라이언트
 * 401 에러 + X-Token-Expired 헤더 감지 시 자동으로 토큰을 갱신하고 요청을 재시도합니다.
 */
class ApiClient {
    constructor() {
        this.baseUrl = '';
        this.isRefreshing = false;
        this.failedQueue = [];
    }

    /**
     * 실패한 요청들을 큐에 추가
     * @param {Function} resolve - Promise resolve 함수
     * @param {Function} reject - Promise reject 함수
     */
    addToFailedQueue(resolve, reject) {
        this.failedQueue.push({ resolve, reject });
    }

    /**
     * 큐에 있는 모든 요청들 처리
     * @param {Error|null} error - 에러 객체 (실패 시)
     * @param {string|null} token - 새로운 액세스 토큰 (성공 시)
     */
    processQueue(error, token = null) {
        this.failedQueue.forEach(promise => {
            if (error) {
                promise.reject(error);
            } else {
                promise.resolve(token);
            }
        });

        this.failedQueue = [];
    }

    /**
     * 리프레시 토큰으로 새로운 액세스 토큰 발급
     * @returns {Promise<string>} 새로운 액세스 토큰
     */
    async refreshAccessToken() {
        const refreshToken = localStorage.getItem('bluecrab_refresh_token');

        if (!refreshToken) {
            throw new Error('리프레시 토큰이 없습니다. 다시 로그인해주세요.');
        }

        try {
            const response = await fetch(`${this.baseUrl}/api/auth/refresh`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
            });

            if (!response.ok) {
                throw new Error('토큰 갱신에 실패했습니다.');
            }

            const result = await response.json();

            if (result.success && result.data) {
                const { accessToken, refreshToken: newRefreshToken } = result.data;

                // 새로운 토큰 저장
                localStorage.setItem('bluecrab_access_token', accessToken);
                if (newRefreshToken) {
                    localStorage.setItem('bluecrab_refresh_token', newRefreshToken);
                }

                // 전역 변수 업데이트 (token-manager.js와 연동)
                if (typeof window.accessToken !== 'undefined') {
                    window.accessToken = accessToken;
                }
                if (typeof window.refreshToken !== 'undefined' && newRefreshToken) {
                    window.refreshToken = newRefreshToken;
                }

                console.log('✅ 토큰이 자동으로 갱신되었습니다.');
                return accessToken;
            } else {
                throw new Error(result.message || '토큰 갱신 응답이 올바르지 않습니다.');
            }
        } catch (error) {
            console.error('토큰 갱신 실패:', error);
            throw error;
        }
    }

    /**
     * HTTP 요청 실행 (토큰 리프레시 로직 포함)
     * @param {string} url - 요청 URL
     * @param {Object} options - fetch 옵션
     * @returns {Promise<Response>} fetch Response 객체
     */
    async request(url, options = {}) {
        const fullUrl = `${this.baseUrl}${url}`;

        // Authorization 헤더 자동 추가
        const accessToken = localStorage.getItem('bluecrab_access_token');
        if (accessToken && !options.skipAuth) {
            options.headers = options.headers || {};
            options.headers['Authorization'] = `Bearer ${accessToken}`;
        }

        try {
            const response = await fetch(fullUrl, options);

            // 401 에러 + 토큰 만료 헤더가 있는 경우
            if (response.status === 401 && response.headers.get('X-Token-Expired') === 'true') {
                console.warn('⚠️ 토큰이 만료되었습니다. 자동 갱신을 시도합니다...');

                // 이미 다른 요청이 리프레시 중인 경우
                if (this.isRefreshing) {
                    // 큐에 추가하고 대기
                    return new Promise((resolve, reject) => {
                        this.addToFailedQueue(resolve, reject);
                    }).then(newToken => {
                        // 새 토큰으로 원래 요청 재시도
                        options.headers['Authorization'] = `Bearer ${newToken}`;
                        return fetch(fullUrl, options);
                    });
                }

                // 첫 번째 요청이 리프레시 시작
                this.isRefreshing = true;

                try {
                    const newToken = await this.refreshAccessToken();

                    // 대기 중인 요청들 모두 처리
                    this.processQueue(null, newToken);

                    // 원래 요청 재시도
                    options.headers['Authorization'] = `Bearer ${newToken}`;
                    return await fetch(fullUrl, options);

                } catch (refreshError) {
                    // 리프레시 실패 - 모든 대기 요청 실패 처리
                    this.processQueue(refreshError, null);

                    // 리프레시 토큰도 만료된 경우 - 로그아웃 처리
                    console.error('❌ 토큰 갱신 실패. 다시 로그인해주세요.');
                    this.handleLogout();

                    throw refreshError;
                } finally {
                    this.isRefreshing = false;
                }
            }

            return response;

        } catch (error) {
            console.error('API 요청 실패:', error);
            throw error;
        }
    }

    /**
     * 로그아웃 처리 (토큰 제거 및 페이지 리다이렉트)
     */
    handleLogout() {
        localStorage.removeItem('bluecrab_access_token');
        localStorage.removeItem('bluecrab_refresh_token');

        // 전역 변수 초기화
        if (typeof window.accessToken !== 'undefined') {
            window.accessToken = '';
        }
        if (typeof window.refreshToken !== 'undefined') {
            window.refreshToken = '';
        }

        // 로그인 페이지로 리다이렉트 (필요한 경우)
        // window.location.href = '/login';

        console.log('로그아웃되었습니다. 다시 로그인해주세요.');
    }

    // ==================== 편의 메서드 ====================

    /**
     * GET 요청
     * @param {string} url - 요청 URL
     * @param {Object} options - 추가 옵션
     * @returns {Promise<any>} JSON 응답 데이터
     */
    async get(url, options = {}) {
        const response = await this.request(url, {
            ...options,
            method: 'GET'
        });
        return response.json();
    }

    /**
     * POST 요청
     * @param {string} url - 요청 URL
     * @param {Object} data - 요청 바디 데이터
     * @param {Object} options - 추가 옵션
     * @returns {Promise<any>} JSON 응답 데이터
     */
    async post(url, data, options = {}) {
        const response = await this.request(url, {
            ...options,
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(options.headers || {})
            },
            body: JSON.stringify(data)
        });
        return response.json();
    }

    /**
     * PUT 요청
     * @param {string} url - 요청 URL
     * @param {Object} data - 요청 바디 데이터
     * @param {Object} options - 추가 옵션
     * @returns {Promise<any>} JSON 응답 데이터
     */
    async put(url, data, options = {}) {
        const response = await this.request(url, {
            ...options,
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                ...(options.headers || {})
            },
            body: JSON.stringify(data)
        });
        return response.json();
    }

    /**
     * DELETE 요청
     * @param {string} url - 요청 URL
     * @param {Object} options - 추가 옵션
     * @returns {Promise<any>} JSON 응답 데이터
     */
    async delete(url, options = {}) {
        const response = await this.request(url, {
            ...options,
            method: 'DELETE'
        });
        return response.json();
    }
}

// 싱글톤 인스턴스 생성 및 전역 노출
const apiClient = new ApiClient();

// ==================== 사용 예시 ====================
/*
// 기본 사용법
const result = await apiClient.get('/api/courses');

// POST 요청
const newCourse = await apiClient.post('/api/courses', {
    name: '새 강의',
    description: '강의 설명'
});

// 커스텀 헤더
const data = await apiClient.get('/api/users', {
    headers: {
        'X-Custom-Header': 'value'
    }
});

// 인증 없이 요청 (로그인 API 등)
const loginResult = await apiClient.post('/api/auth/login',
    { username: 'user', password: 'pass' },
    { skipAuth: true }
);
*/
