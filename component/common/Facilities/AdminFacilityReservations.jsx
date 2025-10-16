// component/common/facilities/AdminFacilityReservations.jsx
// 관리자 시설 예약: 탭(승인 대기 / 전체 예약) + 필터 + 테이블 뷰 + 페이지네이션 + 상세 모달

import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  adminPendingList,
  adminSearchReservations,
  getAdminReservationDetail, // 모달 내에서 사용할 수 있도록 로드 트리거만 남김
} from "../../../src/api/adminReservations";
import { postFacilities } from "../../../src/api/facility";
import AdminReservationDetailModal from "./AdminReservationDetailModal";
import "../../../css/Facilities/admin-resv.css";

const PAGE_SIZE = 10;

// 상태 옵션 (백엔드 검색 statusList 값과 라벨 매핑)
const STATUS_OPTIONS = [
  { value: "", label: "전체" },
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
  // 탭: 승인 대기 / 전체 예약
  const [tab, setTab] = useState("PENDING"); // "PENDING" | "ALL"

  // 페이지
  const [page, setPage] = useState(0);

  // 목록/로딩/오류
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");
  const [rows, setRows] = useState([]);

  // 상세 모달
  const [selectedIdx, setSelectedIdx] = useState(null);

  // 필터
  const [facilityOptions, setFacilityOptions] = useState([]);
  const [filters, setFilters] = useState({
    status: "",       // STATUS_OPTIONS value
    facilityIdx: "",  // number|string
    dateFrom: "",     // YYYY-MM-DD
    dateTo: "",       // YYYY-MM-DD
    keyword: "",      // free text
  });

  // 시설 목록(셀렉트)
  useEffect(() => {
    (async () => {
      try {
        const res = await postFacilities();
        const list = Array.isArray(res?.data) ? res.data : [];
        setFacilityOptions(list.map(f => ({ value: String(f.facilityIdx), label: f.facilityName })));
      } catch (e) {
        // 무시(필터에 시설 항목만 비게 됨)
        console.error(e);
      }
    })();
  }, []);

  // 목록 로드
  const load = useCallback(
    async (nextTab = tab, nextPage = page) => {
      setLoading(true);
      setErr("");
      try {
        if (nextTab === "PENDING") {
          // 승인 대기 목록
          const res = await adminPendingList({ page: nextPage, size: PAGE_SIZE });
          const data = Array.isArray(res?.data) ? res.data : [];
          setRows(data);
        } else {
          // 전체 예약 검색
          const body = {
            page: nextPage,
            size: PAGE_SIZE,
          };
          // 필터 적용
          if (filters.dateFrom) body.dateFrom = filters.dateFrom;
          if (filters.dateTo) body.dateTo = filters.dateTo;
          if (filters.facilityIdx) body.facilityIdx = Number(filters.facilityIdx);
          if (filters.keyword) body.keyword = filters.keyword.trim();
          if (filters.status) body.statusList = [filters.status];

          const res = await adminSearchReservations(body);
          const data = Array.isArray(res?.data) ? res.data : [];
          setRows(data);
        }
      } catch (e) {
        setErr(e?.message || "목록을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    },
    [tab, page, filters]
  );

  // 탭 변경 시 1페이지로
  const switchTab = async (t) => {
    setTab(t);
    setPage(0);
  };

  // 탭/페이지/필터 변화에 따라 로드
  useEffect(() => {
    load(tab, page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, page]);

  // 필터 변경 → 페이지 0으로 리셋 후 조회
  const onFilterChange = (patch) => {
    setFilters((prev) => ({ ...prev, ...patch }));
  };
  const applyFilters = async () => {
    setPage(0);
    await load("ALL", 0);
  };

  // 페이지네이션 (total 미제공 가정: 한 페이지가 꽉 차면 다음 페이지 있다고 판단)
  const canPrev = useMemo(() => page > 0, [page]);
  const canNext = useMemo(() => rows.length === PAGE_SIZE, [rows.length]);

  const goPrev = async () => {
    if (!canPrev) return;
    const next = page - 1;
    setPage(next);
  };
  const goNext = async () => {
    if (!canNext) return;
    const next = page + 1;
    setPage(next);
  };

  // 상세 모달
  const openModal = (reservationIdx) => setSelectedIdx(reservationIdx);
  const closeModal = () => setSelectedIdx(null);
  const handleActionDone = async () => {
    closeModal();
    await load(tab, page);
  };

  return (
    <div className="ar-wrap">
      {/* 헤더: 탭 + (ALL일 때) 필터 */}
      <header className="ar-header">
        <div className="ar-tabs">
          <button
            className={`ar-tab ${tab === "PENDING" ? "active" : ""}`}
            onClick={() => switchTab("PENDING")}
          >
            승인 대기
          </button>
          <button
            className={`ar-tab ${tab === "ALL" ? "active" : ""}`}
            onClick={() => switchTab("ALL")}
          >
            전체 예약
          </button>
        </div>
      </header>

      {/* 필터 바: 전체 예약 탭에서만 표시 */}
      {tab === "ALL" && (
        <section className="ar-filter" aria-label="검색 필터">
          <div className="field">
            <label>상태</label>
            <select
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

          <div className="field">
            <label>기간</label>
            <div className="range">
              <input
                type="date"
                value={filters.dateFrom}
                onChange={(e) => onFilterChange({ dateFrom: e.target.value })}
              />
              <input
                type="date"
                value={filters.dateTo}
                onChange={(e) => onFilterChange({ dateTo: e.target.value })}
              />
            </div>
          </div>

          <div className="field">
            <label>검색</label>
            <input
              type="text"
              placeholder="이름, 이메일, 목적 등"
              value={filters.keyword}
              onChange={(e) => onFilterChange({ keyword: e.target.value })}
              onKeyDown={(e) => {
                if (e.key === "Enter") applyFilters();
              }}
            />
          </div>

          {/* 필터 적용 버튼은 별도 두지 않고 Enter/변경 후 자동 조회가 필요하면 여기에 버튼 추가 */}
        </section>
      )}

      {/* 테이블 */}
      <section style={{ background: "#fff", border: "1px solid #e5e7eb", borderRadius: 12 }}>
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
                      {item.startTime?.replace(" ", " ") || "-"} ~ {item.endTime?.split(" ")[1] || ""}
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

        {/* 페이지네이션 */}
        {!loading && !err && rows.length > 0 && (
          <div className="ar-pager">
            <button className="pg-btn" disabled={!canPrev} onClick={goPrev}>이전</button>
            <div className="pg-page">{page + 1}</div>
            <button className="pg-btn" disabled={!canNext} onClick={goNext}>다음</button>
          </div>
        )}
      </section>

      {/* 상세 모달 */}
      {selectedIdx !== null && (
        <AdminReservationDetailModal
          reservationIdx={selectedIdx}
          mode={tab} // "PENDING" | "ALL"
          onClose={closeModal}
          onActionDone={handleActionDone}
        />
      )}
    </div>
  );
}

/* ------- inline 스타일(간결하게) ------- */
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
