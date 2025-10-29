import { useState, useEffect } from "react";
import {UseUser} from "../../../hook/UseUser";
import "../../../css/MyPages/MyScoreModal.css";

function MyScoreModal({onClose, lecSerial, lecTitle}) {
    const {user} = UseUser();
    const accessToken = user.data.accessToken;
    const studentIdx = user.data.user.id;
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

    const [scoreDetail, setScoreDetail] = useState();

    const fetchMyScore = async() => {
        try {
            const response = await fetch(`${BASE_URL}/enrollments/grade-info`, {
                method: 'POST',
                headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json',
                },
                body: JSON.stringify({ action: "get-grade", lecSerial: lecSerial, studentIdx: studentIdx })
            })
        if (!response.ok) throw new Error('성적 상세 정보를 불러오는데 실패했습니다.');
                const data = await response.json();
                setScoreDetail(data.data.grade);
            } catch (err) {
                setError(err.message);
                setScoreDetail(null);
            }
        };

    useEffect(() => {
        fetchMyScore();
    }, []);

console.log("data : ", data);

    return (
        <div className="myScore-modal-container">
            <div className="myScore-modal-content">
                {scoreDetail ? (
                    <>
                        <div>과목 : {lecTitle}</div>
                        <div>출석
                            <span>출석률 : {scoreDetail.attendanceScore.percentage}%</span>
                            <span>출석 점수 : {scoreDetail.attendanceScore.currentScore}</span>
                            <span>출석 만점 : {scoreDetail.attendanceScore.maxScore}</span>
                        </div>
                        {/* <div>과제
                            <span>과제 총점 : {scoreDetail.assignments.score}</span>
                            <span>과제 만점 : {scoreDetail.assignments.maxScore}</span>
                            <span>과제 점수 백분율 : {scoreDetail.assignments.percentage}</span>
                        </div> */}
                        <div>총점
                            <span>점수 : {scoreDetail.total.score}</span>
                            <span>만점 : {scoreDetail.total.maxScore}</span>
                            <span>순위 백분율 : {scoreDetail.total.percentage}</span>
                        </div>
                    </>
                ) : (
                    <div>로딩 중...</div>
                )}
                <br/>
                <button className="myScoreModalCloseBtn" onClick={onClose}>닫기</button>
            </div>
        </div>
    )
};

export default MyScoreModal;