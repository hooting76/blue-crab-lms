// [역할] 백엔드 없이도 UI 구동을 위한 더미 데이터
// [포맷] 실제 API 응답을 흉내낸 최소 필드(id, title, writer, views, date)

export const MOCK_NOTICES = {
  academy: [
    { id: 101, title: "학사 일정 공지 (9월)", writer: "교무처", views: 12, date: "2025-09-01" },
    { id: 100, title: "수강정정 안내",         writer: "교무처", views: 42, date: "2025-08-30" },
    { id:  99, title: "졸업 자격 요건 변경",   writer: "학사지원", views: 88, date: "2025-08-20" },
  ],
  admin: [
    { id: 210, title: "건물 점검(전기) 안내",  writer: "총무팀", views: 15, date: "2025-09-07" },
    { id: 209, title: "주차 등록 안내",        writer: "총무팀", views: 33, date: "2025-09-02" },
  ],
  etc: [
    { id: 310, title: "동아리 모집 공고",      writer: "학생지원", views: 55, date: "2025-09-05" },
    { id: 309, title: "도서관 야간 운영",      writer: "도서관",  views: 21, date: "2025-08-28" },
  ],
};
