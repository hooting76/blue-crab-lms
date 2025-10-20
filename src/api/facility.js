// src/api/facility.js
// BlueCrab LMS - 시설 API (실서버 연동, POST-only, 401 자동 갱신 1회 재시도)
// *타임아웃 제거* 버전

import { ensureAccessTokenOrRedirect } from '../utils/authFlow';
import { readAccessToken } from '../utils/readAccessToken';

/* 절대 베이스 URL */
const BASE_API_ROOT = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

// 공통 헤더
const getHeaders = (accessToken) => ({
  Authorization: `Bearer ${accessToken}`,
  'Content-Type': 'application/json',
});

/* 안전 JSON 파싱 */
const parseJsonSafe = async (res) => {
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) return res.json();
  const body = await res.text();
  const err = new Error('API가 JSON이 아닌 응답을 반환했습니다.');
  err.nonJsonBody = body;
  err.status = res.status;
  err.contentType = ct;
  throw err;
};

// 401 → 토큰 갱신 후 1회 재시도 (POST 기본)
// (타임아웃/Promise.race 제거)
async function postRetry401(url, bodyObj) {
  let token = readAccessToken();
  let res = await fetch(url, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify(bodyObj ?? {}),
  });

  if (res.status === 401) {
    token = await ensureAccessTokenOrRedirect();
    res = await fetch(url, {
      method: 'POST',
      headers: getHeaders(token),
      body: JSON.stringify(bodyObj ?? {}),
    });
  }
  return res;
}

// 공통 응답 처리: { success, message, data, ... }
async function handleResponse(res, defaultErrMsg = '요청 처리 실패') {
  const payload = await parseJsonSafe(res);

  if (!res.ok) {
    const msg = typeof payload === 'object' ? payload?.message || defaultErrMsg : defaultErrMsg;
    const err = new Error(msg);
    err.payload = payload;
    err.status = res.status;
    throw err;
  }
  if (typeof payload === 'object' && payload.success === false) {
    const err = new Error(payload.message || defaultErrMsg);
    err.payload = payload;
    throw err;
  }
  return payload; // ApiResponse<T>
}

/*시설 조회 & 가용성 / 일정 */
// 활성 시설 목록
export const postFacilities = async () => {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${BASE_API_ROOT}/facilities`, {});
  return handleResponse(res, '시설 목록 조회 실패');
};

// (선택) 유형별 시설 목록
export const postFacilitiesByType = async (facilityType) => {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${BASE_API_ROOT}/facilities/type/${facilityType}`, {});
  return handleResponse(res, '시설 목록 조회 실패');
};

// 일일 일정 조회 (09~18 시간대 표시용)
export const postDailySchedule = async (facilityIdx, dateYMD) => {
  await ensureAccessTokenOrRedirect();
  const url = `${BASE_API_ROOT}/facilities/${facilityIdx}/daily-schedule?date=${encodeURIComponent(
    dateYMD
  )}`;
  const res = await postRetry401(url, {}); // 서버는 POST-only
  return handleResponse(res, '일일 일정 조회 실패');
};

// 가용성 확인
export const postAvailability = async (facilityIdx, startTime, endTime) => {
  await ensureAccessTokenOrRedirect();
  const url = `${BASE_API_ROOT}/facilities/${facilityIdx}/availability?startTime=${encodeURIComponent(
    startTime
  )}&endTime=${encodeURIComponent(endTime)}`;
  const res = await postRetry401(url, {}); // 서버는 POST-only
  return handleResponse(res, '가용성 확인 실패');
};

/* 예약 생성/조회 */
export const postReservation = async (payload) => {
  // payload: { facilityIdx, startTime, endTime, partySize, purpose, requestedEquipment }
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${BASE_API_ROOT}/reservations`, payload);
  return handleResponse(res, '예약 생성 실패');
};

// 내 예약 전체
export const postMyReservations = async () => {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${BASE_API_ROOT}/reservations/my`, {});
  return handleResponse(res, '예약 목록 조회 실패');
};

// 상태별 내 예약 (PENDING/APPROVED/REJECTED/CANCELLED/COMPLETED)
export const postMyReservationsByStatus = async (statusEnum) => {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${BASE_API_ROOT}/reservations/my/status/${statusEnum}`, {});
  return handleResponse(res, '예약 목록 조회 실패');
};

// 예약 상세
export const postReservationDetail = async (reservationIdx) => {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${BASE_API_ROOT}/reservations/${reservationIdx}`, {});
  return handleResponse(res, '예약 상세 조회 실패');
};

/* 예약 취소 (DELETE) */
export const cancelReservation = async (reservationIdx) => {
  await ensureAccessTokenOrRedirect();
  const token = readAccessToken();

  const res = await fetch(`${BASE_API_ROOT}/reservations/${reservationIdx}`, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  const payload = await parseJsonSafe(res);
  if (!res.ok) {
    const msg = payload?.message || '예약 취소 실패';
    const err = new Error(msg);
    err.payload = payload;
    throw err;
  }
  return payload;
};

/* 상수 (상태 enum – 경로용)*/
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
