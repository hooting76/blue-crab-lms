import { useState } from "react";
import { UseUser } from "../../../hook/UseUser";
import "../../../css/MyPages/AssignmentSubmitModal.css";

const AssignmentSubmitModal = ({ onClose, assignIdx }) => {
  const BASE_URL = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api";
  const { user } = UseUser();
  const accessToken = user?.data?.accessToken;
  const studentIdx = user?.data?.user?.id;

  // ✅ 입력된 텍스트와 파일 상태 관리
  const [submissionContent, setSubmissionContent] = useState("");
  const [submissionFiles, setSubmissionFiles] = useState([]);

  // ✅ 파일 선택 시 상태에 저장
  const handleFileChange = (e) => {
    setSubmissionFiles(Array.from(e.target.files));
  };

  // ✅ 과제 제출 함수
  const assignSubmit = async () => {
  if (!submissionContent || submissionFiles.length === 0) {
    alert("내용과 파일 둘 다 제출해야 합니다.");
    return;
  }

  console.log("accessToken:", accessToken);
console.log("studentIdx:", studentIdx);
console.log("assignIdx:", assignIdx);


  try {
    // 1️⃣ 파일 업로드
    const uploadUrls = [];
    for (const file of submissionFiles) {
      const formData = new FormData();
      formData.append("file", file);

      const uploadRes = await fetch(`${BASE_URL}/uploads`, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}` },
        body: formData,
      });
      const uploadData = await uploadRes.json();
      if (!uploadRes.ok) throw new Error(uploadData.message);

      uploadUrls.push({
        name: file.name,
        url: uploadData.url, // 업로드 API가 반환하는 경로
      });
    }

    // 2️⃣ 과제 제출 (JSON 전송)
    const requestBody = {
      assignIdx,
      studentIdx,
      submissionContent,
      submissionFiles: JSON.stringify(uploadUrls)
    };

    const response = await fetch(`${BASE_URL}/assignments/submit`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify(requestBody)
    });


    const text = await response.text();
    console.log("서버 응답 원문:", text);
    const data = await response.json();
    if (!response.ok) throw new Error(data.message || "과제 제출 실패");

    alert("✅ 과제 제출이 완료되었습니다!");
    console.log("✅ 과제 제출 성공:", data);
    onClose();
  } catch (err) {
    console.error("❌ 과제 제출 오류:", err);
    alert("과제 제출 중 문제가 발생했습니다: " + err.message);
  }
};


  return (
    <div className="assignmentSubmit-modal-container">
      <div className="assignmentSubmit-modal-content">
        <h2>📘 과제 제출</h2>

        <textarea
          placeholder="과제 내용을 입력하세요..."
          value={submissionContent}
          onChange={(e) => setSubmissionContent(e.target.value)}
          className="submission-textarea"
        />

        <input
          type="file"
          multiple
          onChange={handleFileChange}
          className="submission-file-input"
        />

        {submissionFiles.length > 0 && (
          <ul className="file-list">
            {submissionFiles.map((file, idx) => (
              <li key={idx}>{file.name}</li>
            ))}
          </ul>
        )}

        <div className="assignmentSubmit-btns">
          <button className="assignmentSubmitBtn" onClick={assignSubmit}>
            제출하기
          </button>
          <br/>
          <button className="assignmentSubmitCloseBtn" onClick={onClose}>
            닫기
          </button>
        </div>
      </div>
    </div>
  );
};

export default AssignmentSubmitModal;
