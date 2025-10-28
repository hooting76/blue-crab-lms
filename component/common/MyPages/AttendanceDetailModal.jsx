import { useState, useEffect } from "react";
import { UseUser } from "../../../hook/UseUser";
import "../../../css/MyPages/AttendanceDetailModal.css";

const AttendanceDetailModal = ({ onClose, lecSerial }) => {
  const { user } = UseUser();
  const accessToken = user.data.accessToken;
  const [attendanceDetail, setAttendanceDetail] = useState({});
  const BASE_URL = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api";

  const FetchAttendanceDetail = async () => {
    console.log("FetchAttendanceDetail 호출됨");
  try {
    const res = await fetch(`${BASE_URL}/attendance/student/view`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({lecSerial}),
    });


    if (!res.ok) {
      const text = await res.text();
      console.error("❌ 출결내역 조회 실패 응답:", text);
      throw new Error("출결내역 조회 실패");
    }

    const data = await res.json();
    console.log("✅ 출결내역 데이터:", data);
    setAttendanceDetail(data);
  } catch (err) {
    console.error("❌ 출결내역 조회 에러:", err);
  }
};


 useEffect(() => {
    console.log("lecSerial:", lecSerial, "accessToken:", accessToken);
    if (lecSerial && accessToken) {
        FetchAttendanceDetail();
    }
}, [lecSerial, accessToken]);



    
    console.log("attendanceDetail : ", attendanceDetail);

  return (
    <div className="attendance-detail-modal-container">
      <div className="attendance-detail-modal-content">
        <p>
          총 출석현황 :{" "}
          {attendanceDetail.summary.attended}/{attendanceDetail.summary.totalSessions}
        </p>

        <table className="notice-table">
          <thead>
            <tr>
              <th style={{ width: "50%" }}>회차</th>
              <th style={{ width: "50%" }}>출결여부</th>
            </tr>
          </thead>
          <tbody>
            {attendanceDetail?.sessions?.length > 0 ? (
              attendanceDetail.sessions.map((detail) => (
                <tr key={detail.sessionNumber}>
                  <td>{detail.sessionNumber}</td>
                  <td>{detail.status}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="2" style={{ textAlign: "center" }}>
                  출결 데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>

        <button className="attendanceDetailCloseBtn" onClick={onClose}>
          닫기
        </button>
      </div>
    </div>
  );
};

export default AttendanceDetailModal;
