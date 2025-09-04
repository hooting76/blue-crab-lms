import React from 'react';

// calendar func library
import { Calendar, momentLocalizer } from 'react-big-calendar';
import moment from 'moment';
import WithDragAndDrop from 'react-big-calendar/lib/addons/dragAndDrop'
import 'react-big-calendar/lib/css/react-big-calendar.css';
import 'react-big-calendar/lib/addons/dragAndDrop/styles.css';
import "moment-timezone/builds/moment-timezone-with-data";
import "moment/locale/ko";
// calendar func library end

// localization settings // date format setting library
const localizer = momentLocalizer(moment);
const DnDCalendar = WithDragAndDrop(Calendar);

export default function CalendarComp() {
    const events = [{
        title: '긴 회의',
        start: new Date(2025, 9, 4, 10, 0), 
        end: new Date(2025, 9, 4, 12, 0),
        },{
        title: '점심 식사',
        start: new Date(2025, 9, 11, 12, 0),
        end: new Date(2025, 9, 11, 13, 0),
    },];
      
    return(
        <DnDCalendar
            localizer={localizer}
            events={events}
            startAccessor="start"
            endAccessor="end"
            views={["month"]}
            draggableAccessor={() => true}
            style={{ 
                height: 100 + '%',
            }} 
        />
    );
}