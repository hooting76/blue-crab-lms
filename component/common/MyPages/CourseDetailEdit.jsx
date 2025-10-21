import { useState } from "react";
import { UseUser } from "../../../hook/UseUser";
import CourseList from "./CourseList";

function CourseDetailEdit({ lecture, currentPage, setCurrentPage }) {
    if (!lecture) return <div>강의 정보를 불러오는 중입니다...</div>; // ✅ null-safe 처리

    const [lecTit, setLecTit] = useState(lecture.lecTit);
    const [lecSummary, setLecSummary] = useState(lecture.lecSummary);
    const [lecMany, setLecMany] = useState(lecture.lecMany);
    const [lecPoint, setLecPoint] = useState(lecture.lecPoint);
    const [lecTime, setLecTime] = useState(lecture.lecTime);
    const [lecMin, setLecMin] = useState(lecture.lecMin);

    const { user } = UseUser();
    const accessToken = user?.data?.accessToken;
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

    const submitCourseEdit = async () => {
        const requestBody = {
            lecSerial: lecture.lecSerial,
            lecTit,
            lecMany,
            lecTime,
            lecPoint,
            lecMin,
            lecSummary
        };

        try {
            const response = await fetch(`${BASE_URL}/lectures/update`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify(requestBody),
            });

            if (!response.ok) throw new Error('서버 에러가 발생했습니다.');
            alert('강의가 성공적으로 수정되었습니다!');
            setCurrentPage("강의 수정");
        } catch (error) {
            alert(error.message);
        }
    };

    // 수정 완료 후 돌아가기
    if (currentPage === "강의 수정") {
        return <CourseList currentPage={currentPage} setCurrentPage={setCurrentPage} />;
    }

    return (
        <>
            <div>
                <label>강의 제목</label><br />
                <input type="text" value={lecTit} onChange={(e) => setLecTit(e.target.value)} />
            </div>

            <div>
                <label>강의 개요</label><br />
                <input type="text" value={lecSummary} onChange={(e) => setLecSummary(e.target.value)} />
            </div>

            <div>
                <label>최대 수강 인원</label><br />
                <input type="number" value={lecMany} onChange={(e) => setLecMany(Number(e.target.value))} />
            </div>

            <div>
                <label>강의 시간</label><br />
                <input type="text" value={lecTime} onChange={(e) => setLecTime(e.target.value)} />
            </div>

            <div>
                <label>학점</label><br />
                <input type="number" value={lecPoint} onChange={(e) => setLecPoint(Number(e.target.value))} />
            </div>

            <div>
                <label>수강 최저 학년</label><br />
                <input type="number" value={lecMin} onChange={(e) => setLecMin(Number(e.target.value))} />
            </div>

            <div>
                <button className="courseEditBtn" onClick={submitCourseEdit}>강의 수정</button>
            </div>
        </>
    );
}

export default CourseDetailEdit;
