// src/api/facility.js
// BlueCrab LMS - 시설 API (실서버 연동, POST-only 우선, 401 자동 갱신 후 1회 재시도)

import { ensureAccessTokenOrRedirect } from '../utils/authFlow';
import { readAccessToken } from '../utils/readAccessToken';

const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api'; // 절대 URL 유지
const REQ_TIMEOUT_MS = 10000;

// 공통 헤더
const getHeaders = (accessToken) => ({
  'Authorization': `Bearer ${accessToken}`,
  'Content-Type': 'application/json',
});

// 안전 JSON 파싱
const parseJsonSafe = async (res) => {
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) return res.json();
  return res.text();
};

// 타임아웃 포함 fetch
const fetchWithTimeout = (url, opts = {}, ms = REQ_TIMEOUT_MS) =>
  Promise.race([
    fetch(url, opts),
    new Promise((_, rej) => setTimeout(() => rej(new Error('요청 시간 초과')), ms)),
  ]);

// 401 → 토큰 갱신 후 1회 재시도 (POST 기본)
async function postRetry401(url, bodyObj) {
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

// (예외) 서버가 꼭 DELETE 를 요구하는 경우를 대비한 fallback
async function deleteRetry401(url) {
  let token = readAccessToken();
  let res = await fetchWithTimeout(url, { method: 'DELETE', headers: getHeaders(token) });

  if (res.status === 401) {
    token = await ensureAccessTokenOrRedirect();
    res = await fetchWithTimeout(url, { method: 'DELETE', headers: getHeaders(token) });
  }
  return res;
}

// 공통 응답 처리: { success, message, data, ... }
async function handleResponse(res, defaultErrMsg = '요청 처리 실패') {
  const payload = await parseJsonSafe(res);

  if (!res.ok) {
    const msg = typeof payload === 'object' ? (payload.message || defaultErrMsg) : defaultErrMsg;
    const err = new Error(msg);
    err.payload = payload;
    err.status = res.status;
    throw err;
  }
  if (typeof payload === 'object' && payload.success === false) {
    const msg = payload.message || defaultErrMsg;
    const err = new Error(msg);
    err.payload = payload;
    throw err;
  }
  return payload; // ApiResponse<T> 혹은 텍스트
}

/* ===========================
 *  시설 조회 & 가용성 / 일정
 * =========================== */

// 활성 시설 목록
export const postFacilities = async () => {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${BASE_URL}/facilities`, {});
  return handleResponse(res, '시설 목록 조회 실패');
};

// (선택) 유형별 시설 목록
export const postFacilitiesByType = async (facilityType) => {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${BASE_URL}/facilities/type/${facilityType}`, {});
  return handleResponse(res, '시설 목록 조회 실패');
};

// 일일 일정 조회 (09~18 시간대 표시용)
export const postDailySchedule = async (facilityIdx, dateYMD) => {
  await ensureAccessTokenOrRedirect();
  const url = `${BASE_URL}/facilities/${facilityIdx}/daily-schedule?date=${encodeURIComponent(dateYMD)}`;
  const res = await postRetry401(url, {}); // 서버는 POST-only
  return handleResponse(res, '일일 일정 조회 실패');
};

// 가용성 확인
export const postAvailability = async (facilityIdx, startTime, endTime) => {
  await ensureAccessTokenOrRedirect();
  const url = `${BASE_URL}/facilities/${facilityIdx}/availability?startTime=${encodeURIComponent(startTime)}&endTime=${encodeURIComponent(endTime)}`;
  const res = await postRetry401(url, {}); // 서버는 POST-only
  return handleResponse(res, '가용성 확인 실패');
};

/* ==============
 *  예약 생성/조회
 * ============== */

// 예약 생성
export const postReservation = async (payload) => {
  // payload: { facilityIdx, startTime, endTime, partySize, purpose, requestedEquipment }
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${BASE_URL}/reservations`, payload);
  return handleResponse(res, '예약 생성 실패');
};

// 내 예약 전체
export const postMyReservations = async () => {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${BASE_URL}/reservations/my`, {});
  return handleResponse(res, '예약 목록 조회 실패');
};

// 상태별 내 예약 (PENDING/APPROVED/REJECTED/CANCELLED/COMPLETED)
export const postMyReservationsByStatus = async (statusEnum) => {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${BASE_URL}/reservations/my/status/${statusEnum}`, {});
  return handleResponse(res, '예약 목록 조회 실패');
};

// 예약 상세
export const postReservationDetail = async (reservationIdx) => {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${BASE_URL}/reservations/${reservationIdx}`, {});
  return handleResponse(res, '예약 상세 조회 실패');
};

/* ==========
 *  예약 취소
 * ========== */

// 실서버 정책: POST-only 우선.
// 1) 우선 POST /reservations/cancel 로 시도
// 2) 없다면 DELETE /reservations/{id} 로 폴백
export const cancelReservation = async (reservationIdx) => {
  await ensureAccessTokenOrRedirect();

  // 1차: POST 방식(서버에 /cancel 엔드포인트가 구현돼있다면)
  try {
    const res1 = await postRetry401(`${BASE_URL}/reservations/cancel`, { reservationIdx });
    if (res1.ok) return handleResponse(res1, '예약 취소 실패');
    // ok=false면 아래로 캐치됨
    const payload = await parseJsonSafe(res1);
    const msg = typeof payload === 'object' ? (payload.message || '예약 취소 실패') : '예약 취소 실패';
    throw Object.assign(new Error(msg), { payload, status: res1.status });
  } catch (e) {
    // 2차: DELETE 폴백(백엔드가 원래 스펙 그대로인 경우)
    try {
      const res2 = await deleteRetry401(`${BASE_URL}/reservations/${reservationIdx}`);
      return handleResponse(res2, '예약 취소 실패');
    } catch (e2) {
      throw e2;
    }
  }
};

/* ===========================
 *  상수 (상태 enum – 경로용)
 * =========================== */
export const RESERVATION_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  CANCELLED: 'CANCELLED',
  COMPLETED: 'COMPLETED',
};

export default {
  postFacilities,
  postFacilitiesByType,
  postDailySchedule,
  postAvailability,
  postReservation,
  postMyReservations,
  postMyReservationsByStatus,
  postReservationDetail,
  cancelReservation,
  RESERVATION_STATUS,
};
