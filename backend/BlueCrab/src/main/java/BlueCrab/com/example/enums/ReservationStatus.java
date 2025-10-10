package BlueCrab.com.example.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 예약 상태 Enum
 */
public enum ReservationStatus {
    PENDING("대기중"),
    APPROVED("승인됨"),
    REJECTED("반려됨"),
    CANCELLED("취소됨"),
    COMPLETED("완료됨");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static ReservationStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.name().equalsIgnoreCase(value) || status.description.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ReservationStatus: " + value);
    }

    public static ReservationStatus fromDbValue(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.name().equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ReservationStatus DB value: " + dbValue);
    }

    public String toDbValue() {
        return this.name();
    }
}
