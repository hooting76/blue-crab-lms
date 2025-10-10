// src/api/registryApi.js
// REGIST_TABLE 최신 학적 상태 조회 (POST /api/registry/me)

import { readAccessToken } from '../utils/readAccessToken';
import { ensureAccessTokenOrRedirect } from '../utils/authFlow';

const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/registry';

const getHeaders = (token) => ({
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
});

// 10s timeout
const fetchWithTimeout = (url, opts = {}, ms = 10000) =>
    Promise.race([
        fetch(url, opts),
        new Promise((_, rej) => setTimeout(() => rej(new Error('요청 시간 초과')), ms)),
    ]);

// 401 시 1회 토큰갱신 후 재시도
async function postRetry401(path, bodyObj) {
    let token = readAccessToken();
    let res = await fetchWithTimeout(`${BASE_URL}${path}`, {
        method: 'POST',
        headers: getHeaders(token),
        body: JSON.stringify(bodyObj ?? {}),
    });
if (res.status === 401) {
    token = await ensureAccessTokenOrRedirect();
    res = await fetchWithTimeout(`${BASE_URL}${path}`, {
        method: 'POST',
        headers: getHeaders(token),
        body: JSON.stringify(bodyObj ?? {}),
    });
  }
  return res;
}

async function ensureOkJson(res, fallbackMsg) {
    const ct = res.headers.get('content-type') || '';
    const payload = ct.includes('application/json') ? await res.json() : await res.text();
    if (!res.ok) throw new Error(typeof payload === 'object' ? (payload.message || fallbackMsg) : fallbackMsg);
    if (typeof payload === 'object' && payload.success === false) {
    const err = new Error(payload.message || payload.errorCode || fallbackMsg);
    err.payload = payload;
    throw err;
  }
  return payload;
}

// 최신 학적 1건
export async function getMyRegistry() {
    await ensureAccessTokenOrRedirect();
    const res = await postRetry401('/me', {});
    const payload = await ensureOkJson(res, '학적 정보를 불러오지 못했습니다.');
return payload?.data || null; // { userCode, stdStat, joinPath, cntTerm, ... } 예상
}
