// 수강신청 API 어댑터 (문서 스펙 준수 / timeout 미사용)
// - 절대경로(BASE_API_ROOT) 사용
// - Bearer 토큰: ensureAccessTokenOrRedirect() 사용
// - 응답/에러: 문서 포맷에 맞춰 처리

import { ensureAccessTokenOrRedirect } from '../utils/authFlow';

// 절대 베이스 URL 
const BASE_API_ROOT = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

const BASE = {
  eligible: `${BASE_API_ROOT}/lectures/eligible`,
  enroll:   `${BASE_API_ROOT}/enrollments/enroll`,
  list:     `${BASE_API_ROOT}/enrollments/list`,
};

const getHeaders = (token) => ({
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json',
});

// 공통 JSON 파서(+에러 메시지 규격화)
async function readJsonOrThrow(res) {
  let data = null;
  try { data = await res.json(); } catch {/* empty */}
  if (!res.ok) {
    // 문서: { success:false, message:"..." }
    const msg = (data && data.message) ? data.message : `HTTP_${res.status}`;
    const err = new Error(msg);
    err.status = res.status;
    err.payload = data;
    throw err;
  }
  return data ?? {};
}

/* ─────────────────────────────────────────────────────────
 * 1) 수강 가능 강의 조회: POST /api/lectures/eligible
 * Request: { studentId:number, page?:0, size?:20 }
 * Response: {
 *   eligibleLectures:[{lecSerial, lecTit, lecProf|lecProfName, lecPoint, lecTime, lecCurrent, lecMany, ...}],
 *   totalCount, eligibleCount, ineligibleCount,
 *   pagination:{ currentPage, pageSize, totalElements, totalPages },
 *   studentInfo:{ ... }
 * }
 * 프론트 표준형(list)도 함께 반환
 * ───────────────────────────────────────────────────────── */
export async function fetchEligibleLectures({ studentId, page = 0, size = 20 }) {
  const token = await ensureAccessTokenOrRedirect();

  const res = await fetch(BASE.eligible, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify({ studentId, page, size }),
  });

  const data = await readJsonOrThrow(res);

  const list = Array.isArray(data.eligibleLectures) ? data.eligibleLectures.map((s) => ({
    // 표준 DTO (UI 바인딩용)
    id: s.lecSerial,
    name: s.lecTit,
    credit: s.lecPoint,
    prof: s.lecProfName ?? s.lecProf ?? '',
    time: s.lecTime,
    enrolled: s.lecCurrent ?? 0,
    capacity: s.lecMany ?? 0,
    mcode: s.lecMcode,
    mcodeDep: s.lecMcodeDep,
    min: s.lecMin ?? 0,
    eligible: s.isEligible ?? true,
    eligibilityReason: s.eligibilityReason ?? '',
  })) : [];

  return {
    list,                                   // UI 표준 리스트
    counts: {
      totalCount: data.totalCount ?? list.length,
      eligibleCount: data.eligibleCount ?? 0,
      ineligibleCount: data.ineligibleCount ?? 0,
    },
    pagination: data.pagination ?? {
      currentPage: page,
      pageSize: size,
      totalElements: list.length,
      totalPages: 1,
    },
    studentInfo: data.studentInfo ?? null,
    raw: data,                               // 원본 그대로 필요 시 사용
  };
}

/* ─────────────────────────────────────────────────────────
 * 2) 수강신청: POST /api/enrollments/enroll
 * Request: { studentIdx:number, lecSerial:string }
 * Response(성공): EnrollmentExtendedTbl 엔티티 그대로
 * Response(에러): { success:false, message:"..." }
 * ───────────────────────────────────────────────────────── */
export async function enrollLecture({ studentIdx, lecSerial }) {
  const token = await ensureAccessTokenOrRedirect();

  const res = await fetch(BASE.enroll, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify({ studentIdx, lecSerial }),
  });

  // 성공 시 엔티티 그대로 반환 (래퍼 없음)
  return readJsonOrThrow(res);
}

/* ─────────────────────────────────────────────────────────
 * 3) 수강목록 조회: POST /api/enrollments/list
 * Request: { studentIdx:number, enrolled:true, page?:0, size?:20 }
 * Response(성공): Page 객체 { content:[], totalElements, totalPages, size, number }
 * ───────────────────────────────────────────────────────── */
export async function fetchMyEnrollments({ studentIdx, enrolled = true, page = 0, size = 20 }) {
  const token = await ensureAccessTokenOrRedirect();

  const res = await fetch(BASE.list, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify({ studentIdx, enrolled, page, size }),
  });

  const data = await readJsonOrThrow(res);
  const items = Array.isArray(data.content) ? data.content : [];

  const list = items.map((s) => ({
    id: s.lecSerial,
    name: s.lecTit,
    credit: s.lecPoint,
    prof: s.lecProfName ?? '',
    time: s.lecTime,
    status: s.status ?? 'ACTIVE',
    enrollmentIdx: s.enrollmentIdx,
    enrollmentDate: s.enrollmentDate,
    lecIdx: s.lecIdx,
  }));

  return {
    list,
    page: data.number ?? page,
    size: data.size ?? size,
    totalElements: data.totalElements ?? list.length,
    totalPages: data.totalPages ?? 1,
    raw: data,
  };
}

/* ─────────────────────────────────────────────────────────
 * 4) 수강신청 여부 확인 (중복확인): POST /api/enrollments/list
 * Request: { studentIdx:number, lecSerial:string, checkEnrollment:true }
 * Response(성공): { enrolled:boolean, studentIdx, lecSerial }
 * ───────────────────────────────────────────────────────── */
export async function checkEnrollment({ studentIdx, lecSerial }) {
  const token = await ensureAccessTokenOrRedirect();

  const res = await fetch(BASE.list, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify({ studentIdx, lecSerial, checkEnrollment: true }),
  });

  // 문서상: 실패 시에도 200이 아닐 수 있으나, 안전하게 false로 처리할 수 있도록 try/catch
  try {
    const data = await readJsonOrThrow(res);
    return {
      enrolled: !!data.enrolled,
      studentIdx: data.studentIdx ?? studentIdx,
      lecSerial: data.lecSerial ?? lecSerial,
      raw: data,
    };
  } catch (e) {
    return { enrolled: false, studentIdx, lecSerial, error: e.message };
  }
}

// 취소 (서버 스펙에 맞게 활성화)
// export async function cancelEnrollment({ studentIdx, lecSerial }) {
//   const token = await ensureAccessTokenOrRedirect();
//   const res = await fetchWithTimeout(BASE.cancel, {
//     method: 'POST', // 혹은 DELETE/PUT 등 서버 정의에 맞춤
//     headers: getHeaders(token),
//     body: JSON.stringify({ studentIdx, lecSerial }),
//   });
//   return ensureOkJson(res);
// }
