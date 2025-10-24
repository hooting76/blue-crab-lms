package BlueCrab.com.example.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 상담 진행 상태 Enum
 */
public enum ConsultationStatus {
    SCHEDULED("예정됨"),
    IN_PROGRESS("진행중"),
    COMPLETED("완료됨"),
    CANCELLED("취소됨");

    private final String description;

    ConsultationStatus(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static ConsultationStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        for (ConsultationStatus status : ConsultationStatus.values()) {
            if (status.name().equalsIgnoreCase(value) || status.description.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ConsultationStatus: " + value);
    }

    public static ConsultationStatus fromDbValue(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (ConsultationStatus status : ConsultationStatus.values()) {
            if (status.name().equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ConsultationStatus DB value: " + dbValue);
    }

    public String toDbValue() {
        return this.name();
    }
}
