// src/constants/facultyMapping.js
// DB 스키마 기준 시드 매핑. 필요하면 언제든 보강/수정 가능.
// 백엔드는 코드만 반환 → 화면에서 이름 주입용.

export const FACULTY = {
  '01': { 
    name: '해양학부', 
    depts: {
    '01': '항해학과',
    '02': '해양경찰', 
    '03': '해군사관',
    '04': '도선학과', 
    '05': '해양수산학', 
    '06': '조선학과',
  }},
  
  '02': { 
    name: '보건학부', 
    depts: {
    '01': '간호학', 
    '02': '치위생', 
    '03': '약학과', 
    '04': '보건정책학',
  }},
  
  '03': {
     name: '자연과학부', 
     depts: {
    '01': '물리학', 
    '02': '수학', 
    '03': '분자화학',
  }},
  
  '04': { 
    name: '인문학부',
     depts: {
    '01': '철학', 
    '02': '국어국문', 
    '03': '역사학',
    '04': '경영', 
    '05': '경제', 
    '06': '정치외교', 
    '07': '영어영문',
  }},
  
  '05': { 
    name: '공학부', 
    depts: {
    '01': '컴퓨터공학',
    '02': '기계공학',
    '03': '전자공학', 
    '04': 'ICT융합',
  }},
};

export function getFacultyName(facultyCode) {
  return FACULTY?.[String(facultyCode)?.padStart(2, '0')]?.name || null;
}

export function getDeptName(facultyCode, deptCode) {
  const f = FACULTY?.[String(facultyCode)?.padStart(2, '0')];
  if (!f) return null;
  return f.depts?.[String(deptCode)?.padStart(2, '0')] || null;
}

/** 강의 리스트에 mcode/mcodeDep 이름 주입 */
export function mapListWithNames(list = []) {
  return list.map((c) => ({
    ...c,
    mcodeName: getFacultyName(c.mcode) || c.mcode,
    mcodeDepName: getDeptName(c.mcode, c.mcodeDep) || c.mcodeDep,
  }));
}
