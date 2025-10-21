import { useState, useEffect } from 'react';
import { UseUser } from '../../../hook/UseUser';
import "../../../css/MyPages/CourseDetail.css";

function CourseDetail({lecture, onFetchComplete}) {

    const { user } = UseUser();
    const [course, setCourse] = useState(null);

    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const lecSerial = lecture.lecSerial;
    const lecMcode = lecture.lecMcode;
    const lecMcodeDep = lecture.lecMcodeDep;

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


    // 학부 함수
    const formatMcode = (lecMcode) => {
    switch (lecMcode) {
        case "01": return "해양학부";
        case "02": return "보건학부";
        case "03": return "자연과학부";
        case "04": return "인문학부";
        case "05": return "공학부";
        default: return "기타";
    }
};


// 학과 함수
const formatMcodeDep = (lecMcode, lecMcodeDep) => {
    if (lecMcode === "01") {
        switch (lecMcodeDep) {
            case "01": return "항해학과";
            case "02": return "해양경찰";
            case "03": return "해군사관";
            case "04": return "도선학과";
            case "05": return "해양수산학";
            case "06": return "조선학과";
        }
    } else if (lecMcode === "02") {
        switch (lecMcodeDep) {
            case "01": return "간호학";
            case "02": return "치위생";
            case "03": return "약학과";
            case "04": return "보건정책학";
        }
    } else if (lecMcode === "03") {
        switch (lecMcodeDep) {
            case "01": return "물리학";
            case "02": return "수학";
            case "03": return "분자화학";
        }
    } else if (lecMcode === "04") {
        switch (lecMcodeDep) {
            case "01": return "철학";
            case "02": return "국어국문";
            case "03": return "역사학";
            case "04": return "경영";
            case "05": return "경제";
            case "06": return "정치외교";
            case "07": return "영어영문";
        }
    } else if (lecMcode === "05") {
        switch (lecMcodeDep) {
            case "01": return "컴퓨터공학";
            case "02": return "기계공학";
            case "03": return "전자공학";
            case "04": return "ICT융합";
        }
    } else return "기타"
}


// 열림 여부 함수
const formatOpen = (lecOpen) => {
    if (lecOpen === 1) {return "열림"} else {return "닫힘"};
}




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
                        <span>학부 : {formatMcode(lecMcode)}</span>
                        <span>학과 : {formatMcodeDep(lecMcode, lecMcodeDep)}</span>
                    </div>

                    <div className="courseTearSemesterMinOpen">
                        <span>대상 학년 : {course.lecYear}</span>
                        <span>학기 : {course.lecSemester}</span>
                        <span>수강 최저 학년 : {course.lecMin}</span>
                        <span>열림 여부 : {formatOpen(course.lecOpen)}</span>
                    </div>
                </div>
            }
        </>
    )
}

export default CourseDetail;