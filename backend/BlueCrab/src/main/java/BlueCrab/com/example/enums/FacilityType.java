package BlueCrab.com.example.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 시설 유형 Enum
 */
public enum FacilityType {
    SEMINAR_ROOM("세미나실"),
    LECTURE_ROOM("강의실"),
    CONFERENCE_ROOM("회의실"),
    LAB("실습실"),
    AUDITORIUM("강당"),
    STUDY_ROOM("스터디룸");

    private final String description;

    FacilityType(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static FacilityType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (FacilityType type : FacilityType.values()) {
            if (type.name().equalsIgnoreCase(value) || type.description.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown FacilityType: " + value);
    }

    public static FacilityType fromDbValue(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (FacilityType type : FacilityType.values()) {
            if (type.name().equals(dbValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown FacilityType DB value: " + dbValue);
    }

    public String toDbValue() {
        return this.name();
    }
}
