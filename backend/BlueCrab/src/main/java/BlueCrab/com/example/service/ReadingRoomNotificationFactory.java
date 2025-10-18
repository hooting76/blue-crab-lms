package BlueCrab.com.example.service;

import BlueCrab.com.example.entity.ReadingSeat;
import BlueCrab.com.example.entity.ReadingUsageLog;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds notification payloads for reading room related events.
 */
@Component
public class ReadingRoomNotificationFactory {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public NotificationTemplate createPreExpiryTemplate(ReadingSeat seat,
                                                         ReadingUsageLog usageLog,
                                                         LocalDateTime referenceTime) {
        LocalDateTime endTime = seat.getEndTime();
        int minutesLeft = (int) Math.max(0, Duration.between(referenceTime, endTime).toMinutes());

        String title = "열람실 좌석 퇴실 15분 전";
        String body = String.format("%d번 좌석 이용이 %d분 후 종료됩니다.", seat.getSeatNumber(), minutesLeft);

        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", "reading_room_pre_expiry");
        data.put("seatNumber", String.valueOf(seat.getSeatNumber()));
        data.put("minutesLeft", String.valueOf(minutesLeft));
        data.put("endTime", ISO_FORMATTER.format(endTime));
        data.put("deeplink", "/reading-room");
        data.put("userCode", usageLog.getUserCode());

        return new NotificationTemplate(title, body, data);
    }

    public static final class NotificationTemplate {
        private final String title;
        private final String body;
        private final Map<String, String> data;

        private NotificationTemplate(String title, String body, Map<String, String> data) {
            this.title = title;
            this.body = body;
            this.data = data;
        }

        public String getTitle() {
            return title;
        }

        public String getBody() {
            return body;
        }

        public Map<String, String> getData() {
            return data;
        }
    }
}
