import {useState, useEffect} from "react";
import {UseUser} from "../../../hook/UseUser";
import CourseList from "./CourseList";

function CourseDetailEdit({lecture, currentPage, setCurrentPage}) {
    const [lecTit, setLecTit] = useState();
    const [lecSummary, setLecSummary] = useState();
    const [lecMany, setLecMany] = useState();
    const [lecOpen, setLecOpen] = useState();

    const {user} = UseUser();
    const accessToken = user.data.accessToken;

    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';


      useEffect(() => {
        if (lecture?.lecTit) {
          setLecTit(lecture.lecTit);
        }
        if (lecture?.lecSummary) {
          setLecSummary(lecture.lecSummary);
        }
        if (typeof lecture?.lecMany === 'number') {
          setLecMany(lecture.lecMany);
        }
        if (lecture?.lecOpen) {
          setLecOpen(lecture.lecOpen);
        }
      }, [lecture]);


    const submitCourseEdit = async(e) => {
        const EditedCourseRegisterDetails = {
                lecTit,
                lecSummary,
                lecMany,
                lecOpen
            }
            console.log(("EditedCourseRegisterDetails :"), EditedCourseRegisterDetails);

            try {const response = await fetch(`${BASE_URL}/lectures/${lecture.lectureIdx}`, {
                method: 'POST',
                headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify(EditedCourseRegisterDetails),
            });

        if (!response.ok) throw new Error('서버 에러가 발생했습니다.');
        alert('강의가 성공적으로 수정되었습니다!');
        setLecTit('');
        setLecSummary('');
        setLecMany(null);
        setLecOpen('');
        setCurrentPage("강의 수정");
        } catch (error) {
        alert(error.message);
        }
    }

    if (currentPage === "강의 수정")
        return <CourseList currentPage={currentPage} setCurrentPage={setCurrentPage} />;


    return (
        <>
            <div>
                <label>강의 제목</label><br />
                <input
                type="text"
                value={lecTit}
                onChange={(e) => setLecTit(e.target.value)}
                required
                style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                />
            </div>

            <div>
                <label>강의 개요</label><br />
                <input
                type="text"
                value={lecSummary}
                onChange={(e) => setLecSummary(e.target.value)}
                required
                style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                />
            </div>

            <div>
                <label>최대 수강 인원</label><br />
                <input
                type="number"
                value={lecMany}
                onChange={(e) => setLecMany(Number(e.target.value))}
                required
                style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                />
            </div>

            <div>
                <label>강의 열림 여부</label><br />
                <select
                value={lecOpen}
                onChange={(e) => setLecOpen(Number(e.target.value))}
                required
                style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                >
                <option value="">강의 열림 여부</option>
                <option value={1}>열림</option>
                <option value={0}>닫힘</option>
                </select>
            </div>

            <div>
                <button className='courseEditBtn' onClick={submitCourseEdit}>강의 수정</button>
            </div>
        </>
    )
}

export default CourseDetailEdit;