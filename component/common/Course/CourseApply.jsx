import React, { useMemo, useState } from 'react';
import '../../../css/Course/CourseApply.css';

/**
 * 기존 SPA 상태 전환 로직을 건드리지 않습니다.
 * - props.currentPage / setCurrentPage 그대로
 * - 더미데이터 렌더 (API 연동 전)
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

  // 수업계획서 모달
  const [syllabus, setSyllabus] = useState(null); // {title, url}

  // 사이드 신청목록 패널(헤더 안보이게 전체 오버레이)
  const [openSheet, setOpenSheet] = useState(false);
  const [applied, setApplied] = useState([]); // 신청한 과목 배열

  // 검색/필터(현재는 UI만)
  const years = useMemo(() => [2025, 2024, 2023], []);
  const [year, setYear] = useState(2025);
  const [term, setTerm] = useState('2학기');
  const [dept, setDept] = useState('전체');
  const [q, setQ] = useState('');
  const [qName, setQName] = useState('');

  // 행 클릭 → 신청 확인 모달
  const handleRowClick = (course) => {
    setPendingApply(course);
    setShowConfirm(true);
  };

  // 신청 버튼 클릭(행과 동일)
  const handleApplyClick = (e, course) => {
    e.stopPropagation();
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

  // 수업계획서 아이콘 클릭(버블링 중단)
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
  };

  return (
    <div id="course-apply">
      {/* 안내문 카드 */}
      <section className="notice-card ca-card">
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
      <section className="filter-card ca-card">
        <form className="filter-grid" onSubmit={doSearch}>
          <label className="filter-field">
            <span className="label nowrap">학년도</span>
            <select value={year} onChange={(e) => setYear(Number(e.target.value))}>
              {years.map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </select>
          </label>

          <label className="filter-field">
            <span className="label nowrap">학기</span>
            <select value={term} onChange={(e) => setTerm(e.target.value)}>
              <option>1학기</option>
              <option>2학기</option>
            </select>
          </label>

          <label className="filter-field">
            <span className="label nowrap">개설전공</span>
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

          <label className="filter-field wide">
            <span className="label nowrap">통합검색</span>
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="학점/과목명/코드 검색"
            />
          </label>

          <label className="filter-field wide">
            <span className="label nowrap">교과목명</span>
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

      {/* 목록 헤더 + 우측 버튼(흰 배경 카드 위에) */}
      <div className="list-head">
        <h3>개설 교과목 목록</h3>
        <div className="action-card ca-card">
          <button className="btn btn-primary" onClick={openAppliedSheet}>
            신청과목 확인
          </button>
        </div>
      </div>

      {/* 테이블 카드 (가운데 정렬 + 가로스크롤 허용) */}
      <section className="table-card ca-card">
        <div className="table-scroll">
          {/* 헤더 */}
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

          {/* 본문 */}
          {courses.map((c) => (
            <div
              key={c.id}
              className="trow tbody"
              role="button"
              onClick={() => handleRowClick(c)}
              title="클릭하여 수강신청"
            >
              <div className="tcell code">
                <span className="pill">{c.id}</span>
              </div>
              <div className="tcell name">{c.name}</div>
              <div className="tcell credit">{c.credit}</div>
              <div className="tcell prof">{c.prof}</div>
              <div className="tcell time">{c.time}</div>
              <div className="tcell plan">
                <button
                  className="icon-btn"
                  title="수업계획서 보기"
                  onClick={(e) => openSyllabus(e, c)}
                >
                  📄
                </button>
              </div>
              <div className="tcell cap">{c.capacity}</div>
              <div className="tcell action">
                <button className="btn btn-primary sm" onClick={(e) => handleApplyClick(e, c)}>
                  신청
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* 신청 확인 모달 */}
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

      {/* 수업계획서 모달 */}
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
                    <div className="title">{c.name}</div>
                    <div className="meta">
                      {c.id} · {c.prof} · {c.time}
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
