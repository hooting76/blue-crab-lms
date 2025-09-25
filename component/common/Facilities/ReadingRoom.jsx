// src/component/common/Facilities/ReadingRoom.jsx
import React, { useEffect, useMemo, useState, useRef } from "react";
import { getSeats, reserveSeat } from "../../../src/api/readingRoom";
import "../../../css/Facilities/ReadingRoom.css";

const STATUS = {
  OCCUPIED: "occupied",
  AVAILABLE: "available",
};

export default function ReadingRoom() {
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const [showGuide, setShowGuide] = useState(false);     // 이용안내 팝오버
  const [confirmSeat, setConfirmSeat] = useState(null);  // 예약 확인 모달
  const [isReserving, setIsReserving] = useState(false); // 중복 클릭 방지
  
  const cardRef = useRef(null);                          
  
  // ────────────────────────────────────────────────────────────
  // 초기 로딩: 더미 API → 프론트 표준형으로 매핑
  // 백엔드 DTO: { id, seat_no, state(0/1) }
  // 프론트 매핑: status = 1 ? 'occupied' : 'available'
  // ────────────────────────────────────────────────────────────
  
  // 좌석 로딩
  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        setLoading(true);
        const raw = await getSeats();
        if (!alive) return;
        const mapped = raw.map((s) => ({
          id: s.id,
          label: String(s.seat_no).padStart(2, "0"),
          status: s.state === 1 ? STATUS.OCCUPIED : STATUS.AVAILABLE,
        }));
        setSeats(mapped);
      } catch (e) {
        console.error(e);
        setErr("좌석 정보를 불러오지 못했습니다.");
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => { alive = false; };
  }, []);

  const availableCount = useMemo(
    () => seats.filter((s) => s.status === STATUS.AVAILABLE).length,
    [seats]
  );

  const onSeatClick = (seat) => {
    if (seat.status !== STATUS.AVAILABLE) return;
    setConfirmSeat(seat); // 예약 확인 모달만 띄움
  };

  const onReserveConfirm = async () => {
    if (!confirmSeat || isReserving) return; // PREVENT: 중복 실행
    setIsReserving(true);                    
    try {
      const r = await reserveSeat(confirmSeat.id);
      if (r.ok === false) {
        setErr(
          r.code === "occupied"
            ? "이미 다른 사용자가 선점했습니다. 잠시 후 다시 시도해주세요."
            : "예약에 실패했습니다. 잠시 후 다시 시도해주세요."
        );
        setConfirmSeat(null);
        return;
      }
    setSeats((prev) =>
      prev.map((s) =>
        s.id === confirmSeat.id ? { ...s, status: STATUS.OCCUPIED } : s
      )
    );
    setConfirmSeat(null);
  } finally {
      setIsReserving(false);             
    }
  };

  if (loading) {
    return (
      <div className="readingroom-wrap">
        <div className="banner banner-center"><h2>열람실 신청</h2></div>
        <div className="loading">불러오는 중…</div>
      </div>
    );
  }

  return (
    <div className="readingroom-wrap">
      {/* 백드롭은 예약확인 모달에만 적용.*/}
      {confirmSeat && (
        <div
          className="global-backdrop"
          onClick={() => setConfirmSeat(null)}
        />
      )}

      {/* 배너(제목 가운데 + 우측 버튼) */}
      <div className="banner banner-center">
        <h2>열람실 신청</h2>
        <div className="banner-right">
          <span className="avail-chip">예약 가능 {availableCount}석</span>
          <button className="use-guide" onClick={() => setShowGuide(true)}>
            이용안내
          </button>
        </div>
      </div>

      {/* 에러 메시지 */}
      {err && <div className="error-inline">{err}</div>}

      {/* 이용안내 우측 상단(배너 아래) 고정 */}
      {showGuide && (
        <div className="guide-popover" role="dialog" aria-modal="true">
          <div className="popover-arrow" />
          <div className="guide-title">이용 안내</div>
          <div className="guide-body">
            <p>클릭 시 자리 예약이 가능합니다.</p>
            <ul>
              <li><b>초록</b>: 예약가능 좌석</li>
              <li><b>핑크</b>: 사용중인 좌석</li>
            </ul>
          </div>
          <div className="guide-actions">
            <button className="primary" onClick={() => setShowGuide(false)}>확인</button>
          </div>
        </div>
      )}

      {/* 좌석 카드 */}
      <div className="room-card" ref={cardRef}>
        <div className="room-header">
          <span className="gate">출구</span>
        </div>

        <div className="seat-grid" role="grid" aria-label="열람실 좌석" style={{ "--cols": 10 }}>
          {seats.map((seat) => {
            const cls =
              seat.status === STATUS.AVAILABLE ? "seat available" : "seat occupied";
            return (
              <button
                key={seat.id}
                className={cls}
                type="button"
                disabled={seat.status !== STATUS.AVAILABLE}
                onClick={() => onSeatClick(seat)}
                aria-label={`${seat.label}번 좌석 ${seat.status === STATUS.AVAILABLE ? "예약 가능" : "사용 중"}`}
                title={`${seat.label}번`}
              >
                {seat.label}
              </button>
            );
          })}
        </div>

        <div className="legend">
          <span><i className="chip chip-available" /> 예약 가능</span>
          <span><i className="chip chip-occupied" /> 사용 중</span>
        </div>
      </div>

      {/* 예약확인 모달(가운데) */}
      {confirmSeat && (
        <div className="confirm-modal" role="dialog" aria-modal="true">
          <div className="confirm-title">예약 확인</div>
          <div className="confirm-body">
            <p><b>{confirmSeat.label}번 좌석</b>을 예약하시겠습니까?</p>
          </div>
          <div className="confirm-actions">
            <button className="primary" onClick={onReserveConfirm}>신청하기</button>
            <button className="ghost" onClick={() => setConfirmSeat(null)}>취소</button>
          </div>
        </div>
      )}
    </div>
  );
}
