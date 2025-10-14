// 시설물 예약 관리 (관리자 페이지) 
// 목록/필터/통계 + 상세 모달

import React, { useEffect, useMemo, useState } from "react";
import {
  adminStats,
  adminPendingList,
  adminSearchList,
  adminGetDetail,
} from "../../src/api/adminReservations";
import AdminReservationDetailModal from "./AdminReservationDetailModal";
import "../../../css/facilities/admin-resv.css";

/** 상태 뱃지 색상 */
const statusBadge = (statusKo) => {
  switch (statusKo) {
    case "대기중": return "badge wait";
    case "승인됨": return "badge ok";
    case "반려됨": return "badge reject";
    case "취소됨": return "badge cancel";
    case "완료됨": return "badge done";
    default: return "badge";
  }
};

export default function AdminFacilityReservations() {
  const [tab, setTab] = useState("PENDING"); // PENDING | ALL
  const [stats, setStats] = useState(null);

  // filters (ALL 탭에서 사용)
  const [status, setStatus] = useState("ALL");
  const [facilityIdx, setFacilityIdx] = useState("ALL");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [size] = useState(10);

  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  const [detailId, setDetailId] = useState(null); // modal

  // 통계
  useEffect(() => {
    (async () => {
      try {
        const r = await adminStats();
        setStats(r?.data || null);
      } catch (_) {/* 무시 */}
    })();
  }, []);

  // 목록
  useEffect(() => {
    setLoading(true);
    setErr("");
    const load = async () => {
      try {
        if (tab === "PENDING") {
          const r = await adminPendingList({ page, size });
          setRows(r?.data?.items || r?.data || []); // 백엔드 응답 형태에 맞게 양쪽 케이스 처리
          setTotal(r?.data?.total ?? (r?.data?.length ?? 0));
        } else {
          const r = await adminSearchList({
            status: status === "ALL" ? undefined : status,
            facilityIdx: facilityIdx === "ALL" ? undefined : Number(facilityIdx),
            query: query || undefined,
            page,
            size,
          });
          setRows(r?.data?.items || r?.data || []);
          setTotal(r?.data?.total ?? (r?.data?.length ?? 0));
        }
      } catch (e) {
        setErr(e?.message || "목록을 불러오지 못했습니다.");
        setRows([]);
        setTotal(0);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [tab, page, size, status, facilityIdx, query]);

  const onOpenDetail = async (id) => {
    // 상세는 모달 내부에서 다시 불러오지만, 404 방지용 ping 가능
    setDetailId(id);
  };

  const totalPages = useMemo(() => Math.max(1, Math.ceil(total / size)), [total, size]);

  return (
    <div className="admin-resv-page">
      <h2>시설물 예약 관리</h2>
      <p className="muted">예약 승인 및 전체 현황을 관리합니다.</p>

      {/* Stats */}
      <div className="stats">
        <div className="card">
          <div className="label">승인 대기</div>
          <div className="num">{stats?.pending ?? "-"}</div>
        </div>
        <div className="card">
          <div className="label">오늘 예약</div>
          <div className="num">{stats?.today ?? "-"}</div>
        </div>
        <div className="card">
          <div className="label">이번 주</div>
          <div className="num">{stats?.thisWeek ?? "-"}</div>
        </div>
        <div className="card">
          <div className="label">이번 달</div>
          <div className="num">{stats?.thisMonth ?? "-"}</div>
        </div>
      </div>

      {/* Tabs */}
      <div className="tabs">
        <button className={tab === "PENDING" ? "active" : ""} onClick={() => { setTab("PENDING"); setPage(0); }}>승인 대기</button>
        <button className={tab === "ALL" ? "active" : ""} onClick={() => { setTab("ALL"); setPage(0); }}>전체 예약</button>
      </div>

      {/* Filters (ALL 탭 전용) */}
      {tab === "ALL" && (
        <div className="filters">
          <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
            <option value="ALL">전체 상태</option>
            <option value="PENDING">대기중</option>
            <option value="APPROVED">승인됨</option>
            <option value="REJECTED">반려됨</option>
            <option value="CANCELLED">취소됨</option>
            <option value="COMPLETED">완료됨</option>
          </select>

          <select value={facilityIdx} onChange={(e) => { setFacilityIdx(e.target.value); setPage(0); }}>
            <option value="ALL">전체 시설</option>
            {/* 필요 시 시설목록 API 붙여 채우기 */}
            {/* <option value="1">세미나실 301호</option> */}
          </select>

          <div className="search">
            <input
              value={query}
              onChange={(e) => { setQuery(e.target.value); setPage(0); }}
              placeholder="이름, 학번, 시설명"
            />
            <span className="icon">🔍</span>
          </div>
        </div>
      )}

      {/* List */}
      <div className="list">
        {loading ? (
          <div className="muted">불러오는 중…</div>
        ) : err ? (
          <div className="error">{err}</div>
        ) : rows.length === 0 ? (
          <div className="muted">데이터가 없습니다.</div>
        ) : (
          <table className="tbl">
            <thead>
              <tr>
                <th>신청일시</th>
                <th>시설</th>
                <th>예약일시</th>
                <th>신청자</th>
                <th>인원</th>
                <th>상태</th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.reservationIdx}>
                  <td>{(r.createdAt || "").replace(" ", " ")}</td>
                  <td>{r.facilityName}</td>
                  <td>
                    {(r.startTime || "").slice(0, 16)} ~ {(r.endTime || "").slice(11, 16)}
                  </td>
                  <td>{r.userName} <span className="muted">{r.userCode}</span></td>
                  <td>{r.partySize ? `${r.partySize}명` : "-"}</td>
                  <td><span className={statusBadge(r.status)}>{r.status}</span></td>
                  <td>
                    <button className="link" onClick={() => onOpenDetail(r.reservationIdx)}>상세보기</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="pager">
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>이전</button>
          <span>{page + 1} / {totalPages}</span>
          <button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>다음</button>
        </div>
      )}

      {/* Detail Modal */}
      {detailId != null && (
        <AdminReservationDetailModal
          reservationIdx={detailId}
          onClose={() => setDetailId(null)}
          // 갱신 콜백: 승인/반려 후 목록 새고
          onChanged={() => {
            // 간단히 현재 페이지 재조회 트리거
            setPage((p) => p);
          }}
        />
      )}
    </div>
  );
}
