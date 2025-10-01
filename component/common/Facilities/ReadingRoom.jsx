// src/component/common/Facilities/ReadingRoom.jsx
import React, { useEffect, useMemo, useState, useRef } from "react";
import {getSeats, reserveSeat, releaseSeat, getMyReservation } from "../../../api/readingRoomApi";
import "../../../css/Facilities/ReadingRoom.css";

const STATUS = {
  OCCUPIED: "occupied",
  AVAILABLE: "available",
  MINE: "mine", 
};

export default function ReadingRoom() {
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const [showGuide, setShowGuide] = useState(false);
  const [confirmSeat, setConfirmSeat] = useState(null);   // 예약용 모달 seat
  const [confirmCheckout, setConfirmCheckout] = useState(false); // 퇴실용 모달
  const [isReserving, setIsReserving] = useState(false);
  const [isCheckingOut, setIsCheckingOut] = useState(false);

  const [mySeatNo, setMySeatNo] = useState(null); // ★ 내 좌석 seatNumber

  const cardRef = useRef(null);

  // --- 공통 로더: 좌석 + 내 예약 동시 조회 후 병합 표시 ---
  const loadAll = async () => {
    setErr("");
    const [rawSeats, myRes] = await Promise.all([
      getSeats(),
      getMyReservation().catch(() => null),
    ]);
    const mapped = rawSeats.map((s) => ({
      id: s.id,
      label: String(s.seat_no).padStart(2, "0"),
      status: s.state === 1 ? STATUS.OCCUPIED : STATUS.AVAILABLE,
    }));

    const myNo = myRes?.seatNumber ?? null;
    setMySeatNo(myNo);

    if (myNo) {
      // 내 좌석은 파란색(상태: MINE)으로 덮어쓰기
      for (let i = 0; i < mapped.length; i++) {
        if (mapped[i].id === myNo) {
          mapped[i] = { ...mapped[i], status: STATUS.MINE };
          break;
        }
      }
    }
    setSeats(mapped);
  };

  // 최초 로딩
  useEffect(() => {
    let alive = true;
    (async () => {
      setLoading(true);
      try {
        await loadAll();
      } catch (e) {
        console.error(e);
        setErr("좌석 정보를 불러오지 못했습니다.");
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, []);

  // 30초 오토 리프레시
  useEffect(() => {
    const t = setInterval(() => {
      loadAll();
    }, 30000);
    return () => clearInterval(t);
  }, []);

  const availableCount = useMemo(
    () => seats.filter((s) => s.status === STATUS.AVAILABLE).length,
    [seats]
  );

  const onSeatClick = (seat) => {
    // 예약 가능 좌석 → 예약 확인 모달
    if (seat.status === STATUS.AVAILABLE) {
      setErr("");
      setConfirmSeat(seat);
      return;
    }
    // 내 좌석(파란색) → 퇴실 확인 모달
    if (seat.status === STATUS.MINE) {
      setErr("");
      setConfirmCheckout(true);
      return;
    }
    // 사용중(다른 사람) → 무시
  };

  // 예약 확정
  const onReserveConfirm = async () => {
    if (!confirmSeat || isReserving) return;
    setIsReserving(true);
    try {
      const r = await reserveSeat(confirmSeat.id);
      if (r.ok === false) {
        const msg =
          r.code === "occupied"
            ? "이미 다른 사용자가 선점했습니다. 잠시 후 다시 시도해주세요."
            : r.code === "already_reserved"
            ? "이미 다른 좌석을 사용 중입니다."
            : r.code === "invalid_seat"
            ? "존재하지 않는 좌석입니다."
            : "예약에 실패했습니다. 잠시 후 다시 시도해주세요.";
        setErr(msg);
        setConfirmSeat(null);
        return;
      }

      // 성공: 즉시 표시에 반영 + 서버 최신과 동기화
      setConfirmSeat(null);
      await loadAll();
    } catch (e) {
      console.error(e);
      setErr("예약 처리 중 오류가 발생했습니다.");
    } finally {
      setIsReserving(false);
    }
  };

  // 퇴실 확정
  const onCheckoutConfirm = async () => {
    if (!mySeatNo || isCheckingOut) return;
    setIsCheckingOut(true);
    try {
      const r = await releaseSeat(mySeatNo);
      if (r.ok === false) {
        const msg =
          r.code === "unauthorized_seat"
            ? "본인이 예약한 좌석이 아닙니다."
            : r.code === "invalid_seat"
            ? "존재하지 않는 좌석입니다."
            : "퇴실 처리에 실패했습니다. 잠시 후 다시 시도해주세요.";
        setErr(msg);
        setConfirmCheckout(false);
        return;
      }

      // 성공: 즉시 반영 + 동기화
      setConfirmCheckout(false);
      await loadAll();
    } catch (e) {
      console.error(e);
      setErr("퇴실 처리 중 오류가 발생했습니다.");
    } finally {
      setIsCheckingOut(false);
    }
  };

  if (loading) {
    return (
      <div className="readingroom-wrap">
        <div className="banner banner-center">
          <h2>열람실 신청</h2>
        </div>
        <div className="loading">불러오는 중…</div>
      </div>
    );
  }

  return (
    <div className="readingroom-wrap">
      {/* 예약/퇴실용 백드롭 */}
      {(confirmSeat || confirmCheckout) && (
        <div
          className="global-backdrop"
          onClick={() => {
            setConfirmSeat(null);
            setConfirmCheckout(false);
          }}
        />
      )}

      {/* 배너 */}
      <div className="banner banner-center">
        <h2>열람실 신청</h2>
        <div className="banner-right">
          <span className="avail-chip">예약 가능 {availableCount}석</span>
          <button className="use-guide" onClick={() => setShowGuide(true)}>
            이용안내
          </button>
        </div>
      </div>

      {/* 에러 */}
      {err && <div className="error-inline">{err}</div>}

      {/* 이용안내 우측 상단(배너 아래) 고정 */}
    {showGuide && (
      <div className="guide-popover" role="dialog" aria-modal="true">
      <div className="popover-arrow" />
      <div className="guide-title">이용 안내</div>
      <div className="guide-body">
        <p>클릭 시 자리 예약이 가능합니다.</p>
        <p>내 좌석(파란색)을 클릭하면 <b>퇴실</b>할 수 있습니다.</p>
      <ul>
        <li><b>초록</b>: 예약가능 좌석</li>
        <li><b>핑크</b>: 사용중인 좌석</li>
        <li><b style={{ color: '#3B82F6' }}>파란색</b>: 내 좌석</li>
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
              seat.status === STATUS.AVAILABLE ? "seat available" :
              seat.status === STATUS.MINE ? "seat mine" :
              "seat occupied";
            const disabled = seat.status === STATUS.OCCUPIED; // 내 좌석은 클릭 가능(퇴실)
            return (
              <button
                key={seat.id}
                className={cls}
                type="button"
                disabled={disabled}
                onClick={() => onSeatClick(seat)}
                aria-label={`${seat.label}번 좌석 ${
                  seat.status === STATUS.AVAILABLE
                    ? "예약 가능"
                    : seat.status === STATUS.MINE
                    ? "내 좌석"
                    : "사용 중"
                }`}
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
          <span><i className="chip chip-mine" /> 내 좌석</span>
        </div>
      </div>

      {/* 예약 확인 모달 */}
      {confirmSeat && (
        <div className="confirm-modal" role="dialog" aria-modal="true">
          <div className="confirm-title">예약 확인</div>
          <div className="confirm-body">
            <p><b>{confirmSeat.label}번 좌석</b>을 예약하시겠습니까?</p>
          </div>
          <div className="confirm-actions">
            <button className="primary" onClick={onReserveConfirm} disabled={isReserving}>
              {isReserving ? "신청 중…" : "신청하기"}
            </button>
            <button className="ghost" onClick={() => setConfirmSeat(null)}>취소</button>
          </div>
        </div>
      )}

      {/* 퇴실 확인 모달 */}
      {confirmCheckout && (
        <div className="confirm-modal" role="dialog" aria-modal="true">
          <div className="confirm-title">퇴실 확인</div>
          <div className="confirm-body">
            <p><b>{String(mySeatNo).padStart(2, "0")}번 좌석</b>에서 퇴실하시겠습니까?</p>
          </div>
          <div className="confirm-actions">
            <button className="primary" onClick={onCheckoutConfirm} disabled={isCheckingOut}>
              {isCheckingOut ? "처리 중…" : "퇴실"}
            </button>
            <button className="ghost" onClick={() => setConfirmCheckout(false)}>취소</button>
          </div>
        </div>
      )}
    </div>
  );
}
