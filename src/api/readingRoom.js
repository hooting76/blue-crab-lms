// src/api/readingRoom.js
//  API 합의서 나오기 전까지 사용하는 "임시 어댑터"입니다.
//  컴포넌트는 이 함수들만 호출합니다. 명세 확정되면 이 파일의 구현만 교체

// 좌석 DTO 타입(참고):
// { id: number, seat_no: number, state: 0|1 }  // 0=비어있음(예약 가능), 1=사용중(예약 불가)

// ────────────────────────────────────────────────────────────
// 1) 더미 좌석 생성: 80석(8x10 그리드 가정)
//    규칙: 짝수는 사용중(1, 핑크) / 홀수는 비어있음(0, 초록) 예시
// ────────────────────────────────────────────────────────────
const DUMMY_SEATS = Array.from({ length: 80 }, (_, i) => {
  const seatNo = i + 1;
  const isOccupied = seatNo % 2 === 0; // 임시 규칙
  return {
    id: seatNo,
    seat_no: seatNo,
    state: isOccupied ? 1 : 0, // 1=사용중(핑크), 0=예약가능(초록)
  };
});

// 메모리상 좌석 상태(더미 환경에서만 사용)
let memorySeats = [...DUMMY_SEATS];

// ────────────────────────────────────────────────────────────
// 2) 좌석 조회 (합의 후: GET /api/reading-room/seats 로 교체)
// ────────────────────────────────────────────────────────────
export async function getSeats() {
  // TODO(합의 후): fetch(`${BASE}/seats`, { credentials:'include' }).then(r => r.json())
  await sleep(120); // 로딩 느낌만 주기
  return structuredClone(memorySeats);
}

// ────────────────────────────────────────────────────────────
// 3) 좌석 예약 (합의 후: POST /api/reading-room/reserve { seatId })
//   - 성공: { ok:true }
//   - 이미 점유: { ok:false, code:'occupied' }
// ────────────────────────────────────────────────────────────
export async function reserveSeat(seatId) {
  // TODO(합의 후): 실제 API 호출로 교체
  await sleep(150);

  const idx = memorySeats.findIndex(s => s.id === seatId);
  if (idx === -1) return { ok: false, code: 'not_found' };

  // 이미 점유(1)면 실패 시뮬레이션
  if (memorySeats[idx].state === 1) {
    return { ok: false, code: 'occupied' };
  }

  // 성공 시: 메모리상 좌석을 점유 상태로 변경
  memorySeats[idx] = { ...memorySeats[idx], state: 1 };
  return { ok: true };
}

// (선택) 좌석 해제 — 실제 명세 나오면 필요 시 구현
export async function releaseSeat(seatId) {
  await sleep(120);
  const idx = memorySeats.findIndex(s => s.id === seatId);
  if (idx === -1) return { ok: false, code: 'not_found' };
  memorySeats[idx] = { ...memorySeats[idx], state: 0 };
  return { ok: true };
}

// 유틸
function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}
// /src/api/readingRoom.js 내부의 getSeats, reserveSeat를 실제 fetch 호출로 교체하고,
// 백엔드 응답 DTO가 다르면 여기서 프론트 표준형으로 매핑