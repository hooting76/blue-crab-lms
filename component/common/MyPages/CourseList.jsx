import { useEffect, useState } from 'react';
import { UseUser } from '../../../hook/UseUser';
import CourseDetail from './CourseDetail';

function CourseList({ currentPage, setCurrentPage }) {
    const { user } = UseUser();
    const [currentPage, setCurrentPage] = useState("강의 수정");
    const [lectureList, setLectureList] = useState([]);
    const [selectedLecture, setSelectedLecture] = useState(null);s
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const accessToken = user?.data?.accessToken;

    const fetchLectureData = async (user, accessToken) => {
        if (!accessToken) return;

        setLoading(true);
        setError(null);
        try {
            const requestBody = {
                page: 0,
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
            setError(error.message || '알 수 없는 에러가 발생했습니다.');
            setLectureList([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
    if (accessToken) {
        fetchLectureData(user, accessToken);
    }
}, [user, accessToken, currentPage]); // 🔄 currentPage도 의존성에 추가


    const openModal = (lecture) => {
        setSelectedLecture(lecture);
        setIsModalOpen(true);
    };

    const closeModal = () => {
        setSelectedLecture(null);
        setIsModalOpen(false);
    };


    return (
        <>
            <h2>강의 목록</h2>

            {loading && <div>강의 목록을 불러오는 중입니다...</div>}
            {error && <div style={{ color: 'red' }}>에러: {error}</div>}

            {!loading && !error && (
                <table>
                    <tbody>
                        {lectureList.length > 0 ? (
                            lectureList.map((lecture) => (
                                <tr
                                    key={lecture.lecIdx}
                                    onClick={() => openModal(lecture)}
                                    style={{ cursor: 'pointer' }}
                                >
                                    <td>{lecture.lecTit}</td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td>강의 목록 없음</td>
                            </tr>
                        )}
                    </tbody>
                </table>
            )}

            {isModalOpen && selectedLecture && (
                <div className="modal-overlay" onClick={closeModal}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <button className="modal-close" onClick={closeModal}>
                            ✖
                        </button>

                        <CourseDetail
                            lectureDetails={selectedLecture}
                            setIsModalOpen={setIsModalOpen}
                            currentPage={currentPage}
                            setCurrentPage={setCurrentPage}
                        />

                    </div>
                </div>
            )}
        </>
    );
}

export default CourseList;
