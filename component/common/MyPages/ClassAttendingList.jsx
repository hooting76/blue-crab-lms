import React, { useState } from 'react';
import '../../../css/MyPages/ClassAttendingList.css';
import classAttendingDummy from '../../../src/mock/classAttendingDummy.js'; //더미데이터

function ClassAttendingList() {
    const [openRow, setOpenRow] = useState(null);
    const [selectedSemester, setSelectedSemester] = useState("0"); // 학기 선택 상태

    const totalCredits = classAttendingDummy.reduce(
        (sum, cls) => sum + (Number(cls.LEC_POINT) || 0),
        0
    ); // 해당 학기 총 이수 학점

    const handleRowClick = (index) => {
        setOpenRow(openRow === index ? null : index);
    };

    // select 변경 핸들러
    const handleSemesterChange = (e) => {
        setSelectedSemester(e.target.value);
        setOpenRow(null); // 학기 바뀌면 열려있는 행 닫기
    };

    return (
        <div className="classAttending_list_container">
            <select value={selectedSemester} onChange={handleSemesterChange} className='selectSemester'>
                <option value="0">2025년 2학기</option>
                <option value="1">2025년 1학기</option>
                <option value="2">2024년 2학기</option>
                <option value="3">2024년 1학기</option>
            </select> 
            {/* 학기에 고유값(selectedSemester)을 어떻게 부여할 것인지 의논이 필요할 듯 합니다. 일단 현재 학기를 0으로 지정. */}
            {/* 그리고 지금은 2025년 2학기, 1학기...를 하드코딩해놨지만
            현재 날짜를 계산해서 지금이 몇년도 몇학기인지 자동으로 기입되게 할 수 있을테니
            그렇게 한다면, 1, 2학기를 몇월 며칠을 기준으로 나눌지 논의 필요 */}

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
                                    <td colSpan={7}>
                                        <div className="class-details">
                                            <strong>{cls.LEC_NAME}</strong> 과목의 {selectedSemester === "0" ? "진행사항" : "점수"}<br/><br/>
                                            출석수 : <br/>
                                            중간고사 점수 : <br/>
                                            기말고사 점수 : <br/>
                                            과제 1 점수 : <br/>
                                            과제 2 점수 : <br/>
                                            {selectedSemester !== "0" && (<><br/>총합 점수 및 등급 :</>)}
                                            {/* 지나간 학기에 들었던 강의는 총점 및 등급 표시 */}
                                        </div>
                                    </td>
                                </tr>
                            )}
                        </React.Fragment>
                    ))}
                </tbody>
            </table>

            <div className='totalpoints'>
                이수학점: {totalCredits}점
                {selectedSemester !== "0" && "학기 평점: "}
                {/* 지나간 학기는 총 평점 표시 */}
            </div>
        </div>
    );
}

export default ClassAttendingList;
