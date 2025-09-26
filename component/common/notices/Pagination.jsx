// 페이징ui
// basepath: /community/academy, /community/notice-admin, /community/etc
// page/size/total 기반으로 앵커 링크 생성
import "../../../css/Communities/Notice-ui.css";
import {useState} from "react";

export default function Pagination({ total, currentNoticePage, size, basepath= "" }) {
    const pages = Math.max(1, Math.ceil(total/size)); //총 페이지수
    const [currentNoticePage, setCurrentNoticePage] = useState(page);

    return(
        <nav className="pagination">
           <a onClick={setCurrentNoticePage(Math.max(1, page-1))}>previous</a>
           {Array.from({length: pages}, (_, i) => i + 1).map(p =>(
            <a
              key={p}
              onClick={setCurrentNoticePage(p)}
              className={p === currentNoticePage ? "active" : ""}
            >
            {p}
            </a>
        ))}
         <a onClick={setCurrentNoticePage(Math.min(pages, page+1))}>next</a>
        </nav>
    );
}