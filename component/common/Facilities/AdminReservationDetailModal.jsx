// components/common/facilities/AdminReservationDetailModal.jsx
// 관리자 시설 예약 상세 모달 (CSS 클래스 기반 레이아웃 + 하단 액션 sticky + 예약일시 포맷)

import React, { useEffect, useState } from "react";
import {
  getAdminReservationDetail,
  adminApprove,
  adminReject,
} from "../../../src/api/adminReservations";
import "../../../css/Facilities/admin-resv.css";

export default function AdminReservationDetailModal({
  reservationIdx,
  mode = "PENDING", // "PENDING" | "ALL"
  onClose,
  onActionDone,
}) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const [adminNote, setAdminNote] = useState("");
  const [rejectionReason, setRejectionReason] = useState("");

  // 상태 표준화
  const normStatus = (v) => {
    const s = String(v || "").toUpperCase();
    if (["PENDING", "대기", "대기중"].includes(s)) return "PENDING";
    if (["APPROVED", "승인", "승인됨"].includes(s)) return "APPROVED";
    if (["REJECTED", "반려", "반려됨"].includes(s)) return "REJECTED";
    return s || "PENDING";
  };

  const isPending = mode === "PENDING" || normStatus(detail?.status) === "PENDING";

  useEffect(() => {
    let mounted = true;
    (async () => {
      setLoading(true);
      setErr("");
      try {
        const res = await getAdminReservationDetail(reservationIdx);
        if (!mounted) return;
        setDetail(res?.data || null);
      } catch (e) {
        if (!mounted) return;
        setErr(e?.message || "상세 정보를 불러오지 못했습니다.");
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, [reservationIdx]);

  const onApprove = async () => {
    try {
      await adminApprove(reservationIdx, { adminNote: adminNote || null });
      onActionDone?.();
    } catch (e) {
      alert(e?.message || "승인 처리에 실패했습니다.");
    }
  };

  const onReject = async () => {
    if (!rejectionReason.trim()) {
      alert("반려 사유를 입력하세요.");
      return;
    }
    try {
      await adminReject(reservationIdx, { rejectionReason });
      onActionDone?.();
    } catch (e) {
      alert(e?.message || "반려 처리에 실패했습니다.");
    }
  };

  const badgeClass = (() => {
    const n = normStatus(detail?.status);
    if (n === "APPROVED") return "ar-badge green";
    if (n === "REJECTED") return "ar-badge red";
    return "ar-badge yellow";
  })();

  const badgeText = (() => {
    const n = normStatus(detail?.status);
    if (n === "APPROVED") return "승인됨";
    if (n === "REJECTED") return "반려됨";
    return "대기중";
  })();

  // 예약일시 포맷: "YYYY-MM-DD HH:mm~HH:mm"
  const formatStartEnd = (start, end) => {
    if (!start || !end) return (start || "-") + " ~ " + (end || "-");
    const [d1, t1 = ""] = String(start).split(" ");
    const [, t2 = ""] = String(end).split(" ");
    const hhmm1 = t1.slice(0,5);
    const hhmm2 = t2.slice(0,5);
    return `${d1} ${hhmm1}~${hhmm2}`;
  };

  return (
    <div className="modal-backdrop">
      <div className="modal">
        {/* 헤더 */}
        <div className="modal-head">
          <div className="mh-left">
            <b>예약 상세</b>
            {detail && <span className={badgeClass} style={{ marginLeft: 8 }}>{badgeText}</span>}
          </div>
          <button className="icon" onClick={onClose} aria-label="닫기">✕</button>
        </div>

        <div className="modal-body">
          {loading && <div className="ar-loading">불러오는 중…</div>}
          {err && <div className="ar-error">{err}</div>}

          {!loading && !err && detail && (
            <>
              {/* 기본 정보: 2열 정렬 */}
              <div className="info-grid">
                <div className="row">
                  <b>시설</b>
                  <div>{detail.facilityName || "-"}</div>
                </div>
                <div className="row">
                  <b>예약 일시</b>
                  <div>{formatStartEnd(detail.startTime, detail.endTime)}</div>
                </div>
                <div className="row">
                  <b>신청자</b>
                  <div>
                    {detail.userName}
                    {detail.userCode ? ` (${detail.userCode})` : ""}
                  </div>
                </div>
                <div className="row">
                  <b>이메일</b>
                  <div>{detail.userEmail || "-"}</div>
                </div>
                <div className="row">
                  <b>예상 인원</b>
                  <div>{detail.partySize}명</div>
                </div>
                <div className="row">
                  <b>신청 일시</b>
                  <div>{detail.createdAt}</div>
                </div>
              </div>

              {/* 사용 목적 / 요청 장비 - 읽기 전용 인풋 형태 */}
              <label className="section-label">사용 목적</label>
              <div className="readonly-input">{detail.purpose || "-"}</div>

              <label className="section-label">요청 장비</label>
              <div className="readonly-input">{detail.requestedEquipment || "-"}</div>

              {/* 승인됨/반려됨 메모 */}
              {normStatus(detail.status) === "APPROVED" && detail.adminNote && (
                <>
                  <label className="section-label">관리자 비고</label>
                  <div className="readonly-input note-approved">{detail.adminNote}</div>
                </>
              )}
              {normStatus(detail.status) === "REJECTED" && detail.rejectionReason && (
                <>
                  <label className="section-label">반려 사유</label>
                  <div className="readonly-input note-rejected">{detail.rejectionReason}</div>
                </>
              )}

              {/* 처리 영역: 승인 대기일 때만 */}
              {isPending && (
                <>
                  <label className="section-label">승인 비고 (선택)</label>
                  <textarea
                    className="readonly-input"
                    style={{ minHeight: 96 }}
                    placeholder="승인 시 전달할 메시지를 입력하세요 (예: 장비 세팅 완료)"
                    value={adminNote}
                    onChange={(e) => setAdminNote(e.target.value)}
                  />

                  <label className="section-label">반려 사유 (필수)</label>
                  <textarea
                    className="readonly-input"
                    style={{ minHeight: 96 }}
                    placeholder="반려 시 사유를 입력하세요"
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                  />

                  {/* 하단 액션바 (sticky) */}
                  <div className="action-bar-sticky">
                    <button className="btn-approve" onClick={onApprove}>✅ 승인</button>
                    <button className="btn-reject" onClick={onReject}>❌ 반려</button>
                  </div>
                </>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
