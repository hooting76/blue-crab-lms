// src/component/common/Facilities/ReservationModal.jsx
import React, { useEffect, useMemo, useState } from "react";
import {
  postDailySchedule,
  postAvailability,
  postReservation,
} from "../../../src/api/facility";
import {
  genBusinessSlotsWithBoundary, // 09~18(18은 경계 전용)
  toYMD,
  toYMDHMS,
  validateBusinessHours,
  packContiguousRanges,
  isPastHourYMD,              // 오늘의 지난 시간(끝시각 기준) 비활성화
} from "../../../src/utils/timeUtils";
import "../../../css/Facilities/FacilityReserve.css";

export default function ReservationModal({ facility, onClose }) {
  const [date, setDate] = useState(() => toYMD(new Date()));
  const [slots, setSlots] = useState([]);       // [{ hour, isAvailable, reason? }]
  const [selected, setSelected] = useState([]); // number[]
  const [purpose, setPurpose] = useState("");
  const [equip, setEquip] = useState("");
  const [headcount, setHeadcount] = useState(1);
  const [msg, setMsg] = useState("");
  const [saving, setSaving] = useState(false);

  // 09..18 (18은 경계용 표시만 함)
  const hours = genBusinessSlotsWithBoundary();

  // 일정 불러오기 + 오늘 날짜의 지난 슬롯 비활성화
  useEffect(() => {
    if (facility.isBlocked) return;

    (async () => {
      try {
        const res = await postDailySchedule(facility.facilityIdx, date);
        const arr = res?.data?.timeSlots ?? [];
        const map = new Map(
          arr.map((x) => [Number((x.hour || "00").split(":")[0]), !!x.isAvailable])
        );

        const v = hours.map((h) => {
          // 18시는 시작 슬롯이 아니므로 항상 비활성 + 안내 문구
          if (h === 18) return { hour: h, isAvailable: false, reason: "운영 종료" };

          // 기본: 서버가 false 주면 불가
          let ok = map.get(h) !== false;

          // 오늘이라면, 종료시각(h+1:00)이 현재시각을 지나면 불가
          if (isPastHourYMD(date, h)) ok = false;

          return { hour: h, isAvailable: ok };
        });

        setSlots(v);
        setSelected([]);
      } catch (e) {
        console.error(e);
        // 장애 시 기본 정책 적용
        const v = hours.map((h) => {
          if (h === 18) return { hour: h, isAvailable: false, reason: "운영 종료" };
          return { hour: h, isAvailable: !isPastHourYMD(date, h) };
        });
        setSlots(v);
        setSelected([]);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [facility?.facilityIdx, date]);

  // 슬롯 토글
  const toggle = (h) => {
    const s = slots.find((v) => v.hour === h);
    if (!s?.isAvailable) return;
    setSelected((prev) =>
      prev.includes(h) ? prev.filter((x) => x !== h) : [...prev, h]
    );
  };

  // 비연속 선택 여부
  const nonContiguous = useMemo(() => {
    if (selected.length <= 1) return false;
    const s = [...selected].sort((a, b) => a - b);
    for (let i = 1; i < s.length; i++) if (s[i] !== s[i - 1] + 1) return true;
    return false;
  }, [selected]);

  // 제출
  async function submit() {
    if (facility.isBlocked) {
      setMsg(facility.blockReason || "이 시설은 현재 사용이 차단되었습니다.");
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
          throw new Error("예약 가능 시간은 09:00~18:00 사이입니다.");
        }

        const startStr = toYMDHMS(start);
        const endStr   = toYMDHMS(end);

        // 가용성 재확인(선택)
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
    <div className="resv-modal-backdrop">
      <div className="resv-dim" />
      <div className="resv-modal" onClick={(e) => e.stopPropagation()}>
        <header className="rm-head">
          <h3>{facility.name}</h3>
          <button className="icon" onClick={onClose} aria-label="닫기">
            ✕
          </button>
        </header>

        <section className="rm-section">
          <label>예약 날짜</label>
          <input
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
          />
          <p className="muted">
            예약 가능 시간: <strong>09:00 ~ 18:00</strong>
          </p>
          {facility.isBlocked && (
            <p className="error">현재 차단된 시설입니다. {facility.blockReason || ""}</p>
          )}
        </section>

          <section className="rm-section">
            <div className="legend">
              <span className="chip available">예약 가능</span>
              <span className="chip selected">선택됨</span>
              <span className="chip booked">예약 불가</span>
            </div>

          <div className="grid">
            {slots.map((s) => {
              const isSel = selected.includes(s.hour);
              const disabled = !s.isAvailable;
              const cls = ["slot", disabled ? "booked" : isSel ? "selected" : ""]
                .join(" ")
                .trim();

              const hourLabel =
                s.hour === 18 ? "18:00" : `${String(s.hour).padStart(2, "0")}:00`;

              const sub =
                s.hour === 18
                  ? "운영 종료"
                  : disabled
                  ? "예약 불가"
                  : isSel
                  ? "✓ 선택됨"
                  : "예약 가능";

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
            <div className="warn">
              비연속 시간대가 포함되어 있습니다 — 제출 시 여러 건으로 접수됩니다.
            </div>
          )}
        </section>

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
          <p className="muted">정원 {facility.maxCapacity ?? "-"}명</p>
        </section>

        {msg && <p className="error">{msg}</p>}

        <footer className="rm-actions">
          <button
            className="primary"
            disabled={saving || facility.isBlocked}
            onClick={submit}
          >
            예약 신청하기
          </button>
            <button className="secondary" onClick={onClose}>
              취소
            </button>
        </footer>
      </div>
    </div>
  );
}
