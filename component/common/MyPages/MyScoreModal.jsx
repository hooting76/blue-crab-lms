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

console.log("scoreDetail : ", scoreDetail);

    return (
        <div className="myScore-modal-container">
            <div className="myScore-modal-content">
                {scoreDetail ? (
                    <>
                        <div>과목 : {lecTitle}</div>
                        <div>출석<br/>
                            <span>출석률 : {scoreDetail.attendanceScore.percentage}%</span>
                            <span>출석 점수 : {scoreDetail.attendanceScore.currentScore}</span>
                            <span>출석 만점 : {scoreDetail.attendanceScore.maxScore}</span>
                        </div>
                        <div>과제<br/>
                            <table className="notice-table">
                                <thead>
                                    <tr>
                                        <th style={{ width: "40%" }}>제목</th>
                                        <th style={{ width: "20%" }}>제출여부</th>
                                        <th style={{ width: "10%" }}>점수</th>
                                        <th style={{ width: "10%" }}>만점</th>
                                        <th style={{ width: "20%" }}>백분율</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {scoreDetail.assignments.length > 0 ? (
                                        scoreDetail.assignments.map((assign) => {
                                            return (
                                                <tr key={assign.name}>
                                                    <td>{assign.name}</td>
                                                    <td>{assign.submitted ? "제출" : "미제출"}</td>
                                                    <td>{assign.score}</td>
                                                    <td>{assign.maxScore}</td>
                                                    <td>{assign.percentage}</td>
                                                </tr>
                                            );
                                        })
                                    ) : (
                                        <tr>
                                            <td colSpan="4">과제 목록이 없습니다.</td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                        <div>총점<br/>
                            <span>점수 : {scoreDetail.total.score}</span>
                            <span>만점 : {scoreDetail.total.maxScore}</span>
                            <span>점수 백분율 : {scoreDetail.total.percentage}</span>
                        </div>
                        <div>등급 : {scoreDetail.total.letterGrade}</div>
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