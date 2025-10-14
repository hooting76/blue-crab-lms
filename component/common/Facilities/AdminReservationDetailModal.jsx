//상세 & 승인/반려 모달

import React, { useEffect, useState } from "react";
import {
  adminGetDetail,
  adminApprove,
  adminReject,
} from "../../src/api/adminReservations";

export default function AdminReservationDetailModal({ reservationIdx, onClose, onChanged }) {
  const [d, setD] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const [adminNote, setAdminNote] = useState("");
  const [rejectReason, setRejectReason] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    (async () => {
      setLoading(true);
      setErr("");
      try {
        const r = await adminGetDetail(reservationIdx);
        setD(r?.data || null);
      } catch (e) {
        setErr(e?.message || "상세를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();
  }, [reservationIdx]);

  const canApprove = d?.status === "대기중";
  const canReject = d?.status === "대기중";

  const doApprove = async () => {
    if (!canApprove) return;
    setSaving(true);
    try {
      const r = await adminApprove(reservationIdx, { adminNote: adminNote || undefined });
      alert(r?.message || "승인되었습니다.");
      onChanged?.();
      onClose?.();
    } catch (e) {
      alert(e?.message || "승인 실패");
    } finally {
      setSaving(false);
    }
  };

  const doReject = async () => {
    if (!canReject) return;
    if (!rejectReason.trim()) {
      alert("반려 사유를 입력하세요.");
      return;
    }
    setSaving(true);
    try {
      const r = await adminReject(reservationIdx, { rejectionReason: rejectReason.trim() });
      alert(r?.message || "반려되었습니다.");
      onChanged?.();
      onClose?.();
    } catch (e) {
      alert(e?.message || "반려 실패");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <header className="modal-head">
          <h3>예약 상세</h3>
          <button className="icon" onClick={onClose}>✕</button>
        </header>

        {loading ? (
          <div className="muted" style={{padding:16}}>불러오는 중…</div>
        ) : err ? (
          <div className="error" style={{padding:16}}>{err}</div>
        ) : !d ? (
          <div className="muted" style={{padding:16}}>데이터가 없습니다.</div>
        ) : (
          <div className="modal-body">
            <div className="row"><b>상태</b><span>{d.status}</span></div>
            <div className="row"><b>시설</b><span>{d.facilityName}</span></div>
            <div className="row"><b>예약 일시</b><span>{d.startTime} ~ {d.endTime}</span></div>
            <div className="row"><b>신청자</b><span>{d.userName} ({d.userCode})</span></div>
            <div className="row"><b>이메일</b><span>{d.email || "-"}</span></div>
            <div className="row"><b>예상 인원</b><span>{d.partySize ? `${d.partySize}명` : "-"}</span></div>
            <div className="row"><b>사용 목적</b><span>{d.purpose || "-"}</span></div>
            <div className="row"><b>요청 장비</b><span>{d.requestedEquipment || "-"}</span></div>

            <label className="block">승인 비고 (선택)</label>
            <textarea
              value={adminNote}
              onChange={(e) => setAdminNote(e.target.value)}
              placeholder="승인 시 전달할 메시지 (예: 장비 세팅 완료 예정)"
            />

            <label className="block">반려 사유 (필수)</label>
            <textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="반려 사유를 입력하세요"
            />

            <div className="actions">
              <button className="approve" disabled={!canApprove || saving} onClick={doApprove}>승인</button>
              <button className="reject" disabled={!canReject || saving} onClick={doReject}>반려</button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
