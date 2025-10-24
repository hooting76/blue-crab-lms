const AssignmentDetailModal = ({onClose}) => {
    const maxScore = 10;


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