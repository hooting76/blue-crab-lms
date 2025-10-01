// 공지테이블
import {useState} from "react";
import "../../../css/Communities/Notice-ui.css";
import "../../../css/Communities/NoticeDetailModal.css";
import { UseUser } from "../../../hook/UseUser";
import { UseAdmin } from "../../../hook/UseAdmin";
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

    // 사용자 컨텍스트
        const userContext = UseUser();
        const { user, isAuthenticated: isUserAuth } = userContext || { user: null, isAuthenticated: false };
    
        // 관리자 컨텍스트
        const adminContext = UseAdmin() || { admin: null, isAuthenticated: false };
        const { admin, isAuthenticated: isAdminAuth } = adminContext;
    
        // Admin 또는 User의 accessToken 가져오기
        const getAccessToken = () => {
            // 로컬스토리지에서 먼저 확인 (가장 최신 토큰)
            const storedToken = localStorage.getItem('accessToken');
            if (storedToken) return storedToken;
    
            // Admin 토큰 확인
            if (isAdminAuth && admin?.data?.accessToken) {
                return admin.data.accessToken;
            }
    
            // User 토큰 확인
            if (isUserAuth && user?.data?.accessToken) {
                return user.data.accessToken;
            }
    
            return null;
        };
    
        const accessToken = getAccessToken();

        const handleEdit = () => {
    setCurrentPage("Admin 공지 작성")
  }

  if (currentPage === "Admin 공지 작성") {
    return <AdminNoticeWritingPage boardIdx={notice.boardIdx} notice={notice} accessToken={accessToken} setCurrentPage={setCurrentPage} />;
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
                        onClick={(e) => e.stopPropagation()} // 내부 클릭 시 닫히지 않게
                    >
                        <button className="modal-close" onClick={closeModal}>
                            ✖
                        </button>
                        <NoticeDetail
                            boardIdx={selectedIdx}
                            currentPage={currentPage}
                            setCurrentPage={setCurrentPage}
                        />
                    </div>   
                    <button className="noticeEditButton" onClick={handleEdit}>공지 수정</button>
                </div>
            )}
        </>
    );
}