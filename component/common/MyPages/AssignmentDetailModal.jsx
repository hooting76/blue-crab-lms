import {useState, useEffect} from "react";
import {UseUser} from "../../../hook/UseUser";
import "../../../css/MyPages/AssignmentDetailModal.css"

const AssignmentDetailModal = ({onClose, assignmentIdx}) => {
    const maxScore = 10;
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const {user} = UseUser();
    const accessToken = user.data.accessToken;
    const [assignmentData, setAssignmentData] = useState();

    const fetchAssignmentData = async () => {
        const requestBody = {assignmentIdx: assignmentIdx}
        try {
            const response = await fetch(`${BASE_URL}/assignments/data`, {
                method: "POST",
                headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestBody)
            });
            if (!response.ok) throw new Error('과제 상세 조회 실패');
            const data = await response.json();
            setAssignmentData(data);
        } catch (error) {
            console.error('과제 상세 에러:', error);
            setAssignmentData();
        }
    }

    useEffect(() => {
    if (accessToken && assignmentIdx) {
      fetchAssignmentData(accessToken, assignmentIdx);
    }
  }, [accessToken, assignmentIdx]);

    console.log("assignmentData : ", assignmentData);

    return (
            <div className="assignment-detail-modal-container">
                <div className="assignment-detail-modal-content">
                    <div>과제 제목:
                        {assignmentData.assignment.title}
                    </div>
                    <div>과제 설명:
                        {assignmentData.assignment.description}
                    </div>
                    <div>마감일:
                        {assignmentData.assignment.dueDate}
                    </div>
                    <div>배점: {maxScore}</div>
    
                    
                    <button className="assignmentDetailCloseBtn" onClick={onClose}>닫기</button>
                </div>
            </div>
        );
}

export default AssignmentDetailModal;