import React, { useState } from 'react';
import { UseUser } from '../../../hook/UseUser';
import ProfNoticeWritingPage from './ProfNoticeWritingPage.jsx';

function ClassAttendingNotice({currentPage, setCurrentPage}) {
    const {user} = UseUser();

    const profNoticeWrite = () => {
    setCurrentPage("과목별 공지 작성");
    }

    if (currentPage === "과목별 공지 작성") {
        return (
            <ProfNoticeWritingPage currentPage={currentPage} setCurrentPage={setCurrentPage}/>
        );
    }

    return(
        <>
            수강과목 공지사항

            <div className="profNoticeWriteBtnArea">
                {user.data.user.userStudent === 1 &&
                <button className="profNoticeWriteBtn" onClick={profNoticeWrite}>과목별 공지 작성</button>}
            </div>
        </>
    )
}

export default ClassAttendingNotice;