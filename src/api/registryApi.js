// src/api/registryApi.js
// 학적(레지스트리) 조회 API 어댑터
// - 기본: POST /api/registry/me (서버 구현 시)
// - 404 응답 시: /api/profile/me 로 폴백 조회 필요한 필드 매핑

import { ensureAccessTokenOrRedirect } from '../utils/authFlow';
import { readAccessToken } from '../utils/readAccessToken';

const BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
const REQ_TIMEOUT_MS = 10000;

const getHeaders = (token) => ({
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json',
});

const fetchWithTimeout = (url, opts = {}, ms = REQ_TIMEOUT_MS) =>
  Promise.race([
    fetch(url, opts),
    new Promise((_, rej) => setTimeout(() => rej(new Error('요청 시간 초과')), ms)),
  ]);

const parseJsonSafe = async (res) => {
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) return res.json();
  return res.text();
};

// 공통 POST
async function postJson(path, body = {}) {
  let token = readAccessToken();
  let res = await fetchWithTimeout(`${BASE}${path}`, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify(body),
  });

  // 401 → 토큰 갱신 1회
  if (res.status === 401) {
    token = await ensureAccessTokenOrRedirect();
    res = await fetchWithTimeout(`${BASE}${path}`, {
      method: 'POST',
      headers: getHeaders(token),
      body: JSON.stringify(body),
    });
  }
  return res;
}

// 프로필 폴백: /api/profile/me → 레지스트리 유사 DTO로 변환
async function fetchProfileAsRegistry() {
  const res = await postJson('/profile/me', {});
  const payload = await parseJsonSafe(res);
  if (!res.ok || !payload || payload.success === false) {
    const msg = payload?.message || '프로필 조회 실패';
    const err = new Error(msg);
    err.payload = payload;
    throw err;
  }

  const p = payload.data || {};
  // 레지스트리에서 쓰는(또는 증명서 화면에 필요한) 최소 필드 구성
  return {
    ok: true,
    data: {
      // 레지스트리 성격 필드
      userIdx: p.userIdx ?? null,                    // 서버에 따라 없을 수 있음
      userCode: p.majorCode || null,                 // 학번/교번
      joinPath: p.admissionRoute || '신규',          // 입학경로
      stdStat: p.academicStatus || '재학',           // 학적상태
      cntTerm: 0,                                    // 누적학기(서버 미구현 가정시 0)

      // 증명서 렌더에 편의 필드
      userName: p.userName || '',
      userEmail: p.userEmail || '',
      userTypeText: p.userTypeText || '학생',
      majorCode: p.majorCode || '',
      zipCode: p.zipCode || '',
      mainAddress: p.mainAddress || '',
      detailAddress: p.detailAddress || '',
      birthDate: p.birthDate || '',
      image: p.image || null,
    },
  };
}

// 메인: 레지스트리 조회 시도 → 404면 프로필 폴백
export async function getMyRegistry() {
  await ensureAccessTokenOrRedirect();

  try {
    const res = await postJson('/registry/me', {}); // 서버 구현 시 여기서 정상 동작
    if (res.status === 404) {
      // 엔드포인트 미구현 → 프로필로 폴백
      return await fetchProfileAsRegistry();
    }

    const payload = await parseJsonSafe(res);

    if (!res.ok || !payload || payload.success === false) {
      // 다른 오류는 그대로 던짐(권한/서버오류 등)
      const msg = payload?.message || '학적 정보 조회 실패';
      const err = new Error(msg);
      err.payload = payload;
      throw err;
    }

    const r = payload.data || {};
    // 서버가 레지스트리 응답을 주는 경우 그대로/안전 매핑
    return {
      ok: true,
      data: {
        userIdx: r.userIdx ?? r.USER_IDX ?? null,
        userCode: r.userCode ?? r.USER_CODE ?? null,
        joinPath: r.joinPath ?? r.JOIN_PATH ?? '신규',
        stdStat: r.stdStat ?? r.STD_STAT ?? '재학',
        cntTerm: r.cntTerm ?? r.CNT_TERM ?? 0,

        // 화면 편의(서버가 같이 내려주지 않으면 빈 값)
        userName: r.userName ?? '',
        userEmail: r.userEmail ?? '',
        userTypeText: r.userTypeText ?? '학생',
        majorCode: r.majorCode ?? r.USER_CODE ?? '',
        zipCode: r.zipCode ?? '',
        mainAddress: r.mainAddress ?? '',
        detailAddress: r.detailAddress ?? '',
        birthDate: r.birthDate ?? '',
        image: r.image ?? null,
      },
    };
  } catch (e) {
    // 네트워크/예상치 못한 에러 → 프로필 폴백 한 번 더 시도
    try {
      return await fetchProfileAsRegistry();
    } catch (_) {
      // 폴백도 실패면 최종 에러
      return { ok: false, message: e.message || '학적 정보 조회 실패' };
    }
  }
}
