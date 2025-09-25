// 공지테이블
import {useState} from "react";
import "../../../css/Communities/Notice-ui.css";
import NoticeDetail from "../Communities/NoticeDetail";

const openNoticeDetail = (boardIdx) => {
    <NoticeDetail boardIdx={boardIdx} />
}

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
                    <tr key={r.boardIdx} onClick={openNoticeDetail(r.boardIdx)} style={{cursor: "pointer"}}>
                        <td>{r.boardIdx}</td>
                        <td>{r.boardTitle}</td>  
                        <td>{r.boardWriter}</td>
                        <td>{r.boardView}</td>
                        <td>{r.boardReg}</td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}