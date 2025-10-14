package BlueCrab.com.example.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 시설 유형 Enum
 * DB에 실제 저장된 값과 일치하도록 구성
 */
public enum FacilityType {
    // 실제 DB에 저장된 시설 타입 (2025-10-14 확인)
    AQUACULTURE("양식장"),
    AUDITORIUM("강당"),
    GYM("체육관"),
    MARINE("해양시설"),
    MEETING_ROOM("회의실"),
    SPORTS("체육시설"),
    STORAGE("창고"),
    WORKSHOP("작업장"),
    
    // 향후 확장을 위한 추가 타입
    SEMINAR_ROOM("세미나실"),
    LECTURE_ROOM("강의실"),
    CONFERENCE_ROOM("회의실"),
    LAB("실습실"),
    STUDY_ROOM("스터디룸"),
    LIBRARY("도서관"),
    CAFETERIA("식당"),
    PARKING("주차장"),
    COMPUTER_LAB("컴퓨터실"),
    MUSIC_ROOM("음악실"),
    ART_ROOM("미술실"),
    STUDIO("스튜디오"),
    LOUNGE("라운지"),
    OUTDOOR_FACILITY("야외시설"),
    OTHER("기타");

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
