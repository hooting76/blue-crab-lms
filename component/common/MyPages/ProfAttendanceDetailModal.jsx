import {useState, useEffect} from "react";
import {UseUser} from "../../../hook/UseUser";
import "../../../css/MyPages/ProfAttendanceDetailModal.css";

const ProfAttendanceDetailModal = ({onClose, lecSerial}) => {
    const {user} = UseUser();
    const accessToken = user.data.accessToken;
    const BASE_URL = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api";
    const [profAttendanceDetail, setProfAttendanceDetail] = useState(null);

    const FetchProfAttendanceDetail = async() => {
        try {
            const response = await fetch(`${BASE_URL}/attendance/professor/view`, {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${accessToken}`,
                    "Contend-Type": "application/json"
                },
                body: JSON.stringify({lecSerial})
            });

            if (!response.ok) {
                const text = await response.text();
                console.error("❌ 출결내역 조회 실패 응답:", text);
                throw new Error("출결내역 조회 실패");
            }

             const data = await response.json();
            console.log("✅ 출결내역 데이터:", data.data);
            setProfAttendanceDetail(data.data);
            } catch (err) {
            console.error("❌ 출결내역 조회 에러:", err);
            }
        };

    // useEffect에서 user와 lecSerial이 준비되면 Fetch 호출
    useEffect(() => {
        if (lecSerial && accessToken) {
        FetchProfAttendanceDetail();
        }
    }, [lecSerial, accessToken]);



    return (
    <div className="prof-attendance-detail-modal-container">
      <div className="prof-attendance-detail-modal-content">
        <table className="notice-table">
          <thead>
            <tr>
              <th style={{ width: "20%" }}>번호</th>
              <th style={{ width: "50%" }}>이름</th>
              <th style={{ width: "30%" }}>출석률</th>
            </tr>
          </thead>
          <tbody>
            {profAttendanceDetail?.length > 0 ? (
              profAttendanceDetail.map((detail) => (
                <tr key={detail.studentIdx}>
                  <td>{detail.studentIdx}</td>
                  <td>{detail.studentName}</td>
                  <td>{detail.attendanceRate}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="3" style={{ textAlign: "center" }}>
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
    )
};

export default ProfAttendanceDetailModal;