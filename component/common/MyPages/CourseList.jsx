import { useEffect, useState } from 'react';
import { UseUser } from '../../../hook/UseUser';
import CourseDetail from './CourseDetail';
import CourseDetailEdit from './CourseDetailEdit';

function CourseList({ currentPage, setCurrentPage }) {
    const { user } = UseUser();
    const [lectureList, setLectureList] = useState([]);
    const [selectedLecture, setSelectedLecture] = useState(null);
    const [detailedLecture, setDetailedLecture] = useState(null); // 상세 데이터 별도 관리
    const [isModalOpen, setIsModalOpen] = useState(false);

    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const accessToken = user?.data?.accessToken;

    const fetchLectureData = async (page = 0) => {
        try {
            const requestBody = {
                page,
                size: 20,
                professor: String(user.data.user.id),
            };

            const response = await fetch(`${BASE_URL}/lectures`, {
                method: 'POST',
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestBody),
            });

            if (!response.ok) throw new Error('강의 목록을 불러오는 데 실패했습니다.');
            const data = await response.json();
            setLectureList(data);
        } catch (error) {
            console.error('강의 목록 조회 에러:', error);
        }
    };

    useEffect(() => {
        if (accessToken) {
            fetchLectureData(currentPage - 1 || 0);
        }
    }, [accessToken, currentPage]);

    const openModal = (lecture) => {
        setSelectedLecture(lecture);
        setDetailedLecture(null);  // 상세 데이터 초기화
        setIsModalOpen(true);
    };

    const closeModal = () => {
        setSelectedLecture(null);
        setDetailedLecture(null);
        setIsModalOpen(false);
    };

    const handleEdit = () => {
        if (!detailedLecture) {
            alert("강의 상세 정보를 불러오는 중입니다.");
            return;
        }
        setIsModalOpen(false);
        setTimeout(() => {
            setCurrentPage("강의 수정 상세 페이지");
        }, 100);
    };

    // 강의 수정 상세 페이지 렌더링
    if (currentPage === "강의 수정 상세 페이지") {
        if (!detailedLecture) {
            return <div>강의 상세 정보를 불러오는 중입니다...</div>;
        }

        return (
            <CourseDetailEdit
                lecture={detailedLecture}
                currentPage={currentPage}
                setCurrentPage={setCurrentPage}
            />
        );
    }

    return (
        <>
            <h2>강의 목록</h2>

            <table>
                <tbody>
                    {lectureList.length > 0 ? (
                        lectureList.map((lecture) => (
                            <tr key={lecture.lecIdx} onClick={() => openModal(lecture)}>
                                <td>{lecture.lecTit}</td>
                            </tr>
                        ))
                    ) : (
                        <tr><td>강의 목록 없음</td></tr>
                    )}
                </tbody>
            </table>

            {isModalOpen && (
                <div className="modal-overlay" onClick={closeModal}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <button className="modal-close" onClick={closeModal}>✖</button>

                        <CourseDetail
                            lecture={selectedLecture}
                            onFetchComplete={(data) => {
                                setDetailedLecture(data);
                            }}
                        />

                        <button
                            className="courseEditButton"
                            onClick={handleEdit}
                            disabled={!detailedLecture}
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
