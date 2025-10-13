import React, { useEffect, useState } from "react";
import CommunitySidebar from "../notices/CommunitySidebar";
import { postFacilities } from "../../../src/api/facility";
import ReservationModal from "./ReservationModal";
import "../../../css/Facilities/FacilityReserve.css";

export default function FacilityRequest() {
  const [list, setList] = useState([]);
  const [open, setOpen] = useState(null);

  useEffect(() => {
    (async () => {
      const res = await postFacilities();
      const rows = res?.data ?? [];
      setList(rows);
    })().catch(console.error);
  }, []);

  const equipmentText = (f) => (f.defaultEquipment || "").trim();

  return (
    <div className="notice-page">
      <div className="grid">
        <main className="left">
          <h2 className="page-title">시설물 예약</h2>
          <p className="page-sub">원하는 시설을 클릭해 예약을 진행하세요. (가능 시간 09:00~18:00)</p>

          <div className="facility-cards">
            {list.map(f => (
              <button
                key={f.facilityIdx}
                className="facility-card"
                disabled={f.isBlocked}
                onClick={() => setOpen({
                  facilityIdx: f.facilityIdx,
                  name: f.facilityName,
                  location: f.location,
                  maxCapacity: f.capacity,
                  requiresApproval: !!f.requiresApproval,
                  isBlocked: !!f.isBlocked,
                  blockReason: f.blockReason,
                  availableEquipText: equipmentText(f), // 보유 장비 안내
                })}
              >
                <div className="fc-title">{f.facilityName}</div>
                <div className="fc-sub">정원 {f.capacity ?? "-"}명 · {f.location}</div>
                <div className="fc-meta">
                  {f.requiresApproval ? "승인 필요" : "자동 승인"} {f.isBlocked ? " · 사용 불가" : ""}
                </div>
              </button>
            ))}
          </div>
        </main>

        <aside className="right">
          <CommunitySidebar />
        </aside>
      </div>

      {open && <ReservationModal facility={open} onClose={() => setOpen(null)} />}
    </div>
  );
}
