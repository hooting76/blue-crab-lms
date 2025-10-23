// component/common/Course/CourseApply.jsx
import React, { useEffect, useMemo, useRef, useState } from 'react';
import '../../../css/Course/CourseApply.css';

import {
  fetchEligibleLectures,
  fetchMyEnrollments,
  enrollLecture,
  checkEnrollment,
  cancelEnrollmentByIdx,
  viewCourseApplyNotice,
} from '../../../src/api/courseRegistrationApi';

import { mapListWithNames } from '../../../src/constants/facultyMapping';

/** 간단 Sanitizer (스크립트/자바스크립트 링크 제거) */
function sanitizeNoticeHTML(html = '') {
  // <script> 태그 제거
  let safe = html.replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '');
  // javascript: 링크 차단
  safe = safe.replace(/href\s*=\s*"(javascript:[^"]*)"/gi, 'href="#"');
  safe = safe.replace(/href\s*=\s*'(javascript:[^']*)'/gi, "href='#'");
  // on* 핸들러 제거 (onclick 등)
  safe = safe.replace(/\son\w+\s*=\s*(['"]).*?\1/gi, '');
  return safe;
}

/** 로그인 정보 파서 (현재 프로젝트 저장방식 존중) */
function resolveStudent() {
  try {
    const raw = JSON.parse(localStorage.getItem('user') || 'null');
    const u   = raw?.data?.user ?? raw?.user;
    const uid = Number(u?.id);
    const roleFlag = Number(u?.userStudent); // 0=학생, 1=교수
    if (Number.isFinite(uid)) return { studentId: uid, studentIdx: uid, userStudent: roleFlag };
  } catch {}
  const sId  = Number(localStorage.getItem('studentId'));
  const sIdx = Number(localStorage.getItem('studentIdx'));
  const flag = Number(localStorage.getItem('userStudent'));
  if (Number.isFinite(sId))  return { studentId: sId,  studentIdx: sId,  userStudent: flag };
  if (Number.isFinite(sIdx)) return { studentId: sIdx, studentIdx: sIdx, userStudent: flag };
  throw new Error('학생 식별자(studentId/studentIdx)가 없습니다.');
}

export default function CourseApply({ setCurrentPage }) {
  // 공지(안내문)
  const [notice, setNotice] = useState('');
  const bcRef = useRef(null);

  // 서버 데이터
  const [courses, setCourses] = useState([]);     // 수강 가능 목록(코드→이름 매핑 후)
  const [myEnrolls, setMyEnrolls] = useState([]); // 내 수강 목록(페이징)
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // 상태/모달
  const [pendingApply, setPendingApply] = useState(null);
  const [showConfirm, setShowConfirm] = useState(false);
  const [infoOpen, setInfoOpen] = useState(false);
  const [infoMsg, setInfoMsg] = useState('');
  const [openSheet, setOpenSheet] = useState(false);
  const [applying, setApplying] = useState(false); // 신청 중 버튼 방지

  // 필터(백엔드와 합의된 것만)
  const years = useMemo(() => [2025, 2024, 2023], []);
  const [year, setYear] = useState(2025);
  const [lecYear, setLecYear] = useState(null);       // 대상 학년(1~4), 선택 시 전송
  const [lecSemester, setLecSemester] = useState(2);  // 1|2 (기본 2학기)
  const [lecMajor, setLecMajor] = useState(null);     // 1=전공,0=교양,null=전체

  // 내 수강 목록 페이징
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const pageSize = 10;

  // 메인(개설과목) 클라이언트 페이징
  const [cPage, setCPage] = useState(0);
  const cPageSize = 10;
  const cTotalPages = Math.max(1, Math.ceil((courses?.length || 0) / cPageSize));
  const pagedCourses = useMemo(() => {
    const start = cPage * cPageSize;
    return courses.slice(start, start + cPageSize);
  }, [courses, cPage]);

  const isApplied = (c) => myEnrolls.some((x) => x.id === c.id);
  const quotaFull = (c) => Number(c.enrolled ?? 0) >= Number(c.capacity ?? 0);

  // 안내문 로딩 + 즉시 반영 구독
  useEffect(() => {
    (async () => {
      try {
        const res = await viewCourseApplyNotice();
        if (res?.success) setNotice(res.message || '');
      } catch {}
    })();
    try {
      bcRef.current = new BroadcastChannel('course-apply-notice');
      bcRef.current.onmessage = (ev) => {
        if (ev?.data?.type === 'NOTICE_UPDATED') setNotice(ev.data.message || '');
      };
    } catch {}
    return () => { try { bcRef.current?.close(); } catch {} };
  }, []);

  // 안내문에 포함된 링크 보정(target, rel)
  useEffect(() => {
    const container = document.querySelector('.notice-body');
    if (!container) return;
    container.querySelectorAll('a').forEach(a => {
      if (!a.getAttribute('target')) a.setAttribute('target', '_blank');
      const rel = (a.getAttribute('rel') || '').split(/\s+/);
      if (!rel.includes('noopener')) rel.push('noopener');
      if (!rel.includes('noreferrer')) rel.push('noreferrer');
      a.setAttribute('rel', rel.join(' ').trim());
    });
  }, [notice]);

  // 목록 로딩
  const refetch = async () => {
    setLoading(true); setError('');
    try {
      const { studentId, studentIdx, userStudent } = resolveStudent();
      if (userStudent === 1) {
        setCourses([]); setMyEnrolls([]);
        setError('학생 계정이 아닙니다. 수강신청은 학생만 이용할 수 있습니다.');
        return;
      }

      const eligible = await fetchEligibleLectures({
        studentId,
        page: 0,
        size: 100, // 서버 쿼리; 화면은 클라 페이징
        lecYear,
        lecSemester,
        lecMajor,
      });
      const mapped = mapListWithNames(eligible.list);

      const mine = await fetchMyEnrollments({
        studentIdx, enrolled: true, page, size: pageSize,
      });

      setCourses(mapped);
      setCPage(0); // 필터 변경 시 1페이지로
      setMyEnrolls(mine.list || []);
      setTotalPages(mine.totalPages || 1);
    } catch (e) {
      setError(e.message || '목록 조회 실패');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { refetch(); /* eslint-disable-next-line */ }, [page, lecYear, lecSemester, lecMajor]);

  // 신청 플로우
  const handleRowClick = (c) => {
    if (isApplied(c)) { setInfoMsg('이미 신청완료된 과목입니다.'); setInfoOpen(true); return; }
    if (c.eligible === false) { setInfoMsg(c.eligibilityReason || '수강 자격이 없습니다.'); setInfoOpen(true); return; }
    if (quotaFull(c)) { setInfoMsg('정원이 초과되었습니다.'); setInfoOpen(true); return; }
    setPendingApply(c); setShowConfirm(true);
  };
  const handleApplyClick = (e, c) => { e.stopPropagation(); handleRowClick(c); };

  const confirmApply = async () => {
    if (!pendingApply || applying) return;

    const lecSerial = typeof pendingApply.id === 'string' ? pendingApply.id.trim() : '';
    if (!lecSerial) {
      setInfoMsg('과목 코드가 올바르지 않습니다.');
      setInfoOpen(true);
      setShowConfirm(false);
      setPendingApply(null);
      return;
    }

    setApplying(true);
    try {
      const { studentIdx } = resolveStudent();

      // (1) 중복확인 (서버가 미지원이면 내부 폴백 사용)
      const dup = await checkEnrollment({ studentIdx, lecSerial });
      if (dup?.enrolled) {
        setInfoMsg('이미 신청완료된 과목입니다.');
        setInfoOpen(true);
        return;
      }

      // (2) 신청
      await enrollLecture({ studentIdx, lecSerial });

      // (3) 성공 UX – 시트 열고 내 목록/메인 재조회
      setOpenSheet(true);
      const mine = await fetchMyEnrollments({ studentIdx, enrolled: true, page: 0, size: pageSize });
      setPage(0);
      setMyEnrolls(mine.list || []);
      setTotalPages(mine.totalPages || 1);

      await refetch(); // 정원/버튼 상태 반영
      setInfoMsg('신청이 완료되었습니다.');
      setInfoOpen(true);
    } catch (e) {
      const msg =
        e?.payload?.message ||
        (e.status === 500 ? '서버 내부 오류로 신청에 실패했습니다.' : e.message) ||
        '신청 실패';
      setInfoMsg(msg);
      setInfoOpen(true);
    } finally {
      setApplying(false);
      setShowConfirm(false);
      setPendingApply(null);
    }
  };

  // 취소(DELETE by enrollmentIdx)
  const cancelOne = async (enrollmentIdx) => {
    if (!window.confirm('정말 취소하시겠습니까?')) return;
    try {
      await cancelEnrollmentByIdx({ enrollmentIdx });
      setInfoMsg('취소되었습니다.');
      setInfoOpen(true);
      await refetch();
    } catch (e) {
      setInfoMsg(e?.payload?.message || e.message || '취소 실패');
      setInfoOpen(true);
    }
  };

  const handleSearch = async (e) => { e?.preventDefault(); setPage(0); setCPage(0); await refetch(); };

  // 렌더
  return (
    <div id="course-apply" className="course-apply-wrap">

      {/* 안내문 카드 (HTML 렌더 + sanitizer) */}
      <section className="notice-card">
        <div className="notice-title">안내문</div>
        <div
          className="notice-body"
          dangerouslySetInnerHTML={{
            __html: sanitizeNoticeHTML(
              notice && notice.trim()
                ? notice
                : '자세한 신청 관련 공지는 학사 공지사항을 확인해 주세요.'
            ),
          }}
        />
        <button className="btn ghost-link" onClick={() => setCurrentPage?.('학사공지')}>
          공지사항 바로가기
        </button>
      </section>

      {/* 필터 (백엔드 연동되는 항목만) */}
      <section className="filter-card">
        <form className="filter-row" onSubmit={handleSearch}>
          <label className="filter-field">
            <span className="label">학년도</span>
            <select value={year} onChange={(e) => setYear(Number(e.target.value))}>
              {years.map((y) => <option key={y} value={y}>{y}</option>)}
            </select>
          </label>

          <label className="filter-field">
            <span className="label">학기</span>
            <select value={lecSemester ?? ''} onChange={(e) => setLecSemester(e.target.value ? Number(e.target.value) : null)}>
              <option value="">전체</option>
              <option value="1">1학기</option>
              <option value="2">2학기</option>
            </select>
          </label>

          <label className="filter-field">
            <span className="label">대상 학년</span>
            <select value={lecYear ?? ''} onChange={(e) => setLecYear(e.target.value ? Number(e.target.value) : null)}>
              <option value="">전체</option>
              <option value="1">1학년</option>
              <option value="2">2학년</option>
              <option value="3">3학년</option>
              <option value="4">4학년</option>
            </select>
          </label>

          <label className="filter-field">
            <span className="label">전공/교양</span>
            <select value={lecMajor ?? ''} onChange={(e) => {
              const v = e.target.value;
              setLecMajor(v === '' ? null : Number(v)); // ''|0|1
            }}>
              <option value="">전체</option>
              <option value="1">전공</option>
              <option value="0">교양</option>
            </select>
          </label>

          <button type="submit" className="btn btn-primary query-btn">조회</button>
        </form>
      </section>

      {/* 상단 바 */}
      <div className="course-list-header">
        <h3>개설 교과목 목록</h3>
        <button className="btn btn-primary" onClick={() => setOpenSheet(true)}>신청과목 확인</button>
      </div>

      {/* 상태 */}
      {error && <div style={{ color:'#b91c1c', margin:'8px 0' }}>{error}</div>}
      {loading && <div style={{ margin:'8px 0' }}>불러오는 중…</div>}

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
              {pagedCourses.map((c) => {
                const applied = isApplied(c);
                const full = quotaFull(c);
                const ineligible = c.eligible === false;

                return (
                  <tr key={c.id}
                      className={`clickable-row ${applied ? 'row-enrolled' : full ? 'row-full' : (ineligible ? 'row-ineligible' : '')}`}
                      onClick={() => handleRowClick(c)}>
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
                          disabled={full || ineligible || loading || applying}
                          title={ineligible ? (c.eligibilityReason || '수강 자격이 없습니다.') : (full ? '정원 초과' : '')}
                        >신청</button>
                      )}
                    </td>
                  </tr>
                );
              })}
              {(!loading && pagedCourses.length === 0) && (
                <tr><td colSpan={7} style={{ textAlign:'center', height:72, color:'#64748b' }}>표시할 과목이 없습니다.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {/* 메인 표 페이지네이션 */}
        {cTotalPages > 1 && (
          <div className="pagination" style={{ marginTop: 12 }}>
            <button className="btn sm" disabled={cPage === 0} onClick={() => setCPage(cPage - 1)}>« 이전</button>
            {Array.from({ length: cTotalPages }, (_, i) => (
              <button key={i}
                className={`btn sm ${cPage === i ? 'active' : ''}`}
                onClick={() => setCPage(i)}
                aria-current={cPage === i ? 'page' : undefined}>
                {i + 1}
              </button>
            ))}
            <button className="btn sm" disabled={cPage === cTotalPages - 1} onClick={() => setCPage(cPage + 1)}>다음 »</button>
          </div>
        )}
      </section>

      {/* 신청 확인 모달 */}
      {showConfirm && (
        <div className="overlay" onClick={() => setShowConfirm(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h4>수강신청 확인</h4>
            <p>
              아래 과목으로 신청하시겠습니까?<br/>
              <strong>{pendingApply?.name}</strong> ({pendingApply?.credit}학점) · {pendingApply?.time} · {pendingApply?.id}
            </p>
            <div className="modal-actions">
              <button className="btn secondary" onClick={() => setShowConfirm(false)}>취소</button>
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

      {/* 신청목록 사이드 시트 + 페이지네이션 */}
      {openSheet && (
        <div className="sheet-root">
          <div className="sheet-dim" onClick={() => setOpenSheet(false)} />
          <aside className="sheet">
            <div className="sheet-head">
              <h4>신청과목 목록</h4>
              <button className="btn secondary sm" onClick={() => setOpenSheet(false)}>닫기</button>
            </div>

            {myEnrolls.length === 0 ? (
              <div className="sheet-empty">신청한 과목이 없습니다.</div>
            ) : (
              <>
                <ul className="sheet-list">
                  {myEnrolls.map((c) => (
                    <li key={c.enrollmentIdx || c.id} className="sheet-item">
                      <div>
                        <div className="title">{c.name}</div>
                        <div className="meta">{c.id} · {c.prof} · {c.time}</div>
                      </div>
                      <button className="btn danger sm" onClick={() => cancelOne(c.enrollmentIdx)}>
                        취소
                      </button>
                    </li>
                  ))}
                </ul>

                {totalPages > 1 && (
                  <div className="pagination">
                    <button className="btn sm" disabled={page === 0} onClick={() => setPage(page - 1)}>« 이전</button>
                    {Array.from({ length: totalPages }, (_, i) => (
                      <button key={i}
                        className={`btn sm ${page === i ? 'active' : ''}`}
                        onClick={() => setPage(i)}
                        aria-current={page === i ? 'page' : undefined}>
                        {i + 1}
                      </button>
                    ))}
                    <button className="btn sm" disabled={page === totalPages - 1} onClick={() => setPage(page + 1)}>다음 »</button>
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
