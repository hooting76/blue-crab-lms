// component/common/facilities/AdminFacilityReservations.jsx
// 관리자 시설 예약: 탭(승인 대기 / 전체 예약) + 필터(기간 제거) + 테이블 + 페이지네이션 + 상세 모달

import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  adminPendingList,
  adminSearchReservations,
} from "../../../src/api/adminReservations";
import { postFacilities } from "../../../src/api/facility";
import AdminReservationDetailModal from "./AdminReservationDetailModal";
import "../../../css/Facilities/admin-resv.css";

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
  { value: "", label: "전체 상태" },
  { value: "PENDING", label: "승인 대기" },
  { value: "APPROVED", label: "승인 완료" },
  { value: "REJECTED", label: "반려됨" },
  { value: "CANCELLED", label: "취소됨" },
  { value: "COMPLETED", label: "사용 완료" },
];

function StatusBadge({ status }) {
  const S = String(status || "").toUpperCase();
  if (S === "APPROVED") return <span className="ar-badge green">승인 완료</span>;
  if (S === "PENDING") return <span className="ar-badge yellow">승인 대기</span>;
  if (S === "REJECTED") return <span className="ar-badge red">반려됨</span>;
  if (S === "CANCELLED") return <span className="ar-badge red">취소됨</span>;
  if (S === "COMPLETED") return <span className="ar-badge green">사용 완료</span>;
  return <span className="ar-badge">{status || "-"}</span>;
}

export default function AdminFacilityReservations() {
  const [tab, setTab] = useState("PENDING"); // "PENDING" | "ALL"
  const [page, setPage] = useState(0);

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");
  const [rows, setRows] = useState([]);
  const [totalPages, setTotalPages] = useState(1);

  const [selectedIdx, setSelectedIdx] = useState(null);

  const [facilityOptions, setFacilityOptions] = useState([]);
  const [filters, setFilters] = useState({
    status: "",
    facilityIdx: "",
    query: "",
  });

  // 시설 목록(셀렉트)
  useEffect(() => {
    (async () => {
      try {
        const res = await postFacilities();
        const list = Array.isArray(res?.data) ? res.data : [];
        setFacilityOptions(
          list.map((f) => ({ value: String(f.facilityIdx), label: f.facilityName }))
        );
      } catch (e) {
        console.error(e);
      }
    })();
  }, []);

  // 목록 로드
  const load = useCallback(
    async (nextTab = tab, nextPage = page, nextFilters = filters) => {
      setLoading(true);
      setErr("");
      try {
        if (nextTab === "PENDING") {
          const res = await adminPendingList({ page: nextPage, size: PAGE_SIZE });
          const content = res?.data?.content ?? res?.data ?? [];
          setRows(Array.isArray(content) ? content : []);
          setTotalPages(res?.data?.totalPages ?? 1);
        } else {
          const body = { page: nextPage, size: PAGE_SIZE };
          if (nextFilters.facilityIdx) body.facilityIdx = Number(nextFilters.facilityIdx);
          if (nextFilters.query?.trim()) body.query = nextFilters.query.trim();
          if (nextFilters.status) body.status = nextFilters.status;

          const res = await adminSearchReservations(body);
          const content = res?.data?.content ?? res?.data ?? [];
          setRows(Array.isArray(content) ? content : []);
          setTotalPages(res?.data?.totalPages ?? 1);
        }
      } catch (e) {
        const msg =
          e?.status === 401
            ? "세션이 만료되었습니다. 다시 로그인해 주세요."
            : e?.message || "목록을 불러오지 못했습니다.";
        setErr(msg);
        setRows([]);
        setTotalPages(1);
      } finally {
        setLoading(false);
      }
    },
    [tab, page, filters]
  );

  // 탭 전환 → 페이지 0으로
  const switchTab = (t) => {
    setTab(t);
    setPage(0);
  };

  // 탭/페이지 변경 시 로드
  useEffect(() => {
    load(tab, page, filters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, page]);

  // 필터 변경
  const onFilterChange = (patch) => {
    setFilters((prev) => ({ ...prev, ...patch }));
  };

  // 검색 적용(ALL 탭에서만)
  const applyFilters = async () => {
    setPage(0);
    await load("ALL", 0, { ...filters });
  };

  // 페이지네이션
  const canPrev = useMemo(() => page > 0, [page]);
  const canNext = useMemo(() => page + 1 < (totalPages || 1), [page, totalPages]);
  const goPrev = () => { if (canPrev) setPage((p) => p - 1); };
  const goNext = () => { if (canNext) setPage((p) => p + 1); };

  // 모달
  const openModal = (reservationIdx) => setSelectedIdx(reservationIdx);
  const closeModal = () => setSelectedIdx(null);
  const handleActionDone = async () => {
    closeModal();
    await load(tab, page, filters);
  };

  return (
    <div className="ar-wrap">
      {/* 상단(탭 + 필터) : 네 CSS의 .ar-sticky 컨테이너 사용 */}
      <div className="ar-sticky">
        <header className="ar-header">
          <div className="ar-tabs">
            <button
              className={`ar-tab ${tab === "PENDING" ? "active" : ""}`}
              onClick={() => { setTab("PENDING"); setPage(0); }}
            >
              승인 대기
            </button>
            <button
              className={`ar-tab ${tab === "ALL" ? "active" : ""}`}
              onClick={() => { setTab("ALL"); setPage(0); }}
            >
              전체 예약
            </button>
          </div>
        </header>

        {tab === "ALL" && (
          <section className="ar-filter ar-filter--grid" aria-label="검색 필터">
            <div className="filter-row">
              <div className="field">
                <label>상태</label>
                <select
                  className="ctl"
                  value={filters.status}
                  onChange={(e) => onFilterChange({ status: e.target.value })}
                >
                  {STATUS_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label>시설</label>
                <select
                  className="ctl"
                  value={filters.facilityIdx}
                  onChange={(e) => onFilterChange({ facilityIdx: e.target.value })}
                >
                  <option value="">전체 시설</option>
                  {facilityOptions.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field search-field">
                <label>검색</label>
                <div className="search-inline">
                  <input
                    className="ctl"
                    type="text"
                    placeholder="이름, 학번/사번, 시설명"
                    value={filters.query}
                    onChange={(e) => onFilterChange({ query: e.target.value })}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") applyFilters();
                    }}
                  />
                  <button className="ar-primary search-btn" onClick={applyFilters}>
                    검색
                  </button>
                </div>
              </div>
            </div>
          </section>
        )}
      </div>

      {/* 테이블 */}
      <section className="ar-table-wrap">
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ background: "#f8fafc" }}>
                <th style={th}>신청일시</th>
                <th style={th}>시설</th>
                <th style={th}>예약일시</th>
                <th style={th}>신청자</th>
                <th style={thCenter}>인원</th>
                <th style={thCenter}>상태</th>
                <th style={thCenter}>액션</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={7} style={tdCenter}>불러오는 중…</td>
                </tr>
              )}
              {err && !loading && (
                <tr>
                  <td colSpan={7} style={tdCenter}>{err}</td>
                </tr>
              )}
              {!loading && !err && rows.length === 0 && (
                <tr>
                  <td colSpan={7} style={tdCenter}>
                    {tab === "PENDING" ? "승인 대기 중인 예약이 없습니다." : "검색 결과가 없습니다."}
                  </td>
                </tr>
              )}
              {!loading && !err && rows.map((item) => (
                <tr key={item.reservationIdx}>
                  <td style={td}>
                    <div>{item.createdAt?.split(" ")[0] || "-"}</div>
                    <div style={muted}>{item.createdAt?.split(" ")[1] || ""}</div>
                  </td>
                  <td style={td}>
                    <div>{item.facilityName || "-"}</div>
                    <div style={muted}>{item.facilityLocation || ""}</div>
                  </td>
                  <td style={td}>
                    <div>
                      {item.startTime || "-"} ~ {item.endTime?.split(" ")[1] || ""}
                    </div>
                  </td>
                  <td style={td}>
                    <div>{item.userName || "Unknown"}</div>
                    <div style={muted}>{item.userEmail || item.userCode || ""}</div>
                  </td>
                  <td style={tdCenter}>{item.partySize ?? "-"}</td>
                  <td style={tdCenter}><StatusBadge status={item.status} /></td>
                  <td style={tdCenter}>
                    <button className="ar-primary" onClick={() => openModal(item.reservationIdx)}>
                      상세보기
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {!loading && !err && rows.length > 0 && (
          <div className="ar-pager">
            <button className="pg-btn" disabled={!canPrev} onClick={goPrev}>이전</button>
            <div className="pg-page">{page + 1}{totalPages ? ` / ${totalPages}` : ""}</div>
            <button className="pg-btn" disabled={!canNext} onClick={goNext}>다음</button>
          </div>
        )}
      </section>

      {selectedIdx !== null && (
        <AdminReservationDetailModal
          reservationIdx={selectedIdx}
          mode={tab}
          onClose={closeModal}
          onActionDone={handleActionDone}
        />
      )}
    </div>
  );
}

/* ------- 테이블 기본 스타일 (컴포넌트 내부) ------- */
const th = {
  textAlign: "left",
  padding: "12px 14px",
  fontSize: 14,
  color: "#374151",
  borderBottom: "1px solid #e5e7eb",
  whiteSpace: "nowrap",
};
const thCenter = { ...th, textAlign: "center" };

const td = {
  padding: "14px",
  borderBottom: "1px solid #f1f5f9",
  verticalAlign: "top",
  color: "#111827",
  fontSize: 14,
};
const tdCenter = { ...td, textAlign: "center" };
const muted = { color: "#6b7280", fontSize: 12, marginTop: 2 };
