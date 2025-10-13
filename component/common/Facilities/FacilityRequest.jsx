// src/component/common/Facilities/FacilityRequest.jsx
import React, { useEffect, useState } from "react";
import CommunitySidebar from "../notices/CommunitySidebar";
import { postFacilities } from "../../../src/api/facility";
import "../../../css/Facilities/FacilityReserve.css";
import ReservationModal from "./ReservationModal";

export default function FacilityRequest({ currentPage, setCurrentPage }) {
  const [list, setList] = useState([]);
  const [selFacility, setSelFacility] = useState(null);

  useEffect(() => {
    setCurrentPage?.("시설물 예약");               // ← 타이틀/사이드바 활성화
    (async () => {
      try {
        const res = await postFacilities();
        setList(res?.data || []);
      } catch (e) {
        console.error(e);
      }
    })();
  }, []);

  return (
    <div className="facility-page">
      {/* 상단 타이틀/설명 */}
      <section className="page-head">
        <h2>시설물 예약</h2>
        <p className="sub">
          원하는 시설을 클릭해 예약을 진행하세요. <b>가능 시간: 09:00 ~ 18:00</b>
        </p>
      </section>

      {/* 좌: 메인, 우: 사이드 */}
      <div className="content-grid">
        <main className="main">
          {/* (선택) 카테고리/필터 영역 필요하면 여기에 */}
          <div className="card-list">
            {list.map((f) => (
              <button
                key={f.facilityIdx}
                className="facility-card"
                onClick={() => !f.isBlocked && setSelFacility({
                  facilityIdx: f.facilityIdx,
                  name: f.facilityName,
                  maxCapacity: f.capacity,
                  requiresApproval: f.requiresApproval,
                  isBlocked: f.isBlocked,
                  blockReason: f.blockReason,
                  availableEquipText: f.defaultEquipment
                })}
                disabled={f.isBlocked}
              >
                <div className="fc-title">
                  <span className="name">{f.facilityName}</span>
                  {f.isBlocked && <span className="badge danger">차단됨</span>}
                  {(!f.isBlocked && f.requiresApproval) && <span className="badge warn">승인 필요</span>}
                </div>
                <div className="fc-meta">
                  <span>위치: {f.location || "-"}</span>
                  <span>수용 인원: {f.capacity ?? "-"}</span>
                </div>
                {!!f.isBlocked && f.blockReason && (
                  <div className="fc-note">{f.blockReason}</div>
                )}
              </button>
            ))}
          </div>
        </main>

        <aside className="side">
          <CommunitySidebar
            currentPage={"시설물 예약"}
            setCurrentPage={setCurrentPage}
          />
        </aside>
      </div>

      {selFacility && (
        <ReservationModal
          facility={selFacility}
          onClose={() => setSelFacility(null)}
        />
      )}
    </div>
  );
}
