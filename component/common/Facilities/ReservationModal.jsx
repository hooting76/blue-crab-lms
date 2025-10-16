import React, { useEffect, useMemo, useState } from "react";
import {
  postDailySchedule,
  postAvailability,
  postReservation,
} from "../../../src/api/facility";
import {
  genBusinessSlotsWithBoundary, // 09..18 시작 슬롯(18:00~19:00 포함)
  toYMD,
  toYMDHMS,
  validateBusinessHours,
  packContiguousRanges,
  isPastHourYMD,              // 오늘의 지난 슬롯(끝시각 기준) 비활성화
  isAfterBusinessEnd,         // 오늘 18:00 경계가 지났는지
} from "../../../src/utils/timeUtils";
import "../../../css/Facilities/FacilityReserve.css";

/** 지금이 ‘접수 가능 시간(09:00~18:00)’인지 */
function isNowInSubmissionWindow() {
  const now = new Date();
  const h = now.getHours();
  // 09:00 <= now < 18:00
  return h >= 9 && h < 18;
}

export default function ReservationModal({ facility, onClose }) {
  const [date, setDate] = useState(() => toYMD(new Date()));
  const [slots, setSlots] = useState([]);       // [{ hour, isAvailable, reason?, status? }]
  const [selected, setSelected] = useState([]); // number[]
  const [purpose, setPurpose] = useState("");
  const [equip, setEquip] = useState("");
  const [headcount, setHeadcount] = useState(1);
  const [msg, setMsg] = useState("");
  const [saving, setSaving] = useState(false);

  const hours = genBusinessSlotsWithBoundary();
  const canSubmitNow = isNowInSubmissionWindow(); // ★ 현재 접수 가능 여부(날짜 무관, 지금 기준)

  /** 상단 시설 요약 */
  const metaLine = useMemo(() => {
    const pieces = [];
    if (facility?.location) pieces.push(facility.location);
    if (facility?.maxCapacity) pieces.push(`정원 ${facility.maxCapacity}명`);
    return pieces.join(" · ");
  }, [facility]);

  useEffect(() => {
    if (facility?.isBlocked) return;

    (async () => {
      try {
        const res = await postDailySchedule(facility.facilityIdx, date);
        const arr = Array.isArray(res?.data?.timeSlots) ? res.data.timeSlots : [];

        const slotMap = new Map();
        for (const t of arr) {
          const hh = Number(String(t.hour || "0").split(":")[0]);
          const status = t?.status ? String(t.status).toUpperCase() : undefined;
          slotMap.set(hh, { isAvailable: t.isAvailable !== false, status });
        }

        const endedToday = isAfterBusinessEnd(date);

        let computed = hours.map((h) => {
          // 18시는 일반 예약 슬롯(18:00~19:00). 단, 오늘 18:00 지나면 운영 종료.
          if (h === 18) {
            if (endedToday) return { hour: h, isAvailable: false, reason: "운영 종료" };
            return { hour: h, isAvailable: true };
          }

          let ok = true;
          let reason = "";
          let status = slotMap.get(h)?.status;

          if (slotMap.has(h) && slotMap.get(h).isAvailable === false) {
            ok = false;
            reason = "예약 불가";
          }

          if (status === "APPROVED") { ok = false; reason = "예약됨"; }
          else if (status === "PENDING") { ok = false; reason = "승인 대기"; }

          if (ok && isPastHourYMD(date, h)) { ok = false; reason = "예약 불가"; }

          if (endedToday) { ok = false; reason = "운영 종료"; }

          return { hour: h, isAvailable: ok, reason, status };
        });

        // ★ 접수 시간이 아니면(현재 시각 기준), 선택한 날짜가 무엇이든 전부 ‘운영 종료’
        if (!canSubmitNow) {
          computed = hours.map((h) => ({ hour: h, isAvailable: false, reason: "운영 종료" }));
        }

        setSlots(computed);
        setSelected([]);
      } catch (e) {
        console.error(e);

        const endedToday = isAfterBusinessEnd(date);
        let fallback = hours.map((h) => {
          if (h === 18) {
            return endedToday
              ? { hour: h, isAvailable: false, reason: "운영 종료" }
              : { hour: h, isAvailable: true };
          }
          if (endedToday) return { hour: h, isAvailable: false, reason: "운영 종료" };
          const past = isPastHourYMD(date, h);
          return { hour: h, isAvailable: !past, reason: past ? "예약 불가" : "" };
        });

        // ★ 접수 시간이 아니면 예외 상황에서도 동일 정책 적용
        if (!canSubmitNow) {
          fallback = hours.map((h) => ({ hour: h, isAvailable: false, reason: "운영 종료" }));
        }

        setSlots(fallback);
        setSelected([]);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [facility?.facilityIdx, date, canSubmitNow]);

  const toggle = (h) => {
    const s = slots.find((v) => v.hour === h);
    if (!s?.isAvailable) return;
    setSelected((prev) => (prev.includes(h) ? prev.filter((x) => x !== h) : [...prev, h]));
  };

  const nonContiguous = useMemo(() => {
    if (selected.length <= 1) return false;
    const s = [...selected].sort((a, b) => a - b);
    for (let i = 1; i < s.length; i++) if (s[i] !== s[i - 1] + 1) return true;
    return false;
  }, [selected]);

  const packedLabels = useMemo(() => {
    if (!selected.length) return [];
    const ranges = packContiguousRanges(selected);
    return ranges.map(([a, b]) => {
      const pad = (n) => String(n).padStart(2, "0");
      return `${pad(a)}:00 ~ ${pad(b)}:00`;
    });
  }, [selected]);

  async function submit() {
    if (facility.isBlocked) {
      setMsg(facility.blockReason || "이 시설은 현재 사용이 차단되었습니다.");
      return;
    }
    if (!canSubmitNow) {
      setMsg("접수 가능 시간(09:00~18:00)이 아닙니다.");
      return;
    }
    if (!selected.length) return setMsg("시간대를 1개 이상 선택해 주세요.");
    if (!purpose.trim()) return setMsg("사용 목적을 입력해 주세요.");
    if (!equip.trim()) return setMsg("사용할 장비를 입력해 주세요.");
    if (headcount < 1 || (facility.maxCapacity && headcount > facility.maxCapacity)) {
      return setMsg(`인원은 1~${facility.maxCapacity} 범위여야 합니다.`);
    }

    setSaving(true);
    setMsg("");
    try {
      const ranges = packContiguousRanges(selected);
      for (const [hStart, hEnd] of ranges) {
        const start = new Date(`${date}T${String(hStart).padStart(2, "0")}:00:00`);
        const end   = new Date(`${date}T${String(hEnd).padStart(2, "0")}:00:00`);

        if (!validateBusinessHours(start, end)) {
          throw new Error("예약 가능 시간은 09:00~19:00 사이입니다.");
        }

        const startStr = toYMDHMS(start);
        const endStr   = toYMDHMS(end);

        const chk = await postAvailability(
          facility.facilityIdx, startStr, endStr
        ).catch(() => null);

        if (chk?.data?.isAvailable === false) {
          const list = chk?.data?.conflictingReservations || [];
          const hint = list.map((c) => `${c.startTime}~${c.endTime}`).join(", ");
          throw new Error(hint ? `충돌 예약: ${hint}` : "해당 시간에는 이미 다른 예약이 존재합니다.");
        }

        await postReservation({
          facilityIdx: facility.facilityIdx,
          startTime: startStr,
          endTime: endStr,
          partySize: headcount,
          purpose: purpose.trim(),
          requestedEquipment: equip.trim(),
        });
      }

      alert(
        facility.requiresApproval
          ? "예약이 생성되었습니다. 관리자 승인을 기다립니다."
          : "예약이 생성되었습니다."
      );
      onClose?.();
    } catch (e) {
      setMsg(e?.response?.data?.message || e?.message || "신청 중 오류가 발생했습니다.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="resv-modal-backdrop" onClick={onClose}>
      <div className="resv-dim" />
      <div className="resv-modal" onClick={(e) => e.stopPropagation()}>
        <header className="rm-head">
          <div>
            <h3 style={{ marginBottom: 2 }}>{facility?.name || "시설 예약"}</h3>
            {metaLine && <div className="muted">{metaLine}</div>}
          </div>
          <button className="icon" onClick={onClose} aria-label="닫기">✕</button>
        </header>

        <section className="rm-section">
          <label>예약 날짜</label>
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          <p className="muted">
            접수 가능 시간: <strong>09:00~18:00</strong>
          </p>
          {!canSubmitNow && (
            <p className="warn">지금은 접수 시간이 아닙니다. 접수는 매일 09:00~18:00에 가능합니다.</p>
          )}
          {facility.isBlocked && (
            <p className="error">현재 차단된 시설입니다. {facility.blockReason || ""}</p>
          )}
        </section>

        <section className="rm-section">
          <div className="legend">
            <span className="chip available">예약 가능</span>
            <span className="chip selected">선택됨</span>
            <span className="chip booked">예약됨</span>
            <span className="chip wait">승인 대기</span>
          </div>

          <div className="grid">
            {slots.map((s) => {
              const isSel = selected.includes(s.hour);
              const disabled = !s.isAvailable;
              const stateClass =
                s.status === "APPROVED"
                  ? "approved"
                  : s.status === "PENDING"
                  ? "pending"
                  : disabled
                  ? "booked"
                  : isSel
                  ? "selected"
                  : "";
              const cls = ["slot", stateClass].filter(Boolean).join(" ");
              const hourLabel = `${String(s.hour).padStart(2, "0")}:00`;

              let sub = "예약 가능";
              if (s.reason === "운영 종료") sub = "운영 종료";
              else if (s.status === "APPROVED") sub = "예약됨";
              else if (s.status === "PENDING") sub = "승인 대기";
              else if (disabled && s.reason) sub = s.reason;
              else if (isSel) sub = "✓ 선택됨";

              return (
                <button
                  key={s.hour}
                  className={cls}
                  disabled={disabled}
                  onClick={() => toggle(s.hour)}
                  aria-disabled={disabled}
                >
                  <div className="h">{hourLabel}</div>
                  <div className="sub">{sub}</div>
                </button>
              );
            })}
          </div>

          {nonContiguous && (
            <div className="warn" style={{ marginTop: 10 }}>
              비연속 시간대가 포함되어 있습니다 — 제출 시 여러 건으로 접수됩니다.
            </div>
          )}
        </section>

        {!!packedLabels.length && (
          <section className="rm-section" style={{ paddingTop: 6 }}>
            <div className="info-box">
              <div style={{ fontWeight: 800, marginBottom: 6 }}>선택한 시간</div>
              <ol style={{ margin: 0, paddingLeft: 18 }}>
                {packedLabels.map((lab, idx) => (
                  <li key={idx} style={{ marginBottom: 4 }}>{lab}</li>
                ))}
              </ol>
            </div>
          </section>
        )}

        <section className="rm-section">
          <label>사용 목적 *</label>
          <textarea
            value={purpose}
            onChange={(e) => setPurpose(e.target.value)}
            placeholder="예: 팀 프로젝트 회의"
          />

          <label>사용할 장비 *</label>
          <input
            value={equip}
            onChange={(e) => setEquip(e.target.value)}
            placeholder="예: 빔프로젝터, 화이트보드"
          />
          {facility.availableEquipText && (
            <p className="muted">이 시설의 보유 장비: {facility.availableEquipText}</p>
          )}

          <label>예상 인원 *</label>
          <input
            type="number"
            min={1}
            max={facility.maxCapacity || 999}
            value={headcount}
            onChange={(e) => setHeadcount(Number(e.target.value))}
          />
          {facility.maxCapacity && <p className="muted">정원 {facility.maxCapacity}명</p>}
        </section>

        {msg && <p className="error">{msg}</p>}

        <footer className="rm-actions">
          <button
            className="primary"
            disabled={saving || facility.isBlocked || !canSubmitNow}
            onClick={submit}
          >
            예약 신청하기
          </button>
          <button className="secondary" onClick={onClose}>취소</button>
        </footer>
      </div>
    </div>
  );
}
