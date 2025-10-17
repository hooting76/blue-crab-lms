import { useState, useEffect } from 'react';
import { UseUser } from '../../../hook/UseUser';

function CourseDetail({lectureIdx, onFetchComplete}) {

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
                onFetchComplete?.(data);
            });
        }
    }, [lectureIdx]);


    const getCourseDetail = async (accessToken, lectureIdx) => {
      try {
        const url = `${BASE_URL}/professor/lectures/detail/${lectureIdx}`;
        
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

    console.log("course : ", course);

    return (
        <>
            <div className="courseDetailContainer">
                <div className="courseDetailTitleCode">
                    <span>강의 제목 : {course.lecTit}</span>
                    <span>강의 코드 : {course.lecSerial}</span>
                </div>

                <div className="courseDetailProfMax">
                    <span>담당 교수 : {course.lecProf}</span>
                    <span>최대 수강 인원 : {course.lecMany}</span>
                </div>

                <div className='courseSummary'>
                    강의 개요 : {course.lecSummary}
                </div>

                <div className="coursePointTimeMcodeDep">
                    <span>학점 : {course.lecPoint}</span>
                    <span>강의시간 : {course.lecTime}</span>
                    <span>학부 : {course.lecMcode}</span>
                    <span>학과 : {course.lecMcodeDep}</span>
                </div>

                <div className="courseTearSemesterMajorMust">
                    <span>대상 학년 : {course.lecYear}</span>
                    <span>학기 : {course.lecSemester}</span>
                    <span>전공 여부 : {course.lecMajor}</span>
                    <span>필수 여부 : {course.lecMust}</span>
                </div>

                <div className="lecMinOpen">
                    <span>수강 최저 학년 : {course.lecMin}</span>
                    <span>열림 여부 : {course.lecOpen}</span>
                </div>
            </div>
        </>
    )
}

export default CourseDetail;