import {useState, useEffect} from "react";
import "../../../css/MyPages/AttendanceDetailModal.css";

const AttendanceDetailModal = ({onClose}) => {
    const [attendanceDetail, setAttendanceDetail] = useState();

     const FetchAttendanceDetail = async () => {
        try {
            const body = {
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

    return (
        <div className="attendance-detail-modal-container">
            <div className="attendance-detail-modal-content">
                {assignmentData ? (
                    <>
                        <div>과제 제목: {assignmentData.assignment.title}</div>
                        <div>과제 설명: {assignmentData.assignment.description}</div>
                        <div>마감일: {formatDate(assignmentData.assignment.dueDate)}</div>
                        <div>배점: {assignmentData.assignment.maxScore}</div>
                    </>
                ) : (
                    <div>로딩 중...</div>
                )}

                
                <button className="attendanceDetailCloseBtn" onClick={onClose}>닫기</button>
            </div>
        </div>
    );
}

export default AttendanceDetailModal;