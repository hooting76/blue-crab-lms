import { useState, useEffect } from 'react';
import { UseUser } from '../../../hook/UseUser';

function CourseDetail({lectureIdx}) {

    const { user } = UseUser();
    const [course, setCourse] = useState(null);

    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

    const getAccessToken = () => {
        const storedToken = localStorage.getItem('accessToken');
        if (storedToken) return storedToken;
        if (user && user.data && user.data.accessToken) return user.data.accessToken;
        return null;
    };

    useEffect(() => {
        const token = getAccessToken();
        if (token && lectureIdx) {
            getCourseDetail(token, lectureIdx).then((data) => {
                setCourse(data);
            });
        }
    }, [lectureIdx]);


    const getCourseDetail = async (accessToken, lectureIdx) => {
      try {
        const url = `${BASE_URL}/v1/professor/lectures/${lectureIdx}`;
        
        const response = await fetch(url, {
          method: 'POST', // POST 방식 명시
          headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                }
        });
    
        if (!response.ok) throw new Error('강의 상세 정보를 불러오는데 실패했습니다.');
        return await response.json();
      } catch (error) {
        console.error('강의 상세 조회 에러:', error);
        throw error;
      }
    };

    return (
        <div className="courseDetailContainer">
            <div className="courseDetailTitleAndCode">
                <span className="courseDetailTitle">강의 제목 : {course.lectureName}</span>
                <span className="courseDetailCode">{course.lectureCode}</span>
            </div>

            <div className="courseDetailProfMax">
                <span className="courseDetailProf">담당 교수 : {}</span>
                <span className="courseDetailMax">수강 인원 : {course.maxStudents}</span>
            </div>
        </div>
    )
}

export default CourseDetail;