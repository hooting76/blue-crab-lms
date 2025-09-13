//공지목록 + 페이징
//'작성하기' 버튼은 관리자만 노출
import { useEffect, useState, useMemo } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import NoticeTable from "./NoticeTable"; //작성중
import Pagination from "../notices/Pagination";
import{ UseUser } from "../../../hook/UseUser";
import notices from "../../../src/mock/notices"; //목업데이터 임포트
//import { getNotices } from "../../api/noticeAPI"; //API 함수 임포트,백엔드 붙일때 사용

export default function NoticeList({ category = "academy", page = 1, size = 10 }) {
    const navigate = useNavigate();
    const location = useLocation();
    const { user, isAuthenticated } = UseUser();

    //<권한 판별>       
    const isAdmin = user?.data?.user?.role === "admin"; //관리자 여부
    
    const[state, setState] = useState({items: [], total:0, loading: true});

    useEffect(() => {
        let alive = true; 
        setState((s) =>({...s, loading: true})); //로딩 시작
        
        //------------------(A) UI 전용 단계 : 목업데이터로 페이징-----------------
        const all = MOCK_NOTICES[category] ?? []; //해당 카테고리 전체 공지
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
        //setState({items: res.data.items, total: res.total, loading: false}); //로딩 완료
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

        //현재 카테고리의 라우트 베이스(페이징 링크 생성에 사용)
          const basepath =
          category === "academy" ? "/community/academy" :
          category === "admin" ? "/community/notice-admin" :
          "/community/etc";

        if (state.loading) return <div>로딩중...</div>;

        return(
            <>
            {/* 작성 버튼은 관리자만 노출 ->UI단계에서는 주석처리 */}
            {/* <div className="notice-actions"><button>작성하기</button></div> */}

            <NoticeTable rows={rows} />
            <Pagination total ={state.total} page={page} size={size} basepath={basepath}/>
            </>
    );
    }