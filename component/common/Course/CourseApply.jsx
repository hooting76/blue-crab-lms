import { useEffect, useMemo, useState } from "react";
import "../../../css/Course/CourseApply.css";

// === TODO: 실제 API 붙일 때 이 네 함수만 교체 ===
async function apiFetchCatalog(params) {
//params: { year, term, college, dept, q }
    return [
        { id: 1, no: 1, type: "전공", major: "국어국문학과", code: "KOR201", name: "현대시론", credit: 3, prof: "김보미", time: "월수 09:00-10:15", seats: "40명" },
        { id: 2, no: 2, type: "전공", major: "철학과", code: "PHI310", name: "현대철학의 법", credit: 2, prof: "한지훈", time: "목 15:00-16:45", seats: "55명" },
        { id: 3, no: 3, type: "교양", major: "심리학과", code: "PSY105", name: "심리학의 이해", credit: 2, prof: "최유리", time: "금 13:00-14:15", seats: "70명" },
    ];
}
async function apiFetchMyEnrolls() {
    return [
        { id: 9001, code: "HIS110", name: "세계사 개론", credit: 2, prof: "박교수", time: "화 10:30-11:45" },
    ];
}
async function apiEnroll(courseId) { return { ok: true }; }
async function apiDrop(courseId) { return { ok: true }; }
// ====================================================

export default function CourseApply() {
    // 필터들(초기값은 드롭다운 기본값처럼)
    const [year, setYear] = useState("2025");
    const [term, setTerm] = useState("2");
    const [college, setCollege] = useState("전체");
    const [dept, setDept] = useState("전체");
    const [q, setQ] = useState("");

    const [catalog, setCatalog] = useState([]);
    const [mine, setMine] = useState([]);
    const [loading, setLoading] = useState(false);
    const [busy, setBusy] = useState(false);
    const [msg, setMsg] = useState("");

    // 우측 슬라이드 패널 열림 여부
    const [openPanel, setOpenPanel] = useState(false);

    const loadCatalog = async () => {
        setLoading(true);
    const rows = await apiFetchCatalog({ year, term, college, dept, q });
        setCatalog(rows);
        setLoading(false);
    };
    const loadMine = async () => {
    const rows = await apiFetchMyEnrolls();
        setMine(rows);
    };

    useEffect(() => { loadCatalog(); loadMine(); }, []);
    // 통합검색 즉시 반영(선호에 따라 버튼으로만 검색하게 바꿔도 됨)
    useEffect(() => { loadCatalog(); /* eslint-disable-next-line */ }, [q]);

    const onSearchClick = () => loadCatalog();

    const onEnroll = async (row) => {
        if (busy) return;
            setBusy(true); setMsg("");
    const r = await apiEnroll(row.id);
        if (r.ok) { setMsg("수강신청이 완료되었습니다."); await loadMine(); }
        else setMsg("수강신청에 실패했습니다.");
            setBusy(false);
    };

    const onDrop = async (row) => {
        if (busy) return;
            setBusy(true); setMsg("");
    const r = await apiDrop(row.id);
        if (r.ok) { setMsg("수강취소가 완료되었습니다."); await loadMine(); }
        else setMsg("수강취소에 실패했습니다.");
            setBusy(false);
    };

return (
    <div className="course-apply">
        {/* 상단 제목 + 우측 패널 토글 */}
        <div className="title-bar">
            <h2>수강신청</h2>
                <button className="btn ghost" onClick={() => setOpenPanel(true)}>신청과목 확인</button>
        </div>

        {/* 안내 영역 (피그마의 공지 박스 자리) */}
        <div className="notice">
        <div className="notice-title">안내문</div>
        <div className="notice-body">
            자세한 사항은 우측 상단의 첨부파일 및 홈페이지 공지사항을 참고하세요.
        </div>
    </div>

    {/* 필터 바 */}
    <div className="filters">
        <div className="row">
        <div className="group">
            <label>학년도</label>
            <select value={year} onChange={(e)=>setYear(e.target.value)}>
                <option value="2025">2025</option>
                <option value="2024">2024</option>
            </select>
        </div>
        <div className="group">
            <label>학기</label>
            <select value={term} onChange={(e)=>setTerm(e.target.value)}>
                <option value="1">1학기</option>
                <option value="2">2학기</option>
            </select>
        </div>
        <div className="group">
            <label>개설학과</label>
            <select value={dept} onChange={(e)=>setDept(e.target.value)}>
                <option>전체</option>
                <option>국어국문학과</option>
                <option>철학과</option>
                <option>심리학과</option>
            </select>
        </div>
            <button className="btn" onClick={onSearchClick}>조회</button>
        </div>

        <div className="row">
            <div className="group">
            <label>통합검색</label>
            <input
                placeholder="학점/과목명/코드 검색"
                value={q}
                onChange={(e)=>setQ(e.target.value)}
            />
        </div>
            <div className="group">
            <label>교과목명</label>
            <input
                placeholder="교과목명 입력"
                value={q}
                onChange={(e)=>setQ(e.target.value)}
            />
        </div>
            <button className="btn" onClick={onSearchClick}>조회</button>
        </div>
    </div>

        {/* 결과 테이블 */}
        <div className="table-card">
        <div className="table-title">개설 교과목 목록</div>
            {msg && <div className="msg">{msg}</div>}

        <div className="table-wrap">
            <table>
                <thead>
                <tr>
                <th>순번</th>
                <th>종별</th>
                <th>개설전공</th>
                <th>교과목명</th>
                <th>학점</th>
                <th>담당교수</th>
                <th>강의시간</th>
                <th>수업계획서</th>
                <th>수강인원</th>
                <th>신청</th>
                </tr>
            </thead>
            <tbody>
                {loading && (
                <tr><td colSpan={10} style={{textAlign:'center'}}>불러오는 중…</td></tr>
                )}
            {!loading && catalog.map(row => (
                <tr key={row.id}>
                <td>{row.no}</td>
                <td>{row.type}</td>
                <td>{row.major}</td>
                <td><div className="sub">{row.code}</div>{row.name}</td>
                <td>{row.credit}</td>
                <td>{row.prof}</td>
                <td>{row.time}</td>
                <td>
                
                {/* 유니코드 이모지(📄)라서 리액트에서도 그대로 렌더 */}
                <button className="icon" title="수업계획서">📄</button></td>
                <td>{row.seats}</td>
                <td>
                    <button className="btn tiny" disabled={busy} onClick={()=>onEnroll(row)}>신청</button>
                </td>
                </tr>
                ))}
            {!loading && catalog.length === 0 && (
            <tr><td colSpan={10} style={{textAlign:'center',color:'#64748b'}}>검색 결과가 없습니다.</td></tr>
            )}
            </tbody>
        </table>
        </div>
    </div>

        {/* === 우측 슬라이드 패널: 신청과목 확인 === */}
    <aside className={`side-panel ${openPanel ? 'open' : ''}`} aria-hidden={!openPanel}>
        <div className="panel-head">
            <div className="panel-title">신청한 과목</div>
            <button className="icon" onClick={()=>setOpenPanel(false)} aria-label="닫기">✕</button>
        </div>
        <div className="panel-body">
            {mine.length === 0 && <div className="empty">신청한 과목이 없습니다.</div>}
            {mine.map((c)=>(
            <div key={c.id} className="mine-row">
                <div className="meta">
                    <div className="name">{c.name}</div>
                    <div className="sub">{c.code} · {c.prof} · {c.credit}학점 · {c.time}</div>
            </div>
                <button className="btn danger tiny" disabled={busy} onClick={()=>onDrop(c)}>취소</button>
            </div>
        ))}
        </div>
    </aside>

        {/* 패널 오버레이 */}
    {openPanel && <div className="overlay" onClick={()=>setOpenPanel(false)} />}
    </div>
    );
}
