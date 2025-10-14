import React, { useState } from 'react';
import { UseUser } from '../../../hook/UseUser';
import classAttendingDummy from '../../../src/mock/classAttendingDummy.js'; //더미데이터
import ProfNoticeWritingPage from './ProfNoticeWritingPage.jsx';

function ClassAttendingNotice({currentPage, setCurrentPage}) {
    const {user} = UseUser();

        // select 변경 핸들러
        const handleSemesterChange = (e) => {
            setSelectedSemester(e.target.value);
        };
    
    const today = new Date();
    let currentYear = today.getFullYear();
    const currentMonth = today.getMonth() + 1;
    
    let currentSemester;
    
    if (currentMonth >= 3 && currentMonth <= 8) {
        currentSemester = 1;
    } else if (currentMonth >= 9) {
        currentSemester = 2;
    } else {
        // 1~2월은 전년도 2학기
        currentYear -= 1;
        currentSemester = 2;
    }
    
    
    // 현재 학기를 기준으로 지난 8개 학기 생성
    const generateSemesters = (count = 8) => {
        const semesters = [];
        let year = currentYear;
        let semester = currentSemester;
    
        for (let i = 0; i < count; i++) {
            const value = `${year}_${semester}`;
            const label = `${year}년 ${semester}학기`;
            semesters.push({ value, label });
    
            // 이전 학기로 이동
            if (semester === 1) {
                semester = 2;
                year -= 1;
            } else {
                semester = 1;
            }
        }
    
        return semesters;
    };
    
    const semesterOptions = generateSemesters(8);
    const currentSemesterValue = `${currentYear}_${currentSemester}`; // 현재 학기 value
    const [selectedSemester, setSelectedSemester] = useState(currentSemesterValue); // 학기 선택 상태

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
            <select value={selectedSemester} onChange={handleSemesterChange} className='selectSemester'>
                {semesterOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                        {option.label}
                    </option>
                ))}
            </select>

            <select className="lectureName">
                {classAttendingDummy.map((cls) => (
                    <option key={cls.LEC_IDX} value={cls.LEC_IDX}>
                        {cls.LEC_NAME}
                    </option>
                ))}
            </select>

            <div className="profNoticeWriteBtnArea">
                {user.data.user.userStudent === 1 &&
                <button className="profNoticeWriteBtn" onClick={profNoticeWrite}>과목별 공지 작성</button>}
            </div>
        </>
    )
}

export default ClassAttendingNotice;