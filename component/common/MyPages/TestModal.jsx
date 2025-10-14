const TestModal = ({ onClose }) => {
    return (
        <div className="test-modal-container">
            <div className="test-modal-content">
                <h2>시험 관리</h2>
                {/* 필요한 내용 추가 */}
                <button onClick={onClose}>닫기</button>
            </div>
        </div>
    );
};

export default TestModal;