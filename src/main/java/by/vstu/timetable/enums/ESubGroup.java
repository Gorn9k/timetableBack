package by.vstu.timetable.enums;


import lombok.Getter;

import java.util.Arrays;

public enum ESubGroup {

    FIRST(0),
    SECOND(1),
    ALL(2),
    THIRD(3),
    FOURTH(4),
    UNKNOWN(-1);

    @Getter
    final int id;

    ESubGroup(int id) {
        this.id = id;
    }

    public static ESubGroup valueOf(int id) {
        return Arrays.stream(values()).filter(p -> p.id == id).findFirst().orElse(UNKNOWN);
    }
}
