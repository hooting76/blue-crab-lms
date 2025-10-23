// 수강신청 API 어댑터 (문서 스펙 준수)
// - BASE 절대경로 사용 (vite.config.js 수정 불가)
// - Bearer 토큰: ensureAccessTokenOrRedirect()
// - "필요한 필드만" 정확히 전송 (studentId 제거)

import { ensureAccessTokenOrRedirect } from '../utils/authFlow';

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

// 500 에러 원인 메시지까지 보려고 text도 로깅
async function readJsonOrThrow(res) {
  const contentType = res.headers.get('content-type') || '';
  let data = null;

  try {
    if (contentType.includes('application/json')) {
      data = await res.json();
    } else {
      // 서버가 500에서 text/html을 줄 수도 있음
      const text = await res.text();
      data = text ? { message: text } : null;
    }
  } catch { /* ignore */ }

  if (!res.ok) {
    const msg = (data && data.message) ? String(data.message) : `HTTP_${res.status}`;
    const err = new Error(msg);
    err.status = res.status;
    err.payload = data;
    throw err;
  }
  return data ?? {};
}

/** 1) 개설 강의 조회 (필터 선택값만 전송) */
export async function fetchEligibleLectures({
  studentId,
  page = 0, size = 20,
  lecYear = null, lecSemester = null, lecMajor = null,
} = {}) {
  const token = await ensureAccessTokenOrRedirect();
  const payload = {
    studentId: Number(studentId),       // ← 이 엔드포인트는 studentId 사용
    page, size,
    ...(lecYear != null      ? { lecYear: Number(lecYear) }         : {}),
    ...(lecSemester != null  ? { lecSemester: Number(lecSemester) } : {}),
    ...(lecMajor != null     ? { lecMajor: Number(lecMajor) }       : {}),
  };

  const res = await fetch(BASE.eligible, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify(payload),
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
        mcode: s.lecMcode,
        mcodeDep: s.lecMcodeDep,
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
      currentPage: page, pageSize: size, totalElements: list.length, totalPages: 1,
    },
    studentInfo: data.studentInfo ?? null,
    raw: data,
  };
}

/** 2) 수강신청 (명세 그 자체: studentIdx + lecSerial 만) */
export async function enrollLecture({ studentIdx, lecSerial }) {
  const token = await ensureAccessTokenOrRedirect();
  const res = await fetch(BASE.enroll, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify({
      studentIdx: Number(studentIdx),
      lecSerial: String(lecSerial).trim(),
    }),
  });
  return readJsonOrThrow(res);
}

/** 3) 내 수강 목록
 *    - 서버가 배열([]) 또는 객체({ content: [] })로 줄 수 있어 둘 다 대응
 */
export async function fetchMyEnrollments({ studentIdx, enrolled = true, page = 0, size = 20 }) {
  const token = await ensureAccessTokenOrRedirect();
  const res = await fetch(BASE.list, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify({
      studentIdx: Number(studentIdx),
      enrolled: !!enrolled,
      page: Number(page),
      size: Number(size),
    }),
  });

  const data = await readJsonOrThrow(res);

  // ✅ 응답 유연 처리
  const items = Array.isArray(data)
    ? data
    : (Array.isArray(data.content) ? data.content : []);

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

  // 페이지 정보도 유연하게
  const pg = Array.isArray(data)
    ? { number: 0, size: list.length, totalElements: list.length, totalPages: 1 }
    : {
        number: data.number ?? page,
        size: data.size ?? size,
        totalElements: data.totalElements ?? list.length,
        totalPages: data.totalPages ?? 1,
      };

  return {
    list,
    page: pg.number,
    size: pg.size,
    totalElements: pg.totalElements,
    totalPages: pg.totalPages,
    raw: data,
  };
}

/** 4) 중복확인 (문서: studentIdx + lecSerial + checkEnrollment)
 *    - 문서형 실패 시, 내 수강목록으로 Fallback
 */
export async function checkEnrollment({ studentIdx, lecSerial }) {
  const token = await ensureAccessTokenOrRedirect();

  // 1) 문서 스펙대로 시도
  try {
    const res = await fetch(BASE.list, {
      method: 'POST',
      headers: getHeaders(token),
      body: JSON.stringify({ studentIdx: Number(studentIdx), lecSerial: String(lecSerial), checkEnrollment: true }),
    });
    const data = await readJsonOrThrow(res);

    if (typeof data?.enrolled === 'boolean') {
      return {
        enrolled: data.enrolled,
        studentIdx: data.studentIdx ?? studentIdx,
        lecSerial: data.lecSerial ?? lecSerial,
        raw: data,
      };
    }
    // 형태 다르면 아래 fallback 진행
  } catch {
    // ignore → fallback
  }

  // 2) Fallback: 내 수강 목록으로 직접 판정
  try {
    const res2 = await fetch(BASE.list, {
      method: 'POST',
      headers: getHeaders(token),
      body: JSON.stringify({ studentIdx: Number(studentIdx), enrolled: true, page: 0, size: 200 }),
    });
    const data2 = await readJsonOrThrow(res2);

    const items = Array.isArray(data2)
      ? data2
      : (Array.isArray(data2.content) ? data2.content : []);

    const found = items.some((it) => it.lecSerial === lecSerial);
    return { enrolled: found, studentIdx, lecSerial, raw: data2 };
  } catch (e2) {
    return { enrolled: false, studentIdx, lecSerial, error: e2.message };
  }
}

/** 5) 취소 */
export async function cancelEnrollmentByIdx({ enrollmentIdx }) {
  const token = await ensureAccessTokenOrRedirect();
  const res = await fetch(BASE.cancelById(enrollmentIdx), {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` },
  });
  return readJsonOrThrow(res);
}

/** 6) 안내문 (변경 없음) */
export async function viewCourseApplyNotice() {
  const res = await fetch(NOTICE.view, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({}),
  });
  return readJsonOrThrow(res);
}

export async function saveCourseApplyNotice({ message }) {
  const token = await ensureAccessTokenOrRedirect();
  const res = await fetch(NOTICE.save, {
    method: 'POST',
    headers: getHeaders(token),
    body: JSON.stringify({ message }),
  });
  return readJsonOrThrow(res);
}
