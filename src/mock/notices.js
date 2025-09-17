// [역할] 백엔드 없이도 UI 구동을 위한 더미 데이터
// [포맷] 실제 API 응답을 흉내낸 최소 필드(id, title, writer, views, date)

const MOCK_NOTICES = [
  // 학사공지
  { id: 247, category: "academy", title: "2025학년도 1학기 학자예탁 등록금 납부 안내", author: "교무처", views: 2548, createdAt: "2025-03-18" },
  { id: 246, category: "academy", title: "신입생 오리엔테이션 안내 (셔틀 시간 포함)", author: "학생지원팀", views: 2120, createdAt: "2025-03-17" },
  { id: 245, category: "academy", title: "2025년 신입생 수강신청 추가 등록 안내", author: "교무처", views: 1876, createdAt: "2025-03-15" },
  { id: 244, category: "academy", title: "전공 배정 신청 일정 변경 안내", author: "교무처", views: 2548, createdAt: "2025-03-13" },
  { id: 243, category: "academy", title: "교내 식당 운영 시간 변경 및 메뉴 리뉴얼 안내", author: "총무과", views: 2520, createdAt: "2025-03-12" },

  // 행정공지
  { id: 242, category: "admin", title: "2025-1학기 휴·복학 신청 기간 연장 안내", author: "학적팀", views: 2548, createdAt: "2025-03-11" },
  { id: 241, category: "admin", title: "캠퍼스 내 분실물 접수 창구 변경 공지", author: "총무과", views: 2548, createdAt: "2025-03-10" },
  { id: 240, category: "admin", title: "학내 차량 통행 규정 개정 안내", author: "시설관리팀", views: 1701, createdAt: "2025-03-09" },
  { id: 239, category: "admin", title: "비즈니스 허브 구역 재정비 공지 및 공사 일정 안내", author: "학복위", views: 2548, createdAt: "2025-03-08" },

  // 기타공지
  { id: 238, category: "etc", title: "교내 벚꽃 사진 공모전 안내", author: "학생처", views: 980, createdAt: "2025-03-18" },
  { id: 237, category: "etc", title: "동아리 신규 가입자 모집 (상반기)", author: "동아리연합회", views: 721, createdAt: "2025-03-16" },
  { id: 236, category: "etc", title: "분실물 센터 운영시간 조정 안내", author: "총무과", views: 604, createdAt: "2025-03-14" },
];
export default MOCK_NOTICES;
