// src/utils/readAccessToken.js
// BlueCrab LMS - 로컬스토리지에서 액세스 토큰 읽기 유틸

export function readAccessToken() {
    // 1) 단일 키(과거 방식) 우선
    const t1 = localStorage.getItem('authToken') || 
    localStorage.getItem('accessToken')||
    localStorage.getItem('sessionToken');
        if (t1) return t1;

        // 2) 'user' JSON 내부 (응답 래퍼 보관 방식)
    try {
        const raw = localStorage.getItem('user');
            if (!raw) return '';

    // 서버가 이미 문자열(JSON)로 저장한 경우
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw;

    // 케이스별 호환
    return (
        obj?.data?.accessToken ||
        obj?.data?.sessionToken ||                        // 표준 응답 래퍼 방식
        obj?.accessToken ||                               // 혹시 data 없이 저장된 경우
        obj?.sessionToken ||
        ''
    );
  } catch {
    return '';
  }
}
