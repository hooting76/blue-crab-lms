import {useState, useEffect} from "react";

const AssignmentDetailModal = ({onClose}) => {
    const maxScore = 10;
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const [assignmentDetail, setAssignmentDetail] = useState();

    const fetchAssignmentDetail = async () => {
        const requestBody = {}
        try {
            const response = await fetch(`${BASE_URL}/assignments/${assignmentIdx}`, {
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

    return (
            <div className="assignment-detail-modal-container">
                <div className="assignment-detail-modal-content">
                    <div>과목: {lecTitle}</div>
                    <div>과제 제목:
                        
                    </div>
                    <div>과제 설명:
                        
                    </div>
                    <div>마감일:
                        
                    </div>
                    <div>배점: {maxScore}</div>
    
                    
                    <button onClick={onClose}>닫기</button>
                </div>
            </div>
        );
}

export default AssignmentDetailModal;