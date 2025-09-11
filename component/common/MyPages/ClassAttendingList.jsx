import '../../../css/MyPages/ClassAttendingList.css';
import classAttendingDummy from '../../../src/mock/classAttendingDummy.js'; //임시로 만든 더미데이터.

function ClassAttendingList() {
    // 학점 총합 계산
    const totalCredits = classAttendingDummy.reduce((sum, cls) => sum + Number(cls.LEC_POINT), 0);

    return (
        <div className="classAttending_list_container">
            <table className="classAttending_list_header">
                <thead>
                    <tr>
                        <th style={{ width: "5%" }}>순번</th>
                        <th style={{ width: "10%" }}>종별</th>
                        <th style={{ width: "25%" }}>교과목명</th>
                        <th style={{ width: "20%" }}>강의시간</th>
                        <th style={{ width: "15%" }}>수업계획</th>
                        <th style={{ width: "15%" }}>담당교수</th>
                        <th style={{ width: "10%" }}>학점</th>
                    </tr>
                </thead>
            </table>

            <table className="classAttending_list_body">
                <tbody>
                    {classAttendingDummy.map((cls, index) => (
                        <tr key={cls.LEC_IDX}>
                            <td style={{ width: "5%" }}>{index + 1}</td>
                            <td style={{ width: "10%" }}>{cls.LEC_BASIC === "1" ? "전공" : "교양"}</td>
                            <td style={{ width: "25%" }}>{cls.LEC_NAME}</td>
                            <td style={{ width: "20%" }}>{cls.LEC_TIME}</td>
                            <td style={{ width: "15%" }}></td>
                            <td style={{ width: "15%" }}>{cls.LEC_PROF}</td>
                            <td style={{ width: "10%" }}>{cls.LEC_POINT}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
            <div className='totalpoints'>
                총 학점: {totalCredits}
            </div>
        </div>
    );
}

export default ClassAttendingList;
