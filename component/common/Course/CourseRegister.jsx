import {useState} from 'react';

function CourseRegister() {

    const [lectureName, setLectureName] = useState("");
    const [lectureCode, setLectureCode] = useState("");
    const [lectureDescription, setLectureDescription] = useState("");
    const [maxStudents, setMaxStudents] = useState(0);
    const [credit, setCredit] = useState(0);

    return (
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

            
        </form>
    )
}

export default CourseRegister;