// component/common/Course/CourseApply.jsx
import React, { useEffect, useMemo, useState } from 'react';
import '../../../css/Course/CourseApply.css';

import {
  fetchEligibleLectures,   // { list, pagination, counts, studentInfo }
  fetchMyEnrollments,      // { list, totalElements, totalPages, page, size }
  enrollLecture,           // 성공 시 엔티티 그대로 반환
  checkEnrollment,         // { enrolled:boolean }
  // cancelEnrollment,      // 서버 취소 API 나오면 사용
} from '../../../src/api/courseRegistrationApi';

/** 학부-학과 매핑(프로젝트 내 정의와 동일) */
const FACULTY_DEPTS = {
  '학부 제한 없음': ['학과 제한 없음'],
  '공학부': ['학과 제한 없음', '컴퓨터공학', '기계공학', '전자공학', 'ICT융합'],
  '인문학부': ['학과 제한 없음', '철학', '국어국문', '역사학', '경영', '경제', '정치외교', '영어영문'],
  '자연과학부': ['학과 제한 없음', '물리학', '수학', '분자화학'],
  '보건': ['학과 제한 없음', '간호학', '치위생', '약학과', '보건정책학'],
  '해양학부': ['학과 제한 없음', '항해학과', '해양경찰', '해군사관', '도선학과', '해양수산학', '조선학과'],
};

/** 현재 저장 방식(UserContext.jsx)학생 식별 파서 */
function resolveStudent() {
  try {
    const raw = JSON.parse(localStorage.getItem('user') || 'null');
    const u = raw?.data?.user ?? raw?.user;
    const uid = Number(u?.id);
    const flag = Number(u?.userStudent); // 0=학생, 1=교수
    if (Number.isFinite(uid)) {
      return { studentId: uid, studentIdx: uid, userStudent: flag };
    }
  } catch { /* ignore */ }

  const sId = Number(localStorage.getItem('studentId'));
  const sIdx = Number(localStorage.getItem('studentIdx'));
  const flag = Number(localStorage.getItem('userStudent'));
  if (Number.isFinite(sId) && Number.isFinite(sIdx)) return { studentId: sId, studentIdx: sIdx, userStudent: flag };
  if (Number.isFinite(sId))  return { studentId: sId, studentIdx: sId, userStudent: flag };
  if (Number.isFinite(sIdx)) return { studentId: sIdx, studentIdx: sIdx, userStudent: flag };

  throw new Error('학생 식별자(studentId/studentIdx)가 없습니다. 로그인 정보를 확인해주세요.');
}

export default function CourseApply({ setCurrentPage }) {
  /* -------------------------- 검색/필터 상태 -------------------------- */
  const years = useMemo(() => [2025, 2024, 2023], []);
  const [year, setYear] = useState(2025);
  const [term, setTerm] = useState('2학기');
  const [majorType, setMajorType] = useState('전체');          // '전공' | '교양' | '전체'
  const [faculty, setFaculty] = useState('학부 제한 없음');
  const [department, setDepartment] = useState('학과 제한 없음');
  const [qName, setQName] = useState('');                      // 교과목명

  /* -------------------------- 서버 데이터 -------------------------- */
  const [courses, setCourses] = useState([]);     // 수강 가능(eligible) 목록 (표시용)
  const [myEnrolls, setMyEnrolls] = useState([]); // 내 수강 목록
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  /* -------------------------- 신청/모달 상태 -------------------------- */
  const [pendingApply, setPendingApply] = useState(null);
  const [showConfirm, setShowConfirm] = useState(false);
  const [infoOpen, setInfoOpen] = useState(false);
  const [infoMsg, setInfoMsg] = useState('');
  const [openSheet, setOpenSheet] = useState(false);

  /* -------------------------- 페이지네이션 -------------------------- */
  // 메인(개설 교과목 목록) : 10개 단위
  const [pageEligible, setPageEligible] = useState(0);
  const [totalPagesEligible, setTotalPagesEligible] = useState(1);
  const ELIGIBLE_SIZE = 10;

  // 사이드시트(내 수강목록) : 10개 단위
  const [pageMine, setPageMine] = useState(0);
  const [totalPagesMine, setTotalPagesMine] = useState(1);
  const MINE_SIZE = 10;

  /* -------------------------- 유틸 -------------------------- */
  const isApplied = (course) => myEnrolls.some((c) => c.id === course.id);
  const quotaFull = (c) => Number(c.enrolled ?? 0) >= Number(c.capacity ?? 0);

  // 학부 변경 시 학과 초기화
  useEffect(() => {
    const list = FACULTY_DEPTS[faculty] || ['학과 제한 없음'];
    if (!list.includes(department)) setDepartment('학과 제한 없음');
  }, [faculty, department]);

  /* -------------------------- 서버 조회 -------------------------- */
  const refetch = async () => {
    setLoading(true);
    setError('');
    try {
      const { studentId, studentIdx, userStudent } = resolveStudent();

      // 교수(1) 계정이면 서버 호출 차단
      if (userStudent === 1) {
        setCourses([]);
        setMyEnrolls([]);
        setError('학생 계정이 아닙니다. 수강신청은 학생만 이용할 수 있습니다.');
        setLoading(false);
        return;
      }

      // 메인/사이드 각각 페이지 기준으로 호출
      const [eligible, mine] = await Promise.all([
        fetchEligibleLectures({
          studentId,
          page: pageEligible,
          size: ELIGIBLE_SIZE,
          // TODO: 백엔드 연동 시 아래 필드 전달
          // year, term, majorType, faculty, department, qName
        }),
        fetchMyEnrollments({ studentIdx, enrolled: true, page: pageMine, size: MINE_SIZE }),
      ]);

      // (임시) 프론트 필터 – 백엔드 정식 스펙 나오면 제거
      let filtered = eligible.list || [];
      if (majorType !== '전체') {
        // 현재 스키마상 전공/교양 구분 필드가 없으므로 보류(백엔드 스펙 확정 시 적용)
        filtered = filtered.filter(() => true);
      }
      if (faculty !== '학부 제한 없음') {
        filtered = filtered.filter((c) => (c.mcodeName || c.mcode || '').includes(faculty));
      }
      if (department !== '학과 제한 없음') {
        filtered = filtered.filter((c) => (c.mcodeDepName || c.mcodeDep || '').includes(department));
      }
      if (qName.trim()) {
        const key = qName.trim();
        filtered = filtered.filter((c) => (c.name || '').includes(key));
      }

      setCourses(filtered);

      // 메인 페이지 토탈 계산(서버 pagination 우선)
      const eTotalPages =
        eligible.pagination?.totalPages ??
        Math.max(1, Math.ceil((eligible.counts?.totalCount ?? filtered.length) / ELIGIBLE_SIZE));
      setTotalPagesEligible(eTotalPages);

      // 내 수강목록
      setMyEnrolls(mine.list || []);
      setTotalPagesMine(mine.totalPages || 1);
    } catch (e) {
      setError(e.message || '목록 조회 실패');
    } finally {
      setLoading(false);
    }
  };

  // 최초 & 각 페이지 변경 시 재조회
  useEffect(() => {
    refetch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageEligible, pageMine]);

  /* -------------------------- 신청 로직 -------------------------- */
  const handleRowClick = (course) => {
    if (isApplied(course)) { setInfoMsg('이미 신청완료된 과목입니다.'); setInfoOpen(true); return; }
    if (course.eligible === false) { setInfoMsg(course.eligibilityReason || '수강 자격이 없습니다.'); setInfoOpen(true); return; }
    if (quotaFull(course)) { setInfoMsg('정원이 초과되었습니다.'); setInfoOpen(true); return; }
    setPendingApply(course); setShowConfirm(true);
  };
  const handleApplyClick = (e, course) => { e.stopPropagation(); handleRowClick(course); };

  const confirmApply = async () => {
    if (!pendingApply) return;
    try {
      const { studentIdx, userStudent } = resolveStudent();
      if (userStudent === 1) { setInfoMsg('학생 계정이 아닙니다. 신청할 수 없습니다.'); setInfoOpen(true); return; }
      const dup = await checkEnrollment({ studentIdx, lecSerial: pendingApply.id });
      if (dup?.enrolled) {
        setInfoMsg('이미 신청완료된 과목입니다.'); setInfoOpen(true);
      } else {
        await enrollLecture({ studentIdx, lecSerial: pendingApply.id });
        setOpenSheet(true);
        await refetch();
      }
    } catch (e) {
      setInfoMsg(e?.payload?.message || e.message || '신청 실패'); setInfoOpen(true);
    } finally {
      setShowConfirm(false); setPendingApply(null);
    }
  };
  const cancelApply = () => { setShowConfirm(false); setPendingApply(null); };

  /* -------------------------- 시트/검색/페이지 이동 -------------------------- */
  const openAppliedSheet = () => setOpenSheet(true);
  const closeAppliedSheet = () => setOpenSheet(false);

  const cancelOne = async () => {
    setInfoMsg('신청 취소는 서버 API 준비 후 활성화됩니다.');
    setInfoOpen(true);
  };

  const doSearch = (e) => {
    e?.preventDefault();
    // 검색 조건 바뀌면 메인 1페이지로
    setPageEligible(0);
    refetch();
  };

  const goEligiblePage = (p) => {
    if (p >= 0 && p < totalPagesEligible) setPageEligible(p);
  };
  const goMinePage = (p) => {
    if (p >= 0 && p < totalPagesMine) setPageMine(p);
  };

  const deptOptions = FACULTY_DEPTS[faculty] || ['학과 제한 없음'];

  /* ------------------------------- JSX ------------------------------- */
  return (
    <div id="course-apply" className="course-apply-wrap">
      {/* 안내문 */}
      <section className="notice-card">
        <div className="notice-title">안내문</div>
        <div className="notice-body">자세한 신청 관련 공지는 학사 공지사항을 확인해 주세요.</div>
        <button className="btn ghost-link" onClick={() => setCurrentPage?.('학사공지')}>공지사항 바로가기</button>
      </section>

      {/* 검색 바 */}
      <section className="filter-card">
        <form className="filter-row" onSubmit={doSearch}>
          <label className="filter-field">
            <span className="label">학년도</span>
            <select value={year} onChange={(e) => setYear(Number(e.target.value))}>
              {years.map((y) => (<option key={y} value={y}>{y}</option>))}
            </select>
          </label>

          <label className="filter-field">
            <span className="label">학기</span>
            <select value={term} onChange={(e) => setTerm(e.target.value)}>
              <option>1학기</option>
              <option>2학기</option>
            </select>
          </label>

          <label className="filter-field">
            <span className="label">전공여부</span>
            <select value={majorType} onChange={(e) => setMajorType(e.target.value)}>
              <option value="전체">전체</option>
              <option value="전공">전공</option>
              <option value="교양">교양</option>
            </select>
          </label>

          <label className="filter-field">
            <span className="label">학부</span>
            <select value={faculty} onChange={(e) => setFaculty(e.target.value)}>
              {Object.keys(FACULTY_DEPTS).map((f) => (<option key={f} value={f}>{f}</option>))}
            </select>
          </label>

          <label className="filter-field">
            <span className="label">학과</span>
            <select value={department} onChange={(e) => setDepartment(e.target.value)}>
              {deptOptions.map((d) => (<option key={d} value={d}>{d}</option>))}
            </select>
          </label>

          <button type="submit" className="btn btn-primary query-btn">조회</button>

          <label className="filter-field grow">
            <span className="label">교과목명</span>
            <div className="name-row">
              <input value={qName} onChange={(e) => setQName(e.target.value)} placeholder="교과목명 입력" />
              <button type="button" className="btn btn-primary" onClick={doSearch}>조회</button>
            </div>
          </label>
        </form>
      </section>

      {/* 상단 바 */}
      <div className="course-list-header">
        <h3>개설 교과목 목록</h3>
        <button className="btn btn-primary" onClick={openAppliedSheet}>신청과목 확인</button>
      </div>

      {/* 로딩/에러 */}
      {error && <div style={{ color: '#b91c1c', margin: '8px 0' }}>{error}</div>}
      {loading && <div style={{ margin: '8px 0' }}>불러오는 중…</div>}

      {/* 메인 표 */}
      <section className="course-list-card">
        <div className="table-wrap">
          <table className="course-table">
            <colgroup>
              <col className="col-code" />
              <col className="col-name" />
              <col className="col-credit" />
              <col className="col-prof" />
              <col className="col-time" />
              <col className="col-cap" />
              <col className="col-action" />
            </colgroup>

            <thead>
              <tr>
                <th>교과코드</th>
                <th>교과목명</th>
                <th className="cell-center">학점</th>
                <th>담당교수</th>
                <th>강의시간</th>
                <th className="cell-center">수강인원</th>
                <th className="cell-center">신청</th>
              </tr>
            </thead>

            <tbody>
              {courses.map((c) => {
                const applied = isApplied(c);
                const full = quotaFull(c);
                const ineligible = c.eligible === false;
                return (
                  <tr
                    key={c.id}
                    className={`clickable-row ${
                      applied ? 'row-enrolled' :
                      full ? 'row-full' :
                      (ineligible ? 'row-ineligible' : '')
                    }`}
                    onClick={() => handleRowClick(c)}
                  >
                    <td><span className="pill">{c.id}</span></td>
                    <td className="cell-name">{c.name}</td>
                    <td className="cell-center">{c.credit}</td>
                    <td>{c.prof}</td>
                    <td className="cell-time">{c.time}</td>
                    <td className="cell-center">{(c.enrolled ?? 0)}/{(c.capacity ?? 0)}</td>
                    <td className="cell-center">
                      {applied ? (
                        <button className="btn secondary sm" disabled>신청완료</button>
                      ) : (
                        <button
                          className="btn btn-primary sm"
                          onClick={(e) => handleApplyClick(e, c)}
                          disabled={full || ineligible || loading}
                          title={
                            ineligible ? (c.eligibilityReason || '수강 자격이 없습니다.') :
                            full ? '정원 초과' : ''
                          }
                        >
                          신청
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
              {(!loading && courses.length === 0) && (
                <tr><td colSpan={7} style={{ textAlign:'center', height: 72, color:'#64748b' }}>표시할 과목이 없습니다.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {/* 메인 페이지네이션(10개 단위) */}
        {totalPagesEligible > 1 && (
          <div className="pagination">
            <button className="btn sm" disabled={pageEligible === 0} onClick={() => goEligiblePage(pageEligible - 1)}>
              « 이전
            </button>
            {Array.from({ length: totalPagesEligible }, (_, i) => (
              <button
                key={i}
                className={`btn sm ${pageEligible === i ? 'active' : ''}`}
                onClick={() => goEligiblePage(i)}
              >
                {i + 1}
              </button>
            ))}
            <button
              className="btn sm"
              disabled={pageEligible === totalPagesEligible - 1}
              onClick={() => goEligiblePage(pageEligible + 1)}
            >
              다음 »
            </button>
          </div>
        )}
      </section>

      {/* 수강신청 확인 모달 */}
      {showConfirm && (
        <div className="overlay" onClick={cancelApply}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h4>수강신청 확인</h4>
            <p>
              아래 과목으로 신청하시겠습니까?<br/>
              <strong>{pendingApply?.name}</strong> ({pendingApply?.credit}학점) · {pendingApply?.time} · {pendingApply?.id}
            </p>
            <div className="modal-actions">
              <button className="btn secondary" onClick={cancelApply}>취소</button>
              <button className="btn btn-primary" onClick={confirmApply}>신청</button>
            </div>
          </div>
        </div>
      )}

      {/* 알림 모달 */}
      {infoOpen && (
        <div className="overlay" onClick={() => setInfoOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h4>알림</h4>
            <p>{infoMsg}</p>
            <div className="modal-actions">
              <button className="btn btn-primary" onClick={() => setInfoOpen(false)}>확인</button>
            </div>
          </div>
        </div>
      )}

      {/* 신청목록 사이드 시트 (페이지네이션 포함) */}
      {openSheet && (
        <div className="sheet-root">
          <div className="sheet-dim" onClick={closeAppliedSheet} />
          <aside className="sheet">
            <div className="sheet-head">
              <h4>신청과목 목록</h4>
              <button className="btn secondary sm" onClick={closeAppliedSheet}>닫기</button>
            </div>

            {myEnrolls.length === 0 ? (
              <div className="sheet-empty">신청한 과목이 없습니다.</div>
            ) : (
              <>
                <ul className="sheet-list">
                  {myEnrolls.map((c) => (
                    <li key={c.id} className="sheet-item">
                      <div>
                        <div className="title">{c.name}</div>
                        <div className="meta">{c.id} · {c.prof} · {c.time}</div>
                      </div>
                      <button className="btn danger sm" onClick={cancelOne} disabled>취소</button>
                    </li>
                  ))}
                </ul>

                {/* 시트 페이지네이션 */}
                {totalPagesMine > 1 && (
                  <div className="pagination">
                    <button className="btn sm" disabled={pageMine === 0} onClick={() => goMinePage(pageMine - 1)}>« 이전</button>
                    {Array.from({ length: totalPagesMine }, (_, i) => (
                      <button
                        key={i}
                        className={`btn sm ${pageMine === i ? 'active' : ''}`}
                        onClick={() => goMinePage(i)}
                      >
                        {i + 1}
                      </button>
                    ))}
                    <button className="btn sm" disabled={pageMine === totalPagesMine - 1} onClick={() => goMinePage(pageMine + 1)}>다음 »</button>
                  </div>
                )}
              </>
            )}
          </aside>
        </div>
      )}
    </div>
  );
}
