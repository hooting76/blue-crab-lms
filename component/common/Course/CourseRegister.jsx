import {useState} from 'react';
import { UseAdmin } from '../../../hook/UseAdmin';
import "../../../css/Course/CourseRegister.css";

function CourseRegister() {

    const [lectureName, setLectureName] = useState("");
    const [lectureCode, setLectureCode] = useState("");
    const [lectureDescription, setLectureDescription] = useState("");
    const [maxStudents, setMaxStudents] = useState(0);
    const [credit, setCredit] = useState(0);
    const [professorIdx, setProfessorIdx] = useState(0);
    const [year, setYear] = useState(null);
    const [semester, setSemester] = useState("");
    const [majorType, setMajorType] = useState("");
    const [requiredType, setRequiredType] = useState("");
    const [minGrade, setMinGrade] = useState("");

    const {admin} = UseAdmin();
    const accessToken = admin.accessToken;

    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

    const submitCourseRegister = async (e) => {
         e.preventDefault();

        const CourseRegisterDetails = {
            lectureName,
            lectureCode,
            lectureDescription,
            maxStudents,
            credit,
            professorIdx,
            year,
            semester,
            majorType,
            requiredType,
            minGrade
        }

            if  (lectureName === "" || lectureCode === "" || lectureDescription === "" || maxStudents === 0 ||
                 credit === 0 || professorIdx === 0 || year === null || semester === "" || 
                 majorType === "" || requiredType === "" || minGrade === "")
                 alert("모든 칸을 작성해주세요");
             else {
                try {const response = await fetch(`${BASE_URL}/lectures`, {
                method: 'POST',
                headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify(CourseRegisterDetails),
            });

        if (!response.ok) throw new Error('서버 에러가 발생했습니다.');
        alert('강의가 성공적으로 등록되었습니다!');
        } catch (error) {
        alert(error.message);
        }
    }
}

    return (
        <>
            <form>
                <div className='lectureNameCode'>
                    <span>
                        <label>강의 제목</label><br/>
                        <input
                        type="text"
                        value={lectureName}
                        onChange={(e) => setLectureName(e.target.value)}
                        required
                        style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                        />
                    </span>
                
                    <span>
                        <label>강의 코드</label><br/>
                        <input
                        type="text"
                        value={lectureCode}
                        onChange={(e) => setLectureCode(e.target.value)}
                        required
                        style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                        />
                    </span>
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

                <div className="maxStudentsCredit">
                    <span>
                        <label>수강 인원</label><br/>
                        <input
                        type="number"
                        value={maxStudents}
                        onChange={(e) => setMaxStudents(Number(e.target.value))}
                        required
                        style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                        />
                    </span>
                    
                    <span>
                        <label>학점</label><br/>
                        <input
                        type="number"
                        value={credit}
                        onChange={(e) => setCredit(Number(e.target.value))}
                        required
                        style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                        />
                    </span>
                </div>

                <div>
                    <label>담당 교수 Idx</label><br/>
                    <input
                    type="number"
                    value={professorIdx}
                    onChange={(e) => setProfessorIdx(Number(e.target.value))}
                    required
                    style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                    />
                </div>

                <div className='lectureYearSemester'>
                    <span>
                        <label>연도</label><br/>
                        <input
                        type="number"
                        value={year}
                        onChange={(e) => setYear(Number(e.target.value))}
                        required
                        style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                        />
                    </span>

                    <span>
                        <label>학기</label><br/>
                        <select
                        value={semester}
                        onChange={(e) => setSemester(Number(e.target.value))}
                        required
                        style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                        >
                            <option value="">학기를 선택하세요</option>
                            <option value={1}>1학기</option>
                            <option value={2}>2학기</option>
                        </select>
                    </span>
                </div>

                <div className='majorRequiredType'>
                    <span>
                        <label>전공 여부</label><br/>
                        <select
                        value={majorType}
                        onChange={(e) => setMajorType(Number(e.target.value))}
                        required
                        style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                        >
                            <option value="">전공 여부를 선택하세요</option>
                            <option value={1}>전공</option>
                            <option value={0}>교양</option>
                        </select>
                    </span>

                    <span>
                        <label>필수 여부</label><br/>
                        <select
                        value={requiredType}
                        onChange={(e) => setRequiredType(Number(e.target.value))}
                        required
                        style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                        >
                            <option value="">필수 여부를 선택하세요</option>
                            <option value={1}>필수</option>
                            <option value={0}>선택</option>
                        </select>
                    </span>
                </div>

                <div>
                    <label>수강 최소 학년</label><br/>
                    <select
                    value={minGrade}
                    onChange={(e) => setMinGrade(Number(e.target.value))}
                    required
                    style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
                    >
                        <option value="">수강에 필요한 최소 학년을 선택하세요</option>
                        <option value={1}>1학년</option>
                        <option value={2}>2학년</option>
                        <option value={3}>3학년</option>
                        <option value={4}>4학년</option>
                    </select>
                </div>
            </form>

            
            <div className='courseRegisterBtn'>
                <button onClick={submitCourseRegister}>강의 등록</button>
            </div>
        </>
    )
}

export default CourseRegister;