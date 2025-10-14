// src/component/common/Facilities/MyFacilityRequests.jsx
import React, { useEffect, useState } from "react";
import CommunitySidebar from "../notices/CommunitySidebar";
import { postMyReservations, postMyReservationsByStatus, cancelReservation } from "../../../src/api/facility";
import "../../../css/Facilities/FacilityReserve.css";

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
        // ★ 핵심 수정: ALL 은 /my, 그 외는 /my/status/{ENUM}
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
    <div className="facility-page">{/* 공통 컨테이너 */}
      <div className="page-head">{/* 상단 타이틀 박스 */}
        <h2>내 예약 현황</h2>
        <p className="sub">예약 상태를 선택해 확인하고, 진행 중 예약은 취소할 수 있어요.</p>
      </div>

      <div className="content-grid">{/* 본문/사이드 2열 */}
        {/* 왼쪽 본문 */}
        <div className="left">
          {/* 기존의 상태 셀렉트 + 리스트 UI를 이곳에 그대로 배치 */}
          <div style={{display:"flex", alignItems:"center", gap:12, marginBottom:12}}>
            <label style={{margin:0}}>예약 상태별 보기</label>
            <select value={status} onChange={(e)=>setStatus(e.target.value)}>
              <option value="ALL">전체</option>
              <option value="PENDING">대기중</option>
              <option value="APPROVED">승인됨</option>
              <option value="REJECTED">반려됨</option>
              <option value="CANCELLED">취소됨</option>
              <option value="COMPLETED">완료됨</option>
            </select>
          </div>

          {/* 기존 카드/테이블 렌더링을 여기에 그대로 */}
          {loading ? (
            <p className="muted">불러오는 중…</p>
          ) : list.length === 0 ? (
            <p className="muted">예약이 없습니다.</p>
          ) : (
            <ul className="resv-list">
              {list.map(r => (
                <li key={r.reservationIdx} className="resv-item">
                  <div className="head">
                    <div className="title">{r.facilityName}</div>
                    {/* 상태 배지 (기존 로직 그대로) */}
                    <span className={`badge ${
                      r.status === "승인됨" ? "ok" :
                      r.status === "대기중" ? "wait" :
                      r.status === "반려됨" ? "reject" :
                      r.status === "완료됨" ? "done" : ""
                    }`}>{r.status}</span>
                  </div>

                  <div className="row">📅 {r.startTime.slice(0,10)} &nbsp; ⏰ {r.startTime.slice(11,16)} - {r.endTime.slice(11,16)}</div>

                  <details>
                    <summary>상세</summary>
                    <dl>
                      <dt>사용 목적</dt><dd>{r.purpose || "-"}</dd>
                      <dt>예상 인원</dt><dd>{r.partySize ? `${r.partySize}명` : "-"}</dd>
                      <dt>요청 장비</dt><dd>{r.requestedEquipment || "-"}</dd>
                      <dt>관리자 비고</dt><dd>{r.adminNote || "-"}</dd>
                    </dl>
                    {r.rejectionReason && (
                      <div className="reject-box">반려 사유: {r.rejectionReason}</div>
                    )}
                  </details>

                  {/* 취소 버튼 조건 (기존 조건 유지) */}
                  {(r.status === "대기중" || r.status === "승인됨") && (
                    <div style={{marginTop:12, textAlign:"right"}}>
                      <button
                        className="danger"
                        onClick={async () => {
                          if (!confirm("정말 취소하시겠습니까?")) return;
                          const ok = await cancelReservation(r.reservationIdx);
                          if (ok?.success) {
                            alert("예약이 취소되었습니다.");
                            setList(prev => prev.map(x => x.reservationIdx === r.reservationIdx
                              ? {...x, status:"취소됨"} : x));
                          } else {
                            alert(ok?.message || "취소에 실패했습니다.");
                          }
                        }}
                      >예약 취소하기</button>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* 오른쪽 사이드바 */}
        <div className="right side">
          <CommunitySidebar
            currentPage="내 예약 현황"
            setCurrentPage={setCurrentPage}
          />
        </div>
      </div>
    </div>
  );
}
