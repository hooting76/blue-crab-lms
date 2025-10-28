import {useState, useEffect} from "react";
import { UseUser } from "../../../hook/UseUser";
import "../../../css/MyPages/AttendanceDetailModal.css";

const AttendanceDetailModal = ({onClose, enrollmentIdx}) => {
    const {user} = UseUser();
    const accessToken = user.data.accessToken;
    const [attendanceDetail, setAttendanceDetail] = useState();
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

     const FetchAttendanceDetail = async () => {
        try {
            const body = {
                enrollmentIdx: enrollmentIdx
            };

            const res = await fetch(`${BASE_URL}/student/attendance/detail`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${accessToken}`,
            },
            body: JSON.stringify(body),
            });

            const data = await res.json();
            if (!res.ok) throw new Error(`출결내역 조회 실패: ${data.message || 'Unknown error'}`);
            setAttendanceDetail(data);
        } catch (err) {
            console.error('❌ 출결내역 조회 에러:', err);
        }
        };

    useEffect(() => {
        FetchAttendanceDetail();
    }, [enrollmentIdx]);
    

    return (
    <div className="attendance-detail-modal-container">
        <div className="attendance-detail-modal-content">

        <p>
            총 출석현황 :{" "}
            {attendanceDetail ? attendanceDetail.data.attendanceRate : "해당사항 없음"}
        </p>

        <table className="notice-table">
            <thead>
            <tr>
                <th style={{ width: "50%" }}>회차</th>
                <th style={{ width: "50%" }}>출결여부</th>
            </tr>
            </thead>
            <tbody>
            {attendanceDetail && attendanceDetail.data.details.length > 0 ? (
                attendanceDetail.data.details.map((detail) => (
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

        <button className="attendanceDetailCloseBtn" onClick={onClose}>닫기</button>
        </div>
    </div>
    );

}

export default AttendanceDetailModal;