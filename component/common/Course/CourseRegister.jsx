import {useState} from 'react';
import { UseAdmin } from '../../../hook/UseAdmin';
import "../../../css/Course/CourseRegister.css";

function CourseRegister() {
    const [lecSerial, setLecSerial] = useState();
    const [lecTit, setLecTit] = useState();
    const [lecSummary, setLecSummary] = useState();
    const [lecMany, setLecMany] = useState();
    const [lecPoint, setLecPoint] = useState();
    const [lecTime, setLecTime] = useState();
    const [lecProf, setLecProf] = useState();
    const [lecMcode, setLecMcode] = useState();
    const [lecMcodeDep, setLecMcodeDep] = useState();
    const [lecYear, setLecYear] = useState();
    const [lecSemester, setLecSemester] = useState();
    const [lecMajor, setLecMajor] = useState();
    const [lecMust, setLecMust] = useState();
    const [lecMin, setLecMin] = useState();
    const [lecOpen, setLecOpen] = useState();

    const {admin} = UseAdmin();
    const accessToken = admin.accessToken;

    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';


    const submitCourseRegister = async (e) => {
         e.preventDefault();

        const date = new Date().toLocaleString("sv-SE", {
        timeZone: "Asia/Seoul",
        hour12: false,
        });
        const lecReg = date.slice(0, 16);

        const CourseRegisterDetails = {
            lecSerial,
            lecTit,
            lecSummary,
            lecMany,
            lecPoint,
            lecTime,
            lecProf,
            lecMcode : String(`0${lecMcode}`),
            lecMcodeDep: String(`0${lecMcodeDep}`),
            lecYear,
            lecSemester,
            lecMajor,
            lecMust,
            lecMin,
            lecOpen,
            lecReg
        }
        console.log(("CourseRegisterDetails :"), CourseRegisterDetails);
        
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


    return (
        <>
            <form>
                <div className='lecSerialTit'>
                    <span>
                        <label>강의 코드</label><br/>
                        <input
                        type="text"
                        value={lecSerial}
                        onChange={(e) => setLecSerial(e.target.value)}
                        required
                        />
                    </span>

                    <span>
                        <label>강의 제목</label><br/>
                        <input
                        type="text"
                        value={lecTit}
                        onChange={(e) => setLecTit(e.target.value)}
                        required
                        />
                    </span>
                </div>


                    <label>강의 개요</label><br/>
                    <input
                    type="text"
                    value={lecSummary}
                    onChange={(e) => setLecSummary(e.target.value)}
                    />
                
                <div className='lecManyPointTimeProfMcode'>
                    <span>
                        <label>최대 수강 인원</label><br/>
                        <input
                        type="number"
                        value={lecMany}
                        onChange={(e) => setLecMany(Number(e.target.value))}
                        required
                        />
                    </span>

                    <span>
                        <label>학점</label><br/>
                        <input
                        type="number"
                        value={lecPoint}
                        onChange={(e) => setLecPoint(Number(e.target.value))}
                        required
                        />
                    </span>

                    <span>
                        <label>강의 시간</label><br/>
                        <input
                        type="text"
                        value={lecTime}
                        onChange={(e) => setLecTime(e.target.value)}
                        required
                        />
                    </span>
                
                    <span>
                        <label>담당 교수</label><br/>
                        <input
                        type="text"
                        value={lecProf}
                        onChange={(e) => setLecProf(e.target.value)}
                        required
                        />
                    </span>

                    <span>
                        <label>학부</label><br/>
                        <select
                        value={lecMcode}
                        onChange={(e) => {setLecMcode(Number(e.target.value)); setLecMcodeDep("");}}
                        required
                        >
                            <option value="">학부</option>
                            <option value={1}>해양학부</option>
                            <option value={2}>보건학부</option>
                            <option value={3}>자연과학부</option>
                            <option value={4}>인문학부</option>
                            <option value={5}>공학부</option>
                        </select>
                    </span>

                    <span>
                        <label>학과</label><br/>
                        <select
                        value={lecMcodeDep}
                        onChange={(e) => setLecMcodeDep(Number(e.target.value))}
                        required
                        >
                            {lecMcode === 1 &&
                            <>
                            <option value="">학과</option>
                            <option value={1}>항해학과</option>
                            <option value={2}>해양경찰</option>
                            <option value={3}>해군사관</option>
                            <option value={4}>도선학과</option>
                            <option value={5}>해양수산학</option>
                            <option value={6}>조선학과</option>
                            </>}

                            {lecMcode === 2 &&
                            <>
                            <option value="">학과</option>
                            <option value={1}>간호학</option>
                            <option value={2}>치위생</option>
                            <option value={3}>약학과</option>
                            <option value={4}>보건정책학</option>
                            </>}

                            {lecMcode === 3 &&
                            <>
                            <option value="">학과</option>
                            <option value={1}>물리학</option>
                            <option value={2}>수학</option>
                            <option value={3}>분자화학</option>
                            </>}

                            {lecMcode === 4 &&
                            <>
                            <option value="">학과</option>
                            <option value={1}>철학</option>
                            <option value={2}>국어국문</option>
                            <option value={3}>역사학</option>
                            <option value={4}>경영</option>
                            <option value={5}>경제</option>
                            <option value={6}>정치외교</option>
                            <option value={7}>영어영문</option>
                            </>}

                            {lecMcode === 5 &&
                            <>
                            <option value="">학과</option>
                            <option value={1}>컴퓨터공학</option>
                            <option value={2}>기계공학</option>
                            <option value={3}>전자공학</option>
                            <option value={4}>ICT융합</option>
                            </>}
                        </select>
                    </span>
                </div>

                <div className='lecYearSemesterMajorMustMinOpen'>
                    <span>
                        <label>대상 학년</label><br/>
                        <select
                        value={lecYear}
                        onChange={(e) => setLecYear(Number(e.target.value))}
                        >
                            <option value="">학년</option>
                            <option value={1}>1학년</option>
                            <option value={2}>2학년</option>
                            <option value={3}>3학년</option>
                            <option value={4}>4학년</option>
                        </select>
                    </span>

                    <span>
                        <label>학기</label><br/>
                        <select
                        value={lecSemester}
                        onChange={(e) => setLecSemester(Number(e.target.value))}
                        >
                            <option value="">학기</option>
                            <option value={1}>1학기</option>
                            <option value={2}>2학기</option>
                        </select>
                    </span>

                    <span>
                        <label>전공 여부</label><br/>
                        <select
                        value={lecMajor}
                        onChange={(e) => setLecMajor(Number(e.target.value))}
                        required
                        >
                            <option value="">전공 여부</option>
                            <option value={1}>전공</option>
                            <option value={0}>교양</option>
                        </select>
                    </span>

                    <span>
                        <label>필수 여부</label><br/>
                        <select
                        value={lecMust}
                        onChange={(e) => setLecMust(Number(e.target.value))}
                        required
                        >
                            <option value="">필수 여부</option>
                            <option value={1}>필수</option>
                            <option value={0}>선택</option>
                        </select>
                    </span>
                
                    <span>
                        <label>수강 가능 최저 학년</label><br/>
                        <select
                        value={lecMin}
                        onChange={(e) => setLecMin(Number(e.target.value))}
                        required
                        >
                            <option value={0}>제한 없음</option>
                            <option value={1}>2학년 이상</option>
                            <option value={2}>3학년 이상</option>
                            <option value={3}>4학년 이상</option>
                        </select>
                    </span>

                    <span>
                        <label>수강신청 가능 여부</label><br/>
                        <select
                        value={lecOpen}
                        onChange={(e) => setLecOpen(Number(e.target.value))}
                        required
                        >
                            <option value="">수강신청 가능 여부</option>
                            <option value={1}>열림</option>
                            <option value={0}>닫힘</option>
                        </select>
                    </span>
                </div>

            </form>
            
            <div>
                <button className='courseRegisterBtn' onClick={submitCourseRegister}>강의 등록</button>
            </div>
        </>
    )
}

export default CourseRegister;