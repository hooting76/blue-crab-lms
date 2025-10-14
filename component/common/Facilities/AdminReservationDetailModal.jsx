// components/common/facilities/AdminReservationDetailModal.jsx
// 관리자 시설예약 상세 모달
//  - 예약 상세 정보 표시
//  - adminFetchReservationDetail(reservationIdx)로 상세 로딩
//  - 승인(adminApprove), 반려(adminReject) 처리
//  - 반려 사유는 필수, 관리자 비고는 선택
//  - 처리 성공 시 onActionDone() 호출하여 목록 새로고침

import React, { useEffect, useState } from "react";
import {
  adminFetchReservationDetail,
  adminApprove,
  adminReject,
} from "../../../src/api/adminReservations";
import "../../../css/facilities/admin-resv.css";

export default function AdminReservationDetailModal({
  reservationIdx,
  onClose,
  onActionDone,
}) {
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [adminNote, setAdminNote] = useState("");
  const [rejectionReason, setRejectionReason] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let mounted = true;
    (async () => {
      setLoading(true);
      setErr("");
      try {
        const res = await adminFetchReservationDetail(reservationIdx);
        if (mounted) setDetail(res.data);
      } catch (e) {
        if (mounted) setErr(e?.message || "예약 상세 조회 실패");
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, [reservationIdx]);

  const handleApprove = async () => {
    if (!detail) return;
    setSubmitting(true);
    setErr("");
    try {
      await adminApprove({ reservationIdx: detail.reservationIdx, adminNote: adminNote || undefined });
      onActionDone?.();
    } catch (e) {
      setErr(e?.message || "승인 처리 실패");
    } finally {
      setSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!detail) return;
    if (!rejectionReason.trim()) {
      setErr("반려 사유를 입력해 주세요.");
      return;
    }
    setSubmitting(true);
    setErr("");
    try {
      await adminReject({
        reservationIdx: detail.reservationIdx,
        rejectionReason: rejectionReason.trim(),
      });
      onActionDone?.();
    } catch (e) {
      setErr(e?.message || "반려 처리 실패");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="ar-modal-backdrop" onClick={onClose}>
      <div className="ar-modal" onClick={(e) => e.stopPropagation()}>
        <div className="ar-modal-head">
          <h3>예약 상세</h3>
          <button className="ar-close" onClick={onClose}>×</button>
        </div>

        {loading && <div className="ar-loading p16">불러오는 중...</div>}
        {err && !loading && <div className="ar-error p16">{err}</div>}

        {!loading && !err && detail && (
          <>
            {/* 상태 뱃지 */}
            <div className="ar-badge-row">
              <span
                className={
                  detail.status === "승인됨"
                    ? "ar-badge green"
                    : detail.status === "반려됨"
                    ? "ar-badge red"
                    : "ar-badge yellow"
                }
              >
                {detail.status}
              </span>
            </div>

            {/* 상단 정보 */}
            <div className="ar-grid">
              <div className="ar-grid-row">
                <div className="ar-cell-label">시설</div>
                <div className="ar-cell">{detail.facilityName}</div>
                <div className="ar-cell-label">예약 일시</div>
                <div className="ar-cell">
                  {detail.startTime} ~ {detail.endTime}
                </div>
              </div>
              <div className="ar-grid-row">
                <div className="ar-cell-label">신청자</div>
                <div className="ar-cell">
                  {detail.userName} ({detail.userCode})
                </div>
                <div className="ar-cell-label">이메일</div>
                <div className="ar-cell">{detail.email || "-"}</div>
              </div>
              <div className="ar-grid-row">
                <div className="ar-cell-label">예상 인원</div>
                <div className="ar-cell">{detail.partySize}명</div>
                <div className="ar-cell-label">신청 일시</div>
                <div className="ar-cell">{detail.createdAt}</div>
              </div>
            </div>

            {/* 목적/요청장비 */}
            <div className="ar-section">
              <div className="ar-label">사용 목적</div>
              <div className="ar-box">{detail.purpose || "-"}</div>
              <div className="ar-label">요청 장비</div>
              <div className="ar-box">{detail.requestedEquipment || "-"}</div>
            </div>

            {/* 관리자 비고(선택): 승인 케이스에서 노출용, 입력은 항상 가능 */}
            {detail.adminNote && (
              <div className="ar-section">
                <div className="ar-label">관리자 비고</div>
                <div className="ar-box info">{detail.adminNote}</div>
              </div>
            )}

            {/* 반려 사유: 반려된 예약이면 표시 */}
            {detail.status === "반려됨" && detail.rejectionReason && (
              <div className="ar-section">
                <div className="ar-label">반려 사유</div>
                <div className="ar-box danger">{detail.rejectionReason}</div>
              </div>
            )}

            {/* 처리 입력 영역 */}
            <div className="ar-section">
              <label className="ar-field">
                <span>승인 비고 (선택)</span>
                <textarea
                  placeholder="승인 시 전달할 메시지를 입력하세요 (예: 장비 세팅 완료)"
                  value={adminNote}
                  onChange={(e) => setAdminNote(e.target.value)}
                />
              </label>

              <label className="ar-field">
                <span>반려 사유 (필수)</span>
                <textarea
                  placeholder="반려 시 사유를 입력하세요"
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                />
              </label>
            </div>

            {/* 액션 */}
            <div className="ar-modal-actions">
              <button
                className="ar-btn green"
                onClick={handleApprove}
                disabled={submitting}
              >
                승인
              </button>
              <button
                className="ar-btn red"
                onClick={handleReject}
                disabled={submitting}
              >
                반려
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
