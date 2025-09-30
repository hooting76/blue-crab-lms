// src/utils/authFlow.js
// 로그인 상태에서 액세스 토큰 보장:
// 없으면 refresh → 성공 시 갱신, 그래도 없으면 로그인 페이지로 이동

import { readAccessToken } from '../utils/readAccessToken';

const AUTH_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/auth';

function readRefreshToken() {
  const t1 = localStorage.getItem('refreshToken') || localStorage.getItem('rtToken');
  if (t1) return t1;
  try {
    const raw = localStorage.getItem('user');
    if (!raw) return '';
    const obj = JSON.parse(raw);
    return obj?.data?.refreshToken || obj?.refreshToken || '';
  } catch {
    return '';
  }
}

function persistTokens({ accessToken, refreshToken }) {
  if (accessToken) localStorage.setItem('accessToken', accessToken);
  if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
  try {
    const raw = localStorage.getItem('user');
    if (!raw) return;
    const obj = JSON.parse(raw);
    if (obj && typeof obj === 'object') {
      obj.data = obj.data || {};
      if (accessToken) obj.data.accessToken = accessToken;
      if (refreshToken) obj.data.refreshToken = refreshToken;
      localStorage.setItem('user', JSON.stringify(obj));
    }
  } catch { /* noop */ }
}

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

export async function ensureAccessTokenOrRedirect() {
  let token = readAccessToken();
  if (token) return token;

  token = await tryRefresh();
  if (token) return token;

  // 실패: 로그인 화면으로 이동
  localStorage.removeItem('accessToken');
  localStorage.removeItem('authToken');
  // 정책에 따라 user 제거 여부 결정
  // localStorage.removeItem('user');

  window.location.href = '/';
  throw new Error('로그인이 필요합니다. (토큰 없음)');
}
