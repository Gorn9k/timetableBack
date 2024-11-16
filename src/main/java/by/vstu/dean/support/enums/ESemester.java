package by.vstu.dean.support.enums;

import by.vstu.dean.core.enums.BaseEnum;

import java.util.Arrays;
import java.util.List;

public enum ESemester implements BaseEnum<ESemester> {
    SPRING(0),
    AUTUMN(1),
    UNKNOWN(-1);

    final int id;

    ESemester(int id) {
        this.id = id;
    }

    public List<ESemester> getValues() {
        return Arrays.<ESemester>stream(values()).toList();
    }
}
