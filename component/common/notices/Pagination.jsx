// 페이징ui
// basepath: /community/academy, /community/admin, /community/etc
// page/size/total 기반으로 앵커 링크 생성

export default function Pagination({ total= 0, page= 1, size= 10, basepath= "" }) {
    const pages = Math.max(1, Math.ceil(total/size)); //총 페이지수
    const makeUrl = (p) => `${basepath}?page=${p}&size=${size}`; //페이지 링크 생성

    return(
        <nav className="pagination">
           <a href={makeUrl(Math.max(1, page-1))}>previous</a>
           {Array.from({length: pages}, (_, i) => i + 1).map(p =>(
            <a
              key={p}
              href={makeUrl(p)}
              className={p === page ? "active" : ""}
            >
            {p}
            </a>
        ))}
         <a href={makeUrl(Math.min(pages, page+1))}>next</a>
        </nav>
    );
}