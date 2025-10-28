import { useState, useEffect } from "react";
import { UseUser } from "../../../hook/UseUser";
import "../../../css/MyPages/ApproveAttendanceModal.css";
import Pagination from "../notices/Pagination";

const ApproveAttendanceModal = ({ onClose, lecSerial }) => {
  const BASE_URL = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api";
  const { user } = UseUser();

  const [sessionNumber, setSessionNumber] = useState(1); // 기본값 1
  const [studentList, setStudentList] = useState([]);
  const [showRejectPrompt, setShowRejectPrompt] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);

  const accessToken = user.data.accessToken;

  const handlePageChange = (newPage) => {
    setPage(newPage);
  };

  // ✅ 학생 목록 불러오기
  const fetchStudentList = async (accessToken, lecSerial, page) => {
    if (!accessToken) return;

    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`${BASE_URL}/enrollments/list`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify({ lecSerial: lecSerial, page: Math.max(page - 1, 0), size: 20 }),
      });

      if (!response.ok) {
        const errMsg = await response.text();
        console.error("서버 에러 응답:", errMsg);
        throw new Error(errMsg || "학생 목록 조회 실패");
      }

      const data = await response.json();

      // ✅ 이름순 정렬
      const sortedList = [...data.content].sort((a, b) =>
        a.studentName.localeCompare(b.studentName, "ko", { sensitivity: "base" })
      );

      // 출석 상태를 추적할 수 있도록 초기값 추가
      const initializedList = sortedList.map((student) => ({
        ...student,
        attendanceStatus: null, // '출', '지', '결' 중 하나가 나중에 들어감
      }));

      setStudentList(initializedList);
      setTotal(data.totalElements);
    } catch (error) {
      console.error("학생 목록 조회 에러:", error);
      setError(error.message || "알 수 없는 에러가 발생했습니다.");
      setStudentList([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (accessToken && lecSerial) {
      fetchStudentList(accessToken, lecSerial, page);
    }
  }, [accessToken, lecSerial, page]);

  // ✅ 출석 승인 (단일 학생 단위)
  const approveAttendance = async (studentIdx, status) => {
    const requestData = {
      lecSerial,
      sessionNumber,
      attendanceRecords: [
        {
          studentIdx,
          status, // '출', '지', '결'
        },
      ],
    };

    try {
      const response = await fetch(`${BASE_URL}/attendance/approve`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify(requestData),
      });

      const data = await response.json();

      if (response.ok) {
        console.log("✅ 출석 승인 성공:", data);

        // ✅ 승인된 학생 상태를 로컬에서 업데이트
        setStudentList((prevList) =>
          prevList.map((s) =>
            s.studentIdx === studentIdx
              ? { ...s, attendanceStatus: status }
              : s
          )
        );
      } else {
        console.error("❌ 출석 승인 실패:", data);
      }
    } catch (error) {
      console.error("❌ 네트워크 오류:", error);
    }
  };

  // ✅ 결석 처리
  const rejectAttendance = async (studentIdx, reason) => {
    const requestData = {
      lecSerial,
      sessionNumber,
      attendanceRecords: [
        {
          studentIdx,
          status: "결",
          reason,
        },
      ],
    };

    try {
      const response = await fetch(`${BASE_URL}/attendance/approve`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify(requestData),
      });

      const data = await response.json();

      if (response.ok) {
        console.log("✅ 결석 처리 완료:", data);

        // ✅ 결석 학생 색상 갱신
        setStudentList((prevList) =>
          prevList.map((s) =>
            s.studentIdx === studentIdx
              ? { ...s, attendanceStatus: "결" }
              : s
          )
        );
      } else {
        console.error("❌ 결석 처리 실패:", data);
      }
    } catch (error) {
      console.error("❌ 네트워크 오류:", error);
    }
  };

  // ✅ 결석 버튼 클릭 시 모달 오픈
  const handleRejectClick = (student) => {
    setSelectedStudent(student);
    setShowRejectPrompt(true);
  };

  // ✅ 결석 사유 제출
  const handleRejectSubmit = async () => {
    if (!rejectReason || !selectedStudent) return;
    await rejectAttendance(selectedStudent.studentIdx, rejectReason);

    // 모달 닫기 및 상태 초기화
    setShowRejectPrompt(false);
    setRejectReason("");
    setSelectedStudent(null);
  };

  // ✅ 색상 스타일 함수
  const getRowStyle = (status) => {
    switch (status) {
      case "출":
        return { backgroundColor: "#d2f8d2" }; // 연초록
      case "지":
        return { backgroundColor: "#fff8b3" }; // 연노랑
      case "결":
        return { backgroundColor: "#ffd6d6" }; // 연빨강
      default:
        return {};
    }
  };

  return (
    <div className="approve-attendance-modal-container">
      <div className="approve-attendance-modal-content">
        {loading ? (
          <p>불러오는 중...</p>
        ) : error ? (
          <p style={{ color: "red" }}>{error}</p>
        ) : (
          <>
          <div className="session-number-input">
            <label>강의 회차 (1~80): </label>
            <input
              type="number"
              min="1"
              max="80"
              value={sessionNumber}
              onChange={(e) => setSessionNumber(Number(e.target.value))}
            />
          </div>


          <table className="notice-table">
            <thead>
              <tr>
                <th style={{ width: "20%" }}>학생 번호</th>
                <th style={{ width: "50%" }}>이름</th>
                <th style={{ width: "10%" }}>출석</th>
                <th style={{ width: "10%" }}>지각</th>
                <th style={{ width: "10%" }}>결석</th>
              </tr>
            </thead>
            <tbody>
              {studentList.length > 0 ? (
                studentList.map((student) => (
                  <tr
                    key={student.studentIdx}
                    style={getRowStyle(student.attendanceStatus)}
                  >
                    <td>{student.studentIdx}</td>
                    <td>{student.studentName}</td>
                    <td>
                      <button
                        className="attendanceApproveClick"
                        onClick={() =>
                          approveAttendance(student.studentIdx, "출")
                        }
                      >
                        출석
                      </button>
                    </td>
                    <td>
                      <button
                        className="attendanceLateClick"
                        onClick={() =>
                          approveAttendance(student.studentIdx, "지")
                        }
                      >
                        지각
                      </button>
                    </td>
                    <td>
                      <button
                        className="attendanceRejectClick"
                        onClick={() => handleRejectClick(student)}
                      >
                        결석
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5">학생 목록 없음</td>
                </tr>
              )}
            </tbody>
          </table>
          </>
        )}

        <Pagination page={page} size={20} total={total} onChange={handlePageChange} />
        <button className="approveAttendanceCloseBtn" onClick={onClose}>
          닫기
        </button>
      </div>

      {showRejectPrompt && (
        <div className="reject-reason-modal">
          <div className="modal-content">
            <h3>결석 사유 입력</h3>
            <textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="결석 사유를 입력하세요"
            />
            <div className="modal-buttons">
              <button onClick={handleRejectSubmit}>제출</button>
              <button onClick={() => setShowRejectPrompt(false)}>취소</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ApproveAttendanceModal;
