import { useState } from "react";
import {UseUser} from "../../../hook/UseUser";
import "../../../css/MyPages/GradeConfigModal.css";

const GradeConfigModal = ({onClose, lecSerial}) => {
    const {user} = UseUser();
    const accessToken = user.data.accessToken;
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

    const [attendanceMaxScore, setAttendanceMaxScore] = useState(20);
    const [assignmentTotalScore, setAssignmentTotalScore] = useState(50);
    const [latePenaltyPerSession, setLatePenaltyPerSession] = useState(0);
    const [distributionA, setDistributionA] = useState(30);
    const [distributionB, setDistributionB] = useState(40);
    const [distributionC, setDistributionC] = useState(20);
    const [distributionD, setDistributionD] = useState(10);

    const submitGradeConfig = async() => {
        const requestBody ={
            action: "set-config",
            lecSerial: lecSerial,
            attendanceMaxScore: attendanceMaxScore,
            assignmentTotalScore: assignmentTotalScore,
            latePenaltyPerSession: latePenaltyPerSession,
            gradeDistribution : {
                A: distributionA,
                B: distributionB,
                C: distributionC,
                D: distributionD
            }
        };

        try {
            const response = await fetch(`${BASE_URL}/enrollments/grade-config`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) throw new Error('서버 에러가 발생했습니다.');
            alert('성적 구성이 저장되었습니다.');
        } catch (error) {
            alert(error.message);
        }
    }

    return (
        <div className="gradeConfig-modal-container">
            <div className="gradeConfig-modal-content">
                <div>과목: {lecSerial}</div>
                <div>출석 배점 :
                    <input type="number"
                     value={attendanceMaxScore}
                     onChange={(e) => setAttendanceMaxScore(e.target.value)}
                     required/>
                </div>
                <div>과제 총점 :
                    <input type="number"
                     value={assignmentTotalScore}
                     onChange={(e) => setAssignmentTotalScore(e.target.value)}
                     required/>
                </div>
                <div>지각당 감점 :
                    <input type="number"
                     value={latePenaltyPerSession}
                     onChange={(e) => setLatePenaltyPerSession(e.target.value)}
                     required/>
                </div>
                <div>등급 분포 비율 :<br/>
                        A : 
                    <input type="number"
                     value={distributionA}
                     onChange={(e) => setDistributionA(e.target.value)}
                     required/>%<br/>
                        B :
                     <input type="number"
                     value={distributionB}
                     onChange={(e) => setDistributionB(e.target.value)}
                     required/>%<br/>
                        C :
                     <input type="number"
                     value={distributionC}
                     onChange={(e) => setDistributionC(e.target.value)}
                     required/>%<br/>
                        D :
                     <input type="number"
                     value={distributionD}
                     onChange={(e) => setDistributionD(e.target.value)}
                     required/>%
                </div>

                <button className="gradeConfigSubmitBtn" onClick={submitGradeConfig}>성적 구성 저장</button>
                    <br/>
                <button className="gradeConfigModalCloseBtn" onClick={onClose}>닫기</button>
            </div>
        </div>
    )
};

export default GradeConfigModal;