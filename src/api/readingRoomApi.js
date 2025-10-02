// src/api/readingRoomApi.js
// BlueCrab LMS - 열람실 API 어댑터 (실서버 연동 / POST-only / 401 자동 갱신 후 1회 재시도)
// 프론트 표준 DTO로 매핑: { id:number, seat_no:number, state:0|1 }  // 0=비어있음, 1=사용중

import { ensureAccessTokenOrRedirect } from '../utils/authFlow';
import { readAccessToken } from '../utils/readAccessToken';

// 절대 URL 고정(바꾸지 말 것)
const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/reading-room';
const REQ_TIMEOUT_MS = 10000; // 10s

// 공통 헤더: 모든 엔드포인트가 JSON 바디 요구(명세 기준)
const getHeaders = (token) => ({
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json',
});

// 타임아웃 래퍼
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

// 401이면 1회 토큰 갱신 후 재시도 (POST 전용)
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
  const payload = await parseJsonSafe(res);

  // [FIX] 4xx/5xx인 경우에도 payload를 에러 객체에 부착해서
  //       catch에서 e?.payload?.errorCode 매핑이 가능하도록 한다.
  if (!res.ok) {
    const msg = typeof payload === 'object' ? (payload.message || fallbackMsg) : fallbackMsg;
    const err = new Error(msg);
    // [FIX] 항상 payload를 달아줌(문자열이면 raw로 래핑)
    err.payload = (typeof payload === 'object') ? payload : { raw: payload };
    err.status = res.status; // 상태코드도 함께
    throw err;
  }

  // [기존 유지] success=false 응답도 에러로 처리
  if (typeof payload === 'object' && payload.success === false) {
    const msg = payload.message || payload.errorCode || fallbackMsg;
    const err = new Error(msg);
    err.payload = payload;            // [확인] 이 경로도 payload 유지
    err.status = res.status || 200;
    throw err;
  }
  return payload; // { success, message, data, ... } 또는 텍스트
}

// 서버 → 프론트 표준 DTO 매핑 (명세서 기반)
// 서버(status)의 seats 원소: { seatNumber, isOccupied, ... }
function mapStatusToSeatDTO(seat) {
  const n = seat.seatNumber;
  const occupied = !!seat.isOccupied;
  return {
    id: n,
    seat_no: n,
    state: occupied ? 1 : 0, // 프론트 표준: 1=사용중(핑크), 0=비어있음(초록)
  };
}

// ─────────────────────────────────────────────
// 1) 좌석 조회 (명세: POST /status)
// 반환: 프론트 표준 좌석배열 [{ id, seat_no, state }, ...]
// ─────────────────────────────────────────────
export async function getSeats() {
  try {
    await ensureAccessTokenOrRedirect();
    const res = await postRetry401('/status', {}); // body는 {} 고정
    const payload = await ensureOkJson(res, '열람실 현황 조회에 실패했습니다.');

    const seats = Array.isArray(payload?.data?.seats) ? payload.data.seats : [];
    return seats.map(mapStatusToSeatDTO);
  } catch (e) {
    console.error('열람실 좌석 조회 에러:', e);
    throw e;
  }
}

// ─────────────────────────────────────────────
// 2) 좌석 예약 (명세: POST /reserve  { seatNumber })
// 성공: { ok:true }
// 실패: { ok:false, code: 'occupied' | 'already_reserved' | 'invalid_seat' | 'unknown' }
// ─────────────────────────────────────────────
export async function reserveSeat(seatId) {
  try {
    if (!Number.isInteger(seatId) || seatId < 1) {
      return { ok: false, code: 'invalid_seat' };
    }
    await ensureAccessTokenOrRedirect();
    const res = await postRetry401('/reserve', { seatNumber: seatId });
    const payload = await ensureOkJson(res, '좌석 예약에 실패했습니다.');

    // success true면 통상 예약 성공
    return { ok: true, data: payload?.data || null };
  } catch (e) {
    // 명세서 errorCode 매핑
    const code =
      e?.payload?.errorCode === 'SEAT_ALREADY_OCCUPIED' ? 'occupied' :
      e?.payload?.errorCode === 'USER_ALREADY_RESERVED' ? 'already_reserved' :
      e?.payload?.errorCode === 'INVALID_SEAT_NUMBER'   ? 'invalid_seat' :
      e?.payload?.errorCode === 'MISSING_REQUIRED_PARAMETER' ? 'missing_param' :
      'unknown';
    return { ok: false, code, message: e.message };
  }
}

// ─────────────────────────────────────────────
// 3) 좌석 해제/퇴실 (명세: POST /checkout { seatNumber })
// 성공: { ok:true, data }
// 실패: { ok:false, code:'unauthorized_seat' | 'invalid_seat' | 'unknown' }
// ─────────────────────────────────────────────
export async function releaseSeat(seatId) {
  try {
    if (!Number.isInteger(seatId) || seatId < 1) {
      return { ok: false, code: 'invalid_seat' };
    }
    await ensureAccessTokenOrRedirect();
    const res = await postRetry401('/checkout', { seatNumber: seatId });
    const payload = await ensureOkJson(res, '퇴실 처리에 실패했습니다.');
    return { ok: true, data: payload?.data || null };
  } catch (e) {
    const code =
      e?.payload?.errorCode === 'UNAUTHORIZED_SEAT'   ? 'unauthorized_seat' :
      e?.payload?.errorCode === 'INVALID_SEAT_NUMBER' ? 'invalid_seat' :
      'unknown';
    return { ok: false, code, message: e.message };
  }
}

// ─────────────────────────────────────────────
// 4) 내 예약 조회
// POST /api/reading-room/my-reservation
// 성공(data=null 이면 예약 없음)
// 반환 형태(프론트 표준):
//  - 예약 있음: { ok:true, data:{ id, seat_no, state:1, endTime, remainingMinutes } }
//  - 예약 없음: { ok:true, data:null }
//  - 실패:     { ok:false, message }
// ─────────────────────────────────────────────
export async function getMyReservation() {
  try {
    await ensureAccessTokenOrRedirect();

    // 명세 경로 그대로 사용
    const res = await postRetry401('/my-reservation', {});
    const payload = await ensureOkJson(res, '내 예약 조회에 실패했습니다.');

    // 예약 없음
    if (!payload?.data) {
      return { ok: true, data: null };
    }

    // 예약 있음
    const seatNumber = payload.data.seatNumber;
    const isOccupied = !!payload.data.isOccupied;

    // 표준 DTO 변환: id/seat_no/state(1=사용중)
    const dto = {
      id: seatNumber,
      seat_no: seatNumber,
      state: isOccupied ? 1 : 0,
      // 편의 필드(있으면 UI에 바로 쓸 수 있음)
      endTime: payload.data.endTime ?? null,
      remainingMinutes: Number.isFinite(payload.data.remainingMinutes)
        ? payload.data.remainingMinutes
        : null,
    };

    return { ok: true, data: dto };
  } catch (e) {
    console.error('내 예약 조회 에러:', e);
    return { ok: false, message: e.message };
  }
}
