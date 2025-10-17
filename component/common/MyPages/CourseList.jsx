import { useEffect, useState } from 'react';
import { UseUser } from '../../../hook/UseUser';
import CourseDetail from './CourseDetail';

function CourseList() {
    const { user } = UseUser();
    const [courseList, setCourseList] = useState([]);
    const [selectedLecture, setSelectedLecture] = useState(null);
    const [fetchedLecture, setFetchedLecture] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

    const openModal = (lectureIdx) => {
    setSelectedLecture(lectureIdx);
    setIsModalOpen(true);
    };


    const closeModal = () => {
        setSelectedLecture(null);
        setIsModalOpen(false);
    };

    const accessToken = user?.data?.accessToken;
    console.log("user : ", user);
    console.log("accessToken : ", accessToken);

    const getCourseList = async (accessToken) => {
        try {
            const response = await fetch(`${BASE_URL}/professor/lectures`, {
                method: 'POST',
                headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json'
                },
            body: JSON.stringify({})
            });
            if (!response.ok) throw new Error('강의 목록을 불러오는 데 실패했습니다.');
            const data = await response.json();
            setCourseList(data); // ✅ 받아온 데이터 저장
        } catch (error) {
            console.error('강의 목록 조회 에러:', error);
        }
    };

    useEffect(() => {
        if (accessToken) {
            getCourseList(accessToken);
        }
    }, [accessToken]); // ✅ accessToken이 생겼을 때 호출



    const handleEdit = () => {
        if (!fetchedLecture) {
        alert("강의 상세 정보 데이터를 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
        return;
        }

    setSelectedLecture(fetchedNotice);
    setIsModalOpen(false);
    setCurrentPage("강의 수정 상세 페이지");
    };


    return (
        <>
        <div>
            <h2>강의 목록</h2>
            <ul>
                {courseList.map((course) => (
                    <li key={course.lectureIdx} onClick={() => openModal(course.lectureIdx)}>{course.lectureName}</li>
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
                        lectureIdx={selectedLecture}
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
