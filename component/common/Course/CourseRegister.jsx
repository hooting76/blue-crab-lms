import {useState} from 'react';
import { UseAdmin } from '../../../hook/UseAdmin';

function CourseRegister() {

    const [lectureName, setLectureName] = useState("");
    const [lectureCode, setLectureCode] = useState("");
    const [lectureDescription, setLectureDescription] = useState("");
    const [maxStudents, setMaxStudents] = useState(0);
    const [credit, setCredit] = useState(0);
    const [professorIdx, setProfessorIdx] = useState(0);

    const {admin} = UseAdmin();
    const accessToken = admin.data.accessToken;

    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

    const submitCourseRegister = async (e) => {
        const CourseRegisterDetails = {
            lectureName,
            lectureCode,
            lectureDescription,
            maxStudents,
            credit
        }

        try {
            const response = await fetch(`${BASE_URL}/lectures`, {
                method: 'POST',
                headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify(CourseRegisterDetails),
            });

        if (!response.ok) throw new Error('서버 에러가 발생했습니다.');
        } catch (error) {
        alert(error.message);
        }
    }

    return (
        <>
            <form>
                <div>
                    <label>강의 제목</label><br/>
                    <input
                    type="text"
                    value={lectureName}
                    onChange={(e) => setLectureName(e.target.value)}
                    required
                    style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                    />
                </div>

                <div>
                    <label>강의 코드</label><br/>
                    <input
                    type="text"
                    value={lectureCode}
                    onChange={(e) => setLectureCode(e.target.value)}
                    required
                    style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                    />
                </div>

                <div>
                    <label>강의 설명</label><br/>
                    <input
                    type="text"
                    value={lectureDescription}
                    onChange={(e) => setLectureDescription(e.target.value)}
                    required
                    style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                    />
                </div>

                <div>
                    <label>수강 인원</label><br/>
                    <input
                    type="number"
                    value={maxStudents}
                    onChange={(e) => setMaxStudents(e.target.value)}
                    required
                    style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                    />
                </div>

                <div>
                    <label>학점</label><br/>
                    <input
                    type="number"
                    value={credit}
                    onChange={(e) => setCredit(e.target.value)}
                    required
                    style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                    />
                </div>

                <div>
                    <label>담당 교수</label><br/>
                    <input
                    type="number"
                    value={professorIdx}
                    onChange={(e) => setProfessorIdx(e.target.value)}
                    required
                    style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                    />
                </div>
            </form>

            
            <div className='courseRegisterBtn'>
                <button onClick={submitCourseRegister}>강의 등록</button>
            </div>
        </>
    )
}

export default CourseRegister;