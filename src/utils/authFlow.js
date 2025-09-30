// src/utils/authFlow.js
import { readAccessToken } from '../utils/readAccessToken';

const AUTH_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/auth';

// ADD: 토큰 정규화 유틸
function normalizeToken(t) {
  if (!t) return '';
  let s = String(t).trim();
  if (/^Bearer\s+/i.test(s)) s = s.replace(/^Bearer\s+/i, '');
  if ((s.startsWith('"') && s.endsWith('"')) || (s.startsWith("'") && s.endsWith("'"))) {
    s = s.slice(1, -1);
  }
  return s.trim();
}

function readRefreshToken() {
  const t1 = localStorage.getItem('refreshToken') || localStorage.getItem('rtToken');
  if (t1) return t1;
  try {
    const raw = localStorage.getItem('user');
    if (!raw) return '';
    const obj = JSON.parse(raw);
    return obj?.data?.refreshToken || obj?.refreshToken || '';
  } catch { return ''; }
}

// REPLACE: 정규화해서 저장
export function persistTokens({ accessToken, refreshToken }) {
  const a = normalizeToken(accessToken);
  const r = normalizeToken(refreshToken);

  if (a) localStorage.setItem('accessToken', a);
  if (r) localStorage.setItem('refreshToken', r);

  try {
    const raw = localStorage.getItem('user');
    if (raw) {
      const obj = JSON.parse(raw);
      obj.data = obj.data || {};
      if (a) obj.data.accessToken = a;
      if (r) obj.data.refreshToken = r;
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

  // USE: 정규화 저장
  persistTokens({ accessToken: newAccess, refreshToken: newRefresh });
  return newAccess || null;
}

export async function ensureAccessTokenOrRedirect() {
  let token = readAccessToken();
  if (token && token.length >= 20) return token;

  token = await tryRefresh();
  if (token) return token;

  localStorage.removeItem('accessToken');
  localStorage.removeItem('authToken');
  window.location.href = '/';
  throw new Error('로그인이 필요합니다. (토큰 없음)');
}

