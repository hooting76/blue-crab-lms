// src/utils/authFlow.js
// 로그인 상태에서 액세스 토큰 보장: 
// 없으면 리프레시 시도 → 실패 시 로그인 페이지로 이동

import { readAccessToken } from './readAccessToken';

const AUTH_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/auth';

// refreshToken 읽기
function readRefreshToken() {
    const t1 = localStorage.getItem('refreshToken') || localStorage.getItem('rtToken');
        if (t1) return t1;
            try {
    const raw = localStorage.getItem('user');
        if (!raw) return '';
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw;
        return obj?.data?.refreshToken || obj?.refreshToken || '';
        } catch {
    return '';
  }
}

// 새 토큰을 표준 방식으로 저장(가능한 호환 유지)
function persistTokens({ accessToken, refreshToken }) {
    if (accessToken) localStorage.setItem('accessToken', accessToken);
    if (refreshToken) localStorage.setItem('refreshToken', refreshToken);

    try {
        const userRaw = localStorage.getItem('user');
    if (userRaw) {
        const user = JSON.parse(userRaw);
        if (user && typeof user === 'object') {
            user.data = user.data || {};
                if (accessToken) user.data.accessToken = accessToken;
                if (refreshToken) user.data.refreshToken = refreshToken;
        localStorage.setItem('user', JSON.stringify(user));
        }
    }
    } catch { /* noop */ }
}

// 토큰 연장 시도
async function tryRefresh() {
    const refreshToken = readRefreshToken();
    if (!refreshToken) return null;

    const res = await fetch(`${AUTH_BASE}/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
  });

    const ct = res.headers.get('content-type') || '';
    const data = ct.includes('application/json') ? await res.json() : await res.text();

        if (!res.ok || typeof data !== 'object' || !data.success) return null;

    const newAccess = data?.data?.accessToken || data?.accessToken;
    const newRefresh = data?.data?.refreshToken || data?.refreshToken;
        if (newAccess) persistTokens({ accessToken: newAccess, refreshToken: newRefresh });
            return newAccess || null;
}

//  * 액세스 토큰 확보:
//  - 있으면 그대로
//  - 없으면 refresh → 성공 시 갱신 토큰 반환
//  - 그래도 없으면 로그인 페이지로 이동

export async function ensureAccessTokenOrRedirect() {
    let token = readAccessToken();
        if (token) return token;

    token = await tryRefresh();
        if (token) return token;

    //실패: 로그인 페이지로 이동
    //현재 앱 라우팅 기준으로 루트가 로그인 화면
    localStorage.removeItem('accessToken');
    localStorage.removeItem('authToken');
    
    //'user' 등 필요한 항목도 정리할지 정책에 따라 결정
    //localStorage.removeItem('user');

    //SPA 내에서 자연스럽게 이동
    window.location.href = '/';
        throw new Error('로그인이 필요합니다. (토큰 없음)');
}
