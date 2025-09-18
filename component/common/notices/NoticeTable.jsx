// 공지테이블
import "../../../css/Communities/Notice-ui.css";

export default function NoticeTable({ rows = [] }) {
    return(
        <table className="notice-table">
            <thead>
                <tr>
                    <th>번호</th>
                    <th>제목</th>
                    <th>작성자</th>
                    <th>조회수</th>
                    <th>작성일</th>
                </tr>
            </thead>
            <tbody> 
                {rows.map(r =>(
                    <tr key={r.id}>
                        <td>{r.id}</td>
                        <td>{r.title}</td>  
                        <td>{r.author}</td>
                        <td>{r.views}</td>
                        <td>{r.date}</td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}