import { useState, useEffect } from "react";
import { UseUser } from "../../../hook/UseUser";
import "../../../css/MyPages/ApproveAttendanceModal.css";
import Pagination from "../notices/Pagination";

const ApproveAttendanceModal = ({ onClose, lecSerial }) => {
  const BASE_URL = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api";
  const { user } = UseUser();
  const accessToken = user.data.accessToken;

  // ✅ 상태 관리
  const [sessionNumber, setSessionNumber] = useState(null);
  const [studentList, setStudentList] = useState([]);
  const [attendanceCache, setAttendanceCache] = useState({}); // ✅ 회차별 캐시
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

  // ✅ 학생 목록 불러오기 (API)
  const fetchStudentList = async (pageNum, sessionNum) => {
    if (!accessToken || sessionNum === null) return;
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

      // ✅ 새 목록 병합 (이전 출결 상태 유지)
      setAttendanceCache((prevCache) => {
        const prevList = prevCache[sessionNum] || [];
        const mergedList = data.content.map((student) => {
          const existing = prevList.find((s) => s.studentIdx === student.studentIdx);
          return {
            ...student,
            attendanceStatus: existing?.attendanceStatus ?? null,
          };
        });

        const sortedList = mergedList.sort((a, b) =>
          a.studentName.localeCompare(b.studentName, "ko", { sensitivity: "base" })
        );

        return {
          ...prevCache,
          [sessionNum]: sortedList,
        };
      });

      setTotal(data.totalElements);
    } catch (err) {
      setError(err.message || "알 수 없는 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // ✅ 회차 or 페이지 변경 시 처리
  useEffect(() => {
    if (!sessionNumber || !accessToken || !lecSerial) return;

    // 1️⃣ 캐시에 데이터가 있으면 API 요청 없이 사용
    if (attendanceCache[sessionNumber]) {
      setStudentList(attendanceCache[sessionNumber]);
    } else {
      // 2️⃣ 없으면 API 호출
      fetchStudentList(page, sessionNumber);
    }
  }, [sessionNumber, page, accessToken, lecSerial]);

  // ✅ attendanceCache 변경 시 studentList 동기화
  useEffect(() => {
    if (attendanceCache[sessionNumber]) {
      setStudentList(attendanceCache[sessionNumber]);
    }
  }, [attendanceCache, sessionNumber]);

  // ✅ 출석/지각/결석 처리
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

      // ✅ 출결 상태 캐시 및 화면 모두 업데이트
      setAttendanceCache((prevCache) => {
        const updated = (prevCache[sessionNumber] || []).map((s) =>
          s.studentIdx === studentIdx ? { ...s, attendanceStatus: status } : s
        );
        return { ...prevCache, [sessionNumber]: updated };
      });

      console.log("✅ 출결 처리 성공:", data);
    } catch (error) {
      console.error("❌ 출결 처리 오류:", error);
      alert("출결 처리 중 문제가 발생했습니다.");
    }
  };

  // ✅ 결석 사유 모달
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
                    <tr
                      key={student.studentIdx}
                      style={getRowStyle(student.attendanceStatus)}
                    >
                      <td>{student.studentIdx}</td>
                      <td>{student.studentName}</td>
                      <td>
                        <button onClick={() => updateAttendance(student.studentIdx, "출")}>
                          출석
                        </button>
                      </td>
                      <td>
                        <button onClick={() => updateAttendance(student.studentIdx, "지")}>
                          지각
                        </button>
                      </td>
                      <td>
                        <button onClick={() => handleRejectClick(student)}>결석</button>
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
