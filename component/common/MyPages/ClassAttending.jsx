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

  // 1. 선택한 강의 ID 상태 추가
  const [selectedLecIdx, setSelectedLecIdx] = useState();

  // 모달 상태들
  const [isAttendanceModalOpen, setIsAttendanceModalOpen] = useState(false);
  const [isTestModalOpen, setIsTestModalOpen] = useState(false);
  const [isAssignmentCreateModalOpen, setIsAssignmentCreateModalOpen] = useState(false);
  const [isClassDetailModalOpen, setIsClassDetailModalOpen] = useState(false);


  // 강의 목록 받아올 때 첫 강의로 기본 선택 설정
  useEffect(() => {
    if (lectureList.length > 0) {
      setSelectedLecIdx(lectureList[0].lecIdx);
    }
  }, [lectureList]);

  // 강의 선택 변경 핸들러
  const handleLectureChange = (e) => {
    setSelectedLecIdx(e.target.value);
  };

  // 강의 목록 가져오기 (교수/학생 구분)
  const fetchLectureList = async (accessToken, user) => {
    try {
      const requestBody = {
        page: 0,
        size: 20,
        professor: String(user.data.user.id),
      };

      const response = await fetch(`${BASE_URL}/lectures`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody),
      });

      if (!response.ok) throw new Error('강의 목록을 불러오는 데 실패했습니다.');

      const data = await response.json();
      setLectureList(data);
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
        enrolled: true,
      };

      const response = await fetch(`${BASE_URL}/enrollments/list`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody),
      });

      if (!response.ok) throw new Error('강의 목록을 불러오는 데 실패했습니다.');

      const data = await response.json();
      setLectureList(data);
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
  }, [accessToken, user]);

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

  console.log("lectureList : ", lectureList);

  

  return (
    <div className="classAttending_list_container">
      {/* 강의 선택 박스 */}
      <select className="lectureName" value={selectedLecIdx || ''} onChange={handleLectureChange}>
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
          <div className="lectureNotice">과목별 공지사항</div>

          {isProf ? (
            <div className="profNoticeWriteBtnArea">
              <button className="profNoticeWriteBtn" onClick={profNoticeWrite}>
                과목별 공지 작성
              </button>
            </div>
          ) : (
            <div className="studentClassDetailBtnArea">
              <button className="studentClassDetailBtn" onClick={openClassDetailModal}>
                강의 상세 정보
              </button>
            </div>
          )}

          {/* 강의 상세 모달 */}
          {isClassDetailModalOpen && (
            <div className="modal-overlay" onClick={closeClassDetailModal}>
              <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={closeClassDetailModal}>
                  ✖
                </button>
                <CourseDetail
                  lecture={lectureList.find((lec) => lec.lecIdx === selectedLecIdx)}
                  currentPage={currentPage}
                  setCurrentPage={setCurrentPage}
                />
              </div>
            </div>
          )}

          <div className="lectureChat">실시간 채팅</div>
        </div>

        <div className="attendanceStatus">
          출결
          {!isProf && (
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
            {!isProf ? (
              <button className="attendanceCallBtn" onClick={attendanceRequestSubmit}>
                출석인정 신청
              </button>
            ) : (
              <button className="attendanceCallBtn" onClick={openAttendanceModal}>
                출석인정 승인
              </button>
            )}
          </div>
          {isAttendanceModalOpen && <ApproveAttendanceModal onClose={closeAttendanceModal} />}
        </div>

        <div className="testAssignment">
          시험 및 과제
          {!isProf ? (
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
          ) : (
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
              </div>
              {isTestModalOpen && <TestModal onClose={closeTestModal} />}
              {isAssignmentCreateModalOpen && (
                <AssignmentCreateModal
                onClose={closeAssignmentCreateModal}
                lecSerial={selectedLecIdx}
                lecTitle={lectureList.find(lec => lec.lecIdx === selectedLecIdx)?.lecTit || ''}/>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default ClassAttending;
