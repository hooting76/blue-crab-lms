// ====================== common: url str workspace ======================
const BASE = 'https://bluecrab.chickenkiller.com';
const CONTEXT = '/BlueCrab-1.0.0';
export default function apiUrl(path) {
  return new URL(
    [CONTEXT.replace(/\/+$/, ''), String(path).replace(/^\/+/, '')].join('/'),
    BASE.replace(/\/+$/, '') + '/'
  ).toString();
};

// ====================== JWT utils (timeout/decode) ======================
export function b64urlToUtf8(b64url) {
  const b64 = b64url.replace(/-/g, '+').replace(/_/g, '/');
  const json = atob(b64)
    .split('')
    .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
    .join('');
  return decodeURIComponent(json);
};

export function parseJwtPayload(token) {
  try {
    const [, payload] = token.split('.');
    return JSON.parse(b64urlToUtf8(payload));
  } catch { return null; }
};

export function isTokenExpired(token, leewaySec = 30) {
  const p = parseJwtPayload(token);
  if (!p || !p.exp) return true;
  const now = Math.floor(Date.now() / 1000);
  return now >= (p.exp - leewaySec);
};

export function fmtTs(sec) {
  if (!sec) return '—';
  return new Date(sec * 1000).toLocaleString();
};

export function humanize(sec) {
  sec = Math.max(0, Math.floor(sec));
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  return (h ? `${h}h ` : '') + (m ? `${m}m ` : '') + `${s}s`;
};