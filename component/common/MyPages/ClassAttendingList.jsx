import React, { useState } from 'react';
import '../../../css/MyPages/ClassAttendingList.css';
import classAttendingDummy from '../../../src/mock/classAttendingDummy.js'; //더미데이터

function ClassAttendingList() {
    // 어떤 행이 열려 있는지 상태 관리
    const [openRow, setOpenRow] = useState(null);

    // 학점 총합 계산
    const totalCredits = classAttendingDummy.reduce(
        (sum, cls) => sum + (Number(cls.LEC_POINT) || 0),
        0
    );

    // 행 클릭 핸들러
    const handleRowClick = (index) => {
        setOpenRow(openRow === index ? null : index); // toggle
    };

    return (
        <div className="classAttending_list_container">
            <div className='classAttending_text'>
                수강과목 클릭 시 해당과목 진행사항 표시
            </div>
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
                                    <td colSpan="7">
                                        <div className="class-details">
                                            <strong>{cls.LEC_NAME}</strong> 과목의 진행사항
                                        </div>
                                    </td>
                                </tr>
                            )}
                        </React.Fragment>
                    ))}
                </tbody>
            </table>

            <div className='totalpoints'>
                총 학점: {totalCredits}점
            </div>
        </div>
    );
}

export default ClassAttendingList;
