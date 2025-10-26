import {useState, useEffect} from "react";
import {UseUser} from "../../../hook/UseUser";
import "../../../css/MyPages/AssignmentDetailModal.css"

const AssignmentDetailModal = ({onClose, assignmentIdx}) => {
    const maxScore = 10;
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const {user} = UseUser();
    const accessToken = user.data.accessToken;
    const [assignmentDetail, setAssignmentDetail] = useState();

    const fetchAssignmentDetail = async () => {
        const requestBody = {assignmentIdx: assignmentIdx}
        try {
            const response = await fetch(`${BASE_URL}/assignments/detail`, {
                method: "POST",
                headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestBody)
            });
            if (!response.ok) throw new Error('과제 상세 조회 실패');
            const data = await response.json();
            setAssignmentDetail(data);
        } catch (error) {
            console.error('과제 상세 에러:', error);
            setAssignmentDetail();
        }
    }

    useEffect(() => {
    if (accessToken && assignmentIdx) {
      fetchAssignmentDetail(accessToken, assignmentIdx);
    }
  }, [accessToken, assignmentIdx]);

    console.log("assignmentDetail : ", assignmentDetail);

    return (
            <div className="assignment-detail-modal-container">
                <div className="assignment-detail-modal-content">
                    <div>과목: </div>
                    <div>과제 제목:
                        {assignmentDetail.assignmentData.title}
                    </div>
                    <div>과제 설명:
                        {assignmentDetail.assignmentData.description}
                    </div>
                    <div>마감일:
                        {assignmentDetail.assignmentData.dueDate}
                    </div>
                    <div>배점: {maxScore}</div>
    
                    
                    <button className="assignmentDetailCloseBtn" onClick={onClose}>닫기</button>
                </div>
            </div>
        );
}

export default AssignmentDetailModal;