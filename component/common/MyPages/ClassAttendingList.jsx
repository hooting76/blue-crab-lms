import React, { useState } from 'react';
import '../../../css/MyPages/ClassAttendingList.css';
import classAttendingDummy from '../../../src/mock/classAttendingDummy.js'; //더미데이터

function ClassAttendingList() {
    // const [openRow, setOpenRow] = useState(null);
    
    // const totalCredits = classAttendingDummy.reduce(
    //     (sum, cls) => sum + (Number(cls.LEC_POINT) || 0),
    //     0
    // ); // 해당 학기 총 이수 학점

    // const handleRowClick = (index) => {
    //     setOpenRow(openRow === index ? null : index);
    // };

    // // select 변경 핸들러
    // const handleSemesterChange = (e) => {
    //     setSelectedSemester(e.target.value);
    //     setOpenRow(null); // 학기 바뀌면 열려있는 행 닫기
    // };

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



    return (
        <div className="classAttending_list_container">
            <select value={selectedSemester} className='selectSemester'>
                {semesterOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                        {option.label}
                    </option>
                ))}
            </select>

            <select value={selectedLecture} className="lectureName">
                {classAttendingDummy.map((cls) => (
                    <option key={cls.LEC_IDX} value={cls.LEC_IDX}>
                        {cls.LEC_NAME}
                    </option>
                ))}
            </select>

            <span className="noticeChat">

            </span>

            <span className="attendance">

            </span>

            <span className="testAssignment">

            </span>

        {/* <div className='classAttending_text'>
            수강과목 클릭 시 해당과목 진행사항 표시
        </div>

        <div className="table-wrap">
            <table>
                <thead className='classAttending_list_header'>
                    <tr>
                        <th style={{ width: "5%" }}>순번</th>
                        <th style={{ width: "10%" }}>종별</th>
                        <th style={{ width: "25%" }}>교과목명</th>
                        <th style={{ width: "20%" }}>강의시간</th>
                        <th style={{ width: "15%" }}>수업계획</th>
                        <th style={{ width: "15%" }}>담당교수</th>
                        <th style={{ width: "10%" }}>학점</th>
                    </tr>
                </thead>
                <tbody className='classAttending_list_body'>
                    {classAttendingDummy.map((cls, index) => (
                        <React.Fragment key={cls.LEC_IDX}>
                            <tr
                                className="clickable-row"
                                onClick={() => handleRowClick(index)}
                                style={{ cursor: 'pointer' }}
                            >
                                <td>{index + 1}</td>
                                <td>{cls.LEC_BASIC === "1" ? "전공" : "교양"}</td>
                                <td>{cls.LEC_NAME}</td>
                                <td>{cls.LEC_TIME}</td>
                                <td></td>
                                <td>{cls.LEC_PROF}</td>
                                <td>{cls.LEC_POINT}</td>
                            </tr>
                            {openRow === index && (
                                <tr className="class_details_row">
                                    <td colSpan={7}>
                                        <div className="class-details">
                                            <strong>{cls.LEC_NAME}</strong> 과목의 {selectedSemester === currentSemesterValue ? "진행사항" : "점수"}<br/><br/>
                                            출석수 : <br/>
                                            중간고사 점수 : <br/>
                                            기말고사 점수 : <br/>
                                            과제 1 점수 : <br/>
                                            과제 2 점수 : <br/>
                                            {selectedSemester !== currentSemesterValue && (<><br/>총합 점수 및 등급 :</>)}
                                            {/* 지나간 학기에 들었던 강의는 총점 및 등급 표시 */}
                                        {/* </div>
                                    </td>
                                </tr>
                            )}
                        </React.Fragment>
                    ))}
                </tbody>
            </table>
            </div>


            <div className='totalpoints'>
                이수학점: {totalCredits}점
                {selectedSemester !== currentSemesterValue && "학기 평점: "} */}
                {/* 지나간 학기는 총 평점 표시 */}
            {/* </div> */}
        </div>
    );
}

export default ClassAttendingList;
