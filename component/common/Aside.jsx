import { useEffect, useState } from 'react';
import { Calendar, momentLocalizer } from 'react-big-calendar';
import moment from 'moment';
import "moment-timezone/builds/moment-timezone-with-data";
import "moment/locale/ko"; // korean locale
import AsideCss from '../../css/modules/Aside.module.css';

import GetProcessedEvents from './Calendar/CalendarEvents';

moment.locale('ko');
const localizer = momentLocalizer(moment);

function Aside(){
    const [eventList, setEvents] = useState([]);

    useEffect(() => {
        const processedEvents = GetProcessedEvents();
        setEvents(processedEvents);
    }, []);  
    
    // list custom
    const CustomToolbar = () => {
        return (
            <>
            <div className="rbc-toolbar agenda">
                <span className="time-display">
                    {moment().format('MM월 DD일 HH시 mm분')}
                </span>
            </div>
            </>
        )
    };

    const CustomAgendaEvent = ({ event }) => {
        return(
            <div className="custom-agenda-event">
                <p>{event.title}</p>
                {event.details.where && event.details
                    ? <p>{event.details.where}</p>
                    : null}
            </div>
        );
    };

    return(
        <div className={AsideCss.wrap}>
            <Calendar
                localizer={localizer}
                events={eventList}
                resizable={false}
                view={'agenda'}
                defaultView={'agenda'}
                readOnly={true}
                components={{
                    toolbar: CustomToolbar,
                    agenda: {
                        event: CustomAgendaEvent
                    }
                }}
                formats={{
                    agendaTimeRangeFormat: ({ start, end }) => {
                        return `${moment(start).format('HH:mm')} - ${moment(end).format('HH:mm')}`;
                    }
                }}
                 // Show only 1 day
                date={new Date('2025-10-20')}
                />
        </div>
    );
};

export default Aside;