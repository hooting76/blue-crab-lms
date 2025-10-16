// component/common/facilities/AdminFacilityReservations.jsx
//
// 관리자 시설 예약: 탭(승인 대기 / 전체 예약)
// - 승인 대기: 기존 목록 API 그대로 사용
// - 전체 예약: 상태/시설/기간/검색어 필터 + 페이지네이션 + 상세 모달
//

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  adminPendingList,
  adminSearchReservations, // 새 검색 API
} from "../../../src/api/adminReservations";
import AdminReservationDetailModal from "./AdminReservationDetailModal";
import "../../../css/Facilities/admin-resv.css";

const PAGE_SIZE = 5;

// 간단한 디바운스 헬퍼
function useDebouncedValue(value, delay = 400) {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return v;
}

/**
 * props.facilityOptions 예시 (선택 사항)
 *  [
 *    { value: "", label: "전체 시설" },
 *    { value: 101, label: "세미나실 301호" },
 *    { value: 102, label: "세미나실 302호" },
 *    ...
 *  ]
 * 전달이 없으면 시설 선택은 "전체 시설"만 노출됨
 */
export default function AdminFacilityReservations({ facilityOptions }) {
  // ===== 탭/페이지 =====
  const [tab, setTab] = useState("PENDING"); // "PENDING" | "ALL"
  const [page, setPage] = useState(0);

  // ===== 목록 공통 상태 =====
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");
  const [list, setList] = useState([]);

  // ===== 상세 모달 =====
  const [selectedIdx, setSelectedIdx] = useState(null);
  const openModal = (reservationIdx) => setSelectedIdx(reservationIdx);
  const closeModal = () => setSelectedIdx(null);

  // ====== 전체 예약(ALL) 필터 ======
  const [status, setStatus] = useState(""); // "", "PENDING", "APPROVED", "REJECTED"
  const [facility, setFacility] = useState(""); // facilityIdx (number/string) 또는 ""
  const [startDate, setStartDate] = useState(""); // YYYY-MM-DD
  const [endDate, setEndDate] = useState(""); // YYYY-MM-DD
  const [query, setQuery] = useState(""); // 이름/학번/시설명 등
  const debouncedQuery = useDebouncedValue(query, 400); // 입력 중 과도 호출 방지

  // 필터 리셋
  const resetFilters = () => {
    setStatus("");
    setFacility("");
    setStartDate("");
    setEndDate("");
    setQuery("");
    setPage(0);
  };

  // ===== 목록 호출 =====
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
          // 전체 예약 목록 (검색 API)
          const payload = {
            page: p,
            size: PAGE_SIZE,
            // 필터 값이 비었으면 undefined로 보내 백엔드에서 무시되게
            status: status || undefined,
            facilityIdx: facility || undefined,
            startDate: startDate || undefined,
            endDate: endDate || undefined,
            keyword: debouncedQuery || undefined,
          };
          const res = await adminSearchReservations(payload);
          setList(Array.isArray(res?.data) ? res.data : []);
        }
      } catch (e) {
        setErr(e?.message || "목록을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    },
    [tab, page, status, facility, startDate, endDate, debouncedQuery]
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab]);

  // 전체 예약 탭에서 필터/검색 변경 시 1페이지부터 재조회
  // (debouncedQuery가 바뀌면 load()가 다시 호출됨)
  useEffect(() => {
    if (tab !== "ALL") return;
    setPage(0);
    load("ALL", 0);
  }, [status, facility, startDate, endDate, debouncedQuery, tab, load]);

  // 페이지네이션 가능 여부 (총 카운트 미제공 가정: PAGE_SIZE만큼 오면 next 가능)
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

  // 모달에서 승인/반려 후, 현재 페이지 그대로 새로고침
  const handleActionDone = async () => {
    closeModal();
    await load(tab, page);
  };

  // 시설 옵션 (없으면 "전체 시설"만)
  const facilities = useMemo(() => {
    const base = [{ value: "", label: "전체 시설" }];
    if (Array.isArray(facilityOptions) && facilityOptions.length > 0) {
      return base.concat(facilityOptions);
    }
    return base;
  }, [facilityOptions]);

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
          >
            전체 예약
          </button>
        </div>
      </header>

      {/* ===== 전체 예약 필터 UI ===== */}
      {tab === "ALL" && (
        <section className="ar-filters">
          <div className="ar-filter-grid">
            {/* 상태 */}
            <div className="f-col">
              <div className="f-label">상태</div>
              <select
                className="f-input"
                value={status}
                onChange={(e) => setStatus(e.target.value)}
              >
                <option value="">전체 상태</option>
                <option value="PENDING">승인 대기</option>
                <option value="APPROVED">승인 완료</option>
                <option value="REJECTED">반려됨</option>
              </select>
            </div>

            {/* 시설 */}
            <div className="f-col">
              <div className="f-label">시설</div>
              <select
                className="f-input"
                value={facility}
                onChange={(e) => setFacility(e.target.value)}
              >
                {facilities.map((opt) => (
                  <option key={String(opt.value)} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            {/* 기간 */}
            <div className="f-col f-dates">
              <div className="f-label">기간</div>
              <div className="f-date-row">
                <input
                  type="date"
                  className="f-input"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
                <span className="f-tilde">~</span>
                <input
                  type="date"
                  className="f-input"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
              </div>
            </div>

            {/* 검색어 */}
            <div className="f-col f-search">
              <div className="f-label">검색</div>
              <input
                className="f-input"
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="이름, 학번, 시설명"
              />
            </div>

            {/* 액션 */}
            <div className="f-col f-actions">
              <button
                className="ar-secondary"
                type="button"
                onClick={resetFilters}
                disabled={loading}
              >
                초기화
              </button>
            </div>
          </div>
        </section>
      )}

      {/* ===== 목록 ===== */}
      <section className="ar-list">
        {loading && <div className="ar-loading">불러오는 중…</div>}
        {err && <div className="ar-error">{err}</div>}

        {!loading && !err && list.length === 0 && (
          <div className="ar-empty">
            {tab === "PENDING"
              ? "승인 대기 중인 예약이 없습니다."
              : "검색 조건에 해당하는 예약이 없습니다."}
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
                    item.status === "승인됨" || item.status === "APPROVED"
                      ? "green"
                      : item.status === "반려됨" || item.status === "REJECTED"
                      ? "red"
                      : "yellow"
                  }`}
                >
                  {item.statusKor || item.status || "승인 대기"}
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
                {"partySize" in item && <div>인원: {item.partySize}명</div>}
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

      {/* 상세 모달: 승인대기/전체예약 공통으로 사용 */}
      {selectedIdx !== null && (
        <AdminReservationDetailModal
          reservationIdx={selectedIdx}
          mode={tab} // "PENDING" | "ALL" (모달 내부에서 관리자 상세 API/공용 상세 API 선택에 활용)
          onClose={closeModal}
          onActionDone={handleActionDone}
        />
      )}
    </div>
  );
}
