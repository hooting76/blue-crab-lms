// src/component/common/Facilities/ReadingRoom.jsx
// 목적: 열람실 좌석 신청 화면(UI + 더미 연동)
// ▸ API 명세 전: /api/readingRoom.js의 더미 함수 호출로 동작
// ▸ API 명세 후: /api/readingRoom.js 내부 구현만 fetch로 교체

import React, { useEffect, useMemo, useState } from "react";
import { getSeats, reserveSeat } from "../../../src/api/readingRoom";
import "../../../css/Facilities/ReadingRoom.css";

const STATUS = {
  OCCUPIED: "occupied",   // 1 = 사용중(핑크)
  AVAILABLE: "available", // 0 = 예약가능(초록)
};

export default function ReadingRoom() {
  // 좌석 목록(프론트 표준형 {id, label, status})
  const [seats, setSeats] = useState([]);
  // 로딩/에러
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  // 모달 상태
  const [showGuide, setShowGuide] = useState(false); // 이용안내 모달
  const [confirmSeat, setConfirmSeat] = useState(null); // 예약 확인 모달 대상 좌석

  // ────────────────────────────────────────────────────────────
  // 초기 로딩: 더미 API → 프론트 표준형으로 매핑
  // 백엔드 DTO: { id, seat_no, state(0/1) }
  // 프론트 매핑: status = 1 ? 'occupied' : 'available'
  // ────────────────────────────────────────────────────────────
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
    return () => {
      alive = false;
    };
  }, []);

  // 예약 가능 좌석 수
  const availableCount = useMemo(
    () => seats.filter((s) => s.status === STATUS.AVAILABLE).length,
    [seats]
  );

  // 좌석 클릭(초록만 가능) → 상단 확인 모달 오픈
  const onSeatClick = (seat) => {
    if (seat.status !== STATUS.AVAILABLE) return;
    setConfirmSeat(seat);
  };

  // 확인 모달에서 "신청하기"
  const onReserveConfirm = async () => {
    if (!confirmSeat) return;
    const r = await reserveSeat(confirmSeat.id);
    if (r.ok === false) {
      // 선점 등 실패 시 메시지 노출(상단 에러 영역)
      setErr(
        r.code === "occupied"
          ? "이미 다른 사용자가 선점했습니다. 새로고침 후 다시 시도해주세요."
          : "예약에 실패했습니다. 잠시 후 다시 시도해주세요."
      );
      setConfirmSeat(null);
      return;
    }

    // 성공: 해당 좌석만 즉시 핑크로 반영(낙관적 업데이트)
    setSeats((prev) =>
      prev.map((s) =>
        s.id === confirmSeat.id ? { ...s, status: STATUS.OCCUPIED } : s
      )
    );
    setConfirmSeat(null);
  };

  // ────────────────────────────────────────────────────────────
  // 렌더
  // ────────────────────────────────────────────────────────────
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
      {/* 상단 배너: 제목 가운데 + 우측 정보 */}
      <div className="banner banner-center">
        <h2>열람실 신청</h2>
        <div className="banner-right">
          <span className="avail-chip">예약 가능 {availableCount}석</span>
          <button className="use-guide" onClick={() => setShowGuide(true)}>
            이용안내
          </button>
        </div>
      </div>

      {/* 에러 안내(있을 때만) */}
      {err && <div className="error-inline">{err}</div>}

      {/* 상단 이용안내 모달(배너 아래 중앙) */}
      {showGuide && (
        <div className="top-modal" role="dialog" aria-modal="true">
          <div className="top-modal__content">
            <div className="top-modal__title">이용 안내</div>
            <div className="top-modal__body">
              <p>클릭 시 자리 예약이 가능합니다.</p>
              <ul>
                <li>
                  <b>초록</b>: 예약가능 좌석
                </li>
                <li>
                  <b>핑크</b>: 사용중인 좌석
                </li>
              </ul>
            </div>
            <div className="top-modal__actions">
              <button className="primary" onClick={() => setShowGuide(false)}>
                확인
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 예약 확인 모달(배너 아래 중앙) */}
      {confirmSeat && (
        <div className="top-modal" role="dialog" aria-modal="true">
          <div className="top-modal__content">
            <div className="top-modal__title">예약 확인</div>
            <div className="top-modal__body">
              <p>
                <b>일반열람실 {confirmSeat.label}번</b> 좌석으로 예약하시겠습니까?
              </p>
            </div>
            <div className="top-modal__actions">
              <button className="primary" onClick={onReserveConfirm}>
                신청하기
              </button>
              <button className="ghost" onClick={() => setConfirmSeat(null)}>
                취소
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 좌석 카드 */}
      <div className="room-card">
        <div className="room-header">
          <span className="gate">출구</span>
        </div>

        {/* 좌석 그리드: 기본 10열(가로 확장) */}
        <div
          className="seat-grid"
          role="grid"
          aria-label="열람실 좌석"
          style={{ "--cols": 10 }} /* 기본 10열 */
        >
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
                aria-label={`${seat.label}번 좌석 ${
                  seat.status === STATUS.AVAILABLE ? "예약 가능" : "사용 중"
                }`}
                title={`${seat.label}번`}
              >
                {seat.label}
              </button>
            );
          })}
        </div>

        {/* 범례 */}
        <div className="legend">
          <span>
            <i className="chip chip-available" /> 예약 가능
          </span>
          <span>
            <i className="chip chip-occupied" /> 사용 중
          </span>
        </div>
      </div>
    </div>
  );
}
