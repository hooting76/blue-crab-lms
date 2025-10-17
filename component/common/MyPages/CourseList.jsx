import { useEffect, useState } from 'react';
import { UseUser } from '../../../hook/UseUser';
import CourseDetail from './CourseDetail';
import CourseDetailEdit from './CourseDetailEdit';

function CourseList({ currentPage, setCurrentPage }) {

    const { user } = UseUser();
    const [lectureList, setLectureList] = useState([]);
    const [selectedLecture, setSelectedLecture] = useState(null);
    const [fetchedLecture, setFetchedLecture] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

    const openModal = (lecture) => {
    setSelectedLecture(lecture);
    setIsModalOpen(true);
    };


    const closeModal = () => {
        setSelectedLecture(null);
        setIsModalOpen(false);
    };

    const accessToken = user?.data?.accessToken;
    console.log("user : ", user);
    console.log("accessToken : ", accessToken);


            // select 변경 핸들러
            const handleSemesterChange = (e) => {
                setSelectedSemester(e.target.value);
            };
        
        const today = new Date();
        let currentYear = today.getFullYear();
        const currentMonth = today.getMonth() + 1;
        
        let currentSemester;
        
        if (currentMonth >= 3 && currentMonth <= 8) {
            currentSemester = 1;
        } else if (currentMonth >= 9) {
            currentSemester = 2;
        } else {
            // 1~2월은 전년도 2학기
            currentYear -= 1;
            currentSemester = 2;
        }
        
        
        // 현재 학기를 기준으로 지난 8개 학기 생성
        const generateSemesters = (count = 8) => {
            const semesters = [];
            let year = currentYear;
            let semester = currentSemester;
        
            for (let i = 0; i < count; i++) {
                const value = `${year}_${semester}`;
                const label = `${year}년 ${semester}학기`;
                semesters.push({ value, label });
        
                // 이전 학기로 이동
                if (semester === 1) {
                    semester = 2;
                    year -= 1;
                } else {
                    semester = 1;
                }
            }
        
            return semesters;
        };
        
        const semesterOptions = generateSemesters(8);
        const currentSemesterValue = `${currentYear}_${currentSemester}`; // 현재 학기 value
        const [selectedSemester, setSelectedSemester] = useState(currentSemesterValue); // 학기 선택 상태


    const fetchLectureList = async (accessToken, selectedSemester) => {
        try {
            const [year, semester] = selectedSemester.split('_');
    
            const response = await fetch(`${BASE_URL}/lectures`, {
                method: "POST",
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({page: 0, size: 20, year: parseInt(year), semester: parseInt(semester)})
            });
        if (!response.ok) throw new Error('강의 목록을 불러오는 데 실패했습니다.');
                const data = await response.json();
                setLectureList(data); // ✅ 받아온 데이터 저장
            } catch (error) {
                console.error('강의 목록 조회 에러:', error);
            }
        };
    
        useEffect(() => {
                fetchLectureList(accessToken, selectedSemester);
            }, [accessToken, selectedSemester]); // ✅ accessToken이 생겼을 때, 학기가 선택되었을 때 호출
    
            console.log("lectureList : ", lectureList);



    const handleEdit = () => {
        if (!fetchedLecture) {
        alert("강의 상세 정보 데이터를 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
        return;
        }

    setSelectedLecture(fetchedLecture);
    setIsModalOpen(false);
    setCurrentPage("강의 수정 상세 페이지");
    };

    if (currentPage === "강의 수정 상세 페이지") {
    return (
        <CourseDetailEdit
            lecture={selectedLecture}
            currentPage={currentPage}
            setCurrentPage={setCurrentPage}
        />
    );
}



    return (
        <>
            <select value={selectedSemester} onChange={handleSemesterChange} className='selectSemester'>
                {semesterOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                        {option.label}
                    </option>
                ))}
            </select>

            <div>
                <h2>강의 목록</h2>
                <ul>
                    {lectureList.content.map((course) => (
                        <li key={course.lectureIdx} onClick={() => openModal(course)}>{course.lectureName}</li>
                    ))}
                </ul>
            </div>

            {isModalOpen && (
                <div className="modal-overlay" onClick={closeModal}>
                    <div
                        className="modal-content"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <button className="modal-close" onClick={closeModal}>
                            ✖
                        </button>
                        <CourseDetail
                        lecture={selectedLecture}
                        currentPage={currentPage}
                        setCurrentPage={setCurrentPage}
                        onFetchComplete={(lecture) => {
                            setFetchedLecture(lecture);        // 업데이트된 강의 상세 정보
                            setSelectedLecture(lecture);
                        }}
                        />

                            <button
                            className="courseEditButton"
                            onClick={handleEdit}
                            >
                            강의 수정
                            </button>
                    </div>
                </div>
            )}
        </>
    );
}

export default CourseList;
