// src/api/adminReservations.js
// BlueCrab LMS - 관리자 시설예약 API (POST-only, 토큰 자동 리프레시 내장)

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

/* ================================
 *  관리자 토큰 자동 갱신 로직
 * ================================ */
async function refreshAdminToken() {
  const adminData = JSON.parse(localStorage.getItem("Admin") || "{}");
  const refreshToken = adminData?.refreshToken;

  if (!refreshToken) throw new Error("관리자 리프레시 토큰이 없습니다.");

  const url = `${BASE_API_ROOT.replace("/api", "")}/api/admin/auth/refresh`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });

  const data = await res.json();
  if (!res.ok || !data?.success) {
    throw new Error(data?.message || "관리자 토큰 재발급 실패");
  }

  // 새 토큰 저장
  const updatedAdmin = {
    ...adminData,
    accessToken: data?.data?.accessToken,
    refreshToken: data?.data?.refreshToken || adminData.refreshToken,
  };

  localStorage.setItem("Admin", JSON.stringify(updatedAdmin));
  localStorage.setItem("accessToken", updatedAdmin.accessToken);
  return updatedAdmin.accessToken;
}

/* ================================
 *  401 → 새 토큰으로 재시도
 * ================================ */
async function postRetry401(url, body) {
  let admin = JSON.parse(localStorage.getItem("Admin") || "{}");
  let token = admin?.accessToken || localStorage.getItem("accessToken");

  let res = await fetchWithTimeout(url, {
    method: "POST",
    headers: getHeaders(token),
    body: JSON.stringify(body ?? {}),
  });

  // 토큰 만료 시 새로 갱신 후 1회 재시도
  if (res.status === 401) {
    const newToken = await refreshAdminToken();
    res = await fetchWithTimeout(url, {
      method: "POST",
      headers: getHeaders(newToken),
      body: JSON.stringify(body ?? {}),
    });
  }

  return res;
}

/* ================================
 * 응답 처리 공통
 * ================================ */
async function handle(res, fallbackMessage = "요청 처리 실패", url = "") {
  const payload = await parseJsonSafe(res, url);
  if (!res.ok || payload?.success === false) {
    const msg = payload?.message || fallbackMessage;
    const e = new Error(msg);
    e.status = res.status;
    e.payload = payload;
    throw e;
  }
  return payload;
}

/* ================================
 * 관리자 예약 API들
 * ================================ */

// 승인 대기 목록
export async function adminPendingList({ page = 0, size = 5 } = {}) {
  const url = `${ADMIN}/pending`;
  const res = await postRetry401(url, { page, size });
  return handle(res, "승인 대기 목록 조회 실패", url);
}

// 예약 승인
export async function adminApprove(reservationIdx, { adminNote } = {}) {
  const url = `${ADMIN}/approve`;
  const res = await postRetry401(url, { reservationIdx, adminNote });
  return handle(res, "예약 승인 실패", url);
}

// 예약 반려
export async function adminReject(reservationIdx, { rejectionReason } = {}) {
  const url = `${ADMIN}/reject`;
  const res = await postRetry401(url, { reservationIdx, rejectionReason });
  return handle(res, "예약 반려 실패", url);
}

// 관리자 전체 검색
export async function adminSearchReservations(filters = {}) {
  const url = `${ADMIN}/search`;
  const clean = Object.fromEntries(
    Object.entries(filters).filter(([_, v]) =>
      Array.isArray(v) ? v.length > 0 : v !== undefined && v !== null && v !== ""
    )
  );
  const res = await postRetry401(url, clean);
  return handle(res, "예약 검색 실패", url);
}

// 관리자 상세 조회
export async function getAdminReservationDetail(reservationIdx) {
  const url = `${ADMIN}/${reservationIdx}`;
  const res = await postRetry401(url, {});
  return handle(res, "관리자 예약 상세 조회 실패", url);
}
