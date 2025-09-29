// 공지테이블
import {useState} from "react";
import "../../../css/Communities/Notice-ui.css";
import "../../../css/Communities/NoticeDetailModal.css";
import NoticeDetail from "../Communities/NoticeDetail";

export default function NoticeTable({ rows = [], boardOn = 1 }) {
    const [selectedIdx, setSelectedIdx] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [noticeVisibility, setNoticeVisibility] = useState("visible");

    const openModal = (boardIdx) => {
        setSelectedIdx(boardIdx);
        setIsModalOpen(true);
    };

    const closeModal = () => {
        setSelectedIdx(null);
        setIsModalOpen(false);
    };

    const formattedTime = (boardReg) => {
    return boardReg.replace('T', ' ').slice(0, 16);
    };

    useEffect(() => {
        setNoticeVisibility(boardOn === 1 ? "visible" : "hidden");
    }, [boardOn]);

    return(
        <>
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
                    <tr
                    key={r.boardIdx}
                    onClick={() => openModal(r.boardIdx)}
                    style={{ cursor: "pointer", visibility: noticeVisibility }}
                    >
                        <td>{r.boardIdx}</td>
                        <td>{r.boardTitle}</td>  
                        <td>{r.boardWriter}</td>
                        <td>{r.boardView}</td>
                        <td>{formattedTime(r.boardReg)}</td>
                    </tr>
                ))}
            </tbody>
        </table>

        {isModalOpen && (
                <div className="modal-overlay" onClick={closeModal}>
                    <div
                        className="modal-content"
                        onClick={(e) => e.stopPropagation()} // 내부 클릭 시 닫히지 않게
                    >
                        <button className="modal-close" onClick={closeModal}>
                            ✖
                        </button>
                        <NoticeDetail boardIdx={selectedIdx} />
                    </div>
                </div>
            )}
        </>
    );
}