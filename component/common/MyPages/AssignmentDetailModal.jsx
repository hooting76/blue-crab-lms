import { useState, useEffect } from "react";
import { UseUser } from "../../../hook/UseUser";
import "../../../css/MyPages/AssignmentDetailModal.css"

const AssignmentDetailModal = ({ onClose, onDelete, assignmentIdx }) => {
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const { user } = UseUser();
    const accessToken = user.data.accessToken;
    const isProf = user.data.user.userStudent === 1;
    const [assignmentData, setAssignmentData] = useState();

    // ✅ 과제 상세 불러오기
    const fetchAssignmentData = async () => {
        const requestBody = { assignmentIdx };
        try {
            const response = await fetch(`${BASE_URL}/assignments/data`, {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${accessToken}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(requestBody),
            });
            if (!response.ok) throw new Error("과제 상세 조회 실패");
            const data = await response.json();
            setAssignmentData(data);
        } catch (error) {
            console.error("과제 상세 에러:", error);
            setAssignmentData(null);
        }
    };

    useEffect(() => {
        if (accessToken && assignmentIdx) {
            fetchAssignmentData();
        }
    }, [accessToken, assignmentIdx]);

    // ✅ YYYYMMDD → YYYY-MM-DD 변환 함수
    const formatDate = (dateStr) => {
        if (!dateStr || dateStr.length !== 8) return dateStr;
        const year = dateStr.slice(0, 4);
        const month = dateStr.slice(4, 6);
        const day = dateStr.slice(6, 8);
        return `${year}-${month}-${day}`;
    };

    // ✅ 과제 삭제 함수
    const assignmentDelete = async () => {
        if (!window.confirm("정말 이 과제를 삭제하시겠습니까?")) return;

        try {
            const response = await fetch(`${BASE_URL}/assignments/${assignmentIdx}`, {
                method: "DELETE",
                headers: {
                    "Authorization": `Bearer ${accessToken}`,
                    "Content-Type": "application/json",
                },
            });

            const data = await response.json();

            if (response.ok) {
                alert(data.message || "과제가 삭제되었습니다.");
                if (onDelete) onDelete(assignmentIdx); // 상위 컴포넌트에 알림
                onClose(); // 모달 닫기
            } else {
                alert(data.message || "과제 삭제에 실패했습니다.");
            }
        } catch (error) {
            console.error("과제 삭제 오류:", error);
            alert("과제 삭제 중 오류가 발생했습니다.");
        }
    };

    return (
        <div className="assignment-detail-modal-container">
            <div className="assignment-detail-modal-content">
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

                {isProf && (
                    <button className="assignmentDeleteBtn" onClick={assignmentDelete}>
                        과제 삭제
                    </button>
                )}
                <br/>
                <button className="assignmentDetailCloseBtn" onClick={onClose}>닫기</button>
            </div>
        </div>
    );
};

export default AssignmentDetailModal;
