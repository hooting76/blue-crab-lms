import {useState} from "react";
import CalendarModalCss from './css/CalendarModal.module.css';

function CalendarModal(start){
    const startData = start;
    const [useModal, setUseModal] = useState(false);

    if(useModal === false){
        setUseModal(true);
        {useModal && (
            <div className={CalendarModalCss.modal}>
                modal
                {startData}
            </div>
        )}
    }else{
        setUseModal(false);
    };
};

export default CalendarModal;