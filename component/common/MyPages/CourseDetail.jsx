import { useEffect, useState } from 'react';
import { UseUser } from '../../../hook/UseUser';
import CourseDetailEdit from './CourseDetailEdit';
import "../../../css/MyPages/CourseDetail.css";

// CourseDetail.jsx
function CourseDetail({ lectureDetails, setIsModalOpen, currentPage, setCurrentPage, setLectureToEdit }) {

    const { user } = UseUser();
    const [lectureDetail, setLectureDetail] = useState(lectureDetails);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

 useEffect(() => {
  if (!lectureDetails?.lecSerial) return;

  const accessToken = user?.data?.accessToken || localStorage.getItem('accessToken');
  if (!accessToken) return;

  const fetchCourseDetail = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`${BASE_URL}/lectures/detail`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ lecSerial: lectureDetails.lecSerial }),
      });

      if (!response.ok) throw new Error('강의 상세 정보를 불러오는데 실패했습니다.');

      const data = await response.json();
      setLectureDetail(data);
    } catch (err) {
      setError(err.message);
      setLectureDetail(null);
    } finally {
      setLoading(false);
    }
  };

  fetchCourseDetail();
}, [lectureDetails?.lecSerial, user]);


    if (loading) {
        return <div className="courseDetailContainer">강의 정보를 불러오는 중입니다...</div>;
    }

    if (error) {
        return <div className="courseDetailContainer">에러: {error}</div>;
    }

    if (!lectureDetail) {
        return <div className="courseDetailContainer">강의 정보를 불러올 수 없습니다.</div>;
    }

    // 유틸 함수들
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


   const onEditClick = (lectureDetail) => {
        setIsModalOpen(false);
        setLectureToEdit(lectureDetail);
        setCurrentPage("강의 수정 상세 페이지");
    };


    // 페이지가 '강의 수정 상세 페이지'일 경우
    if (currentPage === "강의 수정 상세 페이지") {
        if (!lectureDetail) {
            return <div>강의 상세 정보를 불러오는 중입니다...</div>;
        }

        return (
            <CourseDetailEdit
                lectureDetails={lectureDetail}
                currentPage={currentPage}
                setCurrentPage={setCurrentPage}
            />
        );
    }

    console.log("lectureDetail : ", lectureDetail);


    return (
        <div className="courseDetailContainer">
            <div className="courseDetailTitleCode">
                <span>강의 제목 : {lectureDetail.lecTit}</span>
                <span>강의 코드 : {lectureDetail.lecSerial}</span>
            </div>

            <div className="courseDetailProfMax">
                <span>담당 교수 : {lectureDetail.lecProfName}</span>
                <span>최대 수강 인원 : {lectureDetail.lecMany}</span>
            </div>

            <div className='courseSummary'>
                강의 개요 : {lectureDetail.lecSummary}
            </div>

            <div className="coursePointTimeMcodeDep">
                <span>학점 : {lectureDetail.lecPoint}</span>
                <span>강의시간 : {lectureDetail.lecTime}</span>
                <span>학부 : {formatMcode(lectureDetail.lecMcode)}</span>
                <span>학과 : {formatMcodeDep(lectureDetail.lecMcode, lectureDetail.lecMcodeDep)}</span>
            </div>

            <div className="courseTearSemesterMinOpen">
                <span>대상 학년 : {lectureDetail.lecYear}</span>
                <span>학기 : {lectureDetail.lecSemester}</span>
                <span>수강 최저 학년 : {lectureDetail.lecMin}</span>
                <span>열림 여부 : {formatOpen(lectureDetail.lecOpen)}</span>
            </div>

            <div style={{ marginTop: '20px' }}>
                <button
                    className="courseEditButton"
                    onClick={() => onEditClick(lectureDetail)}
                >
                    강의 수정
                </button>
            </div>
        </div>
    );
}

export default CourseDetail;
