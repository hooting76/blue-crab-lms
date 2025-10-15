// src/api/adminReservations.js
// BlueCrab LMS - 관리자 시설예약 API (실서버 연동, POST-only, 401 자동 갱신 후 1회 재시도)

import { ensureAccessTokenOrRedirect } from "../utils/authFlow";
import { readAccessToken } from "../utils/readAccessToken";

//    - 프론트 서버로 가지 않고 항상 백엔드로 직행
const BASE_API_ROOT = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api";
const ADMIN = `${BASE_API_ROOT}/admin/reservations`;

const REQ_TIMEOUT_MS = 10000;

const getHeaders = (token) => ({
  Authorization: `Bearer ${token}`,
  "Content-Type": "application/json",
});

const fetchWithTimeout = (url, opts = {}, ms = REQ_TIMEOUT_MS) =>
  Promise.race([
    fetch(url, opts),
    new Promise((_, rej) => setTimeout(() => rej(new Error("요청 시간 초과")), ms)),
  ]);

const parseJsonSafe = async (res, urlForMsg = "") => {
  const ct = res.headers.get("content-type") || "";
  if (ct.includes("application/json")) return res.json();
  const txt = await res.text();
  throw new Error(`API가 JSON을 반환하지 않았습니다. (url: ${urlForMsg || "unknown"})`);
};

// 401 → 토큰 갱신 후 1회 재시도
async function postRetry401(url, body) {
  let token = readAccessToken();
  let res = await fetchWithTimeout(url, {
    method: "POST",
    headers: getHeaders(token),
    body: JSON.stringify(body ?? {}),
  });

  if (res.status === 401) {
    token = await ensureAccessTokenOrRedirect();
    res = await fetchWithTimeout(url, {
      method: "POST",
      headers: getHeaders(token),
      body: JSON.stringify(body ?? {}),
    });
  }
  return res;
}

async function handle(res, fallbackMessage = "요청 처리 실패", url = "") {
  const payload = await parseJsonSafe(res, url);
  if (!res.ok) {
    const msg = typeof payload === "object" ? payload?.message || fallbackMessage : fallbackMessage;
    const e = new Error(msg);
    e.status = res.status;
    e.payload = payload;
    throw e;
  }
  if (typeof payload === "object" && payload.success === false) {
    throw new Error(payload.message || fallbackMessage);
  }
  return payload; // { success, message, data }
}

/* ===========================
 *   관리자 API (구현된 것)
 * =========================== */

// 승인 대기 목록: POST /api/admin/reservations/pending
export async function adminPendingList({ page = 0, size = 5 } = {}) {
  await ensureAccessTokenOrRedirect();
  const url = `${ADMIN}/pending`;
  const res = await postRetry401(url, { page, size });
  return handle(res, "승인 대기 목록 조회 실패", url);
}

// 예약 승인: POST /api/admin/reservations/approve
export async function adminApprove(reservationIdx, { adminNote } = {}) {
  await ensureAccessTokenOrRedirect();
  const url = `${ADMIN}/approve`;
  const res = await postRetry401(url, { reservationIdx, adminNote });
  return handle(res, "예약 승인 실패", url);
}

// 예약 반려: POST /api/admin/reservations/reject
export async function adminReject(reservationIdx, { rejectionReason } = {}) {
  await ensureAccessTokenOrRedirect();
  const url = `${ADMIN}/reject`;
  const res = await postRetry401(url, { reservationIdx, rejectionReason });
  return handle(res, "예약 반려 실패", url);
}

// 예약 상세(유저 엔드포인트 재사용): POST /api/reservations/{reservationIdx}
export async function adminFetchReservationDetail(reservationIdx) {
  await ensureAccessTokenOrRedirect();
  const url = `${BASE_API_ROOT}/reservations/${reservationIdx}`;
  const res = await postRetry401(url, {}); // POST
  return handle(res, "예약 상세 조회 실패", url);
}

/* ===========================
 *   추후 추가 예정(백엔드 확정 대기)
 * =========================== */

// 전체 예약 조회(필터/페이지)
// 백엔드에서 /api/admin/reservations/search 열리면 주석 해제해서 사용
// export async function adminAllList({ page = 0, size = 5, status, facilityIdx } = {}) {
//   await ensureAccessTokenOrRedirect();
//   const url = `${ADMIN}/search`;
//   const res = await postRetry401(url, { page, size, status, facilityIdx });
//   return handle(res, "전체 예약 조회 실패", url);
// }
