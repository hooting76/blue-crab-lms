import React, { useState, useEffect } from 'react';
import { UseUser } from '../../../hook/UseUser';
import ProfNoticeWritingPage from './ProfNoticeWritingPage.jsx';

function ClassAttendingNotice({currentPage, setCurrentPage}) {
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const {user} = UseUser();
    const accessToken = user.data.accessToken;
    const isProf = user.data.user.userStudent === 1;
    const [lectureList, setLectureList] = useState([]);


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



    const profNoticeWrite = () => {
    setCurrentPage("과목별 공지 작성");
    }

    if (currentPage === "과목별 공지 작성") {
        return (
            <ProfNoticeWritingPage currentPage={currentPage} setCurrentPage={setCurrentPage}/>
        );
    }



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
        </>
    )
}

export default ClassAttendingNotice;