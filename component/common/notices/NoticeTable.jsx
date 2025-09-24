// 공지테이블
import "../../../css/Communities/Notice-ui.css";

export default function NoticeTable({ rows = [] }) {
    return(
        <table className="notice-table">
            <thead>
                <tr>
                    <th style={{width: "10%"}}>번호</th>
                    <th style={{width: "40%"}}>제목</th>
                    <th style={{width: "20%"}}>작성자</th>
                    <th style={{width: "10%"}}>조회수</th>
                    <th style={{width: "20%"}}>작성일</th>
                </tr>
            </thead>
            <tbody> 
                {rows.map(r =>(
                    <tr key={r.BOARD_IDX}>
                        <td>{r.BOARD_IDX}</td>
                        <td>{r.BOARD_TIT}</td>  
                        <td>{r.BOARD_WRITER}</td>
                        <td>{r.BOARD_VIEWS}</td>
                        <td>{r.BOARD_REG}</td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}