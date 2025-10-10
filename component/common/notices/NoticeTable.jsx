// 공지테이블
import {useState, useEffect} from "react";
import "../../../css/Communities/Notice-ui.css";
import "../../../css/Communities/NoticeDetailModal.css";
import { UseAdmin } from "../../../hook/UseAdmin";
import NoticeDetail from "../Communities/NoticeDetail";

export default function NoticeTable({ rows, currentPage, setCurrentPage, setSelectedNotice }) {
    const [selectedIdx, setSelectedIdx] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [fetchedNotice, setFetchedNotice] = useState(null); // fetch된 진짜 notice
    const [noticeList, setNoticeList] = useState(rows);

useEffect(() => {
  setNoticeList(rows); // props가 바뀌면 내부 상태도 업데이트
}, [rows]);


useEffect(() => {
  if (fetchedNotice) {
    setNoticeList(prev =>
      prev.map(item =>
        item.boardIdx === fetchedNotice.boardIdx ? { ...item, boardView: fetchedNotice.boardView } : item
      )
    );
  }
}, [fetchedNotice]);



    const openModal = (boardIdx) => {
    const notice = noticeList.find(row => row.boardIdx === boardIdx);
    setSelectedIdx(notice.boardIdx);
    setSelectedNotice(notice);
    setIsModalOpen(true);
    };

    const closeModal = () => {
        setSelectedIdx(null);
        setIsModalOpen(false);
        setFetchedNotice(null); // ✅ 초기화
    };

    const formattedTime = (boardReg) => {
    return boardReg.replace('T', '\n').slice(0, 16);
    };
    
const { isAuthenticated: isAdminAuth } = UseAdmin() || { admin: null, isAuthenticated: false };

const handleEdit = () => {
  if (!fetchedNotice) {
    alert("공지 데이터를 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
    return;
  }

  setSelectedNotice(fetchedNotice); // ✅ 이제 진짜 데이터
  setIsModalOpen(false);
  setCurrentPage("Admin 공지 작성");
};


    const decodeBase64 = (str) => {
  try {
    const cleanStr = str.replace(/\s/g, '');
    const binary = atob(cleanStr);
    const decoded = decodeURIComponent(Array.prototype.map.call(binary, (ch) =>
      '%' + ('00' + ch.charCodeAt(0).toString(16)).slice(-2)
    ).join(''));
    return decoded;
  } catch (e) {
    console.error("Base64 디코딩 오류:", e);
    return "";
  }
};

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
                {noticeList.map(r => {
                    const isSelected = r.boardIdx === selectedIdx;
                    const boardView = isSelected && fetchedNotice
                    ? fetchedNotice.boardView
                    : r.boardView;

                    return (
                    <tr
                        key={r.boardIdx}
                        onClick={() => openModal(r.boardIdx)}
                        style={{ cursor: "pointer" }}
                    >
                        <td>{r.boardIdx}</td>
                        <td>{decodeBase64(r.boardTitle)}</td>  
                        <td>{r.boardWriter}</td>
                        <td>{boardView}</td> {/* ✅ 실시간 조회수 반영 */}
                        <td>{formattedTime(r.boardReg)}</td>
                    </tr>
                    );
                })}
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
                        onFetchComplete={(notice) => setFetchedNotice(notice)} // ✅ fetch 완료 시 상태 저장
                        />


                        {/* ✅ modal-content 내부로 이동 */}
                        {isAdminAuth && (
                            <button
                            className="noticeEditButton"
                            onClick={handleEdit}
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