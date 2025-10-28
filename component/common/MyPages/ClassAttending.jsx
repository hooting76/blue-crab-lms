import React, { useState, useEffect } from 'react';
import '../../../css/MyPages/ClassAttending.css';
import { UseUser } from '../../../hook/UseUser';
import ClassAttendingNotice from './ClassAttendingNotice.jsx';
import ApproveAttendanceModal from './ApproveAttendanceModal.jsx';
import TestModal from './TestModal.jsx';
import AssignmentCreateModal from './AssignmentCreateModal.jsx';
import ProfNoticeWritingPage from './ProfNoticeWritingPage.jsx';
import CourseDetail from './CourseDetail';
import AssignmentDetailModal from './AssignmentDetailModal.jsx';

function ClassAttending({ currentPage, setCurrentPage, selectedLectureSerial, setSelectedLectureSerial }) {
  const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
  const { user } = UseUser();
  const accessToken = user.data.accessToken;
  const isProf = user.data.user.userStudent === 1;
  const [lectureList, setLectureList] = useState([]);
  const [assignmentList, setAssignmentList] = useState([]);
  const [selectedAssignmentIdx, setSelectedAssignmentIdx] = useState(null);
  const [noticeList, setNoticeList] = useState([]);
  const page = 0;
  const NOTICE_BOARD_CODE = 3;
  const sessionNumber = 1;
  const requestReason = "";

  // 모달 상태들
  const [isEvaluationModalOpen, setIsEvaluationModalOpen] = useState(false);
  const [isAttendanceModalOpen, setIsAttendanceModalOpen] = useState(false);
  const [isTestModalOpen, setIsTestModalOpen] = useState(false);
  const [isAssignmentCreateModalOpen, setIsAssignmentCreateModalOpen] = useState(false);
  const [isClassDetailModalOpen, setIsClassDetailModalOpen] = useState(false);
  const [isAssignmentDetailModalOpen, setIsAssignmentDetailModalOpen] = useState(false);


  // 강의 목록 받아올 때 첫 강의로 기본 선택 설정
    useEffect(() => {
      if (isProf) {
        // 교수일 때: lectureList 자체 사용
        if (lectureList.length > 0) {
          setSelectedLectureSerial(lectureList[0].lecSerial);
        }
      } else {
        // 교수 아닐 때: lectureList.content 사용
        if (lectureList?.content?.length > 0) {
          setSelectedLectureSerial(lectureList.content[0].lecSerial);
        }
      }
    }, [lectureList, isProf]);

  // 강의 선택 변경 핸들러
  const handleLectureChange = (e) => {
  setSelectedLectureSerial(e.target.value); // e.target.value = lecSerial
};


  // 강의 목록 가져오기 (교수/학생 구분)
const fetchLectureData = async (accessToken, user, isProf) => {
  try {
    const requestBody = isProf
      ? {
          page: 0,
          size: 100,
          professor: String(user.data.user.id)
        }
      : {
          page: 0,
          size: 100,
          studentIdx: Number(user.data.user.id)
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

// 공지 목록 불러오기
const fetchNotices = async () => {
        if (!accessToken || !selectedLectureSerial) return;
        try {
            const response = await fetch(`${BASE_URL}/boards/list`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    page: 0,
                    size: 5,
                    boardCode: NOTICE_BOARD_CODE,
                    lecSerial: selectedLectureSerial
                }),
            });

            if (!response.ok) throw new Error('공지사항 조회 실패');
            const data = await response.json();
            setNoticeList(data.content);
        } catch (error) {
            console.error('공지사항 에러:', error);
            setNoticeList([]);
        }
    };

    // Base64 디코딩 함수
    const decodeBase64 = (str) => {
        try {
            const cleanStr = str.replace(/\s/g, '');
            const binary = atob(cleanStr);
            return decodeURIComponent(
                Array.prototype.map
                    .call(binary, (ch) => '%' + ('00' + ch.charCodeAt(0).toString(16)).slice(-2))
                    .join('')
            );
        } catch (e) {
            console.error("Base64 디코딩 오류:", e);
            return "";
        }
    };

    // 공지 작성시간 변환 함수
    const formatTime = (timeStr) => {
        const date = new Date(timeStr);
        const now = new Date();

        const isToday =
            date.getFullYear() === now.getFullYear() &&
            date.getMonth() === now.getMonth() &&
            date.getDate() === now.getDate();

        const formatted = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;

        if (isToday) return formatted;

        return `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')} ${formatted}`;
    };


  // 과제 목록 불러오기
  const getAssignments = async(accessToken, selectedLectureSerial) => {
    const requestBody = {lecSerial: selectedLectureSerial, page: 0, size: 100, action: "list"}
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
  if (!selectedLectureSerial) {
    // 강의가 선택되지 않은 경우 → 과제 목록 초기화
    setAssignmentList({ content: [] }); // or setAssignmentList([])
    return;
  }
  // 강의가 선택되어 있을 때만 과제 목록 불러오기
  getAssignments(accessToken, selectedLectureSerial);
}, [accessToken, selectedLectureSerial]);



  useEffect(() => {
    getAssignments(accessToken, selectedLectureSerial);
  }, [accessToken, selectedLectureSerial]);


  // ✅ 과제 삭제 후 목록 갱신 함수 추가
  const handleDeleteAssignment = async () => {
    await getAssignments(accessToken, selectedLectureSerial); // 최신 목록 다시 불러오기
  };



  // 학생 출석 요청
 const attendanceRequestSubmit = async () => {
  try {
    const body = {
      lecSerial: String(selectedLectureSerial),
      sessionNumber: sessionNumber,
      requestReason: requestReason || null
    };


    const res = await fetch(`${BASE_URL}/attendance/request`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify(body),
    });

    const data = await res.json();
    if (!res.ok) throw new Error(`출석 요청 실패: ${data.message || 'Unknown error'}`);
    alert("출석 요청을 성공적으로 보냈습니다.")
    console.log('✅ 출석 요청 성공:', data);
  } catch (err) {
    console.error('❌ 출석 요청 에러:', err);
    alert("출석 요청 실패");
  }
};

  // 과제 상세 모달 이동
  const toAssignmentDetailModal = (assignmentIdx) => {
    setSelectedAssignmentIdx(assignmentIdx); // 선택한 과제 idx 저장
    openAssignmentDetailModal(); // 모달 열기
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
  const openAssignmentDetailModal = () => setIsAssignmentDetailModalOpen(true);
  const closeAssignmentDetailModal = () => setIsAssignmentDetailModalOpen(false);

  // 과목별 공지 페이지 렌더링
  if (currentPage === "수강/강의과목 공지사항") {
    return <ClassAttendingNotice currentPage={currentPage} setCurrentPage={setCurrentPage} selectedLecSerial={selectedLectureSerial}/>
  }
  
  // 공지 작성 페이지 렌더링
  if (currentPage === '과목별 공지 작성') {
    return <ProfNoticeWritingPage currentPage={currentPage} setCurrentPage={setCurrentPage} />;
  }


  return (
<div className="classAttending_list_container">
      {/* 강의 선택 박스 */}
      {isProf ? (
        <select
          className="lectureName"
          value={selectedLectureSerial || ''}
          onChange={handleLectureChange}
        >
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
      ) : (
        <select
          className="lectureName"
          value={selectedLectureSerial || ''}
          onChange={handleLectureChange}
        >
          {lectureList?.content?.length > 0 ? (
            lectureList.content.map((lec) => (
              <option key={lec.lecSerial} value={lec.lecSerial}>
                {lec.lecTit}
              </option>
            ))
          ) : (
            <option disabled>강의 목록 없음</option>
          )}
        </select>
      )}


      <div className="classAttendingContent">
        <div className="noticeAndChat">
          <div className="lectureNotice">
            과목별 공지사항
             <table className="notice-table">
                <thead>
                    <tr>
                        <th style={{ width: "70%" }}>제목</th>
                        <th style={{ width: "30%" }}>작성일</th>
                    </tr>
                </thead>
                <tbody>
                    {noticeList.length > 0 ? (
                        noticeList.map((notice) => {
                            return (
                                <tr key={notice.boardIdx} onClick={() => setCurrentPage("수강/강의과목 공지사항")} style={{ cursor: "pointer" }}>
                                    <td>{decodeBase64(notice.boardTitle)}</td>
                                    <td>{formatTime(notice.boardReg)}</td>
                                </tr>
                            );
                        })
                    ) : (
                        <tr>
                            <td colSpan="4">공지사항이 없습니다.</td>
                        </tr>
                    )}
                </tbody>
            </table>
          </div>

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
                  lectureDetails={lectureList.content.find(
                    (lec) => String(lec.lecSerial) === String(selectedLectureSerial)
                  )}
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
          {isAttendanceModalOpen && <ApproveAttendanceModal onClose={closeAttendanceModal} lecSerial={selectedLectureSerial}/>}
        </div>

        <div className="testAssignment">
          <p>시험 및 과제</p>

          {!isProf &&
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
          }

          {/* 과제 목록 (학생/교수 공용) */}
          <div className="assignmentList">
            {isProf && (
              <button className="assignmentCreateModalBtn" onClick={openAssignmentCreateModal}>
                과제 생성
              </button>
            )}
            <br />
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
                      <tr key={assignment.assignmentIdx} onClick={() => toAssignmentDetailModal(assignment.assignmentIdx)}>
                        <td>{index + 1}</td>
                        <td style={{ wordBreak: "keep-all" }}>{title}</td>
                        <td>{formatDate(dueDate)}</td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan="3" style={{ textAlign: "center" }}>과제가 없습니다.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {isProf && isAssignmentCreateModalOpen && (
            <AssignmentCreateModal
              lecSerial={selectedLectureSerial}
              lecTitle={lectureList.find(lec => lec.lecSerial === selectedLectureSerial)?.lecTit || ''}
              onClose={async () => {
                closeAssignmentCreateModal();          // 모달 닫기
                await getAssignments(accessToken, selectedLectureSerial);  // ✅ 과제 목록 갱신
              }}
            />
          )}


          {isAssignmentDetailModalOpen && (
            <AssignmentDetailModal
              onClose={closeAssignmentDetailModal}
              onDelete={handleDeleteAssignment}   // ✅ 추가
              assignmentIdx={selectedAssignmentIdx}
            />
          )}


          {isProf && isTestModalOpen && <TestModal onClose={closeTestModal} />}
        </div>
      </div>
    </div>
  );
}

export default ClassAttending;
