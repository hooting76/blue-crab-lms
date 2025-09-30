// src/api/profileApi.js
// BlueCrab LMS - 프로필 API (실서버 연동, POST-only, 401 자동 갱신 후 1회 재시도)

import { ensureAccessTokenOrRedirect } from '../utils/authFlow';
import { readAccessToken } from '../utils/readAccessToken';
//console.log('[profileApi] tokenLen=', (readAccessToken() || '').length);

const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/profile'; // 절대 URL 유지
const REQ_TIMEOUT_MS = 10000; // 10s

const getHeaders = (accessToken) => ({
  'Authorization': `Bearer ${accessToken}`,
  'Content-Type': 'application/json',
});

const parseJsonSafe = async (res) => {
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) return res.json();
  return res.text();
};

const fetchWithTimeout = (url, opts = {}, ms = REQ_TIMEOUT_MS) =>
  Promise.race([
    fetch(url, opts),
    new Promise((_, rej) => setTimeout(() => rej(new Error('요청 시간 초과')), ms)),
  ]);

// 401 이면 1회 갱신 후 재시도
async function fetchRetry401(url, bodyObj) {
  let token = readAccessToken();
  let res = await fetchWithTimeout(url, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify(bodyObj ?? {}),
  });

  if (res.status === 401) {
    token = await ensureAccessTokenOrRedirect();
    res = await fetchWithTimeout(url, {
      method: 'POST',
      headers: getHeaders(token),
      body: JSON.stringify(bodyObj ?? {}),
    });
  }
  return res;
}

/**
 * 1) 내 프로필 조회
 * POST /api/profile/me  (body: {})
 * 공통 래퍼: { success, message, data, errorCode, timestamp }
 */
export const getMyProfile = async () => {
  try {
    // 최소 1차 보장: 토큰 확보(없으면 refresh → 실패시 로그인 이동)
    await ensureAccessTokenOrRedirect();

    const res = await fetchRetry401(`${BASE_URL}/me`, {});
    const payload = await parseJsonSafe(res);

    if (!res.ok) {
      const msg = typeof payload === 'object' ? (payload.message || '프로필 조회 실패') : '프로필 조회 실패';
      throw new Error(msg);
    }
    if (typeof payload === 'object' && payload.success === false) {
      const msg = payload.message || payload.errorCode || '프로필 조회 실패';
      const err = new Error(msg);
      err.payload = payload;
      throw err;
    }
    return payload; // { success, message, data, ... }
  } catch (e) {
    console.error('프로필 조회 에러:', e);
    throw e;
  }
};

/**
 * 2) 내 프로필 이미지 조회
 * POST /api/profile/me/image/file  (body: { imageKey })
 * 응답: 바이너리 → objectURL
 */
export const getMyProfileImage = async (imageKey) => {
  try {
    if (!imageKey) throw new Error('imageKey가 필요합니다.');
    await ensureAccessTokenOrRedirect();

    const res = await fetchRetry401(`${BASE_URL}/me/image/file`, { imageKey });

    if (!res.ok) {
      // 이미지 API는 상태코드로 에러 표현
      throw new Error(`프로필 이미지 로드 실패 (HTTP ${res.status})`);
    }
    const blob = await res.blob();
    return URL.createObjectURL(blob);
  } catch (e) {
    console.error('프로필 이미지 조회 에러:', e);
    throw e;
  }
};

export default getMyProfile;
