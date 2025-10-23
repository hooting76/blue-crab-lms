// 수강신청 API 어댑터 (문서 스펙 준수)
// - BASE 절대경로 사용 (vite.config.js 수정 불가 환경)
// - Bearer 토큰: ensureAccessTokenOrRedirect()
// - eligible: lecYear/lecSemester/lecMajor (선택)만 전송, name 미전송
// - 취소: DELETE /enrollments/{enrollmentIdx}
// - 안내문: POST https://.../notice/course-apply/{view|save}

import { ensureAccessTokenOrRedirect } from '../utils/authFlow';

// 절대 베이스 URL 
const BASE_API_ROOT    = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
const BASE_NOTICE_ROOT = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/notice';

const BASE = {
  eligible:   `${BASE_API_ROOT}/lectures/eligible`,
  enroll:     `${BASE_API_ROOT}/enrollments/enroll`,
  list:       `${BASE_API_ROOT}/enrollments/list`,
  cancelById: (enrollmentIdx) => `${BASE_API_ROOT}/enrollments/${enrollmentIdx}`,
};

const NOTICE = {
  view: `${BASE_NOTICE_ROOT}/course-apply/view`,
  save: `${BASE_NOTICE_ROOT}/course-apply/save`,
};

const getHeaders = (token) => ({
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json',
});

function compact(obj = {}) {
  const out = {};
  Object.keys(obj).forEach((k) => {
    const v = obj[k];
    if (v !== null && v !== undefined && v !== '') out[k] = v;
  });
  return out;
}

async function readJsonOrThrow(res) {
  let data = null;
  try { data = await res.json(); } catch {}
  if (!res.ok) {
    const msg = (data && data.message) ? data.message : `HTTP_${res.status}`;
    const err = new Error(msg);
    err.status = res.status;
    err.payload = data;
    throw err;
  }
  return data ?? {};
}

/** 1) 개설 강의(수강 가능) 조회 */
export async function fetchEligibleLectures({
  studentId,
  page = 0,
  size = 20,
  lecYear = null,
  lecSemester = null,
  lecMajor = null,   // 1=전공, 0=교양, null=전체(전송X)
} = {}) {
  const token = await ensureAccessTokenOrRedirect();

  // name(교과목명) 검색은 백엔드 미지원 → 절대 전송 X
  const payload = compact({ studentId, page, size, lecYear, lecSemester, lecMajor });

  const res = await fetch(BASE.eligible, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify(payload),
    // credentials: 'include', // 필요 시
    // mode: 'cors',           // 기본값 cors
  });

  const data = await readJsonOrThrow(res);

  const list = Array.isArray(data.eligibleLectures)
    ? data.eligibleLectures.map((s) => ({
        id: s.lecSerial,
        name: s.lecTit,
        credit: s.lecPoint,
        prof: s.lecProfName ?? s.lecProf ?? '',
        time: s.lecTime,
        enrolled: s.lecCurrent ?? 0,
        capacity: s.lecMany ?? 0,
        mcode: s.lecMcode,       // 학부 코드
        mcodeDep: s.lecMcodeDep, // 학과 코드
        eligible: s.isEligible ?? true,
        eligibilityReason: s.eligibilityReason ?? '',
      }))
    : [];

  return {
    list,
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
    raw: data,
  };
}

/** 2) 수강신청 */
export async function enrollLecture({ studentIdx, lecSerial }) {
  const token = await ensureAccessTokenOrRedirect();
  const res = await fetch(BASE.enroll, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify({ studentIdx, lecSerial }),
  });
  return readJsonOrThrow(res); // 성공 시 엔티티 그대로
}

/** 3) 내 수강 목록 (페이지네이션) */
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
    enrollmentIdx: s.enrollmentIdx, // ← 취소에 쓰임
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

/** 4) 중복 확인 */
export async function checkEnrollment({ studentIdx, lecSerial }) {
  const token = await ensureAccessTokenOrRedirect();
  const res = await fetch(BASE.list, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify({ studentIdx, lecSerial, checkEnrollment: true }),
  });
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

/** 5) 수강신청 취소 (DELETE /enrollments/{enrollmentIdx}) */
export async function cancelEnrollmentByIdx({ enrollmentIdx }) {
  const token = await ensureAccessTokenOrRedirect();
  const res = await fetch(BASE.cancelById(enrollmentIdx), {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` },
  });
  return readJsonOrThrow(res);
}

/** 6) 안내문 조회/저장 (POST 기반) */
export async function viewCourseApplyNotice() {
  // 공개 API (토큰 불필요)
  const res = await fetch(NOTICE.view, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({}),
  });
  return readJsonOrThrow(res); // { success, message, updatedAt, updatedBy }
}

export async function saveCourseApplyNotice({ message }) {
  const token = await ensureAccessTokenOrRedirect();
  const res = await fetch(NOTICE.save, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify({ message }),
  });
  return readJsonOrThrow(res); // { success, message, data:{...} }
}
