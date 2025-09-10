import { useCallback, useState } from 'react';

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
// calendar func&config library end

// localization settings // date format setting library
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
        },
        {
            id: 1,
            title: '점심 식사',
            start: new Date(2025, 9, 11, 12, 0),
            end: new Date(2025, 9, 11, 13, 0),
        },
    ]);
    const [selectingSlot, setSelectingSlot] = useState(null);
    // state  end //


    // select slot evt
    const handleSelectSlot = useCallback(({ start, end, action, slots }) => {
        let className = "rbc-selected-cell";
        
        if (action) {
            // rbc-selected-cell
            const duration = moment(end).diff(moment(start), 'hours', true);
            const title = window.prompt(
                `${duration}시간의 이벤트를 생성하시겠습니까?\n` +
                `시작: ${moment(start).format('YYYY-MM-DD HH:mm')}\n` +
                `종료: ${moment(end).format('YYYY-MM-DD HH:mm')}\n\n` +
                `이벤트 제목을 입력하세요:`
            )

        if (title) {
            setEvents(prev => [...prev, {
                id: Date.now(),
                title: `${title} (${duration.toFixed(1)}시간)`,
                start,
                end
            }])
        }}
        setSelectingSlot(null)
    }, [])
    // select evt start

    const handleSelecting = useCallback((range) => {
        // 실시간으로 선택 중인 범위 표시
        setSelectingSlot(range)
        return true // 선택 계속 허용
    }, [])    

    // 날자별 style/속성 설정
    const dayPropGetter = (date) =>{
        if(date.getDay() === 0 || date.getDay() === 6){
            return {
                className: 'weekend',
                style: { backgroundColor: '#aabbcc' },
            }
        }
    }; // dayPropGetter end

    // custom toolbar
    const CustomToolbar = ({ label, onNavigate }) => {
        let labels = label.split(" ");
        switch(labels[0]){
            case 'January':
                labels[0] = '1월'
                break;
            case 'February':
                labels[0] = '2월'
                break;
            case 'March':
                labels[0] = '3월'
                break;
            case 'April':
                labels[0] = '4월'
                break;
            case 'May':
                labels[0] = '5월'
                break;           
            case 'June':
                labels[0] = '6월'
                break;
            case 'July':
                labels[0] = '7월'
                break;
            case 'August':
                labels[0] = '8월'
                break;        
            case 'September':
                labels[0] = '9월'
                break;   
            case 'October':
                labels[0] = '10월'
                break;            
            case 'November':
                labels[0] = '11월'
                break;
            default:
                labels[0] = '12월'
            break;
        };

        let cvt = labels[1] + '년 ' + labels[0];

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

    return(<>
        {selectingSlot && (
            <div style={{ 
                padding: '10px', 
                backgroundColor: '#e6f7ff', 
                border: '1px solid #91d5ff',
                borderRadius: '4px',
                marginBottom: '10px'
            }}>
            <strong>선택 중</strong>
                {moment(selectingSlot.start).format('YY:MM:DD')} - 
                {moment(selectingSlot.end).format('YY:MM:DD')}
                ({moment(selectingSlot.end).diff(moment(selectingSlot.start), 'DAY')}일)
            </div>
        )}
        <DnDCalendar 
            localizer={localizer}
            events={eventList}
            startAccessor="start"
            endAccessor="end"
            style={{ 
                height: 100 + '%',
            }} 
            selectable={true}
            resizable={true}
            defaultView='month'

            onEventDrop={({ event, start, end }) => {
                // 이벤트 드롭 처리 console.log(event); start/end => 옮겨놓은 위치의 시작/끝
                event.start = start;
                event.end = end;
            }}
            onEventResize={({ event, start, end }) => {
                // 이벤트 리사이즈 처리 console.log(event);
                event.start = start;
                event.end = end;
                // console.log(eventList[0]);
            }}

            // event select call
            dayPropGetter={dayPropGetter}
            onSelectSlot={handleSelectSlot}
            onSelecting={handleSelecting}
            components={
                {toolbar: CustomToolbar}
            }
        />
    </>)
}
export default CalendarComp;