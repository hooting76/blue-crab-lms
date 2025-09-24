//표(번호/제목/작성자/조회수/작성일) + 하단 Pagination 출력
//'작성하기' 버튼은 관리자만 노출
import { useEffect, useState, useMemo } from "react";
import NoticeTable from "./NoticeTable";
import Pagination from "../notices/Pagination";
import { UseUser } from "../../../hook/UseUser";
import { getNotices, getNoticesByCode } from "../../api/noticeAPI";
import "../../../css/Communities/Notice-ui.css";

export default function NoticeList({ 
    boardCode = "0", 
    page = 1, 
    size = 10,
    onPageChange,
    onWrite,
}) {

    const { user, isAuthenticated } = UseUser()

    //<권한 판별>       
    const isAdmin = user?.data?.user?.role === "admin"; //관리자 여부
    
    const[state, setState] = useState({items: [], total:0, loading: true});

    useEffect(() => {
        let alive = true;
        setState((s) => ({ ...s, loading: true }));

        // accessToken 가져오기
        const accessToken = user?.data?.token;
        
        // 페이지 인덱스 조정 (백엔드는 0-based, 프론트엔드는 1-based)
        const pageIndex = page - 1;
        
        if (boardCode === "0") {
            // 전체 게시글 조회
            getNotices(accessToken, pageIndex, size)
                .then(res => {
                    if (!alive) return;
                    
                    setState({
                        items: res.content || [], // Spring Data JPA Page 응답 구조
                        total: res.totalElements || 0,
                        loading: false
                    });
                })
                .catch(error => {
                    console.error('게시글 로딩 실패:', error);
                    setState(s => ({ ...s, loading: false }));
                });
        } else {
            // 특정 코드의 게시글 조회
            getNoticesByCode(accessToken, parseInt(boardCode), pageIndex, size)
                .then(res => {
                    if (!alive) return;
                    
                    setState({
                        items: res.content || [],
                        total: res.totalElements || 0,
                        loading: false
                    });
                })
                .catch(error => {
                    console.error('게시글 로딩 실패:', error);
                    setState(s => ({ ...s, loading: false }));
                });
        }

        return () => { alive = false };
    }, [boardCode, page, size, user?.data?.token]);


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
              size={size}
              total={state.total}
              onChange={handlePageChange}/>
    </>
  );
}
