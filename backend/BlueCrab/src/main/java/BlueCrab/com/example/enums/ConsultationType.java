package BlueCrab.com.example.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 상담 유형 Enum
 */
public enum ConsultationType {
    ACADEMIC("학업상담"),
    CAREER("진로상담"),
    CAMPUS_LIFE("학교생활"),
    ETC("기타");

    private final String description;

    ConsultationType(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static ConsultationType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (ConsultationType type : ConsultationType.values()) {
            if (type.name().equalsIgnoreCase(value) || type.description.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ConsultationType: " + value);
    }

    public static ConsultationType fromDbValue(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (ConsultationType type : ConsultationType.values()) {
            if (type.name().equals(dbValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ConsultationType DB value: " + dbValue);
    }

    public String toDbValue() {
        return this.name();
    }
}
