// 공지테이블
import {useState} from "react";
import "../../../css/Communities/Notice-ui.css";
import "../../../css/Communities/NoticeDetailModal.css";
import NoticeDetail from "../Communities/NoticeDetail";
import getAccessToken from "../../auth/getAccessToken";

export default function NoticeTable({ rows = [], currentPage, setCurrentPage }) {
    const [selectedIdx, setSelectedIdx] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isEditing, setIsEditing] = useState(false);

    const handleEditNotice = () => {
    setIsEditing(true); // 수정 모드로 전환
    };

    const openModal = (boardIdx) => {
        setSelectedIdx(boardIdx);
        setIsModalOpen(true);
    };

    const closeModal = () => {
        setSelectedIdx(null);
        setIsModalOpen(false);
    };

    const formattedTime = (boardReg) => {
    return boardReg.replace('T', '\n').slice(0, 16);
    };
    
        const accessToken = getAccessToken();

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
                    style={{ cursor: "pointer" }}
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
                        {isEditing ? (
                            <AdminNoticeWritingPage
                            boardIdx={selectedIdx}
                            accessToken={accessToken}
                            setIsEditing={setIsEditing} // 수정 완료 후 다시 상세로 돌아올 수 있게
                            setCurrentPage={setCurrentPage}
                            closeModal={closeModal}
                            />
                        ) : (
                            <NoticeDetail
                            boardIdx={selectedIdx}
                            onEditClick={handleEditNotice}
                            currentPage={currentPage}
                            setCurrentPage={setCurrentPage}
                            />
                        )}

                    </div>
                </div>
            )}
        </>
    );
}