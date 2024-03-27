package by.vstu.timetable.enums;

import lombok.Getter;

import java.util.Arrays;

public enum EWeekType {

    ALWAYS(0),
    NUMERATOR(1),
    DENOMINATOR(2),

    FIRST(11),
    SECOND(12),
    THIRD(13),
    FOURTH(14);

    final int id;

    EWeekType(int id) {
        this.id = id;
    }

    public static EWeekType valueOf(int id) {
        return Arrays.stream(values()).filter(p -> p.id == id).findFirst().orElseThrow();
    }
}
