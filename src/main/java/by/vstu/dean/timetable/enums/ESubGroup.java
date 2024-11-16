package by.vstu.dean.timetable.enums;


import lombok.Getter;

import java.util.Arrays;

public enum ESubGroup {

    FIRST(0),
    SECOND(1),
    ALL(2),
    THIRD(3),
    FOURTH(4),
    SEWING(5),      // швейники
    SHOE(6),        // обувщики
    TEXTILE(7),     // текстильщики
    GRAPHIC(8),     // графический дизайн
    MULTIMEDIA(9),  // мультимедиа дизайн
    WEAVERS(10),    // ткачи
    KNITWEAR(11),   // трикотажники

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
