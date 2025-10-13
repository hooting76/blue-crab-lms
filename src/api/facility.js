import axios from "axios";

const api = axios.create({ baseURL: "/api" });

// ---------- 시설 ----------
export const postFacilities = () =>
  api.post("/facilities").then(r => r.data);

export const postDailySchedule = (facilityIdx, dateYMD) =>
  api.post(`/facilities/${facilityIdx}/daily-schedule`, null, {
    params: { date: dateYMD },
  }).then(r => r.data);

export const postAvailability = (facilityIdx, start, end) =>
  api.post(`/facilities/${facilityIdx}/availability`, null, {
    params: { startTime: start, endTime: end },
  }).then(r => r.data);

// ---------- 예약 ----------
export const postReservation = (payload) =>
  api.post("/reservations", payload).then(r => r.data);

export const postMyReservations = () =>
  api.post("/reservations/my").then(r => r.data);

export const postMyReservationsByStatus = (statusEnum) =>
  api.post(`/reservations/my/status/${statusEnum}`).then(r => r.data);

export const postReservationDetail = (reservationIdx) =>
  api.post(`/reservations/${reservationIdx}`).then(r => r.data);

export const deleteReservation = (reservationIdx) =>
  api.delete(`/reservations/${reservationIdx}`).then(r => r.data);
