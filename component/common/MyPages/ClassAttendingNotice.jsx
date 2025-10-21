import React, { useState, useEffect, useMemo } from 'react';
import { UseUser } from '../../../hook/UseUser';
import ProfNoticeWritingPage from './ProfNoticeWritingPage.jsx';
import ProfNoticeDetail from './ProfNoticeDetail.jsx'; // 누락된 import 추가

const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
const NOTICE_BOARD_CODE = 3;

function ClassAttendingNotice({ currentPage, setCurrentPage }) {
    const { user } = UseUser();
    const accessToken = user?.data?.accessToken;
    const userId = user?.data?.user?.id;
    const isProf = user?.data?.user?.userStudent === 1;

    const [selectedLectureSerial, setSelectedLectureSerial] = useState(null);
    const [lectureList, setLectureList] = useState([]);
    const [noticeList, setNoticeList] = useState([]);
    const [selectedIdx, setSelectedIdx] = useState(null);
    const [fetchedNotice, setFetchedNotice] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

    /** ========== Fetch ========== */
    const fetchLectureList = async () => {
        const endpoint = isProf ? '/lectures' : '/enrollments/list';
        const requestBody = isProf
            ? { page: 0, size: 20, professor: String(userId) }
            : { page: 0, size: 20, studentIdx: String(userId), enrolled: true };

        try {
            const response = await fetch(`${BASE_URL}${endpoint}`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) throw new Error('강의 목록 조회 실패');
            const data = await response.json();
            setLectureList(data);
            if (data.length > 0) setSelectedLectureSerial(data[0].lecSerial); // 첫 강의 선택
        } catch (error) {
            console.error('강의 목록 에러:', error);
            setLectureList([]);
        }
    };

    console.log("lectureList : ", lectureList);

    const fetchNotices = async () => {
        try {
            const response = await fetch(`${BASE_URL}/boards/list`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ page: 0, size: 20, boardCode: NOTICE_BOARD_CODE, lecSerial: selectedLectureSerial }),
            });

            if (!response.ok) throw new Error('공지사항 조회 실패');
            const data = await response.json();
            console.log("📦 notices response:", data);
            setNoticeList(data.content);
        } catch (error) {
            console.error('공지사항 에러:', error);
            setNoticeList([]);
        }
    };

    /** ========== useEffect ========== */
    useEffect(() => {
        if (accessToken && userId) {
            fetchLectureList();
        }
    }, [accessToken, userId]);

    useEffect(() => {
        if (accessToken, selectedLectureSerial) {
            fetchNotices();
        }
    }, [accessToken, selectedLectureSerial]);

    /** ========== Helpers ========== */
    const decodeBase64 = (str) => {
        try {
            const cleanStr = str.replace(/\s/g, '');
            const binary = atob(cleanStr);
            return decodeURIComponent(
                Array.prototype.map
                    .call(binary, (ch) => '%' + ('00' + ch.charCodeAt(0).toString(16)).slice(-2))
                    .join('')
            );
        } catch (e) {
            console.error("Base64 디코딩 오류:", e);
            return "";
        }
    };

    const formatTime = (timeStr) => {
        const date = new Date(timeStr);
        const now = new Date();

        const isToday =
            date.getFullYear() === now.getFullYear() &&
            date.getMonth() === now.getMonth() &&
            date.getDate() === now.getDate();

        const formatted = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;

        if (isToday) return formatted;

        return `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')} ${formatted}`;
    };


    /** ========== Event Handlers ========== */
    const handleLectureChange = (e) => {
        setSelectedLectureSerial(e.target.value);
    };

    const handleNoticeClick = (boardIdx) => {
        setSelectedIdx(boardIdx);
        setIsModalOpen(true);
    };

    const handleModalClose = () => {
        setSelectedIdx(null);
        setFetchedNotice(null);
        setIsModalOpen(false);
    };

    const handleEdit = () => {
        setCurrentPage("과목별 공지 작성");
    };

    /** ========== Page Change ========== */
    if (currentPage === "과목별 공지 작성") {
        return <ProfNoticeWritingPage currentPage={currentPage} setCurrentPage={setCurrentPage} />;
    }

    console.log("selectedLectureSerial : ", selectedLectureSerial);

    /** ========== Render ========== */
    return (
        <>
            {/* 강의 선택 드롭다운 */}
            <select className="lectureName" onChange={handleLectureChange} value={selectedLectureSerial}>
                {lectureList.length > 0 ? (
                    lectureList.map((lecture) => (
                        <option key={lecture.lecIdx} value={lecture.lecSerial}>
                            {lecture.lecTit}
                        </option>
                    ))
                ) : (
                    <option disabled>강의 목록 없음</option>
                )}
            </select>

            {isProf && (
                <div className="profNoticeWriteBtnArea">
                    <button className="profNoticeWriteBtn" onClick={handleEdit}>
                        과목별 공지 작성
                    </button>
                </div>
            )}

            {/* 공지 테이블 */}
            <table className="notice-table">
                <thead>
                    <tr>
                        <th style={{ width: "10%" }}>번호</th>
                        <th style={{ width: "60%" }}>제목</th>
                        <th style={{ width: "10%" }}>조회수</th>
                        <th style={{ width: "20%" }}>작성일</th>
                    </tr>
                </thead>
                <tbody>
                    {noticeList.length > 0 ? (
                        noticeList.map((notice) => {
                            const isSelected = notice.boardIdx === selectedIdx;
                            const boardView = isSelected && fetchedNotice
                                ? fetchedNotice.boardView
                                : notice.boardView;

                            return (
                                <tr key={notice.boardIdx} onClick={() => handleNoticeClick(notice.boardIdx)} style={{ cursor: "pointer" }}>
                                    <td>{notice.boardIdx}</td>
                                    <td>{decodeBase64(notice.boardTitle)}</td>
                                    <td>{boardView}</td>
                                    <td>{formatTime(notice.boardReg)}</td>
                                </tr>
                            );
                        })
                    ) : (
                        <tr>
                            <td colSpan="4">공지사항이 없습니다.</td>
                        </tr>
                    )}
                </tbody>
            </table>

            {/* 공지 모달 */}
            {isModalOpen && (
                <div className="modal-overlay" onClick={handleModalClose}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <button className="modal-close" onClick={handleModalClose}>✖</button>
                        <ProfNoticeDetail
                            boardIdx={selectedIdx}
                            currentPage={currentPage}
                            setCurrentPage={setCurrentPage}
                            onFetchComplete={(notice) => setFetchedNotice(notice)}
                        />
                        <button className="noticeEditButton" onClick={handleEdit}>
                            공지 수정
                        </button>
                    </div>
                </div>
            )}
        </>
    );
}

export default ClassAttendingNotice;
