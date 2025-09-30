// src/utils/readAccessToken.js
// BlueCrab LMS - 로컬스토리지에서 액세스 토큰 읽기 유틸

// 로컬스토리지에서 액세스 토큰 읽기
// - 과거 방식: 'authToken' 또는 'accessToken' 키 단일 저장
// - 현재 방식: 'user' 키에 JSON으로 저장 (마이페이지-개인정보)
export function readAccessToken() {
    // 1) 단일 키(과거 방식) 우선
    const t1 = localStorage.getItem('authToken') || localStorage.getItem('accessToken');
        if (t1) return t1;

    //2) 현재 방식: user에 JSON으로 저장
    try {
        const user = JSON.parse(localStorage.getItem('user') || 'null');
            return user?.data?.accessToken || '';
        } catch {
    return '';
  }
}
