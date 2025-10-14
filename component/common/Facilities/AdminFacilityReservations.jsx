// component/common/facilities/AdminFacilityReservations.jsx
// 관리자 시설 예약: 탭(승인 대기 / 전체 예약) + 5개/페이지 페이지네이션 + 상세 모달

import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  adminPendingList,
  // adminAllList, // 전체 예약 API가 준비되면 주석 해제 후 연결
} from "../../../src/api/adminReservations";
import AdminReservationDetailModal from "./AdminReservationDetailModal";
import "../../../css/Facilities/admin-resv.css";

const PAGE_SIZE = 5;

export default function AdminFacilityReservations() {
  // 탭: 승인 대기 / 전체 예약
  const [tab, setTab] = useState("PENDING"); // "PENDING" | "ALL"
  // 페이지 인덱스(0-base)
  const [page, setPage] = useState(0);
  // 목록 로딩/오류/데이터
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");
  const [list, setList] = useState([]);
  // 상세 모달용 예약 ID
  const [selectedIdx, setSelectedIdx] = useState(null);

  // 목록 호출
  const load = useCallback(
    async (t = tab, p = page) => {
      setLoading(true);
      setErr("");
      try {
        if (t === "PENDING") {
          // 승인 대기 목록
          const res = await adminPendingList({ page: p, size: PAGE_SIZE });
          setList(Array.isArray(res?.data) ? res.data : []);
        } else {
          // 전체 예약 목록 (백엔드 준비 전까지 비워둠)
          // const res = await adminAllList({ page: p, size: PAGE_SIZE });
          // setList(Array.isArray(res?.data) ? res.data : []);
          setList([]);
        }
      } catch (e) {
        setErr(e?.message || "목록을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    },
    [page, tab]
  );

  // 탭 변경 시 1페이지부터 재조회
  const resetAndLoad = useCallback(
    async (nextTab = tab) => {
      setPage(0);
      await load(nextTab, 0);
    },
    [tab, load]
  );

  useEffect(() => {
    resetAndLoad(tab);
  }, [tab, resetAndLoad]);

  // 페이지네이션 가능 여부 (total 미제공 가정: 페이지 크기 == PAGE_SIZE 이면 다음 페이지 존재)
  const canPrev = useMemo(() => page > 0, [page]);
  const canNext = useMemo(() => list.length === PAGE_SIZE, [list.length]);

  const goPrev = async () => {
    if (!canPrev) return;
    const next = page - 1;
    setPage(next);
    await load(tab, next);
  };

  const goNext = async () => {
    if (!canNext) return;
    const next = page + 1;
    setPage(next);
    await load(tab, next);
  };

  // 모달 제어
  const openModal = (reservationIdx) => setSelectedIdx(reservationIdx);
  const closeModal = () => setSelectedIdx(null);

  // 모달에서 승인/반려 후, 현재 페이지 그대로 새로고침
  const handleActionDone = async () => {
    closeModal();
    await load(tab, page);
  };

  return (
    <div className="ar-wrap">
      {/* 상단 탭 */}
      <header className="ar-header">
        <div className="ar-tabs">
          <button
            className={`ar-tab ${tab === "PENDING" ? "active" : ""}`}
            onClick={() => setTab("PENDING")}
          >
            승인 대기
          </button>
          <button
            className={`ar-tab ${tab === "ALL" ? "active" : ""}`}
            onClick={() => setTab("ALL")}
            title="전체 예약 조회는 백엔드 API 준비 후 연결됩니다"
          >
            전체 예약
          </button>
        </div>
      </header>

      {/* 목록 영역 */}
      <section className="ar-list">
        {loading && <div className="ar-loading">불러오는 중…</div>}
        {err && <div className="ar-error">{err}</div>}

        {!loading && !err && list.length === 0 && (
          <div className="ar-empty">
            {tab === "PENDING"
              ? "승인 대기 중인 예약이 없습니다."
              : "전체 예약 조회 API 준비 중입니다."}
          </div>
        )}

        {!loading &&
          !err &&
          list.map((item) => (
            <article className="ar-card" key={item.reservationIdx}>
              <div className="ar-card-head">
                <h3 className="ar-title">{item.facilityName}</h3>
                <span
                  className={`ar-badge ${
                    item.status === "승인됨"
                      ? "green"
                      : item.status === "반려됨"
                      ? "red"
                      : "yellow"
                  }`}
                >
                  {item.status || "승인 대기"}
                </span>
              </div>

              <div className="ar-meta">
                <div>
                  예약 일시: {item.startTime} ~ {item.endTime}
                </div>
                <div>
                  신청자: {item.userName}
                  {item.userCode ? ` (${item.userCode})` : ""}
                </div>
                <div>인원: {item.partySize}명</div>
                <div>신청일시: {item.createdAt}</div>
              </div>

              {(item.purpose || item.requestedEquipment) && (
                <div className="ar-desc">
                  {item.purpose && (
                    <>
                      <div className="ar-label">사용 목적</div>
                      <div className="ar-box">{item.purpose}</div>
                    </>
                  )}
                  {item.requestedEquipment && (
                    <>
                      <div className="ar-label">요청 장비</div>
                      <div className="ar-box">{item.requestedEquipment}</div>
                    </>
                  )}
                </div>
              )}

              <div className="ar-actions">
                <button
                  className="ar-primary"
                  onClick={() => openModal(item.reservationIdx)}
                >
                  상세보기
                </button>
              </div>
            </article>
          ))}

        {/* 페이지네이션 */}
        {!loading && !err && list.length > 0 && (
          <div className="ar-pager">
            <button className="pg-btn" disabled={!canPrev} onClick={goPrev}>
              이전
            </button>
            <div className="pg-page">{page + 1}</div>
            <button className="pg-btn" disabled={!canNext} onClick={goNext}>
              다음
            </button>
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
