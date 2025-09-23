//표(번호/제목/작성자/조회수/작성일) + 하단 Pagination 출력
//'작성하기' 버튼은 관리자만 노출
import { useEffect, useState, useMemo } from "react";
import NoticeTable from "./NoticeTable"; //작성중(rows 받아서 표 렌더)
import Pagination from "../notices/Pagination";
import{ UseUser } from "../../../hook/UseUser";
import MOCK_NOTICES from "../../../src/mock/notices";  //목업데이터 임포트
//import { getNotices } from "../../api/noticeAPI"; //API 함수 임포트,백엔드 붙일때 사용
import "../../../css/Communities/Notice-ui.css";

export default function NoticeList({ 
    category = "academy", 
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
        setState((s) =>({...s, loading: true})); //로딩 시작
        
        //------------------(A) UI 전용 단계 : 목업데이터로 페이징-----------------
        const key = String(category).toLowerCase(); // 'academy'|'admin'|'etc'
        const allRaw = Array.isArray(MOCK_NOTICES) ? MOCK_NOTICES : [];
        const all = allRaw.filter((n) => n.category === key);
        
        //최신순 보장: 날짜 기준 내림차순
        all.sort((a,b) => (b.createdAt || "").localeCompare(a.createdAt ||""))

        const start = (page -1) * size; //현재 페이지 첫 공지 인덱스
        const end = start + size; //현재 페이지 마지막 공지 인덱스
        const pageItems = all.slice(start, end); //현재 페이지 공지 목록
        const totalItems = all.length; //전체 공지 개수

        const timer = setTimeout(() => {
            if(!alive) return;
            setState({items: pageItems, total: totalItems, loading: false}); //로딩 완료
        }, 150) //0.15초 후에 로딩 완료(로딩 애니메이션 효과를 위해 약간의 딜레이)

        //------------------(B) 백엔드 연동 시 (A)대신 이 블록사용------------------
        //getNotices(category, page, size).then(res => {
        //if(!alive) return;
        //setState({items: res.data.items, 
        // total: res.total, //백엔드 스키마에 맞게 조정
        // loading: false}); //로딩 완료
        //}).catch(()=> {
        //if(!alive) return;
        //setState({items: [], total: 0, loading: false}); //로딩 완료
        //});

        return () => {
            alive = false;
            clearTimeout(timer);
        };
        },[category, page, size]);

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
            alert("작성 페이지는 준비 중입니다.") //임시
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
              pageSize={size}          // ⚠️ Pagination prop명이 size면 pageSize→size로 바꿔야함
              total={state.total}
              onChange={handlePageChange}/>
    </>
  );
}
