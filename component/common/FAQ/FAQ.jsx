import React, { useState } from 'react';
import CommunitySidebar from "../notices/CommunitySidebar";
import "../../../css/Communities/FAQ.css"

function FAQ({ currentPage, setCurrentPage }) {

  const [openRow, setOpenRow] = useState(null);
  const handleRowClick = (index) => {
        setOpenRow(openRow === index ? null : index);
    };

const faqList = [
  { question: "Q. 일반휴학은 어떤 경우 신청합니까?", answer: "A. 가사 및 질병 문제 등 개인적 사정으로 휴학하는 경우입니다." },
  { question: "Q. 군휴학은 어떻게 하나요?", answer: "A. 입영통지서와 본인도장, 보호자도장을 지참하여 입대일 2주 전에 군휴학원을 제출해야 합니다." },
  { question: "Q. 일반휴학 기간 및 횟수는 얼마까지 가능한지요?", answer: "A. 군휴학을 제외한 일반휴학은 1회에 1년을 초과할 수 없으며, 통산하여 2회를 초과할 수 없습니다. 군휴학은 횟수에 산입하지 않습니다." },
  { question: "Q. 일반휴학 신청기간은?", answer: "A. 개강 후 수업일수 1/4일 이전에만 가능합니다." },
  { question: "Q. 휴학 중 주소가 변경되었으면 어떻게 하나요?", answer: "A. 학과 또는 교무처로 연락하여 변경하면 됩니다." }
];

return (
  <div className="notice-page">
    <div className="banner" aria-label="공지 배너">
      <h2>FAQ</h2>
    </div>

    <div className="faq">
      {faqList.map((faq, index) => (
        <React.Fragment key={index}>
          <div
            className="faqquestion"
            onClick={() => handleRowClick(index)}
            style={{ cursor: 'pointer' }}
          >
            {faq.question}
          </div>
          {openRow === index && <div className='faqanswer'>{faq.answer}</div>}
        </React.Fragment>
      ))}
    </div>

    <div className="grid">
      <aside className="right">
        <CommunitySidebar currentPage={currentPage} setCurrentPage={setCurrentPage} />
      </aside>
    </div>
  </div>
);

}
export default FAQ;
