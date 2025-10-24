import React, { useState, useEffect } from 'react';
import { UseUser } from '../../../hook/UseUser';
import Pagination from '../notices/Pagination.jsx';
import ProfNoticeWritingPage from './ProfNoticeWritingPage.jsx';
import ProfNoticeDetail from './ProfNoticeDetail.jsx';

const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
const NOTICE_BOARD_CODE = 3;

function ClassAttendingNotice({ currentPage, setCurrentPage, selectedLecSerial, noticeToEdit, setNoticeToEdit }) {
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
    const [page, setPage] = useState(1);
    const [totalNotices, setTotalNotices] = useState(0); // 전체 공지 수


    /** ========== Fetch ========== */
    const fetchLectureData = async (accessToken, userId, isProf) => {
        try {
            const requestBody = isProf
            ? {
                page: 0,
                size: 100,
                professor: String(user.data.user.id)
                }
            : {
                page: 0,
                size: 100,
                studentIdx: Number(userId)
                };

            const url = isProf
            ? `${BASE_URL}/lectures`
            : `${BASE_URL}/enrollments/list`;

            const response = await fetch(url, {
            method: 'POST',
            headers: {
                Authorization: `Bearer ${accessToken}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestBody),
            });

            if (!response.ok) {
            throw new Error('강의 목록을 불러오는 데 실패했습니다.');
            }

            const data = await response.json();
            setLectureList(data);
        } catch (error) {
            console.error('강의 목록 조회 에러:', error);
        }
        };

    const fetchNotices = async () => {
        if (!accessToken || !selectedLectureSerial) return;
        try {
            const response = await fetch(`${BASE_URL}/boards/list`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    page: page - 1,
                    size: 10,
                    boardCode: NOTICE_BOARD_CODE,
                    lecSerial: selectedLectureSerial
                }),
            });

            if (!response.ok) throw new Error('공지사항 조회 실패');
            const data = await response.json();
            setNoticeList(data.content);
            setTotalNotices(data.totalElements);
        } catch (error) {
            console.error('공지사항 에러:', error);
            setNoticeList([]);
        }
    };

    /** ========== useEffect ========== */
    // prop이 바뀔 때 내부 상태 동기화
useEffect(() => {
    if (selectedLecSerial) {
        setSelectedLectureSerial(selectedLecSerial);
        setPage(1);
    }
}, [selectedLecSerial]);

// 처음 mount나 userId/accessToken 바뀔 때 강의 목록 fetch
useEffect(() => {
    if (accessToken && userId) {
        fetchLectureData(accessToken, userId, isProf);
    }
}, [accessToken, userId]);

// 강의 목록 받아올 때 첫 강의로 기본 선택 설정
    useEffect(() => {
      if (isProf) {
        // 교수일 때: lectureList 자체 사용
        if (lectureList.length > 0) {
          setSelectedLectureSerial(lectureList[0].lecSerial);
        }
      } else {
        // 교수 아닐 때: lectureList.content 사용
        if (lectureList?.content?.length > 0) {
          setSelectedLectureSerial(lectureList.content[0].lecSerial);
        }
      }
    }, [lectureList, isProf]);

// selectedLectureSerial이 바뀔 때마다 공지 fetch
useEffect(() => {
    if (accessToken && selectedLectureSerial) {
        fetchNotices();
    }
}, [accessToken, selectedLectureSerial, page]);


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
        setPage(1); // 강의 바뀌면 페이지 초기화
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
        if (fetchedNotice) {
            setNoticeToEdit(fetchedNotice);
            setCurrentPage("과목별 공지 작성");
        }
    };

    const handlePageChange = (newPage) => {
        setPage(newPage);
    };

    /** ========== Page Change ========== */
    if (currentPage === "과목별 공지 작성") {
        return (
            <ProfNoticeWritingPage
                notice={noticeToEdit}
                currentPage={currentPage}
                setCurrentPage={setCurrentPage}
            />
        );
    }

    /** ========== Render ========== */
    return (
        <>
            {isProf ? (
                <select
                className="lectureName"
                value={selectedLectureSerial || ''}
                onChange={handleLectureChange}
                >
                {lectureList.length > 0 ? (
                    lectureList.map((lec) => (
                    <option key={lec.lecSerial} value={lec.lecSerial}>
                        {lec.lecTit}
                    </option>
                    ))
                ) : (
                    <option disabled>강의 목록 없음</option>
                )}
                </select>
            ) : (
                <select
                className="lectureName"
                value={selectedLectureSerial || ''}
                onChange={handleLectureChange}
                >
                {lectureList?.content?.length > 0 ? (
                    lectureList.content.map((lec) => (
                    <option key={lec.lecSerial} value={lec.lecSerial}>
                        {lec.lecTit}
                    </option>
                    ))
                ) : (
                    <option disabled>강의 목록 없음</option>
                )}
                </select>
            )}

            {isProf && (
                <div className="profNoticeWriteBtnArea">
                    <button className="profNoticeWriteBtn" onClick={() => {
                        setNoticeToEdit(null);
                        setCurrentPage("과목별 공지 작성");
                    }}>
                        과목별 공지 작성
                    </button>
                </div>
            )}

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

            <Pagination
                page={page}
                size={10}
                total={totalNotices}
                onChange={handlePageChange}
            />

            {isModalOpen && (
                <div className="modal-overlay" onClick={handleModalClose}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <button className="modal-close" onClick={handleModalClose}>✖</button>
                        <ProfNoticeDetail
                            boardIdx={selectedIdx}
                            currentPage={currentPage}
                            setCurrentPage={setCurrentPage}
                            onFetchComplete={(notice) => {
                                setFetchedNotice(notice);
                                setNoticeToEdit(notice);
                            }}
                            // ✅ 삭제 성공 시 목록 갱신 + 모달 닫기
                            onDeleteSuccess={() => {
                            fetchNotices();
                            setIsModalOpen(false);
                            }}
                        />
                        {fetchedNotice && fetchedNotice.boardWriter === user?.data?.user?.name &&
                            <button className="noticeEditButton" onClick={handleEdit}>
                                공지 수정
                            </button>
                        }
                    </div>
                </div>
            )}
        </>
    );
}

export default ClassAttendingNotice;
