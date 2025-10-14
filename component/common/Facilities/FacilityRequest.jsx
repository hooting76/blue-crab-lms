import React, { useEffect, useMemo, useState } from "react";
import CommunitySidebar from "../notices/CommunitySidebar";
import { postFacilities } from "../../../src/api/facility";
import "../../../css/Facilities/FacilityReserve.css";
import ReservationModal from "./ReservationModal";

const PAGE_SIZE = 5;

export default function FacilityRequest({ currentPage, setCurrentPage }) {
  const [list, setList] = useState([]);
  const [selFacility, setSelFacility] = useState(null);
  const [page, setPage] = useState(1);

  useEffect(() => {
    setCurrentPage?.("시설물 예약");
    (async () => {
      try {
        const res = await postFacilities();
        const data = res?.data || [];
        setList(Array.isArray(data) ? data : []);
        setPage(1); // 초기 페이지
      } catch (e) {
        console.error(e);
        alert("시설 목록을 불러오지 못했습니다.");
      }
    })();
  }, []);

  const total = list.length;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const pageItems = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return list.slice(start, start + PAGE_SIZE);
  }, [list, page]);

  const goto = (p) => setPage(Math.min(Math.max(1, p), totalPages));

  return (
    <div className="facility-page">
      {/* 상단 타이틀/설명 — 배너 폭 키움 */}
      <section className="page-head">
        <h2>시설물 예약</h2>
        <p className="sub">
          원하는 시설을 클릭해 예약을 진행하세요. <b>가능 시간: 09:00 ~ 18:00</b>
        </p>
      </section>

      {/* 좌: 메인, 우: 사이드 — 배너 폭과 동일 정렬 */}
      <div className="content-grid">
        <main className="main">
          <div className="card-list">
            {pageItems.map((f) => (
              <button
                key={f.facilityIdx}
                className="facility-card"
                onClick={() =>
                  !f.isBlocked &&
                  setSelFacility({
                    facilityIdx: f.facilityIdx,
                    name: f.facilityName,
                    maxCapacity: f.capacity,
                    requiresApproval: f.requiresApproval,
                    isBlocked: f.isBlocked,
                    blockReason: f.blockReason,
                    availableEquipText: f.defaultEquipment,
                  })
                }
                disabled={f.isBlocked}
              >
                <div className="fc-title">
                  <span className="name">{f.facilityName}</span>
                  {f.isBlocked && <span className="badge danger">차단됨</span>}
                  {!f.isBlocked && f.requiresApproval && (
                    <span className="badge warn">승인 필요</span>
                  )}
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

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="pager" aria-label="시설 목록 페이지네이션">
              <button onClick={() => goto(1)} disabled={page === 1}>
                «
              </button>
              <button onClick={() => goto(page - 1)} disabled={page === 1}>
                ‹
              </button>

              {Array.from({ length: totalPages }).map((_, i) => {
                const p = i + 1;
                const active = p === page ? "active" : "";
                return (
                  <button
                    key={p}
                    className={`num ${active}`}
                    onClick={() => goto(p)}
                    aria-current={active ? "page" : undefined}
                  >
                    {p}
                  </button>
                );
              })}

              <button onClick={() => goto(page + 1)} disabled={page === totalPages}>
                ›
              </button>
              <button onClick={() => goto(totalPages)} disabled={page === totalPages}>
                »
              </button>
            </div>
          )}
        </main>

        <aside className="side">
          <CommunitySidebar currentPage={"시설물 예약"} setCurrentPage={setCurrentPage} />
        </aside>
      </div>

      {selFacility && (
        <ReservationModal facility={selFacility} onClose={() => setSelFacility(null)} />
      )}
    </div>
  );
}
