import '../../../css/MyPages/ApproveAttendanceModal.css';

const ApproveAttendanceModal = ({ onClose }) => {
    return (
        <div className="approve-attendance-modal-container">
            <div className="approve-attendance-modal-content">
                출석 인정 요청
                <p>출석 인정 신청 내역을 확인하고 승인해주세요.</p>
                {/* 필요한 내용 추가 */}
                <button onClick={onClose}>닫기</button>
            </div>
        </div>
    );
};

export default ApproveAttendanceModal;
