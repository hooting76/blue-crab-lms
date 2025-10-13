export const toYMD = (d) =>
  `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`;

export const toYMDHMS = (d) =>
  `${toYMD(d)} ${String(d.getHours()).padStart(2,"0")}:${String(d.getMinutes()).padStart(2,"0")}:${String(d.getSeconds()).padStart(2,"0")}`;

// 09~18: 시작 슬롯 09..17 (각 1시간 구간)
export const genBusinessSlots = () => Array.from({length:9},(_,i)=>9+i);

export function validateBusinessHours(start, end) {
  const sameDay = toYMD(start) === toYMD(end);
  const okStart = start.getHours() >= 9 && start.getHours() <= 17; // 17→18 허용
  const okEnd = end.getHours() < 18 || (end.getHours() === 18 && end.getMinutes() === 0);
  const after = end.getTime() > start.getTime();
  return sameDay && okStart && okEnd && after;
}

export function packContiguousRanges(hours){
  const s=[...new Set(hours)].sort((a,b)=>a-b);
  if(!s.length) return [];
  const out=[]; let a=s[0], p=a;
  for(let i=1;i<s.length;i++){
    if(s[i]===p+1){ p=s[i]; continue; }
    out.push([a,p+1]); a=s[i]; p=a;
  }
  out.push([a,p+1]); return out;
}
