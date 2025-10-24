package BlueCrab.com.example.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 상담 요청 상태 Enum
 */
public enum RequestStatus {
    PENDING("대기중"),
    APPROVED("승인됨"),
    REJECTED("반려됨"),
    CANCELLED("취소됨");

    private final String description;

    RequestStatus(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static RequestStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        for (RequestStatus status : RequestStatus.values()) {
            if (status.name().equalsIgnoreCase(value) || status.description.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown RequestStatus: " + value);
    }

    public static RequestStatus fromDbValue(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (RequestStatus status : RequestStatus.values()) {
            if (status.name().equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown RequestStatus DB value: " + dbValue);
    }

    public String toDbValue() {
        return this.name();
    }
}
