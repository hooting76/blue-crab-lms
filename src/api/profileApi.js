// src/api/profileApi.js
// BlueCrab LMS - 프로필 API (목업 없이 실서버 연동, 무한로딩/500 방지)

const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/profile';

/* 공통 헤더 */
const REQ_TIMEOUT_MS = 10000; // 10s

const getHeaders = (accessToken) => ({
  'Authorization': `Bearer ${accessToken}`,
  'Content-Type': 'application/json',
});

/* 안전한 JSON 파서 */
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

/*
 *  1) 내 프로필 조회
 *  명세: POST /api/profile/me
 *  -body는 비우지 말고 {} 전달
 *  공통 응답 래퍼: { success, message, data, errorCode, timestamp }
 */
export const getMyProfile = async (accessToken) => {
  try {
    // (선택): 토큰 가드로 즉시 실패 처리
    if (!accessToken) throw new Error('로그인이 필요합니다. (토큰 없음)');

    const res = await fetchWithTimeout(`${BASE_URL}/me`, {
      method: 'POST',
      headers: getHeaders(accessToken),
      body: JSON.stringify({}), // 빈 바디 대신 {}
    });

    const payload = await parseJsonSafe(res);

    // HTTP 에러
    if (!res.ok) {
      const msg = typeof payload === 'object' ? (payload.message || '프로필 조회 실패') : '프로필 조회 실패';
      throw new Error(msg);
    }

    // 래퍼 success=false 처리
    if (typeof payload === 'object' && payload.success === false) {
      const msg = payload.message || payload.errorCode || '프로필 조회 실패';
      const err = new Error(msg);
      err.payload = payload;
      throw err;
    }

    return payload; // { success, message, data, errorCode, timestamp }
  } catch (error) {
    // 콘솔은 남기되, 상위에서 처리할 수 있게 그대로 throw
    console.error('프로필 조회 에러:', error);
    throw error;
  }
};

/**
 * 2) 내 프로필 이미지 조회 (바이너리)
 *  명세: POST /api/profile/me/image/file
 *  -body: { imageKey }
 *  -응답: 이미지 바이너리 (jpeg/png/gif 등)
 *  -반환: objectURL (img.src에 바로 사용)
 */
export const getMyProfileImage = async (accessToken, imageKey) => {
    try {
        if (!imageKey) throw new Error('imageKey가 필요합니다.');

    const res = await fetchWithTimeout(`${BASE_URL}/me/image/file`, {
      method: 'POST',
      headers: getHeaders(accessToken),
      body: JSON.stringify({ imageKey }),
    });

    if (!res.ok) {
    //이 API는 보통 JSON 대신 상태코드로 에러 표현 (명세)
    throw new Error(`프로필 이미지 로드 실패 (HTTP ${res.status})`);
    }

    const blob = await res.blob();
    return URL.createObjectURL(blob);
  } catch (error) {
    console.error('프로필 이미지 조회 에러:', error);
    throw error;
  }
};

export default getMyProfile;
