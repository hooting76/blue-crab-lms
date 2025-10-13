//표(번호/제목/작성자/조회수/작성일) + 하단 Pagination 출력
//'작성하기' 버튼은 관리자만 노출
import { useEffect, useState, useMemo } from "react";
import NoticeTable from "./NoticeTable";
import Pagination from "../notices/Pagination";
import { UseUser } from "../../../hook/UseUser";
import { UseAdmin } from "../../../hook/UseAdmin";
import getNotices from "../../api/noticeAPI";
import "../../../css/Communities/Notice-ui.css";

export default function NoticeList({
    boardCode,
    page = 1, 
    size,
    onPageChange,
    onWrite,
    currentPage,
    setCurrentPage,
    setSelectedNotice
}) {


    // 사용자 컨텍스트
    const userContext = UseUser();
    const { user, isAuthenticated: isUserAuth } = userContext || { user: null, isAuthenticated: false };

    // 관리자 컨텍스트
    const adminContext = UseAdmin() || { admin: null, isAuthenticated: false };
    const { admin, isAuthenticated: isAdminAuth } = adminContext;

    // 권한 판별 - Admin으로 로그인했거나 User의 role이 ADMIN인 경우
    const isAdmin = isAdminAuth || (isUserAuth && user?.data?.role === "ADMIN");
    
    const[state, setState] = useState({items: [], total:0, loading: true});

    // Admin 또는 User의 accessToken 가져오기
    const getAccessToken = () => {
        // 로컬스토리지에서 먼저 확인 (가장 최신 토큰)
        const storedToken = localStorage.getItem('accessToken');
        if (storedToken) return storedToken;

        // Admin 토큰 확인
        if (isAdminAuth && admin?.data?.accessToken) {
            return admin.data.accessToken;
        }

        // User 토큰 확인
        if (isUserAuth && user?.data?.accessToken) {
            return user.data.accessToken;
        }

        return null;
    };

    const accessToken = getAccessToken();

    useEffect(() => {
      let alive = true;
      setState((s) => ({ ...s, loading: true }));

      const fetchNotices = async () => {
        try {
          if (!accessToken) {
            console.warn('접근 토큰이 없습니다. 로그인이 필요할 수 있습니다.');
            setState({ items: [], total: 0, loading: false });
            return;
          }

          const res = await getNotices(accessToken, page, size, boardCode);
          if (!alive) return;

          const items = Array.isArray(res.content) ? res.content : [];

          // ✅ 최신순 정렬 (작성일 기준)
          items.sort((a, b) => (b.boardReg || "").localeCompare(a.boardReg || ""));

          // ✅ 페이징 처리
          // const start = (page - 1) * size;
          // const end = start + size;
          // const pageItems = filtered.slice(start, end);

          setState({
            items,
            total: res.totalElements || 0,
            loading: false
          });
        } catch (error) {
          console.error('게시글 목록 조회 실패:', error);
          if (!alive) return;
          setState({ items: [], total: 0, loading: false });
        }
      };

      fetchNotices();


      return () => {
        alive = false;
      };
    }, [accessToken, page, size, boardCode]);


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
            setSelectedNotice(null); //새글 작성시 기존 선택 초기화
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
            <NoticeTable
              rows={rows}
              total={state.total}
              page={page}
              size={size}
              currentPage={currentPage}
              setCurrentPage={setCurrentPage}
              selectedNotice={null}
              setSelectedNotice={setSelectedNotice}
            />


            {/* 하단 페이지네이션: URL basepath 제거, 상태 콜백만 사용 */}
            <Pagination
              page={page}
              size={10}
              total={state.total}
              onChange={handlePageChange}/>
    </>
  );
}
