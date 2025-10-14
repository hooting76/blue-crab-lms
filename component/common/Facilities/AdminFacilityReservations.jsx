// components/common/facilities/AdminFacilityReservations.jsx
// 관리자 시설예약
//  -  승인 대기 목록 & 전체 예약 목록 탭
//  -  대기 목록: adminPendingList() 연동
//  -  모달 열기/닫기 & 처리 후 목록 재조회
//  - 전체예약 탭은 API가 아직 없어 비활성(또는 동일 목록 재사용) 처리

import React, { useEffect, useState, useCallback } from "react";
import { adminPendingList } from "../../../src/api/adminReservations";
import AdminReservationDetailModal from "./AdminReservationDetailModal";
import "../../../css/facilities/admin-resv.css";

export default function AdminFacilityReservations() {
  const [activeTab, setActiveTab] = useState("PENDING"); // "PENDING" | "ALL"(비활성)
  const [pending, setPending] = useState([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");
  const [selectedIdx, setSelectedIdx] = useState(null); // 모달용

  const loadPending = useCallback(async () => {
    setLoading(true);
    setErr("");
    try {
      const res = await adminPendingList({ page: 0, size: 20 });
      // 응답 형식: { success, message, data: [...] }
      setPending(Array.isArray(res.data) ? res.data : []);
    } catch (e) {
      setErr(e?.message || "승인 대기 목록 조회 실패");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeTab === "PENDING") {
      loadPending();
    }
  }, [activeTab, loadPending]);

  const openModal = (reservationIdx) => setSelectedIdx(reservationIdx);
  const closeModal = () => setSelectedIdx(null);

  // [CHANGED] 모달에서 승인/반려 후 목록 새로고침
  const handleActionDone = async () => {
    closeModal();
    await loadPending();
  };

  return (
    <div className="ar-wrap">
      <header className="ar-header">
        <h2>시설물 예약 관리</h2>
        <p className="ar-sub">예약 승인 및 전체 현황을 관리합니다</p>
        <div className="ar-tabs">
          <button
            className={`ar-tab ${activeTab === "PENDING" ? "active" : ""}`}
            onClick={() => setActiveTab("PENDING")}
          >
            승인 대기
          </button>
          <button
            className={`ar-tab ${activeTab === "ALL" ? "active" : ""}`}
            onClick={() => setActiveTab("ALL")}
            disabled
            title="전체예약 API 준비되면 활성화"
          >
            전체 예약
          </button>
        </div>
      </header>

      {activeTab === "PENDING" && (
        <section className="ar-list">
          {loading && <div className="ar-loading">불러오는 중...</div>}
          {err && <div className="ar-error">{err}</div>}
          {!loading && !err && pending.length === 0 && (
            <div className="ar-empty">승인 대기 중인 예약이 없습니다.</div>
          )}

          {pending.map((item) => (
            <article className="ar-card" key={item.reservationIdx}>
              <div className="ar-card-head">
                <h3 className="ar-title">{item.facilityName}</h3>
                <span className="ar-badge yellow">승인 대기</span>
              </div>

              <div className="ar-meta">
                <div>예약 일시: {item.startTime} ~ {item.endTime}</div>
                <div>신청자: {item.userName} ({item.userCode})</div>
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
                <button className="ar-primary" onClick={() => openModal(item.reservationIdx)}>
                  처리하기
                </button>
              </div>
            </article>
          ))}
        </section>
      )}

      {/* 상세 모달 */}
      {selectedIdx !== null && (
        <AdminReservationDetailModal
          reservationIdx={selectedIdx}
          onClose={closeModal}
          onActionDone={handleActionDone}
        />
      )}
    </div>
  );
}
