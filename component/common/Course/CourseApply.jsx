// component/common/Course/CourseApply.jsx
import React, { useMemo, useState } from 'react';
import '../../../css/Course/CourseApply.css';

/**
 * 기존 SPA 상태 전환 로직을 건드리지 않습니다.
 * - props.currentPage / setCurrentPage 는 그대로 둠
 * - 더미데이터로 렌더 (API 연동 전)
 */
export default function CourseApply({ currentPage, setCurrentPage }) {
  // 더미 과목 목록
  const [courses] = useState([
    {
      id: 'KOR201',
      major: '국어국문학과',
      name: '현대시론',
      credit: 3,
      prof: '김보미',
      time: '월·수 09:00-10:15',
      capacity: 40,
      syllabus: { url: '#', title: '현대시론 계획서' },
    },
    {
      id: 'PHI310',
      major: '철학과',
      name: '선형대수학',
      credit: 2,
      prof: '한지훈',
      time: '목 13:00-14:50',
      capacity: 55,
      syllabus: { url: '#', title: '선형대수학 계획서' },
    },
    {
      id: 'PSY105',
      major: '심리학과',
      name: '심리학의 이해',
      credit: 2,
      prof: '최유리',
      time: '금 10:30-12:10',
      capacity: 70,
      syllabus: { url: '#', title: '심리학의 이해 계획서' },
    },
  ]);

  // 선택/모달 상태
  const [pendingApply, setPendingApply] = useState(null); // {id, name}
  const [showConfirm, setShowConfirm] = useState(false);

  // "이미 신청" 알림 모달
  const [infoOpen, setInfoOpen] = useState(false);
  const [infoMsg, setInfoMsg] = useState('');

  // 수업계획서 모달
  const [syllabus, setSyllabus] = useState(null); // {title, url}

  // 사이드 신청목록 패널(헤더 안보이게 전체 오버레이)
  const [openSheet, setOpenSheet] = useState(false);
  const [applied, setApplied] = useState([]); // 신청한 과목 배열

  // 이미 신청했는지 체크
  const isApplied = (course) => applied.some((c) => c.id === course.id);

  // 검색/필터(현재는 UI만)
  const years = useMemo(() => [2025, 2024, 2023], []);
  const [year, setYear] = useState(2025);
  const [term, setTerm] = useState('2학기');
  const [dept, setDept] = useState('전체');
  const [q, setQ] = useState('');
  const [qName, setQName] = useState('');

  // 행 클릭 → 신청 확인 모달(이미 신청이면 안내)
  const handleRowClick = (course) => {
    if (isApplied(course)) {
      setInfoMsg('이미 신청완료된 과목입니다.');
      setInfoOpen(true);
      return;
    }
    setPendingApply(course);
    setShowConfirm(true);
  };

  // 신청 버튼 클릭(행과 동일, 버블링 방지)
  const handleApplyClick = (e, course) => {
    e.stopPropagation();
    if (isApplied(course)) {
      setInfoMsg('이미 신청완료된 과목입니다.');
      setInfoOpen(true);
      return;
    }
    setPendingApply(course);
    setShowConfirm(true);
  };

  // 확인 모달 승인
  const confirmApply = () => {
    if (pendingApply) {
      setApplied((list) =>
        list.find((c) => c.id === pendingApply.id) ? list : [...list, pendingApply]
      );
    }
    setShowConfirm(false);
    setPendingApply(null);
  };
  const cancelApply = () => {
    setShowConfirm(false);
    setPendingApply(null);
  };

  // 수업계획서 아이콘 클릭(버블링 중단) → 신청 아님, 계획서만
  const openSyllabus = (e, course) => {
    e.stopPropagation();
    setSyllabus(course.syllabus);
  };
  const closeSyllabus = () => setSyllabus(null);

  // 신청목록 사이드 시트
  const openAppliedSheet = () => setOpenSheet(true);
  const closeAppliedSheet = () => setOpenSheet(false);
  const cancelOne = (id) => setApplied((list) => list.filter((c) => c.id !== id));

  // 검색/조회 버튼 (UI만, 실제 필터링은 미구현)
  const doSearch = (e) => {
    e.preventDefault();
    // TODO: 추후 API 연동
  };

  return (
    <div id="course-apply" className="course-apply-wrap">
      {/* 안내문 카드 */}
      <section className="notice-card">
        <div className="notice-title">안내문</div>
        <div className="notice-body">
          자세한 신청 관련 공지는 학사 공지사항을 확인해 주세요.
        </div>
        <button
          className="btn btn-primary ghost-link"
          onClick={() => setCurrentPage?.('학사공지')}
          title="공지사항 바로가기"
        >
          공지사항 바로가기
        </button>
      </section>

      {/* 검색 바 */}
      <section className="filter-card">
        <form className="filter-row" onSubmit={doSearch}>
          <label className="filter-field">
            <span className="label">학년도</span>
            <select value={year} onChange={(e) => setYear(Number(e.target.value))}>
              {years.map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
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
            <span className="label">개설전공</span>
            <select value={dept} onChange={(e) => setDept(e.target.value)}>
              <option>전체</option>
              <option>국어국문학과</option>
              <option>철학과</option>
              <option>심리학과</option>
            </select>
          </label>

          <button type="submit" className="btn btn-primary query-btn">
            조회
          </button>

          <label className="filter-field grow">
            <span className="label">통합검색</span>
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="학점/과목명/코드 검색"
            />
          </label>

          <label className="filter-field grow">
            <span className="label">교과목명</span>
            <div className="name-row">
              <input
                value={qName}
                onChange={(e) => setQName(e.target.value)}
                placeholder="교과목명 입력"
              />
              <button type="button" className="btn btn-primary" onClick={doSearch}>
                조회
              </button>
            </div>
          </label>
        </form>
      </section>

      {/* 목록 헤더 + 버튼 (중앙 흰색 카드) */}
      <div className="course-list-header">
        <h3>개설 교과목 목록</h3>
        <button className="btn btn-primary" onClick={openAppliedSheet}>
          신청과목 확인
        </button>
      </div>

      {/* 표 */}
      <section className="course-list-card">
        <div className="table-wrap">
          <table className="course-table">
            <colgroup>
              <col className="col-code" />
              <col className="col-name" />
              <col className="col-credit" />
              <col className="col-prof" />
              <col className="col-time" />
              <col className="col-plan" />
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
                <th className="cell-center">수업계획서</th>
                <th className="cell-center">수강인원</th>
                <th className="cell-center">신청</th>
              </tr>
            </thead>

            <tbody>
              {courses.map((c) => (
                <tr
                  key={c.id}
                  className="clickable-row"
                  onClick={() => handleRowClick(c)}
                >
                  <td>
                    <span className="pill">{c.id}</span>
                  </td>
                  <td className="cell-name">{c.name}</td>
                  <td className="cell-center">{c.credit}</td>
                  <td>{c.prof}</td>
                  <td className="cell-time">{c.time}</td>
                  <td className="cell-center">
                    <button
                      className="icon-btn"
                      title="수업계획서 보기"
                      onClick={(e) => openSyllabus(e, c)}
                    >
                      📄
                    </button>
                  </td>
                  <td className="cell-center">{c.capacity}</td>
                  <td className="cell-center">
                    <button
                      className="btn btn-primary sm"
                      onClick={(e) => handleApplyClick(e, c)}
                    >
                      신청
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* 수강신청 확인 모달 */}
      {showConfirm && (
        <div className="overlay" onClick={cancelApply}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h4>수강신청 확인</h4>
            <p>
              해당 <strong>“{pendingApply?.name}”</strong> 과목으로 신청하시겠습니까?
            </p>
            <div className="modal-actions">
              <button className="btn secondary" onClick={cancelApply}>
                취소
              </button>
              <button className="btn btn-primary" onClick={confirmApply}>
                신청
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 이미 신청 완료 안내 모달 */}
      {infoOpen && (
        <div className="overlay" onClick={() => setInfoOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h4>알림</h4>
            <p>{infoMsg}</p>
            <div className="modal-actions">
              <button className="btn btn-primary" onClick={() => setInfoOpen(false)}>
                확인
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 수업계획서 모달 (신청 아님) */}
      {syllabus && (
        <div className="overlay" onClick={closeSyllabus}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h4>수업계획서</h4>
            <p className="syllabus-title">{syllabus.title}</p>
            <div className="modal-actions">
              <button className="btn secondary" onClick={closeSyllabus}>
                닫기
              </button>
              <a className="btn btn-primary" href={syllabus.url} target="_blank" rel="noreferrer">
                새 창으로 열기
              </a>
            </div>
          </div>
        </div>
      )}

      {/* 신청목록 사이드시트(헤더 포함 전면 오버레이 → 헤더 안 보임 효과) */}
      {openSheet && (
        <div className="sheet-root">
          <div className="sheet-dim" onClick={closeAppliedSheet} />
          <aside className="sheet">
            <div className="sheet-head">
              <h4>신청과목 목록</h4>
              <button className="btn secondary sm" onClick={closeAppliedSheet}>
                닫기
              </button>
            </div>
            {applied.length === 0 ? (
              <div className="sheet-empty">신청한 과목이 없습니다.</div>
            ) : (
              <ul className="sheet-list">
                {applied.map((c) => (
                  <li key={c.id} className="sheet-item">
                    <div>
                      <div className="title">{c.name}</div>
                      <div className="meta">
                        {c.id} · {c.prof} · {c.time}
                      </div>
                    </div>
                    <button className="btn danger sm" onClick={() => cancelOne(c.id)}>
                      취소
                    </button>
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
