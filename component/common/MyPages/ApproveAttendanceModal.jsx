import { useState, useEffect } from "react";
import { UseUser } from "../../../hook/UseUser";
import "../../../css/MyPages/ApproveAttendanceModal.css";
import Pagination from "../notices/Pagination";

const ApproveAttendanceModal = ({ onClose, lecSerial }) => {
  const BASE_URL = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api";
  const { user } = UseUser();
  const accessToken = user?.data?.accessToken;

  // ✅ 상태 관리
  const [sessionNumber, setSessionNumber] = useState(null);
  const [studentList, setStudentList] = useState([]);
  const [attendanceCache, setAttendanceCache] = useState({}); // { [sessionNumber]: { [page]: students[] } }
  const [showRejectPrompt, setShowRejectPrompt] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);

  // ✅ 회차 입력 받기
  useEffect(() => {
    if (sessionNumber !== null) return;

    let input = null;
    while (true) {
      input = prompt("강의 회차를 입력하세요 (1~80)");
      if (input === null) {
        onClose();
        return;
      }
      const number = Number(input);
      if (!isNaN(number) && number >= 1 && number <= 80) {
        setSessionNumber(number);
        break;
      }
      alert("1~80 사이의 숫자를 입력해주세요.");
    }
  }, [sessionNumber, onClose]);

  // ✅ 학생 목록 불러오기
  const fetchStudentList = async (pageNum, sessionNum) => {
    if (!accessToken || !lecSerial || !sessionNum) return;
    setLoading(true);
    setError(null);

    try {
      const res = await fetch(`${BASE_URL}/enrollments/list`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify({ lecSerial, page: Math.max(pageNum - 1, 0), size: 20 }),
      });

      if (!res.ok) throw new Error(await res.text() || "학생 목록 조회 실패");

      const data = await res.json();

      setAttendanceCache((prev) => {
        const sessionCache = prev[sessionNum] || {};
        const prevPageList = sessionCache[pageNum] || [];

        const mergedList = data.content.map((student) => {
          const existing = prevPageList.find((s) => s.studentIdx === student.studentIdx);
          return {
            ...student,
            attendanceStatus: existing?.attendanceStatus ?? null,
          };
        });

        const sortedList = mergedList.sort((a, b) =>
          a.studentName.localeCompare(b.studentName, "ko", { sensitivity: "base" })
        );

        return {
          ...prev,
          [sessionNum]: {
            ...sessionCache,
            [pageNum]: sortedList,
          },
        };
      });

      setTotal(data.totalElements);
    } catch (err) {
      setError(err.message || "학생 목록 조회 중 오류 발생");
    } finally {
      setLoading(false);
    }
  };

  // ✅ 회차나 페이지 변경 시 캐시 확인 후 fetch
  useEffect(() => {
    if (!accessToken || !lecSerial || !sessionNumber) return;

    const cachedPage = attendanceCache[sessionNumber]?.[page];
    if (cachedPage) {
      setStudentList(cachedPage);
    } else {
      fetchStudentList(page, sessionNumber);
    }
  }, [sessionNumber, page, accessToken, lecSerial]);

  // ✅ 캐시 변경 시 현재 페이지 데이터 동기화
  useEffect(() => {
    const cachedPage = attendanceCache[sessionNumber]?.[page];
    if (cachedPage) {
      setStudentList(cachedPage);
    }
  }, [attendanceCache, sessionNumber, page]);

  // ✅ 출결 처리 (출석, 지각, 결석)
  const updateAttendance = async (studentIdx, status, reason = "") => {
    const requestData = {
      lecSerial,
      sessionNumber,
      attendanceRecords: [{ studentIdx, status, rejectReason: reason }],
    };

    try {
      const res = await fetch(`${BASE_URL}/attendance/approve`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify(requestData),
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.message || "출결 처리 실패");

      // ✅ 캐시 및 화면 데이터 동기화
      setAttendanceCache((prev) => {
        const sessionCache = prev[sessionNumber] || {};
        const pageCache = sessionCache[page] || [];

        const updatedPage = pageCache.map((s) =>
          s.studentIdx === studentIdx ? { ...s, attendanceStatus: status } : s
        );

        return {
          ...prev,
          [sessionNumber]: {
            ...sessionCache,
            [page]: updatedPage,
          },
        };
      });

      console.log("✅ 출결 처리 성공:", data);
    } catch (err) {
      console.error("❌ 출결 처리 오류:", err);
      alert("출결 처리 중 문제가 발생했습니다.");
    }
  };

  // ✅ 결석 사유 모달 처리
  const handleRejectClick = (student) => {
    setSelectedStudent(student);
    setShowRejectPrompt(true);
  };

  const handleRejectSubmit = async () => {
    if (!rejectReason.trim() || !selectedStudent) return;
    await updateAttendance(selectedStudent.studentIdx, "결", rejectReason);
    setShowRejectPrompt(false);
    setRejectReason("");
    setSelectedStudent(null);
  };

  // ✅ 색상 스타일
  const getRowStyle = (status) => {
    const colors = { 출: "#d2f8d2", 지: "#fff8b3", 결: "#ffd6d6" };
    return { backgroundColor: colors[status] || "transparent" };
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
            <table className="notice-table">
              <thead>
                <tr>
                  <th>학생 번호</th>
                  <th>이름</th>
                  <th>출석</th>
                  <th>지각</th>
                  <th>결석</th>
                </tr>
              </thead>
              <tbody>
                {studentList.length > 0 ? (
                  studentList.map((student) => (
                    <tr key={student.studentIdx} style={getRowStyle(student.attendanceStatus)}>
                      <td>{student.studentIdx}</td>
                      <td>{student.studentName}</td>
                      <td>
                        <button className="attendanceApproveClick" onClick={() => updateAttendance(student.studentIdx, "출")}>출석</button>
                      </td>
                      <td>
                        <button className="attendanceLateClick" onClick={() => updateAttendance(student.studentIdx, "지")}>지각</button>
                      </td>
                      <td>
                        <button className="attendanceRejectClick" onClick={() => handleRejectClick(student)}>결석</button>
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

            <Pagination page={page} size={20} total={total} onChange={setPage} />

            <button className="approveAttendanceCloseBtn" onClick={onClose}>
              닫기
            </button>
          </>
        )}
      </div>

      {/* ✅ 결석 사유 모달 */}
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
