import React, { useEffect, useMemo, useState } from "react";
import CommunitySidebar from "../notices/CommunitySidebar";
import { postMyReservationsByStatus, deleteReservation } from "../../../src/api/facility";
import "../../../css/Facilities/FacilityReserve.css";

const ENUM = { PENDING:"PENDING", APPROVED:"APPROVED", REJECTED:"REJECTED", CANCELLED:"CANCELLED", COMPLETED:"COMPLETED" };

const BADGE = {
  "승인됨":"ok",
  "대기중":"wait",
  "반려됨":"reject",
  "취소됨":"done",
  "완료됨":"done",
};

export default function MyFacilityRequests(){
  const [tab, setTab] = useState("ONGOING"); // 진행중/완료
  const [filter, setFilter] = useState("전체");
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState("");

  useEffect(()=>{ load(); /* eslint-disable-next-line */ },[tab]);

  async function load(){
    setLoading(true); setMsg("");
    try{
      if(tab==="ONGOING"){
        const [p,a] = await Promise.all([
          postMyReservationsByStatus(ENUM.PENDING),
          postMyReservationsByStatus(ENUM.APPROVED),
        ]);
        setRows([...(p?.data??[]), ...(a?.data??[])]);
      }else{
        const [c,r,x] = await Promise.all([
          postMyReservationsByStatus(ENUM.COMPLETED),
          postMyReservationsByStatus(ENUM.REJECTED),
          postMyReservationsByStatus(ENUM.CANCELLED),
        ]);
        setRows([...(c?.data??[]), ...(r?.data??[]), ...(x?.data??[])]);
      }
    }catch(e){
      setMsg(e?.response?.data?.message || "목록을 불러오지 못했습니다.");
    }finally{ setLoading(false); }
  }

  const filtered = useMemo(()=>{
    if(filter==="전체") return rows;
    return rows.filter(r => (r.status||"").includes(filter));
  },[rows, filter]);

  const canCancel = (statusKo) => statusKo==="대기중" || statusKo==="승인됨";
  const cancel = async (r) => {
    if(!canCancel(r.status)) return;
    if(!window.confirm("이 예약을 취소하시겠습니까?")) return;
    try{ await deleteReservation(r.reservationIdx); await load(); }
    catch(e){ alert(e?.response?.data?.message || "취소 중 오류가 발생했습니다."); }
  };

  return (
    <div className="notice-page">
      <div className="grid">
        <main className="left">
          <button className="link" onClick={() => history.back()}>시설 목록으로 돌아가기</button>
          <h2 className="page-title">내 예약 현황</h2>

          <div style={{display:"flex", gap:12, alignItems:"center"}}>
            <label>예약 상태별 보기</label>
            <select value={filter} onChange={e=>setFilter(e.target.value)}>
              <option>전체</option><option>승인됨</option><option>대기중</option>
              <option>반려됨</option><option>취소됨</option><option>완료됨</option>
            </select>
          </div>

          <div className="tabs" style={{margin:"12px 0"}}>
            <button className={tab==="ONGOING"?"active":""} onClick={()=>setTab("ONGOING")}>진행중</button>
            <button className={tab==="DONE"?"active":""} onClick={()=>setTab("DONE")}>완료</button>
          </div>

          {loading ? <p>불러오는 중…</p> :
            filtered.length===0 ? <p className="muted">예약이 없습니다.</p> :
            <ul className="resv-list">
              {filtered.map(r=>(
                <li key={r.reservationIdx} className="resv-item">
                  <div className="head">
                    <div className="title">{r.facilityName}</div>
                    <span className={`badge ${BADGE[r.status]||""}`}>{r.status}</span>
                  </div>

                  <div className="row" style={{display:"flex", gap:16}}>
                    <span>📅 {r.startTime.slice(0,10)}</span>
                    <span>🕒 {r.startTime.slice(11,16)}–{r.endTime.slice(11,16)}</span>
                    <span>👥 {r.partySize}명</span>
                  </div>

                  <details className="detail" style={{marginTop:8}} open={r.status==="승인됨"}>
                    <summary>세부 정보</summary>
                    <dl>
                      <dt>사용 목적</dt><dd>{r.purpose || "—"}</dd>
                      <dt>요청 장비</dt><dd>{r.requestedEquipment || "—"}</dd>
                      <dt>관리자 비고</dt><dd>{r.adminNote || "—"}</dd>
                      {r.rejectionReason && (<>
                        <dt>반려 사유</dt><dd>{r.rejectionReason}</dd>
                      </>)}
                      {r.approvedBy && (<>
                        <dt>승인자 / 시간</dt><dd>{r.approvedBy} / {r.approvedAt}</dd>
                      </>)}
                    </dl>

                    {canCancel(r.status) && (
                      <button className="danger" style={{marginTop:8}} onClick={()=>cancel(r)}>
                        예약 취소하기
                      </button>
                    )}
                  </details>
                </li>
              ))}
            </ul>
          }

          {msg && <p className="error" style={{marginTop:8}}>{msg}</p>}
        </main>

        <aside className="right">
          <CommunitySidebar />
        </aside>
      </div>
    </div>
  );
}
