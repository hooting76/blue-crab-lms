//표(번호/제목/작성자/조회수/작성일) + 하단 Pagination 출력
//'작성하기' 버튼은 관리자만 노출
import { useEffect, useState, useMemo } from "react";
import NoticeTable from "./NoticeTable"; //작성중(rows 받아서 표 렌더)
import Pagination from "../notices/Pagination";
import{ UseAdmin } from "../../../hook/UseAdmin"; //관리자 컨텍스트 훅
import getNotices from "../../api/noticeAPI"; //API 함수 임포트,백엔드 붙일때 사용
import "../../../css/Communities/Notice-ui.css";

export default function NoticeList({
    boardCode,
    page = 1, 
    size,
    onPageChange,
    onWrite,
}) {

    const { admin } = UseAdmin();

    //<권한 판별>       
    const isAdmin = admin.role === "ADMIN"; //관리자 여부
    
    const[state, setState] = useState({items: [], total:0, loading: true});

    useEffect(() => {
      let alive = true;
      setState((s) => ({ ...s, loading: true }));

      getNotices(page, size) // BOARD_CODE 제거: 전체를 가져오고, 프론트에서 필터링
        .then(res => {
          if (!alive) return;

          const allItems = res.content;

          // ✅ BOARD_CODE 필터링
          const filtered = allItems.filter((item) => item.boardCode === boardCode);

          // ✅ 최신순 정렬 (작성일 기준)
          filtered.sort((a, b) => (b.boardReg || "").localeCompare(a.boardReg || ""));

          console.log("res :", res);
          console.log("filtered:", filtered);

          // ✅ 페이징 처리
          const start = (page - 1) * size;
          const end = start + size;
          const pageItems = filtered.slice(start, end);

          setState({
            items: pageItems,
            total: filtered.length,
            loading: false
          });
        })
        .catch(() => {
          if (!alive) return;
          setState({ items: [], total: 0, loading: false });
        });

      return () => {
        alive = false;
      };
    }, [page, size, boardCode]);


        const rows = useMemo(() => state.items, [state.items]); //공지 목록

        if (state.loading) return <div className="notice=skeleton">로딩중...</div>;

        //URL 없이 상태로만 페이징 -> Pagination에 onChange 전달
        const handlePageChange = (next) => {
            if (typeof onPageChange === "function") onPageChange(next);
            //부모에서 page 상태를 들고 있지 않다면, 여기에 Local state를 추가
        };

        //작성하기버튼 핸들러
        const handleWrite =() => {
            if(!isAdmin) return;
            if(onWrite) return onWrite();     // 외부핸들러있으면 호출
        };

        return(
            <>
            {/* 작성 버튼은 관리자만 노출 ->UI단계에서는 주석처리 */}
            {isAdmin &&(
                <div className="notice-actions">
                    <button type="button" className="btn-primary" onClick={handleWrite}>
                        작성하기
                    </button>
                </div>
            )}

            {/* 표는 NoticeTable이 rows로 렌더(번호/제목/작성자/조회수/작성일) */}
            <NoticeTable rows={rows} total={state.total} page={page} size={size}/>

            {/* 하단 페이지네이션: URL basepath 제거, 상태 콜백만 사용 */}
            <Pagination
              page={page}
              size={10}
              total={state.total}
              onChange={handlePageChange}/>
    </>
  );
}
