import React, { useState, useEffect } from 'react';
import { UseUser } from '../../../hook/UseUser';
import ProfNoticeWritingPage from './ProfNoticeWritingPage.jsx';

function ClassAttendingNotice({rows, currentPage, setCurrentPage}) {
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const {user} = UseUser();
    const accessToken = user.data.accessToken;
    const isProf = user.data.user.userStudent === 1;
    const [selectedIdx, setSelectedIdx] = useState(null);
    const [selectedNotice, setSelectedNotice] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [lectureList, setLectureList] = useState([]);
    const [noticeList, setNoticeList] = useState(null);
    const [fetchedNotice, setFetchedNotice] = useState(null); // fetch된 진짜 notice


const fetchLectureList = async (accessToken, user) => {
    try {

        const requestBody = {
            page: 0,
            size: 20,
            professor: String(user.data.user.id)
        };

        const response = await fetch(`${BASE_URL}/lectures`, {
            method: "POST",
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });
        console.log("Request body:", requestBody);

        if (!response.ok) throw new Error('강의 목록을 불러오는 데 실패했습니다.');

        const data = await response.json();
        setLectureList(data); // ✅ 받아온 데이터 저장
    } catch (error) {
        console.error('강의 목록 조회 에러:', error);
    }
};

const fetchEnrolledList = async (accessToken, user) => {
    try {

        const requestBody = {
            page: 0,
            size: 20,
            studentIdx: String(user.data.user.id),
            enrolled: true
        };

        const response = await fetch(`${BASE_URL}/enrollments/list`, {
            method: "POST",
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });
        console.log("Request body:", requestBody);

        if (!response.ok) throw new Error('강의 목록을 불러오는 데 실패했습니다.');

        const data = await response.json();
        setLectureList(data); // ✅ 받아온 데이터 저장
    } catch (error) {
        console.error('강의 목록 조회 에러:', error);
    }
};


    useEffect(() => {
        if (isProf) {
            fetchLectureList(accessToken, user);
            } else {
            fetchEnrolledList(accessToken, user);
            }
        }, [accessToken, user]); // ✅ accessToken이 생겼을 때 호출

        console.log("lectureList : ", lectureList);


const fetchAllNotices = async () => {
  try {
    const response = await fetch(`${BASE_URL}/boards/list`, {
      method: "POST",
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        page: 0,
        size: 1000, // 충분히 크게 설정
        boardCode: 3 // 공지사항 코드
      })
    });

    if (!response.ok) throw new Error("공지사항 목록 조회 실패");

    const data = await response.json();
    setNoticeList(data); // 모든 공지사항 저장
  } catch (error) {
    console.error("공지사항 조회 에러:", error);
    setNoticeList([]); // 실패 시 비움
  }
};

useEffect(() => {
  fetchAllNotices();
}, []);

const filteredNotices = noticeList?.filter(
  (notice) => notice.lecIdx === Number(selectedLecIdx)
);




    const profNoticeWrite = () => {
    setCurrentPage("과목별 공지 작성");
    }

    if (currentPage === "과목별 공지 작성") {
        return (
            <ProfNoticeWritingPage currentPage={currentPage} setCurrentPage={setCurrentPage}/>
        );
    }

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

const formattedTime = (boardReg) => {
    const date = new Date(boardReg);
    const now = new Date();

    // 오늘 날짜 비교 (년, 월, 일)
    const isToday =
        date.getFullYear() === now.getFullYear() &&
        date.getMonth() === now.getMonth() &&
        date.getDate() === now.getDate();

    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');

    if (isToday) {
        // 오늘이면 시간:분만 표시
        return `${hours}:${minutes}`;
    } else {
        // 오늘이 아니면 월/일 + 시간:분 표시 (MM/DD HH:mm)
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const day = date.getDate().toString().padStart(2, '0');
        return `${month}/${day} ${hours}:${minutes}`;
    }
    };

     const openModal = (boardIdx) => {
    setSelectedIdx(boardIdx);
    setIsModalOpen(true);
    };


    const closeModal = () => {
        setSelectedIdx(null);
        setIsModalOpen(false);
        setFetchedNotice(null); // ✅ 초기화
    };

    return(
        <>
            <select className="lectureName">
                {lectureList.length > 0 ? (
                    lectureList.map((cls) => (
                        <option key={cls.lecIdx} value={cls.lecIdx}>
                            {cls.lecTit}
                        </option>
                    ))
                ) : (
                    <option disabled>강의 목록 없음</option>
                )}
            </select>

            {isProf && // 교수일 경우 공지 작성 버튼 표시
                <>
                    <div className="profNoticeWriteBtnArea">
                        <button className="profNoticeWriteBtn" onClick={profNoticeWrite}>과목별 공지 작성</button>
                    </div>
                </>
            }

                    <table className="notice-table">
            <thead>
                <tr>
                    <th style={{width: "10%"}}>번호</th>
                    <th style={{width: "60%"}}>제목</th>
                    <th style={{width: "10%"}}>조회수</th>
                    <th style={{width: "20%"}}>작성일</th>
                </tr>
            </thead>
            <tbody>
                {filteredNotices?.length > 0 ? (
                    filteredNotices.map((r) => {
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
                        <td>{boardView}</td>
                        <td>{formattedTime(r.boardReg)}</td>
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

        {isModalOpen && (
                <div className="modal-overlay" onClick={closeModal}>
                    <div
                        className="modal-content"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <button className="modal-close" onClick={closeModal}>
                            ✖
                        </button>
                        <ProfNoticeDetail
                        boardIdx={selectedIdx}
                        currentPage={currentPage}
                        setCurrentPage={setCurrentPage}
                        onFetchComplete={(notice) => {
                            setFetchedNotice(notice);        // 업데이트된 공지
                            setSelectedNotice(notice);
                        }}
                        />

                            <button
                            className="noticeEditButton"
                            onClick={handleEdit}
                            >
                            공지 수정
                            </button>
                    </div>
                </div>
            )}
        </>
    )
}

export default ClassAttendingNotice;