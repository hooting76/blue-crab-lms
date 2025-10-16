// components/common/facilities/AdminReservationDetailModal.jsx
// 공용 상세 API 호출 + (승인대기일 때만) 승인/반려 처리 영역 노출

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

  // 상태 표기(서버가 한글/영문 둘 다 올 수 있어 안전 매핑)
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
    return () => {
      mounted = false;
    };
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

  const statusForBadge = (() => {
    const n = normStatus(detail?.status);
    if (n === "APPROVED") return "green";
    if (n === "REJECTED") return "red";
    return "yellow";
  })();

  const statusForText = (() => {
    const n = normStatus(detail?.status);
    if (n === "APPROVED") return "승인됨";
    if (n === "REJECTED") return "반려됨";
    return "대기중";
  })();

  return (
    <div className="modal-backdrop">
      <div className="modal">
        <div className="modal-head">
          <div className="mh-left">
            <b>예약 상세</b>
            {detail && (
              <span className={`ar-badge ${statusForBadge}`} style={{ marginLeft: 8 }}>
                {statusForText}
              </span>
            )}
          </div>
          <button className="icon" onClick={onClose}>
            ✕
          </button>
        </div>

        <div className="modal-body">
          {loading && <div>불러오는 중…</div>}
          {err && <div className="ar-error">{err}</div>}

          {!loading && !err && detail && (
            <>
              <div className="row">
                <b>시설</b>
                <div>{detail.facilityName || "-"}</div>
              </div>
              <div className="row">
                <b>예약 일시</b>
                <div>
                  {detail.startTime} ~ {detail.endTime}
                </div>
              </div>
              <div className="row">
                <b>신청자</b>
                <div>
                  {detail.userName} {detail.userCode ? `(${detail.userCode})` : ""}
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

              {detail.purpose && (
                <>
                  <span className="block">사용 목적</span>
                  <div className="ar-box">{detail.purpose}</div>
                </>
              )}
              {detail.requestedEquipment && (
                <>
                  <span className="block">요청 장비</span>
                  <div className="ar-box">{detail.requestedEquipment}</div>
                </>
              )}

              {/* 보기 전용 메모/반려사유 (승인됨/반려됨에서만) */}
              {normStatus(detail.status) === "APPROVED" && detail.adminNote && (
                <>
                  <span className="block">관리자 비고</span>
                  <div className="ar-box info">{detail.adminNote}</div>
                </>
              )}
              {normStatus(detail.status) === "REJECTED" && detail.rejectionReason && (
                <>
                  <span className="block">반려 사유</span>
                  <div className="ar-box danger">{detail.rejectionReason}</div>
                </>
              )}

              {/* 처리 영역: 승인 대기일 때만 노출 */}
              {isPending && (
                <>
                  <span className="block">승인 비고 (선택)</span>
                  <textarea
                    placeholder="승인 시 전달할 메시지를 입력하세요 (예: 장비 세팅 완료)"
                    value={adminNote}
                    onChange={(e) => setAdminNote(e.target.value)}
                  />
                  <span className="block">반려 사유 (필수)</span>
                  <textarea
                    placeholder="반려 시 사유를 입력하세요"
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                  />

                  <div className="actions">
                    <button className="approve" onClick={onApprove}>
                      승인
                    </button>
                    <button className="reject" onClick={onReject}>
                      반려
                    </button>
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
