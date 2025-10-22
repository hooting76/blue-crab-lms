import React, { useState, useEffect } from 'react';
import '../../../css/MyPages/ClassAttending.css';
import { UseUser } from '../../../hook/UseUser';
import ApproveAttendanceModal from './ApproveAttendanceModal.jsx';
import TestModal from './TestModal.jsx';
import AssignmentCreateModal from './AssignmentCreateModal.jsx';
import ProfNoticeWritingPage from './ProfNoticeWritingPage.jsx';
import CourseDetail from './CourseDetail';

function ClassAttending({ currentPage, setCurrentPage }) {
  const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
  const { user } = UseUser();
  const accessToken = user.data.accessToken;
  const isProf = user.data.user.userStudent === 1;
  const [lectureList, setLectureList] = useState([]);
  const [assignmentList, setAssignmentList] = useState([]);

  // 1. 선택한 강의 ID 상태 추가
  const [selectedLecSerial, setSelectedLecSerial] = useState("");

  // 모달 상태들
  const [isEvaluationModalOpen, setIsEvaluationModalOpen] = useState(false);
  const [isAttendanceModalOpen, setIsAttendanceModalOpen] = useState(false);
  const [isTestModalOpen, setIsTestModalOpen] = useState(false);
  const [isAssignmentCreateModalOpen, setIsAssignmentCreateModalOpen] = useState(false);
  const [isClassDetailModalOpen, setIsClassDetailModalOpen] = useState(false);


  // 강의 목록 받아올 때 첫 강의로 기본 선택 설정
  useEffect(() => {
    if (lectureList.length > 0) {
      setSelectedLecSerial(lectureList[0].lecSerial);
    }
  }, [lectureList]);

  // 강의 선택 변경 핸들러
  const handleLectureChange = (e) => {
  setSelectedLecSerial(e.target.value); // e.target.value = lecSerial
};


  // 강의 목록 가져오기 (교수/학생 구분)
const fetchLectureData = async (accessToken, user, isProf) => {
  try {
    const requestBody = isProf
      ? {
          page: 0,
          size: 100,
          professor: String(user.data.user.id),
        }
      : {
          page: 0,
          size: 100,
          studentIdx: String(user.data.user.id),
          enrolled: true,
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


  // 과제 목록 불러오기
  const getAssignments = async(accessToken, selectedLecSerial) => {
    const requestBody = {lecSerial: selectedLecSerial, page: 0, size: 100, action: "list"}
    try {
        const response = await fetch(`${BASE_URL}/assignments/list`, {
            method: "POST",
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestBody)
            });
             if (!response.ok) throw new Error('과제 목록 조회 실패');
            const data = await response.json();
            setAssignmentList(data);
        } catch (error) {
            console.error('과제 목록 에러:', error);
            setAssignmentList([]);
        }
    };

  useEffect(() => {
    fetchLectureData(accessToken, user, isProf);
  }, [accessToken, user, isProf]);


  useEffect(() => {
    getAssignments(accessToken, selectedLecSerial);
  }, [accessToken, selectedLecSerial]);


  // 출석인정 신청
  const attendanceRequestSubmit = (e) => {
    e.preventDefault();
    alert('출석인정 신청이 완료되었습니다.');
  };

  // 과목별 공지 작성 페이지 이동
  const profNoticeWrite = () => {
    setCurrentPage('과목별 공지 작성');
  };

  // 모달 오픈/클로즈 핸들러
  const openEvaluationModal = () => setIsEvaluationModalOpen(true);
  const closeEvaluationModal = () => setIsEvaluationModalOpen(false);
  const openAttendanceModal = () => setIsAttendanceModalOpen(true);
  const closeAttendanceModal = () => setIsAttendanceModalOpen(false);
  const openTestModal = () => setIsTestModalOpen(true);
  const closeTestModal = () => setIsTestModalOpen(false);
  const openAssignmentCreateModal = () => setIsAssignmentCreateModalOpen(true);
  const closeAssignmentCreateModal = () => setIsAssignmentCreateModalOpen(false);
  const openClassDetailModal = () => setIsClassDetailModalOpen(true);
  const closeClassDetailModal = () => setIsClassDetailModalOpen(false);

  // 공지 작성 페이지 렌더링
  if (currentPage === '과목별 공지 작성') {
    return <ProfNoticeWritingPage currentPage={currentPage} setCurrentPage={setCurrentPage} />;
  }


  return (
    <div className="classAttending_list_container">
      {/* 강의 선택 박스 */}
      <select className="lectureName" value={selectedLecSerial || ''} onChange={handleLectureChange}>
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


      <div className="classAttendingContent">
        <div className="noticeAndChat">
          <div className="lectureNotice">과목별 공지사항</div>

          {isProf ? ( // 교수
            <div className="profNoticeWriteBtnArea">
              <button className="profNoticeWriteBtn" onClick={profNoticeWrite}>
                과목별 공지 작성
              </button>
            </div>
          ) : ( // 학생
            <>
              <div className="studentClassDetailBtnArea">
                <button className="studentClassDetailBtn" onClick={openClassDetailModal}>
                  강의 상세 정보
                </button>
              </div>
              <div className="studentEvaluationBtnArea">
                <button className="studentEvaluationBtn" onClick={openEvaluationModal}>
                  강의 평가
                </button>
              </div>
            </>
          )}

          {/* 강의 상세 모달 */}
          {isClassDetailModalOpen && (
            <div className="modal-overlay" onClick={closeClassDetailModal}>
              <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={closeClassDetailModal}>
                  ✖
                </button>
                <CourseDetail
                  lectureDetails={lectureList.find((lec) => lec.lecSerial === selectedLecSerial)}
                  currentPage={currentPage}
                  setCurrentPage={setCurrentPage}
                />
              </div>
            </div>
          )}


          {/* 강의 평가 모달 */}
          {isEvaluationModalOpen && (
            <div className="modal-overlay" onClick={closeEvaluationModal}>
              <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={closeEvaluationModal}>
                  ✖
                </button>
                
              </div>
            </div>
          )}

          <div className="lectureChat">실시간 채팅</div>
        </div>

        <div className="attendanceStatus">
          출결
          {!isProf && ( // 학생
            <>
              <div className="attendance">
                출석일수
                <br />
                전체 (강의일수)일 중
                <br />
                (출석일수)회
              </div>
              <div className="absence">
                결석일수
                <br />
                전체 (강의일수)일 중
                <br />
                (결석일수)회
              </div>
            </>
          )}
          <div className="attendanceCall">
            {!isProf ? ( // 학생
              <button className="attendanceCallBtn" onClick={attendanceRequestSubmit}>
                출석인정 신청
              </button>
            ) : ( // 교수
              <button className="attendanceCallBtn" onClick={openAttendanceModal}>
                출석인정 승인
              </button>
            )}
          </div>
          {isAttendanceModalOpen && <ApproveAttendanceModal onClose={closeAttendanceModal} lecSerial={selectedLecSerial}/>}
        </div>

        <div className="testAssignment">
          시험 및 과제
          {!isProf ? ( // 학생
            <>
              <div className="studentTest">
                중간고사 : 점
                <br />
                기말고사 : 점
              </div>
              <div className="studentAssignment">
                과제1 : 점
                <br />
                과제2 : 점
              </div>
            </>
          ) : ( // 교수
            <>
              <div className="profTest">
                <button className="testModalBtn" onClick={openTestModal}>
                  시험 관리
                </button>
              </div>
              <div className="profAssignment">
                <button className="assignmentCreateModalBtn" onClick={openAssignmentCreateModal}>
                  과제 생성
                </button>
                <br/>
                <table className="assignment-list">
                <thead>
                    <tr>
                        <th style={{ width: "10%" }}>번호</th>
                        <th style={{ width: "60%" }}>제목</th>
                        <th style={{ width: "30%" }}>마감일</th>
                    </tr>
                </thead>
                <tbody>
                    {assignmentList.content && assignmentList.content.length > 0 ? (
                        assignmentList.content.map((assignment, index) => {
                        const parsedData = JSON.parse(assignment.assignmentData);
                        const { title, dueDate } = parsedData.assignment;

                        const formatDate = (dateString) => {
                            if (!dateString || dateString.length !== 8) return dateString;
                            const year = dateString.slice(0, 4);
                            const month = dateString.slice(4, 6);
                            const day = dateString.slice(6, 8);
                            return `${year}-${month}-${day}`;
                        };


                        return (
                            <tr key={assignment.assignmentIdx}>
                            <td>{index + 1}</td>
                            <td style={{wordBreak: "keep-all"}}>{title}</td>
                            <td>{formatDate(dueDate)}</td>
                            </tr>
                        );
                        })
                    ) : (
                        <tr>
                        <td colSpan="3" style={{ textAlign: 'center' }}>과제가 없습니다.</td>
                        </tr>
                    )}
                </tbody>
            </table>
              </div>
              {isTestModalOpen && <TestModal onClose={closeTestModal} />}
              {isAssignmentCreateModalOpen && (
                <AssignmentCreateModal
                    onClose={closeAssignmentCreateModal}
                    lecSerial={selectedLecSerial}
                    lecTitle={lectureList.find(lec => lec.lecSerial === selectedLecSerial)?.lecTit || ''}
                />
                )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default ClassAttending;
