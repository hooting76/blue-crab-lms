// src/api/adminReservations.js
// BlueCrab LMS - 관리자 시설예약 API (POST-only)
// 변경점: 타임아웃(Promise.race) 제거, 관리자 리프레시 토큰 로직 유지

const BASE_API_ROOT = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api";
const ADMIN = `${BASE_API_ROOT}/admin/reservations`;

const getHeaders = (token) => ({
  Authorization: `Bearer ${token}`,
  "Content-Type": "application/json",
});

const parseJsonSafe = async (res, urlForMsg = "") => {
  const ct = res.headers.get("content-type") || "";
  if (ct.includes("application/json")) return res.json();
  const txt = await res.text();
  const e = new Error(`API가 JSON을 반환하지 않았습니다. (url: ${urlForMsg || "unknown"})`);
  e.nonJsonBody = txt;
  e.status = res.status;
  throw e;
};

/* ================================
 *  관리자 토큰 자동 갱신 로직 (유지)
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
 *  401 → 토큰 갱신 후 1회 재시도 (타임아웃 제거)
 * ================================ */
async function postRetry401(url, body) {
  let admin = JSON.parse(localStorage.getItem("Admin") || "{}");
  let token = admin?.accessToken || localStorage.getItem("accessToken");

  // 1차 시도
  let res = await fetch(url, {
    method: "POST",
    headers: getHeaders(token),
    body: JSON.stringify(body ?? {}),
  });

  // 401이면 리프레시 후 1회 재시도
  if (res.status === 401) {
    const newToken = await refreshAdminToken();
    res = await fetch(url, {
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
export async function adminPendingList({ page = 0, size = 10 } = {}) {
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

// 관리자 전체 검색 (statusList 비지정 시 모든 상태 포함)
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
