import {useState, useEffect} from "react";
import {UseUser} from "../../../hook/UseUser";
import '../../../css/MyPages/ApproveAttendanceModal.css';
import Pagination from "../notices/Pagination";

const ApproveAttendanceModal = ({ onClose, lecSerial }) => {
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const {user} = UseUser();
    const [studentList, setStudentList] = useState();
    const [showRejectPrompt, setShowRejectPrompt] = useState(false);
    const [rejectReason, setRejectReason] = useState('');
    const [selectedStudent, setSelectedStudent] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState(0);
    const accessToken = user.data.accessToken;

     const handlePageChange = (newPage) => {
        setPage(newPage);
    };


    // 학생 목록 불러오기
    const fetchStudentList = async (accessToken) => {
            if (!accessToken) return;
    
            setLoading(true);
            setError(null);
            try {
    
                const response = await fetch(`${BASE_URL}/lectures/${lecSerial}/students?page=${page - 1}&size=20`, {
                    method: 'POST',
                    headers: {
                        Authorization: `Bearer ${accessToken}`
                    }
                });
    
                if (!response.ok) throw new Error('학생 목록을 불러오는 데 실패했습니다.');
                const data = await response.json();
                setStudentList(data.content);
                setTotal(data.totalElements);
            } catch (error) {
                console.error('학생 목록 조회 에러:', error);
                setError(error.message || '알 수 없는 에러가 발생했습니다.');
                setStudentList([]);
            } finally {
                setLoading(false);
            }
        };
    
        useEffect(() => {
        if (accessToken) {
            fetchStudentList(accessToken);
        }
    }, [accessToken, page]);


    // 출석 승인
    const approveAttendance = async({accessToken, requestIdx, professorIdx}) => {
        try {
            const approveResponse = await fetch(`/api/professor/attendance/requests/${requestIdx}/approve`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({professorIdx})
            });
            if (!approveResponse.ok) {
                throw new Error("!approveResponse.ok");
            }
        } catch (error) {
            console.error("출석 승인 실패 : ", error);
        }
    }

    // 결석 처리
    const rejectAttendance = async ({ accessToken, requestIdx, professorIdx, rejectReason }) => {
        try {
            const rejectResponse = await fetch(`/api/professor/attendance/requests/${requestIdx}/reject`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    professorIdx,
                    rejectReason
                })
            });

            if (!rejectResponse.ok) throw new Error('출석 거부 실패');
        } catch (error) {
            console.error("출석 거부 실패 : ", error);
        }
    };

    const handleRejectClick = (student) => {
        setSelectedStudent(student);
        setShowRejectPrompt(true); // 모달 열기
    };


    const handleRejectSubmit = async () => {
        if (!rejectReason || !selectedStudent) return;

        await rejectAttendance({
            accessToken,
            requestIdx: selectedStudent.studentIdx,
            professorIdx: user.data.user.userIdx,
            rejectReason,
        });
        fetchStudentList(accessToken);

        // 상태 초기화
        setShowRejectPrompt(false);
        setRejectReason('');
        setSelectedStudent(null);
    };

    const handleApproveClick = async (student) => {
        await approveAttendance({
            accessToken,
            requestIdx: student.studentIdx,
            professorIdx: user.data.user.userIdx,
        });

        await fetchStudentList(accessToken); // 목록 다시 불러오기
    };


    return (
        <div className="approve-attendance-modal-container">
            <div className="approve-attendance-modal-content">
               <table>
                    <tbody>
                        {studentList && studentList.length > 0 ? (
                            studentList.map((student) => (
                                <tr
                                    key={student.studentIdx}
                                >
                                    <td>{student.userName}</td>
                                    <td><button onClick={() => handleApproveClick(student)}>출석</button></td>
                                    <td><button>지각</button></td>
                                    <td><button onClick={() => handleRejectClick(student)}>결석</button></td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td><p>학생 목록 없음</p></td>
                            </tr>
                        )}
                    </tbody>
                </table>

                <Pagination
                    page={page}
                    size={20}
                    total={total}
                    onChange={handlePageChange}
                />

                <button onClick={onClose}>닫기</button>
            </div>
            
            {showRejectPrompt && (
                <div className="reject-reason-modal">
                    <div className="modal-content">
                        <h3>결석 사유 입력</h3>
                        <textarea
                            value={rejectReason}
                            onChange={(e) => setRejectReason(e.target.value)}
                            placeholder="결석 사유를 입력하세요"
                        />
                        <button onClick={handleRejectSubmit}>제출</button>
                        <button onClick={() => setShowRejectPrompt(false)}>취소</button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ApproveAttendanceModal;
