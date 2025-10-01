// 공지테이블
import {useState} from "react";
import "../../../css/Communities/Notice-ui.css";
import "../../../css/Communities/NoticeDetailModal.css";
import NoticeDetail from "../Communities/NoticeDetail";
import AdminNoticeWritingPage from '../Communities/AdminNoticeWritingPage';

export default function NoticeTable({ rows = [], currentPage, setCurrentPage }) {
    const [selectedIdx, setSelectedIdx] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

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

        const handleEdit = () => {
        setIsModalOpen(false); // 모달 닫기
        setCurrentPage("Admin 공지 작성");
        };


  if (currentPage === "Admin 공지 작성") {
    return <AdminNoticeWritingPage />;
  }

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
                        onClick={(e) => e.stopPropagation()}
                    >
                        <button className="modal-close" onClick={closeModal}>
                            ✖
                        </button>
                        <NoticeDetail
                            boardIdx={selectedIdx}
                            currentPage={currentPage}
                            setCurrentPage={setCurrentPage}
                        />

                        {/* ✅ modal-content 내부로 이동 */}
                        {isAdminAuth && (
                            <button
                                className="noticeEditButton"
                                onClick={handleEdit}
                                style={{ marginTop: '20px' }}
                            >
                                공지 수정
                            </button>
                        )}
                    </div>
                </div>
            )}
        </>
    );
}