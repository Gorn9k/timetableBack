package by.vstu.timetable.enums;

import lombok.Getter;

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
}
