package BlueCrab.com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 시설 예약 정책 설정을 관리하는 클래스
 */
@Configuration
@ConfigurationProperties(prefix = "reservation.policy")
public class ReservationPolicyProperties {

    private Integer maxDaysInAdvance = 30;
    private Integer minDurationMinutes = 30;
    private Integer maxDurationMinutes = 480;
    private Integer autoCompleteHours = 1;

    public Integer getMaxDaysInAdvance() {
        return maxDaysInAdvance;
    }

    public void setMaxDaysInAdvance(Integer maxDaysInAdvance) {
        this.maxDaysInAdvance = maxDaysInAdvance;
    }

    public Integer getMinDurationMinutes() {
        return minDurationMinutes;
    }

    public void setMinDurationMinutes(Integer minDurationMinutes) {
        this.minDurationMinutes = minDurationMinutes;
    }

    public Integer getMaxDurationMinutes() {
        return maxDurationMinutes;
    }

    public void setMaxDurationMinutes(Integer maxDurationMinutes) {
        this.maxDurationMinutes = maxDurationMinutes;
    }

    public Integer getAutoCompleteHours() {
        return autoCompleteHours;
    }

    public void setAutoCompleteHours(Integer autoCompleteHours) {
        this.autoCompleteHours = autoCompleteHours;
    }
}
