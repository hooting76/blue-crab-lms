import React, { useState, useEffect } from 'react';
import '../../../css/MyPages/ClassAttending.css';
import classAttendingDummy from '../../../src/mock/classAttendingDummy.js'; //더미데이터
import { UseUser } from '../../../hook/UseUser';
import ApproveAttendanceModal from './ApproveAttendanceModal.jsx';
import TestModal from './TestModal.jsx';
import AssignmentModal from './AssignmentModal.jsx';
import ProfNoticeWritingPage from './ProfNoticeWritingPage.jsx';

function ClassAttending({currentPage, setCurrentPage}) {
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const { user } = UseUser(); // 유저 정보
    const accessToken = user.data.accessToken;
    const [lectureList, setLectureList] = useState([]);

    console.log("user : ", user);

    // const [openRow, setOpenRow] = useState(null);
    
    // const totalCredits = classAttendingDummy.reduce(
    //     (sum, cls) => sum + (Number(cls.LEC_POINT) || 0),
    //     0
    // ); // 해당 학기 총 이수 학점

    // const handleRowClick = (index) => {
    //     setOpenRow(openRow === index ? null : index);
    // };

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

const attendanceRequestSubmit = (e) => {
    e.preventDefault();
    alert("출석인정 신청이 완료되었습니다.");
}

const profNoticeWrite = () => {
    setCurrentPage("과목별 공지 작성");
}

if (currentPage === "과목별 공지 작성") {
    return (
        <ProfNoticeWritingPage currentPage={currentPage} setCurrentPage={setCurrentPage}/>
    );
}

const [isAttendanceModalOpen, setIsAttendanceModalOpen] = useState(false);
    const openAttendanceModal = () => setIsAttendanceModalOpen(true);
    const closeAttendanceModal = () => setIsAttendanceModalOpen(false);

const [isTestModalOpen, setIsTestModalOpen] = useState(false);
    const openTestModal = () => setIsTestModalOpen(true);
    const closeTestModal = () => setIsTestModalOpen(false);

const [isAssignmentModalOpen, setIsAssignmentModalOpen] = useState(false);
    const openAssignmentModal = () => setIsAssignmentModalOpen(true);
    const closeAssignmentModal = () => setIsAssignmentModalOpen(false);

const fetchClassAttendingList = async (accessToken) => {
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
        }};

    useEffect(() => {
        if (accessToken) {
            fetchClassAttendingList(accessToken);
        }
        }, [accessToken]); // ✅ accessToken이 생겼을 때 호출

    return (
        <div className="classAttending_list_container">
            <select value={selectedSemester} onChange={handleSemesterChange} className='selectSemester'>
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

            <select className="lectureName">
                {lectureList.map((cls) => (
                    <option key={cls.lecIdx} value={cls.lecIdx}>
                        {cls.lecTit}
                    </option>
                ))}
            </select>

            <div className="classAttendingContent">
                <div className="noticeAndChat">
                    <div className="lectureNotice">
                        과목별 공지사항
                    </div>

                    {user.data.user.userStudent === 1 && // 교수일 경우 공지 작성 버튼 추가
                        <>
                            <div className="profNoticeWriteBtnArea">
                                <button className="profNoticeWriteBtn" onClick={profNoticeWrite}>과목별 공지 작성</button>
                            </div>
                        </>
                    }

                    <div className="lectureChat">
                        실시간 채팅
                    </div>
                </div>

                <div className="attendanceStatus">
                    출결
                    {user.data.user.userStudent === 0 && // 학생일 경우 출결상황 표시
                        <>
                            <div className="attendance">
                                출석일수<br/>
                                전체 (강의일수)일 중<br/>
                                (출석일수)회
                            </div>
                            <div className="absence">
                                결석일수<br/>
                                전체 (강의일수)일 중<br/>
                                (결석일수)회
                            </div>
                        </>
                    }
                    <div className="attendanceCall">
                        {user.data.user.userStudent === 0 ? ( // 학생
                            <button className="attendanceCallBtn" onClick={attendanceRequestSubmit}>출석인정 신청</button>
                        ) : ( // 교수
                            <button className="attendanceCallBtn" onClick={openAttendanceModal}>출석인정 승인</button>
                        )}
                    </div>
                    {/* 모달 렌더링 */}
                    {isAttendanceModalOpen && <ApproveAttendanceModal onClose={closeAttendanceModal} />}
                </div>

                <div className="testAssignment">
                    시험 및 과제
                    {user.data.user.userStudent === 0 ? ( // 학생일 경우 개인 성적 표시
                        <>
                            <div className="studentTest">
                                중간고사 : 점<br/>
                                기말고사 : 점
                            </div>
                            <div className="studentAssignment">
                                과제1 : 점<br/>
                                과제2 : 점
                            </div>
                        </>
                    ) : ( // 교수
                        <>
                            <div className="profTest">
                                <button className="testModalBtn" onClick={openTestModal}>시험 관리</button>
                            </div>
                            <div className="profAssignment">
                                <button className="assignmentModalBtn" onClick={openAssignmentModal}>과제 관리</button>
                            </div>
                            {/* 모달 렌더링 */}
                            {isTestModalOpen && <TestModal onClose={closeTestModal}/>}
                            {isAssignmentModalOpen && <AssignmentModal onClose={closeAssignmentModal}/>}
                        </>
                    )}
                </div>
            </div>
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

export default ClassAttending;
