// src/api/adminReservation.js
// 관리자 시설예약 API (엔드포인트 한 곳에서 관리)
// UI/UX (탭 2개·페이지네이션·상세모달 승인/반려)

import { ensureAccessTokenOrRedirect } from "../utils/authFlow";
import { readAccessToken } from "../utils/readAccessToken";

/* -------------------------------------------------------------------------------------------------
 * [기존 유지] BASE_URL
 *  - 현재 prod 절대경로 사용. 이 설정은 유지한다.
 *  - (선택) 로컬/프록시 환경에서의 동작이 필요하면 아래 주석을 참고해 분기 추가.
 * ------------------------------------------------------------------------------------------------- */
const BASE_URL = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api";
// TODO(선택): 환경 분기를 쓰려면 아래처럼 교체
// const BASE_URL = import.meta.env.PROD ? "/BlueCrab-1.0.0/api" : "/api";

/* -------------------------------------------------------------------------------------------------
 * 관리자 prefix
 * ------------------------------------------------------------------------------------------------- */
const ADMIN = `${BASE_URL}/admin/reservations`;

/* -------------------------------------------------------------------------------------------------
 * 공통 fetch 유틸
 * ------------------------------------------------------------------------------------------------- */
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

/**
 * 서버가 HTML(로그인 페이지/에러페이지)을 반환할 때를 대비해
 * JSON이 아닐 경우 명확히 에러를 던진다. (UI에서 "Failed to fetch" 같은 모호함 방지)
 */
const parseJsonSafe = async (res, urlForMsg = "") => {
  const ct = res.headers.get("content-type") || "";
  if (ct.includes("application/json")) return res.json();

  const txt = await res.text();
  // 디버깅 보조용: 필요한 경우 콘솔에서 확인 가능
  // console.debug("[Non-JSON response]", urlForMsg, txt?.slice?.(0, 200));
  throw new Error(`API가 JSON을 반환하지 않았습니다. (url: ${urlForMsg || "unknown"})`);
};

// 401 → 토큰 갱신 후 1회 재시도 (POST 전용 래퍼)
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

// 공통 핸들러
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

/* ================================================================================================
 *  관리자 API (명세서 기반)
 *  - UI/UX: 탭 2개(승인 대기/전체), 페이지네이션(5개), 상세모달 승인/반려 버튼을 위한 최소 세트
 * ================================================================================================ */

/**
 * 승인 대기 목록
 * UI에서 탭=승인대기, 페이지네이션(page/size)로 호출.
 * - 엔드포인트: POST /api/admin/reservations/pending
 * - 바디: { page, size }
 */
export async function adminPendingList({ page = 0, size = 5 } = {}) {
  await ensureAccessTokenOrRedirect();
  const url = `${ADMIN}/pending`;
  const res = await postRetry401(url, { page, size });
  return handle(res, "승인 대기 목록 조회 실패", url);
}

/**
 *  예약 승인
 * - 엔드포인트: POST /api/admin/reservations/approve
 * - 바디: { reservationIdx, adminNote? }
 * - 상세모달의 "승인" 버튼에서 사용
 */
export async function adminApprove(reservationIdx, { adminNote } = {}) {
  await ensureAccessTokenOrRedirect();
  const url = `${ADMIN}/approve`;
  const res = await postRetry401(url, { reservationIdx, adminNote });
  return handle(res, "예약 승인 실패", url);
}

/**
 *  예약 반려
 * - 엔드포인트: POST /api/admin/reservations/reject
 * - 바디: { reservationIdx, rejectionReason } (반려 사유 필수)
 * - 상세모달의 "반려" 버튼에서 사용
 */
export async function adminReject(reservationIdx, { rejectionReason } = {}) {
  await ensureAccessTokenOrRedirect();
  const url = `${ADMIN}/reject`;
  const res = await postRetry401(url, { reservationIdx, rejectionReason });
  return handle(res, "예약 반려 실패", url);
}

/**
 * 예약 상세 조회 (관리자 모달에서 사용)
 * - 확정된 관리자용 상세 API가 없으므로, 사용자 API를 재사용:
 *   POST /api/reservations/{reservationIdx}
 * - 상세모달 오픈 시 호출, 승인/반려 후에는 목록만 갱신(모달은 닫기)
 */
export async function adminFetchReservationDetail(reservationIdx) {
  await ensureAccessTokenOrRedirect();
  const url = `${BASE_URL}/reservations/${reservationIdx}`;
  const res = await postRetry401(url, {}); // POST 호출
  return handle(res, "예약 상세 조회 실패", url);
}

/* -------------------------------------------------------------------------------------------------
 *  아래부터는 "전체 예약" 탭을 위한 확장 포인트 (아직 백엔드 확정 전이라 주석 처리)
 *  - UI에는 탭 버튼이 존재하지만, API가 준비될 때까지 비활성/더미 처리 중.
 *  - 백엔드가 확정되면 주석을 해제/수정하여 연결하면 된다.
 * ------------------------------------------------------------------------------------------------- */

// TODO(백엔드 확정 시 연결):
// 1) 전체 예약 목록(페이지네이션)
//    - 예상 엔드포인트 예시 A: POST /api/admin/reservations/search
//      바디: { status?, facilityIdx?, dateFrom?, dateTo?, page, size }
//    - 예상 엔드포인트 예시 B: POST /api/admin/reservations/all
//      바디: { page, size }
//
// export async function adminAllList({ page = 0, size = 5 } = {}) {
//   await ensureAccessTokenOrRedirect();
//   const url = `${ADMIN}/search`; // 또는 `${ADMIN}/all` 등 확정된 경로로 변경
//   const res = await postRetry401(url, { page, size });
//   return handle(res, "전체 예약 목록 조회 실패", url);
// }
//
// 2) (선택) 상태 필터 포함 검색
//    - UI에서 "전체 예약" 탭의 하위 필터(승인/반려/취소/완료 등)를 추가할 때 연결
//
// export async function adminSearchList({
//   status,        // "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED" | "COMPLETED"
//   facilityIdx,
//   dateFrom,      // 'YYYY-MM-DD'
//   dateTo,        // 'YYYY-MM-DD'
//   page = 0,
//   size = 5,
// } = {}) {
//   await ensureAccessTokenOrRedirect();
//   const url = `${ADMIN}/search`;
//   const res = await postRetry401(url, { status, facilityIdx, dateFrom, dateTo, page, size });
//   return handle(res, "예약 검색 실패", url);
// }

/* -------------------------------------------------------------------------------------------------
 * 참고/가이드
 * - "Failed to fetch"가 아닌 "API가 JSON을 반환하지 않았습니다"가 뜬다면:
 *   1) URL 프리픽스가 중복되거나(예: /BlueCrab-1.0.0/BlueCrab-1.0.0/api...), 
 *   2) 인증 실패로 로그인 HTML이 반환되는 경우가 많음.
 *   => 콘솔 네트워크 탭에서 Response 확인, 토큰/경로 둘 다 점검 필요.
 * ------------------------------------------------------------------------------------------------- */
