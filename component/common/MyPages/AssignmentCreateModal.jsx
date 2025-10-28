import {useState} from "react";
import "../../../css/MyPages/AssignmentCreateModal.css";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { UseUser } from '../../../hook/UseUser';

const AssignmentCreateModal = ({ onClose, lecSerial, lecTitle }) => {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [dueDate, setDueDate] = useState(null);
    const [maxScore, setMaxScore] = useState(null);
    const {user} = UseUser();
    const accessToken = user.data.accessToken;
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    const now = new Date();

    const formatDateToYYYYMMDD = (date) => {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, "0");
    const day = date.getDate().toString().padStart(2, "0");
    return `${year}${month}${day}`;
};

    const submitAssignmentCreate = async (e) => {


        if (!title || !description || !dueDate) {
            alert("모든 필드를 입력해주세요");
            return;
        }

        try {
            const assignmentData = {
                lecSerial: lecSerial,
                title: title,
                description: description,
                maxScore: maxScore,
                dueDate: formatDateToYYYYMMDD(dueDate)
            }

                const response = await fetch(`${BASE_URL}/assignments`, {
                method: "POST",
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(assignmentData)
            });

            if (!response.ok) throw new Error('과제 생성에 실패했습니다.');
            alert('과제가 성공적으로 생성되었습니다!');
            onClose(); // 모달 닫기
        } catch (error) {
            console.error('과제 생성 에러:', error);
        }
    };
    
    return (
        <div className="assignment-create-modal-container">
            <div className="assignment-create-modal-content">
                <div>과목: {lecTitle}</div>
                <div>과제 제목:
                    <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} required/>
                </div>
                <div>과제 설명:
                    <textarea value={description} onChange={(e) => setDescription(e.target.value)} required/>
                </div>
                <div>마감일:
                    <DatePicker
                        selected={dueDate}
                        onChange={(date) => setDueDate(date)}
                        dateFormat="yyyyMMdd"
                        placeholderText="마감일 선택"
                        minDate={now} // 오늘 이후 날짜만 선택 가능
                        required
                    />
                </div>
                <div>배점: 
                    <input type="number" value={maxScore} onChange={(e) => setMaxScore(e.target.value)} required/>
                </div>

                <button className="assignmentCreateSubmitBtn" onClick={submitAssignmentCreate}>과제 생성</button>
                <br/>
                <button className="assignmentCreateModalCloseBtn" onClick={onClose}>닫기</button>
            </div>
        </div>
    );
}

export default AssignmentCreateModal;