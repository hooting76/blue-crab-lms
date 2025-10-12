import React, { useEffect, useMemo, useState } from "react";
import "../../../css/Course/CourseApply.css";

/**
 * 수강신청 페이지 (UI 더미 데이터 기반)
 * - 행/버튼 클릭으로 신청
 * - 수업계획서 아이콘(📄)은 계획서만 표시(신청 아님)
 * - "신청과목 확인" 우측 드로어 열릴 때 헤더 숨김 (html.drawer-open)
 */
export default function CourseApply() {
  // ===== 더미 데이터 =====
  const [year, setYear] = useState("2025");
  const [semester, setSemester] = useState("2학기");
  const [dept, setDept] = useState("전체");
  const [qKeyword, setQKeyword] = useState("");
  const [qCourse, setQCourse] = useState("");

  const allCourses = useMemo(
    () => [
      {
        id: "KOR201",
        type: "전공",
        major: "국어국문학과",
        name: "현대시론",
        credit: 3,
        prof: "김보미",
        time: "월·수 09:00-10:15",
        seats: 40,
        weekday: "월수",
        plan: "현대시의 핵심 이론과 작품 분석…",
      },
      {
        id: "PHI310",
        type: "전공",
        major: "철학과",
        name: "선형대수학",
        credit: 2,
        prof: "한지훈",
        time: "목 13:00-14:50",
        seats: 55,
        weekday: "목",
        plan: "벡터공간/행렬/고유값 개념의 철학적 응용…",
      },
      {
        id: "PSY105",
        type: "교양",
        major: "심리학과",
        name: "심리학의 이해",
        credit: 2,
        prof: "최유리",
        time: "금 10:30-12:10",
        seats: 70,
        weekday: "금",
        plan: "심리학 전반의 개론(지각/기억/학습/동기)…",
      },
    ],
    []
  );

  // ===== 신청 상태 =====
  const [applied, setApplied] = useState([]);
  const isApplied = (id) => applied.some((c) => c.id === id);

  const handleApply = (course) => {
    if (isApplied(course.id)) return; // 중복 방지
    setApplied((prev) => [...prev, course]);
  };
  const handleCancel = (id) => {
    setApplied((prev) => prev.filter((c) => c.id !== id));
  };

  // ===== 수업계획서 모달 =====
  const [syllabus, setSyllabus] = useState(null);
  const openPlan = (course, e) => {
    e.stopPropagation(); // 행 클릭과 버블링 분리
    setSyllabus(course);
  };
  const closePlan = () => setSyllabus(null);

  // ===== 신청내역 드로어(열 때 헤더 숨김) =====
  const [showApplied, setShowApplied] = useState(false);
  const openApplied = () => {
    setShowApplied(true);
    document.documentElement.classList.add("drawer-open");
  };
  const closeApplied = () => {
    setShowApplied(false);
    document.documentElement.classList.remove("drawer-open");
  };
  useEffect(() => {
    return () => document.documentElement.classList.remove("drawer-open");
  }, []);

  // ===== 필터링 =====
  const filtered = useMemo(() => {
    return allCourses.filter((c) => {
      const okDept = dept === "전체" ? true : c.type === dept || c.major.includes(dept);
      const okKeyword =
        !qKeyword ||
        c.type.includes(qKeyword) ||
        c.major.includes(qKeyword) ||
        c.name.includes(qKeyword) ||
        c.id.includes(qKeyword);
      const okName = !qCourse || c.name.includes(qCourse) || c.id.includes(qCourse);
      return okDept && okKeyword && okName;
    });
  }, [allCourses, dept, qKeyword, qCourse]);

  return (
    <div id="course-apply">
      {/* 상단 안내 */}
      <section className="notice">
        <div className="title">안내문</div>
        <div className="box">
          자세한 신청 관련 공지는 <button className="link" onClick={() => window.open("/Community", "_self")}>학사 공지사항</button>을 확인해 주세요.
        </div>

        <button className="applied-check" onClick={openApplied}>
          신청과목 확인
        </button>
      </section>

      {/* 검색/필터 */}
      <section className="filters">
        <div className="row">
          <label>
            학년도
            <select value={year} onChange={(e) => setYear(e.target.value)}>
              <option>2025</option>
              <option>2024</option>
            </select>
          </label>
          <label>
            학기
            <select value={semester} onChange={(e) => setSemester(e.target.value)}>
              <option>1학기</option>
              <option>2학기</option>
            </select>
          </label>
          <label>
            개설전공
            <select value={dept} onChange={(e) => setDept(e.target.value)}>
              <option>전체</option>
              <option>전공</option>
              <option>교양</option>
              <option>국어국문학과</option>
              <option>철학과</option>
              <option>심리학과</option>
            </select>
          </label>
          <button className="btn">조회</button>
        </div>

        <div className="row">
          <label className="grow">
            통합검색
            <input
              type="text"
              placeholder="학점/과목명/코드 검색"
              value={qKeyword}
              onChange={(e) => setQKeyword(e.target.value)}
            />
          </label>
          <label className="grow">
            교과목명
            <input
              type="text"
              placeholder="교과목명 입력"
              value={qCourse}
              onChange={(e) => setQCourse(e.target.value)}
            />
          </label>
          <button className="btn">조회</button>
        </div>
      </section>

      {/* 목록 */}
      <section className="list">
        <div className="list-title">개설 교과목 목록</div>

        <div className="table">
          {/* 헤더 라인 */}
          <div className="thead">
            <div className="th col-type">종별</div>
            <div className="th col-major">개설전공</div>
            <div className="th col-name">교과목명</div>
            <div className="th col-code">교과코드</div>
            <div className="th col-credit">학점</div>
            <div className="th col-prof">담당교수</div>
            <div className="th col-time">강의시간</div>
            <div className="th col-plan">수업계획서</div>
            <div className="th col-seats">수강인원</div>
            <div className="th col-apply">신청</div>
          </div>

          {/* 바디 라인 */}
          <div className="tbody">
            {filtered.map((c) => (
              <div
                key={c.id}
                className={`tr ${isApplied(c.id) ? "applied" : ""}`}
                role="button"
                tabIndex={0}
                onClick={() => handleApply(c)}       // 행 클릭도 신청
                onKeyDown={(e) => (e.key === "Enter" ? handleApply(c) : null)}
              >
                <div className="td col-type">{c.type}</div>
                <div className="td col-major">{c.major}</div>

                {/* 교과목명: 줄바꿈/말줄임 없이 한 줄 유지 */}
                <div className="td col-name no-wrap">{c.name}</div>

                <div className="td col-code">{c.id}</div>
                <div className="td col-credit">{c.credit}</div>
                <div className="td col-prof">{c.prof}</div>
                <div className="td col-time">{c.time}</div>

                {/* 계획서 아이콘: 클릭해도 신청 안 됨 */}
                <div className="td col-plan" onClick={(e) => e.stopPropagation()}>
                  <button className="icon" title="수업계획서" onClick={(e) => openPlan(c, e)}>
                    📄
                  </button>
                </div>

                <div className="td col-seats">{c.seats}명</div>
                <div className="td col-apply" onClick={(e) => e.stopPropagation()}>
                  {isApplied(c.id) ? (
                    <button className="btn small ghost" disabled>
                      신청됨
                    </button>
                  ) : (
                    <button className="btn small" onClick={() => handleApply(c)}>
                      신청
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 수업계획서 모달 */}
      {syllabus && (
        <div className="modal" onClick={closePlan}>
          <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
            <div className="modal-hd">
              <b>{syllabus.name}</b>
              <button className="close" onClick={closePlan}>닫기</button>
            </div>
            <div className="modal-bd">
              <p><b>교과코드</b> {syllabus.id}</p>
              <p><b>담당</b> {syllabus.prof}</p>
              <p><b>시간</b> {syllabus.time}</p>
              <hr />
              <p>{syllabus.plan}</p>
            </div>
          </div>
        </div>
      )}

      {/* 신청과목 확인 드로어 — 열리면 헤더 숨김 */}
      {showApplied && (
        <>
          <div className="drawer-mask" onClick={closeApplied} />
          <aside className="drawer">
            <div className="drawer-hd">
              <b>신청한 과목</b>
              <button className="close" onClick={closeApplied}>취소</button>
            </div>
            <div className="drawer-bd">
              {applied.length === 0 && <div className="empty">아직 신청한 과목이 없습니다.</div>}
              {applied.map((c) => (
                <div key={c.id} className="applied-item">
                  <div className="ttl">{c.name}</div>
                  <div className="meta">
                    {c.id} · {c.prof} · {c.time}
                  </div>
                  <button className="btn small ghost" onClick={() => handleCancel(c.id)}>
                    취소
                  </button>
                </div>
              ))}
            </div>
          </aside>
        </>
      )}
    </div>
  );
}
