// src/api/client.js
//공통 fetch 래퍼 (401 자동갱신 → 재시도 → 실패 시 로그인으로 이동)
import { getAccessToken, getRefreshToken, setTokens, clearAuth } from '@/utils/authTokens';

const REQ_TIMEOUT_MS = 10000; // 그대로 유지

function fetchWithTimeout(url, opts = {}, ms = REQ_TIMEOUT_MS) {
    return Promise.race([
        fetch(url, opts),
        new Promise((_, rej) => setTimeout(() => rej(new Error('요청 시간 초과')), ms)),
    ]);
}

const API_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

async function refreshTokenFlow() {
    const refreshToken = getRefreshToken();
        if (!refreshToken) throw new Error('리프레시 토큰 없음');

    const res = await fetchWithTimeout(`${API_BASE}/api/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
    });

    const payload = await res.json().catch(() => ({}));
        if (!res.ok || payload?.success === false) {
            throw new Error(payload?.message || '토큰 갱신 실패');
    } 

    const newAccess = payload?.data?.accessToken || payload?.accessToken;
    const newRefresh = payload?.data?.refreshToken || payload?.refreshToken;
        if (!newAccess) throw new Error('갱신 응답에 accessToken 없음');

    setTokens({ accessToken: newAccess, refreshToken: newRefresh });
        return newAccess;
}

export async function apiPost(path, body = {}, extraHeaders = {}) {
    const doRequest = async (tokenOverride) => {
    const token = tokenOverride || getAccessToken();
    const res = await fetchWithTimeout(`${API_BASE}${path}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            ...extraHeaders,
        },
        body: JSON.stringify(body ?? {}),
    });
    return res;
  };

    //1차 요청
    let res = await doRequest();
        if (res.status === 401) {
        // 토큰 갱신 시도
        try {
            const newToken = await refreshTokenFlow();
            //갱신 성공 → 재시도
            res = await doRequest(newToken);
        } catch {
        //갱신 실패 → 인증 해제 및 로그인 페이지로
        clearAuth();
        // 라우터 접근이 없을 수도 있으니 하드 리다이렉트
        window.location.replace('/');
        //리턴은 안전하게 예외
            throw new Error('로그인이 만료되어 로그인 화면으로 이동합니다.');
    }
  }
  return res;
}
