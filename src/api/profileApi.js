// src/api/profileApi.js
// BlueCrab LMS - 프로필 API (실서버 연동, 무한로딩/401 자동 복구)

import { ensureAccessTokenOrRedirect } from '../utils/authFlow';
import { readAccessToken } from '../utils/readAccessToken';

const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/profile'; // ← 절대 URL 유지
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

// 401일 때 1회 재시도(리프레시 후) 유틸
async function fetchRetry401(url, opts) {
  let token = readAccessToken();
  let res = await fetchWithTimeout(url, { ...opts, headers: getHeaders(token) });

  if (res.status === 401) {
    // 토큰 연장 시도
    token = await ensureAccessTokenOrRedirect();
    res = await fetchWithTimeout(url, { ...opts, headers: getHeaders(token) });
  }
  return res;
}

/*
 * 1) 내 프로필 조회
 * POST /api/profile/me  (body: {})
 */
export const getMyProfile = async () => {
  try {
    // 최소 1차 보장
    await ensureAccessTokenOrRedirect();
    const res = await fetchRetry401(`${BASE_URL}/me`, {
      method: 'POST',
      body: JSON.stringify({}),
    });

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

/*
 * 2) 내 프로필 이미지 조회
 * POST /api/profile/me/image/file  (body: { imageKey })
 * 반환: objectURL
 */
export const getMyProfileImage = async (imageKey) => {
  try {
    if (!imageKey) throw new Error('imageKey가 필요합니다.');
    await ensureAccessTokenOrRedirect();

    const res = await fetchRetry401(`${BASE_URL}/me/image/file`, {
      method: 'POST',
      body: JSON.stringify({ imageKey }),
    });

    if (!res.ok) {
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
