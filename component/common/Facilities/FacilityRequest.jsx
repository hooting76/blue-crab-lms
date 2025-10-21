// FacilitiesList.jsx
import React, { useEffect, useMemo, useState } from "react";
import CommunitySidebar from "../notices/CommunitySidebar";
import { postFacilities } from "../../../src/api/facility";
import "../../../css/Facilities/FacilityReserve.css";
import ReservationModal from "./ReservationModal";

const PAGE_SIZE = 6;

export default function FacilityRequest({ currentPage, setCurrentPage }) {
  const [list, setList] = useState([]);
  const [selFacility, setSelFacility] = useState(null);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  useEffect(() => {
    setCurrentPage?.("시설물 예약");
    (async () => {
      setLoading(true);
      setErr("");
      try {
        const res = await postFacilities();
        const data = Array.isArray(res?.data) ? res.data : [];
        setList(data);
        setPage(1);
      } catch (e) {
        console.error(e);
        setErr("시설 목록을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
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

  function openModalFromCard(f) {
    if (f?.isBlocked) return;
    // ReservationModal에서 사용하는 필드 이름으로 정규화
    setSelFacility({
      facilityIdx: f.facilityIdx,
      name: f.facilityName,                 // 모달 헤더 표시용
      maxCapacity: f.capacity,              // 정원 표시/검증
      requiresApproval: !!f.requiresApproval,
      isBlocked: !!f.isBlocked,
      blockReason: f.blockReason || "",
      availableEquipText: f.defaultEquipment || "",
      location: f.location || "",
    });
  }

  return (
    <div className="facility-page">
      {/* 상단 타이틀/설명 */}
      <section className="page-head">
        <h2>시설물 예약</h2>
        <p className="sub">
          원하는 시설을 클릭해 예약을 진행하세요. <b>가능 시간: 09:00 ~ 18:00</b>
        </p>
      </section>

      <div className="content-grid">
        <main className="main">
          {loading ? (
            <p className="muted">불러오는 중…</p>
          ) : err ? (
            <p className="error">{err}</p>
          ) : pageItems.length === 0 ? (
            <p className="muted">표시할 시설이 없습니다.</p>
          ) : (
            <div className="card-list">
              {pageItems.map((f) => (
                <button
                  key={f.facilityIdx}
                  type="button"                          // ✅ submit 방지
                  className="facility-card"
                  onClick={() => openModalFromCard(f)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      e.preventDefault();
                      openModalFromCard(f);
                    }
                  }}
                  aria-label={`${f.facilityName} 예약 열기`}
                  aria-disabled={!!f.isBlocked}
                  disabled={!!f.isBlocked}
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
                    <span>수용 인원: {typeof f.capacity === "number" ? f.capacity : "-"} 명</span>
                  </div>

                  {!!f.isBlocked && f.blockReason && (
                    <div className="fc-note">{f.blockReason}</div>
                  )}
                </button>
              ))}
            </div>
          )}

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <nav className="pager" aria-label="시설 목록 페이지네이션">
              <button type="button" onClick={() => goto(1)} disabled={page === 1} aria-label="처음 페이지">
                «
              </button>
              <button
                type="button"
                onClick={() => goto(page - 1)}
                disabled={page === 1}
                aria-label="이전 페이지"
              >
                ‹
              </button>

              {Array.from({ length: totalPages }).map((_, i) => {
                const p = i + 1;
                const active = p === page ? "active" : "";
                return (
                  <button
                    type="button"
                    key={p}
                    className={`num ${active}`}
                    onClick={() => goto(p)}
                    aria-current={active ? "page" : undefined}
                  >
                    {p}
                  </button>
                );
              })}

              <button
                type="button"
                onClick={() => goto(page + 1)}
                disabled={page === totalPages}
                aria-label="다음 페이지"
              >
                ›
              </button>
              <button
                type="button"
                onClick={() => goto(totalPages)}
                disabled={page === totalPages}
                aria-label="마지막 페이지"
              >
                »
              </button>
            </nav>
          )}
        </main>

        <aside className="side">
          <CommunitySidebar currentPage={"시설물 예약"} setCurrentPage={setCurrentPage} />
        </aside>
      </div>

      {/* 모달 — ReservationModal 내부는 Portal로 띄우는 버전 사용 권장 */}
      {selFacility && (
        <ReservationModal
          facility={selFacility}
          onClose={() => setSelFacility(null)}
        />
      )}
    </div>
  );
}
