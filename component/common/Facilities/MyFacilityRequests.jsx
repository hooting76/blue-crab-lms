// src/component/common/Facilities/MyFacilityRequests.jsx
import React, { useEffect, useState } from "react";
import CommunitySidebar from "../notices/CommunitySidebar";
import {
  postMyReservations,
  postMyReservationsByStatus,
  cancelReservation,
} from "../../../src/api/facility";
import { parseYMDHMS } from "../../../src/utils/timeUtils";
import "../../../css/Facilities/FacilityReserve.css";

const MS_24H = 24 * 60 * 60 * 1000;
const K_STATUS_CANCELABLE = new Set(["대기중", "승인됨"]);

// 시작 24시간 전까지만 취소 가능(로컬 타임 기준)
function canCancel(res) {
  if (!res?.startTime) return false;
  if (!K_STATUS_CANCELABLE.has(res.status)) return false;
  const start = parseYMDHMS(res.startTime); // "YYYY-MM-DD HH:mm:ss" 안전 파서
  if (!start) return false;
  return start.getTime() - Date.now() > MS_24H;
}

export default function MyFacilityRequests({ currentPage, setCurrentPage }) {
  const [status, setStatus] = useState("ALL");
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  useEffect(() => {
    (async () => {
      setLoading(true);
      setErr("");
      try {
        // ALL → /my, 그 외 → /my/status/{ENUM}
        const res =
          status === "ALL"
            ? await postMyReservations()
            : await postMyReservationsByStatus(status);

        const arr = Array.isArray(res?.data) ? res.data : [];
        setList(arr);
      } catch (e) {
        setErr(e?.message || "목록을 불러오지 못했습니다.");
        setList([]);
      } finally {
        setLoading(false);
      }
    })();
  }, [status]);

  return (
    <div className="facility-page">
      <div className="page-head">
        <h2>내 예약 현황</h2>
        <p className="sub">예약 상태를 선택해 확인하고, 진행 중 예약은 취소할 수 있어요.</p>
      </div>

      <div className="content-grid">
        <div className="left">
          <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 12 }}>
            <label style={{ margin: 0 }}>예약 상태별 보기</label>
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="ALL">전체</option>
              <option value="PENDING">대기중</option>
              <option value="APPROVED">승인됨</option>
              <option value="REJECTED">반려됨</option>
              <option value="CANCELLED">취소됨</option>
              <option value="COMPLETED">완료됨</option>
            </select>
          </div>

          {loading ? (
            <p className="muted">불러오는 중…</p>
          ) : err ? (
            <p className="error">{err}</p>
          ) : list.length === 0 ? (
            <p className="muted">예약이 없습니다.</p>
          ) : (
            <ul className="resv-list">
              {list.map((r) => (
                <li key={r.reservationIdx} className="resv-item">
                  <div className="head">
                    <div className="title">{r.facilityName}</div>
                    <span
                      className={`badge ${
                        r.status === "승인됨"
                          ? "ok"
                          : r.status === "대기중"
                          ? "wait"
                          : r.status === "반려됨"
                          ? "reject"
                          : r.status === "완료됨"
                          ? "done"
                          : ""
                      }`}
                    >
                      {r.status}
                    </span>
                  </div>

                  <div className="row">
                    📅 {r.startTime.slice(0, 10)} &nbsp; ⏰ {r.startTime.slice(11, 16)} -{" "}
                    {r.endTime.slice(11, 16)}
                  </div>

                  <details>
                    <summary>상세</summary>
                    <dl>
                      <dt>사용 목적</dt>
                      <dd>{r.purpose || "-"}</dd>
                      <dt>예상 인원</dt>
                      <dd>{r.partySize ? `${r.partySize}명` : "-"}</dd>
                      <dt>요청 장비</dt>
                      <dd>{r.requestedEquipment || "-"}</dd>
                      <dt>관리자 비고</dt>
                      <dd>{r.adminNote || "-"}</dd>
                    </dl>
                    {r.rejectionReason && (
                      <div className="reject-box">반려 사유: {r.rejectionReason}</div>
                    )}
                  </details>

                  {(r.status === "대기중" || r.status === "승인됨") && (
                    <div style={{ marginTop: 12, textAlign: "right" }}>
                      <button
                        className={`danger ${!canCancel(r) ? "disabled" : ""}`}
                        disabled={!canCancel(r)}
                        title={!canCancel(r) ? "예약 시작 24시간 전까지만 취소할 수 있어요." : ""}
                        onClick={async () => {
                          if (!canCancel(r)) return;
                          if (!confirm("정말 취소하시겠습니까?")) return;
                          try {
                            const ok = await cancelReservation(r.reservationIdx);
                            if (ok?.success) {
                              alert("예약이 취소되었습니다.");
                              setList((prev) =>
                                prev.map((x) =>
                                  x.reservationIdx === r.reservationIdx
                                    ? { ...x, status: "취소됨" }
                                    : x
                                )
                              );
                            } else {
                              alert(ok?.message || "취소에 실패했습니다.");
                            }
                          } catch (e) {
                            // 서버 메시지 그대로 노출 (예: 24시간 마감 초과)
                            alert(e?.message || e?.payload?.message || "취소 중 오류가 발생했습니다.");
                          }
                        }}
                      >
                        예약 취소하기
                      </button>

                      {!canCancel(r) && (
                        <div className="muted" style={{ marginTop: 6 }}>
                          예약 시작 <b>24시간 전까지만</b> 취소할 수 있어요.
                        </div>
                      )}
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="right side">
          <CommunitySidebar currentPage="내 예약 현황" setCurrentPage={setCurrentPage} />
        </div>
      </div>
    </div>
  );
}
