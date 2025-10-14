// BlueCrab LMS - 시간 유틸 (순수 함수)
// 서버 형식: "yyyy-MM-dd HH:mm:ss"
// 영업시간: 09:00 ~ 18:00 (예약 시작 가능 슬롯: 09..17, 18:00은 경계 안내용)

export const BUSINESS_START_HOUR = 9;   // 포함
export const BUSINESS_END_HOUR   = 18;  // 종료 경계(18:00까지 이용, 18은 시작 슬롯 아님)

const pad2 = (n) => String(n).padStart(2, "0");

/** YYYY-MM-DD */
export const toYMD = (d) =>
  `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

/** YYYY-MM-DD HH:mm:ss */
export const toYMDHMS = (d) =>
  `${toYMD(d)} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;

/** "YYYY-MM-DD" -> Date(로컬 00:00:00) */
export function parseYMD(ymd) {
  const [y, m, d] = String(ymd || "").split("-").map((v) => parseInt(v, 10));
  if (!y || !m || !d) return null;
  return new Date(y, m - 1, d, 0, 0, 0, 0);
}

/** "YYYY-MM-DD HH:mm:ss" -> Date(로컬) */
export function parseYMDHMS(str) {
  const [date, time] = String(str || "").split(" ");
  if (!date || !time) return null;
  const [y, m, d] = date.split("-").map((v) => parseInt(v, 10));
  const [hh, mm, ss] = time.split(":").map((v) => parseInt(v, 10));
  if (!y || !m || !d) return null;
  return new Date(y, m - 1, d, hh || 0, mm || 0, ss || 0);
}

/** 주말 여부 */
export const isWeekend = (d) => {
  const w = d.getDay();
  return w === 0 || w === 6;
};

/* ===========================
 * 슬롯/범위 유틸
 * =========================== */

/** 09..17 */
export const genBusinessSlots = () =>
  Array.from(
    { length: BUSINESS_END_HOUR - BUSINESS_START_HOUR },
    (_, i) => BUSINESS_START_HOUR + i
  );

/** 09..17 + 18(경계 표시 전용) */
export const genBusinessSlotsWithBoundary = () => [...genBusinessSlots(), BUSINESS_END_HOUR];

/** [11,12,15] -> [[11,13],[15,16]]  (end는 미포함 경계 의미) */
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

/** 한 날짜와 [startHour, endHour) -> 서버 전송용 문자열 */
export function rangeToServerStrings(dateYMD, startHour, endHour) {
  const base = parseYMD(dateYMD);
  if (!base) throw new Error("유효하지 않은 날짜 형식입니다.");
  const start = new Date(base); start.setHours(startHour, 0, 0, 0);
  const end   = new Date(base); end.setHours(endHour, 0, 0, 0);
  return { startTime: toYMDHMS(start), endTime: toYMDHMS(end) };
}

/* ===========================
 * 검증/클램프
 * =========================== */

/**
 * 같은 날 & 09:00~18:00 & end>start
 * - 시작: 09:00 이상 17:59:59..까지 허용
 * - 종료: 18:00:00 넘지 않음
 */
export function validateBusinessHours(start, end) {
  if (!(start instanceof Date) || !(end instanceof Date)) return false;
  if (toYMD(start) !== toYMD(end)) return false;

  const okStart = start.getHours() >= BUSINESS_START_HOUR && start.getHours() <= (BUSINESS_END_HOUR - 1);
  const okEnd =
    end.getHours() < BUSINESS_END_HOUR ||
    (end.getHours() === BUSINESS_END_HOUR && end.getMinutes() === 0 && end.getSeconds() === 0);

  return okStart && okEnd && end.getTime() > start.getTime();
}

/**
 * 오늘(dateYMD가 오늘)인 경우에만,
 * '해당 슬롯의 종료시각(h+1:00)'이 현재시각을 지났는지 판단
 *  - 지났으면 true => 비활성
 *  - 아직이면 false => 활성
 */
export function isPastHourYMD(dateYMD, startHour) {
  const sel = parseYMD(dateYMD);
  if (!sel) return false;

  const now = new Date();
  if (toYMD(now) !== toYMD(sel)) return false; // 오늘이 아니면 과거 슬롯 판정 금지

  const end = new Date(sel);
  end.setHours(startHour + 1, 0, 0, 0);  // 슬롯 종료시각

  return now.getTime() >= end.getTime();
}

/* ===========================
 * 서버 응답 매핑
 * =========================== */

/** /daily-schedule timeSlots -> 09..17만 사용 */
export function mapDailyScheduleToSlots(timeSlots) {
  const hours = genBusinessSlots();
  const map = new Map(
    (Array.isArray(timeSlots) ? timeSlots : []).map((t) => {
      const hh = Number(String(t.hour || "0").split(":")[0]);
      return [hh, !!t.isAvailable];
    })
  );
  return hours.map((h) => ({ hour: h, isAvailable: map.get(h) !== false }));
}

/* ===========================
 * 라벨/표시
 * =========================== */

export const formatHourSlotLabel = (startHour) =>
  `${pad2(startHour)}:00–${pad2(startHour + 1)}:00`;

export function formatRangeLabel(start, end) {
  const hhmm = (d) => `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
  return `${hhmm(start)}–${hhmm(end)}`;
}
