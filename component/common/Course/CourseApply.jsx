// component/common/Course/CourseApply.jsx
import React, { useMemo, useState } from 'react';
import '../../../css/Course/CourseApply.css';

export default function CourseApply({ currentPage, setCurrentPage }) {
  const [courses] = useState([
    { id: 'KOR201', major: '국어국문학과', name: '현대시론', credit: 3, prof: '김보미', time: '월·수 09:00-10:15', capacity: 40, syllabus: { url: '#', title: '현대시론 계획서' } },
    { id: 'PHI310', major: '철학과',   name: '선형대수학', credit: 2, prof: '한지훈', time: '목 13:00-14:50',  capacity: 55, syllabus: { url: '#', title: '선형대수학 계획서' } },
    { id: 'PSY105', major: '심리학과', name: '심리학의 이해', credit: 2, prof: '최유리', time: '금 10:30-12:10', capacity: 70, syllabus: { url: '#', title: '심리학의 이해 계획서' } },
  ]);

  const [pendingApply, setPendingApply] = useState(null);
  const [showConfirm, setShowConfirm] = useState(false);
  const [syllabus, setSyllabus] = useState(null);
  const [openSheet, setOpenSheet] = useState(false);
  const [applied, setApplied] = useState([]);

  const years = useMemo(() => [2025, 2024, 2023], []);
  const [year, setYear] = useState(2025);
  const [term, setTerm] = useState('2학기');
  const [dept, setDept] = useState('전체');
  const [q, setQ] = useState('');
  const [qName, setQName] = useState('');

  const handleRowClick = (course) => { setPendingApply(course); setShowConfirm(true); };
  const handleApplyClick = (e, course) => { e.stopPropagation(); setPendingApply(course); setShowConfirm(true); };
  const confirmApply = () => { if (pendingApply) setApplied(l => l.find(c => c.id===pendingApply.id)? l : [...l, pendingApply]); setShowConfirm(false); setPendingApply(null); };
  const cancelApply = () => { setShowConfirm(false); setPendingApply(null); };
  const openSyllabus = (e, course) => { e.stopPropagation(); setSyllabus(course.syllabus); };
  const closeSyllabus = () => setSyllabus(null);
  const openAppliedSheet = () => setOpenSheet(true);
  const closeAppliedSheet = () => setOpenSheet(false);
  const cancelOne = (id) => setApplied(l => l.filter(c => c.id !== id));
  const doSearch = (e) => { e.preventDefault(); };

  return (
    <div id="course-apply" className="course-page">{/* ← 추가 */}
      {/* 안내문 */}
      <section className="notice-card">
        <div className="notice-title">안내문</div>
        <div className="notice-body">자세한 신청 관련 공지는 학사 공지사항을 확인해 주세요.</div>
        <button className="btn btn-primary ghost-link" onClick={() => setCurrentPage?.('학사공지')}>
          공지사항 바로가기
        </button>
      </section>

      {/* 검색 바 */}
      <section className="filter-card">
        <form className="filter-grid course-search-wrap" onSubmit={doSearch}>{/* ← 클래스 추가 */}
          <label className="filter-field">
            <span className="label">학년도</span>
            <select value={year} onChange={(e) => setYear(Number(e.target.value))}>
              {years.map(y => <option key={y} value={y}>{y}</option>)}
            </select>
          </label>

          <label className="filter-field">
            <span className="label">학기</span>
            <select value={term} onChange={(e) => setTerm(e.target.value)}>
              <option>1학기</option><option>2학기</option>
            </select>
          </label>

          <label className="filter-field">
            <span className="label">개설전공</span>
            <select value={dept} onChange={(e) => setDept(e.target.value)}>
              <option>전체</option><option>국어국문학과</option><option>철학과</option><option>심리학과</option>
            </select>
          </label>

          <button type="submit" className="btn btn-primary query-btn">조회</button>

          <label className="filter-field wide">
            <span className="label">통합검색</span>
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="학점/과목명/코드 검색" />
          </label>

          <label className="filter-field wide">
            <span className="label">교과목명</span>
            <div className="name-row">
              <input value={qName} onChange={(e) => setQName(e.target.value)} placeholder="교과목명 입력" />
              <button type="button" className="btn btn-primary" onClick={doSearch}>조회</button>
            </div>
          </label>
        </form>
      </section>

      {/* 목록 헤더 */}
      <div className="list-head">
        <h3>개설 교과목 목록</h3>
        <button className="btn btn-primary" onClick={openAppliedSheet}>신청과목 확인</button>
      </div>

      {/* 테이블 */}
      <section className="table-card">
        <div className="table-scroll">
          <div className="trow thead">
            <div className="tcell code">교과코드</div>
            <div className="tcell name">교과목명</div>
            <div className="tcell credit">학점</div>
            <div className="tcell prof">담당교수</div>
            <div className="tcell time">강의시간</div>
            <div className="tcell plan">수업계획서</div>
            <div className="tcell cap">수강인원</div>
            <div className="tcell action">신청</div>
          </div>

          {courses.map(c => (
            <div key={c.id} className="trow tbody" role="button" onClick={() => handleRowClick(c)}>
              <div className="tcell code"><span className="pill">{c.id}</span></div>
              <div className="tcell name">{c.name}</div>
              <div className="tcell credit">{c.credit}</div>
              <div className="tcell prof">{c.prof}</div>
              <div className="tcell time">{c.time}</div>
              <div className="tcell plan">
                <button className="icon-btn" title="수업계획서 보기" onClick={(e) => openSyllabus(e, c)}>📄</button>
              </div>
              <div className="tcell cap">{c.capacity}</div>
              <div className="tcell action">
                <button className="btn btn-primary sm" onClick={(e) => handleApplyClick(e, c)}>신청</button>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* 모달들/사이드시트는 그대로… */}
      {showConfirm && (
        <div className="overlay" onClick={cancelApply}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h4>수강신청 확인</h4>
            <p>해당 <strong>“{pendingApply?.name}”</strong> 과목으로 신청하시겠습니까?</p>
            <div className="modal-actions">
              <button className="btn secondary" onClick={cancelApply}>취소</button>
              <button className="btn btn-primary" onClick={confirmApply}>신청</button>
            </div>
          </div>
        </div>
      )}

      {syllabus && (
        <div className="overlay" onClick={closeSyllabus}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h4>수업계획서</h4>
            <p className="syllabus-title">{syllabus.title}</p>
            <div className="modal-actions">
              <button className="btn secondary" onClick={closeSyllabus}>닫기</button>
              <a className="btn btn-primary" href={syllabus.url} target="_blank" rel="noreferrer">새 창으로 열기</a>
            </div>
          </div>
        </div>
      )}

      {openSheet && (
        <div className="sheet-root">
          <div className="sheet-dim" onClick={closeAppliedSheet} />
          <aside className="sheet">
            <div className="sheet-head">
              <h4>신청과목 목록</h4>
              <button className="btn secondary sm" onClick={closeAppliedSheet}>닫기</button>
            </div>
            {applied.length === 0 ? (
              <div className="sheet-empty">신청한 과목이 없습니다.</div>
            ) : (
              <ul className="sheet-list">
                {applied.map(c => (
                  <li key={c.id} className="sheet-item">
                    <div className="title">{c.name}</div>
                    <div className="meta">{c.id} · {c.prof} · {c.time}</div>
                    <button className="btn danger sm" onClick={() => cancelOne(c.id)}>취소</button>
                  </li>
                ))}
              </ul>
            )}
          </aside>
        </div>
      )}
    </div>
  );
}
