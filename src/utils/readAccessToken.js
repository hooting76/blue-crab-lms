// src/utils/readAccessToken.js
// BlueCrab LMS - 로컬스토리지에서 액세스 토큰 읽기 유틸(호환/정규화 강화)

function normalizeToken(raw) {
  if (!raw || typeof raw !== 'string') return '';
  let t = raw.trim();

  // 양끝 따옴표 제거
  if ((t.startsWith('"') && t.endsWith('"')) || (t.startsWith("'") && t.endsWith("'"))) {
    t = t.slice(1, -1);
  }
  // 'Bearer ' 접두어 제거
  if (/^Bearer\s+/i.test(t)) t = t.replace(/^Bearer\s+/i, '');

  return t.trim();
}

function parseJsonSafe(raw) {
  try {
    const obj = JSON.parse(raw);
    // 이중 문자열 케이스 방어
    if (typeof obj === 'string') {
      try { return JSON.parse(obj); } catch { /* noop */ }
    }
    return obj;
  } catch {
    return null;
  }
}

export function readAccessToken() {
  try {
    // 1) 과거 단일 키 우선
    const legacy =
      localStorage.getItem('authToken') ||
      localStorage.getItem('accessToken') ||
      localStorage.getItem('sessionToken');
    const legacyNorm = normalizeToken(legacy);
    if (legacyNorm) return legacyNorm;

    // 2) 'user' JSON 내부(응답 래퍼)
    const raw = localStorage.getItem('user');
    if (!raw) return '';
    const userObj = typeof raw === 'string' ? parseJsonSafe(raw) : raw;
    if (!userObj || typeof userObj !== 'object') return '';

    const candidates = [
      userObj?.data?.accessToken,
      userObj?.data?.sessionToken,
      userObj?.accessToken,
      userObj?.sessionToken,
    ];

    for (const c of candidates) {
      const norm = normalizeToken(c);
      if (norm) return norm;
    }
    return '';
  } catch {
    return '';
  }
}
