import React, { useEffect, useMemo, useState } from "react";
import { postDailySchedule, postAvailability, postReservation } from "../../../src/api/facility";
import { genBusinessSlots, toYMD, toYMDHMS, validateBusinessHours, packContiguousRanges } from "../../../src/utils/timeUtils";
import "../../../css/Facilities/FacilityReserve.css";

export default function ReservationModal({ facility, onClose }) {
  const [date, setDate] = useState(() => toYMD(new Date()));
  const [slots, setSlots] = useState([]);      // [{hour, isAvailable}]
  const [selected, setSelected] = useState([]); // number[]
  const [purpose, setPurpose] = useState("");
  const [equip, setEquip] = useState("");       // 요청 장비
  const [headcount, setHeadcount] = useState(1);
  const [msg, setMsg] = useState("");
  const [saving, setSaving] = useState(false);

  const hours = genBusinessSlots(); // [9..17]

  useEffect(() => {
    if (facility.isBlocked) return;
    (async () => {
      try {
        const res = await postDailySchedule(facility.facilityIdx, date);
        const arr = res?.data?.timeSlots ?? [];
        // timeSlots: [{hour:"09:00",isAvailable:true}, ...]
        const map = new Map(arr.map(x => [Number((x.hour || "00").split(":")[0]), !!x.isAvailable]));
        const v = hours.map(h => ({ hour: h, isAvailable: map.get(h) !== false }));
        setSlots(v);
        setSelected([]);
      } catch (e) {
        console.error(e);
        setSlots(hours.map(h => ({ hour: h, isAvailable: true })));
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [facility?.facilityIdx, date]);

  const toggle = (h) => {
    const s = slots.find(v => v.hour === h);
    if (!s?.isAvailable) return;
    setSelected(prev => prev.includes(h) ? prev.filter(x => x !== h) : [...prev, h]);
  };

  const nonContiguous = useMemo(() => {
    if (selected.length <= 1) return false;
    const s = [...selected].sort((a,b)=>a-b);
    for (let i=1;i<s.length;i++) if (s[i]!==s[i-1]+1) return true;
    return false;
  }, [selected]);

  async function submit() {
    if (facility.isBlocked) { setMsg(facility.blockReason || "이 시설은 현재 사용이 차단되었습니다."); return; }
    if (!selected.length) return setMsg("시간대를 1개 이상 선택해 주세요.");
    if (!purpose.trim()) return setMsg("사용 목적을 입력해 주세요.");
    if (!equip.trim()) return setMsg("사용할 장비를 입력해 주세요.");
    if (headcount < 1 || (facility.maxCapacity && headcount > facility.maxCapacity))
      return setMsg(`인원은 1~${facility.maxCapacity} 범위여야 합니다.`);

    setSaving(true); setMsg("");
    try {
      const ranges = packContiguousRanges(selected);
      for (const [hStart, hEnd] of ranges) {
        const start = new Date(`${date}T${String(hStart).padStart(2,"0")}:00:00`);
        const end   = new Date(`${date}T${String(hEnd).padStart(2,"0")}:00:00`);
        if (!validateBusinessHours(start, end))
          throw new Error("예약 가능 시간은 09:00~18:00 사이입니다.");

        const startStr = toYMDHMS(start);
        const endStr   = toYMDHMS(end);

        // (선택) 가용성 확인
        const chk = await postAvailability(facility.facilityIdx, startStr, endStr).catch(()=>null);
        if (chk?.data?.isAvailable === false) {
          const list = chk?.data?.conflictingReservations || [];
          const hint = list.map(c=>`${c.startTime}~${c.endTime}`).join(", ");
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
      alert(facility.requiresApproval ? "예약이 생성되었습니다. 관리자 승인을 기다립니다." : "예약이 생성되었습니다.");
      onClose?.();
    } catch (e) {
      setMsg(e?.response?.data?.message || e?.message || "신청 중 오류가 발생했습니다.");
    } finally { setSaving(false); }
  }

  return (
    // ⚠️ 기존: onClick={onClose}로 백그라운드가 클릭을 가로채며 사이드바를 막음
    // 수정: 배경은 시각적(디밍)만 하고 클릭을 통과시킴 (pointer-events: none)
    <div className="resv-modal-backdrop">
      {/* 시각적 디밍 레이어 (클릭 통과) */}
      <div className="resv-dim" />

      {/* 실제 인터랙션은 모달만 받음 */}
      <div className="resv-modal" onClick={(e)=>e.stopPropagation()}>
        <header className="rm-head">
          <h3>{facility.name}</h3>
          <button className="icon" onClick={onClose} aria-label="닫기">✕</button>
        </header>

        <section className="rm-section">
          <label>예약 날짜</label>
          <input type="date" value={date} onChange={e=>setDate(e.target.value)} />
          <p className="muted">예약 가능 시간: <strong>09:00 ~ 18:00</strong></p>
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
            {slots.map(s=>{
              const isSel = selected.includes(s.hour);
              const cls = ["slot", !s.isAvailable ? "booked" : isSel ? "selected" : ""].join(" ").trim();
              return (
                <button key={s.hour} className={cls} disabled={!s.isAvailable} onClick={()=>toggle(s.hour)}>
                  <div className="h">{String(s.hour).padStart(2,"0")}:00</div>
                  <div className="sub">
                    {!s.isAvailable ? "예약 불가" : isSel ? "✓ 선택됨" : "예약 가능"}
                  </div>
                </button>
              );
            })}
          </div>
          {nonContiguous && <div className="warn">비연속 시간대가 포함되어 있습니다 — 제출 시 여러 건으로 접수됩니다.</div>}
        </section>

        <section className="rm-section">
          <label>사용 목적 *</label>
          <textarea value={purpose} onChange={e=>setPurpose(e.target.value)} placeholder="예: 팀 프로젝트 회의" />

          <label>사용할 장비 *</label>
          <input value={equip} onChange={e=>setEquip(e.target.value)} placeholder="예: 빔프로젝터, 화이트보드" />
          {facility.availableEquipText && (
            <p className="muted">이 시설의 보유 장비: {facility.availableEquipText}</p>
          )}

          <label>예상 인원 *</label>
          <input
            type="number"
            min={1}
            max={facility.maxCapacity||999}
            value={headcount}
            onChange={e=>setHeadcount(Number(e.target.value))}
          />
          <p className="muted">정원 {facility.maxCapacity ?? "-"}명</p>
        </section>

        {msg && <p className="error">{msg}</p>}

        <footer className="rm-actions">
          <button className="primary" disabled={saving || facility.isBlocked} onClick={submit}>예약 신청하기</button>
          <button onClick={onClose}>취소</button>
        </footer>
      </div>
    </div>
  );
}
