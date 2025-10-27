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
        const [now, setNow] = useState(moment());
        useEffect(() => {
            const timer = setInterval(() => setNow(moment()), 1000); // update every second
            return () => clearInterval(timer);
        }, []);

        return (
            <div className="rbc-toolbar agenda">
                <span className="time-display">
                    {now.format('MM월 DD일 HH시 mm분')}
                </span>
            </div>
        );
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

    const [currentDate, setCurrentDate] = useState(new Date());

    useEffect(() => {
        const timer = setInterval(() => {
            setCurrentDate(new Date());
        }, 1000);
        return () => clearInterval(timer);
    }, []);

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
                date={new Date()}
                />
        </div>
    );
};

export default Aside;