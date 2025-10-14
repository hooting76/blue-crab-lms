import "../../../css/MyPages/AssignmentModal.css";

const AssignmentModal = ({ onClose }) => {
    return (
        <div className="assignment-modal-container">
            <div className="assignment-modal-content">
                과제 관리
                {/* 필요한 내용 추가 */}
                <button onClick={onClose}>닫기</button>
            </div>
        </div>
    );
};

export default AssignmentModal;