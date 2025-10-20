import React, { useState, useEffect } from 'react';
import '../../../css/MyPages/ClassAttending.css';
import { UseUser } from '../../../hook/UseUser';
import ApproveAttendanceModal from './ApproveAttendanceModal.jsx';
import TestModal from './TestModal.jsx';
import AssignmentModal from './AssignmentModal.jsx';
import ProfNoticeWritingPage from './ProfNoticeWritingPage.jsx';
import CourseDetail from './CourseDetail';

function ClassAttending({currentPage, setCurrentPage}) {
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const { user } = UseUser(); // 유저 정보
    const accessToken = user.data.accessToken;
    const isProf = user.data.user.userStudent === 1;
    const [lectureList, setLectureList] = useState([]);
    const [selectedSemester, setSelectedSemester] = useState(1); // 학기 선택 상태
    const [selectedYear, setSelectedYear] = useState(1);
    const [ifMajor, setIfMajor] = useState(1);

    console.log("user : ", user);


    // select 변경 핸들러
    const handleSemesterChange = (e) => {
        setSelectedSemester(e.target.value);
    };

    // year 변경 핸들러
    const handleYearChange = (e) => {
        setSelectedYear(e.target.value);
    };

    // 전공 여부 변경 핸들러
    const handleIfMajor = (e) => {
        setIfMajor(e.target.value);
    };


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

const [isClassDetailModalOpen, setIsClassDetailModalOpen] = useState(false);
    const openClassDetailModal = () => setIsClassDetailModalOpen(true);
    const closeClassDetailModal = () => setIsClassDetailModalOpen(false);

    
const fetchLectureList = async (accessToken, user) => {
    try {

        const requestBody = {
            page: 0,
            size: 20,
            professor: user.data.user.id
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
            studentIdx: user.data.user.id,
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
            fetchLectureList(accessToken, selectedSemester);
            } else {
            fetchEnrolledList(accessToken, selectedSemester);
            }
        }, [accessToken, selectedSemester]); // ✅ accessToken이 생겼을 때, 학기가 선택되었을 때 호출

        console.log("lectureList : ", lectureList);

        
    return (
        <div className="classAttending_list_container">
            {isProf &&
                <>
                    <select value={selectedSemester} onChange={handleSemesterChange}>
                        <option value={1}>1학기</option>
                        <option value={2}>2학기</option>
                    </select>

                    <select value={selectedYear} onChange={handleYearChange}>
                        <option value={1}>1학년</option>
                        <option value={2}>2학년</option>
                        <option value={3}>3학년</option>
                        <option value={4}>4학년</option>
                    </select>

                    <select value={ifMajor} onChange={handleIfMajor}>
                        <option value={1}>전공</option>
                        <option value={0}>교양</option>
                    </select>
                </>
            }

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

            <div className="classAttendingContent">
                <div className="noticeAndChat">
                    <div className="lectureNotice">
                        과목별 공지사항
                    </div>

                    {isProf ? // 교수일 경우 공지 작성 버튼 추가
                        (<>
                            <div className="profNoticeWriteBtnArea">
                                <button className="profNoticeWriteBtn" onClick={profNoticeWrite}>과목별 공지 작성</button>
                            </div>
                        </>)
                        : // 학생일 경우 강의 상세정보 페이지 이동 버튼 추가
                        (<>
                            <div className='studentClassDetailBtnArea'>
                                <button className='studentClassDetailBtn' onClick={openClassDetailModal}>강의 상세 정보</button>
                            </div>
                        </>)
                    }

                    {isClassDetailModalOpen && (
                        <div className="modal-overlay" onClick={closeClassDetailModal}>
                            <div
                                className="modal-content"
                                onClick={(e) => e.stopPropagation()}
                            >
                                <button className="modal-close" onClick={closeClassDetailModal}>
                                    ✖
                                </button>
                                <CourseDetail
                                lecture={selectedLecture}
                                currentPage={currentPage}
                                setCurrentPage={setCurrentPage}
                                onFetchComplete={(lecture) => {
                                    setFetchedLecture(lecture);        // 업데이트된 강의 상세 정보
                                    setSelectedLecture(lecture);
                                }}
                                />
                            </div>
                        </div>
                    )}

                    <div className="lectureChat">
                        실시간 채팅
                    </div>
                </div>

                <div className="attendanceStatus">
                    출결
                    {!isProf && // 학생일 경우 출결상황 표시
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
                        {!isProf ? ( // 학생
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
                    {!isProf ? ( // 학생일 경우 개인 성적 표시
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

        </div>
    );
}

export default ClassAttending;
