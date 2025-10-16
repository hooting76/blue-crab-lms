import React, { useState, useEffect } from 'react';
import { UseUser } from '../../../hook/UseUser';
import ProfNoticeWritingPage from './ProfNoticeWritingPage.jsx';

function ClassAttendingNotice({currentPage, setCurrentPage}) {
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const {user} = UseUser();
    const accessToken = user.data.accessToken;
    const [lectureList, setLectureList] = useState([]);

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



const fetchClassAttendingList = async (accessToken) => { // 학생의 경우
    try {
        const response = await fetch(`${BASE_URL}/enrollments?studentIdx=${user.data.user.id}&page=0&size=10`, {
            method: "POST",
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json'
            }
        })
    if (!response.ok) throw new Error('강의 목록을 불러오는 데 실패했습니다.');
            const data = await response.json();
            setLectureList(data); // ✅ 받아온 데이터 저장
        } catch (error) {
            console.error('강의 목록 조회 에러:', error);
        }
    };

const fetchClassLecturingList = async (accessToken) => { // 교수의 경우
    try {
        const response = await fetch(`${BASE_URL}/professor/lectures`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json'
            }
        });
    if (!response.ok) throw new Error('강의 목록을 불러오는 데 실패했습니다.');
            const data = await response.json();
            setLectureList(data); // ✅ 받아온 데이터 저장
        } catch (error) {
            console.error('강의 목록 조회 에러:', error);
        }
    };

    useEffect(() => {
        if (accessToken && user.data.user.userStudent === 0) {
            fetchClassAttendingList(accessToken);
        }
        if (accessToken && user.data.user.userStudent === 1) {
            fetchClassLecturingList(accessToken);
        }
        }, [accessToken]); // ✅ accessToken이 생겼을 때 호출



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

            {user.data.user.userStudent === 1 && // 교수일 경우 공지 작성 버튼 표시
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