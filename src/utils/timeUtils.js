// BlueCrab LMS - 시간 유틸 (순수 함수)
// 서버 형식: "yyyy-MM-dd HH:mm:ss"
// 접수 가능(Submission) 시간: 09:00 ~ 18:00
// 타임 슬롯: 09:00~10:00 ... 18:00~19:00  (=> 시작 가능 시각 9..18, 종료 상한 19:00)

export const BUSINESS_START_HOUR = 9;   // 슬롯 시작 최솟값(포함)
export const LAST_START_HOUR     = 18;  // 슬롯 시작 최댓값(포함) => 18:00~19:00
export const SLOT_END_CEIL_HOUR  = 19;  // 슬롯 종료 상한(미포함 경계)
export const SUBMISSION_END_HOUR = 18;  // 접수 종료 경계(18:00 이후 당일 운영 종료 취급)

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

/* ───────────────── 슬롯/범위 유틸 ───────────────── */

/** 시작 가능 슬롯 목록: 9..18 (포함) */
export const genBusinessSlots = () =>
  Array.from({ length: LAST_START_HOUR - BUSINESS_START_HOUR + 1 },
    (_, i) => BUSINESS_START_HOUR + i
  );

/**  genBusinessSlots 동일 반환 */
export const genBusinessSlotsWithBoundary = () => genBusinessSlots();

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

/* ───────────────── 검증/클램프 ───────────────── */

/**
 * 같은 날 & 09:00~19:00 & end>start
 * - 시작: 09:00 이상 18:59:59..까지 허용 (시작 정시는 9..18)
 * - 종료: 최대 19:00:00
 */
export function validateBusinessHours(start, end) {
  if (!(start instanceof Date) || !(end instanceof Date)) return false;
  if (toYMD(start) !== toYMD(end)) return false;

  const sh = start.getHours();
  const eh = end.getHours();
  const em = end.getMinutes();
  const es = end.getSeconds();

  const okStart = sh >= BUSINESS_START_HOUR && sh <= LAST_START_HOUR;
  const okEnd =
    eh < SLOT_END_CEIL_HOUR ||
    (eh === SLOT_END_CEIL_HOUR && em === 0 && es === 0);

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
  end.setHours(startHour + 1, 0, 0, 0);  // 슬롯 종료시각 (ex: 18시 슬롯 -> 19:00)

  return now.getTime() >= end.getTime();
}

/**
 * 오늘(dateYMD가 오늘) 18:00 경계가 지났는지 여부
 * - 오늘이 아니면 항상 false
 * - 같고, 현재시각이 18:00 이상이면 true
 *   (18:00 이후에는 당일 모든 슬롯을 '운영 종료'로 막는다)
 */
export function isAfterBusinessEnd(dateYMD) {
  const sel = parseYMD(dateYMD);
  if (!sel) return false;

  const now = new Date();
  if (toYMD(now) !== toYMD(sel)) return false;

  const boundary = new Date(sel);
  boundary.setHours(SUBMISSION_END_HOUR, 0, 0, 0); // 18:00
  return now.getTime() >= boundary.getTime();
}

/* ───────────────── 서버 응답 매핑 ───────────────── */

/** /daily-schedule timeSlots -> 9..18만 사용 */
export function mapDailyScheduleToSlots(timeSlots) {
  const hours = genBusinessSlots(); // 9..18
  const map = new Map(
    (Array.isArray(timeSlots) ? timeSlots : []).map((t) => {
      const hh = Number(String(t.hour || "0").split(":")[0]);
      return [hh, !!t.isAvailable];
    })
  );
  return hours.map((h) => ({ hour: h, isAvailable: map.get(h) !== false }));
}

/* ───────────────── 라벨/표시 ───────────────── */
export const formatHourSlotLabel = (startHour) =>
  `${pad2(startHour)}:00–${pad2(startHour + 1)}:00`;

export function formatRangeLabel(start, end) {
  const hhmm = (d) => `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
  return `${hhmm(start)}–${hhmm(end)}`;
}

/* ───────────────── 추가(정책 보조 유틸) ───────────────── */

/** YYYY-MM-DD 두 날짜 비교: 과거:-1, 오늘:0, 미래:1 (로컬 기준) */
export function compareYMD(aYMD, bYMD = toYMD(new Date())) {
  const a = parseYMD(aYMD);
  const b = parseYMD(bYMD);
  if (!a || !b) return 0;
  const at = a.setHours(0,0,0,0);
  const bt = b.setHours(0,0,0,0);
  if (at < bt) return -1;
  if (at > bt) return 1;
  return 0;
}

export const isPastDateYMD   = (ymd) => compareYMD(ymd) < 0;
export const isTodayYMD      = (ymd) => compareYMD(ymd) === 0;
export const isFutureDateYMD = (ymd) => compareYMD(ymd) > 0;

/** 지금이 접수 가능 시간(09:00 <= now < 18:00)인지 */
export function isNowInSubmissionWindow() {
  const now = new Date();
  const h = now.getHours();
  return h >= BUSINESS_START_HOUR && h < SUBMISSION_END_HOUR;
}
