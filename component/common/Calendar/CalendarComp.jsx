import { useEffect, useState } from 'react';

// calendar func&config library
import { Calendar, momentLocalizer } from 'react-big-calendar';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import withDragAndDrop from 'react-big-calendar/lib/addons/dragAndDrop';
import 'react-big-calendar/lib/addons/dragAndDrop/styles.css';
// moment
import moment from 'moment';
import "moment-timezone/builds/moment-timezone-with-data";
import "moment/locale/ko"; // korean locale
import './css/calcCss.css';
import calModalCss from './css/CalendarModal.module.css';
import Modal from 'react-modal';
import { FaWindowClose, FaMapMarkerAlt, FaCalendarWeek } from 'react-icons/fa';

import acJson from '../../../public/schedule.json';
// calendar func&config library end

moment.locale('ko');
const localizer = momentLocalizer(moment);
const DnDCalendar = withDragAndDrop(Calendar);

// cal func start
function CalendarComp () {
    const [eventList, setEvents] = useState([]);

    const acJsonList = {acJson};
    useEffect(() => {
        const processedEvents = acJsonList.acJson.map((data) => {
            if(typeof(data.start) === 'object'){
                return data;
            }
            const startArr = data.start.trim().split(",");
            const endArr = data.end.trim().split(",");

            if (!endArr[3] || endArr[3].trim() === ""){
                endArr[3] = '23';
                startArr[3] = '00';
            }
            if (!endArr[4] || endArr[4].trim() === ""){
                endArr[4] = '59';
                startArr[4] = '00';
            }

            // 문자열을 숫자로 변환 (new Date에 안전하게 전달하기 위해)
            for (let i = 0; i < startArr.length; i++) {
                startArr[i] = Number(String(startArr[i]).trim());
            }
            for (let i = 0; i < endArr.length; i++) {
                endArr[i] = Number(String(endArr[i]).trim());
            }
            return {
                ...data,
                start: new Date(startArr[0], startArr[1], startArr[2], startArr[3], startArr[4]),
                end: new Date(endArr[0], endArr[1], endArr[2], endArr[3], endArr[4])
            };
        });
        setEvents(processedEvents);
    }, []);

    // modal state //
    Modal.setAppElement("#root"); // 접근 설정    
    const [selectedEvent, setSelectedEvent] = useState(null);
    
    const handleSelectEvent = (event) => {
        setSelectedEvent(event);
    };
    const closeModal = () => {
        setSelectedEvent(null);
    };
    // modal state end


    // 날자별 style/속성 설정
    const dayPropGetter = (date) =>{
        if(date.getDay() === 0 || date.getDay() === 6){
            if(date.getDay() === 0){
                return {className: 'weekend zeroDay',}
            }else{
                return {className: 'weekend sixDay',}
            };
        };
    }; // dayPropGetter end

    // custom toolbar
    const CustomToolbar = ({ label, onNavigate }) => {
        // 영문 + 연도 형태의 문자형 자르기 / August 2025
        let labels = label.split(" ");

        // 영문 월명 객체타입으로 저장
        const MonthNames = [
            {'January': '1월'}, {'February': '2월'}, {'March': '3월'}, {'April': '4월'}, 
            {'May': '5월'}, {'June': '6월'}, {'July': '7월'}, {'August':'8월'},
            {'September':'9월'}, {'October': '10월'}, {'November': '11월'}, {'December': '12월'}
        ];

        let names = ('%s', MonthNames);
        let valuseMonth;
        // 영문이름 비교하여 대응 / 키:값 
        names.forEach((el) => {
            if (Object.keys(el)[0] === labels[0]) {
                valuseMonth = Object.values(el);
            }
        });

        let cvt = labels[1] + '년 ' + valuseMonth;

        return(<>
            <div className="rbc-toolbar">
                <span className="rbc-btn-group">
                    <button onClick={() => onNavigate('PREV')}>이전달</button>
                    <button onClick={() => onNavigate('TODAY')}>이번달</button>
                    <button onClick={() => onNavigate('NEXT')}>다음달</button>
                </span>
                <span className="rbc-toolbar-label">{cvt}</span>
            </div> 
        </>);
    };
    // custom toolbar end


    // calendar main
    return(<>
        <DnDCalendar 
            localizer={localizer}
            events={eventList}
            startAccessor="start"
            endAccessor="end"
            style={{ 
                height: 100 + '%',
                borderRadius: '0 0 10px 10px',
                overflow: 'hidden',
            }} 
            selectable={true}
            resizable={false}
            view={['month']}
            defaultView='month'
            onSelectEvent={handleSelectEvent}
            readOnly={true}
            dayPropGetter={dayPropGetter}
            components={{toolbar: CustomToolbar}}
        />

        {/* modal layer */}
        <Modal
            isOpen={!!selectedEvent}
            onRequestClose={closeModal}
            contentLabel='상세보기'
            style={{
                overlay:{
                    zIndex: 9999,
                    backgroundColor: "rgba(0, 0, 0, .25)",
                },
                content:{
                    width: "100%",
                    maxWidth: "420px",
                    height: "100%",
                    maxHeight: "75%",
                    boxSizing: "border-box",
                    margin: "auto",
                    borderRadius: "12px",
                    padding: "20px",
                },
            }}
        > 

            {/* modal main*/}
            {selectedEvent && (
            <div className={calModalCss.modalWrap}>
                <h3 className={calModalCss.tit}>
                    <input 
                        type="text" 
                        readOnly={selectedEvent.readOnly}        
                        defaultValue={selectedEvent.title}/>
                    <button 
                        onClick={closeModal} 
                        className={calModalCss.closeBtn}>
                            <FaWindowClose />
                    </button>
                </h3>

                {/* modal main */}
                <div className={calModalCss.modalMain}>
                    <div>
                        <div className={calModalCss.DateWrap}>
                            <span><FaCalendarWeek/></span>                            
                            {selectedEvent.start.getDate() !== selectedEvent.end.getDate()
                                ? 
                                    <>
                                    <span>
                                        {(selectedEvent.start.getHours() == 0)
                                            && (selectedEvent.start.getMinutes() == 0)
                                            ? <>{moment(selectedEvent.start).format("MM월 DD일")}</>
                                            : <>{moment(selectedEvent.start).format("MM월 DD일 HH:mm")}</>
                                        }
                                    </span>
                                    <span> ~ </span>
                                    </>
                                : null
                            }

                            <span>
                                {moment(selectedEvent.end).format("MM월 DD일")}
                            </span>
                        </div>
                        {/* DateWrap end */}

                        {selectedEvent.details.where 
                            ? 
                                <div>
                                    <span><FaMapMarkerAlt/></span>
                                    <p>{selectedEvent.details.where}</p> 
                                </div>
                            : null}
                        
                        {selectedEvent.details.sub
                            ? <p>{selectedEvent.details.sub}</p>
                            : null}                        
                    </div>
                </div>
            </div>            
            )}

        </Modal>
    </>)
}
export default CalendarComp;