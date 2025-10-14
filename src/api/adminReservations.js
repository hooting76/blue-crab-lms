// 관리자 시설예약 API (엔드포인트 한 곳에서 관리)
// 필요에 따라 BASE_URL_* 만 실제 서버 경로에 맞게 수정하세요.

import { ensureAccessTokenOrRedirect } from "../utils/authFlow";
import { readAccessToken } from "../utils/readAccessToken";

const BASE_URL = "/BlueCrab-1.0.0/api";                // 공용 prefix
const ADMIN = `${BASE_URL}/admin/reservations`;        // 관리자 prefix 가정

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
 *   관리자 API (가정된 경로)
 *   실제 백엔드 경로와 다르면 여기만 바꾸면 됨
 * =========================== */

// 대시보드 카운터(승인대기/오늘/이번주/이번달)
export async function adminStats() {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${ADMIN}/stats`, {});
  return handle(res, "통계 조회 실패");
}

// 승인 대기 목록 (간단 페이징 옵션)
export async function adminPendingList({ page = 0, size = 20 } = {}) {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${ADMIN}/pending`, { page, size });
  return handle(res, "승인 대기 목록 조회 실패");
}

// 전체 예약 검색(상태/시설/키워드/기간/페이징)
export async function adminSearchList({
  status,          // PENDING/APPROVED/REJECTED/CANCELLED/COMPLETED | undefined
  facilityIdx,     // number | undefined
  query,           // string | undefined (이름/학번/시설명)
  dateFrom,        // 'YYYY-MM-DD' | undefined
  dateTo,          // 'YYYY-MM-DD' | undefined
  page = 0,
  size = 20,
} = {}) {
  await ensureAccessTokenOrRedirect();
  const body = { status, facilityIdx, query, dateFrom, dateTo, page, size };
  const res = await postRetry401(`${ADMIN}/search`, body);
  return handle(res, "예약 목록 조회 실패");
}

// 예약 상세
export async function adminGetDetail(reservationIdx) {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${ADMIN}/${reservationIdx}`, {});
  return handle(res, "예약 상세 조회 실패");
}

// 승인
export async function adminApprove(reservationIdx, { adminNote } = {}) {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${ADMIN}/${reservationIdx}/approve`, { adminNote });
  return handle(res, "예약 승인 실패");
}

// 반려
export async function adminReject(reservationIdx, { rejectionReason } = {}) {
  await ensureAccessTokenOrRedirect();
  const res = await postRetry401(`${ADMIN}/${reservationIdx}/reject`, { rejectionReason });
  return handle(res, "예약 반려 실패");
}
