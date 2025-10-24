import {useState, useEffect} from "react";
import {UseUser} from "../../../hook/UseUser";
import '../../../css/MyPages/ApproveAttendanceModal.css';
import Pagination from "../notices/Pagination";

const ApproveAttendanceModal = ({ onClose, lecSerial }) => {
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const {user} = UseUser();
    const [studentList, setStudentList] = useState([]);
    const [showRejectPrompt, setShowRejectPrompt] = useState(false);
    const [rejectReason, setRejectReason] = useState('');
    const [selectedStudent, setSelectedStudent] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState(0);
    const getToken = () => {
    return window.authToken || 
           localStorage.getItem('accessToken') || 
           sessionStorage.getItem('accessToken');
};
    const accessToken = getToken();

     const handlePageChange = (newPage) => {
        setPage(newPage);
    };


    // 학생 목록 불러오기
    const fetchStudentList = async (accessToken, lecSerial, page) => {

            if (!accessToken) return;
    
            setLoading(true);
            setError(null);
            try {
    
                const response = await fetch(`${BASE_URL}/enrollments/list`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${accessToken}`
                    },
                    body: JSON.stringify({ lecSerial, page: Math.max(page - 1, 0), size: 20 })
                });
    
                if (!response.ok) {
                const errMsg = await response.text();
                console.error("서버 에러 응답:", errMsg);
                throw new Error(errMsg || "학생 목록 조회 실패");
                }

                const data = await response.json();
                // ✅ 이름순 정렬 (가나다 / 알파벳 순 모두 대응)
                const sortedList = [...data.content].sort((a, b) =>
                    a.studentName.localeCompare(b.studentName, 'ko', { sensitivity: 'base' })
                );

                setStudentList(sortedList);
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
        if (accessToken && lecSerial) {
            fetchStudentList(accessToken, lecSerial, page);
        }
    }, [accessToken, lecSerial, page]);


    // 출석 기록 입력 받기
    const recordCount = parseInt(prompt('처리할 학생 수를 입력하세요:', '3'));
    const attendanceRecords = [];
    
    for (let i = 0; i < recordCount; i++) {
        const studentIdx = parseInt(prompt(`${i + 1}번째 학생 USER_IDX를 입력하세요:`, `${6 + i}`));
        const status = prompt(`${i + 1}번째 학생 출석 상태를 입력하세요:\n출: 출석\n지: 지각\n결: 결석`, '출');
        
        if (studentIdx && status) {
            attendanceRecords.push({
                studentIdx: studentIdx,
                status: status
            });
        }
    }

    // 출석 승인
    const approveAttendance = async ({ accessToken, lecSerial, attendanceRecords }) => {
    const requestData = {
        lecSerial: lecSerial,
        sessionNumber: 1, // 회차
        attendanceRecords: attendanceRecords
    };

    try {
        const response = await fetch(`${BASE_URL}/attendance/approve`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`
            },
            body: JSON.stringify(requestData)
        });

        const data = await response.json();

        if (response.ok) {
            console.log('✅ 출석 승인 성공!');
            console.log('응답 데이터:', data);
            console.log('메시지:', data.message);
            console.log('성공 여부:', data.success);
        } else {
            console.error('❌ 출석 승인 실패:', response.status);
            console.error('에러 메시지:', data);
        }
    } catch (error) {
        console.error('❌ 네트워크 오류:', error);
    }
};

    // 결석 처리
    // const rejectAttendance = async ({ accessToken, requestIdx, professorIdx, rejectReason }) => {
        
    // };

    // const handleRejectClick = (student) => {
    //     setSelectedStudent(student);
    //     setShowRejectPrompt(true); // 모달 열기
    // };


    // const handleRejectSubmit = async () => {
    //     if (!rejectReason || !selectedStudent) return;

    //     await rejectAttendance({
    //         accessToken,
    //         requestIdx: selectedStudent.studentIdx,
    //         professorIdx: user.data.user.userIdx,
    //         rejectReason,
    //     });
    //     fetchStudentList(accessToken, lecSerial);

    //     // 상태 초기화
    //     setShowRejectPrompt(false);
    //     setRejectReason('');
    //     setSelectedStudent(null);
    // };

    const handleApproveClick = async (student) => {
        await approveAttendance({
            accessToken,
            requestIdx: student.studentIdx,
            professorIdx: user.data.user.userIdx,
        });

        await fetchStudentList(accessToken, lecSerial); // 목록 다시 불러오기
    };


    return (
        <div className="approve-attendance-modal-container">
            <div className="approve-attendance-modal-content">
               <table className="notice-table">
                    <thead>
                        <tr>
                            <th style={{width: "20%"}}>학생 번호</th>
                            <th style={{width: "50%"}}>이름</th>
                            <th style={{width: "10%"}}></th>
                            <th style={{width: "10%"}}></th>
                            <th style={{width: "10%"}}></th>
                        </tr>
                    </thead>
                    <tbody>
                        {studentList && studentList.length > 0 ? (
                            studentList.map((student) => (
                                <tr
                                    key={student.studentIdx}
                                >
                                    <td>{student.studentIdx}</td>
                                    <td>{student.studentName}</td>
                                    <td><button className="attendanceApproveClick" onClick={() => handleApproveClick(student)}>출석</button></td>
                                    <td><button className="attendanceLateClick">지각</button></td>
                                    <td><button className="attendanceRejectClick" onClick={() => handleRejectClick(student)}>결석</button></td>
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

                <button className="approveAttendanceCloseBtn" onClick={onClose}>닫기</button>
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
