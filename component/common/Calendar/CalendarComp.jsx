import { useState } from 'react';

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
import { FaWindowClose } from 'react-icons/fa';
// calendar func&config library end

moment.locale('ko');
const localizer = momentLocalizer(moment);
const DnDCalendar = withDragAndDrop(Calendar);

// cal func start
function CalendarComp () {
    // state //
    const [eventList, setEvents] = useState([
        {
            id: 0,
            title: '긴 회의',
            start: new Date(2025, 9, 4, 10, 0), 
            end: new Date(2025, 9, 4, 12, 0),
            details:{
                where: '보람관 대회의실',
                sub: '제 23회 정기 학위수여식 관련 논문 감사',
            },
            readOnly: true,
            writer: '홍길동',
        },
        {
            id: 1,
            title: '점심 식사',
            start: new Date(2025, 9, 11, 12, 0),
            end: new Date(2025, 9, 11, 13, 0),
            details:{
                where: '나눔관 1층 구내식당',
                sub: '메뉴들',
            },
            readOnly: false,
            writer: '홍길동',
        },
    ]);
    Modal.setAppElement("#root"); // 접근 설정    
    const [selectedEvent, setSelectedEvent] = useState(null);
    const [eventTitle, setEventTitle] = useState('');

    // state update
    const handleSelectEvent = (event) => {
        setSelectedEvent(event);
    };

    const closeModal = () => {
        setSelectedEvent(null);
    };

    function handleTitChange(evt){
        let tmp = evt.target.value;
        setEventTitle(tmp);
    };
    // state update end

    // 날자별 style/속성 설정
    const dayPropGetter = (date) =>{
        if(date.getDay() === 0 || date.getDay() === 6){
            if(date.getDay() === 0){
                return {
                    className: 'weekend zeroDay',
                }
            }else{
                return {
                    className: 'weekend sixDay',
                }
            }
        }
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
            resizable={true}
            defaultView='month'
            readOnly={false}

            onEventDrop={({ event, start, end }) => {
                // 이벤트 드롭 처리 console.log(event); start/end => 옮겨놓은 위치의 시작/끝
                event.start = start;
                event.end = end;
            }}
            onEventResize={({ event, start, end }) => {
                // 이벤트 리사이즈 처리 console.log(event);
                event.start = start;
                event.end = end;
            }}

            // event select call
            onSelectEvent={handleSelectEvent}
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
                    zIndex: 999,
                    backgroundColor: "rgba(0, 0, 0, .25)",
                },
                content:{
                    width: "100%",
                    maxWidth: "450px",
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
                        defaultValue={selectedEvent.title}
                        onChange={handleTitChange} 
                    />
                
                    <button 
                        onClick={closeModal} 
                        className={calModalCss.closeBtn}
                    >
                        <FaWindowClose />
                    </button>
                </h3>

                {/* modal main */}
                <div className={calModalCss.modalMain}>
                    {selectedEvent.readOnly 
                        ? (<span>!! read only !!</span>)
                        : null
                    }
                    <div>
                        <p>
                            시작: {moment(selectedEvent.start).format("YYYY-MM-DD HH:mm")}
                        </p>
                        <p>
                            종료: {moment(selectedEvent.end).format("YYYY-MM-DD HH:mm")}
                        </p>
                        <p>
                            장소: {selectedEvent.details.where}
                        </p>
                        <p>
                            내용: {selectedEvent.details.sub}
                        </p>                        
                    </div>
                </div>
            </div>            
        )}

        </Modal>
    </>)
}
export default CalendarComp;


// {
//     "id": 0,
//     "title": "긴 회의",
//     "start": "2025-10-04T01:00:00.000Z",
//     "end": "2025-10-04T03:00:00.000Z",
//     "details": {
//         "where": "보람관 대강당",
//         "sub": "제 23회 정기 학위수여식 관련 논문 감사회의"
//     },
//     "readOnly": true
// }