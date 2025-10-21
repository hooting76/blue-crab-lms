import { useState, useEffect } from 'react';
import { UseUser } from '../../../hook/UseUser';
import "../../../css/MyPages/CourseDetail.css";

function CourseDetail({lecture, onFetchComplete}) {

    const { user } = UseUser();
    const [course, setCourse] = useState(null);

    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const lecSerial = lecture.lecSerial;

    const getAccessToken = () => {
        const storedToken = localStorage.getItem('accessToken');
        if (storedToken) return storedToken;
        if (user && user.data && user.data.accessToken) return user.data.accessToken;
        return null;
    };


    const getCourseDetail = async (accessToken, lecSerial) => {
      try {
        const url = `${BASE_URL}/lectures/detail`;
        
        const response = await fetch(url, {
          method: 'POST', // POST 방식 명시
          headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
          body: JSON.stringify({ lecSerial })
        });
    
        if (!response.ok) throw new Error('강의 상세 정보를 불러오는데 실패했습니다.');
        return await response.json();
      } catch (error) {
        console.error('강의 상세 조회 에러:', error);
        throw error;
      }
    };

    
    useEffect(() => {
        const token = getAccessToken();
        if (token && lecSerial) {
            getCourseDetail(token, lecSerial).then((data) => {
                setCourse(data);
                onFetchComplete?.(data);
            });
        }
    }, [lecture]);

    console.log("course : ", course);



    return (
        <>
            {course &&
                <div className="courseDetailContainer">
                    <div className="courseDetailTitleCode">
                        <span>강의 제목 : {course.lecTit}</span>
                        <span>강의 코드 : {course.lecSerial}</span>
                    </div>

                    <div className="courseDetailProfMax">
                        <span>담당 교수 : {course.lecProfName}</span>
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

                    <div className="courseTearSemesterMinOpen">
                        <span>대상 학년 : {course.lecYear}</span>
                        <span>학기 : {course.lecSemester}</span>
                        <span>수강 최저 학년 : {course.lecMin}</span>
                        <span>열림 여부 : {course.lecOpen}</span>
                    </div>
                </div>
            }
        </>
    )
}

export default CourseDetail;