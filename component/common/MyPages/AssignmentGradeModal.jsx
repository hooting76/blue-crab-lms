import { useState, useEffect } from "react";
import { UseUser } from "../../../hook/UseUser";
import "../../../css/MyPages/AssignmentGradeModal.css";
import Pagination from "../notices/Pagination";

const AssignmentGradeModal = ({ onClose, lecSerial, assignmentIdx }) => {
  const BASE_URL = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api";
  const { user } = UseUser();
  const accessToken = user?.data?.accessToken;

  // ✅ 상태 관리
  const [studentList, setStudentList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);

  // ✅ 학생 목록 불러오기
  const fetchStudentList = async (pageNum = 1) => {
    if (!accessToken || !lecSerial) return;

    setLoading(true);
    setError(null);

    try {
      const res = await fetch(`${BASE_URL}/enrollments/list`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify({
          lecSerial,
          page: Math.max(pageNum - 1, 0),
          size: 20,
        }),
      });

      if (!res.ok) throw new Error((await res.text()) || "학생 목록 조회 실패");

      const data = await res.json();
      console.log("data : ", data);

      setStudentList(data.content || data.students || []);
      setTotal(data.totalElements || 0);
    } catch (err) {
      console.error("❌ 학생 목록 조회 오류:", err);
      setError(err.message || "학생 목록 조회 중 오류 발생");
    } finally {
      setLoading(false);
    }
  };

  // ✅ 페이지 변경 시 학생 목록 다시 불러오기
  useEffect(() => {
    fetchStudentList(page);
  }, [page]);

  // ✅ 특정 학생 과제 채점
  const assignmentGrade = async (student) => {
    if (!student) return alert("학생 정보가 없습니다.");
    console.log("student : ", student);

    const score = prompt(`${student.studentName} 학생의 점수를 입력하세요:`); // 간단 입력
    const feedback = prompt("피드백을 입력하세요:") || "";

    if (score === null) return; // 취소 시 종료

    const requestData = {
      studentIdx: student.data.user.id,
      score: Number(score),
      feedback
    };

    try {
      const res = await fetch(`${BASE_URL}/assignments/${assignmentIdx}/grade`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify(requestData)
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.message || "과제 채점 실패");

      alert("✅ 과제 채점이 완료되었습니다!");
      console.log("✅ 과제 채점 성공:", data);
    } catch (err) {
      console.error("❌ 과제 채점 오류:", err);
      alert("과제 채점 중 문제가 발생했습니다: " + err.message);
    }
  };

  return (
    <div className="assignmentGrade-modal-container">
      <div className="assignmentGrade-modal-content">
        <h2>과제 채점</h2>

        {loading ? (
          <p>불러오는 중...</p>
        ) : error ? (
          <p className="error">{error}</p>
        ) : (
          <table className="notice-table">
            <thead>
              <tr>
                <th>학생 번호</th>
                <th>이름</th>
                <th>채점</th>
              </tr>
            </thead>
            <tbody>
              {studentList.length > 0 ? (
                studentList.map((student) => (
                  <tr key={student.studentIdx}>
                    <td>{student.studentIdx}</td>
                    <td>{student.studentName}</td>
                    <td>
                      <button
                        className="assignmentGradeClick"
                        onClick={() => assignmentGrade(student)}
                      >
                        채점하기
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="3">학생 목록 없음</td>
                </tr>
              )}
            </tbody>
          </table>
        )}

        <Pagination page={page} size={20} total={total} onChange={setPage} />

        <button className="assignmentGradeCloseBtn" onClick={onClose}>
          닫기
        </button>
      </div>
    </div>
  );
};

export default AssignmentGradeModal;
