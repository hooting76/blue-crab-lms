// src/api/adminReservations.js
// 관리자 시설예약 API (엔드포인트 한 곳에서 관리)
//  - 관리자 대기목록/승인/반려: /api/admin/reservations/*
//  - 상세 조회는 사용자용 /api/reservations/{id} 재사용
//  - 토큰 자동 확인/401 재시도/에러 공통 처리

import { ensureAccessTokenOrRedirect } from "../utils/authFlow";
import { readAccessToken } from "../utils/readAccessToken";

const BASE_URL = "/api";
const ADMIN_BASE = `${BASE_URL}/admin/reservations`;

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

const parseJsonSafe = async (res) => {
  const ct = res.headers.get("content-type") || "";
  if (ct.includes("application/json")) return res.json();
  return res.text();
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

async function handle(res, fallbackMessage = "요청 처리 실패") {
  const payload = await parseJsonSafe(res);
  if (!res.ok) {
    const msg =
      typeof payload === "object" ? payload?.message || fallbackMessage : fallbackMessage;
    const e = new Error(msg);
    e.status = res.status;
    e.payload = payload;
    throw e;
  }
  if (typeof payload === "object" && payload.success === false) {
    throw new Error(payload.message || fallbackMessage);
  }
  return payload; // ApiResponse<T> 그대로 반환
}

/* ===========================
 *   관리자 API (명세에 존재)
 * =========================== */

// 승인 대기 목록
// POST /api/admin/reservations/pending
export async function adminPendingList({ page = 0, size = 20 } = {}) {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${ADMIN_BASE}/pending`, { page, size });
  return handle(res, "승인 대기 목록 조회 실패");
}

// 승인 처리
// POST /api/admin/reservations/approve
// body: { reservationIdx, adminNote? }
export async function adminApprove({ reservationIdx, adminNote } = {}) {
  if (!reservationIdx) throw new Error("reservationIdx는 필수입니다.");
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${ADMIN_BASE}/approve`, { reservationIdx, adminNote });
  return handle(res, "예약 승인 실패");
}

// 반려 처리
// POST /api/admin/reservations/reject
// body: { reservationIdx, rejectionReason }
export async function adminReject({ reservationIdx, rejectionReason } = {}) {
  if (!reservationIdx) throw new Error("reservationIdx는 필수입니다.");
  if (!rejectionReason) throw new Error("반려 사유는 필수입니다.");
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${ADMIN_BASE}/reject`, { reservationIdx, rejectionReason });
  return handle(res, "예약 반려 실패");
}

/* ======================================
 *   상세 조회는 사용자 API 임시 재사용
 * ====================================== */

// 사용자 상세 API 재사용
// POST /api/reservations/{reservationIdx}
export async function adminFetchReservationDetail(reservationIdx) {
  if (!reservationIdx) throw new Error("reservationIdx는 필수입니다.");
  await ensureAccessTokenOrRedirect();
  const token = readAccessToken();
  const res = await fetchWithTimeout(`${BASE_URL}/reservations/${reservationIdx}`, {
    method: "POST",
    headers: getHeaders(token),
  });
  return handle(res, "예약 상세 조회 실패");
}
// (관리자용 상세 조회 API가 명세에 없으므로 사용자용 재사용. 추후 별도 구현 가능)