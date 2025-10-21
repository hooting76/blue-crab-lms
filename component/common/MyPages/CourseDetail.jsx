import { useEffect, useState } from 'react';
import { UseUser } from '../../../hook/UseUser';
import "../../../css/MyPages/CourseDetail.css";

function CourseDetail({ lecture, onFetchComplete }) {
    const { user } = UseUser();
    const [course, setCourse] = useState(null);

    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

    const getAccessToken = () => {
        return user?.data?.accessToken || localStorage.getItem('accessToken') || null;
    };

    const fetchCourseDetail = async (accessToken, lecSerial) => {
        try {
            const response = await fetch(`${BASE_URL}/lectures/detail`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ lecSerial }),
            });

            if (!response.ok) {
                throw new Error('강의 상세 정보를 불러오는데 실패했습니다.');
            }

            return await response.json();
        } catch (error) {
            console.error('강의 상세 조회 에러:', error);
            return null;
        }
    };

    useEffect(() => {
        if (!lecture) return;

        const token = getAccessToken();
        if (!token || !lecture.lecSerial) return;

        fetchCourseDetail(token, lecture.lecSerial).then((data) => {
            if (data) {
                setCourse(data);
                onFetchComplete?.(data); // ✅ 콜백으로 부모에 전달
            }
        });
    }, [lecture]);

    if (!course) {
        return <div className="courseDetailContainer">강의 정보를 불러오는 중입니다...</div>;
    }

    // 유틸: 학부 이름
    const formatMcode = (code) => {
        const map = {
            "01": "해양학부",
            "02": "보건학부",
            "03": "자연과학부",
            "04": "인문학부",
            "05": "공학부",
        };
        return map[code] || "기타";
    };

    // 유틸: 학과 이름
    const formatMcodeDep = (mcode, dep) => {
        const depMap = {
            "01": {
                "01": "항해학과", "02": "해양경찰", "03": "해군사관",
                "04": "도선학과", "05": "해양수산학", "06": "조선학과"
            },
            "02": {
                "01": "간호학", "02": "치위생", "03": "약학과", "04": "보건정책학"
            },
            "03": {
                "01": "물리학", "02": "수학", "03": "분자화학"
            },
            "04": {
                "01": "철학", "02": "국어국문", "03": "역사학", "04": "경영",
                "05": "경제", "06": "정치외교", "07": "영어영문"
            },
            "05": {
                "01": "컴퓨터공학", "02": "기계공학", "03": "전자공학", "04": "ICT융합"
            }
        };
        return depMap[mcode]?.[dep] || "기타";
    };

    const formatOpen = (lecOpen) => (lecOpen === 1 ? "열림" : "닫힘");

    return (
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
                <span>학부 : {formatMcode(course.lecMcode)}</span>
                <span>학과 : {formatMcodeDep(course.lecMcode, course.lecMcodeDep)}</span>
            </div>

            <div className="courseTearSemesterMinOpen">
                <span>대상 학년 : {course.lecYear}</span>
                <span>학기 : {course.lecSemester}</span>
                <span>수강 최저 학년 : {course.lecMin}</span>
                <span>열림 여부 : {formatOpen(course.lecOpen)}</span>
            </div>
        </div>
    );
}

export default CourseDetail;
