//공용 틀: 배너 + 탭 + 좌측 본문(children) + 우측 사이드(커뮤니티 메뉴)
import React, { useEffect, useState, useMemo } from "react";
import CommunitySidebar from "../notices/CommunitySidebar";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import "../../../css/Communities/FacilityRequest.css";


function DatePick({ startDate, endDate, setStartDate, setEndDate }) {
  const minTime = new Date();
  minTime.setHours(8, 0, 0, 0);
  const maxTime = new Date();
  maxTime.setHours(22, 0, 0, 0);

  const handleEndDateChange = (date) => {
    if (!startDate) return;

    const newEndDate = new Date(startDate);
    newEndDate.setHours(date.getHours());
    newEndDate.setMinutes(date.getMinutes());
    setEndDate(newEndDate);
  };

  return (
    <div className="date-picker-wrapper">
      <label>시작 날짜 및 시간</label>
      <DatePicker
        selected={startDate}
        onChange={(date) => {
          setStartDate(date);
          setEndDate(null);
        }}
        showTimeSelect
        timeFormat="HH:mm"
        timeIntervals={60}
        dateFormat="yyyy/MM/dd h:mm aa"
        placeholderText="시작 시간 선택"
        minDate={new Date()}
        minTime={minTime}
        maxTime={maxTime}
      />
      <br />
      <label>종료 날짜 및 시간</label>
      <DatePicker
        selected={endDate}
        onChange={handleEndDateChange}
        showTimeSelect
        timeFormat="HH:mm"
        timeIntervals={60}
        dateFormat="yyyy/MM/dd h:mm aa"
        placeholderText="종료 시간 선택"
        minDate={startDate}
        maxDate={startDate}
        minTime={startDate ? new Date(new Date(startDate).setMinutes(0)) : minTime}
        maxTime={maxTime}
        disabled={!startDate}
      />
    </div>
  );
}


function FacilityRequest({ 
    currentPage ="신청폼", 
    setCurrentPage = () => {},
    children
 }) {

const [startDate, setStartDate] = useState(null);
const [endDate, setEndDate] = useState(null);

const handleSubmit = () => {
    if (!startDate || !endDate) {
      alert("시작 및 종료 시간을 모두 선택하세요.");
      return;
    }
    // 예시 제출 처리
    alert(`신청 완료: ${startDate.toString()} ~ ${endDate.toString()}`);
  };

  return (
    <div className="notice-page">
      {/* 배너 (현재 카테고리 표기) */}
      <div className="banner" aria-label="공지 배너">
        <h2></h2>
      </div>

    <div className="facilityRequest">
      <div className="facWhyDate">
        <span className="facAndWhy">
          <label htmlFor="Facility">시설물 선택</label>
          <select id="Facility">
            <option value="Fac01">시설물01</option>
            <option value="Fac02">시설물02</option>
            <option value="Fac03">시설물03</option>
          </select>
          <br />
          <label htmlFor="FacilityReason">대여 사유 선택</label>
          <select id="FacilityReason">
            <option value="Reason01">사유01</option>
            <option value="Reason02">사유02</option>
            <option value="Reason03">사유03</option>
          </select>
        </span>

        <span className="datePick">
          <DatePick
            startDate={startDate}
            endDate={endDate}
            setStartDate={setStartDate}
            setEndDate={setEndDate}
          />
        </span>
      </div>

      <div className="facEtcReason">
        기타 사유(직접 입력)<br />
        <textarea placeholder="기타사유일때만 입력"></textarea>
        <br />
        <button type="submit" onClick={handleSubmit}>신청하기</button>
      </div>
    </div>

      {/* 본문 2단 레이아웃 */}
      <div className="grid">
        <main className="left">{children}</main>
        <aside className="right">
          <CommunitySidebar currentPage={currentPage} setCurrentPage={setCurrentPage} />
        </aside>
      </div>
    </div>
  );
}

export default FacilityRequest;