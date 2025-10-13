// BlueCrab LMS - 시간 유틸 (순수 함수 모음, 네트워크 없음)
// - 서버 형식: "yyyy-MM-dd HH:mm:ss" 고정
// - 사업시간(예약 가능): 09:00 ~ 18:00 (마지막 슬롯은 17:00~18:00)
// - 같은 날 예약 강제

/* ===========================
 *  상수/헬퍼
 * =========================== */

export const BUSINESS_START_HOUR = 9;   // 포함
export const BUSINESS_END_HOUR   = 18;  // "종료 시각"의 상한 (미포함 경계 아님, 18:00까지 허용)

const pad2 = (n) => String(n).padStart(2, "0");

/** 오늘 YYYY-MM-DD */
export const toYMD = (d) =>
  `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

/** 서버 전송용 YYYY-MM-DD HH:mm:ss */
export const toYMDHMS = (d) =>
  `${toYMD(d)} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;

/** "YYYY-MM-DD" → Date(로컬 00:00:00) */
export function parseYMD(ymd) {
  // 브라우저에서 "2025-01-10" 형태는 로컬타임 기준 Date로 파싱됨
  const [y, m, d] = String(ymd || "").split("-").map((v) => parseInt(v, 10));
  if (!y || !m || !d) return null;
  return new Date(y, m - 1, d, 0, 0, 0);
}

/** "YYYY-MM-DD HH:mm:ss" → Date(로컬) */
export function parseYMDHMS(str) {
  const [date, time] = String(str || "").split(" ");
  if (!date || !time) return null;
  const [y, m, d] = date.split("-").map((v) => parseInt(v, 10));
  const [hh, mm, ss] = time.split(":").map((v) => parseInt(v, 10));
  if (!y || !m || !d) return null;
  return new Date(y, m - 1, d, hh || 0, mm || 0, ss || 0);
}

/** 주말 여부 (일=0, 토=6) */
export const isWeekend = (d) => {
  const w = d.getDay();
  return w === 0 || w === 6;
};

/* ===========================
 *  슬롯/범위 유틸
 * =========================== */

/** 09~17 시작 슬롯(1시간 단위) → [9,10,...,17] */
export const genBusinessSlots = () =>
  Array.from({ length: BUSINESS_END_HOUR - BUSINESS_START_HOUR }, (_, i) => BUSINESS_START_HOUR + i);

/**
 * 선택된 시작시각 배열을 연속 구간으로 묶기
 * 예: [11,12,15] → [[11,13],[15,16]]  (각 항목은 [startHour, endHour) 의미)
 */
export function packContiguousRanges(hours) {
  const s = [...new Set(hours)].sort((a, b) => a - b);
  if (!s.length) return [];
  const out = [];
  let a = s[0], p = a;
  for (let i = 1; i < s.length; i++) {
    if (s[i] === p + 1) { p = s[i]; continue; }
    out.push([a, p + 1]);
    a = s[i]; p = a;
  }
  out.push([a, p + 1]);
  return out;
}

/**
 * 한 날짜와 [startHour, endHour) 범위를 받아 서버 전송용 문자열로 변환
 * 반환: { startTime: "YYYY-MM-DD HH:mm:ss", endTime: "YYYY-MM-DD HH:mm:ss" }
 */
export function rangeToServerStrings(dateYMD, startHour, endHour) {
  const base = parseYMD(dateYMD);
  if (!base) throw new Error("유효하지 않은 날짜 형식입니다.");
  const start = new Date(base); start.setHours(startHour, 0, 0, 0);
  const end   = new Date(base); end.setHours(endHour, 0, 0, 0);
  return { startTime: toYMDHMS(start), endTime: toYMDHMS(end) };
}

/* ===========================
 *  검증/클램프
 * =========================== */

/**
 * 같은 날 & 09:00~18:00 & end>start 를 만족하는지
 * - 시작시간: 09:00 이상 17:59:59…까지 허용(17시 시작 OK)
 * - 종료시간: 18:00:00 을 넘지 않음
 */
export function validateBusinessHours(start, end) {
  if (!(start instanceof Date) || !(end instanceof Date)) return false;
  if (toYMD(start) !== toYMD(end)) return false;

  const okStart = start.getHours() >= BUSINESS_START_HOUR && start.getHours() <= (BUSINESS_END_HOUR - 1);
  const okEnd =
    end.getHours() < BUSINESS_END_HOUR ||
    (end.getHours() === BUSINESS_END_HOUR && end.getMinutes() === 0 && end.getSeconds() === 0);
  const after = end.getTime() > start.getTime();
  return okStart && okEnd && after;
}

/**
 * 범위를 09:00~18:00 윈도에 맞게 잘라내기(필요 시)
 * 반환: { start, end, clamped: boolean }
 */
export function clampToBusinessWindow(start, end) {
  if (!(start instanceof Date) || !(end instanceof Date)) {
    return { start, end, clamped: false };
  }
  const base = parseYMD(toYMD(start)); // same day 가정
  const min = new Date(base); min.setHours(BUSINESS_START_HOUR, 0, 0, 0);
  const max = new Date(base); max.setHours(BUSINESS_END_HOUR, 0, 0, 0);

  const s2 = new Date(Math.max(start.getTime(), min.getTime()));
  const e2 = new Date(Math.min(end.getTime(),   max.getTime()));
  const clamped = s2.getTime() !== start.getTime() || e2.getTime() !== end.getTime();
  return { start: s2, end: e2, clamped };
}

/* ===========================
 *  서버 응답 매핑
 * =========================== */

/**
 * /daily-schedule 응답의 timeSlots를 09..17 슬롯으로 변환
 * @param {{hour:string,isAvailable:boolean}[]} timeSlots  ex) [{hour:"09:00",isAvailable:true}, ...]
 * @return {{ hour:number, isAvailable:boolean }[]}
 */
export function mapDailyScheduleToSlots(timeSlots) {
  const hours = genBusinessSlots();
  const map = new Map(
    (Array.isArray(timeSlots) ? timeSlots : []).map((t) => {
      const hh = Number(String(t.hour || "0").split(":")[0]);
      return [hh, !!t.isAvailable];
    })
  );
  // 서버가 19:00, 20:00까지 줄 수 있어도 UI는 09..17만 사용
  return hours.map((h) => ({ hour: h, isAvailable: map.get(h) !== false }));
}

/**
 * 예약 충돌 목록(서버 응답)과 내가 선택한 범위가 겹치는지
 * @param {{startTime:string,endTime:string}[]} conflicts
 * @param {Date} myStart
 * @param {Date} myEnd
 */
export function hasOverlapWithConflicts(conflicts, myStart, myEnd) {
  if (!Array.isArray(conflicts) || !(myStart instanceof Date) || !(myEnd instanceof Date)) return false;
  const meS = myStart.getTime(), meE = myEnd.getTime(); // [meS, meE)
  return conflicts.some((c) => {
    const s = parseYMDHMS(c.startTime); const e = parseYMDHMS(c.endTime);
    if (!s || !e) return false;
    const cs = s.getTime(), ce = e.getTime();
    return cs < meE && meS < ce; // 구간 겹침 판정
  });
}

/* ===========================
 *  라벨/표시
 * =========================== */

/** "09:00–10:00" 형태의 구간 라벨 */
export const formatHourSlotLabel = (startHour) => `${pad2(startHour)}:00–${pad2(startHour + 1)}:00`;

/** "HH:mm–HH:mm" 형태 라벨 */
export function formatRangeLabel(start, end) {
  const hhmm = (d) => `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
  return `${hhmm(start)}–${hhmm(end)}`;
}

/* ===========================
 *  호환성 (이미 사용 중인 함수 시그니처)
 * =========================== */
// ↑ 위의 export들은 기존 컴포넌트(ReservationModal/FacilityRequest/MyFacilityRequests)가
//  기대하던 이름 그대로 유지됨: toYMD, toYMDHMS, genBusinessSlots, validateBusinessHours, packContiguousRanges
// 추가 유틸은 선택적으로 사용 가능.
